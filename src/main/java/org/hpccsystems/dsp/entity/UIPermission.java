package org.hpccsystems.dsp.entity;

public class UIPermission implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private boolean viewGrid;
    private boolean viewList;
    private boolean mandateCompanyId;
    private boolean advancedMode;
    private String defaultView;
    private boolean convertToComp;
    
    public UIPermission() {
    }

    public UIPermission(UIPermission uiPermission) {
        this.setViewGrid(uiPermission.canViewGrid());
        this.setViewList(uiPermission.canViewList());
        this.setMandateCompanyId(uiPermission.isCompanyIdMandatory());
        this.setDefaultView(uiPermission.getDefaultView());
    }

    public void updateUIPermission(UIPermission uiPermission) {
        if (uiPermission.isCompanyIdMandatory()) {
            this.setMandateCompanyId(true);
        }
        if (uiPermission.canViewGrid()) {
            this.setViewGrid(true);
        }
        if (uiPermission.canViewList()) {
            this.setViewList(true);
        }
        // Setting last value in the list as default view
        if (org.apache.commons.lang.StringUtils.isNotBlank(uiPermission.getDefaultView())) {
            this.setDefaultView(uiPermission.getDefaultView());
        }
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public boolean isGridDefaultView() {
        return "Grid".equals(defaultView);
    }

    public boolean isListDefaultView() {
        return "List".equals(defaultView);
    }

    public boolean canViewGrid() {
        return viewGrid;
    }

    public void setViewGrid(boolean viewGrid) {
        this.viewGrid = viewGrid;
    }

    public boolean canViewList() {
        return viewList;
    }

    public void setViewList(boolean viewList) {
        this.viewList = viewList;
    }
    
    public boolean isCompanyIdMandatory() {
        return mandateCompanyId;
    }

    public void setMandateCompanyId(boolean mandateCompanyId) {
        this.mandateCompanyId = mandateCompanyId;
    }
    
    public boolean isAllowedAdvancedMode() {
        return advancedMode;
    }

    public void setAllowedAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
    }
    
    public boolean isAllowedConvertToComp() {
        return convertToComp;
    }
    
    public void setAllowedConvertToComp(boolean convertToComp) {
        this.convertToComp = convertToComp;
    }
    
    @Override
    public String toString() {
    return "[viewGrid=" + viewGrid + ", viewList=" + viewList + ", defaultView=" + defaultView + "]";
    }

}
