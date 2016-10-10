package org.hpccsystems.dsp.dashboard.entity.widget;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScoredSearchFilter extends Field implements Cloneable{
    
    
    private static final long serialVersionUID = 1L;
    private static final String SPACE = " ";
    private List<String> modifiersList = Arrays.asList("*","/");

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoredSearchFilter.class);
    
    private String operator;
    private String operatorValue;
    private String modifier;
    private String modifierValue;
    
    public ScoredSearchFilter(Field field){
        super(field);
    }
    
    public ScoredSearchFilter(String column, String filterString) {
        setColumn(column);
        StringTokenizer tokenizer = new StringTokenizer(filterString, SPACE);
        String firstToken = tokenizer.nextToken();
        if(modifiersList.contains(firstToken.toString())){
            modifier = firstToken;
            modifierValue = tokenizer.nextToken();
        }else{
            operator = firstToken;
            operatorValue = tokenizer.nextToken();
        }
    }

    public String getOperator() {
        return operator;
    }
    public void setOperator(String operator) {
        this.operator = operator;
    }
    public String getOperatorValue() {
        return operatorValue;
    }
    public void setOperatorValue(String opeartorValue) {
        this.operatorValue = opeartorValue;
    }
    public String getModifier() {
        return modifier;
    }
    public void setModifier(String modifier) {
        this.modifier = modifier;
    }
    public String getModifierValue() {
        return modifierValue;
    }
    public void setModifierValue(String modifierValue) {
        this.modifierValue = modifierValue;
    }
    
    public String getFilterString() {
       StringBuilder filter = new StringBuilder();
       
       if(operator != null && operatorValue != null) {
           filter.append(operator).append(SPACE).append(operatorValue);
       }
       
       if(modifier != null && modifierValue != null) {
           //if operator is added, adding a space
           if(filter.length() > 0) {
               filter.append(SPACE);
           }
           filter.append(modifier).append(modifierValue);
       }
       
       if(LOGGER.isDebugEnabled()) {
           LOGGER.debug("Filter: {} {}", getColumn(), filter.toString());
       }
       
       return filter.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
        result = prime * result + ((modifierValue == null) ? 0 : modifierValue.hashCode());
        result = prime * result + ((modifiersList == null) ? 0 : modifiersList.hashCode());
        result = prime * result + ((operator == null) ? 0 : operator.hashCode());
        result = prime * result + ((operatorValue == null) ? 0 : operatorValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScoredSearchFilter other = (ScoredSearchFilter) obj;
        if (modifier == null) {
            if (other.modifier != null) {
                return false;
            }
        } else if (!modifier.equals(other.modifier)) {
            return false;
        }
        if (modifierValue == null) {
            if (other.modifierValue != null) {
                return false;
            }
        } else if (!modifierValue.equals(other.modifierValue)) {
            return false;
        }
        if (modifiersList == null) {
            if (other.modifiersList != null) {
                return false;
            }
        } else if (!modifiersList.equals(other.modifiersList)) {
            return false;
        }
        if (operator == null) {
            if (other.operator != null) {
                return false;
            }
        } else if (!operator.equals(other.operator)) {
            return false;
        }
        if (operatorValue == null) {
            if (other.operatorValue != null) {
                return false;
            }
        } else if (!operatorValue.equals(other.operatorValue)) {
            return false;
        }
        return true;
    }
    
    @Override
    public ScoredSearchFilter clone() throws CloneNotSupportedException {
        return (ScoredSearchFilter) super.clone();
        
    }
}
