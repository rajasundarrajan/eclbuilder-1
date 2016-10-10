package org.hpccsystems.dsp.ramps.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zkplus.spring.SpringUtil;

public class HIPIEUtil {

    private static final String LOGICAL_FILENAME = "LogicalFilename";
    private static final Logger LOGGER = LoggerFactory.getLogger(HIPIEUtil.class);

    private HIPIEUtil() {
    }

    public static List<Plugin> getOrderedPlugins(Composition composition) {
        List<Plugin> plugins = new ArrayList<Plugin>();
        Plugin plugin;

        List<String> orderList = composition.getPartiallyOrderedCIs();

        ContractInstance contractInstance;
        Contract contract;
        DatasetPlugin datasetPlugin = null;
        int count = 0;
        for (String id : orderList) {
            contractInstance = composition.getContractInstance(id);
            ElementOption option = contractInstance.getOption(Contract.LABEL);
            contract = contractInstance.getContract();
            plugin = new Plugin(contract.getName(), contract.getRepositoryName());
            
            if(option != null) {
                plugin.setLabel(option.getParams().get(0).getFieldLabel());
            } else {
                plugin.setLabel(contract.getLabel());
            }
            
            plugin.setContractInstance(contractInstance);
            plugin.setInstanceId(id);
            
            if (isDataSourcePlugin(contractInstance)) {
            	//If the plugin already has a DataSource plugin then dont create a new DatasetPlugin
            	if(plugins.size() > 0 ){
            		for(Plugin addPlugin : plugins) {
            			count = addPlugin.isDatasourcePlugin() ? ++count : count;
            		}
            	}
            	if(count == 0) {
            		datasetPlugin = new DatasetPlugin(null, null);
            		plugins.add(datasetPlugin);
            	}
	            
            	datasetPlugin.addPlugin(plugin);
            } else {
                plugins.add(plugin);
            }

        }

        return plugins;
    }
    
    /**
     * Checks if a plugin is an input plugin (CATEGORY = INPUT)
     * @param contractInstance
     * @return {@code true} if an input plugin, {@code false} otherwise.
     */
    public static boolean isInputPlugin(ContractInstance contractInstance) {
    	boolean isInputPlugin = false;
        isInputPlugin = Constants.INPUT.equalsIgnoreCase(contractInstance.getContract().getProperty(Contract.CATEGORY).toString()) ? true : false;
        
        return isInputPlugin;
    }
    
    public static boolean isDataSourcePlugin(ContractInstance contractInstance) {

        boolean logicalFileExists = contractInstance.getProps().containsKey(LOGICAL_FILENAME);
        boolean structureExists = contractInstance.getProps().containsKey("Structure");
        boolean isDatasetType = false;
        for (Element element : contractInstance.getContract().getInputElements()) {
            if (InputElement.TYPE_DATASET.equals(element.getType())) {
                isDatasetType = true;
                break;
            }
        }
        
        return logicalFileExists && structureExists && !isDatasetType;
    }

    /**
     * Assoicates Contract instance to Plugin.
     * Pre-populates OUTDATASET file name with referenceScope, if provided 
     * 
     * @param plugin
     * @param referenceScope
     *  Can be null.
     * @throws HipieException
     * @throws Exception
     */
    public static void associateContractInstance(Plugin plugin, String referenceScope) throws HipieException {
        if (plugin.getContractInstance() == null) {
            associateContractInstance(plugin);
        }
        
        if(referenceScope != null && HipieSingleton.OUTDATASET.equals(plugin.getContract().getName())){
            Map<String, Property> props = plugin.getContractInstance().getProps();
            Property prop = new Property();
            StringBuilder nameBuffer = new StringBuilder();
            nameBuffer.append(Constants.TILDE).append(referenceScope).append(Constants.SCOPE_RESOLUTION_OPR);            
            prop.add(nameBuffer.toString());
            props.put(Constants.NAME, prop);
        }
    }
    
    private static void associateContractInstance(Plugin plugin) throws HipieException {
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean("authenticationService");
        Contract contract = null;
        try {
            contract = HipieSingleton.getHipie().getContract(authenticationService.getCurrentUser().getId(), plugin.getName(),
                    plugin.getRepository(), null, false);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Unable to get contract", e);
        }
        plugin.setContractInstance(contract.createContractInstance());
    }

    public static String getFilename(Composition composition) {
        Optional<ContractInstance> datasource = composition.getContractInstances().values().stream()
                .filter(contractInstance -> isDataSourcePlugin(contractInstance)).findFirst();

        if (datasource.isPresent()) {
            return datasource.get().getProperty(LOGICAL_FILENAME);
        }

        return null;
    }

    /**
     * Creates a new name for composition which doesn't exists
     * 
     * @param Composition
     *            name
     * @return Escaped new composition name
     * @throws HipieException
     */
    public static String createCompositionName(String name,boolean isStaticData) throws HipieException {
        HIPIEService hipieService = HipieSingleton.getHipie();
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(RampsUtil.removeSpaceSplChar(name));
        try {
            String[] extensions = new String[] { Composition.FILEEXTENSION };
            for (int i = 1; hipieService.fileExists(HipieSingleton.getHipie().getRepositoryManager().getDefaultCompositionRepository(), nameBuilder.toString(), extensions); i++) {
                nameBuilder.append(i);
            }
        } catch (Exception e) {
            throw new HipieException(e);
        }
        if(isStaticData){
            nameBuilder.append(Dashboard.DATA_BOMB_SUFFIX);
        }
        return nameBuilder.toString().toLowerCase();
    }
}
