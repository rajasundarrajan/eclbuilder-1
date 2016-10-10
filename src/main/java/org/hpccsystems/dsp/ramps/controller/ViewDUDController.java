package org.hpccsystems.dsp.ramps.controller;

import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.Constants;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Label;

public class ViewDUDController extends SelectorComposer<Component>{

    private static final long serialVersionUID = 1L;
    private static final String DUD = ".dud";
    
    @Wire
    private Label dudContent;
    
    @Wire
    private Caption captionDUD;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        ContractInstance data = (ContractInstance) Executions.getCurrent().getArg().get(Constants.CONTRACT_INSTANCE);
        captionDUD.setLabel(data.getContract().getName()+DUD);
        dudContent.setValue(data.getContract().toString());
    }

}
