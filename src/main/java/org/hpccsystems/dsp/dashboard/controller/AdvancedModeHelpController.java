package org.hpccsystems.dsp.dashboard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Window;

public class AdvancedModeHelpController extends SelectorComposer<Component> {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedModeController.class);
    private static final String VERSION = "0.1";
    
    @Wire
    private Button closeBtn;
    @Wire
    private Window advancedModeHelpModal;
    
    @Listen("onClick = #closeBtn")
    public void closeModal() {
        advancedModeHelpModal.detach();
    }

}
