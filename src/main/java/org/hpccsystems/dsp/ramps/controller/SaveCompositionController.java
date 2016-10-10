package org.hpccsystems.dsp.ramps.controller;

import java.util.Date;
import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.CompositionServiceException;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.utils.HIPIEUtil;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class SaveCompositionController extends SelectorComposer<Window> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveCompositionController.class);
    private static final String CONTAINER = "container";
    private static final String HOME_TABBOX = "homeTabbox";

    private String action;
    private Composition composition;

    @Wire
    private Textbox compositionName;
    
    @Wire
    private Window saveCompositionWindow;
    @Wire
    private Label saveLabel;

    private Project project;
    private Project projectToClose;
    private TabData data;

    @Override
    public void doAfterCompose(Window win) throws Exception {
        super.doAfterCompose(win);
        action = (String) Executions.getCurrent().getArg().get(Constants.ACTION_SAVE);
        data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
        composition = data.getComposition();
        
        if (action.equals(Constants.ACTION.SAVE_AS)) {
            saveCompositionWindow.setTitle("Save as");
        } else if (action.equals(Constants.SAVE_AS_TEMPLATE)) {
            saveCompositionWindow.setTitle("Save as template");
            saveLabel.setValue(Labels.getLabel("templateName"));
        }

        this.getSelf().addEventListener(Events.ON_CLOSE, (SerializableEventListener<? extends Event>) event -> {
            Events.postEvent(EVENTS.ON_CLOSE_SAVE_AS_WINDOW, data.getProjectDetailComponent(), null);
            this.getSelf().detach();
        }
        );
        
        // Clone existing project
        project = data.getProject();
        projectToClose = project;
        project = project.clone();
    }

    @Listen("onClick = #save ; onOK= #saveCompositionWindow")
    public void save(Event event) {
        if (compositionName.getText().isEmpty()) {
            Clients.showNotification(Labels.getLabel("provideName"), Clients.NOTIFICATION_TYPE_ERROR, compositionName, Constants.POSITION_END_CENTER,
                    3000);
            return;
        }
        if (Character.isDigit(compositionName.getText().charAt(0))) {
            Clients.showNotification(Labels.getLabel("cmpNameAlphabetOnly"), Clients.NOTIFICATION_TYPE_ERROR, compositionName,
                    Constants.POSITION_END_AFTER, 3000);
            return;
        }

        String label = compositionName.getText();
        String previousLabel = composition.getLabel();
        String previousName = composition.getName();
        String escapedName;
        try {
            escapedName = HIPIEUtil.createCompositionName(label, false);
        } catch (HipieException e) {
            LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(e.getMessage(),Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000);
            return;
        }
        try {
            if (RampsUtil.isFileNameDuplicate(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser(),  label.trim())) {
                Clients.showNotification(Labels.getLabel("templateExists1").concat(label).concat(Labels.getLabel("templateExists2")),
                        Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000);
                return;
            }
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("isFileExists"),Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000);
            return;
        }
        
        try {
            composition = CompositionUtil.cloneComposition(composition, escapedName, label, ((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), false,
                    null, project.getDatasourceStatus());
        }catch (HipieException | RepoException e) {
            LOGGER.error("Exception-{}",e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR,
                    this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
            return;
        }
        project.setName(escapedName);
        project.setLabel(label);
        project.setType("RAMPS");
        project.setLastModifiedDate(new Date(composition.getLastModified()));

        try {
            saveAsAction(escapedName, previousLabel, previousName);
        } catch (HipieException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(),Clients.NOTIFICATION_TYPE_ERROR, this.getSelf(), Constants.POSITION_TOP_CENTER, 3000);
            return;
        }
        Events.postEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

    private void saveAsAction(String name, String previousLabel, String previousName) throws HipieException {
        
        if (action.equals(Constants.ACTION.SAVE_AS)) {
           
            Composition savedComposition = null;
            CompositionService cmpService = null;
            try {
                cmpService = ((CompositionService)SpringUtil.getBean(Constants.COMPOSITION_SERVICE));
                savedComposition = cmpService.saveNewCompositionOnHIPIE(project.getName(), composition);
                cmpService.saveNewCompositionOnDatabase(project, savedComposition);
                Clients.showNotification(Labels.getLabel("compSaved"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf().getParent().getParent()
                        .getParent(), Constants.POSITION_TOP_CENTER, 3000);
                
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            } catch (CompositionServiceException e) {
                LOGGER.error("ExCeption-{}",e);
                Clients.showNotification(Labels.getLabel("compositionSuccessfullySavedHPCCclusterConfigurationIsNotSaved"), Clients.NOTIFICATION_TYPE_WARNING,
                        this.getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
            }
            composition.setLabel(previousLabel);
            composition.setName(previousName);

            // Adding newly saved Project to project list
            Events.postEvent(EVENTS.ON_PROJECT_ADD, getSelf().getParent().getParent().getParent().getFellow(CONTAINER).getParent().getParent()
                    .getFellow(HOME_TABBOX), project);

                // reloading the tabpanel after save as
                TabData tabData = new TabData(project, Constants.Flow.EDIT,savedComposition);
                
                Events.postEvent(EVENTS.ON_CLOSE_OLD_PROJECT,
                        getSelf().getParent().getParent().getParent().getFellow(CONTAINER).getParent().getParent()
                                .getFellow(HOME_TABBOX), projectToClose);
                Events.postEvent(EVENTS.ON_OPEN_COMPOSITION,
                        getSelf().getParent().getParent().getParent().getFellow(CONTAINER).getParent().getParent()
                                .getFellow(HOME_TABBOX), tabData);

        } else if (action.equals(Constants.SAVE_AS_TEMPLATE)) {
            try {
                HipieSingleton.getHipie().saveCompositionAsTemplate(((AuthenticationService)SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(), composition, name);
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION,e);
                throw new HipieException("Could not save compostion", e);
            }
            Clients.showNotification(Labels.getLabel("savedAsTempalte"), Clients.NOTIFICATION_TYPE_INFO, this.getSelf().getParent().getParent()
                    .getParent(), Constants.POSITION_TOP_CENTER, 3000);
        }
    }
}
