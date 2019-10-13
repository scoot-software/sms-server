/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.database;

import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserRole;
import com.scooter1556.sms.server.domain.UserRule;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.exception.DatabaseException;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public final class UserDatabase extends Database {    
    private static final String CLASS_NAME = "UserDatabase";
    
    public static final String DB_NAME = "User";
    public static final int DB_VERSION = 3;
    
    public UserDatabase() {
        super(DB_NAME, DB_VERSION);   
        
        // Initialise database
        try {
            LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Initialising database.", null);
            super.initialise();
        } catch (DatabaseException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error initialising database.", ex);
        } 
    }
    
    @Override
    public void create() {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Creating database.", null);
        
        // Create user database
        createUserTable();
        createUserRoleTable();
        createUserStatsTable();
        createUserRulesTable();
          
        try {
            // Add Default User
            getJdbcTemplate().update("INSERT INTO User (Username,Password) VALUES ('admin',?)", new Object[] {new BCryptPasswordEncoder().encode("admin")});
            getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) VALUES ('admin','ROLE_ADMIN')");
            getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) VALUES ('admin','ROLE_USER')");
            getJdbcTemplate().update("INSERT INTO UserStats (Username) VALUES ('admin')");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Unable to create default user, it may already exist.", null);
        }
    }
    
    private void createUserTable() {
        try {
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS User ("
                        + "Username VARCHAR(50) NOT NULL,"
                        + "Password VARCHAR(100) NOT NULL,"
                        + "Enabled BOOLEAN DEFAULT 1 NOT NULL,"
                        + "PRIMARY KEY (Username))");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error creating user table.", x);
        }
    }
    
    private void createUserRoleTable() {
        try {
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS UserRole ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Role VARCHAR(20) NOT NULL,"
                    + "PRIMARY KEY (Username,Role),"
                    + "FOREIGN KEY (Username) REFERENCES User (Username) ON DELETE CASCADE)");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error creating user role table.", x);
        }
    }
    
    private void createUserStatsTable() {
        try {
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS UserStats ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Streamed BIGINT DEFAULT 0 NOT NULL,"
                    + "Downloaded BIGINT DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (Username),"
                    + "FOREIGN KEY (Username) REFERENCES User (Username) ON DELETE CASCADE)");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error creating user statistics table.", x);
        }
    }
    
    private void createUserRulesTable() {
        try {
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS UserRules ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Path VARCHAR NOT NULL,"
                    + "Rule TINYINT NOT NULL,"
                    + "PRIMARY KEY (Username,Path),"
                    + "FOREIGN KEY (Username) REFERENCES User (Username) ON DELETE CASCADE)");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error creating user rule table.", x);
        }
    }
    
    public static final class UserMapper implements RowMapper {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setUsername(rs.getString("Username"));
            user.setPassword(rs.getString("Password"));
            user.setEnabled(rs.getBoolean("Enabled"));
            return user;
        }
    }
    
    public static final class UserRoleMapper implements RowMapper {
        @Override
        public UserRole mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRole userRole = new UserRole();
            userRole.setUsername(rs.getString("Username"));
            userRole.setRole(rs.getString("Role"));
            return userRole;
        }
    }
    
    public static final class UserStatsMapper implements RowMapper {
        @Override
        public UserStats mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserStats userStats = new UserStats();
            userStats.setUsername(rs.getString("Username"));
            userStats.setStreamed(rs.getLong("Streamed"));
            userStats.setDownloaded(rs.getLong("Downloaded"));
            return userStats;
        }
    }
    
    public static final class UserRuleMapper implements RowMapper {
        @Override
        public UserRule mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRule userRule = new UserRule();
            userRule.setUsername(rs.getString("Username"));
            userRule.setPath(rs.getString("Path"));
            userRule.setRule(rs.getByte("Rule"));
            return userRule;
        }
    }
    
    @Override
    public void upgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Upgrading database from version " + oldVersion + " to " + newVersion, null);

        if(newVersion == 2) {
            // Migrate passwords to bcrypt
            List<User> users = getJdbcTemplate().query("SELECT * FROM User", new UserMapper());
            getJdbcTemplate().update("ALTER TABLE User ALTER COLUMN Password VARCHAR (100) NOT NULL");

            users.forEach((user) -> {
                getJdbcTemplate().update("UPDATE User SET Password=? WHERE Username=?",
                        new Object[] {new BCryptPasswordEncoder().encode(user.getPassword()), user.getUsername()});
            });
        }
        
        if(newVersion == 3) {
            createUserRulesTable();
        }
    }
    
    @Override
    public void downgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Downgrading database from version " + oldVersion + " to " + newVersion, null);

        // Delete table and re-create
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME);
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME + "Role");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME + "Stats");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME + "Rules");
        create();
    }
}

