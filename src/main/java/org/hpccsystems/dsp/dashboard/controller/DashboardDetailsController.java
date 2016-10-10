package org.hpccsystems.dsp.dashboard.controller;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.hpcc.HIPIE.ContractInstance;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Include;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class DashboardDetailsController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardDetailsController.class);

    @Wire
    private Include dashboardDesignInclude;

    @Wire
    private Include dashboardOutputsInclude;

    @Wire
    private Tab dashboardDesign;

    @Wire
    private Tab dashboardOutputs;

    private DashboardConfig dashboardConfig;

    private static final String ON_LOADING = "onLoading";

    private EventListener<Event> loadingListener = event -> loadPage();

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        dashboardConfig.setViewEditTabbox((Tabbox) this.getSelf());
        if (dashboardConfig.getFlow() == Flow.EDIT || dashboardConfig.getFlow() == Flow.NEW || dashboardConfig.getFlow() == Flow.CLONE ) {
            comp.addEventListener(ON_LOADING, loadingListener);
            Events.echoEvent(ON_LOADING, comp, null);
        } else if (dashboardConfig.getFlow() == Flow.VIEW) {
            loadOutputTab();
        }

        this.getSelf().addEventListener(Dashboard.EVENTS.ON_VIEW_DASHBOARD_OUTPUTS, event -> {
                dashboardConfig = (DashboardConfig) event.getData();
                // Change this line to set most recent instance
                loadOutputTab();
            });
        this.getSelf().addEventListener(Dashboard.EVENTS.ON_EDITING_DASHBOARD, event -> {
            dashboardConfig.setFlow(Flow.EDIT);
            dashboardOutputs.setSelected(false);
            dashboardDesign.setSelected(true);
            if (dashboardDesignInclude.getSrc() == null) {
                loadPage();
            }
        });
    }

    private void loadPage() {
        LOGGER.debug("Creating Widgets");

        Map<String, ContractInstance> contractInstances = dashboardConfig.getComposition().getContractInstances();

        try {
            // Constructing Dashboard Config object
            for (Entry<String, ContractInstance> entry : contractInstances.entrySet()) {
                // Creating Widgets
                if (Dashboard.DASHBOARD_REPO.equals(entry.getValue().getContract().getRepositoryName())) {
                    ContractInstance contractInstance = entry.getValue();
                    if(CollectionUtils.isNotEmpty(dashboardConfig.getDashboard().getWidgets())){
                        dashboardConfig.getDashboard().getWidgets().clear();
                    }
                  
                        // Identify Scored search widget
                        if (StringUtils.endsWith(contractInstance.getContract().getName(), Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER)) {
                            dashboardConfig.getDashboard().addWidgets(CompositionUtil.extractScoredSearchWidget(contractInstance));
                        } else {
                        setQueryAndClone(contractInstance);
                        }
                        break;

                }

            }

            LOGGER.debug("Created Widgets. Dashboard config - {}", dashboardConfig);

            includeDashboard();
            Clients.clearBusy(DashboardDetailsController.this.getSelf());
        } catch (DatabaseException e) {
            LOGGER.error(Constants.ERROR, e);
            Clients.showNotification(Labels.getLabel("databaseError"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);

            Clients.clearBusy(DashboardDetailsController.this.getSelf());
            Messagebox.show(Labels.getLabel("stillDelete"), Labels.getLabel("deleteTitle"), new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO },
                    Messagebox.QUESTION, new EventListener<ClickEvent>() {
                        @Override
                        public void onEvent(ClickEvent event) throws Exception {
                            if (Messagebox.Button.YES.equals(event.getButton())) {
                                Events.sendEvent(Constants.EVENTS.ON_DELETE_COMPOSITION, dashboardConfig.getDashboardComponent(), dashboardConfig.getDashboard());
                            } else {
                                Events.postEvent(Events.ON_CLOSE, DashboardDetailsController.this.getSelf(), null);
                            }
                        }
                    });
        }
    }

    private void setQueryAndClone(ContractInstance contractInstance) throws HipieException, HPCCException, DatabaseException {
        dashboardConfig.getDashboard().setQueries(CompositionUtil.extractQueries(contractInstance, dashboardConfig.getDashboard().isStaticData()));
        if(MapUtils.isNotEmpty(dashboardConfig.getDashboard().getQueries())){
            try {
                CompositionUtil.cloneQuerySchema(dashboardConfig.getDashboard());
            } catch (CloneNotSupportedException e) {
                LOGGER.error("Unable to clone Query schema-->",e);
            }
        }
        dashboardConfig.getDashboard().addWidgets(Widget.extractVisualElements(contractInstance, dashboardConfig.getDashboard().getQueries(),
                //Passing Datasource status(Loading|valid|invalid) null as it is not applicable in Dashboard perspective
                dashboardConfig.getDashboard().isStaticData(),null));

        boolean isSmallDataset = CompositionUtil.extractRunOption(contractInstance);
        dashboardConfig.getDashboard().setLargeDataset(!isSmallDataset);
    }

    private void includeDashboard() {
        if(CollectionUtils.isNotEmpty(dashboardConfig.getDashboard().getWidgets())){
            dashboardConfig.getDashboard().getWidgets().stream().forEach(this::setDatasource);
        }
        dashboardDesignInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        dashboardDesignInclude.setSrc("dashboard/design/dashboard.zul");
    }

    private void setDatasource(Widget widget) {
        if (widget.getDatasource() != null) {
            dashboardConfig.addDatasource(widget.getDatasource());
        }
    }

    private void loadOutputTab() {
        dashboardOutputsInclude.setDynamicProperty(Constants.DASHBOARD_CONFIG, dashboardConfig);
        if (dashboardConfig.isReloadOutput()) {
            dashboardOutputsInclude.setSrc(null);
            dashboardConfig.setReloadOutput(false);
        }
        dashboardOutputsInclude.setSrc("dashboard/dashboardOutputs.zul");
        dashboardDesign.setSelected(false);
        dashboardOutputs.setSelected(true);
    }
}
