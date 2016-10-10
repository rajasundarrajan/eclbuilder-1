package org.hpccsystems.dsp.ramps.component.renderer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.GridEntity;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartInfo;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public class CompositionRowRenderer implements RowRenderer<GridEntity>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String IMG_BTN = "img-btn";
    private static final String EDIT_ICON = "z-icon-edit";
    private static final String CLONE_ICON = "z-icon-copy";
    private static final String DELETE_ICON = "z-icon-trash-o";
    private static final String FAV_ICON = "fa fa-star-o";
    private static final String UN_FAV_ICON = "fa fa-star";
    private static final String FAV_ICON_CSS = "img-btn-fav";
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionRowRenderer.class);
    User user=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
    private Component postTo;
    String compositionServiceString = "compositionService";
    String container = "container";
    String homeTabbox = "homeTabbox";

    public CompositionRowRenderer(Component postTo) {
        this.postTo = postTo;
    }

    @Override
    public void render(final Row row, final GridEntity gridEntity, int index) throws Exception {
        boolean isProject = gridEntity instanceof Project;
        Label projectName = new Label();
        projectName.setValue(gridEntity.getLabel());
        projectName.setSclass("hand-cursor");
        
        Composition composition=null;
        try{
            //fetches composition for showing the plugin list and chart list on tooltip
            //Handle the exception DSP should not crash on login
            composition = gridEntity.getComposition();
            List<Plugin> plugins =HIPIEUtil.getOrderedPlugins(composition);
            if(isProject){
                projectName.setTooltiptext(constructProjectTooltip(plugins));
            }else{
                projectName.setTooltiptext(constructDashboardTooltip(gridEntity,composition));
            }
        }catch(Exception ex){
            LOGGER.error("Error occured while fetching composition with canonical name {},{}",gridEntity.getCanonicalName(),ex);
        }
          
        createCheckbox(row, gridEntity);
        
        row.appendChild(projectName);

        row.appendChild(new Label(new SimpleDateFormat(Constants.DATE_FORMAT).format(gridEntity.getLastModifiedDate())));
        
        row.appendChild(new Label(gridEntity.getAuthor()));

        Hbox actionBox = new Hbox();
        
        if (user.canViewProject()) {
            projectName.setStyle("cursor:pointer;color:#3B78EF");
            projectName.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onEvent(Event event) throws Exception {
                    Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, postTo,
                            isProject ? new TabData((Project) gridEntity, Flow.VIEW) 
                                :new DashboardConfig((Dashboard) gridEntity, Flow.VIEW));

                }
            });
        }
      
        createActionButtons(row, gridEntity, isProject, actionBox);

        actionBox.setParent(row);
    }

    private void createCheckbox(final Row row, final GridEntity gridEntity) {
        if (HipieSingleton.canPromote()) {
            Checkbox checkbox = new Checkbox();
            if (!user.getId().equals(gridEntity.getAuthor()) && !user.isGlobalAdmin()) {
                checkbox.setDisabled(true);
            }
            checkbox.addEventListener(Events.ON_CHECK, event -> {
                LOGGER.debug("on click on check box--{}", ((CheckEvent) event).isChecked());
                if (((CheckEvent)event).isChecked()) {
                    Events.postEvent(EVENTS.ON_SELECT_ENTITY, postTo, gridEntity);
                } else {
                    Events.postEvent(EVENTS.ON_REMOVE_ENTITY, postTo, gridEntity);
                }
            });
            row.appendChild(checkbox);
        }
    }

    private void createActionButtons(final Row row, final GridEntity gridEntity, boolean isProject, Hbox actionBox) {
        if (user.canEdit()) {
            Button editButton = new Button();
            editButton.setTooltiptext(Labels.getLabel("dspEdit"));
            editButton.setZclass(IMG_BTN);
            editButton.setIconSclass(EDIT_ICON);
            editButton.setStyle("color: orange; cursor: pointer;");
            editButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
                private static final long serialVersionUID = 1L;
                @Override
                public void onEvent(Event event) throws Exception {

                    Events.postEvent(EVENTS.ON_CLICK_VIEW_OR_EDIT, postTo, 
                            isProject ? new TabData((Project) gridEntity, Flow.EDIT) 
                                    :new DashboardConfig((Dashboard) gridEntity, Flow.EDIT));
                }

            });
            editButton.setParent(actionBox);
        }

        if (user.canCreate()) {
            Button cloneButton = new Button();
            cloneButton.setTooltiptext(Labels.getLabel("clone"));
            cloneButton.setZclass(IMG_BTN);
            cloneButton.setIconSclass(CLONE_ICON);
            cloneButton.setStyle("color: blue; cursor: pointer;");
            cloneButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
                private static final long serialVersionUID = 1L;
                @Override
                public void onEvent(Event arg0) throws Exception {

                    Events.postEvent(EVENTS.ON_CLICK_CLONE, postTo,
                            isProject ? new TabData(((Project) gridEntity).clone(), Constants.Flow.CLONE, gridEntity.getComposition())
                                    : new DashboardConfig(((Dashboard) gridEntity).clone(), Constants.Flow.CLONE, gridEntity.getComposition()));
                }

            });
            cloneButton.setParent(actionBox);
        }
        if (user.canEdit()) {
            Button deleteButton = new Button();
            deleteButton.setTooltiptext(Labels.getLabel("delete"));
            deleteButton.setZclass(IMG_BTN);
            deleteButton.setIconSclass(DELETE_ICON);
            deleteButton.setStyle("color: red; cursor: pointer;");
            deleteButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<MouseEvent>() {
                private static final long serialVersionUID = 1L;
                @Override
                public void onEvent(MouseEvent event) throws Exception {
                    deleteComposition(gridEntity, row);

                }
            });
            deleteButton.setParent(actionBox);
        }
        if (user.canEdit()) {
            Button favButton = new Button();  
            favButton.setTooltiptext(Labels.getLabel("favorite"));
            favButton.setZclass(IMG_BTN +" "+ FAV_ICON_CSS);
            
            if(gridEntity.getIsFavourite()) {
                favButton.setIconSclass(UN_FAV_ICON);
            }else {
                favButton.setIconSclass(FAV_ICON);
            }
            
            favButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<MouseEvent>() {
                private static final long serialVersionUID = 1L;
                @Override
                public void onEvent(MouseEvent event) throws Exception {
                   favoriteComposition(gridEntity, row);
                    if(gridEntity.getIsFavourite()) {
                        favButton.setIconSclass(FAV_ICON);
                    }else {
                        favButton.setIconSclass(UN_FAV_ICON);
                    }   
                }
            });
            favButton.setParent(actionBox);
        }
    }

    private String constructDashboardTooltip(GridEntity gridEntity,Composition composition) {
        StringBuilder toolTip = new StringBuilder("Charts:\n");
       ContractInstance visualizationCI = CompositionUtil.getVisualizationContractInstance(composition);
           List<ChartInfo> charts = DashboardUtil.retrieveChartInfo(visualizationCI);
           ((Dashboard)gridEntity).setCharts(charts);
            if(!charts.isEmpty()){
                charts.forEach(chart ->
                    toolTip.append(chart.getName()).append("\n")
                );
       }
      
        return toolTip.toString();
    }

    private String constructProjectTooltip(List<Plugin> plugins) {
        StringBuilder toolTip = new StringBuilder("Plugins:\n");
        plugins.forEach(plugin -> 
            toolTip.append(plugin.getLabel()).append("\n")
        );
        
        return toolTip.toString();        
    }

    protected void deleteComposition(final GridEntity gridEntity, final Row row) {
        Events.postEvent(EVENTS.ON_DELETE_COMPOSITION, postTo, gridEntity);
    }
    
    protected void favoriteComposition(final GridEntity gridEntity, final Row row) {
        Events.postEvent(EVENTS.ON_FAV_COMPOSITION, postTo, gridEntity);
    }

}
