package org.hpccsystems.dsp.webservices;

/**
 * A generic exception thrown in case an exception occurs processing the web
 * service request.
 * 
 * @author Ashoka_K
 *
 */
public class DspWebserviceException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Arged constructor
     * 
     * @param message
     *            The message for the exception
     */
    public DspWebserviceException(String message) {
        super(message);
    }
}
