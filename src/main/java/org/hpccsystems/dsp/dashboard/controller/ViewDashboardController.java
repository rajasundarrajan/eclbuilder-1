package org.hpccsystems.dsp.dashboard.controller;

import org.hpcc.HIPIE.CompositionInstance;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Include;
import org.zkoss.zul.Tab;
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ViewDashboardController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    
    @Wire
    private Include viewInclude;
    
    private DashboardConfig dashboardConfig;

    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        CompositionInstance compositionInstance = (CompositionInstance) Executions.getCurrent().getArg().get(Dashboard.MOST_RECENT_CI);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        Tab projectTab = (Tab) Executions.getCurrent().getArg().get(Constants.PROJECT_TAB);
        viewInclude.setDynamicProperty(Dashboard.MOST_RECENT_CI, compositionInstance);
        viewInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        viewInclude.setDynamicProperty(Constants.PROJECT_TAB, projectTab);
        viewInclude.setSrc("dashboard/dashboardOutputs.zul");
        this.getSelf().addEventListener(Events.ON_CLOSE, event->{
           Events.postEvent(EVENTS.ON_RETURN_TO_EDIT, dashboardConfig.getRampsContainer(), null);
           Events.postEvent(Events.ON_CLOSE, viewInclude, null); 
        });
    }
}
