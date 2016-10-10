package org.hpccsystems.dsp.dashboard.entity.widget;

public enum ChartType {

    PIE("PIE"), PYRAMID("PIE"), DONUT("PIE"), LINE("LINE"), BAR("BAR"), COLUMN("COLUMN"),
    STACKCOLUMN("LINE"), US_MAP("CHORO"), TABLE("TABLE"), SCORED_SEARCH("SCORED_SEARCH"), GLOBAL_FILTER("GLOBAL_FILTER");

    private final String chartCode;

    private ChartType(String code) {
        chartCode = code;
    }

    public String toString() {
        return chartCode;
    }

}
