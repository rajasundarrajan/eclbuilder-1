package org.hpccsystems.dsp.ramps.controller;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrTokenizer;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.component.renderer.GlobalVariableRenderer;
import org.hpccsystems.dsp.ramps.controller.entity.FileBrowserData;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.FileBrowserRetriver;
import org.hpccsystems.dsp.ramps.entity.BooleanResult;
import org.hpccsystems.dsp.ramps.entity.ClusterConfig;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin.ERROR;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.model.FileListTreeModel;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.dsp.service.PluginService;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;
import org.hpccsystems.ws.client.utils.FileFormat;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Button;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
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
public class FileBrowserController extends SelectorComposer<Component>implements EventListener<Event> {

    private static final String ON_COMPLETE_LOADING = "onCompleteLoading";
    private static final String DO_AFTER_EDIT_PLUGIN = "onEditPlugin";
    private static final String LOADING_FILES = "Loading files";
    private static final String FILE_NAME = "fileName";
    private static final String ON_LOADING = "onLoading";
    private static final String PLUGIN = "Plugin";

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserController.class);

    @Wire
    private Tree fileTree;
    @Wire
    private Auxhead emptyMessage;
    @Wire
    private Label emptyMsglabel;
    @Wire
    private Button filePreview;

    @Wire
    private Popup globalVarPopup;
    @Wire
    private Listbox globalVariablesList;

    @Wire
    private Button newDataSource;

    @Wire
    private Vlayout sprayProgressContainer;

    @WireVariable
    private Desktop desktop;

    @Wire
    private Listbox datasetList;

    @Wire
    private Textbox structureTextBox;

    @Wire
    private Popup structure;

    @Wire
    private Button updateBtn;

    private Label selectedFile;
    long startTime;
    private Project project;
    private String logicalDirName;
    private Tab tab;
    private TabData tabData;
    private List<String> blacklistedFiles;

    private FileListTreeModel fileListTreeModel;
    private FileMeta fileMeta = new FileMeta();
    private ClusterConfig clusterConfig;
    private HPCCConnection hpccConnection;
    private FileMeta fileMetaBrowser = new FileMeta();

    private DatasetPlugin datasetPlugin;
    private Plugin selectedPlugin;
    private ListModelList<Plugin> datasetModel = new ListModelList<Plugin>();

    DatasetPlugin.ERROR error;

    EventListener<Event> pageChangeListener = event -> doAfterPageChange(event);

    private void doAfterPageChange(Event event) {
        FileBrowserData fileBrowserData = (FileBrowserData) event.getData();
        BooleanResult validation = fileBrowserData.getResult();

        // For New composition,show global variables btn.
        if ((Constants.Flow.NEW == tabData.getFlow()) && (FileBrowserController.this.getSelf().getParent() != null)) {
            Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, FileBrowserController.this.getSelf().getParent(), null);
        }

        if (!datasetPlugin.isAllFilesSelected()) {
            if (fileBrowserData.isNotifyUser()) {
                Clients.showNotification(Labels.getLabel("chooseLogicalFile"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                        Constants.POSITION_MIDDLE_CENTER, 3000);
            }
            validation.setFaliure();
            // In edit flow, project.getComposition() doesn't have the updated
            // global variables.
            // So taking from tabData.getComposition()
            if ((Constants.Flow.EDIT == tabData.getFlow()) && (FileBrowserController.this.getSelf().getParent() != null)) {
                Events.postEvent(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, FileBrowserController.this.getSelf().getParent(), null);
            }
            return;
        }
        validation.setSuccess();
    }

    /**
     * Populates the plugin properties in the UI
     */
    private void populateProperties() {
        if (Constants.FILE.equals(selectedFile.getValue())) {
            togglePreview(null);
        }
        if (fileTree.getSelectedItem() == null) {
            selectedFile.setValue("");
        } else if (fileTree.getSelectedItem() != null) {
            togglePreview(selectedPlugin.getLogicalFileName());
        }

    }

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        tabData = (TabData) Executions.getCurrent().getAttribute(Constants.TAB_DATA);
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
    	super.doAfterCompose(comp);

        project = tabData.getProject();
        datasetPlugin = project.getDatasetPlugin();
        datasetModel.addAll(datasetPlugin.getPlugins());
        datasetList.setModel(datasetModel);
        hpccConnection = project.getHpccConnection();
        clusterConfig = project.getClusterConfig();
        tab = tabData.getFilePreviewTab();
        
        try {
        	blacklistedFiles = getBlacklistedFiles();
        } catch (DatabaseException e) {
			LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(Labels.getLabel("databaseError"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
		}

        // Initialize FileMeta Scope
        if (project.getReferenceId() != null) {
            checkAndClearInvalidFiles();
            fileMeta.setScope(project.getBaseScope());
        } else {
            fileMeta.setScope("");
        }

        fileMeta.setFileName("ROOT");
        fileMeta.setIsDirectory(true);

        if (!tabData.isDatasourceValidated()) {
            // Error is set by checking what is wrong with the datasource.
            error = datasetPlugin.updateValidStructures(project.getHpccConnection());
            // This error is shown by showWarnings() after file-browser loads
        } else {
            error = ERROR.NO_ERROR;
        }

        this.getSelf().getParent().addEventListener(EVENTS.ON_PAGE_CHANGE, pageChangeListener);
        this.getSelf().getParent().addEventListener(EVENTS.SHOW_HIDE_GLOBAL_VAR_OPTION, event -> toggleGlobalVariableVisiblity());

        getSelf().addEventListener(EVENTS.ON_SPRAY_COMPLETE, (EventListener<? extends Event>) event -> {
            sprayProgressContainer.setVisible(false);

            Clients.showBusy(getSelf(), "Refreshing files");
            String fileName = (String) event.getData();

            togglePreview(fileName);
            updateContractInstance(fileName);

            constructFileBrowser();

            if (newDataSource != null) {
                newDataSource.setDisabled(false);
            }
        });

        getSelf().addEventListener(EVENTS.ON_POPULATE_FILE_PROPS, event -> updateFileStructure());

        getSelf().addEventListener(EVENTS.ON_POPULATE_GLOBAL_VAR_FILE_PROPS, event -> updateGlobalVariableFileStructure());

        getSelf().addEventListener(ON_COMPLETE_LOADING, event -> doAfterLoading());
        getSelf().addEventListener(DO_AFTER_EDIT_PLUGIN, event -> doAfterEditPlugin());

        globalVariablesList.setModel(tabData.getGlobalVariablesModel());
        LOGGER.debug("Varialbes - {}", tabData.getGlobalVariablesModel());
        globalVariablesList.setItemRenderer(new GlobalVariableRenderer());

        this.getSelf().addEventListener(ON_LOADING, event -> constructFileBrowser());
        Clients.showBusy(getSelf(), LOADING_FILES);
        Events.echoEvent(ON_LOADING, getSelf(), null);

        // Posting this event as datasetList has to be rendered completely
        // before this happens
        Events.echoEvent(ON_COMPLETE_LOADING, getSelf(), null);
        
        fileTree.addEventListener(Events.ON_SELECT, evnt -> {
            if (selectedPlugin.getContractInstance() != null) {
                selectedPlugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, Constants.FILE);
            }
            togglePreview(null);
            addSelectedItem();
        });
    }

    private void doAfterLoading() {
        // Selecting first available Dataset
        Listitem firstItem = (Listitem) datasetList.getChildren().stream().filter(child -> child instanceof Listitem).findFirst().get();

        Plugin firstPlugin = datasetModel.iterator().next();
        datasetModel.addToSelection(firstPlugin);
        selectDataset(firstItem, firstPlugin);

        // Toggling Global variable button visibility
        toggleGlobalVariableVisiblity();
        
        setFilesHeight();
    }

    private void toggleGlobalVariableVisiblity() {
        datasetList.getChildren().stream().filter(child -> child instanceof Listitem && 
                child != null && child.getLastChild() != null && child.getLastChild().getFirstChild() != null
                 && child.getLastChild().getFirstChild().getChildren() != null)
                .forEach(listitem -> listitem.getLastChild().getFirstChild().getChildren().stream()
                        .filter(globalBtn -> globalBtn instanceof Button && "globalVarBtns".equals(((Button) globalBtn).getSclass()))
                        .forEach(useGlobalVar -> useGlobalVar.setVisible(project.isShowGlobalVariable())));
    }

    // Selection logic to select file after tree is rendered.
    @SuppressWarnings("unused")
    private void selectFile(String logicalFilename) throws HPCCException {
        String fileName = logicalFilename.startsWith("~") ? logicalFilename.substring(1) : logicalFilename;
        fileName = StringUtils.removeStart(fileName, fileMeta.getScope());

        List<String> scopes = new StrTokenizer(fileName, "::").getTokenList();
        FileMeta match = fileMeta;

        int[] openPath = new int[scopes.size() - 1];
        int[] selectPath = new int[scopes.size()];
        int index = 0;

        Set<FileMeta> openObjects = new HashSet<FileMeta>();
        for (String name : scopes) {
            LOGGER.debug("Name - {}, Match Object - {}", name, match);

            FileMeta currentMatch = match.getChildlist().stream().filter(file -> file.getFileName().endsWith(name)).findFirst().get();

            int position = match.getChildlist().indexOf(currentMatch);
            selectPath[index] = position;
            if (currentMatch.isDirectory()) {
                currentMatch.setChildlist(FileBrowserRetriver.getFileList(currentMatch.getScope(), hpccConnection, blacklistedFiles));
                openObjects.add(currentMatch);
                openPath[index] = position;
                index++;
            }

            match = currentMatch;
        }

        List<FileMeta> selection = new ArrayList<FileMeta>();
        selection.add(match);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Selected File - {} \nOpen path - {}\nSelect path - {}", match, openPath, selectPath);
        }

        fileListTreeModel.clearOpen();
        fileListTreeModel.clearSelection();

        fileListTreeModel.addOpenPath(openPath);
        fileListTreeModel.addSelectionPath(selectPath);

        if (fileTree.getSelectedItem() != null) {
            fileTree.getSelectedItem().setFocus(true);
        }
    }

    private void togglePreview(String fileName) {
        selectedFile.setValue(fileName);

        if (StringUtils.isEmpty(fileName)) {
            filePreview.setDisabled(true);
        } else {
            filePreview.setDisabled(false);
            tabData.getProject().setDatasourceStatus(DatasourceStatus.VALID);

            if (tab.getAttribute(Constants.PREVIEW_TAB) != null && !fileName.equals(tab.getAttribute(Constants.PREVIEW_TAB))) {
                tab.setVisible(false);
                tab.getLinkedPanel().setVisible(false);
            }
        }
    }

    private void constructFileBrowser() {
        
        if (project.getReferenceId() != null) {
            fileMetaBrowser.setScope(project.getBaseScope());
        } else {
            fileMetaBrowser.setScope("");
        }

        fileMetaBrowser.setFileName("ROOT");
        fileMetaBrowser.setIsDirectory(true);
        desktop.enableServerPush(true);
        startTime = Instant.now().toEpochMilli();
        DSPExecutorHolder.getExecutor().execute(new FileBrowserRetriver(desktop, FileBrowserController.this, fileMetaBrowser, hpccConnection, blacklistedFiles));
    }

    private void checkAndClearInvalidFiles() {
        if(!isFlowNew()){
            datasetPlugin.getPlugins().forEach(datasetPlugin ->{
                if(datasetPlugin.isConfigured()){
                  String fileName = datasetPlugin.getLogicalFileNameUsingProperty().substring(
                       1, datasetPlugin.getLogicalFileNameUsingProperty().length());
                  if(!StringUtils.startsWith(fileName, project.getBaseScope())){
                      datasetPlugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, Constants.FILE);
                   }
                }
            });
        }    
    }
    
    private void fileTreeRenderer(final List<String> fileDirectories, final String fileName) {
        fileTree.setItemRenderer(new TreeitemRenderer<FileMeta>() {
            @Override
            public void render(Treeitem item, FileMeta file, int i) throws Exception {
                renderTreeItems(fileDirectories, fileName, item, file);
            }
        });
    }

    private void renderTreeItems(final List<String> fileDirectories, final String fileName, Treeitem item, FileMeta file) {
    	
    	// If the item to be rendered is on the blacklist, don't render it.
    	if(blacklistedFiles != null && blacklistedFiles.contains(file.getFileName()) && item != null && item.getParent() != null) {
    	    item.getParent().removeChild(item);
    	    return;
    	}
    	
        item.setValue(file);
        String[] names = new StrTokenizer(file.getFileName(), "::").getTokenArray();
        Treecell treecell = new Treecell(names[names.length - 1]);
        Treerow treerow = new Treerow();
        treecell.setIconSclass(file.isDirectory() ? "z-icon-folder" : "z-icon-file");
        treecell.setAttribute(FILE_NAME, file.getFileName());
        treecell.setSpan(2);
        treerow.appendChild(treecell);
        item.appendChild(treerow);
        // Avoid showing selected file, when Composition used global variable
        // for file
        if (selectedPlugin.getLogicalFileName() != null
                && !selectedPlugin.getLogicalFileName().startsWith(Constants.GLOBAL_VAR_PREFIX + Constants.GLOBAL)) {
            if (file.isDirectory() && fileDirectories.contains(names[names.length - 1])) {
                item.setOpen(true);
            } else if (!file.isDirectory() && names[names.length - 1].equals(fileName)) {
                item.setSelected(true);
                item.setFocus(true);
                fileTree.setSelectedItem(item);
            }
        }
    }
    
    private List<String> getBlacklistedFiles() throws DatabaseException {
    	return ((LogicalFileService)SpringUtil.getBean(
    			Constants.LOGICAL_FILE_SERVICE)).getBlacklistedThorFiles();
    }

    private void addSelectedItem() {
        for (Treeitem treeitem : fileTree.getSelectedItems()) {
            if (treeitem.getLastChild() instanceof Treerow) {
                Treerow treerow = (Treerow) treeitem.getLastChild();
                Treecell treecell = (Treecell) treerow.getLastChild();
                String logicalFileName = "~" + treecell.getAttribute(FILE_NAME);
                DFUFileDetailInfo dfuFileDetail;
                try {
                    dfuFileDetail = ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFileName, hpccConnection,
                            clusterConfig.getThorCluster());
                } catch (HPCCException d) {
                    LOGGER.error(Constants.EXCEPTION, d);
                    showNotification();
                    return;
                }
                
                FileFormat fileType = null;
                if (dfuFileDetail != null){
                    try{
                        fileType = dfuFileDetail.getFileType();
                    }catch (Exception e){
                        fileType = null;
                        LOGGER.debug("Selected logical file - {} Does not have a file type", logicalFileName);
                    }
                }
                LOGGER.debug("Selected logical file - {} File type - {}", logicalFileName, fileType);

                if (dfuFileDetail != null && !FileFormat.FLAT.equals(fileType)) {
                    Clients.showNotification(Labels.getLabel("notThor"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                            Constants.POSITION_TOP_CENTER, 3000, true);
                    fileTree.clearSelection();
                    return;
                }

                LOGGER.debug("Plugins ---->{}", datasetPlugin);
                
                togglePreview(logicalFileName);
                updateContractInstance(logicalFileName);
            } else {
                updateTreeItem(treeitem);
            }
            
        }
    }

    private void updateTreeItem(Treeitem treeitem) {
        if (treeitem.isOpen()) {
            treeitem.setOpen(false);
        } else {
            treeitem.setOpen(true);
        }
        logicalDirName = ((FileMeta) treeitem.getValue()).getScope();

        treeitem.setSelected(true);
    }

    // TODO:Need to refactor the method signature in better way
    private void updateContractInstance(String logicalFileName) {
        try {
            updateCIStructure(logicalFileName);
            selectedPlugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, logicalFileName);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showNotification();
        }

    }

    private void updateCIStructure(String logicalFileName) throws HPCCException {
        String fieldseparator = null;
        RecordInstance recordInstance;

        try {
            recordInstance = hpccConnection.getDatasetFields(logicalFileName, fieldseparator);

            LOGGER.debug("Setting structure - {} for file - {} in CI", recordInstance.toEclString(), logicalFileName);

            selectedPlugin.getContractInstance().setProperty(Constants.STRUCTURE, recordInstance.toEclString());
            selectedPlugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, logicalFileName);

            // Enabling Run/Save button in new project flow
            Events.postEvent(EVENTS.ON_SELECT_DATASOURCE, tabData.getProjectDetailComponent(), null);

        } catch (Exception e) {
            // TODO:Instead of throwing all type of exception,
            // should throw only when the structure is invalid/has no structure
            throw new HPCCException(e);
        }
    }

    private boolean canUpdateContractInstance(String logicalFileName) {
        try {
            updateCIStructure(logicalFileName);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            showNotification();
            return false;
        }
        return true;
    }

    @Listen("onClick = #newDataSource")
    public void showModal(Event e) {
        tabData.setFileBrowserComponent(getSelf());
        Events.postEvent(EVENTS.ON_IMPORT_FILE, tabData.getProjectDetailComponent(), logicalDirName);
    }

    @Listen("onClick = #filePreview")
    public void showpreview() {
        Tabpanel panel = tab.getLinkedPanel();
        Include include = (Include) panel.getChildren().get(0);
        String filename = selectedPlugin.getLogicalFileNameUsingProperty();

        tab.setVisible(true);
        panel.setVisible(true);
        Events.postEvent(Events.ON_SELECT, tab, null);
        tab.setSelected(true);

        include.setDynamicProperty(Constants.PROJECT, project);
        include.setDynamicProperty(Constants.FILE, filename);

        if (tab.getAttribute(Constants.PREVIEW_TAB) != null && !filename.equals(tab.getAttribute(Constants.PREVIEW_TAB))) {
            include.setSrc(null);
        }
        LOGGER.debug("filename:{}", filename);
        LOGGER.debug("hpccconID:{}", project.getHpccConnection().getLabel());
        include.setSrc("ramps/project/file_contents_preview.zul");
        tab.setAttribute(Constants.PREVIEW_TAB, filename);
    }

    @Override
    public void onEvent(Event event) throws Exception {

        if (EVENTS.ON_FILE_LOADED.equals(event.getName())) {
            fileMetaBrowser = (FileMeta) event.getData();
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.FILE_BROWSER_RETRIVE, startTime,
                        "Files successfully retrived: " + fileMetaBrowser.getChildlist().size()));
            }
            loadFileBrowser(fileMetaBrowser);

        } else if (EVENTS.ON_FILE_LOAD_FAILED.equals(event.getName())) {
            if (LOGGER.isDebugEnabled()) {
                ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER))
                        .log(new HipieQuery(HipieQuery.FILE_BROWSER_RETRIVE, startTime, "Failed to retrive Files"));
            }
            Exception exception = (Exception) event.getData();
            LOGGER.error(Constants.EXCEPTION, exception);
            Clients.clearBusy(getSelf());
        }

        Events.postEvent(EVENTS.ON_COMPLETE_BROWSER_LOADING, tabData.getProjectDetailComponent(), null);
    }

    public void loadFileBrowser(FileMeta fileMetaBrowser) {
        FileListTreeModel fileListTreeModelBrowser = new FileListTreeModel(fileMetaBrowser, hpccConnection);
        if (fileMetaBrowser.getChildlist().isEmpty()) {
            emptyMessage.setVisible(true);
            StringBuilder builder = new StringBuilder();
            builder.append("No files found in the directory ").append(project.getBaseScope());
            emptyMsglabel.setValue(builder.toString());
        } else {
            emptyMessage.setVisible(false);
        }

        String canonicalFileName = null;
        List<String> dirList = new ArrayList<String>();
        if (selectedPlugin.isConfigured() 
        		&& !StringUtils.isEmpty(selectedPlugin.getLogicalFileName())
                && !selectedPlugin.getLogicalFileName().startsWith(Constants.GLOBAL_VAR_PREFIX + Constants.GLOBAL)) {
            canonicalFileName = selectedPlugin.getLogicalFileName().substring(1).toLowerCase();
            dirList.addAll(Arrays.asList(StringUtils.split(canonicalFileName, "::")));
            canonicalFileName = dirList.get(dirList.size() - 1);
            dirList.remove(canonicalFileName);
        }
        final List<String> fileDirectories = dirList;
        final String fileName = canonicalFileName;

        fileTree.setModel(fileListTreeModelBrowser);
        fileTreeRenderer(fileDirectories, fileName);

        if (selectedPlugin.isConfigured() && !StringUtils.isEmpty(selectedPlugin.getLogicalFileName())) {
            try {
                RecordInstance recordInstance = hpccConnection.getDatasetFields(selectedPlugin.getLogicalFileNameUsingProperty(), null);
                if (recordInstance != null) {
                    if (selectedPlugin.getLogicalFileName().startsWith(Constants.GLOBAL_VAR_PREFIX + Constants.GLOBAL)) {
                        Events.postEvent(EVENTS.ON_POPULATE_GLOBAL_VAR_FILE_PROPS, FileBrowserController.this.getSelf(), null);
                    } else {
                        Events.postEvent(EVENTS.ON_POPULATE_FILE_PROPS, FileBrowserController.this.getSelf(), null);
                    }
                }
            } catch (Exception e) {
                Clients.showNotification("File doesn't exists", Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_MIDDLE_CENTER, 3000, true);
                LOGGER.error(Constants.EXCEPTION, e);
            }
        }

        Clients.clearBusy(getSelf());

        showWarnings();
    }

    /**
     * Show error messages set on Page load. Once.
     */
    private void showWarnings() {
        switch (error) {
        case FILE_MISSING:
            Clients.showNotification(Labels.getLabel("filemissingWarning"), Clients.NOTIFICATION_TYPE_WARNING, getSelf(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            break;

        case STRUCTURE_MISMATCH:
            Clients.showNotification(Labels.getLabel("structureUpdatedWarning"), Clients.NOTIFICATION_TYPE_WARNING, getSelf(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            break;

        default:
            break;
        }

        // Reseting to avoid further notifications
        error = ERROR.NO_ERROR;
    }

    private void updateGlobalVariableFileStructure() {
        try {
            // Updates the file structure with the latest one
            updateCIStructure(selectedPlugin.getLogicalFileNameUsingProperty());
            tabData.getProject().setDatasourceStatus(DatasourceStatus.VALID);
            // Showing the global variable name in textbox, when composition
            // uses global variable
            togglePreview(selectedPlugin.getLogicalFileName());
        } catch (Exception e) {
            showFileErrorMessage();
            tabData.getProject().setDatasourceStatus(DatasourceStatus.INVALID);
            LOGGER.error(Constants.EXCEPTION, e);
            selectedFile.setValue(selectedPlugin.getLogicalFileName());
        }
    }

    private void updateFileStructure() {
        try {
            // If file is invalids, shows error message
            if (fileTree.getSelectedItem() == null) {
                showFileErrorMessage();
                tabData.getProject().setDatasourceStatus(DatasourceStatus.INVALID);
                selectedFile.setValue(selectedPlugin.getLogicalFileName());
                return;
            }
            // If file exists,updates the file structure with the latest one.If
            // unable fetch the structure, sets file as invalid
            updateCIStructure(selectedPlugin.getLogicalFileName());
            tabData.getProject().setDatasourceStatus(DatasourceStatus.VALID);
            populateProperties();
        } catch (Exception e) {
            showFileErrorMessage();
            tabData.getProject().setDatasourceStatus(DatasourceStatus.INVALID);
            LOGGER.error(Constants.EXCEPTION, e);
            selectedFile.setValue(selectedPlugin.getLogicalFileName());
        }
    }

    /**
     * Shows the error message when the file not exists in the cluster or unable
     * to fetch the file structure. It shows the message if the DatasourceStatus
     * is still loading/unknown.
     */
    private void showFileErrorMessage() {
        if (DatasourceStatus.LOADING == tabData.getProject().getDatasourceStatus()) {
            showNotification();
        }
    }

    private void showNotification() {
        Clients.showNotification(Labels.getLabel("unableToFetchFileInfo"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                Constants.POSITION_TOP_CENTER, 5000, true);
    }

    @Listen("onClick = #addUsedataset")
    public void onaddUsedataset() {
        PluginService pluginService = (PluginService) SpringUtil.getBean("pluginService");
        ContractInstance dataset = null;
        try {
            dataset = CompositionUtil.createDatasourceInstance(pluginService.getDatasourceContract());
        } catch (HipieException e) {
            //check error msg
            LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        Plugin plugin = new Plugin(dataset.getContract().getName(), dataset.getContract().getRepositoryName());
        plugin.setContractInstance(dataset);
        datasetModel.add(plugin);
        Events.echoEvent(DO_AFTER_EDIT_PLUGIN, getSelf(), null);
        datasetList.setModel(new ListModelList<Plugin>());
        datasetList.setModel(datasetModel);
        datasetPlugin.addPlugin(plugin);
        tabData.getComposition().addContractInstance(dataset);
        fileTree.clearSelection();
        
        setFilesHeight();
    }

    private void setFilesHeight() {
        switch (datasetModel.size()) {
        case 1:
            datasetList.setHeight("39px");
            break;
        case 2:
            datasetList.setHeight("78px");
            break;
        default:
            datasetList.setHeight("117px");
            break;
        }
    }

    private void doAfterEditPlugin() {
        Listitem lastItem = (Listitem) datasetList.getChildren().stream().filter(child -> child instanceof Listitem)
                .reduce((previous, current) -> current).get();
        Plugin plugin = (Plugin) lastItem.getValue();
        datasetModel.addToSelection(plugin);
        selectDataset(lastItem, plugin);
    }

    @Listen("onClick = #refreshFiles")
    public void onrefreshBrowser() {
        Clients.showBusy(getSelf(), LOADING_FILES);
        constructFileBrowser();
    }

    @Listen("onSelect = #globalVariablesList")
    public void validateAndPopulateFile(SelectEvent<Component, GlobalVariable> event) {
        GlobalVariable selectedVar = event.getSelectedObjects().iterator().next();
        LOGGER.debug("Selected global variable - {}", selectedVar);

        StringBuilder filePrefix = new StringBuilder();

        if (!StringUtils.isEmpty(project.getReferenceId())) {
            filePrefix.append(Constants.TILDE).append(project.getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR);
        } else {
            filePrefix.append(Constants.TILDE);
        }

        if (!selectedVar.getValue().startsWith(filePrefix.toString())) {
            Clients.showNotification(Labels.getLabel("variableHasInvalidFilename"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 3000, true);
        } else if (canUpdateContractInstance(selectedVar.getValue())) {
            selectedPlugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, selectedVar.getNameToPopulate());
            togglePreview(selectedVar.getNameToPopulate());
            ((AbstractTreeModel<Object>) fileTree.getModel()).clearSelection();
        }

        globalVarPopup.close();
        globalVariablesList.clearSelection();
    }

    @Listen("onSelect = #datasetList")
    public void processDatasetSelection(SelectEvent<Listitem, Plugin> event) {
        Listitem selectedItem = event.getReference();
        selectDataset(selectedItem, event.getSelectedObjects().iterator().next());
        fileTree.clearSelection();
    }

    @Listen("onDeleteDataset = #datasetList")
    public void deleteDataset(ForwardEvent event) {
        Plugin deletePlugin = (Plugin) event.getData();
        if (deletePlugin.isConfigured() && project.isHookedToPlugin(deletePlugin.getContractInstance())) {
            Clients.showNotification(Labels.getLabel("cannotDeleteDatasource"), Clients.NOTIFICATION_TYPE_ERROR, event.getTarget(),
                    Constants.POSITION_MIDDLE_CENTER, 5000, true);
            return;
        }
        datasetModel.remove(deletePlugin);
        datasetPlugin.removePlugin(deletePlugin);
        tabData.getComposition().removeContractInstance(deletePlugin.getContractInstance());
        datasetList.setModel(new ListModelList<Plugin>());
        datasetList.setModel(datasetModel);
        Events.echoEvent(DO_AFTER_EDIT_PLUGIN, getSelf(), null);
        fileTree.clearSelection();
        
        setFilesHeight();
    }

    @Listen("onClick = #updateBtn")
    public void updateStructure() {
        String newStructure = null;
        Plugin updatePlugin = (Plugin) updateBtn.getAttribute(PLUGIN);
        try {
            newStructure = hpccConnection.getDatasetFields(updatePlugin.getLogicalFileNameUsingProperty(), null).toEclString();
        } catch (Exception e) {
            LOGGER.debug(Constants.HANDLED_EXCEPTION, e);
            Clients.showNotification("Cannot update structure", Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
        }
        structureTextBox.setText(newStructure);
        updatePlugin.setStructure(newStructure);
    }

    @Listen("onStructureHover = #datasetList")
    public void onStructureHover(ForwardEvent event) {
        Plugin hoverPlugin = (Plugin) event.getData();
        updateBtn.setAttribute(PLUGIN, hoverPlugin);
        structureTextBox.setText(hoverPlugin.getStructure());
        structure.open(event.getOrigin().getTarget(), "after_start");
    }

    private void selectDataset(Listitem selectItem, Plugin selectPlugin) {
        selectedPlugin = selectPlugin;
        if(!selectedPlugin.isConfigured()){
            ContractInstance dataset = null;
            try {
                PluginService pluginService = (PluginService) SpringUtil.getBean("pluginService");
                dataset = CompositionUtil.createDatasourceInstance(pluginService.getDatasourceContract());
            } catch (HipieException e) {
                //check error msg
                LOGGER.error(Constants.ERROR, e);
                Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
            selectedPlugin.setContractInstance(dataset);
        }
        selectedFile = (Label) selectItem
                .getLastChild()
                .getFirstChild()
                .getChildren()
                .stream()
                .filter(child -> child instanceof Label)
                .findFirst()
                .get();
    }

    public ListModelList<Plugin> getDatasetModel() {
        return datasetModel;
    }

    public DatasetPlugin getDatasetPlugin() {
        return datasetPlugin;
    }

    public Project getProject() {
        return project;
    }

    public boolean isFlowView() {
        boolean isFlowView = false;
        if(tabData !=null) {
            isFlowView = Constants.Flow.VIEW == tabData.getFlow();
        }

        return isFlowView;
    }
    
    public boolean isFlowNew() {
        return Constants.Flow.NEW == tabData.getFlow();
    }
}
