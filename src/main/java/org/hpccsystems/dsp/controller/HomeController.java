package org.hpccsystems.dsp.controller;

import java.time.Instant;

import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Perspective;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.log.ApplicationAccess;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.SettingsService;
import org.hpccsystems.dsp.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.Include;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Script;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Window;
import  org.zkoss.zul.Vlayout;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class HomeController extends SelectorComposer<Component> {
    private static final String FA_FA_PIE_CHART = "fa fa-pie-chart";
    private static final String CONTENT_URL = "ramps/process/content.zul";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
    @Wire 
    private Vlayout homeWindow;
    
    @Wire
    private Tabbox prespectiveTabbox;

    @Wire
    private Tab rampsTab;
    @Wire
    private Tab dashboardTab;
    @Wire
    private Tab adminTab;

    @Wire
    private Tab processTab;
    @Wire
    private Tab processDetailTab;

    @Wire
    private Tab shareTab;

    @Wire
    private Popup settingsList;

    @Wire
    private Popup iconPopup;

    @Wire
    private Combobutton homeIcon;

    @Wire
    private Include rampsInclude;
    @Wire
    private Include dashboardInclude;
    @Wire
    private Include adminInclude;
    @Wire
    private Include processInclude;
    @Wire
    private Include processDetailInclude;
    @Wire
    private Include shareInclude;
    @Wire
    private Script requireJS;
    @Wire
    private Script hpccVizJS;
    @Wire
    private Script hpccVizCommonJS;
    @Wire
    private Script hpccVizBundleJS;
    @Wire
    private Include dashboardProcessInclude;

    private Tab activeTab;

    private User user;
    private boolean canViewCompositionMenu;
    private boolean isSharedDashboardView;
    private static final String CONNECTION_CHANGED_OR_NULL = "HPCC connection is null or it is being changed";

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        AuthenticationService authService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        user = authService.getCurrentUser();
        setCanViewCompositionMenu(user.canCreate() ? user.canAccessRAMPS() || user.canAccessDashboard() : false);
        setSharedDashboardView(authService.hasRedirectURL() && authService.getRedirectParams() != null);
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        clearSession();

        // Includes the minified and non minified version js files based on the
        // devmode flag.
        SettingsService settingsService = (SettingsService) SpringUtil.getBean("settingsService");
        
        if (settingsService.isDevEnabled()) {
            hpccVizCommonJS.setSrc(null);
//            requireJS.setSrc("js/Visualization/node_modules/requirejs/require.js");
            hpccVizJS.setSrc(null);
            hpccVizBundleJS.setSrc(null);
            Clients.evalJavaScript("setNonminifiedConfig()");
        } else {
            hpccVizCommonJS.setSrc("js/Visualization/dist-amd/hpcc-viz-common.js");
            requireJS.setSrc(null);
            hpccVizJS.setSrc("js/Visualization/dist-amd/hpcc-viz.js");
            hpccVizBundleJS.setSrc("js/Visualization/dist-amd/hpcc-bundles.js");
            Clients.evalJavaScript("setMinifiedConfig()");
        }

        if (isSharedDashboardView()) {
            String params = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getRedirectParams();
            LOGGER.debug("Params - {}", params);
            String compId = params;
            loadSharedDashboard(compId);
            ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).clearRedirectURL();
            return;
        }
        comp.getDesktop().setAttribute(Constants.HOME_COMPONENT, comp);

        comp.addEventListener(EVENTS.ON_OPEN_PROCESS_INFO, event -> loadProcessDetail((Process) event.getData()));
        comp.addEventListener(EVENTS.ON_CLOSE_PROCESS_INFO, event -> hideProcessDetail());
       comp.addEventListener(EVENTS.ON_OPEN_RAMPS_PERSPECTIVE, event -> openRampsPerspective(event.getData())); 
        
        Perspective lastPersp = ((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).getLastViewPerspective(user);

        loadPerspective(lastPersp);

    }

    private void loadPerspective(Perspective lastPersp) {
        if (lastPersp == null) {
            if (user.canAccessRAMPS()) {
                loadRampsAndSetUI();
            } else if (user.canAccessDashboard()) {
                loadDashboardAndSetUI();
            } else if (user.isGlobalAdmin()) {
                loadAdminAndSetUI();
            } else {
                loadDefaultPrespective();
            }
        } else if (Perspective.ADMINISTRATION == lastPersp) {
            loadAdminAndSetUI();
        } else if (Perspective.DASHBOARD == lastPersp) {
            loadDashboardAndSetUI();
        } else if (Perspective.RAMPS == lastPersp) {
            loadRampsAndSetUI();
        }
    }

    private void clearSession() {
        Sessions.getCurrent().setAttribute(Constants.OPEN_PROJECT_LABELS, null);
        Sessions.getCurrent().setAttribute(Constants.OPEN_DASHBOARD_LABELS, null);
    }

    private void hideProcessDetail() {
        activeTab.setSelected(true);
        activeTab = null;
    }

    private void openRampsPerspective(Object comp) {
       loadRampsAndSetUI();
       Events.postEvent(Constants.EVENTS.ON_LOAD_CLONED_COMPOSITION, rampsInclude.getFirstChild(), comp);  
    }

    private void loadSharedDashboard(String compId) {
        shareTab.setSelected(true);
        shareInclude.setDynamicProperty(Constants.COMPOSITION, compId);
        shareInclude.setSrc("dashboard/dashboardOutputs.zul");
    }

    private void loadProcessDetail(Process process) {
        processDetailInclude.setSrc(null);
        activeTab = prespectiveTabbox.getSelectedTab();
        processDetailTab.setSelected(true);
        processDetailInclude.setDynamicProperty(Constants.PROCESS, process);
        try {
            processDetailInclude.setDynamicProperty(Constants.PROJECT, new Project(process.getCompositionInstance()));
        } catch (HPCCException e) {
            LOGGER.error(CONNECTION_CHANGED_OR_NULL, e);
        }
        processDetailInclude.setSrc("/ramps/process/more_info.zul");
    }

    @Listen("onSelectAbout = #helpList")
    public void createHelpList(ForwardEvent forwardEvent) {
        Window window = (Window) Executions.createComponents("/about.zul", null, null);
        window.doModal();
    }

    @Listen("onClick = #ramps, #rampsVlayout")
    public void loadRAMPSPrespective() {
        loadRampsAndSetUI();
        ((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).setLastViewPerspective(user.getId(), Perspective.RAMPS.name());
    }

    private void loadRampsAndSetUI() {
        homeIcon.setIconSclass("fa fa-cubes");
        homeIcon.setSclass("homeIcon ramps");
        rampsTab.setSelected(true);
        rampsInclude.setSrc("ramps/home.zul");
        iconPopup.close();
    }

    public void loadDefaultPrespective() {
        homeIcon.setVisible(false);
        rampsTab.setSelected(true);
        rampsInclude.setSrc("ramps/noperspective.zul");
        iconPopup.close();
    }

    @Listen("onClick = #dashboard, #dashboardVlayout")
    public void loadDashboardPrespective() {
        loadDashboardAndSetUI();
        ((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).setLastViewPerspective(user.getId(), Perspective.DASHBOARD.name());
    }

    private void loadDashboardAndSetUI() {
        homeIcon.setIconSclass(FA_FA_PIE_CHART);
        homeIcon.setSclass("homeIcon dashboard");
        dashboardTab.setSelected(true);
        dashboardInclude.setSrc("dashboard/home.zul");
        iconPopup.close();
    }
    @Listen("onClick = #eclBuilder, #eclBuilderVlayout")
    public void loadECLBuilderPrespective() {
        loadECLBuilderAndSetUI();
        ((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).setLastViewPerspective(user.getId(), Perspective.DASHBOARD.name());
    }
    
    private void loadECLBuilderAndSetUI() {
    	homeIcon.setIconSclass("fa fa-cog");
    	homeIcon.setSclass("homeIcon admin");
        dashboardTab.setSelected(true);
        dashboardInclude.setSrc("eclBuilder/home.zul");
        iconPopup.close();
    }

    @Listen("onClick = #viewProcess")
    public void viewProcess() {
        settingsList.close();
        homeIcon.setIconSclass("fa fa-tachometer");
        homeIcon.setSclass("homeIcon Process");
        processTab.setSelected(true);
        if (processInclude != null) {
            processInclude.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.USER_COMPOSITIONS);
            processInclude.setSrc(CONTENT_URL);
        }
        if (dashboardProcessInclude != null) {
            dashboardProcessInclude.setDynamicProperty(Constants.PROCESS_PAGE_TYPE, ProcessRetriver.ProcessType.USER_DASHBOARDS);
            dashboardProcessInclude.setSrc(CONTENT_URL);
        }
    }

    @Listen("onClick = #admin, #adminVlayout")
    public void loadAdminPrespective() {
        loadAdminAndSetUI();
        ((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).setLastViewPerspective(user.getId(), Perspective.ADMINISTRATION.name());
    }

    private void loadAdminAndSetUI() {
        homeIcon.setIconSclass("fa fa-cog");
        homeIcon.setSclass("homeIcon admin");
        adminTab.setSelected(true);
        adminInclude.setSrc("admin/home.zul");
        iconPopup.close();
    }

    @Listen("onClick = #logOutLink")
    public void logout() {
        long startTime = Instant.now().toEpochMilli();
        LOGGER.info("Logging out user");
        Sessions.getCurrent().invalidate();
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger) SpringUtil.getBean(Constants.DB_LOGGER)).log(new ApplicationAccess(ApplicationAccess.ACTION_LOGOUT, startTime, "Success"));
        }
        Executions.sendRedirect("/");
    }

    @Listen("onClick = #newRAMPSComposition")
    public void createRAMPSComposition() {
        loadRAMPSPrespective();
        Events.postEvent(Constants.EVENTS.ON_CREATE_COMPOSITION, rampsInclude.getFirstChild(), null);
    }

    @Listen("onClick = #newDashbaord")
    public void createDashboard() {
        loadDashboardPrespective();
        Events.postEvent(Constants.EVENTS.ON_CREATE_COMPOSITION, dashboardInclude.getFirstChild(), null);
    }

    public boolean getCanViewCompositionMenu() {
        return canViewCompositionMenu;
    }

    public void setCanViewCompositionMenu(boolean canViewCompositionMenu) {
        this.canViewCompositionMenu = canViewCompositionMenu;
    }

    public boolean isSharedDashboardView() {
        return isSharedDashboardView;
    }

    public void setSharedDashboardView(boolean isSharedDashboardView) {
        this.isSharedDashboardView = isSharedDashboardView;
    }
}
