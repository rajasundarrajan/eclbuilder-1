package org.hpccsystems.dsp.dashboard.controller;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.DataAccess;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class OutputsController extends SelectorComposer<Component> {
    private static final String POINTER_EVENTS = "pointer-events:auto; border: auto;";
    private static final String PERSONALIZED_VIEW = "personalizedView";
    private static final String DEFAULT_VIEW = "defaultView";
    private static final String SAVE_DASHBOARD_LAYOUT = "saveDashboardLayout('";
    private static final String SHOW_PROPERTIES_FALSE = "showProperties(false,'";
    private static final String AUTHENTICATION_SERVICE = "authenticationService";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputsController.class);

    private static enum Action {
        SAVE_AS_PUBLIC, SAVE_AS_PRIVATE
    }

    @Wire
    private Div chartHolder;
    @Wire
    private Div formDiv;

    @Wire
    private Button layoutSave;

    @Wire
    private Combobutton viewToggleButton;

    @Wire
    private Button editLayout;

    @Wire
    private Button closeLayout;

    @Wire
    private Label lastRunLabel;

    @Wire
    private Hlayout toolset;
    @Wire
    private Hbox titleHolder;
    @Wire
    private Label title;
    @Wire
    private Textbox shareURL;

    @Wire
    private Combobox gcidCombobox;
    @Wire
    private Hlayout gcidBox;

    @Wire
    private Listcell saveAsDefaultView;

    @Wire
    private Listcell saveAsPersonalizedView;

    @Wire
    private Listcell otherViewBtn;

    private DashboardConfig dashboardConfig;
    private Tab projectTab;
    private boolean canEditLayout;
    private boolean isScoredSearchForm;

    CompositionInstance compositionInstance;

    private String userID;
    private Optional<String> ddlName;
    private boolean isSharedView;

    private int activeGCID;
    private ListModelList<Integer> gcidModel = new ListModelList<Integer>();

    private boolean canEdit;
    private Contract contract;
    private boolean isRampsConfig;
    private String errorVal;

    private boolean isDDLValid;

    private Action saveAsAction;
    private boolean isPrivateView;
    
    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        userID = ((AuthenticationService) SpringUtil.getBean(AUTHENTICATION_SERVICE)).getCurrentUser().getId();

        compositionInstance = (CompositionInstance) Executions.getCurrent().getArg().get(Dashboard.MOST_RECENT_CI);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        projectTab = (Tab) Executions.getCurrent().getArg().get(Constants.PROJECT_TAB);

        if (dashboardConfig == null) {
            // Since there is not Dashboard instance, this is Shared dashboard
            // view
            isSharedView = true;

            String compId = (String) Executions.getCurrent().getArg().get(Constants.COMPOSITION);
            dashboardConfig = createConfig(compId);
            errorVal = checkErrors(dashboardConfig);
            if (errorVal != null) {
                return super.doBeforeCompose(page, parent, compInfo);
            }
            compositionInstance = dashboardConfig.getMostRecentCI();
        }
        isRampsConfig = dashboardConfig.isRAMPSConfig();

        if (dashboardConfig.isStaticData()) {
            // Always assuming valid DDL in static dashboards
            isDDLValid = true;
        } else {
            // When CompositionInstance is not passed as Dynamic property look
            // in DashboardConfig
            if (compositionInstance == null) {
                compositionInstance = dashboardConfig.getMostRecentCI();
            }
                
            if(!DashboardUtil.isWorkUnitComplete(compositionInstance)){
               errorVal = Labels.getLabel(Constants.PROCESS_NOT_COMPLETE);
            }
            if (errorVal != null) {
                 return super.doBeforeCompose(page, parent, compInfo);
            }
            isScoredSearchForm = checkForScoredsearch();

            // Validatinfg DDL
            isDDLValid = new Process(compositionInstance).isDDLValid();
            if (!isDDLValid) {
                errorVal = Labels.getLabel("dashboardProcessInvalid");
            }
        }

        setCanEditLayout(((AuthenticationService) SpringUtil.getBean(AUTHENTICATION_SERVICE)).getCurrentUser().canEdit() && HipieSingleton.getHipie()
                .getPermissionsManager().userCanEdit(((AuthenticationService) SpringUtil.getBean(AUTHENTICATION_SERVICE)).getCurrentUser().getId(),
                        dashboardConfig.getComposition()));

        return super.doBeforeCompose(page, parent, compInfo);

    }

    private String checkErrors(DashboardConfig dashConf) {
        String errorValue = null;
        if (dashConf == null || dashConf.getComposition() == null) {
            errorValue = Labels.getLabel("dashboardNotAvailable");
        } else if (!HipieSingleton.getHipie().getPermissionsManager().userCanView(
                ((AuthenticationService) SpringUtil.getBean(AUTHENTICATION_SERVICE)).getCurrentUser().getId(), dashConf.getComposition())) {
            errorValue = Labels.getLabel("donotHavePermissionToViewDashboard");
        } else if(!dashConf.isStaticData()){
            try { 
                  CompositionInstance mostRecentInstance = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(dashConf.getComposition(), false);
                  if(mostRecentInstance == null){
                      errorValue = Labels.getLabel("processNotAvailable");
                  }else if(!DashboardUtil.isWorkUnitComplete(mostRecentInstance)){
                      errorValue = Labels.getLabel(Constants.PROCESS_NOT_COMPLETE);
                  }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                errorValue = Labels.getLabel("unableToGrabWU");
            }
        }
        return errorValue;
    }

    private DashboardConfig createConfig(String compId) {
        try {
            LOGGER.debug("Getting composition for {} using {}", userID, compId);
            Composition composition = HipieSingleton.getHipie().getComposition(userID, compId);
            CompositionInstance ci = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(composition, false);

            Dashboard dashboard = new Dashboard(composition);
            dashboard.setStaticData(composition.getName().toLowerCase().endsWith(Dashboard.DATA_BOMB_SUFFIX));

            DashboardConfig config = new DashboardConfig();
            config.setComposition(composition);
            config.setMostRecentCI(ci);
            config.setDashboard(dashboard);

            return config;
        } catch (Exception e) {
            LOGGER.error("Field to get Composition {}", e);
            return null;
        }
    }

    private boolean checkForScoredsearch() {
        return compositionInstance.getComposition().getContractInstances().values().stream()
                .filter(ci -> Dashboard.DASHBOARD_REPO.equals(ci.getContract().getRepositoryName())
                        && StringUtils.endsWith(ci.getContract().getName(), Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER))
                .findAny().isPresent();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        if (errorVal != null) {
            showError(errorVal);
            toolset.setVisible(false);
            
            if (isSharedView) {
                return;
            } else {
                // TODO: Hide buttons not needed when dashboard is not
                // displayed. Also verify the page re-loads when a valid process
                // is found.
            }
        }

        Optional<ContractInstance> dashboardCI = null;
        if (!dashboardConfig.isStaticData()) {
            dashboardCI = compositionInstance.getComposition().getContractInstances().values().stream()
                    .filter(ci -> Dashboard.DASHBOARD_REPO.equals(ci.getContract().getRepositoryName())).findFirst();
        }

        if (!dashboardConfig.isStaticData() && !dashboardCI.isPresent()) {
            showError(Labels.getLabel("dashboardConfigDeleted"));
            if(isRampsConfig) {
                toolset.setVisible(false);
            }
            return;
        }

        long startTime = Instant.now().toEpochMilli();

        if (isSharedView) {
            toolset.setVisible(false);
            titleHolder.setVisible(true);
            title.setValue(dashboardConfig.getComposition().getLabel());
        } else {
            StringBuilder url = shareURL();
            shareURL.setText(url.toString());
        }

        ddlName = null;

        if (dashboardConfig.isStaticData()) {
            // For static data, Composition name is used in-place of DDL name
            ddlName = Optional.of(dashboardConfig.getDashboard().getCanonicalName());
            canEdit = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canEdit();
        } else {
            String dashboardInstanceId = dashboardCI.get().getInstanceID();

            ddlName = new Process(compositionInstance).getDDLs(false).stream().filter(ddl -> StringUtils.containsIgnoreCase(ddl, dashboardInstanceId))
                    .findFirst();

            canEdit = RampsUtil.currentUserCanEdit(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(),
                    compositionInstance.getComposition());
            String date = new SimpleDateFormat(Constants.DATE_FORMAT).format(compositionInstance.getWorkunitDate());
            lastRunLabel.setValue(Labels.getLabel("createdon") + " " + date);
        }

        if (!isDDLValid) {
            return;
        } else if (ddlName.isPresent()) {
            // If the user have personal layouts retrieving it else retrieve
            // default layout
            String layout = null;
            String personalLayout = retrieveLayout();
            setPrivateView(personalLayout != null);

            if (isPrivateView) {
                // loading personal view
                layout = personalLayout;
                viewToggleButton.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
                otherViewBtn.setLabel(Labels.getLabel(DEFAULT_VIEW));
            } else {
                // Loading default view
                layout = getLayout(false);
            }
            toggleSaveButtons();

            if (dashboardConfig.isStaticData()) {
                contract = getContract();
            }
            renderDashboard(startTime, canEdit, layout, contract);
        } else {
            // TODO:instead of showing as notification , need to show in the
            // page as a text
            Clients.showNotification(Labels.getLabel("RunDashboardToViewChart"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, Constants.MESSAGE_VIEW_TIME, true);
            return;
        }

        if (!canEdit) {
            // Hide save layout button
            editLayout.setVisible(false);
            layoutSave.setVisible(false);
            closeLayout.setVisible(false);
        }

        chartHolder.addEventListener("onSave", (SerializableEventListener<? extends Event>) this::saveLayout);
        
        //Event trigger when user communicate with visualization elements 
        chartHolder.addEventListener("onSessionAlive", (SerializableEventListener<? extends Event>)this::sessionLive);

        this.getSelf().addEventListener(Constants.EVENTS.ON_CONFIRM_GCID, (SerializableEventListener<? extends Event>) this::doAfterGCIDSelection);

        ComboitemRenderer<Integer> gcidItemRenderer = (comboitem, gcid, index) -> comboitem
                .setLabel(gcid == 0 ? "Layout without GCID" : gcid.toString());
        gcidCombobox.setItemRenderer(gcidItemRenderer);
        viewToggleButton.setVisible(getPrivateView());
    }
    private void sessionLive(Event event) {
        LOGGER.debug((String) event.getData());
     }
    private void showError(String message) {
        titleHolder.setVisible(true);
        title.setValue(message);
        title.setSclass("dashboard-error-msg");
        LOGGER.error("Dashboard creation failed. {}", message);
    }

    /**
     * Gets the private layout
     * @return String - Layout string or null if no private layout
     */
    private String retrieveLayout() {
        String layout = null;
        try {
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            LOGGER.debug("ID for compostion - {}", dashboardConfig.getComposition().getId());
            Set<Integer> layoutGCIDs = new TreeSet<Integer>();
            // Get public GCIDs
            layoutGCIDs.addAll(dermatologyService.getLayoutGcIds(Constants.GENERIC_USER, getCompositionId(), ddlName.get()));
            // Get private GCIDs
            layoutGCIDs.addAll(dermatologyService.getLayoutGcIds(userID, getCompositionId(), ddlName.get()));
            LOGGER.debug("GCID's for compostion - {}", layoutGCIDs);
            gcidModel.clear();
            gcidModel.addAll(layoutGCIDs);

            if (!gcidModel.isEmpty()) {
                // Selecting first GCID to get layout
                activeGCID = layoutGCIDs.iterator().next();
                gcidModel.addToSelection(activeGCID);
            }
            
            layout = dermatologyService.getLayout(userID, getCompositionId(), getCompVersion(), ddlName.get(), activeGCID);

            // Adding 0 for layout without GCID
            if (!gcidModel.contains(0)) {
                gcidModel.add(Integer.valueOf(0));
            }

            showGCIDSelector();
        } catch (DermatologyException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
        }
        return layout;
    }

    private String getCompositionId() {
        return dashboardConfig.isStaticData() ? dashboardConfig.getComposition().getId() : compositionInstance.getCompositionId();
    }

    /**
     * Making selectors visible only when at least one non default GCID has a
     * layout
     */
    private void showGCIDSelector() {
        if (gcidModel.size() > 1 || (gcidModel.size() == 1 && !gcidModel.contains(0))) {
            gcidBox.setVisible(true);
        } else {
            gcidBox.setVisible(false);
        }
    }

    private void renderDashboard(long startTime, boolean canEdit, String layout, Contract contract) {
        
        /**
         * Set chart holder ID to resize the dashboard through marshaler.The share and ramps dashboard view, 
         * wont have the tab object and resizing is not required
         */
        if(dashboardConfig.getDashboardTab()!= null){
            dashboardConfig.getDashboardTab().setAttribute(Constants.DASHBOARD_HOLDER_UUID, chartHolder.getUuid());
        } else if (projectTab != null) {
            projectTab.setAttribute(Constants.DASHBOARD_HOLDER_UUID, chartHolder.getUuid());
        }

        if (dashboardConfig.isStaticData()) {
            try {
                RampsUtil.renderDashboard(userID, contract, dashboardConfig.getDashboard().getQueries().keySet(), chartHolder.getUuid(),
                        isScoredSearchForm ? formDiv.getUuid() : null, layout, canEdit);
            } catch (HipieException e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER,
                        Constants.MESSAGE_VIEW_TIME, true);
            }
        } else {
            RampsUtil.renderDashboard(compositionInstance, ddlName.get(), chartHolder.getUuid(), isScoredSearchForm ? formDiv.getUuid() : null,
                    layout, canEdit);

            if (LOGGER.isDebugEnabled()) {
                ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new DataAccess(compositionInstance, ddlName.get(), startTime));
            }
        }
    }

    private Contract getContract() throws HipieException {
        Dashboard dashboard = dashboardConfig.getDashboard();
        Composition composition = dashboard.getComposition();

        // Assuming only contract Hooked-up for a Databomb composition is
        // Dashboard
        ContractInstance contractInstance = composition.getContractInstances().values().iterator().next();

        Map<String, QuerySchema> queries = CompositionUtil.extractQueries(contractInstance, dashboardConfig.getDashboard().isStaticData());
        dashboard.setQueries(queries);

        return contractInstance.getContract();
    }

    private void saveLayout(Event event) {
        if (canEditLayout) {
            boolean success = false;

            try {
                DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
                
                if (Action.SAVE_AS_PRIVATE == saveAsAction) {
                    success = dermatologyService.saveLayout(userID, dashboardConfig.getComposition().getId(), 
                            dashboardConfig.getComposition().getVersion(), ddlName.get(), activeGCID, event.getData().toString());
                } else {
                    success = dermatologyService.saveLayout(Constants.GENERIC_USER, dashboardConfig.getComposition().getId(), 
                            dashboardConfig.getComposition().getVersion(), ddlName.get(), activeGCID, event.getData().toString());
                }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                        true);
            }

            if (success) {
                Clients.showNotification(Labels.getLabel("layoutSaved"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
                if (!gcidModel.contains(activeGCID)) {
                    gcidModel.add(activeGCID);
                }

                gcidModel.addToSelection(activeGCID);
                showGCIDSelector();
            } else {
                Clients.showNotification(Labels.getLabel("unableToSaveLayout"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
            }

        } else {
            Clients.showNotification(Labels.getLabel("dontHaveEditLayoutPermission"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
        }
        viewToggleButton.setVisible(isPrivateViewAvailable());
    }

    private String getCompVersion() {
        return dashboardConfig.isStaticData() ? dashboardConfig.getComposition().getVersion() : compositionInstance.getComposition().getVersion();
    }

    @Listen("onClick = #editbtn")
    public void editChart() {
        Events.postEvent(Dashboard.EVENTS.ON_EDITING_DASHBOARD, dashboardConfig.getViewEditTabbox(), null);
    }

    @Listen("onClick = #editLayout")
    public void editLayout() {
        layoutSave.setVisible(true);
        closeLayout.setVisible(true);
        editLayout.setVisible(false);
        viewToggleButton.setStyle("pointer-events:none;border: none;");
        Clients.evalJavaScript("showProperties(true,'" + chartHolder.getUuid() + "')");
    }

    @Listen("onClick = #layoutSave")
    public void initiateLayoutSave() {
        setSaveAsAction(isPrivateView ? Action.SAVE_AS_PRIVATE : Action.SAVE_AS_PUBLIC);
        triggerJS();
    }

    private void triggerJS() {
        editLayout.setVisible(true);
        layoutSave.setVisible(false);
        closeLayout.setVisible(false);
        viewToggleButton.setStyle(POINTER_EVENTS);

        Clients.evalJavaScript(SHOW_PROPERTIES_FALSE + chartHolder.getUuid() + "')");
        Clients.evalJavaScript(SAVE_DASHBOARD_LAYOUT + chartHolder.getUuid() + "')");

        toggleSaveButtons();
    }

    @Listen("onClick = #saveGCIDLayout")
    public void selectGCID() {
        Window window = (Window) Executions.createComponents("dashboard/select_layout_gcid.zul", this.getSelf(), null);
        window.doModal();
    }

    @Listen("onClick = #saveAsDefaultView")
    public void onclickSaveasDefault() {
        isPrivateView = false;
        toggleSaveButtons();
        setSaveAsAction(Action.SAVE_AS_PUBLIC);
        triggerJS();
    }

    @Listen("onClick = #saveAsPersonalizedView")
    public void onclickSaveasPrivate() {
        isPrivateView = true;
        toggleSaveButtons();
        setSaveAsAction(Action.SAVE_AS_PRIVATE);
        triggerJS();
    }

    @Listen("onClick = #closeLayout")
    public void closeLayoutDiv() {
        layoutSave.setVisible(false);
        editLayout.setVisible(true);
        closeLayout.setVisible(false);
        viewToggleButton.setStyle(POINTER_EVENTS);
        Clients.evalJavaScript(SHOW_PROPERTIES_FALSE + chartHolder.getUuid() + "')");
    }

    @Listen("onClose = #dashboardContainer")
    public void hideDashboardView(Event event) {
        Events.postEvent(EVENTS.ON_RETURN_TO_EDIT, dashboardConfig.getRampsContainer(), null);
    }

    public boolean getIsScoredSearchForm() {
        return isScoredSearchForm;
    }

    public boolean getCanEditLayout() {
        return canEditLayout;
    }

    public void setCanEditLayout(boolean canEditLayout) {
        this.canEditLayout = canEditLayout;
    }

    private void doAfterGCIDSelection(Event event) {
        Company selectedGcid = (Company) event.getData();
        activeGCID = selectedGcid.getGcId().intValue();

        initiateLayoutSave();
    }

    @Listen("onSelect = #gcidCombobox")
    public void getGCIDLayout(Event event) {
        int selectedGCID = gcidModel.getSelection().iterator().next();
        activeGCID = selectedGCID;
        LOGGER.debug("selectedGcid -->{}", selectedGCID);

        String personalLayout = getLayout(true);
        setPrivateView(personalLayout != null);

        String layout = isPrivateView ? personalLayout : getLayout(false);
        toggleSaveButtons();

        renderDashboard(Instant.now().toEpochMilli(), canEdit, layout, contract);
    }

    @Listen("onClick = #otherViewBtn")
    public void toggleViewButton() {
        String layout = null;
        if (isPrivateView) {
            // change to public view
            renderDashboard(Instant.now().toEpochMilli(), canEdit, getLayout(false), contract);
        } else {
            // Change to private view
            layout = getLayout(true);
            if (layout == null || layout.isEmpty()) {
                Clients.showNotification("No personal layouts to view", Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
            renderDashboard(Instant.now().toEpochMilli(), canEdit, layout, contract);
        }
        isPrivateView = !isPrivateView;
        toggleSaveButtons();
    }

    private StringBuilder shareURL() {
        String port = (Executions.getCurrent().getServerPort() == 80) ? "" : (":" + Executions.getCurrent().getServerPort());
        return new StringBuilder(Executions.getCurrent().getScheme()).append("://").append(Executions.getCurrent().getServerName()).append(port)
                .append(Executions.getCurrent().getContextPath()).append("/?").append(Constants.COMPOSITION).append("=")
                .append(dashboardConfig.getComposition().getId());
    }

    private String getLayout(boolean isPrivate) {
        try {
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            return dermatologyService.getLayout(isPrivate ? userID : Constants.GENERIC_USER, getCompositionId(), getCompVersion(), ddlName.get(), activeGCID);
        } catch (DermatologyException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return null;
        }
    }

    private void toggleSaveButtons() {
        saveAsDefaultView.setVisible(isPrivateView);
        saveAsPersonalizedView.setVisible(!isPrivateView);

        if (isPrivateView) {
            viewToggleButton.setVisible(true);
            viewToggleButton.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
            otherViewBtn.setLabel(Labels.getLabel(DEFAULT_VIEW));
        } else {
            viewToggleButton.setLabel(Labels.getLabel(DEFAULT_VIEW));
            otherViewBtn.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
        }
    }

    public ListModelList<Integer> getLayoutGCIDsModel() {
        return gcidModel;
    }

    public boolean getIsRampsConfig() {
        return isRampsConfig;
    }

    public void setIsRampsConfig(boolean isRampsConfig) {
        this.isRampsConfig = isRampsConfig;
    }

    public boolean isPrivateViewAvailable() {
        String layout = getLayout(true);
        return layout != null && !layout.isEmpty();
    }

    public void setPrivateView(boolean isPrivateView) {
        this.isPrivateView = isPrivateView;
    }

    public boolean getPrivateView() {
        return this.isPrivateView;
    }

    public ListModelList<Integer> getGcidModel() {
        return gcidModel;
    }

    public void setGcidModel(ListModelList<Integer> gcidModel) {
        this.gcidModel = gcidModel;
    }

    public Action getSaveAsAction() {
        return saveAsAction;
    }

    public void setSaveAsAction(Action saveAsAction) {
        this.saveAsAction = saveAsAction;
    }
}
