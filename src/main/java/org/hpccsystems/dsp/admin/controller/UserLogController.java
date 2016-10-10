package org.hpccsystems.dsp.admin.controller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.component.FilterColumn;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.log.UserLog;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.DBLogger;
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
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class UserLogController extends SelectorComposer<Component> {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserLogController.class);

    @Wire
    private Grid userLogGrid;

    @Wire
    private Datebox startDate;

    @Wire
    private Datebox endDate;

    private ListModelList<Object> model = new ListModelList<Object>();
    private Map<String, Set<Object>> filters = new HashMap<String, Set<Object>>();
    private List<UserLog> userLogs;

    @SuppressWarnings("unchecked")
    private SerializableEventListener<Event> addFilterListener = event -> {
        Map<String, Set<Object>> data = (Map<String, Set<Object>>) event.getData();
        RampsUtil.filterGridData(filters, model, userLogs, data);
    };

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        try {
            userLogs = ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).getUserLog();
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        model.addAll(userLogs);
        userLogGrid.setModel(model);
        generateGridHeader(userLogs);
        userLogGrid.setRowRenderer(new RowRenderer<Object>() {

            @Override
            public void render(Row row, Object rowObject, int index) throws Exception {
                userLogCreatingRows(row, rowObject);
            }

        });
        userLogGrid.addEventListener(EVENTS.ON_FILTER_CHANGE, addFilterListener);
    }

    private void userLogCreatingRows(Row row, Object rowObject) {
        UserLog userLog = (UserLog) rowObject;
        row.appendChild(new Label(userLog.getUserId()));
        row.appendChild(new Label(userLog.getAction()));
        Date date=new Date(userLog.getStartTime());
        SimpleDateFormat format = new SimpleDateFormat("dd-M-yyyy HH:mm:ss");
        row.appendChild(new Label(format.format(date)));
        row.appendChild(new Label(String.valueOf(userLog.getDuration())));
        row.appendChild(new Label(String.valueOf(userLog.getMemoryUtilized())));
        if (userLog.getDetail() != null) {
            if (userLog.getDetail().length() <= 50) {
                row.appendChild(new Label(userLog.getDetail()));
            } else {

                Hlayout details = new Hlayout();
                Button moreButton = new Button("...");
                details.appendChild(new Label(userLog.getDetail().substring(0, 50)));
                details.appendChild(moreButton);
                final Popup popup = new Popup();
                popup.setWidth("550px");
                popup.setZclass("popup userlogDetails");
                popup.setStyle("word-break: break-all");
                Label detail = new Label(userLog.getDetail());
                detail.setPre(true);
                popup.appendChild(detail);
                details.appendChild(popup);

                moreButton.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onEvent(Event event) throws Exception {
                        popup.open(moreButton, "start_center");
                    }
                });
                row.appendChild(details);
            }
        }
    }

    /**
     * Generates Grid header with user activity log properties
     * 
     * @param projects
     */
    private void generateGridHeader(List<UserLog> logList) {
        Columns columns = userLogGrid.getColumns();
        
        Column userID = new FilterColumn(Labels.getLabel("dspName"), "userId", logList);
        userID.setHflex("min");
        columns.appendChild(userID);
        
        Column action = new FilterColumn(Labels.getLabel("dspActions"), "action", logList);
        action.setHflex("min");
        columns.appendChild(action);
        
        Column dateTime = new Column(Labels.getLabel("dspDateTime"));
        dateTime.setHflex("1");
        columns.appendChild(dateTime);
        
        Column duration = new Column(Labels.getLabel("admUserLogDuration"));
        duration.setHflex("1");
        columns.appendChild(duration);
        
        Column memorey = new Column(Labels.getLabel("admUserLogMemory"));
        memorey.setHflex("1");
        columns.appendChild(memorey);
        
        Column logDetails = new Column(Labels.getLabel("admUserLogDetails"));
        logDetails.setHflex("5");
        columns.appendChild(logDetails);
    }

    @Listen("onClick = #refresh")
    public void getUserLogByDate(Event event) {

        if (startDate.getValue() == null || endDate.getValue() == null) {
            Clients.showNotification(Labels.getLabel("selectDatebeforeRefresh"), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(),
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        } else {
            model.clear();
            try {
                userLogs = ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).getUserLogByDate(startDate.getValue(), endDate.getValue());
            } catch (DatabaseException d) {
                LOGGER.error(Constants.EXCEPTION, d);
                Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
                return;
            }
            model.addAll(userLogs);
        }
    }

    @Listen("onClick = #clear")
    public void getUserLog(Event event) {

        model.clear();
        try {
            userLogs = ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).getUserLog();
        } catch (DatabaseException d) {
            LOGGER.error(Constants.EXCEPTION, d);
            Clients.showNotification(d.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf().getParent(), Constants.POSITION_TOP_CENTER, 5000, true);
            return;
        }
        model.addAll(userLogs);
        startDate.setValue(null);
        endDate.setValue(null);
        startDate.setPlaceholder("Start Date");
        endDate.setPlaceholder("End Date");
    }
}
