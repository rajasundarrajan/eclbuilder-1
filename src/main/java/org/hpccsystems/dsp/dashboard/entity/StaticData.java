package org.hpccsystems.dsp.dashboard.entity;

import org.apache.commons.io.FilenameUtils;
import org.hpccsystems.dsp.Constants;

public class StaticData {
    
    private String user;
    private String fileName;
    private String fileContent;
    
    
    public StaticData(String user, String fileName, String fileContent) {
        super();
        this.user = user;
        this.fileName = FilenameUtils.removeExtension(fileName);
        this.fileContent = fileContent;
    }
    
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) { 
        this.fileName = fileName;
    }
    public String getFileContent() {
        return fileContent;
    }
    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
    //generates query name as 'userId|fileName'
    public String getQueryName() {
        StringBuilder queryName = new StringBuilder();
        queryName.append(/*RampsUtil.removeSpaceSplChar(*/getUser()).append(Constants.PIPE)
                .append(/*RampsUtil.removeSpaceSplChar(*/getFileName());
        return queryName.toString();
    }

    @Override
    public String toString() {
        return "StaticData [user=" + user + ", fileName=" + fileName + ", fileContent (size)=" + fileContent.length() + "]";
    }

    
}
