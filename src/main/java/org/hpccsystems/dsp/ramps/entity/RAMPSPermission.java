package org.hpccsystems.dsp.ramps.entity;

import org.hpccsystems.dsp.entity.UIPermission;

public class RAMPSPermission implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private UIPermission uiPermission;
    private boolean viewPluginSource;
    private boolean importFile;
    private boolean keepECL;

    public RAMPSPermission(UIPermission uiPermission, boolean pluginSource, boolean importFile, boolean keepECL) {
        this.setUiPermission(uiPermission);
        this.setViewPluginSource(pluginSource);
        this.setImportFile(importFile);
        this.setKeepECL(keepECL);
    }

    public RAMPSPermission() {
    }

    public RAMPSPermission(RAMPSPermission rampsPermission, boolean importFile, boolean keepECL) {
        this.setUiPermission(new UIPermission(rampsPermission.getUiPermission()));
        this.setViewPluginSource(rampsPermission.canViewPluginSource());
        this.setImportFile(importFile);
        this.setKeepECL(keepECL);
    }

    public void updateRAMPSPermission(RAMPSPermission groupRampsPermission, boolean importFile, boolean keepECL) {
        if (groupRampsPermission.canViewPluginSource()) {
            this.setViewPluginSource(true);
        }
        if (importFile) {
            this.setImportFile(true);
        }
        if (keepECL) {
            this.setKeepECL(true);
        }
        this.uiPermission.updateUIPermission(groupRampsPermission.getUiPermission());
    }

    public UIPermission getUiPermission() {
        return uiPermission;
    }

    public void setUiPermission(UIPermission uiPermission) {
        this.uiPermission = uiPermission;
    }

    public boolean canViewPluginSource() {
        return viewPluginSource;
    }

    public void setViewPluginSource(boolean pluginSource) {
        this.viewPluginSource = pluginSource;
    }

    public boolean canImportFile() {
        return importFile;
    }

    public void setImportFile(boolean importFile) {
        this.importFile = importFile;
    }

    public boolean isKeepECL() {
        return keepECL;
    }

    public void setKeepECL(boolean keepECL) {
        this.keepECL = keepECL;
    }

    @Override
    public String toString() {
        return "RAMPSPermission [uiPermission=" + uiPermission + ", viewPluginSource=" + viewPluginSource + ", importFile=" + importFile
                + ", keepECL=" + keepECL + "]";
    }
}
