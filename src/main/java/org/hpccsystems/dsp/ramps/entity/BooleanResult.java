package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;

public class BooleanResult implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private boolean flag;

    public boolean isSuccess() {
        return flag;
    }

    public void setResult(boolean result) {
        this.flag = result;
    }

    public void setSuccess() {
        this.flag = true;
    }

    public void setSuccess(boolean result) {
        if (result) {
            setSuccess();
        } else {
            setFaliure();
        }
    }

    public void setFaliure() {
        this.flag = false;
    }

    public void setFaliure(boolean result) {
        if (result) {
            setFaliure();
        } else {
            setSuccess();
        }
    }
}
