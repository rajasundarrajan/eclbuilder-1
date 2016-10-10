package org.hpccsystems.dsp.service.impl;

import java.util.Date;
import java.util.List;

import org.hpccsystems.dsp.dao.DSPDao;
import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.log.DBLog;
import org.hpccsystems.dsp.log.UserLog;
import org.hpccsystems.dsp.service.DBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

@Service("dbLogger")
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DBLoggerImpl implements DBLogger {
    
    private DSPDao dspDao;
    
    @Autowired
    public void setRampsDao(DSPDao dspDao) {
        this.dspDao = dspDao;
    }
    
    @Override
    public void log(DBLog log) {
        dspDao.log(log);
    }
    
    @Override
    public List<UserLog> getUserLog() throws DatabaseException {
        return dspDao.getUserLog();
    }
    
    @Override
    public List<UserLog> getUserLogByDate(Date startDate, Date endDate) throws DatabaseException {
        return dspDao.getUserLogByDate(startDate, endDate);
    }

}
