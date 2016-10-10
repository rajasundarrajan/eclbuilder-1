package org.hpccsystems.dsp.dashboard.entity;

import org.hpccsystems.dsp.entity.UIPermission;

public class DashboardPermission implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private UIPermission uiPermission;

    public DashboardPermission(UIPermission uipermisssion) {
        this.setUiPermission(uipermisssion);
    }

    public DashboardPermission() {
    }

    public DashboardPermission(DashboardPermission dashboardPermission) {
        this.setUiPermission(new UIPermission(dashboardPermission.getUiPermission()));
    }

    public void updateDashboardPermission(DashboardPermission groupDashboardPermission) {
        this.uiPermission.updateUIPermission(groupDashboardPermission.getUiPermission());
    }

    public UIPermission getUiPermission() {
        return uiPermission;
    }

    public void setUiPermission(UIPermission uiPermission) {
        this.uiPermission = uiPermission;
    }

    @Override
    public String toString() {
        return "[UIpermission = " + uiPermission + "]";
    }
}
