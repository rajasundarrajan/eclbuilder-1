package org.hpccsystems.dsp.webservices;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @See {@link MethodVerifierInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object)}
 * 
 * @author Ashoka_K
 *
 */
public class MethodVerifierInterceptor extends HandlerInterceptorAdapter {
    
    /**
     * Verifies that the method of the request is either GET or POST. If so, lets the request
     * through (by returning true) to the next target in the chain. Else, breaks the chain (by returning false)
     * to force the response back to the user with an appropriate message. Also sets the relevant HTTP status
     * code into the response.
     * 
     * @param request The HttpServletRequest object
     * @param response The HttpServletResponse object
     * @param paramObject The params object. Not used
     * 
     * @return A boolean value indicating if the chain should continue or if the response should be returned
     * to the user immediately
     * 
     * @throws Exception In case an IOException occurs when writing to the response.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object paramObject)
            throws Exception {
        if(!"GET".equals(request.getMethod()) && !"POST".equals(request.getMethod())) {
            response.getWriter().write("Only the GET and POST methods are supported. Please call the"
                    + " webservice appropriately");
            response.setStatus(HttpStatus.SC_METHOD_NOT_ALLOWED);
            
            return false;
        }
        
        return true;
    }

}
