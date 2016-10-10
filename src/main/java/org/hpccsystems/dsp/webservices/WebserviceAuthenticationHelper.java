package org.hpccsystems.dsp.webservices;

import static java.lang.String.format;
import static org.apache.log4j.LogManager.getLogger;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.debug;

import java.io.UnsupportedEncodingException;
import java.time.Instant;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.log.WebserviceInvocation;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.DBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @see WebserviceAuthenticationHelper#authenticateUser(HttpServletRequest,
 *      ServletContext)
 * 
 * @author Ashoka_K
 *
 */
@Service("WebserviceAuthenticationHelper")
public class WebserviceAuthenticationHelper {

    private static final String CHAR_ENCODING_UTF_8 = "UTF-8";

    private static final String MISSING_CREDENTIALS = "Missing Credentials";

    private static final Logger LOGGER = getLogger(WebserviceAuthenticationHelper.class);

    private CredentialsCacher credsCacher;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private DBLogger dbLogger;

    @Value("#{wsProperties['DSP_REALM'] ?: 'dataSciencePortal'}")
    private String realm;

    @Value("#{wsProperties['WS_CREDS_VALIDITY_DURATION'] ?: 1800}")
    private int credentialsValidityDuration;

    @Value("#{wsProperties['WS_CACHE_CLEANUP_INTERVAL'] ?: 1800}")
    private int cleanupInterval;

    @PostConstruct
    public void initialize() throws DspWebserviceException {
        credsCacher = new CredentialsCacher(credentialsValidityDuration, cleanupInterval, false);
    }

    /**
     * Authenticates using the Basic Authentication Scheme.
     * 
     * @see http://en.wikipedia.org/wiki/Basic_access_authentication
     * @see http://tools.ietf.org/html/rfc2617
     * 
     *      Authenticates the user and returns the
     *      BasicAuthenticationCredentials for the user in case of successful
     *      authentication. Else, throws a BasicAuthenticationException.
     * 
     *      Utilizes an in-memory cache in order to improve performance whereby
     *      credentials which were successfully authenticated are stored in an
     *      in-memory cache (@See CredentialsCacher) for a certain duration.
     *      Subsequent requests for authentication are first validated against
     *      the cache and the relevant Authentication Service is called only in
     *      case the credentials are not found, are found to be invalid or are
     *      found to have expired.
     * 
     *      The id/password combination will be available in the cache until the
     *      credentials validity duration. The cache will be cleared of all
     *      expired credentials at a certain interval as specified by the
     *      cleanup interval.
     * 
     *      The default values for the credentials validity duration and for the
     *      cleanup interval are both set to 1800 seconds. These can be
     *      overridden by adding the following properties to the
     *      webservices.properties file. - WS_CREDS_VALIDITY_DURATION -
     *      WS_CACHE_CLEANUP_INTERVAL
     * 
     *      In case the authentication fails, a BasicAuthenticationException is
     *      thrown with the appropriate HTTP Status codes and response headers
     *      set into it. In cases where the necessary credentials are not found
     *      in the request, the necessary response headers are set into the
     *      BasicAuthenticationException so as to force the user/caller to
     *      provide his/her credentials.
     * 
     * @param request
     *            The HttpServletReqest object
     * @param servletContext
     *            The ServletContext object
     * 
     * @return The BasicAuthCredentials corresponding to the credentials
     *         provided in the request
     * 
     * @throws BasicAuthenticationException
     *             In case an exception occurs when authenticating the user, or
     *             in case the credentials are found to be invalid
     * 
     * @throws UnsupportedEncodingException
     *             Will not be thrown. Propagated from a 3rd party library call.
     */
    public BasicAuthCredentials authenticateUser(HttpServletRequest request, ServletContext servletContext) throws BasicAuthenticationException,
            UnsupportedEncodingException {
        String remoteAddr = request.getRemoteAddr();
        BasicAuthCredentials authCreds = getUserCredentials(request);
        String userName = authCreds.getUserName(), password = authCreds.getPassword();

        debug(LOGGER, "Successfully retrieved credentials from the request for IP %s and user name %s", remoteAddr, userName);

        if (!credsCacher.verifyAndUpdateCredentials(userName, password)) {
            debug(LOGGER, "Credentials not found (or are expired) within the CredentialsCacher for IP %s, user %s", remoteAddr, userName);

            try {
                debug(LOGGER, "Calling AuthenticationService for user %s", userName);

                long startTime = Instant.now().toEpochMilli();
                User user = authService.fetchUser(userName, password, servletContext);
                long callDuration = System.currentTimeMillis() - startTime;

                String message = format("Call to AuthenticationService.fetchUser() for user %s completed in %s ms", userName, callDuration);
                debug(LOGGER, message);
                if (LOGGER.isDebugEnabled()) {
                    dbLogger.log(new WebserviceInvocation(userName, WebserviceInvocation.ACTION_AUTH_VIA_SERVICE, message, startTime));
                }

                if (user == null) {
                    throw createUnauthorizedException(format("User %s: Unauthorized", userName), null);
                } else if (!user.canGetWebService()) {
                    throw createUnauthorizedException(format("User %s does not have web service access "
                    		+ "permissions in MBS: Unauthorized", userName), null);
                }

                debug(LOGGER, "Successfully authenticated request for IP %s and user name %s", remoteAddr, userName);
            } catch (AuthenticationException e) {
            	throw createUnauthorizedException("Invalid Credentials", e);
            }
        }

        return authCreds;
    }

    private BasicAuthCredentials getUserCredentials(HttpServletRequest request) throws BasicAuthenticationException, UnsupportedEncodingException {
        String authHeader = request.getHeader("Authorization");
        authHeader = (authHeader != null) ? authHeader.trim() : null;

        if (StringUtils.isNotBlank(authHeader) && authHeader.length() > 6 && "Basic ".equals(authHeader.substring(0, 6))) {
            String base64String = authHeader.substring(6).trim();
            String decodedString = new String(new Base64().decode(base64String.getBytes(CHAR_ENCODING_UTF_8)), CHAR_ENCODING_UTF_8).trim();

            int positionOfFirstColon = decodedString.indexOf(':');

            if (positionOfFirstColon < 1 || ((decodedString.length() - 1) == positionOfFirstColon)) {
                throw createUnauthorizedException(MISSING_CREDENTIALS, null);
            }

            return new BasicAuthCredentials(decodedString.substring(0, positionOfFirstColon).trim(), decodedString
                    .substring(positionOfFirstColon + 1).trim());
        } else {
            throw createUnauthorizedException(MISSING_CREDENTIALS, null);
        }
    }

    private BasicAuthenticationException createUnauthorizedException(String message, Throwable cause) {
        BasicAuthenticationException e = new BasicAuthenticationException(
        		HttpStatus.SC_UNAUTHORIZED, message, cause);
        e.addResponseHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");

        return e;
    }

    /**
     * Performs necessary cleanup operations pertaining to this class at shutdown.
     */
    @PreDestroy
    public void cleanup() throws Exception {
        credsCacher.close();
    }

}
