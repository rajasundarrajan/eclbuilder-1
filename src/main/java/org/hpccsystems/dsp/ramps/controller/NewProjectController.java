package org.hpccsystems.dsp.ramps.controller;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.axis.utils.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin.ERROR;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.dsp.service.impl.PluginServiceImpl;
import org.hpccsystems.usergroupservice.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class NewProjectController extends SelectorComposer<Window> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewProjectController.class);

    private Map<String, HPCCConnection> connections;

    @Wire
    private Combobox connectionList;

    @Wire("#thorCluster, #roxieCluster, #connectionList")
    List<Combobox> clusterConnection;

    @Wire
    private Combobox thorCluster;
    @Wire
    private Combobox roxieCluster;
    @Wire
    private Combobox template;
    @Wire
    private Textbox compositionName;
    @Wire
    private Label chooseTemplate;
    @Wire
    private Textbox gcId;
    @Wire
    private Button searchPopbtn;

    @Wire
    private Include searchGCIDIncludeRamps;

    private Component parent;
    private TabData tabData;
    private Project project;
    private Flow flow;
    User user;
    final ListModelList<String> connectionsModel = new ListModelList<String>();
    final ListModelList<String> thorClusterModel = new ListModelList<String>();
    final ListModelList<String> roxieClusterModel = new ListModelList<String>();
    final ListModelList<Project> tempaltesModel = new ListModelList<Project>();

    @Override
    public void doAfterCompose(Window comp) throws Exception {
        super.doAfterCompose(comp);
        user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        parent = this.getSelf().getParent();

        tabData = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        flow = tabData.getFlow();
        project = tabData.getProject();

        connections = HipieSingleton.getHipie().getHpccManager().getConnections();
        List<Project> projectList;
        projectList = ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getProjectTemplates(user);

        applyingTemplateVisuals(projectList);

        connectionList.setModel(connectionsModel);
        thorCluster.setModel(thorClusterModel);
        roxieCluster.setModel(roxieClusterModel);

        // Rendering templates to UI
        template.setModel(tempaltesModel);
        tempaltesModel.addAll(projectList);
        ComboitemRenderer<Project> templatesRenderer = (comboitem, project, index) -> comboitem
                .setLabel(Constants.BASIC_TEMPLATE.equals(project.getName()) ? "None" : project.getLabel());

        template.setItemRenderer(templatesRenderer);

        if (user.isGlobalAdmin()) {
            // Rendering available Conncetions to UI
            connectionsModel.addAll(connections.keySet());
        } else {
            // add public & custom clusters for non-administrators
            Set<String> clusters = ((HPCCService)SpringUtil.getBean(Constants.HPCC_SERVICE)).getPublicAndCustomClusters(user, connections);
            if (!(clusters.isEmpty())) {
                connectionsModel.addAll(clusters);
            }
        }

        connectionList.setItemRenderer((comboitem, hpccId, index) -> comboitem.setLabel(connections.get(hpccId).getLabel()));

        if (flow != Flow.NEW && project.getHpccConnection() != null) {
            populateHpccData(project.getClusterConfig());
        }

        if (isSearchHidden()) {
            searchPopbtn.setVisible(false);
        }

        populateGCID();

        searchGCIDIncludeRamps.addEventListener(Constants.EVENTS.ON_SELECT_COMPANY_ID, event -> {
            Company selectedCompany = (Company) event.getData();
            gcId.setValue(String.valueOf(selectedCompany.getGcId()));
        });

    }

    private void populateGCID() {
        if (!StringUtils.isEmpty(tabData.getProject().getReferenceId())) {
            gcId.setText(String.valueOf(tabData.getProject().getReferenceId()));
        }
    }

    private boolean isSearchHidden() {
        return flow == Flow.EDIT || flow == Flow.VIEW;
    }

    private void applyingTemplateVisuals(List<Project> projectList) {
        if (tabData != null && Flow.CLONE == tabData.getFlow()) {
            template.setVisible(false);
            chooseTemplate.setVisible(false);
        } else if (tabData != null && (Flow.EDIT == tabData.getFlow() || Flow.VIEW == tabData.getFlow())) {
            template.setVisible(false);
            chooseTemplate.setVisible(false);
            compositionName.setValue(tabData.getProject().getLabel());
            compositionName.setDisabled(true);

        } else {
            tempaltesModel
                    .setSelection(projectList.stream().filter(proj -> Constants.BASIC_TEMPLATE.equals(proj.getName())).collect(Collectors.toList()));
        }
    }

    private void populateHpccData(ClusterConfig clusterConfig) {
        if (clusterConfig.getId() != null && !clusterConfig.getId().isEmpty()) {
            Set<String> set = new HashSet<String>();
            set.add(clusterConfig.getId());
            connectionsModel.setSelection(set);

            showHPCCConnectionInfo(clusterConfig.getId());

            if (clusterConfig.getThorCluster() != null) {
                set = new HashSet<String>();
                set.add(clusterConfig.getThorCluster());
                thorClusterModel.setSelection(set);
            }

            if (clusterConfig.getRoxieCluster() != null) {
                set = new HashSet<String>();
                set.add(clusterConfig.getRoxieCluster());
                roxieClusterModel.setSelection(set);
            }
        }

    }

    private void showHPCCConnectionInfo(String hpccId) {
        long startTime = Instant.now().toEpochMilli();
        HPCCConnection connection = connections.get(hpccId);
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.GET_HPCC_CONNECTION, startTime, "Obtained thor and roxy settings"));
        }
        try {
            connection.testConnection();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("hpccConnectionFailed") + ": " + e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, connectionList,
                    Constants.POSITION_END_AFTER, 5000, true);
            connectionsModel.clearSelection();
            return;
        }
        thorClusterModel.clear();
        if (connection.getThorclusters() != null && !connection.getThorclusters().isEmpty()) {
            thorClusterModel.addAll(connection.getThorclusters());
            Set<String> thorSetfirstItem = new HashSet<String>();
            thorSetfirstItem.add(connection.getThorclusters().get(0));
            thorClusterModel.setSelection(thorSetfirstItem);
        } else {
            thorClusterModel.clearSelection();
        }

        roxieClusterModel.clear();
        
        //set roxie selections from list of roxie and hthor
        Set<String> roxieSetfirstItem = new HashSet<String>();
        
        boolean clearRoxieSelection = true;
        if (connection.getRoxieclusters() != null && !connection.getRoxieclusters().isEmpty()) {
            roxieClusterModel.addAll(connection.getRoxieclusters());
            clearRoxieSelection = false;
            roxieSetfirstItem.add(connection.getRoxieclusters().get(0));
            roxieClusterModel.setSelection(roxieSetfirstItem);
        } else if (connection.getHthorclusters() != null && !connection.getHthorclusters().isEmpty()) {
            roxieClusterModel.addAll(connection.getHthorclusters());
            clearRoxieSelection = false;
            roxieSetfirstItem.add(connection.getHthorclusters().get(0));
            roxieClusterModel.setSelection(roxieSetfirstItem);
        }
        
        if (clearRoxieSelection) {
            roxieClusterModel.clearSelection();
        }

    }

    @Listen("onSelect = #connectionList")
    public void onSelectHPCCConnection(SelectEvent<Component, String> event) {
        showHPCCConnectionInfo(event.getSelectedObjects().iterator().next());
    }

    @Listen("onClick = #closeDashboardDialog")
    public void onClose() {

        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

    @Listen("onClick = #continueBtn; onOK = #createNewProjectWindow")
    public void onCreateProject() {

        if (!validateAllFields()) {
            return;
        }
        try {
            if (flow == Flow.EDIT) {
                HPCCConnection newConnection = RampsUtil
                        .getHpccConnection(connectionsModel.getSelection().iterator().next());
                ERROR error = tabData.getProject().getDatasetPlugin().validate(newConnection);
                if (error == ERROR.NO_ERROR) {
                    continueProjectCreation();
                } else {
                    confirmClusterChange(error, newConnection);
                }
            } else {
                continueProjectCreation();
            }
        } catch (Exception e) {
            LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(e.getMessage(),Clients.NOTIFICATION_TYPE_ERROR,
                    compositionName, Constants.POSITION_END_CENTER, 3000);
            return;
        }
    }

    private void continueProjectCreation() throws WrongValueException, HipieException, RepoException {
        String escapedName =  HIPIEUtil.createCompositionName(compositionName.getValue(), false);
        Composition composition;
        boolean fileExists = false;
        fileExists = checkForDuplicateFile();

        if ((flow == Flow.NEW || flow == Flow.CLONE) && fileExists) {
            Clients.showNotification(
                    Labels.getLabel("compositionAlreadyExists1").concat(
                            compositionName.getText().concat(Labels.getLabel("compositionAlreadyExists2"))), Clients.NOTIFICATION_TYPE_ERROR,
                    compositionName, Constants.POSITION_END_CENTER, 3000);
            return;
        }

        // Retriving Composition
        composition = retriveComposition(tabData.getComposition(), escapedName);

        // Reseting permissions
        // Adding data from UI
        if (!resetPermissionAndAddUIData(composition, escapedName)) {
            return;
        }

        // Reset the plugin properties in CLONE flow, if GCID is introduced or
        // changed
        resetPlugin(composition);

        tabData.setComposition(composition);


        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setId(connectionsModel.getSelection().iterator().next());
        clusterConfig.setThorCluster(thorClusterModel.getSelection().iterator().next());
        clusterConfig.setRoxieCluster(roxieClusterModel.getSelection().iterator().next());

        project.setClusterConfig(clusterConfig);

        LOGGER.debug("Composition object while sending event - {}", tabData.getComposition());
        // Updates composition in DB with new cluster config data in edit flow
        if (flow == Flow.EDIT) {
            try {
                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).saveClusterConfig(tabData.getComposition(), project.getClusterConfig());
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                        true);
                return;
            }
        }

        Events.postEvent(EVENTS.ON_OPEN_COMPOSITION, parent.getFellow("homeTabbox"), tabData);
        this.getSelf().detach();

        addNameToSession();
    }

    private void resetPlugin(Composition composition) {
        if (tabData.getFlow() == Flow.CLONE && !StringUtils.isEmpty(gcId.getValue()) && !gcId.getValue().equals(project.getReferenceId())) {
            project.setReferenceId(gcId.getValue());
            RampsUtil.resetPlugingProperties(composition, project);
        }
    }

    private void addNameToSession() {
        // Add Current composition name to session
        if (tabData.getFlow() == Flow.NEW || tabData.getFlow() == Flow.CLONE) {
            RampsUtil.getOpenProjectLabels().add(compositionName.getValue().trim());
        }
    }

    private boolean checkForDuplicateFile() throws HipieException {
        
        try {
            return RampsUtil.isFileNameDuplicate(user, compositionName.getValue().trim());
        } catch (Exception e1) {
            LOGGER.error(Constants.EXCEPTION, e1);
            throw new HipieException("Could not check the for duplicacy", e1);
        }
    }
    
    private void confirmClusterChange(ERROR error, HPCCConnection hpccConnection) {
        String message;
        
        switch (error) {
        case FILE_MISSING:
            message = Labels.getLabel("clusterChangeFileNotExists");
            break;
            
        case STRUCTURE_MISMATCH:
            message = Labels.getLabel("clusterChangeStructurMismatch");
            break;
            
        default:
            message="";
            break;
        }
        
        Messagebox.show(message ,Labels.getLabel("hpccClusterUpdate"),
                new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, 
                Messagebox.QUESTION, evet -> updateCluster(error, hpccConnection, evet));
    }

    private void updateCluster(ERROR error, HPCCConnection hpccConnection, ClickEvent evet) throws HipieException, RepoException {
        if (Messagebox.Button.YES.equals(evet.getButton())) {
            if(error == ERROR.STRUCTURE_MISMATCH) {
                tabData.getProject().getDatasetPlugin().updateStructures(hpccConnection);
                tabData.setDatasourceValidated(true);
            }
            continueProjectCreation();
        } else {
            // Clear cluster selection
            clusterConnection.forEach(combobox -> ((ListModelList<Object>) combobox.getModel()).clearSelection());
        }
    }

    private boolean resetPermissionAndAddUIData(Composition composition, String escapedName) {
        if (flow == Flow.NEW || flow == Flow.CLONE) {
            resetPermissions(composition, project, user.getId());
        }

        return addUIData(composition, escapedName);
    }

    private boolean addUIData(Composition composition, String escapedName) {
        boolean success = true;
        if (flow == Flow.NEW || flow == Flow.CLONE) {
            project.setName(escapedName);
            project.setLabel(compositionName.getValue());
            if (flow == Flow.NEW) {
                composition.setName(escapedName);
                composition.setLabel(compositionName.getValue());
            }
            if ((flow == Flow.NEW && Constants.BATCH_COMPOSITION.equals(tempaltesModel.getSelection().iterator().next().getName()))
                    //In clone flow, if the user has changed the GCID,fetch the compliance tags for the newly selected GCID,otherwise continue with old tag values in CMP file
                    || (flow == Flow.CLONE && project.isBatchTemplate() && !project.getReferenceId().equals(gcId.getValue()))) {
                project.setBatchTemplate(true);
                success = RampsUtil.setGCIDcompliance(composition, this.getSelf(), gcId.getValue());
                if(!success) {
                    return false;
                }
            } 
            
            if (!StringUtils.isEmpty(gcId.getValue())) {
                project.setReferenceId(gcId.getValue());
                // Add reference Id to Composition
                RampsUtil.addReferenceId(composition, gcId.getValue());
            }
        }
        return success;
    }

    private Composition retriveComposition(Composition composition, String escapedName) throws RepoException, HipieException {
        Composition comp = composition;
        if (flow == Flow.CLONE) {
            comp = CompositionUtil.cloneComposition(tabData.getComposition(), escapedName, compositionName.getValue(),
                    user.getId(), true, tabData.getDashboard(), tabData.getProject().getDatasourceStatus());
        } else if (flow == Flow.EDIT || flow == Flow.VIEW) {
            try {
                comp = project.getComposition();
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                throw new HipieException("Unable to get compositions", e);
            }
        } else {
            try {
                HIPIEService hipieService=HipieSingleton.getHipie();
                Composition comptemp = hipieService.getCompositionTemplate(user.getId(),
                        tempaltesModel.getSelection().iterator().next().getCanonicalName());
                comp = new Composition(comptemp);
                if (user.getMbsUser() != null && user.getMbsUser().getNVP(PluginServiceImpl.PLUGINREPOSITORY) != null) {
                    // get the default repo name
                    String defaultrepo = hipieService.getRepositoryManager().getDefaultRepository().getName();

                    // get the forked repo name this user is using
                    Set<NameValuePair> nvps = user.getMbsUser().getNVP(PluginServiceImpl.PLUGINREPOSITORY);
                    String forkedrepo = nvps.iterator().next().getValue();

                    // if this forked repo exists, iterate through the
                    // composition's contract instances
                    // and switch out HIPIE_Plugins. plugins to their forked
                    // version
                    switchOutHipiePlugins(comp, hipieService, comptemp, defaultrepo, forkedrepo);
                }
            } catch (Exception ex) {
                LOGGER.error(Constants.EXCEPTION, ex);
                throw new HipieException(Labels.getLabel("templateMissing"), ex);
            }
        }
        return comp;
    }

    private void switchOutHipiePlugins(Composition comp, HIPIEService hipieService, Composition comptemp, String defaultrepo, String forkedrepo)
            throws HipieException {
        if (hipieService.getRepositoryManager().getRepos().containsKey(forkedrepo)) {
            for (ContractInstance ci : comptemp.getContractInstances().values()) {
                Contract c = ci.getContract();
                if (defaultrepo != null && defaultrepo.equals(c.getRepositoryName())) {
                    Contract newc;
                    try {
                        newc = hipieService.getContract(user.getId(), c.getName(), forkedrepo, c.getVersion(), false);
                    } catch (Exception e) {
                        throw new HipieException(e);
                    }
                    addContractInstance(comp, forkedrepo, ci, newc);
                }
            }
        }
    }

    private void addContractInstance(Composition comp, String forkedrepo, ContractInstance ci, Contract newc) {
        if (newc != null && newc.getRepositoryName() != null && newc.getRepositoryName().equals(forkedrepo)) {
            ContractInstance ci2 = newc.createContractInstance(ci.getInstanceID());
            for (ElementOption eo : ci.getOptions().values()) {
                ci2.addOption(eo);
            }
            ci2.setAllProperties(ci.getProps());
            comp.getContractInstances().remove(ci.getName());
            comp.addContractInstance(ci2);
        }
    }

    private boolean validateAllFields() {
        if (flow == Flow.NEW && (StringUtils.isEmpty(gcId.getValue())
                && Constants.BATCH_COMPOSITION.equals(tempaltesModel.getSelection().iterator().next().getName()))) {
            Clients.showNotification(Labels.getLabel("pleaseChooseGcid"), Clients.NOTIFICATION_TYPE_ERROR, gcId, Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (!isSearchHidden() && user.getPermission().getRampsPermission().getUiPermission().isCompanyIdMandatory()
                && StringUtils.isEmpty(gcId.getValue())) {
            Clients.showNotification(Labels.getLabel("chooseReferenceId"), Clients.NOTIFICATION_TYPE_ERROR, gcId, Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (StringUtils.isEmpty(compositionName.getValue())) {

            Clients.showNotification(Labels.getLabel("compositionNameValidate"), Clients.NOTIFICATION_TYPE_ERROR, compositionName,
                    Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (Character.isDigit(compositionName.getValue().charAt(0))) {
            
            Clients.showNotification(Labels.getLabel("cmpNameAlphabetOnly"), Clients.NOTIFICATION_TYPE_ERROR, compositionName,
                    Constants.POSITION_END_AFTER, 3000);
            return false;
        }

        return RampsUtil.validateClusterSelection(clusterConnection);
    }

 
    private void resetPermissions(Composition composition, Project project, String author) {
        project.setAuthor(author);
        composition.setAuthor(author);
        Map<PermissionType, Permission> permissions = composition.getPermissions();
        for (Entry<PermissionType, Permission> entry : permissions.entrySet()) {
            entry.getValue().setPermissionLevel(PermissionLevel.PRIVATE);
            entry.getValue().getGroups().clear();
            entry.getValue().getUserIds().clear();
            composition.getPermissions().put(entry.getKey(), entry.getValue());
        }
    }

}
