package org.hpccsystems.dsp.exceptions;

public class HPCCException extends Exception {
    private static final long serialVersionUID = 1L;
    public HPCCException(String message, Exception e) {
        super(message, e);
    }

    public HPCCException(String message) {
        super(message);
    }

    public HPCCException(Exception e) {
        super(e);
    }

}
