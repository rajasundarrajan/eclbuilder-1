package org.hpccsystems.dsp.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hpccsystems.dermatology.domain.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.eclBuilder.controller.Builder;
import org.hpccsystems.dsp.eclBuilder.controller.ECLBuilder;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.entity.UIPermission;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.log.DBLog;
import org.hpccsystems.dsp.log.UserLog;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.RAMPSPermission;

/**
 * Dao class,has abstract methods for composition-hpcc connection details
 * related DB hits
 * 
 * @author
 * 
 */
public interface DSPDao {

    /**
     * DAO query and other constants
     */
    
    String VALUE = "value";

    String LAYOUT = "layout";

    String HPCC_CON_ID = "hpcc_con_id";

    String ROLE = "role";

    String USER_ID = "user_id";
    
    String FILE_NAME = "file_name";
    
    String FILE_CONTENET = "file_content";
    
    String USER_LOG_GET_TABLE_FAILED = "userLogGetTableFailed";
    
    String SAVE_PERSPECTIVE_PERMISSION_FAILED = "savePerspectivePermissionFailed";
    
    String SAVE_CLUSTER_CONFIG_FAILED = "saveClusterConfigFailed";
    
    String RAMPS_UPDATE_TABLE_FAILED = "rampsUpdateTableFailed";
    
    String LAYOUT_GET_TABLE_FAILED = "layoutGetTableFailed";
    
    String GROUP_PERMISSION_GET_TABLE_FAILED = "groupPermissionGetTableFailed";
    
    String ERRORDELETEPROJECT = "errordeleteproject";
    
    String DASHBOARD_UPDATE_TABLE_FAILED = "dashboardUpdateTableFailed";
    
    String CLUSTER_CONFIG_FETCH_FAILED = "clusterConfigFetchFailed";
    
    String DATABASE_CONNECTION_FAIL = "databaseConnectionFail";
    
    String UNABLE_TO_UPLOAD_FILE = "uploadFailed";
    
    String RETRIEVE_FAILED = "unableTogetFiles";
    
    String REMOVING_PERMISSION_FAILED = "unableToremovePermisson";
    
    String UPADING_PERMISSION_FAILED = "unableToUpdatePermission";
    
    String UNABLE_TO_DELETE_FILE = "StaticDataFileDeleteFailed";
    
    String DELETE_STATIC_DATA_FILE="DELETE FROM static_data WHERE user_id=? AND file_name=?";
     
    String INSERT_COMPOSITION = "INSERT INTO composition_details(comp_name,hpcc_con_id,hpcc_thor_cluster,hpcc_roxie_cluster) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE hpcc_con_id=?,hpcc_thor_cluster=?,hpcc_roxie_cluster=?";
    String RETRIVE_COMPOSITION = "Select hpcc_con_id,hpcc_thor_cluster,hpcc_roxie_cluster from composition_details where comp_name=?";
    String DELETE_COMPOSITION = "DELETE from composition_details where comp_name=?";
    
    String SELECT_CUSTOM_CLUSTERS = "SELECT hpcc_con_id FROM cluster_settings_permissions WHERE role IN ";
    String SELECT_CUSTOM_GROUPS = "select role from cluster_settings_permissions where hpcc_con_id=?";
    
    String INSERT_LOG = "INSERT INTO user_logs (user_id, session_id, start_time, end_time, memory, action, detail) VALUES (?,?,?,?,?,?,?)";
    String SELECT_USER_LOG = "select user_id,session_id,start_time,action, detail,memory from user_logs where 1=1 and ";
    
    String INSERT_RAMPS_GROUP_VIEW = "INSERT INTO group_permission(group_code,ramps_grid,ramps_list,ramps_default_view,ramps_mandate_company_id,ramps_view_plugin, import_file, keep_ecl) values (?,?,?,?,?,?,?,?)  ON DUPLICATE KEY UPDATE ramps_grid=?,ramps_list=?,ramps_default_view=?,ramps_mandate_company_id=?,ramps_view_plugin=?,import_file=?, keep_ecl=?";
    String INSERT_DASHBOARD_GROUP_VIEW = "INSERT INTO group_permission(group_code,dashboard_grid,dashboard_list,dashboard_default_view,dashboard_mandate_company_id,dashboard_advanced_mode,dashboard_convert_to_comp) values (?,?,?,?,?,?,?)  ON DUPLICATE KEY UPDATE dashboard_grid=?,dashboard_list=?,dashboard_default_view=?,dashboard_mandate_company_id=?,dashboard_advanced_mode=?,dashboard_convert_to_comp=?";
    String INSERT_PERS_PERMISSION = "insert into group_permission(group_code,ramps,dashboard) values (?,?,?)  ON DUPLICATE KEY UPDATE ramps=?,dashboard=?";
    String SELECT_PERS_PERMISSION = "select ramps,dashboard from group_permission where group_code=?";
    String SELECT_PERSPECTIVE = "SELECT perspective FROM dsp_users WHERE  user_id=? LIMIT 1";
    
