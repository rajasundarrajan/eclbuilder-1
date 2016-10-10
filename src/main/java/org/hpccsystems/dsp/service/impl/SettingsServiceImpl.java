package org.hpccsystems.dsp.service.impl;

import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service("settingsService")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SettingsServiceImpl implements SettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsServiceImpl.class);
    
    private DSPDao dspDao;
    
    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }

    @Override
    public boolean isDevEnabled() {
        String devMode;
        try {
            devMode = dspDao.getAppSetting(DEV_MODE);
            // TODO : once min visualization api is ready return false
            if (devMode == null) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Application setting dev mode is not presnt in Database.", e);
            //As no setting is found in database returning default setting ie, Disabled dev_mode
            // TODO : once min visualization api is ready return false
            return true;
        }
        
        LOGGER.debug("DevMode - {}", devMode);
        
        return ENABLED.equals(devMode);
    }

    @Override
    public int getSprayRetryCount() {
        int count = DEFAULT_SPRAY_RETRY_COUNT;
        
        try {
            count = Integer.parseInt(dspDao.getAppSetting(SPRAY_RETRY_COUNT));
        } catch (Exception e) {
            LOGGER.error("App setting retry count not present in Database.", e);
        }
        
        LOGGER.debug("Retry count - {}", count);
        
        return count;
    }

    @Override
    public int getStaticDataSize() {
        int mb = DEFAULT_STATIC_FILE_SIZE_MB;
        
        try {
            mb = Integer.parseInt(dspDao.getAppSetting(STATIC_FILE_SIZE_MB));
        } catch (Exception e) {
            LOGGER.error("App setting file size limit missing", e);
        }
        
        return mb;
    }

    @Override
    public int getSessionTimeout() {
        int sessionTimeout = DEFAULT_SESSION_TIMEOUT_SECONDS;
        
        try {
            sessionTimeout = Integer.parseInt(dspDao.getAppSetting(SESSION_TIMEOUT_SECONDS));
        } catch (Exception e) {
            LOGGER.error("Session timeout retrival failed from DB", e);
        }
        
        return sessionTimeout;
    }
    
    @Override
    public void enableDevMode() {
        dspDao.saveAppSetting(DEV_MODE, ENABLED);        
    }

    @Override
    public void disableDevMode() {
        dspDao.saveAppSetting(DEV_MODE, DISABLED);
    }

    @Override
    public void updateSprayRetryCount(int count) {
        dspDao.saveAppSetting(SPRAY_RETRY_COUNT, String.valueOf(count));
    }

    @Override
    public void updateStaticDataSize(int size) {
        dspDao.saveAppSetting(STATIC_FILE_SIZE_MB, String.valueOf(size));
    }


    @Override
    public void updateSessionTimeout(int seconds) {
        dspDao.saveAppSetting(SESSION_TIMEOUT_SECONDS, String.valueOf(seconds));
    }

    @Override
    public void enableMaintenanceMode() {
        dspDao.saveAppSetting(MAINTENANCE_MODE, ENABLED);
    }

    @Override
    public void disableMaintenanceMode() {
        dspDao.saveAppSetting(MAINTENANCE_MODE, DISABLED);

    }

    @Override
    public boolean isMaintenanceEnabled() {
        String maintenanceMode;
        try {
            maintenanceMode = dspDao.getAppSetting(MAINTENANCE_MODE);
            LOGGER.error("maintenance mode -{}", maintenanceMode);
            if (maintenanceMode == null) {
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Application setting maintenance mode is not presnt in Database.", e);
            return false;
        }
        return ENABLED.equals(maintenanceMode);
    }

}
