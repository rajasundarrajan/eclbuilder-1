package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;


public class Company implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer gcId;
    
    public Company(String companyName, Integer gcId) {
        this.name = companyName;
        this.gcId = gcId;
    }
    public Company() {
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getGcId() {
        return gcId;
    }
    public void setGcId(Integer gcId) {
        this.gcId = gcId;
    }

}
