package org.hpccsystems.dsp.admin.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class PerspectivePermissionController extends SelectorComposer<Component> {

    private static final String USER_SERVICE = "userService";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PerspectivePermissionController.class);


    private final ListModelList<Entry<String, Permission>> perspectiveModel = new ListModelList<Entry<String, Permission>>();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        UserService userService = (UserService) SpringUtil.getBean(USER_SERVICE);
        Collection<Group> groups = userService.getAllGroups();
        Map<String, Permission> persPermissions;
        try {
            persPermissions = userService.getPersPermissions(groups);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        for (Entry<String, Permission> permissionEntry : persPermissions.entrySet()) {
            LOGGER.debug("permissionEntry--->{}", permissionEntry);
            getPerspectiveModel().add(permissionEntry);
        }
        LOGGER.debug("Loaded permissions");
    }

    @Listen("onClick = #save")
    public void onSave() {
        try {
            Map<String, Permission> finalMap = new HashMap<String, Permission>();
            for (Entry<String, Permission> pers : perspectiveModel){
                finalMap.put(pers.getKey(), pers.getValue());
            }
            ((UserService) SpringUtil.getBean(USER_SERVICE)).savePersPermission(finalMap);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            Clients.showNotification("Unable to save permission\n"+e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_MIDDLE_CENTER, 5000, true);
            return;
        }
        Clients.showNotification(Labels.getLabel("savePerspective"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(),
                Constants.POSITION_MIDDLE_CENTER, 5000, true);
    }

    @Listen("onRampsCheck = #perpectiveGrid")
    public void onSelectRamps(ForwardEvent forwardEvent) {

        Event event = forwardEvent.getOrigin();
        CheckEvent checkEvent = (CheckEvent) event;
        Row row = (Row) event.getTarget().getParent();
        Permission persPermission = row.getValue();
        persPermission.setViewRamps(checkEvent.isChecked());

    }

    @Listen("onDashboardCheck = #perpectiveGrid")
    public void onSelectDashboard(ForwardEvent forwardEvent) {
        Event event = forwardEvent.getOrigin();
        CheckEvent checkEvent = (CheckEvent) event;
        Row row = (Row) event.getTarget().getParent();
        Permission persPermission = row.getValue();
        persPermission.setViewDashboard(checkEvent.isChecked());
    }

    public ListModelList<Entry<String, Permission>> getPerspectiveModel() {
        return perspectiveModel;
    }

}
