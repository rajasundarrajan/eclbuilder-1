package org.hpccsystems.dsp.webservices;

import static org.apache.log4j.LogManager.getLogger;
import static org.hpccsystems.util.ExceptionValidator.validateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CannotViewPermissionsException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CompositionNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.DdlNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoLayoutFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoWorkunitsForCompositionException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WorkunitNotFoundException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.context.ContextConfiguration;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HipieSingleton.class, DermatologyServiceImpl.class})
@ContextConfiguration
public class WebserviceHelperTest {
	
	private static final Logger LOGGER = getLogger(WebServiceHelper.class);
	
	@Mock private Composition composition;
	
	@Mock private HIPIEService hipieService;
	
	@Mock private CompositionInstance workunit1, workunit2, workunit3;
	
	@Mock private CompositionService cmpService;
	
	@Mock private DermatologyServiceImpl dermatologyService;
	
	@Mock private org.hpccsystems.dsp.ramps.entity.Process process;
	
	@Mock private DBLogger dbLogger;
	
	@InjectMocks
	private WebServiceHelper helper = new WebServiceHelper();
	
	private TreeMap<String, CompositionInstance> compositionInstances = new TreeMap<String, CompositionInstance>();	
	
	private List<String> ddls = new ArrayList<String>();
	
	@Before
	public void setup() throws Exception {
		LOGGER.setLevel(Level.TRACE);
	    MockitoAnnotations.initMocks(this);
	    
	    when(composition.getName()).thenReturn("myComposition");
	    
		setupForFetchComposition();
		setupForGetCompositionInstance();
		setupForGetDdls();
	}

    private void setupForGetDdls() throws Exception {
        when(workunit1.getCurrentUsername()).thenReturn("myCurrentUsername");
        when(workunit1.getWorkunitId()).thenReturn("myWorkunitId");
        when(workunit1.getComposition()).thenReturn(composition);
        when(workunit1.getWorkunitStatus()).thenReturn("mystatus");
        
        //when(new Process(workunit1)).thenReturn(process);
        when(process.getProjectName()).thenReturn("mylabel");
        when(process.getDDLs(eq(false))).thenReturn(ddls);
    }

    private void setupForGetCompositionInstance() throws Exception {
        when(composition.getCompositionInstances()).thenReturn(compositionInstances);
    }

    private void setupForFetchComposition() {        
		mockStatic(HipieSingleton.class);
		when(HipieSingleton.getHipie()).thenReturn(hipieService);
		when(HipieSingleton.getHipie()).thenReturn(hipieService);
    }
    
    private void populateCompositionInstances() {
        compositionInstances.put("wu1", workunit1);
        compositionInstances.put("wu2", workunit2);
        compositionInstances.put("wu3", workunit3);
    }
    
    @Test 
    public void verifyLayoutAvailableScenario() throws DatabaseException, NoLayoutFoundException, DermatologyException {
        when(dermatologyService.getLayout(eq("myUserId"), eq("myCompositionId"), eq("myVersion"), eq("myDdl"), eq(1234))).
            thenReturn("myLayout");
        
        // Passing generic user
        when(dermatologyService.getLayout(eq(Constants.GENERIC_USER), eq("myCompositionId"), eq("myVersion"), eq("myDdl"), eq(1234))).
            thenReturn("myLayout");
        
        String layout = helper.fetchLayout("myUserId", "myDdl", "myCompositionId","myVersion", 1234);
        assertEquals("myLayout", layout);
        
        //Passing 0 - for method signature without GCID
        when(dermatologyService.getLayout(eq("myUserId"), eq("myCompositionId"), eq("myVersion"), eq("myDdl"), eq(0))).
            thenReturn("myLayout2");
        
        // Passing generic user and 0 gcid
        when(dermatologyService.getLayout(eq(Constants.GENERIC_USER), eq("myCompositionId"), eq("myVersion"), eq("myDdl"), eq(0))).
            thenReturn("myLayout2");
    
        layout = helper.fetchLayout("myUserId", "myDdl", "myCompositionId","myVersion", -1);
        assertEquals("myLayout2", layout);
    }
    
