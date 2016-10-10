package org.hpccsystems.dsp.ramps.entity;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.init.HipieSingleton;

public class ClusterConfig implements Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for HPCCConnection stored by HIPIE
     */
    private String id;

    private String thorCluster;
    private String roxieCluster;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThorCluster() {
        return thorCluster;
    }

    public void setThorCluster(String thorCluster) {
        this.thorCluster = thorCluster;
    }

    public String getRoxieCluster() {
        return roxieCluster;
    }

    public void setRoxieCluster(String roxieCluster) {
        this.roxieCluster = roxieCluster;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public HPCCConnection getConnection() {
        HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnection(id);
        if(connection == null){
            return null;
        }
        connection.setThorCluster(thorCluster);
        
        return connection;
    }

}
