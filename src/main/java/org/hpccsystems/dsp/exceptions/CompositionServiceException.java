package org.hpccsystems.dsp.exceptions;

public class CompositionServiceException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public CompositionServiceException(String message, Exception e) {
        super(message, e);
     }
}
