package org.hpccsystems.dsp.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hpccsystems.dsp.dashboard.entity.DashboardPermission;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.entity.UIPermission;
import org.hpccsystems.dsp.ramps.entity.RAMPSPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class GroupPermissionExtractor implements ResultSetExtractor<Map<String, Permission>> {

    private static final String GROUP_CODE = "group_code";
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupPermissionExtractor.class);
    @Override
    public Map<String, Permission> extractData(ResultSet rs) throws SQLException, DataAccessException {

        Map<String, Permission> permissions = new LinkedHashMap<String, Permission>();
        while (rs.next()) {
            Permission permission;
            RAMPSPermission rampsPermission;
            DashboardPermission dashboardPermission;

            UIPermission rampsUIPermission = new UIPermission();
            rampsUIPermission.setViewGrid(rs.getBoolean("ramps_grid"));
            rampsUIPermission.setViewList(rs.getBoolean("ramps_list"));
            rampsUIPermission.setDefaultView(rs.getString("ramps_default_view"));
            rampsUIPermission.setMandateCompanyId(rs.getBoolean("ramps_mandate_company_id"));
            rampsPermission = new RAMPSPermission(rampsUIPermission, rs.getBoolean("ramps_view_plugin"), rs.getBoolean("import_file"),
                    rs.getBoolean("keep_ecl"));

            UIPermission dashboardUIPermission = new UIPermission();
            dashboardUIPermission.setViewGrid(rs.getBoolean("dashboard_grid"));
            dashboardUIPermission.setViewList(rs.getBoolean("dashboard_list"));
            dashboardUIPermission.setDefaultView(rs.getString("dashboard_default_view"));
            dashboardUIPermission.setMandateCompanyId(rs.getBoolean("dashboard_mandate_company_id"));
            dashboardUIPermission.setAllowedAdvancedMode(rs.getBoolean("dashboard_advanced_mode"));
            dashboardUIPermission.setAllowedConvertToComp(rs.getBoolean("dashboard_convert_to_comp"));
            dashboardPermission = new DashboardPermission(dashboardUIPermission);

            permission = new Permission(rs.getBoolean("ramps"), rs.getBoolean("dashboard"), rampsPermission, dashboardPermission);
            permissions.put(rs.getString(GROUP_CODE), permission);

            LOGGER.debug("Permissions for group code {} is {}", rs.getString(GROUP_CODE), permission.toString());
        }
        return permissions;
    }
}
