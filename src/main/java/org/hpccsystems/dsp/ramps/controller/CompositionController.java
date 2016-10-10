package org.hpccsystems.dsp.ramps.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.hpcc.HIPIE.Compliance;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.component.renderer.GlobalVariableRenderer;
import org.hpccsystems.dsp.ramps.component.renderer.PluginItemRenderer;
import org.hpccsystems.dsp.ramps.controller.entity.FileBrowserData;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.HtmlGenerator;
import org.hpccsystems.dsp.ramps.entity.BooleanResult;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.InsertPluginRelation;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.PluginRelation;
import org.hpccsystems.dsp.ramps.entity.PluginRelations;
import org.hpccsystems.dsp.ramps.entity.PluginRelations.PluginEvent;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.SimplePluginRelation;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.error.HError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.json.JSONObject;
import org.zkoss.json.parser.JSONParser;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class CompositionController extends SelectorComposer<Component> {

    private static final String PROJECT_FILE_BROWSER_ZUL = "/ramps/project/file_browser.zul";
    private static final String COULD_NOT_HOOK = "couldNotHook";
    private static final String STATUS = "status";
    private static final String NO_MORE_PLUGINS = "noMorePlugins";
    private static final String CONTAINER = "container";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionController.class);

    @Wire
    private Listbox flowChart;
    @Wire
    private Include browserInclude;
    
    @Wire
    private Vlayout formLayout;
    
    @Wire
    private Html htmlHolder;
    @Wire
    private Label pluginSource;

    @Wire
    private Tab pluginSelection;

    @Wire
    private Tab pluginDetails;

    @Wire
    private Tab fileInfoTab;

    @Wire
    private Label readMe;

    @Wire
    private Tab propertiesTab;

    @Wire
    private Tabpanel fileInfoPanel;

    @Wire
    private Button previous;
    @Wire
    private Button next;
    @Wire
    private Button addNextStep;

    @Wire
    private Button addStep;

    @Wire
    private Button validate;

    @Wire
    private Include contentHolder;
    
    @Wire
    private Include addPluginsHolder;
    
    @Wire
    private Popup globalVarPopup;
    
    @Wire
    private Listbox globalVariablesList;

    private ListModelList<Plugin> pluginsModel = new ListModelList<Plugin>();

    // Don't instantiate
    private Project project;
    // Don't instantiate
    private TabData data;

    private Plugin activePlugin;
    private Plugin targetPlugin;
    private Plugin deletedPlugin;
    private boolean isView = false;
    private boolean isFileInfoSelected = false;
    
    private PluginEvent pluginEvent;
    private InsertPluginRelation relation;
    private String field;

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {

        // service bean wiring as the zk is not wiring it whenbefore
        // composer is called
        data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);

        project = data.getProject();
        setView(Flow.VIEW == data.getFlow());

        return super.doBeforeCompose(page, parent, compInfo);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        data.setHtmlHolder(htmlHolder);
        data.setUsedatasetFormHolder(browserInclude);
        data.setCompositionControllerComponent(comp);
        
        pluginsModel.addAll(project.getPlugins());
        flowChart.setModel(pluginsModel);
        flowChart.setItemRenderer(new PluginItemRenderer(true));
        
        addPluginsHolder.setDynamicProperty(Constants.TAB_DATA, data);
        addPluginsHolder.setSrc("ramps/project/add_plugins.zul");
        
        // Adding event listeners to Select, Delete & Swap in flowchart
        addEventListeners();

        // Setting the project in native session
        HttpSession session = (HttpSession) Sessions.getCurrent().getNativeSession();
        Map<String, Project> openProjects = (HashMap<String, Project>) session.getAttribute(Constants.OPEN_PROJECTS);
        if (openProjects == null) {
            openProjects = new HashMap<String, Project>();
            session.setAttribute(Constants.OPEN_PROJECTS, openProjects);
        }
        openProjects.put(project.getName(), project);

        // Disabling pointer event
        if (Flow.VIEW == data.getFlow()) {
            htmlHolder.setSclass("htmlWrapperDiv");
            browserInclude.setSclass("formIncludeDiv");
            addStep.setSclass("addStepClass");
            addStep.setDisabled(true);
            addNextStep.setDisabled(true);
            validate.setDisabled(true);
        }
        List<Element> filteredInputs = RampsUtil.filterGlobalVarPopupInputs(data.getComposition().getInputElements());
        data.getProject().setShowGlobalVariable(CollectionUtils.isNotEmpty(filteredInputs));
       
        // Event listener for listening completion of form validation
        htmlHolder.addEventListener("onFormValidation", event -> processClientEvent(event));
        
        htmlHolder.addEventListener("onClickFillGCID", event -> processFillGCIDEvent(event));

        htmlHolder.addEventListener(EVENTS.ON_SAVE_CURRENT_PLUGIN, event -> savePluginEventListener(event));

        browserInclude.addEventListener(EVENTS.ON_CHANGE_HPCC_CONNECTION, event -> {
            browserInclude.setSrc("");
            browserInclude.setSrc(PROJECT_FILE_BROWSER_ZUL);
            Events.postEvent(EVENTS.ON_STARTOF_BROWSER_LOADING, CompositionController.this.getSelf().getParent().getParent().getFellow(CONTAINER), null);
            data.getProject().setDatasourceStatus(DatasourceStatus.LOADING);
        });

        //Creating Global Variables model
        List<Element> inputElements = data.getComposition().getInputElements();
        ListModelList<GlobalVariable> variablesModel = new ListModelList<>();
        globalVariablesList.setModel(variablesModel);
        globalVariablesList.setItemRenderer(new GlobalVariableRenderer());

        data.setGlobalVariablesModel(variablesModel);
        if(CollectionUtils.isNotEmpty(inputElements)) {
            data.updateGlobalVariablesModel(inputElements);
        }
        LOGGER.debug("Varialbes - {}", data.getGlobalVariablesModel());
        
        // Selecting first plugin by default
        if (!pluginsModel.isEmpty()) {
            activePlugin = pluginsModel.get(0);
            selectPlugin(activePlugin);
        } else {
            Clients.showNotification(Labels.getLabel("unableToLoadPlugins"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
        }

        this.getSelf().addEventListener(Constants.ON_CHOOSING_PLUGIN_RELATION, event -> doAfterRelationsEstablished((PluginRelations) event.getData()));
        
        this.getSelf().addEventListener(EVENTS.ON_REFRESH_PLUGIN_BROWSER, event -> {
            if (!CollectionUtils.isEmpty((ArrayList<ContractInstance>) event.getData())) {
                StringBuilder errorMessage = new StringBuilder();
                List<String> labelList = new ArrayList<String>();
                for (ContractInstance contractInstance : (ArrayList<ContractInstance>) event.getData()) {
                    if (contractInstance.getOption(Contract.LABEL) == null) {
                        labelList.add(contractInstance.getContract().getLabel());
                    } else {
                        labelList.add(contractInstance.getOption(Contract.LABEL).iterator().next().getParams().iterator().next().toString());
                    }
                }
                labelList.forEach(instanceLabel -> errorMessage.append(", ").append(instanceLabel));
                Clients.showNotification(Labels.getLabel("missingContracts") + errorMessage.toString(), Clients.NOTIFICATION_TYPE_ERROR, null,
                        Constants.POSITION_MIDDLE_CENTER, 0, true);
            }
            loadPlugin(activePlugin);
        });

        this.getSelf().addEventListener(EVENTS.ON_VALIDATE, event -> {
            HError error = (HError) event.getData();

            Plugin errorPlugin = getPlugin(error.getSourceID());

            // error dashboard plugin refrence comes as null-adding null check to work around.
            if (errorPlugin != null && activePlugin != errorPlugin) {
                targetPlugin = errorPlugin;
                selectPlugin(errorPlugin);
                if (errorPlugin.isDatasourcePlugin()) {
                    Clients.showNotification(error.getErrorString(), Clients.NOTIFICATION_TYPE_ERROR, null, Constants.POSITION_TOP_CENTER, 3000);
                }
            } else {
                Clients.showNotification(error.getErrorString(), Clients.NOTIFICATION_TYPE_ERROR, null, Constants.POSITION_TOP_CENTER, 3000);
            }
        });
        
        this.getSelf().addEventListener(EVENTS.ON_CLOSE_PLUGIN_BROWSER, event -> {
            pluginDetails.setSelected(true);
            showPluginDetails();
        }); 
    }
    
    private void doAfterRelationsEstablished(PluginRelations relations) throws HipieException {
        LOGGER.debug("Relations established - {} ", relations);
        PluginRelation firstRelation = (PluginRelation) relations.getPluginRelations().iterator().next();
        
        //Continue earlier flow
        if (PluginEvent.APPEND_PLUGIN == pluginEvent) {
            relations.establishDefaultRelations();
            appendPluginToModel(firstRelation.getDestplugin());
        } else if (PluginEvent.DELETE_PLUGIN == pluginEvent) {
            deletePluginFromComposition(deletedPlugin);
            
            relations.establishDefaultRelations();
            
            List<Plugin> plugins = project.getPlugins();
            Plugin previousPlugin = plugins.get(plugins.indexOf(deletedPlugin) - 1);
            deletePluginFromModel(deletedPlugin, previousPlugin);
        } else if (PluginEvent.INSERT_PLUGIN == pluginEvent) {
            relation.establishRelations(relations);
            insertPluginInModel();
        } else if (PluginEvent.EDIT_RELATIONS == pluginEvent) {
            relations.establishDefaultRelations();
            selectPlugin(activePlugin);
        }
    }

    private void savePluginEventListener(Event event) {
        FileBrowserData fileBrowserData = (FileBrowserData) event.getData();
        String action = fileBrowserData.getAction();
        boolean validationErrorOccured;
        if (activePlugin.isDatasourcePlugin()) {
            validationErrorOccured = savePlugin(false, fileBrowserData);
            if (!validationErrorOccured && action.equals(Constants.ACTION.SAVE)) {
                Events.postEvent(EVENTS.ON_SAVE, CompositionController.this.getSelf().getParent().getParent().getFellow(CONTAINER), null);
            } else if (!validationErrorOccured && action.equals(Constants.ACTION.RUN)) {
                Events.postEvent(EVENTS.ON_RUN_INITIATED, CompositionController.this.getSelf().getParent().getParent().getFellow(CONTAINER), null);
            }
        } else {
            savePlugin(false, fileBrowserData);
        }
    }

    public Plugin getPlugin(String contractInstanceId) {
        for (Plugin plugin : project.getPlugins()) {
            if (plugin.isDatasourcePlugin()) {
                for (Plugin dataset : ((DatasetPlugin) plugin).getPlugins()) {
                    if (dataset.getContractInstance().getInstanceID().equals(contractInstanceId)) {
                        return plugin;
                    }
                }
            } else if (plugin.getContractInstance().getInstanceID().equals(contractInstanceId)) {
                return plugin;
            }
        }
        return null;
    }

    public void addEventListeners() {
        flowChart.addEventListener(EVENTS.ON_SELECT_PLUGIN, event -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Select event for {}", event.getData());
            }
            showPluginDetails();
            Plugin plugin = (Plugin) event.getData();
            targetPlugin = plugin;
            savePlugin(true, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
        });

        flowChart.addEventListener(EVENTS.ON_DELETE_PLUGIN, event -> deletePlugin((Plugin) event.getData()));

        flowChart.addEventListener(EVENTS.ON_DROP_PLUGIN_ON_ARROW, event -> insertPlugin(event));

        flowChart.addEventListener(EVENTS.ON_SWAP_PLUGIN, event -> {
            swapPlugin(event);
            event.stopPropagation();
        });

        flowChart.addEventListener(EVENTS.ON_PLUGIN_ADD, event -> {
            Plugin lastplugin = project.getPlugins().get(project.getPlugins().size() - 1);
            Plugin plugin = (Plugin) event.getData();
            appendPlugin(lastplugin, plugin.clone());
        });
    }

    private void insertPlugin(Event event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Plugin> args = (HashMap<String, Plugin>) event.getData();
            
            Plugin dragged = args.get(Constants.DRAGGED);
            Plugin dropped = args.get(Constants.DROPPED);
            dragged = dragged.clone();
            HIPIEUtil.associateContractInstance(dragged, project.hasReferenceId() ? project.getBaseScope() : null);
            
            List<Plugin> pluginList = project.getPlugins();
            Plugin nextPlugin = pluginList.get(pluginList.indexOf(dropped) + 1);
            
            relation = new InsertPluginRelation(dropped, dragged, nextPlugin);
            
            if(relation.hasMultipleRelations()) {
                openPluginRelationWindow(relation.createRelations(project.getDatasetPlugin(), project), PluginEvent.INSERT_PLUGIN);
            } else {
                relation.establishRelations(null);
                insertPluginInModel();
            }
            
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToAddPlugin") + ": " + e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, flowChart,
                    Constants.POSITION_END_CENTER, 3000, true);
        }
    }

    private void insertPluginInModel() {
        List<Plugin> pluginList = project.getPlugins();
        
        pluginList.add(pluginList.indexOf(relation.getPrecursor()) + 1, relation.getInsertedPlugin());
        pluginsModel.add(pluginsModel.indexOf(relation.getPrecursor()) + 1, relation.getInsertedPlugin());
        
        //Reloading active plugin's form to refresh Plugin ID in the form 
        if(!activePlugin.isDatasourcePlugin()) {
            loadPluginForm(activePlugin);
        }
    }

    private void deletePlugin(Plugin plugin) {
        List<Plugin> pluginList = project.getPlugins();
        int pluginIndex = pluginList.indexOf(plugin);
        int pluginSize = pluginList.size() - 1;
        Plugin previousPlugin = pluginList.get(pluginIndex - 1);
        try {
            if (pluginIndex < pluginSize) {
                Plugin nextPlugin = pluginList.get(pluginIndex + 1);
                if (previousPlugin.hasMultipleOutputs() || nextPlugin.hasMultipleInputs()) {
                    deletedPlugin = plugin;
                    
                    PluginRelations pluginRelations = new PluginRelations();
                    pluginRelations.addPluginRelation(project.getDatasetPlugin(), new PluginRelation(previousPlugin, nextPlugin, project), project);
                    pluginRelations.setDeleteMessage(plugin);
                    
                    openPluginRelationWindow(pluginRelations, PluginEvent.DELETE_PLUGIN);
                } else {
                    deletePluginFromComposition(plugin);
                    nextPlugin.getContractInstance().addPrecursor(previousPlugin.getContractInstance());
                    deletePluginFromModel(plugin, previousPlugin);
                }
            } else {
                deletePluginFromComposition(plugin);
                deletePluginFromModel(plugin, previousPlugin);
            }

        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("compNotDeleted"), Clients.NOTIFICATION_TYPE_ERROR, flowChart, Constants.POSITION_END_CENTER,
                    3000, true);
        }
    }

    private void deletePluginFromComposition(Plugin plugin) throws HipieException {
        try {
            data.getComposition().removeContractInstance(plugin.getContractInstance());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Unable to delete plugin", e);
        }
    }
    
    private void deletePluginFromModel(Plugin plugin, Plugin previousPlugin) {
        project.getPlugins().remove(plugin);
        pluginsModel.remove(plugin);
        
        if (activePlugin == plugin) {
            selectPlugin(previousPlugin);
            targetPlugin = previousPlugin;
        } else {
            selectPlugin(activePlugin);
            targetPlugin = activePlugin;
        }
        
        disablePreviousNext();
    }

    @Listen("onEditRelation = #formLayout")
    public void editRelation(){
        //Formulation of all possible relations for active plugin
        PluginRelations relations = new PluginRelations();
        activePlugin.getPrecursorCIs(project)
                .forEach(ci -> relations.addPluginRelation(null, new PluginRelation(project.getPlugin(ci), activePlugin, project), project));
        
        openPluginRelationWindow(relations, PluginEvent.EDIT_RELATIONS);
    }
    
    private void processFillGCIDEvent(Event event) {
        JSONObject json = (JSONObject) new JSONParser().parse(event.getData().toString());
        field = json.get("name").toString();
        
        LOGGER.debug("From js: Top - {}, Left -{}", json.get("y"), json.get("x"));
        int y = (int) Double.parseDouble(json.get("y").toString());
        int x = (int) Double.parseDouble(json.get("x").toString());
        
        LOGGER.debug("Global var field - {}", field);
        //TODO append field label to list header
        globalVarPopup.open(x,y); 
        
        /*When active plugin is Hipie Plugin and the selected file is ChildDataset file, 
         * need to save the present form values, unless the parent field values will get lost
         */
        if(activePlugin.isLiveHIPEPlugin()){
            data.setNotifyUser(false);
            savePlugin(true, RampsUtil.getFileBrowserData(Constants.ACTION.SAVE));
        }
    }

    @Listen("onSelect = #globalVariablesList")
    public void populateSelectedVariable(SelectEvent<Component, GlobalVariable> event) {
        globalVarPopup.close();
        globalVariablesList.clearSelection();
        
        GlobalVariable globalVariable = event.getSelectedObjects().iterator().next();
        activePlugin.getContractInstance().setProperty(field, globalVariable.getNameToPopulate());
        
        selectPlugin(activePlugin);
    }
    
    private void processClientEvent(Event event) {
        JSONObject json = (JSONObject) new JSONParser().parse(event.getData().toString());
        if ((Constants.ACTION.SAVE.equals(json.get("flow")) || Constants.ACTION.SAVE_AS.equals(json.get("flow")))
                && Constants.SUCCESS.equals(json.get(STATUS))) {
            postSaveEvent();
        } else if (Constants.ACTION.RUN.equals(json.get("flow")) && Constants.SUCCESS.equals(json.get(STATUS))) {
            postRunEvent();
        } else if (Constants.ACTION.VALIDATE.equals(json.get("flow")) && Constants.SUCCESS.equals(json.get(STATUS))) {
            postValidationEvent();           
        } else if (targetPlugin != null && targetPlugin != activePlugin) {
            selectPlugin(targetPlugin);
        }
    }

    private void postValidationEvent() {
        if(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canEdit()){
            showValidatationSuccess();
        }else{
            Clients.showNotification(Labels.getLabel("donotHavePermissionToValidate"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    Constants.MESSAGE_VIEW_TIME,true);
        }
    }

    private void postRunEvent() {
        if(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canRun()){
            Events.postEvent(EVENTS.ON_RUN_INITIATED, this.getSelf().getParent().getParent().getFellow(CONTAINER), null);
        }else{
            Clients.showNotification(Labels.getLabel("donotHavePermissionToRun"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    Constants.MESSAGE_VIEW_TIME,true);
        }
    }

    private void postSaveEvent() {
        if(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canEdit()){
            Events.postEvent(EVENTS.ON_SAVE, this.getSelf().getParent().getParent().getFellow(CONTAINER), null);           
         }else{
             Clients.showNotification(Labels.getLabel("youDonotHavePermissionToEdit"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                     Constants.MESSAGE_VIEW_TIME,true);
         }
    }

    /**
     * Swaps the dragged/dropped plugins
     * 
     * @param event
     */
    private void swapPlugin(Event event) {
        @SuppressWarnings("unchecked")
        Map<String, Plugin> mapData = (Map<String, Plugin>) event.getData();
        Plugin draggedPlugin = mapData.get(Constants.DRAGGED);
        Plugin droppedPlugin = mapData.get(Constants.DROPPED);

        List<Plugin> prePluginList = new ArrayList<Plugin>(project.getPlugins());
        try {
            // Removes precursors before swapping - temporary fix until HIPIE
            // fixes
            for (int index = 1; index < project.getPlugins().size(); index++) {
                project.getPlugins().get(index).getContractInstance().removePrecursor(project.getPlugins().get(index - 1).getContractInstance());
            }

            Collections.swap(project.getPlugins(), project.getPlugins().indexOf(draggedPlugin), project.getPlugins().indexOf(droppedPlugin));

            for (int index = 1; index < project.getPlugins().size(); index++) {
                project.getPlugins().get(index).getContractInstance().addPrecursor(project.getPlugins().get(index - 1).getContractInstance());
            }

            pluginsModel = new ListModelList<Plugin>(project.getPlugins());
            flowChart.setModel(pluginsModel);
            selectPlugin(draggedPlugin);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToSwapPlugin"), Clients.NOTIFICATION_TYPE_ERROR, flowChart,
                    Constants.POSITION_END_CENTER, 3000, true);
            project.setPlugins(prePluginList);
        }

    }

    private void selectPlugin(Plugin plugin) {
        loadPlugin(plugin);
        showPluginDetails();
    }

    private void loadPlugin(Plugin plugin) {
        List<Plugin> selectedPlugins = new ArrayList<Plugin>();
        selectedPlugins.add(plugin);
        pluginsModel.setSelection(selectedPlugins);

        // Form
        if (plugin.isDatasourcePlugin()) {
            LOGGER.debug("project onbject - {}", project.getHpccConnection());
            //If the plugin is not a UseDataset, load plugin form
            if(plugin.getContract() != null && !(plugin instanceof DatasetPlugin)) {
            	formLayout.setVisible(true);
                browserInclude.setVisible(false);
                loadPluginForm(plugin);
            } else {
            	data.setFilePreviewTab(fileInfoTab);
                browserInclude.setDynamicProperty(Constants.TAB_DATA, data);
                browserInclude.setSrc(PROJECT_FILE_BROWSER_ZUL);
                formLayout.setVisible(false);
                browserInclude.setVisible(true);
            }
        } else {
            formLayout.setVisible(true);
            browserInclude.setVisible(false);
            loadPluginForm(plugin);
        }
        
        Contract contract = plugin.isDatasourcePlugin() ? ((DatasetPlugin)plugin).getUseDatasetContract() : plugin.getContractInstance().getContract();
        File readmefile = null;
        try {
        	readmefile = new File(new File(contract.getFileName()).getParent(),
                    Compliance.getReadmeFileName(contract));
            setPluginReadme(FileUtils.readFileToString(readmefile));
        } catch (IOException e) {
            setPluginReadme(Labels.getLabel("readMeFileMissing"));
            LOGGER.debug(Constants.HANDLED_EXCEPTION, Labels.getLabel("readMeFileMissing")+" "+readmefile);
        }
        setPluginSource(contract.toString());

        activePlugin = plugin;

        showHideFilePreview();

        if (contentHolder.getSrc() == null) {
            fileInfoPanel.setVisible(false);
            fileInfoTab.setVisible(false);
        }

        if (activePlugin.isDatasourcePlugin() && isFileInfoSelected) {
            fileInfoTab.setSelected(true);
        } else {
            // Select First visible tab in selected plugin
            propertiesTab.setSelected(true);
        }

        // Saving current plugin as a temporary measure to avoid invalid
        // validations from HIPIE
        if (!activePlugin.isDatasourcePlugin()) {
            savePlugin(false, RampsUtil.getFileBrowserData("temporary"));
        }

        // diabling the previous next button based on the active plugin
        disablePreviousNext();
    }

    private void loadPluginForm(Plugin plugin) {
        try {
            if(formLayout.getFirstChild() instanceof Hlayout) {
                formLayout.getFirstChild().detach();
            }
            List<SimplePluginRelation> relations = plugin.createSimpleRelations(project);
            
            LOGGER.debug("Simple relations - {}", relations);
            
            //Skipping edit row creation when no relations can be retrieved
            if(CollectionUtils.isNotEmpty(relations)) {
                //Setting editable attribute to control 'Edit' button creation within template
                Plugin precursor = project.getPlugins().get(project.getPlugins().indexOf(plugin) - 1);
                String editable = "editable";
                if(data.getFlow() != Flow.VIEW &&
                        (plugin.hasMultipleInputs() || precursor.hasMultipleOutputs())) {
                    formLayout.setAttribute(editable, true);
                } else {
                    formLayout.setAttribute(editable, false);
                }
                
                formLayout
                    .getTemplate("precursorRelation")
                    .create(formLayout, 
                            htmlHolder, 
                            name -> "relations".equals(name) ? relations: null, 
                                    null);            
                
            }
            
            htmlHolder.setContent(null);
            
            String form = HtmlGenerator.generateForm(project, plugin, pluginsModel.indexOf(plugin), htmlHolder.getUuid(), data.getProject().isShowGlobalVariable());
            htmlHolder.setContent(form);

            LOGGER.debug("Generated form. Id - {}\n{}\n{}", pluginsModel.indexOf(plugin), plugin, form);
        } catch (Exception e) {
            Clients.showNotification(Labels.getLabel("couldNotGenerateForm"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    @Listen("onClick = #next")
    public void onClickNext() {
        // Select Next Plugin if exists
        showPluginDetails();
        if (pluginsModel.size() > pluginsModel.indexOf(activePlugin) + 1) {
            targetPlugin = pluginsModel.get(pluginsModel.indexOf(activePlugin) + 1);
            savePlugin(true, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
        } else {
            savePlugin(false, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
            Clients.showNotification(Labels.getLabel(NO_MORE_PLUGINS), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    3000, true);
        }
    }

    @Listen("onClick = #previous")
    public void onClickPrevious() {
        // close the add pluins popup
        showPluginDetails();
        // Select previous Plugin if exists
        if (pluginsModel.indexOf(activePlugin) - 1 > -1) {
            targetPlugin = pluginsModel.get(pluginsModel.indexOf(activePlugin) - 1);
            savePlugin(true, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
        } else {
            savePlugin(false, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
            Clients.showNotification(Labels.getLabel(NO_MORE_PLUGINS), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                    3000, true);
        }
    }

    @Listen("onClick = #validate")
    public void onClickValidate() {
        
      //Check the contracts of the composition is exists on repository
        try {
            if (HipieSingleton.getHipie().getContract(activePlugin.getContract().getAuthor(),
                    activePlugin.getContract().getCanonicalName()) == null) {

                StringBuilder exeMsg = new StringBuilder(Labels.getLabel("contract"));
                exeMsg.append(" ");
                exeMsg.append(activePlugin.getContract().getName());
                exeMsg.append(" ");
                exeMsg.append(Labels.getLabel("notExistsInRepo"));
                throw new HipieException(exeMsg.toString());

            }

        } catch (Exception e) {
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        }
        
        showPluginDetails();
        if (activePlugin.isDatasourcePlugin()) {
            boolean isErrorOccured = savePlugin(false, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
            if (!isErrorOccured) {
                try {
                    showValidatationSuccess();
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                }
            }
        } else {
            savePlugin(false, RampsUtil.getFileBrowserData(Constants.ACTION.VALIDATE));
        }
    }

    private void showValidatationSuccess() {
        Clients.showNotification(Labels.getLabel("validateSuccess"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf(), Constants.POSITION_TOP_CENTER,
                3000);
    }

    private void showPluginDetails() {
        pluginDetails.setSelected(true);
        previous.setDisabled(false);
        next.setDisabled(false);
        validate.setVisible(true);
        disablePreviousNext();
    }

    /**
     * Saves the active plugin
     * 
     * @param doNavigation
     *            Specifies whether to navigate to targetPlugin
     * @param flow
     * @return Whether any validation error occurred while saving current
     *         plugin. Works only with HPCCConnection plugin & Datasource plugin
     *         Returns true otherwise
     */
    private boolean savePlugin(boolean doNavigation,FileBrowserData data) {
       String flow = data.getAction();
       boolean isValidPlugin = false;
       boolean navigate = doNavigation;
        if (activePlugin.isDatasourcePlugin()) {
            BooleanResult validation = new BooleanResult();
            data.setResult(validation);
            Events.sendEvent(EVENTS.ON_PAGE_CHANGE, browserInclude, data);
            
            if (!validation.isSuccess()) {
                //To avoid loading the selected plugin
                navigate = false;
            }
            if (navigate) {
                selectPlugin(targetPlugin);
            }
            
            // diabling the previous next button based on the active plugin
            disablePreviousNext();
            isValidPlugin = !validation.isSuccess();
        } else if (activePlugin.isLiveHIPEPlugin()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Plugin list - {}", project.getPlugins());
                LOGGER.debug("Saving plugin - {}", activePlugin.getName());
            }
            Clients.evalJavaScript("savePlugin('" + HtmlGenerator.generateFormId(project, activePlugin, pluginsModel.indexOf(activePlugin)) + "', '"
                    + flow + "','" + htmlHolder.getUuid() + "');");
        }
        
        return isValidPlugin;
    }

    @Listen("onDrop = #flowChart")
    public void onPluginDrop(DropEvent event) {
        
        if (event.getDragged() instanceof Treecell) {
            Plugin lastplugin = project.getPlugins().get(project.getPlugins().size() - 1);
            Treecell treecell = (Treecell) event.getDragged();
            Plugin plugin = (Plugin) treecell.getAttribute(Constants.PLUGIN);
            try {
                appendPlugin(lastplugin, plugin.clone());
            } catch (CloneNotSupportedException e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                        Constants.POSITION_TOP_CENTER, 3000, true);
                return;
            }
        } else {
            Clients.showNotification(Labels.getLabel("dropPlugin"), "warning", flowChart, Constants.POSITION_END_CENTER, 3000, true);
        }

    }

    /**
     * 
     * @param precursorPlugin
     *  Must be the last plugin
     * @param plugin
     */
    private void appendPlugin(Plugin precursorPlugin, Plugin plugin) {
        try {
            HIPIEUtil.associateContractInstance(plugin, project.hasReferenceId() ? project.getBaseScope() : null);

            if (precursorPlugin.hasMultipleOutputs() || plugin.hasMultipleInputs()) {
                PluginRelations pluginRelations = new PluginRelations();
                pluginRelations.addPluginRelation(project.getDatasetPlugin(), new PluginRelation(precursorPlugin, plugin, project), project);
                
                pluginRelations.setAddMessage(plugin);
                
                openPluginRelationWindow(pluginRelations, PluginEvent.APPEND_PLUGIN);
            } else {
                plugin.getContractInstance().addPrecursor(precursorPlugin.getContractInstance());
                appendPluginToModel(plugin);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            Clients.showNotification(Labels.getLabel(COULD_NOT_HOOK)+"\"" +precursorPlugin.getName()+"\" \n"+ message, Clients.NOTIFICATION_TYPE_ERROR, flowChart, Constants.POSITION_END_CENTER,
                    3000, true);
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    private void appendPluginToModel(Plugin destPlugin) {
        pluginsModel.add(destPlugin);
        project.getPlugins().add(destPlugin);
    }

    private void openPluginRelationWindow(PluginRelations pluginRelations, PluginEvent event) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.PLUGIN_RELATION, pluginRelations);
        params.put(Constants.PROJECT, project);
        
        pluginEvent = event;
        
        Window window = (Window) Executions.createComponents("/ramps/project/plugin_relation.zul", getSelf(), params);
        window.doModal();
    }

    @Listen("onClick = #addStep")
    public void onAddPlugins() {
        if (!activePlugin.isDatasourcePlugin()) {
            savePlugin(false, RampsUtil.getFileBrowserData(Constants.ACTION.NAVIGATION));
        }
        pluginSelection.setSelected(true);
        previous.setVisible(false);
        next.setVisible(false);
        validate.setVisible(false);
        addNextStep.setVisible(false);
        Clients.evalJavaScript("refactorPluginLabels()"); 
    }

    @Listen("onClick = #addNextStep")
    public void onaddNextStep(Event event) {
        onAddPlugins();
    }

    @Listen("onClose = #pluginsWindow")
    public void onClosePluginBrowser(Event event) {
        showPluginDetails();
        event.stopPropagation();
    }

    @Listen("onSelect = #detailsTabbox")
    public void onSelectTabbox(Event event) {
        if (event.getTarget().equals(fileInfoTab)) {
            isFileInfoSelected = true;
        } else {
            isFileInfoSelected = false;
        }
    }

    private void disablePreviousNext() {

        if (pluginsModel.indexOf(activePlugin) == 0) {
            previous.setVisible(false);
        } else {
            previous.setVisible(true);
        }

        if (pluginsModel.lastIndexOf(activePlugin) == (pluginsModel.size() - 1)) {
            next.setVisible(false);
        } else {
            next.setVisible(true);
        }

        if (activePlugin.isDatasourcePlugin()) {
            validate.setDisabled(true);
        } else if (data.getFlow() != Flow.VIEW) {
            validate.setDisabled(false);
        }

        if (pluginsModel.indexOf(activePlugin) == (pluginsModel.size() - 1)) {
            addNextStep.setVisible(true);
        } else {
            addNextStep.setVisible(false);
        }
    }

    public boolean isView() {
        return isView;
    }

    public void setView(boolean isView) {
        this.isView = isView;
    }

    /**
     * Shows logical file preview tabs for Datasource plugins and hides then
     * when other plugins are active
     */
    private void showHideFilePreview() {
        if (activePlugin.isDatasourcePlugin()) {
            fileInfoTab.setVisible(true);
            fileInfoPanel.setVisible(true);

        } else {
            fileInfoTab.setVisible(false);
            fileInfoPanel.setVisible(false);
        }
    }

    private void setPluginReadme(String readme) {
        readMe.setValue(readme);
    }

    private void setPluginSource(String sourceString) {
        if (((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().isGlobalAdmin()
                || ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getPermission().getRampsPermission().canViewPluginSource()) {
            pluginSource.setValue(sourceString);
        }
    }
}
