package org.hpccsystems.dsp.admin.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.repo.GitRepository;
import org.hpcc.HIPIE.repo.HPCCRepository;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryType;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.PluginException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.RepositoryConfig;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.dsp.service.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ViewRepositoryController extends SelectorComposer<Component> {
    private static final String ADMIN_DSP_EDIT_REPOSITORY_ZUL = "/admin/dsp/edit_repository.zul";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClustersController.class);
    private ListModelList<RepositoryConfig> repositoryModel = new ListModelList<RepositoryConfig>();
    private ListModelList<String> permittedUsers;

    private List<RepositoryConfig> repositories = new ArrayList<RepositoryConfig>();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        try {
            HIPIEService hipieService = HipieSingleton.getHipie();
            IRepository repo = null;
            for (String repository : ((PluginService) SpringUtil.getBean("pluginService")).getAllRepos()) {
                RepositoryConfig repositoryConfig = new RepositoryConfig();
                repositoryConfig.setRepository(repository);
                repo = hipieService.getRepositoryManager().getRepos().get(repository);
                repositoryConfig.setPath(repo.getLocaldir());
                repositoryConfig.setType(repo.getType());
                repositoryConfig.setiRepository(repo);
                
                setGitLegacyRepoProps(repositoryConfig, repo);
                
                permittedUsers = new ListModelList<String>();
                if (((HPCCService) SpringUtil.getBean("hpccService")).isPublicRepository(repository)) {
                    permittedUsers.add("PUBLIC");
                } else {
                    permittedUsers.add("PRIVATE");
                }
                repositoryConfig.setPermittedUsers(permittedUsers);
                repositories.add(repositoryConfig);
            }

            repositoryModel.addAll(repositories);
        } catch (PluginException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    private void setGitLegacyRepoProps(RepositoryConfig repositoryConfig, IRepository repo) {
        if (RepositoryType.LEGACY_HPCC.equals(repo.getType())) {
            HPCCRepository hpccRepo = (HPCCRepository) repo;
            HPCCConnection conn = hpccRepo.getHPCCConnection();
            
            repositoryConfig.setHpccUserName(conn.getUserName());
            try {
                repositoryConfig.setHpccPassword(Utility.decrypt(conn.getPwd()));
            } catch (Exception e) {//Catch this exception, so that other props will get populated
                LOGGER.debug("Unable to retrieve password for HPCC Repo");
                LOGGER.error(Constants.EXCEPTION,e);
            }
            repositoryConfig.setServerHost(conn.getServerHost());
            repositoryConfig.setEspPort(conn.getServerPort());
            repositoryConfig.setAttrPort(conn.getAttributesPort());
            repositoryConfig.setHTTPS(conn.getIsHttps());
            
        }else if(RepositoryType.GIT.equals(repo.getType())){
            GitRepository gitRepo = (GitRepository) repo;
            repositoryConfig.setUrl(gitRepo.getUrl());
            
            if(repo.getSaveProperties().get(GitRepository.USER) != null){
                repositoryConfig.setGitUserName(String.valueOf(gitRepo.getSaveProperties().get(GitRepository.USER)));
            }
            Boolean encrypted = (Boolean) repo.getSaveProperties().get(GitRepository.PASSWORD_ENCRYPTED);
            String pwd = (String)repo.getSaveProperties().get(GitRepository.PASSWORD);
            if(pwd != null && encrypted != null){
                if (encrypted) {
                    repositoryConfig.setGitPassword(Utility.decrypt(pwd));
                } else{
                    repositoryConfig.setGitPassword(pwd);
                }
                repositoryConfig.setGitPwdEncrypted(false);
            }
        }
    }

    @Listen("onClick = #addRepository")
    public void onAddRepository() {
        Map<String, Object> args = new HashMap<>();
        Window window = (Window) Executions.createComponents(ADMIN_DSP_EDIT_REPOSITORY_ZUL, null, args);
        window.addEventListener(Constants.REFRESH_MODEL,
                (SerializableEventListener<? extends Event>) this::refreshModel);
        window.addEventListener(Constants.ADD_TO_MODEL, (SerializableEventListener<? extends Event>) this::addToModel);
        window.doModal();
        getSelf().invalidate();
    }

    @Listen("onEditRepository = #repositoryGrid")
    public void editRepository(ForwardEvent forwardEvent) {
        Map<String, Object> args = new HashMap<>();
        args.put(Constants.FORWARD_EVENT, forwardEvent);
        Window window = (Window) Executions.createComponents(ADMIN_DSP_EDIT_REPOSITORY_ZUL, null, args);
        window.addEventListener(Constants.REFRESH_MODEL,
                (SerializableEventListener<? extends Event>) this::refreshModel);
        window.addEventListener(Constants.ADD_TO_MODEL, (SerializableEventListener<? extends Event>) this::addToModel);
        window.doModal();
        getSelf().invalidate();
    }

    private void addToModel(Event event) {
        RepositoryConfig updateRepositoryConfig = (RepositoryConfig) event.getData();
        repositoryModel.add(updateRepositoryConfig);
    }

    private void refreshModel(Event event) {
        RepositoryConfig updateRepositoryConfig = (RepositoryConfig) event.getData();
        int i = repositoryModel.indexOf(updateRepositoryConfig);
        repositoryModel.remove(updateRepositoryConfig);
        repositoryModel.add(i, updateRepositoryConfig);
    }

    @Listen("onViewPlugin = #repositoryGrid")
    public void viewPlugin(ForwardEvent forwardEvent) {
        Event event = forwardEvent.getOrigin();
        Row row = (Row) event.getTarget().getParent().getParent();
        RepositoryConfig repositoryConfig = row.getValue();

        Map<String, Object> args = new HashMap<>();
        args.put(Constants.REPO, repositoryConfig.getRepository());
        Window window = (Window) Executions.createComponents("/ramps/project/add_plugins.zul", null, args);
        window.doModal();

        getSelf().invalidate();
    }

    public ListModelList<RepositoryConfig> getRepositoryModel() {
        return repositoryModel;
    }

    public ListModelList<String> getPermittedUsers() {
        return permittedUsers;
    }

}
