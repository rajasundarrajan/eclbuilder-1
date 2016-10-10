package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.SortField;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.XYChart;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Vlayout;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class XYChartController extends ConfigurationComposer<Component> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XYChartController.class);
    private static final String SAME_DATASET = "sameDatasetShouldbeUsed";
    private static final String Z_ICON_TIMES = "z-icon-times";
    private static final String ON_CLICK = "onClick";
    private static final String BTN_CLOSE = "btn-close";
    private static final String ON_LOADING = "onLoading";
    private static final long serialVersionUID = 1L;

    private XYChart xyChart;

    @Wire
    private Listbox chartMeasureListbox;
    private ListModelList<Measure> measures = new ListModelList<Measure>();

    @Wire
    private Listbox chartAttributeListbox;
    private ListModelList<Attribute> attributes = new ListModelList<Attribute>();

    private ListitemRenderer<Measure> chartMeasureRenderer = (listitem, measure, index) -> renderMeasureField(listitem, measure);

    private ListitemRenderer<Attribute> chartAttributeRenderer = (listitem, attribute, index) -> getChartAttributeRenderer(listitem, attribute);

    private void getChartAttributeRenderer(Listitem listitem, Attribute attribute) {
        attribute.setDisplayName(attribute.getColumn());
        Vlayout vlayout = new Vlayout();
        vlayout.setHflex("1");
        vlayout.setVflex("1");
        Hlayout hlayout = new Hlayout();
        hlayout.setHflex("1");
        hlayout.setVflex("1");
        hlayout.setParent(vlayout);
        Label label = new Label(attribute.getDisplayName());
        label.setParent(hlayout);
        Listcell cell = new Listcell();
        cell.setSclass("field_hlayout_right_align");
        vlayout.setParent(cell);
        cell.setParent(listitem);
        Textbox textbox = new Textbox();
        textbox.setText(attribute.getTimeFormat());
        textbox.addEventListener(Events.ON_CHANGING, event -> onChangeTimeFormat((InputEvent) event,attribute));
        vlayout.appendChild(textbox);
        Button timeSeries = new Button();
        timeSeries.setIconSclass("fa fa-clock-o");
        timeSeries.setSclass("btn-clock");
        textbox.setVisible(!StringUtils.isEmpty(attribute.getTimeFormat()));
        timeSeries.addEventListener(ON_CLICK, event -> textbox.setVisible(!textbox.isVisible()));
        timeSeries.setTooltiptext(Labels.getLabel("SpecifyDateFormat"));
        Button closeButton = new Button();
        closeButton.setParent(hlayout);
        timeSeries.setParent(hlayout);
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setSclass(BTN_CLOSE);
        closeButton.addEventListener(ON_CLICK, event -> {
            attributes.remove(attribute);
            xyChart.removeAttribute(attribute);
            chartAttributeListbox.setDroppable(Dashboard.TRUE);
            if (measures.isEmpty()) {
                outputName = null;
            }
        });
    }
    
    private void onChangeTimeFormat(InputEvent event,Attribute attribute) {
        //Keep time pattern null, when user clear's the existing value
        if(StringUtils.isEmpty(event.getValue())){
            attribute.setTimeFormat(null);
        }else{
            attribute.setTimeFormat(event.getValue());
        }
    }

    private void renderMeasureField(Listitem listitem, Measure measure) {
        measure.setDisplayName(measure.getColumn());
        listitem.setDroppable(Dashboard.TRUE);
        listitem.setDraggable(Dashboard.TRUE);
        listitem.setValue(measure);
        Listcell listCell = new Listcell(measure.getDisplayName());
        if (!measure.isRowCount()) {
            DashboardUtil.renderAggregateButton(listCell, measure, xyChart.getMeasures(), sortAggregateSync, fieldModel);
        }

        Button closeButton = new Button();
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setSclass(BTN_CLOSE);
        closeButton.setStyle("float:right");
        closeButton.addEventListener(ON_CLICK, event -> {
            measures.remove(measure);
            xyChart.removeMeasure(measure);
            SortField sortFieldRemove = sortAggregateSync.get(measure);
            if (sortFieldRemove != null) {
                int sortFieldIndex = fieldModel.indexOf(sortFieldRemove);
                xyChart.getSortFields().remove(sortFieldRemove);
                fieldModel.remove(sortFieldIndex);
                sortAggregateSync.remove(measure);
                Clients.showNotification("Corresponding sort field is also removed", Clients.NOTIFICATION_TYPE_WARNING, this.getSelf(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
            }
            if (attributes.isEmpty() && measures.isEmpty()) {
                outputName = null;
            }
        });

        closeButton.setParent(listCell);
        listitem.appendChild(listCell);
        listitem.addEventListener(Events.ON_DROP, event -> onDropMeasureBox((DropEvent) event));
    }

    private void onDropMeasureBox(DropEvent event) {
        if ("chartMeasureListbox".equals(event.getDragged().getParent().getId())) {
            reorder(event);
        } else {
            // Dragged from source list
            onDropChartMeasureFromSource(event);
        }
    }

    private void reorder(DropEvent event) {
        int from = chartMeasureListbox.getChildren().indexOf((Listitem) event.getDragged()) - 1;
        if (event.getTarget().getParent() instanceof Listbox) {
            int to = chartMeasureListbox.getChildren().indexOf((Listitem) event.getTarget()) - 1;
            Measure temp = measures.get(from);
            if (from < to) {
                measures.add(to + 1, temp);
                measures.remove(from);
            } else {
                measures.add(to, temp);
                measures.remove(from + 1);
            }
        } else {
            measures.add(measures.get(from));
            measures.remove(from);
        }
        xyChart.setMeasures(new ArrayList<Measure>());
        xyChart.getMeasures().addAll(measures);
    }

    @Override
    public void doAfterCompose(final Component comp) throws Exception {
        super.doAfterCompose(comp);
        xyChart = (XYChart) widgetConfiguration.getWidget();
        comp.addEventListener(ON_LOADING, loadingListener);
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);

        chartMeasureListbox.setModel(measures);
        chartMeasureListbox.setItemRenderer(chartMeasureRenderer);
        chartAttributeListbox.setModel(attributes);
        chartAttributeListbox.setItemRenderer(chartAttributeRenderer);
        chartMeasureListbox.addEventListener(Events.ON_DROP, event -> onDropMeasureBox((DropEvent) event));

        // Remove the measure and attribute which are not available in the
        // present datasource
        xyChart.removeInvalidFields();

        

        LOGGER.debug("Attributes - {}", xyChart.getAttributes());
        
        if(xyChart.getMeasures() != null && xyChart.getMeasures().size()>0){
            measures.addAll(xyChart.getMeasures());
        }
        if(xyChart.getAttributes() != null && xyChart.getAttributes().size()> 0){
            attributes.addAll(xyChart.getAttributes());
            chartAttributeListbox.setDroppable(Constants.FALSE);
        }
        if (xyChart.getSortFields() != null) {
            xyChart.getSortFields().stream().forEach(sortField -> {
                Measure measure = sortField.getEquivalentMeasure(measures);
                if (measure != null) {
                    sortAggregateSync.put(measure, sortField);
                }
                fieldModel.add(sortField);
            });
        }
      

        this.getSelf().addEventListener(EVENTS.ON_WIDGET_NOT_CONFIGURED, this::onWidgetsNotConfigured);
    }

    private void onWidgetsNotConfigured(Event event) {

        if (attributes == null || attributes.isEmpty()) {
            Clients.showNotification("Please drop a attribute variable", Constants.ERROR, chartAttributeListbox, Constants.POSITION_END_CENTER, 3000,
                    true);
        } else if (measures == null || measures.isEmpty()) {
            Clients.showNotification("Please drop a measure variable", Constants.ERROR, chartMeasureListbox, Constants.POSITION_END_CENTER, 3000,
                    true);
        } else if (ChartType.STACKCOLUMN.equals(xyChart.getChartConfiguration().getType())) {
            Clients.showNotification("Please drop atleast two measure variable", Constants.ERROR, chartMeasureListbox, Constants.POSITION_END_CENTER,
                    3000, true);
        }
    }

    private void onDropChartMeasureFromSource(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> node = draggedItem.getValue();
            if (checkOutputs(node.getParent().getData().getColumn())) {
                // Cloning measure object, as old reference should be allowed
                // for use again as another measure
                Field field = node.getData();
                Measure measure = new Measure(field);
                LOGGER.debug("Measure object --------->{}", measure);
                if (xyChart.hasMeasure(measure)) {
                    Clients.showNotification(
                            measure.getColumn() + " " + Labels.getLabel("with") + " " + measure.getAggregation() + " "
                                    + Labels.getLabel("aggregationAlreadyExists"),
                            Clients.NOTIFICATION_TYPE_ERROR, chartMeasureListbox, Constants.POSITION_END_CENTER, 3000,
                            true);
                    return;
                }
                if (draggedItem.getTree().equals(attributeTree)) {
                    measure.setDataType("unsigned");
                    Clients.showNotification("\"" + measure.getColumn() + "\" \n" + Labels.getLabel("willBeHandled"),
                            Clients.NOTIFICATION_TYPE_WARNING, chartMeasureListbox, Constants.POSITION_END_CENTER, 5000,
                            true);
                }
                xyChart.addMeasure(measure);
                measures.add(measure);
            } else {
                Clients.showNotification(Labels.getLabel(SAME_DATASET), Clients.NOTIFICATION_TYPE_ERROR,
                        chartMeasureListbox, Constants.POSITION_END_CENTER, 5000, true);
            }
        } else {
            Clients.showNotification(Labels.getLabel("xyChartMeasure"), Constants.ERROR, chartMeasureListbox,
                    Constants.POSITION_END_CENTER, 5000, true);
            return;
        }
    }

    @Listen("onDrop = #chartAttributeListbox")
    public void onDropChartAttribute(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> node = draggedItem.getValue();
            if (checkOutputs(node.getParent().getData().getColumn())) {
                Field field = node.getData();
                if (field.isRowCount()) {
                    Clients.showNotification(Labels.getLabel("rowCountCannotBeUsedAsAttribute"),
                            Clients.NOTIFICATION_TYPE_ERROR, chartAttributeListbox, Constants.POSITION_END_CENTER, 3000,
                            true);
                    return;
                }
                Attribute attribute = new Attribute(field);
                xyChart.addAttribute(attribute);
                attributes.add(attribute);
                chartAttributeListbox.setDroppable(Constants.FALSE);
            } else {
                Clients.showNotification(Labels.getLabel(SAME_DATASET), Clients.NOTIFICATION_TYPE_ERROR,
                        chartAttributeListbox, "end_center", 5000, true);
                return;
            }
        } else {
            Clients.showNotification(Labels.getLabel("xyChartAttribute"), Constants.ERROR, chartAttributeListbox,
                    Constants.POSITION_END_CENTER, 5000, true);
            return;
        }

    }

    public ListModelList<Attribute> getAttributes() {
        return attributes;
    }

}
