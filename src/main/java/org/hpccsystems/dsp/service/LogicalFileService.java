package org.hpccsystems.dsp.service;

import java.util.List;

import org.hpcc.HIPIE.dude.Element;
import org.hpccsystems.dsp.ramps.entity.DatasetPlugin;

public interface LogicalFileService {
	
	List<String> getBlacklistedThorFiles();

    boolean isFileInBlacklist(String logicalFileName, List<String> blacklist);

    boolean checkPluginsForBlacklistedFiles(DatasetPlugin datasetPlugin, List<String> blacklist);

    boolean checkElementsForBlacklistedFiles(List<Element> filteredGlobalInputs, List<String> blacklist);

}
