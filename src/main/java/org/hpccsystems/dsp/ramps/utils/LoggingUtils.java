package org.hpccsystems.dsp.ramps.utils;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public abstract class LoggingUtils {
    private static Map<String, Appender> map = new HashMap<String, Appender>();
    private static Logger logger = null;
    static {
        Appender appender = null;
        Enumeration<?> e = LogManager.getRootLogger().getAllAppenders();
        while (e.hasMoreElements()) {
            appender = (Appender) e.nextElement();
            if (appender instanceof FileAppender) {
                map.put(appender.getName(), appender);
            }
        }

        Enumeration<?> loggers = LogManager.getCurrentLoggers();
        Enumeration<?> appenders = null;
        while (loggers.hasMoreElements()) {
            logger = (Logger) loggers.nextElement();
            appenders = logger.getAllAppenders();
            while (appenders.hasMoreElements()) {
                appender = (Appender) appenders.nextElement();
                if (appender instanceof FileAppender) {
                    map.put(appender.getName(), appender);
                }
            }
        }
    }

    private LoggingUtils() {
    }

    public static Collection<Appender> getFileAppenders() {

        return map.values();
    }

    public static Appender getFileAppender(String appenderName) {

        return map.get(appenderName);
    }

}
