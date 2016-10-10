package org.hpccsystems.dsp.dashboard.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.controller.utils.FileBrowserRetriver;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.ramps.model.FileListTreeModel;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Button;
import org.zkoss.zul.Include;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vlayout;

/**
 * Responsible for rendering the file tree of logical files retreived from Thor.
 * TODO: Combine this with the ramps FileBrowserController 
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class FileBrowserController extends SelectorComposer<Tabbox> implements EventListener<Event> {

    private static final String LOADING_FILES = "Loading files";
    private static final String FILE_NAME = "fileName";
    private static final String ON_LOADING = "onLoading";

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserController.class);

    @Wire
    private Tree fileTree;
    @Wire
    private Textbox selectedFile;
    @Wire
    private Auxhead emptyMessage;

    @Wire
    private Button newDataSource;

    @Wire
    private Vlayout sprayProgressContainer;

    @Wire
    private Include importInclude;

    @Wire
    private Tab browserTree;

    @Wire
    private Tab importFileAlternate;

    Instant start;
    
    @WireVariable
    private Desktop desktop;

    private String logicalDirName;

    private FileMeta fileMeta = new FileMeta();
    private ClusterConfig clusterConfig;
    private HPCCConnection hpccConnection;
    private DashboardConfig dashboardConfig;
    private WidgetConfig widgetConfig;
    private FileMeta fileMetaBrowser = new FileMeta();
    private List<String> blacklistedFiles;

    /**
     * Populates the plugin properties in the UI
     */
    private void populateProperties() {
        String logicalFile = widgetConfig.getLogicalFile();
        if (logicalFile != null && !logicalFile.isEmpty()) {
            selectedFile.setText(logicalFile);
        }

        if ("file".equals(selectedFile.getText())) {
            selectedFile.setText(null);
        }

    }

    @Override
    public void doAfterCompose(Tabbox comp) throws Exception {
        super.doAfterCompose(comp);

        dashboardConfig = (DashboardConfig) Executions.getCurrent().getAttribute(Constants.DASHBOARD_CONFIG);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG);
        hpccConnection = dashboardConfig.getDashboard().getHpccConnection();
        LOGGER.debug("widgetConfig - {}, hpccConnection - {}", widgetConfig, hpccConnection);
        clusterConfig = dashboardConfig.getDashboard().getClusterConfig();
        
        try {
        	blacklistedFiles = getBlacklistedFiles();
        } catch (DatabaseException e) {
			LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(Labels.getLabel("databaseError"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
		}
        
        // Initialize FileMeta Scope
        if (dashboardConfig.getDashboard().getReferenceId() != null) {
            fileMeta.setScope(dashboardConfig.getDashboard().getBaseScope());
        } else {
            fileMeta.setScope("");
        }

        fileMeta.setFileName("ROOT");
        fileMeta.setIsDirectory(true);

        populateProperties();

        this.getSelf().addEventListener(ON_LOADING,(SerializableEventListener<? extends Event>) event -> constructFileBrowser());
        Clients.showBusy(getSelf(), LOADING_FILES);
        Events.echoEvent(ON_LOADING, getSelf(), null);

        getSelf().addEventListener(EVENTS.ON_SPRAY_COMPLETE, (SerializableEventListener<? extends Event>)event -> {
            sprayProgressContainer.setVisible(false);

            Clients.showBusy(getSelf(), "Refreshing files");
            String fileName = (String) event.getData();

            updateContractInstance(fileName);

            constructFileBrowser();

            newDataSource.setDisabled(false);
        });

        this.getSelf().addEventListener(EVENTS.ON_RETURN_TO_EDIT, (SerializableEventListener<? extends Event>)event -> showDashboardEdit());

    }

    private void showDashboardEdit() {
        browserTree.setSelected(true);
    }

    private void constructFileBrowser() {
        if (dashboardConfig.getDashboard().getReferenceId() != null) {
            fileMetaBrowser.setScope(dashboardConfig.getDashboard().getBaseScope());
        } else {
            fileMetaBrowser.setScope("");
        }

        fileMetaBrowser.setFileName("ROOT");
        fileMetaBrowser.setIsDirectory(true);
        desktop.enableServerPush(true);
        start = Instant.now();
        DSPExecutorHolder.getExecutor().execute(new FileBrowserRetriver(desktop, FileBrowserController.this, fileMetaBrowser, hpccConnection, blacklistedFiles));
    }

    /**
     * This function is what renders the item and adds it to the file tree. Also handles rendering if a file is selected or not
     * @param fileDirectories
     * @param fileName
     */
    private void fileTreeRenderer(final List<String> fileDirectories, final String fileName) {
        fileTree.setItemRenderer(new TreeitemRenderer<FileMeta>() {
            @Override
            public void render(Treeitem item, FileMeta file, int i) throws Exception {
                item.setValue(file);
                String[] names = new StrTokenizer(file.getFileName(), "::").getTokenArray();
                Treecell treecell = new Treecell(names[names.length - 1]);
                Treerow treerow = new Treerow();
                treecell.setIconSclass(file.isDirectory() ? "z-icon-folder" : "z-icon-file");
                treecell.setAttribute(FILE_NAME, file.getFileName());
                treecell.setSpan(2);
                treerow.appendChild(treecell);
                item.appendChild(treerow);
                if (file.isDirectory() && fileDirectories.contains(names[names.length - 1])) {
                    item.setOpen(true);
                } else if (!file.isDirectory() && names[names.length - 1].equals(fileName)) {
                    item.setSelected(true);
                    item.setFocus(true);
                }
            }
        });
    }
    
	/**
	 * Gets a list of blacklisted thor files from MySQL
	 * @return 
	 * @throws DatabaseException
	 */
    private List<String> getBlacklistedFiles() throws DatabaseException {
    	return ((LogicalFileService)SpringUtil.getBean(
    			Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles();
    }

    private void addSelectedItem(Tree targetTree){
        for (Treeitem treeitem : targetTree.getSelectedItems()) {
            if (treeitem.getLastChild() instanceof Treerow) {
                Treerow treerow = (Treerow) treeitem.getLastChild();
                Treecell treecell = (Treecell) treerow.getLastChild();
                String logicalFileName = "~" + treecell.getAttribute(FILE_NAME);
                DFUFileDetailInfo dfuFileDetail;
                try {
                    dfuFileDetail =  ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFileName, hpccConnection, clusterConfig.getThorCluster());
                } catch (HPCCException d) {
                    LOGGER.error(Constants.EXCEPTION, d);
                    Clients.showNotification(Labels.getLabel("unableToFetchFileInfo"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                    return;
                }
                if (dfuFileDetail != null && (!("flat").equalsIgnoreCase(dfuFileDetail.getContentType()))) {
                    Clients.showNotification(Labels.getLabel("notThor"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                            Constants.POSITION_TOP_CENTER, 3000, true);
                    targetTree.clearSelection();
                    return;
                }

                updateContractInstance(logicalFileName);
            } else {
                if (treeitem.isOpen()) {
                    treeitem.setOpen(false);
                } else {
                    treeitem.setOpen(true);
                }
                logicalDirName = ((FileMeta) treeitem.getValue()).getScope();

                treeitem.setSelected(true);
            }
        }
    }

    @Listen("onClick = #newDataSource")
    public void showModal(Event e) {
        dashboardConfig.setFileBrowser(getSelf());
        importInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        importInclude.setDynamicProperty(Dashboard.WIDGET_CONFIG, widgetConfig);
        importInclude.setDynamicProperty(Constants.FILE, logicalDirName);
        importInclude.setSrc("ramps/project/import_file.zul");
        importFileAlternate.setSelected(true);
    }

    private void updateContractInstance(String logicalFileName) {
        selectedFile.setText(logicalFileName);
        try {
            if(widgetConfig.getQueryName() != null){
                widgetConfig.setQueryName(null);
            }
            widgetConfig.updateContractInstance(dashboardConfig, logicalFileName, null);
        } catch (HipieException | HPCCException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    @Override
    public void onEvent(Event event) throws Exception {
        long startTime = Instant.now().toEpochMilli();

        if (EVENTS.ON_FILE_LOADED.equals(event.getName())) {
            fileMetaBrowser = (FileMeta) event.getData();
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.FILE_BROWSER_RETRIVE, startTime, "Files successfully retrived: " + fileMetaBrowser.getChildlist().size()));
            }
            loadFileBrowser(fileMetaBrowser);

        } else if (EVENTS.ON_FILE_LOAD_FAILED.equals(event.getName())) {
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.FILE_BROWSER_RETRIVE, startTime, "Failed to retrive Files"));
            }
            Exception exception = (Exception) event.getData();
            LOGGER.error(Constants.EXCEPTION, exception);
            Clients.clearBusy(getSelf());
        }

    }

    public void loadFileBrowser(FileMeta fileMetaBrowser) {
        FileListTreeModel fileListTreeModelBrowser = new FileListTreeModel(fileMetaBrowser, hpccConnection);
        emptyMessage.setVisible(false);

        String canonicalFileName = null;
        List<String> dirList = new ArrayList<String>();
        if (!StringUtils.isEmpty(selectedFile.getText())) {
            canonicalFileName = selectedFile.getText().substring(1).toLowerCase();
            dirList.addAll(Arrays.asList(StringUtils.split(canonicalFileName, "::")));
            canonicalFileName = dirList.get(dirList.size() - 1);
            dirList.remove(canonicalFileName);
        }
        final List<String> fileDirectories = dirList;
        final String fileName = canonicalFileName;

        fileTree.setModel(null);
        fileTree.setModel(fileListTreeModelBrowser);
        fileTreeRenderer(fileDirectories, fileName);

        fileTree.addEventListener(Events.ON_SELECT, (SerializableEventListener<? extends Event>)evnt -> {
            LOGGER.debug("Open path - {}, Open paths - {}, Selected path - {}", fileListTreeModelBrowser.getOpenPath(),
                    fileListTreeModelBrowser.getOpenPaths(), fileListTreeModelBrowser.getSelectionPath());

            Tree targetTree = (Tree) evnt.getTarget();
            selectedFile.setText(null);
            addSelectedItem(targetTree);
        });

        Clients.clearBusy(getSelf());
    }

    @Listen("onClick = #refreshFileDashboard")
    public void onrefreshBrowser() {
        Clients.showBusy(getSelf(), LOADING_FILES);
        constructFileBrowser();
    }

}
