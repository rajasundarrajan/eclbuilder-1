package org.hpccsystems.dsp.ramps.controller;

import java.util.List;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.entity.Entity;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.ramps.component.renderer.PreviewRenderer;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class FileContentPreviewController extends SelectorComposer<Component> {

    private static final String ON_LOADING = "onLoading";
    private static final long serialVersionUID = 1L; 
    private static final Logger LOGGER = LoggerFactory.getLogger(FileContentPreviewController.class);
    
    @Wire
    private Label fileName;
    
    @Wire
    private Label fileSize;
    
    @Wire
    private Label recordCount;
    @Wire
    private Grid contentlist;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        Project project = (Project)Executions.getCurrent().getArg().get(Constants.PROJECT);
        String logicalFileName = (String)Executions.getCurrent().getArg().get(Constants.FILE);
        
        comp.addEventListener(ON_LOADING, (SerializableEventListener<? extends Event>)event -> {
            try {
                loadPage(project, logicalFileName);
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                Clients.showNotification("Loading data failed", Clients.NOTIFICATION_TYPE_ERROR, comp, Constants.POSITION_TOP_CENTER, 3000, true);
            }
            Clients.clearBusy(comp);
        });
        
        Clients.showBusy(comp, "Loading metadata");
        Events.echoEvent(ON_LOADING, comp, null);
    }

    private void loadPage(Project project, String logicalFileName) throws HPCCException {
        DFUFileDetailInfo dfuFileDetail;
        try {
            dfuFileDetail = ((HPCCService)SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileDetail(logicalFileName, project.getHpccConnection(), project.getClusterConfig().getThorCluster());
        } catch (HPCCException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(Labels.getLabel("unableToFetchFileInfo"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
         fileSize.setValue(dfuFileDetail.getFilesize()); 
         fileName.setValue(dfuFileDetail.getName());
         recordCount.setValue( dfuFileDetail.getRecordCount());
         
         
        ListModelList<Entity> model = new ListModelList<Entity>();
        List<Entity> contents = ((HPCCService)SpringUtil.getBean(Constants.HPCC_SERVICE)).getFileContents(dfuFileDetail.getName(), project.getHpccConnection(), project.getClusterConfig().getThorCluster(), 100);
                  
        PreviewRenderer previewRenderer = new PreviewRenderer();
        previewRenderer.setColumnsInGrid(contents.iterator().next().getChildren(), contentlist, false);
        contentlist.setRowRenderer(previewRenderer);
        contentlist.setModel(model);
        model.addAll(contents);
    }    
    
}
