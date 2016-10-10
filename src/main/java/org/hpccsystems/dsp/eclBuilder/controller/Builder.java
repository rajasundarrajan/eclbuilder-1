
package org.hpccsystems.dsp.eclBuilder.controller;

import java.io.Serializable;
import java.util.Date;

public class Builder implements Serializable  {
	
	private String author;
	private String name;
	private String logicalFiles;
	private Date lastmodifieddate;
	private String eclbuildercode;
	private String hpccId;
	private String wuID;
	
	public Builder() {
		super();
	}

	public Builder(String user_id, String name, String logicalFiles, Date lastmodifieddate, String eclbuildercode, String hpccId, String wuID) {
		super();
		this.author = user_id;
		this.name = name;
		this.logicalFiles = logicalFiles;
		this.lastmodifieddate = lastmodifieddate;
		this.eclbuildercode = eclbuildercode;
		this.hpccId = hpccId;
		this.wuID = wuID;
	}
	
	public String getWuID() {
		return wuID;
	}

	public void setWuID(String wuID) {
		this.wuID = wuID;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLogicalFiles() {
		return logicalFiles;
	}

	public void setLogicalFiles(String logicalFiles) {
		this.logicalFiles = logicalFiles;
	}


	public Date getLastmodifieddate() {
		return lastmodifieddate;
	}

	public void setLastmodifieddate(Date lastmodifieddate) {
		this.lastmodifieddate = lastmodifieddate;
	}

	public String getEclbuildercode() {
		return eclbuildercode;
	}

	public void setEclbuildercode(String eclbuildercode) {
		this.eclbuildercode = eclbuildercode;
	}

	public String getHpccId() {
		return hpccId;
	}

	public void setHpccId(String hpccId) {
		this.hpccId = hpccId;
	}

}
