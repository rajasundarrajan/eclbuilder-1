package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.ListModelMap;

/**
 * Class is used in UI. Restrain from changing Method/Variable names
 *
 */
public class PluginRelationInputs implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRelationInputs.class);

    private List<String> targetInputs;
    private List<PluginRelationOutput> outputs;

    /**
     * This relation is only used to generate tile in the UI. This will hold the
     * mandatory plugin relation or a reference Usedataset relation which may
     * not be mandatory
     */
    private PluginRelation definingRelation;

    public PluginRelationInputs(List<String> inputs, List<PluginRelationOutput> outputRelations) {
        this.targetInputs = inputs;
        this.outputs = outputRelations;

        // Creating definingRelation for UI
        Optional<PluginRelationOutput> mandatoryRelation = outputRelations.stream()
                .filter(prOp -> !prOp.getPluginRelation().getSourcePlugin().isDatasourcePlugin()).findAny();

        if (mandatoryRelation.isPresent()) {
            definingRelation = mandatoryRelation.get().getPluginRelation();
        } else {
            // All relations are UseDataset relations, hence any one can be
            // chosen.
            // Choosing the first one
            definingRelation = outputRelations.iterator().next().getPluginRelation();
        }

    }

    public PluginRelationOutput isOutputAvailable(String input) {
        PluginRelationOutput output = null;
        for (PluginRelationOutput pluginRelationOutput : outputs) {
            if (pluginRelationOutput.getPluginRelation().getRelation(input) != null) {
                output = pluginRelationOutput;
                LOGGER.debug("output-------------->{}", output);
            }
        }

        return output;
    }

    public ListModelMap<String, String> getInputsModel() {
        ListModelMap<String, String> inputModel = new ListModelMap<String, String>(getTargetInputMap());
        return inputModel;
    }

    public ListModelList<PluginRelationOutput> getOutputsModel() {
        ListModelList<PluginRelationOutput> outputModel = new ListModelList<>();
        outputModel.addAll(outputs);
        return outputModel;
    }

    public Map<String, String> getTargetInputMap() {
        Map<String, String> inputsMap = new LinkedHashMap<String, String>();
        for (String input : targetInputs) {
            String val = null;
            String dsInput = "dsInput";
            if (input.contains(dsInput)) {
                val = input.replace(dsInput, getTargetPluginNane() + " Input ");
            }
            inputsMap.put(input, val != null ? val : input);
        }
        return inputsMap;
    }

    public List<String> getTargetInputs() {
        return targetInputs;
    }

    public List<PluginRelationOutput> getOutputs() {
        return outputs;
    }

    public String getSourcePluginNane() {
        return definingRelation.getSourcePlugin().getLabel();
    }

    public String getTargetPluginNane() {
        return definingRelation.getDestplugin().getLabel();
    }

    public boolean hasValidRelation() {
        List<PluginRelation> relations = outputs.stream().map(output -> output.getPluginRelation()).collect(Collectors.toList());

        // When all Relations have datasource as source, any one relation
        // established is valid
        if (relations.stream().allMatch(relation -> relation.getSourcePlugin().isDatasourcePlugin())) {
            LOGGER.debug("All are Dataset relations");
            return relations.stream().anyMatch(relation -> relation.isValid());
        }

        // When non-dataset plugins are also present in the relation definition
        if (relations.stream().anyMatch(relation -> !relation.getSourcePlugin().isDatasourcePlugin())) {
            LOGGER.debug("Non-Dataset relations are present");
            return relations.stream().anyMatch(relation -> !relation.isOptional() && relation.isValid());
        }

        return false;
    }
}