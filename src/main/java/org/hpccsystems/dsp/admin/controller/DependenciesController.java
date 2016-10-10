package org.hpccsystems.dsp.admin.controller;

import org.hpcc.HIPIE.dude.resource.ResourceElement;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;

public class DependenciesController extends SelectorComposer<Component>{

    private static final long serialVersionUID = 1L;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
    }
    
    public String getEclType() {
        return ResourceElement.ECL_TYPE;
    }
    
    public String getIndicesType() {
        return ResourceElement.INDEX_TYPE;
    }
    
    public String getLogicalfileType() {
        return ResourceElement.LOGICALFILE_TYPE;
    }
    
    public String getSuperfileType() {
        return ResourceElement.SUPERFILE_TYPE;
    }
    
    public String getLocalfileType() {
        return ResourceElement.LOCALFILE_TYPE;
    }
}
