package org.hpccsystems.dsp.admin.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.repo.FileRepository;
import org.hpcc.HIPIE.repo.GitRepository;
import org.hpcc.HIPIE.repo.HPCCRepository;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryType;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.init.ClusterManager;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.RepositoryConfig;
import org.hpccsystems.dsp.ramps.entity.RepositoryConfig.ACTION;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.error.ErrorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.InputEvent;
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
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class EditRepositoryController extends SelectorComposer<Window> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EditRepositoryController.class);
    private ListModelList<RepositoryType> typeModel = new ListModelList<RepositoryType>();
    private List<RepositoryType> types = Arrays.asList(RepositoryType.values());
    private ListModelList<String> filteredUsers = new ListModelList<String>();
    private ListModelList<String> selectedUsers = new ListModelList<String>();
    private Set<String> selectedUsersSet;
    private static final String DIRECTORY_IS_NOT_FOUND = "directoryIsNotFound";
    public static final String PLUGINREPOSITORY = "PluginRepository";
    private static final String ADD_REPO_FAILED = "addRepoFailed";

    @Wire("#serverHost, #hpccUserName, #hpccPassword")
    List<Textbox> boxes;

    @Wire("#espPort, #attrPort")
    List<Textbox> intboxes;

    @Wire
    private Grid hpcc;

    @Wire
    private Textbox path;

    @Wire
    private Textbox url;

    @Wire
    private Textbox gitUserName;

    @Wire
    private Textbox gitPassword;

    @Wire
    private Textbox serverHost;

    @Wire
    private Textbox espPort;

    @Wire
    private Textbox attrPort;

    @Wire
    private Textbox hpccUserName;

    @Wire
    private Textbox hpccPassword;

    @Wire
    private Checkbox isHTTPS;
    @Wire
    private Checkbox isPublic;

    @Wire
    private Grid git;

    @Wire
    private Button updateRepository;

    @Wire
    private Textbox reponame;

    @Wire
    private Combobox type;
    
    @Wire
    private Listbox usersListbox;
   
    @Wire
    private Listbox  searchListbox;
    
    RepositoryType selectedType = null;

    boolean isUpdate = true;

    RepositoryConfig updateRepositoryConfig;

    IRepository updateIRepository;
    
    ListitemRenderer<String> customUserRenderer = (item, value, i) -> {
        Listcell cell = new Listcell();
        cell.setParent(item);
        cell.setLabel(value);
        Button deleteButton = new Button();
        deleteButton.setParent(cell);
        deleteButton.setIconSclass("z-icon-trash-o");
        deleteButton.setSclass("img-btn");
        deleteButton.setStyle("float:right");
        deleteButton.addEventListener(Events.ON_CLICK, (SerializableEventListener<? extends Event>) event -> {
            // delete it from nvp
        });

    };

    @Override
    public void doAfterCompose(Window comp) throws Exception {
        super.doAfterCompose(comp);
        ForwardEvent forwardEvent = (ForwardEvent) Executions.getCurrent().getArg().get(Constants.FORWARD_EVENT);
        typeModel.addAll(types);
        type.setModel(typeModel);

        if (forwardEvent != null) {
            Event event = forwardEvent.getOrigin();
            Row row = (Row) event.getTarget().getParent().getParent();
            RepositoryConfig repositoryConfig = row.getValue();
            reponame.setValue(repositoryConfig.getRepository());
            typeModel.addToSelection(repositoryConfig.getType());
            this.getSelf().setTitle("Edit Repository: " + repositoryConfig.getRepository());
            isUpdate = true;
            isPublic.setChecked(((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).isPublicRepository(repositoryConfig.getRepository()));
            onCheckPublic();
            updateRepository.setLabel("Update");
            reponame.setDisabled(true);
            type.setDisabled(true);
            onChangeType();

            if (selectedType == RepositoryType.LEGACY_HPCC) {

                serverHost.setValue(repositoryConfig.getServerHost());
                espPort.setValue(String.valueOf(repositoryConfig.getEspPort()));
                attrPort.setValue(String.valueOf(repositoryConfig.getAttrPort()));
                hpccUserName.setValue(repositoryConfig.getHpccUserName());
                hpccPassword.setValue(repositoryConfig.getHpccPassword());
                isHTTPS.setValue(repositoryConfig.isHTTPS());
                path.setValue(repositoryConfig.getPath());
            } else if (selectedType == RepositoryType.GIT) {

                url.setValue(repositoryConfig.getUrl());
                gitUserName.setValue(repositoryConfig.getGitUserName());
                gitPassword.setValue(repositoryConfig.getGitPassword());
                path.setValue(repositoryConfig.getPath());
            } else {
                path.setValue(repositoryConfig.getPath());
            }

            updateRepositoryConfig = repositoryConfig;
            updateIRepository = repositoryConfig.getiRepository();
        } else {
            reponame.setValue(null);
            type.setValue(null);
            path.setValue(null);
            updateRepository.setLabel("Add");
            this.getSelf().setTitle("Add new Repository");
            reponame.setDisabled(false);
            type.setDisabled(false);
            isUpdate = false;
        }
        
        usersListbox.setItemRenderer(customUserRenderer);
        usersListbox.setModel(selectedUsers);
    }
    

    @Listen("onSelect = #type")
    public void onChangeType() {
        selectedType = typeModel.getSelection().iterator().next();

        if (selectedType == RepositoryType.LEGACY_HPCC) {
            hpcc.setVisible(true);
            git.setVisible(false);
            serverHost.setValue(null);
            espPort.setValue(null);
            attrPort.setValue(null);
            hpccUserName.setValue(null);
            hpccPassword.setValue(null);
            isHTTPS.setChecked(false);

        } else if (selectedType == RepositoryType.GIT) {
            git.setVisible(true);
            hpcc.setVisible(false);
            url.setValue(null);
            gitUserName.setValue(null);
            gitPassword.setValue(null);

        } else {
            git.setVisible(false);
            hpcc.setVisible(false);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Selection - " + selectedType);
        }
        getSelf().invalidate();
    }

    @Listen("onClick = #updateRepository")
    public void onSaveRepository() {
        if (isUpdate) {
            if (validate()) {
                try {
                    ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).updatePublicRepository(reponame.getValue(), isPublic.isChecked());
                } catch (DatabaseException e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000);
                    return;
                }
                if (selectedType != null) {
                    updateRepositoryConfig.setRepository(reponame.getValue());
                    if (RepositoryType.FILE.equals(selectedType.toString())) {

                        updateRepositoryConfig.setRepository(reponame.getValue());
                        updateRepositoryConfig.setType(RepositoryType.FILE);
                        updateRepositoryConfig.setPath(path.getValue());

                        ((FileRepository) updateIRepository).setLocaldir(path.getValue());
                        File localdirfile = new File(path.getValue());

                        if (!localdirfile.exists()) {
                            Clients.showNotification(Labels.getLabel(DIRECTORY_IS_NOT_FOUND), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER, 3000);
                            return;
                        }
                        ((FileRepository) updateIRepository).setType(RepositoryType.FILE);

                    } else if (RepositoryType.GIT.equals(selectedType.toString())) {

                        if (typeGitValidate()) {

                            updateRepositoryConfig.setType(RepositoryType.GIT);
                            updateRepositoryConfig.setPath(path.getValue());
                            updateRepositoryConfig.setUrl(url.getValue());
                            updateRepositoryConfig.setGitUserName(gitUserName.getValue());
                            updateRepositoryConfig.setGitPassword(gitPassword.getValue());

                            IRepository repo = HipieSingleton.getHipie().getRepositoryManager().getRepos().get(reponame.getText());
                            Map<String, Object> props = repo.getSaveProperties();
                            props.put(GitRepository.URL, url.getValue());
                            props.put(GitRepository.USER, gitUserName.getValue());
                            props.put(GitRepository.PASSWORD, gitPassword.getValue());
                            props.put(GitRepository.PASSWORD_ENCRYPTED, updateRepositoryConfig.isGitPwdEncrypted());
                            props.put(GitRepository.TYPE, RepositoryType.GIT);
                            props.put(GitRepository.LOCALDIR, path.getValue());

                            try {
                                updateIRepository = new GitRepository(props);
                            } catch (Exception e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                                Clients.showNotification(Labels.getLabel("gitException"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000);
                                return;
                            }

                            ErrorBlock error;
                            try {
                                error = ((GitRepository) updateIRepository).getLatest(false);
                            } catch (Exception e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                                Clients.showNotification(Labels.getLabel("fetchingRepoStatusFailed"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000); 
                                return;
                            }

                            if (error != null && !error.isEmpty()) {
                                Clients.showNotification(error.iterator().next().toString(), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER,
                                        3000);
                                return;
                            }

                        } else {
                            return;
                        }

                    } else {

                        if (typeHPCCValidate()) {

                            updateRepositoryConfig.setType(RepositoryType.LEGACY_HPCC);
                            updateRepositoryConfig.setPath(path.getValue());
                            updateRepositoryConfig.setServerHost(serverHost.getValue());
                            updateRepositoryConfig.setEspPort(Integer.parseInt(espPort.getValue()));
                            updateRepositoryConfig.setAttrPort(Integer.parseInt(attrPort.getValue()));
                            updateRepositoryConfig.setHpccUserName(hpccUserName.getValue());
                            updateRepositoryConfig.setHpccPassword(hpccPassword.getValue());
                            updateRepositoryConfig.setHTTPS(isHTTPS.isChecked());
                            ((HPCCRepository) updateIRepository).setType(RepositoryType.LEGACY_HPCC);
                            ((HPCCRepository) updateIRepository).setLocaldir(path.getValue());
                            ErrorBlock error = null;
                            try {
                                error = ((HPCCRepository) updateIRepository).getLatest(false);
                            } catch (Exception e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                                Clients.showNotification(Labels.getLabel("fetchingRepoStatusFailed"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000);
                                return;
                            }
                            if (!error.isEmpty()) {
                                Clients.showNotification(error.iterator().next().toString(), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER,
                                        3000);
                                return;
                            }
                            ((HPCCRepository) updateIRepository).setServerHost(serverHost.getValue());
                            ((HPCCRepository) updateIRepository).setServerPort(Integer.parseInt(espPort.getValue()));
                            ((HPCCRepository) updateIRepository).setUsername(hpccUserName.getValue());
                        } else {
                            return;
                        }

                    }
                }
                addPermissionsToRepositories();
                Events.postEvent(Constants.REFRESH_MODEL, this.getSelf(), updateRepositoryConfig);

                try {   
                    //Updates Repo in own server's .cfg file
                    HipieSingleton.getHipie().getRepositoryManager().saveRepositoryProperties(); 
                    
                    //Updates Repo in other server's .cfg file
                    updateRepositoryConfig.setAction(ACTION.UPDATE);
                    ClusterManager.syncRepository(updateRepositoryConfig);
                                      
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(Labels.getLabel("notAbleToSave"), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000);
                    return;
                }

                Clients.showNotification(Labels.getLabel("repositoryUpdateSuccessfully"), Clients.NOTIFICATION_TYPE_INFO, getSelf(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
            }

        } else {
            updateRepositoryConfig = new RepositoryConfig();
            if (validate()) {
                
                //db update
            
                if (selectedType != null) {
                    updateRepositoryConfig.setRepository(reponame.getValue());
                    if (RepositoryType.FILE.equals(selectedType.toString())) {

                        try {

                            FileRepository repository = new FileRepository("", path.getValue());
                            repository.setType(RepositoryType.FILE);
                            updateRepositoryConfig.setiRepository(repository);
                            HipieSingleton.getHipie().getRepositoryManager().addRepository(reponame.getValue(), repository);
                        } catch (ConfigurationException e) {
                            LOGGER.error(Constants.EXCEPTION, e);
                        } catch (WrongValueException e) {
                            LOGGER.error(Constants.EXCEPTION, e);
                        } catch (Exception e) {
                            LOGGER.error(Constants.EXCEPTION, e);
                            Clients.showNotification(Labels.getLabel(ADD_REPO_FAILED)+" : "+e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER, 3000);
                            return;
                        }

                        updateRepositoryConfig.setType(RepositoryType.FILE);
                        updateRepositoryConfig.setPath(path.getValue());

                    } else if (RepositoryType.GIT.equals(selectedType.toString())) {

                        if (typeGitValidate()) {

                            try {
                                Map<String, Object> props = new HashMap<String, Object>();
                                props.put(GitRepository.TYPE, RepositoryType.GIT.toString());
                                props.put(GitRepository.URL, url.getValue());
                                props.put(GitRepository.USER, gitUserName.getValue());
                                props.put(GitRepository.PASSWORD, gitPassword.getValue());
                                props.put(GitRepository.PASSWORD_ENCRYPTED, false);
                                props.put(GitRepository.LOCALDIR, path.getValue());
                                GitRepository repository = new GitRepository(props);

                                updateRepositoryConfig.setiRepository(repository);
                                HipieSingleton.getHipie().getRepositoryManager().addRepository(reponame.getValue(), repository);
                            } catch (ConfigurationException e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                            } catch (WrongValueException e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                            } catch (Exception e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                                Clients.showNotification(Labels.getLabel(ADD_REPO_FAILED)+" : "+e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER,
                                        3000);
                                return;
                            }

                            updateRepositoryConfig.setType(RepositoryType.GIT);
                            updateRepositoryConfig.setPath(path.getValue());
                            updateRepositoryConfig.setUrl(url.getValue());
                            updateRepositoryConfig.setGitUserName(gitUserName.getValue());
                            updateRepositoryConfig.setGitPassword(gitPassword.getValue());
                            updateRepositoryConfig.setGitPwdEncrypted(false);
                        } else {
                            return;
                        }

                    } else {
                        if (typeHPCCValidate()) {

                            try {
                                HPCCRepository repository = new HPCCRepository(serverHost.getValue(), Integer.parseInt(espPort.getValue()),
                                        Integer.parseInt(attrPort.getValue()), hpccUserName.getValue(), hpccPassword.getValue(), isHTTPS.isChecked(), path.getValue());
                                repository.setType(RepositoryType.LEGACY_HPCC);
                                updateRepositoryConfig.setiRepository(repository);
                                HipieSingleton.getHipie().getRepositoryManager().addRepository(reponame.getValue(), repository);
                            } catch (ConfigurationException e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                            } catch (WrongValueException e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                            } catch (Exception e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                                Clients.showNotification(Labels.getLabel(ADD_REPO_FAILED)+" : "+e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER,
                                        3000);
                                return;
                            }
                            updateRepositoryConfig.setType(RepositoryType.LEGACY_HPCC);
                            updateRepositoryConfig.setPath(path.getValue());
                            updateRepositoryConfig.setServerHost(serverHost.getValue());
                            updateRepositoryConfig.setEspPort(Integer.parseInt(espPort.getValue()));
                            updateRepositoryConfig.setAttrPort(Integer.parseInt(attrPort.getValue()));
                            updateRepositoryConfig.setHpccUserName(hpccUserName.getValue());
                            updateRepositoryConfig.setHpccPassword(hpccPassword.getValue());
                            updateRepositoryConfig.setHTTPS(isHTTPS.isChecked());
                        } else {
                            return;
                        }
                    }

                }
                addPermissionsToRepositories();
                Events.postEvent(Constants.ADD_TO_MODEL, this.getSelf(), updateRepositoryConfig);

                try {
                    //Adds Repo in own server's .cfg file
                    HipieSingleton.getHipie().getRepositoryManager().saveRepositoryProperties();
                    
                    //Adds Repo in other server's .cfg file
                    updateRepositoryConfig.setAction(ACTION.ADD);
                    ClusterManager.syncRepository(updateRepositoryConfig);
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                }
                Clients.showNotification(Labels.getLabel("repositoryAddedSuccessfully"), Clients.NOTIFICATION_TYPE_INFO, getSelf(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
            }
        }
        
       
    }

    private boolean validate() {
        if (StringUtils.isEmpty(reponame.getValue())) {

            Clients.showNotification(Labels.getLabel("enterRepositoryName"), Clients.NOTIFICATION_TYPE_ERROR, reponame, Constants.POSITION_END_AFTER, 3000);
            return false;
        } else if (StringUtils.isEmpty(type.getValue())) {

            Clients.showNotification(Labels.getLabel("selectType"), Clients.NOTIFICATION_TYPE_ERROR, type, Constants.POSITION_END_AFTER, 3000);
            return false;
        } else if (StringUtils.isEmpty(path.getValue())) {

            Clients.showNotification(Labels.getLabel("enterLocalDirectoryPath"), Clients.NOTIFICATION_TYPE_ERROR, path, Constants.POSITION_END_AFTER, 3000);
            return false;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Add / Update Repository ");
        }
        return true;
    }

    private boolean typeGitValidate() {
        if (StringUtils.isEmpty(url.getValue())) {

            Clients.showNotification(Labels.getLabel("enterURL"), Clients.NOTIFICATION_TYPE_ERROR, url, Constants.POSITION_END_AFTER, 3000);
            return false;
        } else if (StringUtils.isEmpty(gitUserName.getValue())) {

            Clients.showNotification(Labels.getLabel("enterUsername"), Clients.NOTIFICATION_TYPE_ERROR, gitUserName, Constants.POSITION_END_AFTER, 3000);
            return false;
        } else if (StringUtils.isEmpty(gitPassword.getValue())) {

            Clients.showNotification(Labels.getLabel("enterPassword"), Clients.NOTIFICATION_TYPE_ERROR, gitPassword, Constants.POSITION_END_AFTER, 3000);
            return false;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Add/ Update Git Repository ");
        }
        return true;
    }

    private boolean typeHPCCValidate() {
        for (Textbox box : boxes) {
            if (StringUtils.isEmpty(box.getValue())) {
                Clients.showNotification(Labels.getLabel("enterValidValue"), Clients.NOTIFICATION_TYPE_ERROR, box, Constants.POSITION_AFTER_CENTER, 3000);
                return false;
            }
        }
        for (Textbox box : intboxes) {
            if (!isInteger(box.getValue())) {
                Clients.showNotification(Labels.getLabel("enterIntegerValue"), Clients.NOTIFICATION_TYPE_ERROR, box, Constants.POSITION_AFTER_CENTER, 3000);
                return false;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Add / Update Git Repository ");
        }
        return true;
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
    
    
    @Listen("onCheck = #isPublic")
    public void onCheckPublic(){
        if(isPublic.isChecked()){
            usersListbox.setVisible(false);
        }else{
            usersListbox.setVisible(true);
        }
      
    }
    
    @Listen("onChanging = #onSearchUsers")
    public void onChangingUsers(InputEvent event){
        Collection<String> customUsers = new ArrayList<String>();
        try {
            Collection<String> seachableUsers =CompositionUtil.getAllUsers();
            if(seachableUsers != null){
                customUsers.addAll(seachableUsers.stream()
                        .filter(user -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(user, event.getValue()) ||org.apache.commons.lang3.StringUtils.containsIgnoreCase(user, event.getValue()))
                        .collect(Collectors.toList()));
            }
            filteredUsers.clear();
            filteredUsers.addAll(customUsers);
            searchListbox.setModel(filteredUsers);
        } catch (AuthenticationException e) {
          searchListbox.setEmptyMessage(Labels.getLabel("noUsersToShow"));
          LOGGER.error(Constants.EXCEPTION, e);
        }
    }
    
    @Listen("onSelect =  #searchListbox")
    public void onselectUsers(SelectEvent<?, ?> event){
       Set<Object> selectedUserse = ((ListModelList<Object>) searchListbox.getModel()).getSelection();
       if(CollectionUtils.isNotEmpty(selectedUserse)){
           String user = (String)selectedUserse.iterator().next();
           selectedUsersSet =  new HashSet<String>();
           selectedUsersSet.add(user);
           selectedUsers.addAll(selectedUsersSet);
       }
       
    }
    
    public void addPermissionsToRepositories(){
        try {
            ((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).updatePublicRepository(reponame.getValue(), isPublic.isChecked());
            LOGGER.debug("Added/ Updated repo details to  db");
        } catch (DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.toString(), Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_AFTER_CENTER, 3000);
        } 
      
    }
}
