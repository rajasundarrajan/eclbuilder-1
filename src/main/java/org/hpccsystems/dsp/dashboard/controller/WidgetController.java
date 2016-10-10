package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.error.HipieErrorCode;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartConfiguration;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.entity.widget.SortField;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Include;
import org.zkoss.zul.Tab;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class WidgetController extends SelectorComposer<Component> {
    private static final String CONFIGURED_TAB_STYLE = "config-tab configured";
    private static final String TAB_STYLE = "config-tab";

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(WidgetController.class);

    @Wire
    private Tab chooseWidgetTab;

    @Wire
    private Include chartListInclude;

    @Wire
    private Include configureInclude;
    @Wire
    private Tab configureTab;

    @Wire
    private Tab datasourceTab;
    @Wire
    private Include datasourceInclude;

    private WidgetConfig widgetConfig;
    private DashboardConfig dashboardConfig;
  
    @Wire
    private Button datasourcePrevious;
    @Wire
    private Button  cancel2;
    @Wire
    private Button datasourceNext;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getArg().get(Dashboard.WIDGET_CONFIG);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);

        widgetConfig.setWidgetCanvas(comp);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboardConfig-->{}", dashboardConfig.getDashboard().getClusterConfig());
            LOGGER.debug("composition-->{}", dashboardConfig.getComposition());
            LOGGER.debug("Datasource used: Static - {}", dashboardConfig.getDashboard().isStaticData());
            LOGGER.debug("widgetConfig-->{}", widgetConfig.getIndex());
        }
        if (widgetConfig.getWidget() != null && widgetConfig.getWidget().isConfigured()) {
            chooseWidgetTab.setSclass(CONFIGURED_TAB_STYLE);
            datasourceTab.setSclass(CONFIGURED_TAB_STYLE);
            configureTab.setSclass(CONFIGURED_TAB_STYLE);
        }

        loadWidgetSelection();
        if (!widgetConfig.isNewCreation()) {
            selectWidgetConfig();
        }
        this.getSelf().addEventListener(EVENTS.ON_CLICK_IMPORT, (SerializableEventListener<? extends Event>)event -> {
            datasourcePrevious.setDisabled(true);
            datasourceNext.setDisabled(true);
            cancel2.setDisabled(true);
        });
        this.getSelf().addEventListener(EVENTS.ON_CLICK_IMPORT_CLOSE,(SerializableEventListener<? extends Event>) event -> {
            datasourcePrevious.setDisabled(false);
            datasourceNext.setDisabled(false);
            cancel2.setDisabled(false);
        });
    }

    @Listen("onClick = #closeBtn")
    public void closeWidgetConfiguration() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close button Clicked.!");
        }
        Events.postEvent(Dashboard.EVENTS.ON_WIDGET_CONFIG_CLOSE, widgetConfig.getDashboardCanvas(), null);
    }

    @Listen("onClick = #datasourcePrevious; onSelect = #chooseWidgetTab")
    public void loadWidgetSelection() {
        chooseWidgetTab.setSelected(true);
        chartListInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        chartListInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        chartListInclude.setSrc("dashboard/design/widget/chartList.zul");
    }

    @Listen("onClick = #chooseWidgetNext, #configurePrevious; onSelect = #datasourceTab")
    public void loadDatasource() {
        if (!saveWidgetSelection()) {
            return;
        }

        // Load datasource        
        datasourceInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        datasourceInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        datasourceTab.setSelected(true);
        chooseWidgetTab.setSclass(CONFIGURED_TAB_STYLE);
        loadDatasourcePage();
    }

    private void loadDatasourcePage() {
        if (dashboardConfig.isRAMPSConfig()) {
            datasourceInclude.setSrc("dashboard/design/widget/ramps_datasources.zul");
        } else {
            if(CollectionUtils.isNotEmpty(dashboardConfig.getDashboard().getWidgets()) && !dashboardConfig.getDashboard().isStaticData()){
                datasourceInclude.setSrc(null);
                datasourceInclude.setSrc("dashboard/design/widget/dashboard_datasource.zul");
            } else {
                datasourceInclude.setSrc("dashboard/design/widget/datasource.zul");  
                Events.postEvent(Constants.EVENTS.ON_SELECT_DATASOURCE, datasourceInclude.getFirstChild(), null);
            }
         
        }
    }

    /**
     * Saves Chart list screen - Chart Name & Type
     * 
     * @return Whether the screen is saved with all properties required When
     *         properties are not defined, Chart list screen is focused &
     *         validation is triggered
     */
    private boolean saveWidgetSelection() {
        Events.sendEvent(Dashboard.EVENTS.ON_UPDATE_WIDGET_CONFIG, chartListInclude.getFirstChild(), widgetConfig);
        boolean isSaved = isChartSelectionConfigured();
        if (!isSaved) {
            chooseWidgetTab.setSelected(true);
            setNotConfiguredStyleForTabs();
            Events.postEvent(Dashboard.EVENTS.ON_VALIDATE_CONFIG, chartListInclude.getFirstChild(), widgetConfig);
        }
        return isSaved;
    }

    private void setNotConfiguredStyleForTabs() {
        chooseWidgetTab.setSclass(TAB_STYLE);
        datasourceTab.setSclass(TAB_STYLE);
        configureTab.setSclass(TAB_STYLE);
    }

    private boolean isChartSelectionConfigured() {
        return !StringUtils.isEmpty(widgetConfig.getChartname()) && widgetConfig.getChartType() != null && !CompositionUtil.isChartNameDuplicate(dashboardConfig, widgetConfig);
    }

    private boolean saveDatasourceSelection() {
        Events.sendEvent(Dashboard.EVENTS.ON_UPDATE_WIDGET_CONFIG, chartListInclude.getFirstChild(), widgetConfig);
        boolean savedChartSelection = isChartSelectionConfigured();
        boolean savedDatasource = isDatasourceConfigured();
        if (!savedChartSelection) {
            chooseWidgetTab.setSelected(true);
            setNotConfiguredStyleForTabs();
            Events.postEvent(Dashboard.EVENTS.ON_VALIDATE_CONFIG, chartListInclude.getFirstChild(), widgetConfig);
        } else if (!savedDatasource) {
            datasourceTab.setSelected(true);
            datasourceTab.setSclass(TAB_STYLE);
            configureTab.setSclass(TAB_STYLE);
            Events.postEvent(Dashboard.EVENTS.ON_VALIDATE_CONFIG, datasourceInclude.getFirstChild(), widgetConfig);
        }
            LOGGER.debug("savedChartSelection -->{}", savedChartSelection);
            LOGGER.debug("savedDatasource -->{}", savedDatasource);
        return savedChartSelection && savedDatasource;
    }

    private boolean isDatasourceConfigured() {
        boolean isDatasourceAdded = false;
        if (dashboardConfig.isRAMPSConfig()) {
            isDatasourceAdded = widgetConfig.getDatasource() != null && widgetConfig.getDatasource().getOutputElement() != null;
        } else {
            if (widgetConfig.getDatasourceType() == DATASOURCE.QUERY || widgetConfig.getDatasourceType() == DATASOURCE.STATIC_DATA) {
                isDatasourceAdded = !org.apache.commons.lang3.StringUtils.isEmpty(widgetConfig.getQueryName());
                LOGGER.debug("is data source configured------->{}",isDatasourceAdded);
            } else {
                    LOGGER.debug("Datasource - {}", widgetConfig.getDatasource());
                isDatasourceAdded = widgetConfig.hasValidLogicalFile();
            }
        }
        return isDatasourceAdded;
    }

    private void selectWidgetConfig() {
        ChartConfiguration chartConfiguration = Dashboard.CHARTS_CONFIGURATION.get(widgetConfig.getChartType());
        datasourceTab.setSclass(CONFIGURED_TAB_STYLE);
        if(widgetConfig.getWidget().hasDatasource() || widgetConfig.getWidget().isDatabomb()){
            configureTab.setSelected(true);
        }else{
            datasourceTab.setSelected(true);
            datasourceInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
            datasourceInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
            loadDatasourcePage();
        }

        configureInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        configureInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        configureInclude.setSrc(null);
        configureInclude.setSrc(chartConfiguration.getEditLayout());
    }

    @Listen("onClick = #datasourceNext; onSelect = #configureTab")
    public void loadWidgetConfig() {
        if (datasourceInclude.getFirstChild() == null) {
            loadDatasource();
            return;
        }
        if (!saveDatasourceSelection()) {
            return;
        }
        
        ChartConfiguration chartConfiguration = Dashboard.CHARTS_CONFIGURATION.get(widgetConfig.getChartType());
        Widget widget = widgetConfig.getWidget();
        
        addUnusedFileAndQuery();
        
        widget = afterDatasourceOrCharttypeChange(chartConfiguration, widget);

        widget.setName(DashboardUtil.removeSpaceSplChar(widgetConfig.getChartname()));
        widget.setTitle(widgetConfig.getChartname());
        widget.setDatasource(widgetConfig.getDatasource());
        widget.setDatasourceType(widgetConfig.getDatasourceType());
        try{
            List<String> blacklist = ((LogicalFileService)SpringUtil.getBean(
                    Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles();
            if (blacklist != null && widget.getDatasource() != null && 
                    widget.getDatasource().getLabel() != null && 
                    !blacklist.contains(widget.getDatasource().getLabel().replace("~", ""))) {
                widget.getDatasource().setFileNotExists(false);             
            }else if (widget.getDatasourceType() != DATASOURCE.STATIC_DATA && 
                    widget.getDatasourceType() != DATASOURCE.QUERY){
                Clients.showNotification(Labels.getLabel("invalidFile"), Clients.NOTIFICATION_TYPE_ERROR, 
                        getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            }
        }catch(Exception e){
            Clients.showNotification(Labels.getLabel("invalidFile"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            LOGGER.error(Constants.EXCEPTION, e);
        }
        
        // If this widget config already has a query name set remove it from the dashboard config
        // (this function will not remove the query if it is use in a different widget)
        if (widgetConfig.getWidget() != null && widgetConfig.getWidget().getQueryName() != null) {
            dashboardConfig.getDashboard().removeQuery(widgetConfig.getWidget());
        }

        if (widgetConfig.getDatasourceType() == DATASOURCE.QUERY || widgetConfig.getDatasourceType() == DATASOURCE.STATIC_DATA) {
            widget.setQueryName(widgetConfig.getQueryName());
        }
        
        if(dashboardConfig.getDashboard().isStaticData()){
            widget.setDatasourceType(DATASOURCE.STATIC_DATA);
        }
        
        if (widget.getSortFields() != null) {
            removeInvalidSorts(widget);
        }
        
        if (widget.getFilters() != null) {
            removeInvalidFilters(widget);
        }

        widget.setChartConfiguration(chartConfiguration);
        widgetConfig.setWidget(widget);
        selectWidgetConfig();
    }
    
    /**
     * Checks the sort fields in the widget. If the widget fields does not contain a 
     * field with the same name as the field in the sorts then remove the sort from 
     * the widget
     * @param widget
     */
    private void removeInvalidSorts(Widget widget) {
        List<SortField> sortFields = widget.getSortFields();
        List<SortField> invalidSortFields = new ArrayList<SortField>();
        for (SortField sortField: sortFields) {
            for(Field field: widget.getDatasourceFields()){
                boolean validSortField = false;
                if(field.getColumn().equalsIgnoreCase(sortField.getColumn()) && 
                        field.getDataType().equalsIgnoreCase(sortField.getDataType())){
                    validSortField = true;
                    break;
                }
                if(!validSortField){
                    invalidSortFields.add(sortField);
                }
                    
            }
        }
        for (SortField field: invalidSortFields) {
            widget.removeSortField(field);
            LOGGER.debug("Removed field " + field.getColumn() + " from sort fields.");
        }
    }
    
    /**
     * Checks the local filters in the widget. If there is no datasource field with 
     * the same column name as the filter then remove the filter from the widget.
     * @param widget
     */
    private void removeInvalidFilters(Widget widget) {
        List<Filter> filters = widget.getFilters();
        List<Filter> invalidfilters = new ArrayList<Filter>();
        // Get field column names to compare filters against.
        List<String> datasourceFieldColumns = widget.getDatasourceFields().stream()
                .filter(filter -> filter.getColumn() != null)
                .map(Field::getColumn)
                .collect(Collectors.toList());
        
        for (Filter filter: filters) {
            if (!datasourceFieldColumns.contains(filter.getColumn())) {
                invalidfilters.add(filter);
            }
        }
        for (Filter filter: invalidfilters) {
            widget.removeFilter(filter);
            LOGGER.debug("Removed filter " + filter.getColumn() + " from sort fields.");
        }
    }

    private void addUnusedFileAndQuery() {
        if(widgetConfig.isDatasourceUpdated()){
            if(widgetConfig.getDatasource() != null){
               if(widgetConfig.getUnusedFiles() == null){
                   widgetConfig.setUnusedFiles(new HashSet<>());
                }
               String selectedFile = widgetConfig.getDatasource().getContractInstance().getProperty(Constants.LOGICAL_FILENAME);
               widgetConfig.getUnusedFiles().add(selectedFile);
            }else if(widgetConfig.getQueryName() != null){
                if(widgetConfig.getUnusedQueries() == null){
                    widgetConfig.setUnusedQueries(new HashSet<>());
                 }
                 widgetConfig.getUnusedQueries().add(widgetConfig.getQueryName());
            }
        }
    }

    /**
     * Clean up the config so that it will be recreated every time
     * 
     * @param chartConfiguration
     * @param widget - A new instance of the widget if it has changed and the chartConfiguration has changed
     * @return
     */
    private Widget afterDatasourceOrCharttypeChange(ChartConfiguration chartConfiguration, Widget widget) {
        Widget wid = widget;
        if (wid == null || wid.getChartConfiguration().getType() != widgetConfig.getChartType() || isDatasourceChanged()) {
            // Setting null, so that the configuration will be recreated every time
            // Specifically to handle change in widget type
            configureInclude.setSrc(null);
            configureInclude.clearDynamicProperties();

            try {
                wid = (Widget) Class.forName(chartConfiguration.getClassName()).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                // Assuming this error will never occur, its not handled
                LOGGER.error(Constants.EXCEPTION, e);
            }

        }
        
        return wid;
    }

    /**
     * If the global widget filter column name is present in the widget fields
     * add the global filter to the widget.
     * @param wid
     */
    private void addGlobalFilterToNewWidget(Widget wid) {
        Widget globalFilters = dashboardConfig.getDashboard().getGlobalFilterWidget();
        if (globalFilters != null) {
            List<Filter> widgetGlobalFilters = new ArrayList<>();
            List<Field> widgetFields = wid.getDatasourceFields();
            for (Filter filter : globalFilters.getFilters()) {
                if (RampsUtil.isFieldPresent(widgetFields, filter)) {
                    widgetGlobalFilters.add(filter);
                }
            }
            
            if (CollectionUtils.isNotEmpty(widgetGlobalFilters)) {
                addGlobalFilter(wid,widgetGlobalFilters);
            }
        }
    }
    
    /**
     * Removes all global filters from the widget
     * @param wid
     */
    private void removeGlobalFilterFromWidget(Widget wid) {
        Widget globalFilters = dashboardConfig.getDashboard().getGlobalFilterWidget();
        if (globalFilters != null) {
            for (Filter filter: globalFilters.getFilters()) {
                wid.removeFilter(filter);
            }
        }
    }

    private void addGlobalFilter(Widget wid, List<Filter> widgetGlobalFilters) {
      //In new widget creation, if user dropped any local filter, don't remove it
        if(wid.getFilters() != null){                    
            List<Filter> widgetsFilters = wid.getFilters();
            //If widget has local filter similar to global filter, replace the local
            //filter with global one
            for (Filter globFilter : widgetGlobalFilters) {
                if (widgetsFilters.contains(globFilter)) {
                    widgetsFilters.remove(widgetsFilters.indexOf(globFilter));
                    widgetsFilters.add(globFilter);
                } else {
                    widgetsFilters.add(globFilter);
                }
            }
            
        }else{
            wid.setFilters(widgetGlobalFilters);
        }
    }

    /**
     * First validates the datasource's contractinstance against hipie. If the validation fails 
     * set the datasourceUpdated param to false and return.
     * 
     * @return - true if datasource is updated and validates, false if otherwise
     */
    private boolean isDatasourceChanged() {
        if (dashboardConfig.isRAMPSConfig()) {
            if (widgetConfig.getDatasource() == null || widgetConfig.getOriginalWidget() == null) {
                return false;
            }
            boolean isUpdated = !widgetConfig.getDatasource().equals(widgetConfig.getOriginalWidget().getDatasource());
            if(isUpdated){
                //check the datafields if datasource does not validate set isUpdated to false
               if(widgetConfig.getDatasource() != null && widgetConfig.getDatasource().getContractInstance() != null &&
                       widgetConfig.getDatasource().getContractInstance().validate().getErrors(HipieErrorCode.INVALID_PROPERTY_VALUE).size()==0){
                   isUpdated = false;
               }
            }
            return isUpdated;
        } else {
            boolean isUpdated = widgetConfig.isDatasourceUpdated();
            // Throwing null here when changing datasource because getDatasource() is null
            // Validate the contract instance for the datasource against hipie
            if(widgetConfig.getDatasource() != null && widgetConfig.getDatasource().getContractInstance() != null &&
                    widgetConfig.getDatasource().getContractInstance().validate().getErrors(HipieErrorCode.INVALID_PROPERTY_VALUE).size()==0){
                isUpdated = false;
            }
            widgetConfig.setDatasourceUpdated(false);
            return isUpdated;
        }
    }

    @Listen("onClick = #finish")
    public void saveWidget() {
        if (!widgetConfig.getWidget().isConfigured()) {
            Events.postEvent(EVENTS.ON_WIDGET_NOT_CONFIGURED, configureInclude.getFirstChild(), widgetConfig);
            return;
        }

        if (widgetConfig.isNewCreation()) {
            if(dashboardConfig.getFlow() == Flow.EDIT || dashboardConfig.getFlow() == Flow.CLONE){
                dashboardConfig.getDashboard().setChanged(true); 
            }
            
          //For newly adding widget, adds the global filters
            addGlobalFilterToNewWidget(widgetConfig.getWidget());
            updateWidgetFields();
            
            Events.postEvent(Dashboard.EVENTS.ON_WIDGET_CONFIG_SAVE, widgetConfig.getDashboardCanvas(), widgetConfig.getWidget());
        } else {
        	updateWidgetFields();
            setFilterValuecheckFlag(widgetConfig.getOriginalWidget());
            
            // If the widget has changed then we need to update the changed status for the dashboard.
            if((!widgetConfig.getOriginalWidget().equals(widgetConfig.getWidget())) ) {
                dashboardConfig.getDashboard().setChanged(true); 
            }else{
                if(!widgetConfig.isNewCreation() && 
                        widgetConfig.getOriginalWidget().isQueryBased() && 
                        widgetConfig.getWidget().isQueryBased()){
                    detectQueryBasedCompositionChanges();
                }
            }
            resetFilterValuecheckFlag(widgetConfig.getOriginalWidget());
            
            // call to delete old widget filter fields
            if (isDatasourceModified()) {
                removeGlobalFilterFromWidget(widgetConfig.getWidget());
                addGlobalFilterToNewWidget(widgetConfig.getWidget());
                dashboardConfig.removeGlobalFilters(widgetConfig.getOriginalWidget());
            }
            
            // Update the actual filter values before replacing the widget in the dashboard
            updateWidgetFields();
            
            if (!widgetConfig.getWidget().isQueryBased() && !widgetConfig.getWidget().isDatabomb()) {
                dashboardConfig.getComposition().
                        removeContractInstance(widgetConfig.getOriginalWidget().getDatasource().getContractInstance());
            }
            
            dashboardConfig.getDashboard().getWidgets().remove(widgetConfig.getIndex());
            dashboardConfig.getDashboard().getWidgets().add(widgetConfig.getIndex(), widgetConfig.getWidget());
            
            Events.postEvent(Dashboard.EVENTS.ON_WIDGET_CONFIG_UPDATE, widgetConfig.getDashboardCanvas(), widgetConfig.getWidget());
        }
        
        removeUnusedFileAndQuery();
    }
    
    private void updateWidgetFields() {
        if(widgetConfig.getWidget().getFilters() != null) {
            for (Filter filter: widgetConfig.getWidget().getFilters()) {
                filter.setValue(filter.getNewValue());
            }
        }
    }

    private void resetFilterValuecheckFlag(Widget originalWidget) {
        if(CollectionUtils.isNotEmpty(originalWidget.getFilters())){
            originalWidget.getFilters().forEach(filter->{
                filter.setCheckValue(false);
            });
        }  
    }

    private void setFilterValuecheckFlag(Widget originalWidget) {
        if(CollectionUtils.isNotEmpty(originalWidget.getFilters())){
            originalWidget.getFilters().forEach(filter->{
                filter.setCheckValue(true);
            });
        }        
    }

    private void removeUnusedFileAndQuery() {
        if(widgetConfig.getUnusedFiles() != null){
            widgetConfig.setUnusedFiles(null);
        }
        if(widgetConfig.getUnusedQueries() != null){
            widgetConfig.setUnusedQueries(null);
        }
    }

    @Listen("onClick = #cancel1; onClick = #cancel2; onClick = #cancel3")
    public void cancel() {
        closeWidgetConfiguration();
    }
    
    private void detectQueryBasedCompositionChanges(){
        if(dashboardConfig.getDashboard().getOriginalQueries() != null && dashboardConfig.getDashboard().getQueries() != null){
            QuerySchema originalQuerySchema= dashboardConfig.getDashboard().getOriginalQueries().get(widgetConfig.getWidget().getQueryName());
            QuerySchema querySchema = dashboardConfig.getDashboard().getQueries().get(widgetConfig.getWidget().getQueryName());
            if(widgetConfig.getOriginalWidget().getQueryName().equals(widgetConfig.getWidget().getQueryName())){
                if(!originalQuerySchema.equals(querySchema)){
                    dashboardConfig.getDashboard().setChanged(true);
                }
            }else{
                dashboardConfig.getDashboard().setChanged(true);
            }
        }  
    }
    
    public boolean isDatasourceModified(){
        boolean removeField = false;
        // Check for datasource type changed from FILE to QUERY and vice versa
        if (widgetConfig.getOriginalWidget().getDatasourceType().equals(widgetConfig.getWidget().getDatasourceType())) {
            // handles Logical files
            if (widgetConfig.getWidget().isFileBased()) {
                //If the file is changed, need to remove the previous file's global filters
                if ((widgetConfig.getOriginalWidget() == null || widgetConfig.getOriginalWidget().getDatasource() == null) || !(widgetConfig.getOriginalWidget().getDatasource().equals(widgetConfig.getWidget().getDatasource()))) {
                    removeField = true;
                }
            } else {
                // handles Query and Static data
                //If the Query is changed, need to remove the previous Query's global filters
                if (widgetConfig.getOriginalWidget().getQueryName().equals(widgetConfig.getWidget().getQueryName())) {
                    //If the Query is same but Queryoutput is changed, need to remove the previous Queryoutput's global filters
                    if (!(widgetConfig.getOriginalWidget().getQueryOutput().equals(widgetConfig.getWidget().getQueryOutput()))) {
                        removeField = true;
                    }
                } else {
                    removeField = true;
                }
            }
        } else {
            removeField = true;
        }
        return removeField;
    }
    
}
