package org.hpccsystems.dsp.dashboard.entity.widget;

public enum HipieCharts {

    PIE("AM_PIE"), PYRAMID("AM_PYRAMID"), LINE("AM_LINE"), BAR("AM_BAR"), COLUMN("AM_COLUMN"), US_MAP("CHORO"), TABLE("TABLE"), SCORED_SEARCH(
            "SCORED_SEARCH"),STACKCOLUMN("AM_COLUMN");

    private String chartName;

    HipieCharts(String chartName) {
        this.chartName = chartName;
    }
    
    public String getChartName() {
        return chartName;
    }

}
