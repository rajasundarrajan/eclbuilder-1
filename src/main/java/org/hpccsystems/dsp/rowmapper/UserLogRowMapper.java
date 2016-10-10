package org.hpccsystems.dsp.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hpccsystems.dsp.log.UserLog;
import org.springframework.jdbc.core.RowMapper;

public class UserLogRowMapper implements RowMapper<UserLog> {

    @Override
    public UserLog mapRow(ResultSet rs, int arg1) throws SQLException {

        return new UserLog(rs.getString("user_id"), rs.getString("action"), rs.getString("detail"), rs.getLong("start_time"), rs.getString("session_id"),rs.getLong("memory"));
    }
}
