package org.hpccsystems.dsp.exceptions;

public class HipieException extends Exception {
    private static final long serialVersionUID = 1L;
    public HipieException(Exception e) {
        super(e);
    }
    
    public HipieException(String message, Exception e) {
        super(message, e);
     }
    public HipieException(String message) {
        super(message);
    }
    
}
