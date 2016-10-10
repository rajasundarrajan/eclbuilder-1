package org.hpccsystems.dsp;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * A mock Appender implementation for junit testing purposes
 * 
 * @author Ashoka_K
 *
 */
public class MockAppender extends AppenderSkeleton {
    public List<String> messages = new ArrayList<String>();

    public void doAppend(LoggingEvent event) {
        messages.add(event.getMessage().toString());
    }
    
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent arg0) {
    }
    
}
