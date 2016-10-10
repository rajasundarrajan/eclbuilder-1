package org.hpccsystems.dsp.ramps.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Plugin implements Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugin.class);

    private String instanceId;
    private String name;
    private String label;
    private String repo;
    private Contract contract;
    private ContractInstance contractInstance;
    private Set<String> sideEffectDDLs;

    private List<Element> outputElements;

    public Plugin(String name, String repo) {
        this.name = name;
        this.repo = repo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone() Shallow cloning
     */
    @Override
    public Plugin clone() throws CloneNotSupportedException {
        Plugin clone = (Plugin) super.clone();

        if (clone.getContractInstance() != null) {
            clone.setContractInstance(null);
        }

        return clone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRepository() {
        return repo;
    }

    public void setRepository(String repo) {
        this.repo = repo;
    }

    public boolean isDatasourcePlugin() {
        return HIPIEUtil.isDataSourcePlugin(contractInstance);
    }

    public boolean isLiveHIPEPlugin() {
        return !isDatasourcePlugin();
    }

    public String getLabel() {
        if (label != null && !label.isEmpty()) {
            return label;
        } else {
            return name;
        }

    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ContractInstance getContractInstance() {
        return contractInstance;
    }

    public void setContractInstance(ContractInstance contractInstance) {
        this.contractInstance = contractInstance;
    }

    @Override
    public String toString() {
        return "Plugin [instanceId=" + instanceId + ", name=" + name + ", label=" + label + ", repo=" + repo + ", contract=" + contract
                + ", contractInstance=" + contractInstance + ", sideEffectDDLs=" + sideEffectDDLs + ", outputElements=" + outputElements + "]";
    }

    public String getInstanceId() {
        if (instanceId == null && contractInstance != null) {
            return contractInstance.getInstanceID();
        }
        return instanceId;
    }

    public void setInstanceId(String id) {
        this.instanceId = id;
    }

    public Set<String> getSideEffectDDLs() {
        return sideEffectDDLs;
    }

    public void setSideEffectDDLs(Set<String> sideEffectDDLs) {
        this.sideEffectDDLs = sideEffectDDLs;
    }

    public Contract getContract() {
        if(contract == null && contractInstance != null) {
            return contractInstance.getContract();
        }
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    /**
     * @return Output Datasets in ContractInstance. This filters out
     *         Sub-Datasets in Output Datasets
     */
    public List<Element> getOutputElements() {
        if (outputElements == null) {
            outputElements = RampsUtil.filterSubDatasets(contractInstance.getParent().getOutputElements(Element.TYPE_DATASET, false));
        }
        return outputElements;
    }

    public boolean hasMultipleOutputs() {
        return getOutputElements().size() > 1;
    }

    public List<Element> getInputElements() {
        return contractInstance.getParent().getInputElements(Element.TYPE_DATASET);
    }

    public boolean hasMultipleInputs() {
        return getInputElements().size() > 1;
    }

    public String getLogicalFileName() {
        return Constants.FILE.equals(contractInstance.get(Constants.LOGICAL_FILENAME)) ? null : contractInstance.get(Constants.LOGICAL_FILENAME);
    }

    public String getLogicalFileNameUsingProperty() {
        return contractInstance.getProperty(Constants.LOGICAL_FILENAME);
    }

    /**
     * @return Whether the plugin has multiple outputs or inputs
     */
    public boolean hasMultiplePorts() {
        return hasMultipleInputs() || hasMultipleOutputs();
    }

    public List<ContractInstance> getPrecursorCIs(Project project) {
        List<ContractInstance> cis = new ArrayList<>();

        contractInstance.getContract().getInputElements().forEach(element -> {
            String inputName = element.getName();
            String property = contractInstance.getProperty(inputName);
            if (StringUtils.isNotBlank(property) && property.startsWith("^")) {
                String instanceid = StringUtils.substringBetween(property, "^", "|");
                try {
                    cis.add(project.getContractInstance(instanceid));
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    return;
                }
            }
        });

        return cis;
    }

    public List<SimplePluginRelation> createSimpleRelations(Project project) {
        LOGGER.debug("Creating relation for plugin - {}", getLabel());

        List<SimplePluginRelation> relations = new ArrayList<SimplePluginRelation>();

        contractInstance.getContract().getInputElements().forEach(element -> relateInputOutputs(project, relations, element));

        return relations;
    }

    private void relateInputOutputs(Project project, List<SimplePluginRelation> relations, Element element) {
        String inputName = element.getName();
        String property = contractInstance.getProperty(inputName);
        if (StringUtils.isNotBlank(property) && property.startsWith("^")) {
            String instanceid = StringUtils.substringBetween(property, "^", "|");
            ContractInstance ci;
            try {
                ci = project.getContractInstance(instanceid);
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                return;
            }

            SimplePluginRelation relation = new SimplePluginRelation();
            relation.setInput(inputName);
            relation.setDisplayName(getName());
            if (HIPIEUtil.isDataSourcePlugin(ci)) {
                relation.setOutput(ci.getProperty(Constants.LOGICAL_FILENAME));
                relation.setDatasourceOutput(true);
            } else {
                // Property will have the relation like -
                // dsInput="^Ins001|dsOutput"
                String outputName = StringUtils.substringAfter(property, "|");
                relation.setOutput(project.getPlugin(ci).getLabel() + " - " + outputName);
            }
            relations.add(relation);
        }
    }

    public String getStructure() {
        String structure = null;
        if (getContractInstance() != null) {
            structure = getContractInstance().getProperty(Constants.STRUCTURE);
        }
        return structure;
    }

    public boolean setStructure(String structure) {
        try {
            getContractInstance().setProperty(Constants.STRUCTURE, structure);
            return true;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            return false;
        }
    }
    
    public boolean isConfigured(){
        return contractInstance != null;
    }
    
    /**
     * Checks if it is a input plugin. 
     * @return {@code true} if an input plugin, {@code false} otherwise.
     */
    public boolean isInputPlugin() {
    	boolean result = false;
    	if(isDatasourcePlugin()) {
    		result = true;
    	} else {
    		result = HIPIEUtil.isInputPlugin(contractInstance);
    	}
    	
    	return result;
	}

}
