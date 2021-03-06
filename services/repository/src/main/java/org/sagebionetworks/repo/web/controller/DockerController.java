package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * These services allow Synapse to act as an authorization service for a Docker Registry.
 * For more details see: https://github.com/docker/distribution/blob/master/docs/spec/auth/token.md
 * 
 *
 */
@ControllerInfo(displayName="Docker Authorization Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DockerController extends BaseController {
	@Autowired
	ServiceProvider serviceProvider;
	
	/**
	 * Authorize Docker operation.  This service is called by the Docker client only and is not for general use.
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DOCKER_AUTHORIZATION, method = RequestMethod.GET)
	public @ResponseBody
	DockerAuthorizationToken authorizeDockerAccess(
			@RequestParam(value = AuthorizationConstants.DOCKER_USER_NAME_PARAM, required=true) String userName,
			@RequestParam(value = AuthorizationConstants.DOCKER_USER_ID_PARAM, required=true) Long userId,
			@RequestParam(value = AuthorizationConstants.DOCKER_SERVICE_PARAM, required=true) String service,
			@RequestParam(value = AuthorizationConstants.DOCKER_SCOPE_PARAM, required=true) String scope
			) throws NotFoundException {
		return serviceProvider.getDockerService().authorizeDockerAccess(userName, userId, service, scope);
	}

	/*
	 * TODO service to add a commit to a repo.
	 * Note:  If the commit includes a tag then the current commit holding that tag must release it.
	 * Note:  This must also change modifiedBy, modifiedOn for the  entity.
	 */
	
	/*
	 * TODO service to list the commits for a repo.
	 * Might have a param to return just the commits having tags
	 */

	
	/*
	 * TODO Read the Docker password
	 * 
	 */
	
	/*
	 * TODO Delete (invalidate) the Docker password
	 */

}
