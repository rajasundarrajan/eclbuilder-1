package org.hpccsystems.dsp.log;


public class UserLog extends DBLog {

    private static final long serialVersionUID = 1L;
    
    String details;

    public UserLog(String userId, String action, String details, long start, String sessionId,long memoryUtilized) {
        super(sessionId, userId, start, action,memoryUtilized);
        this.details = details;
    }

    @Override
    public String getDetail() {
        return details;
    }

}
