package org.hpccsystems.dsp.ramps.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.ramps.controller.entity.FileBrowserData;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Include;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ConfigureDashboardController extends SelectorComposer<Component> {
    private static final String ERROR_OCCURED_WHILE_SAVING = "errorOccuredWhileSaving";
    private static final String ERROR_OCCURED_WHILE_OPEN = "errorOccuredWhileOpen";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureDashboardController.class);

    @Wire
    private Include include;
    @Wire
    private Button cancelDashboardSettings;
    @Wire
    private Button saveDashboardSettings;
    
    private DashboardConfig dashboardConfig;

    private static final String ON_LOADING = "onLoading";
    private static final String ON_SAVE = "onSaving";
    
    private EventListener<Event> loadingListener = event -> loading();
    private EventListener<Event> saveListener = event -> onSaveListener();

    private void onSaveListener() {
        Dashboard dashboard = dashboardConfig.getDashboard();
        Composition comosition = dashboardConfig.getComposition();
        
        //check whether dashboard widgets are valid
        if(dashboardConfig.getDashboard() != null){
            boolean hasInvalidWidget = validateDashboardWidget(dashboardConfig.getDashboard());
            if(hasInvalidWidget){
                Clients.clearBusy(ConfigureDashboardController.this.getSelf());
                return;
            }
        } 

        try {
            LOGGER.debug("container -->{}", dashboardConfig.getRampsContainer());
            generateVisualizationPlugin(comosition, dashboard);
        } catch (Exception e) {
            Clients.clearBusy(ConfigureDashboardController.this.getSelf());
            Clients.showNotification(Labels.getLabel(ERROR_OCCURED_WHILE_SAVING), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_MIDDLE_CENTER, 3000);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        }
    }

    private void loading() {
        Map<String, ContractInstance> contractInstances = dashboardConfig.getComposition().getContractInstances();
        try {
            // Constructing Dashboard Config object
            DashboardUtil.constructDashboardConfigFromContractInstance(dashboardConfig, contractInstances);
            
            includeDashboard(contractInstances);
            Clients.clearBusy(ConfigureDashboardController.this.getSelf());
        } catch (CloneNotSupportedException e) {
            Clients.showNotification(Labels.getLabel(ERROR_OCCURED_WHILE_OPEN), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_MIDDLE_CENTER, 3000);
            LOGGER.error("Unable to clone Query schema-->",e);
            return;
        
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);

            Clients.clearBusy(ConfigureDashboardController.this.getSelf());
            Messagebox.show(Labels.getLabel("stillDelete"), Labels.getLabel("deleteTitle"), new Messagebox.Button[] { Messagebox.Button.YES,
                    Messagebox.Button.NO }, Messagebox.QUESTION, new SerializableEventListener<ClickEvent>() {
               
                        private static final long serialVersionUID = 1L;

                @Override
                public void onEvent(ClickEvent event) throws Exception {
                    if (Messagebox.Button.YES.equals(event.getButton())) {
                        Events.sendEvent(Constants.EVENTS.DELETE_DASHBOARD_DUD_AND_SAVE_COMP, dashboardConfig.getRampsContainer(), dashboardConfig);
                        includeDashboard(contractInstances);
                    } else {
                        cancel();
                    }
                }
            });
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboardConfig-->{}", dashboardConfig.getDashboard().getClusterConfig());
            LOGGER.debug("composition-->{}", dashboardConfig.getComposition());
        }
        comp.addEventListener(ON_LOADING, loadingListener);
        comp.addEventListener(ON_SAVE, saveListener);
        Events.echoEvent(ON_LOADING, comp, null);
        
        include.addEventListener(EVENTS.ON_OPEN_WIDGET_CONFIGURATION,(SerializableEventListener<? extends Event>) event -> {
            saveDashboardSettings.setDisabled(true);
            cancelDashboardSettings.setDisabled(true);
            ((Window) this.getSelf()).setClosable(false);
        });
        include.addEventListener(EVENTS.ON_OPEN_INTERACTIVITY, (SerializableEventListener<? extends Event>)event -> {
            saveDashboardSettings.setDisabled(true);
            cancelDashboardSettings.setDisabled(true);
            ((Window) this.getSelf()).setClosable(false);
        });
        
        include.addEventListener(EVENTS.ON_OPEN_ADVANCED_MODE, (SerializableEventListener<? extends Event>)event -> {
        	saveDashboardSettings.setDisabled(true);
        	cancelDashboardSettings.setDisabled(true);
        	((Window) this.getSelf()).setClosable(false);
        });

        include.addEventListener(Constants.ON_CLICK_CLOSE_WIDGET_CONFIGURATION,(SerializableEventListener<? extends Event>) event -> {
            saveDashboardSettings.setDisabled(false);
            cancelDashboardSettings.setDisabled(false);
            ((Window) this.getSelf()).setClosable(true);
        });

    }

    @Listen("onClose = #dashboardContainer")
    public void hideDashboardConfiguration() {
        Include dashboardConfigInclude = (Include) getSelf().getParent();
        dashboardConfigInclude.clearDynamicProperties();
        dashboardConfigInclude.setSrc(null);
        Events.postEvent(EVENTS.ON_RETURN_TO_EDIT, dashboardConfig.getRampsContainer(), null);
    }

    @Listen("onClick = #cancelDashboardSettings")
    public void cancel() {
        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

    @Listen("onClick = #saveDashboardSettings")
    public void saveDashboard() {
        try {
            Dashboard dashboard = dashboardConfig.getDashboard();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("widgets --> {}", dashboard.getWidgets());
            }
            if (CollectionUtils.isEmpty(dashboard.getWidgets())) {
                Messagebox.show(Labels.getLabel("stillDeleteWidget"), Labels.getLabel("noWidget"), new Messagebox.Button[] { Messagebox.Button.YES,
                        Messagebox.Button.NO }, Messagebox.QUESTION, new SerializableEventListener<ClickEvent>() {
                  
                            private static final long serialVersionUID = 1L;

                    @Override
                    public void onEvent(ClickEvent event) throws Exception {
                        if (Messagebox.Button.YES.equals(event.getButton())) {
                            Events.sendEvent(Constants.EVENTS.DELETE_DASHBOARD_DUD_AND_SAVE_COMP, dashboardConfig.getRampsContainer(), dashboardConfig);
                            cancel();
                        } else {
                            return;
                        }
                    }
                });
            } else {
                Clients.showBusy(ConfigureDashboardController.this.getSelf(), "saving..");
                Events.echoEvent(ON_SAVE, ConfigureDashboardController.this.getSelf(), null);
            }

        } catch (Exception e) {
            Clients.showNotification(Labels.getLabel(ERROR_OCCURED_WHILE_SAVING), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_MIDDLE_CENTER, 3000);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        }
    }

    private void generateVisualizationPlugin(Composition comosition, Dashboard dashboard) {
        List<Widget> scoredSearchWidgets = dashboard.getScoredSearchWidgets();
        try {
            String userID=((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
            if (scoredSearchWidgets != null && !scoredSearchWidgets.isEmpty()) {
                CompositionUtil.generateScoredSearchVisualizationPlugin(comosition,userID ,dashboard);
            } else {
                CompositionUtil.generateVisualizationPlugin(comosition, userID, dashboard, false);
            }
            Clients.clearBusy(ConfigureDashboardController.this.getSelf());
            
            Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
            
            FileBrowserData data = RampsUtil.getFileBrowserData(Constants.ACTION.SAVE);
            data.setNotifyUser(false);
            Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, dashboardConfig.getData().getHtmlHolder(), data);
            
        } catch (Exception e) {
            Clients.clearBusy(ConfigureDashboardController.this.getSelf());
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_MIDDLE_CENTER, 3000);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        }

    }
    private boolean validateDashboardWidget(Dashboard dashboard) {
        boolean hasInvalidWidget = dashboard.getNonGlobalFilterWidget().stream()
                .filter(widget -> !widget.isValid()).findAny().isPresent();
        if(hasInvalidWidget){
            Clients.showNotification(Labels.getLabel("invalidWidget"),
                    Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
        }
        
        return hasInvalidWidget;
    }

    private void includeDashboard(Map<String, ContractInstance> contractInstances) {
        /*
         * While extracting PluginOutputs from available list of contractinstances, 
         * Plugins outputs that are already used for Visualization are replaced with instances from Widgets
         */
        
        //Getting used Plugin outputs
        Map<PluginOutput, PluginOutput> usedOutputs = new HashMap<PluginOutput, PluginOutput>();
        if(dashboardConfig.getDashboard().getNonGlobalFilterWidget() != null) {
            dashboardConfig.getDashboard().getNonGlobalFilterWidget()
                .forEach(widget -> usedOutputs.put(widget.getDatasource(), widget.getDatasource()));
        }
        
        List<PluginOutput> pluginOutputs = new ArrayList<PluginOutput>();
        PluginOutput pluginOutPut = null;
        
        //Creating Pluginoutputs list
        for (Entry<String, ContractInstance> entry : contractInstances.entrySet()) {
            extractPluginOutputs(entry.getValue(), usedOutputs, pluginOutputs);
        }
        
        ContractInstance visualCI = CompositionUtil.getVisualizationContractInstance(dashboardConfig.getComposition());
        for (PluginOutput output : pluginOutputs) {
            if(output.getContractInstance().equals(visualCI)){
                pluginOutPut = output;
            }
        }
        
        if(pluginOutPut != null){
            pluginOutputs.remove(pluginOutPut);
        }
        
        dashboardConfig.setdatasources(pluginOutputs);
        include.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        include.setSrc("dashboard/design/dashboard.zul");
    }

    private void extractPluginOutputs(ContractInstance contractinstance, Map<PluginOutput, PluginOutput> usedOutputs, List<PluginOutput> pluginOutputs) {
        for (Element element : contractinstance.getParent().getOutputElements(Element.TYPE_DATASET, false)) {
            PluginOutput output = new PluginOutput(contractinstance, element);

            PluginOutput usedOutput = usedOutputs.get(output);
            
            if(usedOutput != null) {
                pluginOutputs.add(usedOutput);
            } else {
                pluginOutputs.add(output);
            }
        }
        
    }
}
