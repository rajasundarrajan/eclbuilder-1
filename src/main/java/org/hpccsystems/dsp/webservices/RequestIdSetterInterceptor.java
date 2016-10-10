package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;

import java.time.Instant;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.ramps.utils.RampsLogger;
import org.hpccsystems.dsp.service.DBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * @see RequestIdSetterInterceptor#preHandle(HttpServletRequest,
 *      HttpServletResponse, Object)
 * 
 * @author Ashoka_K
 *
 */
public class RequestIdSetterInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private DBLogger dbLogger;

    private static final Logger LOGGER = LogManager.getLogger(RequestIdSetterInterceptor.class);

    /**
     * Creates a unique ID for the incoming request and sets it into the
     * LoggingUtils.threadId field so that other code paths in the same thread
     * have access to this request/thread id for logging purposes.
     * 
     * @param request
     *            The HttpServletRequest object
     * @param response
     *            The HttpServletResponse object
     * @param paramObject
     *            The params object. Not used here.
     * 
     * @return A boolean value indicating if the processing chain should
     *         continue. Always returns true.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object paramObject) {
        long startTime = Instant.now().toEpochMilli();
        RampsLogger.THREAD_ID.set(UUID.randomUUID().toString());
        String message = format("Access from IP: %s, Request URI: %s", request.getRemoteAddr(), request.getRequestURI());
        if (LOGGER.isDebugEnabled()) {
            dbLogger.log(new WebserviceInvocation("NotApplicable", WebserviceInvocation.ACTION_ACCESS, message, startTime));
        }
        return true;
    }

}
