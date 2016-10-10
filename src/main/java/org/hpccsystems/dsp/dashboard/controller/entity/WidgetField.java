package org.hpccsystems.dsp.dashboard.controller.entity;

import java.io.Serializable;

import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;

public class WidgetField implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private Widget widget;
    private Field field;
    
    public WidgetField(Widget widget) {
        this.widget = widget;
    }

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public Field getField() {
        return field;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((widget == null) ? 0 : widget.hashCode());
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
        WidgetField other = (WidgetField) obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.equals(other.field)) {
            return false;
        }
        if (widget == null) {
            if (other.widget != null) {
                return false;
            }
        } else if (!widget.equals(other.widget)) {
            return false;
        }
        return true;
    }

    public void setField(Field field) {
        this.field = field;
    }
}
