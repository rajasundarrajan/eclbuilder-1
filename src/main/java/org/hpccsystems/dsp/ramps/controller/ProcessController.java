package org.hpccsystems.dsp.ramps.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.component.renderer.ProcessRowRenderer;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver.ProcessType;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.DBLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ProcessController extends SelectorComposer<Component> implements EventListener<Event> {

    private static final String STATUS2 = "status";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessController.class);

    // Wires to the Current zk desktop
    @WireVariable
    private Desktop desktop;

    @Wire
    private Grid processList;

    @Wire
    private Column dateColumn;

    @Wire
    private Label filterStatus;
    @Wire
    private Hlayout filterDisplayContainer;

    @Wire
    private Textbox status;
    @Wire
    private Textbox id;

    @Wire
    private Hbox searchHbox;

    @Wire
    private Timer timer;

    @Wire
    private Button filterBtn;
    @Wire
    private Button refreshButton;
    
    @Wire
    private Column actionColumn;
    
    long startTime;

    private Set<Process> processes = new HashSet<Process>();
    private ListModelList<Process> processModel = new ListModelList<Process>();

    private TabData rampsdata;
    private DashboardConfig config;
    private ProcessRetriver.ProcessType processType;

    private void updateProcesses() {
        processModel.addAll(processes);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("updateProcess ---> {}", processes);
        }
        dateColumn.setSortDirection("natural");
        dateColumn.sort(false);

        doAfterLoadingProcess();
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        rampsdata = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        config = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        processType = (ProcessRetriver.ProcessType) Executions.getCurrent().getArg().get(Constants.PROCESS_PAGE_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processsss {}", processType);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating procees.. Tab Data - {}" + Executions.getCurrent().getArg().get(Constants.TAB_DATA));
        }

        // Disable timer. Only to be enabled when a running process is present
        timer.stop();
        
        //Disabling action column for dashboard processes
        if (processType.equals(ProcessRetriver.ProcessType.DASHBOARD) || processType.equals(ProcessRetriver.ProcessType.USER_DASHBOARDS)) {
            actionColumn.setVisible(false);
        }

        this.getSelf().getParent().addEventListener(EVENTS.ON_UPDATE_PROCESSES, event -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("processes --> {}", processes);
                LOGGER.debug("composition -> {}", event.getData());
            }

            desktop.enableServerPush(true);
            String userId = ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
            if (event.getData() != null && event.getData() instanceof Composition) {
                startTime = Instant.now().toEpochMilli();
                DSPExecutorHolder.getExecutor().execute(new ProcessRetriver(desktop, ProcessController.this, null, (Composition) event.getData()));
            } else {
                startTime = Instant.now().toEpochMilli();
                DSPExecutorHolder.getExecutor().execute(new ProcessRetriver(desktop, ProcessController.this, userId, processType));
            }
        });

        this.getSelf().addEventListener(EVENTS.ON_RUN_INITIATED, event -> {
            Process process = new Process();
            processModel.add(0, process);
        });

        processList.setModel(processModel);

        renderProcess();
      
    }

    private void renderProcess() {
        if(rampsdata != null){
            processList.setRowRenderer(new ProcessRowRenderer(rampsdata, timer, processType));
        }else if(config != null){
            processList.setRowRenderer(new ProcessRowRenderer(config.getData(), timer, processType));
        }else{
            processList.setRowRenderer(new ProcessRowRenderer(new TabData(), timer, processType));  
        }

        // Avoiding refresh call in when under new project
        if(rampsdata != null){
            if (!(processType == ProcessType.COMPOSITION && Constants.Flow.NEW == rampsdata.getFlow())) {
                LOGGER.debug("Loading processes...");
                doBeforeLoadingProcess();
                echoRefreshEvent();
            }
        }else{
            doBeforeLoadingProcess();
            echoRefreshEvent();
        }
    }

    private static boolean evaluateProcess(Process process, Map<String, String> searchTerms) {
        Boolean isValid = null;

        for (Entry<String, String> entry : searchTerms.entrySet()) {
            String column = entry.getKey();
            String value = entry.getValue();

            try {
                if (StringUtils.containsIgnoreCase(
                        (String) Process.class.getMethod("get" + WordUtils.capitalize(column), (Class<?>[]) null).invoke(process, (Object[]) null),
                        value)) {
                    isValid = isValid == null ? true : isValid;
                } else {
                    isValid = false;
                }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }

        return isValid;
    }

    @Listen("onClick = #clear,#removeFilters")
    public void clearFilters() {
        status.setText("");
        id.setText("");

        processModel.clear();
        processModel.addAll(processes);

        filterDisplayContainer.setVisible(false);
        searchHbox.invalidate();
    }

    @Listen("onClick = #apply")
    public void applyFilter() {
        Map<String, String> searchTerms = new HashMap<String, String>();

        StringBuilder builder = new StringBuilder();

        if (StringUtils.isEmpty(id.getText().trim()) && StringUtils.isEmpty(status.getText().trim())) {
            clearFilters();
            return;
        }

        if (!StringUtils.isEmpty(id.getText().trim())) {
            searchTerms.put("id", id.getText());
            builder.append(Labels.getLabel("processID")).append(" : ").append(id.getText());
        }

        if (!StringUtils.isEmpty(status.getText().trim())) {
            searchTerms.put(STATUS2, status.getText());
            builder.append("  ").append(Labels.getLabel(STATUS2)).append(" : ").append(status.getText());
        }

        filterStatus.setValue(builder.toString());
        filterDisplayContainer.setVisible(true);
        filterProcesses(searchTerms);
        searchHbox.invalidate();
    }

    public void filterProcesses(Map<String, String> searchTerms) {
        processModel.clear();

        processModel.addAll(processes.stream().filter(process -> evaluateProcess(process, searchTerms)).collect(Collectors.toList()));

    }

    @Listen("onClick = #refreshButton")
    public void refreshProcesses() {
        refreshButton.setIconSclass("fa fa-refresh fa-spin");
        refreshButton.setLabel(Labels.getLabel("refreshing"));
        refreshButton.setSclass("refreshing-btn");
        searchHbox.invalidate();

        doBeforeLoadingProcess();
        echoRefreshEvent();
    }

    @Listen("onTimer = #timer")
    public void startAutoRefresh() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Timer listen - Updating processes");
        }
        echoRefreshEvent();
        timer.stop();
    }

    private void echoRefreshEvent() {
        if (processType == ProcessType.COMPOSITION || processType == ProcessType.DASHBOARD ) {
            if(rampsdata != null){
                Events.echoEvent(EVENTS.ON_UPDATE_PROCESSES, getSelf().getParent(), rampsdata.getComposition());
            }else{
                Events.echoEvent(EVENTS.ON_UPDATE_PROCESSES, getSelf().getParent(), config.getComposition());
            }
           
        } else {
            Events.echoEvent(EVENTS.ON_UPDATE_PROCESSES, getSelf().getParent(), null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEvent(Event event) throws Exception {

        if (EVENTS.ON_PROCESS_LOADED.equals(event.getName())) {
            processes = (Set<Process>) event.getData();
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.PROCESS_RETRIVE, startTime, "Process successfully retrived\nNumber of processes retrived : " + processes.size()));
            }

            LOGGER.debug("Project state - {}", rampsdata != null ? rampsdata.getProject().isRunning() : "Not applicable" );
            
            if(rampsdata != null && rampsdata.getProject().isRunning()){
                processModel.clear();
                updateProcesses();
                processModel.add(0,new Process());
                return;
            }

            processModel.clear();
            updateProcesses();
        } else if (EVENTS.ON_PROCESS_LOAD_FAIL.equals(event.getName())) {
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.PROCESS_RETRIVE, startTime, "Failed to retrive process"));
            }
            Exception exception = (Exception) event.getData();
            LOGGER.error(Constants.EXCEPTION, exception);
            Clients.showNotification(Labels.getLabel("errorGettingProcess"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            doAfterLoadingProcess();
        }
    }

    private void doBeforeLoadingProcess() {
        processList.setSclass("loading-message-grid");
        processList.setEmptyMessage("<i class=\"fa fa-spinner fa-pulse\"></i> " + Labels.getLabel("loadingProcess"));
        refreshButton.setDisabled(true);
        filterBtn.setDisabled(true);
    }

    private void doAfterLoadingProcess() {
        processList.setSclass("");
        processList.setEmptyMessage(Labels.getLabel("noProcess"));
        refreshButton.setIconSclass("z-icon-refresh");
        refreshButton.setLabel(Labels.getLabel("dspRefresh"));
        refreshButton.setSclass("refresh-btn");
        refreshButton.setDisabled(false);
        filterBtn.setDisabled(false);
    }

}
