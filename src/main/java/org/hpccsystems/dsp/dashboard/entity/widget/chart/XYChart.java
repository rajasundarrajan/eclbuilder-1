package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XYChart extends Widget implements Cloneable {

    private static final String TIME = "time";
    private static final long serialVersionUID = 1L;
    public static final String BAR_ORIENTATION = "vertical";
    public static final String ORIENTATION = "_orientation";
    private static final Logger LOGGER = LoggerFactory.getLogger(XYChart.class);
    private List<Attribute> attributes;
    private List<Measure> measures;

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    public List<Measure> getMeasures() {
        return measures;
    }

    public void setMeasures(List<Measure> measures) {
        this.measures = measures;
    }

    public void addMeasure(Measure measure) {
        if (this.measures != null) {
            this.measures.add(measure);
        } else {
            this.measures = new ArrayList<Measure>();
            this.measures.add(measure);
        }
    }

    public void addAttribute(Attribute attribute) {
        if (this.attributes != null) {
            this.attributes.add(attribute);
        } else {
            this.attributes = new ArrayList<Attribute>();
            this.attributes.add(attribute);
        }
    }

    public void removeMeasure(Measure measure) {
        this.measures.remove(measure);
    }

    public void removeAttribute(Attribute attribute) {
        this.attributes.remove(attribute);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((measures == null) ? 0 : measures.hashCode());
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
        XYChart other = (XYChart) obj;
        if (attributes == null) {
            if (other.attributes != null) {
                return false;
            }
        } else if (!attributes.equals(other.attributes)) {
            return false;
        }
        if (measures == null) {
            if (other.measures != null) {
                return false;
            }
        } else if (!measures.equals(other.measures)) {
            return false;
        }
        return true;
    }
    
    @Override
	public VisualElement generateVisualElement() throws HipieException {
        VisualElement visualElement = new VisualElement();
        // Sets chart type
        visualElement.addCustomOption(new ElementOption(CHART_TYPE, new FieldInstance(null, this.getChartConfiguration().getHipieName())));
        // Sets Stacked Property
        if (ChartType.STACKCOLUMN.equals(this.getChartConfiguration().getType())) {
            visualElement.addCustomOption(new ElementOption(STACKED, new FieldInstance(null, "true")));
        }
        // Sets chart Name
        visualElement.setName(DashboardUtil.removeSpaceSplChar(this.getName()));

        generateVisualOption(visualElement);

        // Setting Tittle for chart
        visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, this.getTitle())));

        return visualElement;
	}

    @Override
    public boolean isConfigured() {
        // initialized to false
        boolean isBasicConfig = false;

        // check attribute and measures NOT empty
        if ((this.getAttributes() != null) && (!this.getAttributes().isEmpty()) && (this.getMeasures() != null) && (!this.getMeasures().isEmpty())) {
            // check for stacked column chart
            if (ChartType.STACKCOLUMN.equals(this.getChartConfiguration().getType())) {
                // check for length
                if (this.getMeasures().size() >= 2) {
                    isBasicConfig = true;
                }
            } else {
                isBasicConfig = true;
            }
        }
        return isBasicConfig;
    }

    /**
     * Returns time format for the first item of list of attributes in this
     * xyChart
     **/
    private String getTimePattern(Attribute attribute) {
        return attribute.getTimeFormat();
    }

    public boolean hasAttribute(Attribute attribute) {
        return attributes == null ? false : attributes.contains(attribute);
    }

    public boolean hasMeasure(Measure measure) {
        return measures == null ? false : measures.contains(measure);
    }

    /**
     * This method generates the hipie syntax to represent a dashboard widget
     * includes (widget type,o/p,attribute,measure and interactivity)
     * 
     * @param visualElement
     *            - Hipie object represents the visualization dashboard the
     *            sample o/p of the VisualElement.toString() is as follows LINE
     *            salesreport
     *            (dsOutput{Attribute_salesreport,Attribute1_salesreport
     *            ,SUM(Measure1_salesreport)}): Y(SUM(Measure1_salesreport),
     *            SUM(Measure1_salesreport)),X(Attribute_salesreport,
     *            Attribute1_salesreport),TITLE("salesreport"),
     *            _charttype("C3_COLUMN"), SORT(Attribute_salesreport);
     * @throws HipieException
     */
    private void generateVisualOption(VisualElement visualElement) throws HipieException {

        visualElement.setType(ChartType.LINE.toString());

        RecordInstance ri = generateRecordInstance();
        visualElement.setBasisQualifier(ri);

        boolean appendElement = false;
        // element to achieve x(a1,a2),y(m1,m1)
        for (Measure measure : getMeasures()) {
            // Measures settings
        	// Add Y measure to visual element
            addMeasuresToVisualElement(visualElement, measure, appendElement);

            if (appendElement) {
                // Attribute settings
                ElementOption xElement = visualElement.getOption(VisualElement.X);
                // TODO:Need to handle the multiple attribute
                xElement.addParam(new FieldInstance(null, getAttributes().get(0).getDudName()));
            } else {
                // Attribute settings
                // TODO:Need to handle the multiple attribute
                visualElement.addOption(new ElementOption(VisualElement.X, new FieldInstance(null, getAttributes().get(0).getDudName())));
            }

            appendElement = true;
        }

        if (!StringUtils.isEmpty(getTimePattern(getAttributes().get(0)))) {
            // Sets x axis type and time pattern
            visualElement.addCustomOption(new ElementOption(X_AXIS_TYPE, new FieldInstance(null, TIME)));
            visualElement
                    .addCustomOption(new ElementOption(X_AXIS_TYPE_TIMEPATTERN, new FieldInstance(null, getTimePattern(getAttributes().get(0)))));
        }
        // Adding Filter
        if (getFilters() != null) {
            generateFilterOption(visualElement);
        }

        // Handling Sort fields
        if (getSortFields() != null) {
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
        LOGGER.debug("visual element--->>{}", visualElement);

    }

    private void addMeasuresToVisualElement(VisualElement visualElement, Measure measure, boolean appendMeasure) {
        if (appendMeasure) {
            ElementOption yElement = visualElement.getOption(VisualElement.Y);
            if (measure.isRowCount()) {
                yElement.addParam(new FieldInstance(Constants.COUNT, null));
            } else {
                yElement.addParam(new FieldInstance(
                        (!Measure.AGGREGATION.NONE.equals(measure.getAggregation())) ? measure.getAggregation().name() : null, measure.getDudName()));
            }

        } else {
            if (measure.isRowCount()) {
                visualElement.addOption(new ElementOption(VisualElement.Y, new FieldInstance(Constants.COUNT, null)));
            } else {
                visualElement
                        .addOption(new ElementOption(VisualElement.Y,
                                new FieldInstance(
                                        (!Measure.AGGREGATION.NONE.equals(measure.getAggregation())) ? measure.getAggregation().name() : null,
                                        measure.getDudName())));
            }

        }
    }

    @Override
    public List<Field> getInteractivityFields() {
        List<Field> fields = new ArrayList<Field>();
        for (Attribute attribute : this.getAttributes()) {
            fields.add(attribute);
        }
        return fields;
    }

    @Override
    public XYChart clone() throws CloneNotSupportedException {
        XYChart xychart = (XYChart) super.clone();
        List<Measure> clonedMeasures = new ArrayList<Measure>();
        for (Measure measure : xychart.getMeasures()) {
            clonedMeasures.add(measure.clone());
        }
        xychart.setMeasures(clonedMeasures);
        List<Attribute> clonedAttributes = new ArrayList<Attribute>();
        for (Attribute attribute : xychart.getAttributes()) {
            clonedAttributes.add(attribute.clone());
        }
        xychart.setAttributes(clonedAttributes);
        return xychart;
    }

    @Override
    public void removeInvalidFields() {
        if (this.isConfigured()) {
            // TODO: remove this check once databomb is implemented
            if (isDatabomb()) {
                return;
            }
            List<Field> fields = getDatasourceFields();
            if (fields != null) {
                measures.removeAll(getInvalidMeasures(fields));
                attributes.removeAll(getInvalidAttributes(fields));
            }
        }
    }

    private List<Measure> getInvalidMeasures(List<Field> fields) {
        List<Measure> invalidMeasures = new ArrayList<Measure>();
        for (Measure measure : measures) {
            if (!RampsUtil.isFieldPresent(fields, measure)) {
                invalidMeasures.add(measure);
            }
        }
        return invalidMeasures;
    }

    private List<Attribute> getInvalidAttributes(List<Field> fields) {
        List<Attribute> invalidAttributes = new ArrayList<Attribute>();
        for (Attribute attribute : attributes) {
            if (!RampsUtil.isFieldPresent(fields, attribute)) {
                invalidAttributes.add(attribute);
            }
        }
        return invalidAttributes;
    }

    @Override
    public boolean isValid() {
        List<Field> fields = getDatasourceFields();
        if (fields == null) {
            return false;
        }
        return hasDatasource() && getInvalidAttributes(fields).isEmpty() && getInvalidMeasures(fields).isEmpty();
    }

    @Override
    public List<Field> getInputElementFields() {
        List<Field> chartFields = new ArrayList<Field>();

        measures.stream().filter(measure -> !measure.isRowCount()).forEach(measure -> chartFields.add(measure));

        chartFields.addAll(attributes);

        if (getFilters() != null) {
            chartFields.addAll(getFilters());
        }
        if (getSortFields() != null ) {
              getSortFields().stream().filter(sortFields -> !sortFields.isRowCount()).forEach(sortFields -> chartFields.add(sortFields));
        }
        if (getInteractions() != null) {
            getInteractions().forEach(source -> chartFields.add(source.getField()));
        }
        if (getInteractionTargets() != null) {
            getInteractionTargets().forEach(target -> chartFields.add(target.getField()));
        }

        return chartFields;
    }

    @Override
    protected RecordInstance generateRecordInstance() {
        RecordInstance recordInstance = new RecordInstance();
        
        getUniqueFields(getRecordInstanceFields()).forEach(field -> {
            FieldInstance filterField = new FieldInstance(null, field.getDudName());
            if (!recordInstance.containsField(filterField)) {
                recordInstance.add(filterField);
            }
        });
        
        measures.forEach(measure -> addMeasureToRecordInstance(recordInstance, measure));

        return recordInstance;
    }

    @Override
    public List<Field> getRecordInstanceFields() {
        List<Field> chartFields = new ArrayList<Field>();

        chartFields.addAll(attributes);

        if (getFilters() != null) {
            chartFields.addAll(getFilters());
        }
        if (getInteractions() != null) {
            getInteractions().forEach(source -> chartFields.add(source.getField()));
        }

        if (getSortFields() != null) {
            chartFields.addAll(getSortFields().stream().filter(sort -> sort.getAggregation() == null || sort.getAggregation() == AGGREGATION.NONE)
                    .collect(Collectors.toList()));
        }
        if (getInteractionTargets() != null) {
            chartFields.addAll(getTargetFields(getInteractionTargets()));
        }
        return chartFields;

    }

}
