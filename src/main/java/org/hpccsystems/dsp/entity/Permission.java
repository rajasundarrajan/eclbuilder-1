package org.hpccsystems.dsp.entity;

import org.hpccsystems.dsp.dashboard.entity.DashboardPermission;
import org.hpccsystems.dsp.ramps.entity.RAMPSPermission;

public class Permission implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private boolean viewRamps;
    private boolean viewDashboard;

    private RAMPSPermission rampsPermission;
    private DashboardPermission dashboardPermission;

    public Permission(boolean ramps, boolean dashboard, RAMPSPermission rampsPermission, DashboardPermission dashboardPermission) {
        this.setViewRamps(ramps);
        this.setViewDashboard(dashboard);
        this.setRampsPermission(rampsPermission);
        this.setDashboardPermission(dashboardPermission);
    }

    public Permission() {
    }

    public Permission(Permission permission, boolean importFile, boolean keepECL) {
        this.setViewDashboard(permission.canViewDashboard());
        this.setViewRamps(permission.canViewRamps());
        this.setRampsPermission(new RAMPSPermission(permission.getRampsPermission(), importFile, keepECL));
        this.setDashboardPermission(new DashboardPermission(permission.getDashboardPermission()));
    }

    public void updatePermission(Permission groupPermission) {
        if (groupPermission.canViewRamps()) {
            this.setViewRamps(true);
        }
        if (groupPermission.canViewDashboard()) {
            this.setViewDashboard(true);
        }
        this.rampsPermission.updateRAMPSPermission(groupPermission.getRampsPermission(), groupPermission.getRampsPermission().canImportFile(),
                groupPermission.getRampsPermission().isKeepECL());
        this.dashboardPermission.updateDashboardPermission(groupPermission.getDashboardPermission());
    }

    public boolean canViewRamps() {
        return viewRamps;
    }

    public void setViewRamps(boolean viewRamps) {
        this.viewRamps = viewRamps;
    }

    public boolean canViewDashboard() {
        return viewDashboard;
    }

    public void setViewDashboard(boolean viewDashboard) {
        this.viewDashboard = viewDashboard;
    }

    public RAMPSPermission getRampsPermission() {
        return rampsPermission;
    }

    public void setRampsPermission(RAMPSPermission rampsPermission) {
        this.rampsPermission = rampsPermission;
    }

    public DashboardPermission getDashboardPermission() {
        return dashboardPermission;
    }

    public void setDashboardPermission(DashboardPermission dashboardPermission) {
        this.dashboardPermission = dashboardPermission;
    }

    @Override
    public String toString() {
        return "Permission [viewRamps=" + viewRamps + ", viewDashboard=" + viewDashboard + "Ramps permission=" + rampsPermission
                + "Dashboard permission=" + dashboardPermission + "]";
    }

}
