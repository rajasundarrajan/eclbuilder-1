package org.hpccsystems.dsp.ramps.controller;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.log.DataAccess;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.service.DBLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.A;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Window;

import com.jayway.jsonpath.JsonPath;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ProcessInfoController extends SelectorComposer<Window> {

    private static final String DDL_INVALID = "ddlInvalid";
    private static final String BLANK = "_blank";
    private static final String RESIZE_GRAPH = "resizeGraph()";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessInfoController.class);

    private static final String FAILED = "failed";
    private static final String COMPLETED = "completed";

    private static final String IS_LOADED = "isLoaded";
    private static final String PLUGIN_ID = "pluginID";

    private Process process;
    private Project project;

    @Wire
    private Tabpanel outputsPanel;

    @Wire
    private Tabbox sideEffects;
    @Wire
    private Tabbox outputs;

    @Wire
    private Tabpanel sideeffectsPanel;

    @Wire
    private Tab outputTab;
    @Wire
    private A wuid;
    @Wire
    private Label status;
    @Wire
    private Label lastRunDate;
    @Wire
    private A clusterURL;

    @Wire
    private Hbox projectRunningHbox;
    @Wire
    private Label statusLabel;
    @Wire
    private Tab inputTab;
    @Wire
    private Tab sideEffectTab;
    @Wire
    private Include previewHolder;

    private Composition composition;
    private Map<String, HashMap<String, String>> sideEffectDDLs = null;
    private List<String> outputDDLs;
    private List<Plugin> plugins;

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        process = (Process) Executions.getCurrent().getArg().get(Constants.PROCESS);
        project = (Project) Executions.getCurrent().getArg().get(Constants.PROJECT);
        
        try {
            sideEffectDDLs = process.getCompositionInstance().getComposition().getVisualizationDDLs(null, true);
            outputDDLs = process.getOutputDDLs();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        
        composition = process.getCompositionInstance().getComposition();
        plugins = HIPIEUtil.getOrderedPlugins(composition);

        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Window window) throws Exception {
        super.doAfterCompose(window);
        wuid.setLabel(process.getId());
        wuid.setTarget(BLANK);
        wuid.setHref(process.getWorkunitURL());

        status.setValue(StringUtils.capitalize(process.getStatus()));
        lastRunDate.setValue(process.getLastRunDateAsString());
        clusterURL.setTarget(BLANK);
        String espUrl = process.getCompositionInstance().getHPCCConnection().getESPUrl();
        clusterURL.setLabel(espUrl);
        clusterURL.setHref(espUrl);

        window.setTitle(process.getProjectName() + " - " + process.getId());
        LOGGER.debug("process.getStatus() - {}", process.getStatus());

        createSideeffects();
        createOutputs();
        
        // The process is still running,so activating timer
        if (!COMPLETED.equals(process.getStatus()) && !FAILED.equals(process.getStatus())) {
            final Timer timer = new Timer(3000);
            timer.setRepeats(true);
            timer.setParent(this.getSelf());
            timer.addEventListener(Events.ON_TIMER, new SerializableEventListener<Event>() {

                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                @Override
                public void onEvent(Event event) throws Exception {
                    updateStatus(timer);
                }
            });
            projectRunningHbox.setVisible(true);
            timer.start();
        }
        
        statusLabel.setValue(generateStatusLabel(process.getStatus()));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Completed doAfterCompose");
        }

        window.addEventListener(Events.ON_MAXIMIZE, (SerializableEventListener<? extends Event>)event -> Clients.evalJavaScript(RESIZE_GRAPH));

        hideEmptyTabs();
    }

    /**
     * Hides empty tabs and selects the first visible tab
     */
    private void hideEmptyTabs() {
        if (!outputDDLs.isEmpty()) {
            outputTab.setSelected(true);
            Events.postEvent(Events.ON_SELECT, outputTab, null);
            return;
        } else {
            outputTab.setVisible(false);
            outputTab.getLinkedPanel().setVisible(false);
        }

        if (!sideEffectDDLs.isEmpty()) {
            sideEffectTab.setSelected(true);
            Events.postEvent(Events.ON_SELECT, sideEffectTab, null);
            return;
        } else {
            sideEffectTab.setVisible(false);
            sideEffectTab.getLinkedPanel().setVisible(false);
        }

        inputTab.setSelected(true);
        Events.postEvent(Events.ON_SELECT, inputTab, null);
    }

    private void createSideeffects() {
        if (sideEffectDDLs != null && !sideEffectDDLs.isEmpty()) {
            if (process.isDDLValid()) {
                for (Plugin plugin : plugins) {
                    if (sideEffectDDLs.get(plugin.getInstanceId()) != null) {
                        plugin.setSideEffectDDLs(sideEffectDDLs.get(plugin.getInstanceId()).keySet());
                        for (String visualizationDDL : plugin.getSideEffectDDLs()) {
                            // Creating tabs for Side effects tab
                            Tab tab = new Tab(plugin.getLabel());
                            tab.setAttribute(PLUGIN_ID, plugin.getInstanceId());
                            tab.setAttribute(Constants.VISUALIZATION_DDL, visualizationDDL);
                            Tabpanel tabpanel = new Tabpanel();
                            tabpanel.setVflex("1");
                            tabpanel.setStyle(Constants.OVERFLOW_AUTO);
                            sideEffects.getTabs().appendChild(tab);
                            sideEffects.getTabpanels().appendChild(tabpanel);
                        }
                    }
                }
                sideEffectTab.setAttribute(IS_LOADED, true);
            } else {
                Label label = createInvalidLabel();
                sideEffects.getParent().appendChild(label);
                sideEffects.setVisible(false);
            }
        } else {
            sideeffectsPanel.getLinkedTab().setVisible(false);
            sideeffectsPanel.setVisible(false);
        }

    }

    private void createOutputs() {
        if (!outputDDLs.isEmpty()) {
            if (process.isDDLValid()) {
                outputDDLs.forEach(outputDDL -> {
                    Tab tab = new Tab(extractDashboardLabel(outputDDL));
                    tab.setAttribute(Constants.VISUALIZATION_DDL, outputDDL);

                    Tabpanel tabpanel = new Tabpanel();
                    tabpanel.setVflex("1");
                    tabpanel.setStyle(Constants.OVERFLOW_AUTO);

                    outputs.getTabs().appendChild(tab);
                    outputs.getTabpanels().appendChild(tabpanel);
                });
                outputTab.setAttribute(IS_LOADED, true);
            } else {
                outputs.getParent().appendChild(createInvalidLabel());
                outputs.setVisible(false);
            }
        } else {
            outputsPanel.getLinkedTab().setVisible(false);
            outputsPanel.setVisible(false);
        }
    }

    private String extractDashboardLabel(String outputDDL) {
        HPCCConnection hpccConnection = process.getCompositionInstance().getHPCCConnection();
        StringBuilder urlBuilder = new StringBuilder(hpccConnection.getESPUrl()).append("/WsWorkunits/WUResult.json?Wuid=")
                .append(process.getCompositionInstance().getWorkunitId()).append("&ResultName=").append(outputDDL);

        try {
            URL url = new URL(urlBuilder.toString());
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic" + hpccConnection.getAuthString());

            Scanner scanner = new Scanner(urlConnection.getInputStream());
            scanner.useDelimiter("//A");
            String json = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            //parse through the JSON from workunit
            JSONObject jsonObj = (JSONObject)((JSONObject) JSONValue.parse(json)).get("WUResultResponse");
            JSONObject jsonResult = ((JSONObject)jsonObj.get("Result"));
            JSONObject outputDDLJson = (JSONObject) jsonResult.get(outputDDL);
            JSONArray rowJson = (JSONArray) outputDDLJson.get("Row");
            String DDLString = (String) ((JSONObject)rowJson.get(0)).get(outputDDL);
            JSONObject DDLJson = (JSONObject) ((JSONArray) JSONValue.parse(DDLString)).get(0);
           
            //get the title to be used for display
            return DDLJson.get("label").toString();
            
         
        } catch (IOException e) {
            LOGGER.error("Dashbaord name extraction failed IOException", e);
            return "Dashboard";
        } catch (Exception e){
            LOGGER.error("Dashbaord name extraction failed", e);
            return "Dashboard";
        }
    }

    private Label createInvalidLabel() {
        Label label = new Label(Labels.getLabel(DDL_INVALID));
        label.setSclass(Constants.EMPTY_MESSAGE);
        return label;
    }

    /**
     * This timer event checks for process status for every 4 sec and updates UI
     * accordingly
     * 
     * @param timer
     */
    protected void updateStatus(Timer timer) {
        try {

            String workUnitStatus = process.getCompositionInstance().getWorkunitStatus(true);
            status.setValue(StringUtils.capitalize(workUnitStatus));
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("process.getStatus() in updateStatus() --->" + process.getStatus());
            }
            if (COMPLETED.equals(workUnitStatus) || FAILED.equals(workUnitStatus)) {
                projectRunningHbox.setVisible(false);
                ListModelList<Plugin> model = new ListModelList<Plugin>();
                model.addAll(plugins);
                statusLabel.setValue(generateStatusLabel(process.getStatus()));
                timer.setRepeats(false);
                timer.stop();

                // Setting up for reloading
                outputTab.setAttribute(IS_LOADED, false);
                sideEffectTab.setAttribute(IS_LOADED, false);
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    private String generateStatusLabel(String status) {
        return Labels.getLabel("processStatus") + " " + StringUtils.capitalize(status);
    }

    @Listen("onSelect = #sideEffectTab")
    public void loadSideeffectsTab() {
        if (sideEffectTab.getAttribute(IS_LOADED) == null) {
            createSideeffects();
        } else {
            if (!sideEffects.getTabs().getChildren().isEmpty()) {
                loadVisualization((Tab) sideEffects.getTabs().getFirstChild());
            }
        }
    }

    @Listen("onSelect = #outputTab")
    public void loadOutputsTab() {
        if (outputTab.getAttribute(IS_LOADED) == null) {
            createOutputs();
        } else {
            if (!outputs.getTabs().getChildren().isEmpty()) {
                loadVisualization((Tab) outputs.getTabs().getFirstChild());
            }
        }
    }

    @Listen("onSelect = #inputTab")
    public void onSelectInputTab(Event event) {
        if (event.getTarget().getAttribute(IS_LOADED) == null) {
            createInputs();
            event.getTarget().setAttribute(IS_LOADED, true);
        }
    }

    private void createInputs() {
        LOGGER.debug("filename:{}", HIPIEUtil.getFilename(composition));
        LOGGER.debug("hpccconID:{}", project.getHpccConnection().getLabel());
        previewHolder.setDynamicProperty(Constants.PROJECT, project);
        previewHolder.setDynamicProperty(Constants.FILE, HIPIEUtil.getFilename(composition));
        previewHolder.setSrc("ramps/project/file_contents_preview.zul");
    }

    @Listen("onSelect = #sideEffects, #outputs")
    public void onSelectSideEffects(SelectEvent<Component, Object> event) {
        Tab selectedTab = (Tab) event.getSelectedItems().iterator().next();

        loadVisualization(selectedTab);
    }

    private void loadVisualization(Tab selectedTab) {
        long startTime = Instant.now().toEpochMilli();
        Boolean isLoaded = (Boolean) selectedTab.getAttribute(IS_LOADED);
        if (isLoaded != null && isLoaded) {
            return;
        }

        String visualizationDDL = (String) selectedTab.getAttribute(Constants.VISUALIZATION_DDL);
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new DataAccess(process.getCompositionInstance(), visualizationDDL, startTime));
        }

        Tabpanel linkedPanel = selectedTab.getLinkedPanel();
        linkedPanel.getChildren().clear();

        Include include = new Include();
        linkedPanel.appendChild(include);

        include.setDynamicProperty(Constants.VISUALIZATION_DDL, visualizationDDL);
        include.setDynamicProperty(Constants.PROCESS, process);
        include.setSrc("/ramps/output.zul");

        selectedTab.setAttribute(IS_LOADED, new Boolean(true));
    }

    @Listen("onClose = #infoContainer")
    public void close(Event event) {
        event.stopPropagation();
        Component home = (Component) getSelf().getDesktop().getAttribute(Constants.HOME_COMPONENT);
        Events.sendEvent(EVENTS.ON_CLOSE_PROCESS_INFO, home, null);
    }
}
