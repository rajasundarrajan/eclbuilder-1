package org.hpccsystems.dsp.ramps.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionElement;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.CompositionError;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.impl.MBSCompanyDaoImpl;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.ScoredSearchFilter;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.entity.Entity;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.controller.entity.FileBrowserData;
import org.hpccsystems.dsp.ramps.entity.GlobalVariable;
import org.hpccsystems.dsp.ramps.entity.Project;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.CompositionService;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.error.HError;
import org.hpccsystems.usergroupservice.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.json.JSONObject;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RampsUtil {

    private static final String FIELD = "field";

    private static final String TRUE = "true";

    private static final String FALSE = "false";

    private static final Logger LOGGER = LoggerFactory.getLogger(RampsUtil.class);

    static String[] integetArray = { "byte", "int", "integer", "negativeInteger", "nonNegativeInteger", "nonPositiveInteger", "positiveInteger",
            "short", "unsignedLong", "unsignedInt", "unsignedShort", "unsignedByte" };
    static String[] realArray = { "long", "decimal" };

    private RampsUtil() {
    }

    public static void filterData(final Map<String, Set<Object>> filters, ListModelList<Project> modelList, List<Project> projects,
            Map<String, Set<Object>> data) {

        // Event Data will contain a single item that is added to filter
        final String fieldName = data.entrySet().iterator().next().getKey();
        Set<Object> values = data.entrySet().iterator().next().getValue();

        // Adding new values to filter map
        filters.put(fieldName, values);

        // Updating Model
        modelList.clear();
        modelList.addAll(projects);
        for (final Entry<String, Set<Object>> entry : filters.entrySet()) {

            // Skipping filtering when no filters are selected
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            CollectionUtils.filter(modelList, new Predicate<Object>() {

                @Override
                public boolean evaluate(Object project) {
                    try {
                        return filters.get(entry.getKey()).contains(
                                Project.class.getMethod("get" + WordUtils.capitalize(entry.getKey()), (Class<?>[]) null).invoke(project,
                                        (Object[]) null));

                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                            | SecurityException e) {
                        LOGGER.error(Constants.EXCEPTION, e);
                        return false;
                    }
                }
            });
        }

    }

    public static void filterGridData(final Map<String, Set<Object>> filters, ListModelList<Object> modelList, List<?> rowList,
            Map<String, Set<Object>> data) {

        // Event Data will contain a single item that is added to filter
        final String fieldName = data.entrySet().iterator().next().getKey();
        Set<Object> values = data.entrySet().iterator().next().getValue();

        // Adding new values to filter map
        filters.put(fieldName, values);

        // Updating Model
        modelList.clear();
        modelList.addAll(rowList);
        for (final Entry<String, Set<Object>> entry : filters.entrySet()) {

            // Skipping filtering when no filters are selected
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }

            CollectionUtils.filter(modelList, new Predicate<Object>() {

                @Override
                public boolean evaluate(Object rowObject) {
                    try {
                        return filters.get(entry.getKey()).contains(
                                rowObject.getClass().getMethod("get" + WordUtils.capitalize(entry.getKey()), (Class<?>[]) null)
                                        .invoke(rowObject, (Object[]) null));

                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                            | SecurityException e) {
                        LOGGER.error(Constants.EXCEPTION, e);
                        return false;
                    }
                }
            });
        }

    }

    /**
     * Creates outputs for the DDL passed
     * 
     * @param process
     * @param visualizationDDL
     * @param divId
     */
    public static void renderDashboard(CompositionInstance compositionInstance, String visualizationDDL, String divId, String formId, String layout, boolean isEditable) {
        LOGGER.debug("Selected Tab's ddl --> " + visualizationDDL);
        LOGGER.debug("divId --> " + divId);
        
        JSONObject params = createVisualization(compositionInstance, visualizationDDL, layout, formId);
        
        String editable = isEditable ? TRUE : FALSE;
        
        Clients.evalJavaScript("createDashboardVisualization('" + divId + "','" + StringEscapeUtils.escapeJavaScript(params.toJSONString()) + "', " + editable
                + ");");
    }
    
    /**
     * Calls the javascript function to resize the dashboard
     * 
     * @param dashboardDivId - dashboard holder id
     */
    public static void resizeDashboard(String dashboardDivId) {
        LOGGER.debug("Selected Tab's dashboardDivId --> " + dashboardDivId);
        
        Clients.evalJavaScript("resizeDashboard('" + dashboardDivId + "');");
    }
    
    /**
     * Renders Databomb Dashboard
     * 
     * @throws HipieException
     */
    public static void renderDashboard(String userId, Contract contract, Collection<String> fileNames, 
 String divId, String formId, String layout, boolean isEditable)
            throws HipieException {
        try {
        LOGGER.debug("databomb Contract --> {} ", contract);
        LOGGER.debug("divId --> {}", divId);
        
        Map<String, RecordInstance> datasets = new HashMap<String, RecordInstance>();
        Map<String, Object> databomb = new LinkedHashMap<String, Object>();
        
        ObjectMapper objectMapper = new ObjectMapper();
        
        for (String filename : fileNames) {
            String[] userAndFile = DashboardUtil.getUserAndFilename(filename);           
            
            if(ArrayUtils.isNotEmpty(userAndFile) && userAndFile.length == 2){ 
                char dummyChar = '.';
                CompositionService service =  (CompositionService) SpringUtil.getBean(Constants.COMPOSITION_SERVICE);
                StaticData data = service.getStaticData(userAndFile[0], userAndFile[1]);
                
                Map<String, Object> mapValue = objectMapper.reader(Map.class).readValue(data.getFileContent());  
                databomb.putAll(generateDatabombUniqueKey(mapValue,databomb));
                
                Map<String, RecordInstance> outputFields = Utility.getDatabombOutputFields(null, data.getFileContent(), 
                        null, false, dummyChar, dummyChar, dummyChar, null);
                
                Map<String, RecordInstance> tempoutputFields = generateDatasetUniqueKey(outputFields,datasets);
                datasets.putAll(tempoutputFields);
                LOGGER.debug("File -{} \n Data - {}", userAndFile[1], tempoutputFields);
            }            
        }
        LOGGER.debug("databomb - {}", databomb);
        LOGGER.debug("datasets - {}", datasets);
        LOGGER.debug("visual elements - {}",  contract.getVisualElements().iterator().next().toString());
        
        ErrorBlock errorBlock = new ErrorBlock();
        String databombDDL = HipieSingleton.getHipie().getDatabomb(
                userId, 
                contract.getVisualElements().iterator().next().toString(), 
                (HashMap<String, RecordInstance>) datasets, errorBlock);
        
        for (HError hError : errorBlock.getErrors()) {
            throw new HipieException(hError.getErrorString());
        }
        
        errorBlock.getErrors().forEach(e -> LOGGER.error(Constants.EXCEPTION, e));
        
        LOGGER.debug("Contract ------ {}\nDDL - {}", contract.getVisualElements(), databombDDL);
        
        String editable = isEditable ? TRUE : FALSE;
        
        String databombStr = objectMapper.writeValueAsString(databomb);
        LOGGER.debug("DB - {}", databombStr);
        
        Clients.evalJavaScript("createDatabombVisualization('" + divId + "','" + StringEscapeUtils.escapeJavaScript(databombDDL) + "', " + editable + ",'" + 
                StringEscapeUtils.escapeJavaScript(databombStr) + "','" + StringEscapeUtils.escapeJavaScript(layout) + "');");
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException(Labels.getLabel("unableTogetDataToVisualize"), e);
        }
    }
    //When a file has Entry<String,RecordInstance> with a key that is already available in datasets,
    //generate a different key and add it into datasets    
    private static Map<String, RecordInstance> generateDatasetUniqueKey(
            Map<String, RecordInstance> outputFields,
            Map<String, RecordInstance> datasets) {
        Map<String, RecordInstance> tempoutputFields = new LinkedHashMap<>(); 
        outputFields.forEach((key, value) -> createOutputField(datasets, tempoutputFields, key, value));
        return tempoutputFields;
    }

    private static void createOutputField(Map<String, RecordInstance> datasets, Map<String, RecordInstance> tempoutputFields, String key,
            RecordInstance value) {
        if(datasets.keySet().contains(key)){
            StringBuilder keyBuilder = new StringBuilder(key);
            for (int i = 1; datasets.keySet().contains(keyBuilder.toString()); i++) {
                keyBuilder.append(i);
            }
            tempoutputFields.put(keyBuilder.toString(), value) ;
        }else{
            tempoutputFields.put(key, value);
        }
    }

    //When a file has Entry<String,object> with a key that is already available in databomb,
    //generate a different key and add it into databomb
    private static Map<String, Object> generateDatabombUniqueKey(
            Map<String, Object> mapValue, Map<String, Object> databomb) {
        Map<String, Object> tempMap = new LinkedHashMap<>();
        mapValue.forEach((key, value) -> generateKey(databomb, tempMap, key, value));
        return tempMap;
    }

    private static void generateKey(Map<String, Object> databomb, Map<String, Object> tempMap, String key, Object value) {
        if(databomb.keySet().contains(key)){
            StringBuilder keyBuilder = new StringBuilder(key);
            for (int i = 1; databomb.keySet().contains(keyBuilder.toString()); i++) {
                keyBuilder.append(i);
            }
            tempMap.put(keyBuilder.toString(), value) ;
        }else{
            tempMap.put(key, value);
        }
    }

    private static JSONObject createVisualization(CompositionInstance compositionInstance, String visualizationDDL, String layout, String formId) {
        HPCCConnection hpccConnection = compositionInstance.getHPCCConnection();
        String hpccId = hpccConnection.getLabel();

        StringBuilder url = new StringBuilder(hpccConnection.getESPUrl()).append("WsWorkunits/WUResult.json?").append("Wuid=")
                .append(compositionInstance.getWorkunitId()).append("&ResultName=").append(visualizationDDL).append("&SuppressXmlSchema=true");

        JSONObject params = new JSONObject();
        String wsWorkunits = "WsWorkunits";
        String wsEcl = "WsEcl";
        params.put("url", url.toString());
        params.put("hpccId", hpccId);
        params.put(wsWorkunits, hpccConnection.getESPUrl() + wsWorkunits);
        params.put(wsEcl, hpccConnection.getRoxieServiceUrl() + wsEcl);
        params.put("layout", layout);
        if(formId != null) {
            params.put("formId", formId);
        }
        LOGGER.debug("Params - {} ", params);
        return params;
    }

    public static List<String> getDDLs(Composition composition, boolean isSideeffect) throws HipieException {
        List<String> outputDDLs = new ArrayList<String>();
        Map<String, HashMap<String, String>> ddls = null;
        try {
            ddls = composition.getVisualizationDDLs(null, isSideeffect);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            throw new HipieException("Unable to get visualization ddl", e);
        }

        if (ddls != null) {
            for (Entry<String, HashMap<String, String>> entry : ddls.entrySet()) {
                outputDDLs.addAll(entry.getValue().keySet());
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of DDLs: {}", outputDDLs);
        }

        return outputDDLs;
    }

    public static String removeSpaceSplChar(String str) {
        return str.replaceAll("[^a-zA-Z0-9]+", "");
    }

    public static String removeWhiteSpaces(String str) {
        return str.replaceAll("\\s+", "");
    }

    public static List<Entity> stripRowheader(List<Entity> headerRow) {
        List<Entity> result = new ArrayList<Entity>();

        int count = 0;
        for (Entity val : headerRow) {
            String stripped = removeStartingNumbers(removeSpaceSplChar(val.getValue()));
            // Adds 'field1, field2.. for fields entirely composed of in valid
            // characters
            result.add(stripped.length() > 0 ? new Entity(stripped) : new Entity(FIELD + ++count));
        }

        return result;
    }

    public static List<String> legacyStripRowheader(List<String> headerRow) {
        List<String> result = new ArrayList<String>();

        int count = 0;
        for (String str : headerRow) {
            String stripped = removeStartingNumbers(removeSpaceSplChar(str));
            // Adds 'field1, field2.. for fields entirely composed of in valid
            // characters
            result.add(stripped.length() > 0 ? stripped : FIELD + ++count);
        }

        return result;
    }

    public static String removeStartingNumbers(String name) {
        if (name.length() > 0 && Character.isDigit(name.charAt(0))) {
            return removeStartingNumbers(name.substring(1));
        } else {
            return name;
        }
    }

    public static boolean isDashboardConfigured(Composition composition) {
        boolean isDashboardConfigured = composition.getContractInstances().values().stream()
                .filter(instance -> Dashboard.DASHBOARD_REPO.equals(instance.getContract().getRepositoryName())).findAny().isPresent();
        if (isDashboardConfigured) {
            return true;
        }
        return false;

    }

    public static List<Field> getFileFields(PluginOutput dataSource) throws HipieException {
        List<RecordInstance> outputRecordFormats = new ArrayList<RecordInstance>();
        try {
            outputRecordFormats.add(dataSource.getOutputElement().getRecordInstance(dataSource.getContractInstance(), null));
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            throw new HipieException("Could not add output formate records", e);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Record structure for {} - {}", dataSource.getLabel(), outputRecordFormats);
        }

        return getFileFields(outputRecordFormats);
    }

    public static List<Field> getFileFields(List<RecordInstance> recordInstances) {
        List<Field> fields = new ArrayList<Field>();
        if (recordInstances != null && !recordInstances.isEmpty()) {
            for (RecordInstance record : recordInstances) {
                List<String> fieldString = Arrays.asList(record.getFieldList().split(","));

                if (fieldString != null && !fieldString.isEmpty()) {
                    for (String value : fieldString) {
                        List<String> splittedValue = Arrays.asList(value.trim().split(" "));
                        fields.add(new Field(splittedValue.get(1), splittedValue.get(0)));
                    }
                }
            }
        }
        return fields;
    }

    /**
     * @param composition
     * @return Whether current logged in user has edit permission to the
     *         composition checking with both DSP level and HIPIE level
     *         permissions
     */
    public static boolean currentUserCanEdit(User user, Composition composition) {
        return user.canEdit() && HipieSingleton.getHipie().getPermissionsManager().userCanEdit(user.getId(), composition);
    }

    public static List<Field> getLogicalFileFields(String logicalFile, HPCCConnection hpccConnection) {

        RecordInstance recordInstance;
        List<Field> fields = new ArrayList<Field>();
        try {
            recordInstance = hpccConnection.getDatasetFields(logicalFile, null);
            recordInstance.stream().forEach(fieldInstance -> fields.add(new Field(fieldInstance.getName(), fieldInstance.getType())));

        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
        return fields;

    }

    public static List<ContractInstance> getAllDatasourceContractInstance(Composition composition) {
        List<ContractInstance> dataSources = new ArrayList<ContractInstance>();
        Map<String, ContractInstance> contractInstances = composition.getContractInstances();
        for (Map.Entry<String, ContractInstance> entry : contractInstances.entrySet()) {
            if (HIPIEUtil.isDataSourcePlugin(entry.getValue())) {
                dataSources.add(entry.getValue());
            }
        }
        return dataSources;
    }

    public static String getHipieDatatype(String nodeValue) {
        Set<String> integerTypes = new HashSet<String>();
        Set<String> realTypes = new HashSet<String>();

        integerTypes.addAll(Arrays.asList(integetArray));
        realTypes.addAll(Arrays.asList(realArray));

        if (integerTypes.contains(nodeValue)) {
            return FieldInstance.TYPE_INTEGER;
        } else if (realTypes.contains(nodeValue)) {
            return FieldInstance.TYPE_REAL;
        } else if (StringUtils.containsIgnoreCase(nodeValue, "string")) {
            return FieldInstance.TYPE_STRING;
        } else if ("BOOLEAN".equalsIgnoreCase(nodeValue)) {
            return FieldInstance.TYPE_BOOLEAN;
        } else {
            // TODO Find a better way to tell type.
            // For now returning as integer in order to draw chart
            return FieldInstance.TYPE_INTEGER;
        }

    }
    
    public static String convertToJsonString(Object obj) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = null;

        try {
            jsonString = gson.toJson(obj);
        } catch (StackOverflowError err) {
            LOGGER.error(Constants.EXCEPTION, err);

            jsonString = "StackOverflowError occured when converting to JSON";
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            jsonString = "Exception occured when converting to JSON";
        }

        return jsonString;
    }

    public static Map<String, String[]> getRequestHeadersMap(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, String[]> headerMap = new HashMap<String, String[]>();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            List<String> headers = Collections.list(request.getHeaders(headerName));

            headerMap.put(headerName, headers.toArray(new String[headers.size()]));
        }

        return headerMap;
    }

    
    public static boolean validateComposition(Composition composition,Component component) {
        CompositionError compError = new CompositionError(composition.validate());
        boolean isValidComposition = true;
        if (compError.hasFatalError() || compError.hasError() ) {
            LOGGER.error(Constants.EXCEPTION, compError.gethError());
            Clients.showNotification(compError.getErrorMessage(), Clients.NOTIFICATION_TYPE_ERROR, component, Constants.POSITION_TOP_CENTER, 0, true);
            isValidComposition = false;
        }
        return isValidComposition;
    }
    
    /**
     * Returns true if composition has any Fatal Error
     * @return boolean
     */
    public static boolean checkCompositionError(Composition composition,Component component) {
        CompositionError error = new CompositionError(composition.validate());
        boolean errorOccurred = false;
        if(error.hasFatalError()) {
            LOGGER.error(Constants.EXCEPTION, error.gethError());
            Clients.showNotification(error.getErrorMessage(), Clients.NOTIFICATION_TYPE_ERROR, component, Constants.POSITION_TOP_CENTER, 0, true);
            errorOccurred = true;
        } else if (error.hasError()) {
            LOGGER.error(Constants.EXCEPTION, error.gethError());
            Clients.showNotification(error.getErrorMessage(), Clients.NOTIFICATION_TYPE_WARNING, component, Constants.POSITION_TOP_CENTER, 0, true);
            errorOccurred = false;
        } else {
            errorOccurred = false;
        }
        return errorOccurred;
    }
    
    /**
     * Filters the inner datasets(sub-dataset) in a dataset
     * @param outputLists
     * @return list of filtered datasets of a plugin
     */
    public static List<Element> filterSubDatasets(List<Element> outputLists){
        List<Element> filtered = new ArrayList<Element>();
        for (Element e : outputLists) {
            if (e.getParentContainer() != null
                    && (e.getParentContainer().getType() == null || (e.getParentContainer().getType() != null && !e.getParentContainer().getType()
                            .equals(Element.TYPE_DATASET)))) {
                filtered.add(e);
            }
        }
        return filtered;
    }
    
    /**
     * Returns the 'Output Dataset' contract's instance of a composition
     * 
     * @param composition
     * @return ContractInstance
     * @throws HipieException
     */
    public static ContractInstance getOutputDatasetInstance(Composition composition) throws HipieException {
        try {
            return composition.getContractInstanceByName(HipieSingleton.OUTDATASET);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION);
            throw new HipieException(e);
        }
    }
    /**
     * Returns the 'Name' property value of the 'Output Dataset'
     * @return String
     */
    public static String getOutputDatasetName(ContractInstance outputInstance) {
        String namePropVal = null;
        if (outputInstance != null) {
            namePropVal = outputInstance.getProperty(Constants.NAME);
        }
        return namePropVal;
    }

    /**
     * if reference ID exixts,it checks for the outputdataset name starts with valid name like '~thor_ramps::REFERENCE_ID' 
     * @param name - name property exists in contractInstace
     * @param project - baseScope of this project need to be checked against the 'Name' property value of contractInstace
     * @return boolean
     */
    public static boolean validateOuputDatasetName(String name, Project project) {
        boolean isValidName = true;
        StringBuilder baseName = new StringBuilder();
        baseName.append(Constants.TILDE).append(project.getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR);     
        if(!name.startsWith(baseName.toString()) || name.trim().length() <= baseName.length()) {
            isValidName = false;
        }
        return isValidName;
    }

    /**Returns the referenceId input Element of composition,if exists  
     * @param composition
     * @return Element
     */
    public static Element getReferenceIDElement(Composition composition) {
        Element referenceElement = null;
        if( composition.getInputElements(InputElement.TYPE_STRING) != null && !composition.getInputElements(InputElement.TYPE_STRING).isEmpty()){
            Optional<Element> globalVariable = composition.getInputElements(InputElement.TYPE_STRING).stream()
                    .filter(element -> element.getName().equals(Constants.REFERENCE_ID)).findFirst();
            if (globalVariable.isPresent()) {
                referenceElement =  globalVariable.get();
            }
        }
        return referenceElement;
    }
    
    /**
     * Clears the the UseDataset contract's logical file name and 
     * the OutputDataSet's Name(Output Dataset).
     */
    public static void resetPlugingProperties(Composition composition,Project project) {
        project.getDatasetPlugin().clearAllFiles();
        
        try {
            ContractInstance outputInstance = getOutputDatasetInstance(composition);
            if(outputInstance != null){
                String name = getOutputDatasetName(outputInstance);
                
                if(!validateOuputDatasetName(name, project)){
                    StringBuilder nameBuffer = new StringBuilder();
                    nameBuffer.append("~").append(project.getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR);
                    outputInstance.setProperty(Constants.NAME,nameBuffer.toString());
                }
            }
           
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
        }
    }

    public void createAddReferenceIDElement(Composition composition,String gcid) {
        Element referenceElement = getReferenceIDElement(composition);
        //Adds the ReferenceID input element
        if(referenceElement == null){
            composition.getInputElements().add(createReferenceElement(gcid));
        }else{
            //Updates the ReferenceID input element
            updateElement(referenceElement,gcid);
        }
    }
    
    public static void addReferenceId(Composition composition,String gcIDValue) {
        Element referenceElement = RampsUtil.getReferenceIDElement(composition);
        // Adds the ReferenceID input element
        if (referenceElement == null) {
            referenceElement = new InputElement();
            referenceElement.setName(Constants.REFERENCE_ID);
            referenceElement.setType(InputElement.TYPE_STRING);
            referenceElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, gcIDValue)));
            composition.getInputElements().add(referenceElement);
        } else {
            // Updates the ReferenceID input element
            updateElement(referenceElement, gcIDValue);
        }
    }

    public static void updateElement(Element referenceElement, String gcid) {
        ElementOption referenceIdOption = referenceElement.getOption(Element.DEFAULT);
        referenceIdOption.getParams().clear();
        referenceIdOption.addParam(new FieldInstance(null, gcid));
    }

    private Element createReferenceElement(String gcid) {
        Element referenceElement = new InputElement();
        referenceElement.setName(Constants.REFERENCE_ID);
        referenceElement.setType(InputElement.TYPE_STRING);
        referenceElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, gcid)));
        return referenceElement;
    }

    public static boolean isFieldPresent(List<Field> fields, Field fieldToCheck) {
        if(fieldToCheck.isRowCount()){
            return true;
        }
        
        return fields
                .stream().parallel()
                .filter(field -> field.getColumn().equals(fieldToCheck.getColumn()))
                .findAny().isPresent();
    }
    
    public static boolean isScoredSearchFilterPresent(List<Field> fields,
            ScoredSearchFilter ssFilter) {
        return fields
                .stream()
                .filter(field -> field.getColumn().equals(ssFilter.getColumn())
                        && field.getDataType().equalsIgnoreCase(
                                ssFilter.getDataType())).findAny().isPresent();
    }
    
    public static boolean validateClusterSelection(List<Combobox> clusterConnection) {
        boolean retVal = true;
        List<String> texts = new ArrayList<String>();
        texts.add("chooseConnection");
        texts.add("chooseThorCluster");
        texts.add("chooseRoxieCluster");
        for (Combobox listCombo : clusterConnection) {
            if (StringUtils.isEmpty(listCombo.getValue())) {
                Clients.showNotification(Labels.getLabel(texts.get(clusterConnection.indexOf(listCombo))), Clients.NOTIFICATION_TYPE_ERROR,
                        listCombo, Constants.POSITION_END_AFTER, 3000);
                retVal = false;
                break;
            }
        }
        return retVal;
    } 
    
    public static FileBrowserData getFileBrowserData(String action) {
        FileBrowserData data = new FileBrowserData();
        data.setAction(action);
        return data;
    }
    public static HPCCConnection getHpccConnection(String hpccId) {
        return HipieSingleton.getHipie().getHpccManager().getConnection(hpccId);
    }
    
    @SuppressWarnings("unchecked")
    public static Set<String> getOpenProjectLabels() {
        Set<String> fileNameSet = (Set<String>) Sessions.getCurrent().getAttribute(Constants.OPEN_PROJECT_LABELS);
        if (fileNameSet == null) {
            fileNameSet = new HashSet<String>();
            Sessions.getCurrent().setAttribute(Constants.OPEN_PROJECT_LABELS, fileNameSet);
        }
        return fileNameSet;
    }
    
    public static boolean isFileNameDuplicate(User user, String currentLabelOrName) throws HipieException {
        List<Project> prjts;
        Map<String, CompositionElement> templatesMap;
        try {
            // check for project labels
            prjts = ((CompositionService) SpringUtil.getBean("compositionService")).getProjects(user);
            // Check for template labels
            templatesMap = HipieSingleton.getHipie().getCompositionTemplates(user.getId());
        } catch (Exception e1) {
            LOGGER.error(Constants.EXCEPTION, e1);
            throw new HipieException("Could not get the templates", e1);
        }
        // Check for open tab projects
        Set<String> labels = getOpenProjectLabels();
        boolean isExistsInTemplate = templatesMap != null && isLabelExists(templatesMap.values(), currentLabelOrName);
        return prjts.stream().filter(comp -> comp.getLabel().equals(currentLabelOrName)).count() > 0
                || (labels != null && labels.contains(currentLabelOrName)) || isExistsInTemplate;

    }
    
    private static boolean isLabelExists(Collection<CompositionElement> templatesMap,String label){
        return templatesMap.stream().filter(compElement ->label.equals(compElement.getLabel())).count() > 0;
    }

    public static boolean isInteger(String s) {
        boolean isValidInteger = false;
        try {
            Integer.parseInt(s);
            isValidInteger = true;
        } catch (NumberFormatException ex) {
            LOGGER.error(Constants.HANDLED_EXCEPTION, ex);
        }

        return isValidInteger;
    }

    public static Collection<String> retriveAllGroups() throws AuthenticationException {
        Collection<Group> groups = new ArrayList<>();
        groups.addAll(((UserService) SpringUtil.getBean(Constants.USER_SERVICE)).getAllGroups());

        return groups.stream().map(Group -> Group.getMachineName().toLowerCase()).collect(Collectors.toList());

    }

    public static Element getECLElement(Composition composition) {
        Element eclElement = null;
        Optional<Element> elementOption = composition.getInputElements()
                .stream()
                .filter(element -> Constants.KEEP_ECL.equals(element.getName()))
                .findAny();

        if (elementOption.isPresent()) {
            eclElement = elementOption.get();
        }
        LOGGER.info("\'KeepEcl\' input element --->{}", eclElement);
        return eclElement;
    }

    /**
     * As global variables 'GCID/Reference ID', 'KeepEcl','Visualization service
     * version' need not be shown 'Global variable' popup filtering these inputs
     */
    public static List<Element> filterGlobalVarPopupInputs(List<Element> inputs) {
        return inputs.stream().filter(inputEle -> !Constants.REFERENCE_ID.equals(inputEle.getName())
                && !Constants.KEEP_ECL.equals(inputEle.getName()) 
                && !Constants.VIZ_SERVICE_VERSION.equals(inputEle.getName())).collect(Collectors.toList());
    }

    /**
     * As global variables 'GCID/Reference ID', 'KeepEcl','Visualization service
     * version' need not be shown in 'Settings' page filtering these inputs
     */
    public static List<Element> filterSettingsPageInputs(List<Element> inputs) {
        return inputs.stream()
                .filter(inputEle -> !Constants.KEEP_ECL.equals(inputEle.getName())
                        && !Constants.VIZ_SERVICE_VERSION.equals(inputEle.getName()))
                .collect(Collectors.toList());
    }

    public static String generateFilePrefix(Project project) {
        StringBuilder filePrefix = new StringBuilder();

        if (!StringUtils.isEmpty(project.getReferenceId())) {
            filePrefix.append(Constants.TILDE).append(project.getBaseScope()).append(Constants.SCOPE_RESOLUTION_OPR);
        } else {
            filePrefix.append(Constants.TILDE);
        }
        return filePrefix.toString();
    }

    public static boolean setGCIDcompliance(Composition composition, Component component, String gcidValue) {
        boolean success = true;
        MBSCompanyDaoImpl mbsCompanyDao = (MBSCompanyDaoImpl) SpringUtil.getBean("mbsCompanyDao");
        Set<String> complianceTags = new HashSet<String>();
        for (Element element : composition.getInputElements()) {
            LOGGER.debug("element name--->{}", element.getName());
            if (Constants.INDUSTRY_CLASS.equals(element.getName()) || Constants.GCID.equals(element.getName())
                    || Constants.FCRA.equals(element.getName())) {
                continue;
            }
            complianceTags.add(GlobalVariable.GCID_COMPLIANCE_TAGS.get(element.getName()));
        }
        LOGGER.debug("compliance tags 11---->{}", complianceTags);
        Map<String, String> gcidCompliance = new HashMap<String, String>();
        try {
            gcidCompliance = mbsCompanyDao.getGCIDComplianceValues(gcidValue, complianceTags);
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            success = false;
        }
        if (!success || gcidCompliance == null) {
            Clients.showNotification(Labels.getLabel("complianceParameterFail"), Clients.NOTIFICATION_TYPE_ERROR, component,
                    Constants.POSITION_TOP_CENTER, 5000, true);
            return success;
        }
        // Populates value for GCID compliance Tags
        for (Element inputElement : composition.getInputElements()) {
            LOGGER.debug("inputelement------>{}", inputElement.getName());
            LOGGER.debug("gcidCompliance------->{}", gcidCompliance);
            if (gcidCompliance.containsKey(inputElement.getName())) {
                try {
                    inputElement.addOption(Element.DEFAULT, new FieldInstance(null, gcidCompliance.get(inputElement.getName())));
                } catch (Exception e) {
                    LOGGER.error(Constants.EXCEPTION, e);
                    Clients.showNotification(Labels.getLabel("writeComplianceFail"), Clients.NOTIFICATION_TYPE_ERROR, component,
                            Constants.POSITION_TOP_CENTER, 5000, true);
                    success = false;
                    return success;
                }
                LOGGER.debug("childElements---------->{}", inputElement.getOption(Element.DEFAULT));
            }
        }
        LOGGER.debug("composition element sdfasdfasf --------->{}", composition.getInputElements());
        return success;
    }
}

