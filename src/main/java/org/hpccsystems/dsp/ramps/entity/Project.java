package org.hpccsystems.dsp.ramps.entity;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.GridEntity;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;


public class Project extends GridEntity implements Cloneable {
    private static final long serialVersionUID = 1L;
    private String description;
    private String type;
    private String referenceId;
    private boolean isRunning;
    private List<Plugin> plugins;
    private ClusterConfig clusterConfig;
    private Date lastRunDate;
    private List<Plugin> filteredPlugins;
    private Component projectDetailsComponent;
    private static final String BASE_SCOPE = "thor_ramps";
    private static final Logger LOGGER = LoggerFactory.getLogger(Project.class);
    private DatasourceStatus datasourceStatus;
    
    public Project(CompositionElement compositionElement, String type, Date lastRunDate) throws HipieException {
        super(compositionElement.getName(), compositionElement.getAuthor(), compositionElement.getLabel(), compositionElement.getCanonicalName(),new Date(compositionElement.getLastModified()),compositionElement.getId());
        
        this.description = compositionElement.getDescription();
        this.lastRunDate = lastRunDate;
        this.type = type;
        try {
            this.plugins = HIPIEUtil.getOrderedPlugins(compositionElement.getComposition());
        } catch (Exception e) {
            throw new HipieException(e);
        }
    }
    
    public Project(CompositionInstance instance) throws HPCCException {
        super(instance.getComposition());
        ClusterConfig cluster = new ClusterConfig();
        Composition composition = instance.getComposition();
        this.description = composition.getDescription();
        this.clusterConfig = cluster;
        try {
            cluster.setId(instance.getHPCCConnection().getLabel());
            cluster.setRoxieCluster(instance.getHPCCConnection().getRoxieCluster());
            cluster.setThorCluster(instance.getHPCCConnection().getThorCluster());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            throw new HPCCException(Labels.getLabel("connectionIsNull"));
        }
    }

    public Project(CompositionElement compositionElement) {
        super(compositionElement.getName(), compositionElement.getAuthor(), compositionElement.getLabel(), compositionElement.getCanonicalName(),new Date(compositionElement.getLastModified()),compositionElement.getId());
        this.description = compositionElement.getDescription();
    }

    public Project() {
    }

