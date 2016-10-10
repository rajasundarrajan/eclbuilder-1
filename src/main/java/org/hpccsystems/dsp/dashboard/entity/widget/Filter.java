package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;

public class Filter extends Field implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private String value = "";
    private String newValue = "";
    private boolean isGlobal;
    private boolean checkValue;

    public Filter(Field field) {
        super(field);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Filter clone() throws CloneNotSupportedException {
        Filter filter = (Filter) super.clone();
        filter.setValue(getValue());
        filter.setNewValue(getNewValue());
        filter.setGlobal(isGlobal());
        return filter;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        Filter other = (Filter) obj;
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        // Check to see if the value is changed from the previously saved value
        if(this.getValue() != null &&  other.getValue() != null && !this.getValue().equalsIgnoreCase(other.getValue())) {
        	return false;
        }

        /*in widget's Edit flow alone need to check the filter value against the original filter value.
         * In all other widget/filter comparison just check the column name alone
         */
         if(this.canCheckValue() || other.canCheckValue()){
            if (!super.equals(obj)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
        }else{
            if (!super.equals(obj)) {
                return false;
            }
        }
        return true;
    }


    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    @Override
    public String toString() {
        return "Filter [value=" + value + ", isGlobal=" + isGlobal
                + ", getColumn()=" + getColumn() + "]";
    }

    public boolean canCheckValue() {
        return checkValue;
    }

    public void setCheckValue(boolean checkValue) {
        this.checkValue = checkValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    
}
