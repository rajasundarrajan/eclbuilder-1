package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.Constants.VIEW;
import org.hpccsystems.dsp.GridEntity;
import org.hpccsystems.dsp.HomeComposer;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.component.renderer.CompositionRowRenderer;
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
import org.zkoss.zk.ui.Path;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listitem;
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
    private static final String DELETE_CONFIRMATION_DIALOGUE = "deleteConfirmationDialogue";
    private static final String FA_FA_SORT_AMOUNT_DESC = "fa fa-sort-desc";
    private static final String FA_FA_SORT_AMOUNT_ASC = "fa fa-sort-asc";
    private static final String UNABLE_TO_LOAD_DASHBOARD_TEMPLATE = "Unable to load Dashboard template";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    private static final String PROMOTE_CONFIRMATION_MESSAGE = "promoteProceedmessage";
    private static final String PROMOTE_CONFIRMATION_HEADER = "promoteProceed";
  
    @Wire
    private Tabbox homeTabbox;    
    @Wire
    private Listbox searchListBox;
    @Wire
    private Textbox searchTextBox;
    @Wire
    private Listhead listHead;
    @Wire
    private Popup searchPopup;
    @Wire
    private Tab homeTab;
    
    @Wire
    private Button clearFilter;
    
    @Wire
    private Menuitem  authorItemDashboard;
    
    @Wire
    private Menuitem dateItemDashboard;
    
    @Wire
    private Radiogroup viewSelectRadioGroup;

    @Wire
    private Radio toggleGridView;

    @Wire
    private Radio toggleListView;
    
    @Wire
    private Menuitem nameItemDashboard;
    
    private DashboardGrid dashboardGrid;

    @Wire
    private Menubar sortMenuBar;

    private User user;
    private static final String NEW_DASHBOARD_URI = "/dashboard/design/newDashboard.zul";
    
    private List<Dashboard> dashboards;
    private ListModelList<Dashboard> dashboardModel = new ListModelList<Dashboard>();
    private static final String DASHBOARD_GRID_PROPERTY = "dashboards";
    private static final String GRID_SORT_TYPE = "gridSortType";
    private static final String ASC="asc";
    private static final String DES="des";
    private List<String> favDashboards;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        toggleGridView.addEventListener(Events.ON_CHECK, event -> showGrid());
        toggleListView.addEventListener(Events.ON_CHECK, event -> showList());
        if (user.getPermission().getDashboardPermission().getUiPermission().canViewGrid()
                && user.getPermission().getDashboardPermission().getUiPermission().canViewList()) {
            viewSelectRadioGroup.setVisible(true);
        } else {
            viewSelectRadioGroup.setVisible(false);
        }
        if (user.getPermission().getDashboardPermission().getUiPermission().isGridDefaultView()) {
            toggleGridView.setChecked(true);
            toggleListView.setChecked(false);
            showGrid();
        } else {
            toggleGridView.setChecked(false);
            toggleListView.setChecked(true);
            showList();
        }
        
        comp.addEventListener(EVENTS.ON_CREATE_COMPOSITION, event -> createDashboard());
        
        //getting available Dashboards
        try {
            CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
            dashboards = compositionService.filterDashboards(user, true, true, getCompositions());
            compositionService.assignClusterToDshboard(dashboards);
            favDashboards = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getFavoriteCompositions(user.getId());
            dashboards = updateFavouriteDashboard(dashboards);
        } catch (DatabaseException | CompositionServiceException ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            showErrorNotification(ex.getMessage());
            return;
        }
        
        generateGridHeader();
        dashboardModel.addAll(dashboards);
        entityList.setModel(dashboardModel);
        entityList.setRowRenderer(new CompositionRowRenderer(thumbnailLayout));
        dashboardGrid = (DashboardGrid) thumbnailLayout.getAttribute(Constants.DASHBOARD);
        dashboardGrid.setDashboards(dashboards);
        BindUtils.postNotifyChange(null, null, dashboardGrid, DASHBOARD_GRID_PROPERTY);
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        
        thumbnailLayout.addEventListener(EVENTS.ON_CLICK_VIEW_OR_EDIT, event -> openDashboard((DashboardConfig) event.getData()));
        
        thumbnailLayout.addEventListener(EVENTS.ON_CLICK_CLONE, event ->   cloneComposition((DashboardConfig) event.getData()));
       
        thumbnailLayout.addEventListener(EVENTS.ON_DELETE_COMPOSITION, event -> initiateDashboardDelete((Dashboard) event.getData()));
        
        thumbnailLayout.addEventListener(EVENTS.ON_FAV_COMPOSITION, event -> markAsFavouriteDashbaord((Dashboard) event.getData()));
        
        homeTabbox.addEventListener(Dashboard.EVENTS.ON_OPEN_DASHBOARD, event -> openDashboardTab((DashboardConfig) event.getData()));

        homeTabbox.addEventListener(Dashboard.EVENTS.ON_OPEN_COMPOSITION, event -> openCompositionTab((DashboardConfig) event.getData()));
        
        homeTabbox.addEventListener(Dashboard.EVENTS.ON_SAVE_DASHBOARD, event -> addDashboardToGrid((DashboardConfig) event.getData()));
       
        homeTabbox.addEventListener(EVENTS.ON_CHANGE_LABEL, this::updateLabel);
        
        updateRecentDashboards();
    }

    private List<Dashboard> getCompositions() throws CompositionServiceException {
        return ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getDashboards(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser());
    }
    
    private void searchboxRenderer(Listitem item, Object dashboard, int i) {
        Dashboard dash = (Dashboard) dashboard;
        item.setLabel(dash.getLabel() + " by " + dash.getAuthor());
    }

    private void updateLabel(Event event) {
        Dashboard dashboard = (Dashboard) event.getData();
        List<Dashboard> tmpDashboards = dashboardGrid.getDashboards();
        // Returning when dashboard is not present
        if(!dashboardModel.contains(dashboard)||!(tmpDashboards.contains(dashboard))) {
            return;
        }
        dashboardModel.set(dashboardModel.indexOf(dashboard), dashboard);
        int tmpIndex = tmpDashboards.indexOf(dashboard);
        tmpDashboards.remove(tmpIndex);
        tmpDashboards.add(tmpIndex, dashboard);
        dashboardGrid.setDashboards(tmpDashboards);
        BindUtils.postNotifyChange(null, null, dashboardGrid, DASHBOARD_GRID_PROPERTY);
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
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
    
    private void generateGridHeader() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating columns");
        }
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
            Clients.showNotification(Labels.getLabel("sortingNotAvailableInList"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
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
    
    private void cloneComposition(DashboardConfig config) {
            config.setHomeTabbox(homeTabbox);
            createNewDashboardWindow(config);
    }
    
    private void createNewDashboardWindow(DashboardConfig config){
        Map<Integer, DashboardConfig> newDashboardData = new HashMap<Integer, DashboardConfig>();
        newDashboardData.put(1, config);
        Window window = (Window) Executions.createComponents(NEW_DASHBOARD_URI, getSelf(), newDashboardData);
        window.doModal();
    }
    
    @Listen("onClick = #compoCreatedMenu")
    public void ownCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            dashboardCreatedByMe();
        }else{
            promoteMessageBox("DashboardCreatedByMe");
        }
    }
    
    @Listen("onClick=#sharedCompoMenu")
    public void sharedCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            dashboardSharedByMe();
        }else{
            promoteMessageBox("DashboardSharedByMe");
        }
    }
    
    @Listen("onClick=#allCompoMenu")
    public void allCompositions() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            dashboardAll();
        }else{
            promoteMessageBox("DashboardAll");
        }
    }
    
    @Listen("onClick=#favCompomenu")
    public void favoriteDashboard() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            myFavoriteDashboard();
        }else{
            promoteMessageBox("myFavourite");
        }
    }
    
    @Listen("onClick=#mostAccessedCompomenu")
    public void mostAccessedDashboard() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            frequentlyAccessedDashboard();
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
            case "DashboardSharedByMe":
                dashboardSharedByMe();
                break;
            case "DashboardCreatedByMe":
                dashboardCreatedByMe();
                break;
            case "DashboardAll":
                dashboardAll();
                break;
            case "ClearFilterClick":
                clearFilter();
                break;
            case "searchBoxEnter":
                searchBoxEnter();
                break;
            case "myFavourite":
                myFavoriteDashboard();
                break;
            case "frequentlyAccessed":
                frequentlyAccessedDashboard();
                break;
            }
        }
    }
    public void dashboardSharedByMe() {
        try {
            doAfterFilteringDashboards(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).filterDashboards(user, true, false, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        updateRecentDashboards();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    public void dashboardCreatedByMe() {
        try {
            doAfterFilteringDashboards(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).filterDashboards(user, true, true, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        updateRecentDashboards();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
        
    }
    public void dashboardAll() {
        try {
            doAfterFilteringDashboards(getCompositions());
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        updateRecentDashboards();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    public void searchBoxEnter(){
        List<Dashboard> searched = (List<Dashboard>) getSearchedDashboards();
        dashboardModel.clear();
        dashboardModel.addAll(searched);

        dashboardGrid.setDashboards(searched);
        notifyDashboardListUpdate();

        clearFilter.setVisible(true);
        searchPopup.close();
        updateRecentDashboards();
        homeTab.setSelected(true);
    }
    
    public void clearPromoteSelection(){
        promoteBtn.setSclass("promote-btn");
        promotionEntities = new ArrayList<>();
    }
    
    public void myFavoriteDashboard(){
        try {
            doAfterFilteringDashboards(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .filterFavoriteDashboards(favDashboards, getCompositions()));
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        updateRecentDashboards();
        if (clearFilter.isVisible()) {
            clearFilter.setVisible(false);
        }
    }
    
    public void frequentlyAccessedDashboard() {
        try {
            doAfterFilteringDashboards(((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .filterDashboardsByAccess(user, getCompositions()));
            updateRecentDashboards();
            if (clearFilter.isVisible()) {
                clearFilter.setVisible(false);
            }
        } catch (DatabaseException | CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
    }

    private void doAfterFilteringDashboards(List<Dashboard> list){
        dashboards.clear();
        dashboardModel.clear();
        dashboards.addAll(list);
        try {
            dashboards = updateFavouriteDashboard(dashboards);
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).assignClusterToDshboard(dashboards);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
        }
        dashboardModel.addAll(dashboards);
        dashboardGrid = (DashboardGrid) thumbnailLayout.getAttribute(Constants.DASHBOARD);
        dashboardGrid.setDashboards(dashboards);
        BindUtils.postNotifyChange(null, null, dashboardGrid, DASHBOARD_GRID_PROPERTY);
        updateRecentDashboards();
    }
    
    private void addDashboardToGrid(DashboardConfig config) {
        dashboards.add(config.getDashboard());
        dashboardModel.add(config.getDashboard());
        dashboardGrid.setDashboards(dashboards);
        BindUtils.postNotifyChange(null, null, dashboardGrid, DASHBOARD_GRID_PROPERTY);
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        updateRecentDashboards();
    }

    private void initiateDashboardDelete(Dashboard dashboard) throws HipieException, DatabaseException {
        int startTime = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("deleteComposition() Start --->{}");
        if (!HipieSingleton.getHipie().getPermissionsManager().userCanEdit(user.getId(), dashboard.getComposition())) {
            Clients.showNotification(Labels.getLabel("youDonotHavePermissionToDeleteDashboard"), Clients.NOTIFICATION_TYPE_ERROR, homeTabbox, Constants.POSITION_MIDDLE_CENTER,
                    3000);
            return;
        }
        if (dashboard.isRunning()) {
            Clients.showNotification(Labels.getLabel("cannotDeleteDashboardWhileRunning"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }


        ContractInstance contractInstance = CompositionUtil.getVisualizationContractInstance(dashboard.getComposition());
        
        if (contractInstance != null && !CompositionUtil.extractRunOption(contractInstance)
                && ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(dashboard.getComposition(),
                        true) != null) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("width", "500");
            Messagebox.show(Labels.getLabel("deleteConfirmationService") + Constants.SPACE + dashboard.getLabel() + "?",
                    Labels.getLabel(DELETE_CONFIRMATION_DIALOGUE),
                    new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO, Messagebox.Button.CANCEL },
                    new String[] { Labels.getLabel("deleteEverything"), Labels.getLabel("onlyDashboard"), Labels.getLabel("cancel") },
                    Messagebox.QUESTION, null, event -> confirmDeleteAll(dashboard, startTime, event), params);
        } else {
            Messagebox.show(Labels.getLabel("deleteConfirmation") + Constants.SPACE + dashboard.getLabel() + "?",
                    Labels.getLabel(DELETE_CONFIRMATION_DIALOGUE), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmDeleteDashboard(dashboard, startTime, event));
        }

    }

    private void confirmDeleteDashboard(Dashboard dashboard, int startTime, ClickEvent event) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            deleteDashboard(dashboard, startTime, false);
        }
    }

    private void confirmDeleteAll(Dashboard dashboard, int startTime, ClickEvent event) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            deleteDashboard(dashboard, startTime, true);
        } else if (Messagebox.Button.NO.equals(event.getButton())) {
            deleteDashboard(dashboard, startTime, false);
        }
    }

    private void deleteDashboard(Dashboard dashboard, int startTime, boolean deleteservices) {
        int seconds = 0;
        try {
            seconds = Calendar.getInstance().get(Calendar.SECOND);
            LOGGER.debug("compositionService.deleteComposition() start --->{}");
            
            // Close the dashboard if open
            if (isOpen(dashboard)) {
              closeTab(dashboard);
            }
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).deleteAccessLog(dashboard.getComposition().getId(), user.getId(), false);
            
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).deleteComposition(dashboard.getComposition(),
                    ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), deleteservices);
            LOGGER.debug("compositionService.deleteComposition() end duration --->{}",Calendar.getInstance().get(Calendar.SECOND)-seconds);
        } catch (Exception d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("updateDashboardList() start --->{}");
        updateDashboardList(dashboard);
        LOGGER.debug("updateDashboardList() end duration --->{}",Calendar.getInstance().get(Calendar.SECOND)-seconds);
        
        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("notifyDashboardListUpdate() start --->{}");
        notifyDashboardListUpdate();
        LOGGER.debug("notifyDashboardListUpdate() end duration --->{}",Calendar.getInstance().get(Calendar.SECOND)-seconds);
        
        seconds = Calendar.getInstance().get(Calendar.SECOND);
        LOGGER.debug("updateRecentDashboards() start --->{}");
        updateRecentDashboards();
        LOGGER.debug("updateRecentDashboards() end duration --->{}",Calendar.getInstance().get(Calendar.SECOND)-seconds);
        
         
        LOGGER.debug("deleteComposition() End total time --->{}",Calendar.getInstance().get(Calendar.SECOND)-startTime);
    }

    private void openDashboard(DashboardConfig config) {
        String userId = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
        Composition composition =null;
        try {
            composition = config.getDashboard().getComposition();
            
            dashboardAccessLog(config.getDashboard());
            
            config.setDashboardComponent(thumbnailLayout);
            config.setComposition(composition);
            config.setHomeTabbox(homeTabbox);
            config.setAnchorlayout(thumbnailLayout);
            
            if(!config.getDashboard().isStaticData()) {
                CompositionInstance mostRecentCI = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getmostRecentInstance(composition, true);
                config.setMostRecentCI(mostRecentCI);
                 if(Flow.VIEW == config.getFlow()){
                    if (showCustomnotification(mostRecentCI)) {
                        return;
                    }
                 }
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            //All checked exception are from HIPIE, So displaying the message directly 
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        
        if (Flow.EDIT == config.getFlow()
                && !HipieSingleton.getHipie().getPermissionsManager()
                        .userCanEdit(userId, composition)) {
            
            Clients.showNotification(Labels.getLabel("youDonotHavePermissionToEditDashboard"),
                    Clients.NOTIFICATION_TYPE_ERROR, HomeController.this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            return;
        }
        
        checkStaticdataAndOpen(config, composition);
    }

    private void checkStaticdataAndOpen(DashboardConfig config, Composition composition) {
        if(config.isStaticData()) {
            openDashboardTab(config); 
        } else {
            validateCompositionAndProceed(config, composition);
            try {
                boolean accessLog = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .logCompositionAccess(composition.getId(), user.getId());
                if (!accessLog) {
                    LOGGER.info(Labels.getLabel("notAbleToLogDashboardAccess"));
                }
            } catch (DatabaseException e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }
    }

    private boolean showCustomnotification(CompositionInstance mostRecentCI) {
        if (mostRecentCI == null) {
            Clients.showNotification(Labels.getLabel("processNotAvailableToView"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            return true;
        } else if (!DashboardUtil.isWorkUnitComplete(mostRecentCI)) {
            Clients.showNotification(Labels.getLabel("processNotComplete"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000);
            return true;
        }
        return false;
    }

    private void validateCompositionAndProceed(DashboardConfig config, Composition composition) {
        //Validate the composition before proceeding
        boolean hasErrors = RampsUtil.checkCompositionError(composition,this.getSelf());
        
        Dashboard dashboard = config.getDashboard();
        
        if(!hasErrors){
            if(dashboard.isStaticData()) {
                openDashboardTab(config);
            } else if (dashboard.getHpccConnection() == null) {
                createNewDashboardWindow(config);
            } else {
                try {
                    if(dashboard.getHpccConnection().testConnection()){
                        openDashboardTab(config);
                    }
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION,e);
                    Clients.showNotification(Labels.getLabel("hpccConnectionFailed")+": "+e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
                }
            }
        }
    }
    
    private void updateRecentDashboards() {
        ListModelList<Object> model = (ListModelList<Object>) searchListBox.getModel();
        if (model != null) {
            model.clear();
        }
        searchTextBox.setValue("");
        listHead.setVisible(true);
        List<Dashboard> recentDashboards = dashboards;

        DashboardUtil.sortDashboards(recentDashboards);
        ListModelList<Dashboard> dashboardmodel = new ListModelList<Dashboard>();
        if (recentDashboards.size() >= 5) {
            dashboardmodel.addAll(recentDashboards.subList(0, 4));
        } else {
            dashboardmodel.addAll(recentDashboards);
        }

        searchListBox.setModel(dashboardmodel);
        searchListBox.setItemRenderer(this::searchboxRenderer);
        
    }

    @SuppressWarnings("unchecked")
    private void createDashboardTab(DashboardConfig dashboardconfig) {
        Tab tab = new Tab();
        Tabpanel tabpanel = new Tabpanel();

        tab.setLabel(dashboardconfig.getDashboard().getLabel());
        dashboardconfig.setDashboardTab(tab);
        tab.setAttribute(Constants.DASHBOARD, dashboardconfig.getDashboard());

        tab.setAttribute(Constants.COMPOSITION, dashboardconfig.getComposition());
        tab.setClosable(true);
        tab.addEventListener(Events.ON_CLOSE, event -> {
            Dashboard dashboard = (Dashboard) event.getTarget().getAttribute(Constants.DASHBOARD);
            dashboard.setChanged(false);
            
            if (dashboard.isRunning()) {
                event.stopPropagation();
                Clients.showNotification(Labels.getLabel("cannotCloseDashboard"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                        Constants.POSITION_TOP_CENTER, 3000, true);
            }

            Set<String> fileNameSet = (Set<String>) Sessions.getCurrent().getAttribute(Constants.OPEN_DASHBOARD_LABELS);
            if (fileNameSet != null && fileNameSet.contains(dashboard.getName())) {
                fileNameSet.remove(dashboard.getLabel());
            }
        });
      
        Include include = new Include();
        include.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardconfig);
        include.setSrc("dashboard/dashboardDetails.zul");
        tabpanel.appendChild(include);
        homeTabbox.getTabs().appendChild(tab);
        homeTabbox.getTabpanels().appendChild(tabpanel);

        tab.setSelected(true);
        tab.addEventListener(Events.ON_SELECT, event -> selectTab(dashboardconfig, tab));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("create tab dahboard name ---> {}", dashboardconfig.getDashboard().getName());
        }
    }

    private void selectTab(DashboardConfig dashboardconfig, Tab tab) {
        if (tab.getAttribute(Constants.DASHBOARD_HOLDER_UUID) != null) {
            RampsUtil.resizeDashboard((String)tab.getAttribute(Constants.DASHBOARD_HOLDER_UUID));
        }
    }

    @Listen("onClick = #newDashboard")
    public void createDashboard()  {
        try {
            if (getSelf().query("#createNewDashboardWindow") != null) {
                LOGGER.error(
                        "Cannot create more than one component with the same id, must be an error due to user action(Clicking same button more than once)");
                return;
            }
            Composition composition = null;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating new Dashbaord");
            }

            DashboardConfig dashboardConfig = new DashboardConfig();
            dashboardConfig.setFlow(Flow.NEW);
            dashboardConfig.setHomeTabbox(homeTabbox);
            dashboardConfig.setDashboard(new Dashboard());
            composition = HipieSingleton.getHipie().getCompositionTemplate(
                    ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(),
                    Dashboard.DASHBOARD_TEMPLATE);
            if (composition == null) {
                throw new HipieException(UNABLE_TO_LOAD_DASHBOARD_TEMPLATE);
            }

            dashboardConfig.setComposition(composition);
            Map<Integer, DashboardConfig> newDashboardData = new HashMap<Integer, DashboardConfig>();
            newDashboardData.put(1, dashboardConfig);
            Window window = (Window) Executions.createComponents(NEW_DASHBOARD_URI, getSelf(), newDashboardData);
            window.doModal();
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            Clients.showNotification(UNABLE_TO_LOAD_DASHBOARD_TEMPLATE, Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
    }
    private void openCompositionTab(DashboardConfig config) {
		//get access to the component with Path.getComponent, css selector-like selector. 
		// the double-slash represents the page ID, followed by element id of full path. 
		// homepage is the page ID of index.zul, component for homewindow is dsp.controller.HomeController
		//	enables access to methods in dsp.homecontroller
     	Component comp=Path.getComponent("//homePage/homeWindow/");
		Composition C=config.getComposition();
		Events.postEvent(EVENTS.ON_OPEN_RAMPS_PERSPECTIVE,comp, config); 
   
    }
    private void openDashboardTab(DashboardConfig config) {
    	 Dashboard dashboardToOpen = config.getDashboard();
        
        ErrorBlock error = config.getComposition().getParseErrors().getErrors();
        if (config.isStaticData() || error.isEmpty()) {
            if (isOpen(dashboardToOpen)) {
                selectOpenedTab(dashboardToOpen);
                return;
            }
            config.setAnchorlayout(thumbnailLayout);
            createDashboardTab(config);
        } else {
            Iterator<HError> iterator = error.iterator();
            if (iterator.hasNext()) {
                Clients.showNotification(Labels.getLabel("errorInComposition").concat(iterator.next().toString()), Clients.NOTIFICATION_TYPE_ERROR,
                        getSelf(), Constants.POSITION_TOP_CENTER, 50000, true);
                return;
            }
        }
    }
     
    protected void updateDashboardList(Dashboard dashboard) {
        dashboards.remove(dashboard);
        dashboardModel.remove(dashboard);
        dashboardGrid.getDashboards().remove(dashboard);
        // Need to enable, if duplicate composition allowed
        Clients.showNotification(Labels.getLabel("dashboardDeleted"), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER,
                3000, true);
    }
    
    private void notifyDashboardListUpdate() {
        DashboardGrid dashGrid = (DashboardGrid) thumbnailLayout.getAttribute(Constants.DASHBOARD);
        BindUtils.postNotifyChange(null, null, dashGrid, DASHBOARD_GRID_PROPERTY);
    }
    
    protected void closeTab(Dashboard dashboard) {
        Tab tab;
        Dashboard tabDash;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabDash= (Dashboard) tab.getAttribute(Constants.DASHBOARD);
            try {
                if (tabDash != null && dashboard.getComposition().getId().equals(tabDash.getComposition().getId())) {
                    tab.close();
                    LOGGER.info("Dahboard tab closed :: "+dashboard.getComposition().getCanonicalName());
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
           
        }

    }
    
    @Listen("onOK = #searchTextBox")
    public void enter() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            searchBoxEnter();
        }else{
            promoteMessageBox("searchBoxEnter");
        }
    }
    
    @Listen("onClick = #clearFilter")
    public void viewAllProjects() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            clearFilter();
        }else{
            promoteMessageBox("ClearFilterClick");
        }
    }
    public void clearFilter(){
        CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
        try {
            dashboards = compositionService.filterDashboards(user, true, true, getCompositions());
            dashboards = updateFavouriteDashboard(dashboards);
        } catch (CompositionServiceException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        }
        dashboardModel.clear();
        dashboardModel.addAll(dashboards);
        dashboardGrid.setDashboards(dashboards);
        notifyDashboardListUpdate();
        clearFilter.setVisible(false);
        Column labelCol = (Column) entityList.getColumns().getChildren().iterator().next();
        labelCol.sort(true, true);
        gridSortDesc(dateItemDashboard, CompositionUtil.SORT_BY_DATE_DES);
        
    }
    private Collection<Dashboard> getSearchedDashboards() {
        List<Dashboard> searched=new ArrayList<Dashboard>();
        try {
            searched= getCompositions()
            .stream()
                    .filter(dashboard -> StringUtils.containsIgnoreCase(dashboard.getName(), searchTextBox.getValue())
                            || StringUtils.containsIgnoreCase(dashboard.getAuthor(), searchTextBox.getValue())
                            || StringUtils.equals(dashboard.getUuid(), searchTextBox.getValue())
                            || StringUtils.containsIgnoreCase(dashboard.getLabel(), searchTextBox.getValue())).collect(Collectors.toList());
            
            CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
            compositionService.assignClusterToDshboard(searched);
                
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return null;
        } 
        return searched;
        
    }
    

    @Listen("onChanging = #searchTextBox")
    public void filterProjects(InputEvent event) {
        if (event.getValue() == null || event.getValue().isEmpty()) {
            updateRecentDashboards();
            return;
        }
        ListModelList<Dashboard> filteredDashboards = new ListModelList<Dashboard>();
           
        try {
            List<Dashboard> recentDashboard=getCompositions()
                  .stream()
                  .filter(dashboard -> StringUtils.containsIgnoreCase(dashboard.getLabel(), event.getValue())
                          || StringUtils.containsIgnoreCase(dashboard.getName(), event.getValue())
                          || StringUtils.equals(dashboard.getUuid(), event.getValue())
                          || StringUtils.containsIgnoreCase(dashboard.getAuthor(), event.getValue())).collect(Collectors.toList());
            
            CompositionService compositionService = (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
            compositionService.assignClusterToDshboard(recentDashboard);
            
            filteredDashboards.addAll(recentDashboard);
            searchListBox.setModel(filteredDashboards);
            searchListBox.setItemRenderer(this::searchboxRenderer);
            listHead.setVisible(false); 
        } catch (CompositionServiceException | DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showErrorNotification(e.getMessage());
            return;
        } 
    }
    
    @Listen("onSelect = #searchListBox")
    public void createNewFilteredProject(SelectEvent<Component, Dashboard> event) {
        Dashboard selectedDashboard = event.getSelectedObjects().iterator().next();
        if(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canViewProject()) {
            openDashbordFromSearch(selectedDashboard);
        }
        searchPopup.close();
    }

    private void openDashbordFromSearch(Dashboard selectedDashboard) {
        try {
            DashboardConfig dashboardConfig = new DashboardConfig();
            dashboardConfig.setComposition(selectedDashboard.getComposition());
            dashboardConfig.setFlow(Constants.Flow.VIEW);
            dashboardConfig.setDashboard(selectedDashboard);
            openDashboard(dashboardConfig);
            updateRecentDashboards();
            
            searchTextBox.setValue("");
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
        }
    }
    
    protected void selectOpenedTab(Dashboard dash) {
        Tab tab;
        Dashboard tabDash;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabDash= (Dashboard) tab.getAttribute(Constants.DASHBOARD);
            try {
                if (tabDash != null && dash.getComposition().getId().equals(tabDash.getComposition().getId())) {
                    tab.setSelected(true);
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
           
        }
    }
    
    @Listen("onClick = #authorItemDashboard")
    public void sortByAuthor() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(authorItemDashboard);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,authorItemDashboard));
        }
    }
    
    @Listen("onClick = #dateItemDashboard")
    public void sortByDate() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(dateItemDashboard);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,dateItemDashboard));
        }
    }
    
    @Listen("onClick = #nameItemDashboard")
    public void sortByName() {
        if(CollectionUtils.isEmpty(promotionEntities)){
            gridViewSort(nameItemDashboard);
        }else{
            Messagebox.show(Labels.getLabel(PROMOTE_CONFIRMATION_MESSAGE)  + "?",
                    Labels.getLabel(PROMOTE_CONFIRMATION_HEADER), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, (EventListener<ClickEvent>) event -> confirmSort(event,nameItemDashboard));
        }
    }
    public void  confirmSort(ClickEvent event, Menuitem item) {
        if (Messagebox.Button.YES.equals(event.getButton())) {
            clearPromoteSelection();
            gridViewSort(item);
        }
    }
    private void sortGridView(Comparator<GridEntity> sortType){
        Collections.sort(dashboards, sortType);
        dashboardGrid.setDashboards(dashboards);
        BindUtils.postNotifyChange(null, null, dashboardGrid, DASHBOARD_GRID_PROPERTY);
    }
    public void gridViewSort(Menuitem item) {
        toggleSortType(item, CompositionUtil.SORT_BY_NAME_ASC, CompositionUtil.SORT_BY_NAME_DES, getView());
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
        if (item != nameItemDashboard) {
            nameItemDashboard.setAttribute(toClear, null);
        }
        if (item != authorItemDashboard) {
            authorItemDashboard.setAttribute(toClear, null);
        }
        if (item != dateItemDashboard) {
            dateItemDashboard.setAttribute(toClear, null);
        }
    }

    private void refreshSortIcons() {
        String gridSort = null;

        if (getView() == VIEW.GRID) {
            gridSort = (String) nameItemDashboard.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, nameItemDashboard);

            gridSort = (String) dateItemDashboard.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, dateItemDashboard);

            gridSort = (String) authorItemDashboard.getAttribute(GRID_SORT_TYPE);
            setSortIcon(gridSort, authorItemDashboard);
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
    
    private void showErrorNotification(String message){
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
    }
    
    protected boolean isOpen(Dashboard dashToOpen) {
        boolean exists=false;
        Tab tab;
        Dashboard tabDash;
        for (Component comp : homeTabbox.getTabs().getChildren()) {
            tab = (Tab) comp;
            tabDash= (Dashboard) tab.getAttribute(Constants.DASHBOARD);
            try {
                if (tabDash != null && dashToOpen.getComposition().getId().equals(tabDash.getComposition().getId())) {
                    exists=true;
                    LOGGER.info("Dashboard is opened in another tab :: "+dashToOpen.getComposition().getCanonicalName());
                    break;
                }
            } catch (HipieException e) {
               LOGGER.error(Constants.ERROR, e);
            }
        }
        return exists;
    }
    
    private void markAsFavouriteDashbaord(Dashboard dashboard){
        try {
            if (dashboard.getIsFavourite()) {
                // code to unFav a dashboard
                // change the star accordingly
                boolean onComplete = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .unMarkAsFavoriteComposition(dashboard.getComposition().getId(), user.getId());
                if(onComplete){
                    dashboard.setIsFavourite(false);
                    favDashboards.remove(dashboard.getComposition().getId());
                }
            } else {
                // code to Fav a dashboard
                // change the star accordingly
                boolean onComplete = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .markAsFavoriteComposition(dashboard.getComposition().getId(), user.getId());
                if(onComplete){
                    dashboard.setIsFavourite(true);
                    favDashboards.add(dashboard.getComposition().getId());
                }
            }
        } catch (DatabaseException | HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }
    
    private List<Dashboard> updateFavouriteDashboard(List<Dashboard> dashboards) {
        dashboards.forEach(dash -> {
            try {
                if(favDashboards.contains(dash.getComposition().getId())){
                    dash.setIsFavourite(true);
                }
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        });
        return dashboards;
    }
    
    private void dashboardAccessLog(Dashboard dashboard){
        try {
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
            .logCompositionAccess(dashboard.getComposition().getId(), user.getId());
        } catch (DatabaseException | HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }
    
}
