package org.hpccsystems.dsp.init;

import org.hpccsystems.dsp.SessionHolder;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.SessionCleanup;

public class DSPSessionCleanup implements SessionCleanup {

    @Override
    public void cleanup(Session sess) throws Exception {
        SessionHolder.removeSession(sess);
    }

}
