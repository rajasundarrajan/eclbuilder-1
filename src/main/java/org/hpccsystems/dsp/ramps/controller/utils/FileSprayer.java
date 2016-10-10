package org.hpccsystems.dsp.ramps.controller.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.HPCCLogicalFile;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.exceptions.DSPException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.ramps.controller.entity.SprayProgress;
import org.hpccsystems.dsp.ramps.entity.FileMeta;
import org.hpccsystems.dsp.ramps.entity.FileStructure;
import org.hpccsystems.dsp.ramps.entity.SprayConfiguration;
import org.hpccsystems.ws.client.HPCCFileSprayClient;
import org.hpccsystems.ws.client.HPCCFileSprayClient.SprayVariableFormat;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.gen.filespray.v1_13.ProgressRequest;
import org.hpccsystems.ws.client.gen.filespray.v1_13.ProgressResponse;
import org.hpccsystems.ws.client.platform.Platform;
import org.hpccsystems.ws.client.platform.WorkunitInfo;
import org.hpccsystems.ws.client.utils.Connection;
import org.hpccsystems.ws.client.utils.DelimitedDataOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

/**
 * The SprayFile attempts to spraying a file onto a target cluster on the target
 * HPCC System
 * 
 * @author 407003
 */
public class FileSprayer implements Runnable {

