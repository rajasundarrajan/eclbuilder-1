package org.hpccsystems.dsp.log;

import java.util.Collection;
import java.util.Iterator;

import org.hpcc.HIPIE.Composition;

public class Promotion extends DBLog {

    private static final long serialVersionUID = 1L;
    public static final String MIGRATION_FAILIURE = "Promotion: Migrate Failed";
    public static final String UPGRADE_FAILIURE = "Promotion: Upgrade Failed";

    private String message;

    public Promotion(String sessionId, String userId, long startTime, String action, Collection<Composition> comps) {
        super(sessionId, userId, startTime, action);
        this.message = createList(comps).toString();
    }

    @Override
    public String getDetail() {
        return message;
    }
    
    private StringBuilder createList(Collection<Composition> failedComps) {
        Iterator<Composition> iterator = failedComps.iterator();
        StringBuilder comps = new StringBuilder();
        while (iterator.hasNext()) {
            comps.append(iterator.next().getName());
            if(iterator.hasNext()) {
                comps.append("\n" + iterator.next().getName());
            }
        }
        return comps;
    }
}