package org.hpccsystems.dsp.ramps.controller.entity;

import java.io.Serializable;

import org.hpccsystems.dsp.ramps.entity.BooleanResult;

public class FileBrowserData  implements Serializable{
    private static final long serialVersionUID = 1L;
    private BooleanResult result;
    private boolean notifyUser = true;
    private String action;
    
    public boolean isNotifyUser() {
        return notifyUser;
    }
    public void setNotifyUser(boolean notifyUser) {
        this.notifyUser = notifyUser;
    }
    public BooleanResult getResult() {
        return result;
    }
    public void setResult(BooleanResult result) {
        this.result = result;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
}
