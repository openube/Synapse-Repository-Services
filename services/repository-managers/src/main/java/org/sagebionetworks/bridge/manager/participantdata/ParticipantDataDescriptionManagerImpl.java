package org.sagebionetworks.bridge.manager.participantdata;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sagebionetworks.bridge.model.ParticipantDataDescriptorDAO;
import org.sagebionetworks.bridge.model.ParticipantDataId;
import org.sagebionetworks.bridge.model.ParticipantDataStatusDAO;
import org.sagebionetworks.bridge.model.data.ParticipantDataColumnDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataDescriptor;
import org.sagebionetworks.bridge.model.data.ParticipantDataStatus;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.PaginatedResultsUtil;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ParticipantDataDescriptionManagerImpl implements ParticipantDataDescriptionManager {

	@Autowired
	private ParticipantDataDescriptorDAO participantDataDescriptorDAO;

	@Autowired
	private ParticipantDataStatusDAO participantDataStatusDAO;

	@Autowired
	private ParticipantDataIdMappingManager participantDataMappingManager;

	@Override
	public ParticipantDataDescriptor createParticipantDataDescriptor(UserInfo userInfo, ParticipantDataDescriptor participantDataDescriptor) {
		return participantDataDescriptorDAO.createParticipantDataDescriptor(participantDataDescriptor);
	}
	
	@Override
	public void updateParticipantDataDescriptor(UserInfo userInfo, ParticipantDataDescriptor participantDataDescriptor) throws NotFoundException {
		participantDataDescriptorDAO.updateParticipantDataDescriptor(participantDataDescriptor);
	}
	

	@Override
	public ParticipantDataDescriptor getParticipantDataDescriptor(UserInfo userInfo, String participantDataId) throws DatastoreException,
			NotFoundException {
		return participantDataDescriptorDAO.getParticipantDataDescriptor(participantDataId);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getAllParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset) {
		List<ParticipantDataDescriptor> participantDatas = participantDataDescriptorDAO.getParticipantDatas();
		return PaginatedResultsUtil.createPaginatedResults(participantDatas, limit, offset);
	}

	@Override
	public PaginatedResults<ParticipantDataDescriptor> getUserParticipantDataDescriptors(UserInfo userInfo, Integer limit, Integer offset)
			throws IOException, GeneralSecurityException {
		List<ParticipantDataId> participantDataIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		Map<ParticipantDataId, ParticipantDataDescriptor> participantDataDescriptorMap = participantDataDescriptorDAO
				.getParticipantDataDescriptorsForUser(participantDataIds);

		Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap = Maps.newHashMap();
		for (Entry<ParticipantDataId, ParticipantDataDescriptor> entry : participantDataDescriptorMap.entrySet()) {
			participantDataDescriptorToDataIdMap.put(entry.getValue().getId(), entry.getKey());
		}

		ArrayList<ParticipantDataDescriptor> participantDataDescriptors = Lists.newArrayList(participantDataDescriptorMap.values());
		participantDataStatusDAO.getParticipantStatuses(participantDataDescriptors, participantDataDescriptorToDataIdMap);
		return PaginatedResultsUtil.createPaginatedResults(participantDataDescriptors, limit, offset);
	}

	@Override
	public ParticipantDataColumnDescriptor createParticipantDataColumnDescriptor(UserInfo userInfo,
			ParticipantDataColumnDescriptor participantDataColumnDescriptor) {
		return participantDataDescriptorDAO.createParticipantDataColumnDescriptor(participantDataColumnDescriptor);
	}

	@Override
	public PaginatedResults<ParticipantDataColumnDescriptor> getParticipantDataColumnDescriptor(UserInfo userInfo, String participantDataId,
			Integer limit, Integer offset) {
		List<ParticipantDataColumnDescriptor> participantDataColumns = participantDataDescriptorDAO
				.getParticipantDataColumns(participantDataId);
		return PaginatedResultsUtil.createPaginatedResults(participantDataColumns, limit, offset);
	}

	@Override
	public void updateStatuses(UserInfo userInfo, List<ParticipantDataStatus> statuses) throws IOException, GeneralSecurityException {
		List<ParticipantDataId> participantIds = participantDataMappingManager.mapSynapseUserToParticipantIds(userInfo);
		Map<ParticipantDataId, ParticipantDataDescriptor> participantDataDescriptorMap = participantDataDescriptorDAO
				.getParticipantDataDescriptorsForUser(participantIds);

		Map<String, ParticipantDataId> participantDataDescriptorToDataIdMap = Maps.newHashMap();
		for (Entry<ParticipantDataId, ParticipantDataDescriptor> entry : participantDataDescriptorMap.entrySet()) {
			participantDataDescriptorToDataIdMap.put(entry.getValue().getId(), entry.getKey());
		}

		participantDataStatusDAO.update(statuses, participantDataDescriptorToDataIdMap);
	}

	@Override
	public List<ParticipantDataColumnDescriptor> getColumns(String participantDataId) {
		return participantDataDescriptorDAO.getParticipantDataColumns(participantDataId);
	}
}
