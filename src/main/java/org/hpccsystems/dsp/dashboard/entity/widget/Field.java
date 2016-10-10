package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;
import java.util.Optional;

import org.hpccsystems.dsp.Constants;

public class Field implements Cloneable , Serializable{
   
    private static final long serialVersionUID = 1L;

    private String column;
    private String dataType;
    private boolean rowCount;
    private String dudName;
    private String displayName;

    public Field() {
    }

    public Field(String column, String dataType) {
        this.column = column;
        this.dataType = dataType;
    }
    
    public Field(Field field) {
        this.column = field.column;
        this.dataType = field.dataType;
        this.rowCount = field.rowCount;
        this.displayName = field.displayName;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    
    public boolean isRowCount() {
        return rowCount;
    }

    public void setRowCount(boolean rowCount) {
        this.rowCount = rowCount;
    }

    

    public boolean isNumeric() {
        Optional<String> optionalDatatype = Optional.ofNullable(dataType);
        if (optionalDatatype.isPresent()) {
            String lowerCaseType = optionalDatatype.get().trim().toLowerCase();
            if (lowerCaseType.contains("integer") || lowerCaseType.contains("real") || lowerCaseType.contains("decimal")
                    || lowerCaseType.contains("unsigned")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Field)) {
            return false;
        }
        Field other = (Field) obj;
        if (column == null) {
            if (other.column != null) {
                return false;
            }
        } else if (!column.equals(other.column)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((column == null) ? 0 : column.hashCode());
        result = prime * result
                + ((displayName == null) ? 0 : displayName.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Field [column=" + column + ", dataType=" + dataType + "]";
    }
    
    @Override
    public Field clone() throws CloneNotSupportedException {
        return (Field) super.clone();
    }

    public String getDudName() {
        return isRowCount() ? Constants.COUNT:dudName;
    }

    public void setDudName(String dudName) {
        this.dudName = dudName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

}
