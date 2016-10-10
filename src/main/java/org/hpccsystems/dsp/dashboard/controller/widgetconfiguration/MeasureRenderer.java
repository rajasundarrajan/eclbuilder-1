package org.hpccsystems.dsp.dashboard.controller.widgetconfiguration;

import java.util.List;

import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure;
import org.hpccsystems.dsp.dashboard.entity.widget.Measure.AGGREGATION;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zul.Button;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;
import org.zkoss.zul.TreeNode;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;
import org.zkoss.zul.Treerow;

public class MeasureRenderer implements TreeitemRenderer<TreeNode<Measure>> {

    @Override
    public void render(Treeitem item, TreeNode<Measure> node, int index) throws Exception {
        Measure measure = node.getData();
        if(!node.isLeaf()) {
            item.setLabel(measure.getColumn());
            item.setOpen(true);
            return;
        }
        
        item.setValue(node);
        Treecell treecell = new Treecell(measure.getColumn());
        item.setDraggable(Dashboard.TRUE);
         if(! measure.isRowCount()){
             final Popup popup = new Popup();
             popup.setWidth("100px");
             popup.setZclass(Dashboard.STYLE_POPUP);        
             
             final Button button = new Button("NONE");
             button.setTooltiptext("Aggregation");
             measure.setAggregation(AGGREGATION.NONE);
             button.setZclass("btn btn-xs btn-sum");
             button.setPopup(popup);
             Listbox listbox = new Listbox();
             listbox.setMultiple(false);

             List<AGGREGATION> list = DashboardUtil.getAggregationList();
             listbox.setModel(new ListModelList<>(list));
             ListitemRenderer<AGGREGATION> renderer = (listitem, agg, ind) -> listitem.setLabel(agg.name());
             listbox.setItemRenderer(renderer);

             EventListener<SelectEvent<Component, AGGREGATION>> selectListener = event -> {
                 AGGREGATION selectedItem = event.getSelectedObjects().iterator().next();
                 measure.setAggregation(selectedItem);
                 button.setLabel(selectedItem.name());
                 popup.close();
             };
             listbox.addEventListener(Dashboard.ON_SELECT, selectListener);

             popup.appendChild(listbox);
             treecell.appendChild(popup);
             treecell.appendChild(button);

         }
        Treerow treerow = new Treerow();
        treerow.appendChild(treecell);
        item.appendChild(treerow);

    }
}
