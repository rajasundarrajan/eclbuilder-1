package org.hpccsystems.dsp.ramps.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.PluginRelation;
import org.hpccsystems.dsp.ramps.entity.PluginRelationInputs;
import org.hpccsystems.dsp.ramps.entity.PluginRelationOutput;
import org.hpccsystems.dsp.ramps.entity.PluginRelations;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Window;

public class PluginRelationsController extends SelectorComposer<Component> {

    private static final String RELATION_OUTPUT = "relationOutput";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginRelationsController.class);
    private PluginRelations pluginRelations;

    private ListModelList<PluginRelationInputs> relationsModel = new ListModelList<PluginRelationInputs>();

    @Override
    public void doAfterCompose(final Component comp) throws Exception {
        super.doAfterCompose(comp);

        pluginRelations = (PluginRelations) Executions.getCurrent().getArg().get(Constants.PLUGIN_RELATION);
        Project project = (Project) Executions.getCurrent().getArg().get(Constants.PROJECT);

        if (pluginRelations.hasDatasetPluginRelation()) {
            pluginRelations.disintegrateDatasetRelation(project);
        }

        // Transforming relation list into Model
        Map<Plugin, List<PluginRelation>> relationMap = pluginRelations.getPluginRelations().stream()
                .collect(Collectors.groupingBy(PluginRelation::getDestplugin));

        List<PluginRelationInputs> inputRelations = relationMap.entrySet().stream().map(entry -> {
            Plugin target = entry.getKey();
            List<String> inputs = target.getInputElements().stream().map(element -> element.getName()).collect(Collectors.toList());

            List<PluginRelation> relations = entry.getValue();
            List<PluginRelationOutput> outputRelations = new ArrayList<>();
            relations.forEach(relation -> relation.getSourcePlugin().getOutputElements()
                    .forEach(element -> outputRelations.add(new PluginRelationOutput(element.getName(), relation))));

            return new PluginRelationInputs(inputs, outputRelations);
        }).collect(Collectors.toList());

        getRelationsModel().addAll(inputRelations);

    }

    @Listen("onCatchOutput = #parentBox")
    public void attachInput(ForwardEvent forwardEvent) {
        LOGGER.debug("Listening drop..,");
        DropEvent dropEvent = (DropEvent) forwardEvent.getOrigin();
        Div target = (Div) dropEvent.getTarget();
        PluginRelationInputs relationInputs = (PluginRelationInputs) target.getAttribute("relationIP");
        Listitem draggeditem = (Listitem) dropEvent.getDragged();
        PluginRelationOutput relationOutput = draggeditem.getValue();

        if (relationInputs.getOutputs().contains(relationOutput)) {
            String inputName = (String) forwardEvent.getData();
            String outputName = relationOutput.getSourceOutput();
            relationOutput.getPluginRelation().addRelation(inputName, outputName);
            LOGGER.debug("dropped on plugin relation---------->{}, {}", inputName, outputName);

            Label label = (Label) target.getFirstChild();
            label.setAttribute(RELATION_OUTPUT, relationOutput);
            label.setValue(relationOutput.getDisplayName());

            target.setDroppable(Constants.FALSE);
            target.setSclass("dropdiv-dropped");
            target.getLastChild().setVisible(true);
        } else {
            LOGGER.debug("Dropped output is of defferent origin");
        }

    }

    @Listen("onRemoveRelation = #parentBox")
    public void removeRelation(ForwardEvent forwardEvent) {
        MouseEvent clickEvent = (MouseEvent) forwardEvent.getOrigin();
        Button target = (Button) clickEvent.getTarget();
        String inputName = (String) forwardEvent.getData();
        PluginRelationOutput relationOutput = (PluginRelationOutput) target.getParent().getFirstChild().getAttribute(RELATION_OUTPUT);
        relationOutput.getPluginRelation().removeRelation(inputName);
        Label label = (Label) target.getParent().getFirstChild();
        label.setValue(Labels.getLabel("dropOutput"));
        ((Div) target.getParent()).setDroppable(Constants.TRUE);
        ((Div) target.getParent()).setSclass("dropdiv-droppable");
        target.setVisible(false);
    }

    @Listen("onClick = #OkButton")
    public void onSelectOutput() {
        // Validating relations
        List<PluginRelationInputs> relations = relationsModel.getInnerList();
        if (relations.stream().anyMatch(relationIP -> !relationIP.hasValidRelation())) {
            Clients.showNotification(Labels.getLabel("relationInvalid"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_MIDDLE_CENTER,
                    5000, true);
            return;
        }

        // Removed un-hooked relations
        pluginRelations.getPluginRelations().removeIf(relation -> relation.hasNoRelations());

        LOGGER.debug("Relations established - {}", pluginRelations);

        Events.postEvent(Constants.ON_CHOOSING_PLUGIN_RELATION, ((Window) getSelf()).getParent(), pluginRelations);
        ((Window) getSelf()).detach();
    }

    public ListModelList<PluginRelationInputs> getRelationsModel() {
        return relationsModel;
    }

}
