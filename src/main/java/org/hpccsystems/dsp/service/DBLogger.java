package org.hpccsystems.dsp.service;

import java.util.Date;
import java.util.List;

import org.hpccsystems.dsp.exceptions.DatabaseException;
import org.hpccsystems.dsp.log.DBLog;
import org.hpccsystems.dsp.log.UserLog;

public interface DBLogger {
    void log(DBLog log);

    List<UserLog> getUserLog() throws DatabaseException;

    List<UserLog> getUserLogByDate(Date startDate, Date endDate) throws DatabaseException;
}
