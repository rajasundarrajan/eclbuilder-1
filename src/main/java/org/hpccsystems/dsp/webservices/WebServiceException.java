package org.hpccsystems.dsp.webservices;

import org.hpcc.HIPIE.error.HipieErrorType;
import org.hpccsystems.error.ErrorBlock;
import org.hpccsystems.error.HError;
import org.hpccsystems.error.HError.ErrorLevel;
import org.hpccsystems.error.IErrorCode;
import org.hpccsystems.error.IErrorType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A superclass of exceptions thrown for DSP web services (i.e. ones that are
 * processed by DSP as opposed to being delegated to other systems like HIPIE)
 * 
 * @author Ashoka_K
 *
 */
public abstract class WebServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    protected final ErrorLevel errorLevel;

    protected IErrorType errorType = HipieErrorType.VALIDATE;

    protected IErrorCode errorCode;

    /**
     * Arged constructor
     * 
     * @param message
     *            The error message
     * @param statusCode
     *            The HTTP status code corresponding to the error
     */
    public WebServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        errorLevel = HError.ErrorLevel.ERROR;
    }

    /**
     * Arged constructor
     * 
     * @param message
     *            The error message
     * @param statusCode
     *            The HTTP status code corresponding to the error
     * @param cause
     *            the Throwable which caused this exception
     */
    public WebServiceException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        errorLevel = HError.ErrorLevel.ERROR;
    }

    /**
     * Gets the HTTP status code corresponding to this exception
     * 
     * @return The HTTP status code corresponding to this exception
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets a serialized representation of the ErrorBlock corresponding to this
     * exception
     * 
     * @return A serialized representation of the ErrorBlock corresponding to
     *         this exception
     * 
     * @throws JsonProcessingException
     *             When serializing the ErrorBlock
     */
    public String getErrorString() throws JsonProcessingException {
        ErrorBlock errorBlock = new ErrorBlock();
        errorBlock.add(new HError(errorLevel, errorType, errorCode, getMessage()));

        return new ObjectMapper().writeValueAsString(errorBlock);
    }

}
