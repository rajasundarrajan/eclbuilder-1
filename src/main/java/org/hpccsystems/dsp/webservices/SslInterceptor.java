package org.hpccsystems.dsp.webservices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @See {@link SslInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)}
 * @author Ashoka_K
 *
 */
public class SslInterceptor extends HandlerInterceptorAdapter {
    
    private static final String UPGRADE = "Upgrade";

    public static final int HTTP_STATUS_UPGRADE_REQUIRED = 426;
    
    @Value("#{wsProperties['ALLOW_NON_SSL'] ?: false}")
    private boolean nonSslAllowed;
    
    /**
     * Verifies that the request is secure. If so, lets the request through (by returning true) to the next
     * target in the chain. Else, breaks the chain (by returning false) to force the response back to the user
     * with an appropriate message. Also sets the relevant HTTP status code into the response.
     * 
     * The check (for SSL) can be turned off by adding the property 'ALLOW_NON_SSL' (with the value set to true)
     * within the webservices.properties file. This is meant only for local development environments where SSL
     * certificates might not be available on the local servers.
     * 
     * @param request The HttpServletRequest object.
     * @param response The HttpServletResponse object.
     * @param paramObject The params object. Not used here.
     * 
     * @return A boolean value indicating if the chain should continue or if the response should be returned
     * to the user immediately
     * 
     * @throws Exception In case an IOException occurs when writing to the response.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object paramObject)
            throws Exception {
        if(request.isSecure() || nonSslAllowed) {
            return true;
        } else {
            response.getWriter().write("Non SSL requests not supported");
            response.setStatus(HTTP_STATUS_UPGRADE_REQUIRED);
            response.addHeader(UPGRADE, "TLS/1.0, HTTP/1.1");
            response.addHeader("Connection", UPGRADE);
            
            return false;
        }
    }
    
}
