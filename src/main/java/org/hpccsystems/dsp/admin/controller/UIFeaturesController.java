package org.hpccsystems.dsp.admin.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.entity.Permission;
import org.hpccsystems.dsp.entity.UIPermission;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class UIFeaturesController extends SelectorComposer<Component> {

    private static final String USER_SERVICE = "userService";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UIFeaturesController.class);


    @Wire
    private Grid rampsGroupsGrid;

    ListModelList<String> availableRampsGroups = new ListModelList<String>();
    Map<String, Permission> allGroupsUIPermissions;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        
        Collection<Group> groups = ((UserService) SpringUtil.getBean(USER_SERVICE)).getAllGroups();

        try {
            allGroupsUIPermissions = ((UserService) SpringUtil.getBean(USER_SERVICE)).getGroupPermissions(groups);
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }

        List<String> rampsSelection = new ArrayList<String>();
        for (Map.Entry<String, Permission> rampsGroupName : allGroupsUIPermissions.entrySet()) {
            availableRampsGroups.add(rampsGroupName.getKey());
            if (rampsGroupName.getValue().getRampsPermission().getUiPermission().canViewList()) {
                rampsSelection.add(rampsGroupName.getKey());
            }

        }
        availableRampsGroups.setMultiple(true);
        availableRampsGroups.setSelection(rampsSelection);
        rampsGroupsGrid.setModel(availableRampsGroups);
        rampsGroupsGrid.setRowRenderer(new RowRenderer<String>() {

            @Override
            public void render(Row row, String data, int index) throws Exception {
                creatingGroupsGrid(row, data);
            }

        });
    }

    private void creatingGroupsGrid(Row row, String data) {
        UIPermission currentUI = allGroupsUIPermissions.get(data).getRampsPermission().getUiPermission();
        Label name = new Label(data);
        name.setParent(row);
        Checkbox listCheckbox = new Checkbox();
        listCheckbox.setChecked(currentUI.canViewList());
        row.appendChild(listCheckbox);
        Checkbox gridCheckbox = new Checkbox();
        gridCheckbox.setChecked(currentUI.canViewGrid());
        row.appendChild(gridCheckbox);
        Combobox comb = new Combobox();
        ListModelList<String> views = new ListModelList<String>();
        views.add("List");
        views.add("Grid");
        comb.setModel(views);
        comb.setReadonly(true);
        comb.setDisabled(true);
        comb.setHflex("1");
        row.appendChild(comb);
        Checkbox companyIdCheckBox = new Checkbox();
        companyIdCheckBox.setChecked(currentUI.isCompanyIdMandatory());
        row.appendChild(companyIdCheckBox);
        Checkbox viewPluginCheckBox = new Checkbox();
        viewPluginCheckBox.setChecked(allGroupsUIPermissions.get(data).getRampsPermission().canViewPluginSource());
        row.appendChild(viewPluginCheckBox);

        Checkbox importFileCheckBox = new Checkbox();
        importFileCheckBox.setChecked(allGroupsUIPermissions.get(data).getRampsPermission().canImportFile());
        row.appendChild(importFileCheckBox);

        Checkbox keepEclCheckBox = new Checkbox();
        keepEclCheckBox.setChecked(allGroupsUIPermissions.get(data).getRampsPermission().isKeepECL());
        row.appendChild(keepEclCheckBox);

        selectingDefaultView(currentUI, comb, views);
        listCheckbox.addEventListener(Events.ON_CHECK, event -> {
            currentUI.setViewList(listCheckbox.isChecked());
            selectingDefaultView(currentUI, comb, views);
        });
        gridCheckbox.addEventListener(Events.ON_CHECK, event -> {
            currentUI.setViewGrid(gridCheckbox.isChecked());
            selectingDefaultView(currentUI, comb, views);
        });
        companyIdCheckBox.addEventListener(Events.ON_CHECK, event -> currentUI.setMandateCompanyId(companyIdCheckBox.isChecked()));
        viewPluginCheckBox.addEventListener(Events.ON_CHECK,
                  event -> allGroupsUIPermissions.get(data).getRampsPermission().setViewPluginSource(viewPluginCheckBox.isChecked()));
        importFileCheckBox.addEventListener(Events.ON_CHECK,
                event -> allGroupsUIPermissions.get(data).getRampsPermission().setImportFile(importFileCheckBox.isChecked()));
        keepEclCheckBox.addEventListener(Events.ON_CHECK,
                event -> allGroupsUIPermissions.get(data).getRampsPermission().setKeepECL(keepEclCheckBox.isChecked()));
    }

    private void selectingDefaultView(UIPermission currentUI, Combobox combobox, ListModelList<String> views) {
        if (!currentUI.canViewGrid() && currentUI.canViewList()) {
            combobox.setDisabled(true);
            views.addToSelection("List");
            currentUI.setDefaultView(null);
        } else if (currentUI.canViewGrid() && !currentUI.canViewList()) {
            combobox.setDisabled(true);
            views.addToSelection("Grid");
            currentUI.setDefaultView(null);
        } else if (currentUI.canViewGrid() && currentUI.canViewList()) {
            combobox.setDisabled(false);
            views.clearSelection();
            if (currentUI.getDefaultView() != null) {
                String[] selectedViews = { currentUI.getDefaultView() };
                views.setSelection(Arrays.asList(selectedViews));
            }
            combobox.addEventListener(Events.ON_SELECT, new SerializableEventListener<Event>() {
                private static final long serialVersionUID = 1L;

                @Override
                public void onEvent(Event event) throws Exception {
                    if (((Comboitem) combobox.getSelectedItem()) != null) {
                        currentUI.setDefaultView(views.getSelection().iterator().next().toString());
                    }
                }
            });
        } else {
            views.clearSelection();
            combobox.setDisabled(true);
            currentUI.setDefaultView(null);
        }
    }

    @Listen("onClick = #rampsSave")
    public void onRampsSave() {

        LOGGER.debug("all group permissions --->{}", allGroupsUIPermissions);
        for (Map.Entry<String, Permission> rampsGroup : allGroupsUIPermissions.entrySet()) {
            if (rampsGroup.getValue().getRampsPermission().getUiPermission().canViewGrid()
                    && !rampsGroup.getValue().getRampsPermission().getUiPermission().canViewList()) {
                rampsGroup.getValue().getRampsPermission().getUiPermission().setDefaultView("Grid");
            }
            if (!rampsGroup.getValue().getRampsPermission().getUiPermission().canViewGrid()
                    && rampsGroup.getValue().getRampsPermission().getUiPermission().canViewList()) {
                rampsGroup.getValue().getRampsPermission().getUiPermission().setDefaultView("List");
            }

            try {
                ((UserService) SpringUtil.getBean(USER_SERVICE)).saveRampsUserGroups(rampsGroup.getKey(), rampsGroup.getValue());
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            } catch (Exception e) {
                LOGGER.error("Unable to save user groups -- >{}", e);
                Clients.showNotification(Labels.getLabel("groupViewPermissionFailed"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                        Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
        }
        Clients.showNotification(Labels.getLabel("groupViewPermissionUpdate"), Clients.NOTIFICATION_TYPE_INFO, getSelf().getParent(), Constants.POSITION_TOP_CENTER,
                5000, true);
    }

}