package org.hpccsystems.dsp.ramps.controller.utils;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class RunComposition implements Runnable {

    private static final String ON_RUN_COMPOSITION_FAILED = "OnRunCompositionFailed";
    private final Desktop desktop;
    private final EventListener<Event> eventListener;
    private final Composition composition;
    private final HPCCConnection hpccCon;
    private final String userID;

    private static final Logger LOGGER = LoggerFactory.getLogger(RunComposition.class);

    public RunComposition(Composition composition, HPCCConnection hpccCon, String userID, Desktop desktop, EventListener<Event> eventListener) {
        this.composition = composition;
        this.hpccCon = hpccCon;
        this.userID = userID;
        this.desktop = desktop;
        this.eventListener = eventListener;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Composition {} by {} is being run", composition.getName(), userID);
            boolean deleteEcl = canDeleteEcl(); 
            LOGGER.info("deleteEcl --->{}",deleteEcl);
            CompositionInstance ci = HipieSingleton.getHipie().runComposition(composition, hpccCon, userID, deleteEcl);
            if (!ci.getCompileErrors().isEmpty()) {
                LOGGER.error(ci.getCompileErrors().toECLErrorString());
                schedule(new Event(ON_RUN_COMPOSITION_FAILED, null, new Exception(ci.getCompileErrors().toECLErrorString())));
                return;
            }
            if (!ci.getRunErrors().isEmpty()) {
                LOGGER.error(ci.getRunErrors().toECLErrorString());
                schedule(new Event(ON_RUN_COMPOSITION_FAILED, null, new Exception(ci.getRunErrors().toECLErrorString())));
                return;
            }
        } catch (Exception e) {
            schedule(new Event(ON_RUN_COMPOSITION_FAILED, null, e));
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        }
        LOGGER.info("Composition {} by {} is successfully ran", composition.getName(), userID);
        schedule(new Event("OnRunCompositionCompleted"));
    }

    private boolean canDeleteEcl() {
        boolean deleteEcl = true;
        
        Element eclElement = RampsUtil.getECLElement(composition);
        
        if (eclElement != null) {
            String keepEcl = eclElement.getOptionValues().iterator().next().getParams().get(0).getName();
            if (Constants.TRUE.equalsIgnoreCase(keepEcl)) {
                deleteEcl = false;
            }
        }
        return deleteEcl;
    }

    private void schedule(Event event) {
        if (desktop.isAlive()) {
            Executions.schedule(desktop, eventListener, event);
        } else {
            LOGGER.warn("Desktop is unavailable to shedule run completion");
        }
    }

}
