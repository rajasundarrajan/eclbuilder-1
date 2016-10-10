package org.hpccsystems.dsp.component;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.text.WordUtils;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Popup;

public class FilterColumn extends Column {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FilterColumn.class);

    private Button button = new Button();
    private Popup popup = new Popup();

    public FilterColumn(String displayName, final String fieldName,
            Collection<?> rowObjects) {

        this.setLabel(displayName);

        // Getting values of the field, to add to Filter list
        List<Object> values = (List<Object>) CollectionUtils.collect(rowObjects,
                new Transformer<Object, Object>() {

                    @Override
                    public Object transform(Object rowObject) {
                        Object myObject;
                        try {
                            myObject= rowObject.getClass().getMethod(
                                    "get" + WordUtils.capitalize(fieldName),
                                    (Class<?>[]) null).invoke(rowObject,
                                    (Object[]) null);
                        } catch (IllegalAccessException
                                | IllegalArgumentException
                                | InvocationTargetException
                                | NoSuchMethodException | SecurityException e) {
                            LOGGER.error(Constants.EXCEPTION, e);
                            myObject= null;
                        }
                        return myObject;
                    }
                });

        LOGGER.debug("Projects - {}", rowObjects.size());
        LOGGER.debug("Values - {}", values);

        // Removing duplicates
        Set<Object> valueSet = new HashSet<Object>(values);

        // Enable sorting
        try {
            this.setSort("auto(" + fieldName + ")");
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }

        button.setIconSclass("glyphicon glyphicon-filter");
        button.setZclass("btn btn-link img-btn right-btn");
        button.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                popup.open(button, "after_center");
            }
        });
        this.appendChild(button);

        popup.setWidth("230px");
        popup.setZclass("popup");

        final Listbox listbox = new Listbox();
        final ListModelList<Object> listModelList = new ListModelList<Object>(
                valueSet);
        listbox.setModel(listModelList);
        listbox.setItemRenderer(new ListitemRenderer<Object>() {

            @Override
            public void render(Listitem listitem, Object object, int index)
                    throws Exception {
                if (object instanceof Date) {
                    listitem.setLabel(new SimpleDateFormat(
                            Constants.DATE_FORMAT).format(object));
                } else {
                    listitem.setLabel(String.valueOf(object));
                }
            }
        });

        Listhead listhead = new Listhead();
        Listheader listheader = new Listheader(Labels.getLabel("selectAll"));
        Button clearButton = new Button(Labels.getLabel("clear"));
        clearButton.setSclass("right-btn");
        clearButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {
                    @Override
                    public void onEvent(Event event) throws Exception {
                        listModelList.clearSelection();
                    }
                });
        Button applyButton = new Button();
        applyButton.setIconSclass("glyphicon glyphicon-ok");
        applyButton.setZclass("btn img-btn right-btn");
        applyButton.addEventListener(Events.ON_CLICK,
                new EventListener<Event>() {

                    @Override
                    public void onEvent(Event event) throws Exception {
                        Map<String, Set<Object>> filter = new HashMap<String, Set<Object>>();
                        filter.put(fieldName, listModelList.getSelection());

                        Events.postEvent(EVENTS.ON_FILTER_CHANGE,
                                FilterColumn.this.getParent().getParent(),
                                filter);
                    }
                });
        listheader.appendChild(applyButton);
        listheader.appendChild(clearButton);
        listhead.appendChild(listheader);
        listbox.appendChild(listhead);

        listbox.setMultiple(true);
        listbox.setCheckmark(true);
        listbox.setRows(7);
        popup.appendChild(listbox);

        this.appendChild(popup);
    }


}
