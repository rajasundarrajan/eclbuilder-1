package org.hpccsystems.dsp.requestwrapper;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
/**
 * 
 * Filtering the HTTP requests from HIPIE form validation and save.
 * Also visualization web service request
 *
 */
public class RequestWrappingFilter implements Filter{
 
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException{
        chain.doFilter(new MyHttpRequestWrapper((HttpServletRequest) req), res);
    }
 
    public void init(FilterConfig config) throws ServletException{
        //nothing to initialize
    }

    public void destroy(){
        //nothing to clean up
    }
}