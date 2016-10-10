package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.ScoredSearchFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.ScoredSearch;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;

public class ScoredSearchController extends ConfigurationComposer<Component> {
    private static final String UPDATE_MODIFIER = "updateModifier";
    private static final long serialVersionUID = 1L;
    private static final String ON_LOADING = "onLoading";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoredSearchController.class);
    
    private ScoredSearch scoredSearch;
    
    @Wire
    private Listbox fieldsListbox;
    
    @Wire
    private Listbox filterListbox;
    
    private List<Field> fields;
    
    
    private ListModelList<ScoredSearchFilter> scoredSearchFilters;
    private ListitemRenderer<ScoredSearchFilter> scoredSearchFieldRenderer = (listitem, scoredfield, index) -> 
    scoredSearchRenderer(listitem, scoredfield);
    
    private ListitemRenderer<Field> fieldsRenderer = (listitem, field, index) -> {
        listitem.setLabel(field.getColumn());
        listitem.setDraggable(Dashboard.TRUE);
        listitem.setValue(field);
    };
    
    SerializableEventListener<Event> scoreLoadingListener = event -> {

        fields = RampsUtil. getFileFields(widgetConfiguration.getWidget().getDatasource());

        fieldsListbox.setModel(new ListModelList<Field>(fields));
        fieldsListbox.setItemRenderer(fieldsRenderer);

    };

    private void scoredSearchRenderer(Listitem listitem, ScoredSearchFilter scoredfield) {
        Listcell listCellOne = new Listcell();
           Listcell listCellTwo = new Listcell();
           Listcell listCellThree = new Listcell();
           Listcell listCellFour = new Listcell();
           Listcell listCellFive = new Listcell();
           Listcell listCellSix = new Listcell();
             Label label = new Label(scoredfield.getColumn());
             listCellOne.appendChild(label);
             
             Combobox operators = new Combobox();
             List<String> operandsList = new ArrayList<String>();
             operandsList.add("=");
             operandsList.add(">");
             operandsList.add("<");
             operandsList.add(">=");
             operandsList.add("<=");
             
             ListModelList<String> operatorList = new ListModelList<String>(operandsList); 
             if (scoredfield.getOperator() != null) {
                 List<String> listOfOperators = new ArrayList<String>();
                 listOfOperators.add(scoredfield.getOperator());
                 operatorList.setSelection(listOfOperators);         
             }
             operators.setHflex("1");
             operators.setReadonly(true);
             operators.setModel(operatorList);
             operators.addEventListener(Events.ON_SELECT, (SerializableEventListener<? extends Event>)event -> scoredfield.setOperator(operatorList.getSelection().iterator().next()));
             listCellTwo.appendChild(operators);
             
             Textbox valueOne = new Textbox();
             valueOne.setText(scoredfield.getOperatorValue());     
             valueOne.setHflex("1");
             valueOne.addEventListener(Events.ON_CHANGE,(SerializableEventListener<? extends Event>) event -> scoredfield.setOperatorValue(valueOne.getText()));
             listCellThree.appendChild(valueOne);
             
             Combobox modifierOperators = new Combobox();
             modifierOperators.setHflex("1");
             modifierOperators.setReadonly(true);
             List<String> listOfModifiers = new ArrayList<String>();
             listOfModifiers.add("*");
             listOfModifiers.add("/");
             ListModelList<String> modifierList = new ListModelList<String>(listOfModifiers); 
             if (scoredfield.getModifier() != null) {
                 List<String> modifiers = new ArrayList<String>();
                 modifiers.add(scoredfield.getModifier());
                 modifierList.setSelection(modifiers);
             }
             modifierOperators.setModel(modifierList);
             modifierOperators.addEventListener(Events.ON_SELECT,(SerializableEventListener<? extends Event>) event -> scoredfield.setModifier(modifierList.getSelection().iterator().next()));
             listCellFour.appendChild(modifierOperators);
             
             Textbox valueTwo = new Textbox();
             valueTwo.setText(scoredfield.getModifierValue());
             valueTwo.setHflex("1");
             valueTwo.addEventListener(Events.ON_CHANGE, (SerializableEventListener<? extends Event>)event ->
                 scoredfield.setModifierValue(valueTwo.getText())
             );
             listCellFive.appendChild(valueTwo);
             
             Button closeButton = new Button();
             closeButton.setIconSclass("fa fa-trash");
             closeButton.setSclass("img-btn xml-delete-btn");
             listCellSix.appendChild(closeButton);
             
             listitem.appendChild(listCellOne);
             listitem.appendChild(listCellTwo);
             listitem.appendChild(listCellThree);
             listitem.appendChild(listCellFour);
             listitem.appendChild(listCellFive);
             listitem.appendChild(listCellSix);
             
             closeButton.addEventListener("onClick", (SerializableEventListener<? extends Event>)event -> {
                 scoredSearchFilters.remove(scoredfield);  
                 scoredSearch.getScoredSearchfilters().remove(scoredfield);
             });
    }
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        scoredSearch = (ScoredSearch) widgetConfiguration.getWidget();
        if(scoredSearch.getScoredSearchfilters() == null){
            scoredSearch.setScoredSearchfilters(new ArrayList<ScoredSearchFilter>());
        }
        scoredSearchFilters = new ListModelList<ScoredSearchFilter>(scoredSearch.getScoredSearchfilters()); 
        comp.addEventListener(ON_LOADING, scoreLoadingListener);
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);
        
        //Remove the filter columns which are not available in the present datasource
        if(scoredSearch.isConfigured()){
            scoredSearch.removeInvalidFields();
        }
        
        filterListbox.setModel(scoredSearchFilters);
        filterListbox.setItemRenderer(scoredSearchFieldRenderer);
        Clients.clearBusy(comp);
        
        
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scored search Configuration - {}", scoredSearch);
        }
        
        this.getSelf().addEventListener(EVENTS.ON_WIDGET_NOT_CONFIGURED, (SerializableEventListener<? extends Event>)this::onWidgetNotConfigured);
    }

    private void onWidgetNotConfigured(Event event) {
        if(scoredSearch.getScoredSearchfilters() != null){
            for(ScoredSearchFilter filter : scoredSearchFilters) {
              
                if ((StringUtils.isEmpty(filter.getOperator()) && StringUtils.isEmpty(filter.getOperatorValue()))
                        && (!StringUtils.isEmpty(filter.getModifier()) && StringUtils.isEmpty(filter.getModifierValue()))) {

                    Clients.showNotification(Labels.getLabel(UPDATE_MODIFIER), Constants.ERROR,
                            filterListbox.getItems().get(scoredSearchFilters.indexOf(filter)), Constants.POSITION_END_CENTER, 3000, true);
                    return;
                }
                
                if ((StringUtils.isEmpty(filter.getOperator()) && StringUtils.isEmpty(filter.getOperatorValue()))
                        && (StringUtils.isEmpty(filter.getModifier()) && !StringUtils.isEmpty(filter.getModifierValue()))) {
                    Clients.showNotification(Labels.getLabel(UPDATE_MODIFIER), Constants.ERROR,
                            filterListbox.getItems().get(scoredSearchFilters.indexOf(filter)), Constants.POSITION_END_CENTER, 3000, true);
                    return;

                }
                
                if ((StringUtils.isEmpty(filter.getOperator()) || StringUtils.isEmpty(filter.getOperatorValue()))
                        && (StringUtils.isEmpty(filter.getModifier()) || StringUtils.isEmpty(filter.getModifierValue()))) {
                    Clients.showNotification(Labels.getLabel("updateOperator"), Constants.ERROR,
                            filterListbox.getItems().get(scoredSearchFilters.indexOf(filter)), Constants.POSITION_END_CENTER, 3000, true);
                    return;
                }
            }
        }
    }
    
    @Listen("onDrop = #filterListbox")
    public void onDropFields(DropEvent event) {
        Listitem draggedItem = (Listitem) event.getDragged();
        Field field = draggedItem.getValue();
        ScoredSearchFilter scoredSearchFilter = new ScoredSearchFilter(field);
        if (scoredSearch.getScoredSearchfilters().contains(scoredSearchFilter)){
            Clients.showNotification(field.getColumn()+" already exists", "error", this.getSelf(), Constants.POSITION_MIDDLE_CENTER, 3000, true);
            return;
        }
        scoredSearchFilters.add(scoredSearchFilter);
        scoredSearch.getScoredSearchfilters().add(scoredSearchFilter);
    }

}
