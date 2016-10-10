package org.hpccsystems.dsp.eclBuilder.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.util.Clients;


public class PivotTableController extends SelectorComposer<Component> implements EventListener<Event> {
	
	String wuID;
	
	String resultName;
	
	String hpccId;
	
	@Override
	public void doAfterCompose(Component comp) throws Exception {
		
		
		
		
		wuID = (String) Executions.getCurrent().getArg().get("wuId");
		
		resultName = (String) Executions.getCurrent().getArg().get("resultName");
		
		hpccId = (String) Executions.getCurrent().getArg().get("hpccId");
		
		Clients.evalJavaScript("loadContent(\"" + wuID	+ "\", \""+ hpccId  + "\", \""+ resultName + "\")");

		
	}

	@Override
	public void onEvent(Event arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	
}


