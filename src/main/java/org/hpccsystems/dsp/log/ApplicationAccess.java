package org.hpccsystems.dsp.log;


public class ApplicationAccess extends DBLog {
    
    private static final long serialVersionUID = 1L;

    private String status;
    
    public static final String ACTION_LOGIN = "Log in";
    public static final String ACTION_LOGOUT = "Log out";
    
    /**
     * Instantiates a log entry with current time & session. 
     * Use ApplicationAccess(ACTION login, String status) constructor when user details is present in session  
     * @param userId
     *  Overrides the user id in current session
     * @param action
     *  LOGIN / LOGOUT
     * @param status
     *  Detailed status for logging.
     */
    public ApplicationAccess(String userId, String action, String status,long start) {
        super(userId, action, start);
        this.status = status;
    }

    /**
     * Instantiates a log entry with current time & session. User id is retrieved from session
     * @param action
     * @param status
     *   Detailed status message to log.
     */

    public ApplicationAccess(String actionLogin, long start, String status2) {
        super(actionLogin,start);
        this.status = status2;
    }

    public ApplicationAccess(String nm, String actionLogin, long start, String status2) {
        super(nm, actionLogin,start);
        this.status = status2;
    }

    @Override
    public String getDetail() {
        return status;
    }
}
