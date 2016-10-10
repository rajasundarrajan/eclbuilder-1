package org.hpccsystems.dsp.dashboard.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.HPCCService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class NewDashboardController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewDashboardController.class);

    private DashboardConfig dashboardConfig;

    @Wire
    private Textbox dashboardName;

    final ListModelList<String> connectionsModel = new ListModelList<String>();
    final ListModelList<String> thorClusterModel = new ListModelList<String>();
    final ListModelList<String> roxieClusterModel = new ListModelList<String>();

    @Wire
    private Radiogroup datasource;

    @Wire
    private Radio hpccRadio;

    @Wire
    private Radio staticRadio;

    @Wire
    private Vlayout hpccContainer;

    @Wire
    private Combobox connectionList;

    @Wire
    private Combobox thorCluster;
    @Wire
    private Combobox roxieCluster;

    @Wire
    private Textbox gcIdDashboard;

    @Wire
    private Include searchInclude;

    @Wire
    private Button searchPopbtnDashboard;

    @Wire
    private Label securitySettingsWarning;

    @Wire
    private Label cloningCompInfo;

    @Wire
    private Checkbox convertComposition;

    private Flow flow;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        User user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(1);

        Map<String, HPCCConnection> connections = HipieSingleton.getHipie().getHpccManager().getConnections();

        if (dashboardConfig != null && (Flow.EDIT == dashboardConfig.getFlow() || Flow.VIEW == dashboardConfig.getFlow())) {
            dashboardName.setValue(dashboardConfig.getDashboard().getLabel());
            dashboardName.setDisabled(true);

        }
        connectionList.setModel(connectionsModel);
        thorCluster.setModel(thorClusterModel);
        roxieCluster.setModel(roxieClusterModel);

        if (user.isGlobalAdmin()) {
            // Rendering available Conncetions to UI
            connectionsModel.addAll(connections.keySet());
        } else {
            // add public & custom clusters for non-administrators
            Set<String> clusters = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getPublicAndCustomClusters(user, connections);
            if (!(clusters.isEmpty())) {
                connectionsModel.addAll(clusters);
            }
        }

        connectionList.setItemRenderer(new ComboitemRenderer<String>() {

            @Override
            public void render(Comboitem comboitem, String hpccId, int index) throws Exception {
                comboitem.setLabel(connections.get(hpccId).getLabel());
            }
        });

        if (isSearchHidden()) {
            searchPopbtnDashboard.setVisible(false);
        }

        populateGCID();

        searchInclude.setDynamicProperty(Constants.PARENT, this.getSelf().getParent());
        searchInclude.addEventListener(Constants.EVENTS.ON_SELECT_COMPANY_ID, (SerializableEventListener<? extends Event>)event -> {
            Company selectedCompany = (Company) event.getData();
            gcIdDashboard.setValue(String.valueOf(selectedCompany.getGcId()));
        });
     boolean isStatic=false;

        if (flow != Flow.NEW && dashboardConfig.getDashboard().getHpccConnection() != null) {
            datasource.setSelectedIndex(0);
            hpccContainer.setVisible(true);
            populateHpccData(dashboardConfig.getDashboard().getClusterConfig());
            searchPopbtnDashboard.setVisible(false);
            isStatic=false;
        }else if(dashboardConfig.getDashboard().isStaticData()){
            datasource.setSelectedIndex(1);
            isStatic=true;
        }

         convertComposition.setVisible(userCanConvert() & isCloning() & !hasRoxie());
         if(isStatic){convertComposition.setVisible(false);}

        if (isCloning()){
            if(isStatic){
                hpccRadio.setDisabled(true);
          } else {
                staticRadio.setDisabled(true);
            }

        if (dashboardConfig.getFlow() == Flow.CLONE && securitySettingsWarning != null) {
            securitySettingsWarning.setVisible(true);
        }

    }
    }

    @Listen("onClick = #convertComposition")
        public void warnConvert(){
            if(convertComposition.isChecked() & userCanConvert()){
                 cloningCompInfo.setVisible(true);
            }
            else {
                cloningCompInfo.setVisible(false);
            }
    }

    @Listen("onClick = #datasource")
    public void setDatasource() {
        String selectedSource = datasource.getSelectedItem().getValue();
        if ("hpcc".equals(selectedSource)) {
            hpccContainer.setVisible(true);
            dashboardConfig.getDashboard().setStaticData(false);
            getSelf().invalidate();
            if (isCloning() & userCanConvert()){
                 convertComposition.setVisible(true);
            }
        } else if ("staticdata".equals(selectedSource)) {
            hpccContainer.setVisible(false);
            dashboardConfig.getDashboard().setStaticData(true);
            convertComposition.setVisible(false);
        }
    }

    @Listen("onClick = #continueBtn")
    public void createDashboardTab() {
        try {
            if (!isSearchHidden() && datasource.getSelectedItem().getValue().equals("hpcc")
                    && ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
                            .getPermission().getDashboardPermission().getUiPermission().isCompanyIdMandatory()
                    && StringUtils.isEmpty(gcIdDashboard.getValue())) {
                Clients.showNotification(Labels.getLabel("chooseReferenceId"), Clients.NOTIFICATION_TYPE_ERROR,
                        gcIdDashboard, Constants.POSITION_END_AFTER, 3000);
                return;
            }

            if (StringUtils.isEmpty(dashboardName.getValue())) {

                Clients.showNotification(Labels.getLabel("dashboardName"), Clients.NOTIFICATION_TYPE_ERROR, dashboardName,
                        Constants.POSITION_END_AFTER, 3000);
                return;
            }
            if (Character.isDigit(dashboardName.getValue().charAt(0))) {

                Clients.showNotification(Labels.getLabel("dashNameAlphabetOnly"), Clients.NOTIFICATION_TYPE_ERROR, dashboardName,
                        Constants.POSITION_END_AFTER, 3000);
                return;
            }
            if (datasource.getSelectedItem() == null) {
                Clients.showNotification(Labels.getLabel("selectDatasource"), Clients.NOTIFICATION_TYPE_ERROR, datasource,
                        Constants.POSITION_END_AFTER, 3000);
                return;
            }

            if (!dashboardConfig.getDashboard().isStaticData()) {
                boolean saveSucess = saveClusterConfig();
                if (!saveSucess) {
                    return;
                }
            }

            if (dashboardConfig.getFlow() == Flow.NEW || dashboardConfig.getFlow() == Flow.CLONE) {
                boolean fileExists;
                fileExists = validateDashboardLabel(dashboardName.getValue());

                if (fileExists) {
                    Clients.showNotification(
                            Labels.getLabel("dashboardAlreadyExists1")
                                    .concat(dashboardName.getText().concat(Labels.getLabel("dashboardAlreadyExists2"))),
                            Clients.NOTIFICATION_TYPE_ERROR, dashboardName, Constants.POSITION_END_CENTER, 3000);
                    return;
                }

                String compositionName = dashboardName.getValue().replaceAll("[^ a-zA-Z0-9]+","");
                compositionName= compositionName.trim();
                retriveNewOrClonedComposition(compositionName);
                if (!StringUtils.isEmpty(gcIdDashboard.getValue())) {
                    dashboardConfig.getDashboard().setReferenceId(gcIdDashboard.getValue());
                    // Add reference Id to Composition
                    RampsUtil.addReferenceId(dashboardConfig.getComposition(), gcIdDashboard.getValue());
                }

                // Reseting permissions
                resetPermissions(dashboardConfig.getComposition(), dashboardConfig.getDashboard(),
                        ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()
                                .getId());

                // Add Current composition name to session
                getOpenDashboardLabels().add(compositionName);

            }
            // Updates dashboard in DB with new cluster config data in edit flow
            if (dashboardConfig.getFlow() == Flow.EDIT && !dashboardConfig.getDashboard().isStaticData()) {
                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).saveClusterConfig(
                        dashboardConfig.getComposition(), dashboardConfig.getDashboard().getClusterConfig());
            }
            if(convertComposition.isChecked() & userCanConvert()){
                Events.postEvent(Dashboard.EVENTS.ON_OPEN_COMPOSITION, dashboardConfig.getHomeTabbox(), dashboardConfig);
                Events.postEvent(Events.ON_CLOSE, getSelf(), null);
            }
            else {
                Events.postEvent(Dashboard.EVENTS.ON_OPEN_DASHBOARD, dashboardConfig.getHomeTabbox(), dashboardConfig);
                Events.postEvent(Events.ON_CLOSE, getSelf(), null);
            }
            LOGGER.debug("Creating Dashboard with data type: Static - {}",
                    dashboardConfig.getDashboard().isStaticData());
        } catch (WrongValueException e) {
            Clients.showNotification(Labels.getLabel("validDashboard"), Clients.NOTIFICATION_TYPE_ERROR,
                    dashboardName, Constants.POSITION_END_AFTER, 3000);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        } catch (DatabaseException | HipieException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToCreateComposition"), Clients.NOTIFICATION_TYPE_ERROR, dashboardName,
                    Constants.POSITION_END_AFTER, 3000);
           return;
        }
    }

    private void retriveNewOrClonedComposition(String compositionName) throws RepoException, HipieException {

        if (dashboardConfig.getFlow() == Flow.NEW) {
            Composition newComposition = null;
            try {
                newComposition = new Composition(dashboardConfig.getComposition());
                HIPIEService hipieService = HipieSingleton.getHipie();
                Composition temp = hipieService.saveCompositionAs(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), newComposition, compositionName);
                newComposition = temp;
                hipieService.deleteComposition(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), temp);
            } catch (Exception e) {
                LOGGER.error(Constants.ERROR, e);
                throw new HipieException(e);
            }
            dashboardConfig.setComposition(newComposition);
            dashboardConfig.getComposition().setName(compositionName);
            dashboardConfig.getComposition().setLabel(dashboardName.getValue());
            dashboardConfig.getDashboard().setWidgets(new ArrayList<Widget>());
            dashboardConfig.getDashboard().setName(compositionName);
            dashboardConfig.getDashboard().setLabel(dashboardName.getValue());

        } else {
            boolean doConvert=convertComposition.isChecked();
            DashboardConfig newDashboardConfig = DashboardUtil.cloneDashboard(dashboardConfig, compositionName,
                    ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), doConvert);


            dashboardConfig = newDashboardConfig;
         }
    }

    private void populateGCID() {
        if (!StringUtils.isEmpty(dashboardConfig.getDashboard().getReferenceId())) {
            gcIdDashboard.setText(String.valueOf(dashboardConfig.getDashboard().getReferenceId()));
        }
    }

    private boolean saveClusterConfig() {
        if (StringUtils.isEmpty(connectionList.getValue())) {

            Clients.showNotification(Labels.getLabel("chooseConnection"), Clients.NOTIFICATION_TYPE_ERROR, connectionList,
                    Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (StringUtils.isEmpty(thorCluster.getValue())) {

            Clients.showNotification(Labels.getLabel("chooseThorCluster"), Clients.NOTIFICATION_TYPE_ERROR, thorCluster, Constants.POSITION_END_AFTER,
                    3000);
            return false;
        }
        if (StringUtils.isEmpty(roxieCluster.getValue())) {

            Clients.showNotification(Labels.getLabel("chooseRoxieCluster"), Clients.NOTIFICATION_TYPE_ERROR, roxieCluster,
                    Constants.POSITION_END_AFTER, 3000);
            return false;
        }

        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setId(connectionsModel.getSelection().iterator().next());
        clusterConfig.setThorCluster(thorClusterModel.getSelection().iterator().next());
        clusterConfig.setRoxieCluster(roxieClusterModel.getSelection().iterator().next());
        dashboardConfig.getDashboard().setClusterConfig(clusterConfig);
        return true;
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

    @Listen("onSelect = #connectionList")
    public void onSelectHPCCConnection(SelectEvent<Component, String> event) {
        showHPCCConnectionInfo(event.getSelectedObjects().iterator().next());
    }

    private void showHPCCConnectionInfo(String hpccId) {
        long startTime = Instant.now().toEpochMilli();
        HPCCConnection connection = HipieSingleton.getHipie().getHpccManager().getConnections().get(hpccId);
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
        if (connection.getThorclusters() != null) {
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

    @SuppressWarnings("unchecked")
    private Set<String> getOpenDashboardLabels() {
        Set<String> fileNameSet = (Set<String>) Sessions.getCurrent().getAttribute(Constants.OPEN_DASHBOARD_LABELS);
        if (fileNameSet == null) {
            fileNameSet = new HashSet<String>();
            Sessions.getCurrent().setAttribute(Constants.OPEN_DASHBOARD_LABELS, fileNameSet);
        }
        return fileNameSet;
    }

    /**
     * Checks to see if the dashboard that is being created has the same name as an existing dashboard.
     * @param label - The name of the new dashboard
     * @return
     */
    private boolean validateDashboardLabel(String label) {
        boolean labelExists;
        try {
            labelExists =  ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).getDashboards(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()).stream()
                    .filter(comp -> comp.getLabel().equals(label)).count() > 0;
        } catch (Exception d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return true;
        }
        if (labelExists) {
            return labelExists;
        }

        Set<String> fileNameSet = getOpenDashboardLabels();
        return fileNameSet.contains(label);
    }

    /**
     * Checks to see what flow we are in
     * @return True if edit or view, false if otherwise.
     */
    private boolean isSearchHidden() {
        return flow == Flow.EDIT || flow == Flow.VIEW;
    }

    private boolean isCloning( ) {
        return dashboardConfig.getFlow() == Flow.CLONE;
   }
    private boolean hasRoxie() {
        boolean hasRoxie = false;

        if (dashboardConfig.getDashboard().getQueries() != null &&
                MapUtils.isNotEmpty(dashboardConfig.getDashboard().getQueries()))
            hasRoxie = true;

        return hasRoxie;
    }
   private boolean userCanConvert(){
       boolean canUseRamps= ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().canAccessRAMPS();
       boolean canCovertToComp=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().isAllowedConvertToComp();
       return canUseRamps & canCovertToComp;
   }

    private static void resetPermissions(Composition composition, Dashboard dashboard, String author) {
        dashboard.setAuthor(author);
        composition.setAuthor(author);
        Map<PermissionType, Permission> permissions = composition.getPermissions();
        for (Entry<PermissionType, Permission> entry : permissions.entrySet()) {
            entry.getValue().setPermissionLevel(PermissionLevel.PRIVATE);
            entry.getValue().getGroups().clear();
            entry.getValue().getUserIds().clear();
            composition.getPermissions().put(entry.getKey(), entry.getValue());
        }
    }

    @Listen("onClick = #closeProjectDialog")
    public void onClose() {

        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

}
