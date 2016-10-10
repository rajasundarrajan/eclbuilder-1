package org.hpccsystems.dsp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.utils.CompositionPromoter;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.MigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Anchorchildren;
import org.zkoss.zul.Anchorlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class HomeComposer extends SelectorComposer<Component> implements EventListener<Event>{

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeComposer.class);

    
    @Wire
    protected Anchorlayout thumbnailLayout;
    
    @Wire
    protected Grid entityList;
    
    @Wire
    protected Button promoteBtn;
    
    @WireVariable
    private Desktop desktop;
    
    protected List<GridEntity> promotionEntities;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        if(HipieSingleton.canPromote()) {
            promoteBtn.setVisible(true);
        }
        
        thumbnailLayout.addEventListener(EVENTS.ON_SELECT_ENTITY, event -> addToSlection((GridEntity)event.getData()));
        thumbnailLayout.addEventListener(EVENTS.ON_REMOVE_ENTITY, event -> removeFromSlection((GridEntity)event.getData()));
    }
    
    
    @Listen("onClick = #promoteBtn")
    public void promote() {
        //Validations
        if(CollectionUtils.isEmpty(promotionEntities)) {
            Clients.showNotification(Labels.getLabel("promoteNoEntry"), Clients.NOTIFICATION_TYPE_WARNING, promoteBtn,
                    Constants.POSITION_AFTER_CENTER, 3000, true);
            return;
        }
        
        //Transferring selections
        List<Composition> compositions = new ArrayList<>();
        for (GridEntity entity : promotionEntities) {
            try {
                compositions.add(entity.getComposition());
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(Labels.getLabel("promoteProcessBlocked"), Clients.NOTIFICATION_TYPE_WARNING, promoteBtn,
                        Constants.POSITION_AFTER_CENTER, 3000, true);
                return;
            }
        }
        
        //Setting style
        promoteBtn.setSclass("promoting");
        promoteBtn.setDisabled(true);
        promoteBtn.setLabel(Labels.getLabel("promoting"));
        
        clearSelections();
        
        desktop.enableServerPush(true);
        MigrationService migrationService = (MigrationService) SpringUtil.getBean("migrationService");
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        String userId=authenticationService.getCurrentUser().getId();
        String sessionId= ((HttpSession) Sessions.getCurrent().getNativeSession()).getId();
        CompositionPromoter compositionPromoter = new CompositionPromoter(compositions, migrationService, desktop, this,userId,sessionId);
        DSPExecutorHolder.getExecutor().execute(compositionPromoter);
    }
    
    private void addToSlection(GridEntity entity) {
        LOGGER.debug("Adding selection");
        if(promotionEntities == null) {
            promotionEntities = new ArrayList<>();
        }
        promotionEntities.add(entity);
        
        togglePromoteButton();
    }

    private void removeFromSlection(GridEntity entity) {
        LOGGER.debug("Removing selection");
        promotionEntities.remove(entity);
        togglePromoteButton();
    }
    
    private void togglePromoteButton() {
        if(CollectionUtils.isNotEmpty(promotionEntities)) {
            promoteBtn.setSclass("promote-btn green-promote");
        } else {
            promoteBtn.setSclass("promote-btn");
        }
    }
    
    protected void clearSelections() {
        //Clearing selections
        entityList.getRows()
            .getChildren()
            .forEach(row -> {
                Row comp = (Row) row;
                LOGGER.debug("Checkinh status - {}", (GridEntity)comp.getValue());
               if( row.getFirstChild() instanceof Checkbox) {
                   Checkbox check = (Checkbox) row.getFirstChild();
                   check.setChecked(false);
               }
            });
        for (Component layout : thumbnailLayout.getChildren()) {
                if(layout instanceof Anchorchildren) {
                    Anchorchildren child = (Anchorchildren) layout;
                    child.setSclass("gridAnchor");
                }
        }
        
        if(CollectionUtils.isNotEmpty(promotionEntities)) {
            promotionEntities.clear();
        }
        togglePromoteButton();
    }
    
    @Override
    public void onEvent(Event event) throws Exception {
        promoteBtn.setDisabled(false);
        promoteBtn.setLabel(Labels.getLabel("promote"));
        
        if(EVENTS.ON_PROMOTION_FAIL.equals(event.getName())) {
            Clients.showNotification(event.getData() == null ? Labels.getLabel("promoteFailed") : event.getData().toString(), 
                    Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 3000,true);
            return;
        }

        @SuppressWarnings("unchecked")
        Set<Composition> failedCompositions = (Set<Composition>) event.getData();
        if (failedCompositions.isEmpty()) {
            Clients.showNotification(Labels.getLabel("promoteComplte"), Clients.NOTIFICATION_TYPE_INFO,getSelf().getParent(), Constants.POSITION_TOP_CENTER, 3000, true);
        } else if (promotionEntities.size() == failedCompositions.size()) {
            Clients.showNotification(Labels.getLabel("promoteFailed"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 3000, true);
        } else {
            StringBuilder failedCompositionName = new StringBuilder();
            Iterator<Composition> itr = failedCompositions.iterator();
            boolean first = true;
            while (itr.hasNext()) {
                if (first) {
                    failedCompositionName.append(itr.next().getName());
                    first = false;
                } else {
                    failedCompositionName.append("," + itr.next().getName());
                }
            }
            Clients.showNotification(Labels.getLabel("partialPromoteComplete") + " " + failedCompositionName, Clients.NOTIFICATION_TYPE_WARNING, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 3000,true);
        }
    }
    
}
