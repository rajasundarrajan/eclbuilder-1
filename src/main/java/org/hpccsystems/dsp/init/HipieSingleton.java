package org.hpccsystems.dsp.init;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.HIPIEFactory;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.repo.GitRepository;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryType;
import org.hpcc.HIPIE.ws.WsHipie;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.service.SettingsService;
import org.hpccsystems.logging.Trace;
import org.hpccsystems.logging.Trace.TraceLevel;
import org.hpccsystems.usergroupservice.IUserGroupService;
import org.hpccsystems.usergroupservice.UserGroupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zk.ui.util.WebAppInit;

public class HipieSingleton implements WebAppInit, WebAppCleanup {
    private static final String REPOSITORY_CONFIG_FILE = "RepositoryConfigFile";
    private static ServletContext context = null;
    private static String absolutePath = "";
    private static String repositoryConfigFile = "";
    private static String HIPIE = "HIPIE";
    private static String HIPIE_WEBSERVICE = "HIPIE_WEBSERVICE";
    private static final String USER_GROUP_SVS_DOMAIN = "UserGroupSvcDomain";
    private static final String DSPDEV_APPROVER = "DSPDevApprover";
    private static String devApprover;
    /**
     * Name of OUTDATASET. Used to identify contract that writes to file
     */
    public static final String OUTDATASET = "OutDataset";
    public static final String USEDATASET = "UseDataset";
    public static final String ISDEVMODE = "isDevMode";

    private static Contract rawDataset;
    private static Contract scoredSearch;
    private static String ugsDomain;
    
    private static boolean isUpstreamConfigured;
    private static boolean canPromote;

    private static final Logger LOGGER = LoggerFactory.getLogger(HipieSingleton.class);

    @Override
    public void init(WebApp wapp) throws Exception {
        context = wapp.getServletContext();
        String realPath = context.getRealPath("");
        HipieSingleton.absolutePath = realPath;
        
        if (context.getInitParameter(REPOSITORY_CONFIG_FILE) != null) {
            repositoryConfigFile = context.getInitParameter(REPOSITORY_CONFIG_FILE);
        }else{
            throw new HipieException("Repository config file is not defined");
        }
        
        loadHIPIE(false);
                
        initializeDSPResource();
        //TOOD get session timeout from Databse
        wapp.getConfiguration().setSessionMaxInactiveInterval(SettingsService.DEFAULT_SESSION_TIMEOUT_SECONDS);
    }

    private static void loadHIPIE(boolean refresh) throws HipieException {
        HIPIEService rm = null;
        IUserGroupService ugsvc = null;
        LOGGER.info("Initializing HIPIE. refresh - {}", refresh);
        try {
            rm = HIPIEFactory.getInstance().getService(absolutePath + repositoryConfigFile, refresh);
            ugsvc = UserGroupFactory.GetService(context);
        } catch (Exception e) {  
            throw new HipieException(e);
        }
        rm.getPermissionsManager().setAuthManager(ugsvc);
        Trace.setLevel(TraceLevel.INFO);
        WsHipie ws = new WsHipie(rm);
        context.setAttribute(HIPIE, rm);
        context.setAttribute(HIPIE_WEBSERVICE, ws);
        LOGGER.info("HIPIE Initialized");
        
        evaluatePromotion();
    }

    private static void evaluatePromotion() {
        if(isUpstreamConfigured) {
            LOGGER.info("Upstream database is configured");
            LOGGER.info("Checking DefaultComposition repo");
            if(checkUpstreamRepo(getHipie().getRepositoryManager().getDefaultCompositionRepository())) {
                LOGGER.info("Checking Dashbaord repo");
                canPromote = checkUpstreamRepo(getHipie().getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO));
            }
        }
    }
    
    private static boolean checkUpstreamRepo(IRepository repository) {
        if(RepositoryType.GIT != repository.getType()) {
            LOGGER.error("Not a GIT repo. Promotion disabled");
            return false;
        }
        
        Object upstreamRepo = repository.getSaveProperties().get(GitRepository.UPSTREAM_URL);
        if(upstreamRepo == null || StringUtils.isBlank(upstreamRepo.toString())) {
            LOGGER.error("Upstrem repo not available. Promotion is disabled");
            return false;
        }
        
        LOGGER.info("Upstream configuration available");
        return true;
    }
    
    private static void initializeDSPResource() throws HipieException {
        setRawdataset();
        setScoredSearch();
        initializeUserDomain();
    }

    private static void initializeUserDomain() throws HipieException {
        ugsDomain = (String) context.getInitParameter(USER_GROUP_SVS_DOMAIN);

        if (StringUtils.isBlank(ugsDomain)) {
            ugsDomain = "";
            throw new HipieException("UserGroupSvcDomain is not defined");
        }
    }

    private static void setScoredSearch() throws HipieException {
        IRepository dashboardRepo =getHipie().getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO);
        if(dashboardRepo==null){
            throw new HipieException("Dashboard repo is not defined");
        }
        try {
            scoredSearch = getHipie().getContract(null, "ScoredSearchTemplate", dashboardRepo.getName(), null, null);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        if(scoredSearch==null){
            throw new HipieException("Scored search template not available");
        }
    }

    private static void setRawdataset() throws HipieException {
        try {
            rawDataset = getHipie().getContract(null, USEDATASET, getHipie().getRepositoryManager().getDefaultRepository().getName(), null, null);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        if(rawDataset==null){
            throw new HipieException("Could not find raw dataset plugin");
            
        }
    }

    public static void reloadHIPIE() throws HipieException {
        LOGGER.info("Reloading HIPIE. Hash - {}", getHipie().hashCode());
        loadHIPIE(true);
        LOGGER.info("Reloaded. Hash - {}", getHipie().hashCode());
        setRawdataset();
        setScoredSearch();
    }

    public static HIPIEService getHipie() {
        HIPIEService hipie = null;
        try {
            if (context.getAttribute(HIPIE) == null) {
                loadHIPIE(false);
            }
            hipie = (HIPIEService) context.getAttribute(HIPIE);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return hipie;
    }

    public static WsHipie getHipieWebService() {
        WsHipie cb = null;
        try {
            if (context.getAttribute(HIPIE_WEBSERVICE) == null) {
                loadHIPIE(false);
            }
            cb = (WsHipie) context.getAttribute(HIPIE_WEBSERVICE);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return cb;
    }
    public static String getdspDevApprover(){
        devApprover=(String) context.getInitParameter(DSPDEV_APPROVER);
        return devApprover;
    }

    public static Contract getRawDataset() {
        return rawDataset;
    }

    public static Contract getScoredSearch() {
        return scoredSearch;
    }

    public static String getDSPWebAppPath() {
        return absolutePath;
    }

    public static String getUgsDomain() {
        return ugsDomain;
    }

    public static boolean isUpstreamConfigured() {
        return isUpstreamConfigured;
    }

    public static void setUpstreamConfigured(boolean isUpstreamConfigured) {
        HipieSingleton.isUpstreamConfigured = isUpstreamConfigured;
    }

    /**
     * Performs any necessary cleanups on shutdown
     * 
     * @param paramWebApp A ZK container representing the Web Application
     *  
     */
    @Override
    public void cleanup(WebApp paramWebApp) throws Exception {
        for(IRepository repo: getHipie().getRepositoryManager().getRepos().values()) {
            repo.close();
        }
    }

    public static boolean canPromote() {
        return canPromote;
    }
}
