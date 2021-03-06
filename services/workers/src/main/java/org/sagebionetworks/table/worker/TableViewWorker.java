package org.sagebionetworks.table.worker;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionFactory;
import org.sagebionetworks.repo.manager.table.TableIndexConnectionUnavailableException;
import org.sagebionetworks.repo.manager.table.TableIndexManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.table.RowBatchHandler;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker will completely re-build a table view
 * on any change to a the view schema or scope.
 *
 */
public class TableViewWorker implements ChangeMessageDrivenRunner {
	
	public static final String DEFAULT_ETAG = "DEFAULT";

	static private Logger log = LogManager.getLogger(TableViewWorker.class);
	
	public static int TIMEOUT_MS = 1000*60*10;
	public static int BATCH_SIZE_BYTES = 1024*1024*5; // 5 MBs

	@Autowired
	TableViewManager tableViewManager;
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	TableIndexConnectionFactory connectionFactory;
	

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback,
			ChangeMessage message) throws RecoverableMessageException,
			Exception {
		// This worker is only works on FileView messages
		if(ObjectType.ENTITY_VIEW.equals(message.getObjectType())){
			final String tableId = message.getObjectId();
			final TableIndexManager indexManager;
			try {
				indexManager = connectionFactory.connectToTableIndex(tableId);
				if(ChangeType.DELETE.equals(message.getChangeType())){
					// just delete the index
					indexManager.deleteTableIndex();
					return;
				}else{
					// create or update the index
					createOrUpdateIndex(tableId, indexManager, progressCallback, message);
				}
			} catch (TableIndexConnectionUnavailableException e) {
				// try again later.
				throw new RecoverableMessageException();
			}
		}
	}
	
	/**
	 * Create or update the index for the given table.
	 * 
	 * @param tableId
	 * @param indexManager
	 * @throws RecoverableMessageException 
	 */
	public void createOrUpdateIndex(final String tableId, final TableIndexManager indexManager, ProgressCallback<ChangeMessage> outerCallback, final ChangeMessage message) throws RecoverableMessageException{
		// get the exclusive lock to update the table
		try {
			tableManagerSupport.tryRunWithTableExclusiveLock(outerCallback, tableId, TIMEOUT_MS, new ProgressingCallable<Void, ChangeMessage>() {

				@Override
				public Void call(ProgressCallback<ChangeMessage> innerCallback)
						throws Exception {
					// next level.
					createOrUpdateIndexHoldingLock(tableId, indexManager, innerCallback, message);
					return null;
				}

			} );
		} catch (TableUnavailableException e) {
			// try again later.
			throw new RecoverableMessageException();
		} catch (LockUnavilableException e) {
			// try again later.
			throw new RecoverableMessageException();
		} catch (RecoverableMessageException e) {
			throw e;
		}  catch (Exception e) {
			log.error("Failed to build index: ", e);
		}
	}
	
	/**
	 * Create or update the index for the given table while holding the lock.
	 * 
	 * @param tableId
	 * @param indexManager
	 * @param callback
	 */
	public void createOrUpdateIndexHoldingLock(final String tableId, final TableIndexManager indexManager, final ProgressCallback<ChangeMessage> callback, final ChangeMessage message){
		// Is the index out-of-synch?
		if(tableManagerSupport.isIndexSynchronizedWithTruth(tableId)){
			// nothing to do
			return;
		}
		// Start the worker
		final String token = tableManagerSupport.startTableProcessing(tableId);
		// Look-up the type for this table.
		ViewType viewType = tableManagerSupport.getViewType(tableId);

		// Since this worker re-builds the index, start by deleting it.
		indexManager.deleteTableIndex();
		// Lookup the table's schema
		final List<ColumnModel> currentSchema = tableViewManager.getViewSchemaWithBenefactor(tableId);

		// create the table in the index.
		indexManager.setIndexSchema(currentSchema);
		// Calculate the number of rows per bath based on the current schema
		final int rowsPerBatch = BATCH_SIZE_BYTES/TableModelUtils.calculateMaxRowSize(currentSchema);
		final RowSet rowSetBatch = new RowSet();
		rowSetBatch.setHeaders(TableModelUtils.getSelectColumns(currentSchema));
		rowSetBatch.setTableId(tableId);
		// Stream all of the file data into the index.
		Long viewCRC = tableViewManager.streamOverAllEntitiesInViewAsBatch(tableId, viewType, currentSchema, rowsPerBatch, new RowBatchHandler() {
			
			@Override
			public void nextBatch(List<Row> batch, long currentProgress,
					long totalProgress) {
				// apply the batch to index.
				rowSetBatch.setRows(batch);
				indexManager.applyChangeSetToIndex(rowSetBatch, currentSchema);
				// report progress for each batch
				callback.progressMade(message);
				tableManagerSupport.attemptToUpdateTableProgress(tableId, token, "Building view...", currentProgress, totalProgress);
			}
		});
		
		indexManager.setIndexVersion(viewCRC);
		// Attempt to set the table to complete.
		tableManagerSupport.attemptToSetTableStatusToAvailable(tableId, token, DEFAULT_ETAG);
	}	
	
}
