package org.hpccsystems.dsp.dashboard.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartConfiguration;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Anchorchildren;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

public class ChartsController extends SelectorComposer<Component> {
	
	
	//Commenting the charts which are not being used.
    /*
		private static final String XY_CHART_WITH_DATE_BASED_AXIS = "XY chart with date-based axis";
	    private static final String TREND_LINE = "Trend Line";
	    private static final String STEP_LINE = "Step Line";
	    private static final String STACKED_AREA = "Stacked Area";
	    private static final String SCATTER = "Scatter";
	    private static final String REVERSED_AXIS = "Reversed Axis";
	    private static final String MULTIPLE_VALUE_AXES = "Multiple Value Axes";
	    private static final String MIXED_BAR_LINE = "Mixed Bar Line";
	    private static final String GAUGE = "Gauge";
	    private static final String DURATION_ON_VALUE_AXIS = "Duration on Value Axis";
	    private static final String BUBBLE = "Bubble";
    */
    private static final String STYLE_CHART_CONTAINER = "chart-container";
    private static final String STYLE_CHART_CONTAINER_SELECTED = "chart-container selected";
    private static final String STYLE_CHART_HEADER_LABEL = "dsb-wdg-chart-hdr-label";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartsController.class);

    @Wire
    private Anchorlayout chartsLayout;

    @Wire
    private Anchorlayout chartsLayout1;

    @Wire
    private Anchorlayout chartsLayout2;

    @Wire
    private Anchorlayout chartsLayout3;

    @Wire
    private Anchorlayout chartsLayout4;

    @Wire
    private Textbox chartname;

    private Anchorchildren selectedAnchorchildren;
    private ChartConfiguration selectedChart;
    private WidgetConfig widgetConfig;
    private DashboardConfig dashboardConfig;
    Map<String, Anchorlayout> typesOFCharts = new HashMap<String, Anchorlayout>();

    private SerializableEventListener<Event> updateWidgetConfigListener = event -> {
        if (selectedChart != null) {
            widgetConfig.setChartType(selectedChart.getType());
        }

        if (StringUtils.isNotEmpty(chartname.getValue()) && Character.isDigit(chartname.getValue().charAt(0))) {
            widgetConfig.setChartname("");
            Clients.showNotification(Labels.getLabel("giveCharactorOnly"), Clients.NOTIFICATION_TYPE_ERROR, chartname, Constants.POSITION_END_AFTER,
                    3000, true);
            return;
        } else {
            widgetConfig.setChartname(chartname.getValue());
        }

        LOGGER.debug("Updated chart selection to widget config - {}", widgetConfig);
        LOGGER.debug("dashboardConfig-->{}", dashboardConfig.getDashboard().getWidgets());
    };

    private void validateListerner(Event event) {
        if (selectedChart == null) {
            Clients.showNotification(Labels.getLabel("chooseChart"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    3000, true);
            return;
        } else if (chartname.getValue() == null || chartname.getValue().isEmpty()) {
            Clients.showNotification(Labels.getLabel("giveName"), Clients.NOTIFICATION_TYPE_ERROR, chartname, Constants.POSITION_END_AFTER, 3000,
                    true);
            return;
        } else if (CompositionUtil.isChartNameDuplicate(dashboardConfig, widgetConfig)) {
            chartname.setValue("");
            Clients.showNotification(Labels.getLabel("chartNameAlreadyExists"), Clients.NOTIFICATION_TYPE_ERROR, chartname,
                    Constants.POSITION_END_AFTER, 3000, true);
            return;
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        typesOFCharts.put("Pie Chart", chartsLayout);
        typesOFCharts.put("Pyramid", chartsLayout);
        typesOFCharts.put("Donut", chartsLayout);

        typesOFCharts.put("Line Chart", chartsLayout1);
        //typesOFCharts.put(STACKED_AREA, chartsLayout1);
        //typesOFCharts.put(STEP_LINE, chartsLayout1);
        //typesOFCharts.put(TREND_LINE, chartsLayout1);
        //typesOFCharts.put(REVERSED_AXIS, chartsLayout1);
        //typesOFCharts.put(DURATION_ON_VALUE_AXIS, chartsLayout1);

        typesOFCharts.put("Bar Chart", chartsLayout2);
        typesOFCharts.put("Column Chart", chartsLayout2);
        //typesOFCharts.put(MIXED_BAR_LINE, chartsLayout2);
        typesOFCharts.put("Stacked Column Chart", chartsLayout2);

        //typesOFCharts.put(MULTIPLE_VALUE_AXES, chartsLayout3);
        //typesOFCharts.put(SCATTER, chartsLayout3);
        //typesOFCharts.put(XY_CHART_WITH_DATE_BASED_AXIS, chartsLayout3);
        //typesOFCharts.put(BUBBLE, chartsLayout3);

        //typesOFCharts.put(GAUGE, chartsLayout4);
        typesOFCharts.put("US_Map", chartsLayout4);
        typesOFCharts.put("Table Widget", chartsLayout4);
        typesOFCharts.put("Scored Search", chartsLayout4);

        this.getSelf().addEventListener(Dashboard.EVENTS.ON_UPDATE_WIDGET_CONFIG, updateWidgetConfigListener);
        this.getSelf().addEventListener(Dashboard.EVENTS.ON_VALIDATE_CONFIG, (SerializableEventListener<? extends Event>) this::validateListerner);

        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getAttribute(Constants.DASHBOARD_CONFIG);
        chartname.setValue(widgetConfig.getChartname());

        Dashboard.getNonGlobalFilterWidgetConfig().stream().forEach(config -> renderingChartList(config, null, null));
        Map<String, String> charts = new LinkedHashMap<String, String>();
        //charts.put("step_line", STEP_LINE);
        //charts.put("duration_value", DURATION_ON_VALUE_AXIS);
        //charts.put("reversed_axis", REVERSED_AXIS);
        //charts.put("trend_line", TREND_LINE);
        //charts.put("area", STACKED_AREA);
        //charts.put("mixed_bar_line", MIXED_BAR_LINE);
        //charts.put("scatter", SCATTER);
        //charts.put("bubble", BUBBLE);
        //charts.put("xychart_date_based", XY_CHART_WITH_DATE_BASED_AXIS);
        //charts.put("multiple_axes", MULTIPLE_VALUE_AXES);
        //charts.put("gauge", GAUGE);
        for (Map.Entry<String, String> chart : charts.entrySet()) {
            renderingChartList(null, "dashboard/assets/image/charts/" + chart.getKey() + ".png", chart.getValue());
        }

    }

    private void renderingChartList(ChartConfiguration config, String imagePath, String name) {
        if (dashboardConfig.isStaticData() && config != null && ChartType.SCORED_SEARCH.equals(config.getType())) {
            return;
        }
        Anchorchildren anchorChildren = new Anchorchildren();
        if (config != null) {
            anchorChildren.setClass(STYLE_CHART_CONTAINER);
            if (config.getType().equals(widgetConfig.getChartType())) {
                selectChart(config, anchorChildren);
            }

            anchorChildren.addEventListener(Events.ON_CLICK, (SerializableEventListener<? extends Event>) event -> {
                if (selectedAnchorchildren != null) {
                    selectedAnchorchildren.setClass(STYLE_CHART_CONTAINER);
                }
                selectChart(config, anchorChildren);
            });
        } else {
            anchorChildren.setClass("chart-container-static");
        }
        Vbox vbox = new Vbox();
        Label label = new Label();
        if (config != null) {
            label.setValue(config.getName());
        } else {
            label.setValue(name);
        }
        LOGGER.debug(label.getValue());
        label.setSclass(STYLE_CHART_HEADER_LABEL);
        Image img = new Image();
        if (config != null) {
            img.setSrc(config.getStaticColorImage());
            LOGGER.debug("static colour-->{}", config.getStaticColorImage());
        } else {
            img.setSrc(imagePath);
        }
        img.setWidth("420px");
        img.setHeight("250px");
        label.setParent(vbox);
        img.setParent(vbox);
        vbox.setParent(anchorChildren);
        anchorChildren.setParent(typesOFCharts.get(label.getValue()));
    }

    private void selectChart(ChartConfiguration config, Anchorchildren anchorChildren) {
        anchorChildren.setClass(STYLE_CHART_CONTAINER_SELECTED);
        selectedAnchorchildren = anchorChildren;
        selectedChart = config;
    }
}
