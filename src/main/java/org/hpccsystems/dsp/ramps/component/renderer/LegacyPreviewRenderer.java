package org.hpccsystems.dsp.ramps.component.renderer;

import java.io.Serializable;
import java.util.List;

import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public class LegacyPreviewRenderer implements RowRenderer<List<String>>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void render(Row row, List<String> values, int i) throws Exception {
        Label label = new Label("" + (i + 1));
        row.appendChild(label);
        values.forEach(value -> row.appendChild(new Label(value)));
    }

    public static void setColumnsInGrid(List<String> headerRow, Grid previewGrid) {
        if (previewGrid.getColumns() == null) {
            previewGrid.appendChild(new Columns());
        }

        Columns previewColumns = previewGrid.getColumns();
        previewColumns.getChildren().clear();
        Column hashColumn = new Column("##");
        previewColumns.appendChild(hashColumn);

        headerRow.forEach(element -> {
            Column column = new Column(element);
            previewColumns.appendChild(column);
        });
        previewColumns.setSizable(true);

        previewGrid.invalidate();
    }

}