    String SELECT_CLUSTER_TYPE = "select cluster_type from cluster_settings where hpcc_con_id=? limit 1";
    String SELECT_PUBLIC_CLUSTERS = "select hpcc_con_id from cluster_settings where cluster_type=1";
    String INSERT_OR_UPDATE_PUBLIC_CLUSTER = "INSERT INTO cluster_settings (hpcc_con_id, cluster_type) VALUES(?, ?) ON DUPLICATE KEY UPDATE "
             + " cluster_type=?";
    String DELETE_CLUSTER_CUSTOM_PERMISSIONS = "DELETE FROM cluster_settings_permissions  WHERE hpcc_con_id=?";
    String BATCH_UPDATE_INTO_CUSTOM_PERMISSION_SETTINGS = "INSERT INTO cluster_settings_permissions "
             + "(hpcc_con_id,role) VALUES (?, ?)";
    
    String SELECT_PUBLIC_REPOSITORIES = "select repo_name from repository_settings where repo_type=1";
    String INSERT_OR_UPDATE_PUBLIC_REPOSITORY = "INSERT INTO repository_settings (repo_name, repo_type) VALUES(?, ?) ON DUPLICATE KEY UPDATE "
             + " repo_type=?";
    String SELECT_REPO_TYPE = "select repo_type from repository_settings where repo_name=? limit 1";
    
    String INSERT_OR_UPDATE_STATIC_DATA = "INSERT INTO static_data (user_id, file_name, file_content) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE "
             + " file_content=?";
    String RETRIEVE_STATIC_FILES = "SELECT user_id, file_name, file_content FROM static_data WHERE user_id=? ";
    String GET_STATIC_FILE = "SELECT user_id, file_name, file_content FROM static_data WHERE user_id=? and file_name=? ";
    
    String SELECT_USER_MIGRATION_STATUS = "select user_id,ddl,gcid, max(workunit_id) wuid from dashboard_layout WHERE migration_status = 'Failed' or migration_status is null group by user_id,ddl,gcid";
    
    String GET_LAYOUTS = "SELECT * from dermatology WHERE composition_id = ? AND composition_version = ? AND ddl = ?";
    String GET_LAYOUTS_WITH_USERID = "SELECT * from dermatology WHERE composition_id = ? AND composition_version = ? AND ddl = ? AND user_id = ?";
    
    String GET_VALUE_LIST = "SELECT value FROM application_values WHERE category = ?";

    String GET_ECL_BUILDERS = "SELECT * FROM (SELECT author,name,logicalFiles,lastmodifieddate, eclbuildercode, hpccConnId,wuid, @builderRank := IF(@curr_builder = name, @builderRank + 1, 1)" +
    		"AS builderRank, @curr_builder := name FROM ramps.eclbuilder where author = ? ORDER BY name, lastmodifieddate DESC ) ranked    WHERE builderRank = 1;";
    
    String GET_ECL_BUILDERS_By_Name = "SELECT * FROM (SELECT author,name,logicalFiles,lastmodifieddate, eclbuildercode, hpccConnId,wuid, @builderRank := IF(@curr_builder = name, @builderRank + 1, 1)" +
    		"AS builderRank, @curr_builder := name FROM ramps.eclbuilder where author = ? and name = ? ORDER BY name, lastmodifieddate DESC ) ranked    WHERE builderRank = 1;";

    String GET_ECL_BUILDER = "SELECT * FROM ECLBUILDER WHERE author = ? and name = ? and (hpccConnId = ? or \"\" = ?)  and wuid != '' order by lastmodifieddate desc";
    
    String ADD_ECL_BUILDERS = "INSERT INTO ECLBUILDER VALUES (?,?,?,?,?,?,?)";
    
    String GET_QUERY_BY_WUID = "SELECT * from ECLBUILDER WHERE wuid = ?";
    
