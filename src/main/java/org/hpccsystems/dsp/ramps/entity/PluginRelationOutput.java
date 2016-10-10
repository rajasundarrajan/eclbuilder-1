package org.hpccsystems.dsp.ramps.entity;

public class PluginRelationOutput {
    private String sourceOutput;
    private PluginRelation pluginRelation;

    public PluginRelationOutput(String output, PluginRelation relation) {
        this.sourceOutput = output;
        this.pluginRelation = relation;
    }

    public String getSourceOutput() {
        return sourceOutput;
    }

    public void setSourceOutput(String sourceOutput) {
        this.sourceOutput = sourceOutput;
    }

    public PluginRelation getPluginRelation() {
        return pluginRelation;
    }

    public void setPluginRelation(PluginRelation pluginRelation) {
        this.pluginRelation = pluginRelation;
    }

    public boolean isDatasource() {
        return pluginRelation.getSourcePlugin().isDatasourcePlugin();
    }

    public String getDisplayName() {
        if (isDatasource()) {
            return pluginRelation.getSourcePlugin().getLogicalFileName() == null ? "Logical file not selected"
                    : pluginRelation.getSourcePlugin().getLogicalFileName();
        } else {
            return pluginRelation.getSourcePlugin().getLabel() + " - " + sourceOutput;
        }
    }
}
