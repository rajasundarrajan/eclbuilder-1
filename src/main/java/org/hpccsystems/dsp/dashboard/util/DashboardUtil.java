package org.hpccsystems.dsp.dashboard.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.DriveFieldInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.dude.option.SelectElementOption;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.controller.WidgetConfig;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartConfiguration;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartInfo;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.HipieCharts;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.entity.widget.SortField;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.XYChart;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.CompositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

public class DashboardUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardUtil.class);
    public static final String STYLE_POPUP = "popup";
    private static final String[] EMPTY_ARRAY = new String[0];
    private static final String Z_ICON_TIMES = "z-icon-times";
    private static final String BTN_CLOSE = "btn-close";
    private static final String LIST_ITEM_ALIGN_LEFT = "global-filter-list-item";
    private static final String ALLOW_EMPTY_REQUEST = "_allowEmptyRequest";
    private static final String FLYOUT = "_flyout";

    private DashboardUtil() {
    }

    public static String removeSpaceSplChar(String str) {
        return str.replaceAll("[^a-zA-Z0-9]+", "");
    }

    public static void sortDashboards(List<Dashboard> recentDashboards) {
        Collections.sort(recentDashboards, new Comparator<Dashboard>() {

            @Override
            public int compare(Dashboard o1, Dashboard o2) {
                return o2.getLastModifiedDate().compareTo(o1.getLastModifiedDate());
            }

        });
    }

    public static void renderAggregateButton(Component component, Measure measure, List<Measure> measures,
            Map<Measure, SortField> sortAggregateSync, ListModelList<SortField> fieldModel) {
        final Popup popup = new Popup();
        popup.setWidth("100px");
        popup.setZclass(Dashboard.STYLE_POPUP);

        final Button button = new Button();
        if (measure.getAggregation() == null) {
            button.setLabel("NONE");
            measure.setAggregation(AGGREGATION.NONE);
        } else {
            button.setLabel(measure.getAggregation().toString());
        }
        button.setTooltiptext("Aggregation");
        button.setPopup(popup);
        button.setZclass("btn btn-xs btn-sum");
        Listbox listbox = new Listbox();
        listbox.setMultiple(false);

        List<AGGREGATION> list = getAggregationList();
        ListModelList<AGGREGATION> aggregationModel = new ListModelList<>(list);
        listbox.setModel(aggregationModel);
        ListitemRenderer<AGGREGATION> renderer = (item, agg, ind) -> item.setLabel(agg.name());
        listbox.setItemRenderer(renderer);

        EventListener<SelectEvent<Component, AGGREGATION>> selectListener = event -> getAggregation(component, measure, measures, sortAggregateSync,
                fieldModel, popup, button, aggregationModel,
                    event);
        
        listbox.addEventListener(Dashboard.ON_SELECT, selectListener);

        popup.appendChild(listbox);
        component.appendChild(popup);
        component.appendChild(button);

    }

    private static void getAggregation(Component component, Measure measure, List<Measure> measures,
            Map<Measure, SortField> sortAggregateSync, ListModelList<SortField> fieldModel, final Popup popup,
            final Button button, ListModelList<AGGREGATION> aggregationModel,
            SelectEvent<Component, AGGREGATION> event) {
        AGGREGATION selectedItem = event.getSelectedObjects().iterator().next();
        aggregationModel.clearSelection();
        if (measures != null && selectedItem != measure.getAggregation()) {
            LOGGER.debug("Measures for change aggregate check (Dashboard util) - {}", measures);
            Measure testMeasure = new Measure(measure);
            LOGGER.debug("test measure for aggregate change check - {}", testMeasure);
            testMeasure.setAggregation(selectedItem);
            boolean containsMeasure = false;
            for (Measure fieldColumn : measures) {
                containsMeasure = fieldColumn.getColumn().equals(testMeasure.getColumn())
                        && ((Measure) fieldColumn).getAggregation() == testMeasure.getAggregation();
                if (containsMeasure) {
                    break;
                }
            }
            if (containsMeasure) {
                Clients.showNotification(
                        testMeasure.getColumn() + " " + Labels.getLabel("with") + " " + testMeasure.getAggregation()
                                + " " + Labels.getLabel("aggregationAlreadyExists"),
                        Clients.NOTIFICATION_TYPE_ERROR, component, Constants.POSITION_END_CENTER, 3000, true);
                popup.close();
                return;
            }
        }
        SortField sortField = sortAggregateSync.get(measure);
        Measure preserveMeasure = new Measure(measure);
        measure.setAggregation(selectedItem);
        if (sortField != null) {
            int sortFieldIndex = fieldModel.indexOf(sortField);
            sortField.setAggregation(selectedItem);
            sortAggregateSync.remove(preserveMeasure);
            sortAggregateSync.put(measure, sortField);
            fieldModel.set(sortFieldIndex, sortField);
        }
        button.setLabel(selectedItem.name());
        popup.close();
    }

    public static List<AGGREGATION> getAggregationList() {
        List<AGGREGATION> list = new ArrayList<AGGREGATION>();
        list.add(AGGREGATION.AVE);
        list.add(AGGREGATION.MAX);
        list.add(AGGREGATION.MIN);
        list.add(AGGREGATION.NONE);
        list.add(AGGREGATION.SUM);
        return list;
    }

    public static List<Field> removeRowCountField(List<Field> chartFields) {

        List<Field> fieldsWithoutRowcount = new ArrayList<Field>(chartFields);
        Optional<Field> option = chartFields.stream().filter(uniqueField -> uniqueField.isRowCount()).findAny();
        if (option.isPresent()) {
            fieldsWithoutRowcount.remove(option.get());
        }
        return fieldsWithoutRowcount;
    }

    public static List<ChartInfo> retrieveChartInfo(ContractInstance visualizationCI) {
        List<ChartInfo> charts = new ArrayList<ChartInfo>();
        if (visualizationCI == null) {
            return Collections.emptyList();
        }
        // Handling Exception as it breaks flow, when Visual element is invalid
        try {
            if (visualizationCI.getContract().getVisualElements().iterator().hasNext()) {
                List<Element> visulaElements = visualizationCI.getContract().getVisualElements().iterator().next()
                        .getChildElements();
                List<Element> chartVisualElements = visulaElements.stream()
                        .filter(visualElement -> !visualElement.getType().equals(VisualElement.FORM))
                        .collect(Collectors.toList());
                
                // Check to see if string ends with the suffix 'scoredsearchdashboard'
                if (StringUtils.endsWith(visualizationCI.getContract().getName(),
                        Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER)) {
                    ChartInfo chart = new ChartInfo();
                    // Taking First Table(Result1 table) name for scored search
                    // widget
                    chart.setName(
                            chartVisualElements.get(0).getOption(VisualElement.TITLE).getParams().get(0).getName());

                    // keeping table icon for scored search widget,Once icon
                    // found for scored search table, can replace it
                    ChartConfiguration chartConfig = Dashboard.CHARTS_CONFIGURATION.values().stream()
                            .filter(config -> ChartType.TABLE.toString().equals(config.getHipieName())).findFirst()
                            .get();

                    chart.setIcon(chartConfig.getFaIcon());
                    charts.add(chart);
                } else {
                    chartVisualElements.forEach(element -> {
                        ChartInfo chart = new ChartInfo();
                        // If the chart title is null set the name
                        if (element.getOption(VisualElement.TITLE) != null) {
                            chart.setName(element.getOption(VisualElement.TITLE).getParams().get(0).getName());
                        }
                        
                        ChartConfiguration chartConfig = null;
                        if (element.getCustomOptions().get(Widget.CHART_TYPE) != null) {
                        	// Get the chart configuration
                        	chartConfig = DashboardUtil.getChartConfig(element);	
                        }
                        
                        if (chartConfig != null) {
                            chart.setIcon(chartConfig.getFaIcon());
                        }
                        charts.add(chart);
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }

        return charts;
    }

    /**
     * @returns Chart type string used in HIPIE
     */
    public static ChartConfiguration getChartConfig(Element visualElement) {
        LOGGER.debug("Extracting type - {}", visualElement);
        // Get chart type from Element
        String chartType = visualElement.getCustomOptions().get(Widget.CHART_TYPE).getParams().get(0).getName();
        if (ChartType.PIE.toString().equals(visualElement.getType())
                && visualElement.getCustomOptions().get(Widget.HOLE_PERCENT) != null
                && visualElement.getCustomOptions().get(Widget.HOLE_PERCENT).getParams().get(0).getName() != null) {
            LOGGER.debug("donut chart extracted");
            return Dashboard.CHARTS_CONFIGURATION.get(ChartType.DONUT);
        }

        String orientation = null;
        if (visualElement.getCustomOptions().containsKey(XYChart.ORIENTATION)) {
            orientation = visualElement.getCustomOptions().get(XYChart.ORIENTATION).getParams().get(0).getName();
        }

        List<String> names = Arrays.asList(HipieCharts.values()).stream().map(chart -> chart.getChartName())
                .collect(Collectors.toList());

        if (orientation != null && HipieCharts.BAR.getChartName().equals(chartType)
                && XYChart.BAR_ORIENTATION.equals(orientation)) {

            return Dashboard.CHARTS_CONFIGURATION.values().stream().filter(config -> ChartType.BAR.equals(config.getType()))
                    .findFirst().get();

        } else if (!names.contains(chartType)) {
            ChartType type = null;
            if (ChartType.LINE.toString().equals(visualElement.getType())) {
                if (chartType.contains("LINE")) {
                    type = ChartType.LINE;
                } else if (chartType.contains("COLUMN")) {
                    type = ChartType.COLUMN;
                } else {
                    type = ChartType.BAR;
                }
            } else if (ChartType.PIE.toString().equals(visualElement.getType())) {
                type = ChartType.PIE;
            } else {
                type = Arrays.asList(ChartType.values()).stream()
                        .filter(chart -> chart.toString().equals(visualElement.getType())).findFirst().get();
            }

            LOGGER.debug("chart type --->{}", chartType);
            ChartType chart = type;
            return Dashboard.CHARTS_CONFIGURATION.values().stream().filter(config ->config.getType().equals(chart) )
                    .findFirst().get();
        }
        
        ChartConfiguration objChartConfig = Dashboard.CHARTS_CONFIGURATION.values().stream().filter(config -> config.getHipieName().equals(chartType)).findFirst().get();
        // To find out if the chart is Stacked Column chart, check if the _stacked property is set in dud and if the chart is of type COLUMN.
        if(objChartConfig.getType().equals(ChartType.COLUMN) && visualElement.getCustomOptions().containsKey("_stacked")) {
        	return Dashboard.CHARTS_CONFIGURATION.get(ChartType.STACKCOLUMN);
        } else {
        	return objChartConfig;
        }
    }

    /**
     * @param widgetConfig
     * @returns false if the widget's name/chart type/filter/Time Series pattern has been modified 
     * from original widget in EDIT flow
     */
    public static boolean canUpdateVisualizationCI(WidgetConfig widgetConfig) {
        Widget originalWidget = widgetConfig.getOriginalWidget();
        Widget currentWidget = widgetConfig.getWidget();
        boolean canUpdate = true;

        if (originalWidget.getName() == null) {
            if (currentWidget.getName() != null) {
                canUpdate = false;
            }
        } else if (!originalWidget.getName().equals(currentWidget.getName())) {
            canUpdate = false;
        }

       boolean updateCI = canUpdateCIForChartTypeOrTimePattern(originalWidget, currentWidget);
        if(!updateCI){
            canUpdate =  updateCI;      
        }

        if (originalWidget.getFilters() == null) {
            if (currentWidget.getFilters() != null) {
                canUpdate = false;
            }
        } else if (!originalWidget.getFilters().equals(currentWidget.getFilters())) {
            canUpdate = false;
        }

        return canUpdate;
    }

    private static boolean canUpdateCIForChartTypeOrTimePattern(Widget originalWidget, Widget currentWidget) {

        boolean update = true;
        if (originalWidget.getChartConfiguration() == null) {
            if (currentWidget.getChartConfiguration() != null) {
                update = false;
            }
        } else if (originalWidget.getChartConfiguration().getType() == null) {
            if (currentWidget.getChartConfiguration().getType() != null) {
                update = false;
            }
        } else if (!originalWidget.getChartConfiguration().getType()
                .equals(currentWidget.getChartConfiguration().getType())) {
            update =  false;
        } else if (originalWidget instanceof XYChart) {
            // Compares 'time series pattern' for XY chart
            //If 'time series pattern' is modified , need to generate new DDL
            XYChart originalXYChart =  (XYChart) originalWidget;
            XYChart currentXYChart =  (XYChart) currentWidget;           
            Attribute originalAttribute = originalXYChart.getAttributes().get(0);
            Attribute currentAttribute = currentXYChart.getAttributes().get(0);
            
            if (originalAttribute.getTimeFormat() == null) {
                if (currentAttribute.getTimeFormat() != null) {
                    update =  false;
                }
            }else if (!originalAttribute.getTimeFormat().equals(currentAttribute.getTimeFormat())) {
                update = false;
            }
        }
        return update;
    }

    /**
     * This will be used only to compare the schema's input param values so
     * setting the name as null
     * 
     * @return cloned QuerySchema
     * @throws CloneNotSupportedException
     */
    public static QuerySchema cloneQuerySchema(QuerySchema querySchema) throws CloneNotSupportedException {
        QuerySchema clonedQuerySchema = querySchema.clone();
        clonedQuerySchema.setName(null);
        if (querySchema.getInputParameters() != null) {
            List<Filter> clonedParams = new ArrayList<>();

            for (Filter inputParam : querySchema.getInputParameters()) {
                clonedParams.add(inputParam.clone());
            }
            clonedQuerySchema.setInputParameters(clonedParams);
        }
        return clonedQuerySchema;
    }

    public static String[] getUserAndFilename(String query) {
        if (query == null) {
            return EMPTY_ARRAY;
        }
        return StringUtils.split(query, Constants.PIPE, 2);
    }
    
    public static boolean isWorkUnitComplete(CompositionInstance mostRecentInstance) {
        boolean workunitComplete = false;
       try {
           workunitComplete = mostRecentInstance != null && Constants.STATUS_COMPLETED.equals(mostRecentInstance.getWorkunitStatus());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
       return workunitComplete;
    }

    /**
     * Adds chart name/type/Title
     */
    public static void updateBasicData(VisualElement visualElement,Widget widget) throws Exception {
        visualElement.addCustomOption(FLYOUT, new Property(Constants.TRUE));
        visualElement.setType(VisualElement.FORM);
        visualElement.setName(DashboardUtil.removeSpaceSplChar(widget.getName()) + VisualElement.FORM);
        visualElement.addOption(VisualElement.TITLE, new FieldInstance(null, widget.getTitle()));
        visualElement.addCustomOption(ALLOW_EMPTY_REQUEST, new Property(Constants.TRUE));
    }
    
    /**
     * Creates 'SELECT' part of filter form ex: SELECTS(*->chart1, chart2)
     */
    public static SelectElementOption generateFilterFormSelectOption(List<String> targetWidgets) {
        StringBuilder nameBuilder = new StringBuilder();
        targetWidgets.forEach(widgetName -> nameBuilder
                .append(DashboardUtil.removeSpaceSplChar(widgetName))
                .append(",")
         );
        
        // Removing last appended comma(",")
        nameBuilder.replace(nameBuilder.length() - 1, nameBuilder.length(), "");

        SelectElementOption select = new SelectElementOption(new ElementOption(VisualElement.SELECTS, new FieldInstance(null, "*")));
        List<FieldInstance> drives = new ArrayList<FieldInstance>();
        DriveFieldInstance driveFieldInstance = new DriveFieldInstance(null, nameBuilder.toString());
        driveFieldInstance.setDriverField(new FieldInstance(null, "*"));
        drives.add(driveFieldInstance);
        select.setDrives((ArrayList<FieldInstance>) drives);

        return select;
    }
    
    /**
     * Creates part 'STRING Field1_pie: LABEL("priceeach"),DEFAULT("125478");'
     * of filter form
     * 
     * @throws HipieException
     */
    public static void addFormFilterField(VisualElement visualElement, List<Filter> filters) throws HipieException {
        for (Filter filter : filters) {
            InputElement element = new InputElement();
            element.setName(filter.getDudName());
            element.setType(InputElement.TYPE_STRING);
            try {
                element.addOption(Element.LABEL, new FieldInstance(null, filter.getColumn()));
                element.addOption(Element.DEFAULT, new FieldInstance(null, "\"" + filter.getValue() + "\""));
            } catch (Exception e) {
                throw new HipieException(e);
            }
            visualElement.addChildElement(element);
        }        
    }

    public static void renderFilter(Listitem item, Filter filter, ListModelList<Filter> filterModel, Widget widget) {
        Listcell listcell = new Listcell();
        Hlayout hlayout = new Hlayout(); 
        hlayout.setHflex("1");
        Button closeButton = new Button();
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setZclass(BTN_CLOSE + " img-btn");
        closeButton.setStyle("float:right");
        closeButton.addEventListener(Events.ON_CLICK, event -> {
            filterModel.remove(filter);
            //for widgets except global filter
            if (widget != null) {
                widget.removeFilter(filter);
            }
        });
        Label label = new Label(filter.getColumn());
        hlayout.appendChild(label);
        Textbox textbox = new Textbox();
        textbox.setText(filter.getValue());
        textbox.setHflex("1");
        
        //for widgets except global filter
        if(widget != null) {
            Vlayout vlayout = new Vlayout();
            vlayout.setHflex("1");
            vlayout.appendChild(hlayout);
            vlayout.appendChild(textbox);
            listcell.appendChild(vlayout);
            listcell.setSclass("field_hlayout_right_align");
        } else {
            hlayout.appendChild(textbox);
            listcell.appendChild(hlayout);
            listcell.setSclass("global-filter-right");
        }
        hlayout.appendChild(closeButton);
        item.appendChild(listcell);
        if (filter.isGlobal()) {
            item.setSclass(LIST_ITEM_ALIGN_LEFT);
        }
        textbox.addEventListener(Events.ON_CHANGING, event -> DashboardUtil.onChangeFilterName((InputEvent) event,filter));
    }
    
    public static void onChangeFilterName(InputEvent event,Filter filter) {
        filter.setNewValue(StringUtils.isEmpty(event.getValue()) ? "" : event.getValue());
    }
    
    /**
     * Takes the dashboard config passed in and returns a new instance of dashboard config
     * @param dashboardConfig
     * @param newName
     */
    public static DashboardConfig cloneDashboard(DashboardConfig dashboardConfig, String newName, User user, boolean doConvert) {
        DashboardConfig newConfig = null;
        
        try {
            newConfig = new DashboardConfig(dashboardConfig.getDashboard().clone(), Flow.CLONE, 
                    new Composition(dashboardConfig.getComposition()));
            
            newConfig.setHomeTabbox(dashboardConfig.getHomeTabbox());
            
            newConfig.getDashboard().setLabel(newName);
            newConfig.getDashboard().setName(newName);
            
            String compositionName = HIPIEUtil.createCompositionName(newConfig.getDashboard().getLabel(),
                    newConfig.getDashboard().isStaticData());
            
            Composition clonedComposition = CompositionUtil.cloneCompositionOrConvert(newConfig.getComposition(), compositionName,
                    newConfig.getDashboard().getLabel(), user.getId(), true, newConfig.getDashboard(), null, doConvert);

            newConfig.getDashboard().setName(compositionName);
            newConfig.getDashboard().setLabel(newConfig.getDashboard().getLabel());
            newConfig.setComposition(clonedComposition);
            
            if (!StringUtils.isEmpty(newConfig.getDashboard().getReferenceId())) {
                newConfig.getDashboard().setReferenceId(newConfig.getDashboard().getReferenceId());
                // Add reference Id to Composition
                RampsUtil.addReferenceId(newConfig.getComposition(), newConfig.getDashboard().getReferenceId());
            }
                        
            newConfig.getDashboard().setAuthor(user.getId());
            newConfig.getComposition().setAuthor(user.getId());
            Map<PermissionType, Permission> permissions = newConfig.getComposition().getPermissions();
            for (Entry<PermissionType, Permission> entry : permissions.entrySet()) {
                entry.getValue().setPermissionLevel(PermissionLevel.PRIVATE);
                entry.getValue().getGroups().clear();
                entry.getValue().getUserIds().clear();
                newConfig.getComposition().getPermissions().put(entry.getKey(), entry.getValue());
            }
            
        } catch (HipieException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            return null;
        } catch (RepoException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            return null;
        } catch (CloneNotSupportedException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            return null;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            return null;
        }
        
        return newConfig;
    }
    
    public static Boolean saveDashboard(DashboardConfig dashboardConfig) {
        Composition savedComposition;
        try {
            savedComposition =  ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                    .saveNewCompositionOnHIPIE(dashboardConfig.getComposition().getName(), dashboardConfig.getComposition());
        } catch (Exception d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(Labels.getLabel("saveFailed"), Clients.NOTIFICATION_TYPE_ERROR, null, 
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return false;
        }
        
        dashboardConfig.setComposition(savedComposition);
        dashboardConfig.getDashboard().setLastModifiedDate(new Date(dashboardConfig.getComposition().getLastModified()));
        dashboardConfig.getDashboard().setCanonicalName(savedComposition.getCanonicalName());
        
        if (!dashboardConfig.getDashboard().isStaticData()) {
            try {
                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE))
                        .saveClusterConfig(dashboardConfig.getComposition(), dashboardConfig.getDashboard().getClusterConfig());
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(Labels.getLabel("saveFailed"), Clients.NOTIFICATION_TYPE_ERROR, null, 
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return false;
            }
        }
        
        // Let the home tabbox know that there is a new composition
        Events.postEvent(Dashboard.EVENTS.ON_SAVE_DASHBOARD, dashboardConfig.getHomeTabbox(), dashboardConfig);
        return true;
    }
    
    public static DashboardConfig generateDashboardConfigFromRamps(TabData data, Component container) {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(DashboardUtil.removeSpaceSplChar(data.getComposition().getName()));
        dashboard.setClusterConfig(data.getProject().getClusterConfig());
        DashboardConfig dashboardConfig = new DashboardConfig();
        dashboardConfig.setComposition(data.getComposition());
        dashboardConfig.setDashboard(dashboard);
        dashboardConfig.setRampsContainer(container);
        dashboardConfig.setRAMPSConfig(true);
        dashboardConfig.setHomeTabbox((Tabbox) container.getParent().getParent().getFellow(Constants.HOME_TABBOX));
        dashboardConfig.setData(data);
        return dashboardConfig;
    }
    
    public static void constructDashboardConfigFromContractInstance(DashboardConfig dashboardConfig, Map<String, ContractInstance> contractInstances) throws CloneNotSupportedException, HipieException, HPCCException, DatabaseException {
        // Constructing Dashboard Config object
        for (Entry<String, ContractInstance> entry : contractInstances.entrySet()) {
            // Creating Widgets
            if (Dashboard.DASHBOARD_REPO.equals(entry.getValue().getContract().getRepositoryName())) {
                ContractInstance contractInstance = entry.getValue();

                // Identify Scored search widget
                if (!contractInstance.getContract().getGenerateElements().isEmpty()) {
                    dashboardConfig.getDashboard().addWidgets(CompositionUtil.extractScoredSearchWidget(contractInstance));
                } else {
                    dashboardConfig.getDashboard().setQueries(CompositionUtil.extractQueries(contractInstance, false));
                    if(MapUtils.isNotEmpty(dashboardConfig.getDashboard().getQueries())){
                       CompositionUtil.cloneQuerySchema(dashboardConfig.getDashboard());
                    }
                    dashboardConfig.getDashboard().addWidgets(Widget.extractVisualElements(contractInstance, dashboardConfig.getDashboard().getQueries(),
                            dashboardConfig.getDashboard().isStaticData(),dashboardConfig.getData().getProject().getDatasourceStatus()));

                    boolean isSmallDataset = CompositionUtil.extractRunOption(contractInstance);
                    dashboardConfig.getDashboard().setLargeDataset(!isSmallDataset);
                }
                break;
            }
        }
    }
    
}