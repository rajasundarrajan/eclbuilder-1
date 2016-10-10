package org.hpccsystems.dsp.dashboard.entity.widget;

import java.util.List;
import java.util.Optional;

import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;

public class SortField extends Field {
   
    private static final long serialVersionUID = 1L;
    private boolean isAscending;
    private AGGREGATION aggregation;

    public SortField(Field field) {
        super(field);
        isAscending = true;
        aggregation = null;
    }

    public SortField() {
        isAscending = true;
    }

    public boolean isAscending() {
        return isAscending;
    }

    public void setAscending(boolean isAscending) {
        this.isAscending = isAscending;
    }

    public Measure getEquivalentMeasure(List<Measure> fields) {
        Optional<Measure> measure = fields.stream().filter(field -> field.getColumn().equals(this.getColumn()) && field.getAggregation() == this.getAggregation())
                .findFirst();
        return measure.isPresent() ? measure.get() : null;
    }

    @Override
    public SortField clone() throws CloneNotSupportedException {
        SortField sortField = (SortField) super.clone();
        sortField.setAscending(isAscending());
        sortField.setAggregation(getAggregation());
        return sortField;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((aggregation == null) ? 0 : aggregation.hashCode());
        result = prime * result + (isAscending ? 1231 : 1237);
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
        SortField other = (SortField) obj;
        if (aggregation != other.aggregation) {
            return false;
        }
        boolean returnString = true;
        if (isAscending != other.isAscending) {
            returnString = false;
        }
        return returnString;
    }

    public AGGREGATION getAggregation() {
        return aggregation;
    }

    public void setAggregation(AGGREGATION aggregation) {
        this.aggregation = aggregation;
    }

    public boolean isNotNone() {
        return this.getAggregation() != null && this.getAggregation() != AGGREGATION.NONE;
    }

}
