package org.hpccsystems.dsp.ramps.controller.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.KelbaseInputElement;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.ramps.entity.Plugin;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.error.ErrorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;

public class HtmlGenerator {
    private static final String RADIO = "radio";
    private static final String GROUP = "group";
    private static final String INPUT_TYPE = "<input type='hidden' name='";
    private static final String DIV = "</div>";
    private static final String VALUE2 = "' value='";
    private static final String CHECKBOX = "checkbox";
    private static final String FOR_SELECTOBJECT = "     for (var i=0; i<selectobject.length; i++){\n";
    private static final String ENTER1 = "     }\n";
    private static final String ENTER = "         }\n";
    private static final String SHOW_HIDE_FIELD = "showhidefield('";
    private static final String SPAN_DIV = "</span></div>";
    private static final String PROPERTY_FORM_DESCRIPTION = "<div id='propertyformdescriptiondiv'><span class='propertyformdescription propertyformdescription";
    private static final String NOFRAME = "noframe";
    private static final String LABEL_DIV = "</label></div>";
    private static final String CHECKED = " checked";
    private static final String VALUE = "value=\"";
    private static final String ONCLICK = "onclick=\"";
    private static final String DIV_ID_FORMITEM = "<div id='formitem";
    private static final String ON_CHANGE = " onChange=\"";
    private static final String DISABLED2 = " disabled ";
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlGenerator.class);
    private static final Object UNDERSCORE = "_";
    private static final String FORM_LINE = "formline";
    
    public static final String CANONICAL_NAME = "canonicalName";
    public static final String PLUGIN_ID = "pluginid";
    public static final String FORM_HOLDER_UUID= "holderFormUuid";
    
    private HtmlGenerator() {
    }

    /**
     * Generates Hipie plugin Form
     */
    public static String generateForm(Project project, Plugin plugin, int pluginId, String holderUUID, boolean showGlobalaVar) throws HipieException {
        StringBuilder html = new StringBuilder(); 
                
        generateHTML(plugin, html, holderUUID, showGlobalaVar);
        
        StringBuilder htmlContent = new StringBuilder();
                
        htmlContent.append("<form action='./pluginsave.do' method='POST' id='")
        .append(generateFormId(project, plugin, pluginId)).append("' onsubmit='return false;'>")
        .append(html.toString())
.append(INPUT_TYPE)
            .append(PLUGIN_ID)
            .append(VALUE2)
            .append(pluginId).append("'/>")
.append(INPUT_TYPE)
            .append(CANONICAL_NAME)
            .append(VALUE2)
            .append(project.getName()).append("'/>")
.append(INPUT_TYPE)
            .append(FORM_HOLDER_UUID)
            .append(VALUE2)
            .append(holderUUID).append("'/>")
        .append("</form>");
        
        return htmlContent.toString();
    }
    
    /**
     * Generates unique ID for each Hipie plugin Form
     */
    public static String generateFormId(Project project, Plugin plugin, int pluginId) {
        StringBuilder idBuilder = new StringBuilder();
        return idBuilder
                   .append(project.getName())
                   .append(plugin.getName())
                   .append(pluginId).toString();
    }
    
    /**
     * Generates HTML elements for a plugin which will be binded into the Hipie form
     */
    public static void generateHTML(Plugin plugin, StringBuilder html,
            String holderUUID, boolean showGlobalaVar) throws HipieException {
        ContractInstance ci = plugin.getContractInstance();
        if (ci.getParent().getInputElements() == null) {
            return;
        }
       
        try {
            html.append("<style>" + InputElement.GetHtmlStyles() + "</style>");
            html.append("<script  type=\"text/javascript\">"  + getJavascript());
            html.append( " function onformload() {\n");
            
            //loads onformload() with onchange() methods- trigger on page load
            for (Element e:ci.getParent().getInputElements()) {
               StringBuilder builder = new StringBuilder();
               builder.append(getOnChange(ci.getContract(),(InputElement)e,holderUUID,true));
                
                if (!StringUtils.isEmpty(builder)) {
                    html.append( builder + "\n");
                }
            }
            html.append( "}\n");
            html.append("</script>");

            for (Element e : ci.getParent().getInputElements()) {
                InputElement ie = (InputElement) e;               
                String inputEleForm = generateInputEleHTML(plugin,ci.getContract(), ci, true,ie, holderUUID,showGlobalaVar);
                html.append(inputEleForm);
            }

            //Adding the error block
            String valdiv = "<div id='errors' class='propertyformerrors'>";
            ErrorBlock eb = new ErrorBlock();
            eb.addAll(ci.validate());
            if (!eb.getErrors().isEmpty()) {
                valdiv = valdiv + eb.toHtmlErrorString();
            }
            valdiv = valdiv + DIV;

            html.append(valdiv);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException(e);
        }
    }    
        
    /**
     * Generates HTML elements corresponding to a Hipie InputElement
     */
    private static String generateInputEleHTML(Plugin plugin, Contract c,
            ContractInstance contractInstance, Boolean includeValidation,
            InputElement inputElement, String holderUUID, boolean showGlobalaVar) throws HipieException {
        Boolean disabled=inputElement.hasOption(InputElement.DISABLED);
        Boolean many=inputElement.hasOption(InputElement.MANY);
        Boolean enableWhen=inputElement.hasOption(InputElement.ENABLE);
        
        String defaultValue = inputElement.getOptionValue(InputElement.DEFAULT, "", "");
        String maxlength=inputElement.getOptionValue(InputElement.MAXLENGTH, "", "");
        String label=inputElement.getOptionValue(InputElement.LABEL, "", "");
        String nullValueDefinedAs=inputElement.getOptionValue(InputElement.NULL, "", "");        
        String fieldlength=inputElement.getFieldLength();
        String rows=inputElement.getOptionValue(InputElement.ROWS, "", "");
        String fieldtype=inputElement.getHTMLType();
        String contractInstanceValue="";
        List<String> contractInstanceValues = new ArrayList<String>();
        try {
        if (contractInstance != null && contractInstance.getProperty(inputElement.getName()) != null) {
            //retrieve the property value WITHOUT replacing global variables for display in property form
            contractInstanceValue=contractInstance.get(inputElement.getName());
            if (!Composition.GetOrigin().equals(contractInstance.getRefSource(inputElement.getName()))) {
                contractInstanceValue=contractInstance.getProperty(inputElement.getName());
            }
            contractInstanceValues=contractInstance.getArray(inputElement.getName());            
        }

        String styleClass = inputElement.getOptionValue(InputElement.STYLECLASS,"","");
        String cols = inputElement.getOptionValue(InputElement.COLS,"","");
        
        //generate the javascript on change string: javascript to update the visibility of any elements affected when this element's value changes
        String onchangestring = getOnChange(c, inputElement, holderUUID, false);
        
        Boolean enabled=inputElement.getEnabled(contractInstance);
        String outputstring = "";
        String elementID = generateHipieElementId(holderUUID,inputElement.getName());
        outputstring="<div id='"+elementID + "' class='propertyformfield" + inputElement.getName() + " propertyformfield" + styleClass ;
        if (enableWhen && !enabled) {
            outputstring=outputstring + " hiddenfield invisibleoption"; 
        }
        outputstring=outputstring + "'>";
        
        String validclass="valid";
        ErrorBlock eb=new ErrorBlock();
        if (contractInstance != null && includeValidation && contractInstance.getArray(inputElement.getName()) != null){
            for (String val:contractInstance.getArray(inputElement.getName())) {
                inputElement.validate(contractInstance, val, eb);               
            }
            if (!eb.getErrors().isEmpty()) {
                validclass="invalid";
            }
        }
        String labeltext="<label class='propertyformlabel propertyformlabel" + fieldtype + " propertyformlabel" + validclass + "'>" + label + "</label>";
        String inputclasses="class='propertyforminput propertyforminput" + fieldtype + " propertyforminput" + validclass + " " + styleClass + "'";
        if ("text".equalsIgnoreCase(fieldtype)) {
            if (!"".equals(label)) {
                outputstring=outputstring + labeltext;
            }
            outputstring = outputstring + " <input " + inputclasses + " id=\"" + inputElement.getName() + "\" name=\"" + inputElement.getName() + "\" type=\"text\" ";
            if (!"".equals(fieldlength)) {
                outputstring = outputstring  + " size=\"" + fieldlength + "\"";
            }
            if (!"".equals(maxlength)) {
                outputstring = outputstring + " maxLength=\""+ maxlength + "\"";
            }
            if (disabled) {
                outputstring = outputstring + DISABLED2;
            }
            if (!"".equals(onchangestring)) {
                  outputstring=outputstring + "onChange=\"" + onchangestring + "\"";                                                     
            }
            
            outputstring = outputstring + inputElement.getHTMLValue(contractInstanceValue,defaultValue,fieldtype);
                
            outputstring = outputstring  + "/>";
            if(showGlobalaVar){
                outputstring = outputstring + "&nbsp;";
                outputstring = outputstring + "<button onclick='fillVariable(\"" + inputElement.getName() +"\",\""+holderUUID+"\", this)' >"+Labels.getLabel("useGlobalVar")+"</button>";
            }
           
            
            // hidden field type
        } else if ("hidden".equalsIgnoreCase(fieldtype)) {
            outputstring = outputstring + "<input type='hidden' id='" + inputElement.getName() + "'";
            outputstring = outputstring + inputElement.getHTMLValue(contractInstanceValue,defaultValue,fieldtype);           
            outputstring=outputstring + "/>";                                           
            // textarea field type
        } else if ("textarea".equalsIgnoreCase(fieldtype)) {
            
            if (!"".equals(label)) {
                outputstring = outputstring + labeltext ;
            }
            outputstring = outputstring + "<textarea " + inputclasses + " id='" + inputElement.getName() + "'  name='" + inputElement.getName() + "' ";
            if (disabled) {
                outputstring = outputstring + " readonly style=\"background-color:lightgrey\"";
            }
            if (!"".equals(rows)) {
                outputstring = outputstring + " rows='" + rows + "' ";
            }
            if (!"".equals(cols)) {
                outputstring = outputstring  + " cols='" + cols + "' ";
            }
            if (!"".equals(onchangestring)) {
                outputstring = outputstring + ON_CHANGE + onchangestring + "\"";
            }
            outputstring = outputstring  + ">";
            
            outputstring = outputstring +inputElement.getHTMLValue(contractInstanceValue,defaultValue,fieldtype);
            
            outputstring = outputstring  + "</textarea>";
            
        } else if (CHECKBOX.equalsIgnoreCase(fieldtype)) {
            
            if (!"".equals(label)) {
                outputstring=outputstring + labeltext;
            }
            for (FieldInstance fi:inputElement.getAllowedValues(c,contractInstance)) {
                
                outputstring = outputstring + DIV_ID_FORMITEM + inputElement.getName() + "' " + inputclasses + ">";
                
                outputstring=outputstring + "<input " + inputclasses + " type='checkbox' name='" + inputElement.getName() + "' id='" + inputElement.getName() + "' ";
                if (disabled) {
                    outputstring = outputstring + DISABLED2;
                }

                if (!"".equals(onchangestring)) {
                    outputstring = outputstring + ONCLICK + onchangestring + "\"";
                }
                
                outputstring = outputstring + VALUE + StringEscapeUtils.escapeHtml4(fi.getName()) + "\"";
                if (contractInstanceValues.contains(fi.getName())) {
                    outputstring = outputstring + CHECKED;
                } else if (defaultValue.equalsIgnoreCase(fi.getName())) {
                    outputstring = outputstring + CHECKED;
                }
                
                outputstring = outputstring + ">" ;
                String assign=fi.getName();
                if (fi.getAssignment()!=null) {
                    assign=fi.getAssignment().toString();
                }
                outputstring = outputstring + "<label  class='propertyformlabel propertyformlabelcheckbox' >" + StringEscapeUtils.escapeHtml4(assign)
                        + LABEL_DIV;
            }
            
        } else if (RADIO.equalsIgnoreCase(fieldtype)) {
            
            if (!"".equals(label)) {
                outputstring=outputstring + labeltext;
            }
            for (FieldInstance fi:inputElement.getAllowedValues(c,contractInstance)) {
                outputstring = outputstring + DIV_ID_FORMITEM + inputElement.getName() + "' " + inputclasses + ">";
                
                outputstring=outputstring + "<input  " + inputclasses + "id='" + inputElement.getName() + "' name='" + inputElement.getName() + "' type='radio' ";
                if (disabled) {
                    outputstring = outputstring + DISABLED2;
                }

                outputstring = outputstring + VALUE + StringEscapeUtils.escapeHtml4(fi.getName()) + "\"";
                if (contractInstanceValue.equalsIgnoreCase(fi.getName())) {
                    outputstring = outputstring + CHECKED;
                } else if (defaultValue.equalsIgnoreCase(fi.getName())) {
                    outputstring = outputstring + CHECKED;
                }
                
                if (!"".equals(onchangestring)) {
                    outputstring = outputstring + ONCLICK + onchangestring + "\"";
                }
                
                outputstring = outputstring + ">" ;
                outputstring = outputstring + "<label  class='propertyformlabel propertyformlabelradio'>"
                        + StringEscapeUtils.escapeHtml4(fi.getAssignment().toString()) + LABEL_DIV;
            }                                           
        } else if ("select".equalsIgnoreCase(fieldtype)) {
            outputstring =outputstring + labeltext + "<select " + inputclasses + " name=\"" + inputElement.getName() + "\" id=\"" + inputElement.getName() + "\" ";
            if (!"".equals(fieldlength)) {
                outputstring = outputstring  + " width=\"" + fieldlength + "\"";
            }
            if (!"".equals(rows) && !rows.equals(1)) {
                outputstring = outputstring  + " multiple size=\"" + rows + "\"";
            }
            if (disabled) {
                outputstring = outputstring + DISABLED2;
            }
            if (!"".equals(onchangestring)) {
                outputstring = outputstring + ON_CHANGE + onchangestring + "\"";
            }

            outputstring = outputstring  + ">";
            if (inputElement.hasOption(InputElement.NULL)) {
                outputstring = outputstring + "<option value=\"" + nullValueDefinedAs + "\"";
                outputstring = outputstring + ">--Select--</option>";
            }

            outputstring=outputstring + inputElement.generateOptions(c,contractInstance);
            outputstring = outputstring + "</select>";
  
        } else if (inputElement.getType().equals(InputElement.TYPE_DATASET)) {
            if (inputElement.countChildElements()>0) {
                for (Element e:inputElement.getChildElements()) {
                    InputElement ie=(InputElement) e;
                    outputstring=outputstring + generateInputEleHTML(plugin, c, contractInstance, includeValidation, ie, holderUUID, showGlobalaVar);
                }
            }
        } else if (GROUP.equalsIgnoreCase(fieldtype)) {
            if (!NOFRAME.equals(styleClass)) {
                outputstring=outputstring + "<fieldset><legend>" + label + "</legend>";
            }
            String groupname=inputElement.getName();
            
            outputstring=outputstring + "<div id='fieldgroupcollection" + groupname + "' class='propertyformfieldgroup" + styleClass + "'>";
            if (inputElement.countChildElements()>0) {                                              
                if (many) {
                    // MORE verify that multiple group instances are being
                    // handled this way in contractInstance
                    for (int i=1; i <= inputElement.countChildElements(); i++) {                                                        
                        outputstring=outputstring + inputElement.generateFieldGroup(groupname,inputElement.getChildElements(),c, contractInstance,includeValidation);                                                   
                    }
                } else {                                                
                        outputstring=outputstring + inputElement.generateFieldGroup(groupname,inputElement.getChildElements(),c,contractInstance,includeValidation);
                }
            }
            if (inputElement.hasOption(InputElement.DESCRIPTION)) {
                String desc=StringEscapeUtils.escapeHtml4(inputElement.getOptionValue(InputElement.DESCRIPTION,"",""));
                outputstring = outputstring
                        + PROPERTY_FORM_DESCRIPTION + inputElement.getName() + "'>" + desc + SPAN_DIV;
            }
            outputstring=outputstring + "</div>\n<div style='clear:both;'></div>\n";
            if (many) {
                //MORE: only activate link for field input selects if data exists
                outputstring=outputstring + "<a id='add" + groupname + "' class='propertyformlink' onclick='propertyform.addFieldGroup(\"" + groupname + "\");'>Add</a>";                                               
            }
            if (!NOFRAME.equals(styleClass)) {
                outputstring=outputstring + "</fieldset>";
            }

        }
        if ((inputElement.getType().equalsIgnoreCase(InputElement.TYPE_KELBASE)
                || inputElement.getType().equalsIgnoreCase(InputElement.TYPE_KEL_ENTITY)) && inputElement.countChildElements() > 0) {
            for (Element e : inputElement.getChildElements()) {
                InputElement ie = (InputElement) e;
                outputstring = outputstring + generateInputEleHTML(plugin, c, contractInstance, includeValidation, ie, holderUUID, showGlobalaVar);
            }
        }
        
        if (!GROUP.equalsIgnoreCase(fieldtype) && inputElement.hasOption(InputElement.DESCRIPTION)) {
            String desc = StringEscapeUtils.escapeHtml4(inputElement.getOptionValue(InputElement.DESCRIPTION, "", ""));
            
            outputstring = outputstring                            
 + PROPERTY_FORM_DESCRIPTION + inputElement.getName() + "'>" + desc + SPAN_DIV;
        }
        outputstring=outputstring + DIV;
        return outputstring;
        
        }catch (Exception e) {                                   
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException(e);      
        }
    }
    
    private static String generateHipieElementId(String holderUUID, String elementName) {
        StringBuilder idBuilder = new StringBuilder();
        return idBuilder
                .append(FORM_LINE)
                .append(UNDERSCORE)
                .append(elementName)
                .append(UNDERSCORE)
                .append(holderUUID)
                .toString();       
    }
    
    /**
     * Generates string represents a javaScript function which will be invoked when a selectbox/combobx is changed 
     */
    private static String getOnChange(Contract c, InputElement inputElemet,
            String holderUUID, boolean recurse) throws HipieException {
        
        if (c == null) {
            return "";
        }        
        String onchangestring="";
        try {  
        if(inputElemet instanceof KelbaseInputElement){
            
            List<String> thingsToEnable = new ArrayList<String>();
            
            for (Element ie:c.getInputElements(Element.TYPE_KEL_ATTRIBUTE)) {
                if (ie.getParentContainer() != null && ie.getParentContainer()==inputElemet) {
                    thingsToEnable.add(ie.getName());
                }
            }
            
            String elementID = null;
            for (String elname : thingsToEnable) {
                elementID = generateHipieElementId(holderUUID, elname);
                String cmd = "filterOptionsStartingWith('" + elementID + "',this.value + '.');";
                onchangestring = onchangestring + cmd;
            }
            if (recurse) {
                for (Element e:inputElemet.getChildElements()) {
                    onchangestring=onchangestring + getOnChange(c, (InputElement)e, holderUUID, recurse);
                }
            }
            
        }else{              
            
            String elementID = null;
            List<String> thingsToEnable = c.getEnablers().get(inputElemet.getName());            
            String controllerElement=inputElemet.getHTMLType();
            if (thingsToEnable != null) {
                
                for (String elname : thingsToEnable) {
                    
                    elementID = generateHipieElementId(holderUUID,elname); 
                    ElementOption eo=new ElementOption(elname);
                    eo=c.getElement(elname,InputElement.GetOrigin()).getOption(Element.ENABLE);
                    
                    for (FieldInstance fi:eo.getParams()) {
                        String cmd="";
                        if (CHECKBOX.equalsIgnoreCase(controllerElement) || RADIO.equalsIgnoreCase(controllerElement)) {                           
                            cmd = SHOW_HIDE_FIELD + elementID + "',this.checked);";
                        } else {
                            Property fassign=fi.getAssignment();
                            if (fassign != null) {
                                if (fassign.toString().startsWith("FUNCTION:")) {
                                    String func=fassign.toString().substring(9);
                                    cmd = SHOW_HIDE_FIELD + elementID + "'," + func + ");";
                                } else {
                                    cmd = SHOW_HIDE_FIELD + elementID + "',this.value=='" + fi.getAssignment() + "');";
                                }
                            }
                        }
                        onchangestring=onchangestring + cmd;
                    }
                }
            }
            
            if (recurse) {
                for (Element e:inputElemet.getChildElements()) {
                    onchangestring=onchangestring + getOnChange(c, (InputElement)e, holderUUID, recurse);
                }
            }
        }
        } catch (Exception e) {     
            LOGGER.error(Constants.EXCEPTION, e);     
            throw new HipieException(e);      
        }
        return onchangestring;    
    }

    private static String getJavascript() {
        String js="function filterOptionsStartingWith(objname,start) {\n"
                + " var selectobject=document.getElementById(objname);\n"
                + FOR_SELECTOBJECT
                + "         if (selectobject.options[i].value.startsWith(start)) {\n"
                + "             selectobject.options[i].className = \"visibleoption\";\n"
                + "         } else { \n"
                + "             selectobject.options[i].className = \"invisibleoption\"\n"
                + "             if (selectobject.selectedIndex==i) {selectobject.selectedIndex=-1;}\n"
 + ENTER + ENTER1
                + " }\n";
        
         js=js + "function " + InputElement.CHILD_ROW_CHECK_FUNCTION + "(objname) {\n"
                 + "   var selectobject=document.getElementById(objname);\n"
                + FOR_SELECTOBJECT
                 + "         if (selectobject.selectedIndex==i && selectobject.options[i].value.indexOf('.') !== -1) {\n"
                 + "               return true;\n"
 + ENTER + ENTER1
                 + "     return false;\n"
                 + "}\n";
         
         js=js + "function showhidefield(objname,show) {\n"
                 + "var selectobject=document.getElementById(objname);\n"
                    + "if (show==false) {\n"
                    + " selectobject.style.visibility='hidden';\n"
                    + " selectobject.style.display='none';\n"
                    + "} else if (show==true) {\n"
                    + " selectobject.style.visibility='visible';\n"
                    + " selectobject.style.display='inherit';\n"
                    + "} \n"
               + "}\n";
        return js;
    }

}
