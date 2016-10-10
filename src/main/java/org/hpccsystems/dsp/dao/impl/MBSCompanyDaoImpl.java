package org.hpccsystems.dsp.dao.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.CompanyDao;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;

@Service("mbsCompanyDao")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MBSCompanyDaoImpl implements CompanyDao {

    private static final String UNABLE_TO_GET_GCID_TAGS = "unableToGetGCIDTags";

    private static final String DATABASE_CONNECTION_FAIL = "databaseConnectionFail";

    private static final Logger LOGGER = LoggerFactory.getLogger(MBSCompanyDaoImpl.class);

    private static final String SELECT_COMPANY = "SELECT DISTINCT g.company_name,g.gc_id FROM gbl_company g JOIN company_status_type as c ";
    private static final String FILTER_COMPANY_NAME = "ON g.company_status = c.company_status WHERE (company_name LIKE ?) AND c.active_status = 1 ";
    private static final String LIMIT_RECORD = "LIMIT 150";

    private static final String SELECT_COMPANY_FROM_GCID = SELECT_COMPANY
            + "ON g.company_status = c.company_status WHERE gc_id = ? AND c.active_status = 1 " + LIMIT_RECORD;

    private static final String SELECT_COMPANY_FROM_NAME = SELECT_COMPANY + FILTER_COMPANY_NAME + LIMIT_RECORD;

    private static final String SELECT_COMPANY_FROM_NAME_AND_GCID = SELECT_COMPANY + FILTER_COMPANY_NAME + " AND gc_id =?" + LIMIT_RECORD;

    private static final String SELECT_FCRA = "SELECT is_fcra FROM gbl_company WHERE gc_id =?";

    private static final String SELECT_GCID_TAG_DATA = "SELECT ec.cat_name, ce.detail1 , ce.detail2 "
            + "FROM gbl_company_ext ce JOIN gbl_ext_cat ec ON ec.ext_cat_id = ce.ext_cat_id " + "WHERE gc_id = ? ";

    protected static final String COMAPNY_NAME = "company_name";
    protected static final String GC_ID = "gc_id";
    private static final String CAT_NAME = "cat_name";
    private static final String DETAIL_1 = "detail1";
    private static final String DETAIL_2 = "detail2";
    private static final String FCRA = "is_fcra";
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("mbsDataSource")
    public void setDataSourceToJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @PostConstruct
    public void initialize() {
        LOGGER.info("Testing MBS Database connection.");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            LOGGER.info("Test connection success.");
        } catch (Exception e) {
            LOGGER.info("Test connection failed.");
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    @Override
    public List<Company> getMatchingCompany(Company company) throws DatabaseException {
        try {
            List<Company> companies = null;

            ResultSetExtractor<List<Company>> rsExtractor = rs -> {
                List<Company> companyList = new ArrayList<Company>();
                while (rs.next()) {
                    companyList.add(new Company(rs.getString(COMAPNY_NAME), rs.getInt(GC_ID)));
                }
                return companyList;
            };
            companies = getCompanies(company, rsExtractor);

            if (companies.isEmpty()) {
                throw new DatabaseException(Labels.getLabel("companyIdsNotAvailable"));
            }
            LOGGER.debug("companies size --->{}", companies.size());
            return companies;
        } catch (CannotGetJdbcConnectionException cex) {
            LOGGER.error(Constants.EXCEPTION, cex);
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), cex);
        } catch (Exception pex) {
            LOGGER.error(Constants.EXCEPTION, pex);
            throw new DatabaseException(Labels.getLabel("matchingCompanyIdFailed"), pex);
        }
    }

    private List<Company> getCompanies(Company company, ResultSetExtractor<List<Company>> rsExtractor) {
        List<Company> companies;
        if (!StringUtils.isEmpty(company.getName()) && company.getGcId() != null) {
            companies = jdbcTemplate.query(SELECT_COMPANY_FROM_NAME_AND_GCID, new Object[] { "%" + company.getName() + "%", company.getGcId() },
                    rsExtractor);
        } else if (!StringUtils.isEmpty(company.getName())) {
            companies = jdbcTemplate.query(SELECT_COMPANY_FROM_NAME, new Object[] { "%" + company.getName() + "%" }, rsExtractor);
        } else {

            companies = jdbcTemplate.query(SELECT_COMPANY_FROM_GCID, new Object[] { company.getGcId() }, rsExtractor);
        }
        return companies;
    }

    @Override
    public Map<String, String> getGCIDComplianceValues(String gcid, Collection<String> complianceTags) throws DatabaseException {
        boolean hasFrca = complianceTags.contains("FCRA");
        if (hasFrca) {
            complianceTags.remove("FRCA");
        }
        ResultSetExtractor<Map<String, String>> gcidTagExtractor = rs -> getGcidTag(rs);
        

        try {
            String query = SELECT_GCID_TAG_DATA;
            if (CollectionUtils.isNotEmpty(complianceTags)) {
                query = query + constructInClause(complianceTags);
            } else {
                return null;
            }
            Map<String, String> complianceValues = jdbcTemplate.query(query, new Object[] { gcid }, gcidTagExtractor);
            if (hasFrca) {
                complianceValues.put("FCRA", getFCRAComplianceValue(gcid));
            }
            // TODO:Remove adding INDUSTRYCLASS tag with value 'OTHER', once
            // BatchManager handles INDUSTRYCLASS
            complianceValues.put("INDUSTRYCLASS", "OTHER");

            return complianceValues;
        } catch (CannotGetJdbcConnectionException cex) {
            LOGGER.error(Constants.EXCEPTION, cex);
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), cex);
        } catch (Exception pex) {
            LOGGER.error(Constants.EXCEPTION, pex);
            throw new DatabaseException(Labels.getLabel(UNABLE_TO_GET_GCID_TAGS), pex);
        }
    }

    private Map<String, String> getGcidTag(ResultSet rs) throws SQLException {
        Map<String, String> complianceValues = new HashMap<String, String>();
        while (rs.next()) {
            String catName = rs.getString(CAT_NAME);
            String detail1 = rs.getString(DETAIL_1);
            String detail2 = rs.getString(DETAIL_2);

            if (catName.endsWith("permission mask")) {
                complianceValues.put("DATAPERMISSIONMASK", detail1);
            } else if (catName.endsWith("restriction mask")) {
                complianceValues.put("DATARESTRICTIONMASK", detail1);
            } else if (catName.endsWith("ssn masking")) {
                complianceValues.put("SSNMASK", detail1);
            } else if (catName.endsWith("license masking")) {
                complianceValues.put("DLMASK", detail1);
            } else if (catName.endsWith("dob masking")) {
                complianceValues.put("DOBMASK", detail1);
            } else if (catName.endsWith("usage")) {
                complianceValues.put("DPPAPURPOSE", detail2);
                complianceValues.put("GLBPURPOSE", detail1);
            }
        }
        LOGGER.debug("Gcid complianceValues --->{}", complianceValues);
        return complianceValues;
    }

    private String constructInClause(Collection<String> complianceTags) {
        final StringBuilder builder = new StringBuilder();
        builder.append("AND ec.cat_name IN (");
        complianceTags.forEach(tag -> builder.append("'").append(tag).append("',"));
        String inclause = StringUtils.removeEnd(builder.toString(), ",");
        inclause = inclause + ")";
        LOGGER.info("GCID Compliance in clause {}", inclause);
        return inclause;
    }

    private String getFCRAComplianceValue(String gcid) throws DatabaseException {
        ResultSetExtractor<String> fcraExtractor = rs -> {
            String fcra = null;
            while (rs.next()) {
                fcra = rs.getString(FCRA);
            }
            LOGGER.debug("Gcid compliance FCRA --->{}", fcra);
            return fcra;
        };

        try {
            return jdbcTemplate.query(SELECT_FCRA, new Object[] { gcid }, fcraExtractor);
        } catch (CannotGetJdbcConnectionException cex) {
            LOGGER.error(Constants.EXCEPTION, cex);
            throw new DatabaseException(Labels.getLabel(DATABASE_CONNECTION_FAIL), cex);
        } catch (Exception pex) {
            LOGGER.error(Constants.EXCEPTION, pex);
            throw new DatabaseException(Labels.getLabel(UNABLE_TO_GET_GCID_TAGS), pex);
        }
    }
}
