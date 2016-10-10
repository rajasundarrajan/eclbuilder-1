package org.hpccsystems.dsp.ramps.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

public class RampsLogger {

    /** Holds the thread id for the current thread (if set) */
    public static final ThreadLocal<String> THREAD_ID = new ThreadLocal<String>();

    /** Holds the user id for the current thread (if set) */
    public static final ThreadLocal<String> USER_ID = new ThreadLocal<String>();

    private static final String MESSAGE_FMT = "Req ID: %s, User ID: %s, %s";
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RampsLogger.class);
    private RampsLogger(){
    }

    /**
     * Logs the given message and the throwable (if passed in) using the Logger
     * provided. The provided <code>objects</code> are replaced into the the
     * <code>messageFmt</code> as arguments. See
     * {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is equal
     * to or is higher than the log level of the <code>level</code> passed in.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param level
     *            The Log level
     * @param logAsJson
     *            In case the <code>logAsJson</code> is passed in as true, the
     *            <code>objects</code> are converted to a JSON string before
     *            replacing them into <code>messageFmt</code>
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param throwable
     *            The throwable/exception if any (in case an error resulted in
     *            this call)
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void log(Logger inputLogger, Level level, boolean logAsJson, String messageFmt, Throwable throwable, Object... objects) {
        Level logLevel = getLevel(inputLogger);

        if (logLevel == null) {
            LOGGER.error("From RampsLogger.log() >>>> Log level for " + inputLogger.getName() + " is null");
            return;
        }

        if (level.isGreaterOrEqual(logLevel)) {
            try {
                Object[] objArr = (logAsJson && objects != null) ? convertToJsonStringArray(objects) : objects;
                String callerMessage = String.format(messageFmt, objArr);
                String messageToLog = String.format(MESSAGE_FMT, THREAD_ID.get(), USER_ID.get(), callerMessage);

                if (throwable == null) {
                    inputLogger.log(level, messageToLog);
                } else {
                    inputLogger.error(messageToLog, throwable);
                }
            } catch (Exception e) {
                LOGGER.error("From RampsLogger.log() >>>> Exception when logging-->{}", e);
            }
        }
    }

    private static Level getLevel(Logger logger) {
        Level level = logger.getLevel();
        int counter = 0;

        if (level == null && logger.getParent() != null && (++counter < 100)) {
            level = logger.getParent().getLevel();
        }

        return level;
    }

    private static Object[] convertToJsonStringArray(Object[] objects) {
        int counter = 0;
        Object[] objJsonArr = new Object[objects.length];

        for (Object object : objects) {
            if (object != null) {
                objJsonArr[counter] = RampsUtil.convertToJsonString(object);
            }

            counter++;
        }

        return objJsonArr;
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> replace the arguments in the <code>messageFmt</code>
     * See {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Trace
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void trace(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.TRACE, false, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> are converted to a JSON string and are replaced as
     * arguments into the <code>messageFmt</code> See
     * {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Trace
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void traceAsJson(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.TRACE, true, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> replace the arguments in the <code>messageFmt</code>
     * See {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Debug
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void debug(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.DEBUG, false, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> are converted to a JSON string and are replaced as
     * arguments into the <code>messageFmt</code> See
     * {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Debug
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void debugAsJson(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.DEBUG, true, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> replace the arguments in the <code>messageFmt</code>
     * See {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Info
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void info(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.INFO, false, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> are converted to a JSON string and are replaced as
     * arguments into the <code>messageFmt</code> See
     * {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Info
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void infoAsJson(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.INFO, true, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> replace the arguments in the <code>messageFmt</code>
     * See {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Warn
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void warn(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.WARN, false, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> are converted to a JSON string and are replaced as
     * arguments into the <code>messageFmt</code> See
     * {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Error
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void warnAsJson(Logger inputLogger, String messageFmt, Object... objects) {
        log(inputLogger, Level.WARN, true, messageFmt, null, objects);
    }

    /**
     * Logs the given message using the Logger provided. The provided
     * <code>objects</code> replace the arguments in the <code>messageFmt</code>
     * . See {@link String#format(String, Object...)}
     * 
     * The logging happens only if the log level for the calling class is Error
     * or higher.
     * 
     * @param inputLogger
     *            The Logger object on which the logging is to be invoked. This
     *            should be the Logger object corresponding to the calling
     *            class.
     * @param messageFmt
     *            The String (with place holders if required) to be logged. See
     *            {@link String#format(String, Object...)}
     * @param throwable
     *            The throwable/exception which caused this error
     * @param objects
     *            The arguments to be replaced into the <code>messaegFmt</code>
     */
    public static void error(Logger inputLogger, String messageFmt, Throwable throwable, Object... objects) {
        log(inputLogger, Level.ERROR, false, messageFmt, throwable, objects);
    }

}
