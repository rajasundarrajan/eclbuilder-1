package org.hpccsystems.dsp.admin.controller;

import java.util.Set;

import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.resource.ResourceElement;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Column;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModelList;

public class DependencyGridController extends SelectorComposer<Grid>{

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyGridController.class);
    
    @Wire
    private Column urlColumn;
    
    @Override
    public void doAfterCompose(Grid grid) throws Exception {
        super.doAfterCompose(grid);
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean("authenticationService");
        
        String resourceType = (String) Executions.getCurrent().getAttribute("argument");
        LOGGER.debug("resource type - {}", resourceType);
        
        if(ResourceElement.LOCALFILE_TYPE.equals(resourceType)) {
            urlColumn.setVisible(false);
        }
        
        ListModelList<ResourceElement> elements = new ListModelList<>();
        Set<ResourceElement> eclResources = HipieSingleton.getHipie().getResourcesByType(authenticationService.getCurrentUser().getId(),
                resourceType);
        eclResources.forEach(re -> elements.add(re));
        
        grid.setModel(elements);
    }
    
    public String getFile() {
        return Element.FILENAME;
    }
    
    public String getURL() {
        return ResourceElement.URL;
    }
}
