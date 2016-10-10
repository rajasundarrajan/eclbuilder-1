package org.hpccsystems.dsp.ramps.entity;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import org.hpccsystems.dsp.ramps.entity.FileStructure.Type;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.SettingsService;
import org.hpccsystems.ws.client.HPCCFileSprayClient.SprayVariableFormat;
import org.hpccsystems.ws.client.utils.DelimitedDataOptions;
import org.zkoss.zkplus.spring.SpringUtil;

public class SprayConfiguration implements Serializable{
   
    private static final long serialVersionUID = 1L;
    private String userId;
    
    private String logicalFile;
    private File file;
    
    private List<FileStructure> structure;
    
    private Type type;
    private DelimitedDataOptions delimitedOptions;
    private int recordLength;
    
    private String rootTag;
    private String sprayRootTag;
    private SprayVariableFormat xmlFormat;
    
    private boolean keepOriginalFile;
    
    private int sprayRetryCount;
    
    /**
     * Instantiates a spray configuration for the current user
     */
    public SprayConfiguration() {
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean("authenticationService");
        userId = authenticationService.getCurrentUser().getId();
        SettingsService settingsService = (SettingsService) SpringUtil.getBean("settingsService");
        sprayRetryCount = settingsService.getSprayRetryCount();
    }
    
    public String getLogicalFile() {
        return logicalFile;
    }
    public void setLogicalFile(String logicalFile) {
        this.logicalFile = logicalFile;
    }
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
    public int getRecordLength() {
        return recordLength;
    }
    public void setRecordLength(int recordLength) {
        this.recordLength = recordLength;
    } 
    public List<FileStructure> getStructure() {
        return structure;
    }
    public void setStructure(List<FileStructure> structure) {
        this.structure = structure;
    }
    public DelimitedDataOptions getDelimitedOptions() {
        return delimitedOptions;
    }
    public void setDelimitedOptions(DelimitedDataOptions delimitedOptions) {
        this.delimitedOptions = delimitedOptions;
    }
    public Type getType() {
        return type;
    }
    public void setType(Type type) {
        this.type = type;
    }
    
    public boolean isCSV() {
        return this.type == Type.CSV;
    }
    public boolean isFLAT() {
        return this.type == Type.FLAT;
    }
    public boolean isXML() {
        return this.type == Type.XML;
    }
    public boolean isKeepOriginalFile() {
        return keepOriginalFile;
    }
    public void setKeepOriginalFile(boolean keepOriginalFile) {
        this.keepOriginalFile = keepOriginalFile;
    }
    public String getRootTag() {
        return rootTag;
    }
    public void setRootTag(String rootTag) {
        this.rootTag = rootTag;
    }
    public SprayVariableFormat getXmlFormat() {
        return xmlFormat;
    }
    public void setXmlFormat(SprayVariableFormat xmlFormat) {
        this.xmlFormat = xmlFormat;
    }
    public String getSprayRootTag() {
        return sprayRootTag;
    }
    public void setSprayRootTag(String sprayRootTag) {
        this.sprayRootTag = sprayRootTag;
    }

    public String getUserId() {
        return userId;
    }

    public int getSprayRetryCount() {
        return sprayRetryCount;
    }
}