package org.hpccsystems.dsp.ramps.component.renderer;

import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

public class GlobalVariableRenderer implements ListitemRenderer<GlobalVariable>{

    @Override
    public void render(Listitem listitem, GlobalVariable variable, int index) throws Exception {
        Listcell nameCell = new Listcell();
        Listcell valueCell = new Listcell();
        Label variableName = new Label();
        Label variablevalue = new Label();
        
        listitem.appendChild(nameCell);
        listitem.appendChild(valueCell);
        nameCell.appendChild(variableName);
        valueCell.appendChild(variablevalue);               
        
        variableName.setValue(variable.getName());            
        variablevalue.setValue(variable.getValue());
    }
    
}
