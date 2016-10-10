package org.hpccsystems.dsp.ramps.controller;

import java.security.AccessControlException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.CompositionAccess;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.hpccsystems.dsp.ramps.controller.utils.RunComposition;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.error.HError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.lang.Exceptions;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ProjectDetailController extends SelectorComposer<Component> implements EventListener<Event> {

    private static final String INVALID_DASHBOARD = "invalidDashboard";
    private static final String ERROR_OCCURED_WHILE_RUNNING = "errorOccuredWhileRunning";
    private static final String NOT_ENOUGH_DATA = "notEnoughData";
    private static final String ALREADY_LOADED = "already loaded";
    private static final String VBOX_CONTAINER = "vboxContainer";
    private static final String PROJECT_RUNNING = "projectRunning";
    private static final String NOTIFY_LABEL = "notifyLabel";
    private static final String HTML_HOLDER = "htmlHolder";
    private static final String ERROR = "error";
    private static final String PROJECT_EDIT_PROJECT_ZUL = "/ramps/project/edit_project.zul";
    
    private static final long serialVersionUID = -1134949976263404906L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDetailController.class);

    @Wire
    private Include editProjectHolder;
    @Wire
    private Include processHolder;

    @Wire
    private Button configButton;

    @Wire
    private Label notifyLabel;

    @Wire
    private A moreInfo;

    @Wire
    private Timer timer;

    @Wire
    private Button viewFlowEdit;

    // Don't instantiate
    private TabData data;

    @Wire
    private Tab processTab;

    @Wire
    private Include importInclude;
    @Wire
    private Include dashboardConfigInclude;
    @Wire
    private Include dashboardViewInclude;
    @Wire
    private Tab rampsTab;
    @Wire
    private Tab importTab;
    @Wire
    private Tab dashboardTab;
    @Wire
    private Tab viewDashboardTab;

    @Wire
    private Combobutton viewDashboardCombo;

    @Wire
    private Button configDashboardButton;

    @Wire
    private Popup viewDashboardPopup;

    @Wire
    private Combobutton saveRootButton;

    @Wire
    private Button runBtn;

    @Wire
    private Button viewDashboardButton;
    
    @Wire
    private Listcell downloadComp;

    private Composition originalComposition;

    @WireVariable
    private Desktop desktop;

    private String userId;
    private CompositionInstance recentInstance;

    private boolean isSaveAs;
    private boolean hipieCanEdit;
    private boolean hipieCanView;
    private boolean hipieCanRun;
    private boolean canViewProject;
    private boolean canConfigureDashboard;
    private boolean canViewDashboard;
    private boolean blacklistedDatasource = false;

    private boolean runAfterSave;
    private boolean isDashboardConfigured;

    Window configWindow;

    // Instance for run logger
    long startTime;

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {

        try {
            LOGGER.debug("Args - {}", Executions.getCurrent().getArg());
            userId = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
                    .getId();
            data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
            data.getProjectDetailComponent();

            if (data.getComposition() != null) {
                setHIPIECanEdit(
                        HipieSingleton.getHipie().getPermissionsManager().userCanEdit(userId, data.getComposition()));
                setHIPIECanView(
                        HipieSingleton.getHipie().getPermissionsManager().userCanView(userId, data.getComposition()));
                setHIPIECanRun(
                        HipieSingleton.getHipie().getPermissionsManager().userCanRun(userId, data.getComposition()));

                CompositionUtil.addVizVersion(data.getComposition(), false);
            }

            settingFeatures();

            setCanConfigureDashboard(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
                    .getCurrentUser().canEdit() && getHIPIECanEdit() && Flow.VIEW != data.getFlow());

            setCanViewDashboard(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
                    .getCurrentUser().canViewOutput() && getHIPIECanView());

        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            String excepMessage = Exceptions.getMessage(ex);
            Throwable excepObj = Exceptions.getRealCause(ex);
            excepMessage = typeOfException(excepMessage, excepObj);
            Clients.showNotification(excepMessage, Clients.NOTIFICATION_TYPE_ERROR, null, Constants.POSITION_TOP_CENTER,
                    0, true);
            return null;
        }

        return super.doBeforeCompose(page, parent, compInfo);
    }

    private void settingFeatures() throws HipieException {
        if (Flow.VIEW == data.getFlow()) {
            setCanViewProject(true);
        }

        if (Constants.Flow.EDIT.equals(data.getFlow())) {
            cloneComposition(false);
        }
        if (Constants.Flow.CLONE.equals(data.getFlow())) {
            cloneComposition(true);
        }
    }

    private void disableSaveAndRunButtons() {
        if (saveRootButton != null) {
            saveRootButton.setDisabled(true);
            saveRootButton.setTooltiptext(Labels.getLabel(NOT_ENOUGH_DATA));
        }
        runBtn.setDisabled(true);
        configDashboardButton.setDisabled(true);
        runBtn.setTooltiptext(Labels.getLabel(NOT_ENOUGH_DATA));
        configDashboardButton.setTooltiptext(Labels.getLabel("notEnoughDataconfiguredashboard"));
        if (downloadComp != null) {
            downloadComp.setVisible(false);            
        }
    }

    private void enableSaveAndRunButtons() {
        checkProjectForBlacklistedFiles();
        if (runBtn != null  && !blacklistedDatasource) {
            runBtn.setDisabled(false);
        }
        
        if (saveRootButton != null && !blacklistedDatasource) {
            saveRootButton.setDisabled(false);
            saveRootButton.setTooltiptext("");
        }
        configDashboardButton.setDisabled(false);
        runBtn.setTooltiptext("");

        configDashboardButton.setTooltiptext("");
        if ((!Constants.Flow.NEW.equals(data.getFlow())) && (viewDashboardCombo != null)) {
            viewDashboardCombo.setDisabled(false);
        }
    }

    private void enableConfigDashboardButton() {
        if (configDashboardButton != null) {
            configDashboardButton.setDisabled(false);
        }
        if (viewDashboardCombo != null) {
            viewDashboardCombo.setDisabled(false);
        }
    }

    private void disableConfigDashboardButton() {
        if (configDashboardButton != null) {
            configDashboardButton.setDisabled(true);
        }
        if (viewDashboardCombo != null) {
            viewDashboardCombo.setDisabled(true);
        }
    }

    private String typeOfException(String excepMessageCopy, Throwable excepObj) {
        String excepMessage;
        if (excepObj instanceof RuntimeException) {
            excepMessage = org.zkoss.util.resource.Labels.getLabel("errorruntime");
        } else {
            excepMessage = excepMessageCopy.indexOf(":") != -1 ? excepMessageCopy.split(":")[1].trim().length() > 0
                    ? excepMessageCopy.split(":")[1] : excepMessageCopy.split(":")[0] : excepMessageCopy;
        }
        return excepMessage;
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        data = (TabData) Executions.getCurrent().getAttribute(Constants.TAB_DATA);

        editProjectHolder.setDynamicProperty(Constants.TAB_DATA, data);
        editProjectHolder.setSrc(PROJECT_EDIT_PROJECT_ZUL);

        processHolder.setDynamicProperty(Constants.TAB_DATA, data);

        this.getSelf().addEventListener(EVENTS.ON_RUN_COMPLETE, new SerializableEventListener<Event>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                doAfterRun(event);
            }
        });

        // Disabling Run and Save buttons to be enabled when data source is
        // configured
        if ((data.getFlow().equals(Flow.NEW) || data.getFlow().equals(Flow.CLONE) )&& 
                data.getProject().getDatasetPlugin() != null && 
                !data.getProject().getDatasetPlugin().hasValidFiles()) {
            disableSaveAndRunButtons();
        } else if(data.getProject().getPlugins() != null && ((Plugin)data.getProject().getPlugins().get(0)).isInputPlugin()) { // Check if the plugin is an input plugin and disable Run and Save buttons.
            enableSaveAndRunButtons();
            if (downloadComp != null) {
                downloadComp.setVisible(true);
            }
        } else {
            if (DatasourceStatus.LOADING.equals(data.getProject().getDatasourceStatus())) {
                if (configDashboardButton != null) {
                    configDashboardButton.setDisabled(true);
                }
                if (viewDashboardCombo != null) {
                    viewDashboardCombo.setDisabled(true);
                }
            }
        }

        isDashboardConfigured = RampsUtil.isDashboardConfigured(data.getComposition());
        if (isDashboardConfigured) {
            try {
                recentInstance = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .getmostRecentInstance(data.getComposition(), true);
            } catch (HipieException d) {
                LOGGER.error(Constants.EXCEPTION, d);
            }
        }
        boolean isdashboardCreated = recentInstance != null;
        boolean canOnlyView = isDashboardConfigured && isdashboardCreated && getCanViewDashboard();
        if (canOnlyView) {
            showViewEditDashboardButton();
        } else if (canOnlyView && !getCanConfigureDashboard()) {
            showViewDashboardButton();
        } else if (getCanConfigureDashboard()) {
            showConfigureDashboardButton();
        }
        
        checkProjectForBlacklistedFiles();
        
        this.getSelf().addEventListener(EVENTS.ON_SAVE, event -> initiateSave());

        this.getSelf().addEventListener(EVENTS.ON_RUN_INITIATED, event -> runComposition());

        this.getSelf().addEventListener(EVENTS.ON_CHANGE_HPCC_CONNECTION, event -> {
            if (data.getProject() != null && data.getProject().getDatasetPlugin() != null) {
                Events.postEvent(EVENTS.ON_CHANGE_HPCC_CONNECTION, editProjectHolder.getFirstChild().getFellow("browserInclude"), null);
            } else {
                enableSaveAndRunButtons();
            }
        });

        this.getSelf().addEventListener(EVENTS.ON_RETURN_TO_EDIT, event -> showCompositionEdit());

        this.getSelf().addEventListener(EVENTS.ON_DASHBOARD_VIEW, event -> showViewDashboard());

        this.getSelf().addEventListener(EVENTS.ON_IMPORT_FILE, event -> showImportFile((String) event.getData()));

        this.getSelf().addEventListener(EVENTS.ON_SELECT_DATASOURCE, event -> enableSaveAndRunButtons());

        this.getSelf().addEventListener(Constants.EVENTS.DELETE_DASHBOARD_DUD_AND_SAVE_COMP, event -> {
            DashboardConfig config = (DashboardConfig) event.getData();
            CompositionUtil.deleteDashboardConfiguration(config.getComposition());
            initiateSave();
        });

        this.getSelf().addEventListener(EVENTS.ON_COMPLETE_BROWSER_LOADING, this::doAfterLoadingBrowser);

        data.setProjectDetailComponent(getSelf());
        this.getSelf().addEventListener(EVENTS.ON_CLOSE_SAVE_AS_WINDOW, event -> isSaveAs = false);

        this.getSelf().addEventListener(EVENTS.ON_STARTOF_BROWSER_LOADING, event -> disableConfigDashboardButton());

        timer.stop();
    }
    
    private void checkProjectForBlacklistedFiles() {
        blacklistedDatasource = false;
        
        boolean logicalFile = false;
        boolean globalVariable = false;
        
        List<String> blacklist = null;
        LogicalFileService logicalFileService = ((LogicalFileService) SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE));
        blacklist = logicalFileService.getBlacklistedThorFiles();
        
        if (data.getProject().getDatasetPlugin() != null &&
                logicalFileService.checkPluginsForBlacklistedFiles(
                        data.getProject().getDatasetPlugin(), blacklist)) {
            logicalFile = true;
        }
        
        // Also check the global variables
        List<Element> filteredGlobalInputs;
        filteredGlobalInputs = RampsUtil.filterSettingsPageInputs(data.getComposition().getInputElements());
        if (filteredGlobalInputs != null && logicalFileService.checkElementsForBlacklistedFiles(filteredGlobalInputs, blacklist)) {
            globalVariable = true;
        }
        if (logicalFile || globalVariable) {
            setInvalidPluginInProject(logicalFile, globalVariable);
        }
    }
    
    private void setInvalidPluginInProject(boolean isLogicalFile, boolean isGlobal) {
        data.getProject().setDatasourceStatus(DatasourceStatus.INVALID);
        blacklistedDatasource = true;
        disableSaveAndRunButtons();
        
        if (isLogicalFile && isGlobal) {
            Clients.showNotification(Labels.getLabel("invalidDatasource").concat("<br><br>")
                    .concat(Labels.getLabel("invalidDatasourceInGlobalVariables")), 
                    Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 10000, true);
        }else if (isGlobal) {
            Clients.showNotification(Labels.getLabel("invalidDatasourceInGlobalVariables"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
        } else {
            Clients.showNotification(Labels.getLabel("invalidDatasource"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
        }
        
    }

    private void doAfterLoadingBrowser(Event event) {
        if (data.getProject().getDatasetPlugin().hasValidFiles()) {
            // TODO Removed file structure validation check from here. Look for
            // potential impacts
            enableConfigDashboardButton();
            data.getProject().setDatasourceStatus(DatasourceStatus.VALID);
        } else {
            data.getProject().setDatasourceStatus(DatasourceStatus.INVALID);
            disableConfigDashboardButton();
        }
    }

    private void doAfterRun(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) event.getData();
        data.setComposition((Composition) properties.get(Constants.COMPOSITION));
        data.setProject((Project) properties.get(Constants.PROJECT));
        if (configDashboardButton != null && data.getFlow() != Flow.VIEW) {
            configDashboardButton.setDisabled(false);
        }
        if (viewDashboardCombo != null && data.getFlow() != Flow.VIEW) {
            viewDashboardCombo.setDisabled(false);
        }
        onClickProcessTab();
        if (configWindow != null) {
            Events.postEvent(EVENTS.ON_ENABLE_SETTINGS, configWindow, null);
        }
        Events.postEvent(EVENTS.ON_UPDATE_PROCESSES, processHolder, properties.get(Constants.COMPOSITION));
    }

    private void showViewDashboardButton() {
        configDashboardButton.setVisible(false);
        viewDashboardButton.setVisible(true);
        viewDashboardCombo.setVisible(false);
    }

    private void showViewEditDashboardButton() {
        configDashboardButton.setVisible(false);
        viewDashboardButton.setVisible(false);
        viewDashboardCombo.setVisible(true);
    }

    private void showConfigureDashboardButton() {
        configDashboardButton.setVisible(true);
        viewDashboardButton.setVisible(false);
        viewDashboardCombo.setVisible(false);
    }

    /**
     * Required to call only in 'Save As' Flow Changes the isOpen flag in
     * project that is being closed and Newly saved project
     */
    protected void updateSavedprojectToTab() {
        Tab editTab = ((Tabpanel) ProjectDetailController.this.getSelf().getParent().getParent()).getLinkedTab();

        editTab.setAttribute(Constants.PROJECT, data.getProject());
    }

    @Listen("onClick = #configButton")
    public void createConfigurationModal(Event event) {
        if (getSelf().hasFellow("configContainer")) {
            LOGGER.error(
                    "Cannot create more than one component with the same id, must be an error due to user action(Clicking same button more than once)");
            return;
        }
        // Setting the original composition into tabData,.
        // When the project is opened Edit mode and trying to edit the GCID,it
        // will be again opened in Edit mode.
        data.setOriginalComposition(originalComposition);
        Map<String, Object> newProjectData = new HashMap<String, Object>();
        newProjectData.put(Constants.TAB_DATA, data);
        configWindow = (Window) Executions.createComponents("ramps/project/configuration.zul", getSelf(),
                newProjectData);
        configWindow.doModal();
        if (data.getProject().isRunning()) {
            Events.postEvent(EVENTS.ON_DISABLE_SETTINGS, configWindow, null);
        } else {
            Events.postEvent(EVENTS.ON_ENABLE_SETTINGS, configWindow, null);
        }
    }

    private void showCompositionEdit() {
        rampsTab.setSelected(true);
        enableButtons();
    }

    private void enableButtons() {
        checkProjectForBlacklistedFiles();
        if (saveRootButton != null && !blacklistedDatasource) {
            saveRootButton.setDisabled(false);
        }
        if (runBtn != null && !blacklistedDatasource) {
            runBtn.setDisabled(false);
        }
        if (configButton != null) {
            configButton.setDisabled(false);
        }
        if (configDashboardButton != null) {
            configDashboardButton.setDisabled(false);
        }
        if (viewDashboardCombo != null) {
            viewDashboardCombo.setDisabled(false);
        }
    }

    private DashboardConfig generateDashboardConfig() {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(DashboardUtil.removeSpaceSplChar(data.getComposition().getName()));
        dashboard.setClusterConfig(data.getProject().getClusterConfig());
        DashboardConfig dashboardConfig = new DashboardConfig();
        dashboardConfig.setComposition(data.getComposition());
        dashboardConfig.setDashboard(dashboard);
        dashboardConfig.setRampsContainer(getSelf());
        dashboardConfig.setRAMPSConfig(true);
        dashboardConfig.setHomeTabbox((Tabbox) getSelf().getParent().getParent().getFellow(Constants.HOME_TABBOX));
        dashboardConfig.setData(data);
        return dashboardConfig;
    }

    private void showImportFile(String directoryName) {
        importInclude.setDynamicProperty(Constants.TAB_DATA, data);
        importInclude.setDynamicProperty(Constants.FILE, directoryName);
        importInclude.setSrc("ramps/project/import_file.zul");
        importTab.setSelected(true);

        disableButtons();
    }

    @Listen("onClick = #configDashboardButton; onClick = #configureDashboard")
    public void showDashboardConfig() {
        viewDashboardPopup.close();
        dashboardConfigInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, 
                DashboardUtil.generateDashboardConfigFromRamps(data, getSelf()));
        dashboardConfigInclude.setSrc("ramps/configure_dashboard.zul");
        dashboardTab.setSelected(true);
        disableButtons();
    }

    @Listen("onClick = #viewDashboardCombo, #viewDashboardButton")
    public void showViewDashboard() {
        dashboardViewInclude.setSrc(null);
        dashboardViewInclude.clearDynamicProperties();

        try {
            recentInstance = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .getmostRecentInstance(data.getComposition(), false);
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToGrabWU"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }

        dashboardViewInclude.setDynamicProperty(Dashboard.MOST_RECENT_CI, recentInstance);
        dashboardViewInclude.setDynamicProperty(Constants.PROJECT_TAB, data.getProjectTab());
        dashboardViewInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, generateDashboardConfig());
        dashboardViewInclude.setSrc("ramps/view_dashboard.zul");
        viewDashboardTab.setSelected(true);

        disableButtons();
    }

    void disableButtons() {
        if (saveRootButton != null) {
            saveRootButton.setDisabled(true);
        }
        if (runBtn != null) {
            runBtn.setDisabled(true);
        }
        if (configButton != null) {
            configButton.setDisabled(true);
        }
        if (configDashboardButton != null) {
            configDashboardButton.setDisabled(true);
        }
        if (viewDashboardCombo != null) {
            viewDashboardCombo.setDisabled(true);
        }
    }

    @Listen("onClick = #saveRootButton, #saveButton, #saveAsButton, #saveAsView")
    public void saveRootButton(Event event) {
        data.setNotifyUser(true);
        runAfterSave = false;
        if (hasDatasourceErrors()) {
            return;
        }

        if (data.getProject().isRunning()) {
            showRunningNotification();
            return;
        }
        isSaveAs = "saveAsButton".equals(event.getTarget().getId()) || "saveAsView".equals(event.getTarget().getId());
        Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, editProjectHolder.getFirstChild().getFellow(HTML_HOLDER),
                RampsUtil.getFileBrowserData(Constants.ACTION.SAVE));
    }

    private void showRunningNotification() {
        Clients.showNotification(Labels.getLabel("projectAlreadyRunning"), Clients.NOTIFICATION_TYPE_WARNING, getSelf(),
                Constants.POSITION_TOP_CENTER, 5000, true);
    }

    private void initiateSave() {
        boolean errorOccured = doHIPIEValidation();
        if (errorOccured) {
            // Enable/Disable global variable btn based on variables available
            // in tabdata.getComposition()/unsaved composition
            Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, data.getUsedatasetFormHolder(), null);
            return;
        }

        if (isSaveAs) {
            callSaveAsDialog(Constants.ACTION.SAVE_AS);
        } else {
            executeSave();
        }
    }

    /**
     * @return Validation errors occurred
     */
    private boolean doHIPIEValidation() {
        
        //Check the contracts of the composition is exists on repository
        try {
            for (ContractInstance contractInstance : data.getComposition().getContractInstances().values()) {
                if (HipieSingleton.getHipie().getContract(contractInstance.getContract().getAuthor(),
                        contractInstance.getContract().getCanonicalName()) == null) {
                    StringBuilder exeMsg = new StringBuilder(Labels.getLabel("contract"));
                    exeMsg.append(" ");
                    exeMsg.append(contractInstance.getContract().getName());
                    exeMsg.append(" ");
                    exeMsg.append(Labels.getLabel("notExistsInRepo"));
                    throw new HipieException(exeMsg.toString());
                }
            }
        } catch (Exception e) {
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
            LOGGER.error(Constants.EXCEPTION, e);
            return true;
        }
        
        if (data.getProject().getReferenceId() != null) {
            try {
                ContractInstance outputInstance = RampsUtil.getOutputDatasetInstance(data.getComposition());
                if (outputInstance != null) {
                    String name = RampsUtil.getOutputDatasetName(outputInstance);
                    // TODO:need to test whether the output file is correctly
                    // written HPCC cluster,While defining GCID
                    if (name != null && !RampsUtil.validateOuputDatasetName(name, data.getProject())) {
                        Clients.showNotification(Labels.getLabel("outputHasInvalidFilename"),
                                Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000,
                                true);
                        return true;
                    }
                }

            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }

        ErrorBlock errorBlock = data.getComposition().validate();
        for (HError error : errorBlock.getErrors()) {
            LOGGER.error(Constants.EXCEPTION, error);

            if (error.getErrorSource() != null && "CONTRACT".equals(error.getErrorSource().toString())
                    && error.getFilename() != null) {
                HIPIEService hipieService = HipieSingleton.getHipie();
                String dashboardRepo = hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO)
                        .getLocaldir();
                if (error.getFilename().contains(dashboardRepo)) {
                    Clients.showNotification(Labels.getLabel(INVALID_DASHBOARD), Clients.NOTIFICATION_TYPE_ERROR,
                            this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
                }

            } else if (error.getErrorSource() != null && "CONTRACTINSTANCE".equals(error.getErrorSource().toString())) {
                String contractRepoName = data.getComposition().getContractInstance(error.getSourceID()).getContract()
                        .getRepositoryName();
                HIPIEService hipieService = HipieSingleton.getHipie();
                String contractRepo = hipieService.getRepositoryManager().getRepos().get(contractRepoName)
                        .getLocaldir();
                String dashboardRepo = hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO)
                        .getLocaldir();
                if (dashboardRepo.contains(contractRepo)) {
                    // Handles Dashboard Contract
                    Clients.showNotification(Labels.getLabel(INVALID_DASHBOARD), Clients.NOTIFICATION_TYPE_ERROR,
                            this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
                } else {
                    // Handles Ramps Plugins
                    Events.postEvent(EVENTS.ON_VALIDATE, editProjectHolder.getFirstChild(), error);
                }
            } else {
                Clients.showNotification(error.getErrorString(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                        Constants.POSITION_TOP_CENTER, 0, true);
            }
            return true;
        }
        return false;
    }

    private void executeSave() {
        try {
            Project projectData = data.getProject();
            Composition composition = data.getComposition();

            if (Constants.Flow.EDIT == data.getFlow()) {
                // temp fix-hipie should hold the file obj for cloned
                // composition
                Map<String, ContractInstance> cis = new HashMap<String, ContractInstance>(
                        originalComposition.getContractInstances());
                for (Entry<String, ContractInstance> entry : cis.entrySet()) {
                    originalComposition.removeContractInstance(entry.getValue());
                }
                cis = composition.getContractInstances();
                for (Entry<String, ContractInstance> entry : cis.entrySet()) {
                    // set the original composition to container
                    originalComposition.addContractInstance(entry.getValue());
                }

                // global variables
                List<Element> globalInputs = originalComposition.getInputElements();
                originalComposition.getInputElements().removeAll(globalInputs);

                List<Element> clonedGlobalInputs = composition.getInputElements();
                originalComposition.getInputElements().addAll(clonedGlobalInputs);

                // security permissions
                Map<PermissionType, Permission> permissions = originalComposition.getPermissions();
                Map<PermissionType, Permission> newPermissions = composition.getPermissions();
                permissions.clear();
                permissions.putAll(newPermissions);

                originalComposition.setAuthor(composition.getAuthor());
                originalComposition.setLabel(composition.getLabel());

                // updating the project using Original composition
                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).updateProject(projectData,
                        ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(),
                        originalComposition);
                originalComposition.setLastModified(new Date().getTime());
                projectData.setLastModifiedDate(new Date(originalComposition.getLastModified()));

                // Making contractInstances(HIPIE container) refer 'composition'
                // instance
                cis = new HashMap<String, ContractInstance>(composition.getContractInstances());
                for (Entry<String, ContractInstance> entry : cis.entrySet()) {
                    composition.removeContractInstance(entry.getValue());
                }
                cis = originalComposition.getContractInstances();
                for (Entry<String, ContractInstance> entry : cis.entrySet()) {
                    // set contractInstances(HIPIE container) refer
                    // 'composition' instance
                    composition.addContractInstance(entry.getValue());
                }

                Events.postEvent(EVENTS.ON_UPDATE_GRID_PLUGINS,
                        getSelf().getParent().getParent().getFellow(Constants.HOME_TABBOX), projectData);
            } else if (Constants.Flow.NEW == data.getFlow() || Constants.Flow.CLONE == data.getFlow()) {
                LOGGER.info("Saving a new composition\n{}", composition);

                composition = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .saveNewCompositionOnHIPIE(projectData.getName(), composition);
                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .saveNewCompositionOnDatabase(projectData, composition);
                data.setComposition(composition);

                // Adding new project to UI
                projectData.setType("RAMPS");
                projectData.setLastModifiedDate(new Date(composition.getLastModified()));

                Events.postEvent(EVENTS.ON_PROJECT_ADD, getSelf().getParent().getParent().getFellow(Constants.HOME_TABBOX),
                        projectData);

                // Changing the Flow to edit
                data.setFlow(Constants.Flow.EDIT);
                cloneComposition(false);
            } else {
                Clients.showNotification(Labels.getLabel("saveFailed"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                        Constants.POSITION_TOP_CENTER, 3000, true);
                LOGGER.error("Save is skipped due to incorrect flow.");
            }

            // Enable/Disable global variable btn based on the flag in
            // project.showGlobalVariable
            Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, data.getUsedatasetFormHolder(), null);

            // Event which gets posted there is a change in Project's Label
            Events.postEvent(EVENTS.ON_CHANGE_LABEL, getSelf().getParent().getParent().getFellow(Constants.HOME_TABBOX),
                    projectData);

            doAfterSave();

        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            // Enable/Disable global variable btn based on variables available
            // in tabdata.getComposition()/unsaved composition
            Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, data.getUsedatasetFormHolder(), null);
            return;
        } catch (Exception e) {
            Clients.showNotification(Labels.getLabel("errorOccuredWhileSaving") + " : " + e.getLocalizedMessage(),
                    Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
            LOGGER.error(Constants.EXCEPTION, e);
            runAfterSave = false;
            // Enable/Disable global variable btn based on variables available
            // in tabdata.getComposition()/unsaved composition
            Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, data.getUsedatasetFormHolder(), null);
        }
    }

    private void doAfterSave() {
        if (runAfterSave) {
            Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, editProjectHolder.getFirstChild().getFellow(HTML_HOLDER),
                    RampsUtil.getFileBrowserData(Constants.ACTION.RUN));
            runAfterSave = false;
        } else if ((Constants.Flow.NEW == data.getFlow() || Constants.Flow.EDIT == data.getFlow()) && data.isNotifyUser()) {
            enableSaveAndRunButtons();
            Clients.showNotification(Labels.getLabel("compSaved"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
        }
        data.setNotifyUser(true);
        
        if (downloadComp != null) {
            downloadComp.setVisible(true);            
        }
    }

    @Listen("onClick = #saveAsTemplateButton")
    public void saveAsButtonTemplate(Event event) {
        callSaveAsDialog(Constants.SAVE_AS_TEMPLATE);
    }

    @Listen("onClick = #runBtn")
    public void validateBeforeRun() {
        if (hasDatasourceErrors()) {
            return;
        }

        if (data.getProject().isRunning()) {
            showRunningNotification();
            return;
        }

        if (!(hipieCanRun && ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
                .getCurrentUser().canRun())) {
            LOGGER.debug("User does not have permission to run composition");
            Clients.showNotification(Labels.getLabel("donotHavePermissionToRun"), Clients.NOTIFICATION_TYPE_ERROR,
                    this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }

        if (hipieCanEdit && ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE))
                .getCurrentUser().canEdit() && !(Constants.Flow.VIEW == data.getFlow())) {
            // Posting Save event
            isSaveAs = false;
            runAfterSave = true;
            Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, editProjectHolder.getFirstChild().getFellow(HTML_HOLDER),
                    RampsUtil.getFileBrowserData(Constants.ACTION.SAVE));
        } else {
            Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, editProjectHolder.getFirstChild().getFellow(HTML_HOLDER),
                    RampsUtil.getFileBrowserData(Constants.ACTION.RUN));
            runAfterSave = false;
        }

    }

    public void runComposition() {

        try {
            data.getProject().getHpccConnection().testConnection();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("hpccConnectionFailed") + ": " + e.getMessage(),
                    Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }

        if (data.getProject().isRunning()) {
            showRunningNotification();
            return;
        }

        if (!(Constants.Flow.VIEW == data.getFlow()) && !(Constants.Flow.EDIT == data.getFlow())) {
            Clients.showNotification(Labels.getLabel("saveComposition"), Clients.NOTIFICATION_TYPE_ERROR,
                    this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }

        if (!checkForErrors()) {
            return;
        }

        runAfterValidatingComposition();
    }

    private void runAfterValidatingComposition() {
        try {
            data.getProject().setRunning(true);
            processTab.setSelected(true);
            onClickProcessTab();
            Events.postEvent(EVENTS.ON_RUN_INITIATED, processHolder.getFirstChild(), null);
            if (configWindow != null) {
                Events.postEvent(EVENTS.ON_DISABLE_SETTINGS, configWindow, null);
            }
            disableEditing();

            // Run composition in separate thread
            desktop.enableServerPush(true);
            startTime = Instant.now().toEpochMilli();

            DSPExecutorHolder.getExecutor().execute(new RunComposition(data.getComposition(),
                    data.getProject().getHpccConnection(), userId, desktop, this));
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
        }
    }

    boolean checkForErrors() {
        ErrorBlock errorBlock = data.getComposition().validate();
        for (HError error : errorBlock.getErrors()) {
            LOGGER.error(Constants.EXCEPTION, error);
            Clients.showNotification(error.getErrorString(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
            return false;
        }

        if (!HipieSingleton.getHipie().getPermissionsManager().userCanRun(userId, data.getComposition())) {
            Clients.showNotification(Labels.getLabel("youDonotHavePermissionToRun"), Clients.NOTIFICATION_TYPE_ERROR,
                    getSelf().getParent().getParent().getFellow(Constants.HOME_TABBOX), Constants.POSITION_TOP_CENTER, 3000);
            return false;
        }
        return true;
    }

    /**
     * Clones the composition object Preserves actual composition in
     * 'originalComposition' reference 'composition' variable will hold cloned
     * Object
     * 
     * @throws Exception
     */
    private void cloneComposition(boolean newid) throws HipieException {
        // taking old comp ref for update
        originalComposition = data.getComposition();
        // Cloning the composition for handling the clone,save as
        String oldid = originalComposition.getId();
        Composition composition;
        try {
            composition = new Composition(data.getComposition());
            if (!newid) {
                composition.setId(oldid);
            }
        } catch (Exception e) {
            throw new HipieException(e);
        }

        // Updating contract instances in plugin
        for (Plugin plugin : data.getProject().getPlugins()) {
            if (plugin.isDatasourcePlugin()) {
                DatasetPlugin dsPlugin = (DatasetPlugin) plugin;
                dsPlugin.getPlugins().forEach(p -> p
                        .setContractInstance(composition.getContractInstance(p.getContractInstance().getInstanceID())));
            } else {
                plugin.setContractInstance(
                        composition.getContractInstance(plugin.getContractInstance().getInstanceID()));
            }
        }

        data.setComposition(composition);
    }

    private void callSaveAsDialog(String action) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.ACTION_SAVE, action);
        params.put(Constants.TAB_DATA, data);
        Window window = (Window) Executions.createComponents("ramps/project/save_composition.zul", this.getSelf(),
                params);
        window.doModal();
    }

    @Override
    public void onEvent(Event arg0) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER))
                    .log(new CompositionAccess(CompositionAccess.RUN, data.getComposition(), startTime));
        }
        if ("OnRunCompositionCompleted".equals(arg0.getName())) {
            LOGGER.debug("Run Composition completed");
            notifyLabel.setValue(Labels.getLabel("runCompleted"));
            notifyLabel.setSclass("notifyLabel-green");
            if (RampsUtil.isDashboardConfigured(data.getComposition())) {
                showViewEditDashboardButton();
            }
            timer.start();
        } else {
            LOGGER.debug("Run Composition failed.");
            LOGGER.debug("Error occured while running - {}", arg0.getData().toString());

            StringBuilder errorMessg = new StringBuilder();
            errorMessg.append(Labels.getLabel(Constants.DATA_TOO_LARGE))
                    .append(Labels.getLabel(Constants.LARGE_DATA_TOO_LARGE));

            if (arg0.getData().toString().contains(Constants.ERROR_MSG_LARGE_DATA)) {
                notifyLabel.setValue(errorMessg.toString());
            } else {
                notifyLabel.setValue(Labels.getLabel(ERROR_OCCURED_WHILE_RUNNING));
            }
            notifyLabel.setSclass("notifyLabel-red");
            moreInfo.setVisible(true);
            moreInfo.setAttribute(ERROR, arg0.getData().toString());
            if (arg0.getData() instanceof AccessControlException) {
                Clients.showNotification(Labels.getLabel("dontHaveRunPermission"), Clients.NOTIFICATION_TYPE_ERROR,
                        this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            }
        }

        data.getProject().setRunning(false);
        enableEditing();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Constants.COMPOSITION, data.getComposition());
        properties.put(Constants.PROJECT, data.getProject());
        Events.postEvent(EVENTS.ON_RUN_COMPLETE, this.getSelf(), properties);
    }

    private void disableEditing() {
        if (viewFlowEdit != null && Flow.VIEW == data.getFlow()) {
            viewFlowEdit.setDisabled(true);
        }
        if (configDashboardButton != null && data.getFlow() != Flow.VIEW) {
            configDashboardButton.setDisabled(true);
        }
        if (viewDashboardCombo != null && data.getFlow() != Flow.VIEW) {
            viewDashboardCombo.setDisabled(true);
        }
        notifyLabel.setVisible(true);
        notifyLabel.setValue(Labels.getLabel(PROJECT_RUNNING));
        notifyLabel.setSclass(NOTIFY_LABEL);
        timer.stop();

        moreInfo.setVisible(false);
        disableTabPanel();
    }

    private void disableTabPanel() {
        ((Vbox) editProjectHolder.getFirstChild().getFellow(VBOX_CONTAINER)).setSclass("disableTabPanel");
    }

    private void enableTabPanel() {
        ((Vbox) editProjectHolder.getFirstChild().getFellow(VBOX_CONTAINER)).setSclass("no");
    }

    private void enableEditing() {
        enableTabPanel();
        if (viewFlowEdit != null && Flow.VIEW == data.getFlow()) {
            viewFlowEdit.setDisabled(false);
        }
    }

    @Listen("onClick= #moreInfo")
    public void onClickMoreInfo() {
        Clients.showNotification(moreInfo.getAttribute(ERROR).toString(), Clients.NOTIFICATION_TYPE_ERROR,
                this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        notifyLabel.setVisible(false);
        moreInfo.setVisible(false);
    }

    @Listen("onSelect = #processTab")
    public void onClickProcessTab() {
        if (processTab.getAttribute(ALREADY_LOADED) == null) {
            processHolder.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.COMPOSITION);
            processHolder.setSrc("ramps/process/content.zul");
            processTab.setAttribute(ALREADY_LOADED, true);
        }
    }

    @Listen("onSelect= #projectTab")
    public void onSelectProjectTab(Event event) {
        if (!data.getProject().isRunning()) {
            editProjectHolder.setSrc(PROJECT_EDIT_PROJECT_ZUL);
        }
    }

    @Listen("onClick= #viewFlowEdit")
    public void onViewFlowEdit() {
        data.setFlow(Flow.EDIT);
        Include include = (Include) this.getSelf().getParent();
        include.setSrc("");
        include.setSrc("ramps/project/project_details.zul");
    }

    @Listen("onClick = #downloadComp")
    public void downloadComposition() {
        try {
           Filedownload.save(data.getProject().getComposition().toString(), "text", data.getProject().getComposition().getName() + ".CMP");

        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
        }
    }

    public boolean getHIPIECanEdit() {
        return hipieCanEdit;
    }

    public boolean getHIPIECanView() {
        return hipieCanView;
    }

    public boolean getHIPIECanRun() {
        return hipieCanRun;
    }

    public void setHIPIECanView(boolean userCanView) {
        this.hipieCanView = userCanView;
    }

    public void setHIPIECanEdit(boolean hipieCanEdit) {
        this.hipieCanEdit = hipieCanEdit;
    }

    public void setHIPIECanRun(boolean hipieCanRun) {
        this.hipieCanRun = hipieCanRun;
    }

    public boolean getCanViewProject() {
        return canViewProject;
    }

    public void setCanViewProject(boolean canViewProject) {
        this.canViewProject = canViewProject;
    }

    @Listen("onTimer = #timer")
    public void onTimer() {
        timer.stop();
        notifyLabel.setVisible(false);
        notifyLabel.setSclass(NOTIFY_LABEL);
        notifyLabel.setValue(Labels.getLabel(PROJECT_RUNNING));
    }

    @Listen("onClick = #changeCluster")
    public void changeCluster() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(Constants.TAB_DATA, data);
        Window window = (Window) Executions.createComponents("ramps/project/change_cluster.zul", null, props);
        window.doModal();

    }

    @Listen("onClick = #viewDashboardDUD")
    public void onViewDashboardDUD() {

        try {
            ContractInstance ins = data.getComposition()
                    .getContractInstanceByName(data.getComposition().getName() + Dashboard.CONTRACT_IDENTIFIER);
            if (ins == null) {
                ins = data.getComposition().getContractInstanceByName(
                        data.getComposition().getName() + Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER);
            }
            if (ins != null) {
                Map<String, Object> instanceMap = new HashMap<String, Object>();
                instanceMap.put(Constants.CONTRACT_INSTANCE, ins);
                Window window = (Window) Executions.createComponents("ramps/project/view_dud.zul", null, instanceMap);
                window.doModal();
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
        }

    }

    private boolean hasDatasourceErrors() {
    	if(data.getProject().getDatasetPlugin() != null) {
	        if (data.getProject().getDatasetPlugin().hasValidFiles()) {
	            return false;
	        } else {
	            Clients.showNotification(Labels.getLabel("chooseValidFile"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
	                    Constants.POSITION_TOP_CENTER, 3000, true);
	            return true;
	        }
    	} else {
    		return false;
    	}
    }

    public boolean getCanConfigureDashboard() {
        return canConfigureDashboard;
    }

    public void setCanConfigureDashboard(boolean canConfigureDashboard) {
        this.canConfigureDashboard = canConfigureDashboard;
    }

    public boolean getCanViewDashboard() {
        return canViewDashboard;
    }

    public void setCanViewDashboard(boolean canViewDashboard) {
        this.canViewDashboard = canViewDashboard;
    }
}
