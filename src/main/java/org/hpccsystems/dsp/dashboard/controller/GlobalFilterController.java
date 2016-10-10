package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartConfiguration;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.OutputSchema;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.GlobalFilter;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;

public class GlobalFilterController extends SelectorComposer<Component> {

    private static final String FILTER = " Filter";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalFilterController.class);
    private static final String ON_LOAD = "onLoad";


    @Wire
    private Tree fileTree;

    @Wire
    private Listbox droppedFilterbox;

    private ListModelList<Filter> filterModel = new ListModelList<>();

    private Dashboard dashboard;
    private Component parent;
    private List<Filter> originalFilters = null;

    private TreeitemRenderer<TreeNode<Field>> fileTreeRenderer = (item, data, index) -> {
        item.setLabel(data.getData().getColumn());
        //Expanding first node alone
        if(index == 0){
            item.setOpen(true);
        }
        if (data.isLeaf()) {
            item.setDraggable(Dashboard.TRUE);
        }
        item.setValue(data);
        item.setTooltiptext(item.getLabel());
    };
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        getSelf().addEventListener(ON_LOAD,event -> loadUI());
        Events.postEvent(ON_LOAD, getSelf(), null);
    }

    public void loadUI() {
        constructFiletreeModel();
        droppedFilterbox.setModel(filterModel);
        droppedFilterbox.setItemRenderer((item, filter, index) 
                -> DashboardUtil.renderFilter(item, (Filter)filter, filterModel, null));
        Widget globalFilterWidget = dashboard.getGlobalFilterWidget();
        
        if (globalFilterWidget != null && CollectionUtils.isNotEmpty(globalFilterWidget.getFilters())) {
            filterModel.addAll(globalFilterWidget.getFilters());
            originalFilters = new ArrayList<>();
            for (Filter filter : globalFilterWidget.getFilters()) {
                try {
                    originalFilters.add(filter.clone());
                } catch (CloneNotSupportedException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                }
            }
        }
    }
    
    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        dashboard = (Dashboard) Executions.getCurrent().getArg().get(Constants.DASHBOARD);
        this.parent = (Component) Executions.getCurrent().getArg().get(Dashboard.PARENT);
        return super.doBeforeCompose(page, parent, compInfo);
    }

    private void constructFiletreeModel() {
        Collection<TreeNode<Field>> nodes = createFileTreeNodes();
        nodes.addAll(createQueryNodes());
        TreeNode<Field> fileRoot = new DefaultTreeNode<>(new Field("", ""), nodes);
        DefaultTreeModel<Field> fileTreeModel = new DefaultTreeModel<>(fileRoot);
        fileTree.setModel(fileTreeModel);
        fileTree.setItemRenderer(fileTreeRenderer);
    }

   

    private Collection<TreeNode<Field>> createFileTreeNodes() {
        List<TreeNode<Field>> nodes = new ArrayList<>();
        // Handling widgets which uses logical files
        List<Widget> fileWidgets = dashboard.getWidgets().stream().filter(widget -> widget.isFileBased()).collect(Collectors.toList());
        fileWidgets = getUniqueFileWidgets(fileWidgets);
        
        if (fileWidgets != null) {
            fileWidgets.forEach(fileWidget -> {
                List<Field> fields;
                try {
                    fields = RampsUtil.getFileFields(fileWidget.getDatasource());
                    nodes.add(new DefaultTreeNode<Field>(new Field(fileWidget.getDatasource().getLabel(), null),createFieldNodes(fields)));
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                }

            });
        }
        return nodes;
    }

    private List<Widget> getUniqueFileWidgets(List<Widget> fileWidgets) {
        Map<String,Widget> uniqueDatasetWidgets = new HashMap<>();
        fileWidgets.forEach(widget ->
            uniqueDatasetWidgets.put(widget.getLogicalFile(), widget)
        );
        return new ArrayList<>(uniqueDatasetWidgets.values());
    }

    private Collection<TreeNode<Field>> createQueryNodes() {
        List<TreeNode<Field>> nodes = new ArrayList<>();
        // Handling widgets which uses queries
        List<Widget> queryWidgets = dashboard.getWidgets().stream().filter(widget -> widget.canUseNativeName()).collect(Collectors.toList());
        if (queryWidgets != null) {
            queryWidgets.forEach(queryWidget -> {
                List<OutputSchema> outputs = dashboard.getQueries().get(queryWidget.getQueryName()).getOutputs();
                for (OutputSchema output : outputs) {
                    nodes.add(new DefaultTreeNode<Field>(new Field(output.getName(), null),createFieldNodes(output.getFields())));
                }
            });
        }
        return nodes;
    }

    private Collection<TreeNode<Field>> createFieldNodes(List<Field> filteredFields) {
        List<TreeNode<Field>> nodes = new ArrayList<>();
        for (Field field : filteredFields) {
            nodes.add(new DefaultTreeNode<Field>(field));
        }
        return nodes;
    }

    @Listen("onClose = #globalFilterContainer")
    public void closeWidgetConfiguration() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cloing Global filter page.!");
        }
        Events.postEvent(Dashboard.EVENTS.ON_GLOBAL_FILTER_CLOSE, parent, null);
    }

    @Listen("onDrop = #droppedFilterbox")
    public void onDropFilters(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> fieldNode = draggedItem.getValue();
            Filter filter = new Filter(fieldNode.getData());
            filter.setGlobal(true);
            if (filterAlreadyExists(filter)) {
                Clients.showNotification(Labels.getLabel("filterAlreadyExists"), Clients.NOTIFICATION_TYPE_ERROR,
                        droppedFilterbox, Constants.POSITION_TOP_CENTER, 5000, true);
            } else {
                filterModel.add(filter);
            }
        }
    }
    
    @Listen("onClick= #cancel")
    public void closeGlobalFilter(){
        Events.postEvent(Dashboard.EVENTS.ON_GLOBAL_FILTER_CLOSE, parent, null); 
    }

    @Listen("onClick = #saveGlobalFilter")
    public void saveGlobalFilter() {
        //check for dropped filters
        if (!filterModel.isEmpty()) {

            try {
                Widget globalFilterWidget = dashboard.getGlobalFilterWidget();
                if(globalFilterWidget == null){
                    globalFilterWidget = createGlobalFilter();
                    dashboard.getWidgets().add(globalFilterWidget);
                }

                for (Filter filter: filterModel.getInnerList()) {
                    filter.setValue(filter.getNewValue());
                }
                
                globalFilterWidget.setFilters(filterModel.getInnerList());
                
                //iterate through all widgets
                dashboard.getNonGlobalFilterWidget().forEach(localWidget -> 
                    //set the global filters at each widget level
                    this.addGlobalFilterToWidgets(localWidget)
                );
               
                //To enable  save button in canvas page, when there is changes in filters
                LOGGER.debug("filterModel ---->{}",filterModel);
                LOGGER.debug("originalFilters ---->{}",originalFilters);
                if(!filterModel.equals(originalFilters)){
                    dashboard.setChanged(true);
                }
                
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                LOGGER.error("Not able to create global filter widget {}", e);
                Clients.showNotification(Labels.getLabel("globalFilterNotconfigured"), Clients.NOTIFICATION_TYPE_ERROR,
                        getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            }           
            
        } else {
            Widget globalFilterWidget = dashboard.getGlobalFilterWidget();
            //User didnt add any filter
            if(globalFilterWidget == null){
                Clients.showNotification(Labels.getLabel("globalFilterNotconfigured"), Clients.NOTIFICATION_TYPE_INFO,
                        getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            }else{
                //To enable  save button in canvas page
                dashboard.setChanged(true);
                
                //Removing all global filters at each widget level
                dashboard.getNonGlobalFilterWidget().forEach(localWidget -> 
                    removeOldGlobalFilter(localWidget)
                );
                dashboard.removeGlobalWidget();
                
                //User removed all the global filters
                Clients.showNotification(Labels.getLabel("globalFilterRemoved"), Clients.NOTIFICATION_TYPE_INFO,
                        getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
            }
        }
        
        Events.postEvent(Dashboard.EVENTS.ON_GLOBAL_FILTER_CLOSE, parent, null);
    }

    private Widget createGlobalFilter() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // get chart config for global filter
        ChartConfiguration chartConfiguration = Dashboard.CHARTS_CONFIGURATION.get(ChartType.GLOBAL_FILTER);
        Widget globalFilterWidget = (GlobalFilter) Class.forName(chartConfiguration.getClassName()).newInstance();
        // creating global filter widget
        globalFilterWidget.setChartConfiguration(chartConfiguration);
        globalFilterWidget.setName(DashboardUtil.removeSpaceSplChar(dashboard.getName()));
        globalFilterWidget.setTitle(dashboard.getLabel()+FILTER);
        
        return globalFilterWidget;
    }

    /**
     * Adds the global filter object to the local widget
     */
    public void addGlobalFilterToWidgets(Widget widget) {

        if(widget.getFilters() == null){
            widget.setFilters(new ArrayList<>());
        }else{
            removeOldGlobalFilter(widget);
        }        
        
        List<Filter> widgetsFilters = widget.getFilters(); 
        List<Field> widgetFileFields = widget.getDatasourceFields();
        
        filterModel.forEach(globalfilter -> updateFilter(widgetsFilters, widgetFileFields, globalfilter));
        LOGGER.debug("local widget.filters --->{}",widget.getFilters());
        
    }

    private void updateFilter(List<Filter> widgetsFilters, List<Field> widgetFileFields, Filter globalfilter) {
        //Add the global filter to widget, 
        //if the widget's logicalFile/query has the global filter column
        if(RampsUtil.isFieldPresent(widgetFileFields, globalfilter)){
          //If widget already has the selected global filter as local filter
            //override it with the global filter
            if (widgetsFilters.contains(globalfilter)) {
                widgetsFilters.remove(widgetsFilters.indexOf(globalfilter));
                widgetsFilters.add(globalfilter);
            } else {
                widgetsFilters.add(globalfilter);
            }
        }
    }

    /**
     * Removes the global filters which are in local widget's filter list
     */
    private void removeOldGlobalFilter(Widget widget) {
       if(CollectionUtils.isNotEmpty(widget.getFilters())){
            List<Filter> widgetsGlobalFilter = widget.getGlobalFilters();
            // remove the global filters from local widget
            widget.getFilters().removeAll(widgetsGlobalFilter); 
       }
    }
    
    /**
     * Checks if the column already exists in the existing list of Global Filters
     * @param filter
     * @return
     */
    public boolean filterAlreadyExists(Filter filter) {
    	boolean result = false;
    	for(Filter objFilter : filterModel) {
    		if(filter.getColumn().equals(objFilter.getColumn())) {
    			result = true;
            }
    	}
    	return result;
    }
}
