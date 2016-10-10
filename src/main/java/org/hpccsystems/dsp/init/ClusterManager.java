package org.hpccsystems.dsp.init;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelException;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.UniqueId;
import org.apache.catalina.tribes.group.GroupChannel;
import org.hpcc.HIPIE.HPCCManager;
import org.hpcc.HIPIE.repo.FileRepository;
import org.hpcc.HIPIE.repo.GitRepository;
import org.hpcc.HIPIE.repo.HPCCRepository;
import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryManager;
import org.hpcc.HIPIE.repo.RepositoryType;
import org.hpcc.HIPIE.utils.HPCCConnection;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.ramps.entity.RepositoryConfig;
import org.hpccsystems.dsp.ramps.entity.RepositoryConfig.ACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppInit;

import com.google.gson.Gson;

public class ClusterManager implements WebAppInit {

    private final class ChannelListenerImplementation implements ChannelListener {
        @Override
        public void messageReceived(Serializable data, Member member) {
            LOGGER.info("Processing message from {}\n Message is - {}", member.getHost(), data); 
            
            try {
                Gson gson = new Gson();
                if(data instanceof String){
                    String dataReceived = (String)data;
                    
                    if(!dataReceived.contains(SERVER_HOST)){                        
                        //Handles Repositories
                        RepositoryConfig dspConfig = gson.fromJson(dataReceived,RepositoryConfig.class);
                        LOGGER.info("RepositoryConfig received --->{}",dspConfig); 
                        LOGGER.info("Action --->{}",dspConfig.getAction());
                        
                        if(ACTION.UPDATE.equals(dspConfig.getAction())){
                            updateRepository(dspConfig);
                        }else if(ACTION.ADD.equals(dspConfig.getAction())){
                            addRepository(dspConfig);
                        }
                    } else {
                       
                        HPCCConnection connection = gson.fromJson(dataReceived,HPCCConnection.class);
                        LOGGER.info("DSP HPCCConnection received --->{}",connection);                           
                        
                        //Fetches the HipeConnection object which is in Hipie memory
                        HPCCConnection hipieConnection = HipieSingleton.getHipie()
                                .getHpccManager().getConnections().get(connection.getLabel());
                        LOGGER.info("Hipie HPCCConnection --->{}",hipieConnection);
                        
                        //if object is not null, it is updating the existing Repo
                        if (hipieConnection != null) {
                            updateCluster(connection);
                            // Adds new repo
                        } else {
                            addCluster(connection);
                        }
                    } 
                }
               
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION,e);                    
            }
        }

        @Override
        public boolean accept(Serializable data, Member member) {
            LOGGER.info("Accepting message from {}\n Message is - {}", member.getHost(), data);
            return true;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManager.class);

    protected static final String ACTION_TYPE = "action";
    
    private static final String SERVER_HOST = "serverHost";
    private static Channel channel;
    private static final String PWD = "pwd";
    
    @Override
    public void init(WebApp wapp) throws Exception {
        LOGGER.info("Server info - {}", wapp.getServletContext().getServerInfo());
        
        channel = new GroupChannel();
        
        //This ChannelListener will be invoked when ever there is a post(calling channel.send()) to this Channel
        channel.addChannelListener(new ChannelListenerImplementation());
        
        //start the channel
        channel.start(Channel.DEFAULT);

        //retrieve my current members
        Member[] group = channel.getMembers();
        
        Stream<Member> stream = Stream.of(group);
        stream.forEach(m -> LOGGER.info("Member: ID - {}, Obj - {} ", m.getUniqueId(), m));
        LOGGER.info("Cluster size - {}", group.length);
        
    }
    
    /**
     * Sync the newly added cluster into other server by saving the cluster details into repository.cfg file
     * of other cluster
     */
    protected void addCluster(HPCCConnection connection) {
        HPCCConnection hipieConnection = new HPCCConnection();      
        setHPCCConnection(connection, hipieConnection); 
        HPCCManager hipieManager = HipieSingleton.getHipie().getHpccManager();
        try {
            hipieManager.addConnection(connection.getLabel(), hipieConnection);
            hipieManager.saveProperties();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
        }
       
    }

    /**
     * Updates the cluster detail, which are running in other Server
     */
    protected void updateCluster(HPCCConnection connection) {
        HPCCManager hipieManager = HipieSingleton.getHipie().getHpccManager();
        HPCCConnection hipieConnection = hipieManager.getConnections().get(connection.getLabel());
        setHPCCConnection(connection, hipieConnection);
        
        try {
            hipieManager.saveProperties();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
        }
    }

