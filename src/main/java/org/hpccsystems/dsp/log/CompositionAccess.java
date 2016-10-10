package org.hpccsystems.dsp.log;

import org.hpcc.HIPIE.Composition;

public class CompositionAccess extends DBLog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public static final String SAVE = "Save Composition"; 
    public static final String RUN = "Run Composition"; 
    public static final String DELETE = "Delete Composition"; 
    
    private Composition composition;
    private boolean isPermissionUpdate;
    
    /**
     * Instantiates a log entry with current time, session & user.
     * Use this only to log permissions and not the composition.
     * @param composition
     */
    public CompositionAccess(Composition composition,long startTime) {
        super("Permission Update", startTime);
        this.isPermissionUpdate = true;
        this.composition = composition;
    }
    
    /**
     * Instantiates a log entry with current time, session & user.
     * @param action
     *  Composition action SAVE, RUN or DELETE
     * @param COMPOSITION
     *  Composition accessed
     */
    public CompositionAccess(String action, Composition savedComposition, long startTime) {
        super(action,startTime);
        this.composition = savedComposition;
    }

    @Override
    public String getDetail() {
        StringBuilder builder = new StringBuilder();
        builder.append("Composition name: ")
            .append(composition.getCanonicalName())
            .append("\n");
        
        builder.append("Repository: ")
            .append(composition.getRepositoryName())
            .append("\n");
         
        if(isPermissionUpdate) {
            builder.append("Updated permissions:\n")
                .append(composition.getPermissions());
        } else {
            builder.append("Contents:\n")
                .append(composition.toString());
        }
        
        return builder.toString();
     }

}
