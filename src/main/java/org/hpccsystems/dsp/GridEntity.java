package org.hpccsystems.dsp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hpcc.HIPIE.Composition;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GridEntity.class);
    
    private String name;
    private String author;
    private String canonicalName;
    private Date lastModifiedDate;
    private String label;
    private String uuid;
    private boolean showGlobalVariable;
    private boolean isBatchTemplate;
    private boolean isFavourite=false;
    
    public GridEntity(Composition composition) {
        this.name = composition.getName();
        this.author = composition.getAuthor();
        this.label = composition.getLabel();
        this.canonicalName = composition.getCanonicalName();
        this.lastModifiedDate = new Date(composition.getLastModified());
        this.setUuid(composition.getId());
    }
    
    public GridEntity(String name, String author, String label, String canonicalName, Date lastModifiedDate, String id) {
        this.name = name;
        this.author = author;
        this.label = label;
        this.canonicalName = canonicalName;
        this.lastModifiedDate = lastModifiedDate;
        this.setUuid(id);
    }
    
    public GridEntity() {
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModified) {
        this.lastModifiedDate = lastModified;
    }

    public String getFormattedLastModifiedDate() {
        return new SimpleDateFormat(Constants.DATE_FORMAT).format(lastModifiedDate);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Composition getComposition() throws HipieException {
        try {
            return HipieSingleton.getHipie().getComposition(author, canonicalName, null, null, false, false);
        } catch (Exception e) {
            LOGGER.debug("Composition retrieval failed. Composition - {}, Userid - {}", canonicalName, author);
            throw new HipieException(e);
        }
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isShowGlobalVariable() {
        return showGlobalVariable;
    }

    public void setShowGlobalVariable(boolean variablesPresent) {
        showGlobalVariable = variablesPresent;
    }

    public boolean isBatchTemplate() {
        return isBatchTemplate;
    }

    public void setBatchTemplate(boolean isBatchTemplate) {
        this.isBatchTemplate = isBatchTemplate;
    }
    
    public boolean getIsFavourite(){
        return isFavourite;
    }
    
    public void setIsFavourite(boolean isFavourite){
        this.isFavourite = isFavourite;
    }
    
}
