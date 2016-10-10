package org.hpccsystems.dsp.service;

public interface SettingsService {
    
    public static final String DEV_MODE = "dev_mode";
    public static final String MAINTENANCE_MODE = "maintenance_mode";
    public static final String SPRAY_RETRY_COUNT = "spray_retry_count";
    public static final String STATIC_FILE_SIZE_MB = "static_size_limit_mb";
    public static final String SESSION_TIMEOUT_SECONDS = "session_timeout_seconds";

    static final String ENABLED = "enabled";
    static final String DISABLED = "disabled";
    
    static final int DEFAULT_SPRAY_RETRY_COUNT = 5;
    static final int DEFAULT_STATIC_FILE_SIZE_MB = 5;
    static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 1200;
    
    public boolean isDevEnabled();
    
    public void enableDevMode();
    
    public void disableDevMode();

    public boolean isMaintenanceEnabled();

    public void enableMaintenanceMode();

    public void disableMaintenanceMode();

    public int getSprayRetryCount();
    
    public void updateSprayRetryCount(int count);

    public int getStaticDataSize();
    
    public void updateStaticDataSize(int sizeInMB);

    public int getSessionTimeout();
    
    public void updateSessionTimeout(int seconds);
}
