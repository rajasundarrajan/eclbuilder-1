package org.hpccsystems.dsp.ramps.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.Constants.EVENTS;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.controller.WidgetConfig;
import org.hpccsystems.dsp.exceptions.DSPException;
import org.hpccsystems.dsp.init.DSPExecutorHolder;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.component.renderer.LegacyPreviewRenderer;
import org.hpccsystems.dsp.ramps.controller.entity.SprayProgress;
import org.hpccsystems.dsp.ramps.controller.entity.TabData;
import org.hpccsystems.dsp.ramps.controller.utils.FileSprayer;
import org.hpccsystems.dsp.ramps.entity.FileStructure;
import org.hpccsystems.dsp.ramps.entity.FileStructure.Type;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.SprayConfiguration;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.ws.client.HPCCFileSprayClient.SprayVariableFormat;
import org.hpccsystems.ws.client.utils.DelimitedDataOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.io.Files;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Include;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vlayout;
import org.zkoss.zul.Window;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ImportFileController extends SelectorComposer<Component> implements EventListener<Event> {

    /**
     * Minimum buffer size to read in kb. This will be multiplied by 10 for subsequent reads 
     */
    private static final int BUFFER_SIZE = 10;
    
    /**
     * Minimum number of records to show in preview 
     */
    private static final int NO_OF_RECORDS = 10;
    
    private static final String SPECIFY_DELIMITORS = "specifyDelimitors";
    private static final String NO_PREVIEW_TO_SHOW = "noPreviewToShow";
    private static final String INCORRECT_COL_SIZE = "incorrectColSize";
    private static final String FILE_SPRAY_SUCCESS = "fileSpraySuccess";
    private static final String FILE_SPRAY_FAILURE = "fileSprayFailure";
    private static final String UPLOAD_FILE = "uploadFile";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportFileController.class);

    private static final String OTHER = "other";

    @Wire
    private Radiogroup fileType;

    @Wire
    private Row csvRow;

    @Wire
    private Row flatRow;

    @Wire
    private Row xmlRow;

    @Wire
    private Button upload;
    @Wire
    private Label uploadLabel;

    @Wire
    private Textbox customFieldSpeperator;
    @Wire
    private Textbox customRecordSpeperator;
    @Wire
    private Textbox customEncloseCharacter;
    @Wire
    private Textbox customEscapeCharacter;

    @Wire
    private Grid previewGrid;
    private ListModelList<List<String>> previewModel;
    private List<String> strippedHeaders;

    @Wire
    private Label fileContents;

    @Wire
    private Radiogroup fieldRadiogroup;

    @Wire
    private Radiogroup recordRadiogroup;
    
    @Wire
    private Radiogroup encloseCharacterRadiogroup;

    @Wire
    private Radiogroup escapeCharacterRadiogroup;
    
    @Wire
    private Textbox logicalFile;

    @Wire
    private Tabbox previewTabbox;

    @Wire
    private Label baseScope;

    @Wire
    private Hbox controlsContainer;

    @Wire
    private Vlayout progressbarContainer;
    @Wire
    private Label progressStatus;
    @Wire
    private Grid fixedFileTable;
    @Wire
    private Intbox recordLengthBox;
    @Wire
    private Intbox noofColumnBox;
    @Wire
    private Textbox rootTag;
    @Wire
    private Textbox sprayrootTag;
    @Wire
    private Combobox sourceFormat;
    @Wire
    private Checkbox hasHeader;
    @Wire
    private Column xpathColumn;
    @Wire
    private Column deleteColumn;
    @Wire
    private Foot addFoot;

    @Wire
    private Checkbox keepOriginalCheck;

    @Wire
    private Tab previewTab;

    @Wire
    private Tab configTab;

    private Progressmeter progressmeter;


    // These default values are selected in UI as well
    private String fieldSeperator = ",";
    private String recordSeperator = "\n";
    private String fieldEncloseCharacter = "'";
    private String escapeCharacter = "\\";

    private int recordLength;
    private File file;
    private boolean dashFlow;
    private String rootTagValue;

    private String logicalDir;
    private List<String> headerRow;
    private DashboardConfig dashboardConfig;
    private WidgetConfig widgetConfig;
    private HPCCConnection hpccConnection;

    @WireVariable
    private Desktop desktop;

    private ListModelList<FileStructure> fixedFileRowModel = new ListModelList<FileStructure>();
    private ListModelList<SprayVariableFormat> fileFormats = new ListModelList<SprayVariableFormat>();

    private TabData data;
    private Project project;

    private ListModelList<Type> fileTypeModel = new ListModelList<FileStructure.Type>();

    public ListModelList<FileStructure> getFixedFileRowModel() {
        return fixedFileRowModel;
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        // Filling up file type list
        fileTypeModel.addAll(FileStructure.getSprayTypes());

        // Enabling server push to handle threads
        desktop.enableServerPush(true);

        dashboardConfig = (DashboardConfig) Executions.getCurrent().getArg().get(Constants.DASHBOARD_CONFIG);
        if (dashboardConfig == null) {
            dashFlow = false;
            data = (TabData) Executions.getCurrent().getArg().get(Constants.TAB_DATA);
            project = data.getProject();
            hpccConnection = project.getHpccConnection();
        } else {
            dashFlow = true;
            hpccConnection = dashboardConfig.getDashboard().getHpccConnection();
            widgetConfig = (WidgetConfig) Executions.getCurrent().getArg().get(Dashboard.WIDGET_CONFIG);
            Events.postEvent(org.hpccsystems.dsp.dashboard.Dashboard.EVENTS.ON_CLICK_IMPORT, widgetConfig.getWidgetCanvas(), null);
        }
        logicalDir = (String) Executions.getCurrent().getArg().get(Constants.FILE);

        previewModel = new ListModelList<List<String>>();
        previewGrid.setModel(previewModel);
        previewGrid.setRowRenderer(new LegacyPreviewRenderer());

        if (logicalDir != null) {
            logicalFile.setValue(logicalDir + "::");
        }
        if (!dashFlow && project.getReferenceId() != null) {
            baseScope.setVisible(true);
            baseScope.setValue("~" + project.getBaseScope() + "::");
        }else if(dashFlow && dashboardConfig.getDashboard().getReferenceId() != null ){
            baseScope.setVisible(true);
            baseScope.setValue("~" + dashboardConfig.getDashboard().getBaseScope() + "::");
            
        }

        // Disabling selection on preview tab

        fileFormats.addAll(Arrays.asList(SprayVariableFormat.values()));
        sourceFormat.setModel(fileFormats);
        fileFormats.addToSelection(SprayVariableFormat.DFUff_utf8);

        hasHeader.addEventListener(Events.ON_CHECK, (SerializableEventListener<? extends Event>)event -> renderHeaderFields());
        if(dashFlow){
            this.getSelf().addEventListener(Events.ON_CLOSE, (SerializableEventListener<? extends Event>)event -> Events
                    .postEvent(org.hpccsystems.dsp.dashboard.Dashboard.EVENTS.ON_CLICK_IMPORT_CLOSE, widgetConfig.getWidgetCanvas(), null));
        }
     
    }

    private void renderHeaderFields() {
        fixedFileRowModel.clear();

        for (String st : strippedHeaders) {
            FileStructure fileRow = new FileStructure();
            if (hasHeader.isChecked()) {
                fileRow.setColumnName(st);
            } else {
                fileRow.setColumnName("");
            }

            fixedFileRowModel.add(fileRow);
        }
    }

    @Listen("onUpload = #upload")
    public void upload(UploadEvent event) {
        Media media = event.getMedia();

        // Deleting existing file
        if (file != null) {
            deleteTemporaryFile();
        }

        file = new File(System.getProperty("java.io.tmpdir") + media.getName());

        try {
            Files.copy(file, media.getStreamData());
        } catch (IOException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            notifyFieldError(Labels.getLabel("uploadFailed"), event.getTarget());
            return;
        }

        fileContents.setValue(new String(readBytes(10)));

        uploadLabel.setValue(media.getName());

        // Clearing previews & hiding options. Specifically for uploading a file
        // second time
        clearPreview();
        fixedFileRowModel.clear();
        fileType.setSelectedItem(null);
        csvRow.setVisible(false);
        flatRow.setVisible(false);
        xmlRow.setVisible(false);
    }

    private byte[] readBytes(int kb) {
        FileInputStream is = null;
        int size = kb * 1024;
        byte[] content = new byte[size];
        
        try {
            is = new FileInputStream(file);
            is.read(content);
        } catch (FileNotFoundException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.error(Constants.EXCEPTION, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception ex) {
                LOGGER.error(Constants.EXCEPTION, ex);
            }
        }

        return content;
    }

    @Listen("onClick = #add")
    public void add() {
        fixedFileRowModel.add(new FileStructure());
    }

    @Listen("onCheck = #fileType")
    public void showFileOptions() {
        if (file == null) {
            notifyFieldError(Labels.getLabel(UPLOAD_FILE), upload);
            fileType.setSelectedItem(null);
            return;
        }

        switch (getSelectedFileType()) {

        case CSV:
            toggleSettingsForCSV();
            previewGrid.setEmptyMessage(Labels.getLabel(NO_PREVIEW_TO_SHOW));
            break;
        case FLAT:
            toggleSettingsForFLAT();
            previewGrid.setEmptyMessage(Labels.getLabel(NO_PREVIEW_TO_SHOW));
            break;
        case XML:
            toggleSettingsForXML();
            previewGrid.setEmptyMessage(Labels.getLabel("xmlFilesHaveNoPreview"));

            break;
        default:
            break;
        }
        previewTab.setStyle("");
        createPreview(false);
        fixedFileTable.invalidate();
    }

    private void toggleSettingsForXML() {
        rootTag.setValue(null);
        sprayrootTag.setValue(null);
        fixedFileRowModel.clear();
        addFoot.setVisible(true);
        deleteColumn.setVisible(true);
        csvRow.setVisible(false);
        flatRow.setVisible(false);
        xmlRow.setVisible(true);
        xpathColumn.setVisible(true);
        xmlRow.setFocus(true);
    }

    private void toggleSettingsForFLAT() {
        recordLengthBox.setValue(null);
        noofColumnBox.setValue(null);
        fixedFileRowModel.clear();
        addFoot.setVisible(false);
        deleteColumn.setVisible(false);
        xpathColumn.setVisible(false);
        csvRow.setVisible(false);
        xmlRow.setVisible(false);
        flatRow.setVisible(true);
        flatRow.setFocus(true);
    }

    private void toggleSettingsForCSV() {
        fixedFileRowModel.clear();
        addFoot.setVisible(false);
        deleteColumn.setVisible(false);
        xpathColumn.setVisible(false);
        csvRow.setVisible(true);
        flatRow.setVisible(false);
        xmlRow.setVisible(false);
        csvRow.setFocus(true);
        hasHeader.setChecked(false);
    }

    public boolean isXMLfile() {
        return getSelectedFileType() == Type.XML;
    }

    private Type getSelectedFileType() {
        return fileTypeModel.getSelection().isEmpty() ? null : fileTypeModel.getSelection().iterator().next();
    }

    @Listen("onCheck = #fieldRadiogroup")
    public void updateFieldDelimiter() {
        updateFieldDelimiter(false);
        if (OTHER.equals(fieldRadiogroup.getSelectedItem().getValue())) {
            fieldSeperator = null;
            customFieldSpeperator.focus();
        }
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter!=null && escapeCharacter!=null) {
            createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
            if (hasHeader.isChecked()) {
                renderHeaderFields();
            }
        }
    }

    private void updateFieldDelimiter(boolean includeCustomValue) {
        if (OTHER.equals(fieldRadiogroup.getSelectedItem().getValue())) {
            customFieldSpeperator.setVisible(true);
            if (includeCustomValue) {
                fieldSeperator = customFieldSpeperator.getValue();
            }
        } else {
            customFieldSpeperator.setVisible(false);
            fieldSeperator = fieldRadiogroup.getSelectedItem().getValue().toString();
        }
    }

    @Listen("onCheck = #recordRadiogroup")
    public void updateRecordDelimiter() {
        updateRecordDelimiter(false);
        if (OTHER.equals(recordRadiogroup.getSelectedItem().getValue())) {
            customRecordSpeperator.focus();
            recordSeperator = null;
        }
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter!=null && escapeCharacter!=null) {
            createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
            if (hasHeader.isChecked()) {
                renderHeaderFields();
            }
        }
    }
    
    private void updateRecordDelimiter(boolean includeCustomValue) {
        if (OTHER.equals(recordRadiogroup.getSelectedItem().getValue())) {
            customRecordSpeperator.setVisible(true);
            if (includeCustomValue) {
                recordSeperator = customRecordSpeperator.getText();
            }
        } else {
            customRecordSpeperator.setVisible(false);
            recordSeperator = recordRadiogroup.getSelectedItem().getValue().toString();
        }
    }
    
    @Listen("onCheck = #encloseCharacterRadiogroup")
    public void updateEncloseCharacter() {
        updateEncloseCharacter(false);
        if (OTHER.equals(encloseCharacterRadiogroup.getSelectedItem().getValue())) {
            fieldEncloseCharacter = null;
            customEncloseCharacter.focus();
        }
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter!=null && escapeCharacter!=null) {
            createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
            if (hasHeader.isChecked()) {
                renderHeaderFields();
            }
        }
    }
    
    private void updateEncloseCharacter(boolean includeCustomValue) {
        if (OTHER.equals(encloseCharacterRadiogroup.getSelectedItem().getValue())) {
            customEncloseCharacter.setVisible(true);
            if (includeCustomValue) {
                fieldEncloseCharacter = customEncloseCharacter.getText();
            }
        } else {
            customEncloseCharacter.setVisible(false);
            fieldEncloseCharacter = encloseCharacterRadiogroup.getSelectedItem().getValue().toString();
        }
    }
    
    @Listen("onCheck = #escapeCharacterRadiogroup")
    public void updateEscapeCharacter() {
        updateEscapeCharacter(false);
        if (OTHER.equals(escapeCharacterRadiogroup.getSelectedItem().getValue())) {
            escapeCharacter = null;
            customEscapeCharacter.focus();
        }
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter!=null && escapeCharacter!=null) {
            createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
            if (hasHeader.isChecked()) {
                renderHeaderFields();
            }
        }
    }
    
    private void updateEscapeCharacter(boolean includeCustomValue) {
        if (OTHER.equals(escapeCharacterRadiogroup.getSelectedItem().getValue())) {
            customEscapeCharacter.setVisible(true);
            if (includeCustomValue) {
                escapeCharacter = customEscapeCharacter.getText();
            }
        } else {
            customEscapeCharacter.setVisible(false);
            escapeCharacter = escapeCharacterRadiogroup.getSelectedItem().getValue().toString();
        }
    }
    
    @Listen("onClick = #previewTab")
    public void showPreviewTab(Event event) {
        boolean isConfigured = createPreview();
        if(isConfigured) {
            previewTab.setSelected(true);
        }
    }

    @Listen("onSelect = #contentsTab, #configTab")
    public void disablePreviewTabSelection() {
        LOGGER.debug("Seleting other tabs");
        previewTab.setZclass("z-tab");
    }

    public boolean createPreview() {
        boolean isConfigured = validateConfig();
        if (isConfigured) {
            createPreview(false);
        }
        return isConfigured;
    }

    private void createPreview(boolean includeCustomDelimiters) {
        clearPreview();

        List<List<String>> previewData1 = null;
        switch (getSelectedFileType()) {
        case CSV:
            previewData1 = createPreviewForCSV(includeCustomDelimiters);
            break;

        case FLAT:
            if (recordLength > 0) {
                previewData1 = createFLATPreview();
            } else {
                return;
            }
            break;

        case XML:
            LOGGER.debug("File type XML has been selected");
            return;

        default:
            break;
        }

        createPreviewHeader(previewData1);
        previewModel.addAll(previewData1);
    }

    private List<List<String>> createPreviewForCSV(boolean includeCustomDelimiters) {
        List<List<String>> previewData1 = null;
        updateFieldDelimiter(includeCustomDelimiters);
        updateRecordDelimiter(includeCustomDelimiters);
        updateEncloseCharacter(includeCustomDelimiters);
        updateEscapeCharacter(includeCustomDelimiters);
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter != null && escapeCharacter != null) {
            previewData1 = createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
        }
        return previewData1;
    }

    private void createPreviewHeader(List<List<String>> previewData1) {
        if (previewData1.size() > 1) {
            headerRow = previewData1.iterator().next();
            // Removing spaces from header
            List<String> list = new ArrayList<String>();
            headerRow.forEach(str -> list.add(StringUtils.trim(str)));
            headerRow = list;

            LegacyPreviewRenderer.setColumnsInGrid(headerRow, previewGrid);
            previewData1.remove(0);
        }

    }

    @Listen("onChanging = #customFieldSpeperator, #customRecordSpeperator, #customEncloseCharacter, #customEscapeCharacter")
    public void updateCustomDelimiters(InputEvent event) {
        String value = event.getValue();
        LOGGER.debug("Changing listerner - id {}, Value {}", event.getTarget().getId(), value);
        if (customFieldSpeperator.getId().equals(event.getTarget().getId())) {
            fieldSeperator = value;
        } else if (customRecordSpeperator.getId().equals(event.getTarget().getId())) {
            recordSeperator = value;
        } else if (customEncloseCharacter.getId().equals(event.getTarget().getId())) {
            fieldEncloseCharacter = value;
        } else if (customEscapeCharacter.getId().equals(event.getTarget().getId())) {
            escapeCharacter = value;
        }
        if (fieldSeperator != null && recordSeperator != null && fieldEncloseCharacter != null && escapeCharacter != null) {
            createCSVPreview(fieldSeperator, recordSeperator, escapeCharacter, fieldEncloseCharacter, BUFFER_SIZE);
            if (hasHeader.isChecked()) {
                renderHeaderFields();
            }
        }

    }

    private void clearPreview() {
        previewTabbox.invalidate();
        previewModel.clear();
        if (previewGrid.getColumns() != null) {
            previewGrid.getColumns().getChildren().clear();
        }
    }

    @Listen("onChanging = #recordLengthBox")
    public void changeRecordLengthBox(InputEvent event) {
        if (event.getValue() != null && !event.getValue().isEmpty() && RampsUtil.isInteger(event.getValue())) {
            recordLength = Integer.parseInt(event.getValue());
        } else {
            recordLength = 0;
        }
    }

    private List<List<String>> createFLATPreview() {
        List<List<String>> dataList = new ArrayList<List<String>>();
        dataList.add(createHeader());
        
        //Reading Data for 10 records
        byte[] previewData = readBytes(NO_OF_RECORDS * recordLength / 1024);
        
        int extraByteSize = previewData.length % recordLength;
        int size = previewData.length;
        int fullSize = size - extraByteSize;
        byte[] newpreviewData = new byte[fullSize];
        newpreviewData = previewData;

        List<String> row;
        String lineContent = null;
        int startIndex = 0;
        int endIndex = 0;
        try {
            for (int i = 0; i < fullSize - recordLength + 1; i += recordLength) {
                lineContent = new String(Arrays.copyOfRange(newpreviewData, i, i + recordLength));
                row = new ArrayList<String>();
                startIndex = 0;
                endIndex = 0;
                for (FileStructure fileStruct : fixedFileRowModel.getInnerList()) {
                    endIndex = startIndex + fileStruct.getColumnSize();
                    row.add(lineContent.substring(startIndex, endIndex));
                    startIndex = endIndex;
                }
                if (dataList.size() < 51) {
                    dataList.add(row);
                } else {
                    break;
                }

            }
        } catch (StringIndexOutOfBoundsException ex) {
            notifyFieldError(Labels.getLabel(INCORRECT_COL_SIZE), fixedFileTable);
            LOGGER.error(Constants.EXCEPTION, ex);
            dataList = new ArrayList<List<String>>();
        }
        return dataList;
    }

    private List<String> createHeader() {
        List<String> headerRowList = new ArrayList<String>();
        fixedFileRowModel.getInnerList().forEach(fileStruct -> headerRowList.add(fileStruct.getColumnName()));

        return headerRowList;
    }

    // TODO Implement Quotes
    private List<List<String>> createCSVPreview(String seperator, String terminator, String escape, String quote, int kb) {
        strippedHeaders = new ArrayList<>();
        List<List<String>> data1 = new ArrayList<List<String>>();
        String contents = new String(readBytes(kb));
        LOGGER.debug("field seperator-->{}", seperator);
        LOGGER.debug("record seperator-->{}", terminator);
        LOGGER.debug("escape-->{}", escape);
        LOGGER.debug("quote-->{}", quote);
        try {
            data1 = csvParser(contents, escape, quote, terminator, seperator);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("unableToParseCsv"), Clients.NOTIFICATION_TYPE_WARNING, getSelf(), Constants.POSITION_TOP_CENTER, 5000,
                    true);
            return data1;
        }

        LOGGER.debug("seperated list-->{}", data1);
        strippedHeaders = RampsUtil.legacyStripRowheader(data1.get(0));
        generateRows(data1.get(0).size());
        
        addHeader(data1);
        
        //Truncating the last record as that might contain an incomplete record
        data1.remove(data1.size() - 1);
        
        if(data1.size() >= NO_OF_RECORDS) {
            //Return when set number of records is met
            LOGGER.debug("Returning records of size - {}", kb);
            return data1;
        } else {
            if(kb < 1000) {
                LOGGER.debug("Making a recorsieve call with size - {}kb", kb*10);
                // Limiting the maximum read size at 1000kb and trying recursively untill that is reached.
                return createCSVPreview(seperator, terminator, escape, quote, kb*10);
            } else if(data1.size() > 1) {
                // As maximum size is reached, checking whether at-least one record is present for Header calculations
                LOGGER.debug("Maximum size reached with header and less number of records");
                return data1;
            } else {
                data1.clear();
                LOGGER.debug("Size exceeded");
                Clients.showNotification(Labels.getLabel("tooLargeFile"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 5000, true);
                return data1;
            }
        }
    }

    private void addHeader(List<List<String>> data1) {
        if(!hasHeader.isChecked()) {
            data1.add(0, createHeader());
        }
    }

    private List<List<String>> csvParser(String stringToSplit, String escape, String quotes, String terminator, String seperator)
            throws DSPException {
        List<List<String>> parsedList = new ArrayList<List<String>>();
        String lookBehind = "(?<!" + getSplitterKey(escape) + ")(?=" + getSplitterKey(quotes) + ")";
        String replaceSearch = getSplitterKey(escape) + getSplitterKey(quotes);

        String[] fin = stringToSplit.split(lookBehind);
        /**
         *  splitting quoted characters - This may lead in to performance and memory consumption issues,
         *  if all the fields in a huge volume of file is wrapped -needs to be tweaked
         */
        for (int i = 0; i < fin.length; i++) {
            if (i != fin.length - 1 && fin[i].startsWith(quotes) && fin[i + 1].startsWith(quotes)) {
                fin[i] = fin[i].concat(quotes);
                fin[i + 1] = fin[i + 1].substring(1, fin[i + 1].length());
            }
        }
        List<String> list = new ArrayList<String>(Arrays.asList(fin));
        // removing empty elements
        list.removeAll(Arrays.asList(""));

        boolean wasLastQuoted = false;
        // generating preview list for first 50 records
        boolean reachedFifty = false;
        for (int i = 0; i < list.size() && !reachedFifty; i++) {
            if (!(list.get(i).startsWith(quotes) && list.get(i).endsWith(quotes))) {

                // splitting lines into list for each not quoted element
                List<String> lineBroke = Arrays.asList(list.get(i).split(getSplitterKey(terminator), -1));
                for (String line : lineBroke) {
                    String[] fieldSplitArray = line.split(getSplitterKey(seperator), -1);
                    // removing escape character
                    for (int x = 0; x < fieldSplitArray.length; x++) {
                        fieldSplitArray[x].replaceAll(replaceSearch, quotes);
                    }
                    List<String> fieldBroke;
                    // concating with previous element if it was quoted
                    if (wasLastQuoted) {
                        wasLastQuoted = false;
                        fieldBroke = parsedList.get(parsedList.size() - 1);
                        fieldBroke.add(fieldBroke.size() - 1, fieldBroke.get(fieldBroke.size() - 1).concat(fieldSplitArray[0]));
                        fieldBroke.remove(fieldBroke.size() - 1);
                        for (int x = 1; x < fieldSplitArray.length; x++) {
                            fieldBroke.add(fieldSplitArray[x]);
                        }
                    } else {
                        if (parsedList.size() >= 50) {
                            reachedFifty = true;
                            break;
                        }
                        fieldBroke = new ArrayList<String>();
                        fieldBroke.addAll(Arrays.asList(fieldSplitArray));
                        parsedList.add(fieldBroke);
                    }
                }
            } else {
                List<String> lastList;
                String quotedString;
                if (parsedList.size() - 1 <= -1) {
                    lastList = new ArrayList<String>();
                    parsedList.add(lastList);
                    quotedString = list.get(i);
                } else {
                    lastList = parsedList.get(parsedList.size() - 1);
                    quotedString = lastList.get(lastList.size() - 1).concat(list.get(i));
                }
                // removing quotes
                String quoteEscaped = quotedString.substring(1, quotedString.length() - 1);
                // removing escape character
                String finalString = quoteEscaped.replaceAll(replaceSearch, quotes);
                // concating quoted element with previous element
                if (lastList.size() - 1 <= -1) {
                    lastList.add(lastList.size(), finalString);
                } else {
                    lastList.add(lastList.size() - 1, finalString);
                    lastList.remove(lastList.size() - 1);
                }
                LOGGER.debug("lastlist--->{}", lastList);
                wasLastQuoted = true;
            }
        }

        return parsedList;
    }

    private String getSplitterKey(String key) {
        String splitterKey = key;
        LOGGER.debug("key-->{}", key);
        switch (key) {
        case "\\t":
            splitterKey = "\\t";
            break;

        case "\\n":
            splitterKey = "\\n";
            break;
        default:
            LOGGER.debug("using pattern");
            splitterKey = Pattern.quote(key);
            break;
        }
        LOGGER.debug("splitterKey--->{}", splitterKey);
        return splitterKey;
    }

    private void notifyFieldError(String msg, Component component) {
        Clients.showNotification(msg, Clients.NOTIFICATION_TYPE_ERROR, component, Constants.POSITION_END_CENTER, 3000, true);
    }

    @Listen("onClick = #spray")
    public void validateAndSpary() {
        configTab.setSelected(true);
        boolean validationSuccess = validateConfig();

        if (!validationSuccess) {
            return;
        }

        if (validationSuccess && getSelectedFileType() == Type.CSV) {
            if (headerRow != null) {
                initiateSprayThread();
                return;
            } else if (hasHeader.isChecked()) {
                Clients.showNotification(Labels.getLabel("thereIsNoHeaderInYourFile"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(),
                        Constants.POSITION_TOP_CENTER, 3000, true);
                return;
            }
        }

        updateProgress(0, "validateFile");

        disableControls();

        initiateSprayThread();

    }

    private boolean validateConfig() {
        if (file == null) {
            notifyFieldError(Labels.getLabel(UPLOAD_FILE), upload);
            return false;
        } else if (logicalFile.getText() == null || logicalFile.getText().isEmpty() || logicalFile.getText().endsWith("::")) {
            notifyFieldError(Labels.getLabel("provideLogicalfileName"), logicalFile);
            return false;
        }

        Type selectedType = getSelectedFileType();
        if (selectedType == null) {
            notifyFieldError(Labels.getLabel("chooseFileType"), upload);
            return false;
        }
        boolean isCSVparametersPresent = StringUtils.isEmpty(recordSeperator) || StringUtils.isEmpty(fieldSeperator)
                || StringUtils.isEmpty(fieldEncloseCharacter) || StringUtils.isEmpty(escapeCharacter);
        if (selectedType == Type.FLAT) {
            if (recordLengthBox.getValue() == null) {
                notifyFieldError(Labels.getLabel("provideRecordLength"), flatRow);
                return false;
            } else if (!validateFlatFileStructure()) {
                notifyFieldError(Labels.getLabel("missingColInfo"), fixedFileTable);
                return false;
            }
        } else if (selectedType == Type.XML) {
            if(StringUtils.isEmpty(rootTag.getText())) {
                notifyFieldError(Labels.getLabel("provideRootTag"), rootTag);
                return false;
            } else if(StringUtils.isEmpty(sprayrootTag.getText())) {
                notifyFieldError(Labels.getLabel("provideRowTag"), sprayrootTag);
                return false;
            }
        } else if ((selectedType == Type.CSV) && isCSVparametersPresent) {
            notifyFieldError(Labels.getLabel(SPECIFY_DELIMITORS), csvRow);
            return false;
        }

        List<FileStructure> struct = fixedFileRowModel.getInnerList();
        if (struct.isEmpty() || struct.stream().filter(fs -> !fs.isValid()).count() > 0) {
            Clients.showNotification(Labels.getLabel("invalidStructure"), Clients.NOTIFICATION_TYPE_ERROR, fixedFileTable,
                    Constants.POSITION_MIDDLE_CENTER, 3000, true);
            return false;
        }

        return true;
    }

    private void disableControls() {
        controlsContainer.setSclass("disabledTab");
    }

    private void enableControls() {
        controlsContainer.setSclass("");
    }

    private void stripHeadersinFixedFileModel() {
        int count = 0;
        for (FileStructure filestrct : fixedFileRowModel.getInnerList()) {
            String stripped = RampsUtil.removeStartingNumbers(RampsUtil.removeSpaceSplChar(filestrct.getColumnName()));
            filestrct.setColumnName(stripped.length() > 0 ? stripped : "field" + ++count);
        }
    }

    private void initiateSprayThread() {
        updateProgress(5, "startUpload");
        String logicalFileName;
        if (!dashFlow) {
            logicalFileName = project.getReferenceId() != null ? new StringBuilder(project.getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR)
                    .append(logicalFile.getText()).toString() : logicalFile.getText();
        } else {
            logicalFileName = dashboardConfig.getDashboard().getReferenceId() != null ? new StringBuilder(dashboardConfig.getDashboard().getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR)
                    .append(logicalFile.getText()).toString() : logicalFile.getText();
        }

        DelimitedDataOptions delimitedDataOptions = new DelimitedDataOptions("\\n".equals(recordSeperator) ? "\\n,\\r\\n" : recordSeperator,
                fieldSeperator, escapeCharacter, fieldEncloseCharacter);

        SprayConfiguration sprayConfiguration = new SprayConfiguration();
        sprayConfiguration.setFile(file);
        sprayConfiguration.setType(getSelectedFileType());
        sprayConfiguration.setDelimitedOptions(delimitedDataOptions);
        sprayConfiguration.setLogicalFile(logicalFileName);
        sprayConfiguration.setRecordLength(recordLength);
        stripHeadersinFixedFileModel();
        sprayConfiguration.setStructure(fixedFileRowModel.getInnerList());
        sprayConfiguration.setKeepOriginalFile(keepOriginalCheck.isChecked());
        sprayConfiguration.setRootTag(rootTagValue);
        sprayConfiguration.setSprayRootTag(sprayrootTag.getText());
        sprayConfiguration.setXmlFormat(fileFormats.getSelection().iterator().next());

        FileSprayer sprayFile = new FileSprayer(desktop, ImportFileController.this, sprayConfiguration, hpccConnection);
        long startTime = Instant.now().toEpochMilli();
        DSPExecutorHolder.getExecutor().execute(sprayFile);
        if (LOGGER.isDebugEnabled()) {
            ((DBLogger)SpringUtil.getBean(Constants.DB_LOGGER)).log(new HipieQuery(HipieQuery.SPRAY_FILE, startTime, "File sprayed"));
        }
    }

    @Override
    public void onEvent(Event event) throws Exception {
        Window self = (Window) getSelf();
        // when window is closed before the completion of spraying
        if (self.getParent() == null) {
            return;
        }
        self.setSclass("hideminimizebutton");
        if (event.getName().equals(EVENTS.ON_SPRAY_COMPLETE)) {
            updateProgress(100, FILE_SPRAY_SUCCESS);
            Clients.showNotification(Labels.getLabel(FILE_SPRAY_SUCCESS), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER,
                    3000, true);

            // Sending the event, as the file browser will be ready when spray
            // window is closed
            Events.sendEvent(Events.ON_CLOSE, this.getSelf(), null);
            if (!dashFlow) {
                Events.postEvent(EVENTS.ON_SPRAY_COMPLETE, data.getFileBrowserComponent(), event.getData());
            } else {
                Events.postEvent(EVENTS.ON_SPRAY_COMPLETE, dashboardConfig.getFileBrowser(), event.getData());
            }

        } else if (event.getName().equals(EVENTS.ON_SPRAY_PROGRESS)) {
            updateProgress((SprayProgress) event.getData());
        } else {
            updateProgress(0, FILE_SPRAY_FAILURE);
            enableControls();
            Clients.showNotification(event.getData() == null ? Labels.getLabel(FILE_SPRAY_FAILURE) : event.getData().toString(),
                    Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        }
    }

    private void updateProgress(SprayProgress sprayProgress) {
        updateProgress(sprayProgress.getProgress(), sprayProgress.getStatus());
    }

    private void updateProgress(int progress, String statusLabel) {
        if (progressmeter == null) {
            createProgressmeter();
        }
        progressmeter.setValue(progress);
        progressStatus.setValue(Labels.getLabel(statusLabel));
    }

    private void createProgressmeter() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating progress bar..");
        }
        progressmeter = new Progressmeter();
        progressmeter.setWidth("230px");
        progressbarContainer.appendChild(progressmeter);
    }

    @Listen("onClose = #importWindow")
    public void closeImportWindow() {
        deleteTemporaryFile();

        // Clearing include for reload when this window is opened again
        Include fileInclude = (Include) getSelf().getParent();
        fileInclude.setSrc(null);

        if (!dashFlow) {
            Events.postEvent(EVENTS.ON_RETURN_TO_EDIT, data.getProjectDetailComponent(), null);
        } else {
            Events.postEvent(EVENTS.ON_RETURN_TO_EDIT, dashboardConfig.getFileBrowser(), null);
        }
    }

    @Listen("onChange = #noofColumnBox")
    public void updateFixedFileTable() {
        if (recordLengthBox.getValue() != null && noofColumnBox.getValue() != null) {
            generateRows(noofColumnBox.getValue());
        }
    }

    @Listen("onChange = #rootTag")
    public void updateRootTag() {
        if (!StringUtils.isEmpty(rootTag.getValue())) {
            rootTagValue = rootTag.getValue();
        }
    }

    private void generateRows(Integer columns) {
        FileStructure fileRow = null;
        int tableSize = 0;
        if (columns > fixedFileRowModel.getSize()) {
            tableSize = columns - fixedFileRowModel.getSize();
        } else if (columns < fixedFileRowModel.getSize()) {
            tableSize = columns;
            fixedFileRowModel.clear();
        }

        for (int count = 0; count < tableSize; count++) {
            fileRow = new FileStructure();
            fixedFileRowModel.add(fileRow);
        }

    }

    private void deleteTemporaryFile() {
        if (file != null) {

            file.delete();
        }
    }

    @Listen("onNameUpdate = #fixedFileTable")
    public void updateColumnName(ForwardEvent event) {
        InputEvent inputEvent = (InputEvent) event.getOrigin();
        if (!org.apache.commons.lang.StringUtils.isEmpty(inputEvent.getValue())) {
            Row row = (Row) inputEvent.getTarget().getParent();
            FileStructure structure = row.getValue();
            structure.setColumnName(inputEvent.getValue());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Structure---> {}", structure.getColumnName());
            }
        }
    }

    private boolean validateFlatFileStructure() {
        for (FileStructure fileStruct : fixedFileRowModel) {
            if (!fileStruct.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Listen("onColTypeChange = #fixedFileTable")
    public void updateColumnType(ForwardEvent event) {
        @SuppressWarnings("rawtypes")
        SelectEvent selectEvent = (SelectEvent) event.getOrigin();
        Row row = (Row) selectEvent.getTarget().getParent();
        FileStructure structure = row.getValue();
        structure.setColumnType(((Comboitem) selectEvent.getSelectedItems().iterator().next()).getLabel());
        LOGGER.debug("Structure {}", structure.getColumnType());
    }

    @Listen("onDelete = #fixedFileTable")
    public void onDelete(ForwardEvent event) {
        Row row = (Row) event.getOrigin().getTarget().getParent();
        FileStructure structure = row.getValue();
        fixedFileRowModel.remove(structure);
    }

    @Listen("onSizeUpdate = #fixedFileTable ; onXpathUpdate = #fixedFileTable")
    public void updateColumnSize(ForwardEvent event) {
        InputEvent inputEvent = (InputEvent) event.getOrigin();
        if (!org.apache.commons.lang.StringUtils.isEmpty(inputEvent.getValue()) && RampsUtil.isInteger(inputEvent.getValue())) {
            Row row = (Row) inputEvent.getTarget().getParent();
            FileStructure structure = row.getValue();
            if ("onSizeUpdate".equals(event.getName())) {
                structure.setColumnSize(Integer.parseInt(inputEvent.getValue()));
            } else if ("onXpathUpdate".equals(event.getName())) {
                structure.setXpath(inputEvent.getValue());
            }
            LOGGER.debug("Structure - {}", structure.getColumnSize());
        }
    }
    @Listen("onClick = #closeImportDialog")
    public void onCancel(){
        Events.sendEvent(Events.ON_CLOSE, this.getSelf(), null);
    }

    public ListModelList<Type> getFileTypeModel() {
        return fileTypeModel;
    }

    public ListModelList<SprayVariableFormat> getFileFormats() {
        return fileFormats;
    }

}
