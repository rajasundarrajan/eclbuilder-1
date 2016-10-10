package org.hpccsystems.dsp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.HIPIEService;
import org.hpcc.HIPIE.dude.DriveFieldInstance;
import org.hpcc.HIPIE.dude.Element;
import org.hpcc.HIPIE.dude.FieldInstance;
import org.hpcc.HIPIE.dude.GenerateSALTElement;
import org.hpcc.HIPIE.dude.InputElement;
import org.hpcc.HIPIE.dude.OutputElement;
import org.hpcc.HIPIE.dude.Permission;
import org.hpcc.HIPIE.dude.Permission.PermissionType;
import org.hpcc.HIPIE.dude.Property;
import org.hpcc.HIPIE.dude.RecordInstance;
import org.hpcc.HIPIE.dude.ServiceElement;
import org.hpcc.HIPIE.dude.VisualElement;
import org.hpcc.HIPIE.dude.option.ElementOption;
import org.hpcc.HIPIE.dude.option.SelectElementOption;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpccsystems.dsp.Constants.DatasourceStatus;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.controller.DashboardConfig;
import org.hpccsystems.dsp.dashboard.controller.WidgetConfig;
import org.hpccsystems.dermatology.domain.Dermatology;
import org.hpccsystems.dsp.dashboard.entity.DudElement;
import org.hpccsystems.dsp.dashboard.entity.widget.ChartType;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.OutputSchema;
import org.hpccsystems.dsp.dashboard.entity.widget.PluginOutput;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.entity.widget.ScoredSearchFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget;
import org.hpccsystems.dsp.dashboard.entity.widget.Widget.DATASOURCE;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.GlobalFilter;
import org.hpccsystems.dsp.dashboard.entity.widget.chart.ScoredSearch;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.exceptions.RepoException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.UserService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.usergroupservice.User;
import org.hpccsystems.ws.client.gen.wsworkunits.v1_58.ECLResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.resource.Labels;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.Filedownload;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

public class CompositionUtil {

