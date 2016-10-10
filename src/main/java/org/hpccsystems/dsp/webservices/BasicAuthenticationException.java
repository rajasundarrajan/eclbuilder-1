package org.hpccsystems.dsp.webservices;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown in case an any exceptioinal scenarios are encountereds when either
 * extracting the credentials of the user who is attempting the basic
 * authentication or in case the authentication fails.
 * 
 * @author Ashoka_K
 *
 */
public class BasicAuthenticationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    private final Map<String, String> responseHeaders = new LinkedHashMap<String, String>();

    /**
     * Arged constructor
     * 
     * @param statusCode
     *            The HTTP status code to be set into the response for the
     *            exceptional scenario encountered
     * @param message
     *            The message describing the exceptional scenario encountered
     */
    public BasicAuthenticationException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Arged constructor
     * 
     * @param statusCode
     *            The HTTP status code to be set into the response for the
     *            exceptional scenario encountered
     * @param message
     *            The message describing the exceptional scenario encountered
     * @param cause
     *            The Throwable cause of the exceptional scenario
     */
    public BasicAuthenticationException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * The HTTP status code corresponding to the exceptional scenario
     * 
     * @return
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response headers to be added to the response for this
     * exceptional scenario
     * 
     * @return The response headers to be added to the response for this
     *         exceptional scenario
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Stores the provided HTTP header name and value in this exception.
     * 
     * @param headerName
     *            The name of the HTTP header to be stored
     * @param headerValue
     *            The value of the header to be stored
     */
    public void addResponseHeader(String headerName, String headerValue) {
        responseHeaders.put(headerName, headerValue);
    }

}
