package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;

import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertPluginRelation implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(InsertPluginRelation.class);
    
    private Plugin precursor;
    private Plugin insertedPlugin;
    private Plugin successor;
    
    public InsertPluginRelation(Plugin precursor, Plugin insertedPlugin, Plugin successor) {
       this.precursor = precursor; 
       this.insertedPlugin = insertedPlugin; 
       this.successor = successor; 
    }
    
    public Plugin getPrecursor() {
        return precursor;
    }
    
    public Plugin getInsertedPlugin() {
        return insertedPlugin;
    }
    
    public Plugin getSuccessor() {
        return successor;
    }
    
    public boolean hasMultipleRelations() {
        return precursor.hasMultiplePorts() || insertedPlugin.hasMultiplePorts() || successor.hasMultiplePorts();
    }
    
    public PluginRelations createRelations(DatasetPlugin datasetPlugin, Project project) {
        PluginRelations relations = new PluginRelations();
                
        if(precursor.hasMultipleOutputs() || insertedPlugin.hasMultipleInputs()) {
            relations.addPluginRelation(datasetPlugin, new PluginRelation(precursor, insertedPlugin, project), project);
        }
        
        if(insertedPlugin.hasMultipleOutputs() || precursor.hasMultipleInputs()) {
            relations.addPluginRelation(datasetPlugin, new PluginRelation(insertedPlugin, successor, project), project);
        }
        
        relations.setInsertMessage(insertedPlugin);
        
        return relations;
    }
    
    public void establishRelations(PluginRelations relations) throws HipieException {
        try {
            // Sever all relations of successor
            String nullVal = null;
            successor.getContractInstance().getParent().getInputElements(Element.TYPE_DATASET)
                .forEach(element -> successor.getContractInstance().setProperty(element.getName(), nullVal));
            
            //Create new relations
            if(relations == null) {
                // When no multiple outputs or inputs present
                insertedPlugin.getContractInstance().addPrecursor(precursor.getContractInstance());
                successor.getContractInstance().addPrecursor(insertedPlugin.getContractInstance());
            } else {
                //When relations provided
                
                // Creating default links when relation is not present in relations
                //Skipping UseDataset as datasource as it will be a mandated relation in relations
                if(!precursor.isDatasourcePlugin() 
                        && !relations.containsRelation(new PluginRelation(precursor, insertedPlugin, null))) {
                    insertedPlugin.getContractInstance().addPrecursor(precursor.getContractInstance());
                }
                
                //Creating links in relations
                for (PluginRelation relation : relations.getPluginRelations()) {
                    relation.hookupPlugins();
                }
                
                if(!relations.containsRelation(new PluginRelation(insertedPlugin, successor, null))) {
                    successor.getContractInstance().addPrecursor(insertedPlugin.getContractInstance());
                }
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Unable to establish connection",e);
        }
        
    }
}
