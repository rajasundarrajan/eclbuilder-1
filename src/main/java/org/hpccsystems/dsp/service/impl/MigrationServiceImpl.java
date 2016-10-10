package org.hpccsystems.dsp.service.impl;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.Contract;
import org.hpcc.HIPIE.ContractInstance;
import org.hpcc.HIPIE.repo.GitRepository;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.git.PromoteFilesParams;
import org.hpccsystems.dermatology.exception.DermatologyException;
import org.hpccsystems.dermatology.service.impl.DermatologyServiceImpl;
import org.hpccsystems.dsp.CompositionUtil;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.dao.UpstreamDao;
import org.hpccsystems.dsp.dashboard.Dashboard;
import org.hpccsystems.dsp.dashboard.entity.StaticData;
import org.hpccsystems.dsp.dashboard.entity.widget.QuerySchema;
import org.hpccsystems.dsp.dashboard.util.DashboardUtil;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.Notification;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.service.MailService;
import org.hpccsystems.dsp.service.MigrationService;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.usergroupservice.Group;
import org.hpccsystems.usergroupservice.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.zkoss.util.resource.Labels;
import org.zkoss.zkplus.spring.SpringUtil;

@Service("migrationService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MigrationServiceImpl implements MigrationService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationServiceImpl.class);
    private DSPDao dspDao;
    private UpstreamDao upstreamDao; 
    private MailService mailService; 
    DBLogger dbLogger;

    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }
    
    @Autowired
    public void setUpstreamDao(UpstreamDao upstreamDao) {
        this.upstreamDao = upstreamDao;
    }
    
    @Autowired
    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }
    
    @Autowired
    public void setDBLogger(DBLogger dbLogger) {
        this.dbLogger = dbLogger;
    }


    @Override
    public List<Composition> migrateToUpstream(List<Composition> compositions,String userId, String sessionId) throws HipieException {
        LOGGER.debug("Migrating compostions: {}", compositions.stream().map(cmp -> cmp.getCanonicalName()).collect(Collectors.toList()));
        
        List<Composition> failedComps = new ArrayList<>();
        List<String>recipients= new ArrayList<>();
        long startTime = Instant.now().toEpochMilli();
        
        for (Composition composition : compositions) {
            if (composition.getFileName() == null || composition.getRepository() == null) {
                failedComps.add(composition);
                LOGGER.error("The composition {} either don't have a repository or file name, so it is excluded from promoting.",
                        composition.getCanonicalName());
                if (LOGGER.isDebugEnabled()) {
                    dbLogger.log(new Notification(sessionId, userId, startTime, Notification.INVALID_COMPOSITION, format("The composition {} either don't have a repository or file name",composition.getName())));
                }
            }
        }
        compositions.removeAll(failedComps);
        
        if(CollectionUtils.isEmpty(compositions)) {
            throw new HipieException(Labels.getLabel("repositoryOrFileMissingForCompositions"));
        }

        promoteCompositions(compositions, userId);
        
        promoteDashboards(compositions, userId);
        
        try {
            Group grp = HipieSingleton.getHipie()
                        .getPermissionsManager()
                        .getAuthManager()
                        .getGroup(HipieSingleton.getdspDevApprover());
            
            for (User usr : grp.getUsers()) {
                recipients.add(usr.getEmailAddress());
            }
            mailService.notifyReadyforTesting(compositions, recipients);
        } catch (Exception e) {
            dbLogger.log(new Notification(sessionId, userId, startTime, Notification.APPROVER_NOTIFICATION_FAILED, e.getMessage()));
            LOGGER.error(Constants.EXCEPTION, e);
        }
        
        //Migrating database
        for (Composition composition : compositions) {
            try {
                boolean isMigrated = migrateDatabaseToUpstream(composition);
                if(!isMigrated) {
                    failedComps.add(composition);
                }
            } catch (Exception e) {
                //Adding this catch block to catch any DB exception like unique key violation that might occur due to data issue
                dbLogger.log(new Notification(sessionId, userId, startTime, Notification.STATIC_FILE_MIGRATION_FAILED, e.getMessage()));
                LOGGER.error("Failed migration - {}", e);
                failedComps.add(composition);
            }
        }
        
        return failedComps;
    }

    private void promoteDashboards(List<Composition> compositions, String userId) throws HipieException {
        Set<String> files=new HashSet<> ();
        compositions.forEach(cmp -> {
            ContractInstance contractInstance = CompositionUtil.getVisualizationContractInstance(cmp);
            if(contractInstance != null) {
                Contract contract = contractInstance.getContract();
                LOGGER.debug("Contract file name - {}", contract.getFileName());
                LOGGER.debug("Contract Repo - {}", contract.getRepository());
                IRepository dashRepo = HipieSingleton.getHipie().getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO);
                LOGGER.debug("Dashboard Repo - {}", dashRepo);
                
                files.add(contract.getFileName().replace(dashRepo.getLocaldir(), 
                        dashRepo.getLocaldir() + "_promote" + File.separator + userId + File.separator + "promotefrom"));
            }
        });
        
        if(files.isEmpty()) {
            LOGGER.info("No Dashboards to promote");
            return;
        }
        
        //Retriving first composition's repo as All compositions will belong to a single repo
        GitRepository sourcerepo = (GitRepository) HipieSingleton.getHipie().getRepositoryManager().getRepos().get(Dashboard.DASHBOARD_REPO); 
                
        LOGGER.debug("Dashbaord repo - {}", sourcerepo);
        
        //create a new local dir for this user's promotions, from and to
        Map<String,Object> src=sourcerepo.getSaveProperties();
        String srcDir = src.get(GitRepository.LOCALDIR) + "_promote" + File.separator + userId + File.separator + "promotefrom";
        src.put(GitRepository.LOCALDIR, srcDir);
        
        Map<String,Object> dest=sourcerepo.getSaveProperties();
        String destDir = dest.get(GitRepository.LOCALDIR) + "_promote" + File.separator + userId + File.separator + "promoteto";
        dest.put(GitRepository.LOCALDIR, destDir);
        
        //set the url of the promote-to repo to the "upstreamurl" config value
        dest.put(GitRepository.URL, src.get(GitRepository.UPSTREAM_URL));
        
        PromoteFilesParams params;
        try {
            params = new PromoteFilesParams(files, src, dest, "Promoting dashboards", userId);
            LOGGER.debug("Initiating promotion - {}", sourcerepo);
            ErrorBlock eb=HipieSingleton.getHipie().promoteFiles(params);
            if(!eb.isEmpty()) {
                throw new HipieException(eb.toECLErrorString());
            }
        } catch (Exception e) {
            throw new HipieException(Labels.getLabel("promotionFailed"), e);
        } finally {
            cleanTempDirectories(srcDir, destDir);
        }
    }
    
    private void promoteCompositions(List<Composition> compositions, String userId) throws HipieException {
        Set<String> files=new HashSet<> ();
        compositions.forEach(cmp -> {
            LOGGER.debug("Filename - {}", cmp.getFileName());
            files.add(cmp.getFileName().replace(cmp.getRepository().getLocaldir(), cmp.getRepository().getLocaldir() + "_promote" + File.separator + userId + File.separator + "promotefrom"));
        });
        
        //Retriving first composition's repo as All compositions will belong to a single repo
        GitRepository sourcerepo= (GitRepository) compositions.iterator().next().getRepository();
        
        LOGGER.debug("Source repo - {}", sourcerepo);
        
        //create a new local dir for this user's promotions, from and to
        Map<String,Object> src=sourcerepo.getSaveProperties();
        String srcDir = src.get(GitRepository.LOCALDIR) + "_promote" + File.separator + userId + File.separator + "promotefrom";
        src.put(GitRepository.LOCALDIR, srcDir);
        
        Map<String,Object> dest=sourcerepo.getSaveProperties();
        String destDir = dest.get(GitRepository.LOCALDIR) + "_promote" + File.separator + userId + File.separator + "promoteto";
        dest.put(GitRepository.LOCALDIR, destDir);
        
        //set the url of the promote-to repo to the "upstreamurl" config value
        dest.put(GitRepository.URL, src.get(GitRepository.UPSTREAM_URL));
        
        LOGGER.debug("Composition before promotion - {}", compositions.iterator().next().getFileName());
        
        PromoteFilesParams params;
        try {
            params = new PromoteFilesParams(files, src, dest, "Promoting compositions", userId);
            LOGGER.debug("Initiating promotion - {}", sourcerepo);
            ErrorBlock eb=HipieSingleton.getHipie().promoteFiles(params);
            if(!eb.isEmpty()) {
                throw new HipieException(eb.toECLErrorString());
            }
        } catch (Exception e) {
            throw new HipieException(Labels.getLabel("promotionFailed"), e);
        } finally {
            cleanTempDirectories(srcDir, destDir);
        }
        
        LOGGER.debug("Composition after promotion - {}", compositions.iterator().next().getFileName());
    }

    private void cleanTempDirectories(String srcDir, String destDir) {
        try {
            FileUtils.deleteDirectory(new File(srcDir));
            FileUtils.deleteDirectory(new File(destDir));
        } catch (IOException e) {
            LOGGER.error("Error deleting temp directories.", e);
        }
    }
    
    @Override
    public void upgradeVersion(Composition composition) throws HipieException {
        String originalVersion = composition.getVersion();
        String currentVer = originalVersion;
        String upgradedVersion;
        
        //truncating current version to whole number
        if(currentVer.contains(".")) {
            currentVer = StringUtils.substringBefore(currentVer, ".");
        }
        
        //upgrading version
        if(currentVer.matches("[0-9]+")) {
            int ver = Integer.parseInt(currentVer) + 1;
            upgradedVersion = String.valueOf(ver); 
        } else {
            upgradedVersion = "1";
        }
        
        LOGGER.info("Upgrading version of {} from {} to {}", composition.getCanonicalName(), originalVersion, upgradedVersion);
        
        composition.setVersion(upgradedVersion);
        try {
            HipieSingleton.getHipie().saveComposition(composition.getAuthor(), composition);
            
            DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
            dermatologyService.copyDermatology(composition.getId(), originalVersion, upgradedVersion);
        } catch (Exception e) {
            throw new HipieException(Labels.getLabel("unableToSaveComposition") , e);
        }
    }
        
    private boolean migrateDatabaseToUpstream(Composition composition) {
        //Migrate static data files when present
        if(composition.getName().toLowerCase().endsWith(Dashboard.DATA_BOMB_SUFFIX)) {
            Map<String, QuerySchema> files = CompositionUtil.extractQueries(
                    CompositionUtil.getVisualizationContractInstance(composition), true);
            
            List<StaticData> staticFiles  = new ArrayList<>();
            for (String file : files.keySet()) {
                try {
                    String[] userAndFilename = DashboardUtil.getUserAndFilename(file);
                    staticFiles.add(dspDao.getStaticData(userAndFilename[0],userAndFilename[1]));
                } catch (DatabaseException e) {
                    LOGGER.error("Static data migration failed for {}",composition.getCanonicalName() , e);
                    return false;
                }
            }
            
            LOGGER.debug("List of static files - {}", staticFiles);
            upstreamDao.insertStaticDataTable(staticFiles);
        }
        List<org.hpccsystems.dermatology.domain.Dermatology> dermatology = null;
        DermatologyServiceImpl dermatologyService = (DermatologyServiceImpl) SpringUtil.getBean(Constants.DERMATOLOGY_SERVICE);
        try {
            dermatology = dermatologyService.getDermatologies(composition.getId(), composition.getVersion());
        } catch (DermatologyException e) {
            LOGGER.error("Static data migration failed for {}",composition.getCanonicalName() , e);
            return false;
        }
                
        if (dermatology != null && !dermatology.isEmpty()) {
            upstreamDao.insertDermatology(dermatology);
            return true;
        } else {
            return false;
        }
    }

}
