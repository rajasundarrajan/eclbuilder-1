package org.hpccsystems.dsp.ramps.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.Constants.VIEW;
import org.hpccsystems.dsp.GridEntity;
import org.hpccsystems.dsp.HomeComposer;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.component.renderer.CompositionRowRenderer;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.error.HError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Menubar;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class HomeController extends HomeComposer {
    private static final String DELETE_COMPOSITION_DIALOGUE = "deleteCompositionDialogue";
    private static final long serialVersionUID = 1L;
    private static final String FA_FA_SORT_AMOUNT_DESC = "fa fa-sort-desc";
    private static final String FA_FA_SORT_AMOUNT_ASC = "fa fa-sort-asc";
    private static final String NEW_PROJECT_URI = "/ramps/project/new_project.zul";
    private static final String PROJECT_STRING = "projects";
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    private static final String PROMOTE_CONFIRMATION_MESSAGE = "promoteProceedmessage";
    private static final String PROMOTE_CONFIRMATION_HEADER = "promoteProceed";
    
    @Wire
    private Tabbox homeTabbox;

    @Wire
    private Textbox textBox;

    @Wire
    private Listbox listBox;

    @Wire
    private Button clearFilter;

    @Wire
    private Popup popup;

    private ErrorBlock error;

    @Wire
    private Tab homeTab;

    @Wire
    private Listhead labelHead;

    @Wire
    private Radiogroup viewSelectRadioGroup;

    @Wire
    private Radio toggleGridView;

    @Wire
    private Radio toggleListView;

    @Wire
    private Menuitem authorItem;

    @Wire
    private Menuitem dateItem;

    @Wire
    private Menuitem nameItem;

    @Wire
    private Menubar sortMenuBar;
    
    private User user;
    private List<Project> projects;
    private List<Project> gridProjects = new ArrayList<Project>();
    private ListModelList<Project> modelList = new ListModelList<Project>();
    private Map<String, Set<Object>> filters = new HashMap<String, Set<Object>>();
    private ProjectGrid projectGrid;
    private static final String ASC = "asc";
    private static final String DES = "des";
    private static final String GRID_SORT_TYPE = "gridSortType";
    private List<String> favProjects;

    @SuppressWarnings("unchecked")
    private EventListener<Event> addFilterListener = event -> {
        Map<String, Set<Object>> data = (Map<String, Set<Object>>) event.getData();
        RampsUtil.filterData(filters, modelList, projects, data);
    };

    EventListener<Event> projectAddListener = event -> {
        Clients.showBusy(entityList, Labels.getLabel("refreshingProjects"));
        Project addedProject = (Project) event.getData();
        modelList.removeAll(projects);
        projects.add(addedProject);
        modelList.addAll(projects);
        gridProjects.add(addedProject);
        entityList.getColumns().getChildren().clear();
        projectGrid = (ProjectGrid) thumbnailLayout.getAttribute(Constants.PROJECT);
        projectGrid.setProjects(gridProjects);
        BindUtils.postNotifyChange(null, null, projectGrid, PROJECT_STRING);
        generateGridHeader();
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        uploadMostRecentProjects();
        Clients.clearBusy(entityList);
    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
       comp.addEventListener(EVENTS.ON_CREATE_COMPOSITION, event -> createNewComposition());
        comp.addEventListener(EVENTS.ON_LOAD_CLONED_COMPOSITION, event -> loadClonedComposition(event));
       


        homeTabbox.addEventListener(EVENTS.ON_PROJECT_ADD, projectAddListener);
        homeTabbox.addEventListener(EVENTS.ON_UPDATE_GRID_PLUGINS, event -> updatePluginList((Project) event.getData()));

        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        user = authenticationService.getCurrentUser();
        toggleGridView.addEventListener(Events.ON_CHECK, event -> showGrid());
        toggleListView.addEventListener(Events.ON_CHECK, event -> showList());
        LOGGER.debug("user permission object--->{}", user.getPermission());
        if (user.getPermission().getRampsPermission().getUiPermission().canViewGrid()
                && user.getPermission().getRampsPermission().getUiPermission().canViewList()) {
            viewSelectRadioGroup.setVisible(true);
        } else {
            viewSelectRadioGroup.setVisible(false);
        }
        if (user.getPermission().getRampsPermission().getUiPermission().isGridDefaultView()) {
            toggleGridView.setChecked(true);
            toggleListView.setChecked(false);
            showGrid();
        } else {
            toggleGridView.setChecked(false);
            toggleListView.setChecked(true);
            showList();
        }

        CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
        try {
            projects = compositionService.filterProjects(user, true, true, getCompositions());
            favProjects = compositionService.getFavoriteCompositions(user.getId());
            projects= updateFavouriteProject(projects);
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            showErrorNotification(ex.getMessage());
            return;
        }
        try {
            compositionService.assignClusterToProject(projects);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
        }
        generateGridHeader();
        gridProjects.addAll(projects);
        modelList.addAll(projects);
        projectGrid = (ProjectGrid) thumbnailLayout.getAttribute(Constants.PROJECT);
        projectGrid.setProjects(gridProjects);
        BindUtils.postNotifyChange(null, null, projectGrid, PROJECT_STRING);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        entityList.setModel(modelList);
        entityList.setRowRenderer(new CompositionRowRenderer(thumbnailLayout));

        entityList.addEventListener(EVENTS.ON_FILTER_CHANGE, addFilterListener);

        thumbnailLayout.addEventListener(EVENTS.ON_CLICK_VIEW_OR_EDIT, this::clickListenerForViewAndEdit);

        thumbnailLayout.addEventListener(EVENTS.ON_CLICK_CLONE, this::cloneComposition);

        thumbnailLayout.addEventListener(EVENTS.ON_DELETE_COMPOSITION, this::initiateCompositionDelete);
        
        thumbnailLayout.addEventListener(EVENTS.ON_FAV_COMPOSITION, event -> markAsFavouriteProject((Project) event.getData()));

        homeTabbox.addEventListener(EVENTS.ON_CHANGE_LABEL, evt -> {
            Project prjt = (Project) evt.getData();
            if (modelList.contains(prjt)) {
                modelList.set(modelList.indexOf(prjt), prjt);
            } else {
                LOGGER.info("Skipped label update. Composition '{}' is not in view", prjt.getLabel());
            }
        });

        homeTabbox.addEventListener(EVENTS.ON_OPEN_COMPOSITION, event -> {
            TabData data = (TabData) event.getData();
            openComposition(data.getProject(), data.getFlow(), data.getComposition());
        });

        homeTabbox.addEventListener(EVENTS.ON_CLOSE_OLD_PROJECT, event -> {
            Project proj = (Project) event.getData();

            for (Component component : homeTabbox.getTabs().getChildren()) {
                Project tabProject = (Project) component.getAttribute(Constants.PROJECT);
                if (proj == tabProject) {
                    Events.postEvent(Events.ON_CLOSE, component, null);
                    break;
                }
            }
        });

        // to load most recently modified projects in the searchbox
        uploadMostRecentProjects();

        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
    }

    private void updatePluginList(Project updatedProject) {
        if (gridProjects.contains(updatedProject)) {
            gridProjects.remove(gridProjects.indexOf(updatedProject));
            gridProjects.add(0, updatedProject);
            projectGrid.setProjects(gridProjects);
            BindUtils.postNotifyChange(null, null, projectGrid, PROJECT_STRING);
            // Sorting based on name
            Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
            column.sort(true, true);
            gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        } else {
            LOGGER.info("Saved compostion '{}' is not in view. Skipped plugin update.", updatedProject.getLabel());
        }
    }


    @Listen("onClick = #compoCreatedMenu")
    public void ownCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            compositionCreatedByMe();
        }else{
            promoteMessageBox("compositionCreatedByMe");
        }
    }

    @Listen("onClick=#sharedCompoMenu")
    public void sharedCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            compositionsharedByMe();
        }else{
            promoteMessageBox("compositionsharedByMe");
        }
    }
    
    @Listen("onClick=#allCompoMenu")
    public void allCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            compositionsAll();
        }else{
            promoteMessageBox("compositionsAll");
        }
    }
    
    @Listen("onClick=#favCompomenu")
    public void favoriteCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            myFavoriteComposition();
        }else{
            promoteMessageBox("myFavourite");
        }
    }
    
    @Listen("onClick=#mostAccessedCompomenu")
    public void mostAccessedCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            frequentlyAccessedComposition();
        }else{
            promoteMessageBox("frequentlyAccessed");
        }
    }
    
    public void promoteMessageBox(String  promoteBeforFunction){
        Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmPromote(event,promoteBeforFunction));
        
    }
    private void confirmPromote(ClickEvent event,String promoteBeforFunction) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            clearPromoteSelection();
            switch (promoteBeforFunction) {
            case "compositionsharedByMe":
                compositionsharedByMe();
                break;
            case "compositionCreatedByMe":
                compositionCreatedByMe();
                break;
            case "compositionsAll":
                compositionsAll();
                break;
            case "ClearFilterClick":
                clearFilter();
                break;
            case "searchBoxEnter":
                searchBoxEnter();
                break;
            case "myFavourite":
                myFavoriteComposition();
                break;
            case "frequentlyAccessed":
                frequentlyAccessedComposition();
                break;
            }
        }
    }
    
    public void compositionsharedByMe() {
        try {
            doAfterFilteringProjects(
                    ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).filterProjects(user, true, false, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        uploadMostRecentProjects();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    public void compositionCreatedByMe() {
        try {
            doAfterFilteringProjects(
                    ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).filterProjects(user, true, true, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        uploadMostRecentProjects();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        } 
        
    }
    public void compositionsAll() {
        try {
            doAfterFilteringProjects(getCompositions());
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        uploadMostRecentProjects();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    public void searchBoxEnter(){
        if (StringUtils.isEmpty(textBox.getValue())) {
            return;
        }
        List<Project> tempList = new ArrayList<Project>();
        modelList.clear();
        try {
            tempList.addAll(getCompositions().stream()
                    .filter(project -> StringUtils.containsIgnoreCase(project.getLabel(), textBox.getValue())
                            || StringUtils.containsIgnoreCase(project.getName(), textBox.getValue())
                            || StringUtils.equals(project.getUuid(), textBox.getValue())
                            || StringUtils.containsIgnoreCase(project.getAuthor(), textBox.getValue()))
                    .collect(Collectors.toList()));
            
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).assignClusterToProject(tempList);
            tempList = updateFavouriteProject(tempList);
            
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        modelList.addAll(tempList); 
        projectGrid.setProjects(tempList);
        notifyProjectListUpdate();

        clearFilter.setVisible(true);
        popup.close();
        uploadMostRecentProjects();
        homeTab.setSelected(true);
    }
    public void clearPromoteSelection(){
        promoteBtn.setSclass("promote-btn");
        promotionEntities = new ArrayList<>();
    }
    public void myFavoriteComposition(){
        try {
            doAfterFilteringProjects(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .filterFavoriteComposition(favProjects, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
        uploadMostRecentProjects();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    
    public void frequentlyAccessedComposition() {
        try {
            doAfterFilteringProjects(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .filterProjectsByAccess(user, getCompositions()));
            uploadMostRecentProjects();
            if (clearFilter.isVisible()) {
                clearFilter.setVisible(false);
            }
        } catch (DatabaseException | CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
    }
    
    private void doAfterFilteringProjects(List<Project> newProjects) {
        projects.clear();
        modelList.clear();
        gridProjects.clear();
        projects.addAll(newProjects);
        projects= updateFavouriteProject(projects);
        try {
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).assignClusterToProject(projects);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
        }
        modelList.addAll(projects);
        gridProjects.addAll(projects);
        projectGrid.setProjects(gridProjects);
        notifyProjectListUpdate();
    }

    private void cloneComposition(Event event) {
        TabData data = (TabData) event.getData();
        DashboardConfig dashboardConfig = DashboardUtil.generateDashboardConfigFromRamps(data, homeTabbox);
        try {
            DashboardUtil.constructDashboardConfigFromContractInstance(dashboardConfig, 
                    data.getComposition().getContractInstances());
            data.setDashboard(dashboardConfig.getDashboard());

            // Validate the composition before proceeding
            if (!RampsUtil.checkCompositionError(data.getComposition(), this.getSelf())) {
                Map<String, TabData> newProjectData = new HashMap<String, TabData>();
                newProjectData.put(Constants.TAB_DATA, data);
                Window window = (Window) Executions.createComponents(NEW_PROJECT_URI, HomeController.this.getSelf(), newProjectData);
                window.doModal();
            }
        } catch (Exception e) {
            Clients.showNotification(Labels.getLabel("unableToCloneComposition"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_MIDDLE_CENTER, 3000);
            LOGGER.error("Unable to clone Query schema-->",e);
            return;
        }
    }

    private void initiateCompositionDelete(Event event) throws HipieException, DatabaseException {
        int startTime = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("deleteComposition() Start --->{}");
        Project projectToDelete = (Project) event.getData();
        if (!HipieSingleton.getHipie().getPermissionsManager().userCanEdit(user.getId(), projectToDelete.getComposition())) {
            Clients.showNotification(Labels.getLabel("youDonotHavePermissionToDelete"), Clients.NOTIFICATION_TYPE_ERROR, homeTabbox,
                    Constants.POSITION_MIDDLE_CENTER, 3000);
            return;
        }
        if (projectToDelete.isRunning()) {
            Clients.showNotification(Labels.getLabel("cannotDeleteCompositionWhileRunning"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }

        ContractInstance contractInstance = CompositionUtil.getVisualizationContractInstance(projectToDelete.getComposition());

        if (contractInstance != null && !CompositionUtil.extractRunOption(contractInstance)
                && ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(projectToDelete.getComposition(),
                        true) != null) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("width", "500");
            Messagebox.show(Labels.getLabel("deleteCompositionService"), Labels.getLabel(DELETE_COMPOSITION_DIALOGUE),
                    new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO, Messagebox.Button.CANCEL },
                    new String[] { Labels.getLabel("deleteEverything"), Labels.getLabel("onlyComposition"), Labels.getLabel("cancel") },
                    Messagebox.QUESTION, null, clickEvent -> confirmDeleteAll(event, startTime, projectToDelete, clickEvent) , params);

        } else {
            Messagebox.show(Labels.getLabel("deleteComposition"), Labels.getLabel(DELETE_COMPOSITION_DIALOGUE),
                    new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,
                    (EventListener<ClickEvent>) clickEvent -> confirmDeleteComposition(event, startTime, projectToDelete, clickEvent));
        }

    }

    private void confirmDeleteAll(Event event, int startTime, Project projectToDelete, ClickEvent clickEvent) {
        if (Messagebox.Button.YES.equals(clickEvent.getButton())) {
            deleteComposition(event, startTime, projectToDelete, true);
        } else if (Messagebox.Button.NO.equals(clickEvent.getButton())) {
            deleteComposition(event, startTime, projectToDelete, false);
        }
    }

    private void confirmDeleteComposition(Event event, int startTime, Project projectToDelete, ClickEvent clickEvent) {
        if (Messagebox.Button.YES.equals(clickEvent.getButton())) {
            deleteComposition(event, startTime, projectToDelete, false);
        }
    }

    private void deleteComposition(Event event, int startTime, Project projectToDelete, boolean deleteservices) {
        int seconds = 0;
        try {
            seconds = Calendar.getInstance().get(Calendar.SECOND);
            LOGGER.debug("compositionService.deleteComposition() start --->{}");
            
            // Close the project if open
            if (isOpen(projectToDelete)) {
                closeTab(projectToDelete);
            }
            
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).deleteAccessLog(projectToDelete.getComposition().getId(), user.getId(), false);
            
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).deleteComposition(projectToDelete.getComposition(), user.getId(),
                    deleteservices);
            LOGGER.debug("compositionService.deleteComposition() end duration --->{}", Calendar.getInstance().get(Calendar.SECOND) - seconds);
        } catch (Exception d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return;
        }

        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("updateCompositionList() start --->{}");
        updateProjectList((Project) event.getData());
        LOGGER.debug("updateCompositionList() end duration --->{}", Calendar.getInstance().get(Calendar.SECOND) - seconds);

        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("notifyCompositionListUpdate() start --->{}");
        notifyProjectListUpdate();
        LOGGER.debug("notifyCompositionListUpdate() end duration --->{}", Calendar.getInstance().get(Calendar.SECOND) - seconds);

        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("updateRecentCompositions() start --->{}");
        uploadMostRecentProjects();
        LOGGER.debug("updateRecentCompositions() end duration --->{}", Calendar.getInstance().get(Calendar.SECOND) - seconds);

        LOGGER.debug("filteredModelList after -->{}", listBox.getModel());

        
        LOGGER.debug("deleteComposition() End total time --->{}", Calendar.getInstance().get(Calendar.SECOND) - startTime);
    }
    
    private void clickListenerForViewAndEdit(Event event) {
        TabData data = (TabData) event.getData();
        Composition composition;
        try {
            composition = data.getProject().getComposition();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("retrieveCompositionFailed"), Clients.NOTIFICATION_TYPE_ERROR, HomeController.this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            return;
        }

        if (Flow.VIEW != data.getFlow() && !HipieSingleton.getHipie().getPermissionsManager()
                .userCanEdit(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), composition)) {
            Clients.showNotification(Labels.getLabel("youDonotHavePermissionToEdit"), Clients.NOTIFICATION_TYPE_ERROR, HomeController.this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            return;
        }

        // Validate the composition before proceeding
        if (!RampsUtil.checkCompositionError(composition, this.getSelf())) {
            initializeCompositionOrOpenComposition(data, composition);
        }
        
    }

    private void showList() {
        thumbnailLayout.setVisible(false);
        entityList.setVisible(true);
        sortMenuBar.setVisible(false);
        refreshSortIcons();
        clearSelections();
    }

    private void showGrid() {
        thumbnailLayout.setVisible(true);
        entityList.setVisible(false);
        sortMenuBar.setVisible(true);
        refreshSortIcons();
        clearSelections();
    }

    private void initializeCompositionOrOpenComposition(TabData data, Composition composition) {
        if (data.getProject().getHpccConnection() == null) { // New composition
            try {
                Map<String, TabData> newProjectData = new HashMap<String, TabData>();
                newProjectData.put(Constants.TAB_DATA, data);
                Window window = (Window) Executions.createComponents(NEW_PROJECT_URI, this.getSelf(), newProjectData);
                window.doModal();

            } catch (Exception ex) {
                LOGGER.error(Constants.EXCEPTION, ex);
            }

        } else { // Existing composition
            try {
                if (data.getProject().getHpccConnection().testConnection()) {
                    openComposition(data.getProject(), data.getFlow(), composition);
                }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(Labels.getLabel("hpccConnectionFailed") + ": " + e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR,
                        this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            }
        }
    }

    protected void closeTab(Project compToOpen) {
        Tab tab;
        Project tabProj;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabProj = (Project) tab.getAttribute(Constants.PROJECT);
            try {
                if (tabProj != null && compToOpen.getComposition().getId().equals(tabProj.getComposition().getId())) {
                    tab.close();
                    LOGGER.info("Project Tab closed :: "+compToOpen.getComposition().getCanonicalName());
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
        }

    }

    protected void selectOpenedTab(Project compToOpen) {
        Tab tab;
        Project tabProj;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabProj = (Project) tab.getAttribute(Constants.PROJECT);
            try {
                if (tabProj != null && compToOpen.getComposition().getId().equals(tabProj.getComposition().getId())) {
                    tab.setSelected(true);
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createProjectTab(TabData data) {
        Tab tab = new Tab();
        Tabpanel tabpanel = new Tabpanel();

        tab.setLabel(data.getProject().getLabel());
        data.setProjectTab(tab);
        tab.setAttribute(Constants.PROJECT, data.getProject());

        tab.setAttribute(Constants.COMPOSITION, data.getComposition());
        tab.setClosable(true);
        tab.addEventListener(Events.ON_CLOSE, event -> {
            Project proj = (Project) event.getTarget().getAttribute(Constants.PROJECT);
            if (proj.isRunning()) {
                event.stopPropagation();
                Clients.showNotification(Labels.getLabel("cannotClose"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER,
                        3000, true);
            }

            Set<String> fileNameSet = (Set<String>) Sessions.getCurrent().getAttribute(Constants.OPEN_PROJECT_LABELS);
            if (fileNameSet != null && fileNameSet.contains(proj.getLabel())) {
                fileNameSet.remove(proj.getLabel());
            }
        });
        data.getProject().setDatasourceStatus(DatasourceStatus.LOADING);
        Include include = new Include("ramps/project/project_details.zul");
        include.setDynamicProperty(Constants.TAB_DATA, data);
        tabpanel.appendChild(include);
        homeTabbox.getTabs().appendChild(tab);
        homeTabbox.getTabpanels().appendChild(tabpanel);

        tab.setSelected(true);
        tab.addEventListener(Events.ON_SELECT, event -> selectTab(tab));
        LOGGER.debug("create tab project name ---> {}", data.getProject().getName());
    }
    
    private void selectTab(Tab tab) {
        if (tab.getAttribute(Constants.DASHBOARD_HOLDER_UUID) != null) {
            RampsUtil.resizeDashboard((String)tab.getAttribute(Constants.DASHBOARD_HOLDER_UUID));
        }
    }

    /**
     * removes the deleted project from model list
     * 
     * @param project
     */
    protected void updateProjectList(Project project) {
        projects.remove(project);
        modelList.remove(project);
        gridProjects.remove(project);
        projectGrid.getProjects().remove(project);
        // Need to enable, if duplicate composition allowed

        Clients.showNotification(Labels.getLabel("compositionDeleted"), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER,
                3000, true);
    }
      public void loadClonedComposition(Event event) {
          DashboardConfig config = (DashboardConfig) event.getData();
          if (config != null ) {
              Composition DashboardData=config.getComposition();
              Project addedProject = new Project();
              addedProject.setClusterConfig(config.getDashboard().getClusterConfig());
              addedProject.setName(config.getDashboard().getName());
              addedProject.setLabel(config.getDashboard().getLabel());
              addedProject.setDescription("New Composition");
              openComposition(addedProject, Flow.CLONE, DashboardData);
          }
  }
    @Listen("onClick = #newComposition")
    public void createNewComposition() {
        if (getSelf().query("#createNewProjectWindow") != null) {
            LOGGER.error(
                    "Cannot create more than one component with the same id, must be an error due to user action(Clicking same button more than once)");
            return;
        }
        Map<String, TabData> newProjectData = new HashMap<String, TabData>();
        newProjectData.put(Constants.TAB_DATA, new TabData(new Project(), Flow.NEW, null));
        Window window = (Window) Executions.createComponents(NEW_PROJECT_URI, getSelf(), newProjectData);
        window.doModal();
    }

    @Listen("onClick = #newCompositionFromTemplate")
    public void createTemplate(Event event) {
        try {
            List<Project> projectList = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .getProjectTemplates(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser());
            if (!projectList.isEmpty()) {
                Window window = (Window) Executions.createComponents("ramps//project/add_project_template.zul", this.getSelf().getParent(), null);
                window.doModal();
            } else {
                Clients.showNotification(Labels.getLabel("noTemplates"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER,
                        3000, true);
                return;
            }

        } catch (CompositionServiceException ex) {
            LOGGER.error("{} ---> {}", ex.getMessage(), ex);
            Clients.showNotification(ex.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }

    }

    @Listen("onChanging = #textBox")
    public void filterProjects(InputEvent event) {
        if (event.getValue() == null || event.getValue().isEmpty()) {
            uploadMostRecentProjects();
            return;
        }
       
        ListModelList<Object> filteredModelList = new ListModelList<Object>();
        try {
            List<Project> recentProjects=getCompositions().stream()
                    .filter(project -> (StringUtils.containsIgnoreCase(project.getLabel(), event.getValue())
                            || StringUtils.containsIgnoreCase(project.getName(), event.getValue())
                            || StringUtils.equals(project.getUuid(), event.getValue())
                            || StringUtils.containsIgnoreCase(project.getAuthor(), event.getValue())) )
                    .collect(Collectors.toList());
            
          ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).assignClusterToProject(recentProjects);
            
            filteredModelList.addAll(recentProjects);
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            showErrorNotification(d.getMessage());
            return;
        }
        listBox.setModel(filteredModelList);
        labelHead.setVisible(false);
        ListitemRenderer<Project> renderer = (item, project, i) -> item.setLabel(project.getLabel() + " by " + project.getAuthor());
        listBox.setItemRenderer(renderer);
    }

    @Listen("onSelect = #listBox")
    public void createNewFilteredProject(SelectEvent<Component, Project> event) {
        Project selectedProject = event.getSelectedObjects().iterator().next();
        if (((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canViewProject()) {
            openCompositionFromSearch(selectedProject);
        }
        popup.close();
    }

    private void openCompositionFromSearch(Project selectedProject) {
        try {
            if (selectedProject.getClusterConfig() != null) {
                openComposition(selectedProject, Constants.Flow.VIEW, selectedProject.getComposition());
            } else {
                Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, thumbnailLayout, new TabData(selectedProject, Constants.Flow.VIEW));
            }

            uploadMostRecentProjects();

            textBox.setValue("");
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
        }
    }

    @Listen("onOK = #textBox")
    public void enter() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            searchBoxEnter();
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmPromote(event,"searchBoxEnter"));
        }
    }

    private void notifyProjectListUpdate() {
        ProjectGrid projGrid = (ProjectGrid) thumbnailLayout.getAttribute(Constants.PROJECT);
        BindUtils.postNotifyChange(null, null, projGrid, PROJECT_STRING);
    }

    @Listen("onClick = #clearFilter")
    public void viewAllProjects() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            clearFilter();
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmPromote(event,"ClearFilterClick"));
        }
 
    }
    
    public void clearFilter(){
        CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
        try {
            projects = compositionService.filterProjects(user, true, true, getCompositions());
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        modelList.clear();
        modelList.addAll(projects);
        gridProjects.clear();
        gridProjects.addAll(projects);
        notifyProjectListUpdate();
        clearFilter.setVisible(false);
        // Sorting based on name
        Column column = (Column) entityList.getColumns().getChildren().listIterator().next();
        column.sort(true, true);
        gridSortDesc(dateItem, CompositionUtil.SORT_BY_DATE_DES);
    }

    /**
     * Generates Grid header with Project properties
     * 
     * @param projects
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    private void generateGridHeader() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        LOGGER.debug("Creating columns");
        String lastModifiedDate = "lastModifiedDate";
        Columns columns = entityList.getColumns();
        Column dspName = new Column(Labels.getLabel("dspName"));
        Column lastModified = new Column(Labels.getLabel(lastModifiedDate));
        Column authorColumn = new Column(Labels.getLabel("author"));
        dspName.addEventListener(Events.ON_SORT, event -> clearPromoteSelection());
        lastModified.addEventListener(Events.ON_SORT, event -> clearPromoteSelection());
        authorColumn.addEventListener(Events.ON_SORT, event -> clearPromoteSelection());
        try {
            dspName.setSort("auto('label')");
            lastModified.setSort("auto('lastModifiedDate')");
            authorColumn.setSort("auto('author')");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            Clients.showNotification(Labels.getLabel("sortingNotAvailableInList"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            LOGGER.error(Constants.EXCEPTION, e);
        }
        
        if(HipieSingleton.canPromote()) {
            Column checkbox=new Column();
            checkbox.setWidth("25px");
            columns.appendChild(checkbox);
        }
        
        columns.appendChild(dspName);
        columns.appendChild(lastModified);
        columns.appendChild(authorColumn);
        columns.appendChild(new Column(Labels.getLabel("dspActions")));

    }

    private void openComposition(Project project, Flow flow, Composition composition) {
        Project projectToOpen = project;
        List<Plugin> plugins = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getPlugins(composition);

        // Removing Visualization plugin from the plugins list.
        List<Plugin> pluginsWithoutVisualization = plugins.stream()
                .filter(plugin -> plugin instanceof DatasetPlugin
                        || !plugin.getContractInstance().getContract().getRepositoryName().equals(Dashboard.DASHBOARD_REPO))
                .collect(Collectors.toList());

        // Add plugins to project
        project.setPlugins(pluginsWithoutVisualization);
        
        error = composition.getParseErrors().getErrors();
        
        
        if (error.isEmpty()) {
            if (flow != Flow.CLONE) {
                if (isOpen(projectToOpen)) {
                    selectOpenedTab(projectToOpen);
                    return;
                } 
            }

            if (projectToOpen.getClusterConfig() == null) {
                try {
                    projectToOpen.setClusterConfig(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).retriveClusteConfig(
                            projectToOpen.getCanonicalName(),
                            ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId()));
                } catch (DatabaseException d) {
                    LOGGER.error(Constants.EXCEPTION, d);
                }
            }

            TabData tabData = new TabData(projectToOpen, flow, composition);
            tabData.setComposition(composition);
            createProjectTab(tabData);
            compositionAccessLog(projectToOpen);
        } else {
            showErrorNotification();
        }
    }

    private void showErrorNotification() {
        Iterator<HError> iterator = error.iterator();
        if (iterator.hasNext()) {
            Clients.showNotification(Labels.getLabel("errorInComposition").concat(iterator.next().toString()), Clients.NOTIFICATION_TYPE_ERROR,
                    getSelf(), Constants.POSITION_TOP_CENTER, 50000, true);
        }
    }

    private void uploadMostRecentProjects() {
        ListModelList<Object> model = (ListModelList<Object>) listBox.getModel();
        if (model != null) {
            model.clear();
        }
        textBox.setValue("");
        labelHead.setVisible(true);
        List<Project> recentProjects = projects;
        ListModelList<Project> recentProjectsModel = new ListModelList<Project>();

        Collections.sort(recentProjects, (o1, o2) -> o2.getLastModifiedDate().compareTo(o1.getLastModifiedDate()));

        if (recentProjects.size() >= 5) {
            recentProjectsModel.addAll(recentProjects.subList(0, 4));
        } else {
            recentProjectsModel.addAll(recentProjects);
        }
        listBox.setModel(recentProjectsModel);
        ListitemRenderer<Project> renderer = (item, project, i) -> item.setLabel(project.getLabel() + " by " + project.getAuthor());
        listBox.setItemRenderer(renderer);
    }

    @Listen("onClick = #authorItem")
    public void sortByAuthor() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(authorItem);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,authorItem));
        }
    }

    @Listen("onClick = #dateItem")
    public void sortByDate() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(dateItem);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,dateItem));
        }
    }

    @Listen("onClick = #nameItem")
    public void sortByName() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(nameItem);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,nameItem));
        }
    }
    public void  confirmSort(ClickEvent event, Menuitem item) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            clearPromoteSelection();
            gridViewSort(item);
        }
    }
    public void gridViewSort(Menuitem item) {
        toggleSortType(item, CompositionUtil.SORT_BY_NAME_ASC, CompositionUtil.SORT_BY_NAME_DES, getView());
    }

    private void sortGridView(Comparator<GridEntity> sortType) {
        Collections.sort(gridProjects, sortType);
        projectGrid.setProjects(gridProjects);
        BindUtils.postNotifyChange(null, null, projectGrid, PROJECT_STRING);
    }

    private void toggleSortType(Menuitem item, Comparator<GridEntity> ascSort, Comparator<GridEntity> desSort, VIEW view) {
        String gridSort = null;
        if (item.getAttribute(GRID_SORT_TYPE) != null) {
            gridSort = (String) item.getAttribute(GRID_SORT_TYPE);
        }
        if (view == VIEW.GRID) {
            if (gridSort == null || DES.equals(gridSort)) {
                gridSortAsc(item, ascSort);
            } else {
                gridSortDesc(item, desSort);
            }
        }
    }

    private void gridSortAsc(Menuitem item, Comparator<GridEntity> ascSort) {
        sortGridView(ascSort);
        item.setAttribute(GRID_SORT_TYPE, ASC);
        deleteOtherSortTypes(item, GRID_SORT_TYPE);
        refreshSortIcons();
    }

    private void gridSortDesc(Menuitem item, Comparator<GridEntity> desSort) {
        sortGridView(desSort);
        item.setAttribute(GRID_SORT_TYPE, DES);
        deleteOtherSortTypes(item, GRID_SORT_TYPE);
        refreshSortIcons();
    }

    private VIEW getView() {
        if (toggleGridView.isChecked()) {
            return VIEW.GRID;
        } else {
            return VIEW.LIST;
        }
    }

    private void deleteOtherSortTypes(Menuitem item, String toClear) {
        if (item != nameItem) {
            nameItem.setAttribute(toClear, null);
        }
        if (item != authorItem) {
            authorItem.setAttribute(toClear, null);
        }
        if (item != dateItem) {
            dateItem.setAttribute(toClear, null);
        }
    }

    private void refreshSortIcons() {
        String gridSort = null;

        if (getView() == VIEW.GRID) {
            gridSort = (String) nameItem.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, nameItem);

            gridSort = (String) dateItem.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, dateItem);

            gridSort = (String) authorItem.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, authorItem);
        }
    }

    private void setSortIcon(String sort, Menuitem item) {
        if (sort == null) {
            item.setSclass("hiddenIcon");
        } else if (ASC.equals(sort)) {
            item.setSclass("");
            item.setIconSclass(FA_FA_SORT_AMOUNT_ASC);
        } else {
            item.setSclass("");
            item.setIconSclass(FA_FA_SORT_AMOUNT_DESC);
        }
    }
    private List<Project> getCompositions() throws CompositionServiceException {
        return ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getProjects(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser());
    }
    public void showErrorNotification(String message){
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
    }
    
    protected boolean isOpen(Project compToOpen) {
        boolean exists=false;
        Tab tab;
        Project tabProj;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabProj = (Project) tab.getAttribute(Constants.PROJECT);
            try {
                if (tabProj != null && compToOpen.getComposition().getId().equals(tabProj.getComposition().getId())) {
                    exists=true;
                    LOGGER.info("Project is opened in another tab :: "+compToOpen.getComposition().getCanonicalName());
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
        }
        return exists;
    }
    
    
    
    private void markAsFavouriteProject(Project project){
        try {
//            if (favProjects.contains(project.getComposition().getId())) {
                if (project.getIsFavourite()) {
                // code to unFav a project
                // change the star accordingly
                boolean onComplete = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .unMarkAsFavoriteComposition(project.getComposition().getId(), user.getId());
                if(onComplete){
                    project.setIsFavourite(false);
                    favProjects.remove(project.getComposition().getId());
                }
            } else {
                // code to Fav a project
                // change the star accordingly
                boolean onComplete = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .markAsFavoriteComposition(project.getComposition().getId(), user.getId());
                if(onComplete){
                    project.setIsFavourite(true);
                    favProjects.add(project.getComposition().getId());
                }
            }
        } catch (DatabaseException | HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }
    
    private List<Project> updateFavouriteProject(List<Project> projects) {
        projects.forEach(proj -> {
            try {
                if(favProjects.contains(proj.getComposition().getId())){
                    proj.setIsFavourite(true);
                }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        });
        return projects;
    }
    
    private void compositionAccessLog(Project project){
        try {
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
            .logCompositionAccess(project.getComposition().getId(), user.getId());
        } catch (DatabaseException | HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }
    
}
