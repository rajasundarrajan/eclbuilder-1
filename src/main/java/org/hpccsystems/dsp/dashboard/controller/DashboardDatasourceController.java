package org.hpccsystems.dsp.dashboard.controller;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

public class DashboardDatasourceController extends SelectorComposer<Vlayout> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardDatasourceController.class);

    @Wire
    private Listbox latestFiles;

    @Wire
    private Listbox latestQueries;

    @Wire
    private Tab browsertab;
   
    @Wire
    private Include browserInclude;
    
    @Wire
    private Textbox selectedRecentFile;

    private DashboardConfig dashboardConfig;
    private WidgetConfig widgetConfig;
    private ListModelList<String> queriesModel = new ListModelList<String>();
    private ListModelList<String> filesModel = new ListModelList<String>();
    private Set<String> queries = new HashSet<>();
    private Set<String> files = new HashSet<>();
    
    private ListitemRenderer<String> fileRenderer = (listitem, file, index) -> {
        Listcell cell = new Listcell();
        cell.setIconSclass("z-icon-file");
        cell.setLabel(file);
        cell.setParent(listitem);
    };
    
    private ListitemRenderer<String> queryRenderer = (listitem, query, index) -> {
        Listcell cell = new Listcell();
        cell.setIconSclass("fa fa-file-text");
        cell.setLabel(query);
        cell.setParent(listitem);
    };
    
    @Override
    public void doAfterCompose(Vlayout comp) throws Exception {
        super.doAfterCompose(comp);

        dashboardConfig = (DashboardConfig) Executions.getCurrent().getAttribute(Constants.DASHBOARD_CONFIG);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG);
        
        latestFiles.setItemRenderer(fileRenderer);
        latestQueries.setItemRenderer(queryRenderer);
        if (CollectionUtils.isNotEmpty(dashboardConfig.getDashboard().getWidgets())) {
            
            for (Widget wid : dashboardConfig.getDashboard().getWidgets()) {
                if(wid.getQueryName() != null){
                    queries.add(wid.getQueryName());  
                }
               
            }
           
            if(CollectionUtils.isNotEmpty(widgetConfig.getUnusedQueries())){
                queries.addAll(widgetConfig.getUnusedQueries());
            }   
            
            queriesModel.addAll(queries);
            latestQueries.setModel(queriesModel);
            
            
            for (Widget wid : dashboardConfig.getDashboard().getWidgets()) {
                if(wid.getDatasource() != null){
                    files.add(wid.getLogicalFile());  
                }
            }   
            
            if(CollectionUtils.isNotEmpty(widgetConfig.getUnusedFiles()) ){
                files.addAll(widgetConfig.getUnusedFiles());
            }
            
            filesModel.addAll(files);
            latestFiles.setModel(filesModel);
        }

        updateSelections();
        
        this.getSelf().addEventListener(Dashboard.EVENTS.ON_VALIDATE_CONFIG, (SerializableEventListener<? extends Event>) this::doAfterConfigValidation);
        
    }

    private void updateSelections() {
        if (widgetConfig.getLogicalFile() != null) {

            if (filesModel.indexOf(widgetConfig.getLogicalFile()) >= 0) {
                selectedRecentFile.setValue(widgetConfig.getLogicalFile());
                filesModel.clearSelection();
                queriesModel.clearSelection();
                filesModel.addToSelection(filesModel.get(filesModel.indexOf(widgetConfig.getLogicalFile())));
            } else {
                widgetConfig.setDatasource(null);
            }

        }

        if (widgetConfig.getQueryName() != null) {

            if (queriesModel.indexOf(widgetConfig.getQueryName()) >= 0) {
                selectedRecentFile.setValue(widgetConfig.getQueryName());
                queriesModel.clearSelection();
                filesModel.clearSelection();
                queriesModel.addToSelection(queriesModel.get(queriesModel.indexOf(widgetConfig.getQueryName())));
            } else {
                widgetConfig.setQueryName(null);
            }

        }
    }

    private void doAfterConfigValidation(Event event) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(widgetConfig.getQueryName()) && !widgetConfig.hasValidLogicalFile()) {
            Clients.showNotification("Choose a datasource", "error", this.getSelf().getParent(), Constants.POSITION_TOP_CENTER, 3000, true);
            LOGGER.error("Datasource not selected");
        }
    }

    @Listen("onClick = #loadBrowserButton")
    public void onloadBrowserButton() {
        browserInclude.setSrc(null);
        browsertab.setSelected(true);
        browserInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        browserInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        browserInclude.setSrc("dashboard/design/widget/datasource.zul");
        Events.postEvent(Constants.EVENTS.ON_SELECT_DATASOURCE, browserInclude.getFirstChild(), null);
    }

    @Listen("onSelect = #latestFiles")
    public void onSelectrecentFile(SelectEvent<?, ?> event) {
        latestQueries.clearSelection();
        selectedRecentFile.setValue( event.getSelectedObjects().iterator().next().toString());
        
        if(widgetConfig.getWidget() != null && widgetConfig.getWidget().getQueryName() != null) {
            dashboardConfig.getDashboard().removeQuery(widgetConfig.getWidget());
            widgetConfig.getWidget().setQueryName(null);
            widgetConfig.setQueryName(null);
        }
        
        try {
            widgetConfig.updateContractInstance(dashboardConfig, event.getSelectedObjects().iterator().next().toString(), null);
            widgetConfig.setDatasourceType(DATASOURCE.FILE);            
        } catch (HipieException | HPCCException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    @Listen("onSelect = #latestQueries")
    public void onSelectlatestQueries(SelectEvent<?, ?> event) {
        if(widgetConfig.getWidget() != null && widgetConfig.getWidget().getDatasource() != null) {
            dashboardConfig.removeDatasource(widgetConfig.getWidget());
            widgetConfig.getWidget().setDatasource(null);
            widgetConfig.setDatasource(null);
        }
        latestFiles.clearSelection();
        String query = event.getSelectedObjects().iterator().next().toString();
        selectedRecentFile.setValue(query);
        widgetConfig.setDatasourceType(DATASOURCE.QUERY);
        widgetConfig.setDatasourceUpdated(!query.equals(widgetConfig.getQueryName()));
        widgetConfig.setQueryName(query);

    }

}
