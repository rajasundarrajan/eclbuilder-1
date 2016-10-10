package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.controller.WidgetConfig;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.entity.widget.OutputSchema;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.entity.widget.SortField;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.GlobalFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.Table;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.XYChart;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.HPCCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.DefaultTreeModel;
import org.zkoss.zul.DefaultTreeNode;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Vlayout;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ConfigurationComposer<T> extends SelectorComposer<Component> {

    private static final String IMG_BTN_BTN_CLOSE = "img-btn btn-close";
    private static final String FLOAT_RIGHT = "float : right;";
    private static final String NATURAL_ORDER = "Natural order";
    private static final String CLICKED = "clicked";
    private static final String SORT_ALPHABETICALLY = "Sort alphabetically";
    private static final String NOT_CLICKED = "not_clicked";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationComposer.class);

    protected WidgetConfig widgetConfiguration;
    protected DashboardConfig dashboardConfig;
    protected Widget widget;
    protected String outputName;
    Map<Measure, SortField> sortAggregateSync = new HashMap<Measure, SortField>();

    @Wire
    protected Listbox inputParameterListbox;

    @Wire
    protected Vlayout inputParameterLayout;

    @Wire
    protected Tree measureTree;

    @Wire
    protected Tree attributeTree;

    @Wire
    private Button measureSort;

    @Wire
    private Button attributeSort;

    @Wire
    private Listbox sortfieldBox;

    @Wire
    private Intbox records;
    
    @Wire
    private Listbox filterbox;

    @Wire
    private Vlayout filterLayout;

    private ListModelList<Filter> filterModel = new ListModelList<Filter>(); 

    ListModelList<SortField> fieldModel = new ListModelList<SortField>();
    List<Field> fields = null;
    QuerySchema querySchema = null;

    private static final String Z_ICON_TIMES = "z-icon-times";

    public static final Comparator<OutputSchema> SORT_BY_OUTPUT_NAME = new Comparator<OutputSchema>() {
        public int compare(OutputSchema e1, OutputSchema e2) {
            return e1.getName().compareTo(e2.getName());
        }
    };
    
    private ListitemRenderer<Filter> filterRenderer = (item, filter, index) -> 
        DashboardUtil.renderFilter(item, filter, filterModel, widget);
    
    private ListitemRenderer<Filter> inputParameterRenderer = (listitem, param, index) -> renderInputParameter(listitem, param);

    private ListitemRenderer<SortField> sortfieldBoxRenderer = (listitem, param, index) -> renderSortField(listitem, param);

    private TreeitemRenderer<TreeNode<Field>> attributeRenderer = (item, data, index) -> {
        item.setLabel(data.getData().getColumn());
        item.setOpen(true);
        if (data.isLeaf()) {
            item.setDraggable(Dashboard.TRUE);
        }
        item.setValue(data);
        item.setTooltiptext(item.getLabel());
    };

    private TreeitemRenderer<TreeNode<Field>> measureRenderer = (item, data, index) -> {
        item.setLabel(data.getData().getColumn());
        item.setOpen(true);
        if (data.isLeaf()) {
            item.setDraggable(Dashboard.TRUE);
        }
        item.setValue(data);
        item.setTooltiptext(item.getLabel());
    };
    protected SerializableEventListener<Event> loadingListener = new SerializableEventListener<Event>() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        @Override
        public void onEvent(Event event) throws Exception {
            try {
                load();
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                        true);
                Clients.clearBusy(ConfigurationComposer.this.getSelf());
                return;
            }

        }
    };

    private void renderInputParameter(Listitem listitem, Filter param) {
        Listcell listcell = new Listcell();
        Vlayout vlayout = new Vlayout();       
        listcell.appendChild(vlayout);
        
        Label paramName = new Label(param.getColumn());
        Textbox inputParamValue = new Textbox();
        inputParamValue.setHflex("min");
        
        vlayout.appendChild(paramName);
        vlayout.appendChild(inputParamValue);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("paramValue -->{}", param.getValue());
        }
        
        inputParamValue.setValue(param.getValue());
        EventListener<InputEvent> changeListener = event -> param.setValue(event.getValue());
        inputParamValue.addEventListener(Events.ON_CHANGING, changeListener);
        
        listitem.appendChild(listcell);
    }

    private void renderSortField(Listitem listitem, SortField param) {
        listitem.setTooltiptext(param.getColumn());
        Listcell cell = new Listcell();
        cell.setSclass("sort_field_cell");
        Label name = new Label(param.getColumn());
        name.setParent(cell);
        if (param.isNotNone()) {
            Button aggregate = new Button();
            aggregate.setZclass("btn btn-xs btn-sum");
            aggregate.setStyle("vertical-align: -webkit-baseline-middle;");
            aggregate.setLabel(param.getAggregation().toString());
            aggregate.setParent(cell);
        }
        Button delete = new Button();
        delete.addEventListener(Events.ON_CLICK, event -> {
            fieldModel.remove(param);
            widget.removeSortField(param);
            for (Map.Entry<Measure, SortField> removeEntry : sortAggregateSync.entrySet()) {
                if (removeEntry.getValue().equals(param)) {
                    sortAggregateSync.remove(removeEntry.getKey());
                    break;
                }
            }
        });
        delete.setStyle(FLOAT_RIGHT);
        delete.setSclass(IMG_BTN_BTN_CLOSE);
        delete.setIconSclass(Z_ICON_TIMES);
        Button sort = new Button();
        sort.setStyle(FLOAT_RIGHT);
        sort.setSclass(IMG_BTN_BTN_CLOSE);
        toggleAscendingIcon(param, sort, cell);
        sort.addEventListener(Events.ON_CLICK, event -> {
            param.setAscending(!param.isAscending());
            toggleAscendingIcon(param, sort, cell);
        });
        delete.setParent(cell);
        sort.setParent(cell);
        cell.setParent(listitem);
        listitem.setDraggable(Dashboard.TRUE);
        listitem.setDroppable(Dashboard.TRUE);
        listitem.addEventListener(Events.ON_DROP, event -> onDropFieldBox((DropEvent) event));
    }

    private void load() throws IOException, HPCCException, HipieException {
        List<Filter> inputParameters = null;

        attributeTree.setItemRenderer(attributeRenderer);
        measureTree.setItemRenderer(measureRenderer);

        setWidgetModel();
        if (widget.getRecordLimit() > 0) {
            records.setValue(widget.getRecordLimit());
        }

        if (widget.canUseNativeName()) {
            if (dashboardConfig.getDashboard().getQueries() == null) {
                dashboardConfig.getDashboard().setQueries(new HashMap<String, QuerySchema>());
            }
            querySchema = dashboardConfig.getDashboard().getQueries().get(widget.getQueryName());
            if (querySchema == null) {
                checkAndUpdateQuerySchema();
            }
            constructAttributeModelsForSchema(false);
            constructMeasureModelsForSchema(false);

            if (querySchema.getInputParameters() != null && !querySchema.getInputParameters().isEmpty()) {
                inputParameterLayout.setVisible(true);
                getSelf().invalidate();
                
                inputParameters = querySchema.getInputParameters();
                inputParameterListbox.setItemRenderer(inputParameterRenderer);
                inputParameterListbox.setModel(new ListModelList<Filter>(inputParameters));
            }

            dashboardConfig.getDashboard().getQueries().put(widget.getQueryName(), querySchema);

        } else if(widgetConfiguration.getWidget().hasDatasource() || widgetConfiguration.getWidget().isDatabomb()){
            fields = RampsUtil.getFileFields(widgetConfiguration.getDatasource());
            constructAttributeModelsForFields(false);
            constructMeasureModelsForFields(false);
        }

        measureSort.setSclass(NOT_CLICKED);
        attributeSort.setSclass(NOT_CLICKED);
        measureSort.setTooltiptext(SORT_ALPHABETICALLY);
        attributeSort.setTooltiptext(SORT_ALPHABETICALLY);
        measureSort.setAttribute(Constants.MEASURE_SORTED, false);
        attributeSort.setAttribute(Constants.ATTRIBUTE_SORTED, false);

        measureSort.addEventListener(Events.ON_CLICK, event -> sortMeasure());

        attributeSort.addEventListener(Events.ON_CLICK,  event -> sortAttribute());

        Clients.clearBusy(ConfigurationComposer.this.getSelf());
    }

    private void setWidgetModel() {
        if (widget instanceof Table || widget instanceof XYChart) {
            sortfieldBox.setModel(fieldModel);
            sortfieldBox.setItemRenderer(sortfieldBoxRenderer);
        }
    }

    private void checkAndUpdateQuerySchema() throws HipieException {
        if (widget.isQueryBased()) {
            try {
                querySchema = ((HPCCService)SpringUtil.getBean(Constants.HPCC_SERVICE)).getQuerySchema(widget.getQueryName(), dashboardConfig.getDashboard().getHpccConnection());
            } catch (HPCCException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                throw new HipieException(d);
            }
        } else if (widget.isDatabomb()) {
            querySchema = new QuerySchema();
            String[] userAndFile = DashboardUtil.getUserAndFilename(widget.getQueryName());
            
            if(ArrayUtils.isNotEmpty(userAndFile) && userAndFile.length == 2){                         
                Map<String, RecordInstance> outputFields;
                try {
                    CompositionService service =  (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
                    StaticData data = service.getStaticData(userAndFile[0], userAndFile[1]);
                    outputFields = Utility.getDatabombOutputFields(null, data.getFileContent(), null, false, '.', '.', '.', null);
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    throw new HipieException(e);
                }
                for (Entry<String, RecordInstance> entry : outputFields.entrySet()) {
                    OutputSchema schema = new OutputSchema();
                    
                    schema.setName(entry.getKey());
                    List<Field> fieldsList = new ArrayList<Field>();
                    entry.getValue().stream().forEach( fi -> fieldsList.add(new Field(fi.getName(), fi.getType())));
                    schema.setFields(fieldsList);
                    querySchema.addOutput(schema);
                }
            } 
        }
    }

    private void toggleAscendingIcon(SortField param, Button sort, Listcell cell) {
        if (param.isAscending()) {
            cell.setIconSclass("fa fa-sort-alpha-asc");
            sort.setIconSclass("fa fa-sort-asc");
            sort.setTooltiptext("Sort Descending");
        } else {
            cell.setIconSclass("fa fa-sort-alpha-desc");
            sort.setIconSclass("fa fa-sort-desc");
            sort.setTooltiptext("Sort Ascending");
        }
    }
    
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        widgetConfiguration = (WidgetConfig) Executions.getCurrent().getArg().get(Dashboard.WIDGET_CONFIG);

        LOGGER.debug("Is Ramps config - {}", dashboardConfig.isRAMPSConfig());

        widget = widgetConfiguration.getWidget();
        
        // Skipping rendering filter UI as filter will not be available for
        // Scored Search and query
        LOGGER.debug("Is query based --->{}", widget.isQueryBased());
        if (!ChartType.SCORED_SEARCH.equals(widget.getChartConfiguration().getType())
               && !ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType()) && !widget.isQueryBased()) {
            filterLayout.setVisible(true);
            LOGGER.debug("Is query based ---->{}", widget.isQueryBased());
            filterbox.setModel(filterModel);
            filterbox.setItemRenderer(filterRenderer);
            
            if(CollectionUtils.isNotEmpty(widget.getLocalFilters())){
                filterModel.addAll(widget.getLocalFilters());
            }
        }
        
        if (widget.canUseNativeName() && widget.getQueryOutput() != null) {
            outputName = widget.getQueryOutput().getName();
        }

    }

    @Listen("onChange=#records")
    public void onAddLimitRecords() {
        widget.setRecordLimit(records.getValue() == null ? 0 : records.getValue());
    }

    @Listen("onDrop=#sortfieldBox")
    public void onDropFieldBox(DropEvent event) {
        Field droppedField = null;
        AGGREGATION sortAggregate = null;
        Measure measure = null;
        if (!(event.getDragged() instanceof Treeitem)) {
            if ("sortfieldBox".equals(event.getDragged().getParent().getId())) {
                reorder(event);
                return;
            } else {
                Listitem draggedItem = (Listitem) event.getDragged();
                if (draggedItem.getValue() instanceof Measure) {
                    measure = (Measure) draggedItem.getValue();
                    sortAggregate = measure.getAggregation();
                }
                droppedField = new Field(draggedItem.getValue());
            }
        } else {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> node = draggedItem.getValue();
            droppedField = (Field) node.getData();
        }
        if (widget.getSortFields() != null && widget.hasSortField(droppedField)) {
            Clients.showNotification("Field already available", Clients.NOTIFICATION_TYPE_ERROR, sortfieldBox, Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }
        SortField sort = new SortField(droppedField);
        
        //For row count the aggregation is "SUM" by default this can't be changed on the UI
        if(!droppedField.isRowCount()){
            sort.setAggregation(sortAggregate);
        }
        
        if (sort.getAggregation() != null) {
            sortAggregateSync.put(measure, sort);
        }
        widget.addSortField(sort);
        fieldModel.add(sort);
    }

    private void reorder(DropEvent event) {
        int from = sortfieldBox.getChildren().indexOf((Listitem) event.getDragged()) - 1;
        if (event.getTarget().getParent() instanceof Listbox) {
            int to = sortfieldBox.getChildren().indexOf((Listitem) event.getTarget()) - 1;
            Field temp = fieldModel.get(from);
            if (from < to) {
                fieldModel.add(to + 1, new SortField(temp));
                fieldModel.remove(from);
            } else {
                fieldModel.add(to, new SortField(temp));
                fieldModel.remove(from + 1);
            }
        } else {
            fieldModel.add(fieldModel.get(from));
            fieldModel.remove(from);
        }
        widget.setSortFields(new ArrayList<SortField>());
        widget.getSortFields().addAll(fieldModel);
    }

    private void sortAttribute() {
        boolean previousState = (boolean) attributeSort.getAttribute(Constants.ATTRIBUTE_SORTED);
        if (!previousState) {
            attributeSort.setSclass(CLICKED);
            attributeSort.setTooltiptext(NATURAL_ORDER);
            if (widget.canUseNativeName()) {
                constructAttributeModelsForSchema(true);
            } else {
                constructAttributeModelsForFields(true);
            }
        } else {
            attributeSort.setSclass(NOT_CLICKED);
            attributeSort.setTooltiptext(SORT_ALPHABETICALLY);
            if (widget.canUseNativeName()) {
                constructAttributeModelsForSchema(false);
            } else {
                constructAttributeModelsForFields(false);
            }
        }
        attributeSort.setAttribute(Constants.ATTRIBUTE_SORTED, !previousState);
    }

    private void sortMeasure() {
        boolean previousState = (boolean) measureSort.getAttribute(Constants.MEASURE_SORTED);
        if (!previousState) {
            measureSort.setSclass(CLICKED);
            measureSort.setTooltiptext(NATURAL_ORDER);
            if (widget.canUseNativeName()) {
                constructMeasureModelsForSchema(true);
            } else {
                constructMeasureModelsForFields(true);
            }
        } else {
            measureSort.setSclass(NOT_CLICKED);
            measureSort.setTooltiptext(SORT_ALPHABETICALLY);
            if (widget.canUseNativeName()) {
                constructMeasureModelsForSchema(false);
            } else {
                constructMeasureModelsForFields(false);
            }
        }
        measureSort.setAttribute(Constants.MEASURE_SORTED, !previousState);
    }

    private void constructAttributeModelsForFields(boolean sortAlpha) {
        TreeNode<Field> attributeRoot = new DefaultTreeNode<Field>(new Field("", ""), createAttributeOutputNodesForFields());
        DefaultTreeModel<Field> attributeModel = new DefaultTreeModel<Field>(attributeRoot);
        if (sortAlpha) {
            attributeModel.sort(ConfigurationComposer::compareFieldNodes, true);
        }
        attributeTree.setModel(attributeModel);
    }

    private void constructAttributeModelsForSchema(boolean sortAlpha) {
        TreeNode<Field> attributeRoot = new DefaultTreeNode<Field>(new Field("", ""), createAttributeOutputNodesForQuery());
        DefaultTreeModel<Field> attributeModel = new DefaultTreeModel<Field>(attributeRoot);
        if (sortAlpha) {
            attributeModel.sort(ConfigurationComposer::compareFieldNodes, true);
        }
        attributeTree.setModel(attributeModel);
    }

    private void constructMeasureModelsForFields(boolean sortAlpha) {
        TreeNode<Field> measureRoot = new DefaultTreeNode<Field>(new Field("", ""), createMeasureOutputNodesForFields());
        DefaultTreeModel<Field> measureModel = new DefaultTreeModel<Field>(measureRoot);
        if (sortAlpha) {
            measureModel.sort(ConfigurationComposer::compareFieldNodes, true);
        }
        measureTree.setModel(measureModel);
    }

    private void constructMeasureModelsForSchema(boolean sortAlpha) {
        TreeNode<Field> measureRoot = new DefaultTreeNode<Field>(new Field("", ""), createMeasureOutputNodesForQuery());
        DefaultTreeModel<Field> measureModel = new DefaultTreeModel<Field>(measureRoot);
        if (sortAlpha) {
            measureModel.sort(ConfigurationComposer::compareFieldNodes, true);
        }
        measureTree.setModel(measureModel);
    }

    private Collection<TreeNode<Field>> createAttributeOutputNodesForFields() {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        nodes.add(new DefaultTreeNode<Field>(new Field(widget.getDatasource().getLabel(), null), createAttributeNodes(fields.stream()
                .filter(field -> !field.isNumeric()).collect(Collectors.toList()))));

        return nodes;
    }

    private Collection<TreeNode<Field>> createMeasureOutputNodesForFields() {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        nodes.add(new DefaultTreeNode<Field>(new Field(widget.getDatasource().getLabel(), null), createMeasureNodes(fields.stream()
                .filter(field -> field.isNumeric()).collect(Collectors.toList()))));

        return nodes;
    }

    private Collection<TreeNode<Field>> createAttributeOutputNodesForQuery() {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        List<OutputSchema> outputs = querySchema.getOutputs();
        Collections.sort(outputs, SORT_BY_OUTPUT_NAME);
        for (OutputSchema output : outputs) {
            nodes.add(new DefaultTreeNode<Field>(new Field(output.getName(), null), createAttributeNodes(output.getFields().stream()
                    .filter(field -> !field.isNumeric()).collect(Collectors.toList()))));
        }

        Collections.sort(nodes, ConfigurationComposer::compareFieldNodes);
        return nodes;
    }

    private Collection<TreeNode<Field>> createMeasureOutputNodesForQuery() {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        List<OutputSchema> outputs = querySchema.getOutputs();
        Collections.sort(outputs, SORT_BY_OUTPUT_NAME);
        for (OutputSchema output : outputs) {
            nodes.add(new DefaultTreeNode<Field>(new Field(output.getName(), null), createMeasureNodes(output.getFields().stream()
                    .filter(field -> field.isNumeric()).collect(Collectors.toList()))));
        }

        return nodes;
    }

    private Collection<TreeNode<Field>> createAttributeNodes(List<Field> filteredFields) {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        for (Field field : filteredFields) {
            nodes.add(new DefaultTreeNode<Field>(field));
        }
        return nodes;
    }

    private Collection<TreeNode<Field>> createMeasureNodes(List<Field> filteredFields) {
        List<TreeNode<Field>> nodes = new ArrayList<TreeNode<Field>>();
        for (Field field : filteredFields) {
            nodes.add(new DefaultTreeNode<Field>(field));
        }
        
        //Adding row count measure for non-Databomb visualizations
        if (!dashboardConfig.isStaticData()) {
            Field field = new Field();
            field.setColumn(Constants.ROW_COUNT);
            field.setRowCount(true);
            field.setDataType(Constants.UNSIGNED);
            nodes.add(new DefaultTreeNode<Field>(field));
        }
        
        return nodes;
    }

    /**
     * checks whether the dropped Attribute/Measure are from same dataset/output
     * 
     * @param draggedOutputName
     * @return boolean
     */
    protected boolean checkOutputs(String draggedOutputName) {
        if (!widget.canUseNativeName()) {
            return true;
        }
        if (outputName == null) {
            outputName = draggedOutputName;

            widget.setQueryOutput(dashboardConfig.getDashboard().getQueries().get(widget.getQueryName()).getOutputs().stream()
                    .filter(output -> outputName.equals(output.getName())).findAny().get());
            return true;
        } else {
            return draggedOutputName.equals(outputName);
        }
    }

    public static int compareFieldNodes(TreeNode<Field> o1, TreeNode<Field> o2) {
        return o1.getData().getColumn().compareTo(o2.getData().getColumn());
    }
    
    @Listen("onDrop = #filterbox")
    public void onDropFilters(DropEvent event) {
        if(event.getDragged() instanceof Treeitem){
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> fieldNode = draggedItem.getValue();
            if(fieldNode.getData().isRowCount()){
                Clients.showNotification(Labels.getLabel("rowcountNotFilterfield"), Clients.NOTIFICATION_TYPE_ERROR,
                        filterbox, Constants.POSITION_TOP_CENTER, 3000, true);
                return;
            }
            Filter filter= new Filter(fieldNode.getData());
            if (widget.getLocalFilters() != null && widget.getLocalFilters().contains(filter)) {
                Clients.showNotification(Labels.getLabel("filterAlreadyExists"), Clients.NOTIFICATION_TYPE_ERROR,
                        filterbox, Constants.POSITION_END_CENTER, 5000, true);
            } else {
                List<String> globalFilters = null;
                if (dashboardConfig.getDashboard().getGlobalFilterWidget() != null) {
                    globalFilters = dashboardConfig.getDashboard().getGlobalFilterWidget().getFilters()
                            .stream().map(Filter::getColumn).collect(Collectors.toList());
                }
                if (globalFilters != null && globalFilters.contains(filter.getColumn())) {
                    Clients.showNotification(Labels.getLabel("globalFilterAlreadyExists"),
                            Clients.NOTIFICATION_TYPE_ERROR, filterbox, Constants.POSITION_TOP_CENTER, 5000, true);
                } else {
                    filterModel.add(filter);
                    widget.addFilter(filter);
                    //TODO:here while adding filter to local widget, it is getting added into global widget,
                    //Should avoid it, but no clue of how it happens.
                    GlobalFilter globalWidget = (GlobalFilter) dashboardConfig.getDashboard().getGlobalFilterWidget();
                    if(globalWidget != null && globalFilters != null){
                        ((GlobalFilter)globalWidget).removeNonGlobalFilters();
                    }
                }   
                
            }  
        }      
    }

    public Widget getWidget() {
        return widget;
    }

}
