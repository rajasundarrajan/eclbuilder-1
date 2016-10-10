package org.hpccsystems.dsp.dashboard.controller;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Include;
import org.zkoss.zul.Window;

public class DashboardProcessController extends SelectorComposer<Window> {

    private static final long serialVersionUID = 1L;
    private DashboardConfig dashboardConfig;
    Window window;
    
    
    @Wire
    private Include contentInclude;
    
    @Override
    public void doAfterCompose(Window comp) throws Exception {
        super.doAfterCompose(comp);
        
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        contentInclude.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.DASHBOARD);
        contentInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        contentInclude.setSrc("ramps/process/content.zul");
    }
    
    @Listen("onClose = #processWindow")
    public void onCloseProcessTab(Event event){
        event.stopPropagation();
        Events.postEvent(EVENTS.ON_CLOSE_PROCESS_WINDOW, getSelf().getParent(), null);
    }
}
