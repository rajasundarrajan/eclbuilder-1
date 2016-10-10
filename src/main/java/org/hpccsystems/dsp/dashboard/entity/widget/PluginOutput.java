package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;

public class PluginOutput implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private String label;
    private ContractInstance contractInstance;
    private Element outputElement;
    private boolean fileNotExists;
     
    
    /**
     * Use this only for Contract Instance with single output. eg. RawDataset 
     * @param contractInstance
     *   
     */
    public PluginOutput(ContractInstance contractInstance) {
        this.contractInstance  = contractInstance;
        this.outputElement = contractInstance.getContract().getOutputElements().iterator().next();
    }

    public PluginOutput(ContractInstance contractInstance, Element outputElement) {
        this.contractInstance = contractInstance;
        this.outputElement = outputElement;
    }
    
   
    
    @Override
    public String toString() {
        return "PluginOutput [label=" + getLabel() + "]";
    }



    public String getLabel() {
        String fileName = contractInstance.getProperty(Constants.LOGICAL_FILENAME);
        //Making PluginOutput output to return logical file name when it is based on UseDataset plugin
        if(StringUtils.isNotEmpty(fileName)){
            label = fileName;
        }else if (label == null) {
            if(contractInstance.getOption(Contract.LABEL)==null){
                label = contractInstance.getContract().getLabel() + " - " + outputElement.getName();
            }else{
                label = contractInstance.getOption(Contract.LABEL).iterator().next().getParams().iterator().next()+ " - " + outputElement.getName();
            }
            
        }
        return label;
    }

    public ContractInstance getContractInstance() {
        return contractInstance;
    }

    public Element getOutputElement() {
        return outputElement;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contractInstance == null) ? 0 : contractInstance.hashCode());
        result = prime * result + ((outputElement == null) ? 0 : outputElement.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null){
            return false;
        }
        if (getClass() != obj.getClass()){
            return false;
        }
        PluginOutput other = (PluginOutput) obj;
        if (contractInstance == null) {
            if (other.contractInstance != null){
                return false;
            }
        } else if (!contractInstance.equals(other.contractInstance)){
            return false;
        }
        if (outputElement == null) {
            if (other.outputElement != null){
                return false;
            }
        } else if (!outputElement.equals(other.outputElement)){
            return false;
        }
        return true;
    }

    public boolean isFileNotExists() {
        return fileNotExists;
    }

    public void setFileNotExists(boolean fileNotExists) {
        this.fileNotExists = fileNotExists;
    }

}
