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
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.utilities.DatabaseUtils;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class Database {
    
    DataSource dataSource = null;
    JdbcTemplate jdbcTemplate;
    
    String db;
    int version;
    
    public Database(String db, int version) {
        this.db = db;
        this.version = version;
    }
    
    public void initialise() throws DatabaseException {
        // Check database version
        int currentVersion = DatabaseUtils.getDatabaseVersion(db);

        // Database doesn't exist
        if(currentVersion == -1) {
            dataSource = DatabaseUtils.getDataSource(db, version);
            create();
        }

        // If an older version of the database exists copy it to a new file and upgrade
        else if(currentVersion < version) {
            DatabaseUtils.createNewDatabaseFile(db, currentVersion, version);
            dataSource = DatabaseUtils.getDataSource(db, version);
            upgrade(currentVersion, version);
        }

        // If a newer version of the database exists copy it to a new file and downgrade
        else if(currentVersion > version) {
            DatabaseUtils.createNewDatabaseFile(db, currentVersion, version);
            dataSource = DatabaseUtils.getDataSource(db, version);
            downgrade(currentVersion, version);
        }

        // If versions match simply load the database
        else if(currentVersion == version) {
            dataSource = DatabaseUtils.getDataSource(db, version);
        }
        
        else {
            throw new DatabaseException("Unable to process database version.");
        }
    }
    
    public void create() {
        // To be overridden
    }
    
    public void upgrade(int oldVersion, int newVersion) {
        // To be overridden
    }
    
    public void downgrade(int oldVersion, int newVersion) {
        // To be overridden
    }
    
    public static DataSource getDataSource(String db, int version) throws DatabaseException {
        if(SettingsService.getHomeDirectory() == null) {
            throw new DatabaseException("Home directory is unreachable!");
        }
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        dataSource.setUrl("jdbc:h2:" + SettingsService.getHomeDirectory() + "/db/" + db.toLowerCase() + "." + version + ";" + "MV_STORE=FALSE;MVCC=FALSE;FILE_LOCK=FS");
        
        return dataSource;
    }
    
    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
