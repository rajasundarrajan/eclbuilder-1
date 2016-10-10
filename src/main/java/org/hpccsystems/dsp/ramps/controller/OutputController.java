package org.hpccsystems.dsp.ramps.controller;

import java.util.Set;

import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class OutputController extends SelectorComposer<Component> {

    private static final String PERSONALIZED_VIEW = "personalizedView";
    private static final String DEFAULT_VIEW = "defaultView";

    private static final String POINTER_EVENTS = "pointer-events:none;border: none;";

    private static final String SHOW_PROPERTIES_FALSE = "showProperties(false,'";

    private static final long serialVersionUID = 1L;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OutputController.class);

    private Process process;
    private String visualizationDDL;
    
    @Wire
    private Div chartHolder;

    @Wire
    private Button layoutSave;
    
    @Wire
    private Button editLayout;
    
    @Wire
    private Button closeLayout;
    
    @Wire
    private Combobox gcidCombobox;
    @Wire
    private Hlayout gcidBox;
    
    @Wire 
    private Listcell saveAsDefaultView;
    
    @Wire 
    private Listcell saveAsPersonalizedView;
    
    @Wire
    private Combobutton viewToggleButton;
    
    @Wire
    private Listcell otherViewBtn;
    
    private static enum Action {
        SAVE_AS_PUBLIC, SAVE_AS_PRIVATE
    } 
    private boolean isPublicView;
    private String userId;
        
    private int activeGCID; 
    private Action saveAsAction;
        
    private ListModelList<Integer> gcidModel = new ListModelList<Integer>();
    boolean canEdit;
    private boolean isPrivateView;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        visualizationDDL = (String) Executions.getCurrent().getArg().get(Constants.VISUALIZATION_DDL);
        process = (Process) Executions.getCurrent().getArg().get(Constants.PROCESS);
        
        userId = ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();

        //If the user have personal layouts retrieving it  else retrieve default layout
        String layout = null;
        String personalLayout =  retrieveLayout();
        setPrivateView(personalLayout != null);
        
        if(isPrivateView) {
            //loading personal view
            layout = personalLayout;
            viewToggleButton.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
            otherViewBtn.setLabel(Labels.getLabel(DEFAULT_VIEW));
        } else {
            //Loading default view
            layout = getLayout(false);
        }
        toggleSaveButtons();
        
        canEdit = RampsUtil.currentUserCanEdit(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), process.getCompositionInstance().getComposition());
        
        renderDashboard(layout);
        
        this.getSelf().addEventListener(Constants.EVENTS.ON_CONFIRM_GCID,(SerializableEventListener<? extends Event>) this::doAfterGCIDSelection );
        
        ComboitemRenderer<Integer> gcidItemRenderer = (comboitem, gcid, index) ->  comboitem.setLabel(gcid == 0 ? "Layout without GCID" : gcid.toString());
        gcidCombobox.setItemRenderer(gcidItemRenderer);
        
        chartHolder.addEventListener("onSave", (SerializableEventListener<? extends Event>)this::saveLayout);
        
      //Event trigger when user communicate with visualization elements 
        chartHolder.addEventListener("onSessionAlive", (SerializableEventListener<? extends Event>)this::sessionLive);
    }
    private void sessionLive(Event event) {
        LOGGER.debug((String) event.getData());
     }
    private void saveLayout(Event event) {
        boolean success = false;
        if(event.getData() != null) {

            try {
                DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
                
                if (Action.SAVE_AS_PRIVATE == saveAsAction) {
                    success = dermatologyService.saveLayout(userId, process.getCompositionInstance().getCompositionId(), 
                            getCompVersion(), visualizationDDL, activeGCID, event.getData().toString());
                } else {
                    success = dermatologyService.saveLayout(Constants.GENERIC_USER, process.getCompositionInstance().getCompositionId(), 
                            getCompVersion(), visualizationDDL, activeGCID, event.getData().toString());
                }
            } catch (DermatologyException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }

            if (success) {
                Clients.showNotification(Labels.getLabel("layoutSaved"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
                if(!gcidModel.contains(activeGCID)) {
                    gcidModel.add(activeGCID);
                }
                
                gcidModel.addToSelection(activeGCID);
                showGCIDSelector();
            } else {
                Clients.showNotification(Labels.getLabel("unableToSaveLayout"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
            }
            
        }else{
            Clients.showNotification(Labels.getLabel("dontHaveEditLayoutPermission"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, Constants.MESSAGE_VIEW_TIME, true);
        }
        viewToggleButton.setVisible(isPrivateViewAvailable());
    }
    
    private String getCompVersion() {
        return process.getCompositionInstance().getComposition().getVersion();
    }

    private CompositionService getCompositionService() {
        return (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
    }

    private String retrieveLayout() {
        String layout = null;
        try {
            CompositionService compositionService = getCompositionService();
            Set<Integer> layoutGCIDs = compositionService.getLayoutGCIDS(process.getCompositionInstance().getCompositionId(), userId, visualizationDDL);
            LOGGER.debug("GCID's for compostion - {}", layoutGCIDs);
            gcidModel.clear();
            gcidModel.addAll(layoutGCIDs);
            
            if(!gcidModel.isEmpty()){
                //Selecting first GCID to get layout
                activeGCID = layoutGCIDs.iterator().next();
                gcidModel.addToSelection(activeGCID);
            }
            
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            layout = dermatologyService.getLayout(userId, process.getCompositionInstance().getCompositionId(), 
                    getCompVersion(), visualizationDDL, activeGCID);
            
            //Adding 0 for layout without GCID
            if(!gcidModel.contains(0)) {
                gcidModel.add(Integer.valueOf(0));
            }
            
            showGCIDSelector();
        } catch (DermatologyException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
        }
        return layout;
    }
    
    @Listen("onClick = #editLayout")
    public void editLayout() {
        layoutSave.setVisible(true);
        closeLayout.setVisible(true);
        editLayout.setVisible(false);
        viewToggleButton.setStyle(POINTER_EVENTS);
        Clients.evalJavaScript("showProperties(true,'" + chartHolder.getUuid() + "')");
    }
    
    private void doAfterGCIDSelection(Event event) {
        Company selectedGcid = (Company) event.getData();
        activeGCID = selectedGcid.getGcId().intValue();
        
        initiateLayoutSave();
    }
    
    @Listen("onClick = #layoutSave")
    public void initiateLayoutSave() {
        setSaveAsAction(isPrivateView() ? Action.SAVE_AS_PRIVATE : Action.SAVE_AS_PUBLIC);
        triggerJS();

    }
    
    private void triggerJS() {
        editLayout.setVisible(true);
        layoutSave.setVisible(false);
        closeLayout.setVisible(false);
        viewToggleButton.setStyle(POINTER_EVENTS);
        
        Clients.evalJavaScript(SHOW_PROPERTIES_FALSE + chartHolder.getUuid() + "')");
        Clients.evalJavaScript("saveDashboardLayout('" + chartHolder.getUuid() + "')");
        
        toggleSaveButtons();
    }
    
    @Listen("onClick = #saveGCIDLayout")
    public void selectGCID() {
        Window window = (Window) Executions.createComponents("dashboard/select_layout_gcid.zul", this.getSelf(), null);
        window.doModal();
    }

    @Listen("onClick = #saveAsDefaultView")
    public void onclickSaveasDefault() {
        setPrivateView(false);
        toggleSaveButtons();
        setSaveAsAction(Action.SAVE_AS_PUBLIC);
        triggerJS();
    }
    
    @Listen("onClick = #saveAsPersonalizedView")
    public void onclickSaveasPrivate() {
        setPrivateView(true);
        toggleSaveButtons();
        setSaveAsAction(Action.SAVE_AS_PRIVATE);
        triggerJS();
    }
    
    @Listen("onClick = #closeLayout")
    public void closeLayoutDiv(){
        layoutSave.setVisible(false);
        editLayout.setVisible(true);
        closeLayout.setVisible(false);
        viewToggleButton.setStyle(POINTER_EVENTS);
        Clients.evalJavaScript(SHOW_PROPERTIES_FALSE + chartHolder.getUuid() + "')");
    }
    
    /**
     * Making selectors visible only when at least one non default GCID has a layout
     */
    private void showGCIDSelector() {
        if(gcidModel.size() > 1 || 
                (gcidModel.size() == 1 && !gcidModel.contains(0))){
            gcidBox.setVisible(true);
        } else {
            gcidBox.setVisible(false);
        }
    }
    
    @Listen("onSelect = #gcidCombobox")
    public void getGCIDLayout(Event event) {
        int selectedGCID = gcidModel.getSelection().iterator().next();
        activeGCID = selectedGCID;
        LOGGER.debug("selectedGcid -->{}",selectedGCID);
        
        String personalLayout = getLayout(true);
        setPrivateView(personalLayout != null);
        
        String layout = isPrivateView() ? personalLayout : getLayout(false);
        toggleSaveButtons();
        
        renderDashboard(layout);
    }

    private void renderDashboard(String layout) {
        RampsUtil.renderDashboard(process.getCompositionInstance(), visualizationDDL, chartHolder.getUuid(), null, layout, canEdit);
    }
    
    public boolean canEdit() {
        return canEdit;
    }
    
    @Listen("onClick = #otherViewBtn")
    public void toggleViewButton(){
        String layout = null;
        if (isPrivateView()) {
            // change to public view
            renderDashboard(getLayout(false));
        } else {
            // Change to private view
            layout = getLayout(true);
            if (layout == null || layout.isEmpty()) {
                Clients.showNotification("No personal layouts to view", Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
            renderDashboard(layout);
        }
        setPrivateView(!isPrivateView());
        toggleSaveButtons();
    }
    
    private String getLayout(boolean isPrivate) {
        try {
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            return dermatologyService.getLayout(isPrivate ? userId : null, process.getCompositionInstance().getCompositionId(), 
                    getCompVersion(), visualizationDDL, activeGCID);
        } catch (DermatologyException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return null;
        }
    }
    
    private void toggleSaveButtons() {
        saveAsDefaultView.setVisible(isPrivateView());
        saveAsPersonalizedView.setVisible(!isPrivateView());
        
        if(isPrivateView()) {
            viewToggleButton.setVisible(true);
            viewToggleButton.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
            otherViewBtn.setLabel(Labels.getLabel(DEFAULT_VIEW));
        } else {
            viewToggleButton.setLabel(Labels.getLabel(DEFAULT_VIEW));
            otherViewBtn.setLabel(Labels.getLabel(PERSONALIZED_VIEW));
        }
    }
    
    public boolean isPrivateViewAvailable() {
        String layout = getLayout(true);
        return layout != null && !layout.isEmpty();
    }
    
    public Action getSaveAsAction() {
        return saveAsAction;
    }

    public void setSaveAsAction(Action saveAsAction) {
        this.saveAsAction = saveAsAction;
    }
    public ListModelList<Integer> getGcidModel() {
        return gcidModel;
    }

    public void setGcidModel(ListModelList<Integer> gcidModel) {
        this.gcidModel = gcidModel;
    }
    
    public boolean isPublicView() {
        return isPublicView;
    }

    public void setPublicView(boolean isPublicView) {
        this.isPublicView = isPublicView;
    }

    public boolean isPrivateView() {
        return isPrivateView;
    }

    public void setPrivateView(boolean isPrivateView) {
        this.isPrivateView = isPrivateView;
    }
}
