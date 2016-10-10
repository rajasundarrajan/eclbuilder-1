package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.List;

import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pie extends Widget implements Cloneable {
  
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Pie.class);

    private Attribute label;
    private Measure weight;

    public Attribute getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((weight == null) ? 0 : weight.hashCode());
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
        Pie other = (Pie) obj;
        if (label == null) {
            if (other.label != null){
                return false;
            }
        } else if (!label.equals(other.label)){
            return false;
        }
        if (weight == null) {
            if (other.weight != null){
                return false;
            }
        } else if (!weight.equals(other.weight)) {
            return false;
        }
        return true;
    }

    public void setLabel(Attribute label) {
        this.label = label;
    }

    public Measure getWeight() {
        return weight;
    }

    public void setWeight(Measure weight) {
        this.weight = weight;
    }

    @Override
    public VisualElement generateVisualElement() throws HipieException {

        VisualElement visualElement = new VisualElement();
        visualElement.setType(this.getChartConfiguration().getType().toString());
        visualElement.addCustomOption(new ElementOption(CHART_TYPE, new FieldInstance(null, this.getChartConfiguration().getHipieName())));
        if (ChartType.DONUT == this.getChartConfiguration().getType()) {
            visualElement.addCustomOption(new ElementOption(HOLE_PERCENT, new FieldInstance(null, "55")));
        }
        visualElement.setName(DashboardUtil.removeSpaceSplChar(this.getName()));
        
        generateVisualOption(visualElement);

        // Setting Tittle for chart
        visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, this.getTitle())));
        return visualElement;
    }

    private void generateVisualOption(VisualElement visualElement) throws HipieException {
        String measure = null;

        RecordInstance ri = generateRecordInstance();
        visualElement.setBasisQualifier(ri);
        
        //Adding Attribute
        visualElement.addOption(new ElementOption(VisualElement.LABEL, new FieldInstance(null, label.getDudName())));

        //Adding Measure
        visualElement.addOption(new ElementOption(VisualElement.WEIGHT,weight.isRowCount() ?
                new FieldInstance(weight.getDudName(), null) : new FieldInstance(weight.getAggregationString(), weight.getDudName())));
        
        //Adding Filter
        if(getFilters() != null){
            generateFilterOption(visualElement);
        }
        
        //handling Interactivity 'SELECT(INPUTS.sourcefield->targetchart.field)' part of visual element
        if (getInteractions() != null) {
            generateInteractivitySelectOption(visualElement);
        }

        //handling Interactivity 'FILTER(FilterField1_chart,FilterField1_chart)' part of visual element
        if (getInteractionTargets() != null) {
            generateInteractivityFilterOption(visualElement);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Aggregation - {}, Measure {} \n Visual Element - {}", weight.getAggregationString(), measure, visualElement);
        }
    }
        

    @Override
    protected RecordInstance generateRecordInstance() {
        RecordInstance recordInstance = new RecordInstance();
        
        getUniqueFields(getRecordInstanceFields())            
            .forEach(field -> {
                FieldInstance filterField = new FieldInstance(null, field.getDudName());
                if (!recordInstance.containsField(filterField)) {
                    recordInstance.add(filterField);
                }
            });
        
        addMeasureToRecordInstance(recordInstance, weight);
        
        return recordInstance;
    }

    @Override
    public boolean isConfigured() {
        return (this.weight != null) && (this.label != null);
    }

    @Override
    public String toString() {
        return "Pie [label=" + label + ", weight=" + weight + ", getName()=" + getName() + ", toString()=" + super.toString() + "]";
    }

    @Override
    public List<Field> getInteractivityFields() {
        Field field = this.getLabel();
        List<Field> fields = new ArrayList<Field>();
        fields.add(field);
        return fields;
    }

    @Override
    public Pie clone() throws CloneNotSupportedException {
        Pie pie = (Pie) super.clone();
        pie.setWeight(this.weight.clone());
        return pie;

    }

    @Override
    public void removeInvalidFields() {
        // TODO: remove this check once databomb is implemented
        if (isDatabomb()) {
            return;
        }
        List<Field> fields = getDatasourceFields();
        if(fields != null){
            if (!RampsUtil.isFieldPresent(fields, weight)) {
                weight = null;
            }
            if (!RampsUtil.isFieldPresent(fields, label)) {
                label = null;
            }
        }
    }
    
    @Override
    public boolean isValid(){
        List<Field> fields = getDatasourceFields();
        if(fields == null){
            return false;
        }
        return hasDatasource() && RampsUtil.isFieldPresent(fields,weight) && RampsUtil.isFieldPresent(fields, label);
    }
    
    @Override
    public List<Field> getInputElementFields(){
        List<Field> chartFields = new ArrayList<Field>();
        chartFields.add(label);
        
        if(!weight.isRowCount()) {
            chartFields.add(weight);
        }
        
        if(getFilters() != null){
            chartFields.addAll(getFilters());
        }
        if(getSortFields() != null){
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
    public List<Field> getRecordInstanceFields() {
        List<Field> chartFields = new ArrayList<Field>();
        chartFields.add(label);
        
        if(getFilters() != null){
            chartFields.addAll(getFilters());
        }
        if(getSortFields() != null){
            chartFields.addAll(getSortFields());
        }
        if(getInteractions() != null){
            getInteractions().forEach(source ->
                chartFields.add(source.getField())
            );
        }   
        
        if(getInteractionTargets() != null){
            chartFields.addAll(getTargetFields(getInteractionTargets()));
        }  
        return chartFields;
    }

}
