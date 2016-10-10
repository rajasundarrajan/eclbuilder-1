package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.debug;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.error;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.ws.IWsResponse;
import org.hpcc.HIPIE.ws.WsHipie;
import org.hpccsystems.dsp.Constants;
import org.hpccsystems.dsp.exceptions.HipieException;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.service.DBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * See
 * {@link HipieWebserviceDelegator#handleRequest(HttpServletRequest, HttpServletResponse)}
 * 
 * @author Ashoka_K
 *
 */
@Controller
public class HipieWebserviceDelegator extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LogManager.getLogger(HipieWebserviceDelegator.class);

    @Autowired
    private DBLogger dbLogger;

    /**
     * Delegates incoming requests to the WsHipie and writes the response
     * obtained into the HttpServletResponse object. Does not validate the
     * request before passing the details over to WsHipie.
     * 
     * Errors returned by WsHipie are handled and appropriate message(s) and
     * HTTP status code is set into the response.
     * 
     * @param request
     *            The HttpSevletRequest object
     * @param response
     *            The HttpServletResposne object
     * 
     * @throws IOException
     *             In case an exception occurs when writing to the Http
     *             response.
     */
    @RequestMapping(value = "/**")
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long startTime = Instant.now().toEpochMilli();
        WsHipie wsHipie = HipieSingleton.getHipieWebService();

        if (wsHipie == null) {
            String message = "Could not instantiate WsHipie. Null object received.";
            error(LOGGER, message, null);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Unexpected Internal Error. Could not create" + " wsHipie instance");
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_RESPONSE, message, startTime));
            }

            return;
        }

        try {
            updateResponseWithWebserviceResponse(wsHipie, request, response);
        } catch (Exception e) {
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Internal Server Error");
            String message = "Exception occured calling the Hipie Webservice. ";
            error(LOGGER, message, e);
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_RESPONSE, message + e.getMessage(), startTime));
            }
        }
    }

    private void updateResponseWithWebserviceResponse(WsHipie wsHipie, HttpServletRequest request, HttpServletResponse response) throws IOException,
            HipieException {
        IWsResponse iwsResponse = null;
        long startTime;
        try {
            debug(LOGGER, "Calling WsHipie.get() with requestUri %s", request.getRequestURI());

            startTime = Instant.now().toEpochMilli();
            iwsResponse = wsHipie.get(request.getRequestURI(), request.getParameterMap());
            long callDuration = System.currentTimeMillis() - startTime;

            String message = format("Call to WsHipie.get() with requestUri %s completed in %s ms", request.getRequestURI(), callDuration);

            debug(LOGGER, message);
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_RESPONSE, message, startTime));
            }
        } catch (Exception e) {
            error(LOGGER, "Exception occured when calling WsHipie.get()", e);
            String message = e.getMessage();

            if (StringUtils.isNotBlank(message) && message.toUpperCase().contains("NO FORMAT DEFINED")) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.getWriter().write(message);

                return;
            } else {
                throw new HipieException(e);
            }
        }

        if (iwsResponse == null) {
            startTime = Instant.now().toEpochMilli();
            error(LOGGER, "Null IwsResponse received", null);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            String message = "Internal Server Error. Null IWsResponse received.";
            response.getWriter().write(message);
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_RESPONSE, message, startTime));
            }
        } else {
            startTime = Instant.now().toEpochMilli();
            try {
                debug(LOGGER, "Request successful. Response obtained: %s", (Object) iwsResponse.getResult());
                response.setContentType(iwsResponse.getMimeType());
                response.getWriter().write(iwsResponse.getResult());
            } catch (Exception e) {
                LOGGER.error(Constants.EXCEPTION, e);
                throw new HipieException("Unable to get write on response", e);
            }
            response.setStatus(HttpStatus.SC_OK);
            if (LOGGER.isDebugEnabled()) {
                dbLogger.log(new WebserviceInvocation(WebserviceInvocation.ACTION_RESPONSE, "Request successful.", startTime));
            }
        }
    }

}
