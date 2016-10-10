package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the functionality available in the MethodVerifierInterceptor
 * 
 * @author Ashoka_K
 *
 */
public class MethodVerifierInterceptorTest {
	
	private MethodVerifierInterceptor interceptor = new MethodVerifierInterceptor();
	
	private MockHttpServletRequest request = new MockHttpServletRequest();
	
	private MockHttpServletResponse response = new MockHttpServletResponse();
	
	@Test
	public void verifyUnsupportedMethodHandling() throws Exception {
		request.setMethod("PUT");
		verifyInvalidMethodProcessing();
		
		response = new MockHttpServletResponse();
		request.setMethod("InvaidMethod");		
		verifyInvalidMethodProcessing();
	}

	private void verifyInvalidMethodProcessing() throws Exception,
			UnsupportedEncodingException {
		assertFalse(interceptor.preHandle(request, response, null));
		
		assertEquals("Only the GET and POST methods are supported. Please call the"
                + " webservice appropriately", response.getContentAsString());
		assertEquals(0, response.getHeaderNames().size());
	}
	
	@Test
	public void verifyGetHandling() throws Exception {
	    request.setMethod("GET");
	    verifySupportedMethodHandling();
	}
	
	@Test
    public void verifyPostHandling() throws Exception {
        request.setMethod("POST");
        verifySupportedMethodHandling();
    }
	
	private void verifySupportedMethodHandling() throws Exception {		
		assertTrue(interceptor.preHandle(request, response, null));
		
		assertEquals("", response.getContentAsString());
		assertEquals(0, response.getHeaderNames().size());
	}
	
}
