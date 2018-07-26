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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class MediaDatabase extends Database {
    private static final String CLASS_NAME = "MediaDatabase";
    
    public static final String DB_NAME = "Media";
    public static final int DB_VERSION = 5;
    
    public MediaDatabase() {
        super(DB_NAME, DB_VERSION);   
        
        // Initialise database
        try {
            LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Initialising database.", null);
            super.initialise();
        } catch (DatabaseException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error initialising database.", ex);
        }  
    }
    
    @Override
    public void create() {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Creating database.", null);
        
        try {
            // Media Elements
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS MediaElement ("
                    + "ID UUID NOT NULL,"
                    + "Type TINYINT NOT NULL,"
                    + "DirectoryType TINYINT,"
                    + "Path VARCHAR NOT NULL,"
                    + "ParentPath VARCHAR NOT NULL,"
                    + "Created TIMESTAMP DEFAULT NOW() NOT NULL,"
                    + "LastPlayed TIMESTAMP,"
                    + "LastScanned TIMESTAMP NOT NULL,"
                    + "Excluded BOOLEAN DEFAULT 0 NOT NULL,"
                    + "Format INT,"
                    + "Size BIGINT,"
                    + "Duration DOUBLE,"
                    + "Bitrate INT,"
                    + "Title VARCHAR NOT NULL,"
                    + "Artist VARCHAR,"
                    + "AlbumArtist VARCHAR,"
                    + "Album VARCHAR,"
                    + "Year SMALLINT,"
                    + "DiscNumber SMALLINT,"
                    + "DiscSubtitle VARCHAR,"
                    + "TrackNumber SMALLINT,"
                    + "Genre VARCHAR,"
                    + "Rating REAL,"
                    + "Tagline VARCHAR,"
                    + "Description VARCHAR,"
                    + "Certificate VARCHAR(10),"
                    + "Collection VARCHAR,"
                    + "PRIMARY KEY (ID))");
            
            // Video Streams
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS VideoStream ("
                    + "MEID UUID NOT NULL,"
                    + "SID INT NOT NULL,"
                    + "Title VARCHAR,"
                    + "Codec INT,"
                    + "Width INT,"
                    + "Height INT,"
                    + "Interlaced BOOLEAN DEFAULT 0,"
                    + "FPS DOUBLE,"
                    + "Bitrate INT,"
                    + "MaxBitrate INT,"
                    + "BPS INT,"
                    + "Language VARCHAR,"
                    + "Default BOOLEAN DEFAULT 0 NOT NULL,"
                    + "Forced BOOLEAN DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (MEID,SID),"
                    + "FOREIGN KEY (MEID) REFERENCES MediaElement (ID) ON DELETE CASCADE)");
            
            // Audio Streams
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS AudioStream ("
                    + "MEID UUID NOT NULL,"
                    + "SID INT NOT NULL,"
                    + "Title VARCHAR,"
                    + "Codec INT,"
                    + "SampleRate INT,"
                    + "Channels INT,"
                    + "Bitrate INT,"
                    + "BPS INT,"
                    + "Language VARCHAR,"
                    + "Default BOOLEAN DEFAULT 0 NOT NULL,"
                    + "Forced BOOLEAN DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (MEID,SID),"
                    + "FOREIGN KEY (MEID) REFERENCES MediaElement (ID) ON DELETE CASCADE)");
            
            // Subtitle Streams
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS SubtitleStream ("
                    + "MEID UUID NOT NULL,"
                    + "SID INT NOT NULL,"
                    + "Title VARCHAR,"
                    + "Codec INT,"
                    + "Language VARCHAR,"
                    + "Default BOOLEAN DEFAULT 0 NOT NULL,"
                    + "Forced BOOLEAN DEFAULT 0 NOT NULL,"
                    + "PRIMARY KEY (MEID,SID),"
                    + "FOREIGN KEY (MEID) REFERENCES MediaElement (ID) ON DELETE CASCADE)");
            
            // Playlists
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS Playlist ("
                    + "ID UUID NOT NULL,"
                    + "Name VARCHAR NOT NULL,"
                    + "Description VARCHAR,"
                    + "Username VARCHAR(50),"
                    + "Path VARCHAR,"
                    + "ParentPath VARCHAR,"
                    + "LastScanned TIMESTAMP,"
                    + "PRIMARY KEY (ID))");
            
            // Playlist Contents
            getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS PlaylistContent ("
                    + "PID UUID NOT NULL,"
                    + "MEID UUID NOT NULL,"
                    + "PRIMARY KEY (PID,MEID),"
                    + "FOREIGN KEY (MEID) REFERENCES MediaElement (ID) ON DELETE CASCADE,"
                    + "FOREIGN KEY (PID) REFERENCES Playlist (ID) ON DELETE CASCADE)");
                    
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS PathIndex on MediaElement(Path)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS ParentPathIndex on MediaElement(ParentPath)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS TitleIndex on MediaElement(Title)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS ArtistIndex on MediaElement(Artist)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS AlbumArtistIndex on MediaElement(AlbumArtist)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS AlbumIndex on MediaElement(Album)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS GenreIndex on MediaElement(Genre)");
            getJdbcTemplate().execute("CREATE INDEX IF NOT EXISTS PlaylistIndex on Playlist(Name)");
        } catch (DataAccessException x) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error creating database.", x);
        }
    }
    
    @Override
    public void upgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Upgrading database from version " + oldVersion + " to " + newVersion, null);
    
        if(newVersion == 2 || newVersion == 4 || newVersion == 5) {
            getJdbcTemplate().execute("DROP TABLE IF EXISTS MediaElement");
        }
        
        if(newVersion == 4 || newVersion == 5) {
            getJdbcTemplate().execute("DROP TABLE IF EXISTS PlaylistContent");
        }
        
        if(newVersion == 5) {
            getJdbcTemplate().execute("DROP TABLE IF EXISTS VideoStream");
            getJdbcTemplate().execute("DROP TABLE IF EXISTS AudioStream");
            getJdbcTemplate().execute("DROP TABLE IF EXISTS SubtitleStream");
        }
        
        create();
    }
    
    @Override
    public void downgrade(int oldVersion, int newVersion) {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Downgrading database from version " + oldVersion + " to " + newVersion, null);
        
        // Delete table and re-create
        getJdbcTemplate().execute("DROP TABLE IF EXISTS MediaElement");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS VideoStream");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS AudioStream");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS SubtitleStream");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS Playlist");
        getJdbcTemplate().execute("DROP TABLE IF EXISTS PlaylistContent");
        
        create();
    }
}
