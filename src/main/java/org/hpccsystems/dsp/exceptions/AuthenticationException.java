package org.hpccsystems.dsp.exceptions;

public class AuthenticationException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public AuthenticationException(String message, Exception e) {
        super(message, e);
     }
    public AuthenticationException(String message) {
        super(message);
     }
}
