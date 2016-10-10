package org.hpccsystems.dsp.dashboard.controller;

import java.util.ArrayList;
import java.util.List;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.service.HPCCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class QueryBrowserController extends SelectorComposer<Listbox> {

    private static final String ON_LOADING = "onLoading";

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserController.class);

    @Wire
    private Listbox queryList;
    @Wire
    private Textbox selectedQuery;

    private ListModelList<String> queryListModel = new ListModelList<String>();

    private HPCCConnection hpccConnection;
    private DashboardConfig dashboardConfig;
    private WidgetConfig widgetConfig;

    /**
     * Populates the plugin properties in the UI
     */
    private void populateProperties() {
        if (widgetConfig.getQueryName() != null && !widgetConfig.getQueryName().isEmpty()) {
            List<String> selection = new ArrayList<String>();
            selection.add(widgetConfig.getQueryName());
            queryListModel.setSelection(selection);
            queryList.getSelectedItem().setFocus(true);
            togglePreview(widgetConfig.getQueryName());
        }
    }

    @Override
    public void doAfterCompose(Listbox comp) throws Exception {
        super.doAfterCompose(comp);

        dashboardConfig = (DashboardConfig) Executions.getCurrent().getAttribute(Constants.DASHBOARD_CONFIG);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG);
        hpccConnection = dashboardConfig.getDashboard().getHpccConnection();
        LOGGER.debug("WidgetConfig - {}, HpccConnection - {}", widgetConfig, hpccConnection);

        this.getSelf().addEventListener(ON_LOADING, (SerializableEventListener<? extends Event>)event -> constructQueryBrowser());
        queryList.setSclass("queryList loading-message-grid");
        queryList.setEmptyMessage("<i class=\"fa fa-spinner fa-pulse\"></i> " + "Loading queries");
        Events.echoEvent(ON_LOADING, getSelf(), null);
    }

    private void togglePreview(String queryName) {
        selectedQuery.setText(queryName);
        widgetConfig.setQueryName(queryName);
    }

    private void constructQueryBrowser() throws HPCCException {
        queryListModel.addAll(((HPCCService) SpringUtil.getBean(Constants.HPCC_SERVICE)).getQueries(hpccConnection));
        queryListModel.setMultiple(false);
        queryList.setModel(queryListModel);
        queryList.setItemRenderer(new ListitemRenderer<String>() {

            @Override
            public void render(Listitem item, String data, int index) throws Exception {
                Listcell listCell = new Listcell();
                listCell.setIconSclass("fa fa-file-text");
                listCell.setLabel(data);
                listCell.setParent(item);
            }
        });
        populateProperties();
        queryList.setSclass("queryList");
        queryList.setEmptyMessage("No queries");
    }

    @Listen("onSelect = #queryList")
    public void onSelectQuery(SelectEvent<Listbox, String> event) {
        String query = queryListModel.getSelection().iterator().next();
        if(widgetConfig.getDatasource() != null) {
            widgetConfig.setDatasource(null);
        }
        widgetConfig.setDatasourceUpdated(!query.equals(widgetConfig.getQueryName()));
        togglePreview(query);
        

    }

}
