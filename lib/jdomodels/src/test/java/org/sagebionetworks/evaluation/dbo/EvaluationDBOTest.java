package org.sagebionetworks.evaluation.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EvaluationDBOTest {
 
    @Autowired
	private DBOBasicDao dboBasicDao;
 
    private long ownerId;
    private long id = 1000;
    private String name = "Foo";
    private String eTag = "Bar";
    private Long contentSource = KeyFactory.ROOT_ID;
 
    @Before
    public void before() {
    	ownerId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
    }
    
    @After
    public void after() throws DatastoreException {
        if(dboBasicDao != null){
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", id);
            dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class, params);
        }
    }
    @Test
    public void testCRUD() throws Exception {
        // Initialize a new competition
        EvaluationDBO eval = new EvaluationDBO();
        eval.setId(id);
        eval.seteTag(eTag);
        eval.setName(name);
        eval.setOwnerId(ownerId);
        eval.setCreatedOn(System.currentTimeMillis());
        eval.setContentSource(contentSource);
        eval.setStatusEnum(EvaluationStatus.PLANNED);
        eval.setDescription("my description".getBytes());
        eval.setSubmissionInstructionsMessage("foo".getBytes());
        eval.setSubmissionReceiptMessage("bar".getBytes());
 
        // Create it
        EvaluationDBO clone = dboBasicDao.createNew(eval);
        assertNotNull(clone);
        assertEquals(eval, clone);
        
        // Fetch it
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        clone = dboBasicDao.getObjectByPrimaryKey(EvaluationDBO.class, params);
        assertNotNull(clone);
        assertEquals(eval.getId(), clone.getId());
        assertEquals(eval.getName(), clone.getName());
        
		// Update it
		clone.setDescription("This is a new description".getBytes("UTF-8"));
		clone.setSubmissionInstructionsMessage("baz".getBytes());
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Verify it
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		EvaluationDBO clone2 = dboBasicDao.getObjectByPrimaryKey(EvaluationDBO.class, params);
		assertEquals(clone, clone2);
        
        // Delete it
        result = dboBasicDao.deleteObjectByPrimaryKey(EvaluationDBO.class,  params);
        assertTrue("Failed to delete the type created", result); 
    }
    
    @Test
    public void testSameName() throws Exception{
        // Initialize a new competition
        EvaluationDBO competition = new EvaluationDBO();
        competition.setId(id);
        competition.seteTag(eTag);
        competition.setName(name);
        competition.setOwnerId(ownerId);
        competition.setCreatedOn(System.currentTimeMillis());
        competition.setContentSource(contentSource);
        competition.setStatusEnum(EvaluationStatus.PLANNED);
 
        // Create it
        EvaluationDBO clone = dboBasicDao.createNew(competition);
        clone.setId(id + 1);
        try {
        	dboBasicDao.createNew(clone);
        	fail("Should not be able to create two Competitions with the same name");
        } catch (IllegalArgumentException e) {
        	// Expected name conflict
        }
    }
 
}
