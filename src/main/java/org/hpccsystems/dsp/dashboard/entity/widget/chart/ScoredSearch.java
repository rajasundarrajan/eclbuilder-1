package org.hpccsystems.dsp.dashboard.entity.widget.chart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.ScoredSearchFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;

public class ScoredSearch extends Widget implements Cloneable {

    private static final long serialVersionUID = 1L;
    private List<ScoredSearchFilter> scoredSearchfilters;

    public List<ScoredSearchFilter> getScoredSearchfilters() {
        return scoredSearchfilters;
    }

    public void setScoredSearchfilters(List<ScoredSearchFilter> scoredSearchfilters) {
        this.scoredSearchfilters = scoredSearchfilters;
    }

    @Override
    public boolean isConfigured() {
        boolean isValid = false;
        if (scoredSearchfilters != null) {
            for (ScoredSearchFilter filter : scoredSearchfilters) {
                if (StringUtils.isEmpty(filter.getOperator()) || StringUtils.isEmpty(filter.getOperatorValue())) {
                    if (StringUtils.isEmpty(filter.getModifier()) || StringUtils.isEmpty(filter.getModifierValue())) {
                        isValid = false;
                    } else {
                        isValid = true;
                    }
                } else {
                    isValid = true;
                }
            }
        }
        return isValid;
    }

    @Override
    public VisualElement generateVisualElement() {
        // implementation is not required
        return null;
    }

    @Override
    public List<InputElement> generateInputElement(List<Element> contractElements, String datasourceName) {
        // implementation is not required
        List<InputElement> inputElements;
        inputElements = null;
        return inputElements;
    }

    @Override
    public List<Field> getInteractivityFields() {
        List<Field> interactivityFields;
        interactivityFields = null;
        return interactivityFields;
    }

    @Override
    public void removeInvalidFields() {
        // TODO: remove this check once databomb is implemented
        if (isDatabomb()) {
            return;
        }
        List<Field> fields = getDatasourceFields();
        if (fields != null) {
            List<ScoredSearchFilter> ssFilterToRemove = new ArrayList<ScoredSearchFilter>();
            scoredSearchfilters.stream().forEach(ssFilter -> {
                boolean validFilter = RampsUtil.isScoredSearchFilterPresent(fields, ssFilter);
                if (!validFilter) {
                    ssFilterToRemove.add(ssFilter);
                }
            });
            scoredSearchfilters.removeAll(ssFilterToRemove);
        }
    }

    @Override
    public boolean isValid() {
        List<Field> fields = getDatasourceFields();
        if (fields == null) {
            return false;
        }
        List<ScoredSearchFilter> ssFilterToRemove = new ArrayList<ScoredSearchFilter>();
        scoredSearchfilters.stream().forEach(ssFilter -> {
            boolean validFilter = RampsUtil.isScoredSearchFilterPresent(fields, ssFilter);
            if (!validFilter) {
                ssFilterToRemove.add(ssFilter);
            }
        });
        return hasDatasource() && ssFilterToRemove.isEmpty();
    }

    @Override
    public List<Field> getInputElementFields() {
        // Not required
        return Collections.emptyList();
    }

    @Override
    protected RecordInstance generateRecordInstance() {
        // Not required
        return null;
    }

    @Override
    public List<Field> getRecordInstanceFields() {
        return Collections.emptyList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((scoredSearchfilters == null) ? 0 : scoredSearchfilters.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScoredSearch other = (ScoredSearch) obj;
        if (scoredSearchfilters == null) {
            if (other.scoredSearchfilters != null) {
                return false;
            }
        } else if (!scoredSearchfilters.equals(other.scoredSearchfilters)) {
            return false;
        }
        return true;
    }

    @Override
    public ScoredSearch clone() throws CloneNotSupportedException {
        ScoredSearch scoredSearch = (ScoredSearch) super.clone();
        List<ScoredSearchFilter> clonedList = new ArrayList<>();
        for (ScoredSearchFilter filter : this.scoredSearchfilters) {
            clonedList.add(filter.clone());
            scoredSearch.setScoredSearchfilters(clonedList);
        }
        return scoredSearch;

    }
}
