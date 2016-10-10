package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;

public class GlobalVariable implements Serializable{
   
    private static final String USAGE = "usage";
    private static final long serialVersionUID = 1L;
    private String name;
    private String value;
    private String nameToPopulate;
    public static final Map<String, String> GCID_COMPLIANCE_TAGS = new LinkedHashMap<String, String>();

    static {
        GCID_COMPLIANCE_TAGS.put("DATAPERMISSIONMASK","data permission mask");
        GCID_COMPLIANCE_TAGS.put("DATARESTRICTIONMASK","data restriction mask");
        GCID_COMPLIANCE_TAGS.put("SSNMASK","ssn masking");
        GCID_COMPLIANCE_TAGS.put("DLMASK","driver license masking");
        GCID_COMPLIANCE_TAGS.put("DOBMASK","dob masking");
        GCID_COMPLIANCE_TAGS.put("DPPAPURPOSE",USAGE);
        GCID_COMPLIANCE_TAGS.put("GLBPURPOSE",USAGE);
    }
    
    public GlobalVariable(String name, String value) {
        this.name = name;
        this.value = value;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public String getNameToPopulate() {
        return nameToPopulate;
    }
    public void setNameToPopulate(String uIName) {
        nameToPopulate = uIName;
    }

    public static Set<GlobalVariable> extractGlobalVariables(List<Element> inputs) {
        Set<GlobalVariable> variables = new HashSet<>();        
        for (Element inputEle : inputs) {
            if (inputEle.getOption(Element.DEFAULT) != null
                    && !Constants.REFERENCE_ID.equals(inputEle.getName())) {
                GlobalVariable variable = new GlobalVariable(inputEle.getName(),
                        inputEle.getOption(Element.DEFAULT).getParams().get(0).getName());
                
                StringBuilder nameToPopulate = new StringBuilder();
                nameToPopulate.append(Constants.GLOBAL_VAR_PREFIX).append(Constants.GLOBAL).append("|").append(inputEle.getName());
                variable.setNameToPopulate(nameToPopulate.toString());
                
                variables.add(variable);
            }
            
        }
        return variables;
    }
    
    @Override
    public String toString() {
        return "GlobalVariable [name=" + name + ", value=" + value + ", nameToPopulate=" + nameToPopulate + "]";
    }
}
