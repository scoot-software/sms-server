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
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.MediaDatabase;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.service.LogService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class MediaDao {
    
    @Autowired
    private MediaDatabase mediaDatabase;
    
    private static final String CLASS_NAME = "MediaDao";
    
    //
    // Media Elements
    //
    
    public boolean createMediaElement(MediaElement mediaElement)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("INSERT INTO MediaElement (Type,DirectoryType,Path,ParentPath,LastScanned,Excluded,Format,Size,Duration,Bitrate,VideoWidth,VideoHeight,VideoCodec,AudioCodec,AudioSampleRate,AudioConfiguration,AudioLanguage,SubtitleLanguage,SubtitleFormat,SubtitleForced,Title,Artist,AlbumArtist,Album,Year,DiscNumber,DiscSubtitle,TrackNumber,Genre,Rating,Tagline,Description,Certificate,Collection) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", 
                                new Object[] {mediaElement.getType(),
                                              mediaElement.getDirectoryType(),
                                              mediaElement.getPath(),
                                              mediaElement.getParentPath(),
                                              mediaElement.getLastScanned(),
                                              mediaElement.getExcluded(),
                                              mediaElement.getFormat(),
                                              mediaElement.getSize(),
                                              mediaElement.getDuration(),
                                              mediaElement.getBitrate(),
                                              mediaElement.getVideoWidth(),
                                              mediaElement.getVideoHeight(),
                                              mediaElement.getVideoCodec(),
                                              mediaElement.getAudioCodec(),
                                              mediaElement.getAudioSampleRate(),
                                              mediaElement.getAudioConfiguration(),
                                              mediaElement.getAudioLanguage(),
                                              mediaElement.getSubtitleLanguage(),
                                              mediaElement.getSubtitleFormat(),
                                              mediaElement.getSubtitleForced(),
                                              mediaElement.getTitle(),
                                              mediaElement.getArtist(),
                                              mediaElement.getAlbumArtist(),
                                              mediaElement.getAlbum(),
                                              mediaElement.getYear(),
                                              mediaElement.getDiscNumber(),
                                              mediaElement.getDiscSubtitle(),
                                              mediaElement.getTrackNumber(),
                                              mediaElement.getGenre(),
                                              mediaElement.getRating(),
                                              mediaElement.getTagline(),
                                              mediaElement.getDescription(),
                                              mediaElement.getCertificate(),
                                              mediaElement.getCollection()});
        }
        catch (InvalidResultSetAccessException e) 
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create media element for file: " + mediaElement.getPath(), e);
            return false;
        } 
        catch (DataAccessException e)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create media element for file: " + mediaElement.getPath(), e);
            return false;
        }
        
        return true;
    }
    
    public boolean removeMediaElement(Long id)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement WHERE ID=?", id);
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public boolean removeAllMediaElements()
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement");
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public void removeDeletedMediaElements(String path, Timestamp lastScanned) 
    {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement WHERE ParentPath LIKE ? AND LastScanned != ?", new Object[] {path + "%",lastScanned});
    }
    
    public boolean updateMediaElementByID(MediaElement mediaElement)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,VideoWidth=?,VideoHeight=?,VideoCodec=?,AudioCodec=?,AudioSampleRate=?,AudioConfiguration=?,AudioLanguage=?,SubtitleLanguage=?,SubtitleFormat=?,SubtitleForced=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE ID=?", 
                                new Object[] {mediaElement.getDirectoryType(),
                                              mediaElement.getLastScanned(),
                                              mediaElement.getExcluded(),
                                              mediaElement.getSize(),
                                              mediaElement.getDuration(),
                                              mediaElement.getBitrate(),
                                              mediaElement.getVideoWidth(),
                                              mediaElement.getVideoHeight(),
                                              mediaElement.getVideoCodec(),
                                              mediaElement.getAudioCodec(),
                                              mediaElement.getAudioSampleRate(),
                                              mediaElement.getAudioConfiguration(),
                                              mediaElement.getAudioLanguage(),
                                              mediaElement.getSubtitleLanguage(),
                                              mediaElement.getSubtitleFormat(),
                                              mediaElement.getSubtitleForced(),
                                              mediaElement.getTitle(),
                                              mediaElement.getArtist(),
                                              mediaElement.getAlbumArtist(),
                                              mediaElement.getAlbum(),
                                              mediaElement.getYear(),
                                              mediaElement.getDiscNumber(),
                                              mediaElement.getDiscSubtitle(),
                                              mediaElement.getTrackNumber(),
                                              mediaElement.getGenre(),
                                              mediaElement.getRating(),
                                              mediaElement.getTagline(),
                                              mediaElement.getDescription(),
                                              mediaElement.getCertificate(),
                                              mediaElement.getCollection(),
                                              mediaElement.getID()});
        }
        catch (InvalidResultSetAccessException e) 
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element for file: " + mediaElement.getPath(), e);
            return false;
        } 
        catch (DataAccessException e)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element for file: " + mediaElement.getPath(), e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateMediaElementByPath(MediaElement mediaElement)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,VideoWidth=?,VideoHeight=?,VideoCodec=?,AudioCodec=?,AudioSampleRate=?,AudioConfiguration=?,AudioLanguage=?,SubtitleLanguage=?,SubtitleFormat=?,SubtitleForced=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE PATH=?", 
                                new Object[] {mediaElement.getDirectoryType(),
                                              mediaElement.getLastScanned(),
                                              mediaElement.getExcluded(),
                                              mediaElement.getSize(),
                                              mediaElement.getDuration(),
                                              mediaElement.getBitrate(),
                                              mediaElement.getVideoWidth(),
                                              mediaElement.getVideoHeight(),
                                              mediaElement.getVideoCodec(),
                                              mediaElement.getAudioCodec(),
                                              mediaElement.getAudioSampleRate(),
                                              mediaElement.getAudioConfiguration(),
                                              mediaElement.getAudioLanguage(),
                                              mediaElement.getSubtitleLanguage(),
                                              mediaElement.getSubtitleFormat(),
                                              mediaElement.getSubtitleForced(),
                                              mediaElement.getTitle(),
                                              mediaElement.getArtist(),
                                              mediaElement.getAlbumArtist(),
                                              mediaElement.getAlbum(),
                                              mediaElement.getYear(),
                                              mediaElement.getDiscNumber(),
                                              mediaElement.getDiscSubtitle(),
                                              mediaElement.getTrackNumber(),
                                              mediaElement.getGenre(),
                                              mediaElement.getRating(),
                                              mediaElement.getTagline(),
                                              mediaElement.getDescription(),
                                              mediaElement.getCertificate(),
                                              mediaElement.getCollection(),
                                              mediaElement.getPath()});
        }
        catch (InvalidResultSetAccessException e) 
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element for file: " + mediaElement.getPath(), e);
            return false;
        } 
        catch (DataAccessException e)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element for file: " + mediaElement.getPath(), e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastPlayed(Long id)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET LastPlayed=NOW() WHERE ID=?", 
                                new Object[] {id});
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastScanned(Long id, Timestamp lastScanned)
    {
        try
        {
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET LastScanned=? WHERE ID=?", 
                                new Object[] {lastScanned, id});
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public List<MediaElement> getMediaElements()
    {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement", new MediaElementMapper());
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public MediaElement getMediaElementByID(Long id)
    {
        MediaElement mediaElement = null;

        try
        {
            if(id != null)
            {

                List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ID=?", new MediaElementMapper(), new Object[] {id});

                if(mediaElements != null)
                {
                    if(mediaElements.size() > 0)
                    {
                        mediaElement = mediaElements.get(0);
                    }
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        
        return mediaElement;
    }
    
    public MediaElement getMediaElementByPath(String path)
    {
        MediaElement mediaElement = null;
        
        try
        {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Path=?", new MediaElementMapper(), new Object[] {path});

            if(mediaElements != null)
            {
                if(mediaElements.size() > 0)
                {
                    mediaElement = mediaElements.get(0);
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        
        return mediaElement;
    }
    
    public List<MediaElement> getMediaElementsByParentPath(String path) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? ORDER BY Type,Album,DiscNumber,TrackNumber,Year,Title", new MediaElementMapper(), new Object[] {path});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getAlphabeticalMediaElementsByParentPath(String path) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? ORDER BY Type,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {path});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Title LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Created DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getArtists() {
        try {
            List<String> artists = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Artist FROM MediaElement WHERE Artist IS NOT NULL ORDER BY Artist", String.class);
            return artists;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getAlbumArtists() {
        try {
            List<String> albumArtists = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT AlbumArtist FROM MediaElement WHERE AlbumArtist IS NOT NULL ORDER BY AlbumArtist", String.class);
            return albumArtists;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getAlbums() {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE Album IS NOT NULL ORDER BY Album", String.class);
            return albums;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getAlbumsByArtist(String artist) {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE Artist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {artist}, String.class);
            return albums;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getAlbumsByAlbumArtist(String albumArtist) {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE AlbumArtist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {albumArtist}, String.class);
            return albums;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByArtistAndAlbum(String artist, String album) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Artist=? AND Album=? ORDER BY DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, artist, album});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByAlbumArtistAndAlbum(String albumArtist, String album) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND AlbumArtist=? AND Album=? ORDER BY DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, albumArtist, album});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByArtist(String artist) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Artist=? ORDER BY Year,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, artist});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByAlbumArtist(String albumArtist) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND AlbumArtist=? ORDER BY Year,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, albumArtist});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<String> getCollections() {
        try {
            List<String> collections = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Collection FROM MediaElement WHERE Collection IS NOT NULL ORDER BY Collection", String.class);
            return collections;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByCollection(String collection) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Collection=? ORDER BY Year,Title", new MediaElementMapper(), new Object[] {MediaElementType.VIDEO, collection});
            return mediaElements;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    private static final class MediaElementMapper implements RowMapper
    {
        @Override
        public MediaElement mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            MediaElement mediaElement = new MediaElement();
            mediaElement.setID(rs.getLong("ID"));
            mediaElement.setType(rs.getByte("Type"));
            mediaElement.setDirectoryType(rs.getByte("DirectoryType"));
            mediaElement.setPath(rs.getString("Path"));
            mediaElement.setParentPath(rs.getString("ParentPath"));
            mediaElement.setCreated(rs.getTimestamp("Created"));
            mediaElement.setLastPlayed(rs.getTimestamp("LastPlayed"));
            mediaElement.setLastScanned(rs.getTimestamp("LastScanned"));
            mediaElement.setExcluded(rs.getBoolean("Excluded"));
            mediaElement.setFormat(rs.getString("Format"));
            mediaElement.setSize(rs.getLong("Size"));
            mediaElement.setDuration(rs.getInt("Duration"));
            mediaElement.setBitrate(rs.getInt("Bitrate"));
            mediaElement.setVideoWidth(rs.getShort("VideoWidth"));
            mediaElement.setVideoHeight(rs.getShort("VideoHeight"));
            mediaElement.setVideoCodec(rs.getString("VideoCodec"));
            mediaElement.setAudioCodec(rs.getString("AudioCodec"));
            mediaElement.setAudioSampleRate(rs.getString("AudioSampleRate"));
            mediaElement.setAudioConfiguration(rs.getString("AudioConfiguration"));
            mediaElement.setAudioLanguage(rs.getString("AudioLanguage"));
            mediaElement.setSubtitleLanguage(rs.getString("SubtitleLanguage"));
            mediaElement.setSubtitleFormat(rs.getString("SubtitleFormat"));
            mediaElement.setSubtitleForced(rs.getString("SubtitleForced"));
            mediaElement.setTitle(rs.getString("Title"));
            mediaElement.setArtist(rs.getString("Artist"));
            mediaElement.setAlbumArtist(rs.getString("AlbumArtist"));
            mediaElement.setAlbum(rs.getString("Album"));
            mediaElement.setYear(rs.getShort("Year"));
            mediaElement.setDiscNumber(rs.getByte("DiscNumber"));
            mediaElement.setDiscSubtitle(rs.getString("DiscSubtitle"));
            mediaElement.setTrackNumber(rs.getShort("TrackNumber"));
            mediaElement.setGenre(rs.getString("Genre"));
            mediaElement.setRating(rs.getFloat("Rating"));
            mediaElement.setTagline(rs.getString("Tagline"));
            mediaElement.setDescription(rs.getString("Description"));
            mediaElement.setCertificate(rs.getString("Certificate"));
            mediaElement.setCollection(rs.getString("Collection"));
            
            return mediaElement;
        }
    }
}