    private static final String DS_DATASET = "DS := DATASET('";
    private static final String FILE_SPRAY_FAILURE = "fileSprayFailure";
    private static final String RECORD = "RECORD ";
    private static final String SPRAYING = "spraying";
    private static final String CONVERT_TO_THOR = "convetToThor";
    private static final String DELETE_TEMP = "deletingOriginalFile";
    private static final String STRUCT = "struct := ";
    private static final String CLUSTER = "',CLUSTER('";
    private static final String OVERWRITE = "'),OVERWRITE);";
    private static final String FILE_UPLOADED = "fileUploaded";
    private static final String FAILED = "FAILED";
    private static final String PREFIX = "~";
    private final Desktop executionDesktop;
    private final EventListener<Event> eventListener;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSprayer.class);
    private SprayConfiguration sprayConfiguration;
    private HPCCConnection hpccConnection;
    protected boolean verbosemode = false;
    protected Connection connection = null;
    protected Object connectionLock = new Object();
    public static final String ERROR_INITIALIZE_FILESPRAY_CLIENT = "Could not initialize HPCC fileSpray Client";
    HPCCWsClient connector = null;
    
    /**
     * Instantiates SprayFile, communicates with HPCC over given Desktop,
     * eventListener(ImportFileController), sprayConfiguration which is a pojo
     * for spraying a file and HPCCConection credentials
     * 
     * @param executionDesktop
     * @param eventListener
     * @param sprayConfiguration
     * @param hpccConnection
     */
    public FileSprayer(Desktop executionDesktop, EventListener<Event> eventListener, SprayConfiguration sprayConfiguration,
            HPCCConnection hpccConnection) {
        this.executionDesktop = executionDesktop;
        this.eventListener = eventListener;
        this.sprayConfiguration = sprayConfiguration;
        this.hpccConnection = hpccConnection;

    }

    /**
     * Thread for spraying a file
     */
    @Override
    public void run() {
        spray();
    }

    /**
     * File spraying to target cluster
     */
    private void spray() {
        try {
            String logicalFile = sprayConfiguration.getLogicalFile();
            File file = sprayConfiguration.getFile();

            boolean spraySuccess = false;
            logicalFile = logicalFile.startsWith(PREFIX) ? logicalFile : PREFIX + logicalFile;

            Platform platform = hpccConnection.getPlatform();
            connector = platform.getHPCCWSClient();
            uploadToLandingzone(file, connector);

            // Passing the percentage of completion of the spraying process
            postProgress(35, FILE_UPLOADED);

            String targetCluster = getTargetcluster(connector);
            postProgress(40, SPRAYING);

            String temprayFileName = null;

            if (sprayConfiguration.isCSV()) {
                temprayFileName = logicalFile + "csv";

                sprayCSVFile(connector, file.getName(), temprayFileName, targetCluster, sprayConfiguration.getDelimitedOptions());

                postProgress(98, CONVERT_TO_THOR);
                spraySuccess = convertToTHOR(hpccConnection, sprayConfiguration.getStructure(), temprayFileName, logicalFile, false);

            } else if (sprayConfiguration.isFLAT()) {
                temprayFileName = logicalFile + "thor";

                spraySuccess = sprayFlatHPCCFile(connector, file.getName(), temprayFileName, sprayConfiguration.getRecordLength(), targetCluster,
                        true);

                postProgress(98, CONVERT_TO_THOR);
                spraySuccess = addStrucutureToTHOR(hpccConnection, sprayConfiguration.getStructure(), logicalFile);
            } else if (sprayConfiguration.isXML()) {
                temprayFileName = logicalFile + "xml";

                sprayXMLFile(connector, file.getName(), temprayFileName, targetCluster);

                postProgress(98, CONVERT_TO_THOR);

                spraySuccess = convertToTHOR(hpccConnection, sprayConfiguration.getStructure(), temprayFileName, logicalFile, true);
            }

            if (!sprayConfiguration.isKeepOriginalFile()) {
                postProgress(98, DELETE_TEMP);
                deleteFile(temprayFileName);
            }

            setSprayStatus(logicalFile, spraySuccess);
        } catch (DSPException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            schedule(new Event(EVENTS.ON_SPRAY_FAILED, null, Labels.getLabel("clusterBusy")));
            return;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            schedule(new Event(EVENTS.ON_SPRAY_FAILED, null, e.getMessage()));
            return;
        }

    }

    private void setSprayStatus(String logicalFile, boolean spraySuccess) {
        String tempLogicalFile = logicalFile;
        if (spraySuccess) {
            tempLogicalFile = tempLogicalFile.startsWith(PREFIX) ? tempLogicalFile.substring(1, tempLogicalFile.length()) : tempLogicalFile;
            tempLogicalFile = tempLogicalFile.contains("::") ? tempLogicalFile : ".::".concat(tempLogicalFile);
            tempLogicalFile = PREFIX.concat(tempLogicalFile);
            schedule(new Event(EVENTS.ON_SPRAY_COMPLETE, null, tempLogicalFile));
        } else {
            schedule(new Event(EVENTS.ON_SPRAY_FAILED, null, "Spray Failed"));
        }
    }

    private void postProgress(int percent, String label) {
        schedule(new Event(EVENTS.ON_SPRAY_PROGRESS, null, new SprayProgress(percent, label)));
    }

    private void uploadToLandingzone(File file, HPCCWsClient connector) throws HPCCException {
        if (!connector.httpUploadFileToFirstHPCCLandingZone(file.getAbsolutePath())) {
            throw new HPCCException(Labels.getLabel("failedToTransferFile"));
        }
    }

    private String getTargetcluster(HPCCWsClient connector) throws HPCCException {
        try {
            List<String> clusters = connector.getAvailableTargetClusterNames();
            return clusters.get(0);
        } catch (Exception e) {
            throw new HPCCException(Labels.getLabel("clusterNamesCouldNotBeFetched"), e);
        }
    }

    private void sprayXMLFile(HPCCWsClient connector, String fileName, String logicalFile, String targetCluster) throws HPCCException {
        String tempLogicalFile = logicalFile.startsWith(PREFIX) ? logicalFile : PREFIX + logicalFile;
        HPCCFileSprayClient fileSprayClient = connector.getFileSprayClient();

        try {
            ProgressResponse response = fileSprayClient.sprayLocalXML(fileName, tempLogicalFile, "", targetCluster, true,
                    sprayConfiguration.getXmlFormat(), sprayConfiguration.getSprayRootTag(), 8192);
            boolean isSprayed = handleSprayResponse(response);

            if (!isSprayed) {
                throw new HPCCException(Labels.getLabel(FILE_SPRAY_FAILURE));
            }
        } catch (Exception e) {
            throw new HPCCException("Unable to Spray file onto the target cluster", e);
        }
    }

    /**
     * Spraying a CSV file onto a target cluster on the target HPCC System
     * 
     */
    private void sprayCSVFile(HPCCWsClient connector, String fileName, String logicalFile, String targetCluster, DelimitedDataOptions options)
            throws HPCCException {

        String tempLogicalFile = logicalFile.startsWith(PREFIX) ? logicalFile : PREFIX + logicalFile;
        HPCCFileSprayClient fileSprayClient = connector.getFileSprayClient();

        try {
            ProgressResponse response = fileSprayClient.sprayVariableLocalDropZone(options, fileName, tempLogicalFile, "", targetCluster, true,
                    SprayVariableFormat.DFUff_csv);
            boolean isSprayed = handleSprayResponse(response);

            if (!isSprayed) {
                throw new HPCCException(Labels.getLabel(FILE_SPRAY_FAILURE));
            }
        } catch (Exception e) {
            throw new HPCCException("Unable to Spray file onto a target cluster", e);
        }
    }

    private boolean addStrucutureToTHOR(HPCCConnection hpccConnection, List<FileStructure> fields, String logicalFile)
            throws HPCCException, DSPException {
        HPCCWsClient hpccConnector = hpccConnection.getPlatform().getHPCCWSClient();
        StringBuilder ecl = new StringBuilder();

        // Record Structure
        ecl.append(STRUCT).append(RECORD);
        fields.forEach(field -> ecl.append(field.getAsString()).append("; "));
        ecl.append("END").append("; ");

        ecl.append("\n");

        ecl.append("ds := DATASET('").append(logicalFile).append("thor").append("', struct,THOR); \n");
        ecl.append("OUTPUT( ds,,'").append(logicalFile).append(CLUSTER).append(getTargetcluster(hpccConnector)).append(OVERWRITE);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ECL to re write THOR \n{}", ecl.toString());
        }

        return executeECL(hpccConnection, ecl.toString());
    }

    private boolean deleteFile(String file) throws HPCCException, DSPException {
        StringBuilder ecl = new StringBuilder();
        ecl.append("import STD; ").append("STD.File.DeleteLogicalFile('").append(file).append("');");

        LOGGER.debug("Deleting file - {}", ecl.toString());

        return executeECL(hpccConnection, ecl.toString());
    }

    private boolean convertToTHOR(HPCCConnection hpccConnection, List<FileStructure> fields, String tempraryFileName, String logicalFile,
            boolean isXML) throws HPCCException, DSPException {
        HPCCWsClient hpccConnector = hpccConnection.getPlatform().getHPCCWSClient();
        StringBuilder ecl = new StringBuilder();

        // Record Structure
        ecl.append(STRUCT).append(RECORD);
        fields.forEach(field -> ecl.append(field.getAsString()).append("; "));
        ecl.append("END").append("; ");

        ecl.append("\n");

        if (isXML) {
            ecl.append(DS_DATASET).append(tempraryFileName).append("',struct,XML('").append(sprayConfiguration.getRootTag()).append("')); \n");
        } else {
            ecl.append(DS_DATASET).append(tempraryFileName).append("',struct,CSV(HEADING(1))); \n");
        }
        ecl.append("OUTPUT( DS,,'").append(logicalFile).append(CLUSTER).append(getTargetcluster(hpccConnector)).append(OVERWRITE);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ECL to convert to THOR \n{}", ecl.toString());
        }
        return executeECL(hpccConnection, ecl.toString());
    }

    private boolean executeECL(HPCCConnection hpccConnection, String ecl) throws HPCCException, DSPException {
        try {
            WorkunitInfo wu = new WorkunitInfo();
            wu.setECL(ecl);
            wu.setJobname("DSP_" + sprayConfiguration.getUserId() + "_convert_to_thor");
            wu.setCluster(hpccConnection.getThorCluster());
            wu.setResultLimit(100);
            wu.setMaxMonitorMillis(180000);
            LOGGER.debug("File spray Retries value is - {}", sprayConfiguration.getSprayRetryCount());
            String results = executeECLProgress(hpccConnection,wu,sprayConfiguration.getSprayRetryCount());

            LOGGER.debug("Results- {}", results);
            if (!StringUtils.isEmpty(results) && results.contains("Timed out")) {
                String workunit = results.substring(results.indexOf("<Message>") + 48, results.indexOf("</Message>"));
                LOGGER.debug("Results - {}", workunit);
                throw new HPCCException(Labels.getLabel("sprayTimeout1") + hpccConnection.getESPUrl() + Labels.getLabel("sprayTimeout2") + workunit
                        + Labels.getLabel("sprayTimeout3") + workunit + Labels.getLabel("sprayTimeout4"));
            }
        } catch (Exception e) {
            throw new HPCCException(e);
        }
        return true;
    }

    private String executeECLProgress(HPCCConnection hpccConnection, WorkunitInfo wu, int sprayRetryCount) throws DSPException {
        int count=sprayRetryCount;
        try {
            return hpccConnection.getPlatform().getHPCCWSClient().submitECLandGetResults(wu);

        } catch (Exception e) {
            try {
                Thread.sleep(20000);
            } catch (InterruptedException ie) {
                LOGGER.debug("InterruptedException - {}", ie);
            }
            if (count < 6) {
                count++;
                LOGGER.debug("Retries the file spraying");
                executeECLProgress(hpccConnection, wu, count);
            }
            LOGGER.debug("Reaches maximum retries, file spraying failed - {}", e);
            throw new DSPException("Spray failed. HPCC cluster is busy. Please try again later");
        }
    }

    public List<FileMeta> getFileList(String scope, HPCCConnection hpccConnection) throws HPCCException {

        List<FileMeta> results = new ArrayList<FileMeta>();

        List<HPCCLogicalFile> resultsArray;
        try {
            resultsArray = hpccConnection.getFilenames(scope, hpccConnection.getThorCluster());
            FileMeta fileMeta;
            for (HPCCLogicalFile hpccLogicalFile : resultsArray) {
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
            throw new HPCCException(Labels.getLabel("unableToFetchFileList"), e);
        }

        return results;
    }

    private FileMeta settingScope(String scope, FileMeta fileMeta, HPCCLogicalFile hpccLogicalFile) {
        if (scope.length() > 0) {
            fileMeta.setScope(scope + "::" + hpccLogicalFile.getFileName());
        } else {
            fileMeta.setScope(PREFIX + hpccLogicalFile.getFileName());
        }
        return fileMeta;
    }

    private boolean handleSprayResponse(ProgressResponse sprayResponse) throws HPCCException {
        LOGGER.debug("Spray DFU Workunit - {} ; State - {}", sprayResponse.getWuid(), sprayResponse.getState());

        boolean success = false;

        HPCCFileSprayClient fileSprayClient = connector.getFileSprayClient();

        if (fileSprayClient == null) {
            LOGGER.error(Constants.EXCEPTION, new HPCCException(ERROR_INITIALIZE_FILESPRAY_CLIENT));
        }
        try {
            ProgressRequest dfuprogressparams = new ProgressRequest();
            dfuprogressparams.setWuid(sprayResponse.getWuid());
            ProgressResponse progressResponse = fileSprayClient.getSoapProxy().getDFUProgress(dfuprogressparams);
            if (progressResponse.getExceptions() != null) {
                LOGGER.error(Constants.EXCEPTION, new HPCCException("Spray progress status fetch failed."));
            } else {
                if (!progressResponse.getState().equalsIgnoreCase(FAILED)) {
                    // this should be in a dedicated thread.
                    while (progressResponse.getPercentDone() < 100 && !progressResponse.getState().equalsIgnoreCase(FAILED)) {
                        // progress bar value increasing based on percent done.
                        postProgress(40 + (progressResponse.getPercentDone() * (58 / 100)), SPRAYING);
                        progressResponse = fileSprayClient.getSoapProxy().getDFUProgress(dfuprogressparams);
                        Thread.sleep(100);
                    }

                    if (!progressResponse.getState().equalsIgnoreCase(FAILED)) {
                        success = true;
                    }
                }
            }
        } catch (Exception e) {
            throw new HPCCException("Error occured while spraying the file in HPCC cluster", e);
        }
        return success;
    }

    /**
     * Spray a fixed record length data file onto a target cluster on the target
     * HPCC System
     * 
     * @param connector
     *            - Service specific client class
     * @param fileName
     *            - The existing file (on the target HPCC System) to spray
     * @param targetFileLabel
     *            - The full label the sprayed file will be assigned
     * @param recordSize
     *            - The record length
     * @param targetCluster
     *            - The cluster on which to spray
     * @param overwritesprayedfile
     *            - Boolean, overwrite possibly sprayed file of same name
     * @return - Boolean, success.
     */
    public boolean sprayFlatHPCCFile(HPCCWsClient connector, String fileName, String targetFileLabel, int recordSize, String targetCluster,
            boolean overwritesprayedfile) throws HPCCException {
        boolean success = true;

        try {
            HPCCFileSprayClient fileSprayClient = connector.getFileSprayClient();

            if (fileSprayClient != null) {
                success = handleSprayResponse(
                        fileSprayClient.sprayFixedLocalDropZone(fileName, recordSize, targetFileLabel, "", targetCluster, overwritesprayedfile));
            } else {
                throw new HPCCException(ERROR_INITIALIZE_FILESPRAY_CLIENT);
            }
        } catch (Exception e) {
            throw new HPCCException(ERROR_INITIALIZE_FILESPRAY_CLIENT, e);
        }

        return success;
    }

    private void schedule(Event event) {
        if (executionDesktop.isAlive()) {
            Executions.schedule(executionDesktop, eventListener, event);
        } else {
            LOGGER.warn("Desktop unavailable to shedule spray operation");
        }
    }

}
