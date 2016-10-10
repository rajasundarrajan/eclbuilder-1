package org.hpccsystems.dsp.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.dude.option.SelectElementOption;
import org.hpcc.HIPIE.dude.option.SelectElementOption.EventType;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.GridEntity;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartConfiguration;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartInfo;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.HipieCharts;
import org.hpccsystems.dsp.dashboard.entity.widget.Interaction;
import org.hpccsystems.dsp.dashboard.entity.widget.InteractionTarget;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dashboard extends GridEntity implements Cloneable {
   
    private static final long serialVersionUID = 1L;
    private static final String PIE_CHART = "pie-chart";
    private static final String ORG_HPCCSYSTEMS_DSP_DASHBOARD_ENTITY_WIDGET_CHART_PIE = "org.hpccsystems.dsp.dashboard.entity.widget.chart.Pie";
    private static final String DASHBOARD_DESIGN_WIDGET_PIE_ZUL = "dashboard/design/widget/pie.zul";
    private static final String BAR_CHART = "bar-chart";
    private static final String XY_CHART_PACKAGE = "org.hpccsystems.dsp.dashboard.entity.widget.chart.XYChart";
    private static final String XY_CHART_ZUL = "dashboard/design/widget/xyChart.zul";
    public static final Map<ChartType, ChartConfiguration> CHARTS_CONFIGURATION = new LinkedHashMap<ChartType, ChartConfiguration>();

    static {
        CHARTS_CONFIGURATION.put(ChartType.PIE, new ChartConfiguration(ChartType.PIE, "Pie Chart", ORG_HPCCSYSTEMS_DSP_DASHBOARD_ENTITY_WIDGET_CHART_PIE,
                "dashboard/assets/image/charts/pie", PIE_CHART, DASHBOARD_DESIGN_WIDGET_PIE_ZUL, HipieCharts.PIE.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.DONUT, new ChartConfiguration(ChartType.DONUT, "Donut", ORG_HPCCSYSTEMS_DSP_DASHBOARD_ENTITY_WIDGET_CHART_PIE,
                "dashboard/assets/image/charts/donut", PIE_CHART, DASHBOARD_DESIGN_WIDGET_PIE_ZUL, HipieCharts.PIE.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.PYRAMID, new ChartConfiguration(ChartType.PYRAMID, "Pyramid", ORG_HPCCSYSTEMS_DSP_DASHBOARD_ENTITY_WIDGET_CHART_PIE,
                "dashboard/assets/image/charts/pyramid", PIE_CHART, DASHBOARD_DESIGN_WIDGET_PIE_ZUL, HipieCharts.PYRAMID.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.LINE, new ChartConfiguration(ChartType.LINE, "Line Chart", XY_CHART_PACKAGE, "dashboard/assets/image/charts/line",
                "line-chart", XY_CHART_ZUL, HipieCharts.LINE.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.BAR, new ChartConfiguration(ChartType.BAR, "Bar Chart", XY_CHART_PACKAGE, "dashboard/assets/image/charts/bar", BAR_CHART,
                XY_CHART_ZUL, HipieCharts.BAR.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.COLUMN, new ChartConfiguration(ChartType.COLUMN, "Column Chart", XY_CHART_PACKAGE, "dashboard/assets/image/charts/column",
                BAR_CHART, XY_CHART_ZUL, HipieCharts.COLUMN.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.STACKCOLUMN, new ChartConfiguration(ChartType.STACKCOLUMN, "Stacked Column Chart", XY_CHART_PACKAGE, "dashboard/assets/image/charts/stacked_clustered_column", BAR_CHART,
                XY_CHART_ZUL, HipieCharts.STACKCOLUMN.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.US_MAP, new ChartConfiguration(ChartType.US_MAP, "US_Map", "org.hpccsystems.dsp.dashboard.entity.widget.chart.USMap",
                "dashboard/assets/image/charts/geo", "globe", "dashboard/design/widget/usMap.zul", HipieCharts.US_MAP.getChartName()));
        CHARTS_CONFIGURATION.put(ChartType.TABLE, new ChartConfiguration(ChartType.TABLE, "Table Widget", "org.hpccsystems.dsp.dashboard.entity.widget.chart.Table",
                "dashboard/assets/image/charts/table", "table", "dashboard/design/widget/table.zul", HipieCharts.TABLE.getChartName()));

        CHARTS_CONFIGURATION.put(ChartType.SCORED_SEARCH,
                new ChartConfiguration(ChartType.SCORED_SEARCH, "Scored Search", "org.hpccsystems.dsp.dashboard.entity.widget.chart.ScoredSearch",
                        "dashboard/assets/image/charts/scored_search", "", "dashboard/design/widget/scoredSearch.zul", HipieCharts.SCORED_SEARCH.getChartName()));
        
        CHARTS_CONFIGURATION.put(ChartType.GLOBAL_FILTER, new ChartConfiguration(ChartType.GLOBAL_FILTER, "Global Filter", "org.hpccsystems.dsp.dashboard.entity.widget.chart.GlobalFilter",
                "", "", "", ""));
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Dashboard.class);

    public static final String COMMA = " , ";
    public static final String WIDGET = "widget";
    public static final String WIDGET_CONFIG = "widgetConfig";
    public static final String TRUE = "true";
    public static final String ON_SELECT = "onSelect";
    public static final String DASHBOARD_REPO = "dashboardRepo";

    public static final String CONTRACT_CATAGORY = "VISUALIZE";
    public static final String MOST_RECENT_CI = "mostRecentCI";
    public static final String LOGICAL_FILE = "logicalFile";
    public static final String QUERY = "query";
    public static final String DASHBOARD_TEMPLATE = "DashboardTemplate";
    public static final String QUERY_OUTPUT = "QueryOutput";

    /**
     * Appended with name while creating Dashboard visualization DUD files
     */
    public static final String CONTRACT_IDENTIFIER = "dashboard";
    /**
     * Appended with name 'ScoredSearch' while creating ScoredSearch
     * visualization DUD files
     */
    public static final String SCORED_SEARCH = "scoredsearch";
    
    public static final String DATA_BOMB_SUFFIX = "databomb";
    public static final String BACKUP_SUFFIX = "-bck";
    
    public static final String DASHBOARD_MODE = "_MODE";
    public static final String DASHBOARD_MODE_ADVANCED = "ADVANCED";

    // Style class names
    public static final String STYLE_POPUP = "popup";
    public static final String EXCEPTION = "Exception";
    public static final String FALSE = "false";
    public static final String COMPOSITION_NAME = "compositionName";
    public static final String PARENT = "parent";

    public class EVENTS implements Serializable {
        private static final long serialVersionUID = 1L;
        public static final String ON_WIDGET_CONFIG_CLOSE = "onWidgetConfigClose";
        public static final String ON_WIDGET_CONFIG_SAVE = "onWidgetConfigSave";
        public static final String ON_WIDGET_CONFIG_UPDATE = "onWidgetConfigUpdate";
        public static final String ON_UPDATE_WIDGET_CONFIG = "onUpdateWidgetConfig";
        public static final String ON_VALIDATE_CONFIG = "onValidateConfig";
        public static final String ON_CONFIGURE_WIDGET = "onConfigureWidget";
        public static final String ON_DELETE_WIDGET = "onDeleteWidget";
        public static final String ON_WIDGET_NOT_CONFIGURED = "onWidgetNotFullyConfigured";
        public static final String ON_FINISH_INTERACTIVITY_CONFIG = "onFinishInteractivityConfig";
        public static final String ON_SAVE_ADVANCED_MODE = "onSaveAdvancedMode";
        public static final String ON_INTERACTIVITY_CONFIG_CLOSE = "onCloseInteractivityConfig";
        public static final String ON_ADVANCED_MODE_CLOSE = "onCloseAdvancedMode";
        public static final String ON_VIEW_DASHBOARD_OUTPUTS = "onViewDashboardOutputs";
        public static final String ON_EDITING_DASHBOARD = "onEditDashboard";
        public static final String ON_SAVE_DASHBOARD = "onSaveDashboard";
        public static final String ON_CLICK_IMPORT = "onClickImport";

        public static final String ON_OPEN_DASHBOARD = "onOpenDashboard";
        public static final String ON_OPEN_COMPOSITION = "onOpenComposition";
        public static final String ON_CLICK_IMPORT_CLOSE = "onClickCloseImport";
        public static final String ON_UPDATE_AGGREGATE = "onUpdateAggregate";
        public static final String ON_GLOBAL_FILTER_CLOSE = "onCloseGlobalFilter";
        public static final String ENTER_ADVANCED_MODE = "enterAdvancedMode";

        private EVENTS() {

        }
    }

    public enum ACTION {

        CLICK(SelectElementOption.EventType.click), DOUBLE_CLICK(SelectElementOption.EventType.dblclick), MOUSE_DOWN(
                SelectElementOption.EventType.mousedown), MOUSE_UP(SelectElementOption.EventType.mouseup), MOUSE_ENTER(
                SelectElementOption.EventType.mouseenter), MOUSE_LEAVE(SelectElementOption.EventType.mouseleave), MOUSE_MOVE(
                SelectElementOption.EventType.mousemove), MOUSE_OVER(SelectElementOption.EventType.mouseover), MOUSE_OUT(
                SelectElementOption.EventType.mouseout), NULL_EVENT(SelectElementOption.EventType.nullEvent), FEATURE_SELECTED(
                SelectElementOption.EventType.featureselected), FEATURE_UN_SELECTED(SelectElementOption.EventType.featureunselected);

        private final EventType name;

        private ACTION(EventType s) {
            name = s;
        }

        public EventType getEventType() {
            return name;
        }

        public static ACTION getAction(EventType eventType) {
            for (ACTION action : ACTION.values()) {
                if (action.name.equals(eventType)) {
                    return action;
                }
            }
            return null;
        }

    }

    private static final String BASE_SCOPE = "thor_ramps";
    private List<Widget> widgets;
    private ClusterConfig clusterConfig;
    private boolean running;
    private boolean isChanged;
    private boolean isStaticData;
    private boolean isLargeDataset;
    private String referenceId;
    private Map<String, QuerySchema> queries;
    private Map<String, QuerySchema> originalQueries;
    private List<ChartInfo> charts;

    public Dashboard(String name) {
        super(name, null, null, null, null,null);
        this.setName(name);
    }

    public Dashboard() {
        super(null, null, null, null, null,null);
    }
    
    public Dashboard(Composition composition) {
        super(composition.getName(), 
                composition.getAuthor(), 
                composition.getLabel(), 
                composition.getCanonicalName(), 
                new Date(composition.getLastModified()),
                composition.getId());
    }

    public Dashboard(String name, String author, String label, String canonicalName, Date lastModified,String id) {
        super(name, author, label, canonicalName, lastModified,id);
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
        this.widgets = widgets;
    }

    public void addWidgets(List<Widget> widgets) {
        if (this.widgets == null) {
            this.widgets = new ArrayList<Widget>();
        }

        this.widgets.addAll(widgets);
    }

    public HPCCConnection getHpccConnection() {
        if (getClusterConfig() != null) {
            HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnection(getClusterConfig().getId());
            if (connection == null) {
                return null;
            }
            connection.setThorCluster(getClusterConfig().getThorCluster());

            if (!connection.isLegacyFlag()) {
                return connection;
            } else {
                HPCCConnection h = connection;
                HPCCConnection newconn = new HPCCConnection();
                try {
                    newconn.setAllowInvalidCerts(h.getAllowInvalidCerts().toString());
                    newconn.setAttributesPort(9999);
                    newconn.setIsHttps(h.getIsHttps().toString());
                    newconn.setLabel(h.getLabel());
                    newconn.setRoxieEspPort(h.getRoxieEspPort());
                    newconn.setRoxieInternalServerHost(h.getRoxieInternalServerHost());
                    newconn.setRoxieServicePort(h.getRoxieServicePort());
                    newconn.setServerHost(h.getServerHost());
                    newconn.setServerPort(h.getServerPort());
                    newconn.setUseDefaultCluster(h.isUseDefaultCluster());
                    newconn.setAuthString(h.getAuthString());
                    newconn.setThorCluster(h.getThorCluster());
                    for (String s:h.getSelectedRoxieClusters()) {
                        newconn.addSelectedRoxieCluster(s);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed getting Connection", e);
                    return null;
                }
                return newconn;
            }
        }
        return null;
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public List<Interaction> getInteractivities() {
        List<Interaction> interactivities = new ArrayList<Interaction>();
        
        for (Widget widget : widgets) {
            if(widget.getInteractions() != null){
                widget.getInteractions().forEach(interaction -> 
                    interactivities.add(interaction)
                );
            }
        }
        
        return interactivities;
    }

    public List<Widget> getScoredSearchWidgets() {
        if (widgets != null) {
            return new ArrayList<Widget>(widgets.stream().filter(widget -> ChartType.SCORED_SEARCH.equals(widget.getChartConfiguration().getType()))
                    .collect(Collectors.toList()));
        } else {
            return widgets;
        }

    }
    
    public List<Widget> getNonScoredSearchWidgets(){
        if (widgets != null) {
            return new ArrayList<Widget>(widgets.stream().filter(widget -> !ChartType.SCORED_SEARCH.equals(widget.getChartConfiguration().getType()))
                    .collect(Collectors.toList()));
        } else {
            return widgets;
        }
        
    }
    
    /** 
     * Goes through the list of all widgets and return an ArrayList of widgets whose chart type is not scored search or global filter.
     * 
     * @return - An array list of the widgets collected.
     */
    public List<Widget> getNonGlobalAndScoredSearchWidget(){
        if (widgets != null) {
            ArrayList<Widget> filteredWidgets = new ArrayList<Widget>();
            Iterator<Widget> iterator = widgets.iterator();
            // Loop through all the widgets and get the ones that are not of the type scored search or global filter
            while (iterator.hasNext()) {
                Widget widget = iterator.next();
                if (!ChartType.SCORED_SEARCH.equals(widget.getChartConfiguration().getType()) && !ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType())) {
                    filteredWidgets.add(widget);
                }
            }
            return filteredWidgets;
        } else {
            return widgets;
        }
        
    }
    
    public List<Widget> getNonGlobalFilterWidget(){
        if (widgets != null) {
            return new ArrayList<Widget>(widgets.stream().filter(widget -> !ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType()))
                    .collect(Collectors.toList()));
        } else {
            return widgets;
        }
        
    }
    
    public static Collection<ChartConfiguration> getNonGlobalFilterWidgetConfig(){
       return Dashboard.CHARTS_CONFIGURATION.values().stream().filter(
               config -> !ChartType.GLOBAL_FILTER.equals(config.getType())).collect(Collectors.toList());
    }

    public Widget getWidgetByName(String sourceWidget) {
        return widgets.stream().filter(widget -> sourceWidget.equals(widget.getName())).findFirst().get();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Map<String, QuerySchema> getQueries() {
        return queries;
    }

    public void setQueries(Map<String, QuerySchema> queries) {
        this.queries = queries;
    }

    /**
     * Checks if any widgets in the Dashboard uses the query used in widget and
     * removes the query from dashboard's queries map if it is not used by any
     * other widgets
     * 
     * @param widget
     *            Widget which no longer uses the query in its queryName field
     */
    public void removeQuery(Widget widget) {
        String queryName = widget.getQueryName();

        List<Widget> dashboardWidgets = getWidgets();
        // Check any widgets use this query
        if(dashboardWidgets != null){
            boolean isQueryUsed = dashboardWidgets.stream().filter(w -> queryName.equals(w.getQueryName())).count() > 0;
    
            if (!isQueryUsed) {
                queries.remove(queryName);
            }
        }
    }

    public boolean isChanged() {
        return isChanged;
    }

    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

    public boolean isStaticData() {
        return isStaticData;
    }

    public void setStaticData(boolean isStaticData) {
        this.isStaticData = isStaticData;
    }

    @Override
    public String toString() {
        return "Dashboard [widgets=" + widgets + ", clusterConfig=" + clusterConfig
                + ", running=" + running + ", isChanged=" + isChanged + ", isStaticData=" + isStaticData
                + ", queries=" + queries + "]";
    }

    public boolean isLargeDataset() {
        return isLargeDataset;
    }

    public void setLargeDataset(boolean isLargeDataset) {
        this.isLargeDataset = isLargeDataset;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getBaseScope() {
        StringBuilder builder = new StringBuilder();
        builder.append(BASE_SCOPE).append(Constants.SCOPE_RESOLUTION_OPR).append(getReferenceId());
        return builder.toString();
    }
    
    /**
     * Delete the source widget and target widgets in the Interaction if a
     * widget is deleted from the dashboard
     * 
     * @param widgetToDelete
     */
    public void deleteWidgetInteractivities(Widget widgetToDelete) {
        
        if (widgetToDelete.getInteractions() != null && widgetToDelete.getInteractions().iterator().hasNext()) {
            //removes the target from deleting widget's interaction.Here the deleting widget has source(ie has SELECT() part)
            widgetToDelete.getInteractions().forEach(interaction -> {
                LOGGER.debug("Interactions -->{}", interaction);
                for (InteractionTarget target : interaction.getTargets()) {
                    if (target.getWidget().equals(interaction.getSourceWidget())) {
                        target.getWidget().removeTarget(target,interaction.getSourceWidget());
                    }
                }
            });
        }
        //processes the TargetPart of the deleting widget.It goes to target's widget.And takes that widget's interaction.
        //removes the deleting widget from that interaction's target list.
        //InteractionTarget -> widget -> interaction -> targetlist().remove(deleting widget).     
        if (widgetToDelete.getInteractionTargets() != null) {
            widgetToDelete.getInteractionTargets().forEach(interactionTarget -> {
                List<Interaction> interactionToRemove = new ArrayList<>();
                if(interactionTarget.getWidget().getInteractions() != null){
                    interactionTarget.getWidget().getInteractions().forEach(interaction -> {
                        List<InteractionTarget> targetsToRemove = getTargetsToRemove(widgetToDelete, interaction);
                        interaction.getTargets().removeAll(targetsToRemove);
                        //After removing the target, if that interaction's target list is empty, deletes the interaction
                        if (interaction.getTargets().isEmpty()) {
                            interactionToRemove.add(interaction);
                        }
                    });
                }
                interactionToRemove.forEach(interactionToDelete -> interactionTarget.getWidget().removeSource(interactionToDelete));
            });
            
        }
      
    }

    private List<InteractionTarget> getTargetsToRemove(Widget widgetToDelete, Interaction interaction) {
        List<InteractionTarget> targetsToRemove = new ArrayList<>();
        for (InteractionTarget trg : interaction.getTargets()) {
            if (trg.getWidget().equals(widgetToDelete)) {
                targetsToRemove.add(trg);
            }
        }
        return targetsToRemove;
    }

    public Map<String, QuerySchema> getOriginalQueries() {
        return originalQueries;
    }

    public void setOriginalQueries(Map<String, QuerySchema> originalQueries) {
        this.originalQueries = originalQueries;
    }
    @Override
    public Dashboard clone() throws CloneNotSupportedException {
        super.clone();
        Dashboard dashboard = new Dashboard();
        dashboard.setName(null);
        dashboard.setLabel(null);
        dashboard.setCanonicalName(null);
        dashboard.setRunning(false);
        dashboard.setWidgets(null);
        dashboard.setClusterConfig(this.clusterConfig);
        dashboard.setReferenceId(this.referenceId);
        dashboard.setLargeDataset(this.isLargeDataset);
        dashboard.setStaticData(this.isStaticData);
        
        
        if(this.queries != null){
            Map<String, QuerySchema> clonedQueries = new TreeMap<String, QuerySchema>();
            for(Map.Entry<String, QuerySchema> entry : this.queries.entrySet()){
                clonedQueries.put(entry.getKey(), entry.getValue().clone());
            }
            dashboard.setQueries(clonedQueries);
        }
       
        return dashboard;
        
    }

    public List<ChartInfo> getCharts() {
        return charts;
    }

    public void setCharts(List<ChartInfo> charts) {
        this.charts = charts;
    }

    public Widget getGlobalFilterWidget() {
        Widget globalFilter = null;
        if(CollectionUtils.isEmpty(widgets)){
            return globalFilter;
        }
        Optional<Widget> globalFilterOption = 
         widgets.stream().filter(eachWidget -> ChartType.GLOBAL_FILTER.equals(eachWidget.getChartConfiguration().getType())).findAny();
        if(globalFilterOption.isPresent()){
            //Alwasy a dashboard can hold only one widget
            globalFilter = globalFilterOption.get();
        }
        
        return globalFilter;
    }

    public List<Widget> getLogicalFileWidgets() {
        if(getWidgets() == null){
            return getWidgets();
        }
       return getWidgets().stream().filter(widget -> widget.isFileBased()).collect(Collectors.toList());
    }

    public void removeGlobalWidget() {
        Optional<Widget> globalWidgetOption = getWidgets().stream().filter(widget -> 
            ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType())).findAny();
        if(globalWidgetOption.isPresent()){
            getWidgets().remove(globalWidgetOption.get());
        }
    }
}
