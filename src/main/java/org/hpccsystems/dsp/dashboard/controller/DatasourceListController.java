package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.ListitemRenderer;

public class DatasourceListController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceListController.class);

    private WidgetConfig widgetConfig;

    private DashboardConfig dashboardConfig;

    @Wire
    private Listbox datasourceListbox;

    private ListModelList<PluginOutput> outputsModel = new ListModelList<PluginOutput>();

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getArg().get(Dashboard.WIDGET_CONFIG);
        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dashboard config-- >{}", dashboardConfig);
            LOGGER.debug("widget config-- >{}", widgetConfig);
            LOGGER.debug("widgetDatasource-->{}", widgetConfig.getDatasource());
        }
        
        comp.addEventListener(EVENTS.ON_VALIDATE_CONFIG, (SerializableEventListener<? extends Event>)this::validateDatasource);

        List<PluginOutput> outputs = dashboardConfig.getDatasources();
        List<PluginOutput> outputswithoutVisualization = new ArrayList<PluginOutput>();
        for (PluginOutput output : outputs) {
            if (! output.getContractInstance().getContract().getRepositoryName().equals(Dashboard.DASHBOARD_REPO)) {
                outputswithoutVisualization.add(output);
            }

        }
        outputsModel.addAll(outputswithoutVisualization);
        outputsModel.setMultiple(false);
        outputsModel.setSelection(new HashSet<PluginOutput>(Arrays.asList(widgetConfig.getDatasource())));
        datasourceListbox.setModel(outputsModel);
        ListitemRenderer<PluginOutput> listRenderer = (item, pluginOutput, i) -> {
            Listcell cell = new Listcell();
            cell.setIconSclass("fa fa-puzzle-piece");
            cell.setLabel(pluginOutput.getLabel());
            cell.setParent(item);
        };

        datasourceListbox.setItemRenderer(listRenderer);
    }

    private void validateDatasource(Event event) {
        if(widgetConfig.getDatasource() == null || widgetConfig.getDatasource().getOutputElement() == null) {
            Clients.showNotification("Choose a datasource", "error", this.getSelf(), Constants.POSITION_TOP_CENTER, 3000, true); 
        }
    }

    @Listen("onSelect = #datasourceListbox")
    public void onDatasourceList(Event event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Selected output - {}", outputsModel.getSelection().iterator().next());
        }
        widgetConfig.setDatasource(outputsModel.getSelection().iterator().next());
        widgetConfig.setDatasourceType(DATASOURCE.FILE);
    }

}
