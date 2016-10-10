package org.hpccsystems.dsp.dashboard.controller;

import java.io.Serializable;
import java.util.List;

import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Anchorchildren;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Button;

public class DashboardGrid implements Serializable{
    
    private static final long serialVersionUID = 1L;

    User user=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardGrid.class);

    @Wire("#thumbnailLayout")
    Anchorlayout thumbnailLayout;
    
    private List<Dashboard> dashboards;
    String compositionServiceString = "compositionService";

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {

        Selectors.wireComponents(view, this, false);

        thumbnailLayout.setAttribute(Constants.DASHBOARD, this);
    }

    public List<Dashboard> getDashboards() {
        return dashboards;
    }

    public void setDashboards(List<Dashboard> dashboards) {
        LOGGER.debug("Dashboards - {}, Size -{}", dashboards, dashboards.size());
        dashboards.forEach(this::updateDashboard);
        this.dashboards = dashboards;
    }

    private void updateDashboard(Dashboard dashboard) {
        try {
            ContractInstance visualizationCI = CompositionUtil.getVisualizationContractInstance(dashboard.getComposition());
            dashboard.setCharts(DashboardUtil.retrieveChartInfo(visualizationCI));
        } catch (Exception e) {
            LOGGER.error("Retriving composition failed", e);
        }
    }

    public void setModel(List<Dashboard> dashboards) {
        setDashboards(dashboards);
    }

    @GlobalCommand
    @NotifyChange("dashboards")
    public void updateGridView() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboards ---> {}", dashboards);
        }
    }
    

    @Command
    public void viewDashboard(@BindingParam("gridViewDashboard") Dashboard dashboard) {

        Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, thumbnailLayout, new DashboardConfig(dashboard, Flow.VIEW));

    }

    @Command
    public void editDashboard(@BindingParam("gridViewDashboard") Dashboard dashboard) {
        Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, thumbnailLayout, new DashboardConfig(dashboard, Flow.EDIT));
    }
    
    @Command
    public void  cloneDashboard(@BindingParam("gridCloneDashboard") Dashboard dashboard) throws HipieException {
        try {
            Events.postEvent(EVENTS.ON_CLICK_CLONE, thumbnailLayout, new DashboardConfig(dashboard.clone(), Flow.CLONE, dashboard.getComposition()));
        } catch (CloneNotSupportedException e) {
            LOGGER.error(Constants.EXCEPTION,e);
            throw new HipieException("Unable to clone dashboard", e);
        }
    }

    @Command
    public void selectEntity(@BindingParam("entity") Dashboard dashboard, @ContextParam(ContextType.TRIGGER_EVENT) Event event) {
        if(HipieSingleton.canPromote() && canSelect(dashboard)) {
            Anchorchildren container = (Anchorchildren) event.getTarget();
            
            if(container.getSclass().contains("selected")) {
                Events.postEvent(EVENTS.ON_REMOVE_ENTITY, thumbnailLayout, dashboard);
                container.setSclass("gridAnchor");
            } else {
                Events.postEvent(EVENTS.ON_SELECT_ENTITY, thumbnailLayout, dashboard);
                container.setSclass("gridAnchor selected");
            }
        } else {
            LOGGER.debug("Selection not allowed");
        }
    }
    
    @Command
    @NotifyChange("dashboards")
    public void deleteDashboard(@BindingParam("gridViewDashboard") Dashboard dashboard) throws HipieException {
        Events.postEvent(EVENTS.ON_DELETE_COMPOSITION, thumbnailLayout, dashboard);
    }
    
    @Command
    public void favoriteDashboard(@BindingParam("gridViewDashboard") Dashboard dashboard, @ContextParam(ContextType.COMPONENT) Component component) throws HipieException {
        Button button=(Button) component;
        if(!dashboard.getIsFavourite()) {
            button.setIconSclass("fa fa-star");
        }else {
            button.setIconSclass("fa fa-star-o");
        }
        Events.postEvent(EVENTS.ON_FAV_COMPOSITION, thumbnailLayout, dashboard);
    }
    
    public boolean canSelect(Dashboard dashboard) {
        return user.getId().equals(dashboard.getAuthor()) || user.isGlobalAdmin();
    }
}
