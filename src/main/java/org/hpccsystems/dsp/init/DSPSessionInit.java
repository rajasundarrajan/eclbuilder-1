package org.hpccsystems.dsp.init;

import org.hpccsystems.dsp.SessionHolder;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.util.SessionInit;

public class DSPSessionInit implements SessionInit {

    @Override
    public void init(Session sess, Object request) throws Exception {
        SessionHolder.addSession(sess);
    }

}
