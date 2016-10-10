package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;


public class ChartConfiguration implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private static final String FA_FA = "fa fa-";
    private ChartType type;
    private String className;
    private String name;
    private String imagePath;
    private String icon;
    private String editLayout;
    private String hipieName;
    private String faIcon;
    
    public ChartConfiguration(ChartType type, String name, String className, String image, String icon,
            String layout, String hipieChartName) {
        this.type = type;
        this.name = name;
        this.className = className;
        this.imagePath = image;
        this.icon = icon;
        this.editLayout = layout;
        this.hipieName = hipieChartName;
        this.faIcon = FA_FA + icon;
    }
    
  
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void setType(ChartType type) {
        this.type = type;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEditLayout(String editLayout) {
        this.editLayout = editLayout;
    }

    public void setHipieName(String hipieName) {
        this.hipieName = hipieName;
    }

    public String getName() {
        return name;
    }
    public String getStaticColorImage() {
        return imagePath+".png";
    }

    public String getEditLayout() {
        return editLayout;
    }

    public ChartType getType() {
        return type;
    }
    
    public String getTypeName() {
        return type.name();
    }

    public String getHipieName() {
        return hipieName;
    }

    public String getClassName() {
        return className;
    }
    
    public String getFaIcon() {
        return faIcon;
    }

    public String getIcon() {
        return icon;
    }
    
    public void setIcon(String icon) {
        this.faIcon = FA_FA + icon;
        this.icon = icon;
    }

}
