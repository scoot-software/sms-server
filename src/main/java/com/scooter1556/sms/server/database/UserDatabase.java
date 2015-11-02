/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.database;

import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.SettingsService;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/**
 *
 * @author scott2ware
 */

@Component
public final class UserDatabase {
    
    DataSource dataSource = null;
    
    private static final String CLASS_NAME = "UserDatabase";
    
    JdbcTemplate jdbcTemplate;
    
    public UserDatabase()
    {
        if(SettingsService.getHomeDirectory() != null)
        {
            dataSource = getDataSource();
            createSchema();
            //updateSchema();
        }
    }
    
    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
    
    public static DataSource getDataSource()
    {   
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        ds.setUrl("jdbc:h2:" + SettingsService.getHomeDirectory() + "/db/user");
        
        return ds;
    }
    
    private void createSchema()
    {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Initialising database.", null);
        
        try
        {
            // Users
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS User ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Password VARCHAR(50) NOT NULL,"
                    + "Enabled BOOLEAN DEFAULT 1 NOT NULL,"
                    + "PRIMARY KEY (Username))");

            // User Role
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS UserRole ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Role VARCHAR(20) NOT NULL,"
                    + "PRIMARY KEY (Username,Role),"
                    + "FOREIGN KEY (Username) REFERENCES User (Username) ON DELETE CASCADE)");
            
            // User Statistics
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS UserStats ("
                    + "Username VARCHAR(50) NOT NULL,"
                    + "Streamed BIGINT DEFAULT 0 NOT NULL,"
                    + "Downloaded BIGINT DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (Username),"
                    + "FOREIGN KEY (Username) REFERENCES User (Username) ON DELETE CASCADE)");
            
            
        }
        catch (DataAccessException x)
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error initialising database.", x);
        }
          
        try {
            // Add Default User
            getJdbcTemplate().update("INSERT INTO User (Username,Password) VALUES ('admin','admin')");
            getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) VALUES ('admin','ROLE_ADMIN')");
            getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) VALUES ('admin','ROLE_USER')");
            getJdbcTemplate().update("INSERT INTO UserStats (Username) VALUES ('admin')");
        }    
        catch (DataAccessException x)
        {
            LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Unable to create default user, it may already exist.", null);
        }
    }
    
    private void updateSchema()
    {
        // Any updates to the database structure go here.
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Updating database.", null);
    }
}

