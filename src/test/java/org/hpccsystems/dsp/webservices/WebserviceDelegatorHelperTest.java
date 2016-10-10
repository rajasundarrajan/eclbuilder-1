package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.exceptions.AuthenticationException;
import org.hpccsystems.dsp.ramps.entity.User;
import org.hpccsystems.dsp.ramps.utils.RampsTesterBase;
import org.hpccsystems.dsp.service.AuthenticationService;
import org.hpccsystems.dsp.service.DBLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

/**
 * Tests the functionality available in the WebserviceDelegatorHelper
 * 
 * @author Ashoka_K
 *
 */
public class WebserviceDelegatorHelperTest extends RampsTesterBase {
	
	private static final Logger LOGGER = LogManager.getLogger(WebserviceAuthenticationHelper.class);
    
    @Mock private CredentialsCacher credsCacher;
    
    @Mock private AuthenticationService authService;
    
    @Mock private DBLogger dbLogger;

    @InjectMocks
    private WebserviceAuthenticationHelper helper = new WebserviceAuthenticationHelper();
    
    private HttpServletRequest request;
    
    @Mock private MockServletContext servletContext;
    
    @Mock private User user;
    
    @Before
    public void setup() throws AuthenticationException {
        MockitoAnnotations.initMocks(this);
        
        initAppender(LOGGER);
		messages.clear();
        
        setupRequest();
        ((MockHttpServletRequest) request).addHeader("Authorization", "Basic " + encodeToBase64("myUser:myPassword"));
        setInternalState(helper, "realm", "myRealm");
        
        when(credsCacher.verifyAndUpdateCredentials(eq("myUser"), eq("myPassword"))).thenReturn(false);
        when(authService.fetchUser(eq("myUser"), eq("myPassword"), same(servletContext))).thenReturn(user);
    }

    private void setupRequest() {
        request = spy(MockHttpServletRequest.class);
        when(request.getServletContext()).thenReturn(servletContext);        
    }
    
    @Test
    public void verifyMissingCredentialsScenarios() throws UnsupportedEncodingException, InterruptedException {
        request = new MockHttpServletRequest();
        verifyException("Missing Credentials", null, null);
        
        verifyMissingCredentialsScenario("", null);
        verifyMissingCredentialsScenario(" ", "");
        verifyMissingCredentialsScenario("Ba ", "");
        verifyMissingCredentialsScenario("Basic ", " ");
        verifyMissingCredentialsScenario("BasiC ", "myUser:myPassword");
        verifyMissingCredentialsScenario("", "myUser:myPassword");
        verifyMissingCredentialsScenario("Basic ", ":myPassword");
        verifyMissingCredentialsScenario("Basic ", "myUser:");
        verifyMissingCredentialsScenario("Basic ", "myUser: ");
    }
    
    private void verifyMissingCredentialsScenario(String basicString, String idPassword)
            throws UnsupportedEncodingException {
        verifyException("Missing Credentials", basicString, idPassword);
    }
    
    private String encodeToBase64(String input) {
        if(input == null) {
            return "";
        }
        
        return new String(new Base64().encode(input.getBytes()));
    }
    
    private void verifyException(String message, String basicString, String idPassword)
            throws UnsupportedEncodingException {
        setupRequest();
        
        if(basicString != null) {
            ((MockHttpServletRequest) request).addHeader("Authorization", basicString + encodeToBase64(idPassword));
        }
        
        BasicAuthenticationException exception = null;
        
        try {
            helper.authenticateUser(request, servletContext);
        } catch(BasicAuthenticationException e) {
            exception = e;
        }
        
        assertEquals(HttpStatus.SC_UNAUTHORIZED, exception.getStatusCode());
        assertEquals(message, exception.getMessage());
        assertEquals(1, exception.getResponseHeaders().size());
        assertEquals("Basic realm=\"myRealm\"", exception.getResponseHeaders().get("WWW-Authenticate"));
    }
    
