package org.hpccsystems.dsp.ramps.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetPlugin.class);

    private List<Plugin> plugins;

    public static enum ERROR {
        FILE_MISSING, STRUCTURE_MISMATCH, NO_ERROR
    }

    public DatasetPlugin(String name, String repo) {
        super(name, repo);
    }

    @Override
    public DatasetPlugin clone() throws CloneNotSupportedException {
        DatasetPlugin clone = (DatasetPlugin) super.clone();

        List<Plugin> clonedPlugins = new ArrayList<>();
        for (Plugin plugin : plugins) {
            clonedPlugins.add(plugin.clone());
        }
        clone.setPlugins(clonedPlugins);

        return clone;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    public void addPlugin(Plugin plugin) {
        if (plugins == null) {
            plugins = new ArrayList<Plugin>();
            this.setName(plugin.getName());
            this.setLabel(plugin.getLabel());
            this.setRepository(plugin.getRepository());
        }
        plugins.add(plugin);
    }

    public void removePlugin(Plugin plugin) {
        if (plugins != null) {
            plugins.remove(plugin);
        }
    }

    /**
     * @return true when at least one dataset is present and all dataset objects
     *         contains valid file
     */
    public boolean isAllFilesSelected() {
        if (CollectionUtils.isNotEmpty(plugins)) {
            for (Plugin plugin : plugins) {
                if (Constants.FILE.equals(plugin.getContractInstance().getProperty(Constants.LOGICAL_FILENAME))) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    public boolean isAnyFileSelected() {
        for (Plugin plugin : plugins) {
            if (!Constants.FILE.equals(plugin.getContractInstance().getProperty(Constants.LOGICAL_FILENAME))) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return This method will return true if more than one file is chosen on
     *         the file browser for data source plugin
     */
    @Override
    public boolean hasMultipleOutputs() {
        return plugins != null ? plugins.size() > 1 : false;
    }

    public Contract getUseDatasetContract() {
        if (CollectionUtils.isNotEmpty(plugins)) {
            return plugins.iterator().next().getContractInstance().getContract();
        }
        return null;
    }

    @Override
    public ContractInstance getContractInstance() {
        if (!hasMultipleOutputs()) {
            return plugins.iterator().next().getContractInstance();
        }

        LOGGER.error("Coding error. This method should not be called when multiple Datasources are pesent");
        return super.getContractInstance();
    }

    @Override
    public boolean isDatasourcePlugin() {
        return this instanceof DatasetPlugin;
    }

    @Override
    public boolean hasMultipleInputs() {
        // Datasource plugins will never have multiple inputs
        return false;
    }

    @Override
    public boolean hasMultiplePorts() {
        // Due to lack of inputs, ports can just be decided with outputs
        return hasMultipleOutputs();
    }

    public boolean containsFile(String logicalFileName) {
        for (Plugin plugin : plugins) {
            if (plugin.isConfigured() && logicalFileName.equals(plugin.getLogicalFileNameUsingProperty())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DatasetPlugin [plugins=" + plugins + "]";
    }

    /**
     * Clears all files selected and resets them to FILE
     */
    public void clearAllFiles() {
        plugins.forEach(p -> p.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, Constants.FILE));
    }

    /**
     * @return Whether all dataset have files selected
     */
    public boolean hasValidFiles() {
        for (Plugin plugin : plugins) {
            if (!plugin.isConfigured() || StringUtils.isBlank(plugin.getLogicalFileNameUsingProperty()) || Constants.FILE.equals(plugin.getLogicalFileNameUsingProperty())) {
                return false;
            }
        }
        return true;
    }

    public ERROR updateValidStructures(HPCCConnection hpccConnection) {
        return validate(hpccConnection, true);
    }

    public ERROR validate(HPCCConnection hpccConnection) {
        return validate(hpccConnection, false);
    }

    private ERROR validate(HPCCConnection hpccConnection, boolean update) {
        ERROR error = ERROR.NO_ERROR;

        for (Plugin plugin : plugins) {
            if(plugin.isConfigured()){
                RecordInstance datasetFields;
    
                // Skipping validation for non-selected files
                if (plugin.getLogicalFileName() == null) {
                    continue;
                }
    
                try {
                    datasetFields = hpccConnection.getDatasetFields(plugin.getLogicalFileNameUsingProperty(), null);
                } catch (Exception e) {
                    LOGGER.error("Logical file '" + plugin.getLogicalFileNameUsingProperty() + "' may be missing.", e);
                    error = ERROR.FILE_MISSING;
                    if (update) {
                        plugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, Constants.FILE);
                    }
                    continue;
                }
    
                if (datasetFields == null) {
                    LOGGER.error("Logicalfile '" + plugin.getLogicalFileNameUsingProperty() + "' is missing.");
                    error = ERROR.FILE_MISSING;
                    if (update) {
                        plugin.getContractInstance().setProperty(Constants.LOGICAL_FILENAME, Constants.FILE);
                    }
                } else {
                    if (!datasetFields.toEclString().equals(plugin.getContractInstance().getProperty(Constants.STRUCTURE))) {
                        LOGGER.error("Structure mismatch");
    
                        // Setting Structure mismatch error only when all files are
                        // present
                        if (error != ERROR.FILE_MISSING) {
                            error = ERROR.STRUCTURE_MISMATCH;
                        }
    
                        if (update) {
                            plugin.getContractInstance().setProperty(Constants.STRUCTURE, datasetFields.toString());
                        }
                    }
                }
            }
        }

        LOGGER.debug("Validation result - {}", error);
        return error;
    }

    /**
     * Updates structures for files available in provided HPCCConnection.
     */
    public void updateStructures(HPCCConnection hpccConnection) {
        for (Plugin plugin : plugins) {
            RecordInstance datasetFields;
            try {
                datasetFields = hpccConnection.getDatasetFields(plugin.getLogicalFileNameUsingProperty(), null);

                if (datasetFields == null) {
                    LOGGER.error("Update skipped. File - '" + plugin.getLogicalFileNameUsingProperty() + "' not found.");
                } else {
                    plugin.getContractInstance().setProperty(Constants.STRUCTURE, datasetFields.toEclString());
                }
            } catch (Exception e) {
                LOGGER.error("Update failed for file '" + plugin.getLogicalFileNameUsingProperty() + "'", e);
            }
        }
    }
}