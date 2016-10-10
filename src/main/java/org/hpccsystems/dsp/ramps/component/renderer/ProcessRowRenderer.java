package org.hpccsystems.dsp.ramps.component.renderer;

import java.io.Serializable;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.ProcessRetriver.ProcessType;
import org.hpccsystems.dsp.ramps.entity.Process;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Timer;

public class ProcessRowRenderer implements RowRenderer<Process>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final String ROW_BTN = "row-btn";
    
    private static final String ABORTED= "aborted";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRowRenderer.class);
    
    private TabData data;
    private Timer timer;
    private ProcessType processType;

    /**
     * @param data
     * @param timer
     * @param processType 
     */
    public ProcessRowRenderer(TabData data, Timer timer, ProcessType processType) {
        this.data = data;
        this.timer = timer;
        this.processType = processType;
    }

    @Override
    public void render(final Row row, final Process process, final int index) throws Exception {
        
        if (process.getId() != null) {

            A hyperLink = new A(process.getId());
            hyperLink.setHref(process.getWorkunitURL());
            hyperLink.setTarget("_blank");
            hyperLink.setStyle("cursor:pointer;text-decoration: underline;");

            row.appendChild(hyperLink);
            row.appendChild(new Label(process.getProjectName()));
            Label globalStatus = new Label(process.getStatus());
            row.appendChild(globalStatus);
            row.appendChild(new Label(process.getLastRunDateAsString()));
            row.appendChild(new Label(process.getRunner()));

            Hlayout hlayout = new Hlayout();

            if (processType == ProcessType.COMPOSITION
                    && index == 0
                    && RampsUtil.isDashboardConfigured(process.getCompositionInstance().getComposition())) {
                Button viewDashboard = new Button();
                viewDashboard.setLabel(Labels.getLabel("viewDashBoard"));
                viewDashboard.setSclass(ROW_BTN);
                
                if (process.isStatusComplete()) {
                    viewDashboard.addEventListener(Events.ON_CLICK, event ->
                        Events.postEvent(EVENTS.ON_DASHBOARD_VIEW, data.getProjectDetailComponent(), null)
                    );
                } else {
                    viewDashboard.setDisabled(true);
                }
                hlayout.appendChild(viewDashboard);
            }

            if (index == 0 && !process.isRunComplete()) {
                LOGGER.debug("Process - {} - is not complete. Initiating timer", process.getId());
                timer.start();
            }

            Button button = new Button();
            button.setLabel(Labels.getLabel("moreInfo"));
            button.setSclass(ROW_BTN);
            
            if (process.isRunning()) {

                Button abortWU = new Button();
                abortWU.setLabel(Labels.getLabel("abort"));
                abortWU.setSclass(ROW_BTN);

                abortWU.addEventListener(Events.ON_CLICK, event -> abortWU(row, process, globalStatus, abortWU));
                hlayout.appendChild(abortWU);

            }

            if (process.isStatusComplete()) {
                button.addEventListener(
                        Events.ON_CLICK,
                        event -> {
                            Component home = (Component) row.getDesktop().getAttribute(Constants.HOME_COMPONENT);
                            Events.postEvent(EVENTS.ON_OPEN_PROCESS_INFO, home, process);
                        });
            } else {
                button.setDisabled(true);
            }

            hlayout.appendChild(button);
            row.appendChild(hlayout);
        } else {
            Label label = new Label();
            Cell cell = new Cell();
            cell.appendChild(label);
            label.setValue("The process has been submitted...");
            label.setSclass("processLabel");
            cell.setColspan(6);
            row.appendChild(cell);
        }
    }

    private void abortWU(final Row row, final Process process, Label globalStatus, Button abortWU) {
        try {
            if (process.getCompositionInstance().isRunning()) {
                process.getCompositionInstance().abort();
                LOGGER.info("Process {} was aborted by user", process.getId());
                Clients.showNotification(Labels.getLabel("processAborted"), Clients.NOTIFICATION_TYPE_INFO,
                        row.getParent().getParent(), Constants.POSITION_TOP_CENTER, 3000, true);
                globalStatus.setValue(ABORTED);
                abortWU.setVisible(false);
                timer.stop();
            } else {
                Clients.showNotification(Labels.getLabel("cannotAborted"), Clients.NOTIFICATION_TYPE_ERROR,
                        row.getParent().getParent(), Constants.POSITION_TOP_CENTER, 3000, true);
            }
        } catch (Exception ex) {
            LOGGER.error(Constants.EXCEPTION, ex);
            Clients.showNotification(Labels.getLabel("abortFailed") + " " + ex.getMessage(),
                    Clients.NOTIFICATION_TYPE_ERROR, row.getParent().getParent(),Constants.POSITION_TOP_CENTER, 3000, true);
        }
    }

}
