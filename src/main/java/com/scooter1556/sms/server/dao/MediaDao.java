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
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
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
        String sql = "INSERT INTO MediaElement (ID,Type,DirectoryType,Path,ParentPath,LastScanned,Excluded,Format,Size,Duration,Bitrate,Title,Artist,AlbumArtist,Album,Year,DiscNumber,DiscSubtitle,TrackNumber,Genre,Rating,Tagline,Description,Certificate,Collection) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setObject(1, mediaElement.getID());
                    ps.setByte(2, mediaElement.getType());
                    ps.setByte(3, mediaElement.getDirectoryType());
                    ps.setString(4, mediaElement.getPath());
                    ps.setString(5, mediaElement.getParentPath());
                    ps.setTimestamp(6, mediaElement.getLastScanned());
                    ps.setBoolean(7, mediaElement.getExcluded());
                    ps.setString(8, mediaElement.getFormat());
                    ps.setLong(9, mediaElement.getSize());
                    ps.setDouble(10, mediaElement.getDuration());
                    ps.setInt(11, mediaElement.getBitrate());
                    ps.setString(12, mediaElement.getTitle());
                    ps.setString(13, mediaElement.getArtist());
                    ps.setString(14, mediaElement.getAlbumArtist());
                    ps.setString(15, mediaElement.getAlbum());
                    ps.setShort(16, mediaElement.getYear());
                    ps.setShort(17, mediaElement.getDiscNumber());
                    ps.setString(18, mediaElement.getDiscSubtitle());
                    ps.setShort(19,mediaElement.getTrackNumber());
                    ps.setString(20, mediaElement.getGenre());
                    ps.setFloat(21, mediaElement.getRating());
                    ps.setString(22, mediaElement.getTagline());
                    ps.setString(23, mediaElement.getDescription());
                    ps.setString(24, mediaElement.getCertificate());
                    ps.setString(25, mediaElement.getCollection());
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
    
    public boolean removeMediaElement(UUID id) {
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
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE ID=?";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setByte(1, mediaElement.getDirectoryType());
                    ps.setTimestamp(2, mediaElement.getLastScanned());
                    ps.setBoolean(3, mediaElement.getExcluded());
                    ps.setLong(4, mediaElement.getSize());
                    ps.setDouble(5, mediaElement.getDuration());
                    ps.setInt(6, mediaElement.getBitrate());
                    ps.setString(7, mediaElement.getTitle());
                    ps.setString(8, mediaElement.getArtist());
                    ps.setString(9, mediaElement.getAlbumArtist());
                    ps.setString(10, mediaElement.getAlbum());
                    ps.setShort(11, mediaElement.getYear());
                    ps.setShort(12, mediaElement.getDiscNumber());
                    ps.setString(13, mediaElement.getDiscSubtitle());
                    ps.setShort(14,mediaElement.getTrackNumber());
                    ps.setString(15, mediaElement.getGenre());
                    ps.setFloat(16, mediaElement.getRating());
                    ps.setString(17, mediaElement.getTagline());
                    ps.setString(18, mediaElement.getDescription());
                    ps.setString(19, mediaElement.getCertificate());
                    ps.setString(20, mediaElement.getCollection());
                    ps.setObject(21, mediaElement.getID());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element!", e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateMediaElementsByPath(final List<MediaElement> mediaElements) {
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=? WHERE PATH=?";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    MediaElement mediaElement = mediaElements.get(i);
                    ps.setByte(1, mediaElement.getDirectoryType());
                    ps.setTimestamp(2, mediaElement.getLastScanned());
                    ps.setBoolean(3, mediaElement.getExcluded());
                    ps.setLong(4, mediaElement.getSize());
                    ps.setDouble(5, mediaElement.getDuration());
                    ps.setInt(6, mediaElement.getBitrate());
                    ps.setString(7, mediaElement.getTitle());
                    ps.setString(8, mediaElement.getArtist());
                    ps.setString(9, mediaElement.getAlbumArtist());
                    ps.setString(10, mediaElement.getAlbum());
                    ps.setShort(11, mediaElement.getYear());
                    ps.setShort(12, mediaElement.getDiscNumber());
                    ps.setString(13, mediaElement.getDiscSubtitle());
                    ps.setShort(14, mediaElement.getTrackNumber());
                    ps.setString(15, mediaElement.getGenre());
                    ps.setFloat(16, mediaElement.getRating());
                    ps.setString(17, mediaElement.getTagline());
                    ps.setString(18, mediaElement.getDescription());
                    ps.setString(19, mediaElement.getCertificate());
                    ps.setString(20, mediaElement.getCollection());
                    ps.setString(21, mediaElement.getPath());
                }

                @Override
                public int getBatchSize() {
                    return mediaElements.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to update media element!", e);
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastPlayed(UUID id) {
        try{
            mediaDatabase.getJdbcTemplate().update("UPDATE MediaElement SET LastPlayed=NOW() WHERE ID=?", 
                                new Object[] {id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean updateLastScanned(UUID id, Timestamp lastScanned) {
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
    
    public MediaElement getMediaElementByID(UUID id) {
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
            mediaElement.setID((UUID)rs.getObject("ID"));
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
            
            return mediaElement;
        }
    }
    
    //
    // Video Stream
    //
    
    public boolean createVideoStreams(final List<VideoStream> videoStreams) {
        String sql = "INSERT INTO VideoStream (MEID,SID,Title,Codec,Profile,Width,Height,PixelFormat,ColorSpace,ColorTransfer,ColorPrimaries,Interlaced,FPS,Bitrate,MaxBitrate,BPS,Language,Default,Forced) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {                    
                    VideoStream videoStream = videoStreams.get(i);
                    ps.setString(1, videoStream.getMediaElementId().toString());
                    ps.setInt(2, videoStream.getStreamId());
                    ps.setString(3, videoStream.getTitle());
                    ps.setString(4, videoStream.getCodec());
                    ps.setString(5, videoStream.getProfile());
                    ps.setInt(6, videoStream.getWidth());
                    ps.setInt(7, videoStream.getHeight());
                    ps.setString(8, videoStream.getPixelFormat());
                    ps.setString(9, videoStream.getColorSpace());
                    ps.setString(10, videoStream.getColorTransfer());
                    ps.setString(11, videoStream.getColorPrimaries());
                    ps.setBoolean(12, videoStream.isInterlaced());
                    ps.setDouble(13, videoStream.getFPS());
                    ps.setInt(14, videoStream.getBitrate());
                    ps.setInt(15, videoStream.getMaxBitrate());
                    ps.setInt(16, videoStream.getBPS());
                    ps.setString(17, videoStream.getLanguage());
                    ps.setBoolean(18, videoStream.isDefault());
                    ps.setBoolean(19, videoStream.isForced());
                }

                @Override
                public int getBatchSize() {
                    return videoStreams.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to add video stream to database!", e);
            return false;
        }
                
        return true;
    }
    
    public boolean updateVideoStream(VideoStream stream) {
        try {
            mediaDatabase.getJdbcTemplate().update("UPDATE VideoStream SET Title=?, Codec=?, Profile=?, Width=?, Height=?, PixelFormat=?, ColorSpace=?, ColorTransfer=?, ColorPrimaries=?, Interlaced=?, FPS=?, Bitrate=?, MaxBitrate=?, BPS=?, Language=?, Default=?, Forced=? WHERE MEID=? AND SID=?",
                    new Object[]{stream.getTitle(),
                                 stream.getCodec(),
                                 stream.getProfile(),
                                 stream.getWidth(),
                                 stream.getHeight(),
                                 stream.getPixelFormat(),
                                 stream.getColorSpace(),
                                 stream.getColorTransfer(),
                                 stream.getColorPrimaries(),
                                 stream.isInterlaced(),
                                 stream.getFPS(),
                                 stream.getBitrate(),
                                 stream.getMaxBitrate(),
                                 stream.getBPS(),
                                 stream.getLanguage(),
                                 stream.isDefault(),
                                 stream.isForced(),
                                 stream.getMediaElementId(),
                                 stream.getStreamId(),
                    });
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }

        return true;
    }
    
    public boolean removeAllVideoStreams() {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM VideoStream");
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public void removeVideoStreamsByMediaElementId(UUID mediaElementId) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM VideoStream WHERE MEID=?", new Object[] {mediaElementId});
    }
    
    public List<VideoStream> getVideoStreamsByMediaElementId(UUID mediaElementId) {
        try {
            List<VideoStream> videoStreams;
            videoStreams = mediaDatabase.getJdbcTemplate().query("SELECT * FROM VideoStream WHERE MEID=?", new VideoStreamMapper(), new Object[] {mediaElementId});
            
            return videoStreams;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<VideoStream> getIncompleteVideoStreams() {
        try {
            List<VideoStream> videoStreams;
            videoStreams = mediaDatabase.getJdbcTemplate().query("SELECT * FROM VideoStream WHERE MaxBitrate=0", new VideoStreamMapper());
            
            return videoStreams;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    private static final class VideoStreamMapper implements RowMapper {
        @Override
        public VideoStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            VideoStream videoStream = new VideoStream();
            videoStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            videoStream.setStreamId(rs.getInt("SID"));
            videoStream.setTitle(rs.getString("Title"));
            videoStream.setCodec(rs.getString("Codec"));
            videoStream.setProfile(rs.getString("Profile"));
            videoStream.setWidth(rs.getInt("Width"));
            videoStream.setHeight(rs.getInt("Height"));
            videoStream.setPixelFormat(rs.getString("PixelFormat"));
            videoStream.setColorSpace(rs.getString("ColorSpace"));
            videoStream.setColorTransfer(rs.getString("ColorTransfer"));
            videoStream.setColorPrimaries(rs.getString("ColorPrimaries"));
            videoStream.setInterlaced(rs.getBoolean("Interlaced"));
            videoStream.setFPS(rs.getDouble("FPS"));
            videoStream.setBitrate(rs.getInt("Bitrate"));
            videoStream.setMaxBitrate(rs.getInt("MaxBitrate"));
            videoStream.setBPS(rs.getInt("BPS"));
            videoStream.setLanguage(rs.getString("Language"));
            videoStream.setDefault(rs.getBoolean("Default"));
            videoStream.setForced(rs.getBoolean("Forced"));
            
            return videoStream;
        }
    }
    
    //
    // Audio Stream
    //
    
    public boolean createAudioStreams(final List<AudioStream> audioStreams) {
        String sql = "INSERT INTO AudioStream (MEID,SID,Title,Codec,SampleRate,Channels,Bitrate,BPS,Language,Default,Forced) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {                    
                    AudioStream audioStream = audioStreams.get(i);
                    ps.setString(1, audioStream.getMediaElementId().toString());
                    ps.setInt(2, audioStream.getStreamId());
                    ps.setString(3, audioStream.getTitle());
                    ps.setString(4, audioStream.getCodec());
                    ps.setInt(5, audioStream.getSampleRate());
                    ps.setInt(6, audioStream.getChannels());
                    ps.setInt(7, audioStream.getBitrate());
                    ps.setInt(8, audioStream.getBPS());
                    ps.setString(9, audioStream.getLanguage());
                    ps.setBoolean(10, audioStream.isDefault());
                    ps.setBoolean(11, audioStream.isForced());
                }

                @Override
                public int getBatchSize() {
                    return audioStreams.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to add audio stream to database!", e);
            return false;
        }
        
        return true;
    }
    
    public boolean removeAllAudioStreams() {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM AudioStream");
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public void removeAudioStreamsByMediaElementId(UUID mediaElementId) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM AudioStream WHERE MEID=?", new Object[] {mediaElementId});
    }
    
    public List<AudioStream> getAudioStreamsByMediaElementId(UUID mediaElementId) {
        try {
            List<AudioStream> audioStreams;
            audioStreams = mediaDatabase.getJdbcTemplate().query("SELECT * FROM AudioStream WHERE MEID=?", new AudioStreamMapper(), new Object[] {mediaElementId});
            
            return audioStreams;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    private static final class AudioStreamMapper implements RowMapper {
        @Override
        public AudioStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            AudioStream audioStream = new AudioStream();
            audioStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            audioStream.setStreamId(rs.getInt("SID"));
            audioStream.setTitle(rs.getString("Title"));
            audioStream.setCodec(rs.getString("Codec"));
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
    
    //
    // Subtitle Stream
    //
    
    public boolean createSubtitleStreams(final List<SubtitleStream> subtitleStreams) {
        String sql = "INSERT INTO SubtitleStream (MEID,SID,Title,Codec,Language,Default,Forced) " +
                                "VALUES (?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    SubtitleStream subtitleStream = subtitleStreams.get(i);
                    ps.setString(1, subtitleStream.getMediaElementId().toString());
                    ps.setInt(2, subtitleStream.getStreamId());
                    ps.setString(3, subtitleStream.getTitle());
                    ps.setString(4, subtitleStream.getCodec());
                    ps.setString(5, subtitleStream.getLanguage());
                    ps.setBoolean(6, subtitleStream.isDefault());
                    ps.setBoolean(7, subtitleStream.isForced());
                }

                @Override
                public int getBatchSize() {
                    return subtitleStreams.size();
                }
            });
        } catch (DataAccessException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to add subtitle stream to database!", e);
            return false;
        }
        
        return true;
    }
    
    public boolean removeAllSubtitleStreams() {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM SubtitleStream");
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public void removeSubtitleStreamsByMediaElementId(UUID mediaElementId) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM SubtitleStream WHERE MEID=?", new Object[] {mediaElementId});
    }
    
    public List<SubtitleStream> getSubtitleStreamsByMediaElementId(UUID mediaElementId) {
        try {
            List<SubtitleStream> subtitleStreams;
            subtitleStreams = mediaDatabase.getJdbcTemplate().query("SELECT * FROM SubtitleStream WHERE MEID=?", new SubtitleStreamMapper(), new Object[] {mediaElementId});
            
            return subtitleStreams;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    private static final class SubtitleStreamMapper implements RowMapper {
        @Override
        public SubtitleStream mapRow(ResultSet rs, int rowNum) throws SQLException {
            SubtitleStream subtitleStream = new SubtitleStream();
            subtitleStream.setMediaElementId(UUID.fromString(rs.getString("MEID")));
            subtitleStream.setStreamId(rs.getInt("SID"));
            subtitleStream.setTitle(rs.getString("Title"));
            subtitleStream.setCodec(rs.getString("Codec"));
            subtitleStream.setLanguage(rs.getString("Language"));
            subtitleStream.setDefault(rs.getBoolean("Default"));
            subtitleStream.setForced(rs.getBoolean("Forced"));
            
            return subtitleStream;
        }
    }
    
    //
    // Streams
    //
    
    public void removeStreamsByMediaElementId(UUID mediaElementId) {
        mediaDatabase.getJdbcTemplate().update("DELETE FROM VideoStream WHERE MEID=?", new Object[] {mediaElementId});
        mediaDatabase.getJdbcTemplate().update("DELETE FROM AudioStream WHERE MEID=?", new Object[] {mediaElementId});
        mediaDatabase.getJdbcTemplate().update("DELETE FROM SubtitleStream WHERE MEID=?", new Object[] {mediaElementId});
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
    
    public boolean updatePlaylistLastScanned(UUID id, Timestamp lastScanned) {
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
    
    public boolean setPlaylistContentFromIds(final UUID id, final List<UUID> mediaElementIds) {
        String sql = "INSERT INTO PlaylistContent (PID,MEID) " +
                                "VALUES (?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    UUID mediaElementId = mediaElementIds.get(i);
                    ps.setString(1, id.toString());
                    ps.setString(2, mediaElementId.toString());
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
                    ps.setString(2, mediaElement.getID().toString());
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
