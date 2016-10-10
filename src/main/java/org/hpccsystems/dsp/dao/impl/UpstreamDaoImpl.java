package org.hpccsystems.dsp.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.UpstreamDao;
import org.hpccsystems.dermatology.domain.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("upstreamDao")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UpstreamDaoImpl implements UpstreamDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpstreamDaoImpl.class);
    
    private JdbcTemplate jdbcTemplate;

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Autowired
    @Qualifier("upstreamDataSource")
    public void setDataSourceToJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void initialize() {
        LOGGER.info("Testing Upstream Database connection.");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            HipieSingleton.setUpstreamConfigured(true);
            LOGGER.info("Test connection success.");
        } catch (Exception e) {
            LOGGER.info("Test connection failed.");
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    @Override
    public void insertDermatology(List<Dermatology> dermatology) {
        String sql ="INSERT INTO dermatology (composition_id, composition_version, user_id, gcid, ddl, layout, modified_date) "
                + "VALUES(?, ?, ?, ?, ?, ?, ?) ";
        
        getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Dermatology dermatologyList = dermatology.get(i);
                ps.setString(1, dermatologyList.getCompositionId());
                ps.setString(2, dermatologyList.getCompositionVersion());
                ps.setString(3, dermatologyList.getUserId());
                ps.setInt(4, dermatologyList.getGcid());
                ps.setString(5, dermatologyList.getDdl());
                ps.setString(6, dermatologyList.getLayout());
                ps.setDate(7, dermatologyList.getModifiedDate());
            }
            @Override
            public int getBatchSize() {
                return dermatology.size();
            }
        });
    }

    @Override
    public void insertStaticDataTable(List<StaticData> staticData) {
        String sql ="INSERT INTO static_data (user_id, file_name, file_content) "
                + "VALUES(?, ?, ?) ";
        
        getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StaticData staticDataList = staticData.get(i);
                ps.setString(1, staticDataList.getUser());
                ps.setString(2, staticDataList.getFileName());
                ps.setString(3, staticDataList.getFileContent());
               
            }
            @Override
            public int getBatchSize() {
                return staticData.size();
            }
        });
        
    }
}