    public List<String> getProperties() throws IntrospectionException {
        List<String> properties = new ArrayList<String>();
        BeanInfo info = java.beans.Introspector.getBeanInfo(this.getClass());
        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : props) {
            properties.add(propertyDescriptor.getName());
        }
        return properties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Plugin> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<Plugin> plugins) {
        this.plugins = plugins;
    }

    public ClusterConfig getClusterConfig() {
        return clusterConfig;
    }

    public void setClusterConfig(ClusterConfig clusterConfig) {
        this.clusterConfig = clusterConfig;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastRunDate() {
        return lastRunDate;
    }

    public void setLastRunDate(Date lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone() Implemented deep cloning
     */
    @Override
    public Project clone() throws CloneNotSupportedException {
        // TODO: Implement a better cloning menthod and set only properties that
        // need to be cloned
        Project project = (Project) super.clone();
        project.setName(null);
        project.setCanonicalName(null);
        project.setLabel(null);
        project.setRunning(false);
        return project;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nName").append(":").append(getName()).append(";\n");
        sb.append("Label").append(":").append(getLabel()).append(";\n");
        sb.append("Type").append(":").append(type).append(";\n");
        sb.append("Desc").append(":").append(description).append(";\n");
        sb.append("LastModifiedDate").append(":").append(getLastModifiedDate()).append(";\n");
        sb.append("lastRunDate").append(":").append(lastRunDate).append(";\n");
        sb.append("clusterConfig").append(":").append(clusterConfig).append(";\n");
        sb.append("author").append(":").append(getAuthor());
        return sb.toString();
    }

    /**
     * @return Returns HPCCConnection from HIPIE based on the ClusterConfig
     *         object associated with the project
     */
    public HPCCConnection getHpccConnection() {
        if (getClusterConfig() != null) {
            return getClusterConfig().getConnection();
        }
        return null;
    }
    
    public String getDisplayLabel() {
        if (StringUtils.isEmpty(getLabel())) {
            return getName();
        }
        return getLabel();
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public boolean hasReferenceId() {
        return referenceId != null;
    }
    
    public String getBaseScope() {
        StringBuilder builder = new StringBuilder();
        builder.append(BASE_SCOPE).append(Constants.SCOPE_RESOLUTION_OPR).append(getReferenceId());
        return builder.toString();
    }

    public List<Plugin> getFilteredPlugins() {
        return filteredPlugins;
    }

    public void setFilteredPlugins(List<Plugin> filteredPlugins) {
        this.filteredPlugins = filteredPlugins;
    }

    public Component getProjectDetailsComponent() {
        return projectDetailsComponent;
    }

    public void setProjectDetailsComponent(Component projectDetailsComponent) {
        this.projectDetailsComponent = projectDetailsComponent;
    }


    public DatasourceStatus getDatasourceStatus() {
        return datasourceStatus;
    }

    public void setDatasourceStatus(DatasourceStatus datasourceStatus) {
        this.datasourceStatus = datasourceStatus;
    }
    
    /**
     * Check if plugins has a DataSource plugin 
     * @return DatasetPlugin or null (if not a DataSource plugin)
     */
    public DatasetPlugin getDatasetPlugin() {
        
    	/*return (DatasetPlugin) plugins.stream()
                .filter(plugin -> plugin.isDatasourcePlugin())
                .findAny()
                .get();*/
    	DatasetPlugin dsPlugin = null;
    	for (Plugin testplugin : plugins) {
    		if(testplugin.isDatasourcePlugin()){
    			dsPlugin = (DatasetPlugin)testplugin;
    		} 
		}
    	
    	return dsPlugin;
    }
    
    /**
     * @param contractInstance
     * @return
     *  The plugin object containing the Contract Instance. 
     *  In case of Dataset plugin, DatasetPlugin instace is returned and Instance match is skipped here
     */
    public Plugin getPlugin(ContractInstance contractInstance) {
        Optional<Plugin> matchedPlugin;
        if(HIPIEUtil.isDataSourcePlugin(contractInstance)) {
            return getDatasetPlugin();
        } else {
            matchedPlugin = plugins.stream()
                    .filter(plugin -> !plugin.isDatasourcePlugin())
                    .filter(plugin -> plugin.getContractInstance() == contractInstance)
                    .findAny();
        
            if(matchedPlugin.isPresent()) {
                return matchedPlugin.get();
            } else {
                LOGGER.error("Unable to find Contract instance {} in Project", contractInstance.getName());
                return null;
            }
        }
    }

    public ContractInstance getContractInstance(String instanceId) {
        LOGGER.debug("Retriving Instance for {}", instanceId);
        
        for (Plugin plugin : getDatasetPlugin().getPlugins()) {
            if(instanceId.equals(plugin.getContractInstance().getInstanceID())){
                return plugin.getContractInstance();
            }
        }
        
        return plugins.stream()
            .filter(plugin -> !plugin.isDatasourcePlugin())
            .filter(plugin -> instanceId.equals(plugin.getContractInstance().getInstanceID()))
            .findAny()
            .get()
            .getContractInstance();
                    
    }

    public boolean isHookedToPlugin(ContractInstance conIns) {
        boolean isHooked = false;
        for (Plugin plugin : getPlugins()) {
            if (plugin.getContractInstance() != null) {
                for (Element element : plugin.getContractInstance().getContract().getInputElements()) {
                    if (plugin.getContractInstance().getProperty(element.getName()) != null && !isHooked
                            && plugin.getContractInstance().getProperty(element.getName()).contains("^")
                            && plugin.getContractInstance().getProperty(element.getName()).contains("|")) {
                        isHooked = conIns.getInstanceID()
                                .equals(plugin.getContractInstance().getProperty(element.getName()).substring(
                                        plugin.getContractInstance().getProperty(element.getName()).indexOf("^") + 1,
                                        plugin.getContractInstance().getProperty(element.getName()).indexOf("|")));
                    }
                }
            }

        }
        return isHooked;
    }
}
