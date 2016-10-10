package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;

public class InteractionTarget implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private Field field;
    private Widget widget; 
    /**
     * This flag will be used in interactivity screen to operate save and cancel
     * button
     */
    private boolean discard; 
    
    public InteractionTarget(Widget widget) {
        super();
        this.widget = widget;
        widget.addInteractivityTarget(this);
    }
    
    public InteractionTarget(Field field, Widget targetWidget,Widget sourceWidget) {
        super();
        this.field = field;
        this.widget = targetWidget;
        //Since the Widget object holds InteractionTarget object with the widget to which it is being target,
        //creating new InteractionTarget object with sourceWidget, and add it to target list 
        if(targetWidget != null){
            targetWidget.addInteractivityTarget(new InteractionTarget(field,sourceWidget));
        }
    }

    public InteractionTarget(Field field, Widget sourceWidget) {
        super();
        this.field = field;
        this.widget = sourceWidget;
    }

    public Field getField() {
        return field;
    }
    public void setField(Field field) {
        this.field = field;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((getWidget() == null) ? 0 : getWidget().hashCode());
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
        InteractionTarget other = (InteractionTarget) obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.getColumn().equals(other.field.getColumn())) {
            return false;
        }
        if (getWidget() == null) {
            if (other.getWidget() != null) {
                return false;
            }
        } else if (!getWidget().equals(other.getWidget())) {
            return false;
        }
        return true;
    }
    
    public Widget getWidget() {
        return widget;
    }

    @Override
    public String toString() {
        return "InteractionTarget [field=" + field + ", widget=" + widget.getName() != null ? widget.getName():null + "]";
    }

    public boolean canDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public void delete(Widget sourceWidget) {        
        getWidget().removeTarget(this,sourceWidget);
    }
    

}
