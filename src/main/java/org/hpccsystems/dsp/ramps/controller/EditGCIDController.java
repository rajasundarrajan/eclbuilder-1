package org.hpccsystems.dsp.ramps.controller;

import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.Company;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zul.Include;
import org.zkoss.zul.Window;

public class EditGCIDController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(EditGCIDController.class);
    
    
    private TabData tabData = null;
    private Include include = null;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        tabData = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        include = (Include)tabData.getProjectDetailComponent().getParent();
        LOGGER.debug("tabData -->{}", ((Include)tabData.getProjectDetailComponent().getParent()).getSrc());
        
        LOGGER.debug("tabData getUsedatasetFormHolder-->{}",include.getSrc());
        //taking the original composition as this flow opens/reloads the project again in Edit mode
        if(tabData.getFlow() == Flow.EDIT){
            tabData.setComposition(tabData.getOriginalComposition());
        }
        
        this.getSelf().addEventListener(Constants.EVENTS.ON_CONFIRM_GCID, (SerializableEventListener<? extends Event>)event ->{  
            
            Company selectedCompany = (Company) event.getData();
            boolean isNotOldGCID = !tabData.getProject().getReferenceId().equals(selectedCompany.getGcId().toString());
            LOGGER.debug("is not old GCID------->{}", isNotOldGCID);
            LOGGER.debug("is not old GCID------>{}", tabData.getProject().getReferenceId());
            LOGGER.debug("is not old GCID-------->{}", selectedCompany.getGcId());
            if (isNotOldGCID && tabData.getProject().isBatchTemplate()
                    && !RampsUtil.setGCIDcompliance(tabData.getComposition(), this.getSelf(), selectedCompany.getGcId().toString())) {
                return;
            }
            ((Window) EditGCIDController.this.getSelf()).detach();
            if (isNotOldGCID) {
                reloadComposition(selectedCompany);
            }
            
        });
    }

    private void reloadComposition(Company selectedCompany) {   
        
            tabData.getProject().setReferenceId(String.valueOf(selectedCompany.getGcId()));
            RampsUtil.resetPlugingProperties(tabData.getComposition(),tabData.getProject());
            
            Element referenceElement= RampsUtil.getReferenceIDElement(tabData.getComposition());
            RampsUtil.updateElement(referenceElement, tabData.getProject().getReferenceId());
            
            String sourceUrl = include.getSrc();
            include.setSrc(null);
            include.setDynamicProperty(Constants.TAB_DATA, tabData);
            include.setSrc(sourceUrl);
           
    }

}
