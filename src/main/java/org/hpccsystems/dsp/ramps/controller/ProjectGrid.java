package org.hpccsystems.dsp.ramps.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
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

public class ProjectGrid implements Serializable {

    private static final long serialVersionUID = 1L;
    User user=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectGrid.class);

    @Wire("#thumbnailLayout")
    Anchorlayout thumbnailLayout;

    private List<Project> projects;

    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {

        Selectors.wireComponents(view, this, false);

        thumbnailLayout.setAttribute(Constants.PROJECT, this);
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        List<Plugin> plugins;
        List<Plugin> pluginsWithoutVisualization;
        for (Project project : projects) {
            plugins = project.getPlugins();
            pluginsWithoutVisualization = new ArrayList<>();
            LOGGER.debug("Project: {} \nPlugins: {}", project, plugins);
            // Removing Visualization plugin from the plugins list.
            for (Plugin plugin : plugins) {

                if (plugin instanceof DatasetPlugin
                        || !plugin.getContractInstance().getContract().getRepositoryName().equals(Dashboard.DASHBOARD_REPO)) {
                    pluginsWithoutVisualization.add(plugin);
                }
            }
            project.setFilteredPlugins(pluginsWithoutVisualization);
        }
        this.projects = projects;
    }

    public void setModel(List<Project> projects) {
        setProjects(projects);
    }

    @GlobalCommand
    @NotifyChange("projects")
    public void updateGridView() {
        LOGGER.debug("Projects ---> {}", projects);
    }

    @Command
    public void viewProject(@BindingParam("gridViewProject") Project project) {

        Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, thumbnailLayout, new TabData(project, Flow.VIEW));

    }

    @Command
    public void editProject(@BindingParam("gridViewProject") Project project) {
        Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, thumbnailLayout, new TabData(project, Flow.EDIT));
    }

    @Command
    public void cloneProject(@BindingParam("gridViewProject") Project project) throws HipieException {
        try {
            Events.postEvent(EVENTS.ON_CLICK_CLONE, thumbnailLayout, new TabData(project.clone(), Constants.Flow.CLONE, project.getComposition()));
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Unable to clone project", e);
        }
    }

    @Command
    public void deleteProject(@BindingParam("gridViewProject") Project project) throws HipieException {
        Events.postEvent(EVENTS.ON_DELETE_COMPOSITION, thumbnailLayout, project);
    }
    
    @Command
    public void selectEntity(@BindingParam("entity") Project project, @ContextParam(ContextType.TRIGGER_EVENT) Event event) {
        if(HipieSingleton.canPromote() && canSelect(project)) {
            Anchorchildren container = (Anchorchildren) event.getTarget();
            
            if(container.getSclass().contains("selected")) {
                Events.postEvent(EVENTS.ON_REMOVE_ENTITY, thumbnailLayout, project);
                container.setSclass("gridAnchor");
            } else {
                Events.postEvent(EVENTS.ON_SELECT_ENTITY, thumbnailLayout, project);
                container.setSclass("gridAnchor selected");
            }
        } else {
            LOGGER.debug("Selection not allowed");
        }
    }
    
    @Command
    public void favoriteProject(@BindingParam("gridViewProject") Project project, @ContextParam(ContextType.COMPONENT) Component component) throws HipieException {
        Button button=(Button) component;
        if(!project.getIsFavourite()) {
            button.setIconSclass("fa fa-star");
        }else {
            button.setIconSclass("fa fa-star-o");
        }
        Events.postEvent(EVENTS.ON_FAV_COMPOSITION, thumbnailLayout, project);
    }
    
    private boolean canSelect(Project project) {
        return user.getId().equals(project.getAuthor()) || user.isGlobalAdmin();
    }

}
