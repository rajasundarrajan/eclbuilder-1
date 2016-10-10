package org.hpccsystems.dsp.ramps.component.renderer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.Constants.Flow;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Messagebox.ClickEvent;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Vlayout;

public class PluginItemRenderer implements ListitemRenderer<Plugin>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String IMG_BTN_DEL_BTN = "img-btn del-btn";
    private boolean isConfigurable;
    private boolean trueFlag = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginItemRenderer.class);

    TabData data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
    User user=((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser();

    public PluginItemRenderer() {
        this(false);
    }

    public PluginItemRenderer(boolean isConfigurable) {
        this.isConfigurable = isConfigurable;
    }

    @Override
    public void render(final Listitem listitem, final Plugin plugin, int index) throws Exception {
       
        Button button = new Button(plugin.getLabel());
        button.setSclass("flowButton");
        button.setAttribute(Constants.PLUGIN, plugin);
        Listcell listcell = generateListCell(listitem, plugin,button);
        button.addEventListener(Events.ON_CLICK, new EventListener<Event>() {

            @Override
            public void onEvent(Event event) throws Exception {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Posting select event to flow chart");
                }
                Events.postEvent(EVENTS.ON_SELECT_PLUGIN, listitem.getParent(), plugin);
            }
        });

        //Creating tool tip
        StringBuilder toolTip = new StringBuilder();
        Contract contract = plugin.isDatasourcePlugin() ? ((DatasetPlugin)plugin).getUseDatasetContract() : plugin.getContractInstance().getContract();
        toolTip.append(contract.getCanonicalName())
            .append("  v").append(contract.getVersion())
            .append("\nby: ").append(contract.getAuthor())
            .append("  Repo: ").append(contract.getRepositoryName())
            .append("\nLast updated on: ").append(new SimpleDateFormat(Constants.DATE_FORMAT).format(contract.getLastModified()));
        
        button.setTooltiptext(toolTip.toString());

        // disabling swapping for Config & rawdataset plugins
        buttonDragDropListener(plugin, button);

        Button arrow = new Button();
        arrow.setIconSclass("z-icon-long-arrow-down");
        arrow.setZclass("img-btn arrow-btn");
        arrow.setDroppable("true");

        arrow.addEventListener(Events.ON_DROP, new EventListener<DropEvent>() {

            @Override
            public void onEvent(DropEvent event) throws Exception {
                if (event.getDragged() instanceof Treecell) {
                    Treecell treecell = (Treecell) event.getDragged();
                    Plugin sourcePlugin = (Plugin) treecell.getAttribute(Constants.PLUGIN);
                    Map<String, Plugin> args = new HashMap<String, Plugin>();
                    args.put(Constants.DRAGGED, sourcePlugin);
                    args.put(Constants.DROPPED, plugin);
                    Events.postEvent(EVENTS.ON_DROP_PLUGIN_ON_ARROW, listitem.getParent(), args);
                    event.stopPropagation();
                } else {
                    Clients.showNotification("Drop on Plugin", "warning", listitem.getParent(), Constants.POSITION_MIDDLE_CENTER, 3000, true);
                }
            }
        });

        Div div = new Div();
        div.setZclass("up-div");
        listcell.appendChild(div);

        listcell.appendChild(button);

        div = new Div();
        div.setZclass("down-div");
        listcell.appendChild(div);

        listcell.appendChild(arrow);
        listitem.appendChild(listcell);

    }

    private Listcell generateListCell(final Listitem listitem, final Plugin plugin,Button button) {
        Listcell listcell = new Listcell();
        listcell.setZclass("flowListcell");
        final Vlayout vlayout = new Vlayout();
        vlayout.setSclass("flow-action-buttons");
        vlayout.setVisible(false);
        vlayout.appendChild(generateEditButton(listcell,plugin,button));
        // not trashing/swapping config & datasource plugins
        if (!plugin.isInputPlugin() && isConfigurable && plugin.isLiveHIPEPlugin() && data.getFlow() != Flow.VIEW && user.canEdit()) {
            Button removeButton = new Button();
            removeButton.setZclass(IMG_BTN_DEL_BTN);
            removeButton.setStyle("color: red;");
            removeButton.setIconSclass("z-icon-trash-o");
            removeButton.setTooltiptext(Labels.getLabel("deletePlugin"));
            removeButton.addEventListener(Events.ON_CLICK, event -> {

                Messagebox.show(Labels.getLabel("confirmdeletePlugin"), Labels.getLabel("deletePlugintitle"),
                        new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,
                        clickEvent -> deletePlugin(listitem, plugin, clickEvent));
                event.stopPropagation();
            });

            vlayout.appendChild(removeButton);
        }

        listcell.addEventListener(Events.ON_MOUSE_OVER, new EventListener<Event>() {

            @Override
            public void onEvent(Event arg0) throws Exception {
                vlayout.setVisible(true);
            }

        });

        listcell.addEventListener(Events.ON_MOUSE_OUT, new EventListener<Event>() {

            @Override
            public void onEvent(Event arg0) throws Exception {
                vlayout.setVisible(false);
            }
        });
      
       listcell.appendChild(vlayout);

        return listcell;
    }

    private void deletePlugin(final Listitem listitem, final Plugin plugin, ClickEvent clickEvent) {
        if (Messagebox.Button.YES.equals(clickEvent.getButton())) {
            Events.postEvent(EVENTS.ON_DELETE_PLUGIN, listitem.getParent(), plugin);
        }
    }

    private void buttonDragDropListener(final Plugin plugin, Button button) {
        if (isConfigurable && plugin.isLiveHIPEPlugin()) {
            //TODO Enable dragging when Swap support is implemented
            button.setDroppable("true");
            button.addEventListener(Events.ON_DROP, new EventListener<DropEvent>() {
                @Override
                public void onEvent(DropEvent event) throws Exception {
                    swapPlugin(event);
                    event.stopPropagation();
                }
            });
        }
    }

    private void swapPlugin(DropEvent event) {
        if (event.getDragged() instanceof Button) {
            Button dragged = (Button) event.getDragged();
            Button dropped = (Button) event.getTarget();

            Plugin draggedPlugin = (Plugin) dragged.getAttribute(Constants.PLUGIN);
            Plugin droppedPlugin = (Plugin) dropped.getAttribute(Constants.PLUGIN);

            Map<String, Plugin> args = new HashMap<String, Plugin>();
            args.put(Constants.DRAGGED, draggedPlugin);
            args.put(Constants.DROPPED, droppedPlugin);

            Events.postEvent(EVENTS.ON_SWAP_PLUGIN, dragged.getParent().getParent().getParent(), args);
        }

    }
    
    
    private Button generateEditButton(Listcell listcell,Plugin plugin, Button button){
        Button editButton = new Button();
        editButton.setZclass(IMG_BTN_DEL_BTN);
        editButton.setIconSclass("fa fa-pencil");
        editButton.setTooltiptext(Labels.getLabel("editPluginName"));
        Popup popup = new Popup();
        popup.setSclass("tableColPopup");
        listcell.appendChild(popup);
        Textbox textbox = new Textbox();
        textbox.setValue( plugin.getLabel());
        popup.appendChild(textbox);
        textbox.setFocus(trueFlag);
        editButton.addEventListener(Events.ON_CLICK, event -> {
            popup.open(button,"overlap");
            textbox.addEventListener(Events.ON_CHANGE, changeEvent -> {
                if(textbox.getValue() != null && !textbox.getValue().trim().isEmpty()){
                    if(plugin.isDatasourcePlugin()) {
                        DatasetPlugin datasetPlugin = (DatasetPlugin) plugin;
                        datasetPlugin.getPlugins()
                            .forEach(p -> p.getContractInstance()
                                    .addOption(Contract.LABEL,new FieldInstance(null,textbox.getValue())));
                    } else {
                        plugin.getContractInstance().addOption(Contract.LABEL,new FieldInstance(null,textbox.getValue()));
                    }
                    button.setLabel(textbox.getValue());
                    plugin.setLabel(textbox.getValue());
                }
                popup.close();
            });
        });
        
        return editButton;
    }

}
