package org.hpccsystems.dsp.log;

import java.io.Serializable;
import java.time.Instant;

import javax.servlet.http.HttpSession;

import org.hpccsystems.dsp.service.AuthenticationService;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zkplus.spring.SpringUtil;

/**
 * The abstract base for all user activity logging. All logging event classes
 * should extend this class.
 * 
 * @author dhanasiddharth
 *
 */
public abstract class DBLog implements Serializable{
    
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private String userId;
    private long startTime;
    private long endTime;
    private long memoryUtilized;
    protected String action;
    private long duration;

    /**
     * Created instance will point to current time, currently logged in user &
     * session.
     * 
     * @param action
     */
    public DBLog(String action, long startTime) {
        this.startTime = startTime;
        this.endTime = Instant.now().toEpochMilli();
        this.memoryUtilized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        AuthenticationService authenticationService = (AuthenticationService) SpringUtil.getBean("authenticationService");
        this.userId = authenticationService.getCurrentUser().getId();
        this.sessionId = ((HttpSession) Sessions.getCurrent().getNativeSession()).getId();
        this.action = action;
        this.duration = endTime - startTime;
    }

    /**
     * Initializes with the userId in parameter instead of retrieving from
     * session
     * 
     * @param userId
     * @param action
     */
    public DBLog(String userId, String action, long startTime) {
        this.startTime = startTime;
        this.userId = userId;
        this.sessionId = ((HttpSession) Sessions.getCurrent().getNativeSession()).getId();
        this.action = action;
        this.memoryUtilized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        this.endTime = Instant.now().toEpochMilli();
        this.duration = endTime - startTime;
    }

    public DBLog(String sessionId, String userId, long startTime, String action) {
        super();
        this.sessionId = sessionId;
        this.userId = userId;
        this.startTime = startTime;
        this.action = action;
        this.memoryUtilized = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        this.endTime = Instant.now().toEpochMilli();
        this.duration = endTime - startTime;
    }

    public DBLog(String sessionId, String userId, long startTime, String action, long memoryUtilized) {
        super();
        this.sessionId = sessionId;
        this.userId = userId;
        this.startTime = startTime;
        this.action = action;
        this.memoryUtilized = memoryUtilized;
        this.endTime = Instant.now().toEpochMilli();
        this.duration = endTime - startTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    protected void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    protected void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getMemoryUtilized() {
        return memoryUtilized;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public abstract String getDetail();
}