    String UPDATE_ECL_BUILDERS = "UPDATE `ramps`.`eclbuilder` SET lastmodifieddate = ? ,eclbuildercode = ? WHERE author = ? and name = ?" ;
    
    String DELETE_ECL_BUILDERS = "DELETE from `ramps`.`eclbuilder` where author = ? and name = ?" ;
    
    /**
     * DAO query and other constants ends
     */
    
    void deleteComposition(String compName, String compId) throws DatabaseException;

    ClusterConfig getClusterConfig(String canonicalName) throws DatabaseException;

    public boolean saveLayout(String userId, String compUuid, String compVersion , String ddl, int gcid, String layout) throws DatabaseException;

    String selectLayout(String compUuid, String compVersion, String userId, int gcid,  String ddl);

    void log(DBLog log);

    List<UserLog> getUserLog() throws DatabaseException;

    void saveClusterConfig(String canaonicalName, ClusterConfig clusterConfig) throws DatabaseException;

    boolean saveRampsUserGroups(String key, RAMPSPermission rampsPermission) throws DatabaseException;

    boolean saveDashboardUserGroups(String key, UIPermission value) throws DatabaseException;

    boolean savePersPermission(String name, Permission permission) throws DatabaseException;

    Permission getPersPermissions(String groupName) throws DatabaseException;

    boolean isPersPermissionsAvailable();

    List<UserLog> getUserLogByDate(Date startDate, Date endDate) throws DatabaseException;

    String getLastViewPerspective(String userId);

    void setLastViewPerspective(String userId, String perspective);

    Map<String, Permission> getGroupPermissions(List<String> groupCodes) throws DatabaseException;

    List<Integer> getLayoutGCIDS(String compUuid, String userId, String ddl);

    boolean isPublicCluster(String cluster);

    void updatePublicCluster(String cluster, boolean isPublicCluster) throws DatabaseException;

    List<String> getPublicClusters() throws DatabaseException;
    
    List<String> getPublicRepositories() throws DatabaseException;
    
    void updatePublicRepository(String repo, boolean isPublicRepo) throws DatabaseException;
    
    boolean isPublicRepo(String repo);
    
    List<String> getCustomClusters(List<String> roles) throws DatabaseException;
   
    List<String> getCustomGroups(String cluster) throws DatabaseException;
    
    void updateCustomClusterPermissions(List<String> groups, String cluster) throws DatabaseException;
  
    void removeOlderCustomPermissions(String cluster) throws DatabaseException;
    
    void addUpdateStaticData(StaticData data) throws DatabaseException;
    
    List<StaticData> retrieveStaticData(String userId) throws DatabaseException;
    
    StaticData getStaticData(String userId, String fileName) throws DatabaseException;
    
    String getAppSetting(String name);
    
    void saveAppSetting(String name, String value);
    
    String migrateDermatology();

    boolean isMigrationPending();

    List<Dermatology> getLayouts(String compID,String compVersion,String ddl,String userId) throws DatabaseException;
    
    List<Dermatology> getLayouts(String compID,String compVersion, String ddl) throws DatabaseException;

    List<Dermatology> getDermatology(String compositonId,String compositonVersion);
    
    void copyDermatology(String id, String oldCompversion, String newCompversion);

    List<String> getApplicationValueList(String category) throws DatabaseException;

    void deleteStaticDataFile(StaticData deleteFile) throws DatabaseException;
    
    boolean logCompositionAccess(String compId, String userId) throws DatabaseException;
    
    boolean markAsFavoriteComposition(String compId, String userId, int isFavorite) throws DatabaseException;
    
    List<String> getCompositionsByAccess(String userId) throws DatabaseException;
    
    int deleteCompositionAccessLog(String compId, String userId,boolean checkCount) throws DatabaseException;
    
    List<String> getFavoriteCompositions(String userId) throws DatabaseException;

    List<String> getAdvancedModeGroups();
    
    List<String> getConvertToCompGroups();
	
	List<Builder> getECLBuilders(String userId) throws DatabaseException;
    
    List<Builder> getECLBuildersByName(String author, boolean byName, String name) throws DatabaseException;
    
    List<Builder> getECLBuilder(String userId, String builderName, String hpccId) throws DatabaseException;

	int addOrUpdateECLBuilders(ECLBuilder eclBuilderDetails, boolean addOrUpdate) throws DatabaseException;
	
	int deleteECLBuilder(String author, String name) throws DatabaseException;
	
	String getECLQueryByWUID(String wuid) throws DatabaseException;
}
