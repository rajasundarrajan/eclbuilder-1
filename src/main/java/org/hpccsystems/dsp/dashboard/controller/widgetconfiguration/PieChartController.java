package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.Pie;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;

public class PieChartController extends ConfigurationComposer<Component> {
    private static final String SAME_DATASET_SHOULDBE_USED = "sameDatasetShouldbeUsed";
    private static final String BTN_CLOSE = "btn-close right-btn";
    private static final String Z_ICON_TIMES = "z-icon-times";
    private static final String ON_LOADING = "onLoading";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PieChartController.class);

    private org.hpccsystems.dsp.dashboard.entity.widget.chart.Pie pie;

    @Wire
    private Listbox weightListbox;
    private ListModelList<Measure> weights = new ListModelList<Measure>();

    @Wire
    private Listbox labelListbox;
    private ListModelList<Attribute> labels = new ListModelList<Attribute>();

    private ListitemRenderer<Measure> weightRenderer = (listitem, measure, index) -> 
        weightListCreator(listitem, measure);

    private ListitemRenderer<Attribute> labelRenderer = (listitem, attribute, index) -> renderLabels(listitem, attribute);
    
    private void weightListCreator(Listitem listitem, Measure measure) {
        measure.setDisplayName(measure.getColumn());
        Listcell listcell = new Listcell(measure.getDisplayName());
        if(!measure.isRowCount()){
            DashboardUtil.renderAggregateButton(listcell, measure, null, sortAggregateSync, fieldModel);
        }
        Button closeButton = new Button();
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setSclass(BTN_CLOSE);
        closeButton.addEventListener(Events.ON_CLICK, (SerializableEventListener<? extends Event>)event -> {
            weights.remove(measure);
            pie.setWeight(null);
            weightListbox.setDroppable(Constants.TRUE);
            if (labels.isEmpty()) {
                outputName = null;
            }
        });

      
        listcell.appendChild(closeButton);
        listitem.appendChild(listcell);
        listitem.setValue(measure);
        listitem.setDraggable(Dashboard.TRUE);
    }

    private void renderLabels(Listitem listitem, Attribute attribute) {
        attribute.setDisplayName(attribute.getColumn());
        Listcell listItemCell = new Listcell();
        listItemCell.setLabel(attribute.getDisplayName());
        listItemCell.setParent(listitem);
        Button closeButton = new Button();
        closeButton.setParent(listItemCell);
        closeButton.setSclass(BTN_CLOSE);
        closeButton.setIconSclass(Z_ICON_TIMES);
        listitem.appendChild(listItemCell);
        closeButton.addEventListener("onClick", (SerializableEventListener<? extends Event>)event -> {
            labels.remove(attribute);
            pie.setLabel(null);
            labelListbox.setDroppable(Constants.TRUE);
            if (weights.isEmpty()) {
                outputName = null;

            }
        });
        listitem.setValue(attribute);
        listitem.setDraggable(Dashboard.TRUE);
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        pie = (Pie) widgetConfiguration.getWidget();
        comp.addEventListener(ON_LOADING, loadingListener);
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);
        if (pie.isConfigured()) {
             //Remove the measure and attribute which are not available in the present datasource
             pie.removeInvalidFields();
        }
        
        weightListbox.setModel(weights);
        weightListbox.setItemRenderer(weightRenderer);
        labelListbox.setModel(labels);
        labelListbox.setItemRenderer(labelRenderer);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Pie Configuration - {}", pie);
        }

        if (pie.isConfigured()) {
            weights.add(pie.getWeight());
            labels.add(pie.getLabel());
            weightListbox.setDroppable(Constants.FALSE);
            labelListbox.setDroppable(Constants.FALSE);
        }

        this.getSelf().addEventListener(EVENTS.ON_WIDGET_NOT_CONFIGURED, event -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("weights-->{}", weights);
                LOGGER.debug("labels-->{}", labels);
            }
            if (weights == null || weights.isEmpty()) {
                Clients.showNotification("Please drop a weight variable", Constants.ERROR, weightListbox, Constants.POSITION_END_CENTER, 3000, true);
            } else if (labels == null || labels.isEmpty()) {
                Clients.showNotification("Please drop a label variable", Constants.ERROR, labelListbox, Constants.POSITION_END_CENTER, 3000, true);
            }
        });
    }

    @Listen("onDrop = #weightListbox")
    public void onDropWeight(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> fieldNode = draggedItem.getValue();

            if (checkOutputs(fieldNode.getParent().getData().getColumn())) {
                Measure measure = new Measure(fieldNode.getData());
                if (draggedItem.getTree().equals(attributeTree)) {
                    measure.setDataType("unsigned");
                    Clients.showNotification("\"" + measure.getColumn() + "\" \n" + Labels.getLabel("willBeHandled"),
                            Clients.NOTIFICATION_TYPE_WARNING, weightListbox, Constants.POSITION_END_CENTER, 5000,
                            true);
                }
                pie.setWeight(measure);
                weights.add(measure);
                weightListbox.setDroppable(Constants.FALSE);
            } else {
                Clients.showNotification(Labels.getLabel(SAME_DATASET_SHOULDBE_USED), Clients.NOTIFICATION_TYPE_ERROR,
                        labelListbox, Constants.POSITION_END_CENTER, 5000, true);
                return;
            }
        } else {
            Clients.showNotification(Labels.getLabel("pieChartWeight"), Constants.ERROR,
                    weightListbox, Constants.POSITION_END_CENTER, 5000, true);
            return;
        }

    }

    @Listen("onDrop = #labelListbox")
    public void onDropLabel(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> fieldNode = draggedItem.getValue();
            LOGGER.debug(" draggedItem.getParent()------------->{}", fieldNode.getParent());
            if (checkOutputs(fieldNode.getParent().getData().getColumn())) {
                if (fieldNode.getData().isRowCount()) {
                    Clients.showNotification(Labels.getLabel("rowCountCannotBeUsedAsLabel"),
                            Clients.NOTIFICATION_TYPE_ERROR, labelListbox, Constants.POSITION_END_CENTER, 3000, true);
                    return;
                }

                Attribute attribute = new Attribute(fieldNode.getData());
                pie.setLabel(attribute);
                labels.add(attribute);
                labelListbox.setDroppable(Constants.FALSE);
            } else {
                Clients.showNotification(Labels.getLabel(SAME_DATASET_SHOULDBE_USED), Clients.NOTIFICATION_TYPE_ERROR,
                        labelListbox, Constants.POSITION_END_CENTER, 5000, true);
                return;
            }
        } else {
            Clients.showNotification(Labels.getLabel("pieChartLabel"),
                    Constants.ERROR, labelListbox, Constants.POSITION_END_CENTER, 5000, true);
            return;
        }

    }

}