    private static final String SELECTS = "SELECTS";
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositionUtil.class);
    private static final String HIPIE_REPO_MISSING = "hipieRepoMissing";
    private static final String RESULT1 = "Result1";
    private static final String RESULT3 = "Result3";
    private static final String SCORES = "Scores";
    private static final String RECORD_MATCHING_NAME = "Record_Matching";
    private static final String RECORD_MATCHING = "RecordMatching";
    public static final String DS_INPUT = "dsInput";
    private static final String DS_OUTPUT = "dsOutput";
    private static final String LOGICAL_FILE = "LogicalFilename";
    private static final String METHOD = "Method";
    private static final String THOR = "THOR";
    private static final String STRUCTURE = "Structure";
    private static final String PLUGIN_DESC = "RAMPS - Dashboard Perspective Contract";
    private static final String VISUALIZE = "VISUALIZE";
    private static final String VERSION = "0.1";
    private static final String SERVICE = "Service";
    private static final String RESPONSE = "Response";
    private static final String SERVICE_URL = "HIPIE.HIPIEConfig.RoxieInternalServiceUrl";

    private static Set<String> scoredSearchFields = new HashSet<String>();

    static {
        scoredSearchFields.add("ScoreCombine");
        scoredSearchFields.add("RecordsToReturn");
        scoredSearchFields.add("Threshold");
    }

    public static final Comparator<GridEntity> SORT_BY_DATE_ASC = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            return e1.getLastModifiedDate().compareTo(e2.getLastModifiedDate());
        }
    };

    public static final Comparator<GridEntity> SORT_BY_AUTHOR_ASC = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            if (e1.getAuthor() == null) {
                return 1;
            }
            if (e2.getAuthor() == null) {
                return (e2.getAuthor() == null) ? 0 : -1;
            }
            return e1.getAuthor().compareTo(e2.getAuthor());
        }
    };

    public static final Comparator<GridEntity> SORT_BY_NAME_ASC = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            return e1.getLabel().compareTo(e2.getLabel());
        }

    };

    public static final Comparator<GridEntity> SORT_BY_DATE_DES = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            return e2.getLastModifiedDate().compareTo(e1.getLastModifiedDate());
        }
    };

    public static final Comparator<GridEntity> SORT_BY_AUTHOR_DES = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            if (e1.getAuthor() == null) {
                return 1;
            }
            if (e2.getAuthor() == null) {
                return (e2.getAuthor() == null) ? 0 : -1;
            }
            return e2.getAuthor().compareTo(e1.getAuthor());
        }
    };

    public static final Comparator<GridEntity> SORT_BY_NAME_DES = new Comparator<GridEntity>() {
        public int compare(GridEntity e1, GridEntity e2) {
            return e2.getLabel().compareTo(e1.getLabel());
        }
    };


    private CompositionUtil() {

    }

    /**
     * Gets unique datasources from the widgets and adds a ContractInstance for each datasource into the composition
     * @param widgets
     * @param composition
     */
    private static void hookDatasources(List<Widget> widgets, Composition composition) {
        // Remove the datasources from the composition before adding current data sources
        removeDatasourceContractInstances(composition);
        
        for (Widget widget : widgets) {
            if(widget.getDatasource() != null){
                composition.addContractInstance(widget.getDatasource().getContractInstance());
            }
        }
    }
    
    /**
     * Removes all of the use datasource contract instances from the composition.
     * @param composition
     */
    private static void removeDatasourceContractInstances(Composition composition) {
        if(composition.getProperty("CATEGORY")!= null && composition.getProperty("CATEGORY").equals("VISUALIZE")){//only run on dashboard perspective
            Set<Entry<String, ContractInstance>> contractInstances = composition.getContractInstances().entrySet();
            List<ContractInstance> removeCIs = new ArrayList<ContractInstance>();
            for (Entry<String, ContractInstance> contractInstance: contractInstances) {
                if (contractInstance.getValue().getProperty((LOGICAL_FILE)) != null) 
                {
                    removeCIs.add(contractInstance.getValue());
                }
            }
            
            for (ContractInstance ci: removeCIs) {
              composition.removeContractInstance(ci);            
            }
        }
    }
    
    /**
     * 
     * @param userID
     *            - current login user
     * @param compositionToDelete
     *            - composition object on which user triggered the delete
     * @throws Exception
     *             - throws hipie exception incase of failure
     */

    public static void generateScoredSearchVisualizationPlugin(Composition comp, String userid,Dashboard dashboard)
            throws RepoException, HipieException {
        comp.setParseErrors(new ErrorBlock());
        HIPIEService hipieService = HipieSingleton.getHipie();

        try {    
            hookDatasources(dashboard.getScoredSearchWidgets(),comp);
            
            IRepository repository = hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO);
            checkRepoForNull(repository);
            Contract scoredsearchtemplate = HipieSingleton.getScoredSearch();
            checkScoredSearchForNull(scoredsearchtemplate);
            scoredsearchtemplate.setRepository(repository);
            String contractName = comp.getName() + Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER;
            scoredsearchtemplate.setProp("NAME", contractName);
            scoredsearchtemplate.setAuthor(userid);

            String overridedir = createDashboardContractSavePath(userid, contractName);
            Contract scoredsearchContract = hipieService.saveContractAs(userid, scoredsearchtemplate, overridedir);

            // USER SELECTS THE DATA SOURCE TO MAKE A SCORED SEARCH FOR
            deleteDashboardConfiguration(comp);

            // hook the data source up
            ContractInstance scoredSearchInstance = new ContractInstance(scoredsearchContract);

            // TODO:Need to iterate for the list of scored search widgets
            ScoredSearch widget = (ScoredSearch) dashboard.getScoredSearchWidgets().get(0);
            scoredSearchInstance.addPrecursor(widget.getDatasource().getContractInstance());

            scoredSearchInstance.setCurrentUsername(userid);

            // get the generated service element and add it into this scored
            // search plugin
            GenerateSALTElement salt = null;
            for (Element els : scoredSearchInstance.getContract().getGenerateElements()) {
                if (els instanceof GenerateSALTElement) {
                    salt = (GenerateSALTElement) els;
                }
            }
            ServiceElement se = salt.getServiceElement(scoredSearchInstance);
            scoredSearchInstance.getContract().getOutputElements().add(se);

            // optionally update the default value of the service inputs; set
            // State to "CA" if nothing selected
            List<Element> inputs = se.getInputElements();

            widget.getScoredSearchfilters().forEach(filter -> {
                Element currentInput = inputs.stream().filter(input -> ((Element) input).getName().equals(filter.getColumn())).findFirst().get();
                ScoredSearchFilter ssf = (ScoredSearchFilter) filter;
                String ssfval;
                if(ssf.getOperator() != null){
                    ssfval = ssf.getOperator() + " " + ssf.getOperatorValue();  
                }else{
                    ssfval = ssf.getModifier() + " " + ssf.getModifierValue();
                }
                ElementOption eo = new ElementOption(Element.DEFAULT, new FieldInstance(null, ssfval));
                currentInput.addOption(eo);
            });

            // add a table visualization referencing the scored search service
            VisualElement visualization = new VisualElement();
            visualization.setType(VisualElement.VISUALIZE);
            visualization.setName(contractName);
            scoredSearchInstance.getContract().addVisualElement(visualization);
            
            VisualElement ve=new VisualElement();
            visualization.addChildElement(ve);
            ve.setType("FORM");
            ve.setName("scoredform");
            // add fields to the table for all the scored search outputs
            ElementOption labels = new ElementOption(Element.LABEL, se.getParseContext());
            labels.setContainer(ve);
            ElementOption values = new ElementOption(VisualElement.VALUE, se.getParseContext());
            values.setContainer(ve);
            for (Element ie : se.getInputElements()) {
                labels.addParam(new FieldInstance(null, ie.getName()));
                values.addParam(new FieldInstance(null, ie.getName()));
            }

            ve.addOption(labels);
            ve.addOption(values);
            
            ve.addOption(SELECTS,new FieldInstance(null,"*"));
            List<FieldInstance> drives = new ArrayList<FieldInstance>();
            DriveFieldInstance drv=new DriveFieldInstance(null,widget.getName());
            drv.setDriverField(new FieldInstance(null,"*"));
            drives.add(drv);
            ((SelectElementOption) ve.getOption(SELECTS)).setDrives((ArrayList<FieldInstance>) drives);
            
            //generating three visual elements(Table) 
            generateScoredSearchVisualElement(RESULT1, widget, visualization, scoredSearchInstance, se);
            generateScoredSearchVisualElement(RECORD_MATCHING, widget, visualization, scoredSearchInstance, se);
            generateScoredSearchVisualElement(RESULT3, widget, visualization, scoredSearchInstance, se);
          
            // save your updated scored search plugin
            hipieService.saveContract(userid, scoredSearchInstance.getContract());
        } catch (RepoException e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw (RepoException) e;
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Error occured while creating the scored search visualization contract(DUD file)", e);
        }

    }
    
    private static void generateScoredSearchVisualElement(String tableName, Widget widget, VisualElement visualization, ContractInstance scoredSearchInstance,
            ServiceElement se) {
        VisualElement visualElement = new VisualElement();
        visualization.addChildElement(visualElement);
        visualElement.setType(VisualElement.TABLE);
        if(tableName == RECORD_MATCHING){
            visualElement.setName(RECORD_MATCHING_NAME);
            visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, RECORD_MATCHING_NAME)));
        }else if(tableName == RESULT3){
            visualElement.setName(SCORES);
            visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, SCORES)));
        }else{
            visualElement.setName(widget.getName());
            visualElement.addOption(new ElementOption(VisualElement.TITLE, new FieldInstance(null, widget.getTitle())));
        }
     
      visualElement.setBasis((OutputElement) scoredSearchInstance.getContract().getElement(tableName));
      

        // add filters to the table for all of the scored search inputs
        if(tableName == RESULT1 || tableName == RESULT3){
            ElementOption filters = new ElementOption(VisualElement.FILTER, se.getParseContext());
            filters.setContainer(visualElement);
            for (Element ie : se.getInputElements()) {
                filters.addParam(new FieldInstance(ie.getType(), ie.getName()));
            }  
            visualElement.addOption(filters);
        }
       

        // add fields to the table for all the scored search outputs
        ElementOption labels = new ElementOption(Element.LABEL, se.getParseContext());
        labels.setContainer(visualElement);
        ElementOption values = new ElementOption(VisualElement.VALUE, se.getParseContext());
        values.setContainer(visualElement);

        for (Element ie : scoredSearchInstance.getContract().getElement(tableName).getChildElements()) {
            labels.addParam(new FieldInstance(null, ie.getName()));
            values.addParam(new FieldInstance(null, ie.getName()));
        }

        visualElement.addOption(labels);
        visualElement.addOption(values);
    }
    
   
    private static void checkScoredSearchForNull(Contract scoredsearchtemplate) throws RepoException {
        if (scoredsearchtemplate == null) {
            throw new RepoException(Labels.getLabel("scoredSearchTemplateMissing"));
        }
    }

    private static void checkRepoForNull(IRepository repository) throws RepoException {
        if (repository == null) {
            throw new RepoException(Labels.getLabel(HIPIE_REPO_MISSING));
        }
    }

    
    public static String getTmpfilepath(String compId, String userId) {
        StringBuilder tmp = new StringBuilder();
        tmp.append(System.getProperty("java.io.tmpdir"))
            .append(File.separator)
            .append(userId)
            .append(File.separator)
            .append(compId);
        return tmp.toString();
    }
    
    public static void downloadCompositionAndContractFile(Composition composition, ContractInstance instance, String ddl, List<org.hpccsystems.dsp.dashboard.entity.Dermatology> layouts,
            CompositionInstance mostRecentInstance, String userId) throws IOException, ZipException, HipieException {

        String zipFileName = RampsUtil.removeSpaceSplChar(composition.getLabel());
        String zipFilePath = getTmpfilepath(composition.getId(), userId) + File.separator + zipFileName;
        File mainDir = new File(zipFilePath);
        if (mainDir.exists()) {
            FileUtils.deleteDirectory(mainDir);
        }
        mainDir.mkdir();
        File compositionFile = new File(zipFilePath + File.separator + zipFileName + ".cmp");
        File contractFile = new File(zipFilePath + File.separator + instance.getContract().getName() + ".dud");
        File ddlFile = null;
        File dermatologyFile = null;

        // fetch DDL string from WU object
        try {
            if (mostRecentInstance != null && ddl != null) {
                LOGGER.debug("Work unit results ----->{}", (Object) mostRecentInstance.getWorkunit().getResults());
                for (ECLResult result : mostRecentInstance.getWorkunit().getResults()) {
                    LOGGER.debug("result name ----->{}", result.getName());
                    if (ddl.equals(result.getName())) {
                        ddlFile = new File(zipFilePath + File.separator + "ddl" + ".txt");
                        FileUtils.writeStringToFile(ddlFile, result.getValue());
                        LOGGER.debug("result Value ----->{}", result.getValue());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            throw new HipieException("Unable to get workunits", e);
        }

        LOGGER.debug("available layouts ----->{}", layouts);
        if (layouts != null && !layouts.isEmpty()) {
            dermatologyFile = new File(zipFilePath + File.separator + "Dermatology" + ".txt");
            StringBuilder dermatology = new StringBuilder();
            for (org.hpccsystems.dsp.dashboard.entity.Dermatology layout : layouts) {
                dermatology.append("User Id :").append("\r\n")
                    .append(layout.getUserId()).append("\r\n")
                    .append("Composition Id :").append("\r\n")
                    .append(layout.getCompositionId()).append("\r\n")
                    .append("Composition Version :").append("\r\n")
                    .append(layout.getCompositionVersion()).append("\r\n")
                    .append("Ddl :").append("\r\n")
                    .append(layout.getDdl()).append("\r\n")
                    .append("Gcid :").append("\r\n")
                    .append(layout.getGcid() + "").append("\r\n")
                    .append("Modified Date :").append("\r\n")
                    .append(layout.getModifiedDate() + "").append("\r\n")
                    .append("Layout :").append("\r\n")
                    .append(layout.getLayout()).append("\r\n\r\n");
            }
            FileUtils.writeStringToFile(dermatologyFile, dermatology.toString());
        }
        FileUtils.writeStringToFile(compositionFile, composition.toString());
        FileUtils.writeStringToFile(contractFile, instance.getContract().toString());

        // using zip4j library to zip the directory
        File zipFileDelete = new File(zipFilePath + ".zip");
        if (zipFileDelete.exists()) {
            zipFileDelete.delete();
        }
        ZipFile zipfile = new ZipFile(zipFilePath + ".zip");
        ZipParameters parameter = new ZipParameters();
        if (dermatologyFile != null) {
            zipfile.addFile(dermatologyFile, parameter);
        }
        if (ddlFile != null) {
            zipfile.addFile(ddlFile, parameter);
        }
        zipfile.addFile(compositionFile, parameter);
        zipfile.addFile(contractFile, parameter);

        // Downloading the zip file
        Filedownload.save(new File(zipFilePath + ".zip"), "text");

    }
    
    public static String createDashboardContractSavePath(String userid, String contractName) {
        HIPIEService hipieService = HipieSingleton.getHipie();
        if (hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO) == null) {
            return null;
        }
        return hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO).getLocaldir() + File.separator + userid + "dashboardduds"
                + File.separator + contractName + ".dud";
    }

    public static String createDashboardPerspectiveSavePath(String userid, HIPIEService hipieService, String dashboardName) {
        if (hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO) == null) {
            return null;
        }
        return hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO).getLocaldir() + File.separator + userid + File.separator
                + dashboardName + ".cmp";
    }

    public static List<Widget> extractScoredSearchWidget(ContractInstance contractInstance) throws HipieException {
        Contract contract = contractInstance.getContract();
        List<ScoredSearchFilter> filters = new ArrayList<ScoredSearchFilter>();
        List<Widget> widgets = null;
        try {
            contract.getOutputElements().forEach(output -> createFilters(filters, output));

            ScoredSearch scoredSearch = new ScoredSearch();
            scoredSearch.setScoredSearchfilters(filters);
            // TODO:need to re factor while adding more scored search widgets
            VisualElement visualElement = (VisualElement) contract.getVisualElements().iterator().next().getChildElements().get(1);

            scoredSearch.setName(visualElement.getName());
            scoredSearch.setTitle(visualElement.getOption(VisualElement.TITLE).getParams().get(0).getName());

            scoredSearch.setChartConfiguration(Dashboard.CHARTS_CONFIGURATION.get(ChartType.SCORED_SEARCH));

            ContractInstance precursor = contractInstance.getPrecursors().values().iterator().next();
            PluginOutput datasource = new PluginOutput(precursor, precursor.getContract().getOutputElements().iterator().next());
            scoredSearch.setDatasource(datasource);
            //As Scored search always uses file, setting datasource type as file
            scoredSearch.setDatasourceType(DATASOURCE.FILE);
            widgets = new ArrayList<Widget>();
            widgets.add(scoredSearch);

        } catch (Exception e) {
            throw new HipieException("Error occured while extracting the Scored Search Widget from contract(DUD file)", e);
        }
        return widgets;
    }

    private static void createFilters(List<ScoredSearchFilter> filters, Element output) {
        if (output instanceof ServiceElement) {
            ServiceElement serviceElement = (ServiceElement) output;
            serviceElement.getInputElements().forEach(input -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Input element type: {}", input.getType());
                    LOGGER.debug("Input element: {}", input.getName());
                }
                if (input.getOptions() != null && !input.getOptions().isEmpty() && !scoredSearchFields.contains(input.getName())) {
                    ElementOption elemetOption = input.getOptions().values().iterator().next();
                    FieldInstance fieldInstance = elemetOption.getParams().iterator().next();
                    ScoredSearchFilter filter = new ScoredSearchFilter(input.getName(), fieldInstance.getName());
                    filter.setDataType(input.getType().toLowerCase());
                    filters.add(filter);
                }
            });
        }
    }

    /**
     * Clones the composition,it will also generate a DUD file in the
     * DashboardRepos if the composition contains dashboard contract instance
     * 
     * @param composition
     *            - Composition to be cloned
     * @param cmpName
     *            - New name
     * @param label
     *            - New label
     * @param userId
     *            - current user ID
     * @return cloned composition
     * @throws RepoException
     * @throws HipieException
     * @throws Exception
     *             - HIPIE exception
     */
    public static Composition cloneComposition(Composition composition,
            String cmpName, String label, String userId, boolean newid,
            Dashboard originalDashboard,DatasourceStatus status) throws RepoException, HipieException
        {
        Composition clonedComposition = null;
         try {
             clonedComposition= cloneCompositionOrConvert(composition,cmpName,label, userId, newid,originalDashboard,status,false);
         }
         catch (RepoException e) {
             throw (RepoException) e;
         } catch (Exception e) {
             throw new HipieException("Error occured while cloning the composition", e);
         }
         return clonedComposition;
    }
    public static Composition cloneCompositionOrConvert(Composition composition,
            String cmpName, String label, String userId, boolean newid,
            Dashboard originalDashboard,DatasourceStatus status,boolean doConvert)
            throws RepoException, HipieException {
        Composition clonedComposition = null;
        try {
            String oldid = composition.getId();
            ContractInstance dashboardCI = getVisualizationContractInstance(composition);
            
            //work around for removing contract instance, as it is not getting removed after cloning
            composition.removeContractInstance(dashboardCI);
            clonedComposition = initiateCloneComposition(composition, newid, oldid, dashboardCI);
            clonedComposition.setLabel(label);
            clonedComposition.name = cmpName;
            if(doConvert) {
                clonedComposition.setProp("CATEGORY","CUSTOM");
            }

            // TODO work around for setting the name to the cloned composition
            HIPIEService hipieService = HipieSingleton.getHipie();
            Composition newlyCloned = hipieService.saveCompositionAs(userId, clonedComposition, cmpName);
            if (!newid) {
                newlyCloned.setId(oldid);
            }

            clonedComposition = newlyCloned;
            hipieService.deleteComposition(userId, newlyCloned);          

            // if the composition contains dashboard contract instance , a new
            // DUD file will be generated
            if (dashboardCI != null) {
              
                Dashboard dashboard = new Dashboard();
                if (StringUtils.endsWith(dashboardCI.getContract().getName(),Dashboard.SCORED_SEARCH + Dashboard.CONTRACT_IDENTIFIER) && !doConvert) {
                    dashboard.addWidgets(CompositionUtil.extractScoredSearchWidget(dashboardCI));
                } else {
                    if(originalDashboard != null && !doConvert){ 
                        //This handles Dashboard perspective.
                        //Extracting queries
                        dashboard.setQueries(CompositionUtil.extractQueries(dashboardCI, originalDashboard.isStaticData()));
                        
                        dashboard.addWidgets(
                                Widget.extractVisualElements(dashboardCI, dashboard.getQueries(), originalDashboard.isStaticData(),null));
                        
                        Widget globalFilterWidget = dashboard.getGlobalFilterWidget();
                        if(globalFilterWidget != null) {
                            globalFilterWidget.setName(cmpName);
                        }
                        
                        dashboard.setStaticData(originalDashboard.isStaticData());
                    }else{
                        //This handles Ramps perspective.As in Ramps flow queries are not supported,passing null for queries.
                        //And databomb not supported.Ao passing 'isStaticData()' as false.
                        dashboard.addWidgets(Widget.extractVisualElements(dashboardCI, null, false,status));
                    }
                    boolean isSmallDataset = CompositionUtil.extractRunOption(dashboardCI);
                    dashboard.setLargeDataset(!isSmallDataset);
                }
                generatePlugin(cmpName, userId, clonedComposition, dashboard, true);
               
            }
        } catch (RepoException e) {
            throw (RepoException) e;
        } catch (Exception e) {
            throw new HipieException("Error occured while cloning the composition", e);
        }
        return clonedComposition;
    }

    private static Composition initiateCloneComposition(Composition composition, boolean newid, String oldid, ContractInstance dashboardCI)
            throws HipieException {
        Composition clonedComposition;
        try {
            clonedComposition = new Composition(composition);
        } catch (Exception e) {
            throw new HipieException(e);
        }
        if (dashboardCI != null) {
            composition.addContractInstance(dashboardCI); 
        }
                  
        if (!newid) {
            clonedComposition.setId(oldid);
        }
        return clonedComposition;
    }

    private static void generatePlugin(String cmpName, String userId, Composition clonedComposition, Dashboard dashboard, boolean cloned)
            throws RepoException, HipieException {
        List<Widget> scoredSearchWidgets = dashboard.getScoredSearchWidgets();
        if (scoredSearchWidgets != null && !scoredSearchWidgets.isEmpty()) {
            CompositionUtil.generateScoredSearchVisualizationPlugin(clonedComposition, userId, dashboard);
        } else {
            CompositionUtil.generateVisualizationPlugin(clonedComposition, userId, dashboard, cloned);
        }
    }

    public static void deleteDashboardConfiguration(Composition composition) throws HipieException {
        ContractInstance oldInstance =  getVisualizationContractInstance(composition);
        if (oldInstance != null) {
            composition.removeContractInstance(oldInstance);
            deleteVisualizationContract(oldInstance);
        }
    }

    public static void deleteVisualizationContract(ContractInstance cotractInstance) throws HipieException {
        HIPIEService hipieService = HipieSingleton.getHipie();       
        try {
            if (cotractInstance != null) {
                Contract contract = cotractInstance.getContract();
                IRepository repository = hipieService.getRepositoryManager().getRepos().get(contract.getRepositoryName());
                repository.deleteFile(contract.getFileName());
            }
        } catch (Exception e) {
            throw new HipieException("Hipie service can't be established", e);
        }
    }

    public static ContractInstance getVisualizationContractInstance(
            Composition composition){
        ContractInstance contractInstance = null;
        
        StringBuilder instanceName = new StringBuilder();
        instanceName.append(composition.getName()).append(Dashboard.CONTRACT_IDENTIFIER);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("instanceName ------->{}", instanceName);
        }
        try {
            contractInstance = composition.getContractInstanceByName(instanceName.toString());
            if(contractInstance == null){
                instanceName = new StringBuilder();
                instanceName.append(composition.getName()).append(Dashboard.SCORED_SEARCH).append(Dashboard.CONTRACT_IDENTIFIER);
                contractInstance = composition.getContractInstanceByName(instanceName.toString());
            }
        } catch (Exception e) {
            //As Hipie throws Exception when it finds more than one ContractInstance with the passed contract name,
            //Handling it from DSP
            LOGGER.error(Constants.HANDLED_EXCEPTION, e);
            contractInstance = getContractInstanceByName(composition,instanceName.toString());
        }
        return contractInstance;
    }

   
    //TODO: This method should be removed once issue of hooking multiple instances into composition for the same contract.
    private static ContractInstance getContractInstanceByName(Composition composition,
            String instanceName) {
        
        ContractInstance contractInstance = null;
        Optional<ContractInstance> option = composition
                .getContractInstances()
                .values()
                .stream()
                .filter(ci -> ci.getContract().getCanonicalName()
                        .toLowerCase().endsWith(instanceName)).findAny();
        if(option.isPresent()){
            contractInstance = option.get();
        }
        return contractInstance;
    }

    /**
     * Creates the contract/DUD file for the dashboard
     * 
     * @param composition
     * @param userId
     * @param dashboard
     * @param cloned
     * @throws RepoException
     * @throws HipieException
     */
    public static void generateVisualizationPlugin(Composition composition, String userId, Dashboard dashboard, boolean cloned)
            throws RepoException, HipieException {

        LOGGER.debug("Dashboard - {} \n Widgets {}", dashboard, dashboard.getWidgets());
        try { 
            if(CollectionUtils.isNotEmpty(dashboard.getNonGlobalAndScoredSearchWidget())) {
                hookDatasources(dashboard.getNonGlobalAndScoredSearchWidget(),composition);
                
                composition.setParseErrors(new ErrorBlock());
                HIPIEService hipieService = HipieSingleton.getHipie();
                // Creating input,ouput,visualelement for first dashboard widget
                Contract contract = createContract(composition, cloned, userId);
    
                // The parent visualization should be generated for both logical and
                // roxie queries
                VisualElement visualization = new VisualElement();
                visualization.setName(contract.getName());
                visualization.setType(VisualElement.VISUALIZE);
                contract.getVisualElements().add(visualization);
    
                // Handling widgets which uses logical files
                List<Widget> logicalFileWidgets = dashboard.getLogicalFileWidgets();
    
                Map<PluginOutput,DudElement> pluginOutputs = new HashMap<PluginOutput, DudElement>();
    
                initiateVisualElement(composition, dashboard, contract, visualization, logicalFileWidgets, pluginOutputs);
    
                //Adding GLobal filter form
                attachGlobalFilterForm(dashboard,visualization);
                
                // validates contract, if valid saves it
                contract = hipieService.saveContractAs(contract.getAuthor(), contract, createDashboardContractSavePath(contract.getAuthor(), contract.getName()));
                
                
                ContractInstance contractInstance;
                contractInstance = updateAndReturnVisualizationCI(composition,contract,userId);
                
                //Add the new contract instance into composition
                composition.addContractInstance(contractInstance);
                
                LOGGER.debug("   Newc contractInstance------------ -->{}", contractInstance);    
                
                hookWidgetDatasource(pluginOutputs, logicalFileWidgets, contractInstance);
    
                LOGGER.debug("   Newc composition------------ -->{}", composition);
                LOGGER.debug("CONTRACT ------------ -->{}", contractInstance.getParent());
    
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Visuslisation contract - " + contractInstance.toCompositionString());
                }
            }
        } catch (RepoException e) {
            throw e;
        } catch (Exception e) {
            throw new HipieException("Error occured while creating the visualization contract(DUD file)", e);
        }

    }

    private static void attachGlobalFilterForm(Dashboard dashboard, VisualElement visualization) throws HipieException {
        Widget globalFilter = dashboard.getGlobalFilterWidget();
        if(globalFilter != null){
            List<Widget> allWidgetsButGlobalFilter = dashboard.getNonGlobalAndScoredSearchWidget();
            List<String> allWidgetName = allWidgetsButGlobalFilter.stream().map(eachWid -> eachWid.getName()).collect(Collectors.toList());
           LOGGER.debug(" -------------------->{}", globalFilter.getFilters());
            GlobalFilter castObj = (GlobalFilter)globalFilter;
            VisualElement ele = castObj.generateVisualElement(allWidgetName);
            visualization.addChildElement(ele);
        }
        
    }

    /**
     * Generates the INPUT and VISUALIZE blocks of the dude
     * @param composition
     * @param dashboard
     * @param contract
     * @param visualization
     * @param logicalFileWidgets
     * @param pluginOutputs
     * @throws HipieException
     */
    private static void initiateVisualElement(Composition composition, Dashboard dashboard, Contract contract, VisualElement visualization,
            List<Widget> logicalFileWidgets, Map<PluginOutput, DudElement> pluginOutputs) throws HipieException {
        
        Widget widget = null;
        Iterator<Widget> fileWidgetIterator = logicalFileWidgets.iterator();
        int uniqueOutputCnt = 1;
        
        while (fileWidgetIterator.hasNext()) {
            widget = fileWidgetIterator.next();
            DudElement dudElement = null;

            // Check to see if the widget's datasource already exists in the outputs set
            if (pluginOutputs.keySet().contains(widget.getDatasource())) {
                dudElement = pluginOutputs.get(widget.getDatasource());
                InputElement inputObj = (InputElement) dudElement.getInputElement();
                widget.updateDUDFieldNames(contract, dudElement.getInputElement().getName());
                widget.generateInputElement(contract.getInputElements(), dudElement.getInputElement().getName()).stream().forEach(inputElement -> inputObj.addChildElement(inputElement));
            } else {
                dudElement = generateInputOutput(contract, composition, uniqueOutputCnt, widget, dashboard.isLargeDataset());
                // creating datasource for the first widget
                try {
                    pluginOutputs.put(widget.getDatasource(),dudElement);
                } catch (Exception e) {
                    // handling here as visual element generation should be
                    // continued for the next widget
                    LOGGER.error(Constants.EXCEPTION, e);
                }
                uniqueOutputCnt++;
            }

            //Adding local filter form
            if(widget.hasLocalFilters()) {
                LOGGER.debug("Form Element {}", widget.generateFormElement());
                visualization.addChildElement(widget.generateFormElement());
            }
            
            VisualElement visualElement = widget.generateVisualElement();
            visualElement.setBasis((OutputElement) dudElement.getOutputElement());
            visualization.addChildElement(visualElement);
   
            LOGGER.debug("Filters - {}", widget.getFilters());
   
        }
   
        // Handling widgets which uses Roxie queries files
        if (dashboard.getQueries() != null && !dashboard.getQueries().isEmpty()) {
            generateQueryService(contract, dashboard.getQueries(), dashboard.isStaticData());
   
            List<Widget> queryWidgets = dashboard.getWidgets().stream().filter(chartWidget -> chartWidget.canUseNativeName())
                    .collect(Collectors.toList());
   
            generateQueryWidgetVisualElement(queryWidgets, contract, dashboard.isStaticData());
        }
    }
    
    /**
     * Creates the INPUT/OUTPUT blocks of the dude
     * @param contract
     * @param composition
     * @param uniqueOutputCnt
     * @param widget
     * @param isLargeDataset
     * @return
     */
    private static DudElement generateInputOutput(Contract contract,
            Composition composition, int uniqueOutputCnt, Widget widget,
            boolean isLargeDataset) {

        DudElement inputOutputElement = new DudElement();

        // Create the input element, set the type and add options.
        InputElement inputObj = new InputElement();
        if (uniqueOutputCnt > 0) {
            inputObj.setName(DS_INPUT + uniqueOutputCnt);
        } else {
            inputObj.setName(DS_INPUT);
        }
        inputObj.setType(InputElement.TYPE_DATASET);
        inputObj.addOption(new ElementOption(Element.MAPBYNAME));
        inputObj.addOption(new ElementOption(Element.OPTIONAL));

        inputOutputElement.setInputElement(inputObj);
        
        widget.updateDUDFieldNames(contract, inputOutputElement.getInputElement().getName());widget.updateDUDFieldNames(contract, inputOutputElement.getInputElement().getName());
        
        // Each dataset element in the input block needs to have a list of their own widgets.
        widget.generateInputElement(contract.getInputElements(), inputOutputElement.getInputElement().getName()).stream().forEach(inputElement -> inputObj.addChildElement(inputElement));

        contract.getInputElements().add(inputObj);
        
        // Create the output element, set the type and add options.
        OutputElement output = new OutputElement();
        if (uniqueOutputCnt > 0) {
            output.setName(DS_OUTPUT + uniqueOutputCnt);
        } else {
            output.setName(DS_OUTPUT);
        }
        output.setType(OutputElement.TYPE_DATASET);
        output.setBase(inputObj.getName());
        
        //If the dataset has large data,run the composition in 'service' mode,otherwise run it in 'WUID' mode
        if(isLargeDataset){
            output.addOption(new ElementOption(OutputElement.LARGE));
        } else {
            output.addOption(new ElementOption(OutputElement.WUID));
        }

        contract.getOutputElements().add(output);

        inputOutputElement.setOutputElement(output);

        return inputOutputElement;
    }

    public static ContractInstance updateAndReturnVisualizationCI(Composition composition, Contract newContract, String userId) {
        
        ContractInstance newCI;
      //get the current dashboard contract instance
       ContractInstance oldCI = getVisualizationContractInstance(composition); 
       
       LOGGER.debug("Composition - {} Instance id - {}", composition, oldCI);
       
       if(oldCI != null){ 
           //create a new contract instance for the new contract, with the same instance id as the old contract
           LOGGER.debug("Old CI ID--->{}",oldCI.getName());
           newCI = newContract.createContractInstance(oldCI.getName());          
            
           //remove existing contract instance from composition
           composition.removeContractInstance(oldCI);            
           return newCI;
        } else {
           return newContract.createContractInstance();
        }
    }

    private static void updateContractAutor(Contract newContract,
            Composition composition, boolean cloned, String userId) {
        //get the current dashboard contract instance
         ContractInstance oldCI = getVisualizationContractInstance(composition);         
        if (cloned) {
            newContract.setAuthor(userId);
        } else if (oldCI != null) {
             //update the new contract's author with the previous contract's author
             newContract.setAuthor(oldCI.getContract().getAuthor()); 
         }else{
             newContract.setAuthor(composition.getAuthor());
         }
    }

    private static void generateQueryWidgetVisualElement(
            List<Widget> queryWidgets, Contract contract, boolean isStaticData) throws HipieException {
        
        List<Element> queryServices = contract.getOutputElements().stream().filter(element -> element instanceof ServiceElement)
                .collect(Collectors.toList());
        
        for (Widget widget : queryWidgets) {
            widget.updateDUDFieldNames(contract);
            
            Optional<Element> outputElementOption = null;
            Element outputElement = null;
            
            //Adding filter form for databomb/query widgets
            if(widget.hasLocalFilters()) {
                LOGGER.debug("Query/Databomb Form Element - {}", widget.generateFormElement());
                contract.getVisualElements().iterator().next().addChildElement(widget.generateFormElement());
            }
            VisualElement visualElement = widget.generateVisualElement();           
            // fetching the output element which is previously set into contract
            // and set it as 'Basis' for visual element
            for (Element service : queryServices) {
                outputElementOption = getOutputElementOption(service,widget,isStaticData);                   
                if (outputElementOption.isPresent()) {
                    outputElement = outputElementOption.get();
                    break;
                }
            }
            visualElement.setBasis((OutputElement) outputElement);
            contract.getVisualElements().iterator().next().addChildElement(visualElement);
        }
    }

    private static Optional<Element> getOutputElementOption(Element service,
            Widget widget,boolean isStaticData) {
        Optional<Element> outputElementOption = null;
        if(isStaticData){
            outputElementOption = service.getChildElements().stream()
                    .filter(output -> widget.getQueryOutput().getName().equals(output.getName())).findAny();
        }else{
            outputElementOption = service.getChildElements().stream()
            .filter(output -> widget.getQueryOutput().getDudName().equals(output.getName())).findAny();
        }
        return outputElementOption;
    }

    /**
     * Set the property for each unique data source and hook it up with the
     * corresponding output.
     * 
     * @param pluginOutput
     * @param dashboard
     * @param contractInstance
     * @throws HipieException
     * @throws Exception
     */
    private static void hookWidgetDatasource(
            Map<PluginOutput, DudElement> pluginOutputs,
            List<Widget> logicalFileWidgets, ContractInstance contractInstance)
            throws HipieException {
        Iterator<Map.Entry<PluginOutput,DudElement>> iterator = pluginOutputs.entrySet().iterator();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("pluginOutputs -->{}", pluginOutputs);
        }
        while (iterator.hasNext()) {
            
            Entry<PluginOutput,DudElement> dataSourceEntry = iterator.next();
            PluginOutput pluginOutput = dataSourceEntry.getKey();
            DudElement dudElement = dataSourceEntry.getValue();
            
            try {
                hookCIFields(contractInstance,dudElement.getInputElement());
                // Hooking contract instance to composition with respect to datasource
                contractInstance.addPrecursor(pluginOutput
                        .getContractInstance(), pluginOutput.getOutputElement()
                        .getName(), dudElement.getInputElement().getName());
            } catch (Exception e) {
                throw new HipieException("Unable to add new precursor", e);
            }
        }
    }


    /**  Adding the measures and attribute properties to the contractInstance
     * @param contractInstance
     * @param inputElement
     */
    public static void hookCIFields(ContractInstance contractInstance, Element inputElement) {
        List<Element> childElements = inputElement.getChildElements();
        
        for (Element childEle: childElements) {
            if (childEle.getType().equals("FIELD")) {
                contractInstance.setProperty(childEle.getName(), childEle.getOption(Element.LABEL).getParam(0));
            }
        }
    }

    private static void generateQueryService(Contract contract, Map<String, QuerySchema> queries, boolean isStaticData) {
        
        if(!isStaticData) {
            generateQueryOutputDUDName(queries);
        }
       
        Iterator<Entry<String, QuerySchema>> entryItr = queries.entrySet().iterator();
        Entry<String, QuerySchema> queryEntry = null;
        QuerySchema schema = null;
        String query = null;
        while (entryItr.hasNext()) {
            queryEntry = entryItr.next();
            schema = queryEntry.getValue();

            query = queryEntry.getKey();
            
            String serviceName;
            String xpath;
            if(isStaticData) {
                /*
                 * For static data, escaped absolute path is kept in place of Service name and
                 *  Actual name is kept as XPATH attribute 
                 */
                serviceName = RampsUtil.removeSpaceSplChar(query);
                xpath = query;
            } else {
                serviceName = query.replaceAll("\\.", "_")+ SERVICE;
                xpath = query + RESPONSE;
            }

            ServiceElement serviceElement = new ServiceElement(Element.TYPE_DATASET, serviceName, contract.getParseContext());

            serviceElement.addOption(new ElementOption(Element.XPATH, new FieldInstance(null, xpath)));
            ElementOption soap = new ElementOption(ServiceElement.SOAP);
            // Url "HIPIE.HIPIEConfig.RoxieInternalServiceUrl" to point to the
            // same Roxie server
            soap.getParams().add(new FieldInstance(null, SERVICE_URL));
            soap.getParams().add(new FieldInstance(null, query));
            serviceElement.addOption(soap);

            generateInputParamater(serviceElement, schema);

            generateOutput(serviceElement, schema, isStaticData);

            contract.getOutputElements().add(serviceElement);
        }

    }

    /**
     * Sets the query output name as 'QueryOutput1' which will be refered in DUD
     * file
     * 
     * @param queries
     */
    private static void generateQueryOutputDUDName(Map<String, QuerySchema> queries) {
        Iterator<Entry<String, QuerySchema>> entryItr = queries.entrySet().iterator();
        Entry<String, QuerySchema> queryEntry = null;
        QuerySchema schema = null;
        int outputCount = 1;
        while (entryItr.hasNext()) {
            queryEntry = entryItr.next();
            schema = queryEntry.getValue();
            for (OutputSchema output : schema.getOutputs()) {
                output.setDudName(Dashboard.QUERY_OUTPUT + outputCount);
                outputCount++;
            }
        }
    }

    private static void generateOutput(ServiceElement serviceElement, QuerySchema schema,boolean isStaticData) {
        // service outputs
        if (schema.getOutputs() != null && !schema.getOutputs().isEmpty()) {
            schema.getOutputs()
                    .stream()
                    .forEach(
                            queryOutput -> {
                                OutputElement outputElement = createQueryOutputElement(queryOutput,isStaticData,serviceElement);
                                
                                outputElement.addOption(new ElementOption(Element.XPATH, new FieldInstance(null, queryOutput.getxPath())));
                                queryOutput
                                        .getFields()
                                        .stream()
                                        .forEach(
                                                outputField -> outputElement.addChildElement(new OutputElement(outputField.getDataType(), outputField
                                                        .getColumn(), serviceElement.getParseContext())));
                                serviceElement.addChildElement(outputElement);
                            });
        }
    }

    private static OutputElement createQueryOutputElement(
            OutputSchema queryOutput, boolean isStaticData,ServiceElement serviceElement) {
        OutputElement outputElement = null;
        if(!isStaticData){
            outputElement = new OutputElement(Element.TYPE_DATASET, queryOutput.getDudName(), serviceElement
                    .getParseContext());
        }else{
            outputElement = new OutputElement(Element.TYPE_DATASET, queryOutput.getName(), serviceElement
                    .getParseContext());
        }
        return outputElement;
    }

    private static void generateInputParamater(final ServiceElement serviceElement, QuerySchema schema) {
        // creates DUD-service file input parameter element for the query
        if (schema.getInputParameters() != null && !schema.getInputParameters().isEmpty()) {
            schema.getInputParameters()
                    .stream()
                    .forEach(
                            inputParam -> serviceElement.getInputElements().add(
                                    new InputElement(inputParam.getDataType(), inputParam.getColumn(), serviceElement.getParseContext())));
            // Setting input parameter's selected value
            List<Filter> inputsWithValue = schema.getInputParameters().stream()
                    .filter(inputParam -> !StringUtils.isEmpty(inputParam.getValue())).collect(Collectors.toList());

            inputsWithValue.stream().forEach(
                    inputParam -> {
                        Element inputElement = serviceElement.getInputElements().stream()
                                .filter(input -> inputParam.getColumn().equals(input.getName())).findFirst().get();
                        inputElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, inputParam.getValue())));
                    });
            // Setting default value to input parameter which are not selected
            List<Filter> inputsWithoutValue = schema.getInputParameters().stream()
                    .filter(inputParam -> StringUtils.isEmpty(inputParam.getValue())).collect(Collectors.toList());

            inputsWithoutValue.stream().forEach(
                    inputParam -> {
                        Element inputElement = serviceElement.getInputElements().stream()
                                .filter(input -> inputParam.getColumn().equals(input.getName())).findFirst().get();
                        inputElement.addOption(new ElementOption(Element.OPTIONAL));
                    });
        }

    }

    private static Contract createContract(Composition composition, boolean cloned, String userId) throws RepoException, HipieException {
        Contract contract;
        try {
            contract = new Contract();
        } catch (Exception e) {
            throw new HipieException("Unable to create instance", e);
        }
        String contractName = composition.getName() + Dashboard.CONTRACT_IDENTIFIER;

        HIPIEService hipieService = HipieSingleton.getHipie();
        if (hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO) == null) {
            throw new RepoException(Labels.getLabel(HIPIE_REPO_MISSING));
        }
        
        contract.setRepository(hipieService.getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO));
        contract.setLabel(contractName);
        contract.setName(contractName);       
        updateContractAutor(contract, composition, cloned, userId);
        
        contract.setDescription(PLUGIN_DESC);
        contract.setProp(Contract.CATEGORY, VISUALIZE);
        contract.setProp(Contract.VERSION, VERSION);
        return contract;
    }

    /**
     * if composition has multiple datasources, it removes all and makes
     * composition to hold only one datasource And the datasource properties
     * will be reset
     * 
     * @param composition
     */
    public static void removeMultipleDatasources(Composition composition) {
        List<ContractInstance> dataSources = RampsUtil.getAllDatasourceContractInstance(composition);
        Iterator<ContractInstance> iterator = dataSources.iterator();
        // skipping first datasource as a composition should have one datasource
        ContractInstance datasource = null;
        if (iterator.hasNext()) {
            datasource = iterator.next();
            composition.removeContractInstance(datasource);
        }
    }

    public static ContractInstance createDatasourceInstance(Contract contract) {
        ContractInstance datasource = contract.createContractInstance();

        datasource.setFileName("");
        datasource.setProperty(STRUCTURE, new RecordInstance());
        Map<String, Property> paramMap = new HashMap<String, Property>();
        Property fileProp = new Property();
        Property methodProp = new Property();
        fileProp.add(new String("file"));
        methodProp.add(THOR);
        paramMap.put(LOGICAL_FILE, fileProp);
        paramMap.put(METHOD, methodProp);
        datasource.setAllProperties(paramMap);

        return datasource;
    }

    public static Map<String, QuerySchema> extractQueries(ContractInstance contractInstance,boolean isStaticData) {
        Map<String, QuerySchema> queries = new HashMap<String, QuerySchema>();
        Contract contract = contractInstance.getContract();
        List<ServiceElement> queryServices = contract.getOutputElements().stream().filter(element -> element instanceof ServiceElement)
                .map(element -> (ServiceElement) element).collect(Collectors.toList());

        QuerySchema schema = null;
        for (ServiceElement service : queryServices) {
            schema = new QuerySchema();            
            if(isStaticData) {
             // XPATH name will be the static data file's absolute path,Setting absolute path as name the Query schema.
                schema.setName(service.getOptions().get(Element.XPATH).getParams().get(0).getName());
            } else {
             // XPATH name will be 'queryNameResponse'.So stripping 'Response' to get query name
                schema.setName(StringUtils.removeEnd(service.getOptions().get(Element.XPATH).getParams().get(0).getName(), RESPONSE));
            }
            
            schema.setInputParameters(extractInputparameters(service));
            schema.setOutputs(extractOutput(service,isStaticData));
            queries.put(schema.getName(), schema);
        }
        return queries;
    }

    private static List<OutputSchema> extractOutput(ServiceElement service,boolean isStaticData) {

        List<OutputSchema> outputSchemas = new ArrayList<OutputSchema>();
        OutputSchema outputSchema = null;
        List<Field> fields = new ArrayList<Field>();
        String xpath = null;
        for (Element element : service.getChildElements()) {
            if (element instanceof OutputElement) {

                xpath = element.getOption(Element.XPATH).getParams().get(0).getName();
                outputSchema = new OutputSchema();
                if(isStaticData){
                    outputSchema.setName(element.getName());
                }else{
                    outputSchema.setDudName(element.getName()); 
                    outputSchema.setxPath(xpath);
                }
                
                element.getChildElements().stream().forEach(childElement -> {
                    Field field = new Field(childElement.getName(), childElement.getType());
                    fields.add(field);
                });

                outputSchema.setFields(fields);
            }
            outputSchemas.add(outputSchema);
        }

        return outputSchemas;
    }

    private static List<Filter> extractInputparameters(ServiceElement service) {
        List<Filter> inputParameters = new ArrayList<Filter>();

        service.getInputElements().stream().forEach(inputElement -> {
            Filter inputParam = new Filter(new Field(inputElement.getName(), inputElement.getType()));
            ElementOption option = inputElement.getOption(Element.DEFAULT);
            if (option != null) {
                inputParam.setValue(option.getParams().get(0).getName());
            }
            inputParameters.add(inputParam);
        });
        return inputParameters;
    }
    
    public static boolean isChartNameDuplicate(DashboardConfig config, WidgetConfig widgetConfig) {
        List<Widget> allWidgets = config.getDashboard().getNonGlobalFilterWidget();
        if(allWidgets != null){
            List<String> chartNames = new ArrayList<String>();
            for (Widget widget : allWidgets) {
                chartNames.add(widget.getName());
            }
            if(!widgetConfig.isNewCreation()){
                chartNames.remove(widgetConfig.getChartname());
            }
            return chartNames.contains(widgetConfig.getChartname().trim());
        }else{
            return false; 
        }

    }
    public static  Map<PermissionType, Permission> clonePermissions(Composition composition){
        Map<PermissionType, Permission> permissions = new LinkedHashMap<PermissionType, Permission>();
        // Cloning Permission from composition and Creating Security UI
        for (Entry<PermissionType, Permission> entry : composition.getPermissions().entrySet()) {
            Permission permission = new Permission();
            permission.setPermissionLevel(entry.getValue().getPermissionLevel());
            permission.setPermissionType(entry.getValue().getPermissionType());

            Set<String> groups = new HashSet<String>();
            groups.addAll(entry.getValue().getGroups());
            permission.setGroups((HashSet<String>) groups);

            Set<String> users = new HashSet<String>();
            users.addAll(entry.getValue().getUserIds());
            permission.setUserIds((HashSet<String>) users);

            permissions.put(entry.getKey(), permission);
        }
        return permissions;
    }

    /**
     * Returns true, if the output type is 'WUID'(ie output generated as DATASET dsOutput(dsInput): WUID;),
     * otherwise returns false
     * @param contractInstance
     * @return boolean
     */
    public static boolean extractRunOption(ContractInstance contractInstance) {
        boolean isSmallDataset = true;
        if(contractInstance != null){
            Contract visualizationContract = contractInstance.getContract();        
            isSmallDataset = visualizationContract.getOutputElements().stream().filter(outputEle -> outputEle.getOption("WUID") != null).findAny().isPresent();
        }
        return isSmallDataset;
    }
    
    public static boolean hasRunAsLargeDataset(Composition composition) {
        ContractInstance visualizationCI = getVisualizationContractInstance(composition);
        if(visualizationCI != null){
            return visualizationCI.getContract()
                                  .getOutputElements()
                                  .stream()
                                  .filter(outputEle -> outputEle.getOptions() != null)
                                  .filter(outputEle -> outputEle.getOptions()
                                                                  .values()
                                                                  .stream()
                                                                  .filter(option -> option.getName() != null)
                                                                  .filter(option -> OutputElement.LARGE.equals(option.getName()) || (option.getName().equals(ServiceElement.SOAP) || option.getName().equals(ServiceElement.XPATH))).findAny().isPresent())
                                                                  .findAny()
                                                                  .isPresent();
            
        } 
        return false;
    }
    
    public static void cloneQuerySchema(Dashboard dashboard) throws CloneNotSupportedException{
        dashboard.setOriginalQueries(new HashMap<String, QuerySchema>(dashboard.getQueries()));
        dashboard.getQueries().clear();
        for(Map.Entry<String, QuerySchema> entry : dashboard.getOriginalQueries().entrySet() ){
            dashboard.getQueries().put(entry.getKey(), entry.getValue().clone());
        }
     
    }
    
    public static Collection<String> getAllUsers() throws AuthenticationException {
        Collection<User> users = new ArrayList<>();
            UserService userService = (UserService) SpringUtil.getBean("userService");
            users=   userService.getAllUsers();
        
        return users.stream()
                .map(User -> User.getUserName().toLowerCase())
                .collect(Collectors.toList());

    }
    

    /**
     * This method is for compatibility. Current versions of Templates includes viz version global variables
     *  - Adds viz version as '_' if not available
     * @param composition
     * @param forceAdd
     *  - Adds viz version even if composition is not set to 'Run as largedata'
     * @return
     *  Whether visualization version is added
     */ 
    public static boolean addVizVersion(Composition composition, boolean forceAdd) {
        //Setting 'HIPIE Version global variable when appropiate
        if(forceAdd || hasRunAsLargeDataset(composition)) {
            Optional<Element> versionElement = extractVersionElement(composition);
            if(!versionElement.isPresent()) {
                composition.getInputElements().add(createVersionElement("1"));
                LOGGER.debug("Added viz version. Composition - {}", composition);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sets the visualization version to compostion
     * @param composition
     * @param version
     */
    public static void setVizVersion(Composition composition,String version) {
        Optional<Element> versionElement = extractVersionElement(composition);
        
        if(versionElement.isPresent()) {
            versionElement.get().addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, "\""+ version +"\"")));
        } else {
            composition.getInputElements().add(createVersionElement(version));
        }
        LOGGER.debug("Set visualization version.\n{}", composition);
    }

    public static Optional<Element> extractVersionElement(Composition composition) {
        return composition
                    .getInputElements()
                        .stream()
                        .filter(e -> e.getName().equals(Constants.VIZ_SERVICE_VERSION))
                        .findAny();
    }

    private static InputElement createVersionElement(String version) {
        InputElement newElement = new InputElement();
        newElement.setName(Constants.VIZ_SERVICE_VERSION);
        newElement.setType(InputElement.TYPE_STRING);
        newElement.addOption(new ElementOption(Element.DEFAULT, new FieldInstance(null, "\""+ version +"\"")));
        return newElement;
    }
    
}
