package org.hpccsystems.dsp.ramps.controller.utils;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.log.Promotion;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class CompositionPromoter implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionPromoter.class);
    
    private List<Composition> compositions;
    private MigrationService migrationService;
    private Desktop desktop;
    private EventListener<Event> eventListener;
    private String userId;
    private String sessionId;
    DBLogger dbLogger;
    
    public CompositionPromoter(List<Composition> compositions, MigrationService migrationService, Desktop desktop,EventListener<Event> eventListener, String userId, String sessionId) {
        this.compositions = compositions;
        this.migrationService = migrationService;
        this.desktop = desktop;
        this.eventListener = eventListener;
        this.userId = userId;
        this.sessionId = sessionId;
    }
    
    @Autowired
    public void setDBLogger(DBLogger dbLogger) {
        if (LOGGER.isDebugEnabled()) {
            this.dbLogger = dbLogger;
        }
    }
    
    @Override
    public void run() {
        LOGGER.info("Promoting compositions");
        long startTime = Instant.now().toEpochMilli();
        
        List<Composition> migrationFailedComps;
        try {
            migrationFailedComps = migrationService.migrateToUpstream(compositions, userId, sessionId);
        } catch (Exception e) {
            LOGGER.error(Constants.ERROR, e);
            Executions.schedule(desktop, eventListener,new Event(Constants.EVENTS.ON_PROMOTION_FAIL, null, e.getMessage()));
            return;
        }
        
        LOGGER.debug("Migration to upstream complete");
        
        if(CollectionUtils.isNotEmpty(migrationFailedComps)) {
             if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new Promotion(sessionId, userId, startTime, Promotion.MIGRATION_FAILIURE, migrationFailedComps));
             }
        }
        
        Set<Composition> failedCompositions = new LinkedHashSet<>();
        for (Composition composition : compositions) {
            try {
                migrationService.upgradeVersion(composition);
            } catch (HipieException e) {
                failedCompositions.add(composition);
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }
        
        LOGGER.debug("Upgrading local version complete");
        
        if(CollectionUtils.isNotEmpty(failedCompositions)) {
             if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new Promotion(sessionId, userId, startTime, Promotion.UPGRADE_FAILIURE, failedCompositions));
             }
        }
        
        failedCompositions.addAll(migrationFailedComps);
        
        Executions.schedule(desktop, eventListener,new Event(Constants.EVENTS.ON_PROMOTION_COMPLETE, null, failedCompositions));
        LOGGER.info("Promotion complete");
    }

}
