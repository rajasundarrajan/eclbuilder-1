package org.hpccsystems.dsp.dashboard.controller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Include;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Vlayout;

public class DatasourceController extends SelectorComposer<Vlayout> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceController.class);
    @Wire
    private Radio browseFiles;
    
    @Wire
    private Radio browseQueries;
    
    @Wire
    private Include browserSelect;
    
    @Wire
    private Hbox browserSelectorHbox;
    
    private DashboardConfig dashboardConfig;
    private WidgetConfig widgetConfig;
    
    @Override
    public void doAfterCompose(Vlayout comp) throws Exception {
        super.doAfterCompose(comp);

        dashboardConfig = (DashboardConfig) Executions.getCurrent().getAttribute(Constants.DASHBOARD_CONFIG);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG);

        browserSelect.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        browserSelect.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);

        this.getSelf().addEventListener(
                Dashboard.EVENTS.ON_VALIDATE_CONFIG,
                (SerializableEventListener<? extends Event>) event -> {
                    if (org.apache.commons.lang3.StringUtils.isEmpty(widgetConfig.getQueryName()) && !widgetConfig.hasValidLogicalFile()) {
                        Clients.showNotification("Choose a datasource", "error", this.getSelf().getParent(), Constants.POSITION_TOP_CENTER,
                                3000, true);
                        LOGGER.error("Datasource not selected");
                        return;
                    }
                    Events.postEvent(Dashboard.EVENTS.ON_VALIDATE_CONFIG, browserSelect.getFirstChild(), null);
                });

        if (!widgetConfig.isNewCreation()) {
            LOGGER.debug("is old widget");
            if (widgetConfig.getWidget().isQueryBased()) {
                LOGGER.debug("is query based");
                browseQueries.setChecked(true);
                Events.postEvent(Events.ON_CHECK, browseQueries, null);
            } else if(widgetConfig.getWidget().isFileBased()) {
                LOGGER.debug("is file based");
                browseFiles.setChecked(true);
                Events.postEvent(Events.ON_CHECK, browseFiles, null);
            }
        }
        this.getSelf().addEventListener(Constants.EVENTS.ON_SELECT_DATASOURCE,(SerializableEventListener<? extends Event>) this::doAfterSelectingDatasource);
        
        LOGGER.debug("is Static Data - {}", dashboardConfig.getDashboard().isStaticData());
        
        if(dashboardConfig.getDashboard().isStaticData()) {
            browserSelectorHbox.setVisible(false);
            browserSelect.setSrc("dashboard/design/widget/static_data_browser.zul");
        }
    }

    private void doAfterSelectingDatasource(Event event) {
        // By default showing logical files for scored search widget
        if (ChartType.SCORED_SEARCH.equals(widgetConfig.getChartType())) {
            browseQueries.setVisible(false);
            if (widgetConfig.isNewCreation()) {
                browseFiles.setSelected(true);
                includeFileBrowser();
            }
        } else if(!dashboardConfig.getDashboard().isStaticData()){
            browseQueries.setVisible(true);
            if (widgetConfig.isNewCreation()) {
            	if(widgetConfig.getWidget() != null && widgetConfig.getWidget().isQueryBased() && StringUtils.isNotEmpty(widgetConfig.getQueryName())){
            		browseQueries.setSelected(true);
            		includeQuerybrowser();
            	} else {
            		browseFiles.setSelected(true);
                    includeFileBrowser();
            	}
            }
        }
    }
    
    @Listen("onCheck=#browseFiles")
    public void includeFileBrowser() {
        //Remove Query associations
        Widget widget = widgetConfig.getWidget();
        boolean isWidgetCreated = widget != null;
        if(isWidgetCreated && widget.getQueryName() != null) {
            dashboardConfig.getDashboard().removeQuery(widget);
            widget.setQueryName(null);
        }
        
        widgetConfig.setDatasourceType(DATASOURCE.FILE);
        browserSelect.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        browserSelect.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        browserSelect.setSrc("dashboard/design/widget/file_browser.zul");
    }
    
    @Listen("onCheck=#browseQueries")
    public void includeQuerybrowser() {
        //Remove Logical file associations
        boolean isWidgetCreated = widgetConfig.getWidget() != null;
        if(isWidgetCreated && widgetConfig.getWidget().getDatasource() != null) {
            dashboardConfig.removeDatasource(widgetConfig.getWidget());
            widgetConfig.getWidget().setDatasource(null);
        }
        
        widgetConfig.setDatasourceType(DATASOURCE.QUERY);
        browserSelect.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        browserSelect.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        browserSelect.setSrc("dashboard/design/widget/query_browser.zul");
    }
}
