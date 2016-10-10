package org.hpccsystems.dsp.admin.controller;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.hpccsystems.dsp.ramps.utils.LoggingUtils;
import org.hpccsystems.dsp.ramps.utils.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Textbox;

public class AppLogController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AppLogController.class);
    private static final int BUFFER_SIZE = 1024 * 16;
    private static final String DSP_LOGGER = "org.hpccsystems.dsp";
    private static final String HIPIE_LOGGER ="org.hpcc.HIPIE";

    @Wire
    private Combobox logFileList;

    @Wire
    private Combobox rootCombo;
    @Wire
    private Combobox hipieCombo;
    @Wire
    private Combobox dspCombo;

    @Wire
    private Textbox hipieLabel;
    @Wire
    private Textbox rootLabel;

    @Wire
    private Textbox dspLabel;

    @Wire
    private Textbox noOfLines;

    @Wire
    private Label logContent;
    
    @Wire
    private  Button  saveLevels;
    
    @Wire
    private  Button  editLevels;  
    
    
    final ListModelList<Appender> logFileListModel = new ListModelList<Appender>();
    final ListModelList<Level> rootListModel = new ListModelList<Level>();
    final ListModelList<Level> hipieListModel = new ListModelList<Level>();
    final ListModelList<Level> dspListModel = new ListModelList<Level>();
    List<Level> levels = Arrays.asList(Level.ALL,Level.TRACE,Level.DEBUG,Level.INFO,Level.WARN,Level.ERROR,Level.FATAL,Level.OFF);
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        loadComboBox();
        logFileList.setModel(logFileListModel);
        logFileListModel.addAll(LoggingUtils.getFileAppenders());
        rootLabel.setValue(LogManager.getRootLogger().getLevel().toString());
        dspLabel.setValue(LogManager.getLogger(DSP_LOGGER).getLevel().toString());
        hipieLabel.setValue(LogManager.getLogger(HIPIE_LOGGER).getLevel().toString());
       
        List<Appender> list = null;
        list = new ArrayList<Appender>();
        list.add(logFileListModel.iterator().next());
        logFileListModel.setSelection(list);

        logFileList.setItemRenderer(new ComboitemRenderer<Appender>() {

            @Override
            public void render(Comboitem comboitem, Appender logFileName, int index) throws Exception {
                comboitem.setLabel(logFileName.getName());                
            }
        });
        noOfLines.setValue("20");  
        refreshLog();
    }
    
    @Listen("onClick = #saveLevels")
    public void onSaveLevels(){
        Level rootLevel = rootCombo.getSelectedItem().getValue();
        Level dspLevel = dspCombo.getSelectedItem().getValue();
        Level hipielLevel = hipieCombo.getSelectedItem().getValue();
        LogManager.getRootLogger().setLevel(rootLevel);
        LogManager.getLogger(DSP_LOGGER).setLevel(dspLevel);
        LogManager.getLogger(HIPIE_LOGGER).setLevel(hipielLevel);
        
        toggleComboBox(false, false, false);
        toggleTextBox(true, true, true);
        
        rootLabel.setValue(rootLevel.toString());
        dspLabel.setValue(dspLevel.toString());
        hipieLabel.setValue(hipielLevel.toString());
        toggleSaveEdit(false,true);
    }
    
    @Listen("onClick = #editLevels")
    public void onEditLevels(){
        toggleComboBox(true, true, true);
        toggleTextBox(false, false, false);
      
        dspCombo.setValue(LogManager.getLogger(DSP_LOGGER).getLevel().toString());
        hipieCombo.setValue(LogManager.getLogger(HIPIE_LOGGER).getLevel().toString());
        rootCombo.setValue(LogManager.getRootLogger().getLevel().toString());
       
       
        toggleSaveEdit(true,false);
    }
    
    private  void toggleComboBox(boolean rootState ,boolean dspState, boolean hipieState){
        rootCombo.setVisible(rootState);
        dspCombo.setVisible(dspState);
        hipieCombo.setVisible(hipieState);
    }
    
    private void toggleTextBox(boolean rootlabel, boolean dspLabelState, boolean hipieLabelState) {
        rootLabel.setVisible(rootlabel);
        dspLabel.setVisible(dspLabelState);
        hipieLabel.setVisible(hipieLabelState);
    }

    private void toggleSaveEdit(boolean editState, boolean saveState) {
        saveLevels.setDisabled(saveState);
        editLevels.setDisabled(editState);
    }
    
    private void loadComboBox(){
        rootListModel.addAll(levels);
        hipieListModel.addAll(levels);
        dspListModel.addAll(levels);
        rootCombo.setModel(rootListModel);
        hipieCombo.setModel(hipieListModel);
        dspCombo.setModel(dspListModel);
       
    }
    
    @Listen("onClick = #refresh; onClick = #logview")
    public void refreshLog() {
        String logFileName = logFileListModel.getSelection().iterator().next().getName();
        int lines = 20;
        String nolines = noOfLines.getValue();
        nolines = nolines.replaceAll("[\\D]", "");
        if (logFileName != null) {
            try {
                if (nolines.trim().length() > 0) {
                    lines = Integer.parseInt(nolines);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Number format 'lines' parameter invalid = ", nolines);
            }
            FileAppender fileAppender = (FileAppender) LoggingUtils.getFileAppender(logFileName);
            if (fileAppender != null && lines != 0) {
                try {
                    RandomAccessFile inputFile = new RandomAccessFile(fileAppender.getFile(), "r");
                    StringBuilder output = new StringBuilder();
                    StreamUtils.tailFile(inputFile, output, BUFFER_SIZE, lines);
                    logContent.setValue(output.toString());
                    inputFile.close();
                } catch (IOException e) {
                    LOGGER.error("Error getting the file appender=", fileAppender.getFile(), e);
                }
            } else {
                LOGGER.error("FileAppender with name=" + logFileName + " not exist or lines to be retrieved value is equal to zero ");
            }
        } else {
            LOGGER.error("No appender name parameter specified.");
        }

    }
    
   
}
