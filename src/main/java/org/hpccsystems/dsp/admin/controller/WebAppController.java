package org.hpccsystems.dsp.admin.controller;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.SessionHolder;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;

public class WebAppController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppController.class);

    @Wire
    private Intbox sessionTimeout;
    @Wire
    private Checkbox isDevMode;
    @Wire
    private Checkbox isMaintenanceMode;
    @Wire
    private Intbox sprayRetryCount;
    @Wire
    private Intbox fileSize;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        int timeout = comp.getDesktop().getWebApp().getConfiguration().getSessionMaxInactiveInterval();
        sessionTimeout.setText(String.valueOf(timeout));
    }

    private SettingsService getService() {
        return (SettingsService) SpringUtil.getBean("settingsService");
    }

    @Listen("onClick = #save")
    public void updateConfigProperties() {
        int timeout = sessionTimeout.getValue();
        int minValue = 120;

        if (timeout < minValue) {
            Clients.showNotification(Labels.getLabel("warnMinTimeout"), Clients.NOTIFICATION_TYPE_ERROR, sessionTimeout,
                    Constants.POSITION_END_CENTER, 1500, true);
            return;
        }

        getSelf().getDesktop().getWebApp().getConfiguration().setSessionMaxInactiveInterval(timeout);
        getService().updateSessionTimeout(timeout);
        // TODO propagate session timeout values to all clusters

        if (isDevMode.isChecked()) {
            getService().enableDevMode();
        } else {
            getService().disableDevMode();
        }

        if (isMaintenanceMode.isChecked()) {
            getService().enableMaintenanceMode();
        } else {
            getService().disableMaintenanceMode();
        }

        getService().updateSprayRetryCount(sprayRetryCount.getValue());

        getService().updateStaticDataSize(fileSize.getValue());

        LOGGER.debug("Updated everything..");
        Clients.showNotification(Labels.getLabel("savedConfigInfo"), Clients.NOTIFICATION_TYPE_INFO, getSelf(),
                Constants.POSITION_MIDDLE_CENTER, 3000, true);
    }

    @Listen("onClick = #reloadHipie")
    public void reloadHIPIE() {
        LOGGER.debug("Available active sessions - {} \n Sessions in Static variable - {}", Sessions.getCount(),
                SessionHolder.OPEN_SESSION.size());

        Messagebox.show(Labels.getLabel("reloadWarning"), Labels.getLabel("admReloadHipie"),
                new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,
                this::confirmHipieReload);
    }

    private void confirmHipieReload(ClickEvent event) {
        try {
            if (Messagebox.Button.YES.equals(event.getButton())) {
                HipieSingleton.reloadHIPIE();

                SessionHolder.OPEN_SESSION.values().forEach(session -> session.invalidate());
                SessionHolder.OPEN_SESSION.clear();
                Executions.sendRedirect("/login.zhtml");
            }
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
           Clients.showNotification(Labels.getLabel("hipieReloadError") +" " +e.getMessage()+" ," + Labels.getLabel("configureandreload"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
        }
    }

    @Listen("onClick = #refreshRepository")
    public void refreshRepository() {
        Messagebox.show(Labels.getLabel("refreshRepositoryWarning"), Labels.getLabel("admRefreshRepository"),
                new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,
                this::confirmrefreshRepository);
    }

    private void confirmrefreshRepository(ClickEvent event)  {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            try {
                HipieSingleton.getHipie().refreshData();
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);             
                Clients.showNotification(Labels.getLabel("repoRefreshError")+" " +e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            }
           Clients.showNotification(Labels.getLabel("refreshRepositoryComplete"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    public boolean isDevModeEnabled() {
        return getService().isDevEnabled();
    }

    public boolean isMaintenanceModeEnabled() {
        return getService().isMaintenanceEnabled();
    }

    public int getSprayRetryCount() {
        return getService().getSprayRetryCount();
    }

    public int getStaticFileSize() {
        return getService().getStaticDataSize();
    }
}
