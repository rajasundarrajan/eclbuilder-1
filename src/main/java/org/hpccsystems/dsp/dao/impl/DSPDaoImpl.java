
package org.hpccsystems.dsp.dao.impl;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dermatology.domain.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.eclBuilder.controller.Builder;
import org.hpccsystems.dsp.eclBuilder.controller.ECLBuilder;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.entity.UIPermission;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.DBLog;
import org.hpccsystems.dsp.log.UserLog;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.RAMPSPermission;
import org.hpccsystems.dsp.rowmapper.GroupPermissionExtractor;
import org.hpccsystems.dsp.rowmapper.UserLogRowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;

import com.mchange.v2.c3p0.DataSources;

/**
 * Dao class to do widget related DB hits
 * 
 * @author
 * 
 */
@Service("dspDao")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DSPDaoImpl implements DSPDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(DSPDaoImpl.class);

    private JdbcTemplate jdbcTemplate;
    
    private ResultSetExtractor<List<Dermatology>> dermatologyExtractor = new ResultSetExtractor<List<Dermatology>>() {
        @Override
        public List<Dermatology> extractData(ResultSet resultset) throws SQLException, DataAccessException {
            List<Dermatology> layouts = new ArrayList<Dermatology>();
            while (resultset.next()) {
                Dermatology derm = new Dermatology();
                derm.setCompositionId(resultset.getString("composition_id"));
                derm.setCompositionVersion(resultset.getString("composition_version"));
                derm.setUserId(resultset.getString("user_id"));
                derm.setGcid(resultset.getInt("gcid"));
                derm.setDdl(resultset.getString("ddl"));
                derm.setLayout(resultset.getString("layout"));
                derm.setModifiedDate(resultset.getDate("modified_date"));
                LOGGER.debug("derm ----{}", derm);
                layouts.add(derm);
            }
            
            return layouts;
        }
    };

    private ResultSetExtractor<List<Builder>> eclBuilderExtractor = new ResultSetExtractor<List<Builder>>() {
        @Override
        public List<Builder> extractData(ResultSet resultset) throws SQLException, DataAccessException {
            List<Builder> builders = new ArrayList<Builder>();
            while (resultset.next()) {
            	Builder build = new Builder();
            	build.setAuthor(resultset.getString("author"));
            	build.setLogicalFiles(resultset.getString("logicalFiles"));
            	build.setName(resultset.getString("name"));
            	build.setLastmodifieddate(resultset.getDate("lastmodifieddate"));

            	Blob blob = ((Blob)resultset.getBlob("eclbuildercode"));
            	byte[] bdata = blob.getBytes(1, (int) blob.length());
            	build.setEclbuildercode(new String(bdata));
            	
            	build.setHpccId(resultset.getString("hpccConnId"));
            	build.setWuID(resultset.getString("wuid"));
                LOGGER.debug("derm ----{}", build);
                builders.add(build);
            }

            return builders;
        }
    };

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Autowired
    @Qualifier("mySQLDataSource")
    public void setDataSourceToJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void initialize() {
        LOGGER.info("Testing DSP Database connection.");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            LOGGER.info("Test connection success.");
        } catch (Exception e) {
            LOGGER.info("Test connection failed.");
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    @Override
    public void saveClusterConfig(String canaonicalName, ClusterConfig clusterConfig) throws DatabaseException {
        try {
            getJdbcTemplate().update(INSERT_COMPOSITION,
                    new Object[] { canaonicalName, clusterConfig.getId(), clusterConfig.getThorCluster(),
                            clusterConfig.getRoxieCluster(), clusterConfig.getId(), clusterConfig.getThorCluster(),
                            clusterConfig.getRoxieCluster() });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(SAVE_CLUSTER_CONFIG_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(SAVE_CLUSTER_CONFIG_FAILED), f);
        }
    }

    @Override
    public ClusterConfig getClusterConfig(final String cannonicalName) throws DatabaseException {
        try {
            return getJdbcTemplate().query(RETRIVE_COMPOSITION, new Object[] { cannonicalName },
                    new ResultSetExtractor<ClusterConfig>() {
                        @Override
                        public ClusterConfig extractData(ResultSet rs) throws SQLException {
                            ClusterConfig clusterConfig = null;
                            while (rs.next()) {
                                clusterConfig = new ClusterConfig();
                                clusterConfig.setId(rs.getString(HPCC_CON_ID));
                                clusterConfig.setThorCluster(rs.getString("hpcc_thor_cluster"));
                                clusterConfig.setRoxieCluster(rs.getString("hpcc_roxie_cluster"));
                            }
                            return clusterConfig;
                        }
                    });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(CLUSTER_CONFIG_FETCH_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL),
                    e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(CLUSTER_CONFIG_FETCH_FAILED), f);
        }
    }

    @Override
    public void deleteComposition(String compName, String compId) throws DatabaseException {
        try {
            LOGGER.info("Delete Composition called with canonicalname {} and Uuid {}.", compName, compId);
            getJdbcTemplate().update(DELETE_COMPOSITION, new Object[] { compName });

            // Deleting dermatology associated with composition
            getJdbcTemplate().update("DELETE FROM dermatology WHERE composition_id = ?", new Object[] { compId });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(ERRORDELETEPROJECT) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception exception) {
            throw new DatabaseException(Labels.getLabel(ERRORDELETEPROJECT), exception);
        }

    }

    @Override
    public boolean saveLayout(String userId, String compUuid, String compVersion, String ddl, int gcid, String layout)
            throws DatabaseException {
        int rows = 0;
        try {
            LOGGER.debug(
                    "Save Layout with User ID - {},COmposition - {}, ddl {} and gcid {} \nLayout -  {} \ncompVersion - {}",
                    userId, compUuid, ddl, gcid, layout, compVersion);

            Date date = new Date();
            rows = getJdbcTemplate().update(
                    "INSERT INTO dermatology (composition_id, composition_version, user_id, gcid, ddl, layout, modified_date) "
                            + "VALUES(?, ?, ?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE "
                            + "layout = ?, modified_date = ?",
                    new Object[] { compUuid, compVersion, userId, gcid, ddl, layout, date, layout, date });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(LAYOUT_GET_TABLE_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(LAYOUT_GET_TABLE_FAILED), f);
        }
        return rows > 0;
    }

    @Override
    public String selectLayout(String compUuid, String compVersion, String userId, int gcid, String ddl) {
       String layout = null;
        try {
            LOGGER.debug("Getting layout for Composition {} v{}, user {}, GCID {}, ddl {}", compUuid, compVersion,
                    userId, gcid, ddl);
            layout = getJdbcTemplate().query("SELECT layout from dermatology "
                    + "WHERE composition_id = ? AND composition_version = ? AND user_id = ? AND gcid = ? AND ddl = ?", 
                    new Object[] { compUuid, compVersion, userId, gcid,  ddl }, 
                    resultSet -> {
                        String dbData = null;
                        while (resultSet.next()) {
                            dbData = resultSet.getString(LAYOUT);
                        }
                        return dbData;
                    });

        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error - {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return layout;
    }

    @Override
    public List<Dermatology> getLayouts(String compID,String compVersion,String ddl,String userId) throws DatabaseException {
        List<Dermatology> layouts = null;
        try {
            LOGGER.debug("Getting layouts for Composition version{}, user {}, ddl {}", compVersion, userId, ddl);
            layouts = getJdbcTemplate().query(GET_LAYOUTS_WITH_USERID,new Object[] {compID,compVersion,ddl,userId }, dermatologyExtractor);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error -  {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return layouts;
    }
    
    @Override
    public List<Dermatology> getLayouts(String compID,String compVersion,String ddl) throws DatabaseException {
        List<Dermatology> layouts = null;
        try {
            LOGGER.debug("Getting layouts for Composition version{}, ddl {}", compVersion,ddl);
            layouts = getJdbcTemplate().query(GET_LAYOUTS,new Object[] { compID,compVersion,ddl }, dermatologyExtractor);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error -   {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return layouts;
    }

    @Override
    public void log(DBLog log) {
        try {
            getJdbcTemplate().update(INSERT_LOG, new Object[] { log.getUserId(), log.getSessionId(), log.getStartTime(),
                    log.getEndTime(), log.getMemoryUtilized(), log.getAction(), log.getDetail() });
        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
    }

    @Override
    public List<UserLog> getUserLog() throws DatabaseException {
        try {
            String condition = "FROM_UNIXTIME(start_time/1000) > DATE_SUB(now(),INTERVAL 7 DAY)";
            return jdbcTemplate.query(SELECT_USER_LOG.concat(condition).concat(" ORDER BY start_time DESC"),
                    new UserLogRowMapper());
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(USER_LOG_GET_TABLE_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(USER_LOG_GET_TABLE_FAILED), f);
        }
    }

    @Override
    public boolean saveRampsUserGroups(String groupCode, RAMPSPermission rampsPermission) throws DatabaseException {
        try {
            int viewGrid = checkViewGridRampsPermission(rampsPermission);
            int viewList = checkViewListRampsPermission(rampsPermission);
            int mandateCID = checkMandateGCIDRampsPermission(rampsPermission);
            int canViewPlugin = rampsPermission.canViewPluginSource() ? 1 : 0;
            int canImportFile = rampsPermission.canImportFile() ? 1 : 0;
            int canKeepECL = rampsPermission.isKeepECL() ? 1 : 0;
            LOGGER.debug("Ramps permission to be saved for groupcode {} is {}", groupCode, rampsPermission.toString());
            int rows = getJdbcTemplate().update(INSERT_RAMPS_GROUP_VIEW,
                    new Object[] { groupCode, viewGrid, viewList,
                            rampsPermission.getUiPermission().getDefaultView() == null ? ""
                                    : rampsPermission.getUiPermission().getDefaultView(),
                                    mandateCID, canViewPlugin, canImportFile, canKeepECL, viewGrid, viewList,
                            rampsPermission.getUiPermission().getDefaultView() == null ? ""
                                    : rampsPermission.getUiPermission().getDefaultView(),
                                    mandateCID, canViewPlugin, canImportFile, canKeepECL });
            return rows > 0;
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(RAMPS_UPDATE_TABLE_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(RAMPS_UPDATE_TABLE_FAILED), f);
        }
    }

    private int checkMandateGCIDRampsPermission(RAMPSPermission rampsPermission) {
        return rampsPermission.getUiPermission().isCompanyIdMandatory() ? 1 : 0;
    }

    private int checkViewListRampsPermission(RAMPSPermission rampsPermission) {
        return rampsPermission.getUiPermission().canViewList() ? 1 : 0;
    }

    private int checkViewGridRampsPermission(RAMPSPermission rampsPermission) {
        return rampsPermission.getUiPermission().canViewGrid() ? 1 : 0;
    }

    @Override
    public boolean saveDashboardUserGroups(String groupCode, UIPermission uiPermission) throws DatabaseException {
        try {
            int viewGrid = checkViewGirdPermission(uiPermission);
            int viewList = uiPermission.canViewList() ? 1 : 0;
            int mandateCID = uiPermission.isCompanyIdMandatory() ? 1 : 0;
            int advancedMode = uiPermission.isAllowedAdvancedMode() ? 1 : 0;
            int convrtToComp = uiPermission.isAllowedConvertToComp() ? 1 : 0;
            LOGGER.debug("Dashboard permission to be saved for groupcode {} is {}", groupCode, uiPermission.toString());
            int rows = getJdbcTemplate().update(INSERT_DASHBOARD_GROUP_VIEW,
                    new Object[] { groupCode, viewGrid, viewList,
                            uiPermission.getDefaultView() == null ? "" : uiPermission.getDefaultView(), mandateCID, advancedMode, convrtToComp,
                            viewGrid, viewList,
                            uiPermission.getDefaultView() == null ? "" : uiPermission.getDefaultView(), mandateCID, advancedMode, convrtToComp });
            return rows > 0;
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(DASHBOARD_UPDATE_TABLE_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL),
                    e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DASHBOARD_UPDATE_TABLE_FAILED), f);
        }
    }
    
    @Override
    public List<String> getAdvancedModeGroups() {
        String sql = "SELECT group_code FROM group_permission WHERE dashboard_advanced_mode = ?";
        try {
            return getJdbcTemplate().query(sql, new Object[]{"1"}, 
                    new ResultSetExtractor<List<String>>() {
                        @Override
                        public List<String> extractData(ResultSet rs) throws SQLException {
                            List<String> advancedModeGroups = new ArrayList<String>();
                            while (rs.next()) {
                                advancedModeGroups.add(rs.getString("group_code"));
                            }
                            return advancedModeGroups;
                    }
            });
        } catch (DataAccessException e) {
            LOGGER.error("Failed to get advanced mode groups from the database. Returning null.\n{}", e);
            return null;
        }
    }
    
    @Override
    public List<String> getConvertToCompGroups() {
        String sql = "SELECT group_code FROM group_permission WHERE dashboard_convert_to_comp = ?";
        try {
            return getJdbcTemplate().query(sql, new Object[]{"1"}, 
                    new ResultSetExtractor<List<String>>() {
                        @Override
                        public List<String> extractData(ResultSet rs) throws SQLException {
                            List<String> convertToCompGroups = new ArrayList<String>();
                            while (rs.next()) {
                                convertToCompGroups.add(rs.getString("group_code"));
                            }
                            return convertToCompGroups;
                    }
            });
        } catch (DataAccessException e) {
            LOGGER.error("Failed to get advanced mode groups from the database. Returning null.\n{}", e);
            return null;
        }
    }

    private int checkViewGirdPermission(UIPermission uiPermission) {
        return uiPermission.canViewGrid() ? 1 : 0;
    }

    @Override
    public boolean savePersPermission(String groupCode, Permission permission) throws DatabaseException {
        try {
            int rows = getJdbcTemplate().update(INSERT_PERS_PERMISSION, (PreparedStatementSetter) statement -> {
                int canViewRamps = permission.canViewRamps() ? 1 : 0;
                int canViewDashboard = permission.canViewDashboard() ? 1 : 0;
                statement.setString(1, groupCode);
                statement.setInt(2, canViewRamps);
                statement.setInt(3, canViewDashboard);
                statement.setInt(4, canViewRamps);
                statement.setInt(5, canViewDashboard);
            });
            return rows > 0;
        } catch (CannotGetJdbcConnectionException j) {
            throw new DatabaseException(Labels.getLabel(SAVE_PERSPECTIVE_PERMISSION_FAILED) + " : "
                    + Labels.getLabel(DATABASE_CONNECTION_FAIL), j);
        } catch (DuplicateKeyException e) {
            LOGGER.error(Constants.HANDLED_EXCEPTION, e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(SAVE_PERSPECTIVE_PERMISSION_FAILED), f);
        }
        return false;
    }

    @Override
    public Permission getPersPermissions(String groupName) throws DatabaseException {
        
        ResultSetExtractor<Permission> resultSetExr = resultSet -> {
            Permission permission = new Permission();
            while (resultSet.next()) {
                permission.setViewRamps(resultSet.getBoolean("ramps"));
                permission.setViewDashboard(resultSet.getBoolean("dashboard"));
                return permission;
            }
            return permission;
        };
        
        try {
            return getJdbcTemplate().query(SELECT_PERS_PERMISSION, new Object[] { groupName }, resultSetExr);
            
        } catch (EmptyResultDataAccessException f) {
            LOGGER.error(Constants.HANDLED_EXCEPTION, f);
            return new Permission();
        } catch (Exception e) {
            LOGGER.error("Error getting permission from DB. Returning empty permission object.", e);
            return new Permission();
        }
    }

    @Override
    public boolean isPersPermissionsAvailable() {
        try {
            return !getJdbcTemplate().queryForList("select ramps,dashboard from group_permission").isEmpty();
        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
        return false;
    }

    @Override
    public List<UserLog> getUserLogByDate(Date startDate, Date endDate) throws DatabaseException {
        try {
            long sDate = startDate.getTime();
            long eDate = endDate.getTime() + (1000 * 60 * 60 * 24);
            String condition = "start_time > ?  and end_time < ? ";
            return jdbcTemplate.query(SELECT_USER_LOG.concat(condition), new UserLogRowMapper(),
                    new Object[] { sDate, eDate });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(
                    Labels.getLabel(USER_LOG_GET_TABLE_FAILED) + " : " + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(USER_LOG_GET_TABLE_FAILED), f);
        }
    }

    @Override
    public String getLastViewPerspective(String userId) {
        String result = null;

        try {

            ResultSetExtractor<String> perspectiveExtractor = rs -> {
                if (rs.first()) {
                    return rs.getString("perspective");
                }
                return null;
            };

            result = jdbcTemplate.query(SELECT_PERSPECTIVE, new Object[] { userId }, perspectiveExtractor);

        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
        return result;
    }

    @Override
    public void setLastViewPerspective(String userId, String perspective) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO dsp_users(user_id,perspective) VALUES(?,?) ON DUPLICATE KEY UPDATE perspective=?",
                    new Object[] { userId, perspective, perspective });
        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
    }

    @Override
    public Map<String, Permission> getGroupPermissions(List<String> groupCodes) throws DatabaseException {
        try {

            StringBuilder prepareString = new StringBuilder();
            prepareString.append("(");
            for (String groupName : groupCodes) {
                prepareString.append("'").append(groupName).append("'");
                if (groupCodes.indexOf(groupName) != groupCodes.size() - 1) {
                    prepareString.append(",");
                }
            }
            prepareString.append(")");
            LOGGER.debug("Select group level permission quey string {}",
                    "select * from group_permission where group_code in " + prepareString.toString());
            return jdbcTemplate.query("SELECT * FROM group_permission WHERE group_code IN " + prepareString.toString(),
                    new GroupPermissionExtractor());
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(GROUP_PERMISSION_GET_TABLE_FAILED) + " : "
                    + Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(GROUP_PERMISSION_GET_TABLE_FAILED), f);
        }
    }

    @Override
    public List<Integer> getLayoutGCIDS(String compUuid, String userId, String ddl) {

        ResultSetExtractor<List<Integer>> gcidExtractor = rs -> {
            List<Integer> gcids = new ArrayList<Integer>();
            while (rs.next()) {
                gcids.add(rs.getInt("GCID"));
            }
            LOGGER.debug("gcids --->{}", gcids);
            return gcids;
        };

        return jdbcTemplate.query(
                "SELECT DISTINCT gcid FROM dermatology WHERE composition_id = ? AND user_id = ? AND ddl = ? ORDER BY gcid",
                new Object[] { compUuid, userId, ddl }, gcidExtractor);
    }

    @Override
    public boolean isPublicCluster(String cluster) {

        boolean result = false;
        try {
            ResultSetExtractor<Boolean> clusterTypeExtractor = rs -> {
                if (rs.first()) {
                    return rs.getBoolean("cluster_type");
                }
                return false;
            };
            result = jdbcTemplate.query(SELECT_CLUSTER_TYPE, new Object[] { cluster }, clusterTypeExtractor);
            LOGGER.debug("is {} public cluster ? {}", cluster, result);
        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
        return result;
    }

    @Override
    public void updatePublicCluster(String cluster, boolean isPublicCluster) throws DatabaseException {
        try {
            getJdbcTemplate().update(INSERT_OR_UPDATE_PUBLIC_CLUSTER,
                    new Object[] { cluster, isPublicCluster, isPublicCluster });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException("Error in accessing table", f);
        }
    }

    @Override
    public List<String> getPublicClusters() throws DatabaseException {

        ResultSetExtractor<List<String>> connectionIdExtractor = rs -> {
            List<String> clusters = new ArrayList<String>();
            while (rs.next()) {
                clusters.add(rs.getString(HPCC_CON_ID));
            }
            LOGGER.debug("clusters -->{}", clusters);
            return clusters;
        };

        try {
            return jdbcTemplate.query(SELECT_PUBLIC_CLUSTERS, connectionIdExtractor);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }
    }

    @Override
    public List<String> getPublicRepositories() throws DatabaseException {

        ResultSetExtractor<List<String>> rsExtractor = rs -> {
            List<String> repos = new ArrayList<String>();
            while (rs.next()) {
                repos.add(rs.getString(HPCC_CON_ID));
            }
            LOGGER.debug("clusters --->{}", repos);
            return repos;
        };
        try {
            return jdbcTemplate.query(SELECT_PUBLIC_REPOSITORIES, rsExtractor);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }
    }

    @Override
    public void updatePublicRepository(String repo, boolean isPublicRepo) throws DatabaseException {
        try {
            getJdbcTemplate().update(INSERT_OR_UPDATE_PUBLIC_REPOSITORY,
                    new Object[] { repo, isPublicRepo, isPublicRepo });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }

    }

    @Override
    public boolean isPublicRepo(String repo) {
        boolean result = false;
        try {
            ResultSetExtractor<Boolean> rsExtractor = rs -> {
                if (rs.first()) {
                    return rs.getBoolean("repo_type");
                }
                return false;
            };
            result = jdbcTemplate.query(SELECT_REPO_TYPE, new Object[] { repo }, rsExtractor);

        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
        }
        return result;
    }

    @Override
    public List<String> getCustomClusters(List<String> roles) throws DatabaseException {

        ResultSetExtractor<List<String>> customClustersExtractor = rs -> {
            List<String> clusters = new ArrayList<String>();
            while (rs.next()) {
                clusters.add(rs.getString(HPCC_CON_ID));
            }
            LOGGER.debug("custom clusters --->{}", clusters);
            return clusters;
        };

        try {
            StringBuilder prepareString = new StringBuilder();
            prepareString.append("(");
            for (String role : roles) {
                prepareString.append("'").append(role).append("'");
                if (roles.indexOf(role) != roles.size() - 1) {
                    prepareString.append(",");
                }
            }
            prepareString.append(")");

            return jdbcTemplate.query(SELECT_CUSTOM_CLUSTERS + prepareString.toString(), customClustersExtractor);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }

    }

    @Override
    public List<String> getCustomGroups(String cluster) throws DatabaseException {
        ResultSetExtractor<List<String>> customGroupsExtractor = rs -> {
            List<String> groups = new ArrayList<String>();
            while (rs.next()) {
                groups.add(rs.getString(ROLE));
            }
            LOGGER.debug("custom groups --->{}", groups);
            return groups;
        };
        try {
            return jdbcTemplate.query(SELECT_CUSTOM_GROUPS, new Object[] { cluster }, customGroupsExtractor);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }

    }

    @Override
    public void updateCustomClusterPermissions(List<String> groups, String cluster) throws DatabaseException {
        try {
            getJdbcTemplate().batchUpdate(BATCH_UPDATE_INTO_CUSTOM_PERMISSION_SETTINGS,
                    new BatchPreparedStatementSetter() {

                        @Override
                        public void setValues(java.sql.PreparedStatement ps, int i) throws SQLException {
                            String grp = groups.get(i);
                            ps.setString(1, cluster);
                            ps.setString(2, grp);
                        }

                        @Override
                        public int getBatchSize() {
                            return groups.size();
                        }
                    });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (DuplicateKeyException f) {
            LOGGER.error(Constants.EXCEPTION, f);
        } catch (Exception t) {
            throw new DatabaseException(Labels.getLabel(UPADING_PERMISSION_FAILED), t);
        }

    }

    @Override
    public void removeOlderCustomPermissions(String cluster) throws DatabaseException {
        try {
            getJdbcTemplate().update(DELETE_CLUSTER_CUSTOM_PERMISSIONS, new Object[] { cluster });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception f) {
            throw new DatabaseException(Labels.getLabel(REMOVING_PERMISSION_FAILED), f);
        }

    }

    @Override
    public void addUpdateStaticData(StaticData data) throws DatabaseException {
        try {
            jdbcTemplate.update(INSERT_OR_UPDATE_STATIC_DATA,
                    new Object[] { data.getUser(), data.getFileName(), data.getFileContent(), data.getFileContent() });
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            throw new DatabaseException(Labels.getLabel(UNABLE_TO_UPLOAD_FILE), e);
        }

    }

    @Override
    public List<StaticData> retrieveStaticData(String userId) throws DatabaseException {

        ResultSetExtractor<List<StaticData>> staticFileExtractor = rs -> {
            List<StaticData> files = new ArrayList<>();
            StaticData file = null;
            while (rs.next()) {
                file = new StaticData(rs.getString(USER_ID), rs.getString(FILE_NAME), rs.getString(FILE_CONTENET));
                files.add(file);
            }
            LOGGER.debug("static files --->{}", files);
            return files;
        };
        try {
            return jdbcTemplate.query(RETRIEVE_STATIC_FILES, new Object[] { userId }, staticFileExtractor);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            throw new DatabaseException(Labels.getLabel(RETRIEVE_FAILED), e);
        }
    }

    @Override
    public StaticData getStaticData(String userId, String fileName) throws DatabaseException {

        try{
            return getJdbcTemplate().query(GET_STATIC_FILE, new Object[] { userId, fileName }, resultSet -> {
                StaticData staticData = null;
                while (resultSet.next()) {
                    staticData = new StaticData(resultSet.getString(USER_ID), resultSet.getString(FILE_NAME), resultSet.getString(FILE_CONTENET));
                }
                return staticData;
            });
        
        }catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            throw new DatabaseException(Labels.getLabel(RETRIEVE_FAILED), e);
        }
    }

    @Override
    public String getAppSetting(String name) {
        return getJdbcTemplate().query("SELECT value FROM application_settings WHERE name = ?", new Object[] { name },
                resultSet -> {
                    String appSetting = null;
                    while (resultSet.next()) {
                        appSetting = resultSet.getString(VALUE);
                    }
                    return appSetting;
                });
    }

    @Override
    public void saveAppSetting(String name, String value) {
        getJdbcTemplate().update(
                "INSERT INTO application_settings (name,value) VALUES (?,?) ON DUPLICATE KEY UPDATE value = ?",
                new Object[] { name, value, value });
    }

    @Override
    public String migrateDermatology() {

        StringBuilder failureMsg = new StringBuilder();
        StringBuilder migrationStatus = new StringBuilder();
        int cnt = 0, noCmpCnt = 0, cmpErrorCnt = 0;

        List<Map<String, Object>> rows = getJdbcTemplate().queryForList(SELECT_USER_MIGRATION_STATUS);

        ResultSetExtractor<String> layoutExtractor = rs -> {
            String layout = null;
            while (rs.next()) {
                layout = rs.getString(LAYOUT);
            }
            return layout;
        };

        for (Map<String, Object> row : rows) {
            String ddl = String.valueOf(row.get("ddl"));
            Integer gcid = Integer.valueOf(row.get("gcid").toString());
            String wuid = String.valueOf(row.get("wuid"));
            String usrLayoutTable = String.valueOf(row.get(USER_ID));

            // Getting user id from DDL
            // validate DDL format whether it contains the user_id
            String userId = StringUtils.substringBefore(ddl, "_");

            LOGGER.info("userId : {} for ddl : {}", userId, ddl);
            String layout = getJdbcTemplate().query(
                    "SELECT layout FROM dashboard_layout WHERE user_id = ? AND ddl = ? AND gcid = ? AND workunit_id = ?",
                    new Object[] { userId, ddl, gcid, wuid }, layoutExtractor);

            try {

                LOGGER.info("layout :{} for userId : {} , ddl : {}, gcid : {},wuid : {} ",
                        layout != null ? layout.substring(0, 50) : layout, userId, ddl, gcid, wuid);

                String compName = StringUtils.substringBefore(StringUtils.substringAfter(ddl, "_"), "_");
                LOGGER.info("compName : {} ", compName);

                Composition composition = HipieSingleton.getHipie().getComposition(userId, compName);
                LOGGER.info("composition : {} ", composition);

                if (composition != null) {
                    saveLayout(Constants.GENERIC_USER, composition.getId(), composition.getVersion(), ddl, gcid,
                            layout);
                    getJdbcTemplate().update(
                            "UPDATE dashboard_layout SET migration_status = 'Complete' WHERE user_id = ? AND ddl = ? AND gcid = ?",
                            new Object[] { usrLayoutTable, ddl, gcid });
                    cnt++;
                    LOGGER.info("layout for the group: userId : {} , ddl : {}, gcid : {}  migrated successfully",
                            userId, ddl, gcid);
                } else {
                    noCmpCnt++;
                    LOGGER.info(
                            "layout for the group: userId : {} , ddl : {}, gcid : {}  failed to migrate. composition not found",
                            userId, ddl, gcid);
                }

            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                getJdbcTemplate().update(
                        "UPDATE dashboard_layout SET migration_status = 'Failed' WHERE user_id = ? AND ddl = ? AND gcid = ?",
                        new Object[] { usrLayoutTable, ddl, gcid });
                cmpErrorCnt++;
                LOGGER.info(
                        "layout for the group: userId : {} , ddl : {}, gcid : {}  failed to migrate. Error fetching composition",
                        userId, ddl, gcid);
                if (StringUtils.isNotEmpty(e.getMessage())
                        && !(StringUtils.contains(failureMsg.toString(), e.getMessage()))) {
                    failureMsg.append(e.getMessage());
                    failureMsg.append(" and ");
                }

            }
        }

        LOGGER.info("No.of layout records to be migrated --->{} ", rows.size());
        LOGGER.info("No.of layout records have been migrated --->{} ", cnt);
        LOGGER.info("No.of layout records failed to migrate - composition not found --->{} ", noCmpCnt);
        LOGGER.info("No.of layout records failed to migrate  - Error fetching composition  --->{} ", cmpErrorCnt);

        migrationStatus.append("No.of layouts found to be migrated :: ");
        migrationStatus.append(rows.size());
        migrationStatus.append("\n");
        migrationStatus.append("No.of layouts has been migrated :: ");
        migrationStatus.append(cnt);
        migrationStatus.append("\n");
        migrationStatus.append("No.of layouts failed to migrate - composition not found :: ");
        migrationStatus.append(noCmpCnt);
        migrationStatus.append("\n");
        migrationStatus.append("No.of layouts failed to migrate  - Error fetching composition :: ");
        migrationStatus.append(cmpErrorCnt);
        migrationStatus.append("\n");
        migrationStatus.append("Check log for more details");

        LOGGER.info("failureMsg String: {} ", failureMsg);
        return migrationStatus.toString();
    }

    @Override
    public boolean isMigrationPending() {
        try {
            List<Map<String, Object>> rows = getJdbcTemplate().queryForList(
                    "select user_id,ddl,gcid, max(workunit_id) wuid from dashboard_layout WHERE migration_status = 'Failed' or migration_status is null group by user_id,ddl,gcid");

            if (CollectionUtils.isEmpty(rows)) {
                return false;
            } 
            return true;
            
        } catch (BadSqlGrammarException e) {
            LOGGER.error("Migration not applicable. dashboard_layout table may be missing.", e);
            return false;
        }
    }
    
    @Override
    public List<Dermatology> getDermatology(String compId,String compositonVersion) {
        return getJdbcTemplate().query("SELECT * from dermatology WHERE composition_id = ?  AND composition_version = ?", 
                new Object[] {compId,compositonVersion}, dermatologyExtractor);
    }

    @Override
    public void copyDermatology(String id, String oldCompversion, String newCompversion) {
        StringBuilder query = new StringBuilder("INSERT INTO dermatology (composition_id, composition_version, user_id, gcid, ddl, layout, modified_date) ")
            .append("SELECT composition_id, ?, user_id, gcid, ddl, layout, modified_date ")
            .append("FROM dermatology AS d ")
            .append("WHERE d.composition_id = ? AND d.composition_version = ?");
        
        getJdbcTemplate().update(query.toString(), new Object[] { newCompversion, id, oldCompversion });
    }
    
    @Override
    public List<String> getApplicationValueList(String category) throws DatabaseException {

        return jdbcTemplate.query(GET_VALUE_LIST, new Object[] { category }, new ResultSetExtractor<List<String>>() {

            @Override
            public List<String> extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<String> values = new ArrayList<>();
                while (rs.next()) {
                    values.add(rs.getString(VALUE));
                }
                return values;
            }

        });
    }

    
    @Override
    public void deleteStaticDataFile(StaticData deleteFile) throws DatabaseException {
        try {
            jdbcTemplate.update(DELETE_STATIC_DATA_FILE,
                    new Object[] { deleteFile.getUser(), deleteFile.getFileName()});
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            throw new DatabaseException(Labels.getLabel(UNABLE_TO_DELETE_FILE), e);
        }
    }
    /**
     * Performs any necessary cleanups related to this class on shutdown.
     */
    @PreDestroy
    public void cleanup() throws SQLException {
        DataSources.destroy(jdbcTemplate.getDataSource());
    }
    
    @Override
	public boolean logCompositionAccess(String compId, String userId) throws DatabaseException {
		boolean actionComplete = false;
		try {

			getJdbcTemplate().update(
					"insert into comp_access_log (comp_id,user_id,access_count) values (?,?,1) on DUPLICATE KEY update access_count =(select (access_count+1) from comp_access_log c where c.comp_id=? and c.user_id=?)",
					new Object[] { compId, userId, compId, userId });
			actionComplete = true;
			LOGGER.info("INSERTED log for Composition Access {}", compId, userId);

		} catch (CannotGetJdbcConnectionException e) {
			LOGGER.error(Constants.EXCEPTION, e);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL));
		} catch (Exception f) {
			LOGGER.error(Constants.EXCEPTION, f);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
		}
		return actionComplete;
	}

	@Override
	public boolean markAsFavoriteComposition(String compId, String userId, int isFavorite) throws DatabaseException {
		boolean actionComplete = false;
		try {
		    
			getJdbcTemplate().update(
					"insert into comp_access_log (comp_id,user_id,is_favorite) values (?,?,1) on DUPLICATE KEY UPDATE is_favorite=?",
					new Object[] {compId, userId, isFavorite });
			LOGGER.info("updated favourite flag for Composition with {}",isFavorite, compId, userId);
			actionComplete = true;
		} catch (CannotGetJdbcConnectionException e) {
			LOGGER.error(Constants.EXCEPTION, e);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL));
		} catch (Exception f) {
			LOGGER.error(Constants.EXCEPTION, f);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
		}
		return actionComplete;
	}

	@Override
	public int deleteCompositionAccessLog(String compId, String userId, boolean checkCount) throws DatabaseException {
		int val = 0;
		try {
			if (checkCount) {
				val = getJdbcTemplate().update(
						"DELETE from comp_access_log where comp_id=? and user_id=? and access_count = 0",
						new Object[] { compId, userId });
			} else {
				val = getJdbcTemplate().update("DELETE from comp_access_log where comp_id=? and user_id=?",
						new Object[] { compId, userId });
				LOGGER.info("DELETED log for Composition Access log {}", compId, userId);
			}
			
		} catch (CannotGetJdbcConnectionException e) {
			LOGGER.error(Constants.EXCEPTION, e);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL));
		} catch (Exception f) {
			LOGGER.error(Constants.EXCEPTION, f);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
		}
		return val;
	}

	@Override
	public List<String> getCompositionsByAccess(String userId) throws DatabaseException {
		ResultSetExtractor<List<String>> customGroupsExtractor = rs -> {
			List<String> compositionId = new ArrayList<String>();
			while (rs.next()) {
				compositionId.add(rs.getString("comp_id"));
			}
			return compositionId;
		};
		try {
			LOGGER.info("Calling getCompositionsByAccess for User {}", userId);
			return jdbcTemplate.query("SELECT comp_id from comp_access_log where user_id=? and access_count >0 order by access_count DESC",
					new Object[] { userId }, customGroupsExtractor);

		} catch (CannotGetJdbcConnectionException e) {
			LOGGER.error(Constants.EXCEPTION, e);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL));
		} catch (Exception f) {
			LOGGER.error(Constants.EXCEPTION, f);
			throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
		}

	}
	
	@Override
	public List<String> getFavoriteCompositions(String userId) throws DatabaseException {
        ResultSetExtractor<List<String>> customGroupsExtractor = rs -> {
            List<String> compositionId = new ArrayList<String>();
            while (rs.next()) {
                compositionId.add(rs.getString("comp_id"));
            }
            return compositionId;
        };
        try {
            LOGGER.info("Calling getFavoriteCompositions for User {}", userId);
            return jdbcTemplate.query("SELECT comp_id from comp_access_log where user_id=? and is_favorite=1",
                    new Object[] { userId }, customGroupsExtractor);

        } catch (CannotGetJdbcConnectionException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL));
        } catch (Exception f) {
            LOGGER.error(Constants.EXCEPTION, f);
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), f);
        }

    }

	@Override
	public List<Builder> getECLBuilders(String author) throws DatabaseException {
		return getECLBuildersByName(author, false, null);
	}

	@Override
	public List<Builder> getECLBuildersByName(String author, boolean byName, String name) throws DatabaseException {
        try {
            LOGGER.debug("Getting layouts for Builders");
            return byName ?  getJdbcTemplate().query(GET_ECL_BUILDERS_By_Name,new Object[] {author, name}, eclBuilderExtractor) :
            	getJdbcTemplate().query(GET_ECL_BUILDERS,new Object[] {author}, eclBuilderExtractor) ;
        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error -  {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
		return null;
    }

	@Override
	public List<Builder> getECLBuilder(String author, String name, String hpccId) throws DatabaseException {
        try {
            LOGGER.debug("Getting layouts for Builders");
            return getJdbcTemplate().query(GET_ECL_BUILDER,new Object[] {author, name, hpccId, hpccId}, eclBuilderExtractor);
        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error -  {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
		return null;
    }

	@Override
	public int addOrUpdateECLBuilders(ECLBuilder eclBuilderDetails, boolean addOrUpdate) throws DatabaseException {
        try {
            LOGGER.debug("Getting layouts for Builders");
            if(addOrUpdate){
            	return getJdbcTemplate().update(ADD_ECL_BUILDERS,new Object[] {eclBuilderDetails.getUser_id(),
            				eclBuilderDetails.getName(), eclBuilderDetails.getLogicalFiles(), eclBuilderDetails.getModified_date(),
            					eclBuilderDetails.getEclbuildercode(), eclBuilderDetails.getHpccConnId(), eclBuilderDetails.getWuID()});
            }else{
            	return getJdbcTemplate().update(UPDATE_ECL_BUILDERS,new Object[] {eclBuilderDetails.getModified_date(),
        					eclBuilderDetails.getEclbuildercode(), eclBuilderDetails.getLogicalFiles(),
            				eclBuilderDetails.getName()});

            }
        } catch (EmptyResultDataAccessException e) {
            LOGGER.error("Layout not found. Error -  {}", e);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), e);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
		return 0;
    }

	@Override
	public int deleteECLBuilder(String author, String name) throws DatabaseException {
		// TODO Auto-generated method stub
		return getJdbcTemplate().update(DELETE_ECL_BUILDERS, new Object[] {author, name});
	}

	@Override
	public String getECLQueryByWUID(String wuid) throws DatabaseException {
		// TODO Auto-generated method stub
		return (getJdbcTemplate().query(GET_QUERY_BY_WUID, new Object[] {wuid }, eclBuilderExtractor)).get(0).getEclbuildercode();
	}
}
