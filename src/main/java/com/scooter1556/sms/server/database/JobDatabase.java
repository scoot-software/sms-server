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

import com.scooter1556.sms.server.exception.DatabaseException;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public final class JobDatabase extends Database {
    private static final String CLASS_NAME = "JobDatabase";
    
    public static final String DB_NAME = "Job";
    public static final int DB_VERSION = 2;
        
    public JobDatabase() {
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
        
        try {
            // Jobs
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS Job ("
                    + "ID UUID NOT NULL,"
                    + "Type TINYINT NOT NULL,"
                    + "Username VARCHAR(50) NOT NULL,"
                    + "MediaElement UUID NOT NULL,"
                    + "StartTime TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "EndTime TIMESTAMP,"
                    + "LastActivity TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "BytesTransferred BIGINT DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (ID))");
            
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error creating database.", x);
        }
    }
    
    @Override
    public void upgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Upgrading database from version " + oldVersion + " to " + newVersion, null);
        
        if(newVersion == 2) {
            getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME);
            create();
        }
    }
    
    @Override
    public void downgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Downgrading database from version " + oldVersion + " to " + newVersion, null);
        
        // Delete table and re-create
        getJdbcTemplate().execute("DROP TABLE IF EXISTS " + DB_NAME);
        create();
    }
}

