package org.hpccsystems.dsp.dashboard.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;

public class DashboardConfig implements Serializable{

    private static final long serialVersionUID = 1L;

    private static final String LOGICAL_FILENAME = "LogicalFilename";

    private Tabbox homeTabbox;
    private Component rampsContainer;
    private boolean isRAMPSConfig;
    private boolean reloadOutput;
    private Dashboard dashboard;
    private Composition composition;
    private CompositionInstance mostRecentCI;
    private TabData data;
    private List<PluginOutput> datasources;
    private Component canvasComponent;
    private Component dashboardComponent;
    private Tabbox viewEditTabbox;
    private Constants.Flow flow;
    private Tabbox fileBrowser;
    private Anchorlayout anchorlayout;
    /**
     * The dashboardTab object would be null for non "dashboard perspective" flow.
     * check for null before using it.
     */
    private Tab dashboardTab;

    public DashboardConfig() {
    }

    public DashboardConfig(Dashboard dashboard, Flow flow) {
        this.dashboard = dashboard;
        this.flow = flow;
    }

    public DashboardConfig(Dashboard dashboard, Flow flow, Composition composition) {
        this.dashboard = dashboard;
        this.flow = flow;
        this.composition=composition;
    }

    /*
     * Returns a contract instance which is a data source plugin
     */
    public ContractInstance getDatasourceContractInstance() {
        Map<String, ContractInstance> contractInstances = composition.getContractInstances();
        ContractInstance contractInstance = null;
        for (Map.Entry<String, ContractInstance> entry : contractInstances.entrySet()) {
            if (HIPIEUtil.isDataSourcePlugin(entry.getValue())) {
                contractInstance = entry.getValue();
            }
        }
        return contractInstance;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Tabbox getHomeTabbox() {
        return homeTabbox;
    }

    public void setHomeTabbox(Tabbox homeTabbox) {
        this.homeTabbox = homeTabbox;
    }

    public Composition getComposition() {
        return composition;
    }

    public void setComposition(Composition composition) {
        this.composition = composition;
    }

    public List<PluginOutput> getDatasources() {
        return datasources;
    }

    public void addDatasource(PluginOutput pluginOutput) {
        if (datasources == null) {
            this.datasources = new ArrayList<PluginOutput>();
        }
        this.datasources.add(pluginOutput);
    }

    public void setdatasources(List<PluginOutput> pluginOutputs) {
        this.datasources = pluginOutputs;
    }

    public Component getRampsContainer() {
        return rampsContainer;
    }

    public void setRampsContainer(Component rampsContainer) {
        this.rampsContainer = rampsContainer;
    }

    public boolean isRAMPSConfig() {
        return isRAMPSConfig;
    }

    public void setRAMPSConfig(boolean isRAMPSConfig) {
        this.isRAMPSConfig = isRAMPSConfig;
    }

    public TabData getData() {
        return data;
    }

    public void setData(TabData data) {
        this.data = data;
    }

    public Component getCanvasComponent() {
        return canvasComponent;
    }

    public void setCanvasComponent(Component canvasComponent) {
        this.canvasComponent = canvasComponent;
    }

    public Tabbox getViewEditTabbox() {
        return viewEditTabbox;
    }

    public void setViewEditTabbox(Tabbox viewEditTabbox) {
        this.viewEditTabbox = viewEditTabbox;
    }

    public Constants.Flow getFlow() {
        return flow;
    }

    public void setFlow(Constants.Flow flow) {
        this.flow = flow;
    }

    public Tabbox getFileBrowser() {
        return fileBrowser;
    }

    public void setFileBrowser(Tabbox fileBrowser) {
        this.fileBrowser = fileBrowser;
    }

    public CompositionInstance getMostRecentCI() {
        return mostRecentCI;
    }

    public void setMostRecentCI(CompositionInstance mostRecentCI) {
        this.mostRecentCI = mostRecentCI;
    }

    public Anchorlayout getAnchorlayout() {
        return anchorlayout;
    }

    public void setAnchorlayout(Anchorlayout anchorlayout) {
        this.anchorlayout = anchorlayout;
    }

    public boolean isReloadOutput() {
        return reloadOutput;
    }

    public void setReloadOutput(boolean reloadOutput) {
        this.reloadOutput = reloadOutput;
    }

    public PluginOutput getDatasource(String logicalFile) {
        PluginOutput datasource = null;
        if (datasources != null && !datasources.isEmpty()) {
            Optional<PluginOutput> matchedOutput = datasources.stream()
                    .filter(output -> logicalFile.equals(output.getContractInstance().getProperty(LOGICAL_FILENAME))).findAny();
            if (matchedOutput.isPresent()) {
                datasource = matchedOutput.get();
            }
        }
        return datasource;
    }

    /**
     * Remove datasource of deleting widget if it is not used by any other live
     * widgets
     * 
     * @param deleteWidget
     * @param widgets
     */
    public void removeDatasource(Widget deleteWidget) {

        if (deleteWidget.getDatasource() == null) {
            return;
        }

        boolean otherWidgetExists  = getDashboard().getNonGlobalFilterWidget().stream().filter(otherWidget -> deleteWidget.getDatasource().equals(otherWidget.getDatasource())
                && !deleteWidget.getName().equals(otherWidget.getName())).count() > 0;

        if (!otherWidgetExists && !deleteWidget.canUseNativeName()) {
            datasources.remove(deleteWidget.getDatasource());
        }
    }

    public Component getDashboardComponent() {
        return dashboardComponent;
    }

    public void setDashboardComponent(Component dashboardComponent) {
        this.dashboardComponent = dashboardComponent;
    }

    public boolean isStaticData() {
        return dashboard.isStaticData();
    }

    @Override
    public String toString() {
        return "DashboardConfig [isRAMPSConfig=" + isRAMPSConfig + ", reloadOutput=" + reloadOutput + ", dashboard=" + dashboard
                + ", mostRecentCI=" + mostRecentCI + ", datasources=" + datasources + ", flow=" + flow + "]";
    }

    public Tab getDashboardTab() {
        return dashboardTab;
    }

    public void setDashboardTab(Tab dashboardTab) {
        this.dashboardTab = dashboardTab;
    }

    /**Removes global filters from all the widgets, when the filter field is
     * part of deleting widget alone. 
     * @param deleteWidget
     */
    public void removeGlobalFilters(Widget deleteWidget) {
        
        boolean otherWidgetExists = false;
        
        if(deleteWidget.isFileBased()){
            otherWidgetExists = checkFileBasedWidgets(deleteWidget);
        }else if(deleteWidget.canUseNativeName()){
            otherWidgetExists = checkQueryBasedWidgets(deleteWidget);
        }
                        
        //If any other widget is using the deleting widget's datasource,do nothing. Otherswise
        //Remove the global filters which are based on deleting widget's datasource fields
        if (!otherWidgetExists) {
            List<Field> deletingWidgetFields = deleteWidget.getDatasourceFields();
            
            //this list contains the global filters, which are based on deleting widget
            List<Filter> globalFiltersToProcess=new ArrayList<>();
            Widget globalFltrWidget = getDashboard().getGlobalFilterWidget();
            if (globalFltrWidget != null) {
                for (Filter filter : globalFltrWidget.getFilters()) {
                    if (RampsUtil.isFieldPresent(deletingWidgetFields, filter)) {
                        globalFiltersToProcess.add(filter);
                    }
                }

                if (CollectionUtils.isNotEmpty(globalFiltersToProcess)) {

                    List<Field> allWidgetFields = new ArrayList<>();
                    getDashboard().getNonGlobalFilterWidget()
                        .stream().filter(widget -> !widget.equals(deleteWidget))
                        .forEach(widgets -> 
                            allWidgetFields.addAll(widgets.getDatasourceFields())
                         );

                    // check with other widget's fields, whether it contains the global filter field
                    // if, it is not containing the global filter field,remove the global filter
                    // from other widgets's filters list
                    globalFiltersToProcess.forEach(filter -> 
                        removeFilterFieldFromWidget(allWidgetFields, filter)
                    );

                }
                
                if(CollectionUtils.isEmpty(globalFltrWidget.getFilters())){
                    getDashboard().removeGlobalWidget();
                }
            }
        }
        return ;
    }

    private boolean checkQueryBasedWidgets(Widget deleteWidget) {
        return getDashboard().getNonGlobalFilterWidget().stream()
                .filter(otherWidget -> otherWidget.canUseNativeName() && deleteWidget.getQueryName().equals(otherWidget.getQueryName())
                        && !deleteWidget.equals(otherWidget)).count() > 0;
    }

    private boolean checkFileBasedWidgets(Widget deleteWidget) {
        if(deleteWidget.getDatasource() != null){
            return getDashboard().getNonGlobalFilterWidget().stream()
                .filter(otherWidget -> otherWidget != null && otherWidget.isFileBased() && deleteWidget.getDatasource().equals(otherWidget.getDatasource())
                        && !deleteWidget.equals(otherWidget)).count() > 0;
        }else{
            return false;
        }
        
    }
    
    public void removeFilterFieldFromWidget(List<Field> allWidgetFields,Filter filter){
        if (!RampsUtil.isFieldPresent(allWidgetFields, filter)) {
            for (Widget eachWidget : getDashboard().getWidgets()) {
                    // removes from all widgets (including global filter)
                if(eachWidget.getFilters() != null){
                    eachWidget.getFilters().remove(filter);
                }
            }
        }
    }

}
