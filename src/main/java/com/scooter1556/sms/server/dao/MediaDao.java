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
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.service.LogService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class MediaDao {
    
    private static final String CLASS_NAME = "MediaDao";
    
    @Autowired
    private MediaDatabase mediaDatabase;
        
    //
    // Media Elements
    //
    
    public boolean createMediaElements(final List<MediaElement> mediaElements) {
        String sql = "INSERT INTO MediaElement (Type,DirectoryType,Path,ParentPath,LastScanned,Excluded,Format,Size,Duration,Bitrate,VideoWidth,VideoHeight,VideoCodec,AudioName,AudioCodec,AudioSampleRate,AudioConfiguration,AudioLanguage,SubtitleName,SubtitleLanguage,SubtitleFormat,SubtitleForced,Title,Artist,AlbumArtist,Album,Year,DiscNumber,DiscSubtitle,TrackNumber,Genre,Rating,Tagline,Description,Certificate,Collection) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setByte(1, mediaElement.getType());
                    ps.setByte(2, mediaElement.getDirectoryType());
                    ps.setString(3, mediaElement.getPath());
                    ps.setString(4, mediaElement.getParentPath());
                    ps.setTimestamp(5, mediaElement.getLastScanned());
                    ps.setBoolean(6, mediaElement.getExcluded());
                    ps.setString(7, mediaElement.getFormat());
                    ps.setLong(8, mediaElement.getSize());
                    ps.setInt(9, mediaElement.getDuration());
                    ps.setInt(10, mediaElement.getBitrate());
                    ps.setShort(11, mediaElement.getVideoWidth());
                    ps.setShort(12, mediaElement.getVideoHeight());
                    ps.setString(13, mediaElement.getVideoCodec());
                    ps.setString(14, mediaElement.getAudioName());
                    ps.setString(15, mediaElement.getAudioCodec());
                    ps.setString(16, mediaElement.getAudioSampleRate());
                    ps.setString(17, mediaElement.getAudioConfiguration());
                    ps.setString(18, mediaElement.getAudioLanguage());
                    ps.setString(19, mediaElement.getSubtitleName());
                    ps.setString(20, mediaElement.getSubtitleLanguage());
                    ps.setString(21, mediaElement.getSubtitleFormat());
                    ps.setString(22, mediaElement.getSubtitleForced());
                    ps.setString(23, mediaElement.getTitle());
                    ps.setString(24, mediaElement.getArtist());
                    ps.setString(25, mediaElement.getAlbumArtist());
                    ps.setString(26, mediaElement.getAlbum());
                    ps.setShort(27, mediaElement.getYear());
                    ps.setByte(28, mediaElement.getDiscNumber());
                    ps.setString(29, mediaElement.getDiscSubtitle());
                    ps.setShort(30,mediaElement.getTrackNumber());
                    ps.setString(31, mediaElement.getGenre());
                    ps.setFloat(32, mediaElement.getRating());
                    ps.setString(33, mediaElement.getTagline());
                    ps.setString(34, mediaElement.getDescription());
                    ps.setString(35, mediaElement.getCertificate());
                    ps.setString(36, mediaElement.getCollection());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create media element for file", e);
            return false;
        }
        
        return true;
    }
    
    public boolean removeMediaElement(Long id) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement WHERE ID=?", id);
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean removeAllMediaElements() {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement");
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public void removeDeletedMediaElements(String path, Timestamp lastScanned) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement WHERE ParentPath LIKE ? AND LastScanned != ?", new Object[] {path + "%",lastScanned});
    }
    
    public boolean updateMediaElementsByID(final List<MediaElement> mediaElements) {
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,VideoWidth=?,VideoHeight=?,VideoCodec=?,AudioName=?,AudioCodec=?,AudioSampleRate=?,AudioConfiguration=?,AudioLanguage=?,SubtitleName=?,SubtitleLanguage=?,SubtitleFormat=?,SubtitleForced=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE ID=?";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setByte(1, mediaElement.getDirectoryType());
                    ps.setTimestamp(2, mediaElement.getLastScanned());
                    ps.setBoolean(3, mediaElement.getExcluded());
                    ps.setLong(4, mediaElement.getSize());
                    ps.setInt(5, mediaElement.getDuration());
                    ps.setInt(6, mediaElement.getBitrate());
                    ps.setShort(7, mediaElement.getVideoWidth());
                    ps.setShort(8, mediaElement.getVideoHeight());
                    ps.setString(9, mediaElement.getVideoCodec());
                    ps.setString(10, mediaElement.getAudioName());
                    ps.setString(11, mediaElement.getAudioCodec());
                    ps.setString(12, mediaElement.getAudioSampleRate());
                    ps.setString(13, mediaElement.getAudioConfiguration());
                    ps.setString(14, mediaElement.getAudioLanguage());
                    ps.setString(15, mediaElement.getSubtitleName());
                    ps.setString(16, mediaElement.getSubtitleLanguage());
                    ps.setString(17, mediaElement.getSubtitleFormat());
                    ps.setString(18, mediaElement.getSubtitleForced());
                    ps.setString(19, mediaElement.getTitle());
                    ps.setString(20, mediaElement.getArtist());
                    ps.setString(21, mediaElement.getAlbumArtist());
                    ps.setString(22, mediaElement.getAlbum());
                    ps.setShort(23, mediaElement.getYear());
                    ps.setByte(24, mediaElement.getDiscNumber());
                    ps.setString(25, mediaElement.getDiscSubtitle());
                    ps.setShort(26,mediaElement.getTrackNumber());
                    ps.setString(27, mediaElement.getGenre());
                    ps.setFloat(28, mediaElement.getRating());
                    ps.setString(29, mediaElement.getTagline());
                    ps.setString(30, mediaElement.getDescription());
                    ps.setString(31, mediaElement.getCertificate());
                    ps.setString(32, mediaElement.getCollection());
                    ps.setLong(33, mediaElement.getID());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create media element for file", e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateMediaElementsByPath(final List<MediaElement> mediaElements) {
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,VideoWidth=?,VideoHeight=?,VideoCodec=?,AudioName=?,AudioCodec=?,AudioSampleRate=?,AudioConfiguration=?,AudioLanguage=?,SubtitleName=?,SubtitleLanguage=?,SubtitleFormat=?,SubtitleForced=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE PATH=?";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setByte(1, mediaElement.getDirectoryType());
                    ps.setTimestamp(2, mediaElement.getLastScanned());
                    ps.setBoolean(3, mediaElement.getExcluded());
                    ps.setLong(4, mediaElement.getSize());
                    ps.setInt(5, mediaElement.getDuration());
                    ps.setInt(6, mediaElement.getBitrate());
                    ps.setShort(7, mediaElement.getVideoWidth());
                    ps.setShort(8, mediaElement.getVideoHeight());
                    ps.setString(9, mediaElement.getVideoCodec());
                    ps.setString(10, mediaElement.getAudioName());
                    ps.setString(11, mediaElement.getAudioCodec());
                    ps.setString(12, mediaElement.getAudioSampleRate());
                    ps.setString(13, mediaElement.getAudioConfiguration());
                    ps.setString(14, mediaElement.getAudioLanguage());
                    ps.setString(15, mediaElement.getSubtitleName());
                    ps.setString(16, mediaElement.getSubtitleLanguage());
                    ps.setString(17, mediaElement.getSubtitleFormat());
                    ps.setString(18, mediaElement.getSubtitleForced());
                    ps.setString(19, mediaElement.getTitle());
                    ps.setString(20, mediaElement.getArtist());
                    ps.setString(21, mediaElement.getAlbumArtist());
                    ps.setString(22, mediaElement.getAlbum());
                    ps.setShort(23, mediaElement.getYear());
                    ps.setByte(24, mediaElement.getDiscNumber());
                    ps.setString(25, mediaElement.getDiscSubtitle());
                    ps.setShort(26,mediaElement.getTrackNumber());
                    ps.setString(27, mediaElement.getGenre());
                    ps.setFloat(28, mediaElement.getRating());
                    ps.setString(29, mediaElement.getTagline());
                    ps.setString(30, mediaElement.getDescription());
                    ps.setString(31, mediaElement.getCertificate());
                    ps.setString(32, mediaElement.getCollection());
                    ps.setString(33, mediaElement.getPath());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create media element for file", e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastPlayed(Long id) {
        try{
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET LastPlayed=NOW() WHERE ID=?", 
                                new Object[] {id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastScanned(Long id, Timestamp lastScanned) {
        try {
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET LastScanned=? WHERE ID=?", 
                                new Object[] {lastScanned, id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public List<MediaElement> getMediaElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement", new MediaElementMapper());
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public MediaElement getMediaElementByID(Long id) {
        MediaElement mediaElement = null;

        try {
            if(id != null) {
                List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ID=?", new MediaElementMapper(), new Object[] {id});

                if(mediaElements != null) {
                    if(mediaElements.size() > 0) {
                        mediaElement = mediaElements.get(0);
                    }
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return mediaElement;
    }
    
    public MediaElement getMediaElementByPath(String path) {
        MediaElement mediaElement = null;
        
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Path=?", new MediaElementMapper(), new Object[] {path});

            if(mediaElements != null) {
                if(mediaElements.size() > 0) {
                    mediaElement = mediaElements.get(0);
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return mediaElement;
    }
    
    public List<MediaElement> getMediaElementsByParentPath(String path, Byte type) {
        try {
            List<MediaElement> mediaElements;
                    
            if(type == null) {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? ORDER BY Type,Album,DiscNumber,TrackNumber,Year,Title", new MediaElementMapper(), new Object[] {path});
            } else {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? AND Type=? ORDER BY Type,Album,DiscNumber,TrackNumber,Year,Title", new MediaElementMapper(), new Object[] {path, type});
            }
            
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByName(String name, byte type) {
        try {
            List<MediaElement> mediaElements;
            mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Path LIKE ?", new MediaElementMapper(), new Object[] {type,"%"+name});
            
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRandomMediaElementsByParentPath(String path) {        
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? ORDER BY RAND()", new MediaElementMapper(), new Object[] {path});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRandomMediaElements(int limit, Byte type) {
        List<MediaElement> mediaElements;

        try {
            if(type == null) {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE NOT Excluded ORDER BY RAND() DESC LIMIT ?", new MediaElementMapper(), new Object[] {limit});
            } else {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY RAND() DESC LIMIT ?", new MediaElementMapper(), new Object[] {type, limit});
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return mediaElements;
    }
    
    public List<MediaElement> getAlphabeticalMediaElementsByParentPath(String path) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ParentPath=? ORDER BY Type,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {path});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Title LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Created DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedVideoDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.VIDEO, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedVideoDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND NOT Excluded ORDER BY Created DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.VIDEO, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedAudioDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.AUDIO, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedAudioDirectoryElements(int limit) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND NOT Excluded ORDER BY Created DESC LIMIT ?", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.AUDIO, limit});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getArtists() {
        try {
            List<String> artists = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Artist FROM MediaElement WHERE Artist IS NOT NULL ORDER BY Artist", String.class);
            return artists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getAlbumArtists() {
        try {
            List<String> albumArtists = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT AlbumArtist FROM MediaElement WHERE AlbumArtist IS NOT NULL ORDER BY AlbumArtist", String.class);
            return albumArtists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getAlbums() {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE Album IS NOT NULL ORDER BY Album", String.class);
            return albums;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getAlbumsByArtist(String artist) {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE Artist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {artist}, String.class);
            return albums;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getAlbumsByAlbumArtist(String albumArtist) {
        try {
            List<String> albums = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Album FROM MediaElement WHERE AlbumArtist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {albumArtist}, String.class);
            return albums;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByArtistAndAlbum(String artist, String album) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Artist=? AND Album=? ORDER BY DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, artist, album});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByAlbumArtistAndAlbum(String albumArtist, String album) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND AlbumArtist=? AND Album=? ORDER BY DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, albumArtist, album});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByArtist(String artist) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Artist=? ORDER BY Year,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, artist});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByAlbumArtist(String albumArtist) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND AlbumArtist=? ORDER BY Year,Album,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, albumArtist});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByAlbum(String album) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND Album=? ORDER BY Year,DiscNumber,TrackNumber,Title", new MediaElementMapper(), new Object[] {MediaElementType.AUDIO, album});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<String> getCollections() {
        try {
            List<String> collections = mediaDatabase.getJdbcTemplate().queryForList("SELECT DISTINCT Collection FROM MediaElement WHERE Collection IS NOT NULL ORDER BY Collection", String.class);
            return collections;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getMediaElementsByCollection(String collection) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND NOT Excluded AND Collection=? ORDER BY Year,Title", new MediaElementMapper(), new Object[] {MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.VIDEO, collection});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    private static final class MediaElementMapper implements RowMapper {
        @Override
        public MediaElement mapRow(ResultSet rs, int rowNum) throws SQLException {
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
            mediaElement.setAudioName(rs.getString("AudioName"));
            mediaElement.setAudioCodec(rs.getString("AudioCodec"));
            mediaElement.setAudioSampleRate(rs.getString("AudioSampleRate"));
            mediaElement.setAudioConfiguration(rs.getString("AudioConfiguration"));
            mediaElement.setAudioLanguage(rs.getString("AudioLanguage"));
            mediaElement.setSubtitleName(rs.getString("SubtitleName"));
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
    
    
    //
    // Playlists
    //
    
    public boolean createPlaylist(Playlist playlist) {
        try {
            mediaDatabase.getJdbcTemplate().update("INSERT INTO Playlist (ID, Name, Description, Username, Path, ParentPath, LastScanned) " +
                                "VALUES (?,?,?,?,?,?,?)", new Object[] {
                                    playlist.getID(),
                                    playlist.getName(),
                                    playlist.getDescription(),
                                    playlist.getUsername(),
                                    playlist.getPath(),
                                    playlist.getParentPath(),
                                    playlist.getLastScanned()
                                });
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean removePlaylist(UUID id) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM Playlist WHERE ID=?", id);
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public void removeDeletedPlaylists(String path, Timestamp lastScanned) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM Playlist WHERE ParentPath LIKE ? AND LastScanned != ?", new Object[] {path + "%",lastScanned});
    }
    
    public boolean updatePlaylist(Playlist playlist){
        try{
            mediaDatabase.getJdbcTemplate().update("UPDATE Playlist SET Name=?, Description=? WHERE ID=?", 
                                new Object[] {playlist.getName(), playlist.getDescription(),
                                playlist.getID()});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastScanned(UUID id, Timestamp lastScanned) {
        try {
            mediaDatabase.getJdbcTemplate().update("UPDATE Playlist SET LastScanned=? WHERE ID=?", 
                                new Object[] {lastScanned, id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public List<Playlist> getPlaylists() {
        try {
            List<Playlist> playlists = mediaDatabase.getJdbcTemplate().query("SELECT * FROM Playlist", new PlaylistMapper());
            return playlists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<Playlist> getPlaylistsByUsername(String username) {
        try {
            List<Playlist> playlists = mediaDatabase.getJdbcTemplate().query("SELECT * FROM Playlist WHERE Username IS NULL OR Username=?", new PlaylistMapper(), new Object[] {username});
            return playlists;
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to get playlists for user '" + username + "'", e);
            return null;
        }
    }
    
    public Playlist getPlaylistByID(UUID id) {
        Playlist playlist = null;

        try {
            List<Playlist> playlists = mediaDatabase.getJdbcTemplate().query("SELECT * FROM Playlist WHERE ID=?", new PlaylistMapper(), new Object[] {id});

            if(playlists != null) {
                if(playlists.size() > 0) {
                    playlist = playlists.get(0);
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return playlist;
    }
    
    public Playlist getPlaylistByPath(String path) {
        Playlist playlist = null;
        
        try {
            List<Playlist> playlists = mediaDatabase.getJdbcTemplate().query("SELECT * FROM Playlist WHERE Path=?", new PlaylistMapper(), new Object[] {path});

            if(playlists != null) {
                if(playlists.size() > 0) {
                    playlist = playlists.get(0);
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return playlist;
    }
    
    public List<Playlist> getPlaylistsByParentPath(String path) {
        try {
            List<Playlist> playlists;      
            playlists = mediaDatabase.getJdbcTemplate().query("SELECT * FROM Playlist WHERE ParentPath=?", new PlaylistMapper(), new Object[] {path});
            
            return playlists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public boolean setPlaylistContentFromIds(final UUID id, final List<Long> mediaElementIds) {
        String sql = "INSERT INTO PlaylistContent (PID,MEID) " +
                                "VALUES (?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Long mediaElementId = mediaElementIds.get(i);
                    ps.setString(1, id.toString());
                    ps.setLong(2, mediaElementId);
                }

                @Override
                public int getBatchSize() {
                    return mediaElementIds.size();
                }
            });
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean setPlaylistContent(final UUID id, final List<MediaElement> mediaElements) {
        String sql = "INSERT INTO PlaylistContent (PID,MEID) " +
                                "VALUES (?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setString(1, id.toString());
                    ps.setLong(2, mediaElement.getID());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public List<MediaElement> getPlaylistContent(UUID id) {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE ID IN (SELECT MEID FROM PlaylistContent WHERE PID=?)", new MediaElementMapper(), new Object[] {id});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public boolean removePlaylistContent(UUID id) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM PlaylistContent WHERE PID=?", id);
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    private static final class PlaylistMapper implements RowMapper {
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
}
