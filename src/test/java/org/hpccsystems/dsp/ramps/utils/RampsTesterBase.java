package org.hpccsystems.dsp.ramps.utils;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.MockAppender;

/**
 * A base class for tests which verify classes that log using the RampsLogger
 * 
 * @author Ashoka_K
 *
 */
public class RampsTesterBase {
	
    protected MockAppender mockAppender = new MockAppender();
    
    protected List<String> messages;
    
    protected void initAppender(Logger logger) {
    	logger.removeAllAppenders();
    	logger.addAppender(mockAppender);
    	logger.setLevel(Level.DEBUG);
    	messages = mockAppender.getMessages();
    }
	
	protected void verifyLogging() throws InterruptedException {
		Thread.sleep(10);
        assertTrue(messages.size() > 0);
	}

}
