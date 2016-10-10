package org.hpccsystems.dsp.log;

import org.hpcc.HIPIE.CompositionInstance;

public class DataAccess extends DBLog {

   
    private static final long serialVersionUID = 1L;
    public static final String SPRAY = "Spray file";
    public static final String VIEW = "View Output";

    private boolean fileSprayed;
    private String logicalFilename;

    private CompositionInstance compositionInstance;
    private String ddl;

    /**
     * Instantiates a sprayed file log entry with current time, session & user.
     * 
     * @param logicalFilename
     */
    public DataAccess(String logicalFilename,long startTime) {
        super(SPRAY,startTime);
        fileSprayed = true;
        this.logicalFilename = logicalFilename;
    }

    /**
     * Instantiates a view data log entry with current time, session & user.
     * 
     * @param process
     */
    public DataAccess(CompositionInstance compositionInstance, String ddl,long startTime) {
        super(VIEW,startTime);
        this.compositionInstance = compositionInstance;
        this.ddl = ddl;
    }

    @Override
    public String getDetail() {
        StringBuilder builder = new StringBuilder();
        if (fileSprayed) {
            builder.append("Logical filename: ").append(logicalFilename);
        } else {
            builder.append("Workunit ID:").append(compositionInstance.getWorkunitId()).append("\n")

            .append("Created for Composition: ").append(compositionInstance.getCompositionName()).append("\n")

            .append("Visualisation DDL: ").append(ddl).append("\n");
        }

        return builder.toString();
    }

}
