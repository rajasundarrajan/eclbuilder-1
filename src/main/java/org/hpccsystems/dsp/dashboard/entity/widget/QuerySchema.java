package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class QuerySchema implements Cloneable , Serializable{
   
    private static final long serialVersionUID = 1L;

    private String name;
    private List<Filter> inputParameters;
    private List<OutputSchema> outputs;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<Filter> getInputParameters() {
        return inputParameters;
    }
    public void setInputParameters(List<Filter> inputParameters) {
        this.inputParameters = inputParameters;
    }
    public List<OutputSchema> getOutputs() {
        return outputs;
    }
    public void setOutputs(List<OutputSchema> outputs) {
        this.outputs = outputs;
    }
    public void addOutput(OutputSchema schema) {
        if(outputs == null) {
            outputs = new ArrayList<OutputSchema>();
        }
        outputs.add(schema);
    }
    
    @Override
    public QuerySchema clone() throws CloneNotSupportedException {
        
        QuerySchema clonedQuerySchema = (QuerySchema) super.clone();
        if(inputParameters != null){
            List<Filter> clonedParams = new ArrayList<>();
            
            for(Filter inputParam: inputParameters){
                clonedParams.add(inputParam.clone());
            }
           
            clonedQuerySchema.setInputParameters(clonedParams);
        }
        return clonedQuerySchema;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((inputParameters == null) ? 0 : inputParameters.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        QuerySchema other = (QuerySchema) obj;
        if (inputParameters == null) {
            if (other.inputParameters != null) {
                return false;
            }
        } else if (!inputParameters.equals(other.inputParameters)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}