package org.hpccsystems.dsp.ramps.component.renderer;

import java.io.Serializable;
import java.util.List;

import org.hpccsystems.dsp.entity.Entity;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public class PreviewRenderer implements RowRenderer<Entity>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public void render(Row row, Entity entity, int i) throws Exception {
        Label label = new Label("" + (i+ 1));
        row.appendChild(label);
        for (Entity value : entity.getChildren()) {
            if (value.getChildren() == null || value.getChildren().isEmpty()) {
                row.appendChild(new Label(value.getValue()));
            } else {
                Grid grid = new Grid();
                grid.setParent(row);
                PreviewRenderer previewRenderer = new PreviewRenderer();
                previewRenderer.setColumnsInGrid(value.getChildren().iterator().next().getChildren(), grid, false);
                grid.setRowRenderer(previewRenderer);
                grid.setModel(new ListModelList<Entity>(value.getChildren()));
            }
        }
    }   

    public void setColumnsInGrid(List<Entity> headerRow, Grid previewGrid, boolean isImportFile) {

        if (previewGrid.getColumns() == null) {
            previewGrid.appendChild(new Columns());
        }

        Columns previewColumns = previewGrid.getColumns();
        previewColumns.getChildren().clear();
        Column hashColumn = new Column("##");
        previewColumns.appendChild(hashColumn);

        headerRow.forEach(element -> {
            Column column;
            if (!isImportFile) {
                column = new Column(element.getName());
            } else {
                column = new Column(element.getValue());
            }
            previewColumns.appendChild(column);
        });
        previewColumns.setSizable(true);
        previewGrid.invalidate();

    }
    
}
