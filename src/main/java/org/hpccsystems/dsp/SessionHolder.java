package org.hpccsystems.dsp;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.zkoss.zk.ui.Session;

public class SessionHolder {
    public static final Map<String, Session> OPEN_SESSION = new HashMap<String, Session>();
    
    private SessionHolder(){
    }
    public static void addSession(Session session) {
        HttpSession nativeSession = (HttpSession) session.getNativeSession();
        OPEN_SESSION.put(nativeSession.getId(), session);
    }
    
    public static void removeSession(Session session) {
        HttpSession nativeSession = (HttpSession) session.getNativeSession();
        OPEN_SESSION.remove(nativeSession.getId());
    }
}
