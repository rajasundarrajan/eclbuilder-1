package org.hpccsystems.dsp.dashboard.controller;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.service.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.spring.SpringUtil;

public class WidgetConfig  implements Serializable{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(WidgetConfig.class);
    
    private Component dashboardCanvas;
    private Component widgetCanvas;
    private ChartType chartType;
    private String chartname;
    private PluginOutput datasource;
    private DATASOURCE datasourceType;
    
    private Widget widget;
    private boolean isNewCreation;
    private int index;
    private boolean isDatasourceUpdated;
    private String queryName;
    private Widget originalWidget;
    
    /**
     * These property holds files/queries which is chosen from file/query browser
     * but not used by any other widgets.Will be used in recently used files/queries page and user is not done with widget configuration.
     * Once the user finishing widget configuration, this queries/files will become empty 
     */
    private Set<String> unusedFiles;
    private Set<String> unsedQueries;
    
    public WidgetConfig(boolean isNew) {
        this.isNewCreation = isNew;
    }

    public WidgetConfig() {
    }

    public ChartType getChartType() {
        return chartType;
    }

    public void setChartType(ChartType chartType) {
        this.chartType = chartType;
    }

    public Component getDashboardCanvas() {
        return dashboardCanvas;
    }

    public void setDashboardCanvas(Component dashboardCanvas) {
        this.dashboardCanvas = dashboardCanvas;
    }

    public PluginOutput getDatasource() {
        return datasource;
    }

    public void setDatasource(PluginOutput datasource) {
        this.datasource = datasource;
    }

    public String getChartname() {
        return chartname;
    }

    public void setChartname(String chartname) {
        this.chartname = chartname;
    }

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public boolean isNewCreation() {
        return isNewCreation;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isDatasourceUpdated() {
        return isDatasourceUpdated;
    }

    public void setDatasourceUpdated(boolean isDatasourceUpdated) {
        this.isDatasourceUpdated = isDatasourceUpdated;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public Component getWidgetCanvas() {
        return widgetCanvas;
    }

    public void setWidgetCanvas(Component widgetCanvas) {
        this.widgetCanvas = widgetCanvas;
    }

    public boolean hasValidLogicalFile() {
        return getLogicalFile() != null;
    }

    public Widget getOriginalWidget() {
        return originalWidget;
    }

    public void setOriginalWidget(Widget originalWidget) {
        this.originalWidget = originalWidget;
    }
    
    /**
     * @return
     *  Logical file in associated record instance. 
     *  null if not available or is 'file'
     */
    public String getLogicalFile() {
        String logicalFile = null;
        if(datasource != null && datasource.getContractInstance() != null) {
            logicalFile = datasource.getContractInstance().getProperty(Widget.LOGICAL_FILENAME);
        }
        
        if("file".equals(logicalFile)) {
            logicalFile = null;
        }
        
        return logicalFile;
    }
    
    /**
     * 
     * @param dashboardConfig
     * @param logicalFileName
     * @param structure
     *  Only necessary for Static Data. Can be null for HPCC based Widgets
     * @throws HipieException
     * @throws HPCCException 
     */
    public void updateContractInstance(DashboardConfig dashboardConfig,
            String logicalFileName, List<Object> structure)
                    throws HipieException, HPCCException {
        ContractInstance contractInstance = null;
        if(datasource != null) {
            contractInstance = datasource.getContractInstance();
        } 
        boolean isChanged = contractInstance == null || !logicalFileName.equals(contractInstance.get(Widget.LOGICAL_FILENAME));
        if(isChanged) {
            PluginOutput output = dashboardConfig.getDatasource(logicalFileName);
            //Removing exsting CI
            if(widget != null && !widget.canUseNativeName()) {
                dashboardConfig.removeDatasource(widget);
            }
            
            if(output != null) {
                setDatasource(output);
                contractInstance = datasource.getContractInstance();
                setDatasourceUpdated(true);
                return;
            } else {
                //Creating a new CI
                contractInstance = createDatasourceInstance(dashboardConfig);
            }
            
        }
        
        updateContractInstance(dashboardConfig, logicalFileName, structure, contractInstance);

        // Setting the field to detect change to reload Config page
        setDatasourceUpdated(isChanged);
        
        contractInstance.setProperty(Widget.LOGICAL_FILENAME, logicalFileName);
        this.setDatasourceType(DATASOURCE.FILE);
    }

    private void updateContractInstance(DashboardConfig dashboardConfig, String logicalFileName, List<Object> structure,
            ContractInstance contractInstance)
            throws HPCCException {
        RecordInstance recordInstance = null;
        try {
            Dashboard dashboard = dashboardConfig.getDashboard();
            if(dashboard.isStaticData()) {
                recordInstance = RecordInstance.CreateRecordInstance(structure);
            } else {
                recordInstance = dashboard.getHpccConnection().getDatasetFields(logicalFileName, null);
            }
            LOGGER.debug("record instance ----> {}",recordInstance.toEclString());
            contractInstance.setProperty("Structure", recordInstance.toEclString());
        } catch (Exception e) {
            //TODO:Instead of throwing all type of exception, 
            //should throw only when the structure is invalid/has no structure
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException(Labels.getLabel("unableToFetchFileInfo"));
        }
    }

    private ContractInstance createDatasourceInstance(DashboardConfig dashboardConfig) throws HipieException {
        ContractInstance contractInstance;
        
        PluginService pluginService = (PluginService) SpringUtil.getBean("pluginService");
        ContractInstance dataset = null;
        dataset = CompositionUtil.createDatasourceInstance(pluginService.getDatasourceContract());
        PluginOutput pluginOutput = new PluginOutput(dataset);
        dashboardConfig.addDatasource(pluginOutput);
        setDatasource(pluginOutput);
        
        contractInstance =  datasource.getContractInstance();
        return contractInstance;
    }

    public DATASOURCE getDatasourceType() {
        return datasourceType;
    }

    public void setDatasourceType(DATASOURCE datasourceType) {
        this.datasourceType = datasourceType;
    }

    public boolean hasValidStaticData() {
        return widget != null && widget.getQueryName() != null;
    }
    
    public Set<String> getUnusedFiles() {
        return unusedFiles;
    }

    public void setUnusedFiles(Set<String> unUsedFiles) {
        this.unusedFiles = unUsedFiles;
    }

    public Set<String> getUnusedQueries() {
        return unsedQueries;
    }

    public void setUnusedQueries(Set<String> unUsedQueries) {
        this.unsedQueries = unUsedQueries;
    }
}
