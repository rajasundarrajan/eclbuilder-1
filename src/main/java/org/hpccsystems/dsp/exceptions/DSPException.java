package org.hpccsystems.dsp.exceptions;

public class DSPException extends Exception {
    private static final long serialVersionUID = 1L;

    public DSPException(Exception e) {
        super(e);
    }

    public DSPException(String message, Exception e) {
        super(message, e);
    }

    public DSPException(String message) {
        super(message);
    }
}
