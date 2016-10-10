package org.hpccsystems.dsp.service.impl;

import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.service.LogicalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service("logicalFileService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LogicalFileServiceImpl implements LogicalFileService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LogicalFileServiceImpl.class);

	private DSPDao dspDao;
	
	@Autowired
	public void setRampsDao(DSPDao dspDao) {
		this.dspDao = dspDao;
	}
	
	@Override
	public List<String> getBlacklistedThorFiles() {
		LOGGER.debug("getting blacklisted thor files from MySQL");
		
		List<String> result =  null; //dspDao.getApplicationValueList(Constants.ValueCategory.BLACK_LIST_THOR_FILE.name());
		
		try {
		    result = dspDao.getApplicationValueList(Constants.ValueCategory.BLACK_LIST_THOR_FILE.name());
		} catch (DatabaseException e) {
            // Unable to get blacklist from MySQL. Treat as if file is blacklisted.
            LOGGER.error(Constants.ERROR, e.getMessage());

		    // return null
		    return result;
		}
		
		return result;
	}
	
	@Override
	public boolean checkPluginsForBlacklistedFiles(DatasetPlugin datasetPlugin, List<String> blacklist) {
	    boolean hasBlacklistedFile = false;
	    for (Plugin plugin : datasetPlugin.getPlugins()) {
	        // datasource is on the blacklist
	        if (plugin.getLogicalFileName() != null && blacklist != null) {
	            hasBlacklistedFile = isFileInBlacklist(plugin.getLogicalFileName().replace("~", ""), blacklist);
	        }
	    }
	    return hasBlacklistedFile;
	}
	
	@Override
	public boolean checkElementsForBlacklistedFiles(List<Element> filteredGlobalInputs, List<String> blacklist) {
	    
	    for (Element inputElement: filteredGlobalInputs) {
            String lfName = null;
            if (inputElement != null && inputElement.getOption(Element.DEFAULT) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams() != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0) != null && 
                    inputElement.getOption(Element.DEFAULT).getParams().get(0).getName() != null) {
                lfName = inputElement.getOption(Element.DEFAULT).getParams().get(0).getName().replace("~", "");
            }
            
            if (inputElement.getType() != null && inputElement.getType().equals(InputElement.TYPE_STRING) && 
                    lfName != null) {
                return isFileInBlacklist(lfName, blacklist);
            }
        }
	    return false;
	}
	
	@Override
	public boolean isFileInBlacklist(String logicalFileName, List<String> blacklist) {
        // check the current path
        if (!logicalFileName.isEmpty() && blacklist.contains(logicalFileName)) {
            return true;
        } else {
            if (logicalFileName.isEmpty()) {
                return false;
            }
            // chop off the last element and check the new path
            String[] splitted = logicalFileName.split("::");
            Object[] splittedObj = ArrayUtils.removeElement(splitted, splitted[splitted.length-1]);
            String path = org.apache.commons.lang.StringUtils.join(splittedObj, "::");
            return (isFileInBlacklist(path, blacklist));
        }
    }

}
