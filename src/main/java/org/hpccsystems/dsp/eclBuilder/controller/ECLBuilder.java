package org.hpccsystems.dsp.eclBuilder.controller;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class ECLBuilder implements Serializable  {
	
	private String user_id;
	private String name;
	private String logicalFiles;
	private Timestamp modified_date;
	private String eclbuildercode;
	private String hpccConnId;
	private String wuID;
	
	public ECLBuilder(String user_id, String name, String logicalFiles, Timestamp modified_date, String eclbuildercode, String hpccConnid, String wuID) {
		super();
		this.user_id = user_id;
		this.name = name;
		this.logicalFiles = logicalFiles;
		this.modified_date = modified_date;
		this.eclbuildercode = eclbuildercode;
		this.hpccConnId = hpccConnid;
		this.wuID = wuID;
	}
	
	public String getWuID() {
		return wuID;
	}

	public void setWuID(String wuID) {
		this.wuID = wuID;
	}

	public String getUser_id() {
		return user_id;
	}
	public void setUser_id(String user_id) {
		this.user_id = user_id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getModified_date() {
		return modified_date;
	}
	public void setModified_date(Timestamp modified_date) {
		this.modified_date = modified_date;
	}
	public String getEclbuildercode() {
		return eclbuildercode;
	}
	public void setEclbuildercode(String eclbuildercode) {
		this.eclbuildercode = eclbuildercode;
	}
	public String getLogicalFiles() {
		return logicalFiles;
	}
	public void setLogicalFiles(String logicalFiles) {
		this.logicalFiles = logicalFiles;
	}
	public String getHpccConnId() {
		return hpccConnId;
	}
	public void setHpccConnId(String hpccConnId) {
		this.hpccConnId = hpccConnId;
	}
	
}
