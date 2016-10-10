package org.hpccsystems.dsp.dashboard.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.zkoss.util.media.Media;
import org.zkoss.util.resource.Labels;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Button;
import org.zkoss.zul.Grid;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class StaticDataBrowserController extends SelectorComposer<Component> {

    private static final String JSON = "JSON";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StaticDataBrowserController.class);
    
    @Wire
    private Textbox selectedFile;
    
    @Wire
    private Grid optionsGrid;
    
    @Wire
    private Radiogroup type;
    
    @Wire
    private Textbox name;
    @Wire
    private Textbox delimiter;
    @Wire
    private Textbox seperator;
    @Wire
    private Textbox quote;
    @Wire
    private Textbox escape;
    @Wire
    private Button importFile;    
    @Wire
    private Popup uploadPopup;
    @Wire
    private Button deleteFile;
    
    private ListModelList<StaticData> localFiles = new ListModelList<StaticData>();
    private WidgetConfig widgetConfig;
    
    private StaticData originalFile;
    
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        widgetConfig = (WidgetConfig) Executions.getCurrent().getAttribute(Dashboard.WIDGET_CONFIG); 
        
        loadAvailableFiles();  
        
        if(widgetConfig.hasValidStaticData()) {           
            Optional<StaticData> fileOption = localFiles.stream()
                    .filter(staticData -> staticData.getQueryName()
                            .equals(widgetConfig.getWidget().getQueryName()))
                    .findAny();
            if(fileOption.isPresent()){
                addFileToSelection(fileOption.get());
                originalFile = fileOption.get();
            }            
        }
        if(name.getValue().trim().isEmpty()){
  		  	importFile.setDisabled(true);
  	  	}
    }


    @Listen("onUpload = #importFile")
    public void importFile(UploadEvent event) {
        Media media = event.getMedia();
        byte[] byteData = media.getByteData();
        StaticData staticData = null;
        
        boolean fileExists = validateFile(media);
        if(fileExists){
            Clients.showNotification(Labels.getLabel("fileExists"), Clients.NOTIFICATION_TYPE_WARNING, getSelf(), Constants.POSITION_TOP_CENTER, 2000, true);
            return;
        }
        // validate dataset name
        if (!validateDatasetName(name.getText())) {
            return;
        }
        
        try {
            if(!JSON.equals(type.getSelectedItem().getValue())) {
                List<Object> json = Utility.GetCsvStructure(new String(byteData), true, 
                        delimiter.getText().charAt(0), 
                        quote.getText().charAt(0), 
                        escape.getText().charAt(0), seperator.getText());
                
                Map<String, List<Object>> datasets = new LinkedHashMap<String, List<Object>>();
                datasets.put(name.getText(), json);
                byteData = new ObjectMapper().writeValueAsString(datasets).getBytes();
            } else {
                JSONObject jsonTest = (JSONObject) new JSONParser().parse( new String(byteData));
            }
            LOGGER.debug("Converted Json - {}", new ObjectMapper().writeValueAsString(byteData));
            String stringData = new String(byteData);
            staticData = new StaticData(((AuthenticationService) SpringUtil
                            .getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId(),
                            media.getName(), stringData);
            
            //upload file content into Database
            ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).addUpdateStaticData(staticData);
            localFiles.add(staticData);
            addFileToSelection(staticData);
            Clients.showNotification(Labels.getLabel("uploadSuccess"), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        } catch (JsonParseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("malformattedFile") + ": " + e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 0, true);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(Labels.getLabel("uploadFailed"), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
        }        
        uploadPopup.close();
    }

    private boolean validateFile(Media media) {
        AuthenticationService authService = (AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE);
        String userId = authService.getCurrentUser().getId();
        String fileName = FilenameUtils.removeExtension(media.getName());
        Optional<StaticData> dataOption = localFiles.stream()
                .filter(file -> userId.equals(file.getUser())
                        && fileName.equals(file.getFileName()))
                .findAny();
        return dataOption.isPresent();
    }
    
    private boolean validateDatasetName(String name) {
        // no empty names
        if (StringUtils.isEmpty(name)) {
            Clients.showNotification(Labels.getLabel("datasetName"), 
                    Clients.NOTIFICATION_TYPE_ERROR, this.name,
                    Constants.POSITION_START_CENTER, 3000);
            
            return false;
        }
        // name must not start with a number
        if (Character.isDigit(name.charAt(0)) || !name.matches("[A-Za-z0-9]+")) {
            Clients.showNotification(Labels.getLabel("startWithAlphaOnly") + "<br><br>" + 
                    Labels.getLabel("invalidDatasetName"), 
                    Clients.NOTIFICATION_TYPE_ERROR, this.name,
                    Constants.POSITION_START_CENTER, 5000);
            return false;
        }
        return true;
    }

    /**
     * Sets the selected file
     * @param staticFile
     * @throws HipieException
     * @throws IOException
     */
    private void addFileToSelection(StaticData staticFile) throws HipieException, IOException {
        if (staticFile != null) {
            localFiles.addToSelection(staticFile);
            setSelectedFile(staticFile);
        }
    }
    
    /**
     * Sets the text box and the widget config query name with the values passed in
     * @param staticFile
     */
    private void setSelectedFile(StaticData staticFile) {
        if (staticFile != null) {
            selectedFile.setText(staticFile.getFileName());
            widgetConfig.setQueryName(staticFile.getQueryName());
        }
    }
    
    private void updateSelection(StaticData staticFile) {
        if (staticFile != null) {
        setSelectedFile(staticFile);
            if (staticFile == originalFile) {
                widgetConfig.setDatasourceUpdated(false);
            } else {
                widgetConfig.setDatasourceUpdated(true);
            }
        }
    }
    
    private void loadAvailableFiles() {
        String userId = ((AuthenticationService) SpringUtil.getBean(Constants.AUTHENTICATION_SERVICE)).getCurrentUser().getId();
        List<StaticData> availablesFiles;
        try {
            availablesFiles = ((CompositionService)SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).retrieveStaticData(userId);
        } catch (DatabaseException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            Clients.showNotification(e.getMessage(), Clients.NOTIFICATION_TYPE_ERROR, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
            return;
        }
        localFiles.addAll(availablesFiles);
    }
    
    @Listen("onCheck = #type")
    public void changeFileType() {
    	if(name.getValue().trim().isEmpty()){
  		  	importFile.setDisabled(true);
  	  	}
        if(JSON.equals(type.getSelectedItem().getValue())) {
        	optionsGrid.setVisible(false);
        } else {
            optionsGrid.setVisible(true);
        }
    }
    
    @Listen("onChanging = #name")
    public void onChangeDelimitedName(InputEvent evt){
        if(evt.getValue().trim().isEmpty()){
            importFile.setDisabled(true);
        }else{
            importFile.setDisabled(false);
        }
       
    }

    @Listen("onSelect = #fileList")
    public void setSelection() {
        StaticData selection = localFiles.getSelection().iterator().next();
        updateSelection(selection);
    }
    @Listen("onDeleteFile = #fileList")
    public void deleteFile(ForwardEvent forwardEvent) {
        LOGGER.debug("deleteFile click");
        StaticData deleteFileSelection= (StaticData) forwardEvent.getData();
            Messagebox.show(Labels.getLabel("staticFileDeleteMessage"), "Delete StaticFile?",
                    new Messagebox.Button[] { Messagebox.Button.YES, Messagebox.Button.NO }, Messagebox.QUESTION,event -> {
                        if (Messagebox.Button.YES.equals(event.getButton())) {
                            try {
                                ((CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE)).deleteStaticDataFile(deleteFileSelection);
                                localFiles.remove(deleteFileSelection);
                                selectedFile.setText("");
                                Clients.showNotification(Labels.getLabel("staticFileDeleteSuccess"), Clients.NOTIFICATION_TYPE_INFO, getSelf(), Constants.POSITION_TOP_CENTER, 3000, true);
                            } catch (DatabaseException e) {
                                LOGGER.error(Constants.EXCEPTION, e);
                            }
                        }
                    });
    }

    public ListModelList<StaticData> getLocalFiles() {
        return localFiles;
    }
    public String getFileSize(){
        SettingsService settingsService = (SettingsService) SpringUtil.getBean("settingsService"); 
        
        StringBuilder uploadParam = new StringBuilder();
        uploadParam.append("true")
                .append(",maxsize=")
                .append(String.valueOf(settingsService.getStaticDataSize()*1024))
                .append(",multiple=false,native");
        LOGGER.debug("uploadParam --->{}", uploadParam);
        return uploadParam.toString();
    }
}
