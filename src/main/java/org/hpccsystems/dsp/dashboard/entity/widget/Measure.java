package org.hpccsystems.dsp.dashboard.entity.widget;

public class Measure extends Field implements Cloneable {

    private static final long serialVersionUID = 1L;
    private AGGREGATION aggregation;

    public enum AGGREGATION {
        SUM, MIN, MAX, AVE, NONE;
    }

    public Measure() {
    }

    public Measure(Measure measure) {
        this.setColumn(measure.getColumn());
        this.setAggregation(measure.getAggregation());
        this.setDisplayName(measure.getDisplayName());
        this.setDataType(measure.getDataType());
        this.setRowCount(measure.isRowCount());
    }

    public Measure(Field field) {
        super(field);
        this.setDisplayName(field.getColumn());
        this.setAggregation(AGGREGATION.NONE);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((aggregation == null) ? 0 : aggregation.hashCode());
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
        if (obj instanceof SortField) {
            return super.equals(obj);
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Measure other = (Measure) obj;
        boolean returnBool = true;
        if (aggregation != other.aggregation) {
            returnBool = false;
        }
        return returnBool;
    }

    public AGGREGATION getAggregation() {
        return aggregation;
    }
    
    /**
     * @return
     *  null when aggregation is NONE
     *  Name of Aggregation otherwise
     */
    public String getAggregationString() {
        return Measure.AGGREGATION.NONE != aggregation ? aggregation.name() : null;
    }

    public void setAggregation(AGGREGATION aggregation) {
        this.aggregation = aggregation;
    }

    @Override
    public Measure clone() throws CloneNotSupportedException {
        return (Measure) super.clone();
    }

}
