package org.hpccsystems.dsp.ramps.controller.entity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Html;
import org.zkoss.zul.Include;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Tab;

public class TabData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Constants.Flow flow;
    private Project project;
    private Dashboard dashboard;
    private Composition composition;
    private Component projectDetailComponent;
    private Component fileBrowserComponent;
    private Component compositionControllerComponent;
    private Html htmlHolder;
    private Tab filePreviewTab;
    private Tab projectTab;
    private Include usedatasetFormHolder;
    private boolean notifyUser = true;
    
    private ListModelList<GlobalVariable> globalVariablesModel;
    
    /**
     * Need this composition while editing the GCID as the composition will be opened twice in edit mode
     */
    private Composition originalComposition;
    
    private boolean datasourceValidated;

    public TabData() {
    }

    public TabData(Project project, Flow flow, Composition composition) {
        this.project = project;
        this.flow = flow;
        this.composition = composition;
    }
    
    public TabData(Dashboard dashboard, Flow flow, Composition composition) {
        this.dashboard = dashboard;
        this.flow = flow;
        this.composition = composition;
    }

    public TabData(Project project, Flow flow) {
        this.project = project;
        this.flow = flow;
    }
    
    public TabData(Dashboard dashboard, Flow flow) {
        this.dashboard = dashboard;
        this.flow = flow;
    }

    public Composition getOriginalComposition() {
        return originalComposition;
    }

    public void setOriginalComposition(Composition originalComposition) {
        this.originalComposition = originalComposition;
    }

    public Flow getFlow() {
        return flow;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Composition getComposition() {
        return composition;
    }

    public void setComposition(Composition composition) {
        this.composition = composition;
    }

    public Component getProjectDetailComponent() {
        return projectDetailComponent;
    }

    public void setProjectDetailComponent(Component component) {
        this.projectDetailComponent = component;
    }

    public Html getHtmlHolder() {
        return htmlHolder;
    }

    public void setHtmlHolder(Html htmlHolder) {
        this.htmlHolder = htmlHolder;
    }

    public Component getFileBrowserComponent() {
        return fileBrowserComponent;
    }

    public void setFileBrowserComponent(Component fileBrowserComponent) {
        this.fileBrowserComponent = fileBrowserComponent;
    }

    public Tab getFilePreviewTab() {
        return filePreviewTab;
    }

    public void setFilePreviewTab(Tab filePreviewTab) {
        this.filePreviewTab = filePreviewTab;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Component getCompositionControllerComponent() {
        return compositionControllerComponent;
    }

    public void setCompositionControllerComponent(Component compositionControllerComponent) {
        this.compositionControllerComponent = compositionControllerComponent;
    }

    public Tab getProjectTab() {
        return projectTab;
    }

    public void setProjectTab(Tab projectTab) {
        this.projectTab = projectTab;
    }

    public Include getUsedatasetFormHolder() {
        return usedatasetFormHolder;
    }

    public void setUsedatasetFormHolder(Include usedatasetFormHolder) {
        this.usedatasetFormHolder = usedatasetFormHolder;
    }

    public ListModelList<GlobalVariable> getGlobalVariablesModel() {
        return globalVariablesModel;
    }

    public void setGlobalVariablesModel(ListModelList<GlobalVariable> globalVariablesModel) {
        this.globalVariablesModel = globalVariablesModel;
    }
    
    public void updateGlobalVariablesModel(List<Element> inputs) {
        Set<GlobalVariable> variables = new HashSet<>();  
        List<Element> filteredInpts = RampsUtil.filterGlobalVarPopupInputs(inputs);
        if(filteredInpts != null){
        for (Element inputEle : filteredInpts) {
            if (inputEle.getOption(Element.DEFAULT) != null ) {
                
                GlobalVariable variable = new GlobalVariable(inputEle.getName(),
                        inputEle.getOption(Element.DEFAULT).getParams().get(0).getName());
                
                StringBuilder nameToPopulate = new StringBuilder();
                nameToPopulate.append(Constants.GLOBAL_VAR_PREFIX).append(Constants.GLOBAL).append("|").append(inputEle.getName());
                variable.setNameToPopulate(nameToPopulate.toString());
                
                variables.add(variable);
            }
            
            }
        }        
        this.globalVariablesModel.clear();
        this.globalVariablesModel.addAll(variables);
    }

    public boolean isDatasourceValidated() {
        return datasourceValidated;
    }

    public void setDatasourceValidated(boolean datasourceValidated) {
        this.datasourceValidated = datasourceValidated;
    }

    public boolean isNotifyUser() {
        return notifyUser;
    }

    public void setNotifyUser(boolean notifyUser) {
        this.notifyUser = notifyUser;
    }
}
