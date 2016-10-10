package org.hpccsystems.dsp.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractElement;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryManager;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.PluginException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.PluginService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.usergroupservice.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;

@Service("pluginService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PluginServiceImpl implements PluginService {

    private static final String ERROR_LOADING_PLUGINS = "errorloadingplugins";
    public static final String PLUGINREPOSITORY = "PluginRepository";
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginServiceImpl.class);

    private DSPDao dspDao;
    
    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }
    
    public List<String> getAllPluginRepos(User user) throws PluginException {
        Set<String> result = new HashSet<String>();
        Set<String> filtered = new TreeSet<String>();
        List<String> repos = new ArrayList<String>();
        try {
            // Load user's custom repos
            if (user != null && user.getMbsUser() != null && user.getMbsUser().getNVP(PLUGINREPOSITORY) != null) {
                Set<NameValuePair> nvps = user.getMbsUser().getNVP(PLUGINREPOSITORY);
                result = createResult(result, nvps);
            }
            // now filter out any repos that don't have any plugins viewable by
            // this user
            // also if user does not have plugin repo configured avoid filtering
            // and load default repo
            if (!result.isEmpty()) {
                List<ContractElement> contracts = HipieSingleton.getHipie().getContracts(user.getId(), "", null);
                if (contracts != null) {
                    filterRepos(result, filtered, contracts);
                }
            }
            // add default repo along with the user's custom repo
            repos.addAll(filtered);
            repos.add(RepositoryManager.DEFAULT_REPO_NAME);
        } catch (Exception e) {
            throw new PluginException(Labels.getLabel(ERROR_LOADING_PLUGINS), e);
        }
        return repos;
    }

    private void filterRepos(Set<String> result, Set<String> filtered, List<ContractElement> contracts) {
        for (ContractElement ce : contracts) {
            if (result.contains(ce.getRepositoryName().toUpperCase())) {
                filtered.add(ce.getRepositoryName());
            }
        }
    }

    private Set<String> createResult(Set<String> result, Set<NameValuePair> nvps) {
        Set<String> res = result;
        if (nvps != null) {
            for (NameValuePair nvp : nvps) {
                res.add(nvp.getValue().toUpperCase());
            }
        }
        return res;
    }

    @Override
    public Map<String, List<Plugin>> getAllPlugins(User user, String repoName) throws PluginException {

        Map<String, List<Plugin>> result = new HashMap<String, List<Plugin>>();

        List<ContractElement> contracts;
        try {
            contracts = HipieSingleton.getHipie().getContracts(user.getId(), "", repoName);
            Plugin plugin = null;
            String catagory = null;
            List<String> blockedPlugins = dspDao.getApplicationValueList(Constants.ValueCategory.BLACK_LIST_PLUGIN.name());
            if(CollectionUtils.isNotEmpty(contracts)) {
            	for (ContractElement contractElement : contracts) {
            		//The canonical name in the DB should be like 'HIPIE_Plugins.OutDataset.OutDataset' i.e without base directory
	                if (isSafePlugin(contractElement, blockedPlugins)) {
		                ErrorBlock errorBlock = contractElement.getContract().validate();
		                if (!(errorBlock.getErrors().isEmpty())) {
		                    LOGGER.info("Plugin - {} is invalid - {}", contractElement.getContract().getName(), errorBlock.getErrors().toString());
		                    continue;
		                }
		                catagory = contractElement.getCategory();
		
		                if (!result.containsKey(catagory)) {
		                    result.put(catagory, new ArrayList<Plugin>());
		                }
		                plugin = new Plugin(contractElement.getName(), contractElement.getRepositoryName());
		                plugin.setLabel(contractElement.getLabel());
		                plugin.setContract(contractElement.getContract());
		                plugin.setContract(contractElement.getContract());
		
		                result.get(catagory).add(plugin);
	                }
	            }
            }
        } catch (Exception e) {
            throw new PluginException(Labels.getLabel(ERROR_LOADING_PLUGINS), e);
        }

        return result;
    }

    @Override
    public List<String> getAllRepos() throws PluginException {
        Map<String, IRepository> repos = HipieSingleton.getHipie().getRepositoryManager().getRepos();
        List<String> allRepos = new ArrayList<String>();
        for (Map.Entry<String, IRepository> entry : repos.entrySet()) {
            allRepos.add(entry.getKey());

        }
        return allRepos;
    }

    @Override
    public Contract getDatasourceContract() throws HipieException {
        Contract contract;
        try {
            contract = HipieSingleton.getRawDataset();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Unable to get contract", e);
        }
        return contract;
    }
    
    /**
     * Checks if a plugin is in the Blacklisted plugins list. This will be a case insensitive match. 
     * @param objContractElement
     * @param blockedPlugins
     * @return true if the plugin is not in the list of blacklisted plugins. false otherwise.  
     */
    public boolean isSafePlugin (ContractElement objContractElement, List<String> blockedPlugins) {
 	   boolean result = false;
 	   if(CollectionUtils.isNotEmpty(blockedPlugins)){
 		   result = !blockedPlugins.stream().filter(s -> s.equalsIgnoreCase(objContractElement.getCanonicalName())).findFirst().isPresent();
 	   }
 	   return result;
    }
}