    private void setHPCCConnection(HPCCConnection dspConnection, HPCCConnection hipieConnection ) {
        try{
            BeanUtils.copyProperties(dspConnection, hipieConnection, new String[]{PWD});
            
            if (dspConnection.getPwd() != null) {
                //Sets the decrypted pwd which returned by getPwd()
                hipieConnection.setPwd(dspConnection.getPwd());    
            } else if (hipieConnection.getPwd() == null) {
               hipieConnection.setPassword(null);
            }           
        }catch(Exception e){
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    /**
     * Sync the newly added Repository info into other server's repositories.cfg file.
     */
    protected void addRepository(RepositoryConfig dspConfig) {
        
        try {            
            IRepository newRepo = null;
            if (RepositoryType.FILE.equals(dspConfig.getType())) { 
                newRepo = new FileRepository("", dspConfig.getPath());
                ((FileRepository) newRepo).setType(RepositoryType.FILE);
                
            }else if (RepositoryType.GIT.equals(dspConfig.getType())) {
                Map<String, Object> props = new HashMap<String, Object>();
                props.put(GitRepository.TYPE, RepositoryType.GIT.toString());
                props.put(GitRepository.URL, dspConfig.getUrl());
                props.put(GitRepository.USER, dspConfig.getGitUserName());
                props.put(GitRepository.PASSWORD, dspConfig.getGitPassword());
                props.put(GitRepository.LOCALDIR, dspConfig.getPath());
                newRepo = new GitRepository(props);
                
            }else{
                newRepo = new HPCCRepository(dspConfig.getServerHost(), dspConfig.getEspPort(),
                        dspConfig.getAttrPort(), dspConfig.getHpccUserName(), dspConfig.getHpccPassword(), dspConfig.isHTTPS(), dspConfig.getPath());
                ((HPCCRepository)newRepo).setType(RepositoryType.LEGACY_HPCC);
                
            }            
            LOGGER.info("New Repo --->{}",newRepo);
            RepositoryManager repoManager =  HipieSingleton.getHipie().getRepositoryManager();
            repoManager.addRepository(dspConfig.getRepository(), newRepo);
            repoManager.saveRepositoryProperties();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    /**
     *  Service to sync the updated Repository details into the HipieRepos object which are running in other clusters
     *  and saves those changes into repositories.cfg file  
     */
    protected void updateRepository(RepositoryConfig dspConfig) {
        
        RepositoryManager repoManager =  HipieSingleton.getHipie().getRepositoryManager();
        IRepository hipieRepo = repoManager.getRepos().get(dspConfig.getRepository());
        LOGGER.info("Modifying Repo - {}", hipieRepo.getName());
        
        if (RepositoryType.FILE.equals(dspConfig.getType())) {
            
            FileRepository fileRepo = (FileRepository) hipieRepo;
            fileRepo.setLocaldir(dspConfig.getPath());
            fileRepo.setType(RepositoryType.FILE);
            
        }else if (RepositoryType.GIT.equals(dspConfig.getType())) {
            
            Map<String, Object> props = hipieRepo.getSaveProperties();
            props.put(GitRepository.URL, dspConfig.getUrl());
            props.put(GitRepository.USER,dspConfig.getGitUserName());
            props.put(GitRepository.PASSWORD, dspConfig.getGitPassword());
            props.put(GitRepository.TYPE, RepositoryType.GIT);
            props.put(GitRepository.LOCALDIR, dspConfig.getPath());

        }else{
            HPCCRepository hpccRepo = (HPCCRepository) hipieRepo;
            hpccRepo.setType(RepositoryType.LEGACY_HPCC);
            hpccRepo.setLocaldir(dspConfig.getPath());
            hpccRepo.setServerHost(dspConfig.getServerHost());
            hpccRepo.setServerPort(dspConfig.getEspPort());
            hpccRepo.setUsername(dspConfig.getHpccUserName());
        }
        
        try {
            repoManager.saveRepositoryProperties();
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION, e);
        }
    }

    /**
     * Invokes 'send()' service to Add/update the Hipie repository details into 'repositories.cfg' file
     */
    public static void syncRepository(RepositoryConfig updateRepositoryConfig) {
        if(channel != null){
            Member localMember = channel.getLocalMember(false);
            Member[] filteredGroup = getMemberToPost();
    
            Gson gson = new Gson();
            String jsonString = gson.toJson(updateRepositoryConfig);
            LOGGER.info(" RepositoryConfig json string in send() -->{}", jsonString);
            
            try {
    
                UniqueId messageId = channel.send(filteredGroup, jsonString, Channel.SEND_OPTIONS_DEFAULT);
                LOGGER.info("Sent message from {}. Message Id is - {}", localMember.getHost(), messageId);
            } catch (ChannelException e) {
                LOGGER.error("Serialization error occurred - {}", e);
              
            }
        }else{
            LOGGER.info("syncRepository: No slave hosts to update Repository on");
         }
    }

    /**
     * Invokes channel listener to Add/Update the cluster details
     * @param connection
     */
    public static void syncCluster(HPCCConnection connection) {
        if(channel != null){
            Member[] filteredGroup = getMemberToPost();
       
            Member localMember = channel.getLocalMember(false);
            
            Gson gson = new Gson();
            String jsonString = gson.toJson(connection);
            LOGGER.info("HPCCConnection json string in send() -->{}", jsonString);
            
            try {
    
                UniqueId messageId = channel.send(filteredGroup, jsonString, Channel.SEND_OPTIONS_DEFAULT);
                LOGGER.info("Sent message from {}. Message Id is {}", localMember.getHost(), messageId);
            } catch (ChannelException e) {
                LOGGER.error("Serialization error occurred", e);
            }
        }else{
            LOGGER.info("syncCluster: No slave hosts to update Cluster on");
        }
    }

    /**
     * Returns list of other server/Member to post the message
     */
    private static Member[] getMemberToPost() {
        if(channel != null){
            Member localMember = channel.getLocalMember(false);
    
            LOGGER.info("Cluster size before removing host - {}",channel.getMembers().length);
            // Removing local member
            List<Member> membersToPost = Arrays.asList(channel.getMembers());
            membersToPost.remove(localMember);
    
            LOGGER.info("Cluster size after removing host - {}", membersToPost.size());
            return membersToPost.toArray(new Member[0]);
        }else{
            LOGGER.info("getMemberToPost: No slave hosts to update");
            return null;
        }
    }
    
}
