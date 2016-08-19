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
package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.exception.DatabaseException;
import com.scooter1556.sms.server.service.SettingsService;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DatabaseUtils {
    
    public static DataSource getDataSource(String db, int version) throws DatabaseException {
        if(SettingsService.getHomeDirectory() == null) {
            throw new DatabaseException("Home directory is unreachable!");
        }
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.jdbcx.JdbcDataSource");
        dataSource.setUrl("jdbc:h2:" + SettingsService.getHomeDirectory() + "/db/" + db.toLowerCase() + "." + version + ";" + "MV_STORE=FALSE;MVCC=FALSE;FILE_LOCK=FS");
        
        return dataSource;
    }
    
    public static boolean isDatabaseAvailable(String db) {
        File[] files = getDatabaseFiles(db);
        
        if(files == null) {
            return false;
        }
        
        // Check we found a database file
        return files.length != 0;
    }
    
    public static File[] getDatabaseFiles(String db) {
        if(SettingsService.getHomeDirectory() == null) {
            return null;
        }
        
        File dir = new File(SettingsService.getHomeDirectory() + "/db/");
        FileFilter fileFilter = new WildcardFileFilter(db.toLowerCase() + ".*.h2.db");
        
        return dir.listFiles(fileFilter);
    }
    
    public static int getDatabaseVersion(String db) {
        File[] files = getDatabaseFiles(db);
        
        if(files == null) {
            return -1;
        }
        
        // Check we found the database file
        if(files.length == 0) {
            return -1;
        }
        
        // Extract database version
        int version = 0;
        
        for (File file : files) {
            int v = Integer.valueOf(file.getName().split("\\.")[1]);
            
            if(v > version) {
                version = v;
            }
        }
        
        return version;
    }
    
    public static boolean createNewDatabaseFile(String db, int oldVersion, int newVersion) {
        if(SettingsService.getHomeDirectory() == null) {
            return false;
        }
            
        try {
            Path oldDb = Paths.get(SettingsService.getHomeDirectory() + "/db/" + db.toLowerCase() + "." + oldVersion + ".h2.db");
            Path newDb = Paths.get(SettingsService.getHomeDirectory() + "/db/" + db.toLowerCase() + "." + newVersion + ".h2.db");
            
            // Copy old database file to a new file and remove the old one
            Files.copy(oldDb, newDb);
            
            // Remove old database file
            return oldDb.toFile().delete();
        } catch (IOException ex) {
            return false;
        }
    }
}
