
package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.info;

import java.security.AccessControlException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CannotViewPermissionsException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.CompositionNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.DdlNotFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoLayoutFoundException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.NoWorkunitsForCompositionException;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.WorkunitNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A helper class for Controllers which handle DSP webservice requests
 * 
 * @author Ashoka_K
 *
 */
@Service("WebServiceHelper")
public class WebServiceHelper {
	
    private static final String FETCHED = "Fetched";

    private static final String FETCHING = "Fetching";

    private static final String COMPOSITION = "composition ";

    private static final Logger LOGGER = LogManager.getLogger(WebServiceHelper.class);
	
    @Autowired
    private DBLogger dbLogger;
    
    private DermatologyServiceImpl dermatologyService;
    
	/**
	 * Fetches the composition corresponding to the composition id.
	 * 
	 * @param userId The id/username of the user who has access to view the composition
	 * @param compositionId The id or the name (either works) of the composition
	 * 
	 * @return The Composition object corresponding to the provided composition id
	 * 
	 * @throws Exception In case an exceptoin occurs when fetching the HIPIEService
	 * @throws CannotViewPermissionsException In case the user does not have rights to view compositions
	 * @throws CompositionNotFoundException In case the specified composition was not found
	 * 
	 */
    public Composition fetchComposition(String userId, String compositionId) throws Exception {
        Composition composition = null;
        
        try {
            logEvent(WebserviceInvocation.ACTION_FETCH_COMPOSITION_INIT, COMPOSITION + compositionId, FETCHING);
            composition =
                HipieSingleton.getHipie().getComposition(userId, compositionId);
            logEvent(WebserviceInvocation.ACTION_FETCH_COMPOSITION_COMPLETED, COMPOSITION + compositionId, FETCHED);
        } catch(AccessControlException e) {
            throw new CannotViewPermissionsException(userId, compositionId, e);
        }
        
        if(composition == null) {
            throw new CompositionNotFoundException(compositionId);
        }
        
        return composition;
    }

    /**
     * Fetches the CompositionInstance/Workunit for the specified composition and workunit id
     * 
     * @param composition The Composition for which the workunit is to be retrieved
     * @param workunitId The id of the workunit that is to be retrieved
     * 
     * @return The CompositionInstance object corresponding to the specified workunti id1
     * 
     * @throws Exception In case an exception occurs when fetching the composition instances for the given
     * composition
     * @throws NoWorkunitsForCompositionException In case the composition has no workunits associated with it
     * @throws WorkunitNotFoundException In case there exists no workunit corresponding to the specified
     * composition and workunit
     */
    public CompositionInstance getCompositionInstance(Composition composition) throws Exception {
        Map<String, CompositionInstance> compositionInstances = composition.getCompositionInstances();
        
        if(compositionInstances.isEmpty()) {
            throw new NoWorkunitsForCompositionException(composition.getName());
        }
        
        CompositionInstance workunit = compositionInstances.values().iterator().next();
        
        info(LOGGER, "Workunit fetched: %s", workunit.getWorkunitId());
        return workunit;
    }
    
    /**
     * Fetches a DDL corresponding to the provided composition
     * 
     * @param composition A compositon instance
     * 
     * @return a DDL corresponding to the provided composition
     * 
     * @throws Exception In case an exception occurs when fetching the workunit info
     * @throws DdlNotFoundException In case there are no DDLs corresponding to the composition 
     */
	public String getDdls(CompositionInstance workunit)
			throws Exception {
		
		Process process=new Process(workunit);
	    String message = format("ddls for composition %s", process.getProjectName());
		
		logEvent(WebserviceInvocation.ACTION_FETCH_DDL_INIT, message, FETCHING);
		List<String> ddls = process.getDDLs(false);
		logEvent(WebserviceInvocation.ACTION_FETCH_DDL_COMPLETED, message, FETCHED);
		info(LOGGER, "DDL fetched: %s", ddls);
		
		String ddl = null;
		
		if(ddls.isEmpty()) {
			throw new DdlNotFoundException(process.getProjectName(), workunit.getWorkunitId());
		}
		ddl = ddls.stream().findFirst().get();
		
		if(isBlank(ddl)) {
            throw new DdlNotFoundException(process.getProjectName(), workunit.getWorkunitId());
        }
		return ddl;
	}
	
	/**
	 * Fetches the layout corresponding to the specified inputs
	 * 
	 * @param dspDao Instance of the DSPDao
	 * @param username The username of the user against which the layout was stored in the database
	 * @param hpccId The label of the HPCC cluster
	 * @param workunitId The id of the workunit against which the layout was stored in the database
	 * @param ddl The DDL against which the layout was stored in the database
	 * @param compositionId The id or name of the composition to which the layout corresponds to. Used
	 * only for error logging and not for retrieving the layout
	 * 
	 * @return The layout corresponding to the specified inputs
	 * 
	 * @throws DatabaseException In case an exception occurs when talking to the database
	 * @throws NoLayoutFoundException In case no layout/database entry could be found corresponding to
	 * the supplied inputs
	 * @throws DermatologyException - Problem getting dermatology information from the service.
	 */
	public String fetchLayout(String username, 
			 String ddl, String compositionId,String cmpVersion, int gcid) 
					throws NoLayoutFoundException, DermatologyException {
		String message = format("layout for username %s, ddl %s, compositionId %s, gcid %s",
				username,ddl,compositionId,gcid);
		
		logEvent(WebserviceInvocation.ACTION_FETCH_LAYOUT_INIT, message, FETCHING);
		String layout = null;
		
		if(gcid == -1) {
		    layout = dermatologyService.getLayout(username, compositionId, cmpVersion, ddl, 0);
		    //fall back for default layout
		    if(isBlank(layout)) {
		        layout = dermatologyService.getLayout(Constants.GENERIC_USER, compositionId, cmpVersion, ddl, 0);
	        }
		} else {
		    layout = dermatologyService.getLayout(username, compositionId, cmpVersion, ddl, gcid);
		    //fall back for default layout
            if(isBlank(layout)) {
                layout = dermatologyService.getLayout(Constants.GENERIC_USER, compositionId, cmpVersion, ddl, gcid);
            }
		}
		
		logEvent(WebserviceInvocation.ACTION_FETCH_LAYOUT_COMPLETED, message, FETCHED);
		info(LOGGER, "Layout fetched: %s", layout);
		
		if(isBlank(layout)) {
		    throw new NoLayoutFoundException(username, compositionId);
		}
		
		return layout;
	}
	
	private void logEvent(String action, String message, String status) {
		if (LOGGER.isInfoEnabled()) {
			dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_SELECT_LAYOUT_INIT, 
		    		message, Instant.now().toEpochMilli()));
		    info(LOGGER, status + " " + message);
		}
	}
  
}
