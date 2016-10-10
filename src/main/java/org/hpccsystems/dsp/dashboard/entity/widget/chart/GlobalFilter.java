package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.HipieException;

/**
 * It does generates global filter form
 */
public class GlobalFilter extends Widget {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean isConfigured() {
        return CollectionUtils.isNotEmpty(this.getFilters()) ;
    }

    public VisualElement generateVisualElement(List<String> allWidgets) throws HipieException {
        // Check foe filter availability
        if (CollectionUtils.isEmpty(getFilters())) {
            return null;
        }
        VisualElement visualElement = new VisualElement();
        try {
            DashboardUtil.updateBasicData(visualElement, this);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        
        visualElement.addOption(DashboardUtil.generateFilterFormSelectOption(allWidgets));
        DashboardUtil.addFormFilterField(visualElement, getFilters());
        return visualElement;
    }

    @Override
    public void removeInvalidFields() {

    }

    @Override
    public boolean isValid() {
        // set to ture for dashboard valid check in ramps perspective
        return true;
    }

    @Override
    public List<Field> getInputElementFields() {
        return new ArrayList<>();
    }

    @Override
    public List<Field> getRecordInstanceFields() {
        return new ArrayList<>();
    }

    @Override
    protected RecordInstance generateRecordInstance() {
        return null;
    }

    @Override
    public List<Field> getInteractivityFields() {
        return new ArrayList<>();
    }

    @Override
    public VisualElement generateVisualElement() throws HipieException {
        return null;
    }
    
    public void removeNonGlobalFilters() {
        if(getFilters() != null){
            getFilters().removeAll(getFilters().stream().filter(filter -> !filter.isGlobal()).collect(Collectors.toList()));
        }
        
    }

}
