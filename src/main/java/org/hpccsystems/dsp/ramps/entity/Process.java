package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Process implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private static final String ERROR_OCCURED_WHILE_RETRIVING_PROCESS_STATUS = "Error occured while retriving process status";
    private String id;
    private String projectName;
    private String status;
    private String runner;
    private Date date;

    private CompositionInstance compositionInstance;
    private static final Logger LOGGER = LoggerFactory.getLogger(Process.class);
    
    private Boolean isDDLValid;
    
    private List<String> resultNames;
    
    public Process() {
    }
    
    public Process(CompositionInstance ci) {
        this.compositionInstance = ci;
        this.id = ci.getWorkunitId();
        this.projectName = ci.getComposition().getLabel();
        try {
            this.status = ci.getWorkunitStatus();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        this.runner = ci.getCurrentUsername();
    }


    public Process(String projectName, CompositionInstance instance) throws HipieException {
        setId(instance.getWorkunitId());
        setProjectName(projectName);
        try {
            setStatus(instance.getWorkunitStatus());
            setDate(instance.getWorkunitDate());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            throw new HipieException("Unable to set status and date", e);
        }
        setCompositionInstance(instance);
        setRunner(instance.getCurrentUsername());
    }

    public String getHPCCid() {
        return compositionInstance.getHPCCConnection().getLabel();
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastRunDateAsString() {
        if (date != null) {
            return new SimpleDateFormat(Constants.DATE_FORMAT).format(date);
        } else {
            return "";
        }
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public CompositionInstance getCompositionInstance() {
        return compositionInstance;
    }

    public void setCompositionInstance(CompositionInstance compositionInstance) {
        this.compositionInstance = compositionInstance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (this == obj) {
            isEqual = true;
        }
        if (obj == null) {
            isEqual = false;
        }
        if (obj != null && getClass() != obj.getClass()) {
            isEqual = false;
        }
        Process other = (Process) obj;
        if (id == null) {
            if (other.id != null) {
                isEqual = false;
            }
        } else if (!id.equals(other.id)) {
            isEqual = false;
        }
        return isEqual;
    }

    @Override
    public String toString() {
        return "Process [id=" + id + "]";
    }
    
    /**
     * @return
     *  Whether status is 'completed'
     */
    public boolean isStatusComplete() {
        return Constants.STATUS_COMPLETED.equals(status);
    }
    
    /**
     * @return
     *  Whether work-unit running has ended. Status may be failed, completed, error etc., 
     */
    public boolean isRunComplete() {
        try {
            return compositionInstance.isComplete();
        } catch (Exception e) {
            LOGGER.error(ERROR_OCCURED_WHILE_RETRIVING_PROCESS_STATUS, e);
            return false;
        }
    }
    
    /**
     * @return
     *  true if process status is 'unknown' or 'running' state, false otherwise
     */
    public boolean isRunning() {
        try {
            return compositionInstance.isRunning();
        } catch (Exception e) {
            LOGGER.error(ERROR_OCCURED_WHILE_RETRIVING_PROCESS_STATUS, e);
            return false;
        }
    }

    public String getRunner() {
        return runner;
    }

    public void setRunner(String runner) {
        this.runner = runner;
    }
    
    public String getWorkunitURL() {
        HPCCConnection hpccConnection = compositionInstance.getHPCCConnection();
        StringBuilder processUrl = new StringBuilder();
        processUrl.append(hpccConnection.getESPUrl()).append("?Wuid=").append(id).append("&Widget=WUDetailsWidget");
        
        return processUrl.toString();
    }
    
    
    
    public boolean validteDDLs() {
        Composition composition = compositionInstance.getComposition();

        try {
            
            List<String> results = getResultNames();
            LOGGER.debug("Workunit DDLs: {}", results);
            
            for(String ddl : RampsUtil.getDDLs(composition, true)){
                if(results.contains(ddl) || results.stream().anyMatch(resultName -> resultName.endsWith(ddl))) {
                    continue;
                } else {
                    return false;
                }
            }
            
            for(String ddl : RampsUtil.getDDLs(composition, false)){
                if(results.contains(ddl) || results.stream().anyMatch(resultName -> resultName.endsWith(ddl))) {
                    continue;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Cannot be validated" , e);
            return false;
        }
        return true;
    }


    public Boolean isDDLValid() {
        if(isDDLValid == null) {
            isDDLValid = validteDDLs();
        }
        return isDDLValid;
    }
    
    /**
     * @return
     *  Output DDLs excluding side-effect DDLs and Dashboard DDLs from Dashboard repo
     * @throws HipieException 
     */
    public List<String> getOutputDDLs() throws HipieException {
        List<String> dashboardDDLs = new ArrayList<String>();

        List<String> dashboardCIs = new ArrayList<String>();
        compositionInstance.getComposition().getContractInstances().values().stream()
                .filter(ci -> Dashboard.DASHBOARD_REPO.equals(ci.getContract().getRepositoryName()))
                .forEach(ci -> dashboardCIs.add(ci.getInstanceID()));

        List<String> outputDDLs = getDDLs(false);
        
        LOGGER.debug("Ouput DDLs - {}", outputDDLs);
        
        for (String ddl : outputDDLs) {
            for (String instanceId : dashboardCIs) {
                if (ddl.contains(instanceId)) {
                    dashboardDDLs.add(ddl);
                }
            }
        }
        if (!dashboardDDLs.isEmpty()) {
            for (String element : dashboardDDLs) {
                outputDDLs.remove(element);
            }
        }
        return outputDDLs;
    }

    public List<String> getDDLs(boolean isSideEffect) throws HipieException {
        List<String> compositionDDLs = RampsUtil.getDDLs(compositionInstance.getComposition(), isSideEffect);
        
        return getResultNames().stream().filter(ddl -> compositionDDLs.stream().anyMatch(compDDL -> ddl.endsWith(compDDL)))
                .collect(Collectors.toList());
    }
    
    public List<String> getResultNames() {
        if(resultNames == null) {
            resultNames = new ArrayList<String>();
            try {
                for(int i=0;i<compositionInstance.getHPCCConnection().getWorkunitInfo(compositionInstance.getWorkunitId()).getResults().length;i++){
                    resultNames.add(compositionInstance.getHPCCConnection().getWorkunitInfo(compositionInstance.getWorkunitId()).getResults()[i].getName());
                }
                LOGGER.debug("Result names : {}", resultNames);
            } catch (Exception e) {
                LOGGER.error("HPCC Connectivity error", e);
            }
        }
        return resultNames;
    }

    //TODO Remove this method if not used
    public void refreshDDLs() {
        resultNames = null;
        getResultNames();
    }

}
