package org.hpccsystems.dsp.webservices;

import static org.apache.log4j.LogManager.getLogger;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.debug;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.debugAsJson;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.error;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.info;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.ramps.utils.RampsLogger;
import org.hpccsystems.dsp.ramps.utils.RampsUtil;
import org.hpccsystems.dsp.service.DBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @see AuthenticationInterceptor#preHandle(HttpServletRequest,
 *      HttpServletResponse, Object)
 * 
 * @author Ashoka_K
 *
 */
public class AuthenticationInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOGGER = getLogger(AuthenticationInterceptor.class);

    @Autowired
    private DBLogger dbLogger;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private WebserviceAuthenticationHelper helper;

    /**
     * Authenticates incoming requests against the AuthenticationService and
     * allows the request to go through the chain in case the user is
     * authenticated successfully. Else, returns a response with an appropriate
     * status code and response headers for challenge authentication.
     * 
     * Authenticates using the Basic Authentication Scheme.
     * 
     * @see http://en.wikipedia.org/wiki/Basic_access_authentication
     * @see http://tools.ietf.org/html/rfc2617
     * 
     * @see WebserviceAuthenticationHelper#authenticateUser(HttpServletRequest,
     *      ServletContext)
     * 
     * @param request
     *            The HttpServletRequest object.
     * @param response
     *            The HttpServletResponse object.
     * @param paramObject
     *            The param object. Not used here.
     * 
     * @return true in case the user is authenticated successfully, false
     *         otherwise.
     * 
     * @throws Exception
     *             In case an exception occurs when writing to the response.
     * 
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object paramObject) throws Exception {
        logRequestDetails(request);
        String remoteAddr = request.getRemoteAddr();
        long startTime = Instant.now().toEpochMilli();
        try {
            BasicAuthCredentials authCreds = helper.authenticateUser(request, servletContext);

            RampsLogger.USER_ID.set(authCreds.getUserName());
            String message = "Successfully authenticated for IP: " + remoteAddr;
            info(LOGGER, message);
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(authCreds.getUserName(), WebserviceInvocation.ACTION_AUTH_STATUS, message, startTime));
            }
            return true;
        } catch (BasicAuthenticationException e) {
            String message = "Authentication failed for IP: " + remoteAddr;
            error(LOGGER, message, e);

            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation("NotApplicable", WebserviceInvocation.ACTION_AUTH_STATUS, message, startTime));
            }
            updateResponseWithHeaders(response, e);
            return false;
        }
    }

    private void logRequestDetails(HttpServletRequest request) {
        if (LOGGER.isInfoEnabled()) {
            info(LOGGER, "Attempting to authenticate request from IP: %s", request.getRemoteAddr());
        }

        if (LOGGER.isDebugEnabled()) {
            debug(LOGGER, "request.getRequestURL(): %s", request.getRequestURL());
            debug(LOGGER, "request.getQueryString(): %s", request.getQueryString());
            debug(LOGGER, "Request Parameter Map:\n%s", request.getParameterMap());

            // The header named "Authorization" contains the user's password.
            // Hence removing
            debugAsJson(LOGGER, "Headers minus Authorization:\n%s", (Object) RampsUtil.getRequestHeadersMap(request).remove("Authorization"));
        }
    }

    private void updateResponseWithHeaders(HttpServletResponse response, BasicAuthenticationException e) throws IOException {
        response.setStatus(e.getStatusCode());
        response.getWriter().write(e.getMessage());

        for (Map.Entry<String, String> mapEntry : e.getResponseHeaders().entrySet()) {
            response.setHeader(mapEntry.getKey(), mapEntry.getValue());
        }
    }

}
