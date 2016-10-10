package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.ListModelList;

public class PluginRelation implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRelation.class);
    
    private Plugin sourcePlugin;
    private Plugin destplugin;
    private Map<String, String> relation;
    
    private ListModelList<Element> inputModel;
  
    private boolean isOptional;
    
    public PluginRelation(Plugin sourcePlugin, Plugin destPlugin, Project project) {
        this.sourcePlugin = sourcePlugin;
        setDestplugin(destPlugin);
        isOptional = sourcePlugin.isDatasourcePlugin();
        
        //Adding existing relations, if any
        if(project != null) {
            destPlugin.getContractInstance().getContract().getInputElements().forEach(element -> {
                String inputName = element.getName();
                String property = destPlugin.getContractInstance().getProperty(inputName);
                
                if(StringUtils.isNotBlank(property) && 
                        property.startsWith("^") &&
                        sourcePlugin.getContractInstance() == project.getContractInstance(StringUtils.substringBetween(property, "^", "|"))) {
                    String outputName = StringUtils.substringAfter(property, "|");
                    addRelation(inputName, outputName);
                }
            });
        }
    }

    public Plugin getSourcePlugin(){
      return this.sourcePlugin;
    }
    
    public void setSourcePlugin(Plugin sourcePlugin){
      this.sourcePlugin = sourcePlugin;
    }
    
    public Plugin getDestplugin(){
      return this.destplugin;
    }
    
    public void setDestplugin(Plugin destPlugin){
      this.destplugin = destPlugin;
      
      inputModel = new ListModelList<Element>(destplugin.getContractInstance().getContract().getInputElements(Element.TYPE_DATASET));
    }
    
    public String getRelation(String input) {
        return relation != null ? relation.get(input) : null;
    }

    public void addRelation(String input, String output) {
        if(relation == null) {
            relation = new HashMap<>();
        }
        
        relation.put(input, output);
    }
    
    public void removeRelation(String input) {
        relation.remove(input);
    }

    public void setRelation(Map<String, String> relation) {
        this.relation = relation;
    }
    
    public ListModelList<Element> getInputModel() {
        return inputModel;
    }
    
    public String getInputLabel() {
        return "Available Inputs to " + "(" + getDestplugin().getName() + ")";
    }
    
    public String getOutputLabel() {
        return "Available Outputs from " + "(" + getSourcePlugin().getName() + ")";
    }
    
    public void hookupPlugins() throws HipieException {
        for (Map.Entry<String, String> entry : relation.entrySet()) {
            try {
                LOGGER.debug("making relation. Source - {}, Destination - {}", entry.getKey(), entry.getValue());
                destplugin.getContractInstance().addPrecursor(sourcePlugin.getContractInstance(), entry.getValue(), entry.getKey());
            } catch (Exception e) {
                LOGGER.error("Error adding precursor - {}",e);
                throw new HipieException("Error adding precursor", e);
            }
        }
    }

    /**
     * @return
     *  Whether an output is selected for only one of the inputs 
     */
    public boolean isValid() {
        if(relation == null || relation.isEmpty()) {
            LOGGER.error("Relation not defined");
            return false;
        }
        
        if(relation.size() > 1) {
            Set<String> outputs = new HashSet<String>();
            outputs.addAll(relation.values());
            if(relation.size() > outputs.size()) {
                LOGGER.error("Same output used twice in a single relation");
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "PluginRelation [sourcePlugin=" + sourcePlugin.getLabel() + "(Instance of " + sourcePlugin.getClass() + ")"
                + ", destplugin=" + destplugin.getLabel() + ", relation=" + relation + "]";
    }

    public boolean isOptional() {
        return isOptional;
    }

    public boolean hasNoRelations() {
        return relation == null || relation.isEmpty();
    }
    
    
}
