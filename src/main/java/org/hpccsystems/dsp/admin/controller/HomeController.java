package org.hpccsystems.dsp.admin.controller;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.usergroupservice.Group;
import org.hpccsystems.usergroupservice.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Include;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class HomeController extends SelectorComposer<Component> {

    private static final String COMPOSITION_SERVICE = "compositionService";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @Wire
    private Rows mbsUserRows;
    @Wire
    private Grid mbsUserGrid;

    @Wire
    private Include processInclude;

    @Wire
    private Include reposInclude;

    @Wire
    private Include uiFeaturesInclude;

    @Wire
    private Include userLogInclude;

    @Wire
    private Include clusterInclude;

    @Wire
    private Include permissionInclude;

    @Wire
    private Include appLogInclude;

    @Wire
    private Include persInclude;

    @Wire
    private Include webAppInclude;

    @Wire
    private Include dashboardUIfeatures;

    @Wire
    private Tab persTab;

    @Wire
    private Include dependenciesInclude;

    @Wire
    private Button migratebtn;
    @Wire
    private Label migrationStatus;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadMBSUsers();
        if (!((CompositionService) SpringUtil.getBean(COMPOSITION_SERVICE)).isPersPermissionsAvailable()) {
            persTab.setSelected(true);
            perPermission();
        }
        showMigrationStatus();
    }

    @Listen("onSelect = #mbsTab")
    public void loadMBSUsers() {
        RowRenderer<org.hpccsystems.usergroupservice.User> userRowRenderer = new RowRenderer<User>() {

            @Override
            public void render(Row row, User user, int index) throws Exception {
                Label userLabel = new Label(user.getUserName().toLowerCase());
                Collection<Group> userGroups = user.getGroups();
                List<String> groupnames = userGroups.stream().map(Group -> Group.getMachineName().toLowerCase()).collect(Collectors.toList());
                Label groupLabel = creategroupLabel(groupnames);
                row.appendChild(userLabel);
                row.appendChild(groupLabel);
            }
        };
        try {
            // To avoid fetching MBS users, every time selecting the MBS tab
            if (mbsUserRows.getChildren().isEmpty()) {
                Collection<org.hpccsystems.usergroupservice.User> users = ((UserService) SpringUtil.getBean("userService")).getAllUsers();
                mbsUserGrid.setModel(new ListModelList<org.hpccsystems.usergroupservice.User>(users));
                mbsUserGrid.setRowRenderer(userRowRenderer);
            }
        } catch (AuthenticationException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    protected Label creategroupLabel(List<String> groupnames) {
        if (!groupnames.isEmpty()) {
            final StringBuilder groupNameBuffer = new StringBuilder();
            groupnames.stream().forEach(name -> groupNameBuffer.append(name).append(" , "));
            return new Label(groupNameBuffer.substring(0, groupNameBuffer.length() - 3));
        }
        return new Label();
    }

    @Listen("onSelect = #dependenciesTab")
    public void loadDependencies() {
        dependenciesInclude.setSrc("admin/dsp/dependencies.zul");
    }

    @Listen("onSelect = #processTab")
    public void loadProcesses() {
        processInclude.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.GLOBAL);
        processInclude.setSrc("ramps/process/content.zul");
    }

    @Listen("onSelect = #repoTab")
    public void loadRepos() {
        reposInclude.setSrc("/admin/dsp/repository.zul");
    }

    @Listen("onSelect = #userLogTab")
    public void userLog() {
        userLogInclude.setSrc("admin/dsp/userlog.zul");
    }

    @Listen("onSelect = #clustersTab")
    public void clusters() {
        clusterInclude.setSrc("admin/dsp/clusters.zul");
    }

    @Listen("onSelect = #appLogTab")
    public void appLog() {
        appLogInclude.setSrc("admin/dsp/applog.zul");
    }

    @Listen("onSelect = #permissionTab")
    public void loadGroupPermission() {
        permissionInclude.setSrc("admin/ramps/grouppermission.zul");
    }

    @Listen("onSelect = #rampsTab")
    public void uiFeatures() {
        uiFeaturesInclude.setSrc("admin/ramps/uifeatures.zul");
    }

    @Listen("onSelect = #persTab")
    public void perPermission() {
        persInclude.setSrc("admin/dsp/perspermission.zul");
    }

    @Listen("onSelect = #webappTab")
    public void loadWebAppSettings() {
        webAppInclude.setSrc("admin/dsp/webapp_settings.zul");
    }

    @Listen("onSelect = #dashboardTab")
    public void loadDashboardUifeatures() {
        dashboardUIfeatures.setSrc("/admin/dashboard/uifeatures.zul");
    }

    private void updateMigrationStatus() {
        CompositionService service = (CompositionService) SpringUtil.getBean(COMPOSITION_SERVICE);
        String status = service.migrateDermatology();
        migrationStatus.setValue(status);
        migratebtn.setIconSclass("fa fa-recycle");
        migratebtn.setLabel(Labels.getLabel("migrate"));
        migratebtn.setSclass("refresh-btn");
    }

    @Listen("onSelect = #eclBuilder")
    public void loadECLBuilderUifeatures() {
        dashboardUIfeatures.setSrc("/admin/eclBuilder/uifeatures.zul");
    }

    @Listen("onClick = #migratebtn")
    public void migrateDermatology() {
        migratebtn.setIconSclass("fa fa-spinner fa-pulse");
        migratebtn.setSclass("refreshing-btn");
        migratebtn.setLabel("Migrating Dermatology");
        updateMigrationStatus();
    }

    public void showMigrationStatus() {
        CompositionService service = (CompositionService) SpringUtil.getBean(COMPOSITION_SERVICE);
        boolean migrationPending = service.isMigrationPending();

        if (migrationPending) {
            migrationStatus.setValue(Labels.getLabel("migrationPending"));
            migratebtn.setDisabled(false);
        } else {
            migratebtn.setDisabled(true);
            migrationStatus.setValue(Labels.getLabel("migrationComplete"));
        }
    }

}
