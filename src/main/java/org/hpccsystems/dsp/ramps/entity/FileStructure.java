package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.zkoss.util.resource.Labels;

public class FileStructure implements Serializable{
   
    private static final long serialVersionUID = 1L;
    public static enum Type {
        FLAT(Labels.getLabel("fixedDesc")),
        CSV(Labels.getLabel("delimitedDesc")),
        XML(Labels.getLabel("xml"));
        
        private final String label;
        
        private Type(String label) {
            this.label = label;
        }
        
        @Override
        public String toString() {
            return label;
        }
        
    }
    
    private String columnName;
    private String columnType;
    private int columnSize;
    private String xpath;
    
    public String getColumnName() {
        return columnName;
    }
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public String getColumnType() {
        return columnType;
    }
    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }
    public int getColumnSize() {
        return columnSize;
    }
    public void setColumnSize(int columnSize) {
        this.columnSize = columnSize;
    }
    
    public String getAsString() {
        StringBuilder eclString = new StringBuilder();
        eclString.append(columnType);
        if(columnSize > 0) {
            eclString.append(columnSize); 
        }
        eclString.append(" ").append(columnName);
        
        if(xpath != null){
            xpath = xpath.trim();
            if(xpath.startsWith("{XPATH(")){
                eclString.append(" ").append(xpath);
            } else if(xpath.startsWith("XPATH(")) {
                eclString.append(" ").append("{").append(xpath).append("}");
            } else if(!xpath.contains("XPATH")) {
                eclString.append(" ").append("{XPATH('").append(xpath).append("')}");
            }
        }
        
        return eclString.toString();
    }
    
    public boolean isValid() {
        return !StringUtils.isEmpty(getColumnName()) && !StringUtils.isEmpty(getColumnType());
    }
    
    public static List<Type> getSprayTypes() {
        List<Type> types = new ArrayList<Type>();
        types.addAll(Arrays.asList(Type.values()));
        return types;
    }
    public String getXpath() {
        return xpath;
    }
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

}
