package org.hpccsystems.dsp.dashboard.entity.widget;

public class Attribute extends Field {
    private static final long serialVersionUID = 1L;
    private String timeFormat;

    public Attribute(Field field) {
        super(field);
    }

    @Override
    public Attribute clone() throws CloneNotSupportedException {
        Attribute clonedAttribute = (Attribute) super.clone();
        clonedAttribute.setTimeFormat(timeFormat);
        return clonedAttribute;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((timeFormat == null) ? 0 : timeFormat.hashCode());
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
        Attribute other = (Attribute) obj;
        if (timeFormat == null) {
            if (other.timeFormat != null) {
                return false;
            }
        } else if (!timeFormat.equals(other.timeFormat)) {
            return false;
        }
        return true;
    }

}
