package org.hpccsystems.dsp.ramps.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin.ERROR;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.HPCCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ChangeClusterController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeClusterController.class);
    @Wire
    private Combobox connectionListRamps;

    @Wire
    private Combobox thorClusterRamps;
    @Wire
    private Combobox roxieClusterRamps;

    @Wire("#connectionListRamps, #thorClusterRamps, #roxieClusterRamps")
    List<Combobox> clusterConnection;

    private Map<String, HPCCConnection> connections;

    private TabData rampsData;

    private boolean isClusterChanged;

    final ListModelList<String> connectionsModel = new ListModelList<String>();
    final ListModelList<String> thorClusterModel = new ListModelList<String>();
    final ListModelList<String> roxieClusterModel = new ListModelList<String>();
    private HPCCConnection selectedConnection;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        rampsData = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        User user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        connections = HipieSingleton.getHipie().getHpccManager().getConnections();
        connectionListRamps.setModel(connectionsModel);
        thorClusterRamps.setModel(thorClusterModel);
        roxieClusterRamps.setModel(roxieClusterModel);

        if (rampsData.getProject().getClusterConfig() != null) {
            populateHpccData(rampsData.getProject().getClusterConfig());

        }
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
        connectionListRamps.setItemRenderer(new ComboitemRenderer<String>() {

            @Override
            public void render(Comboitem comboitem, String hpccId, int index) throws Exception {
                comboitem.setLabel(connections.get(hpccId).getLabel());
            }
        });
    }

    private void showHPCCConnectionInfo(String hpccId) {

        selectedConnection = connections.get(hpccId);
        thorClusterModel.clear();
        roxieClusterModel.clear();
        

        //set roxie selections from list of roxie and hthor
        Set<String> roxieSetfirstItem = new HashSet<String>();
        
        boolean isValidConnection = true;

        if (selectedConnection.getThorclusters() != null && !selectedConnection.getThorclusters().isEmpty()) {
            thorClusterModel.addAll(selectedConnection.getThorclusters());
            Set<String> thorSetfirstItem = new HashSet<String>();
            thorSetfirstItem.add(selectedConnection.getThorclusters().get(0));
            thorClusterModel.setSelection(thorSetfirstItem);
        }else{
            isValidConnection = false;
            thorClusterModel.clearSelection();
        }

        
        if (selectedConnection.getRoxieclusters() != null && !selectedConnection.getRoxieclusters().isEmpty()) {
            roxieClusterModel.addAll(selectedConnection.getRoxieclusters());
            roxieSetfirstItem = new HashSet<String>();           
            roxieSetfirstItem.add(selectedConnection.getRoxieclusters().get(0));
            roxieClusterModel.setSelection(roxieSetfirstItem);
        } else if (selectedConnection.getHthorclusters() != null && !selectedConnection.getHthorclusters().isEmpty()) {           
            roxieClusterModel.addAll(selectedConnection.getHthorclusters());
            roxieSetfirstItem = new HashSet<String>();
            roxieSetfirstItem.add(selectedConnection.getHthorclusters().get(0));
            roxieClusterModel.setSelection(roxieSetfirstItem);
            
        }else{
            isValidConnection=false;
            roxieClusterModel.clearSelection();
        }
        
        if (!isValidConnection){
            Clients.showNotification(Labels.getLabel("invalidCluster"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_MIDDLE_CENTER,
                    3000, true);
        }
        
        	

    }

    @Listen("onSelect = #connectionListRamps")
    public void onSelectHPCCConnection(SelectEvent<Component, String> event) {
        showHPCCConnectionInfo(event.getSelectedObjects().iterator().next());
        isClusterChanged = true;
    }

    private void populateHpccData(ClusterConfig clusterConfig) {
        if (clusterConfig.getId() != null) {
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

    @Listen("onClick = #cancel")
    public void onCancel() {
        Events.postEvent("onClose", this.getSelf(), null);
    }

    @Listen("onClick = #save")
    public void onClickSaveCluster() {

        boolean isClusterSelected = RampsUtil.validateClusterSelection(clusterConnection);
        LOGGER.debug("Cluster selected - {}, Cluster changed - {}", isClusterSelected, isClusterChanged);
        if (!isClusterSelected && !isClusterChanged) {
            return;
        }

        DatasetPlugin datasetPlugin = rampsData.getProject().getDatasetPlugin();

        if (datasetPlugin != null && datasetPlugin.isAnyFileSelected()) {
            ERROR error = datasetPlugin.validate(getSelectedCluster().getConnection());

            if (error == ERROR.NO_ERROR) {
                changeClusterProperties();
                Events.postEvent(EVENTS.ON_CHANGE_HPCC_CONNECTION, rampsData.getProjectDetailComponent(), null);
                notifyClusterChange();
            } else {
                String message;
                switch (error) {
                case FILE_MISSING:
                    message = Labels.getLabel("clusterChangeFileNotExists");
                    break;

                case STRUCTURE_MISMATCH:
                    message = Labels.getLabel("clusterChangeStructurMismatch");
                    break;

                default:
                    message = "";
                    break;
                }

                Messagebox.show(message, Labels.getLabel("hpccClusterUpdate"),
                        new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,
                        evet -> updateCluster(error, evet));
            }
        } else {
            changeClusterProperties();
            Events.postEvent(EVENTS.ON_CHANGE_HPCC_CONNECTION, rampsData.getProjectDetailComponent(), null);
            notifyClusterChange();
        }
        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

    private void updateCluster(ERROR error, ClickEvent evet) {
        if (Messagebox.Button.YES.equals(evet.getButton())) {
            if (error == ERROR.STRUCTURE_MISMATCH) {
                // Mark the datasource as invalid so when the page reloads the error message will appear.
                rampsData.setDatasourceValidated(false);
            }
            changeClusterProperties();
            Events.postEvent(EVENTS.ON_CHANGE_HPCC_CONNECTION, rampsData.getProjectDetailComponent(), null);
            notifyClusterChange();
        } else {
            // Clear cluster selection
            clusterConnection.forEach(combobox -> ((ListModelList<Object>) combobox.getModel()).clearSelection());
            notifyClusterRetained();
        }
    }

    private void notifyClusterRetained() {
        Clients.showNotification(Labels.getLabel("oldClusterretained"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                Constants.POSITION_TOP_CENTER, 3000, true);
    }

    private void notifyClusterChange() {
        Clients.showNotification(Labels.getLabel("clusterChanged"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                Constants.POSITION_TOP_CENTER, 3000, true);
    }

    private void changeClusterProperties() {
        ClusterConfig clusterConfig = getSelectedCluster();
        rampsData.getProject().setClusterConfig(clusterConfig);
        // Update DB with edited Hpcc cluster data
        try {
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).saveClusterConfig(rampsData.getComposition(),
                    rampsData.getProject().getClusterConfig());
        } catch (DatabaseException e) {
            LOGGER.error("Cluster update in DB failed", e);
            Clients.showNotification(Labels.getLabel("errorOccuredWhileChangingCluster"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    private ClusterConfig getSelectedCluster() {
        ClusterConfig clusterConfig = new ClusterConfig();
        clusterConfig.setId(connectionsModel.getSelection().iterator().next());
        clusterConfig.setThorCluster(thorClusterModel.getSelection().iterator().next());
        clusterConfig.setRoxieCluster(roxieClusterModel.getSelection().iterator().next());
        return clusterConfig;
    }
}
