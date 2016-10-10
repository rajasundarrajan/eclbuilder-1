package org.hpccsystems.dsp.ramps.controller.entity;

import java.io.Serializable;

public class SprayProgress  implements Serializable{
    private static final long serialVersionUID = 1L;
    private int progress;
    private String status;
    
    public SprayProgress(int progress, String status) {
        this.progress = progress;
        this.status = status;
    }
    
    public int getProgress() {
        return progress;
    }
    public void setProgress(int progress) {
        this.progress = progress;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
