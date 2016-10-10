package org.hpccsystems.dsp.webservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hpccsystems.dsp.ramps.utils.RampsLogger;
import org.hpccsystems.dsp.service.DBLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the functionality available in teh RequestIdInterceptorSetter
 * 
 * @author Ashoka_K
 *
 */
public class RequestIdInterceptorSetterTest {
    
    @Mock private DBLogger dbLogger;
    
    @InjectMocks    
    private RequestIdSetterInterceptor interceptor = new RequestIdSetterInterceptor();
    
    private HttpServletRequest request = new MockHttpServletRequest();
    
    private HttpServletResponse response = new MockHttpServletResponse();
    
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void verifyReturn() {
        assertTrue(interceptor.preHandle(request, response, new Object()));
    }
    
    @Test
    public void verifyUniquenessOfIdGenerated() {
        int numInvocations = 100000;
        Set<String> idsGenerated = new HashSet<String>(numInvocations);
        
        for(int i = 0; i < numInvocations; i++) {
            interceptor.preHandle(request, response, new Object());
            idsGenerated.add(RampsLogger.THREAD_ID.get());
        }
        
        //In case any of the generated IDs are not unique, the size of the idsGenerated
        //Set will be less than numInvocations (since a Set contains only unique values)
        assertEquals(numInvocations, idsGenerated.size());
    }

}
