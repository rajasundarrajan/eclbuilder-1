package org.hpccsystems.dsp.dashboard.controller;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard.EVENTS;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Anchorlayout;

public class CanvasGrid  implements Serializable{

    private static final long serialVersionUID = 1L;

    @Wire("#canvasThumbs")
    Anchorlayout canvasThumbs;
    
    private List<Widget> widgets;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CanvasGrid.class);
    
    @AfterCompose
    public void afterCompose(@ContextParam(ContextType.VIEW) Component view) {

        Selectors.wireComponents(view, this, false);

        canvasThumbs.setAttribute(Constants.WIDGET, this);
    }
    
    @GlobalCommand
    @NotifyChange("widgets")
    public void updateGridView() {
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("{}",widgets);
        }
    }


    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
        if(widgets != null){
            //To hide global filter widget in canvas page
        this.widgets = widgets.stream()
                .filter(widget -> !ChartType.GLOBAL_FILTER.equals(widget.getChartConfiguration().getType()))
                .collect(Collectors.toList());
        }else{
            this.widgets = widgets;
        }
    }
    
    @Command
    public void configureWidget(@BindingParam("widget") Widget widget) {
        Events.postEvent(EVENTS.ON_CONFIGURE_WIDGET, canvasThumbs, widget);
    }
    
    @Command
    public void deleteWidget(@BindingParam("widget") Widget widget) {
        Events.postEvent(EVENTS.ON_DELETE_WIDGET, canvasThumbs, widget);
    }
}
