package org.hpccsystems.dsp.eclBuilder.controller;

import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.util.*;
import org.zkoss.zk.ui.ext.*;
import org.zkoss.zk.au.*;
import org.zkoss.zk.au.out.*;
import org.zkoss.zul.*;

 
public class ECLBuildReportRenderer implements RowRenderer {
	
    public void render(final Row row, final java.lang.Object data) {
        String[] ary = (String[]) data;
        for(int i=0; i < ary.length; i++){
        	new Label(ary[i]).setParent(row);
        }
    }

	@Override
	public void render(Row row, Object data, int arg2) throws Exception {
		String[] ary = (String[]) data;
        for(int i=0; i < ary.length; i++){
        	new Label(ary[i]).setParent(row);
        }
		
	}
}