package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;

public class ChartInfo implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private String name;
    private String icon;
            
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }

}
