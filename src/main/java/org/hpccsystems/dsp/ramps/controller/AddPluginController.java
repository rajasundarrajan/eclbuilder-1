package org.hpccsystems.dsp.ramps.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.hpcc.HIPIE.Compliance;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEOutputString;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.resource.FileResourceElement;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.PluginException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
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
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class AddPluginController extends SelectorComposer<Window> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPluginController.class);

    @Wire
    private Treechildren pluginTreeChildren;

    @Wire
    private Html form;
    @Wire
    private Label description;
    @Wire
    private Label source;

    @Wire
    private Label readMe;

    @Wire
    private Combobox repoList;

    @Wire
    private Tabpanel inputParameter;

    @Wire
    private Button repoPopbtn;
    @Wire
    private Auxhead pluginHelpHeader;

    private HIPIEService hipieService = HipieSingleton.getHipie();

    final ListModelList<String> repositoriesModel = new ListModelList<String>();

    private String repoName = null;
    private boolean isAdminPerspectiveView;
    private TabData data;
    String repoNameArgument;
    
    List<String> list = null;

    @Override
    public void doAfterCompose(Window window) throws Exception {
        super.doAfterCompose(window);
        inputParameter.setSclass("disableTabPanel");
        repoList.setModel(repositoriesModel);
        
        data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        repoNameArgument = (String) Executions.getCurrent().getArg().get(Constants.REPO);
        isAdminPerspectiveView = repoNameArgument != null;
        
        LOGGER.info("Repo argument from admin perspective :: {} and isAdminPerspectiveView is {}",repoNameArgument,isAdminPerspectiveView);
        
        if (isAdminPerspectiveView) {
            createPluginTree(repoNameArgument);
            window.setWidth("900px");
        } else {
            repositoriesModel.addAll(((PluginService)SpringUtil.getBean(Constants.PLUGIN_SERVICE)).getAllPluginRepos( ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser()));
            repoName=repositoriesModel.iterator().next();
            repositoriesModel.addToSelection(repoName);
            createPluginTree(repoName);
            
            repoPopbtn.setVisible(true);
            pluginHelpHeader.setVisible(true);
        }
    }

    @Listen("onSelect = #pluginTree")
    public void onSelectDetails(SelectEvent<Component, Object> event) {
        Treeitem selectedItem = (Treeitem) event.getSelectedItems().iterator().next();
        Plugin selectedPlugin = (Plugin) selectedItem.getFirstChild().getFirstChild().getAttribute(Constants.PLUGIN);

        HIPIEOutputString hipieOutputString;
        try {
            if(selectedPlugin != null) {
                if (selectedPlugin.getContractInstance() == null) {
                    HIPIEUtil.associateContractInstance(selectedPlugin, null);
                }
                setPluginSource(selectedPlugin.getContractInstance().getContract().getFileText());
                
                description.setValue(selectedPlugin.getContractInstance().getContract().getDescription());

                hipieOutputString = new HIPIEOutputString();
                selectedPlugin.getContractInstance().generateHTML(hipieOutputString, false, false);
                form.setContent(hipieOutputString.toString());
                
                final FileResourceElement readme = selectedPlugin.getContractInstance().getContract().getReadmeFileResourceElement();
                if (readme != null) {
                    String readmeContent = readme.getContent(false);
                    if(readmeContent != null && !readmeContent.equals("")){
                        setPluginReadme(readmeContent);
                    }else{
                        setPluginReadme(Labels.getLabel("readMeFileMissing"));
                    }
                }else{
                    setPluginReadme(Labels.getLabel("readMeFileMissing"));
                }
                

            } else {
                form.setContent("");
                setPluginSource("");
                description.setValue("");
                setPluginReadme("");
            }
        } catch (FileNotFoundException e) {
            setPluginReadme(Labels.getLabel("readMeFileMissing"));
            LOGGER.error(Constants.HANDLED_EXCEPTION, e);
        } catch (Exception e) {
            Clients.showNotification("Error occured while retriving plugin details", 
                    Clients.NOTIFICATION_TYPE_ERROR, selectedItem, Constants.POSITION_TOP_CENTER, 3000, true);
            LOGGER.error(Constants.EXCEPTION, e);
        }

    }


    private void setPluginSource(String sourceString) {
        if ( ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getPermission().getRampsPermission().canViewPluginSource() ||  ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().isGlobalAdmin()) {
            source.setValue(sourceString);
        }
    }

    private void setPluginReadme(String readme) {
        readMe.setValue(readme);
    }

    @Listen("onClick= #refresh")
    public void onRefreshPlugins() {
        try {
            
            LOGGER.info("repoName :: {} and repoNameArgument  {}",repoName,repoNameArgument);
            
            initiateGitFetch();
            hipieService.getContractBrowser().reset();
            hipieService.getCompositionBrowser().reset();
            
            LOGGER.info("Refreshed in memory hipie objects");
            
              if (!isAdminPerspectiveView) {
                //Refreshing contracts present in the composition, only in 'Add plugin' mode 
                List<ContractInstance> instancesWithoutCtrct = new ArrayList<ContractInstance>();
                for (ContractInstance ci : data.getComposition().getContractInstances().values()) {
                    Contract contract = hipieService.getContract( ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), ci.getContract().getCanonicalName());
                    if(contract != null){
                        ci.setParent(contract);
                    } else {
                        instancesWithoutCtrct.add(ci);
                    }
                }
                //Event listener for composition controller component to select the active plugin after refresh
                Events.postEvent(EVENTS.ON_REFRESH_PLUGIN_BROWSER, data.getCompositionControllerComponent(), instancesWithoutCtrct);
            }

            createPluginTree(isAdminPerspectiveView ? repoNameArgument : repoName);
            Clients.showNotification(Labels.getLabel("pluginrepoRefreshComplete"), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("pluginrepoRefreshfailed"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

    private void initiateGitFetch() throws Exception {
        if (!isAdminPerspectiveView && repoName != null && hipieService.getRepositoryManager().getRepos().containsKey(repoName)) {
            hipieService.getRepositoryManager().getRepos().get(repoName).getLatest(false);   
            LOGGER.info("Git fetch complete for repo {} on (Add plugin mode)",repoName);
        }else  if (isAdminPerspectiveView && repoNameArgument != null && hipieService.getRepositoryManager().getRepos().containsKey(repoNameArgument)) {
            hipieService.getRepositoryManager().getRepos().get(repoNameArgument).getLatest(false);   
            LOGGER.info("Git fetch complete for repo {} on (Admin perspective mode)",repoNameArgument);
        }
    }

    @Listen("onSelect = #repoList")
    public void onSelectRepository(SelectEvent<Component, String> event) {
        repoName = event.getSelectedObjects().iterator().next();
        try {

            createPluginTree(repoName);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(),
                    Constants.POSITION_TOP_CENTER, 0, true);
        }

    }

    private void createPluginTree(String repoName) throws PluginException {
        LOGGER.info("repoName inside plugin tree composer is {}",repoName);
        Map<String, List<Plugin>> pluginMap = ((PluginService)SpringUtil.getBean(Constants.PLUGIN_SERVICE)).getAllPlugins( ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(), repoName);
        pluginTreeChildren.getChildren().clear();
        for (Entry<String, List<Plugin>> entry : pluginMap.entrySet()) {
            // entry.getKey() - label

            Treeitem rootTreeItem = new Treeitem();
            Treerow rootTreeRow = new Treerow();
            Treechildren treeChildren = new Treechildren();

            rootTreeRow.setLabel(entry.getKey());
            rootTreeItem.appendChild(rootTreeRow);
            rootTreeItem.appendChild(treeChildren);
            rootTreeItem.setOpen(false);

            for (Plugin childPlugin : entry.getValue()) {
                Treeitem treeItemInner = new Treeitem();
                Treerow treeRowInner = new Treerow();
                Treecell treeCellInner = new Treecell();

                treeChildren.appendChild(treeItemInner);
                treeItemInner.appendChild(treeRowInner);
                treeRowInner.appendChild(treeCellInner);
                treeCellInner.setAttribute(Constants.PLUGIN, childPlugin);
                treeCellInner.setIconSclass("z-icon-puzzle-piece");
                treeCellInner.setSclass("plugintree-label");
                treeCellInner.setLabel(childPlugin.getLabel());
                
                if (!isAdminPerspectiveView) {
                    treeCellInner.addEventListener(
                            Events.ON_DOUBLE_CLICK,
                            (SerializableEventListener<? extends Event>) event -> Events.postEvent(EVENTS.ON_PLUGIN_ADD, AddPluginController.this.getSelf().getParent()
                                    .getParent().getFellow("flowChart"), childPlugin));
                    treeCellInner.setDraggable("true");
                }
                treeCellInner.setTooltiptext(childPlugin.getContract().getCanonicalName() + "  v"
                        + childPlugin.getContract().getVersion() + "\nby: " + childPlugin.getContract().getAuthor() + "  Repo: "
                        + childPlugin.getContract().getRepositoryName()+ "\nLast updated on: "+new SimpleDateFormat(Constants.DATE_FORMAT).format(childPlugin.getContract().getLastModified()));

                pluginTreeChildren.appendChild(rootTreeItem);
            }

        }

    }
    
    @Listen("onClose = #pluginsWindow")
    public void hidePluginBrowser(Event event) {
        if (!isAdminPerspectiveView) {
            Events.postEvent(EVENTS.ON_CLOSE_PLUGIN_BROWSER, data.getCompositionControllerComponent(), null);
            event.stopPropagation();
        }
    }
}