package org.hpccsystems.dsp.log;

import org.hpccsystems.dsp.ramps.utils.RampsLogger;

public class WebserviceInvocation extends DBLog {
    
   
    private static final long serialVersionUID = 1L;

    public static final String ACTION_ACCESS = "WEBSERVICE_ACCESS";
    
    public static final String ACTION_AUTH_STATUS = "WEBSERVICE_AUTH_STATUS";
    
    public static final String ACTION_AUTH_VIA_SERVICE = "WEBSERVICE_AUTH_VIA_SERVICE";
    
    public static final String ACTION_HIPIE_CALL = "WEBSERVICE_HIPIE_CALL";
    
    public static final String ACTION_RESPONSE = "WEBSERVICE_RESPONSE";
    
    public static final String ACTION_SELECT_LAYOUT_INIT = "WEBSERVICE_SELECT_LAYOUT_INIT";
    
    public static final String ACTION_SELECT_LAYOUT_RESPONSE = "WEBSERVICE_SELECT_LAYOUT_RESPONSE_RESPONSE";
    
    public static final String ACTION_FETCH_COMPOSITION_INIT = "WEBSERVICE_FETCH_COMPOSITION_INIT";
    
    public static final String ACTION_FETCH_COMPOSITION_COMPLETED = "WEBSERVICE_FETCH_COMPOSITION_COMPLETED";
    
    public static final String ACTION_FETCH_DDL_INIT = "WEBSERVICE_FETCH_DDL_INIT";
    
    public static final String ACTION_FETCH_DDL_COMPLETED = "WEBSERVICE_FETCH_DDL_COMPLETED";
    
    public static final String ACTION_FETCH_LAYOUT_INIT = "WEBSERVICE_FETCH_LAYOUT_INIT";
    
    public static final String ACTION_FETCH_LAYOUT_COMPLETED = "WEBSERVICE_FETCH_LAYOUT_COMPLETED";
    
    private String message;

    public WebserviceInvocation(String userId, String action, String message,long startTime) {
        super(RampsLogger.THREAD_ID.get(), userId, startTime, action);
        this.message = message;
    }
    
    public WebserviceInvocation(String action, String message,long startTime) {
        super(RampsLogger.THREAD_ID.get(), RampsLogger.USER_ID.get(), startTime, action);
        this.message = message;
    }

    @Override
    public String getDetail() {
        return message;
    }

}
