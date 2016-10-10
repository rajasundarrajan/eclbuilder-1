package org.hpccsystems.dsp.dashboard.entity;

import java.io.Serializable;
import java.sql.Date;

public class Dermatology implements Serializable {

    private static final long serialVersionUID = 1L;

    private String compositionId;
    private String compositionVersion;
    private String userId;
    private int gcid;
    private String ddl;
    private String layout;
    private Date modifiedDate;

    public String getCompositionId() {
        return compositionId;
    }

    public void setCompositionId(String compositionId) {
        this.compositionId = compositionId;
    }

    public String getCompositionVersion() {
        return compositionVersion;
    }

    public void setCompositionVersion(String compositionVersion) {
        this.compositionVersion = compositionVersion;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getGcid() {
        return gcid;
    }

    public void setGcid(int i) {
        this.gcid = i;
    }

    public String getDdl() {
        return ddl;
    }

    public void setDdl(String ddl) {
        this.ddl = ddl;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date date) {
        this.modifiedDate = date;
    }

}
