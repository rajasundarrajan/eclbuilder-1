package org.hpccsystems.dsp.webservices;

import static org.apache.log4j.LogManager.getLogger;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.hpcc.HIPIE.ws.IWsResponse;
import org.hpcc.HIPIE.ws.WsHipie;
import org.hpccsystems.dsp.init.HipieSingleton;
import org.hpccsystems.dsp.ramps.utils.RampsTesterBase;
import org.hpccsystems.dsp.service.DBLogger;
import org.hpccsystems.error.ErrorBlock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the functionality available in the HipieWebserviceDelegator
 * 
 * @author Ashoka_K
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({CredentialsCacher.class, HipieSingleton.class})
public class HipieWebserviceDelegatorTest extends RampsTesterBase {
	
	private static final Logger LOGGER = getLogger(HipieWebserviceDelegator.class);
    
    private MockHttpServletRequest mockRequest = new MockHttpServletRequest();
    
    private MockHttpServletResponse mockResponse = new MockHttpServletResponse();
    
    private Map<String, String[]> paramsMap = new HashMap<String, String[]>();
    
    @Mock private WsHipie wsHipie;
    
    @InjectMocks
    private HipieWebserviceDelegator delegator = new HipieWebserviceDelegator();
    
    @Mock private DBLogger dbLogger;
    
    @Before
    public void setup() throws BasicAuthenticationException, UnsupportedEncodingException {
        initMocks(this);
    	
    	initAppender(LOGGER);
		messages.clear();
        
        mockStatic(HipieSingleton.class);
        when(HipieSingleton.getHipieWebService()).thenReturn(wsHipie);
        
        mockRequest.setRequestURI("myRequestUri");
        mockRequest.setParameters(paramsMap);
    }
    
    @Test
    public void verifyNullWsHipieScenario() throws IOException, InterruptedException {
        when(HipieSingleton.getHipieWebService()).thenReturn(null);
        delegator.handleRequest(mockRequest, mockResponse);
        
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, mockResponse.getStatus());
        assertEquals("Unexpected Internal Error. Could not create wsHipie instance",
                mockResponse.getContentAsString());
        verifyLogging();
    }
    
    @Test
    public void verifyNullIwsResponseScenario() throws Exception {
        MockHttpServletRequest request = setupMockRequest();
        when(wsHipie.get(eq("myRequestUri"), same(paramsMap))).thenReturn(null);
        
        delegator.handleRequest(request, mockResponse);        
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, mockResponse.getStatus());
        assertEquals("Internal Server Error. Null IWsResponse received.",
                mockResponse.getContentAsString());
        verifyLogging();
    }
    
    @Test
    public void verifyOkayScenarios() throws Exception {
        IWsResponse iwsResponse = mock(IWsResponse.class);
        MockHttpServletRequest request = setupMockRequest();
        when(wsHipie.get(eq("myRequestUri"), same(paramsMap))).thenReturn(iwsResponse);
        when(iwsResponse.getResult()).thenReturn("iwsResult");
        when(iwsResponse.getMimeType()).thenReturn("myMimeType");
        
        when(iwsResponse.getErrors()).thenReturn(new ErrorBlock());
        validateResponse(request, HttpStatus.SC_OK, "iwsResult", "myMimeType");
        
        when(iwsResponse.getErrors()).thenReturn(null);
        validateResponse(request, HttpStatus.SC_OK, "iwsResult", "myMimeType");
        verifyLogging();
    }
    
    private void validateResponse(MockHttpServletRequest request, int statusCode,
            String contentString, String contentType)
            throws IOException, InterruptedException {
        mockResponse = new MockHttpServletResponse();        
        delegator.handleRequest(request, mockResponse);
        assertEquals(statusCode, mockResponse.getStatus());
        assertEquals(contentString, mockResponse.getContentAsString());
        assertEquals(contentType, mockResponse.getContentType());        
    }

    private MockHttpServletRequest setupMockRequest() {
        MockHttpServletRequest request = mock(MockHttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("myRequestUri");
        when(request.getParameterMap()).thenReturn(paramsMap);
        return request;
    }
    
    @Test
    public void verifyExceptionFromWsHipie() throws Exception {
        MockHttpServletRequest request = setupMockRequest();
        
        validateWsHipieException(request, HttpStatus.SC_BAD_REQUEST,
                "Hey, NO FORMAT DEFINED!!!", "Hey, NO FORMAT DEFINED!!!");
        
        validateWsHipieException(request, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Internal Server Error", "Misc error");
        
        validateWsHipieException(request, HttpStatus.SC_INTERNAL_SERVER_ERROR,
                "Internal Server Error", "");
        
        verifyLogging();
    }
    
    private void validateWsHipieException(MockHttpServletRequest request, int statusCode,
            String contentString, String errorString) throws Exception {
        mockResponse = new MockHttpServletResponse();
        when(wsHipie.get(eq("myRequestUri"), same(paramsMap))).thenThrow(new Exception(errorString));
        delegator.handleRequest(request, mockResponse);
        
        assertEquals(statusCode, mockResponse.getStatus());
        assertEquals(contentString, mockResponse.getContentAsString());
    }

}
