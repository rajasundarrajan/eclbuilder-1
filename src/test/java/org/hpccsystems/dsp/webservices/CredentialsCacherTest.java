package org.hpccsystems.dsp.webservices;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.ramps.utils.RampsTesterBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests the functionality available in the CredentialsCacher
 * 
 * @author Ashoka_K
 *
 */
//Ignoring this particular test since it takes a few seconds to run. Will delay startup on local (via mvn install).
//To run, just comment the @Ignore and run the test
@Ignore
public class CredentialsCacherTest extends RampsTesterBase {
	
	private static final Logger LOGGER = LogManager.getLogger(CredentialsCacher.class);
	
	@Before
	public void setup() {
		initAppender(LOGGER);
		messages.clear();
	}
    
    @Test
    public void testConstructor() throws DspWebserviceException, InterruptedException {
        CredentialsCacher credsCacher = new CredentialsCacher(1234, 4321, false);
        assertEquals(1234, getInternalState(credsCacher, "credentialsValidityDuration"));
        assertEquals(4321, getInternalState(credsCacher, "cleanupInterval"));
        assertFalse((boolean) getInternalState(credsCacher, "userIdCaseSensitive"));
        
        credsCacher = new CredentialsCacher(1234, 4321, true);
        assertTrue((boolean) getInternalState(credsCacher, "userIdCaseSensitive"));
        
        verifyLogging();
    }
    
    @Test
    public void verifyInvalidCredentialsValidityDuration() throws DspWebserviceException, InterruptedException {
        verifyException(-1, 1, true);
        verifyException(0, 1, true);
        
        verifyException(1, -1, true);
        verifyException(1, 0, true);
        
        verifyException(1, 1, false);
        
        verifyLogging();
    }
    
    private void verifyException(int credentialsValidityDuration, int cleanupInterval, boolean throwsException) {
        DspWebserviceException exception = null;
        
        try {
            new CredentialsCacher(credentialsValidityDuration, cleanupInterval, true);
        } catch(Exception e) {
            exception = (DspWebserviceException) e;
        }
        
        if(throwsException) {
            assertEquals("credentialsValidityDuration and cleanupInterval must be greater than 0",
                    exception.getMessage());
        } else {
            Assert.assertNull(exception);
        }
    }
    
    @Test
    public void verifyCredentialsAdditionWithCaseSensitivity() throws DspWebserviceException, InterruptedException {
        CredentialsCacher credsCacher = new CredentialsCacher(1800, 1800, true);
        assertFalse(credsCacher.verifyAndUpdateCredentials("user2", "Pw2"));
        
        credsCacher.addToCache("user1", "pw1");
        credsCacher.addToCache("User2", "Pw2");
        
        assertTrue(credsCacher.verifyAndUpdateCredentials("user1", "pw1"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("user2", "Pw2"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("User2", "pw2"));
        
        credsCacher = new CredentialsCacher(1800, 1800, false);
        credsCacher.addToCache("user1", "pw1");
        credsCacher.addToCache("User2", "Pw2");
        assertTrue(credsCacher.verifyAndUpdateCredentials("user1", "pw1"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("user2", "Pw2"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("User2", "pw2"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("user1", "pw2"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User1", "pw1"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("user3", "pw2"));
        
        verifyLogging();
    }
    
    @SuppressWarnings("rawtypes")
	@Test
    public void verifyCredentialsExpiry() throws InterruptedException, DspWebserviceException {
        CredentialsCacher credsCacher = new CredentialsCacher(1, 1800, true);
        credsCacher.addToCache("user1", "pw1");
        sleep(300);
        credsCacher.addToCache("User2", "Pw2");
        sleep(300);
        credsCacher.addToCache("User3", "Pw3");
        
        assertTrue(credsCacher.verifyAndUpdateCredentials("user1", "pw1"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User3", "Pw3"));
        
        sleep(600);
        assertFalse(credsCacher.verifyAndUpdateCredentials("user1", "pw1"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User3", "Pw3"));
        Map credsCache = (Map) getInternalState(credsCacher, "credentialsCache");
        assertEquals(2, credsCache.size());
        
        sleep(250);
        assertFalse(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertTrue(credsCacher.verifyAndUpdateCredentials("User3", "Pw3"));
        assertFalse(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        
        sleep(250);
        assertFalse(credsCacher.verifyAndUpdateCredentials("user1", "pw1"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("User2", "Pw2"));
        assertFalse(credsCacher.verifyAndUpdateCredentials("User3", "Pw3"));
        assertEquals(0, credsCache.size());
        
        verifyLogging();
    }
    
    @SuppressWarnings("rawtypes")
	@Test
    public void verifyCleanup() throws InterruptedException, DspWebserviceException {
        CredentialsCacher credsCacher = new CredentialsCacher(1, 2, true);
        credsCacher.addToCache("user1", "pw1");
        sleep(1200);
        credsCacher.addToCache("User2", "Pw2");
        credsCacher.addToCache("User3", "Pw3");
        
        Map credsCache = (Map) getInternalState(credsCacher, "credentialsCache");
        assertTrue(credsCache.containsKey("user1"));
        assertTrue(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        
        sleep(1000);
        assertFalse(credsCache.containsKey("user1"));
        assertTrue(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        
        sleep(1000);
        assertTrue(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        assertEquals(2, credsCache.size());
        
        sleep(2000);
        assertEquals(0, credsCache.size());
        
        verifyLogging();
    }
    
    @SuppressWarnings("rawtypes")
	@Test
    public void verifyRefreshCredsCache() throws InterruptedException, DspWebserviceException {
        CredentialsCacher credsCacher = new CredentialsCacher(1, 2, true);
        credsCacher.addToCache("user1", "pw1");
        sleep(1200);
        credsCacher.addToCache("User2", "Pw2");
        credsCacher.addToCache("User3", "Pw3");
        
        Map credsCache = (Map) getInternalState(credsCacher, "credentialsCache");
        assertTrue(credsCache.containsKey("user1"));
        assertTrue(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        
        sleep(1000);
        assertFalse(credsCache.containsKey("user1"));
        assertTrue(credsCache.containsKey("User2"));
        assertTrue(credsCache.containsKey("User3"));
        
        credsCacher.refreshCredsCache();
        
        sleep(2000);
        assertEquals(0, credsCache.size());
        
        verifyLogging();
    }
    
}
