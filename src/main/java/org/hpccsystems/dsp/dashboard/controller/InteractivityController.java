package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.ACTION;
import org.hpccsystems.dsp.dashboard.controller.entity.WidgetField;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Interaction;
import org.hpccsystems.dsp.dashboard.entity.widget.InteractionTarget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Window;


public class InteractivityController  extends SelectorComposer<Component>{
    
    private static final String INVALID_INTERACTIVITY = "invalidInteractivity";
    private static final String CHOOSE_FIELD = "chooseField";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractivityController.class);
   
    @Wire
    private Window container;
    @Wire
    private Listbox targetChartsListbox;
    @Wire
    private Combobox  srcWidgetsCombo;
    @Wire
    private Combobox  srcFieldsCombo;
    @Wire
    private Combobox  actions;
    @Wire
    private Label actionLabel;
    private boolean isInteractivityChanged;
    
    @Wire("#srcWidgetsCombo, #srcFieldsCombo")
    List<Combobox> boxes;
    
    private boolean isActionEdited;
    
    private Dashboard dashboard;
    private List<Widget> widgetsList;
    private Component parent;

    private ListModelList<Interaction> interactivityModel = new ListModelList<Interaction>();
    private ListModelList<Dashboard.ACTION> actionModel = new ListModelList<Dashboard.ACTION>();
    
    private ListModelList<Widget> srcWidgetsModel = new ListModelList<Widget>();
    private ListModelList<Field> srcFieldsModel = new ListModelList<Field>();    
    private ListModelList<WidgetField> targetsModel = new ListModelList<WidgetField>();
   
    List<Field> fieldsList = new ArrayList<Field>();
           
    ComboitemRenderer<Field> targetFieldsRenderer = (item, field, index) -> 
        item.setLabel(((Field)field).getColumn());
   
    ListitemRenderer<WidgetField> targetWidgetsRenderer = (item, widget, index) -> 
        targetWidgetCreator(item, widget);

    private void targetWidgetCreator(Listitem item, WidgetField widgetField) throws HipieException {
        Widget widget = widgetField.getWidget();
        Listcell widgetCell = new Listcell(widget.getTitle());
        widgetCell.setIconSclass(widget.getChartConfiguration().getFaIcon());
        item.appendChild(widgetCell);
        
        Listcell fieldCell = new Listcell();

        Combobox combobox = new Combobox();
        combobox.setHflex("1");
        
        if (!widget.getAvailableFields().isEmpty()) {
            ListModelList<Field> targetFieldsModel = new ListModelList<Field>(widget.getAvailableFields());
            combobox.setItemRenderer(targetFieldsRenderer);
            combobox.setModel(targetFieldsModel);
        }
        
       SerializableEventListener<SelectEvent<Component, Field>> selectListener = 
                event -> widgetField.setField(event.getSelectedObjects().iterator().next());
        combobox.addEventListener(Events.ON_SELECT, selectListener);

        fieldCell.appendChild(combobox);
        item.appendChild(fieldCell);
    }

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parentComponent, ComponentInfo compInfo) {
        dashboard =  (Dashboard) Executions.getCurrent().getArg().get(Constants.DASHBOARD);
        parent =  (Component) Executions.getCurrent().getArg().get(Dashboard.PARENT);
        
        if (dashboard.getInteractivities() != null) {
            interactivityModel.addAll(dashboard.getInteractivities());
        }
        
        widgetsList = dashboard.getNonGlobalFilterWidget();
        fillTargetsmodel();
        
        return super.doBeforeCompose(page, parentComponent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        generateActions();
        
        targetChartsListbox.setModel(targetsModel);
        targetChartsListbox.setItemRenderer(targetWidgetsRenderer);
        targetsModel.setMultiple(true);

        ComboitemRenderer<Widget> srcRenderer = (item, widget, index) -> {
            item.setLabel(widget.getName());
            item.setIconSclass(widget.getChartConfiguration().getFaIcon());
        };
        
        srcWidgetsCombo.setItemRenderer(srcRenderer);
        
        srcWidgetsCombo.addEventListener(Events.ON_SELECT, event -> {
            srcFieldsModel.clear();
            Widget widget = srcWidgetsModel.getSelection().iterator().next();
            List<Field> interactivityFields = widget.getInteractivityFields();
            srcFieldsCombo.setModel(srcFieldsModel);
            srcFieldsModel.addAll(interactivityFields);
            srcFieldsCombo.setItemRenderer((item, field, index) -> item.setLabel(((Field) field).getColumn()));
            targetsModel.clear();
            fillTargetsmodel();
            targetsModel.remove(new WidgetField(widget));
        });
        srcWidgetsCombo.setModel(srcWidgetsModel);
        srcWidgetsModel.addAll(widgetsList);
        
        container.addEventListener(Events.ON_CLOSE, event -> discardUnsavedInteractivities());

    }

    private void generateActions() {
        actions.setModel(actionModel);
        actionModel.addAll(Arrays.asList(ACTION.values()));
        ComboitemRenderer<Dashboard.ACTION> actionRenderer = (item, action, index) -> item.setLabel(StringUtils.capitalize(action.name()));
        actions.setItemRenderer(actionRenderer);
    }

    @Listen("onDelete = #interactivityListbox")
    public void deleteInteractivity(ForwardEvent event) {
        Interaction interactivity = (Interaction) event.getData();
        interactivity.delete();
        interactivityModel.remove(interactivity);
        isInteractivityChanged = true;
    }
   
    @Listen("onClick = #addInteractivitySettings")
    public void addInteractivitySettings() {
        
        if(!validateFields()){
            return;
        }
        
        //Setting Click action as default
        ACTION action = ACTION.CLICK;
        if(!actionModel.getSelection().isEmpty()) {
            action = actionModel.getSelection().iterator().next();
        }
        
        Widget sourceWidget = srcWidgetsModel.getSelection().iterator().next();
        Interaction interaction = new Interaction(action, 
                srcFieldsModel.getSelection().iterator().next(), 
                sourceWidget);
        interaction.setDiscard(true);
       
        List<InteractionTarget> targets = new ArrayList<InteractionTarget>();
        targetsModel.getSelection().forEach( selectedTarget -> {
            InteractionTarget target = new InteractionTarget(selectedTarget.getField(), selectedTarget.getWidget(),sourceWidget);
            target.setDiscard(true);
            targets.add(target);
            
        });
        
        interaction.setTargets(targets);
        
        if (!dashboard.getInteractivities().isEmpty()) {
            if (checkDuplicateInteractivity(interaction)) {
                Clients.showNotification("This interactivity already exists", Clients.NOTIFICATION_TYPE_ERROR, actions.getParent(),
                        Constants.POSITION_END_AFTER, 3000);
                clearFields();
                interaction.delete();
                return;
            }
            
            //Checks the source widget and Action is 
            if (isSourceWidgetDuplicate(interaction)) {
                //if source widget is same then checks for duplication of the source column
                if (isSourceFieldDuplicated(interaction)) {
                  //if source field is same then checks for target widgets of the interactivity
                    if (isTargetsDuplicate(interaction)) {
                        Clients.showNotification(Labels.getLabel(INVALID_INTERACTIVITY), Clients.NOTIFICATION_TYPE_ERROR, actions.getParent(),
                                Constants.POSITION_END_AFTER, 3000);
                        clearFields();
                        interaction.delete();
                        return;
                    } else {
                            addDuplicateTargetWidgets(interaction);
                    }
                } else {
                    Clients.showNotification(Labels.getLabel(INVALID_INTERACTIVITY), Clients.NOTIFICATION_TYPE_ERROR, actions.getParent(),
                            Constants.POSITION_END_AFTER, 3000);
                    clearFields();
                    interaction.delete();
                    return;
                }
            } else {
                addInteractivity(interaction);
            }
        } else {
            addInteractivity(interaction);
        }

        clearFields();
    }
    
    /**
     * checks for duplicate source widget from the interactivities list
     * @param interactivity
     * @return
     */
    private boolean isSourceWidgetDuplicate(Interaction newInteraction){
        boolean sourceWidgetUsed = false;
        if(!this.interactivityModel.isEmpty()){
            sourceWidgetUsed =  interactivityModel
                    .stream()
                    .filter(interaction -> interaction.getSourceWidget().equals(newInteraction.getSourceWidget())
                            && interaction.getAction() == newInteraction.getAction())
                    .findAny()
                    .isPresent();
        }
        return sourceWidgetUsed;
    }
    
    /**
     * checks for duplicate source field 
     * @param interactivity
     * @return
     */
    private boolean isSourceFieldDuplicated(Interaction newInteraction) {
        boolean sourceFieldUsed = false;
        if(!this.interactivityModel.isEmpty()){
         List<Interaction> filterdedInteractivities = interactivityModel
                 .stream()
                 .filter(interaction -> interaction.getSourceWidget().equals(newInteraction.getSourceWidget()))
                 .collect(Collectors.toList());
         
          if(!filterdedInteractivities.isEmpty()){
              sourceFieldUsed = filterdedInteractivities
                      .stream()
                      .filter(filtered -> filtered.getField().getColumn().equals(newInteraction.getField().getColumn()))
                      .findAny()
                      .isPresent();
              }
        }        
        return sourceFieldUsed;
    }

    private boolean validateFields(){
        if(isActionEdited && actionModel.getSelection().isEmpty()){
            Clients.showNotification(Labels.getLabel("chooseAnAction"), Clients.NOTIFICATION_TYPE_ERROR, actions.getParent(),Constants.POSITION_TOP_CENTER, 3000);
            return false;
        }
        
        for(Combobox box : boxes) {
            if(box.getSelectedItem() == null){
                Clients.showNotification("Choose a valid option", Clients.NOTIFICATION_TYPE_ERROR, box,Constants.POSITION_END_AFTER, 3000);
                return false;
            }
        }
        if(targetsModel.getSelection().isEmpty()){
            Clients.showNotification(Labels.getLabel("choosetargetWidget"), Clients.NOTIFICATION_TYPE_ERROR, targetChartsListbox,Constants.POSITION_END_AFTER, 3000);
            return false;  
        }
        
        boolean ret = true;
        
        ret = checkChartsList();
        return ret;
    }

    private boolean checkChartsList() {
        for (WidgetField widgetField : targetsModel.getSelection()) {
            if(widgetField.getField() == null) {
                Clients.showNotification(Labels.getLabel(CHOOSE_FIELD), Clients.NOTIFICATION_TYPE_ERROR, targetChartsListbox,Constants.POSITION_END_AFTER, 3000);
                return false;
            }
        }
        return true;
    }
   
    private  void clearFields(){
        actionModel.clearSelection();
        srcWidgetsCombo.setSelectedItem(null);
        srcFieldsCombo.setSelectedItem(null);
       
        targetsModel.clear();
        fillTargetsmodel();
        
        targetChartsListbox.setSelectedItem(null);
        isActionEdited = false;
        actionLabel.setVisible(true);
        actions.setVisible(false);
        actionModel.clearSelection();
    }
    
    @Listen("onClick = #finishInteractivity")
    public void confirmInteractivity() {
        if(dashboard.getInteractivities() == null){
            Clients.showNotification(Labels.getLabel("noInteractivity"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }
        
        LOGGER.debug("interactivities-->{}",dashboard.getInteractivities());
        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
        if(isInteractivityChanged){           
            dashboard.setChanged(true);
        }
        
        resetInteraction();
        Events.postEvent(Dashboard.EVENTS.ON_FINISH_INTERACTIVITY_CONFIG, parent,null);
    }
    
    /**
     * defaults the Interaction/target's 'discard' property
     */
    private void resetInteraction() {
        dashboard.getInteractivities().forEach(interaction -> {
            interaction.setDiscard(false);  
                
            if(interaction.getTargets() != null){
                interaction.getTargets().forEach(target -> target.setDiscard(false));
             }
       });  
    }

    @Listen("onClick = #cancel")
    public void cancelInteractivity() {
        discardUnsavedInteractivities();
        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);

    }
    
    /**
     * Discards the interactivity when the user the clicks cancel button without saving
     * the added interactions
     */
     private void discardUnsavedInteractivities() {
       
        if (dashboard.getInteractivities() != null) {
            dashboard.getInteractivities().forEach(interaction -> {
                if (interaction.canDiscard()) {
                    interaction.delete();
                } else {
                    if (interaction.getTargets() != null) {
                        List<InteractionTarget> targetsToRemove = new ArrayList<InteractionTarget>();
                        for (InteractionTarget target : interaction.getTargets()) {
                            if (target.canDiscard()) {
                                targetsToRemove.add(target);
                            }
                        }
                        //Removes the targets(need to be discarded) from Interaction object
                        interaction.getTargets().removeAll(targetsToRemove);
                        //Removes the target from Target Widget
                        targetsToRemove.forEach(targetToRemove -> targetToRemove.delete(interaction.getSourceWidget()));
                    }
                }
                LOGGER.debug("interaction - >{}", interaction);
            });

        }
    }
    
    private boolean checkDuplicateInteractivity(Interaction interaction){
       return interactivityModel.contains(interaction);
    }

    /**
     * checks duplicate targetWigets 
     * @param interactivity
     * @return
     */
    private boolean isTargetsDuplicate(Interaction newInteraction) {
        boolean targetUsed = false;
        for (Interaction interaction : interactivityModel) {
            if (compareSources(interaction,newInteraction)) {
                for (InteractionTarget curTarget : newInteraction.getTargets()) {
                    if (interaction.getTargets()
                            .stream()
                            .filter(target -> target.equals(curTarget))
                            .findAny()
                            .isPresent()) {
                        return true;
                    } else {
                        targetUsed = false;
                    }
                }
            }
        }
        return targetUsed;
    }
    
    /**
     * adds the target widgets to the already existing interactivity
     * @param interactivity
     */
    private void addDuplicateTargetWidgets(Interaction newInteraction){
        Interaction interactivityToChange = null;
        List<InteractionTarget> newTargets  = null;
        for (Interaction interaction:interactivityModel){
            if(compareSources(interaction, newInteraction)){
                newTargets = new ArrayList<>();
                for(InteractionTarget newTarget :newInteraction.getTargets() ){
                    //If the target is not duplicated add it
                    if(!interaction.getTargets()
                            .stream()
                            .filter(oldTarget -> oldTarget.equals(newTarget))
                            .findAny()
                            .isPresent()){
                        newTargets.add(newTarget); 
                        interactivityToChange = interaction;
                    }else{
                        //if the target is duplicated, remove the target from widget
                        newTarget.delete(newInteraction.getSourceWidget());
                    }
                }
                break;
            }
        }
        if(newTargets != null && !newTargets.isEmpty()){
            try {
                int index = interactivityModel.indexOf(interactivityToChange);
                interactivityToChange.getTargets().addAll(newTargets);
                interactivityModel.remove(index);
                interactivityModel.add(index, interactivityToChange.clone());               
                isInteractivityChanged = true;
                //Remove only the source from the widget,as it is duplicated
                newInteraction.removeSource();
            } catch (CloneNotSupportedException e) {
                LOGGER.error(Constants.EXCEPTION, e);
            }
        } 
    }
        
    private boolean compareSources(Interaction oldSource, Interaction newSource) {
        return oldSource.getAction() == newSource.getAction() 
                && oldSource.getSourceWidget().equals(newSource.getSourceWidget())
                && oldSource.getField().getColumn().equals(newSource.getField().getColumn());
    }
    
    private void addInteractivity(Interaction interaction){
        interactivityModel.add(interaction);
        isInteractivityChanged = true;
    }
    
    @Listen("onClose = #container")
    public void closeWidgetConfiguration(Event event) {
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Close button Clicked.!");
        }
        Events.postEvent(Dashboard.EVENTS.ON_INTERACTIVITY_CONFIG_CLOSE, parent, null);
    }
    
    @Listen("onClick = #editAction")
    public void editAction() {
        isActionEdited = true;
        actionLabel.setVisible(false);
        actions.setVisible(true);
    }

    public ListModelList<Interaction> getInteractivityModel() {
        return interactivityModel;
    }

    private void fillTargetsmodel() {
        widgetsList.forEach(widget -> targetsModel.add(new WidgetField(widget)));
    }
}
