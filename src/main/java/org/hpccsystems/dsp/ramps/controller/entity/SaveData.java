package org.hpccsystems.dsp.ramps.controller.entity;

import java.io.Serializable;

public class SaveData  implements Serializable{
    private static final long serialVersionUID = 1L;
    private String compositionName;
    private boolean isSaveAs;

    public SaveData(String compositionName, boolean isSaveAs) {
        this.setCompositionName(compositionName);
        this.setSaveAs(isSaveAs);
    }

    public String getCompositionName() {
        return compositionName;
    }

    public void setCompositionName(String compositionName) {
        this.compositionName = compositionName;
    }

    public boolean isSaveAs() {
        return isSaveAs;
    }

    public void setSaveAs(boolean isSaveAs) {
        this.isSaveAs = isSaveAs;
    }
}
