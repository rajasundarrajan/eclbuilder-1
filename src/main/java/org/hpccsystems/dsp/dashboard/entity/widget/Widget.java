package org.hpccsystems.dsp.dashboard.entity.widget;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.DriveFieldInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.dude.option.SelectElementOption;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.ACTION;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.GlobalFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.Pie;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.Table;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.USMap;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.XYChart;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;

public abstract class Widget implements Cloneable , Serializable{
   
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Widget.class);

    public static final String LOGICAL_FILENAME = "LogicalFilename";
    public static final String CHART_TYPE = "_charttype";
    public static final String X_AXIS_TYPE = "_xAxisType";
    public static final String X_AXIS_TYPE_TIMEPATTERN = "_xAxisTypeTimePattern";
    public static final String HOLE_PERCENT = "_holePercent";
    public static final String STACKED = "_stacked";
    private static final String FIELD = "Field";
    public static final String DOT = ".";
    public static final List<String> AGGREGATE_STRINGS = Arrays.asList(AGGREGATION.values()).stream().map(aggrn -> aggrn.name()).collect(Collectors.toList());

    public static enum DATASOURCE {
        FILE, QUERY, STATIC_DATA
    }

    private String name;
    private PluginOutput datasource;
    private List<Filter> filters;
    private String title;
    private ChartConfiguration chartConfiguration;
    private List<SortField> sortFields;
    private int recordLimit;

    /**
     * Holds the source objects which creates
     * 'SELECTS(INPUTS.Field_sourceWidget->targetWidget.field,MERGE)' part of
     * visual element for interactivity
     */
    private List<Interaction> interactions;
    /**
     * Holds the target objects which creates 'FILTER(Field_sourceWidget)' part
     * of visual element for interactivity
     */
    private List<InteractionTarget> interactionTargets;
    private OutputSchema queryOutput;

    private String queryName;

    private DATASOURCE datasourceType;

    public abstract boolean isConfigured();

    public abstract VisualElement generateVisualElement() throws HipieException;

    public abstract void removeInvalidFields();

    public abstract boolean isValid();

    /**
     * Checks the contract to see if the Inputs already contain a field column name
     * already exists in the contract. If there is already a field then skip, otherwise
     * add a new field to the InputElements.
     * 
     * @param contractElements - a list of all the contract elements for the dashboard
     * @return - Modified InputElements with the 
     */
    public List<InputElement> generateInputElement(List<Element> contractElements, String datasourceName){
        List<InputElement> inputs = new ListModelList<InputElement>();
                
        // Check each field against the list of fields already added to the contract
        // to see if the field label already exists.
        for(Field field: getUniqueFields(getInputElementFields())) { 
            if( !checkForDuplicateFieldNames(field, contractElements) ) { // Element doesn't exist. Add it
                InputElement inputElement = new InputElement();
                inputElement.setName(datasourceName.concat("_").concat(field.getColumn()));
                inputElement.addOption(new ElementOption(Element.LABEL, new FieldInstance(null, field.getColumn())));
                inputElement.setType(InputElement.TYPE_FIELD);
                inputs.add(inputElement);
            } // No else because the field already exists so we don't need to add a new one for this measure.
        }

        return inputs;
    }
    
    /**
     * Checks the field against the contract elements already added. Returns true if the
     * field already exists.
     * @param field - The field to be added
     * @param contractElements - The elements that are already in the contract
     * @return true if field exists in contract elements, false if field is not in the contract elements
     */
    private boolean checkForDuplicateFieldNames(Field field, List<Element> contractElements) {
        for (Element element: contractElements) {
            for (Element child: element.getChildElements()) {
                if (child.getName().equals(field.getDudName())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * @return All field used in the Widget except Row Count.Used to generate
     *         input element of DUD
     */
    public abstract List<Field> getInputElementFields();

    /**
     * @return All field used in the Widget except Measure, InteractionTarget's
     *         field
     */
    public abstract List<Field> getRecordInstanceFields();

    protected abstract RecordInstance generateRecordInstance();

    public List<SortField> getSortFields() {
        return sortFields;
    }

    public void setSortFields(List<SortField> sortFields) {
        this.sortFields = sortFields;
    }

    public boolean hasSortField(SortField field) {
        return sortFields.contains(field);
    }

    public void addSortField(SortField field) {
        if (sortFields == null) {
            sortFields = new ArrayList<SortField>();
        }
        this.sortFields.add(field);
    }

    public void removeSortField(SortField field) {
        this.sortFields.remove(field);
    }

    public void removeFilter(Filter filter) {
        this.filters.remove(filter);
    }

    public void addFilter(Filter filter) {
        if (filters == null) {
            filters = new ArrayList<Filter>();
        }
        this.filters.add(filter);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public List<Filter> getLocalFilters() {
        if(filters == null){
            return filters;
        }
        return filters.stream().filter(filter -> !filter.isGlobal()).collect(Collectors.toList());
    }
    
    public List<Filter> getGlobalFilters() {
        if(filters == null){
            return filters;
        }
        return filters.stream().filter(filter -> filter.isGlobal()).collect(Collectors.toList());
    }
    
    public void setFilters(List<Filter> filters) {
        this.filters = filters;
    }

    public boolean hasLocalFilters() {
        if(CollectionUtils.isNotEmpty(filters)){
            return filters.stream().filter(filter -> !filter.isGlobal()).findAny().isPresent();
        }else{
            return false;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ChartConfiguration getChartConfiguration() {
        return chartConfiguration;
    }

    public void setChartConfiguration(ChartConfiguration chartConfiguration) {
        this.chartConfiguration = chartConfiguration;
    }

    public Stream<InteractionTarget> getInteractionTargets() {
        return interactionTargets != null ? interactionTargets.stream() : null;
    }

    public Stream<Interaction> getInteractions() {
        return interactions != null ? interactions.stream() : null;
    }

    public String getLogicalFile(){
        String fileName = null;
        if(isFileBased()){
            fileName = this.getDatasource().getContractInstance().getProperty(Constants.LOGICAL_FILENAME);
        }
        return fileName;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Widget [name=").append(name).append(", filters=").append(filters).append(", title=").append(title).append(", chartConfiguration=")
                .append(chartConfiguration).append(", queryName=").append(queryName).append(", interactionSelectPart=").append(interactions)
                .append(", interactionFilterPart=").append(interactionTargets).append("]");

        return builder.toString();
    }

    private static List<Measure> createMeasures(ElementOption option, VisualElement visualElement, ContractInstance contractInstance, Widget widget) {

        List<Measure> measures = new ArrayList<Measure>();
        option.getParams().stream().forEach(fieldInstance -> {
            
            fieldInstance = visualElement.resolveFieldReference(fieldInstance, contractInstance);
            
            // getting actual column name like 'productline' from instance
            // property as contractInstance.getProperty("Measure_piechart2")
            Field measureField = new Field();
            setFieldProps(widget, measureField, contractInstance, fieldInstance);
            
            Measure measure = new Measure(measureField);
            if(AGGREGATE_STRINGS.contains(fieldInstance.getType())){
                measure.setAggregation(AGGREGATION.valueOf(fieldInstance.getType()));
            }else{
                measure.setAggregation(AGGREGATION.NONE);
            }
           
            setDisplayName(widget, measure, contractInstance, fieldInstance);
            measures.add(measure);
        });

        return measures;
    }

    private static List<SortField> createSortFields(ElementOption option,
            ContractInstance contractInstance, Widget widget, VisualElement visualElement) {

        List<SortField> sortFields = new ArrayList<SortField>();
        option.getParams().stream().forEach(fieldInstance -> {
            fieldInstance = visualElement
                    .resolveFieldReference(fieldInstance, contractInstance);
            SortField sortField = new SortField();
            setSortFieldProps(widget, sortField, contractInstance, fieldInstance);
            
            sortFields.add(sortField);
        });

        return sortFields;
    }

    private static List<Attribute> createAttributes(ElementOption option, VisualElement visualElement, ContractInstance contractInstance, Widget widget) {

        List<Attribute> attributes = new ArrayList<Attribute>();
        option.getParams().stream().forEach(fieldInstance -> {
            fieldInstance = visualElement
                    .resolveFieldReference(fieldInstance, contractInstance);
            Field attributeField = new Field();
            setFieldProps(widget, attributeField, contractInstance, fieldInstance);
            Attribute attribute = new Attribute(attributeField);

            setDisplayName(widget, attribute, contractInstance, fieldInstance);
            
            if(!attributes.contains(attribute)) {
                attributes.add(attribute);
            }

        });
        if (widget instanceof XYChart && visualElement.getCustomOptions().get(Widget.X_AXIS_TYPE_TIMEPATTERN) != null) {
            attributes
            .get(0)
            .setTimeFormat(visualElement
                    .getCustomOptions()
                    .get(Widget.X_AXIS_TYPE_TIMEPATTERN)
                    .getParams()
                    .get(0)
                    .getName());
        }
        return attributes;
    }

    private static List<Field> createTableFields(VisualElement visualElement,
            ContractInstance contractInstance, Widget widget) {
        
        List<Field> tableColumns = new ArrayList<Field>();
        ElementOption valueOption = visualElement.getOption(VisualElement.VALUE);
        ElementOption labelOption = visualElement.getOption(VisualElement.LABEL);
        
        valueOption.getParams().stream().forEach(fieldInstance ->
        // resolve any field references to their actual input field type and name with visualElement.resolveFieldReference()
        tableColumns.add(createTableField(
                visualElement, contractInstance, widget, visualElement
                        .resolveFieldReference(fieldInstance, contractInstance))));
        
        setFieldDisplayName(widget, labelOption, tableColumns, contractInstance);
        return tableColumns;
    }
    
    private static void setFieldDisplayName(Widget widget, ElementOption labelOption, List<Field> tableColumns, ContractInstance contractInstance) {
        labelOption.getParams().stream().forEach(labelInstance -> {
            Field column = tableColumns.get(labelOption.getParams().getFieldInstances().indexOf(labelInstance));
            String displayName;
            displayName = labelInstance.getName();
            if (column instanceof Measure && !column.isRowCount()) {
                String testColumn = ((Measure) column).getAggregation() + "(" + column.getColumn() + ")";
                if (testColumn.equals(displayName)) {
                    displayName = column.getColumn();
                }
            }
            column.setDisplayName(displayName);
        });
    }
    
    private static Field createTableField(VisualElement visualElement, ContractInstance contractInstance, Widget widget, FieldInstance fieldInstance) {

        Field tableField = new Field();
        setFieldProps(widget, tableField, contractInstance, fieldInstance);
        if (tableField.isNumeric()) {
            Measure measure = new Measure(tableField);
            if (!measure.isRowCount()) {
                if(AGGREGATE_STRINGS.contains(fieldInstance.getType())){
                    measure.setAggregation(AGGREGATION.valueOf(fieldInstance.getType()));
                }else{
                    measure.setAggregation(AGGREGATION.NONE);
                }                
            }
            setDisplayName(widget, measure, contractInstance, fieldInstance);

            return measure;
        } else {
            Attribute attribute = new Attribute(tableField);

            setDisplayName(widget, attribute, contractInstance, fieldInstance);

            return attribute;
        }
    }    

    private static void setDisplayName(Widget widget, Field field, ContractInstance contractInstance, FieldInstance fieldInstance) {
        field.setDisplayName(fieldInstance.getName());        
    }

    private static void setFieldProps(Widget widget, Field field, ContractInstance contractInstance, FieldInstance fieldInstance) {
        if (fieldInstance.getName() == null && Constants.COUNT.equals(fieldInstance.getType())) {
            field.setColumn(Constants.ROW_COUNT);
            field.setRowCount(true);
            field.setDataType(Constants.UNSIGNED);
            return;
        }
        
        //field passed in has been resolved if it's a field reference
        field.setColumn(fieldInstance.getName());        
        field.setDataType(fieldInstance.getTypeBase());
    }

    private static void setSortFieldProps(Widget widget, SortField field,
            ContractInstance contractInstance, FieldInstance fieldInstance) {
        
        String fieldLabel = fieldInstance.getFieldLabel();
        for (String aggreStr : AGGREGATE_STRINGS) {
            if (fieldLabel.endsWith(aggreStr)) {
                field.setAggregation(AGGREGATION.valueOf(aggreStr));
            }
        }
        LOGGER.debug("sort column aggregate-->{}", field.getAggregation());
        
        //field passed in has been resolved if it's a field reference
        if (fieldInstance.getName() == null && Constants.COUNT.equals(fieldInstance.getType())) {
            field.setRowCount(true);
            field.setColumn(Constants.ROW_COUNT);
            field.setDataType(fieldInstance.getTypeBase());
        }else{
            field.setColumn(fieldInstance.getName()); 
            field.setDataType(fieldInstance.getTypeBase());
        }
        
        if (fieldInstance.getFieldLabel().startsWith("-")) {
            field.setAscending(false);
        } else {
            field.setAscending(true);
        }
    }

    /**
     * Creates Widget object from DUD file's visual element.Also sets the
     * properties of the widget
     * 
     * @throws HPCCException
     * @throws DatabaseException 
     */
    // TODO: setting datatype of fields need to be done by accessing actual
    // data(Hpcc/Static) throughout the method.

    public static List<Widget> extractVisualElements(
            ContractInstance contractInstance, Map<String, QuerySchema> queries,
            boolean isStaticData,DatasourceStatus status) throws HipieException, HPCCException, DatabaseException {
        Contract contract = contractInstance.getContract();

        LOGGER.debug("Extracting contract - {}", contract.getCanonicalName());
        
        List<Element> visualElements = contract.getVisualElements().iterator().next().getChildElements();
        
        LOGGER.debug("Visualelements - {}", visualElements);
        
        List<Element> chartVisualElements = visualElements.stream().filter(visualElement -> !visualElement.getType().equals(VisualElement.FORM))
                .collect(Collectors.toList());

        List<Element> formVisualElements = visualElements.stream().filter(visualElement -> visualElement.getType().equals(VisualElement.FORM))
                .collect(Collectors.toList());

        List<Widget> extractedwidgets = new ArrayList<Widget>();
        Map<Widget, VisualElement> widgetsVisualElement = new HashMap<Widget, VisualElement>();

        // Holds the datasource CIs which is having invalid filename/Query
        Set<ContractInstance> invalidDatasource = new HashSet<ContractInstance>();
        
        LogicalFileService logicalFileService = ((LogicalFileService)SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE));
        List<String> blacklist = logicalFileService.getBlacklistedThorFiles();
        
        for (Element element : chartVisualElements) {
            VisualElement visualElement = (VisualElement) element;

            ChartConfiguration chartConfig = DashboardUtil.getChartConfig(visualElement);
            Widget widget = null;

            if (chartConfig.getType().equals(ChartType.PIE) || chartConfig.getType().equals(ChartType.DONUT) || chartConfig.getType().equals(ChartType.PYRAMID)) {
                widget = new Pie();

                widget.setDatasourceType(identifyDatasourceType(isStaticData, visualElement));
                if (widget.canUseNativeName()) {
                    extractQuerydata(widget, visualElement, queries);
                }
                ((Pie) widget).setWeight(createMeasures(visualElement.getOption(VisualElement.WEIGHT),visualElement, contractInstance, widget).get(0));
                ((Pie) widget).setLabel(createAttributes(visualElement.getOption(VisualElement.LABEL), visualElement, contractInstance, widget).get(0));

            } else if (chartConfig.getType().equals(ChartType.BAR) || chartConfig.getType().equals(ChartType.COLUMN) || chartConfig.getType().equals(ChartType.STACKCOLUMN)) {
                widget = new XYChart();

                widget.setDatasourceType(identifyDatasourceType(isStaticData, visualElement));
                if (widget.canUseNativeName()) {
                    extractQuerydata(widget, visualElement, queries);
                }
                ElementOption weight = visualElement.getOption(VisualElement.WEIGHT);
                // Chart has single measure
                setMeasureAndAttribute(contractInstance, visualElement, widget, weight);

                if (visualElement.getOption(VisualElement.SORT) != null) {
                    ((XYChart) widget).setSortFields(createSortFields(visualElement.getOption(VisualElement.SORT), contractInstance, widget, visualElement));
                }

                if (visualElement.getOption(VisualElement.FIRST) != null) {
                    ((XYChart) widget).setRecordLimit(Integer.parseInt(visualElement.getOption(VisualElement.FIRST).getParam(0)));
                }

            } else if (ChartType.LINE.equals(chartConfig.getType())) {
                widget = new XYChart();

                widget.setDatasourceType(identifyDatasourceType(isStaticData, visualElement));
                if (widget.canUseNativeName()) {
                    extractQuerydata(widget, visualElement, queries);
                }
                ((XYChart) widget).setMeasures(createMeasures(visualElement.getOption(VisualElement.Y), visualElement, contractInstance, widget));
                ((XYChart) widget).setAttributes(createAttributes(visualElement.getOption(VisualElement.X), visualElement, contractInstance, widget));

                if (visualElement.getOption(VisualElement.SORT) != null) {
                    ((XYChart) widget).setSortFields(createSortFields(visualElement.getOption(VisualElement.SORT), contractInstance, widget, visualElement));
                }

                if (visualElement.getOption(VisualElement.FIRST) != null) {
                    ((XYChart) widget).setRecordLimit(Integer.parseInt(visualElement.getOption(VisualElement.FIRST).getParam(0)));
                }

            } else if (chartConfig.getType().equals(ChartType.US_MAP)) {
                widget = new USMap();

                widget.setDatasourceType(identifyDatasourceType(isStaticData, visualElement));
                if (widget.canUseNativeName()) {
                    extractQuerydata(widget, visualElement, queries);
                }

                ((USMap) widget).setMeasure(createMeasures(visualElement.getOption(VisualElement.WEIGHT), visualElement, contractInstance, widget).get(0));

                if (visualElement.getOption(VisualElement.STATE) != null) {
                    ((USMap) widget).setState(createAttributes(visualElement.getOption(VisualElement.STATE), visualElement,contractInstance, widget).get(0));
                    ((USMap) widget).setCounty(false);
                } else {
                    ((USMap) widget).setState(createAttributes(visualElement.getOption(VisualElement.COUNTY), visualElement, contractInstance, widget).get(0));
                    ((USMap) widget).setCounty(true);
                }

            } else if (chartConfig.getType().equals(ChartType.TABLE)) {
                widget = new Table();
                widget.setDatasourceType(identifyDatasourceType(isStaticData, visualElement));
                if (widget.canUseNativeName()) {
                    extractQuerydata(widget, visualElement, queries);
                }
                
                ((Table) widget).setTableColumns(createTableFields(visualElement, contractInstance, widget));

                if (visualElement.getOption(VisualElement.SORT) != null) {
                    ((Table) widget).setSortFields(createSortFields(visualElement.getOption(VisualElement.SORT), contractInstance, widget, visualElement));
                }
                if (visualElement.getOption(VisualElement.FIRST) != null) {
                    ((Table) widget).setRecordLimit(Integer.parseInt(visualElement.getOption(VisualElement.FIRST).getParam(0)));
                }
            }

            widget.setName(visualElement.getName());
            widget.setChartConfiguration(chartConfig);
            widget.setTitle(visualElement.getOption(VisualElement.TITLE).getParams().get(0).getName());
         
            // creates datasource for the widget.creats single datasource for a
            // logical file, though the logical file is being used in multiple
            // widgets
            String inputName = visualElement.getBasis().getBase();
            ContractInstance precursor = contractInstance.getPrecursors().get(inputName);

            if (precursor != null && precursor.getProperty(Constants.LOGICAL_FILENAME) != null && 
                    logicalFileService.isFileInBlacklist(
                            precursor.getProperty(Constants.LOGICAL_FILENAME).replace("~", ""), blacklist))
            {
              status = DatasourceStatus.INVALID;                
            }
            
            // DatasourceStatus is not null for RAMPS Perspective
            if(status != null ){
                //Handles only UsesDataset CIs,Other Plugins(which has outputs to draw chart) not needed to handle
                if(DatasourceStatus.INVALID.equals(status) && HIPIEUtil.isDataSourcePlugin(precursor)) {
                    invalidDatasource.add(precursor);
                }                
            }else{
                //Dashboard perspective needs to be handled
            }
            
            if (!widget.canUseNativeName() && precursor != null) {
                PluginOutput datasource = createPluginOutput(contractInstance, widget, inputName, precursor);
                Optional<Widget> option = extractedwidgets.stream().filter(extractedWidget -> extractedWidget.getDatasource().equals(datasource)).findAny();
                if (option.isPresent()) {
                    widget.setDatasource(option.get().getDatasource());
                } else {
                    widget.setDatasource(datasource);
                }
            }
            
            //setting fileNotExists to true if the logical file is not present/invalid
            if(!invalidDatasource.isEmpty() && invalidDatasource.contains(widget.getDatasource().getContractInstance())){
                widget.getDatasource().setFileNotExists(true);
            }

            extractedwidgets.add(widget);
            widgetsVisualElement.put(widget, visualElement);
        }

        extractFilters(formVisualElements, extractedwidgets, contractInstance);

        widgetsVisualElement.entrySet().stream().forEach(entry -> createSource(entry.getValue(), entry.getKey(), contractInstance, extractedwidgets));
        
        // TODO: Remove this logic, Once Interactivity object structure is
        // migrated to new one
        extractedwidgets.forEach(currentWidget -> {
            LOGGER.debug("current widget ->{}", currentWidget);
            // Widgets which has filter Form,will not have Interactivity
            if (currentWidget.getFilters() == null && currentWidget.getInteractions() != null
                    && currentWidget.getInteractions().iterator().next().getTargets().get(0).getField() == null) {
                // These are old structured interactivities
                createTargets(contractInstance, chartVisualElements, extractedwidgets, currentWidget);

            }
        });
        
        //Handling GLobal filter Widget
        Widget globalFilterWidget = extractGLobalFilter(formVisualElements, extractedwidgets, contractInstance);
        if(globalFilterWidget != null){
            addGlobalFilterToAllwidgets(globalFilterWidget, extractedwidgets);
            extractedwidgets.add(globalFilterWidget);
        }

        LOGGER.debug("Extracted Widgets. \nText - {}\nWidgets - {}", visualElements, extractedwidgets);

        return extractedwidgets;
    }

    private static void addGlobalFilterToAllwidgets(Widget globalFilterWidget, List<Widget> extractedwidgets) {
        for(Filter globalFilter : globalFilterWidget.getFilters()){
            extractedwidgets.stream().forEach(widget ->{
                List<Field> widgetFileFields = widget.getDatasourceFields();
                if(widget.getFilters() == null){
                    widget.setFilters(new ArrayList<Filter>());
                }
                //Add the global filter to widget, 
                //if the widget's logicalFile/query has the global filter column
                if(RampsUtil.isFieldPresent(widgetFileFields, globalFilter)){
                    widget.getFilters().add(globalFilter);
                }
               
            });
        }
        
    }

    private static void createTargets(ContractInstance contractInstance, List<Element> chartVisualElements, List<Widget> extractedwidgets, Widget currentWidget) {
        currentWidget.getInteractions().forEach(interaction -> {
            interaction.getTargets().forEach(target -> {
                String targetWidgetName = target.getWidget().getName();
                VisualElement targetWidgetVisualElement = (VisualElement) chartVisualElements.stream()
                        .filter(visualElement -> visualElement.getName().equals(targetWidgetName)).findAny().get();

                Field targetField = createTargetFields(targetWidgetVisualElement, extractedwidgets, currentWidget, contractInstance);
                // Sets the target field
                target.setField(targetField);
                // Sets the target field to the target widget's
                // InteractionTarget
                target.getWidget().getInteractionTargets().forEach(targett -> {
                    if (targett.getWidget().equals(currentWidget)) {
                        targett.setField(targetField);
                    }
                    LOGGER.debug("target field ->{}", targett);
                });
            });
            LOGGER.debug("interaction ->{}", interaction);
        });
    }

    // TODO: Remove this method, Once Interactivity object structure is migrated
    // to new one
    private static Field createTargetFields(VisualElement targetWidgetVisualElement, List<Widget> extractedwidgets, Widget currentWidget,
            ContractInstance contractInstance) {
        Field targetField = null;
        ElementOption filterOption = targetWidgetVisualElement.getOption(VisualElement.FILTER);
        // Filters the dashboard widgets to get the widgets which has the
        // current widget as Target
        List<Widget> widgetsHasTarget = extractedwidgets.stream()
                .filter(eachWidget -> eachWidget.getInteractions() != null && eachWidget.getInteractions()
                        .filter(interaction -> interaction.getTargets().stream()
                                .filter(target -> target.getWidget().getName().equals(targetWidgetVisualElement.getName())).count() > 0)
                        .count() > 0)
                .collect(Collectors.toList());

        if (filterOption != null) {
            // based on the widget order taking the filter field
            FieldInstance fieldinstance = filterOption.getParams().get(widgetsHasTarget.indexOf(currentWidget));
            fieldinstance = targetWidgetVisualElement.resolveFieldReference(fieldinstance, contractInstance);
            targetField = new Field(fieldinstance.getName(),fieldinstance.getTypeBase());

        }
        return targetField;
    }

    /**
     * Parses the FORM widget " FORM Filterpie: TITLE(" Filtertable1
     * "),SELECTS(*->pie) STRING Field1_Pie:LABEL("productline") END " and
     * creates the filters of the widget
     */
    private static void extractFilters(List<Element> formVisualElements, List<Widget> extractedwidgets, ContractInstance contractInstance) {
        
        formVisualElements.forEach(formElement -> {
            String formName = formElement.getName();
            String widgetName = StringUtils.removeEnd(formName, VisualElement.FORM);
            Optional<Widget> option = extractedwidgets.stream().filter(widget -> widget.getName().equals(widgetName)).findAny();
            if(option.isPresent()){
                Widget filteredWidget = option.get();
                filteredWidget.setFilters(createFilters(formElement, contractInstance, false));
            }
        });

    }

    private static Widget extractGLobalFilter(List<Element> formVisualElements,
            List<Widget> extractedwidgets, ContractInstance contractInstance) {
        
        Widget globalFilterWid = null;
        Element globalFormElement = null;
        Contract contract = contractInstance.getContract();
        String dashboardName = StringUtils.removeEndIgnoreCase(contract.getName(), Constants.DASHBOARD);
        Optional<Element> option = formVisualElements.stream().filter(
                formElement -> dashboardName.equals(StringUtils.removeEnd(formElement.getName(), VisualElement.FORM))).findFirst();
        
        if(option.isPresent()){
            globalFormElement = option.get();
          //create global filter Widget along with filters
            globalFilterWid = new GlobalFilter();
            globalFilterWid.setName(dashboardName);
            globalFilterWid.setTitle(globalFormElement.getOption(VisualElement.TITLE).getParams().get(0).getName());
            globalFilterWid.setChartConfiguration(Dashboard.CHARTS_CONFIGURATION.get(ChartType.GLOBAL_FILTER));
            globalFilterWid.setFilters(createFilters(globalFormElement, contractInstance, true));
        }
        
        LOGGER.debug("Extracted global widget - {}", globalFilterWid);
        
        return globalFilterWid;
    }
    
    private static List<Filter> createFilters(Element formElement, ContractInstance contractInstance, boolean isGlobal) {
        List<Filter> filters = new ArrayList<Filter>();
        formElement.getChildElements().forEach(childElement -> {
            
            FieldInstance fieldInstance = childElement.getOption(Element.LABEL).getParams().get(0);
            fieldInstance = formElement
                    .resolveFieldReference(fieldInstance, contractInstance);           
            Filter filter = new Filter(new Field(fieldInstance.getName(), fieldInstance.getTypeBase()));
            filter.setGlobal(isGlobal);
            String value = childElement.getOption(Element.DEFAULT).getParams().get(0).getName();
            filter.setValue(value);
            filter.setNewValue(value);
            filters.add(filter);
        });
        return filters;
    }

    private static DATASOURCE identifyDatasourceType(boolean isStaticData, VisualElement visualElement) {
        DATASOURCE datasourceType = null;
        String outputDUDName = visualElement.getBasis().getName();
        if (isStaticData) {
            datasourceType = DATASOURCE.STATIC_DATA;
        } else if (outputDUDName.startsWith(Dashboard.QUERY_OUTPUT)) {
            datasourceType = DATASOURCE.QUERY;
        } else {
            datasourceType = DATASOURCE.FILE;
        }
        return datasourceType;
    }

    private static void setMeasureAndAttribute(ContractInstance contractInstance, VisualElement visualElement, Widget widget, ElementOption weight) {
        if (weight != null) {
            ((XYChart) widget).setMeasures(createMeasures(visualElement.getOption(VisualElement.WEIGHT), visualElement, contractInstance, widget));
            ((XYChart) widget).setAttributes(createAttributes(visualElement.getOption(VisualElement.LABEL), visualElement, contractInstance, widget));
        } else {
            // Chart has multiple measures
            ((XYChart) widget).setMeasures(createMeasures(visualElement.getOption(VisualElement.Y), visualElement, contractInstance, widget));
            ((XYChart) widget).setAttributes(createAttributes(visualElement.getOption(VisualElement.X), visualElement, contractInstance, widget));
        }
    }

    private static PluginOutput createPluginOutput(ContractInstance contractInstance, Widget widget, String inputName, ContractInstance precursor) throws HipieException {
        PluginOutput datasource = null;
        try {
            datasource = new PluginOutput(precursor, precursor.getContract().getElement(contractInstance.getRefPropertyName(inputName)));
        } catch (Exception e) {
            throw new HipieException("Unable to create datasource", e);
        }

        return datasource;
    }

    private static void extractQuerydata(Widget widget, VisualElement visualElement, Map<String, QuerySchema> queries) {
        String outputDUDName = visualElement.getBasis().getName();
        OutputSchema outputSchema = null;
        String query = null;
        for (Entry<String, QuerySchema> entry : queries.entrySet()) {
            outputSchema = getOutputSchema(entry.getValue(), outputDUDName, widget.isDatabomb());
            if (outputSchema != null) {
                query = entry.getKey();
                break;
            }
        }
        widget.setQueryOutput(outputSchema);
        widget.setQueryName(query);
    }

    private static OutputSchema getOutputSchema(QuerySchema querySchema, String outputDUDName, boolean isStaticData) {
        Optional<OutputSchema> outputSchemaOption;
        OutputSchema outputSchema = null;
        if (isStaticData) {
            outputSchemaOption = querySchema.getOutputs().stream().filter(output -> output.getName().equals(outputDUDName)).findAny();
        } else {
            outputSchemaOption = querySchema.getOutputs().stream().filter(output -> output.getDudName().equals(outputDUDName)).findAny();
        }
        if (outputSchemaOption.isPresent()) {
            outputSchema = outputSchemaOption.get();
        }

        return outputSchema;
    }

    /**
     * Parses the 'SELECTS(INPUTS.Field2_line->targetTable.field,MERGE)' part of
     * the visual element and creates Source object for the Widget
     */
    private static void createSource(VisualElement visualElement, Widget sourceWidget, ContractInstance contractInstance, List<Widget> extractedwidgets) {

        SelectElementOption selectOptions = (SelectElementOption) visualElement.getOption(VisualElement.SELECTS);
        SelectElementOption selectOption;
        if (selectOptions != null) {
            for (int i = 0; i < selectOptions.count(); i++) {
                selectOption = (SelectElementOption) selectOptions.get(i);

                ACTION action = Dashboard.ACTION.getAction(selectOption.getEventType());

                // Each source has only one source field
                FieldInstance fieldInstance = selectOption.getParams().get(0);
                fieldInstance = visualElement.resolveFieldReference(fieldInstance, contractInstance);
                Field sourceField = new Field(fieldInstance.getName(),fieldInstance.getTypeBase());
                
                Interaction source = new Interaction(action, sourceField, sourceWidget);
                List<InteractionTarget> targets = new ArrayList<>();
                selectOption.getDrives().forEach(drive -> targets.add(extractTarget(drive, extractedwidgets, sourceWidget)));

                source.setTargets(targets);
                //AN interaction without Target is invalid, so removing it.When the 'SELECTS(INPUTS.Field2_line->targetTable.field' has 
               // target chart name 'targetTable', which is not existing,the targets will be empty.
                if(targets.isEmpty()){
                    source.delete();
                }
                
            }
        }
    }

    /**
     * Creates Target object from 'targetTable.field' part of SELECT part of
     * visual element for Source object
     */
    private static InteractionTarget extractTarget(DriveFieldInstance driveFieldInstance, List<Widget> extractedwidgets, Widget sourceWidget) {

        InteractionTarget target = null;
        List<String> nameList = Arrays.asList(StringUtils.split(driveFieldInstance.getName(), DOT));
        // TODO:Remove this check , once interactivity object structure is
        // migrated to new structure completely
        if (nameList.size() == 2) {
            // TODO:This condition block of code shouldn't be removed
            String targetFieldName = nameList.get(1);
            String taregtWidgetName = nameList.get(0);
            Optional<Widget> option = extractedwidgets.stream().filter(widget -> widget.getName().equals(taregtWidgetName)).findAny();
            Widget targetWidget = null;
            if(option.isPresent()){
                targetWidget = option.get();
            }

            Field targetField = new Field(targetFieldName, null);
            target = new InteractionTarget(targetField, targetWidget, sourceWidget);
        } else {
            // TODO:Remove this part, once interactivity object structure is
            // migrated to new structure completely
            String taregtWidgetName = driveFieldInstance.getName();
            Widget targetWidget = extractedwidgets.stream().filter(widget -> widget.getName().equals(taregtWidgetName)).findAny().get();

            target = new InteractionTarget(null, targetWidget, sourceWidget);
        }
        return target;
    }

    public PluginOutput getDatasource() {
        return datasource;
    }

    public void setDatasource(PluginOutput datasource) {
        this.datasource = datasource;
    }

    public void addInteractivityFilter(String chart, SelectElementOption selectOption, Dashboard.ACTION action) {
        selectOption.setEventType(action.getEventType());
        DriveFieldInstance drv = new DriveFieldInstance(null, chart);
        if (selectOption.getParamCount() > 0) {
            drv.setDriverField(selectOption.getParams().get(0));
        }
        selectOption.getDrives().add(drv);
    }

    /**
     * creates element option 'SORT(Field1_chartName)' to append to the visual
     * element
     * @throws HipieException 
     */
    public void generateSortFieldOption(VisualElement visualElement) throws HipieException {
        Iterator<SortField> sortFieldIterator = getSortFields().iterator();
        if (!sortFieldIterator.hasNext()) {
            return;
        }
        SortField firstField = sortFieldIterator.next();
        sortFieldAddOption(visualElement, firstField);
        
        sortFieldIterator.forEachRemaining(sortField -> {
            String loopSortAggregate = null;
            if (sortField.isNotNone()) {
                loopSortAggregate = sortField.getAggregation().name();
                FieldInstance fi = new FieldInstance(loopSortAggregate, sortField.getDudName());
                if (!sortField.isAscending()) {
                    fi.setSortOrder("-");
                }
                visualElement.getOption(VisualElement.SORT).addParam(fi);
            } else {
                visualElement.getOption(VisualElement.SORT).addParam(new FieldInstance(sortField.isAscending() ? null : "-", sortField.getDudName()));
            }

        });
    }

    private void sortFieldAddOption(VisualElement visualElement, SortField firstField) throws HipieException {
        try {
            String sortAggregate;
            if (firstField.isNotNone()) {
                sortAggregate = firstField.getAggregation().name();
                FieldInstance fi = new FieldInstance(sortAggregate, firstField.getDudName());
                if (!firstField.isAscending()) {
                    fi.setSortOrder("-");
                }
                visualElement.addOption(VisualElement.SORT, fi);
            } else {
                visualElement.addOption(VisualElement.SORT, new FieldInstance(firstField.isAscending() ? null : "-", firstField.getDudName()));
            }
        } catch (Exception e) {
            throw new HipieException(e);
        }
    }

    /**
     * creates element option 'FILTER(Field1_chartName)' to append to the visual
     * element
     * @throws HipieException 
     */
    protected void generateFilterOption(VisualElement visualElement) throws HipieException {
        Iterator<Filter> fileterIterator = getFilters().iterator();
        if (!fileterIterator.hasNext()) {
            return;
        }
        Filter firstFilter = fileterIterator.next();
        try {
            visualElement.addOption(VisualElement.FILTER, new FieldInstance(null, firstFilter.getDudName()));
        } catch (Exception e) {
            throw new HipieException(e);
        }
        fileterIterator.forEachRemaining(filter ->
        // Adds filter column
        visualElement.getOption(VisualElement.FILTER).addParam(new FieldInstance(null, filter.getDudName())));
    }

    public void generateLimitRecords(VisualElement visualElement) throws HipieException {
        try {
            visualElement.addOption(VisualElement.FIRST, new FieldInstance(null, String.valueOf(getRecordLimit())));
        } catch (Exception e) {
            throw new HipieException(e);
        }
    }

    public abstract List<Field> getInteractivityFields();

    public OutputSchema getQueryOutput() {
        return queryOutput;
    }

    public void setQueryOutput(OutputSchema queryOutput) {
        this.queryOutput = queryOutput;
    }

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((chartConfiguration == null) ? 0 : chartConfiguration.hashCode());
        result = prime * result + ((sortFields == null) ? 0 : sortFields.hashCode());
        result = prime * result + ((recordLimit == 0) ? 0 : recordLimit);
        result = prime * result + ((filters == null) ? 0 : filters.hashCode());
        result = prime * result + (isQueryBased() ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((queryName == null) ? 0 : queryName.hashCode());
        result = prime * result + ((queryOutput == null) ? 0 : queryOutput.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Widget other = (Widget) obj;
        if (chartConfiguration == null) {
            if (other.chartConfiguration != null) {
                return false;
            }
        } else if (!chartConfiguration.equals(other.chartConfiguration)) {
            return false;
        }
        if (recordLimit != other.recordLimit) {
            return false;
        }
        if (sortFields == null) {
            if (other.sortFields != null) {
                return false;
            }
        } else if (!sortFields.equals(other.sortFields)) {
            return false;
        }
        if (filters == null) {
            if (other.filters != null) {
                return false;
            }
        } else if (!filters.equals(other.filters)) {
            return false;
        }
        if (datasourceType != other.datasourceType) {
            return false;
        }
        if (datasource != other.getDatasource()) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (queryName == null) {
            if (other.queryName != null) {
                return false;
            }
        } else if (!queryName.equals(other.queryName)) {
            return false;
        }
        if (queryOutput == null) {
            if (other.queryOutput != null) {
                return false;
            }
        } else if (!queryOutput.equals(other.queryOutput)) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        return true;
    }

    @Override
    public Widget clone() throws CloneNotSupportedException {
        Widget widget = (Widget) super.clone();
        if (widget.getSortFields() != null) {
            List<SortField> clonedSortFields = new ArrayList<SortField>();
            for (SortField sortField : widget.getSortFields()) {
                clonedSortFields.add(sortField.clone());
            }
            widget.setSortFields(clonedSortFields);
        }
        if (widget.getFilters() != null) {
            List<Filter> clonedFilters = new ArrayList<Filter>();
            for (Filter filter : widget.getFilters()) {
                clonedFilters.add(filter.clone());
            }
            widget.setFilters(clonedFilters);
        }
        return widget;
    }

    public boolean hasSortField(Field field) {
        Optional<SortField> findSort = sortFields.stream().filter(sort -> sort.getColumn().equals(field.getColumn())).findAny();
        return findSort.isPresent();
    }

    public int getRecordLimit() {
        return recordLimit;
    }

    public void setRecordLimit(int recordLimit) {
        this.recordLimit = recordLimit;
    }

    public void setDatasourceType(DATASOURCE type) {
        this.datasourceType = type;
    }

    public DATASOURCE getDatasourceType() {
        return datasourceType;
    }

    public boolean isQueryBased() {
        return datasourceType == DATASOURCE.QUERY;
    }

    /**
     * @return True if widget dstasource is Querybased or Static Data
     */
    public boolean canUseNativeName() {
        return datasourceType == DATASOURCE.QUERY || datasourceType == DATASOURCE.STATIC_DATA;
    }

    public boolean isDatabomb() {
        return datasourceType == DATASOURCE.STATIC_DATA;
    }

    public boolean isFileBased() {
        return datasourceType == DATASOURCE.FILE;
    }

    public boolean hasDatasource() {
        boolean queryBased = isQueryBased() && !StringUtils.isEmpty(queryName);
        return (isFileBased() && datasource != null && !datasource.isFileNotExists()) || queryBased;
    }

    public VisualElement generateFormElement() throws HipieException {
        // Check foe filter availability
        if (!hasLocalFilters()) {
            return null;
        }

        VisualElement visualElement = new VisualElement();
        try {
            DashboardUtil.updateBasicData(visualElement,this);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        
        visualElement.addOption(DashboardUtil.generateFilterFormSelectOption(new ArrayList<String>(Arrays.asList(name))));        
        DashboardUtil.addFormFilterField(visualElement, getLocalFilters());

        return visualElement;
    }

    /**
     * Checks each Field of the logical file widget to see if the Field already exists in 
     * the contract object. If a field does exist then set the dud name of current Field to 
     * dud name of already existing field. If it doesn't exist then generate a new dud name 
     * and add it to the field. 
     * 
     * @param contract - The contract that the Widget is going to be added to.
     * @param datasourceName 
     */
    public void updateDUDFieldNames(Contract contract, String datasourceName) {
        String existingFieldName = null;
        for (Field chartField : getInputElementFields()) {
        	existingFieldName = getEqualFieldFromComposition(contract.getInputElements(), datasourceName.concat("_").concat(chartField.getColumn()));
            if (existingFieldName == null) { // new field.          	
                chartField.setDudName(datasourceName.concat("_").concat(chartField.getColumn()));
            } else { // field already exists in composition
                chartField.setDudName(existingFieldName);
            }
        }
    }
    
    /**
     * Checks each Field of the query widget to see if the Field already exists in the 
     * contract object. If a field does exist then set the dud name of current Field to 
     * dud name of already existing field. If it doesn't exist then generate a new dud 
     * name and add it to the field. 
     * 
     * @param contract - The contract that the Widget is going to be added to.
     * @param datasourceName 
     */
    public void updateDUDFieldNames(Contract contract) {
        String existingFieldName = null;
        for (Field chartField : getInputElementFields()) {
            existingFieldName = getEqualFieldFromComposition(contract.getInputElements(), chartField.getColumn());
            if (existingFieldName == null) { // new field.              
                chartField.setDudName(chartField.getColumn());
            } else { // field already exists in composition
                chartField.setDudName(existingFieldName);
            }
        }
    }
    
    private String getEqualFieldFromComposition(List<Element> inputElements, String chartField) {
		for (Element element: inputElements) {
			for (Element child: element.getChildElements()) {
			    if (child.getName().equals(chartField)) {
					return child.getName();
				}
			}
		}
    	return null;
    }

    public List<Field> getUniqueFields(List<Field> fields) {
        List<Field> uniqueFields = new ArrayList<Field>();

        fields.forEach(field -> {
            Optional<Field> option = uniqueFields.stream().filter(uniqueField -> uniqueField.getColumn().equals(field.getColumn())).findAny();
            if (!option.isPresent()) {
                uniqueFields.add(field);
            }
        });
        return uniqueFields;
    }

    public List<Field> getDatasourceFields() {
        List<Field> fields = null;
        try {
            if(canUseNativeName()){
                fields = getQueryOutput().getFields();
            } else {
                fields = RampsUtil.getFileFields(getDatasource());
            }
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return fields;
    }

    protected void addMeasureToRecordInstance(RecordInstance recordInstance, Measure measure) {
        
        FieldInstance measureField = new FieldInstance(null, measure.getDudName());
        //As 'rowcount' allowed as sort field, check for rowcount duplication 
        if (measure.isRowCount()) {
            if(!recordInstance.containsField(measureField)){
                recordInstance.add(new FieldInstance(measure.getDudName(), null));   
            }
        } else {
            if (!Measure.AGGREGATION.NONE.equals(measure.getAggregation())) {
                recordInstance.add(new FieldInstance(measure.getAggregationString(), measure.getDudName()));
            } else if (!recordInstance.containsField(measureField)) {
                recordInstance.add(new FieldInstance(null, measure.getDudName()));
            }
        }
    }
    
    /**
     * Adds the filter section 'FILTER(Field1_chartname)' for interactivity target
     * filters into visual element
     * @throws HipieException 
     */
    protected void generateInteractivityFilterOption(VisualElement visualElement) throws HipieException {

        List<Field> duplicatedFields = getTargetFields(getInteractionTargets());
        Iterator<Field> targetIterator = getUniqueFields(duplicatedFields).iterator();
        
        try {
            if (targetIterator.hasNext()) {
                if (visualElement.getOption(VisualElement.FILTER) == null) {
                    visualElement.addOption(VisualElement.FILTER, new FieldInstance(null, targetIterator.next().getDudName()));
                } else {
                    Field targetField =targetIterator.next();
                    FieldInstance fieldInstance = new FieldInstance(null, targetField.getDudName());
                    if(!visualElement.getOption(VisualElement.FILTER).getParams().contains(fieldInstance)){
                        visualElement.getOption(VisualElement.FILTER).addParam(fieldInstance);
                    }      
                }
                targetIterator.forEachRemaining(targetField -> {
                    FieldInstance fieldInstance = new FieldInstance(null, targetField.getDudName());
                    if(!visualElement.getOption(VisualElement.FILTER).getParams().contains(fieldInstance)){
                        visualElement.getOption(VisualElement.FILTER).addParam(fieldInstance);
                    }
                   
                });
            }
        } catch (Exception e) {
            throw new HipieException(e);
        }
    }

    public List<Field> getTargetFields(Stream<InteractionTarget> interactionTargets) {

        List<Field> duplicatedFields = new ArrayList<Field>();
        interactionTargets.forEach(target -> duplicatedFields.add(target.getField()));

        return duplicatedFields;
    }

    /**
     * Adds the Select part
     * 'SELECTS(INPUTS.Field2_line->targetTable.field,MERGE)' into visual
     * element for interactivity
     */
    protected void generateInteractivitySelectOption(final VisualElement visualElement) {
        getInteractions().forEach(source -> {
            List<FieldInstance> driven = new ArrayList<FieldInstance>();

            SelectElementOption selectOption = new SelectElementOption(source.getAction().getEventType(),
                    !this.canUseNativeName() ? "INPUTS." + source.getField().getDudName() : source.getField().getDudName(), (ArrayList<FieldInstance>) driven);

            source.getTargets().forEach(target -> {

                StringBuilder builder = new StringBuilder();
                builder.append(target.getWidget().getName()).append(DOT).append(target.getField().getColumn());

                DriveFieldInstance df = new DriveFieldInstance(null, builder.toString());
                df.setDriverField(new FieldInstance(null, selectOption.getParam(0)));
                df.setMerge(true);
                selectOption.getDrives().add(df);
            });
            visualElement.addOption(selectOption);
        });
    }

    protected void addInteractivityTarget(InteractionTarget target) {
        if (interactionTargets == null) {
            interactionTargets = new ArrayList<InteractionTarget>();
        }
        interactionTargets.add(target);
    }

    protected void addInteractivitySource(Interaction source) {
        if (interactions == null) {
            interactions = new ArrayList<Interaction>();
        }
        interactions.add(source);
    }

    public List<Field> getAvailableFields() {
        List<Field> fields = null;
        if (!canUseNativeName()) {
            try {
                fields = RampsUtil.getFileFields(datasource);
            } catch (HipieException e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        } else {
            fields = queryOutput.getFields();
        }
        return fields;
    }

    public void removeTarget(InteractionTarget targetToRemove, Widget sourceWidget) {

        List<InteractionTarget> targetsToRemove = interactionTargets.stream()
                .filter(target -> target.getField().getColumn().equals(targetToRemove.getField().getColumn()) && target.getWidget().equals(sourceWidget))
                .collect(Collectors.toList());

        interactionTargets.removeAll(targetsToRemove);
    }

    public void removeSource(Interaction source) {
        interactions.remove(source);
    }
    
    public boolean isGlobalFilterWidget(){
        return ChartType.GLOBAL_FILTER.equals(this.getChartConfiguration().getType());
    }

}