    @Ignore
    public void verifyBlankLayoutScenario() throws Exception {
        validateExceptionForBlankLayoutScenario();
        when(dermatologyService.getLayout(eq("myUserId"), eq("myCompositionId"), eq("myVersion"), eq("myDdl"), eq(1234))).thenReturn(" ");
        validateExceptionForBlankLayoutScenario();
    }

    private void validateExceptionForBlankLayoutScenario() throws Exception {
        validateException(helper, "fetchLayout",
                new Class[]{String.class, String.class, String.class, String.class, int.class},
                NoLayoutFoundException.class, "System Error. No layout could be found for the given inputs, "
                        + "i.e. username: myUserId, cmpId: myCompositionId, wuId: myWorkunitId", (Throwable) null,
                (Class<?>) null, "myUserId", "myDdl", "myCompositionId","myVersion", 1234);
    }
    
    @Ignore
    public void verifyDdlsMissingScenario() throws Exception {
        validateExceptionForMissingDdls();
        ddls.add(" ");
        validateExceptionForMissingDdls();
    }

    private void validateExceptionForMissingDdls() throws Exception {
        validateException(helper, "getDdls",
                new Class[]{CompositionInstance.class}, DdlNotFoundException.class,
                "No DDL found corresponding to composition myComposition, work unit myWorkunitId",
                (Throwable) null, (Class<?>) null,  workunit1);
    }
    
    @Ignore
    public void verifyDdlsAvailableScenario() throws HipieException, Exception {
        ddls.add("ddl1");
        assertEquals("ddl1", helper.getDdls(workunit1));
        
        ddls.add("ddl2");
        assertEquals("ddl1", helper.getDdls(workunit1));
    }
    
    @Test
    public void verifyBlankWuIdScenario() throws Exception {
        populateCompositionInstances();
        
        assertSame(workunit1, helper.getCompositionInstance(composition));
        assertSame(workunit1, helper.getCompositionInstance(composition));
    }
    
    @Ignore
    public void verifyValidWorkUnitScenario() throws Exception {
        populateCompositionInstances();
        
        assertSame(workunit1, helper.getCompositionInstance(composition));
        assertSame(workunit3, helper.getCompositionInstance(composition));
    }
    
    @Ignore
    public void verifyInvalidWuIdScenario() throws Exception {
        populateCompositionInstances();
        
        validateException(helper, "getCompositionInstance",
                new Class[]{Composition.class}, WorkunitNotFoundException.class,
                "No workunit wuNonExistent found for composition myComposition", (Throwable) null,
                (Class<?>) null, composition);
    }
	
	@Test
	public void verifyNoWorkunitsScenario() throws Exception {
	    validateException(helper, "getCompositionInstance",
	            new Class[]{Composition.class}, NoWorkunitsForCompositionException.class,
                "No workunits found for composition myComposition", (Throwable) null,
                (Class<?>) null, composition);
	}
	
	@Test
	public void verifyFetchComposition() throws Exception {
		when(hipieService.getComposition(eq("myUserId"), eq("myCompositionId"))).thenReturn(composition);		
		assertSame(composition, helper.fetchComposition("myUserId", "myCompositionId"));
	}
	
	@Test
	public void verifyNoPermissionsScenario() throws Exception {
		AccessControlException cause = new AccessControlException("message");
		when(hipieService.getComposition(eq("myUserId"), eq("myCompositionId"))).thenThrow(cause);
		
		validateException(helper, "fetchComposition", CannotViewPermissionsException.class,
		        "User myUserId does not have permissions to view composition myCompositionId", cause,
		        "myUserId", "myCompositionId");
	}
	
	@Test
	public void verifyNullCompositionScenario() throws Exception {
		when(hipieService.getComposition(eq("myUserId"), eq("myCompositionId"))).thenReturn(null);
		
		validateException(helper, "fetchComposition", CompositionNotFoundException.class,
                "No composition found for composition myCompositionId", (Throwable) null,
                "myUserId", "myCompositionId");
	}

}
