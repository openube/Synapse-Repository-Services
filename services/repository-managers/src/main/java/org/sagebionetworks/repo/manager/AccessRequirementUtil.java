package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.dynamo.dao.nodetree.NodeTreeQueryDao;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public class AccessRequirementUtil {
	
	private static final List<Long> EMPTY_LIST = Arrays.asList(new Long[]{});
	
	public static List<Long> unmetAccessRequirementIdsForEntity(
			UserInfo userInfo, 
			String entityId,
			List<String> entityAncestorIds,
			NodeDAO nodeDAO, 
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<ACCESS_TYPE> accessTypes = Collections.singletonList(ACCESS_TYPE.DOWNLOAD);
		List<String> entityIds = new ArrayList<String>();
		entityIds.add(entityId);
		// if the user is the owner of the object, then she automatically 
		// has access to the object and therefore has no unmet access requirements
		Long principalId = userInfo.getId();
		Node node = nodeDAO.getNode(entityId);
		if (node.getCreatedByPrincipalId().equals(principalId)) {
			return EMPTY_LIST;
		}
		// per PLFM-2477, we inherit the restrictions of the node's ancestors
		entityIds.addAll(entityAncestorIds);

		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		
		return accessRequirementDAO.unmetAccessRequirements(entityIds, RestrictableObjectType.ENTITY, principalIds, accessTypes);
	}

	public static List<Long> unmetAccessRequirementIdsForEvaluation(
			UserInfo userInfo, 
			String evaluationId,
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<String> evaluationIds = Collections.singletonList(evaluationId);
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}
		return accessRequirementDAO.unmetAccessRequirements(evaluationIds, RestrictableObjectType.EVALUATION, principalIds, accessTypes);
	}
	
	public static List<Long> unmetAccessRequirementIdsForTeam(
			UserInfo userInfo, 
			String teamId,
			AccessRequirementDAO accessRequirementDAO
			) throws NotFoundException {
		List<String> teamIds = Collections.singletonList(teamId);
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		Set<Long> principalIds = new HashSet<Long>();
		for (Long ug : userInfo.getGroups()) {
			principalIds.add(ug);
		}	
		return accessRequirementDAO.unmetAccessRequirements(teamIds, RestrictableObjectType.TEAM, principalIds, accessTypes);
	}

}
