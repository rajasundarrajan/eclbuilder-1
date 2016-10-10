package org.hpccsystems.dsp.eclBuilder.controller;

import java.util.List;

public class ECLBuilderReportData {
	String builderReportName;
	
	String wuId;
	
	List<String> rptColumns;
	
	List<String[]> listData;

	public String getBuilderReportName() {
		return builderReportName;
	}

	public void setBuilderReportName(String builderReportName) {
		this.builderReportName = builderReportName;
	}

	public List<String> getRptColumns() {
		return rptColumns;
	}

	public void setRptColumns(List<String> rptColumns) {
		this.rptColumns = rptColumns;
	}

	public List<String[]> getListData() {
		return listData;
	}

	public void setListData(List<String[]> listData) {
		this.listData = listData;
	}

	public String getWuId() {
		return wuId;
	}

	public void setWuId(String wuId) {
		this.wuId = wuId;
	}
	
	
	
}
