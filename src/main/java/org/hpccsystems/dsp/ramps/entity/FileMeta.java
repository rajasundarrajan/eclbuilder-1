package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.List;

public class FileMeta implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private String fileName;
    private String displayFileName;
    private String scope;
    private Boolean isDirectory = false;
    private boolean isLogicalFile = false;
    private List<FileMeta> childlist;

    public String getDisplayFileName() {
		return displayFileName;
	}

	public void setDisplayFileName(String displayFileName) {
		this.displayFileName = displayFileName;
	}

	public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.displayFileName = fileName;
    }

    public Boolean isDirectory() {
        return isDirectory;
    }

    public void setIsDirectory(Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    public List<FileMeta> getChildlist() {
        return childlist;
    }

    public void setChildlist(List<FileMeta> childlist) {
        this.childlist = childlist;
    }

    public boolean isLogicalFile() {
		return isLogicalFile;
	}

	public void setLogicalFile(boolean isLogicalFile) {
		this.isLogicalFile = isLogicalFile;
	}

	@Override
    public String toString() {
        return "FileMeta [fileName=" + fileName + ", scope=" + scope + ", isDirectory=" + isDirectory + ", childsCount="
                + (childlist == null ? null : childlist.size()) + "]";
    }
    
    
}
