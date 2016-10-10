package org.hpccsystems.dsp.ramps.entity;

public class SimplePluginRelation {
    private String input;
    private String displayName;
    private String output;
    private boolean isDatasourceOutput;
    
    public SimplePluginRelation() {
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean isDatasourceOutput() {
        return isDatasourceOutput;
    }

    public void setDatasourceOutput(boolean isDatasourceOutput) {
        this.isDatasourceOutput = isDatasourceOutput;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String pluginName) {
        String val = null;
        String dsInput = "dsInput";
        if (input.contains(dsInput)) {
            val = input.replace(dsInput, pluginName + " Input ");
        }
        this.displayName = val != null ? val : input;
    }

    @Override
    public String toString() {
        return "SimplePluginRelation [input=" + input + ", displayName=" + displayName + ", output=" + output + ", isDatasourceOutput="
                + isDatasourceOutput + "]";
    }
}
