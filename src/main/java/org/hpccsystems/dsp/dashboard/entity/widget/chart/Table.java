package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Table extends Widget {
    
   
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Table.class);

    private List<Field> tableColumns;

    public void addColumn(Field column) {
        if (tableColumns == null) {
            tableColumns = new ArrayList<>();
        }
        tableColumns.add(column);
    }

    public void removeColumn(Field column) {
        tableColumns.remove(column);
    }

    public List<Field> getTableColumns() {
        return tableColumns;
    }

    public void setTableColumns(List<Field> tableColumns) {
        this.tableColumns = tableColumns;
    }

    @Override
    public boolean isConfigured() {
        boolean columnPresent = tableColumns != null && !tableColumns.isEmpty();
        if (columnPresent) {
            columnPresent = tableColumns.stream().filter(column -> !(column instanceof Measure)).findFirst().isPresent();
            if (columnPresent) {
                return columnPresent;
            } else {
                columnPresent = tableColumns.stream().filter(column -> (column instanceof Measure))
                        .anyMatch(column -> ((Measure) column).getAggregation() == AGGREGATION.NONE);
            }
        }
        return columnPresent;
    }

    @Override
    public VisualElement generateVisualElement() throws HipieException {

        VisualElement visualElement = new VisualElement();
        // TODO:Need to set chart type using Hipie's 'Element' class
        visualElement.setType(this.getChartConfiguration().getType().toString());
        visualElement.addCustomOption(new ElementOption(CHART_TYPE, new FieldInstance(null, this.getChartConfiguration().getHipieName())));
        visualElement.setName(DashboardUtil.removeSpaceSplChar(this.getName()));
        
        generateVisualOption(visualElement);
        
        // Setting Title for chart
        visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, this.getTitle())));
        return visualElement;
    }

    private void generateVisualOption(VisualElement visualElement) throws HipieException {

        RecordInstance ri = generateRecordInstance();
        visualElement.setBasisQualifier(ri);

        List<String> labellist = new ArrayList<String>();
        List<String> valueList = new ArrayList<String>();

        // Columns settings
        tableColumns.forEach(column -> renderingValueProperties(ri, labellist, valueList, column));

        Property labelProp = new Property();
        labelProp.addAll(labellist);
        Property valueProp = new Property();
        valueProp.addAll(valueList);
        
        LOGGER.debug("Label prop - {}", labelProp);
        
        ElementOption eo = new ElementOption(VisualElement.LABEL);
        for (String val:labelProp) {
            FieldInstance fi = new FieldInstance(null,val);
            eo.addParam(fi);
        }
        
        visualElement.addOption(eo);
        visualElement.addOption(new ElementOption(VisualElement.VALUE, valueProp));
        
        //Adding Filter
        if(getFilters() != null){
            generateFilterOption(visualElement);
        }
        
        //Handling Sort fields
        if(getSortFields() != null){
            generateSortFieldOption(visualElement); 
        }

        if (getInteractions() != null) {
            generateInteractivitySelectOption(visualElement);
        }
        if (getInteractionTargets() != null) {
            generateInteractivityFilterOption(visualElement);
        }

        if (getRecordLimit() > 0) {
            generateLimitRecords(visualElement);
        }

    }

    private void renderingValueProperties(RecordInstance ri, List<String> labellist, List<String> valueList, Field column) {
        LOGGER.debug("column is numeric -->{}", column.isNumeric());
        LOGGER.debug("column datatype -->{}", column.getDataType());
        LOGGER.debug("column list label -->{}", labellist);
        LOGGER.debug("column list value -->{}", valueList);
        if (column.isNumeric()) {
            // For Numeric field
            Measure measure = (Measure) column;
            
            if (column.isRowCount()) {
                labellist.add(Utility.AddQuotes(column.getDisplayName(), true));
                // creates Value as 'SUM(Column1_Tablewidget)'
                valueList.add(Constants.COUNT);
            } else {
                // creates label as 'SUM(buyprice)'
                if (measure.getColumn().equals(measure.getDisplayName())) {
                    labellist.add(!Measure.AGGREGATION.NONE.equals(measure.getAggregation()) ? measure.getAggregation().name() + "(" + measure.getDisplayName() + ")"
                            : measure.getDisplayName());
                } else {
                    labellist.add(measure.getDisplayName());
                }
                // creates Value as 'SUM(Column1_Tablewidget)'
                valueList.add(!Measure.AGGREGATION.NONE.equals(measure.getAggregation()) ? measure.getAggregation().name() + "("
                        + column.getDudName() + ")" : column.getDudName());
            }

        } else {
            // For String field
            // creates label as 'buyprice'
            Attribute attribute = new Attribute(column);
            labellist.add(Utility.AddQuotes(attribute.getDisplayName(), true));
            // creates value as Column2_Tablewidget
            valueList.add(column.getDudName());
        }
        LOGGER.debug("after column list label -->{}", labellist);
        LOGGER.debug("after column list value -->{}", valueList);
    }

    /**
     * generates Name as 'Column1_chartName[ie: getName()]'
     * 
     * @return String
     */
    public String createInputName(Field field) {
        String inputName;
        if (this.canUseNativeName()) {
            inputName = field.getColumn();
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Column").append(getTableColumns().indexOf(field) + 1).append("_").append(this.getName());
            inputName = builder.toString();
        }
        return inputName;
    }
    
    @Override
    public List<Field> getInteractivityFields() {
        return this.getTableColumns();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((tableColumns == null) ? 0 : tableColumns.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Table other = (Table) obj;
        if (tableColumns == null) {
            if (other.tableColumns != null) {
                return false;
            }
        } else if (!tableColumns.equals(other.tableColumns)) {
            return false;
        }
        return true;
    }

    @Override
    public Table clone() throws CloneNotSupportedException {
        Table table = (Table) super.clone();
        List<Field> fields = new ArrayList<Field>();
        for (Field field : table.getTableColumns()) {
            fields.add(field.clone());
        }
        table.setTableColumns(fields);
        return table;
    }

    @Override
    public void removeInvalidFields() {
        // TODO: remove this check once databomb is implemented
        if (isDatabomb()) {
            return;
        }
        List<Field> fields = getDatasourceFields();
        if(fields != null){
            tableColumns.removeAll(getInvalidFields(fields));
        }
    }
    private List<Field> getInvalidFields(List<Field> fields){
        List<Field> invalidFields = new ArrayList<Field>();
        for (Field field : tableColumns) {
            if(!RampsUtil.isFieldPresent(fields,field)){
                invalidFields.add(field);
            }
        }
        return  invalidFields;
    }
    @Override
    public boolean isValid(){
        List<Field> fields = getDatasourceFields();
        if(fields == null){
            return false;
        }
        return  hasDatasource() && getInvalidFields(fields).isEmpty();
    }

    @Override
    public List<Field> getInputElementFields() {
        List<Field> chartFields = new ArrayList<Field>();
        
        tableColumns.stream()
            .filter(field -> !field.isRowCount())
            .forEach(field -> chartFields.add(field));
        
        if(getFilters() != null){
            chartFields.addAll(getFilters());
        }
        if (getSortFields() != null ) {
            getSortFields().stream().filter(sortFields -> !sortFields.isRowCount()).forEach(sortFields -> chartFields.add(sortFields));
      }
        if(getInteractions() != null){
            getInteractions().forEach(source ->
                chartFields.add(source.getField())
            );
        }        
        if(getInteractionTargets() != null){
            getInteractionTargets().forEach(target ->
            chartFields.add(target.getField())
            );
        }
    
        return chartFields;
    }

    @Override
    protected RecordInstance generateRecordInstance() {
        RecordInstance recordInstance = new RecordInstance();
        
        getUniqueFields(getRecordInstanceFields())
            .forEach(chartField ->{ 
                FieldInstance filterField = new FieldInstance(null, chartField.getDudName());
                if(chartField instanceof Measure){
                    if(chartField.isRowCount()){
                        recordInstance.add(new FieldInstance(chartField.getDudName(), null));
                    }else{
                        if (!Measure.AGGREGATION.NONE.equals(((Measure)chartField).getAggregation())) {
                            recordInstance.add(new FieldInstance(((Measure)chartField).getAggregation().name(),chartField.getDudName()));
                        }else if(!recordInstance.containsField(filterField)){
                            recordInstance.add(new FieldInstance(null, chartField.getDudName()));
                        } 
                    }
                }else if (!recordInstance.containsField(filterField)) {
                    recordInstance.add(filterField);
                }
            });
        
        return recordInstance;
    }

    @Override
    public List<Field> getRecordInstanceFields() {
        List<Field> chartFields = new ArrayList<Field>();
        
        tableColumns.stream()
            .forEach(field -> chartFields.add(field));
        
        if(getFilters() != null){
            chartFields.addAll(getFilters());
        }        
        if(getInteractions() != null){
            getInteractions().forEach(source ->
                chartFields.add(source.getField())
            );
        }      
        
        if (getSortFields() != null) {
            chartFields.addAll(getSortFields()
                    .stream()
                    .filter(sort -> sort.getAggregation() == null || sort.getAggregation() == AGGREGATION.NONE)
                    .collect(Collectors.toList()));
        }
        if(getInteractionTargets() != null){
            chartFields.addAll(getTargetFields(getInteractionTargets()));
        }        
    
        return chartFields; 
    }
}
