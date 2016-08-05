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

import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SettingsService;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class MediaDatabase {
    
    DataSource dataSource = null;
    
    private static final String CLASS_NAME = "MediaDatabase";
    
    JdbcTemplate jdbcTemplate;
    
    public MediaDatabase()
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
        ds.setUrl("jdbc:h2:" + SettingsService.getHomeDirectory() + "/db/media;" + "MV_STORE=FALSE;MVCC=FALSE;FILE_LOCK=FS");
        
        return ds;
    }
    
    private void createSchema()
    {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Initialising database.", null);
        
        try
        {
            // Media Elements
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS MediaElement ("
                    + "ID IDENTITY NOT NULL,"
                    + "Type TINYINT NOT NULL,"
                    + "DirectoryType TINYINT,"
                    + "Path VARCHAR NOT NULL,"
                    + "ParentPath VARCHAR NOT NULL,"
                    + "Created TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "LastPlayed TIMESTAMP,"
                    + "LastScanned TIMESTAMP NOT NULL,"
                    + "Excluded BOOLEAN DEFAULT 0 NOT NULL,"
                    + "Format VARCHAR(20),"
                    + "Size BIGINT,"
                    + "Duration INT,"
                    + "Bitrate INT,"
                    + "VideoWidth SMALLINT,"
                    + "VideoHeight SMALLINT,"
                    + "VideoCodec VARCHAR(20),"
                    + "AudioCodec VARCHAR,"
                    + "AudioSampleRate VARCHAR,"
                    + "AudioConfiguration VARCHAR,"
                    + "AudioLanguage VARCHAR,"
                    + "SubtitleLanguage VARCHAR,"
                    + "SubtitleFormat VARCHAR,"
                    + "SubtitleForced VARCHAR,"
                    + "Title VARCHAR NOT NULL,"
                    + "Artist VARCHAR,"
                    + "AlbumArtist VARCHAR,"
                    + "Album VARCHAR,"
                    + "Year SMALLINT,"
                    + "DiscNumber TINYINT,"
                    + "DiscSubtitle VARCHAR,"
                    + "TrackNumber SMALLINT,"
                    + "Genre VARCHAR,"
                    + "Rating REAL,"
                    + "Tagline VARCHAR,"
                    + "Description VARCHAR,"
                    + "Certificate VARCHAR(10),"
                    + "Collection VARCHAR,"
                    + "PRIMARY KEY (ID))");
            
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS PathIndex on MediaElement(Path)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS ParentPathIndex on MediaElement(ParentPath)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS TitleIndex on MediaElement(Title)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS ArtistIndex on MediaElement(Artist)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS AlbumArtistIndex on MediaElement(AlbumArtist)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS AlbumIndex on MediaElement(Album)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS GenreIndex on MediaElement(Genre)");
            
        }
        catch (DataAccessException x)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error initialising database.", x);
        }
    }
    
    private void updateSchema()
    {
        // Any updates to the database structure go here.
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Updating database.", null);
    }
}
