package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class OutputSchema implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private String name;

    private List<Field> fields;
    private String xPath;
    
    private String dudName;
    
    private static final String PATH = "Results/Result/Dataset[@xmlns=\"";
    public static final String ROW = "\"]/Row";
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public String getxPath() {
        return xPath;
    }

    /**
     * Sets xPath and Extracts name field from xPath
     * @param xPath
     */
    public void setxPath(String xPath) {
        this.xPath = xPath;
        updateNameFromXpath();
    }

    public String getDudName() {
        return dudName;
    }

    public void setDudName(String dudName) {
        this.dudName = dudName;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OutputSchema [name=").append(name).append(", fields=").append(fields).append(", xPath=").append(xPath).append(", dudName=")
                .append(dudName).append("]");

        return builder.toString();
    }

    /**
     * Sets xPath and Name of Outputschema
     * @param namespace
     */
    public void setNamespace(String namespace) {
        this.name = extractName(namespace);
        this.xPath = PATH + namespace + ROW;
    }
    
    private void updateNameFromXpath() {
        String namespace = StringUtils.substringBetween(xPath, PATH, ROW);
        this.name = extractName(namespace);
    }

    private String extractName(String namespace) {
        return namespace.substring(namespace.lastIndexOf(':') + 1);
    }
}