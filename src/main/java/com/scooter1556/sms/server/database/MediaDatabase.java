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

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.exception.DatabaseException;
import com.scooter1556.sms.server.service.LogService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class MediaDatabase extends Database {
    private static final String CLASS_NAME = "MediaDatabase";
    
    public static final String DB_NAME = "Media";
    public static final int DB_VERSION = 6;
    
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
                    + "ReplaygainTrack REAL,"
                    + "ReplaygainAlbum REAL,"
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
                    + "GOP INT,"
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
    
    public static final class MediaElementMapper implements RowMapper {
        @Override
        public MediaElement mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaElement mediaElement = new MediaElement();
            mediaElement.setID((UUID)rs.getObject("ID"));
            mediaElement.setType(rs.getByte("Type"));
            mediaElement.setDirectoryType(rs.getByte("DirectoryType"));
            mediaElement.setPath(rs.getString("Path"));
            mediaElement.setParentPath(rs.getString("ParentPath"));
            mediaElement.setCreated(rs.getTimestamp("Created"));
            mediaElement.setLastPlayed(rs.getTimestamp("LastPlayed"));
            mediaElement.setLastScanned(rs.getTimestamp("LastScanned"));
            mediaElement.setExcluded(rs.getBoolean("Excluded"));
            mediaElement.setFormat(rs.getInt("Format"));
            mediaElement.setSize(rs.getLong("Size"));
            mediaElement.setDuration(rs.getDouble("Duration"));
            mediaElement.setBitrate(rs.getInt("Bitrate"));
            mediaElement.setTitle(rs.getString("Title"));
            mediaElement.setArtist(rs.getString("Artist"));
            mediaElement.setAlbumArtist(rs.getString("AlbumArtist"));
            mediaElement.setAlbum(rs.getString("Album"));
            mediaElement.setYear(rs.getShort("Year"));
            mediaElement.setDiscNumber(rs.getShort("DiscNumber"));
            mediaElement.setDiscSubtitle(rs.getString("DiscSubtitle"));
            mediaElement.setTrackNumber(rs.getShort("TrackNumber"));
            mediaElement.setGenre(rs.getString("Genre"));
            mediaElement.setRating(rs.getFloat("Rating"));
            mediaElement.setTagline(rs.getString("Tagline"));
            mediaElement.setDescription(rs.getString("Description"));
            mediaElement.setCertificate(rs.getString("Certificate"));
            mediaElement.setCollection(rs.getString("Collection"));
            mediaElement.setReplaygainTrack(rs.getFloat("ReplaygainTrack"));
            mediaElement.setReplaygainAlbum(rs.getFloat("ReplaygainAlbum"));
            
            return mediaElement;
        }
    }
    
    public static final class VideoStreamMapper implements RowMapper {
        @Override
        public MediaElement.VideoStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaElement.VideoStream videoStream = new MediaElement.VideoStream();
            videoStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            videoStream.setStreamId(rs.getInt("SID"));
            videoStream.setTitle(rs.getString("Title"));
            videoStream.setCodec(rs.getInt("Codec"));
            videoStream.setWidth(rs.getInt("Width"));
            videoStream.setHeight(rs.getInt("Height"));
            videoStream.setInterlaced(rs.getBoolean("Interlaced"));
            videoStream.setFPS(rs.getDouble("FPS"));
            videoStream.setBitrate(rs.getInt("Bitrate"));
            videoStream.setMaxBitrate(rs.getInt("MaxBitrate"));
            videoStream.setBPS(rs.getInt("BPS"));
            videoStream.setGOPSize(rs.getInt("GOP"));
            videoStream.setLanguage(rs.getString("Language"));
            videoStream.setDefault(rs.getBoolean("Default"));
            videoStream.setForced(rs.getBoolean("Forced"));
            
            return videoStream;
        }
    }
    
    public static final class AudioStreamMapper implements RowMapper {
        @Override
        public MediaElement.AudioStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaElement.AudioStream audioStream = new MediaElement.AudioStream();
            audioStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            audioStream.setStreamId(rs.getInt("SID"));
            audioStream.setTitle(rs.getString("Title"));
            audioStream.setCodec(rs.getInt("Codec"));
            audioStream.setSampleRate(rs.getInt("SampleRate"));
            audioStream.setChannels(rs.getInt("Channels"));
            audioStream.setBitrate(rs.getInt("Bitrate"));
            audioStream.setBPS(rs.getInt("BPS"));
            audioStream.setLanguage(rs.getString("Language"));
            audioStream.setDefault(rs.getBoolean("Default"));
            audioStream.setForced(rs.getBoolean("Forced"));
            
            return audioStream;
        }
    }
    
    public static final class SubtitleStreamMapper implements RowMapper {
        @Override
        public MediaElement.SubtitleStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaElement.SubtitleStream subtitleStream = new MediaElement.SubtitleStream();
            subtitleStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            subtitleStream.setStreamId(rs.getInt("SID"));
            subtitleStream.setTitle(rs.getString("Title"));
            subtitleStream.setCodec(rs.getInt("Codec"));
            subtitleStream.setLanguage(rs.getString("Language"));
            subtitleStream.setDefault(rs.getBoolean("Default"));
            subtitleStream.setForced(rs.getBoolean("Forced"));
            
            return subtitleStream;
        }
    }
    
    public static final class PlaylistMapper implements RowMapper {
        @Override
        public Playlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            Playlist playlist = new Playlist();
            playlist.setID(UUID.fromString(rs.getString("ID")));
            playlist.setName(rs.getString("Name"));
            playlist.setDescription(rs.getString("Description"));
            playlist.setUsername(rs.getString("Username"));
            playlist.setPath(rs.getString("Path"));
            playlist.setParentPath(rs.getString("ParentPath"));
            playlist.setLastScanned(rs.getTimestamp("LastScanned"));
            
            return playlist;
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
        
        if(newVersion == 6) {
            getJdbcTemplate().update("ALTER TABLE MediaElement ADD ReplaygainTrack REAL");
            getJdbcTemplate().update("ALTER TABLE MediaElement ADD ReplaygainAlbum REAL");
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
