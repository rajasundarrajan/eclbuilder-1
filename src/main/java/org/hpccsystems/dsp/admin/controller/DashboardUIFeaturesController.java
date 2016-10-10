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
public class DashboardUIFeaturesController extends SelectorComposer<Component> {

    private static final String USER_SERVICE = "userService";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardUIFeaturesController.class);

    

    @Wire
    private Grid dashboardGroupsGrid;

    ListModelList<String> availableDashboardGroups = new ListModelList<String>();
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

        List<String> dashboardSelection = new ArrayList<String>();
        for (Map.Entry<String, Permission> dashboardGroupName : allGroupsUIPermissions.entrySet()) {
            availableDashboardGroups.add(dashboardGroupName.getKey());
            if (dashboardGroupName.getValue().getDashboardPermission().getUiPermission().canViewList()) {
                dashboardSelection.add(dashboardGroupName.getKey());
            }

        }
        availableDashboardGroups.setMultiple(true);
        availableDashboardGroups.setSelection(dashboardSelection);
        dashboardGroupsGrid.setModel(availableDashboardGroups);
        dashboardGroupsGrid.setRowRenderer(new RowRenderer<String>() {

            @Override
            public void render(Row row, String data, int index) throws Exception {
                creatingGroupsGrid(row, data, false);
            }

        });

    }

    private void creatingGroupsGrid(Row row, String data, boolean isramps) {
        UIPermission currentUI = allGroupsUIPermissions.get(data).getDashboardPermission().getUiPermission();
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
        Checkbox advancedModeCheckbox = new Checkbox();
        advancedModeCheckbox.setChecked(currentUI.isAllowedAdvancedMode());
        row.appendChild(advancedModeCheckbox);
        Checkbox convertToCompCheckbox = new Checkbox();
        convertToCompCheckbox.setChecked(currentUI.isAllowedConvertToComp());
        row.appendChild(convertToCompCheckbox);
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
        advancedModeCheckbox.addEventListener(Events.ON_CHECK, event -> currentUI.setAllowedAdvancedMode(advancedModeCheckbox.isChecked()));
        convertToCompCheckbox.addEventListener(Events.ON_CHECK, event -> currentUI.setAllowedConvertToComp(convertToCompCheckbox.isChecked()));
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

    @Listen("onClick = #dashboardSave")
    public void onDashboardSave() {

        for (Map.Entry<String, Permission> dashboardGroup : allGroupsUIPermissions.entrySet()) {
            if (dashboardGroup.getValue().getDashboardPermission().getUiPermission().canViewGrid()
                    && !dashboardGroup.getValue().getDashboardPermission().getUiPermission().canViewList()) {
                dashboardGroup.getValue().getDashboardPermission().getUiPermission().setDefaultView("Grid");
            }
            if (!dashboardGroup.getValue().getDashboardPermission().getUiPermission().canViewGrid()
                    && dashboardGroup.getValue().getDashboardPermission().getUiPermission().canViewList()) {
                dashboardGroup.getValue().getDashboardPermission().getUiPermission().setDefaultView("List");
            }

            try {
                ((UserService) SpringUtil.getBean(USER_SERVICE)).saveDashboardUserGroups(dashboardGroup.getKey(), dashboardGroup.getValue());
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000,
                        true);
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