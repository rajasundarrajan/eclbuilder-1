package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.List;

import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;

public class USMap extends Widget{
   
    private static final long serialVersionUID = 1L;
    private Attribute state;
    private Measure measure;
    private boolean county;
    

    public Attribute getState() {
        return state;
    }

    public void setState(Attribute state) {
        this.state = state;
    }

    public Measure getMeasure() {
        return measure;
    }

    public void setMeasure(Measure measure) {
        this.measure = measure;
    }
    
    public boolean hasState() {
        return state != null;
    }

    @Override
    public VisualElement generateVisualElement() throws HipieException {
        VisualElement visualElement = new VisualElement();
        visualElement.setType(this.getChartConfiguration().getType().toString());
        
        visualElement.addCustomOption(new ElementOption(CHART_TYPE,
                new FieldInstance(null, this.getChartConfiguration()
                        .getHipieName())));
        
        visualElement.setName(DashboardUtil.removeSpaceSplChar(this.getName()));
        
        generateVisualOption(visualElement);
        
        // Setting Tittle for chart
        visualElement.addOption(new ElementOption(VisualElement.TITLE,
                new FieldInstance(null, this.getTitle())));
        
        return visualElement;

    }

    @Override
    public boolean isConfigured() {
        return (this.getState()!=null)&&(this.getMeasure()!=null);
    }

    private void generateVisualOption(VisualElement visualElement) throws HipieException {

        RecordInstance ri = generateRecordInstance();
        visualElement.setBasisQualifier(ri);
        
        // Attribute settings
        if(isCounty()){
            visualElement.addOption(new ElementOption(VisualElement.COUNTY,
                    new FieldInstance(null, state.getDudName())));
        }else{
            visualElement.addOption(new ElementOption(VisualElement.STATE,
                    new FieldInstance(null, state.getDudName())));
        }       

        // Measures settings
        if(!measure.isRowCount()){

            visualElement.addOption(new ElementOption(VisualElement.WEIGHT,
                    new FieldInstance((!Measure.AGGREGATION.NONE.equals(getMeasure().getAggregation()) ) ? getMeasure()
                            .getAggregation().name() : null,measure.getDudName())));
        }else{
            visualElement.addOption(new ElementOption(VisualElement.WEIGHT,new FieldInstance(Constants.COUNT, null)));            
        }
      

      //Setting color
        visualElement.addOption(new ElementOption(VisualElement.COLOR,
                new FieldInstance(null, new String("Red_Yellow_Blue"))));
        
        //Adding Filter
        if(getFilters() != null){
            generateFilterOption(visualElement);
        }
        
      //handling Interactivity
        if(getInteractions() != null){
            generateInteractivitySelectOption(visualElement);
        }
        
        if(getInteractionTargets() != null){
            generateInteractivityFilterOption(visualElement);
        }
            
    }

    @Override
    public List<Field> getInteractivityFields() {
        Field field = this.getState();
        List<Field> fields = new ArrayList<Field>();
        fields.add(field);
        return fields;
    }
   
    public boolean isCounty() {
        return county;
    }

    public void setCounty(boolean county) {
        this.county = county;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (county ? 1231 : 1237);
        result = prime * result + ((measure == null) ? 0 : measure.hashCode());
        result = prime * result + ((state == null) ? 0 : state.hashCode());
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
        USMap other = (USMap) obj;
        if (county != other.county) {
            return false;
        }
        if (measure == null) {
            if (other.measure != null){
                return false;
            }
        } else if (!measure.equals(other.measure)){
            return false;
        }
        if (state == null) {
            if (other.state != null){
                return false;
            }
        } else if (!state.equals(other.state)) {
            return false;
        }
        return true;
    }
    
    @Override
    public USMap clone() throws CloneNotSupportedException{
        USMap usmap = (USMap) super.clone();
        usmap.setMeasure(this.measure.clone());
        return usmap;
        
    }


    @Override
    public void removeInvalidFields() {
        // TODO: remove this check once databomb is implemented
        if (isDatabomb()) {
            return;
        }
        List<Field> fields = getDatasourceFields();
        if(fields != null){
            if (!RampsUtil.isFieldPresent(fields, measure)) {
                measure = null;
            }
            if (!RampsUtil.isFieldPresent(fields, state)) {
                state = null;
            }
        }
    }
    
    @Override
    public boolean isValid(){
        List<Field> fields = getDatasourceFields();
        if(fields == null){
            return false;
        }
        return hasDatasource() && RampsUtil.isFieldPresent(fields,measure) && RampsUtil.isFieldPresent(fields, state);
    }
    

    @Override
    public List<Field> getInputElementFields() {
        List<Field> chartFields = new ArrayList<Field>();
        chartFields.add(state);
        
        if(!measure.isRowCount()){
            chartFields.add(measure);
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
    protected RecordInstance generateRecordInstance() {
        RecordInstance recordInstance = new RecordInstance();
        
        getUniqueFields(getRecordInstanceFields())
            .forEach(field ->{
                FieldInstance filterField = new FieldInstance(null, field.getDudName());
                if (!recordInstance.containsField(filterField)) {
                    recordInstance.add(filterField);
                }
            });
        
        addMeasureToRecordInstance(recordInstance, measure);
        
        return recordInstance;
    }

    @Override
    public List<Field> getRecordInstanceFields() {

        List<Field> chartFields = new ArrayList<Field>();
        chartFields.add(state);
               
        if(getFilters() != null){
            chartFields.addAll(getFilters());
        }        
        if(getInteractions() != null){
            getInteractions().forEach(source ->
                chartFields.add(source.getField())
            );
        }    
        if (getSortFields() != null) {
            chartFields.addAll(getSortFields());   
        }
        if(getInteractionTargets() != null){
            chartFields.addAll(getTargetFields(getInteractionTargets()));
        }  
        return chartFields;    
    }
}
