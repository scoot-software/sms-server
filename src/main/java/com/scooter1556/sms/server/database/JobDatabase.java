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
public final class JobDatabase {
    
    DataSource dataSource = null;
    
    private static final String CLASS_NAME = "JobDatabase";
    
    JdbcTemplate jdbcTemplate;
    
    public JobDatabase()
    {
        if(SettingsService.getHomeDirectory() != null)
        {
            dataSource = getDataSource();
            createSchema();
            updateSchema();
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
        ds.setUrl("jdbc:h2:" + SettingsService.getHomeDirectory() + "/db/job");
        
        return ds;
    }
    
    private void createSchema()
    {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Initialising database.", null);
        
        try
        {
            // Jobs
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS Job ("
                    + "ID IDENTITY NOT NULL,"
                    + "Type TINYINT NOT NULL,"
                    + "Username VARCHAR(50) NOT NULL,"
                    + "MediaElement BIGINT NOT NULL,"
                    + "StartTime TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "EndTime TIMESTAMP,"
                    + "LastActivity TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "BytesTransferred BIGINT DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (ID))");
            
        }
        catch (DataAccessException x)
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error initialising database.", x);
        }
    }
    
    private void updateSchema()
    {
        // Any updates to the database structure go here.
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Updating database.", null);
    }
}

