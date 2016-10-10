package org.hpccsystems.dsp.webservices;

import static org.hpccsystems.dsp.ramps.utils.RampsLogger.debug;
import static org.hpccsystems.dsp.ramps.utils.RampsLogger.info;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hpccsystems.dsp.ramps.utils.RampsLogger;

/**
 * Provides an in-memory cache for the provided id and password combination. The caller needs to add a valid
 * id/password combination to the CredentialsCacher. The id/password combination will be available until the
 * credentials validity duration. The cache will be cleared of all expired at a certain interval as
 * specified by the cleanup interval.
 * 
 * @author Ashoka_K
 *
 */
public class CredentialsCacher implements Runnable {
    
    private static final Logger LOGGER = LogManager.getLogger(CredentialsCacher.class);
    
    private int credentialsValidityDuration;
    
    private int cleanupInterval;
    
    private boolean userIdCaseSensitive = false;
    
    private Map<String, CredentialsInfo> credentialsCache = new HashMap<String, CredentialsInfo>();
    
    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    
    private Thread credentialsCleanupThread;
    
    private class CredentialsInfo {
        private String password;
        
        private long expiryTime;
        
        public CredentialsInfo(String password) {
            this.password = password;
            this.expiryTime = System.currentTimeMillis() + (credentialsValidityDuration * 1000);
        }
    }
    
    /**
     * Arged constructor
     * @see CredentialsCacher
     * 
     * @param credentialsValidityDuration The duration (in seconds) for which a given id/password combination is
     * valid. The value provided must be greater than 0.
     * @param cleanupInterval The interval between successive runs (in seconds) of cleanup of expired credentials.
     * The value provided must be greater than 0.
     * @param userIdCaseSensitive Indicates whether the user id provided is case sensitive
     * 
     * @throws DspWebserviceException In case an invalid credentialsValidityDuration or cleanupInterval
     * is specified
     */
    public CredentialsCacher(int credentialsValidityDuration, int cleanupInterval,
            boolean userIdCaseSensitive) throws DspWebserviceException {
        super();
        
        if(credentialsValidityDuration < 1 || cleanupInterval < 1) {
            throw new DspWebserviceException("credentialsValidityDuration and cleanupInterval must be greater than 0");
        }
        
        info(LOGGER, "CredentialsCacher intialized. The following values were sent in\n"
                + "credentialsValidityDuration: %s\ncleanupInterval: %s\nuserIdCaseSensitive: %s",
                credentialsValidityDuration, cleanupInterval, userIdCaseSensitive);
        
        this.credentialsValidityDuration = credentialsValidityDuration;
        this.cleanupInterval = cleanupInterval;
        this.userIdCaseSensitive = userIdCaseSensitive;
        
        credentialsCleanupThread = new Thread(this);
        credentialsCleanupThread.start();
    }
    
    /**
     * Determines if the user id/password combination is correct and is still valid (i.e. not expired).
     * In case the id/password combination is expired, the same is removed from the cache.
     * 
     * @param userId The id of the user
     * @param password The password of the user
     * 
     * @return A boolean value indicating if the id/password combination was found in the cache.
     */
    public boolean verifyAndUpdateCredentials(String userId, String password) {
        String userIdWithCase = userIdCaseSensitive? userId: userId.toUpperCase();
        CredentialsInfo credentialsInfo = null;
        
        try {
            readWriteLock.readLock().lock();
            credentialsInfo = credentialsCache.get(userIdWithCase);
        } finally {
            readWriteLock.readLock().unlock();
        }
        
        if(credentialsInfo != null) {
            if(System.currentTimeMillis() < credentialsInfo.expiryTime) {
                if(credentialsInfo.password.equals(password)) {
                    debug(LOGGER, "Credentials found for user %s. Password matches", userId);
                    return true;
                } else {
                    debug(LOGGER, "Credentials found for user %s. Password doesn't match", userId);
                    return false;
                }
            } else {
                try {
                    readWriteLock.writeLock().lock();
                    debug(LOGGER, "Credentials for user %s expired. Removing from cache", userId);
                    credentialsCache.remove(userId);
                } finally {
                    readWriteLock.writeLock().unlock();
                }
            }
        }
        
        return false;
    }
    
    /**
     * Adds the given (authenticated) id/password combination to the cache
     * 
     * @param userId The user id to be added to the cache
     * @param password The password to be added to the cache
     */
    public void addToCache(String userId, String password) {
        try {
            readWriteLock.writeLock().lock();
            String userIdWithCase = userIdCaseSensitive? userId: userId.toUpperCase();
            debug(LOGGER, "Adding credentials for user %s to the cache", userId);            
            credentialsCache.put(userIdWithCase, new CredentialsInfo(password));
            debug(LOGGER, "Added credentials for user %s to the cache", userId);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }
    
    private void cleanup() {
        try {
            readWriteLock.writeLock().lock();
            info(LOGGER, "Cleaning expired credentials");
            long currentTime = System.currentTimeMillis();            
            
            for(Iterator<Map.Entry<String, CredentialsInfo>> iter = credentialsCache.entrySet().iterator();
                    iter.hasNext();) {
                if(iter.next().getValue().expiryTime < currentTime) {
                    iter.remove();
                }
            }
            
            info(LOGGER, "Cleaned up expired credentials");
        } finally {
            readWriteLock.writeLock().unlock();
        }
        
    }
    
    /**
     * Cleans up the expired credentials from the cache.
     */
    public void refreshCredsCache() {
        cleanup();
    }

    /**
     * Runs a cleanup of expired credentials at an interval as specified by the cleanup interval.
     */
    @Override
    public void run() {
        info(LOGGER, "Cleanup cycles initiated");
        
        while(true) {
            try {
                info(LOGGER, "Sleeping for %s seconds", cleanupInterval);
                Thread.sleep(cleanupInterval * 1000);
                info(LOGGER, "Woke up. Initiating cleanup", cleanupInterval);                
                cleanup();
            } catch (InterruptedException e) {
                RampsLogger.info(LOGGER, "The credentials cleanup was interrupted. Closing thread.");
                break;
            }
        }
    }
    
    /**
     * Terminates the credentials cleaup thread
     */
    public void close() {
        credentialsCleanupThread.interrupt();
    }
    
}
