package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.USMap;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;

/**
 * EditChartController class is used to handle the edit page of the Dashboard
 * project and controller class for edit_portlet.zul file.
 * 
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class USMapController extends ConfigurationComposer<Component> {
    private static final String PLEASE_DROP = "pleaseDrop";
    private static final String SAME_DATASET_SHOULDBE_USED = "sameDatasetShouldbeUsed";
    private static final String ON_CLICK = "onClick";
    private static final String Z_ICON_TIMES = "z-icon-times";
    private static final String BTN_CLOSE = "btn-close";
    private static final long serialVersionUID = 1L;
    private static final String ON_LOADING = "onLoading";

    private USMap usMap;
   
    @Wire
    private Radio  stateRadio;
    @Wire
    private Radio  countyRadio;  

    @Wire
    private Listbox chartMeasureListbox;
    private ListModelList<Measure> measures = new ListModelList<Measure>();

    @Wire
    private Listbox chartAttributeListbox;
    private ListModelList<Attribute> states = new ListModelList<Attribute>();

    private ListitemRenderer<Measure> chartMeasureRenderer = (listitem, measure, index) -> 
        chartRenderer(listitem, measure);

    private ListitemRenderer<Attribute> chartAttributeRenderer = (listitem, attribute, index) -> {
        attribute.setDisplayName(attribute.getColumn());
        Listcell listItemCell = new Listcell();
        listItemCell.setLabel(attribute.getDisplayName());
        listItemCell.setParent(listitem);
        Button closeButton = new Button();
        closeButton.setParent(listItemCell);
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setSclass(BTN_CLOSE);
        listitem.appendChild(listItemCell);
        closeButton.addEventListener(ON_CLICK,event -> {
            states.remove(attribute);
            usMap.setState(null);
            chartAttributeListbox.setDroppable(Dashboard.TRUE);
            if (measures.isEmpty()) {
                outputName = null;
            }
        });
    };
    private void chartRenderer(Listitem listitem, Measure measure) {
        measure.setDisplayName(measure.getColumn());
        Listcell listCell = new Listcell(measure.getDisplayName());
        if( !measure.isRowCount()){
            DashboardUtil.renderAggregateButton(listCell, measure, null, sortAggregateSync, fieldModel);
        }
      
        Button closeButton = new Button();
        closeButton.setIconSclass(Z_ICON_TIMES);
        closeButton.setSclass(BTN_CLOSE);
        closeButton.setStyle("float:right");
        closeButton.addEventListener(ON_CLICK,event -> {
            measures.remove(measure);
            usMap.setMeasure(null);
            chartMeasureListbox.setDroppable(Dashboard.TRUE);
            if (states.isEmpty()) {
                outputName = null;
            }
        });
      
        closeButton.setParent(listCell);
        listitem.appendChild(listCell);
    }

    @Override
    public void doAfterCompose(final Component comp) throws Exception {
        super.doAfterCompose(comp);
        usMap = (USMap) widgetConfiguration.getWidget();
        comp.addEventListener(ON_LOADING, loadingListener);
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);

        chartMeasureListbox.setModel(measures);
        chartMeasureListbox.setItemRenderer(chartMeasureRenderer);
        chartAttributeListbox.setModel(states);
        chartAttributeListbox.setItemRenderer(chartAttributeRenderer);
        
        if (usMap.isConfigured()) {
            //Remove the measure and attribute which are not available in the present datasource
            usMap.removeInvalidFields();
        }
        
        //Checking again for configuration, as invalid fields may be removed
        if(usMap.isConfigured()) {
            measures.add(usMap.getMeasure());
            states.add(usMap.getState());
            chartMeasureListbox.setDroppable(Constants.FALSE);
            chartAttributeListbox.setDroppable(Constants.FALSE);
        }
        
        if(usMap.isCounty()){
            countyRadio.setChecked(true); 
        }
        
        stateRadio.addEventListener(Events.ON_CHECK, event -> usMap.setCounty(false));
        countyRadio.addEventListener(Events.ON_CHECK, event -> usMap.setCounty(true));
        this.getSelf().addEventListener(EVENTS.ON_WIDGET_NOT_CONFIGURED, this::onWidgetNotConfigured);
    }

    private void onWidgetNotConfigured(Event event) {
        if (measures == null || measures.isEmpty()) {
            Clients.showNotification(Labels.getLabel(PLEASE_DROP) + " " + Labels.getLabel("measure"), Constants.ERROR, chartMeasureListbox, Constants.POSITION_END_CENTER,
                    3000, true);
        } else if (states == null || states.isEmpty()) {
            Clients.showNotification(Labels.getLabel(PLEASE_DROP) + " " + (stateRadio.isChecked() ? Labels.getLabel("state") : Labels.getLabel("county")),
                    Constants.ERROR, chartAttributeListbox, Constants.POSITION_END_CENTER, 3000, true);
        }
    }

    @Listen("onDrop = #chartMeasureListbox")
    public void onDropChartMeasure(DropEvent event) {
        Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> node = draggedItem.getValue();
            if (checkOutputs(node.getParent().getData().getColumn())) {
                Field field = node.getData();
                Measure measure = new Measure(field);
                if (draggedItem.getTree().equals(attributeTree)) {
                    measure.setDataType("unsigned");
                    Clients.showNotification("\"" +measure.getColumn()+"\" \n"+Labels.getLabel("willBeHandled"), Clients.NOTIFICATION_TYPE_WARNING, chartMeasureListbox, Constants.POSITION_END_CENTER, 5000,
                            true);
                }
                usMap.setMeasure(measure);
                measures.add(measure);
                chartMeasureListbox.setDroppable(Dashboard.FALSE);

            } else {
                Clients.showNotification(Labels.getLabel(SAME_DATASET_SHOULDBE_USED), Clients.NOTIFICATION_TYPE_ERROR, chartMeasureListbox,
                        Constants.POSITION_END_CENTER, 5000, true);
                return;
            }
        }

    @Listen("onDrop = #chartAttributeListbox")
    public void onDropChartAttribute(DropEvent event) {
        Treeitem draggedItem = (Treeitem) event.getDragged();
        TreeNode<Field> node = draggedItem.getValue();
        if (checkOutputs(node.getParent().getData().getColumn())) {
            Field field = node.getData();
            if(field.isRowCount()) {
                Clients.showNotification(Labels.getLabel("rowCountCannotBeUsedAsAttribute"), Clients.NOTIFICATION_TYPE_ERROR, chartAttributeListbox, Constants.POSITION_END_CENTER, 3000, true);
                return;
            }
            Attribute attribute = new Attribute(field);
            usMap.setState(attribute);
            states.add(attribute);
            chartAttributeListbox.setDroppable(Dashboard.FALSE);
        } else {
            Clients.showNotification(Labels.getLabel(SAME_DATASET_SHOULDBE_USED), Clients.NOTIFICATION_TYPE_ERROR, chartAttributeListbox,
                    Constants.POSITION_END_CENTER, 5000, true);
            return;
        }
    }

}
