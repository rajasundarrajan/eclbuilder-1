package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.ramps.utils.RampsTesterBase;
import org.hpccsystems.dsp.service.DBLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/**
 * Tests the functionality available in the AuthenticationInterceptor
 * 
 * @author Ashoka_K
 *
 */
public class AuthenticationInterceptorTest extends RampsTesterBase {
	
	private static final Logger LOGGER = LogManager.getLogger(AuthenticationInterceptor.class);
	
	private MockHttpServletRequest mockRequest = new MockHttpServletRequest();;
    
    private MockHttpServletResponse mockResponse = new MockHttpServletResponse();
    
    private BasicAuthCredentials authCredentials = new BasicAuthCredentials("myId", "myPassword");
    
    @InjectMocks
    private AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
    
    private Map<String, String[]> paramsMap = new HashMap<String, String[]>();
    
    @Mock private WebserviceAuthenticationHelper helper;
    
    @Mock private MockServletContext servletContext;
    
    @Mock private DBLogger dbLogger;
    
    @Before
    public void setup() throws BasicAuthenticationException, UnsupportedEncodingException {
    	initMocks(this);
    	
    	initAppender(LOGGER);
		messages.clear();
        
        mockRequest.setSecure(true);
        when(helper.authenticateUser((same(mockRequest)), same(servletContext))).thenReturn(authCredentials);
        
        mockRequest.setRequestURI("myRequestUri");
        mockRequest.setParameters(paramsMap);
    }
    
    @Test    
    public void verifySuccessfulAuthentication() throws Exception {
    	verifySuccessfulAuthentication(Level.DEBUG);
    	verifySuccessfulAuthentication(Level.INFO);
    	verifySuccessfulAuthentication(Level.WARN);
    	verifyLogging();
    }
    
    private void verifySuccessfulAuthentication(Level logLevel) throws Exception {
    	LOGGER.setLevel(logLevel);
        assertTrue(interceptor.preHandle(mockRequest, mockResponse, null));
        assertEquals("", mockResponse.getContentAsString());
        assertEquals(0, mockResponse.getHeaderNames().size());
    }

    @Test
    public void verifyExceptionWhenAuthenticatingUser() throws Exception {
        BasicAuthenticationException exception = createBasicAuthenticationException();
        doThrow(exception).when(helper).authenticateUser(same(mockRequest), same(servletContext));
        interceptor.preHandle(mockRequest, mockResponse, null);
        verifyBasicAuthenticationException();
        verifyLogging();
    }
    
    private void verifyBasicAuthenticationException()
            throws UnsupportedEncodingException {
        assertEquals(1234, mockResponse.getStatus());
        assertEquals("myMessage", mockResponse.getContentAsString());
        assertEquals("value1", mockResponse.getHeader("header1"));
        assertEquals("value2", mockResponse.getHeader("header2"));
        assertEquals(2, mockResponse.getHeaderNames().size());
    }

    private BasicAuthenticationException createBasicAuthenticationException() {
        BasicAuthenticationException exception = new BasicAuthenticationException(1234, "myMessage");
        exception.addResponseHeader("header1", "value1");
        exception.addResponseHeader("header2", "value2");
        return exception;
    }
    
}
