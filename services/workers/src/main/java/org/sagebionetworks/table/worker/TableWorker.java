package org.sagebionetworks.table.worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.asynchronous.workers.changes.LockTimeoutAware;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableEntityManager;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker updates the index used to support the tables features. It will
 * listen to table changes and apply them to the RDS table that acts as the
 * index of the table feature.
 * 
 * @author John
 * 
 */
public class TableWorker implements ChangeMessageDrivenRunner, LockTimeoutAware {

	enum State {
		SUCCESS, UNRECOVERABLE_FAILURE, RECOVERABLE_FAILURE,
	}
	
	static private Logger log = LogManager.getLogger(TableWorker.class);
	

	@Autowired
	TableEntityManager tableEntityManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	
	Integer lockTimeoutSec;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// If the feature is disabled then we simply swallow all messages
		if (!configuration.getTableEnabled()) {
			return;
		}
		if(lockTimeoutSec == null){
			throw new IllegalStateException("lockTimeoutSec must be set before the worker can be run.");
		}
		// We only care about entity messages here
		if (ObjectType.TABLE.equals((change.getObjectType()))) {
			final String tableId = change.getObjectId();
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(tableId);
			} catch (TableIndexConnectionUnavailableException e) {
				// try again later.
				throw new RecoverableMessageException();
			}
			if (ChangeType.DELETE.equals(change.getChangeType())) {
				// Delete the table in the index
				indexManager.deleteTableIndex();
				return;
			} else {
				// this method does the real work.
				State state = createOrUpdateTable(progressCallback, tableId, indexManager, change);
				if (State.RECOVERABLE_FAILURE.equals(state)) {
					throw new RecoverableMessageException();
				}
			}
		}
	}
	

	/**
	 * This is where a single table index is created or updated.
	 * 
	 * @param tableId
	 * @return
	 */
	public State createOrUpdateTable(
			ProgressCallback<ChangeMessage> progressCallback,
			final String tableId, final TableIndexManager tableIndexManger,
			final ChangeMessage change) {
		// Attempt to run with
		try {
			// Only proceed if work is needed.
			if(!tableManagerSupport.isIndexWorkRequired(tableId)){
				log.info("No work needed for table "+tableId);
				return State.SUCCESS;
			}
			
			/*
			 * Before we start working on the table make sure it is in the processing mode.
			 * This will generate a new reset token and will not broadcast the change.
			 */
			final String tableResetToken = tableManagerSupport.startTableProcessing(tableId);

			// Run with the exclusive lock on the table if we can get it.
			return tableManagerSupport.tryRunWithTableExclusiveLock(progressCallback,tableId,
					lockTimeoutSec,
					new ProgressingCallable<State, ChangeMessage>() {
						@Override
						public State call(ProgressCallback<ChangeMessage> progress) throws Exception {
							// This method does the real work.
							return createOrUpdateWhileHoldingLock(
									progress, tableId, tableIndexManger, tableResetToken,
									change);
						}
					});
		} catch (LockUnavilableException e) {
			// We did not get the lock on this table.
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (ConflictingUpdateException e) {
			// This is thrown when the table gets updated while it is being
			// processed.
			// We cannot continue with this attempt.
			return State.UNRECOVERABLE_FAILURE;
		} catch (NotFoundException e) {
			// This is thrown if the table no longer exits
			return State.UNRECOVERABLE_FAILURE;
		} catch (TableUnavailableException e) {
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (InterruptedException e) {
			// This is a recoverable failure as we can try again later.
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			log.error("Failed with unknown error", e);
			// Cannot recover from this.
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * This is where the table status gets stated and error handling is
	 * performed.
	 * 
	 * @param tableId
	 * @param token
	 * @return
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 */
	private State createOrUpdateWhileHoldingLock(
			ProgressCallback<ChangeMessage> progressCallback, String tableId, TableIndexManager tableIndexManager,
			String tableResetToken, ChangeMessage change)
			throws ConflictingUpdateException, NotFoundException {
		// Start the real work
		log.info("Create index " + tableId);
		try {
			// Save the status before we start
			// This method will do the rest of the work.
			String lastEtag = synchIndexWithTable(progressCallback, tableIndexManager,
					tableId, tableResetToken, change);
			// We are finished set the status
			log.info("Create index " + tableId + " done");
			tableManagerSupport.attemptToSetTableStatusToAvailable(tableId,
					tableResetToken, lastEtag);
			return State.SUCCESS;
		} catch (TableUnavailableException e) {
			// recoverable
			log.info("Create index " + tableId + " aborted, unavailable");
			return State.RECOVERABLE_FAILURE;
		} catch (Exception e) {
			// Failed.
			// Get the stack trace.
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			// Attempt to set the status to failed.
			tableManagerSupport.attemptToSetTableStatusToFailed(tableId,
					tableResetToken, e.getMessage(), writer.toString());
			// This is not an error we can recover from.
			log.info("Create index " + tableId + " aborted, unrecoverable");
			return State.UNRECOVERABLE_FAILURE;
		}
	}

	/**
	 * Synchronizes the table index with the table truth data. After the
	 * successful completion of this method the table index schema will match
	 * the schema of the truth and all row changes that have not already been
	 * applied will be applied. Note: This method will do no work if the index
	 * and truth are already synchronized.
	 * 
	 * @param connection
	 * @param tableId
	 * @param status
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws IOException
	 * @throws TableUnavailableException
	 */
	String synchIndexWithTable(ProgressCallback<ChangeMessage> progressCallback,
			final TableIndexManager indexManager, String tableId, String resetToken,
			ChangeMessage change) throws DatastoreException, NotFoundException,
			IOException, TableUnavailableException {
		// The first task is to get the table schema in-synch.
		// Get the current schema of the table.
		List<ColumnModel> currentSchema = tableManagerSupport
				.getColumnModelsForTable(tableId);
		// Create or update the table with this schema.
		tableManagerSupport.attemptToUpdateTableProgress(tableId, resetToken,
				"Creating table ", 0L, 100L);
		
		// Setup the table's index.
		indexManager.setIndexSchema(currentSchema);

		// List all of the changes
		tableManagerSupport.attemptToUpdateTableProgress(tableId, resetToken,
				"Getting current table row versions ", 0L, 100L);

		// List all change sets applied to this table.
		List<TableRowChange> changes = tableEntityManager.listRowSetsKeysForTable(tableId);
		
		if (changes == null || changes.isEmpty()) {
			/*
			 * If there are no changes for this table then the last etag will be
			 * null and there is nothing else to do.
			 */
			return null;
		}
		
		// Calculate the total work to perform
		long totalProgress = 1;
		for (TableRowChange changSet : changes) {
				totalProgress += changSet.getRowCount();
		}
		// Apply each change set not already indexed
		long currentProgress = 0;
		String lastEtag = null;
		Exception lastFailedException = null;
		for(TableRowChange changeSet: changes){
			progressCallback.progressMade(change);
			currentProgress += changeSet.getRowCount();
			lastEtag = changeSet.getEtag();
			// Only apply changes sets not already applied to the index.
			if(!indexManager.isVersionAppliedToIndex(changeSet.getRowVersion())){
				// This is a change that we must apply.
				RowSet rowSet = tableEntityManager.getRowSet(tableId, changeSet.getRowVersion(), currentSchema);
				tableManagerSupport.attemptToUpdateTableProgress(tableId,
						resetToken, "Applying " + rowSet.getRows().size()
								+ " rows for version: " + changeSet.getRowVersion(), currentProgress,
								totalProgress);
				try {
					// attempt to apply this change set to the table.
					indexManager.applyChangeSetToIndex(rowSet, currentSchema, changeSet.getRowVersion());
					// Clear the exception (we will be removing this in the future).
					lastFailedException = null;
				} catch (Exception e) {
					/*
					 * Log and then skip failures.
					 */
					log.error("Failed to apply a change set to table :"+tableId+" version: "+changeSet.getRowVersion(), e);
					lastFailedException = e;
				}
			}
		}
		if(lastFailedException != null){
			throw new RuntimeException(lastFailedException);
		}
		return lastEtag;
	}


	@Override
	public void setTimeoutSeconds(Long lockTimeoutSec) {
		ValidateArgument.required(lockTimeoutSec, "lockTimeoutSec");
		this.lockTimeoutSec = lockTimeoutSec.intValue();
	}

}
