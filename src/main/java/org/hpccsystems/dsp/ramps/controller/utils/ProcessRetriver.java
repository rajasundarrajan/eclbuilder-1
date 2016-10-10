package org.hpccsystems.dsp.ramps.controller.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class ProcessRetriver implements Runnable {

    private static final String UNABLE_TO_FETCH_COMPOSITION = "unableToFetchComposition";
    private String userId;
    private Composition composition;
    private EventListener<Event> eventListener;
    private Desktop desktop;
    
    
    public enum ProcessType{
        COMPOSITION, USER_DASHBOARDS, USER_COMPOSITIONS, GLOBAL,DASHBOARD;
    }
    
    private ProcessType processType; 
    
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRetriver.class);
    
    /**
     * Thread gets process from all compositions accessible to the user
     * @param executionDesktop
     * @param eventListener
     * @param userId
     */
    public ProcessRetriver(Desktop executionDesktop, EventListener<Event> eventListener, String userId, ProcessType processEnum) {
        this.userId = userId;
        this.eventListener = eventListener;
        this.desktop = executionDesktop;
        processType = processEnum;
    }
    
    /**
     * Thread gets process for the Composition, filtered based on userid if provided
     * @param executionDesktop
     * @param eventListener
     * @param userId
     *  Optional. If provided processes will be filtered based on User.
     * @param composition
     */
    public ProcessRetriver(Desktop executionDesktop, EventListener<Event> eventListener, String userId, Composition composition) {
        this.userId = userId;
        this.eventListener = eventListener;
        this.desktop = executionDesktop;
        this.composition = composition;
        processType = ProcessType.COMPOSITION;
    }
    
    @Override
    public void run() {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Retriving processes. isGlobal - {}", processType);
        }
        try {
            Set<Process> processes;
            if (processType == ProcessType.GLOBAL || processType == ProcessType.USER_DASHBOARDS || processType == ProcessType.USER_COMPOSITIONS) {
                processes = getProcesses(userId);
            } else {
                processes = getProcesses(userId, composition);
            }
            
            schedule(new Event(EVENTS.ON_PROCESS_LOADED, null , processes));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION,e);
            schedule(new Event(EVENTS.ON_PROCESS_LOAD_FAIL, null , e));
        }
    }

    private void schedule(Event event) {
        if(desktop.isAlive()) {
            Executions.schedule(desktop, eventListener, event);
        } else {
            LOGGER.warn("Process loading stopped as Desktop is unavailable");
        }
    }

    public Set<Process> getProcesses(String userId) throws CompositionServiceException {
        Set<Process> allProcesses = new HashSet<Process>();
        
        try {
            Map<String, CompositionElement> comps = HipieSingleton.getHipie().getCompositions(userId);
            Map<String, CompositionElement> filteredComps = new HashMap<String, CompositionElement>();
            comps.remove(Constants.BASIC_TEMPLATE);
            comps.remove(Dashboard.DASHBOARD_TEMPLATE);

            for (Entry<String, CompositionElement> entry : comps.entrySet()) {
                if (processType == ProcessType.GLOBAL) {
                    filteredComps.put(entry.getKey(), entry.getValue());
                } else if (processType == ProcessType.USER_DASHBOARDS) {
                    addFilteredComposition(filteredComps, entry);
                } else if (processType == ProcessType.USER_COMPOSITIONS && !Dashboard.CONTRACT_CATAGORY.equals(entry.getValue().getCategory())) {
                        filteredComps.put(entry.getKey(), entry.getValue());
                }
            }
            for (CompositionElement value : filteredComps.values()) {
                allProcesses.addAll(getProcesses(userId, value.getComposition()));
            }
        } catch (Exception e) {
            LOGGER.error("Could not fetch compositions");
            throw new CompositionServiceException(Labels.getLabel(UNABLE_TO_FETCH_COMPOSITION), e);
        }
        
        return allProcesses;
    }

    private void addFilteredComposition(Map<String, CompositionElement> filteredComps, Entry<String, CompositionElement> entry) throws Exception {
        if (Dashboard.CONTRACT_CATAGORY.equals(entry.getValue().getCategory())) {
            filteredComps.put(entry.getKey(), entry.getValue());
        }
    }
    
    public Set<Process> getProcesses(String userId,Composition composition1) throws CompositionServiceException {
        Set<Process> allProcesses = new HashSet<Process>();
        
        Map<String, CompositionInstance> instances = retriveInstances(userId, composition1);

            String projectName = composition1.getLabel();
            if (projectName == null || projectName.isEmpty()) {
                projectName = composition1.getName();
            }

        LOGGER.debug("Composition: {}", projectName);

            Process process;
            for (Entry<String, CompositionInstance> entry1 : instances.entrySet()) {
                CompositionInstance instance = entry1.getValue();
                try {
                    process = new Process(projectName, instance);
                } catch (Exception e) {
                    throw new CompositionServiceException(Labels.getLabel("couldNotCreateNewProcess"), e);
                }
                
                //Work around for filtering main processes from service processes
                if(!process.getRunner().endsWith(Constants.SERVICE)){
                    allProcesses.add(process);
                }
                
            LOGGER.debug("Process: {}", process);
            }

            return allProcesses;
        }

    private Map<String, CompositionInstance> retriveInstances(String userId, Composition composition1) throws CompositionServiceException {
        try {
            composition1.refreshCompositionInstances();
        } catch (Exception e) {
            throw new CompositionServiceException(Labels.getLabel("unableToRefreshComposition"), e);
        }
        Map<String, CompositionInstance> instances;
        try {
            if (StringUtils.isEmpty(userId) || processType == ProcessType.GLOBAL) {
                instances = composition1.getCompositionInstances();
            } else {
                instances = composition1.getCompositionInstances(userId);
            }
        } catch (Exception e) {
            throw new CompositionServiceException(Labels.getLabel(UNABLE_TO_FETCH_COMPOSITION), e);
        }
        return instances;
    }
}
