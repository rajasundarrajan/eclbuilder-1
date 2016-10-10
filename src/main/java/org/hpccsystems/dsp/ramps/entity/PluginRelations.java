package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginRelations implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRelations.class);
    
    public enum PluginEvent {
        APPEND_PLUGIN,
        DELETE_PLUGIN,
        INSERT_PLUGIN,
        EDIT_RELATIONS
    }
    
    private List<PluginRelation> pluginRelations;
    private String message;
    
    /**
     * In addition to adding relation to the list,
     * Validates if dataset has multiple outputs and target plugin has multiple inputs 
     * & Creates that relation and adds it to the list
     * @param datasetPlugin
     *  Can be null, when all of Dataset plugins are passed individually
     * @param pluginRelation
     */
    public void addPluginRelation(DatasetPlugin datasetPlugin, PluginRelation pluginRelation, Project project) {
        if(pluginRelations == null) {
            pluginRelations = new ArrayList<PluginRelation>();
        }
        
        if(containsRelation(pluginRelation)) {
            return;
        }
        
        if(datasetPlugin != null 
                && !pluginRelation.getSourcePlugin().isDatasourcePlugin()) {
            Plugin targetPlugin = pluginRelation.getDestplugin();
            LOGGER.debug("Source is not Dataset. OPs in Dataset - {}, IPs in target - {}", datasetPlugin.hasMultipleOutputs(), targetPlugin.hasMultipleInputs());
            if(datasetPlugin.hasMultipleOutputs() && targetPlugin.hasMultipleInputs()) {
                pluginRelations.add(new PluginRelation(datasetPlugin, targetPlugin, project));
            }
        }
        
        pluginRelations.add(pluginRelation);
    }
    
    public List<PluginRelation> getPluginRelations() {
        return pluginRelations;
    }
    
    public void setAddMessage(Plugin plugin) {
        message = "Adding " + plugin.getLabel() + " to Composition"; 
    }
    
    public void setDeleteMessage(Plugin plugin) {
        message = "Deleting " + plugin.getLabel(); 
    }

    public String getMessage() {
        return message;
    }
    
    public boolean containsRelation(PluginRelation relation) {
        for (PluginRelation pluginRelation : pluginRelations) {
            if(pluginRelation.getSourcePlugin().equals(relation.getSourcePlugin()) && 
                    pluginRelation.getDestplugin().equals(relation.getDestplugin())) {
                return true;
            }
        }
        return false;
    }

    public void setInsertMessage(Plugin plugin) {
        message = "Inserting " + plugin.getLabel();
    }
    
    @Override
    public String toString() {
        return "PluginRelations [pluginRelations=" + pluginRelations + "]";
    }
    
    /**
     * @return
     *  Contains relation with an Instance of DatasetPlugin
     */
    public boolean hasDatasetPluginRelation() {
        if(CollectionUtils.isNotEmpty(pluginRelations)) {
            return pluginRelations.stream().anyMatch(rel -> rel.getSourcePlugin() instanceof DatasetPlugin);
        }
        return false;
    }
    
    public void disintegrateDatasetRelation(Project project) {
        if(CollectionUtils.isEmpty(pluginRelations)) {
            return;
        }
        
        //DatasetPlugin can only be present as a SourcePlugin
        Optional<PluginRelation> pluginRelation = pluginRelations
                                            .stream()
                                            .filter(rel -> rel.getSourcePlugin() instanceof DatasetPlugin)
                                            .findAny();
        
        if(pluginRelation.isPresent()) {
            //Create relations with individual plugins in DatasetPlugin
            Plugin destPlugin = pluginRelation.get().getDestplugin();
            DatasetPlugin datasetPlugin = (DatasetPlugin) pluginRelation.get().getSourcePlugin();
            
            //Insert new relations in place-of Dataset relation
            int pos = pluginRelations.indexOf(pluginRelation.get());
            pluginRelations.addAll(pos, datasetPlugin.getPlugins()
                                                .stream()
                                                .map(plugin -> new PluginRelation(plugin, destPlugin, project))
                                                .collect(Collectors.toList()));
            
            //Remove DatasetPlugin relation
            pluginRelations.remove(pluginRelation.get());
        }
        
        LOGGER.debug("Disintegrated relations - {}", pluginRelations);
    }
    
    public void establishDefaultRelations() throws HipieException {
        for (PluginRelation relation : pluginRelations) {
            relation.hookupPlugins();
        }
    }
}
