package org.hpccsystems.dsp.init;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.ShadowElement;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.ExecutionInit;
import org.zkoss.zk.ui.util.UiLifeCycle;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Timer;

public class TimeoutAlerter implements ExecutionInit, UiLifeCycle {

    private String timerId = "timeoutNotifyTimer";

    public void init(Execution exec, Execution parent) {

        Timer timer = (Timer) exec.getDesktop().getAttribute(timerId);
        if (timer != null) {

            if (isSendbyMsgBox(timer, exec)) {
                return;
            }

            timer.stop();
            timer.start();
        }
    }

    private boolean isSendbyMsgBox(Timer timer, Execution exec) {
        HttpServletRequest hreq = (HttpServletRequest) exec.getNativeRequest();

        for (int j = 0;; ++j) {
            final String uuid = hreq.getParameter("uuid_" + j);
            if (uuid == null) {
                break;
            }

            if (uuid.equals(timer.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public void afterComponentAttached(Component comp, Page page) {
        //Implementation not required
    }

    public void afterComponentDetached(Component comp, Page prevpage) {
        //Implementation not required
    }

    public void afterComponentMoved(Component parent, Component child, Component prevparent) {
        //Implementation not required
    }

    public void afterPageAttached(Page page, Desktop desktop) {
        Object obj = desktop.getAttribute(timerId);
                
        if (obj == null && !page.getRequestPath().endsWith("login.zhtml")) {

            int timeout = desktop.getWebApp().getConfiguration().getSessionMaxInactiveInterval();

            final Timer timer = new Timer((timeout - 10) * 1000);
            int timeoutMins = timeout/60;

            timer.addEventListener(Events.ON_TIMER,
                    event -> Messagebox.show(Labels.getLabel("timeoutAlertMessage1") + " " + timeoutMins + " " + Labels.getLabel("timeoutAlertMessage2"),
                            Labels.getLabel("dspSessionTimeout"), Messagebox.OK, Messagebox.EXCLAMATION, (EventListener<Event>) okEvent -> onTimeOutOk(timer, okEvent)));
            timer.setPage(page);
            desktop.setAttribute(timerId, timer);
            timer.start();
        }
    }

    private void onTimeOutOk(final Timer timer, Event okEvent) {
        if (Messagebox.ON_OK.equals(okEvent.getName())) {
            timer.start();
        }
    }

    public void afterPageDetached(Page page, Desktop prevdesktop) {
        //Implementation not required
    }

    @Override
    public void afterShadowAttached(ShadowElement arg0, Component arg1) {
      //Implementation not required
        
    }

    @Override
    public void afterShadowDetached(ShadowElement arg0, Component arg1) {
      //Implementation not required
        
    }

}
