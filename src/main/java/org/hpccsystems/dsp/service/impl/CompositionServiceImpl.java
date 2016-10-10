package org.hpccsystems.dsp.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;


import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.error.HipieErrorCode;
import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.CompositionAccess;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.error.ErrorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;
import org.zkoss.zkplus.spring.SpringUtil;

@Service("compositionService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CompositionServiceImpl implements CompositionService {

    private static final String FETCHED_COMPOSITION_INSTANCES = "Fetched composition instances";

    private static final String ERROR_LOADING_PROJECT = "errorloadingproject";

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionServiceImpl.class);

    private DSPDao dspDao;
//    private DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
    private AuthenticationService authenticationService;

    private static final String RAMPS = "RAMPS";
    DBLogger dbLogger;

    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }

    @Autowired
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Autowired
    public void setDBLogger(DBLogger dbLogger) {
        this.dbLogger = dbLogger;
    }

    @Override
    public List<Project> getProjects(User user) throws CompositionServiceException {
        List<Project> projects = new ArrayList<Project>();

        long startTime = Instant.now().toEpochMilli();
        Map<String, CompositionElement> comps = null;
        try {
            comps = HipieSingleton.getHipie().getCompositions(user.getId());
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new HipieQuery(HipieQuery.GET_COMPOSITIONS, startTime, "Failed fetching projects"));
            }
            throw new CompositionServiceException(Labels.getLabel(ERROR_LOADING_PROJECT), e);
        }

        if (LOGGER.isDebugEnabled()) {
            dbLogger.log(new HipieQuery(HipieQuery.GET_COMPOSITIONS, startTime, "Success fetching projects"));
        }

        LOGGER.debug("Composition for user {} - {}", user.getId(), comps);

        // Remove Basic template from projects list.
        comps.remove(Constants.BASIC_TEMPLATE);
        comps.remove(Dashboard.DASHBOARD_TEMPLATE);

        Project project = null;
        Composition composition = null;
        for (Entry<String, CompositionElement> entry : comps.entrySet()) {
            try {
                // Filter out the dashboard compositions on RAMPS perspective
                if (!Dashboard.CONTRACT_CATAGORY.equals(entry.getValue().getCategory())) {
                    // compositions with contracts that don't exist are being
                    // filtered
                    composition = entry.getValue().getComposition();
                    
                    if (!composition.getParseErrors().getErrors(HipieErrorCode.CONTRACT_NOT_FOUND).isEmpty()) {
                        LOGGER.error(Constants.EXCEPTION,
                                composition.getParseErrors().getErrors(HipieErrorCode.CONTRACT_NOT_FOUND));
                        continue;
                    }
                    project = new Project(entry.getValue(), RAMPS, null);
                                        
                    setReferenceID(entry.getValue().getComposition(), project,null);
                    setIsBatchFile(entry.getValue().getComposition(), project);
                    projects.add(project);
                }
            }  catch (Exception e) {
                LOGGER.debug("Error while processing composition --->{}", entry.getValue());
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }
        // Sort project based on last modified date
        Collections.sort(projects, (p1, p2) -> p2.getLastModifiedDate().compareTo(p1.getLastModifiedDate()));

        LOGGER.debug("Projects - {}", projects);

        return projects;
    }

    private void setIsBatchFile(Composition composition, Project project) {
        project.setBatchTemplate(composition.getInputElements().stream()
                        .filter(element -> GlobalVariable.GCID_COMPLIANCE_TAGS.containsKey(element.getName())
                                || Constants.FCRA.equals(element.getName()) || Constants.INDUSTRY_CLASS.equals(element.getName()))
                        .findFirst().isPresent());
    }

    public List<Project> filterProjects(User user, boolean filter, boolean byAuthor, List<Project> projects) {
        List<Project> filteredProjects = projects;
        if (filter) {
            if (byAuthor) {
                filteredProjects = projects.stream().filter(projec -> user.getId().equals(projec.getAuthor())).collect(Collectors.toList());
            } else {
                filteredProjects = projects.stream().filter(projec -> !user.getId().equals(projec.getAuthor())).collect(Collectors.toList());
            }
        }
        return filteredProjects;
    }

    /**
     * search for the referencrId in composition, if available sets into Project
     * 
     * @param composition
     * @param project
     */
    private void setReferenceID(Composition composition, Project project,Dashboard dashboard) {
        if (composition.getInputElements(InputElement.TYPE_STRING) != null && !composition.getInputElements(InputElement.TYPE_STRING).isEmpty()) {
            Optional<Element> globalVariable = composition.getInputElements(InputElement.TYPE_STRING).stream()
                    .filter(element -> element.getName().equals(Constants.REFERENCE_ID)).findFirst();
            if (globalVariable.isPresent()) {
                if(project==null){
                    dashboard.setReferenceId(globalVariable.get().getOptionValues().iterator().next().getParams().get(0).getName());
                }else{
                    project.setReferenceId(globalVariable.get().getOptionValues().iterator().next().getParams().get(0).getName());
                }
            }
        }
    }

    @Override
    public void assignClusterToProject(List<Project> projects) throws DatabaseException {
            for (Project proj : projects) {
                proj.setClusterConfig(dspDao.getClusterConfig(proj.getCanonicalName()));
            }
    }

    /**
     * Gets projects without last run date
     * 
     * @param comps
     * @param id
     * @param projects
     * @return
     * @throws Exception
     */

    @Override
    public List<Project> getProjectTemplates(User user) throws CompositionServiceException {
        List<Project> projectsTemp = new ArrayList<Project>();

        try {
            Map<String, CompositionElement> compsTemp = HipieSingleton.getHipie().getCompositionTemplates(user.getId());
            for (Entry<String, CompositionElement> entryTemp : compsTemp.entrySet()) {
                Project project = new Project(entryTemp.getValue());
                projectsTemp.add(project);
            }
        } catch (Exception e) {
            throw new CompositionServiceException(Labels.getLabel("errorloadingtemplate"), e);
        }

        LOGGER.debug("Templates - {}", projectsTemp);

        // Filter out Dashboard template
        projectsTemp = projectsTemp.stream().filter(project -> !Dashboard.DASHBOARD_TEMPLATE.equals(project.getName())).collect(Collectors.toList());

        return projectsTemp;
    }

    @Override
    public List<Plugin> getPlugins(Composition composition){
        try {
            return HIPIEUtil.getOrderedPlugins(composition);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            return new ArrayList<Plugin>();
        }
    }

    @Override
    public Composition saveNewCompositionOnHIPIE(String projectName, Composition composition) throws CompositionServiceException {
        Composition savedComposition;
        try {
            long startTime = Instant.now().toEpochMilli();
            composition.setAuthor(authenticationService.getCurrentUser().getId());
            savedComposition = HipieSingleton.getHipie().saveCompositionAs(authenticationService.getCurrentUser().getId(), composition,
                    projectName + ".cmp");
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new CompositionAccess(CompositionAccess.SAVE, savedComposition, startTime));
            }
        } catch (Exception e) {
            throw new CompositionServiceException(Labels.getLabel("unableToSaveComposition"), e);
        }
        return savedComposition;
    }

    @Override
    public void saveClusterConfig(Composition savedComposition, ClusterConfig clusterConfig) throws DatabaseException {
        dspDao.saveClusterConfig(savedComposition.getCanonicalName(), clusterConfig);
    }

    @Override
    public void saveNewCompositionOnDatabase(Project project, Composition savedComposition) throws DatabaseException {

        project.setCanonicalName(savedComposition.getCanonicalName());
        project.setAuthor(savedComposition.getAuthor());
        dspDao.saveClusterConfig(project.getCanonicalName(), project.getClusterConfig());
    }

    @Override
    public void updateProject(Project project, User currentUser, Composition composition) throws CompositionServiceException, DatabaseException {
        try {
            HipieSingleton.getHipie().saveComposition(currentUser.getId(), composition);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new CompositionServiceException(e.getLocalizedMessage(), e);
        }
        project.setAuthor(composition.getAuthor());
    }

    @Override
    public void updateDashboard(Dashboard dashboard, User currentUser, Composition composition) throws CompositionServiceException {
        try {
            HipieSingleton.getHipie().saveComposition(currentUser.getId(), composition);
        } catch (Exception e) {
            throw new CompositionServiceException(e.getLocalizedMessage(), e);
        }
        dashboard.setAuthor(composition.getAuthor());
    }

    @Override
    public ClusterConfig retriveClusteConfig(String canonicalName, String userId) throws DatabaseException {
        try {
            return dspDao.getClusterConfig(canonicalName);
        } catch (DatabaseException d) {
            throw d;
        } catch (Exception exception) {
            LOGGER.error(Constants.EXCEPTION, exception);
            return null;
        }
    }

    @Override
    public void deleteComposition(Composition composition, String userId, boolean deleteservices) throws HipieException, DatabaseException {
        long startTime = Instant.now().toEpochMilli();
        String deleteMessage="Unable to delete composition";
        try {
            LOGGER.info("deleting composition");
            String compositionId = composition.getId();
            
            deleteAccessLog(compositionId, userId, false);
            
            // deleting Visualization DUD file if exists
            HIPIEService hipieService = HipieSingleton.getHipie();
            ContractInstance contractInstance = CompositionUtil.getVisualizationContractInstance(composition);
            CompositionUtil.deleteVisualizationContract(contractInstance);

            // deleting CMP file
            ErrorBlock error = hipieService.deleteComposition(userId, composition, deleteservices);
            checkHipieError(deleteMessage, error);
            
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            dermatologyService.deleteLayout(compositionId);

            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new CompositionAccess(CompositionAccess.DELETE, composition, startTime));
            }
        } catch (DatabaseException d) {
            throw d;
        } catch (HipieException e) {
            throw e;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new HipieQuery(HipieQuery.GET_COMPOSITIONS, startTime, "Failed deleting composition"));
            }
            throw new HipieException(deleteMessage, e);
        }
    }

    private void checkHipieError(String deleteMessage, ErrorBlock error) throws HipieException {
        if (!error.isEmpty()) {
            LOGGER.info("error block ------>{}", error);
            throw new HipieException(deleteMessage);
        }
    }

    @Override
    public boolean saveLayout(String userId, String ddl, String layout,int gcid, String compUuid, String compVersion) throws DatabaseException {
        return dspDao.saveLayout(userId,compUuid, compVersion, ddl, gcid, layout);

    }
    
    @Override
    public List<Dashboard> getDashboards(User user) throws CompositionServiceException {
        long startTime = Instant.now().toEpochMilli();
        List<Dashboard> dashbaords = new ArrayList<Dashboard>();
        Map<String, CompositionElement> comps;

        try {
            comps = HipieSingleton.getHipie().getCompositions(user.getId());
        } catch (Exception e) {

            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new HipieQuery(HipieQuery.GET_COMPOSITIONS, startTime, "Failed fetching dashboards"));
            }
            throw new CompositionServiceException(Labels.getLabel(ERROR_LOADING_PROJECT), e);
        }
        if (LOGGER.isDebugEnabled()) {
            dbLogger.log(new HipieQuery(HipieQuery.GET_COMPOSITIONS, startTime, "Success fetching dashboards"));
        }

           
            LOGGER.debug("Dashboards for user {} - {}", user.getId(), comps);

            // Remove Basic template from projects list.
            comps.remove(Constants.BASIC_TEMPLATE);
            comps.remove(Dashboard.DASHBOARD_TEMPLATE);

            Dashboard dashboard = null;
            for (Entry<String, CompositionElement> entry : comps.entrySet()) {
                try{
                    if (Dashboard.CONTRACT_CATAGORY.equals(entry.getValue().getCategory())) {
                        // compositions with contracts that don't exist are being
                        // filtered
                        if (!entry.getValue().getComposition().getParseErrors().getErrors(HipieErrorCode.CONTRACT_NOT_FOUND).isEmpty()) {
                            LOGGER.error(Constants.EXCEPTION,
                                    entry.getValue().getComposition().getParseErrors().getErrors(HipieErrorCode.CONTRACT_NOT_FOUND));
                            continue;
                        }

                        Composition composition = entry.getValue().getComposition();
                        dashboard = new Dashboard(composition.getName(), composition.getAuthor(), composition.getLabel(), composition.getCanonicalName(),
                            new Date(composition.getLastModified()),composition.getId());
                      
                        if (composition.getName().toLowerCase().endsWith(Dashboard.DATA_BOMB_SUFFIX)) {
                            dashboard.setStaticData(true);
                        }
                        setReferenceID(entry.getValue().getComposition(), null,dashboard);
                        dashbaords.add(dashboard);
                    }
                }catch(Exception e){
                LOGGER.debug("Error while processing dashboard --->{}", entry.getValue());
                    LOGGER.error(Constants.EXCEPTION, e);
                }
                
            }

            // Sort project based on last modified date
            LOGGER.debug("Dashboards - {}", dashbaords);
            return dashbaords;
    }

    @Override
    public List<Dashboard> filterDashboards(User user, boolean filter, boolean byAuthor, List<Dashboard> dashboards) {
        List<Dashboard> filteredDashboards = dashboards;
        if (filter) {
            if (byAuthor) {
                filteredDashboards = dashboards.stream().filter(eachDashboard -> user.getId().equals(eachDashboard.getAuthor())).collect(Collectors.toList());
            } else {
                filteredDashboards = dashboards.stream().filter(eachDashboard -> !user.getId().equals(eachDashboard.getAuthor())).collect(Collectors.toList());
            }
        }
        return filteredDashboards;
    }

    @Override
    public void assignClusterToDshboard(List<Dashboard> dashboards) throws DatabaseException {
            for (Dashboard dash : dashboards) {
                dash.setClusterConfig(dspDao.getClusterConfig(dash.getCanonicalName()));
            }
    }

    @Override
    public boolean isPersPermissionsAvailable() {
        return dspDao.isPersPermissionsAvailable();
    }

    @Override
    public CompositionInstance getmostRecentInstance(Composition comp, boolean doRefresh) throws HipieException {
        CompositionInstance compIns = null;
        long startTime = Instant.now().toEpochMilli();
        try {
            if (doRefresh || comp.getCompositionInstances().isEmpty()) {
                LOGGER.debug("Refreshing contract instances for {}", comp.getCanonicalName());
                comp.refreshCompositionInstances();
            }
            
            if(!comp.getCompositionInstances().isEmpty()) {
                compIns = comp.getCompositionInstances().entrySet().iterator().next().getValue();
            }
            
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new HipieQuery(HipieQuery.ACCESSING_COMP_INSTANCE, startTime, FETCHED_COMPOSITION_INSTANCES));
            }
        } catch (DatabaseException e) {
            LOGGER.debug("Databse execption occured while adding {} log,for composition {}",FETCHED_COMPOSITION_INSTANCES,comp.getCanonicalName());
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        return compIns;
    }
    
    @Override
    public Set<Integer> getLayoutGCIDS(String compUuid, String userId, String ddl) {
        Set<Integer> set = new TreeSet<Integer>();
        
        DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
        try {
            //Getting public GCIDs
            set.addAll(dermatologyService.getLayoutGcIds(Constants.GENERIC_USER, compUuid, ddl));
            //Getting private GCIDs
            set.addAll(dermatologyService.getLayoutGcIds(userId, compUuid, ddl));
        } catch (DermatologyException e) {
            // Return an empty set
            return set;
        }
        
        return set;
    }
    
    @Override
    public void deleteStaticDataFile(StaticData deleteFile) throws DatabaseException {
        dspDao.deleteStaticDataFile(deleteFile);
    }
    
    @Override
    public void addUpdateStaticData(StaticData data) throws DatabaseException {
        dspDao.addUpdateStaticData(data);
    }

    @Override
    public List<StaticData> retrieveStaticData(String userId) throws DatabaseException {
        return dspDao.retrieveStaticData(userId);
    }

    @Override
    public StaticData getStaticData(String userId, String fileName) throws DatabaseException {
        return dspDao.getStaticData(userId,fileName);
    }

    @Override
    public String migrateDermatology() {
        return dspDao.migrateDermatology();
    }

    @Override
    public boolean isMigrationPending() {
        return dspDao.isMigrationPending();
    }
    
	@Override
	public boolean logCompositionAccess(String compId, String userId) throws DatabaseException {
		boolean actionComplete = false;

		if (compId != null && userId != null) {
			actionComplete = dspDao.logCompositionAccess(compId, userId);
		}
		return actionComplete;
	}

	@Override
	public boolean markAsFavoriteComposition(String compId, String userId) throws DatabaseException {
		boolean actionComplete = false;

		if (compId != null && userId != null) {
			actionComplete = dspDao.markAsFavoriteComposition(compId, userId, 1);
			LOGGER.info("Marked as favourite composition :: {}", compId , userId);
		}

		return actionComplete;
	}

	@Override
	public boolean unMarkAsFavoriteComposition(String compId, String userId) throws DatabaseException {
		boolean actionComplete = false;

		if (compId != null && userId != null) {
		    LOGGER.info("Un-Favourite composition :: {}", compId , userId);
			// if access count is equal to zero update favourite flag
			int deletedRecCount = dspDao.deleteCompositionAccessLog(compId, userId, true);
			if (deletedRecCount == 0) {
				// if delete record count is 0
				actionComplete = dspDao.markAsFavoriteComposition(compId, userId, 0);
				LOGGER.info("Marked as Un-favourite composition record :: {}", compId, userId);
			} else {
				actionComplete = true;
				LOGGER.info("Deleted favourite composition :: {}", compId, userId);
			}
		}

		return actionComplete;
	}

	@Override
	public List<String> getCompositionsByAccess(String userId) throws DatabaseException {
		return dspDao.getCompositionsByAccess(userId);
	}

	@Override
	public boolean deleteAccessLog(String compId, String userId, boolean checkCount) throws DatabaseException {
		boolean actionComplete = false;
		if (compId != null && userId != null) {
			// int deletedRecCount =
			dspDao.deleteCompositionAccessLog(compId, userId, checkCount);
			actionComplete = true;
		}
		return actionComplete;
	}
	
	@Override
    public List<String> getFavoriteCompositions(String userId) throws DatabaseException {
        return dspDao.getFavoriteCompositions(userId);
    }
	
	public List<Project> filterProjectsByAccess(User user, List<Project> projects) throws DatabaseException {
        List<Project> accessedProjects = new ArrayList<>();
        List<String> compositionId = getCompositionsByAccess(user.getId());
        if (CollectionUtils.isNotEmpty(compositionId)) {

            compositionId.forEach(composition -> {
                projects.forEach(oneProject -> {
                    try {
                        if (composition.equals(oneProject.getComposition().getId())) {
                            accessedProjects.add(oneProject);
                        }
                    } catch (HipieException e) {
                        LOGGER.error(Constants.EXCEPTION, e);
                    }
                });
            });
        }
        return accessedProjects;
    }
	
	public List<Dashboard> filterDashboardsByAccess(User user, List<Dashboard> dashboards) throws DatabaseException {
        List<Dashboard> accessedDashboards = new ArrayList<>();
        List<String> compositionId = getCompositionsByAccess(user.getId());
        if (CollectionUtils.isNotEmpty(compositionId)) {

            compositionId.forEach(composition -> {
                dashboards.forEach(oneDashboard -> {
                    try {
                        if (composition.equals(oneDashboard.getComposition().getId())) {
                            accessedDashboards.add(oneDashboard);
                        }
                    } catch (HipieException e) {
                        LOGGER.error(Constants.EXCEPTION, e);
                    }
                });
            });
        }
        return accessedDashboards;
    }
	
	public List<Project> filterFavoriteComposition(List<String> compositionId, List<Project> projects) {
        List<Project> favoriteProjects = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(compositionId)) {
            
                projects.forEach(oneProject -> {
                    try {
                        if (compositionId.contains(oneProject.getComposition().getId())) {
                            oneProject.setIsFavourite(true);
                            favoriteProjects.add(oneProject);
                        }
                    } catch (HipieException e) {
                        LOGGER.error(Constants.EXCEPTION, e);
                    }
                });
            
        }
        return favoriteProjects;
    }
    
    public List<Dashboard> filterFavoriteDashboards(List<String> compositionId, List<Dashboard> dashboards) {
        List<Dashboard> favoriteDashboard = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(compositionId)) {

            dashboards.forEach(oneDashboard -> {
                try {
                    if (compositionId.contains(oneDashboard.getComposition().getId())) {
                        oneDashboard.setIsFavourite(true);
                        favoriteDashboard.add(oneDashboard);
                    }
                } catch (HipieException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                }
            });
        }
        return favoriteDashboard;
    }

    public Boolean isAdvancedMode(Composition comp) {
        
        ContractInstance ci = null;
        try {
            ci = comp.getContractInstanceByName(comp.getName() + Dashboard.CONTRACT_IDENTIFIER);    
            
        } catch (Exception e) {
            LOGGER.debug("No dashboard contract instance");
        }
        
        if (ci != null) {
            String property = ci.getProperty(Dashboard.DASHBOARD_MODE);
            
            if (property != null && property.equals(Dashboard.DASHBOARD_MODE_ADVANCED)) {
                return true;
            }
        }
        
        return false;
    }
}
