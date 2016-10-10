package org.hpccsystems.dsp.service;

import java.util.List;

import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.exceptions.HipieException;

public interface MigrationService {
    
    List<Composition> migrateToUpstream(List<Composition> compositions, String userId, String sessionId) throws HipieException;

    void upgradeVersion(Composition composition) throws HipieException;
}
