package org.hpccsystems.dsp.eclBuilder.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.axis.utils.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionLevel;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
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
import org.zkoss.zk.ui.Execution;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class NewECLBuilderController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewECLBuilderController.class);

    private DashboardConfig dashboardConfig;
    
    @Wire
    private Textbox eclBuilderName;

    final ListModelList<String> connectionsModel = new ListModelList<String>();
    final ListModelList<String> thorClusterModel = new ListModelList<String>();
    final ListModelList<String> roxieClusterModel = new ListModelList<String>();

    @Wire
    private Radiogroup datasource;

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
    
    String dsType;

    @Wire
    private Include searchInclude;

    @Wire
    private Button searchPopbtnDashboard;

    private Flow flow;
    
    private String userAction = "Create";
    
    private String clonedbuilderName;
    @Wire
    private Window createNewECLBuilderWindow;
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        dsType = (String) Executions.getCurrent().getAttribute("dashboardType");
        
        User user = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(1);

        Map<String, HPCCConnection> connections = HipieSingleton.getHipie().getHpccManager().getConnections();

        connectionList.setModel(connectionsModel);
        thorCluster.setModel(thorClusterModel);
        roxieCluster.setModel(roxieClusterModel);
        
        userAction = (String) Executions.getCurrent().getAttribute("userAction");
        clonedbuilderName = (String) Executions.getCurrent().getAttribute("builderName");

        Set<String> clusters = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getPublicAndCustomClusters(user, connections);
        if (!(clusters.isEmpty())) {
            connectionsModel.addAll(clusters);
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

        if (flow != Flow.NEW && dashboardConfig.getDashboard().getHpccConnection() != null) {
            datasource.setSelectedIndex(0);
            hpccContainer.setVisible(true);
            populateHpccData(dashboardConfig.getDashboard().getClusterConfig());
            searchPopbtnDashboard.setVisible(false);
        }else if(dashboardConfig.getDashboard().isStaticData()){
            datasource.setSelectedIndex(1);
        }

    }

    @Listen("onCheck = #datasource")
    public void setDatasource() {
        String selectedSource = datasource.getSelectedItem().getValue();
        if ("hpcc".equals(selectedSource)) {
            hpccContainer.setVisible(true);
            dashboardConfig.getDashboard().setStaticData(false);
            getSelf().invalidate();
        } else if ("staticdata".equals(selectedSource)) {
            hpccContainer.setVisible(false);
            dashboardConfig.getDashboard().setStaticData(true);
        }
    }

    @Listen("onClick = #continueBtn")
    public void createDashboardTab() {
    	Tabbox homeTabbox = (Tabbox) createNewECLBuilderWindow.getParent().getChildren().stream().filter(comp -> comp.getId().equals("homeTabbox")).findFirst().get();
//    	hpccContainer.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent()
    	Executions.getCurrent().setAttribute("hpccConnID",connectionList.getValue());//, Executions.getCurrent().getParameter("dashboardType"));
    	Executions.getCurrent().setAttribute("BuilderName",eclBuilderName.getText());
    	if(null == userAction){
    		userAction = "create";
    	}
        Executions.getCurrent().setAttribute("userAction",  userAction);
        Executions.getCurrent().setAttribute("clonedbuilderName", clonedbuilderName);
        createNewECLBuilderWindow.detach();
        if(userAction.equals("clone") || userAction.equals("edit")){
        	try {
            	String builderName = eclBuilderName.getText();
            	String userID = ((User)((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()).getId();
            	List<Builder> eclBuilders;
        		
        		eclBuilders = ((DSPDao)SpringUtil.getBean("dspDao")).getECLBuilder(userID, clonedbuilderName, connectionList.getValue());
        		
            	if(eclBuilders.size() == 0){
            		return;
            	}
            	Builder selectedBuilder = eclBuilders.get(0);
                Tab tab = new Tab();
                tab.setClosable(true);
                tab.setLabel(eclBuilderName.getText());
                Tabpanel tabpanel = new Tabpanel();
                Include include = new Include();
//                include.setId("eclBuilderInclude");
            	Executions.getCurrent().setAttribute("hpccConnId",Executions.getCurrent().getAttribute("hpccConnID"));//, Executions.getCurrent().getParameter("dashboardType"));
            	Executions.getCurrent().setAttribute("BuilderName",Executions.getCurrent().getAttribute("BuilderName"));
                include.setDynamicProperty("dashboardType",  Executions.getCurrent().getAttribute("dashboardType"));
                include.setDynamicProperty("selectedBuilder",  selectedBuilder);
                include.setDynamicProperty("userAction",  "clone");
                include.setSrc("eclBuilder/BuildECL.zul");
                
                tabpanel.appendChild(include);
                
                homeTabbox.getTabs().appendChild(tab);
                homeTabbox.getTabpanels().appendChild(tabpanel);
                tab.setSelected(true);
              
        		} catch (DatabaseException e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        	return;
        }
        
        try {

                boolean fileExists;

                fileExists = validateECLBuilderName(eclBuilderName.getValue());

                if (fileExists) {
                    Clients.showNotification(
                            Labels.getLabel("eclBuilderAlreadyExists1")
                                    .concat(eclBuilderName.getText().concat(Labels.getLabel("eclBuilderAlreadyExists2"))),
                            Clients.NOTIFICATION_TYPE_ERROR, eclBuilderName, Constants.POSITION_END_CENTER, 3000);
                    return;
                }
               

            Events.postEvent(Dashboard.EVENTS.ON_OPEN_DASHBOARD, dashboardConfig.getHomeTabbox(), dashboardConfig);
            Events.postEvent(Events.ON_CLOSE, getSelf(), null);

            LOGGER.debug("Creating Dashboard with data type: Static - {}",
                    dashboardConfig.getDashboard().isStaticData());
        } catch (WrongValueException e) {
            Clients.showNotification(Labels.getLabel("validDashboard"), Clients.NOTIFICATION_TYPE_ERROR,
                    eclBuilderName, Constants.POSITION_END_AFTER, 3000);
            LOGGER.error(Constants.EXCEPTION, e);
            return;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToCreateComposition"), Clients.NOTIFICATION_TYPE_ERROR, eclBuilderName,
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
            dashboardConfig.getComposition().setLabel(eclBuilderName.getValue());
            dashboardConfig.getDashboard().setWidgets(new ArrayList<Widget>());
            dashboardConfig.getDashboard().setName(compositionName);
            dashboardConfig.getDashboard().setLabel(eclBuilderName.getValue());

        } else {
            Composition clonedComposition = CompositionUtil.cloneComposition(dashboardConfig.getComposition(), compositionName,
                    eclBuilderName.getValue(),
                    // Passing Datasource status(Loading|valid|invalid) null as
                    // it is not applicable in Dashboard perspective
                    ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), true, dashboardConfig.getDashboard(), null);
            dashboardConfig.getDashboard().setName(compositionName);
            dashboardConfig.getDashboard().setLabel(eclBuilderName.getValue());
            dashboardConfig.setComposition(clonedComposition);

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
        	 System.setProperty("http.proxyHost", "rmtproxy.choicepoint.net");
             System.setProperty("http.proxyPort", "8082");
//            connection.testConnection();
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
        if (connection.getRoxieclusters() != null) {
            roxieClusterModel.addAll(connection.getRoxieclusters());
            Set<String> roxieSetfirstItem = new HashSet<String>();
            roxieSetfirstItem.add(connection.getRoxieclusters().get(0));
            roxieClusterModel.setSelection(roxieSetfirstItem);
        } else {
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
    private boolean validateECLBuilderName(String builderName){
    	List<Builder> buildersList;
		try {
			buildersList = ((DSPDao) SpringUtil.getBean("dspDao")).getECLBuilders(((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId());
		
    	return buildersList.stream().filter(e -> e.getName().equals(builderName)).count() > 0 ?  true : false;
		} catch (DatabaseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
    }
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

    private boolean isSearchHidden() {
        return flow == Flow.EDIT || flow == Flow.VIEW;
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