    @Test
    public void verifyValidCredentialsScenarios() throws BasicAuthenticationException,
    		UnsupportedEncodingException, InterruptedException {
        when(credsCacher.verifyAndUpdateCredentials(eq("myUser"), eq("myPassword"))).thenReturn(true);
        when(credsCacher.verifyAndUpdateCredentials(eq("myUser"), eq("my:Password"))).thenReturn(true);
        
        verifyValidCredsScenario("Basic ", "myUser:myPassword", "myUser", "myPassword");        
        verifyValidCredsScenario(" Basic  ", "myUser:myPassword ", "myUser", "myPassword");
        verifyValidCredsScenario("Basic ", "myUser : myPassword ", "myUser", "myPassword");    
        verifyValidCredsScenario("Basic ", "myUser:my:Password", "myUser", "my:Password");
        
        verifyLogging();
    }    
    
    private void verifyValidCredsScenario(String basicText, String idPassword, String userId, String password)
            throws BasicAuthenticationException, UnsupportedEncodingException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", basicText + encodeToBase64(idPassword));
        BasicAuthCredentials creds = helper.authenticateUser(mockRequest, servletContext);
        assertEquals(userId, creds.getUserName());
        assertEquals(password, creds.getPassword());
    }
    
    @Test
    public void verifyAuthenticationExceptionScenario() throws BasicAuthenticationException,
            AuthenticationException, UnsupportedEncodingException, InterruptedException {
        AuthenticationException authException = new AuthenticationException("message", new Exception("message"));
        when(credsCacher.verifyAndUpdateCredentials(eq("myUser"), eq("myPassword"))).thenReturn(false);        
        when(authService.fetchUser(eq("myUser"), eq("myPassword"), same(servletContext)))
            .thenThrow(authException);
        
        BasicAuthenticationException exception = null;
        
        try {
            helper.authenticateUser(request, servletContext); 
        } catch(BasicAuthenticationException e) {
            exception = e;
        }
        
        assertEquals(HttpStatus.SC_UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid Credentials", exception.getMessage());
        Assert.assertSame(authException, exception.getCause());
        
        verifyLogging();
    }
    
    @Test
    public void verifyUserNullScenario() throws BasicAuthenticationException,
            AuthenticationException, UnsupportedEncodingException, InterruptedException {
        when(authService.fetchUser(eq("myUser"), eq("myPassword"), same(servletContext))).thenReturn(null);
        verifyException("User myUser: Unauthorized", "Basic ", "myUser:myPassword");
        
        verifyLogging();
    }
    
    @Test
    public void verifyNoWebserviceAccessScenario() throws BasicAuthenticationException,
            AuthenticationException, UnsupportedEncodingException, InterruptedException {
        when(user.canGetWebService()).thenReturn(false);
        
        verifyException("User myUser does not have web service access permissions in MBS: Unauthorized",
                "Basic ", "myUser:myPassword");
        
        verifyLogging();
    }
    
    @Test
    public void verifyUserAuthenticationViaAuthService() throws BasicAuthenticationException,
            AuthenticationException, UnsupportedEncodingException, InterruptedException {
        when(user.canGetWebService()).thenReturn(true);        
        
        BasicAuthCredentials authCreds = helper.authenticateUser(request, servletContext);        
        assertEquals("myUser", authCreds.getUserName());
        assertEquals("myPassword", authCreds.getPassword());
        
        verifyLogging();
    }
    
    @Test
    public void verifyUserAuthenticationViaCredsCacher() throws BasicAuthenticationException,
            AuthenticationException, UnsupportedEncodingException, InterruptedException {
        when(credsCacher.verifyAndUpdateCredentials(eq("myUser"), eq("myPassword"))).thenReturn(true);
        
        BasicAuthCredentials authCreds = helper.authenticateUser(request, servletContext);        
        assertEquals("myUser", authCreds.getUserName());
        assertEquals("myPassword", authCreds.getPassword());
        
        verifyLogging();
    }
    
}
