package org.hpccsystems.dsp.dashboard.controller;

import java.util.Date;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

public class AdvancedModeController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedModeController.class);
    private static final String VISUALIZE = "VISUALIZE";
    private static final String VERSION = "0.1";
    
    @Wire
    private Window advancedModeContainer;
    
    @Wire
    private Tab dudTab;
    
    @Wire
    private Tab cmpTab;
    
    @Wire
    private Tabpanel dudTabPanel;
    
    @Wire
    private Tabpanel cmpTabPanel;
    
    @Wire
    private Textbox dudTextbox;
    
    @Wire
    private Textbox cmpTextbox;
        
    private DashboardConfig dashboardConfig;
    private Component parent;
    
    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parentComponent, ComponentInfo compInfo) {
    	dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
    	parent = (Component) Executions.getCurrent().getArg().get(Dashboard.PARENT);
    	
    	return super.doBeforeCompose(page, parentComponent, compInfo);
    }
    
    @Override 
    public void doAfterCompose(Component comp) throws Exception {
    	super.doAfterCompose(comp);
    	
    	if (!((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).isAdvancedMode(dashboardConfig.getComposition())) {
    	    addAdvancedModeFlag();
    	}
    	
    	// populate textboxes
    	populateTextboxes();
        
        dudTab.setSelected(true);
        
        dashboardConfig.getHomeTabbox().addEventListener(EVENTS.ON_CHANGE_LABEL, this::updateLabel);
    }
    
    private void updateLabel(Event event) {
        populateTextboxes();
    }
	
    /**
     * Adds _MODE="ADVANCED" to the contract instance and then saves the composition
     */
    private void addAdvancedModeFlag() {
        try {
            Composition comp = dashboardConfig.getComposition();
            ContractInstance ci = dashboardConfig.getComposition().getContractInstanceByName(dashboardConfig.getComposition().getName() + Dashboard.CONTRACT_IDENTIFIER);
            ci.setProperty(Dashboard.DASHBOARD_MODE, Dashboard.DASHBOARD_MODE_ADVANCED);
            
            // Update compositions file text.
            comp.setFileText(comp.toString());
            
            // Save the dashboard
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).updateDashboard(dashboardConfig.getDashboard(), ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), dashboardConfig.getComposition());
            
            // Update the saved time
            LOGGER.debug("dashboard time -->{}", new Date(dashboardConfig.getComposition().getLastModified()));
            dashboardConfig.getComposition().setLastModified(new Date().getTime());
            dashboardConfig.getDashboard().setLastModifiedDate(new Date(dashboardConfig.getComposition().getLastModified()));

        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
    }

    private void populateTextboxes() {
        dudTextbox.setValue(getDud());
        cmpTextbox.setValue(getCmp());
    }
	
    /**
     * Gets the contents of the dude file.
     * @return - Dude string
     */
    private String getDud() {
        
        String dudContents = null;
        
        try {
            ContractInstance ins = dashboardConfig.getComposition().getContractInstanceByName(dashboardConfig.getComposition().getName() + Dashboard.CONTRACT_IDENTIFIER);
            
            if (ins == null) {
                ins = dashboardConfig.getComposition()
                        .getContractInstanceByName(dashboardConfig.getComposition().getName() + Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER);
                
                dudContents = ins.getContract().getFileText();
            }
            if (ins != null) {
                dudContents = ins.getContract().getFileText();
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
        
        return dudContents;
    }
	
    /**
     * Gets the contents of the composition file
     * @return - Comp string
     */
    private String getCmp() {
        String cmpContents = null;
        Composition comp = dashboardConfig.getComposition();
        
        try {
            cmpContents = comp.toString();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
        return cmpContents;
    }

    @Listen("onClose = #advancedModeContainer")
    public void closeWidgetConfiguration(Event event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cloing advanced mode page.");
            LOGGER.debug("the parent is: {}", parent);
        }
        Events.postEvent(Dashboard.EVENTS.ON_ADVANCED_MODE_CLOSE, parent, null);
    }
	
    /**
     * Create a new contract from the text entered into the text box, save the contract on hipie, create a contract instance,
     * hook the fields in the contract instance, add the precursor (dataset output) to the instance and save it.
     */
    @Listen("onClick = #saveDud")
    public void saveDud() {
        Composition comp = dashboardConfig.getComposition();
        Contract contract = null;
        String userId = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
        ContractInstance oldCI = CompositionUtil.getVisualizationContractInstance(comp);
        
        
        try {
            	// Create a new contract to add to the composition
            contract = createContract(comp.getName(), userId);
            
            contract.setFileText(dudTextbox.getValue());
            
            // Save the new contract
            contract = HipieSingleton.getHipie().saveContractAs(contract.getAuthor(), contract, CompositionUtil.createDashboardContractSavePath(userId, contract.getName()));
            
            // Get inputs from old contract and put into new contract before creating a new contract instance	        
            ContractInstance contractInstance = contract.createContractInstance(oldCI.getName());
            contractInstance.setAllProperties(oldCI.getProps());
            comp.removeContractInstance(oldCI);
            contractInstance.setContainer(comp);
            
            // Set advanced mode flag in the header of the composition
            contractInstance.setProperty(Dashboard.DASHBOARD_MODE, Dashboard.DASHBOARD_MODE_ADVANCED);
            
            comp.addContractInstance(contractInstance);
            
            // Update compositions file text.
            comp.setFileText(comp.toString());
            
            // Save the dashboard
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).updateDashboard(dashboardConfig.getDashboard(), ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), dashboardConfig.getComposition());
            
            // Update the saved time
            LOGGER.debug("dashboard time -->{}", new Date(dashboardConfig.getComposition().getLastModified()));
            dashboardConfig.getComposition().setLastModified(new Date().getTime());
            dashboardConfig.getDashboard().setLastModifiedDate(new Date(dashboardConfig.getComposition().getLastModified()));
            
            // Show save success message
            Clients.showNotification("Save successful", "info", this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            
            // Re-load the dude and composition into the textboxes
            populateTextboxes();
                
        } catch (RepoException | HipieException e) {
            // problem creating new contract in hipie
            Clients.showNotification(Labels.getLabel("createContractHipieFailed") + " " + Labels.getLabel("checkDudeSyntax"), 
            Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
        } catch (CompositionServiceException e) {
            // problem saving dashboard to hipie
            Clients.showNotification(Labels.getLabel("saveDashboardHipieFailed") + " " + Labels.getLabel("checkDudeSyntax"), 
            Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
        } catch (Exception e) {
            // problem saving composition
            Clients.showNotification(Labels.getLabel("saveDashboardHipieFailed") + " " + Labels.getLabel("checkDudeSyntax"), 
            Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
        }
    
    }
	
    /**
     * Creates a new contract based on the contents of the edited dud. 
     * 
     * @param compoName - The name of the composition the contract will live in
     * @param userId - The username of the person writing the contract
     * @return
     * @throws RepoException
     * @throws HipieException
     * @throws Exception
     */
    private Contract createContract(String compName, String userId) throws RepoException, HipieException, Exception {
        Contract contract = null;
        
        String contractName = compName + Dashboard.CONTRACT_IDENTIFIER;
        
        contract = new Contract(contractName, dudTextbox.getValue());
        
        HIPIEService hipieService = HipieSingleton.getHipie();
        
        contract.setRepository(hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO));
        contract.setLabel(contractName);
        contract.setName(contractName);
        contract.setAuthor(userId);
        
        contract.setDescription("RAMPS - Dashboard Perspective Contract");
        contract.setProp(Contract.CATEGORY, VISUALIZE);
        contract.setProp(Contract.VERSION, VERSION);
        
        return contract;
    }

}
