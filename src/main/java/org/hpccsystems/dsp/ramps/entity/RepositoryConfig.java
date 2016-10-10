package org.hpccsystems.dsp.ramps.entity;

import java.io.Serializable;
import java.util.List;

import org.hpcc.HIPIE.repo.IRepository;
import org.hpcc.HIPIE.repo.RepositoryType;

public class RepositoryConfig implements Cloneable , Serializable{
    
    private static final long serialVersionUID = 1L;
    
    public static enum ACTION {
        ADD, UPDATE, DELETE
    }
    
    /**
     * Unique identifier for Repository stored by HIPIE
     */
    private String repository;
    private String path;
    private RepositoryType type;

    private String url;

    private String gitUserName;

    private String gitPassword;

    private String serverHost;

    private int espPort;

    private int attrPort;

    private String hpccUserName;

    private String hpccPassword;

    private boolean isHTTPS;
    
    private List<String> permittedUsers;

    private transient IRepository iRepository;
    
    private ACTION action;
    
    private boolean gitPwdEncrypted;

    public IRepository getiRepository() {
        return iRepository;
    }

    public void setiRepository(IRepository iRepository) {
        this.iRepository = iRepository;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGitUserName() {
        return gitUserName;
    }

    public void setGitUserName(String gitUserName) {
        this.gitUserName = gitUserName;
    }

    public String getGitPassword() {
        return gitPassword;
    }

    public void setGitPassword(String gitPassword) {
        this.gitPassword = gitPassword;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getEspPort() {
        return espPort;
    }

    public void setEspPort(int espPort) {
        this.espPort = espPort;
    }

    public int getAttrPort() {
        return attrPort;
    }

    public void setAttrPort(int attrPort) {
        this.attrPort = attrPort;
    }

    public String getHpccUserName() {
        return hpccUserName;
    }

    public void setHpccUserName(String hpccUserName) {
        this.hpccUserName = hpccUserName;
    }

    public String getHpccPassword() {
        return hpccPassword;
    }

    public void setHpccPassword(String hpccPassword) {
        this.hpccPassword = hpccPassword;
    }

    public boolean isHTTPS() {
        return isHTTPS;
    }

    public void setHTTPS(boolean isHTTPS) {
        this.isHTTPS = isHTTPS;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public RepositoryType getType() {
        return type;
    }

    public void setType(RepositoryType type) {
        this.type = type;
    }

    public List<String> getPermittedUsers() {
        return permittedUsers;
    }

    public void setPermittedUsers(List<String> permittedUsers) {
        this.permittedUsers = permittedUsers;
    }

    public ACTION getAction() {
        return action;
    }

    public void setAction(ACTION action) {
        this.action = action;
    }

    public boolean isGitPwdEncrypted() {
        return gitPwdEncrypted;
    }

    public void setGitPwdEncrypted(boolean passwordEncrypted) {
        this.gitPwdEncrypted = passwordEncrypted;
    }

}
