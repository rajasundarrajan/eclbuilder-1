package org.hpccsystems.dsp.exceptions;

public class RepoException extends Exception { 
    private static final long serialVersionUID = 1L;
    public RepoException(String message, Exception e) {
        super(message, e);
     }
    public RepoException(String message) {
        super(message);
    }
}
