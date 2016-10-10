package org.hpccsystems.dsp.admin.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.HPCCManager;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.init.ClusterManager;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.usergroupservice.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ClustersController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClustersController.class);

    private final ListModelList<HPCCConnection> connectionsModel = new ListModelList<HPCCConnection>();

    @Wire
    private Panel editCluster;

    @Wire
    private Textbox hpccname;

    @Wire
    private Textbox username;

    @Wire
    private Textbox ipaddress;

    @Wire
    private Textbox port;

    @Wire
    private Textbox thorcluster;

    @Wire
    private Textbox attributesPort;

    @Wire
    private Textbox roxieCluster;

    @Wire
    private Textbox createdBy;

    @Wire
    private Checkbox useDefaultCluster;

    @Wire
    private Div passwordDiv;

    @Wire
    private Checkbox isHttps;

    @Wire
    private Checkbox allowInvalidCerts;

    @Wire
    private Textbox roxieServerHost;

    @Wire
    private Textbox roxieInternalServerHost;

    @Wire
    private Textbox roxieServicePort;

    @Wire
    private Textbox roxieInternalServicePort;

    @Wire
    private Textbox roxieEspPort;

    @Wire
    private Button updateCluster;

    @Wire
    private Listbox groupsListbox;

    @Wire
    private Radiogroup permissionRadioGroup;

    @Wire
    private Cell permissionDiv;

    @Wire
    private Grid clusterGrid;
    @Wire
    private Hlayout clusterHlayout;

    private static final String CUSTOM = "custom";

    @Wire("#hpccname,#username,#ipaddress,#thorcluster,#roxieCluster,#roxieServerHost,#roxieInternalServerHost")
    List<Textbox> boxes;

    @Wire("#port,#attributesPort,#roxieServicePort,#roxieInternalServicePort,#roxieEspPort")
    List<Textbox> intBoxes;

    HPCCConnection updateHPCCConnection;

    boolean isUpdate = true;

    Button passwordButton;

    User user;

    Textbox passwordText = new Textbox();
    Hlayout hlayout;
    private ListModelList<String> groups = new ListModelList<>();
    private Collection<String> customGroups = new ArrayList<>();

    public ListModelList<String> getGroups() {
        return groups;
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        groups.setMultiple(true);
        Map<String, HPCCConnection> connections;
        connections = HipieSingleton.getHipie().getHpccManager().getConnections();
        user = (User) Sessions.getCurrent().getAttribute(Constants.USER);
        connectionsModel.addAll(connections.values());
        passwordButton = new Button("Change Password");
        passwordButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                setPasswordVal();
                passwordDiv.removeChild(passwordButton);
            }

        });

        try {
            Collection<String> grps = RampsUtil.retriveAllGroups();
            grps.stream().forEach(grp -> groups.add(grp));
        } catch (AuthenticationException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            groupsListbox.setEmptyMessage(Labels.getLabel("noGroupsToShow"));
        }

    }

    private void setPasswordVal() {
        passwordDiv.invalidate();
        passwordDiv.getChildren().clear();
        hlayout = new Hlayout();
        Label label = new Label("Password");
        label.setStyle("margin-right:1px");
        hlayout.appendChild(label);
        passwordText.setType("password");
        passwordText.setValue(null);
        passwordText.setStyle("margin-right:6px");
        hlayout.setStyle("float:right");
        hlayout.appendChild(passwordText);
        passwordDiv.appendChild(hlayout);
    }

    @Listen("onClick = #updateCluster")
    public void onUpdateCluster() {
        HPCCManager hipieManager = HipieSingleton.getHipie().getHpccManager();
        
        //set the password to memory
        String oldPwd = null;
        try{
            if(isUpdate && updateHPCCConnection.getPwd()!=null){
                oldPwd = Utility.decrypt(updateHPCCConnection.getPwd());
            }
        }catch (Exception e){
            LOGGER.error(Constants.EXCEPTION, e);
        }
        
        //test to see if the key and the label match if they don't we will remove it and add fresh, since hipieManager doesn't have a way to update the key.
        //null check maynot be needed but was sanity check since we try to delete that connection
        boolean isNameChange = false;
        if(isUpdate && updateHPCCConnection.getLabel() != null && (!updateHPCCConnection.getLabel().equals(hpccname.getValue()) || hipieManager.getConnection(updateHPCCConnection.getLabel()) == null) ){
            isNameChange = true;
            try{
                hipieManager.removeConnection(updateHPCCConnection.getLabel());
                connectionsModel.remove(updateHPCCConnection);
                isNameChange = true;
            }catch (Exception e){
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 5000, true);
                return;
            }
        }
        
        //the label hasn't changed, the key/label match in the config file, and this is an update
        if (isUpdate && !isNameChange) {
            if (validate()) {
                try {   
                    int i = connectionsModel.indexOf(updateHPCCConnection);
                    connectionsModel.remove(updateHPCCConnection);
                    //reset to hipieManager object so that it is a pointer to the same and copy it into the connectionsModel later
                    updateHPCCConnection = hipieManager.getConnection(hpccname.getValue());
                    setHPCCConnection(oldPwd);
                    connectionsModel.add(i, updateHPCCConnection);
                    
                    updateClusterSettingsDB(updateHPCCConnection.getLabel());
                    updateCustompermissionsDB(updateHPCCConnection.getLabel());
                    
                    // To update own server '.cfg' file
                    hipieManager.saveProperties();

                    // Service to sync the cluster in other servers
                    ClusterManager.syncCluster(updateHPCCConnection);
                  

                } catch (DatabaseException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                            Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                } catch(HPCCException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR,
                            getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(Labels.getLabel("notSavedInProperties"), Clients.NOTIFICATION_TYPE_ERROR,
                            getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                }

                // Config updated successfully. Load new config file
                try {
					hipieManager.loadConnections();
					
					HashMap<String,CompositionElement> compositions = HipieSingleton.getHipie().getCompositions(
							((User) Sessions.getCurrent().getAttribute(Constants.USER)).getId());
							
					Iterator<Entry<String, CompositionElement>> compIterator = compositions.entrySet().iterator();
					// Get a composition and refresh all composition instances.
					compIterator.next().getValue().getComposition().refreshCompositionInstances();
				} catch (Exception e) {
					LOGGER.error(Constants.EXCEPTION, e);
				}
                
                Clients.showNotification(Labels.getLabel("clusterUpdated"), Clients.NOTIFICATION_TYPE_INFO,
                        getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                Events.postEvent(Events.ON_CLOSE, editCluster, null);
                
                LOGGER.info("Updated cluster configuration.");
            }

        } else {
            updateHPCCConnection = new HPCCConnection();

            if (validate()) {
                try {
                    setHPCCConnection(oldPwd);
                    connectionsModel.add(updateHPCCConnection);
                    
                    updateClusterSettingsDB(updateHPCCConnection.getLabel());
                    updateCustompermissionsDB(updateHPCCConnection.getLabel());

                    // To update own server '.cfg' file
                    hipieManager.addConnection(hpccname.getValue(), updateHPCCConnection);
                    hipieManager.saveProperties();

                    // Service to sync the cluster in other servers
                    ClusterManager.syncCluster(updateHPCCConnection);

                } catch (DatabaseException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                            Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                }catch(HPCCException e){
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR,
                            getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(Labels.getLabel("notSavedInPropOrAddConnection"), Clients.NOTIFICATION_TYPE_ERROR,
                            getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                    return;
                }
                Clients.showNotification(Labels.getLabel("clusterAdded"), Clients.NOTIFICATION_TYPE_INFO,
                        getSelf().getParent(), Constants.POSITION_MIDDLE_CENTER, 5000, true);
                Events.postEvent(Events.ON_CLOSE, editCluster, null);
            }
        }

    }

    private void setHPCCConnection(String oldPwd) throws WrongValueException, HPCCException {
        updateHPCCConnection.setLabel(hpccname.getValue());
        updateHPCCConnection.setUserName(username.getValue());
        updateHPCCConnection.setServerHost(ipaddress.getValue());
        updateHPCCConnection.setServerPort(new Integer(port.getValue()));
        updateHPCCConnection.setThorCluster(thorcluster.getValue());
        updateHPCCConnection.setRoxieCluster(roxieCluster.getValue());
        updateHPCCConnection.setAttributesPort(new Integer(attributesPort.getValue()));
        updateHPCCConnection.setCreatedBy(createdBy.getValue());
        updateHPCCConnection.setUseDefaultCluster(useDefaultCluster.isChecked());
        updateHPCCConnection.setCreatedBy(user.getName());
        //if the passwordButton's Parent is null then user has updated password
        if(passwordButton.getParent() == null){
            if (passwordText.getValue() != null) {
                updateHPCCConnection.setPassword(passwordText.getValue());
    
            } else {
                try {
                    if (updateHPCCConnection.getPwd() == null) {
                        updateHPCCConnection.setPassword(null);
                    }
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    throw new HPCCException("Unable to get password", e);
                }
            }
        }else {
            //use the one from memory null is allowed
                updateHPCCConnection.setPassword(oldPwd);
        }
        updateHPCCConnection.setIsHttps(((Boolean) isHttps.isChecked()).toString());
        updateHPCCConnection.setAllowInvalidCerts(((Boolean) allowInvalidCerts.isChecked()).toString());//.allowInvalidCerts = allowInvalidCerts.isChecked();
        updateHPCCConnection.setRoxieServerHost(roxieServerHost.getValue());
        updateHPCCConnection.setRoxieInternalServerHost(roxieInternalServerHost.getValue());
        updateHPCCConnection.setRoxieServicePort(new Integer(roxieServicePort.getValue()));
        updateHPCCConnection.setRoxieInternalServicePort(new Integer(roxieInternalServicePort.getValue()));
        updateHPCCConnection.setRoxieEspPort(new Integer(roxieEspPort.getValue()));
    }

    @Listen("onClose = #editCluster")
    public void hideEditPanel(Event event) {
        event.stopPropagation();
        editCluster.setVisible(false);
        enableClustersView();

    }

    public ListModelList<HPCCConnection> getConnectionsModel() {
        return connectionsModel;
    }

    @Listen("onEditConnection = #clusterGrid")
    public void editConnection(ForwardEvent forwardEvent) throws WrongValueException, HPCCException {
        disableClustersView();
        permissionRadioGroup.setSelectedItem(null);
        Event event = forwardEvent.getOrigin();
        Row row = (Row) event.getTarget().getParent();
        HPCCConnection hpccConnection = row.getValue();
        editCluster.setVisible(true);
        hpccname.setValue(hpccConnection.getLabel());
        ipaddress.setValue(hpccConnection.getServerHost());
        port.setValue(String.valueOf(hpccConnection.getServerPort()));
        attributesPort.setValue(String.valueOf(hpccConnection.getAttributesPort()));
        username.setValue(hpccConnection.getUserName());
        try {
            thorcluster.setValue(hpccConnection.getThorCluster());
            roxieCluster.setValue(hpccConnection.getRoxieCluster());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException(e);
        }
        createdBy.setValue(hpccConnection.getCreatedBy());
        useDefaultCluster.setChecked(hpccConnection.isUseDefaultCluster());
        passwordDiv.invalidate();
        passwordDiv.getChildren().clear();
        passwordDiv.appendChild(passwordButton);
        isHttps.setChecked(hpccConnection.getIsHttps());
        allowInvalidCerts.setChecked(hpccConnection.getAllowInvalidCerts());
        roxieServerHost.setValue(hpccConnection.getRoxieServerHost());
        roxieInternalServerHost.setValue(hpccConnection.getRoxieInternalServerHost());
        roxieServicePort.setValue(String.valueOf(hpccConnection.getRoxieServicePort()));
        roxieInternalServicePort.setValue(String.valueOf(hpccConnection.getRoxieInternalServicePort()));
        roxieEspPort.setValue(String.valueOf(hpccConnection.getRoxieEspPort()));

        editCluster.setVisible(true);
        isUpdate = true;
        updateHPCCConnection = hpccConnection;
        updateCluster.setLabel("Update");
        editCluster.setTitle("Edit HPCC connection");
        if (isPublicCluster(hpccConnection.getLabel())) {
            permissionRadioGroup.setSelectedIndex(0);
        } else if (!isPublicCluster(hpccConnection.getLabel())
                && !isCustomPermissionsEmpty(hpccConnection.getLabel())) {
            permissionRadioGroup.setSelectedIndex(1);
        }
        onCheckPermissions();
        try {
            customGroups = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE))
                    .getCustomGroups(hpccConnection.getLabel());
        } catch (DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 5000, true);
            return;
        }
        if (!CollectionUtils.isEmpty(customGroups)) {
            groups.setSelection(customGroups);
        }

    }

    @Listen("onClick = #addCluster")
    public void onAddCluster() {
        groupsListbox.clearSelection();
        groups.clearSelection();
        disableClustersView();
        editCluster.setVisible(true);
        hpccname.setValue(null);
        username.setValue(null);
        ipaddress.setValue(null);
        port.setValue(null);
        attributesPort.setValue(null);
        thorcluster.setValue(null);
        roxieCluster.setValue(null);
        createdBy.setValue(user.getName());
        setPasswordVal();
        useDefaultCluster.setChecked(false);
        isHttps.setChecked(false);
        allowInvalidCerts.setChecked(false);
        roxieServerHost.setValue(null);
        roxieInternalServerHost.setValue(null);
        roxieServicePort.setValue(null);
        roxieInternalServicePort.setValue(null);
        roxieEspPort.setValue(null);
        updateCluster.setLabel("Add");
        editCluster.setTitle("Add new HPCC connection");
        isUpdate = false;
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private boolean validate() {
        for (Textbox box : boxes) {
            if (StringUtils.isEmpty(box.getValue())) {
                Clients.showNotification("Provide a valid value", Clients.NOTIFICATION_TYPE_ERROR, box,
                        Constants.POSITION_AFTER_CENTER, 3000);
                return false;
            }
        }
        for (Textbox box : intBoxes) {
            if (StringUtils.isEmpty(box.getValue()) && !isInteger(box.getValue())) {
                Clients.showNotification("Provide a numeric value", Clients.NOTIFICATION_TYPE_ERROR, box,
                        Constants.POSITION_AFTER_CENTER, 3000);
                return false;
            }
        }
        return true;
    }

    public boolean isPublicCluster(String cluster) {
        return ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).isPublicCluster(cluster);
    }

    public boolean isCustomPermissionsEmpty(String cluster) {
        try {
            customGroups = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getCustomGroups(cluster);
        } catch (DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 5000, true);
        }
        return customGroups.isEmpty();
    }

    @Listen("onCheck = #permissionRadioGroup")
    public void onCheckPermissions() {
        if (permissionRadioGroup.getSelectedItem() != null) {
            if (CUSTOM.equals(permissionRadioGroup.getSelectedItem().getValue())) {
                permissionDiv.setVisible(true);
                getSelf().invalidate();
            } else {
                permissionDiv.setVisible(false);
                if (!groups.getSelection().isEmpty()) {
                    groups.clearSelection();
                }
            }
        } else {
            groups.clearSelection();
        }
    }

    public void updateClusterSettingsDB(String clusterName) throws DatabaseException {
        if (CUSTOM != null && CUSTOM.equals(permissionRadioGroup.getSelectedItem().getValue())
                && !groupsListbox.getSelectedItems().isEmpty()) {
            ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).updatePublicCluster(clusterName, false);
        } else {
            ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).updatePublicCluster(clusterName, true);
        }
    }

    public void updateCustompermissionsDB(String label) throws DatabaseException {

        ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).removeOlderCustomPermissions(label);
        if (!groupsListbox.getSelectedItems().isEmpty()) {
            List<String> grpList = new ArrayList<>();
            groupsListbox.getSelectedItems().stream().forEach(grp -> grpList.add(grp.getLabel()));
            ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).updateCustomClusterPermissions(grpList, label);

        }
    }

    public void disableClustersView() {
        clusterGrid.setVisible(false);
        clusterHlayout.setVisible(false);
    }

    public void enableClustersView() {
        clusterGrid.setVisible(true);
        clusterHlayout.setVisible(true);
    }

}
