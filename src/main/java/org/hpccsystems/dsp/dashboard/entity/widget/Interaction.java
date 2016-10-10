package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;
import java.util.List;

import org.hpccsystems.dsp.dashboard.Dashboard.ACTION;

public class Interaction implements Cloneable , Serializable{
   
    private static final long serialVersionUID = 1L;
    private ACTION action;
    private Field field;
    private List<InteractionTarget> targets;
    private Widget sourceWidget;    
    /**
     * This flag will be used in interactivity screen to operate save and cancel
     * button
     */
    private boolean discard;
    
    public Interaction(ACTION action, Field field, Widget widget) {
        super();
        this.action = action;
        this.field = field;
        this.sourceWidget = widget;
        widget.addInteractivitySource(this);
    }

    public Interaction(Widget widget) {
        super();
        this.sourceWidget = widget;
        widget.addInteractivitySource(this);
    }  
    
    public ACTION getAction() {
        return action;
    }
    public void setAction(ACTION action) {
        this.action = action;
    }
    public Field getField() {
        return field;
    }
    public void setField(Field field) {
        this.field = field;
    }
    public List<InteractionTarget> getTargets() {
        return targets;
    }
    public void setTargets(List<InteractionTarget> targets) {
        this.targets = targets;
    }
    public Widget getSourceWidget() {
        return sourceWidget;
    }
    public void setSourceWidget(Widget widget) {
        this.sourceWidget = widget;
    }

    @Override
    public Interaction clone() throws CloneNotSupportedException {
        return (Interaction)super.clone();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((action == null) ? 0 : action.hashCode());
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((targets == null) ? 0 : targets.hashCode());
        result = prime * result + ((sourceWidget == null) ? 0 : sourceWidget.hashCode());
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
        Interaction other = (Interaction) obj;
       
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else if (!field.getColumn().equals(other.field.getColumn())) {
            return false;
        }
        if (targets == null) {
            if (other.targets != null) {
                return false;
            }
        } else if (!targets.equals(other.targets)) {
            return false;
        }
        if (sourceWidget == null) {
            if (other.sourceWidget != null) {
                return false;
            }
        } else if (!sourceWidget.equals(other.sourceWidget)) {
            return false;
        }
        return true;
    }

    /**
     * Remove the source object from Source widget.And removes the target object from the Targets
     */
    public void delete() {
        this.getSourceWidget().removeSource(this);
        for (InteractionTarget target : this.getTargets()) {
            //Target, which is in Interaction object holds the actual target Widget.
            //Where as the Targets in Widget holds the widget to which it is being target(ie, source widget)
            //So to remove target from Widget, need to check against sourceWidget
            if(target != null &&  target.getWidget() != null){
                target.getWidget().removeTarget(target, getSourceWidget());
            }
        }
        
    }

    @Override
    public String toString() {
        return "Interaction [action=" + action + ", field=" + field
                + ", targets=" + targets + ", sourceWidget=" + sourceWidget.getName() != null?sourceWidget.getName():null
                + "]";
    }

    public boolean canDiscard() {
        return discard;
    }

    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    public void removeSource() {
        this.getSourceWidget().removeSource(this);
    }


}
