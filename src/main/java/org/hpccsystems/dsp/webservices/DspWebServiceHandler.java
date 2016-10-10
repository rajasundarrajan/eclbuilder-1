package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;
import static org.hpccsystems.common.Utils.convertExceptionToString;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.info;
import static org.hpccsystems.logging.Trace.Error;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.Composition;
import org.hpcc.HIPIE.CompositionInstance;
import org.hpccsystems.common.Utils;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.dsp.webservices.WebServiceExceptionsContainer.UnknownWebserviceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Handles web service requests that are to be processed by DSP (as opposed to
 * those which are to be handled by HIPIE and other systems
 * 
 * @author Ashoka_K
 *
 */
@Controller
public class DspWebServiceHandler {

    private static final Logger LOGGER = LogManager.getLogger(HipieWebserviceDelegator.class);

    @Autowired
    private DBLogger dbLogger;

    @Autowired
    private WebServiceHelper helper;

    /**
     * Fetches the layout for the given dashboard composition/user/workunit
     * combination. The username and uuid (composition id) are mandatory
     * parameters whereas the workunit id is optional.
     * 
     * username: The username provided here must have rights to view the
     * specified compositions uuid: Either the composition id or the composition
     * name can be provided here wuId: The id of the workunit for which the
     * layout needs to be retrieved. In case the wuId is not specified, the most
     * recent workunit for the composition will be used.
     * 
     * @param request
     *            The request object
     * @param response
     *            The response object
     * 
     * @param input
     *            contains the parameters specified as part of the request
     * 
     * @throws IOException
     *             In case an exception occurs when writing to the PrintWriter
     *             of the response
     */
    @RequestMapping(value = "/selectLayout", method = RequestMethod.GET)
    public void selectLayout(HttpServletRequest request, HttpServletResponse response, @ModelAttribute SelectLayoutInputs input) throws IOException {
        try {
            logEvent(input, "Initiating", WebserviceInvocation.ACTION_SELECT_LAYOUT_INIT);

            input.postProcess();

            Composition composition = helper.fetchComposition(input.getUsername(), input.getUuid());
            composition.refreshCompositionInstances();

            CompositionInstance workunit = helper.getCompositionInstance(composition);
            String ddl = helper.getDdls(workunit);

            String layout = helper.fetchLayout(input.getUsername(), ddl, input.getUuid(),composition.getVersion(), input.getGcid());
            response.getWriter().write(layout);

            logEvent(input, "Completed", WebserviceInvocation.ACTION_SELECT_LAYOUT_RESPONSE);
        } catch (WebServiceException e) {
            populateResponseWithError(response, e);
            Error(Utils.convertExceptionToString(e));
        } catch (Exception e) {
            handleUnknownException(response, input, e);
        }
    }

    private void logEvent(SelectLayoutInputs input, String status, String action) {
        if (LOGGER.isInfoEnabled()) {
            String message = format("%s webservice selectLayout for inputs.. username: %s, " + "uuid: %s", status, input.getUsername(),
                    input.getUuid());
            dbLogger.log(new WebserviceInvocation(action, message, Instant.now().toEpochMilli()));
            info(LOGGER, message);
        }
    }

    private void handleUnknownException(HttpServletResponse response, SelectLayoutInputs input, Exception e)
            throws IOException, JsonProcessingException {
        UnknownWebserviceException exception = new UnknownWebserviceException(
                format("Error when fetching layout for user %s, uuid %s. Exception: %s, %s", input.getUsername(), input.getUuid(),
                         e.getClass().getName(), e.getMessage()));
        Error(convertExceptionToString(exception));
        populateResponseWithError(response, exception);
    }

    private void populateResponseWithError(HttpServletResponse response, WebServiceException e) throws IOException, JsonProcessingException {
        response.setStatus(e.getStatusCode());
        response.getWriter().write(e.getErrorString());
    }

}
