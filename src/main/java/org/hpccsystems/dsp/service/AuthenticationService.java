package org.hpccsystems.dsp.service;

import javax.servlet.ServletContext;

import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.ramps.entity.User;

public interface AuthenticationService {

    static final String URL = "redirectURL";
    static final String PARAMS = "redirectParams";
    
    User getCurrentUser();

    void logout(Object object);

    User fetchUser(String account, String password) throws AuthenticationException;
    
    User fetchUser(String account, String password, ServletContext servletContext) throws AuthenticationException;
    
    void setRedirectURL(String url, String params);
    
    void clearRedirectURL();
    
    boolean hasRedirectURL();
    
    String getRedictURL();
    
    String getRedirectParams();
}
