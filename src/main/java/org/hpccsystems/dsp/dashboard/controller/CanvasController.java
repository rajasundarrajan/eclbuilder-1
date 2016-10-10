package org.hpccsystems.dsp.dashboard.controller;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.CompositionAccess;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.hpccsystems.dsp.ramps.controller.utils.RunComposition;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.A;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.Div;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

import net.lingala.zip4j.exception.ZipException;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class CanvasController extends SelectorComposer<Component>implements EventListener<Event> {

    private static final String NO_WIDGETS_TO_SHOW_INTERACTIVITY = "noWidgetsToShowInteractivity";
    private static final String ERROR_OCCURED_WHILE_RUNNING = "errorOccuredWhileRunning";
    private static final String CANNOT_SAVE_DASHBOARD = "cannotSaveDashboard";
    private static final String DASHBOARD_CREATING = "dashboardCreating";
    private static final String NOTIFY_LABEL = "notifyLabel";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CanvasController.class);
    private static final String WIDGETS = "widgets";
    private static final String NO_WIDGETS_TO_SHOW_FILTER = "noWidgetsToShowFilter";

    @Wire
    private Tab canvasTab;
    @Wire
    private Tab widgetConfigTab;
    
    @Wire
    private Tab processTab;

    @Wire
    private Anchorlayout canvasThumbs;

    @Wire
    private Tab interactivityTab;
    
    @Wire
    private Tab golbalFilterTab;
    
    @Wire
    private Tab advancedTab;
    
    @Wire
    private Tabpanel  canvasTabPanel;

    @Wire
    private Label notifyLabel;

    @Wire
    private A moreInfo;
    
    @Wire
    private A abort;

    @Wire
    private Popup viewDashboardSettings;

    @Wire
    private Include configureWidgetInclude;
    @Wire
    private Include interactivityInclude;
    
    @Wire
    private Include globalFilterInclude;
    
    @Wire
    private Include processInclude;
    
    @Wire
    private Include advancedModeInclude;
    
    private DashboardConfig dashboardConfig;
    private Dashboard dashboard;
    private CanvasGrid canvasGrid;

    @Wire
    private Button addWidget;
    @Wire
    private Button addInteractivity;
    @Wire
    private Combobutton dashboardSettings;
    @Wire
    private Button advancedModeHelp;
    @Wire
    private Listcell viewDashboardPersDUD;
    
    @Wire
    private Listcell downloadDashboard;

    @Wire
    private Button saveBtn;

    @Wire
    private Button viewBtn;

    @Wire
    private Button runBtn;

    @Wire
    private Button addInteractivityRamps;
    
    @Wire
    private Button addGlobalFilter;
    
    @Wire 
    private Listcell advancedMode;

    @Wire
    private Timer timer;
    @Wire
    private Timer ciTimer;
    
    @Wire
    private Div runningProgress;
    @Wire
    private Div runningProgressAdvanced;
    

    private boolean showSaveButton;
    private boolean showViewButton;
    private boolean showRunButton;
    private boolean invalidWidgetDatasource = false;
    private boolean blacklistedDatasource = false;

    @WireVariable
    private Desktop desktop;

    private CompositionInstance mostRecentInstance;
    long startTime;

    private int processAttempt;
    User user;
    private String newName;
    
   
    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        LOGGER.debug("composition-->{}", dashboardConfig.getComposition());
        dashboard = dashboardConfig.getDashboard();
        user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        setShowRunButton(!dashboard.isStaticData() && !dashboardConfig.isRAMPSConfig());
        setShowSaveButton(!dashboardConfig.isRAMPSConfig());
        setShowViewButton(!dashboardConfig.isRAMPSConfig() && user.canViewOutput()
                && HipieSingleton.getHipie().getPermissionsManager().userCanView(user.getId(), dashboardConfig.getComposition()));
        return super.doBeforeCompose(page, parent, compInfo);
    }

    public boolean validateDataSources() {
        
        List<String> dataSources = new ArrayList<String>();
        // Get the list of data sources from the composition
        for (PluginOutput dataSource: dashboardConfig.getDatasources()) {
            dataSources.add(dataSource.getLabel());
        }
        
        invalidWidgetDatasource = false;
        // Check to see if any of the widgets have an invalid datasource
        if(dashboardConfig != null && dashboardConfig.getDashboard() != null && dashboardConfig.getDashboard().getWidgets() != null) {
	        for (Widget widget: dashboardConfig.getDashboard().getWidgets()) {
	            if (!ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType()) &&
	                    !ChartType.SCORED_SEARCH.equals(widget.getChartConfiguration().getType()) &&
	                    widget.getDatasourceType() != DATASOURCE.STATIC_DATA && 
	                    widget.getDatasourceType() != DATASOURCE.QUERY){
	                invalidWidgetDatasource = widget.getDatasource().isFileNotExists();
	                if(invalidWidgetDatasource) break;
	            }
	            
	            // If the widget's datasource is not in the composition make the datasource fileExists = false
	            // so an error will show up when the dashboard is edited.
	            if (dashboardConfig.isRAMPSConfig() && !dataSources.contains(widget.getDatasource().getLabel())) {
	                widget.getDatasource().setFileNotExists(true);
	                invalidWidgetDatasource = true;
	            }
	        }
        }
        return invalidWidgetDatasource;
    }
    
    public boolean validateGlobalVariables() {
        boolean hasBlacklistedGlobalVariables = false;
        blacklistedDatasource = false;
        
        List<String> blacklist = ((LogicalFileService) SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles();
        
        if (blacklist == null) {
            Clients.showNotification(Labels.getLabel("databaseError"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                Constants.POSITION_TOP_CENTER, 5000, true);
            blacklistedDatasource = true;
            return true;
        }
        
        List<Element> filteredInputs = RampsUtil.filterSettingsPageInputs(dashboardConfig.getComposition().getInputElements());
        
        for (Element inputElement: filteredInputs) {
            String lfName = null;
            if (inputElement != null && inputElement.getOption(Element.DEFAULT) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams() != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0).getName() != null) {
                lfName = inputElement.getOption(Element.DEFAULT).getParams().get(0).getName().replace("~",  "");
            }
            
            if (inputElement.getType() != null && inputElement.getType().equals(InputElement.TYPE_STRING) && 
                    lfName != null) {
                if (blacklist != null && ((LogicalFileService) SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE)).isFileInBlacklist(lfName, blacklist)) {
                    blacklistedDatasource = true;
                    hasBlacklistedGlobalVariables = true;
                }
            }
        }
        return hasBlacklistedGlobalVariables;
    }
    
    private void checkDashboardForBlacklistedFiles() {
        boolean hasInvalidDataSources = validateDataSources();
        boolean hasInvalidGlobalVariables = validateGlobalVariables();
        if (hasInvalidDataSources && hasInvalidGlobalVariables) {
            Clients.showNotification(Labels.getLabel("invalidWidget").concat("<br><br>")
                    .concat(Labels.getLabel("invalidDatasourceInGlobalVariables")), 
                    Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), 
                    Constants.POSITION_TOP_CENTER, 10000, true);
        } else if (hasInvalidDataSources) {
            Clients.showNotification(Labels.getLabel("invalidWidget"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
        } else if (hasInvalidGlobalVariables) {
            Clients.showNotification(Labels.getLabel("invalidDatasourceInGlobalVariables"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
        }
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LOGGER.debug("dashboardConfig - {}", dashboardConfig);
        
        checkDashboardForBlacklistedFiles();
        
        canvasGrid = (CanvasGrid) canvasThumbs.getAttribute(Constants.WIDGET);
        if (!dashboardConfig.isRAMPSConfig()) {
            // Set view button to visible if there is a completed workunit.
            mostRecentInstance = dashboardConfig.getMostRecentCI();
            
            if (viewBtn != null) {
                viewBtn.setVisible(DashboardUtil.isWorkUnitComplete(mostRecentInstance) || (dashboard.isStaticData() && dashboardConfig.getFlow() == Flow.EDIT));
            }
            
            if (viewBtn != null && (invalidWidgetDatasource || blacklistedDatasource)) {
                viewBtn.setDisabled(true);
            }

            if (saveBtn != null && (dashboardConfig.getFlow() == Flow.EDIT || dashboardConfig.getFlow() == Flow.NEW)) {
                saveBtn.setDisabled(true);
            }
            
            if(runBtn != null && (dashboardConfig.getFlow() == Flow.NEW || invalidWidgetDatasource || blacklistedDatasource)){
                runBtn.setDisabled(true);
            }
            
            // set visibility of buttons
            dashboardSettings.setVisible(true);
            addInteractivity.setVisible(true);
            addGlobalFilter.setVisible(true);
            addInteractivityRamps.setVisible(false);
            if (viewDashboardPersDUD != null && dashboardConfig.getFlow() == Flow.EDIT) {
                viewDashboardPersDUD.setVisible(true);
            }
            
            if (downloadDashboard != null && dashboardConfig.getFlow() == Flow.EDIT) {
                downloadDashboard.setVisible(true);
            }
            
            if (advancedMode != null && !dashboardConfig.getDashboard().getWidgets().isEmpty()) {
                advancedMode.setVisible(true);
            }
            
        }

        processInclude.addEventListener(org.hpccsystems.dsp.Constants.EVENTS.ON_CLOSE_PROCESS_WINDOW, event -> {
            canvasTab.setSelected(true);
            enableAllButtons();
        });
        
        // Add event listeners for tabpanel close
        comp.addEventListener(EVENTS.ON_WIDGET_CONFIG_CLOSE, event -> closeWidgetConfiguration());
        comp.addEventListener(EVENTS.ON_INTERACTIVITY_CONFIG_CLOSE, event -> closeInteractivityConfig());
        comp.addEventListener(EVENTS.ON_ADVANCED_MODE_CLOSE, event -> closeAdvancedMode());
        comp.addEventListener(EVENTS.ON_GLOBAL_FILTER_CLOSE, event -> closeGlobalFilterConfig());
        comp.addEventListener(Constants.EVENTS.ON_SAVE_DASHBOARD_CONFIG, event -> doAfterConfigClose());
        
        // Event listener for saving widget config
        comp.addEventListener(EVENTS.ON_WIDGET_CONFIG_SAVE, event -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("saving widgets...");
            }

            addWidget((Widget) event.getData());
            closeWidgetConfiguration();
            
            BindUtils.postNotifyChange(null, null, canvasGrid, WIDGETS);
        });
        comp.addEventListener(EVENTS.ON_WIDGET_CONFIG_UPDATE, event -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("widget ON_WIDGET_CONFIG_UPDATE - {}", (Widget) event.getData());
                LOGGER.debug("ON_WIDGET_CONFIG_UPDATE - {}", dashboard.getWidgets());
            }
            canvasGrid.setWidgets(dashboardConfig.getDashboard().getWidgets());
            
            closeWidgetConfiguration();
            BindUtils.postNotifyChange(null, null, canvasGrid, WIDGETS);
        });

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("widgets > {}", dashboard.getWidgets());
        }
        canvasGrid.setWidgets(dashboard.getWidgets());
        BindUtils.postNotifyChange(null, null, canvasGrid, WIDGETS);

        canvasThumbs.addEventListener(EVENTS.ON_CONFIGURE_WIDGET, event -> {
            Widget widget = (Widget) event.getData();
            Widget clonedWidget = widget.clone();
            WidgetConfig config = new WidgetConfig();
            config.setIndex(dashboardConfig.getDashboard().getWidgets().indexOf(widget));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Configuring widget--> {}", widget);
            }
            config.setOriginalWidget(widget);
            config.setChartname(widget.getTitle());
            config.setDatasource(widget.getDatasource());
            config.setChartType(widget.getChartConfiguration().getType());
            config.setQueryName(widget.getQueryName());
            config.setDatasourceType(widget.getDatasourceType());
            config.setWidget(clonedWidget);
            configure(config);
            Events.postEvent(Constants.EVENTS.ON_OPEN_WIDGET_CONFIGURATION, this.getSelf().getParent(), null);
            disableAllButtons();
        });

        canvasThumbs.addEventListener(EVENTS.ON_DELETE_WIDGET, event -> deleteWidget(event));

        comp.addEventListener(EVENTS.ON_FINISH_INTERACTIVITY_CONFIG, event -> {
            closeInteractivityConfig();
            BindUtils.postNotifyChange(null, null, canvasGrid, WIDGETS);
        });
        
        comp.addEventListener(EVENTS.ON_SAVE_ADVANCED_MODE, event -> {
        	closeAdvancedMode();
        });
                
        dashboardConfig.setCanvasComponent(this.getSelf());

        timer.stop();
        ciTimer.stop();
        
        if (CompositionUtil.addVizVersion(dashboardConfig.getComposition(), false) && saveBtn != null) {
            saveBtn.setDisabled(false);
        }
        
        // Check to see if composition is in advanced mode
        if (((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).isAdvancedMode(dashboardConfig.getComposition())) {
            LOGGER.debug("Dashboard is in advanced mode.");
            openAdvancedMode();
        }
    }

    private void deleteWidget(Event event) {
        Widget widget = (Widget) event.getData();

        Messagebox.show(Labels.getLabel("deleteThewidget"), Labels.getLabel("deleteTitle"), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                Messagebox.QUESTION, new EventListener<ClickEvent>() {
                    @Override
                    public void onEvent(ClickEvent event) throws Exception {
                        onConfirmWidgetDelete(widget, event);
                    }
                });
    }

    protected void onConfirmWidgetDelete(Widget widget, ClickEvent event) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            if (dashboard.getInteractivities() != null && !dashboard.getInteractivities().isEmpty()) {
                dashboard.deleteWidgetInteractivities(widget);
            }
            
            dashboardConfig.removeGlobalFilters(widget);
            
            dashboardConfig.getDashboard().getWidgets().remove(widget);

            if (!dashboardConfig.isRAMPSConfig() && !widget.canUseNativeName()) {
                dashboardConfig.removeDatasource(widget);
            }
            // Removing the deleting widget's query schema from dashoardconfig,
            // if the query is not used by any other widget
            if (widget.canUseNativeName()) {
                dashboardConfig.getDashboard().removeQuery(widget);
            }            
            canvasGrid.setWidgets(dashboardConfig.getDashboard().getWidgets());
            
            BindUtils.postNotifyChange(null, null, canvasGrid, WIDGETS);

            dashboard.setChanged(true);
            
            // Enabling save button
            if (saveBtn != null) {
                saveBtn.setDisabled(false);
            }
            
            //Disabling run button when the widget list is empty
            if(runBtn != null && dashboardConfig.getDashboard().getWidgets().isEmpty()){
                runBtn.setDisabled(true);
            }
            
            // disable advanced mode when the dashboard is not saved
            if (advancedMode != null && saveBtn != null && !saveBtn.isDisabled()){
                advancedMode.setVisible(false);
            }

            LOGGER.debug("widgets --> {}", dashboard.getWidgets());
        }
    }

    @Override
    public void onEvent(Event arg0) throws Exception {

        if (LOGGER.isDebugEnabled()) {
            ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new CompositionAccess(CompositionAccess.RUN, dashboardConfig.getComposition(), startTime));
        }
        
        LOGGER.debug("Run Dashboard Complete");
        runningProgress.setVisible(false);
        
        //Running completed the dashboard can be closed
        dashboardConfig.getDashboard().setRunning(false);
        
        notifyLabel.setVisible(true);
        
        if ("OnRunCompositionCompleted".equals(arg0.getName())) {
            try {
                mostRecentInstance =  ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(dashboardConfig.getComposition(), true);
            } catch (HipieException e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(Labels.getLabel("unableToGrabWU"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }

           LOGGER.debug("Run complete. Recent instance id {} & status is {}", mostRecentInstance.getWorkunitId(), mostRecentInstance.getWorkunitStatus());

            if (mostRecentInstance.isRunning()) {
                abort.setVisible(true);
                abort.addEventListener(Events.ON_CLICK, event -> abortProcess());
                notifyLabel.setValue("Waiting for workunit to complete");
                notifyLabel.setSclass("notifyLabel-green");
                processAttempt = 0;
                ciTimer.start();
                return;
            }
            
            doAfterProcessComplete();
        } else {
            try {
                //Getting latest instance to check for workunit status, based on it 'View' button will be enabled
                mostRecentInstance = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                            .getmostRecentInstance(dashboardConfig.getComposition(), true);
            } catch (HipieException e) {
                mostRecentInstance = null;
                LOGGER.error(Constants.EXCEPTION, e);
            }
            LOGGER.debug("Run Composition failed");
            LOGGER.debug(" Error occured while running - {}", arg0.getData().toString());

            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(Labels.getLabel(Constants.DATA_TOO_LARGE)).append(Labels.getLabel(Constants.LARGE_DATA_TOO_LARGE));

            if (arg0.getData().toString().contains(Constants.ERROR_MSG_LARGE_DATA)) {
                notifyLabel.setValue(errorMsg.toString());
            } else {
                notifyLabel.setValue(Labels.getLabel(ERROR_OCCURED_WHILE_RUNNING));
            }
            notifyLabel.setSclass("notifyLabel-red");
            moreInfo.setVisible(true);
            if (arg0.getData().toString().contains("SDS: Lock held Lock")) {
                moreInfo.setAttribute(Clients.NOTIFICATION_TYPE_ERROR, Labels.getLabel("deployRoxieFailed"));
            } else {
                moreInfo.setAttribute(Clients.NOTIFICATION_TYPE_ERROR, arg0.getData().toString());
            }
            resetRunState();
        }
    }

    private void abortProcess() {
        try {
            if (mostRecentInstance.isRunning()) {
                mostRecentInstance.abort();
                LOGGER.info("Most Recent instance {} was aborted by user", mostRecentInstance.getWorkunitId());
                resetRunState();

                Clients.showNotification(Labels.getLabel("processAborted"), Clients.NOTIFICATION_TYPE_INFO,
                        CanvasController.this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
                // hide both the labels after notification
                abort.setVisible(false);
                notifyLabel.setVisible(false);
                timer.stop();
                ciTimer.stop();
            } else {
                Clients.showNotification(Labels.getLabel("cannotAborted"), Clients.NOTIFICATION_TYPE_INFO,
                        CanvasController.this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            }
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            Clients.showNotification(Labels.getLabel("abortFailed") + " " + ex.getMessage(),
                    Clients.NOTIFICATION_TYPE_ERROR, CanvasController.this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    private void resetRunState() {

        enableAllButtons();
        
        if (!dashboardConfig.isRAMPSConfig() && viewBtn != null) {
            viewBtn.setVisible(DashboardUtil.isWorkUnitComplete(mostRecentInstance));
        }
       
        canvasTabPanel.setStyle("pointer-events: auto;");
    }

    private void doAfterProcessComplete() {
        if(DashboardUtil.isWorkUnitComplete(mostRecentInstance)){
            notifyLabel.setValue(Labels.getLabel("dashboardCreated"));
            notifyLabel.setSclass("notifyLabel-green");
            timer.start();
            dashboardConfig.setMostRecentCI(mostRecentInstance);
            dashboardConfig.setReloadOutput(true);
            Events.postEvent(Dashboard.EVENTS.ON_VIEW_DASHBOARD_OUTPUTS, dashboardConfig.getViewEditTabbox(), dashboardConfig);
        }else{
            notifyLabel.setValue(Labels.getLabel("processNotComplete"));
            notifyLabel.setSclass("notifyLabel-red");
            timer.start();
        }

        resetRunState();
    }

    @Listen("onTimer = #ciTimer")
    public void tryForRecentCI() {
        try {
            mostRecentInstance = dashboardConfig.getComposition().getMostRecentInstance(
                    ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId());
            LOGGER.info("Most recent instance retrival attemt {},Wuid - {},Status - {}", processAttempt, mostRecentInstance.getWorkunitId(),
                    mostRecentInstance.getWorkunitStatus());
            if (mostRecentInstance.isComplete()  && !mostRecentInstance.getCurrentUsername().endsWith(Constants.SERVICE)) {
                ciTimer.stop();
                abort.setVisible(false);
                doAfterProcessComplete();
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
            ciTimer.stop();
        }

        processAttempt++;
        if (processAttempt > 60) {
            ciTimer.stop();
            Clients.showNotification("Workunit " + mostRecentInstance.getWorkunitId() + " is taking long to complete. Open this dashboard after some time.",
                    Clients.NOTIFICATION_TYPE_WARNING, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
    }

    private void addWidget(Widget widget) {
        List<Widget> widgets = dashboard.getWidgets();
        LOGGER.debug("widgets- {}", widgets);
        if (widgets == null) {
            widgets = new ArrayList<Widget>();
            dashboard.setWidgets(widgets);
        }
        widgets.add(widget);

        canvasGrid.setWidgets(widgets);
    }

    @Listen("onClick = #viewBtn")
    public void viewDashboard() {
        Events.sendEvent(Dashboard.EVENTS.ON_VIEW_DASHBOARD_OUTPUTS, dashboardConfig.getViewEditTabbox(), dashboardConfig);
        RampsUtil.resizeDashboard((String)dashboardConfig.getDashboardTab().getAttribute(Constants.DASHBOARD_HOLDER_UUID));
    }

    @Listen("onClick= #moreInfo")
    public void onClickMoreInfo() {
        Clients.showNotification(moreInfo.getAttribute("error").toString(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        notifyLabel.setVisible(false);
        moreInfo.setVisible(false);
        timer.stop();
    }

    @Listen("onClick = #saveBtn")
    public void saveDashboard(Event event) {
        saveDashboard((Button) event.getTarget());
    }
    
    public boolean saveDashboard(Button btn) {
        boolean isValidComposition = true;
        
        if (dashboardConfig.isStaticData()) {
            dashboardConfig.setReloadOutput(true);
        }
        try {
            if (CollectionUtils.isNotEmpty(dashboard.getWidgets())) {

                generateVisualizationPlugin();

                if (dashboard.isStaticData()) {
                    // Skiping validation for Databomb Dashboards
                    onSave(btn);
                } else {
                    // Validates composition before saving
                    isValidComposition = RampsUtil.validateComposition(dashboardConfig.getComposition(), this.getSelf());
                    if (isValidComposition) {
                        onSave(btn);
                    }
                }
            } else if (!dashboardConfig.isRAMPSConfig()) {
                onDeleteDashboard();
            }
            
        } catch (CompositionServiceException | HipieException | RepoException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_MIDDLE_CENTER, 3000, true);
            
        }
        return isValidComposition;
    }

    @Listen("onClick = #runBtn")
    public void runDashboard(Event event) {
        
        try {
            if(!HipieSingleton.getHipie().getPermissionsManager().userCanRun(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), dashboardConfig.getComposition())){
                Clients.showNotification(Labels.getLabel("donotHavePermissionToRunDashboard") , Clients.NOTIFICATION_TYPE_ERROR,
                        this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
                return;
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("couldntCheckPermissionDashbaord"), Clients.NOTIFICATION_TYPE_ERROR, 
                    this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }
        
        if (CollectionUtils.isEmpty(dashboard.getWidgets())) {
            Clients.showNotification(Labels.getLabel("cannotRunDashboard"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }
        boolean isValidComposition = true;
        
        if (saveBtn != null && !saveBtn.isDisabled()) {
            isValidComposition = saveDashboard((Button)event.getTarget());            
        }else{
            // Validates composition before running
             isValidComposition = RampsUtil.validateComposition(dashboardConfig.getComposition(), this.getSelf());            
        }
        if (!isValidComposition) {
            return;
        }
        try {
            dashboardConfig.getDashboard().getHpccConnection().testConnection();
            runDashboard();
        }catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("runCompFailed") , Clients.NOTIFICATION_TYPE_ERROR,
                    this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("hpccConnectionFailed") + ": " + e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
           
    }

    private void onSave(Button btn) throws CompositionServiceException {
        // updating the dashboard

        if (Constants.Flow.EDIT == dashboardConfig.getFlow()) {
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).updateDashboard(dashboardConfig.getDashboard(), ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), dashboardConfig.getComposition());
          
            LOGGER.debug("dashboard time -->{}", new Date(dashboardConfig.getComposition().getLastModified()));
            dashboardConfig.getComposition().setLastModified(new Date().getTime());
            dashboardConfig.getDashboard().setLastModifiedDate(new Date(dashboardConfig.getComposition().getLastModified()));
            
            //Posting this event to make it reflect the edited Widget name in the Grid/List view of Dashboards
            Events.postEvent(org.hpccsystems.dsp.Constants.EVENTS.ON_CHANGE_LABEL, dashboardConfig.getHomeTabbox(), dashboardConfig.getDashboard());
        } else if (Constants.Flow.NEW == dashboardConfig.getFlow() || Constants.Flow.CLONE == dashboardConfig.getFlow()) {
            Composition savedComposition;
            try {
                savedComposition =  ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).saveNewCompositionOnHIPIE(dashboardConfig.getComposition().getName(), dashboardConfig.getComposition());
            } catch (Exception d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
            dashboardConfig.setComposition(savedComposition);
            dashboardConfig.getDashboard().setLastModifiedDate(new Date(dashboardConfig.getComposition().getLastModified()));
            dashboardConfig.getDashboard().setCanonicalName(savedComposition.getCanonicalName());
            
            if (!dashboardConfig.getDashboard().isStaticData()) {
                try {
                    ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).saveClusterConfig(dashboardConfig.getComposition(), dashboardConfig.getDashboard().getClusterConfig());
                } catch (DatabaseException d) {
                    LOGGER.error(Constants.EXCEPTION, d);
                    Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                    return;
                }
            }
            Events.postEvent(Dashboard.EVENTS.ON_SAVE_DASHBOARD, dashboardConfig.getHomeTabbox(), dashboardConfig);
            dashboardConfig.setFlow(Constants.Flow.EDIT);

            showDashboardButtons();
        }

        doAfterSave(btn);
    }

    private void showDashboardButtons() {
        if (viewDashboardPersDUD != null) {
            viewDashboardPersDUD.setVisible(true);
        }
        
        if (downloadDashboard != null) {
            downloadDashboard.setVisible(true);
        }
        // TODO: move each of these into a function and call that function.
        if (advancedMode != null && !dashboard.getWidgets().isEmpty()) {
            advancedMode.setVisible(true);
        }
    }

    private void doAfterSave(Button btn) {
        if (dashboardConfig.getDashboard().getQueries() != null) {
            try {
                CompositionUtil.cloneQuerySchema(dashboardConfig.getDashboard());
            } catch (CloneNotSupportedException e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }

        // Disable save button after clicking it
        dashboardConfig.getDashboard().setChanged(false);
        
        saveBtn.setDisabled(true);

        if (!"runBtn".equals(btn.getId())) {
            Clients.showNotification("Save successful", "info", this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
        if (dashboard.isStaticData()) {
            viewBtn.setVisible(true);
        }
        if (advancedMode != null && !dashboard.getWidgets().isEmpty()) {
            advancedMode.setVisible(true);
        }
    }

    private void onDeleteDashboard() {
        if (!Constants.Flow.NEW.equals(dashboardConfig.getFlow())) {
            Messagebox.show("No widgets to save.Do you want to delete the Dashboard?", "Delete Dashboard?",
                    new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION, this::confirmDelete);
        } else {
            Clients.showNotification(Labels.getLabel(CANNOT_SAVE_DASHBOARD), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    private void confirmDelete(ClickEvent event) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            Events.postEvent(org.hpccsystems.dsp.Constants.EVENTS.ON_DELETE_COMPOSITION, dashboardConfig.getAnchorlayout(), dashboard);
        } else {
            Clients.showNotification(Labels.getLabel(CANNOT_SAVE_DASHBOARD), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    private void generateVisualizationPlugin() throws RepoException, HipieException {
        List<Widget> scoredSearchWidgets = dashboard.getScoredSearchWidgets();
        String userId=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
        if (scoredSearchWidgets != null && !scoredSearchWidgets.isEmpty()) {
            CompositionUtil.generateScoredSearchVisualizationPlugin(dashboardConfig.getComposition(), userId,
                    dashboard);
        } else {
            CompositionUtil.generateVisualizationPlugin(dashboardConfig.getComposition(), userId, dashboard, false);
        }
    }

    public void runDashboard() throws HipieException {
        timer.stop();
        moreInfo.setVisible(false);
        desktop.enableServerPush(true);
        Composition cmp = dashboardConfig.getComposition();
        HPCCConnection conn = dashboardConfig.getDashboard().getHpccConnection();
        if (cmp.isServiceVizOnly() && !conn.getHthorclusters().isEmpty()) {
            conn.setThorCluster(conn.getHthorclusters().get(0));
        }

        startTime = Instant.now().toEpochMilli();
        String userId=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
        DSPExecutorHolder.getExecutor().execute(new RunComposition(cmp, conn, userId, desktop, this));
        dashboardConfig.getDashboard().setRunning(true);
        canvasTabPanel.setStyle("pointer-events: none;");
        disableAllButtons();
        
        runningProgress.setVisible(true);
    }

    @Listen("onClick = #addWidget")
    public void configureWidget() {
        WidgetConfig config = new WidgetConfig(true);
        if (dashboard.isStaticData()) {
            config.setDatasourceType(DATASOURCE.STATIC_DATA);
        }
        
        configure(config);
        Events.postEvent(Constants.EVENTS.ON_OPEN_WIDGET_CONFIGURATION, this.getSelf().getParent(), null);
        disableAllButtons();
    }

    @Listen("onClick = #addInteractivity")
    public void configureInteractivity() {
        viewDashboardSettings.close();
        if (CollectionUtils.isEmpty(dashboard.getWidgets())) {
            if (dashboardConfig.isRAMPSConfig()) {
                Clients.showNotification(Labels.getLabel(NO_WIDGETS_TO_SHOW_INTERACTIVITY), Clients.NOTIFICATION_TYPE_ERROR, dashboardSettings.getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                return;
            } else {
                Clients.showNotification(Labels.getLabel(NO_WIDGETS_TO_SHOW_INTERACTIVITY), Clients.NOTIFICATION_TYPE_ERROR, addInteractivityRamps.getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                return;
            }

        }
        Events.postEvent(Constants.EVENTS.ON_OPEN_INTERACTIVITY, this.getSelf().getParent(), null);
        interactivityTab.setSelected(true);
        interactivityInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        interactivityInclude.setDynamicProperty("dashboard", dashboard);
        interactivityInclude.setDynamicProperty(Dashboard.PARENT, getSelf());
        interactivityInclude.setSrc("dashboard/design/configureInteractivity.zul");
        disableAllButtons();

    }
    
    @Listen("onClick = #addGlobalFilter")
    public void configureGlobalFilter() {
        viewDashboardSettings.close();
        if (CollectionUtils.isEmpty(dashboard.getWidgets())) {
            if (dashboardConfig.isRAMPSConfig()) {
                Clients.showNotification(Labels.getLabel(NO_WIDGETS_TO_SHOW_FILTER), Clients.NOTIFICATION_TYPE_ERROR, dashboardSettings.getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                return;
            } else {
                Clients.showNotification(Labels.getLabel(NO_WIDGETS_TO_SHOW_FILTER), Clients.NOTIFICATION_TYPE_ERROR, addInteractivityRamps.getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                return;
            }

        }
        
        golbalFilterTab.setSelected(true);
        globalFilterInclude.setDynamicProperty("dashboard", dashboard);
        globalFilterInclude.setDynamicProperty(Dashboard.PARENT, getSelf());
        globalFilterInclude.setSrc("dashboard/design/global_filter.zul");
        disableAllButtons();
    }

    @Listen("onClick = #addInteractivityRamps")
    public void configureInteractivityRAMPS() {
        configureInteractivity();
    }
    
    @Listen("onClick = #viewWorkunits")
    public void viewProcesses() {
        viewDashboardSettings.close();
        disableAllButtons();
        processTab.setSelected(true);
        processInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        processInclude.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.USER_DASHBOARDS);
       if(processInclude.getSrc() == null) {
           processInclude.setSrc("/dashboard/view_dashboard_process.zul");
       }
    }
    
    @Listen("onClick = #advancedMode")
    public void enterAdvancedMode() {
    	viewDashboardSettings.close();
    	
    	// Check to make sure the user is allowed to use advanced mode
    	User user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
    	
    	if (!user.isAllowedAdvancedMode()) {
    	    Clients.showNotification(Labels.getLabel("notAllowedAdvancedMode"), Clients.NOTIFICATION_TYPE_ERROR, null,
    	            Constants.POSITION_TOP_CENTER, 5000);
    	    
    	    return;
    	}
    	
        disableAllButtons();

    	// Prompt the user to back up existing dashboard
        Messagebox.show(Labels.getLabel("backupDashboard"), Labels.getLabel("backupPrompt"),
                new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO, Messagebox.Button.CANCEL}, 
                Messagebox.QUESTION, this::dashboardBackupPrompt);
        
        enableAllButtons();
    }
    
    @Listen("onClick = #advancedModeHelp")
    public void showAdvancedModeHelp() {
        final Window dialog = (Window) Executions.createComponents("/dashboard/design/advancedModeHelp.zul", 
                canvasTabPanel, null);
        dialog.doModal();
    }
    
    private void dashboardBackupPrompt(ClickEvent event) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            // clone dashboard then open advanced mode
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Backup dashboard before continuing.");
            }
            newDashboardNamePrompt();
        } else if (Messagebox.Button.NO.equals(event.getButton())) {
            openAdvancedMode();
        }
    }
    
    /**
     * Opens a dialog and asks the user to enter a name for the backup
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void newDashboardNamePrompt() {
        final Window dialog = (Window) Executions.createComponents("/dashboard/design/promptDashboardName.zul", 
                canvasTabPanel, null);
        dialog.doModal();
        
        Button saveClone = (Button) dialog.getFellow("saveClone");
        
        ((Textbox) dialog.getFellow("clonedNameTextbox")).setValue(dashboardConfig.getDashboard().getLabel() + 
                Dashboard.BACKUP_SUFFIX);;
        
        saveClone.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                Textbox clonedNameTextbox = (Textbox) dialog.getFellow("clonedNameTextbox");
                newName = clonedNameTextbox.getValue();
                
                if (!validateLabel(newName, clonedNameTextbox)) {
                    return;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Creating backup of dashboard: {}. New dashboard name: {}", dashboardConfig, newName);
                }
                                
                // Create a new dashboard object by cloning the existing one
                DashboardConfig newConfig = DashboardUtil.cloneDashboard(dashboardConfig, newName, user, false);
                
                if (newConfig == null) {
                  Clients.showNotification(Labels.getLabel("unableToCloneDashboard"), Clients.NOTIFICATION_TYPE_ERROR, null,
                          Constants.POSITION_END_AFTER, 3000);
                    return;
                }
                // Save the new dashboard
                if (!DashboardUtil.saveDashboard(newConfig)) {
                    Clients.showNotification(Labels.getLabel("unableToCloneDashboard"), Clients.NOTIFICATION_TYPE_ERROR, null,
                            Constants.POSITION_END_AFTER, 3000);
                      return;
                }
                
                openAdvancedMode();
                dialog.detach();
            }
        });
        
        Button cancelAdvancedMode = (Button) dialog.getFellow("cancel");
        cancelAdvancedMode.addEventListener("onClick", new EventListener() {

            @Override
            public void onEvent(Event arg0) throws Exception {
                dialog.detach();
            }
            
        });
        
    }
    
    private boolean validateLabel(String newName, Textbox clonedNameTextbox) {
       Boolean isValid = true;
        
        // no empty names
        if (StringUtils.isEmpty(newName)) {
            
            Clients.showNotification(Labels.getLabel("dashboardName"), Clients.NOTIFICATION_TYPE_ERROR, clonedNameTextbox,
            Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        // name must not start with a number
        if (Character.isDigit(newName.charAt(0))) {
            
            Clients.showNotification(Labels.getLabel("dashNameAlphabetOnly"), Clients.NOTIFICATION_TYPE_ERROR, clonedNameTextbox,
            Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        
        // Check the name for conflicts
        try {
            if (((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getDashboards(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()).stream()
                    .filter(comp -> comp.getLabel().equals(newName)).count() > 0) {
                isValid = false;
                Clients.showNotification(Labels.getLabel("dashboardAlreadyExists1") + newName + 
                        Labels.getLabel("dashboardAlreadyExists2"), Clients.NOTIFICATION_TYPE_ERROR, clonedNameTextbox,
                        Constants.POSITION_END_AFTER, 3000);
            }
            
        } catch (Exception d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            isValid = false;
        }
        return isValid;
    }

    private void openAdvancedMode() {
      Events.postEvent(Constants.EVENTS.ON_OPEN_ADVANCED_MODE, this.getSelf().getParent(), null);
      advancedTab.setSelected(true);
      
      advancedModeInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
      advancedModeInclude.setDynamicProperty("dashboard", dashboard);
      advancedModeInclude.setDynamicProperty(Dashboard.PARENT, getSelf());
      advancedModeInclude.setSrc("/dashboard/design/advancedMode.zul");
      
      hideShowAdvancedModeButtons();
    }
    
    public void configure(WidgetConfig config) {
        config.setDashboardCanvas(getSelf());
        configureWidgetInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, config);
        configureWidgetInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        configureWidgetInclude.setSrc("dashboard/design/widget/configure_widget.zul");
        widgetConfigTab.setSelected(true);
    }

    private void closeWidgetConfiguration() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Closing Configuration page  ");
        }
        doAfterTabClose(configureWidgetInclude);
    }

    private void closeInteractivityConfig() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Closing Configuration page");
        }
        doAfterTabClose(interactivityInclude);
    }
    
    private void closeAdvancedMode() {
    	if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Closing advanced mode page");
        }
    	doAfterTabClose(advancedModeInclude);
    }
    
    private void closeGlobalFilterConfig() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Closing Global Filter page");
        }
        canvasGrid.setWidgets(dashboardConfig.getDashboard().getWidgets());
        doAfterTabClose(globalFilterInclude);
    }
    
    private void doAfterTabClose(Include include) {
        try {
            validateDataSources();
            validateGlobalVariables();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
        include.clearDynamicProperties();
        include.setSrc(null);
        canvasTab.setSelected(true);
        enableAllButtons();
        Events.postEvent(org.hpccsystems.dsp.Constants.ON_CLICK_CLOSE_WIDGET_CONFIGURATION, this.getSelf().getParent(), null);
    }
    
    private void doAfterConfigClose() {
        validateDataSources();
        validateGlobalVariables();
        enableAllButtons();
        Events.postEvent(Constants.EVENTS.ON_CHANGE_LABEL, this.getSelf().getParent().getParent(), dashboardConfig.getDashboard());
    }
    
    /**
     * Hides or shows the buttons for advanced mode
     */
    private void hideShowAdvancedModeButtons() {
    	if (addWidget != null) {
            addWidget.setVisible(false);
        }
        if (saveBtn != null) {
            saveBtn.setVisible(false);
        }
        if (addInteractivity != null) {
            addInteractivity.setVisible(false);
        }
        if (addGlobalFilter != null) {
            addGlobalFilter.setVisible(false);
        }
        if (advancedMode != null) {
        	advancedMode.setVisible(false);
        }
        
        if (advancedModeHelp != null) {
            advancedModeHelp.setVisible(true);
        }
    }

    /**
     * Disables all buttons (Run,Save,Widget interactivity, View and Add widget)
     */
    private void disableAllButtons() {
        if (runBtn != null) {
            runBtn.setDisabled(true);
        }
        if (addWidget != null) {
            addWidget.setDisabled(true);
        }
        if (saveBtn != null) {
            saveBtn.setDisabled(true);
        }
        if (viewBtn != null) {
            viewBtn.setDisabled(true);
        }
        if (dashboardSettings != null) {
            dashboardSettings.setDisabled(true);
        }
        
        if (addInteractivity != null) {
            addInteractivity.setDisabled(true);
        }
        
        if (addGlobalFilter != null) {
            addGlobalFilter.setDisabled(true);
        }
        
        if (advancedMode != null) {
        	advancedMode.setVisible(false);
        }
    }

    /**
     * Enables all buttons (Run,Save,Widget interactivity, View and Add widget)
     */
    private void enableAllButtons() {
        
        if (runBtn != null && !dashboardConfig.getDashboard().getWidgets().isEmpty() && 
                (!invalidWidgetDatasource && !blacklistedDatasource)) {
            runBtn.setDisabled(false);
        }
        if (addWidget != null) {
            addWidget.setDisabled(false);
        }
        if (canEnableSaveBtn() && (!invalidWidgetDatasource && !blacklistedDatasource)) {
            saveBtn.setDisabled(false);
        }
        if (viewBtn != null && (!invalidWidgetDatasource && !blacklistedDatasource)) {
            viewBtn.setDisabled(false);
        }

        if (dashboardSettings != null) {
            dashboardSettings.setDisabled(false);
        }
        
        if (addInteractivity != null) {
            addInteractivity.setDisabled(false);
        }
        
        if (addGlobalFilter != null) {
            addGlobalFilter.setDisabled(false);
        }
        
        if (advancedMode != null && dashboardConfig.getDashboard().getWidgets() != null && !dashboardConfig.getDashboard().getWidgets().isEmpty() && 
                saveBtn != null && saveBtn.isDisabled()) {
        	advancedMode.setVisible(true);
        }
    }
    
    private boolean canEnableSaveBtn() {
        return (saveBtn != null) && (dashboardConfig.getFlow() == Flow.NEW || dashboard.isChanged() || dashboardConfig.getFlow() == Flow.CLONE);
    }

    @Listen("onTimer = #timer")
    public void hideStatusLabel() {
        timer.stop();
        notifyLabel.setVisible(false);
        notifyLabel.setValue(Labels.getLabel(DASHBOARD_CREATING));
        notifyLabel.setSclass(NOTIFY_LABEL);
    }

    @Listen("onClick = #dashboardSettings")
    public void createSettingsModal(Event event) {
        if (CollectionUtils.isEmpty(dashboard.getWidgets())) {
            Clients.showNotification(Labels.getLabel("configureDashboardFirst"), Clients.NOTIFICATION_TYPE_ERROR, addInteractivityRamps.getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 3000, true);
            return;
        }
        if (getSelf().hasFellow("configContainer")) {
            LOGGER.error(
                "Cannot create more than one component with the same id, must be an error due to user action(Clicking same button more than once)");
             return;
        }
        Map<String, Object> dashboardData = new HashMap<String, Object>();
        dashboardData.put(Constants.DASHBOARD_CONFIG, dashboardConfig);
        Window window = (Window) Executions.createComponents("ramps/project/configuration.zul", getSelf(), dashboardData);
        window.doModal();
    }

    @Listen("onClick = #viewDashboardPersDUD")
    public void onViewDashboarPersdDUD() {

        viewDashboardSettings.close();
        try {
            ContractInstance ins = dashboardConfig.getComposition().getContractInstanceByName(dashboardConfig.getComposition().getName() + Dashboard.CONTRACT_IDENTIFIER);
            if (ins == null) {
                ins = dashboardConfig.getComposition()
                        .getContractInstanceByName(dashboardConfig.getComposition().getName() + Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER);
            }
            if (ins != null) {
                Map<String, Object> instanceMap = new HashMap<String, Object>();
                instanceMap.put(Constants.CONTRACT_INSTANCE, ins);
                Window window = (Window) Executions.createComponents("ramps/project/view_dud.zul", null, instanceMap);
                window.doModal();
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }

    }
    
    @Listen("onClick = #downloadDashboard")
    public void downloadDashboardZip() {
        try {
            Composition cmp = dashboardConfig.getComposition();
            ContractInstance instance = CompositionUtil.getVisualizationContractInstance(cmp);
            CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
            CompositionInstance recentInstance = compositionService.getmostRecentInstance(cmp, false);
            LOGGER.debug("most recent instance ------------>{}", recentInstance);
            String ddl = null;
            if (recentInstance != null) {
                Process process = new Process(recentInstance);
                List<String> ddls =  process.getDDLs(false);
                ddl = ddls.isEmpty() ? null : ddls.iterator().next();
                LOGGER.debug("ddl name------------>{}", ddl);
            }
            List<Dermatology> layouts = null;
            if (ddl != null && user.isGlobalAdmin()) {
//                layouts = compositionService.getLayouts(cmp.getId(),cmp.getVersion(),ddl);
            }else if(ddl != null && !user.isGlobalAdmin()){
                layouts = new ArrayList<Dermatology>();
//                layouts.addAll(compositionService.getLayouts(cmp.getId(),cmp.getVersion(),ddl,Constants.GENERIC_USER));
//                layouts.addAll(compositionService.getLayouts(cmp.getId(),cmp.getVersion(),ddl,user.getId()));
            }
//            CompositionUtil.downloadCompositionAndContractFile(cmp, instance, ddl, layouts, recentInstance, user.getId());
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("errorOccuredWhileDownloadingDashboard"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        } 

    }

    public boolean isShowViewButton() {
        return showViewButton;
    }

    public void setShowViewButton(boolean showViewButton) {
        this.showViewButton = showViewButton;
    }

    public boolean isShowSaveButton() {
        return showSaveButton;
    }

    public void setShowSaveButton(boolean showSaveButton) {
        this.showSaveButton = showSaveButton;
    }

    public boolean isShowRunButton() {
        return showRunButton;
    }

    public void setShowRunButton(boolean showRunButton) {
        this.showRunButton = showRunButton;
    }

}
