package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the functionality available in the SslInterceptor
 * 
 * @author Ashoka_K
 *
 */
public class SslInterceptorTest {
    
    private SslInterceptor interceptor = new SslInterceptor();
    
    private MockHttpServletRequest request = new MockHttpServletRequest();
    
    private MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    public void verifyNotAllowedScenario() throws Exception {
        request.setSecure(false);
        
        assertFalse(interceptor.preHandle(request, response, null));
        setInternalState(interceptor, "nonSslAllowed", false);
        
        assertEquals("Non SSL requests not supported", response.getContentAsString());
        assertEquals(SslInterceptor.HTTP_STATUS_UPGRADE_REQUIRED, response.getStatus());
        
        assertEquals(2, response.getHeaderNames().size());
        assertEquals("TLS/1.0, HTTP/1.1", response.getHeader("Upgrade"));
        assertEquals("Upgrade", response.getHeader("Connection"));
    }
    
    @Test
    public void verifyAllowedScenario() throws Exception {
        request.setSecure(true);
        setInternalState(interceptor, "nonSslAllowed", false);
        
        verifyRequestAllowed();
        
        setInternalState(interceptor, "nonSslAllowed", true);
        verifyRequestAllowed();
        
        request.setSecure(false);
        setInternalState(interceptor, "nonSslAllowed", true);
        verifyRequestAllowed();
        
        
    }

    private void verifyRequestAllowed() throws Exception,
            UnsupportedEncodingException {
        assertTrue(interceptor.preHandle(request, response, null));
        assertEquals("", response.getContentAsString());
        assertEquals(0, response.getHeaderNames().size());
    }
    
}
