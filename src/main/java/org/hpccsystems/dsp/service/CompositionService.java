package org.hpccsystems.dsp.service;

import java.util.List;
import java.util.Set;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.springframework.dao.DataAccessException;

public interface CompositionService {
    List<Project> getProjects(User user) throws CompositionServiceException;
    
    List<Dashboard> getDashboards(User user) throws CompositionServiceException;

    List<Project> getProjectTemplates(User user) throws CompositionServiceException;

    /**
     * Saves a new composition in HIPIE and Adds a new entry in RAMPS DB
     * 
     * @throws CompositionServiceException
     * @throws DatabaseException
     */
    Composition saveNewCompositionOnHIPIE(String projectName, Composition composition) throws CompositionServiceException;
    
     void saveClusterConfig(Composition savedComposition, ClusterConfig clusterConfig) throws DatabaseException;
    
     void saveNewCompositionOnDatabase(Project project, Composition savedComposition) throws DatabaseException;
    

    /**
     * Updated existing composition in HIPIE & Project in RAMPS DB When no
     * entries found in DB to update, inserts a new row
     * 
     * @param project
     * @throws CompositionServiceException 
     * @throws DatabaseException 
     * @throws DataAccessException
     * @throws Exception
     */
     void updateProject(Project project, User currentUser, Composition composition) throws CompositionServiceException, DatabaseException;
    
     void updateDashboard(Dashboard project, User currentUser,
            Composition composition) throws CompositionServiceException;


    ClusterConfig retriveClusteConfig(String name, String userId) throws DatabaseException;
    

    void deleteComposition(Composition projectName, String userId, boolean deleteservices) throws HipieException, DatabaseException;

    List<Plugin> getPlugins(Composition composition);
    
    void assignClusterToProject(List<Project> projects) throws DatabaseException;
    
    void assignClusterToDshboard(List<Dashboard> dashboards) throws DatabaseException;
    
    boolean saveLayout(String userId, String ddl, String layout,int gcid, String compUuid, String compVersion)  throws DatabaseException;
    
    boolean isPersPermissionsAvailable();
    
    CompositionInstance getmostRecentInstance(Composition comp, boolean doRefresh) throws HipieException;

    Set<Integer> getLayoutGCIDS(String compUuid, String userId, String ddl);
  
    List<Dashboard> filterDashboards(User user, boolean filter,  boolean byAuthor, List<Dashboard> dashbaords);
     
    List<Project> filterProjects(User user, boolean filter,  boolean byAuthor, List<Project> projects);
     
    void addUpdateStaticData(StaticData data) throws DatabaseException;
     
    List<StaticData> retrieveStaticData(String userId) throws DatabaseException;
     
    StaticData getStaticData(String userId,String fileName) throws DatabaseException;

    String migrateDermatology();

    boolean isMigrationPending();
    
    boolean logCompositionAccess(String compId, String userId) throws DatabaseException;
    
    boolean markAsFavoriteComposition(String compId, String userId) throws DatabaseException;
    
    boolean unMarkAsFavoriteComposition(String compId, String userId) throws DatabaseException;
    
    List<String> getCompositionsByAccess(String userId) throws DatabaseException;
    
    boolean deleteAccessLog(String compId, String userId, boolean checkCount) throws DatabaseException;

    void deleteStaticDataFile(StaticData deleteFile) throws DatabaseException;
    
    List<Project> filterProjectsByAccess(User user,List<Project> projects) throws DatabaseException;
    
    List<Dashboard> filterDashboardsByAccess(User user, List<Dashboard> dashboards) throws DatabaseException;
    
    public List<String> getFavoriteCompositions(String userId) throws DatabaseException;
    
    List<Project> filterFavoriteComposition(List<String> compositionId, List<Project> projects);
    
    List<Dashboard> filterFavoriteDashboards(List<String> compositionId, List<Dashboard> dashboards);
    
    public Boolean isAdvancedMode(Composition comp) throws Exception;
}
