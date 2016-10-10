package org.hpccsystems.dsp.ramps.controller.utils;

import java.util.ArrayList;
import java.util.List;

import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.HPCCLogicalFile;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class FileBrowserRetriver implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBrowserRetriver.class);
    private EventListener<Event> eventListener;
    private Desktop desktop;
    private HPCCConnection hpccConnection;
    private FileMeta fileMetaBrowser;
    private List<String> blacklist;

    public FileBrowserRetriver(Desktop executionDesktop, EventListener<Event> eventListener, FileMeta fileMetaBrowser, HPCCConnection hpccConnection, List<String> blacklist) {
        this.eventListener = eventListener;
        this.desktop = executionDesktop;
        this.fileMetaBrowser = fileMetaBrowser;
        this.hpccConnection = hpccConnection;
        this.blacklist = blacklist;
    }

    @Override
    public void run() {
        try {
            List<FileMeta> results = getFileList(fileMetaBrowser.getScope(), hpccConnection, blacklist);
            fileMetaBrowser.setChildlist(results);
            schedule(new Event(EVENTS.ON_FILE_LOADED, null, fileMetaBrowser));
        } catch (HPCCException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            schedule(new Event(EVENTS.ON_FILE_LOAD_FAILED, null, e));
        }

    }

    public static List<FileMeta> getFileList(String scope, HPCCConnection hpccConnection, List<String> blacklist) throws HPCCException {

        List<FileMeta> results = new ArrayList<FileMeta>();

        List<HPCCLogicalFile> resultsArray;
        try {
            resultsArray = hpccConnection.getFilenames(scope, hpccConnection.getThorCluster());
            FileMeta fileMeta;

            for (HPCCLogicalFile hpccLogicalFile : resultsArray) {
                // If it's the root directory just check the name
                if (hpccLogicalFile.getScope().isEmpty() && blacklist.contains(hpccLogicalFile.getFileName())) {
                    continue;
                }
                // If its a sub-directory check the scope + name
                else if (hpccLogicalFile.isDirectory() && 
                        blacklist.contains(hpccLogicalFile.getScope() + "::" + hpccLogicalFile.getFileName())){
                     continue;
                }
                // If it's a file in a subdirectory just check the file name
                else if (blacklist.contains(hpccLogicalFile.getFileName())) {
                    continue;
                }
                
                fileMeta = new FileMeta();
                if (hpccLogicalFile.isDirectory()) {
                    fileMeta.setIsDirectory(true);
                    fileMeta.setFileName(hpccLogicalFile.getFileName());
                    fileMeta = settingScope(scope, fileMeta, hpccLogicalFile);
                } else {
                    fileMeta.setIsDirectory(false);
                    fileMeta.setFileName(hpccLogicalFile.getFileName());
                    fileMeta.setScope(hpccLogicalFile.getScope());
                }
                results.add(fileMeta);
            }
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException(Labels.getLabel("unableToFetchFileList"), e);
        }

        return results;
    }

    private static FileMeta settingScope(String scope, FileMeta fileMeta, HPCCLogicalFile hpccLogicalFile) {
        if (scope.length() > 0) {
            fileMeta.setScope(scope + "::" + hpccLogicalFile.getFileName());
        } else {
            fileMeta.setScope("~" + hpccLogicalFile.getFileName());
        }
        return fileMeta;
    }
    
    private void schedule(Event event) {
        if(desktop.isAlive()) {
            Executions.schedule(desktop, eventListener, event);
        } else {
            LOGGER.warn("Desktop is unavailable to shedule retrived files");
        }
    }
}
