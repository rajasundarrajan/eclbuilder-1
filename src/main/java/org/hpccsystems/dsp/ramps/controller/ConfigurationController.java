package org.hpccsystems.dsp.ramps.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.OutputElement;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.dude.ServiceElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.CompositionAccess;
import org.hpccsystems.dsp.ramps.controller.entity.FileBrowserData;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ConfigurationController extends SelectorComposer<Component> {

    private static final String DEFAULT_VERSION = "1";

    private static final String LABEL_ALREADY_EXISTS = "labelAlreadyExists";

    private static final String NOEMPTY = "noempty";

    private static final String ON_ADDED_VARIABLE = "onAddedVariable";

    private static final String NUMBERS_ONLY = "[0-9]+";
    private static final String ALPHANUMERIC_ONLY = "[A-Za-z0-9]+";
    
    private static final String START_WITH_ALPHA_ONLY= "^[A-Za-z]{1}.*";

    private static final String MAXIMUM_HFLEX = "1";
    private static final String FALSE = "False";
    private static final String TRUE = "True";

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);

    @Wire
    private Grid globalVariableGrid;

    @Wire
    private Textbox variableName;

    @Wire
    private Combobox variableType;

    @Wire
    private Column valueHolder;
    
    @Wire
    private Textbox connectionDashboard;

    @Wire
    private Textbox thorClusterDashboard;
    @Wire
    private Textbox roxieClusterDashboard;

    @Wire
    private Textbox titleEdit;
    
    @Wire
    private Hbox eclHbox;
    
    @Wire
    private Checkbox keepEcl;
    
    @Wire
    private Vlayout vlayout;
    
    
    @Wire
    private Checkbox runasLarge;
    
    @Wire
    private Radio serviceOverwrite;
    @Wire
    private Radio serviceNew;
    @Wire
    private Radio serviceCustom;
    @Wire
    private Textbox appendVal;
    @Wire
    private Panel roxieServicePanel;
    
    private List<InputElement> elementsList = new ArrayList<InputElement>();

    Map<PermissionType, Permission> permissions = new LinkedHashMap<PermissionType, Permission>();

    final ListModelList<InputElement> modelList = new ListModelList<InputElement>();

    private TabData rampsData;
    private DashboardConfig dashboardData;
    private Composition composition;
    private boolean isCompositionOwner;
    
    /**
     * true in Dashboard perspective for non static Dashboards. RAMPS perspective
     */
    private boolean isDashboardConfigured = false;

    private Set<String> variableNames = new HashSet<String>();

    public boolean getIsCompositionOwner() {
        return isCompositionOwner;
    }

    public void setIsCompositionOwner(boolean isCompositionOwner) {
        this.isCompositionOwner = isCompositionOwner;
    }

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        rampsData = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        dashboardData = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);

        if (rampsData == null) {
            // Getting composition in the dashboard perspective
            composition = dashboardData.getComposition();
            // Setting true for non-static Dashbaords as Service settings are not applicable for Static Dashbaords
            isDashboardConfigured = !dashboardData.getDashboard().isStaticData();
            
        } else {
            // Getting composition in the ramps perspective
            composition = rampsData.getComposition();
            isDashboardConfigured = CompositionUtil.getVisualizationContractInstance(composition) != null;
        }

        if (authenticationService.getCurrentUser().getId().equals(composition.getAuthor())) {
            setIsCompositionOwner(true);
        } else {
            setIsCompositionOwner(false);
        }
        

        if ((authenticationService.getCurrentUser().isGlobalAdmin() || isCompositionOwner) && (composition.getPermissions() != null)) {
            permissions = CompositionUtil.clonePermissions(composition);
         } 
        
        return super.doBeforeCompose(page, parent, compInfo);
    }

    public List<Permission> getCompositionPermissions() {

        List<Permission> compositionPermissions = new ArrayList<>();
        permissions.entrySet().forEach(permission -> compositionPermissions.add(permission.getValue()));
        return compositionPermissions;

    }

  

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        if (rampsData != null) {
            titleEdit.setValue(rampsData.getComposition().getLabel());
            showConnections(rampsData.getProject().getClusterConfig());
        } else {
            if(!dashboardData.isStaticData()){
                showConnections(dashboardData.getDashboard().getClusterConfig());
            }
            titleEdit.setValue(dashboardData.getComposition().getLabel());
        }

        roxieServicePanel.setVisible(isDashboardConfigured);
        
        populateDashboardServiceSettings();
        
        // Selecting a default Value type
        variableType.appendItem(InputElement.TYPE_STRING);
        variableType.appendItem(InputElement.TYPE_INT);
        variableType.appendItem(InputElement.TYPE_BOOL);
        variableType.setSelectedIndex(0);
        changeValueComponent(InputElement.TYPE_STRING);

        if (composition.getInputElements() != null) {
            modelList.clear();
            elementsList.clear();
            
            List<Element> filteredGlobalInputs = RampsUtil.filterSettingsPageInputs(composition.getInputElements());
            
            filteredGlobalInputs.forEach(inputElement -> {
                InputElement newElement = new InputElement();
                newElement.setName(inputElement.getName());
                newElement.setType(inputElement.getType());
                newElement.addOption(new ElementOption(Element.DEFAULT,
                        new FieldInstance(null, "\""+inputElement.getOption(Element.DEFAULT).getParams().get(0).getName()+"\"")));
                elementsList.add(newElement);
                variableNames.add(newElement.getName());
            });
            modelList.addAll(elementsList);
        }
        globalVariableGrid.setModel(modelList);

        globalVariableGrid.setRowRenderer(new RowRenderer<InputElement>() {

            @Override
            public void render(Row row, final InputElement element, int arg2) throws Exception {
                renderGlobalVariableRows(row, element);

            }
        });

        // Event listener to clear fields after the variable is added
        // A new event is used here, because setting values to null in the
        // onClick listener triggers another validation of constraints
        this.getSelf().addEventListener(ON_ADDED_VARIABLE, new SerializableEventListener<Event>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                variableName.setRawValue(null);
                ((Textbox) valueHolder.getFirstChild()).setRawValue(null);
            }
        });

        if(canShowEclOption()){
            eclHbox.setVisible(true);
            showPersistedECLOption();
        }else{
            eclHbox.setVisible(false);
        }
       
        this.getSelf().addEventListener(EVENTS.ON_DISABLE_SETTINGS, event -> vlayout.setSclass("disableTabPanel"));
        this.getSelf().addEventListener(EVENTS.ON_ENABLE_SETTINGS, event -> vlayout.setSclass("no"));
    }

    private void populateDashboardServiceSettings() {
        if(!isDashboardConfigured) {
            LOGGER.debug("Dashboard not configured.");
            return;
        }
        
        boolean hasRunAsLargeDataset = CompositionUtil.hasRunAsLargeDataset(composition);
        runasLarge.setChecked(hasRunAsLargeDataset);
        
        if(runasLarge.isChecked()){
            enableDeployOption();
        }else{
            disableDeployOption();
        }
        
        Optional<Element> versionElement = CompositionUtil.extractVersionElement(composition);
        if(versionElement.isPresent()) {
            String value = versionElement.get().getOptionValue(Element.DEFAULT, null, null);
            appendVal.setValue(value);
            LOGGER.debug("Visualization Version - {}", value);
            if(DEFAULT_VERSION.equals(value)) {
                serviceOverwrite.setSelected(true);
            } else if (StringUtils.isEmpty(value)) {
                serviceNew.setSelected(true);
            } else {
                serviceCustom.setSelected(true);
                if(runasLarge.isChecked())
                	appendVal.setDisabled(false);
            }
        } else {
            appendVal.setValue(DEFAULT_VERSION);
        }
    }

    private void disableDeployOption() {
        serviceOverwrite.setDisabled(true);  
        serviceNew.setDisabled(true); 
        serviceCustom.setDisabled(true);
        appendVal.setDisabled(true);
    }

    private void enableDeployOption() {
        serviceOverwrite.setDisabled(false);  
        serviceNew.setDisabled(false); 
        serviceCustom.setDisabled(false);
        if(serviceCustom.isChecked()) {
        	appendVal.setDisabled(false);
        }
    }

    private void renderGlobalVariableRows(Row row, final InputElement element) {
        String elementValue = element.getOptionValues().iterator().next().getParams().get(0).getName();

        row.appendChild(new Label(element.getType()));
        row.appendChild(new Label(element.getName()));

        Component component = createValueComponent(element.getType(), element, elementValue);
        if (Constants.REFERENCE_ID.equals(element.getName())
                || GlobalVariable.GCID_COMPLIANCE_TAGS.containsKey(element.getName())
                || Constants.FCRA.equals(element.getName()) || Constants.INDUSTRY_CLASS.equals(element.getName())) {
            ((Textbox) component).setReadonly(true);
        }
        row.appendChild(component);

        if (Constants.REFERENCE_ID.equals(element.getName())) {
            if (rampsData != null) {
                row.appendChild(createGCIDEditButton());
            }
        } else {
            row.appendChild(createDeleteButton(element));
        }

    }

    private Button createGCIDEditButton() {
        Button editButton = new Button(Labels.getLabel("change"));
        editButton.setIconSclass("fa fa-pencil-square-o");
        editButton.setClass("import-btn btn-xs");

        editButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                ((Window) ConfigurationController.this.getSelf()).detach();
                Map<String, Object> dataMap = new HashMap<String, Object>();
                dataMap.put(Constants.TAB_DATA, rampsData);
                Window window = (Window) Executions.createComponents("ramps/project/editGCID.zul", null, dataMap);
                window.doModal();
            }
        });

        return editButton;
    }

    private Button createDeleteButton(final InputElement element) {
        Button deleteButton = new Button(Labels.getLabel("Delete"));
        deleteButton.setIconSclass("fa fa-trash");
        deleteButton.setZclass("img-btn btn-close");

        deleteButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                modelList.remove(element);
                elementsList.remove(element);
                variableNames.remove(element.getName());
                composition.getInputElements().remove(element);
            }
        });

        if(GlobalVariable.GCID_COMPLIANCE_TAGS.containsKey(element.getName())
        || Constants.FCRA.equals(element.getName()) || Constants.INDUSTRY_CLASS.equals(element.getName())){
            deleteButton.setDisabled(true);
        }
        return deleteButton;
    }

    private Component createValueComponent(String type, final InputElement element, String elementValue) {
        final Component component;

        switch (type) {

        case InputElement.TYPE_BOOL:
            component = createCombobox(element, elementValue);
            break;

        case InputElement.TYPE_INT:
            component = createTextbox(element, elementValue, true);
            break;

        default:
            component = createTextbox(element, elementValue, false);
            break;
        }

        return component;
    }

    /**
     * @param element
     *            Passing null doesn't add the on Change listener
     * @param elementValue
     *            Optional. value is set if passed
     * @param numbersOnly
     *            Creates the validation for numbers only.
     * @return
     */
    private Textbox createTextbox(final InputElement element, String elementValue, boolean numbersOnly) {
        final Textbox textbox = new Textbox(elementValue);
        textbox.setHflex(MAXIMUM_HFLEX);
        textbox.setConstraint(numbersOnly ? "/[0-9]+/ : Only numbers are allowed" : "no empty");

        if (element != null) {
            textbox.addEventListener(Events.ON_CHANGE, (SerializableEventListener<? extends Event>)event -> updateValue(element, textbox.getText()));
        }

        return textbox;
    }

    /**
     * @param element
     *            Binds element to UI if passed
     * @param elementValue
     * @return
     */
    private Combobox createCombobox(final InputElement element, String elementValue) {
        final Combobox combobox = new Combobox();
        combobox.setHflex(MAXIMUM_HFLEX);
        combobox.setReadonly(true);
        combobox.appendItem(TRUE);
        combobox.appendItem(FALSE);

        if (elementValue != null) {
            combobox.setSelectedIndex(TRUE.equals(elementValue) ? 0 : 1);
        }

        if (element != null) {
            combobox.addEventListener(Events.ON_SELECT, new SerializableEventListener<Event>() {

                /**
                 * 
                 */
                private static final long serialVersionUID = 1L;

                @Override
                public void onEvent(Event event) throws Exception {
                    updateValue(element, combobox.getSelectedItem().getLabel());
                }
            });
        }
        return combobox;
    }

    @Listen("onClick = #cancel")
    public void onCancel() {
        Events.postEvent("onClose", this.getSelf(), null);
    }

    @Listen("onClick = #add")
    public void onAdd() {
        Textbox textbox = (Textbox) valueHolder.getFirstChild();

        String name = variableName.getText();

        if (!validateName(name)) {
            return;
        }

        String type = variableType.getSelectedItem().getLabel();
        String value = textbox.getValue();

        if (StringUtils.isEmpty(value)) {

            Clients.showNotification(Labels.getLabel(NOEMPTY), Clients.NOTIFICATION_TYPE_ERROR, textbox, Constants.POSITION_END_AFTER, 3000);
            return;
        }
        if (InputElement.TYPE_INT.equals(type) && !value.matches(NUMBERS_ONLY)) {
            Clients.showNotification(Labels.getLabel("onlynumericallowed"), Clients.NOTIFICATION_TYPE_ERROR, textbox, Constants.POSITION_END_AFTER, 3000);
            return;
        }

        if (!variableNames.add(variableName.getText())) {
            Clients.showNotification(Labels.getLabel("alreadyExists"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_MIDDLE_CENTER, 3000);
            return;
        }

        InputElement newElement = new InputElement();
        newElement.setName(name);
        newElement.setType(type);
        newElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, "\""+value+"\"")));
        LOGGER.debug("glbal var added - " + newElement.toString());
        elementsList.add(newElement);
        modelList.add(newElement);

        Events.postEvent(ON_ADDED_VARIABLE, getSelf(), null);
    }

    private boolean validateName(String name) {
        if (StringUtils.isEmpty(name)) {

            Clients.showNotification(Labels.getLabel(NOEMPTY), Clients.NOTIFICATION_TYPE_ERROR, variableName, Constants.POSITION_END_AFTER, 3000);
            return false;
        }

        if (!name.matches(START_WITH_ALPHA_ONLY)) {
            Clients.showNotification(Labels.getLabel("startWithAlphaOnly"), Clients.NOTIFICATION_TYPE_ERROR, variableName, Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (!name.matches(ALPHANUMERIC_ONLY)) {

            Clients.showNotification(Labels.getLabel("onlyalphanumericallowed"), Clients.NOTIFICATION_TYPE_ERROR, variableName, Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        return true;
    }

    @Listen("onSelect = #variableType")
    public void onChangeType(SelectEvent<Comboitem, String> event) {
        String selectedType = event.getSelectedItems().iterator().next().getLabel();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Selection - " + selectedType);
        }

        changeValueComponent(selectedType);
    }

    private void changeValueComponent(String type) {
        valueHolder.getChildren().clear();
        valueHolder.appendChild(createValueComponent(type, null, null));
    }

    private void updateValue(final InputElement element, String value) {
        ElementOption option = element.getOption(Element.DEFAULT);

        if (option != null) {
            option.getParams().clear();
            option.getParams().add(new FieldInstance(null, value));
        } else {
            element.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, value)));
        }

    }


    @Listen("onClick = #save")
    public void save() {
        long startTime = Instant.now().toEpochMilli();

        if(titleEdit.getValue().trim().isEmpty()) {
            Clients.showNotification(Labels.getLabel("compositionLabelCannotBeEmpty"), Clients.NOTIFICATION_TYPE_ERROR, titleEdit,
                    Constants.POSITION_END_AFTER, 3000, true);
            titleEdit.setValue("");
            return;
        }

        if (Character.isDigit(titleEdit.getValue().charAt(0))) {
            Clients.showNotification(Labels.getLabel("startWithoutDigits"), Clients.NOTIFICATION_TYPE_ERROR, titleEdit,
                    Constants.POSITION_END_AFTER, 3000);
            return ;
        }
           
        if(!hasvalidPermission()){
            Clients.showNotification(Labels.getLabel("invalidPermission"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                    Constants.POSITION_MIDDLE_CENTER, 3000, true);
            return;
        }
        
        //Validating version
        if(isDashboardConfigured && serviceCustom.isChecked() && runasLarge.isChecked()) {
            if(StringUtils.isEmpty(appendVal.getValue())) {
                Clients.showNotification(Labels.getLabel("emptyversion"), Clients.NOTIFICATION_TYPE_ERROR, appendVal,
                    Constants.POSITION_END_AFTER, 3000, true);
            return;
        } else if(!appendVal.getValue().matches("[A-Za-z0-9_]+")) {
            Clients.showNotification(Labels.getLabel("versionInvalid"), Clients.NOTIFICATION_TYPE_ERROR, appendVal,
                        Constants.POSITION_END_AFTER, 3000, true);
                return;
            }
        }
        
        initializePermissions();
        
        if (isGlobalVariableInBlacklist() || hasInvalidGlobalVariableFile())
            return;

        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        addGlobalVariable(authenticationService);
       
        if(isDashboardConfigured){
            saveDeployOption();
        }
        
        if (rampsData != null) {
            //Check for RAMPS flow label duplication on Editing Labels
            if (validateRAMPSLabel()) {
                Clients.showNotification(Labels.getLabel(LABEL_ALREADY_EXISTS), Clients.NOTIFICATION_TYPE_ERROR, titleEdit,
                        Constants.POSITION_END_AFTER, 3000, true);
                titleEdit.setValue("");
                return;
            } 
            composition.setLabel(titleEdit.getValue());
            
            rampsData.getProject().setLabel(titleEdit.getValue());
            rampsData.getProjectTab().setLabel(titleEdit.getValue());
        
            List<Element> filteredInputs = RampsUtil.filterGlobalVarPopupInputs(rampsData.getComposition().getInputElements());
            
            rampsData.getProject().setShowGlobalVariable(CollectionUtils.isNotEmpty(filteredInputs));
            rampsData.updateGlobalVariablesModel(rampsData.getComposition().getInputElements());
            
            FileBrowserData data = RampsUtil.getFileBrowserData(Constants.ACTION.SAVE);
            data.setNotifyUser(false);
            Events.postEvent(EVENTS.ON_SAVE_CURRENT_PLUGIN, rampsData.getHtmlHolder(), data);
          
        } else {
            //Check for DASHBOARD flow label duplication on Editing Labels
            if (validateDashboardLabels()) {
                Clients.showNotification(Labels.getLabel(LABEL_ALREADY_EXISTS), Clients.NOTIFICATION_TYPE_ERROR, titleEdit,
                        Constants.POSITION_END_AFTER, 3000, true);
                titleEdit.setValue("");
                return;

            } else {
                composition.setLabel(titleEdit.getValue());
                dashboardData.getDashboard().setLabel(titleEdit.getValue());
                dashboardData.getDashboardTab().setLabel(titleEdit.getValue());
            }
            
            //Updating run as large data
            dashboardData.getDashboard().setLargeDataset( (runasLarge!= null) ? runasLarge.isChecked() : false);
            
            if(CollectionUtils.isNotEmpty(dashboardData.getDashboard().getWidgets()) && 
                    (dashboardData.getDashboard().isChanged() || Flow.NEW == dashboardData.getFlow())) {
                saveVisualizationContract();
            }
            
            // saving the composition using hipieService as there are no plugins
            // involved in dashboard perspective
            try {
                HipieSingleton.getHipie().saveComposition(authenticationService.getCurrentUser().getId(), composition);
                Clients.showNotification(Labels.getLabel("settingsUpdated"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf().getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                Events.postEvent(EVENTS.ON_SAVE_DASHBOARD_CONFIG, dashboardData.getCanvasComponent(), dashboardData.getDashboard());
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(Labels.getLabel("problemOccuredSavingPermission"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf().getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
            }
        }
        
        LOGGER.debug("elementsList in Save-->{}", elementsList);
        LOGGER.debug("modelList in Save-->{}", modelList);

        onCancel();
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new CompositionAccess(composition, startTime));
        }
    }

    @Listen("onCheck = #runasLarge")
    public void deployRoxieService(){        
        if(runasLarge.isChecked()){
            enableDeployOption();
        }else{
            disableDeployOption();
        }
    }
    
    private void saveDeployOption() {
        CompositionUtil.setVizVersion(composition, appendVal.getValue());        
        //Updating run as large data
        ContractInstance visualizationCI = CompositionUtil.getVisualizationContractInstance(composition);
        if(visualizationCI != null){
            Contract contract = visualizationCI.getContract();
            
            contract.getOutputElements().forEach(this::updateDeployOption);
            LOGGER.debug("Saving contract - {}\n By - {}", contract, contract.getAuthor());
            
            try {            
                contract.setRepository(HipieSingleton.getHipie().getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO));
                HipieSingleton.getHipie().saveContract(contract.getAuthor(), contract);
            } catch (Exception e) {
                LOGGER.debug("Unable to update Version and Deploy option");
                LOGGER.error(Constants.EXCEPTION,e);
            }
        }
        
    }

    private void saveVisualizationContract() {
        
        try {           
            Dashboard dashboard = dashboardData.getDashboard();
            List<Widget> scoredSearchWidgets = dashboard.getScoredSearchWidgets();
            String userId=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
            if (scoredSearchWidgets != null && !scoredSearchWidgets.isEmpty()) {
                CompositionUtil.generateScoredSearchVisualizationPlugin(composition, userId, dashboard);
            } else {
                CompositionUtil.generateVisualizationPlugin(composition, userId, dashboard, false);
           }
        } catch (Exception e) {
            LOGGER.error("Contract save failed.", e);
            Clients.showNotification(Labels.getLabel("errorSavingRunConfiguration"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 3000, true);
            return;
        }
        
    }

    private void updateDeployOption(Element element) {
    	Set<String> optionKeys = element.getOptions().keySet();
    	if(! (optionKeys.contains(ServiceElement.SOAP) && optionKeys.contains(ServiceElement.XPATH))) {
    		element.getOptions().clear();
	        element.addOption(new ElementOption(runasLarge.isChecked() ? OutputElement.LARGE : OutputElement.WUID));
    	}
    }

    private void addGlobalVariable(AuthenticationService authenticationService) {
        if(canShowEclOption()){
            if(keepEcl.isChecked()){
                addEclOptionGlobalVar(Constants.TRUE);
            }else{
                addEclOptionGlobalVar(Constants.FALSE);
            }
        }
    }
        
    private boolean hasvalidPermission() {
        boolean isValid = true;
        if (permissions != null) {            
            Optional<Entry<PermissionType, Permission>> invalidCustomPermission = permissions.entrySet().stream().filter(entry ->
                PermissionLevel.CUSTOM.equals(entry.getValue().getPermissionLevel()) 
                && CollectionUtils.isEmpty(entry.getValue().getGroups())
                                && CollectionUtils.isEmpty(entry.getValue().getUserIds())).findAny();
            isValid = !invalidCustomPermission.isPresent();
        }
        return isValid;
    }

    private void addEclOptionGlobalVar(String keepEcl) {
        LOGGER.info("keepEcl --->{}",keepEcl);
        Element eclElement = RampsUtil.getECLElement(composition);
        if(eclElement == null){
        eclElement = new InputElement();
        eclElement.setName(Constants.KEEP_ECL);
        eclElement.setType(InputElement.TYPE_BOOL);
        eclElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, "\""+keepEcl+"\"")));
        composition.getInputElements().add(eclElement);
        }else{
            ElementOption eclOption = eclElement.getOption(Element.DEFAULT);
            eclOption.getParams().clear();
            eclOption.addParam(new FieldInstance(null, "\""+keepEcl+"\""));
        }
    }

    private void showPersistedECLOption() {
        Element eclElement = RampsUtil.getECLElement(composition);
       if(eclElement != null){
           String eclOption = eclElement.getOptionValues().iterator().next().getParams().get(0).getName();
           LOGGER.info("Keep Ecl ---->{}",eclOption);
           if(Constants.TRUE.equalsIgnoreCase(eclOption)){
               keepEcl.setChecked(true);
           }else{
               keepEcl.setChecked(false);
           }          
       }
        
    }
    
    /**
     * Adds the ecl element to the composition and sets the permissions based on what the user
     * set in the settings modal.
     */
    private void initializePermissions() {
        if (elementsList != null) {
            Element eclElement = RampsUtil.getECLElement(composition);
            
            composition.getInputElements().clear();
            //Adding back, 'KeepEcl' Global variable
            if(eclElement != null) {
                composition.getInputElements().add(eclElement);
            }
            
            composition.getInputElements().addAll(elementsList);
            
            for (Permission perm: getCompositionPermissions()) {
                composition.getPermissions().get(perm.getPermissionType()).setGroups(perm.getGroups());
                composition.getPermissions().get(perm.getPermissionType()).setUserIds(perm.getUserIds());
                composition.getPermissions().get(perm.getPermissionType()).setPermissionLevel(perm.getPermissionLevel());
            }
        }
    }
    
    private boolean isGlobalVariableInBlacklist() {
        List<String> blacklist = ((LogicalFileService) SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles();
        
        if (blacklist == null)
            return true;
        
        if (elementsList.isEmpty())
            return false;
        
        for (Element inputElement: elementsList) {
            String lfName = null;
            if (isInputElementNameNull(inputElement)) {
                lfName = inputElement.getName().replace("~", "");
            }
            
            if (inputElement.getType() != null && inputElement.getType().equals(InputElement.TYPE_STRING) && 
                    lfName != null) {
                if (((LogicalFileService) SpringUtil.getBean(Constants.LOGICAL_FILE_SERVICE)).isFileInBlacklist(lfName, blacklist)) {
                    Clients.showNotification(Labels.getLabel("invalidDatasourceInGlobalVariables"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                            Constants.POSITION_TOP_CENTER, 5000, true);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasInvalidGlobalVariableFile() {
        if (elementsList.isEmpty())
            return false;
        
        // Get the list of use dataset plugins from the compostion (this will
        // fetch the name of the file that is represented by the global variable)
        List<String> logicalFilesInComposition = composition
                .getContractInstances().entrySet().stream()
                .filter(contract -> contract.getValue().getProperty(Constants.LOGICAL_FILENAME) != null)
                .map(p -> p.getValue().getProperty(Constants.LOGICAL_FILENAME))
                .collect(Collectors.toList());
        // Check each logical file name to see if it's a valid file on the cluster
        for (String lfName: logicalFilesInComposition) {
            try {
                if (rampsData != null) {
                    rampsData.getProject().getHpccConnection().getDatasetFields(lfName, null);
                } else {
                    dashboardData.getDashboard().getHpccConnection().getDatasetFields(lfName, null);
                }
                continue;
            } catch (Exception e) {
                // File does not exist on the cluster. Show a message to the user.
                Clients.showNotification(Labels.getLabel("invalidDatasourceInGlobalVariables"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return true;
            }
        }
        return false;
    }
    
    private boolean isInputElementNameNull(Element inputElement) {
        if (inputElement != null && inputElement.getOption(Element.DEFAULT) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams() != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0).getName() != null) {
            return false;
        } else {
            return true;
        }
    }
    
    private  boolean validateRAMPSLabel(){
        List<Project> prjts;
        Map<String, CompositionElement> templatesMap = null;
        AuthenticationService authenticationService=(AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        try {
            //check for project labels
            prjts = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getProjects(authenticationService.getCurrentUser());
            Project currentProject = null ;
            for (Project prjt : prjts){
                if(prjt.getLabel().equals(rampsData.getProject().getLabel())){
                    currentProject = prjt;
                }
            }
            prjts.remove(currentProject);
            
            //Check for template labels
            templatesMap = checkTemplate(templatesMap, authenticationService);
            
            //Check for open tab projects
            Set<String> labels = RampsUtil.getOpenProjectLabels();
            labels.remove(rampsData.getProject().getLabel());
            boolean hasTitle = labels != null && labels.contains(titleEdit.getValue());
            return prjts.stream().filter(comp -> comp.getLabel().equals(titleEdit.getValue())).count() > 0 || hasTitle
                    || (templatesMap != null && isLabelExists(templatesMap.values(), titleEdit.getValue()));
            
        } catch (CompositionServiceException | HipieException d) {
           LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return true;
        }
       
    }

    private Map<String, CompositionElement> checkTemplate(Map<String, CompositionElement> templatesMap, AuthenticationService authenticationService)
            throws HipieException {
        Map<String, CompositionElement> compositionTemplates = templatesMap;
        try {
            compositionTemplates = HipieSingleton.getHipie().getCompositionTemplates(authenticationService.getCurrentUser().getId());
        } catch (Exception e1) {
            LOGGER.error(Constants.EXCEPTION, e1);
            throw new HipieException("Could not get the templates", e1);
        }
        return compositionTemplates;
    }
    
    private boolean validateDashboardLabels(){
        List<Dashboard> dashs;
        try {
            CompositionService compositionService=(CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
            dashs = compositionService.getDashboards(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser());
            Dashboard currentDashboard = null ;
            for (Dashboard dashboard : dashs){
                if(dashboard.getLabel().equals(dashboardData.getDashboard().getLabel())){
                    currentDashboard = dashboard;
                }
            }
            dashs.remove(currentDashboard);
            return dashs.stream().filter(comp -> comp.getLabel().equals(titleEdit.getValue())).count() > 0;
        } catch (CompositionServiceException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return true;
        }
       
    }
    
    private void showConnections(ClusterConfig config){
        connectionDashboard.setValue(config.getId());
        thorClusterDashboard.setValue(config.getThorCluster());
        roxieClusterDashboard.setValue(config.getRoxieCluster());
    }

    private boolean isLabelExists(Collection<CompositionElement> templatesMap,String label){
        return templatesMap.stream().filter(compElement ->label.equals(compElement.getLabel())).count() > 0;
    }
    
    /**
     * Checks whether the use has 'PLUGIN_DEVELOPER' role and decides to show the option to 'keep generated ecl'
     * As for Static data dashboard don't generate any ecl, don't show
     */
    private boolean canShowEclOption() {
        boolean canShow = false;
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        if (authenticationService.getCurrentUser().getPermission().getRampsPermission().isKeepECL()) {
            // In Ramps perspective,don't have any Static data Dashboards and it
            // may not have a dashboard configured.So can show the ECL option if
            // user is plugin developer
            if (dashboardData == null || dashboardData.isRAMPSConfig()) {
                canShow = true;
            } else if (!dashboardData.isStaticData()) {
                canShow = true;
            }
        }
        return canShow;
    }
    
    @Listen("onCheck = #serviceGroup")
    public void updateServiceOption() {
        if(serviceOverwrite.isChecked()) {
            appendVal.setText(DEFAULT_VERSION);
            appendVal.setDisabled(true);
        } else if(serviceNew.isChecked()) {
            appendVal.setText("");
            appendVal.setDisabled(true);
            appendVal.setPlaceholder(Labels.getLabel("timeStamp"));
        } else if(serviceCustom.isChecked()) {
            appendVal.focus();
            appendVal.setText("");
            appendVal.setPlaceholder("");
            appendVal.setDisabled(false);
        }
    }
    
    public boolean isDashboardConfigured() {
        return isDashboardConfigured;
    }
}
