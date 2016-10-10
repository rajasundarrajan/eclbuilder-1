package org.hpccsystems.dsp.service;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.entity.Entity;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HPCCException;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.ws.client.platform.DFUFileDetailInfo;

public interface HPCCService {

    boolean sprayFlatHPCCFile(File file, HPCCConnection hpccConnection, String logicalFile, int recordLength) throws HPCCException;

    void sprayCustomCSVHPCCFile(File file, HPCCConnection hpccConnection, String logicalFile, List<String> fields, String escapedFieldDelim,
            String escapedQuote, String escapedRecTerminator) throws HPCCException;

    DFUFileDetailInfo getFileDetail(String fileName, HPCCConnection hpccConnection, String cluster) throws HPCCException;

    List<Entity> getFileContents(String logicalFilename, HPCCConnection hpccConnection, String cluster, int recordCount) throws HPCCException;

    boolean convertToTHOR(HPCCConnection hpccConnection, List<String> fields, String logicalFile) throws HPCCException;

    List<String> getQueries(HPCCConnection hpccConnection) throws HPCCException;

    QuerySchema getQuerySchema(String queryName, HPCCConnection hpccConnection) throws HPCCException;

    boolean isPublicCluster(String cluster);

    void updatePublicCluster(String cluster, boolean isPublicCluster) throws DatabaseException;

    List<String> getPublicClusters() throws DatabaseException;

    Set<String> getPublicAndCustomClusters(User user, Map<String, HPCCConnection> connections) throws DatabaseException;
    
    boolean isPublicRepository(String repo) throws DatabaseException;

    void updatePublicRepository(String repo, boolean isPublicRepo) throws DatabaseException;

    List<String> getPublicRepositories() throws DatabaseException;
    
    Set<String> getPublicAndCustomRepositories(User user,  Map<String, IRepository> repos) throws DatabaseException;
  
    Collection<String> getCustomGroups(String cluster) throws DatabaseException;
    
    void updateCustomClusterPermissions(List<String> groups, String cluster) throws DatabaseException;
    
    void removeOlderCustomPermissions(String cluster) throws DatabaseException;

}
