package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import java.util.ArrayList;
import java.util.List;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.Attribute;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.SortField;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.Table;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treeitem;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class TableWidgetController extends ConfigurationComposer<Component> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableWidgetController.class);

    private static final String FLOAT_RIGHT = "float:right";
    private static final String ON_LOADING = "onLoading";
    private static final long serialVersionUID = 1L;
    private static final String IMG_BTN_DEL_BTN = "img-btn del-btn";

    private Table table;

    @Wire
    private Listbox columnListbox;
    private ListModelList<Field> columns = new ListModelList<Field>();

    private ListitemRenderer<Field> tableColumnRenderer = (listitem, column, index) -> tableColumnCreator(listitem, column);
   
    SerializableEventListener<DropEvent> dropListener = new SerializableEventListener<DropEvent>() {
    private static final long serialVersionUID = 1L;
        @Override
        public void onEvent(DropEvent event) throws Exception {
            if ("columnListbox".equals(event.getDragged().getParent().getId())) {
                reorder(event);
            } else {
                // Dragged from source list
                onDropColumnsFromSourceList(event);
            }
        }
    };

    private void reorder(DropEvent event) {
        int from = columnListbox.getChildren().indexOf((Listitem) event.getDragged()) - 1;
        if (event.getTarget().getParent() instanceof Listbox) {
            int to = columnListbox.getChildren().indexOf((Listitem) event.getTarget()) - 1;
            Field temp = columns.get(from);
            if (from < to) {
                columns.add(to + 1, temp);
                columns.remove(from);
            } else {
                columns.add(to, temp);
                columns.remove(from + 1);
            }
        } else {
            columns.add(columns.get(from));
            columns.remove(from);
        }
        table.setTableColumns(new ArrayList<Field>());
        table.getTableColumns().addAll(columns);
    }
            
    /**
     * Populates the Measures and Attributes with fields from the datasource 
     * @param listitem
     * @param column
     */
    private void tableColumnCreator(Listitem listitem, Field column) {
        listitem.setDroppable("true");
        listitem.setDraggable("true");
        if(column.getDisplayName() == null){
            column.setDisplayName(column.getColumn()); 
        }

        Listcell listcell = new Listcell(column.getDisplayName());
        checkAggregateButton(column, listcell);
        
        Button editButton = new Button();
        editButton.setZclass(IMG_BTN_DEL_BTN);
        editButton.setIconSclass("fa fa-pencil");
        editButton.setStyle(FLOAT_RIGHT);
        Popup popup = new Popup();
        popup.setSclass("tableColPopup");
        listcell.appendChild(popup);
        Textbox textbox = new Textbox();
        textbox.setValue(column.getDisplayName());
        popup.appendChild(textbox);
        textbox.setFocus(true);
        editButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
            private static final long serialVersionUID = 1L;
            @Override
            public void onEvent(Event event) throws Exception {
                popup.open(listcell,"overlap");
                textbox.addEventListener(Events.ON_CHANGE, new SerializableEventListener<Event>() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onEvent(Event event) throws Exception {
                        onChangeTextbox(listitem, column, listcell, popup, textbox);
                    }
                });
            }
        });
        
        Button closeButton = new Button();
        closeButton.setIconSclass("z-icon-times");
        closeButton.setZclass(IMG_BTN_DEL_BTN);
        closeButton.setStyle(FLOAT_RIGHT);
        closeButton.addEventListener("onClick", (SerializableEventListener<? extends Event>)event -> {
            columns.remove(column);
            table.removeColumn(column);
            if (column instanceof Measure) {
                SortField sortFieldRemove = sortAggregateSync.get((Measure) column);
                if (sortFieldRemove != null) {
                    int sortFieldIndex = fieldModel.indexOf(sortFieldRemove);
                    table.getSortFields().remove(sortFieldRemove);
                    fieldModel.remove(sortFieldIndex);
                    sortAggregateSync.remove((Measure) column);
                    Clients.showNotification("Corresponding sort field is also removed", Clients.NOTIFICATION_TYPE_WARNING, this.getSelf(), Constants.POSITION_TOP_CENTER,
                            5000, true);
                }
            }
            if (columns.isEmpty()) {
                outputName = null;

            }

        });
       
        listcell.appendChild(closeButton);
        listcell.appendChild(editButton);
        listitem.appendChild(listcell);
        listitem.addEventListener(Events.ON_DROP, dropListener);
        listitem.setValue(column);
    }

    private void onChangeTextbox(Listitem listitem, Field column, Listcell listcell, Popup popup, Textbox textbox) {
        boolean alreadyExists = false;
        if (textbox.getValue() != null && !textbox.getValue().trim().isEmpty()) {
            for (Field fieldColumn : columns) {
                if (column.getColumn().equals(fieldColumn.getColumn())) {
                    boolean isMeasure = column instanceof Measure && fieldColumn instanceof Measure;
                    boolean isMeasureEqual = false;
                    if (isMeasure) {
                        isMeasureEqual = ((Measure) column).getAggregation() == ((Measure) fieldColumn).getAggregation();
                    }
                    if (!isMeasure || (isMeasure && isMeasureEqual)) {
                        continue;
                    }
                }
                alreadyExists = textbox.getValue().equals(fieldColumn.getDisplayName());
                if (alreadyExists) {
                    Clients.showNotification(textbox.getValue() + " " + Labels.getLabel("alreadyExist"), Clients.NOTIFICATION_TYPE_ERROR, listitem,
                            Constants.POSITION_END_CENTER, 5000, true);
                    textbox.setValue(column.getDisplayName());
                    break;
                }
            }
        } else {
            textbox.setValue(column.getDisplayName());
            return;
        }
        if (!alreadyExists) {
            listcell.setLabel(textbox.getValue());
            column.setDisplayName(textbox.getValue());
            popup.close();
        }
    }

    private void checkAggregateButton(Field column, Listcell listcell) {
        if (column.isNumeric()) {
            Measure measure = (Measure) column;
            LOGGER.debug("aggregation -->>{}", measure.getAggregation());
            List<Measure> measures = new ArrayList<Measure>();
            for(Field measureField : table.getTableColumns()){
                if(measureField instanceof Measure){
                    measures.add((Measure)measureField);
                }
            }
            LOGGER.debug("Measures for change aggregate check - {}",measures);
            if(!measure.isRowCount()){
                DashboardUtil.renderAggregateButton(listcell, measure, measures, sortAggregateSync, fieldModel);
            }
        }
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        table = (Table) widgetConfiguration.getWidget();
        comp.addEventListener(ON_LOADING, loadingListener);
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);

        columnListbox.setModel(columns);
        columnListbox.setItemRenderer(tableColumnRenderer);
        columnListbox.addEventListener(Events.ON_DROP, dropListener);

        if (table.isConfigured()) {          
          //Remove the measure and attribute which are not available in the present datasource
            table.removeInvalidFields();            
            columns.addAll(table.getTableColumns());
            // filling sort and column sync map
            List<Measure> measures = new ArrayList<Measure>();
            for (Field columnField : table.getTableColumns()) {
                if (columnField instanceof Measure) {
                    measures.add((Measure) columnField);
                }
            }
            if (table.getSortFields() != null) {
                table.getSortFields().stream().forEach(sortField -> {
                    Measure measure = sortField.getEquivalentMeasure(measures);
                    if (measure != null) {
                        sortAggregateSync.put(measure, sortField);
                    }
                    fieldModel.add(sortField);
                });
            }
        }

        this.getSelf().addEventListener(EVENTS.ON_WIDGET_NOT_CONFIGURED, this::onWidgetNotConfigured);

    }

    private void onWidgetNotConfigured(Event event) {
        if (table.getTableColumns() == null || table.getTableColumns().isEmpty()) {
            Clients.showNotification(Labels.getLabel("dropColumn"), Clients.NOTIFICATION_TYPE_ERROR, columnListbox, Constants.POSITION_END_CENTER,
                    3000, true);
        } else {
            Clients.showNotification(Labels.getLabel("nonAggregationColumn"), Clients.NOTIFICATION_TYPE_ERROR, columnListbox,
                    Constants.POSITION_END_CENTER, 3000, true);
        }
    }

    private void onDropColumnsFromSourceList(DropEvent event) {
        if (event.getDragged() instanceof Treeitem) {
            Treeitem draggedItem = (Treeitem) event.getDragged();
            TreeNode<Field> node = draggedItem.getValue();
            Field field = node.getData();
            if (field.getDisplayName() == null) {
                field.setDisplayName(field.getColumn());
            }
            if (checkOutputs(node.getParent().getData().getColumn())) {
                Measure measure = new Measure(field);
                Attribute attribute = new Attribute(field);
                boolean containsMeasure = false;
                boolean containsAttribute = false;
                for (Field fieldColumn : columns) {
                    if (fieldColumn instanceof Measure) {
                        containsMeasure = fieldColumn.getColumn().equals(measure.getColumn())
                                && ((Measure) fieldColumn).getAggregation() == measure.getAggregation();
                    } else if (fieldColumn instanceof Attribute) {
                        containsAttribute = fieldColumn.getColumn().equals(attribute.getColumn());
                    }
                    if (containsMeasure || containsAttribute) {
                        break;
                    }
                }
                if (!containsMeasure && !containsAttribute) {
                    if (field.isNumeric()) {
                        if (draggedItem.getTree().equals(attributeTree)) {
                            measure.setDataType("unsigned");
                            Clients.showNotification(
                                    "\"" + measure.getColumn() + "\" \n" + Labels.getLabel("willBeHandled"),
                                    Clients.NOTIFICATION_TYPE_WARNING, columnListbox, Constants.POSITION_END_CENTER,
                                    5000, true);
                        }
                        table.addColumn(measure);
                        columns.add(measure);
                    } else {
                        table.addColumn(attribute);
                        columns.add(attribute);
                    }
                } else {
                    Clients.showNotification(Labels.getLabel("columnExists"), Clients.NOTIFICATION_TYPE_ERROR,
                            columnListbox, Constants.POSITION_END_CENTER, 5000, true);
                    return;
                }
            } else {
                Clients.showNotification(Labels.getLabel("sameDatasetShouldbeUsed"), Clients.NOTIFICATION_TYPE_ERROR,
                        columnListbox, Constants.POSITION_END_CENTER, 5000, true);
                return;
            }
        } else {
            Clients.showNotification(Labels.getLabel("tableColumn"), Constants.ERROR,
                    columnListbox, Constants.POSITION_END_CENTER, 5000, true);
            return;
        }
    }
        
}
