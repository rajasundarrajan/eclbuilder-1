package org.hpccsystems.dsp.service.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpcc.HIPIE.utils.Utility;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dashboard.entity.widget.Field;
import org.hpccsystems.dsp.dashboard.entity.widget.Filter;
import org.hpccsystems.dsp.dashboard.entity.widget.OutputSchema;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.entity.Entity;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.log.HipieQuery;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.HPCCService;
import org.hpccsystems.usergroupservice.NameValuePair;
import org.hpccsystems.ws.client.HPCCFileSprayClient.SprayVariableFormat;
import org.hpccsystems.ws.client.HPCCWsClient;
import org.hpccsystems.ws.client.HPCCWsDFUClient;
import org.hpccsystems.ws.client.gen.wsworkunits.v1_58.QuerySetAlias;
import org.hpccsystems.ws.client.gen.wsworkunits.v1_58.WUQuerySetDetailsResponse;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;
import org.hpccsystems.ws.client.platform.Platform;
import org.hpccsystems.ws.client.platform.WorkunitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.zkoss.util.resource.Labels;

@Service("hpccService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class HPCCServiceImpl implements HPCCService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HPCCServiceImpl.class);
    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC = "Basic ";
    public static final String PLUGINREPOSITORY = "PluginRepository";

    DBLogger dblogger;
    private DSPDao dspDao;

    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }

    @Autowired
    public void setDBLogger(DBLogger dblogger) {
        this.dblogger = dblogger;
    }

    @Override
    public boolean sprayFlatHPCCFile(File file, HPCCConnection hpccConnection, String logicalFile, int recordLength) throws HPCCException {
        Platform platform = hpccConnection.getPlatform();
        HPCCWsClient connector = platform.getHPCCWSClient();

        uploadToLandingzone(file, connector);

        String targetCluster = getTargetcluster(connector);

        return connector.sprayFlatHPCCFile(file.getName(), logicalFile, recordLength, targetCluster, true);
    }

    @Override
    public void sprayCustomCSVHPCCFile(File file, HPCCConnection hpccConnection, String logicalFile, List<String> fields, String escapedFieldDelim,
            String escapedQuote, String escapedRecTerminator) throws HPCCException {

        String tempLogicalFile = logicalFile.startsWith("~") ? logicalFile : "~" + logicalFile;

        Platform platform = hpccConnection.getPlatform();
        HPCCWsClient connector = platform.getHPCCWSClient();

        uploadToLandingzone(file, connector);

        String targetCluster = getTargetcluster(connector);

        sprayCSVFile(connector, file.getName(), tempLogicalFile + "CSV", targetCluster, escapedFieldDelim, escapedQuote, escapedRecTerminator);
    }

    public boolean convertToTHOR(HPCCConnection hpccConnection, List<String> fields, String logicalFile) throws HPCCException {
        try {
            HPCCWsClient connector = hpccConnection.getPlatform().getHPCCWSClient();
            WorkunitInfo wu = new WorkunitInfo();
            wu.setECL(generateECL(fields, logicalFile, getTargetcluster(connector)));
            wu.setJobname("myflatoutput");
            wu.setCluster(hpccConnection.getThorCluster());
            wu.setResultLimit(100);
            wu.setMaxMonitorMillis(50000);
            String results = connector.submitECLandGetResults(wu);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Results - {}", results);
            }
        } catch (Exception e) {
            throw new HPCCException(e);
        }
        return true;
    }

    private String generateECL(List<String> fields, String logicalFile, String cluster) {
        StringBuilder ecl = new StringBuilder();

        // Record Structure
        ecl.append("struct := ").append("RECORD ");
        fields.forEach(field -> ecl.append("STRING ").append(field).append("; "));
        ecl.append("END").append("; ");

        ecl.append("\n");

        ecl.append("csvDS := DATASET('").append(logicalFile).append("CSV").append("',struct,CSV(HEADING(1))); \n");
        ecl.append("OUTPUT( csvDS,,'").append(logicalFile).append("',CLUSTER('").append(cluster).append("'),OVERWRITE);");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ECL to convert to THOR \n{}", ecl.toString());
        }

        return ecl.toString();
    }

    private void sprayCSVFile(HPCCWsClient connector, String fileName, String logicalFile, String targetCluster, String escapedFieldDelim,
            String escapedQuote, String escapedRecTerminator) throws HPCCException {

        String tempLogicalFile = logicalFile.startsWith("~") ? logicalFile : "~" + logicalFile;

        if (!connector.sprayCustomCSVHPCCFile(fileName, tempLogicalFile, targetCluster, "\\", escapedFieldDelim, "\"", escapedRecTerminator, true,
                SprayVariableFormat.DFUff_csv)) {
            throw new HPCCException(Labels.getLabel("fileSprayFailure"));
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

    private void uploadToLandingzone(File file, HPCCWsClient connector) throws HPCCException {
        if (!connector.httpUploadFileToFirstHPCCLandingZone(file.getAbsolutePath())) {
            throw new HPCCException(Labels.getLabel("failedToTransferFile"));
        }
    }

    @Override
    public DFUFileDetailInfo getFileDetail(String fileName, HPCCConnection hpccConnection, String cluster) throws HPCCException {
        long startTime = Instant.now().toEpochMilli();
        try {
            DFUFileDetailInfo fileDetail = hpccConnection.getFileDetails(fileName, null);
            if (LOGGER.isDebugEnabled()) {
                dblogger.log(new HipieQuery(HipieQuery.FILE_FETCH, startTime, "Logical file schema retrived"));
            }
            return fileDetail;
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                dblogger.log(new HipieQuery(HipieQuery.FILE_FETCH, startTime, "Failed to retrive logical file schema"));
            }
            throw new HPCCException(e);
        }
    }

    @Override
    public List<Entity> getFileContents(String logicalFilename, HPCCConnection hpccConnection, String cluster, int recordCount) throws HPCCException {
        try {
            Platform platform = hpccConnection.getPlatform();
            HPCCWsDFUClient dfuClient = platform.getWsDfuClient();
            NodeList response = dfuClient.getFileData(logicalFilename, new Long(0), recordCount, getClusterName(hpccConnection, cluster));

            LOGGER.debug("Response - {}", response);

            List<Entity> result = extractFileData(response);

            LOGGER.debug("Contents - {}", result);
            return result;
        } catch (Exception e) {
            throw new HPCCException(e);
        }
    }

    private List<Entity> extractFileData(NodeList nodelist) {
        List<Entity> result = new ArrayList<Entity>();
        if (nodelist != null && nodelist.getLength() > 0) {
            for (int i = 0; i < nodelist.getLength(); i++) {
                Node row = nodelist.item(i);
                if ((row.getNodeType() == Node.ELEMENT_NODE) && (row.hasChildNodes())) {
                    NodeList fieldList = row.getChildNodes();

                    List<Entity> fieldValues = new ArrayList<Entity>();
                    for (int j = 0; j < fieldList.getLength(); j++) {
                        Node field = fieldList.item(j);
                        if (field.getNodeType() == Node.ELEMENT_NODE) {
                            if (field.getChildNodes().getLength() > 1) {
                                fieldValues.add(new Entity(field.getNodeName(), extractFileData(field.getChildNodes())));
                            } else {
                                fieldValues.add(new Entity(field.getNodeName(), field.getTextContent()));
                            }
                        }
                    }

                    result.add(new Entity(fieldValues));
                }
            }
        }
        LOGGER.debug("Values ----->{}", result);
        return result;
    }

    // TODO Use a better way than this method
    private String getClusterName(HPCCConnection hpccConnection, String clusterGroup) throws HPCCException {
        String clusterName = null;
        try {
            LOGGER.debug("hpccID:{}", hpccConnection.getLabel());
            LOGGER.debug("clusterGroup:{}", clusterGroup);
            LOGGER.debug("clusternames:{}", hpccConnection.getClusterNames(clusterGroup));
            clusterName = hpccConnection.getClusterNames(clusterGroup).stream().findFirst().get();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException("Couldn't fetch cluster name", e);
        }
        return clusterName;
    }

    @Override
    public List<String> getQueries(HPCCConnection hpccConnection) throws HPCCException {

        try {
            long startTime = Instant.now().toEpochMilli();
            WUQuerySetDetailsResponse response = hpccConnection.getHpccWorkunitService().getQueriesDetail(hpccConnection.getRoxieCluster(), hpccConnection.getRoxieCluster(), null);
            if (LOGGER.isDebugEnabled()) {
                dblogger.log(new HipieQuery(HipieQuery.FILE_FETCH, startTime, "Queries retrived"));
            }
            QuerySetAlias[] resultsArray = response.getQuerysetAliases();

            LOGGER.debug("resultsArray -->" + resultsArray);
            List<String> queries = sortQueries(resultsArray);

            return queries.stream().filter(query -> !query.contains("ins00") && !query.endsWith("_service")).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException(Labels.getLabel("unableToFetchQueries"), e);
        }
    }

    private List<String> sortQueries(QuerySetAlias[] resultsArray) {
        // Sorting queries by name
        List<String> queries = new ArrayList<String>();
        if (resultsArray != null && resultsArray.length > 0) {
            Arrays.sort(resultsArray, new Comparator<QuerySetAlias>() {
                @Override
                public int compare(QuerySetAlias o1, QuerySetAlias o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            String fileMeta;

            for (QuerySetAlias querySetAlias : resultsArray) {
                fileMeta = querySetAlias.getName();
                queries.add(fileMeta);
            }
        } else {
            queries.add("No queries found.");
        }
        return queries;
    }

    @Override
    public QuerySchema getQuerySchema(String queryName, HPCCConnection hpccConnection) throws HPCCException {
        QuerySchema querySchema = null;

        long startTime = Instant.now().toEpochMilli();
        try {
            querySchema = new QuerySchema();
            querySchema.setOutputs(getOutput(hpccConnection, queryName));
            querySchema.setInputParameters(getInputParameters(queryName, hpccConnection));
            if (LOGGER.isDebugEnabled()) {
                dblogger.log(new HipieQuery(HipieQuery.FILE_FETCH, startTime, "Retrived query schema"));
            }
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                dblogger.log(new HipieQuery(HipieQuery.FILE_FETCH, startTime, "Failed to retrive query schema"));
            }
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HPCCException(Labels.getLabel("unableToFetchQueryFields"), e);

        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("queryData" + querySchema);
        }
        return querySchema;
    }

    private List<Filter> getInputParameters(String queryName, HPCCConnection hpccConnection) throws HipieException {
        List<Filter> params = new ArrayList<Filter>();
        try {
            StringBuilder urlBuilder = new StringBuilder();
            urlBuilder.append(hpccConnection.getRoxieServiceUrl());
            urlBuilder.append("WsEcl/example/request/query/");
            urlBuilder.append(hpccConnection.getRoxieCluster());
            urlBuilder.append("/");
            urlBuilder.append(queryName);
            urlBuilder.append("?display");

            LOGGER.debug("Input parameters Req  URL-->" + urlBuilder);

            URL url = new URL(urlBuilder.toString());
            URLConnection urlConnection = url.openConnection();
            String authString = hpccConnection.getUserName() + ":" + Utility.decrypt(hpccConnection.getPwd());
            String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
            urlConnection.setRequestProperty(AUTHORIZATION, BASIC + authStringEnc);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            Document doc = dBuilder.parse(urlConnection.getInputStream());
            doc.getDocumentElement().normalize();

            Node row = doc.getElementsByTagName(queryName + "Request").item(0);
            NodeList nodeList = row.getChildNodes();
            Filter param = null;

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    param = createInputparams(node);
                    params.add(param);
                }
            }

        } catch (Exception e1) {
            LOGGER.error(Constants.EXCEPTION, e1);
            throw new HipieException("Unable to get input parameters", e1);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Input params-->" + params);
        }

        return params;
    }

    private Filter createInputparams(Node node) {
        return new Filter(new Field(node.getNodeName(), RampsUtil.getHipieDatatype(node.getTextContent())));
    }

    private List<OutputSchema> getOutput(HPCCConnection hpccConnection, String queryName)
            throws GeneralSecurityException, IOException, ParserConfigurationException, SAXException, URISyntaxException, HipieException {
        List<OutputSchema> outputs = new ArrayList<OutputSchema>();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(hpccConnection.getRoxieServiceUrl());
        if (!urlBuilder.toString().endsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append("WsEcl/definitions/query/");
        try {
            urlBuilder.append(hpccConnection.getRoxieCluster());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Couldn't append roxy cluster in uri", e);
        }
        urlBuilder.append("/");
        urlBuilder.append(queryName);
        urlBuilder.append("/main/");
        urlBuilder.append(queryName);
        urlBuilder.append(".xsd");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("URL -> " + urlBuilder.toString());
        }

        URL url = new URL(urlBuilder.toString());
        URLConnection urlConnection = url.openConnection();
        String authString = null;
        try {
            authString = hpccConnection.getUserName() + ":" + Utility.decrypt(hpccConnection.getPwd());
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
            throw new HipieException("Authuntication string initialization failed", e);
        }
        String authStringEnc = new String(Base64.encodeBase64(authString.getBytes()));
        urlConnection.setRequestProperty(AUTHORIZATION, BASIC + authStringEnc);

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

        Document doc = dBuilder.parse(urlConnection.getInputStream());
        doc.getDocumentElement().normalize();
        NodeList importList = doc.getElementsByTagName("xsd:import");
        for (int i = 0; i < importList.getLength(); i++) {
            NamedNodeMap importAttrs = importList.item(i).getAttributes();
            String namesapce = importAttrs.getNamedItem("namespace").getTextContent();
            String schemaLocation = importAttrs.getNamedItem("schemaLocation").getTextContent();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Name space -> {} SchemaLocation - {}", namesapce, schemaLocation);
            }

            URI uri = new URI(urlBuilder.toString());
            url = uri.resolve(schemaLocation).toURL();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Schema URL -> " + url);
            }

            urlConnection = url.openConnection();
            urlConnection.setRequestProperty(AUTHORIZATION, BASIC + authStringEnc);

            doc = dBuilder.parse(urlConnection.getInputStream());
            doc.getDocumentElement().normalize();
            // TODO:Nedd to fetch multiple datasets instead of fetching the
            // first one alone
            NodeList nodeList = doc.getElementsByTagName("xs:element").item(1).getChildNodes().item(1).getChildNodes().item(1).getChildNodes();
            outputs.add(constructOutputSchema(nodeList, namesapce));
        }
        return outputs;
    }

    private OutputSchema constructOutputSchema(NodeList nodeList, String outputName) {

        Set<String> xsdNumericTypes = new HashSet<String>();
        String[] array = { "byte", "decimal", "int", "integer", "long", "negativeInteger", "nonNegativeInteger", "nonPositiveInteger",
                "positiveInteger", "short", "unsignedLong", "unsignedInt", "unsignedShort", "unsignedByte" };
        xsdNumericTypes.addAll(Arrays.asList(array));

        String tagName;
        String type;
        Field field;
        Node typeNode;
        OutputSchema output = null;
        List<Field> fields = new ArrayList<Field>();

        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                field = new Field();
                tagName = nodeList.item(i).getAttributes().getNamedItem("name").getTextContent();
                if ("fpos".equalsIgnoreCase(tagName)) {
                    continue;
                }
                field.setColumn(tagName);
                typeNode = nodeList.item(i).getAttributes().getNamedItem("type");
                if (typeNode != null) {
                    type = typeNode.getTextContent();
                    if (type.lastIndexOf(':') > 0 && xsdNumericTypes.contains(type.substring(type.lastIndexOf(':') + 1))) {
                        field.setDataType(RampsUtil.getHipieDatatype(type.substring(type.lastIndexOf(':') + 1)));
                    } else {
                        field.setDataType(RampsUtil.getHipieDatatype(type));
                    }
                }
                fields.add(field);
            }

        }
        output = new OutputSchema();
        output.setFields(fields);
        output.setNamespace(outputName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Output Schema-->" + output);
        }
        return output;
    }

    @Override
    public boolean isPublicCluster(String cluster) {
        return dspDao.isPublicCluster(cluster);
    }

    @Override
    public void updatePublicCluster(String cluster, boolean isPublicCluster) throws DatabaseException {
        dspDao.updatePublicCluster(cluster, isPublicCluster);
    }

    @Override
    public List<String> getPublicClusters() throws DatabaseException {
        return dspDao.getPublicClusters();
    }
    
    private List<String> getCustomClusters(List<String> roles) throws DatabaseException{
        return dspDao.getCustomClusters(roles);  

    }
    
    @Override
    public Set<String> getPublicAndCustomClusters(User user, Map<String, HPCCConnection> connections) throws DatabaseException {
        Set<String> clusters = new HashSet<String>();
       
        List<String> rolenames = new ArrayList<String>();
        user.getGroups().stream().forEach(grp -> rolenames.add(grp.getMachineName()));
        
        List<String> clustersList = getPublicClusters();
        List<String> customClustersList = getCustomClusters(rolenames);
        
        LOGGER.debug("Public clusters - {}", clustersList);
        
        //Retrieving public cluster
        for (Map.Entry<String, HPCCConnection> entry : connections.entrySet()) {
            if (clustersList.contains(entry.getKey())) {
                clusters.add(entry.getKey());
            }
        }
        
        retriveCustomClusters(connections, clusters, customClustersList);

        //Retrieving custom clusters defined as NVPs
        if (user != null && user.getMbsUser() != null && user.getMbsUser().getNVP(Constants.CLUSTER) != null) {
            Set<NameValuePair> nvps = user.getMbsUser().getNVP(Constants.CLUSTER);
            LOGGER.debug("Clusters NVPs - {}", nvps);            
            for (NameValuePair nvp : nvps) {
                if (connections.containsKey(nvp.getValue())) {
                    clusters.add(nvp.getValue());
                }
            }
        }
        return clusters;
    }

    private void retriveCustomClusters(Map<String, HPCCConnection> connections, Set<String> clusters, List<String> customClustersList) {
        //Retrieving custom clusters
            LOGGER.debug("Custom clusters - {}", customClustersList);
            if (!customClustersList.isEmpty()) {
                for (Map.Entry<String, HPCCConnection> entry : connections.entrySet()) {
                    if (customClustersList.contains(entry.getKey())) {
                        clusters.add(entry.getKey());
                    }
                }
            }
    }


    @Override
    public boolean isPublicRepository(String repo) {
        return dspDao.isPublicRepo(repo);
    }

    @Override
    public void updatePublicRepository(String repo, boolean isPublicRepo) throws DatabaseException{
         dspDao.updatePublicRepository(repo, isPublicRepo);
        
    }

    @Override
    public List<String> getPublicRepositories() throws DatabaseException {
        return dspDao.getPublicRepositories();
    }

    @Override
    public Set<String> getPublicAndCustomRepositories(User user, Map<String, IRepository> repos) throws DatabaseException {
        Set<String> reposList = new HashSet<String>();
        List<String> allClusterList = getPublicClusters();
        for (Map.Entry<String, IRepository> entry : repos.entrySet()) {
            if (allClusterList.contains(entry.getKey())) {
                reposList.add(entry.getKey());
            }
        }

        // if the user has custom clusters defined as nvps, include these in
        // their list as well
        if (user != null && user.getMbsUser() != null && user.getMbsUser().getNVP(PLUGINREPOSITORY) != null) {
            Set<NameValuePair> nvps = user.getMbsUser().getNVP(PLUGINREPOSITORY);
            for (NameValuePair nvp : nvps) {
                if (repos.containsKey(nvp.getValue())) {
                    reposList.add(nvp.getValue());
                }
            }
        }
        return reposList;
    }
    
   @Override
    public List<String> getCustomGroups(String cluster) throws DatabaseException{
        return dspDao.getCustomGroups(cluster);
    }

    @Override
    public void updateCustomClusterPermissions(List<String> groups, String cluster) throws DatabaseException {
        dspDao.updateCustomClusterPermissions(groups, cluster);
    }

    @Override
    public void removeOlderCustomPermissions(String cluster) throws DatabaseException  {
        dspDao.removeOlderCustomPermissions(cluster);
    }

}
