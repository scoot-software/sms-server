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
import com.scooter1556.sms.server.database.MediaDatabase.AudioStreamMapper;
import com.scooter1556.sms.server.database.MediaDatabase.MediaElementMapper;
import com.scooter1556.sms.server.database.MediaDatabase.PlaylistMapper;
import com.scooter1556.sms.server.database.MediaDatabase.SubtitleStreamMapper;
import com.scooter1556.sms.server.database.MediaDatabase.VideoStreamMapper;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.service.LogService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
        String sql = "INSERT INTO MediaElement (ID,Type,DirectoryType,Path,ParentPath,LastScanned,Excluded,Format,Size,Duration,Bitrate,Title,Artist,AlbumArtist,Album,Year,DiscNumber,DiscSubtitle,TrackNumber,Genre,Rating,Tagline,Description,Certificate,Collection,ReplaygainTrack,ReplaygainAlbum) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
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
                    ps.setInt(8, mediaElement.getFormat());
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
                    ps.setFloat(26, mediaElement.getReplaygainTrack());
                    ps.setFloat(27, mediaElement.getReplaygainAlbum());
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
    
    public void removeMediaElementsByPath(String path) {            
        mediaDatabase.getJdbcTemplate().update("DELETE FROM MediaElement WHERE ParentPath LIKE ?", path + "%");
    }
    
    public boolean updateMediaElementsByID(final List<MediaElement> mediaElements) {
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=?,ReplaygainTrack=?,ReplaygainAlbum=? WHERE ID=?";
        
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
                    ps.setFloat(21, mediaElement.getReplaygainTrack());
                    ps.setFloat(22, mediaElement.getReplaygainAlbum());
                    ps.setObject(23, mediaElement.getID());
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
        String sql = "UPDATE MediaElement SET DirectoryType=?,LastScanned=?,Excluded=?,Size=?,Duration=?,Bitrate=?,Title=?,Artist=?,AlbumArtist=?,Album=?,Year=?,DiscNumber=?,DiscSubtitle=?,TrackNumber=?,Genre=?,Rating=?,Tagline=?,Description=?,Certificate=?,Collection=?,ReplaygainTrack=?,ReplaygainAlbum=? WHERE PATH=?";
        
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
                    ps.setFloat(21, mediaElement.getReplaygainTrack());
                    ps.setFloat(22, mediaElement.getReplaygainAlbum());
                    ps.setString(23, mediaElement.getPath());
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
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Path=?", new MediaElementMapper(), new Object[] {path});

            if(mediaElements != null && !mediaElements.isEmpty()) {
                return mediaElements.get(0);
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return null;
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
    
    public List<MediaElement> getRandomMediaElements(Byte type) {
        List<MediaElement> mediaElements;

        try {
            if(type == null) {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE NOT Excluded ORDER BY RAND() DESC", new MediaElementMapper());
            } else {
                mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY RAND() DESC", new MediaElementMapper(), new Object[] {type});
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
    
    public List<MediaElement> getDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Title DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND NOT Excluded ORDER BY Created DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedVideoDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.VIDEO});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedVideoDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND NOT Excluded ORDER BY Created DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.VIDEO});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyPlayedAudioDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND LastPlayed IS NOT NULL AND NOT Excluded ORDER BY LastPlayed DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.AUDIO});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getRecentlyAddedAudioDirectoryElements() {
        try {
            List<MediaElement> mediaElements = mediaDatabase.getJdbcTemplate().query("SELECT * FROM MediaElement WHERE Type=? AND DirectoryType=? AND NOT Excluded ORDER BY Created DESC", new MediaElementMapper(), new Object[] {MediaElement.MediaElementType.DIRECTORY, MediaElement.DirectoryMediaType.AUDIO});
            return mediaElements;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getArtists() {
        try {
            List<MediaElement> artists = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(Artist,ParentPath) * FROM MediaElement WHERE Artist IS NOT NULL ORDER BY Artist", new MediaElementMapper());
            return artists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getAlbumArtists() {
        try {
            List<MediaElement> albumArtists = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(AlbumArtist,ParentPath) * FROM MediaElement WHERE AlbumArtist IS NOT NULL ORDER BY AlbumArtist", new MediaElementMapper());
            return albumArtists;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getAlbums() {
        try {
            List<MediaElement> albums = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(Album) * FROM MediaElement WHERE Album IS NOT NULL ORDER BY Album", new MediaElementMapper());
            return albums;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getAlbumsByArtist(String artist) {
        try {
            List<MediaElement> albums = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(Album) * FROM MediaElement WHERE Artist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {artist}, new MediaElementMapper());
            return albums;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<MediaElement> getAlbumsByAlbumArtist(String albumArtist) {
        try {
            List<MediaElement> albums = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(Album) * FROM MediaElement WHERE AlbumArtist=? AND Album IS NOT NULL ORDER BY Album", new Object[] {albumArtist}, new MediaElementMapper());
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
    
    public List<MediaElement> getCollections() {
        try {
            List<MediaElement> collections = mediaDatabase.getJdbcTemplate().query("SELECT DISTINCT ON(Collection) * FROM MediaElement WHERE Collection IS NOT NULL ORDER BY Collection", new MediaElementMapper());
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
    
    //
    // Video Stream
    //
    
    public boolean createVideoStreams(final List<VideoStream> videoStreams) {
        String sql = "INSERT INTO VideoStream (MEID,SID,Title,Codec,Width,Height,Interlaced,FPS,Bitrate,MaxBitrate,BPS,GOP,Language,Default,Forced) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        
        try {
            mediaDatabase.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {	
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {                    
                    VideoStream videoStream = videoStreams.get(i);
                    ps.setString(1, videoStream.getMediaElementId().toString());
                    ps.setInt(2, videoStream.getStreamId());
                    ps.setString(3, videoStream.getTitle());
                    ps.setInt(4, videoStream.getCodec());
                    ps.setInt(5, videoStream.getWidth());
                    ps.setInt(6, videoStream.getHeight());
                    ps.setBoolean(7, videoStream.isInterlaced());
                    ps.setDouble(8, videoStream.getFPS());
                    ps.setInt(9, videoStream.getBitrate());
                    ps.setInt(10, videoStream.getMaxBitrate());
                    ps.setInt(11, videoStream.getBPS());
                    ps.setInt(12, videoStream.getGOPSize());
                    ps.setString(13, videoStream.getLanguage());
                    ps.setBoolean(14, videoStream.isDefault());
                    ps.setBoolean(15, videoStream.isForced());
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
            mediaDatabase.getJdbcTemplate().update("UPDATE VideoStream SET Title=?, Codec=?, Width=?, Height=?, Interlaced=?, FPS=?, Bitrate=?, MaxBitrate=?, BPS=?, GOP=?, Language=?, Default=?, Forced=? WHERE MEID=? AND SID=?",
                    new Object[]{stream.getTitle(),
                                 stream.getCodec(),
                                 stream.getWidth(),
                                 stream.getHeight(),
                                 stream.isInterlaced(),
                                 stream.getFPS(),
                                 stream.getBitrate(),
                                 stream.getMaxBitrate(),
                                 stream.getBPS(),
                                 stream.getGOPSize(),
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
    
    public boolean removeVideoStreamsByMediaElementId(UUID mediaElementId) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM VideoStream WHERE MEID=?", new Object[] {mediaElementId});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
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
            videoStreams = mediaDatabase.getJdbcTemplate().query("SELECT * FROM VideoStream WHERE MaxBitrate=0 OR GOP=0     ", new VideoStreamMapper());
            
            return videoStreams;
        } catch (DataAccessException e) {
            return null;
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
                    ps.setInt(4, audioStream.getCodec());
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
    
    public boolean removeAudioStreamsByMediaElementId(UUID mediaElementId) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM AudioStream WHERE MEID=?", new Object[] {mediaElementId});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
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
                    ps.setInt(4, subtitleStream.getCodec());
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
    
    public boolean removeSubtitleStreamsByMediaElementId(UUID mediaElementId) {            
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM SubtitleStream WHERE MEID=?", new Object[] {mediaElementId});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
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
    
    //
    // Streams
    //
    
    public boolean removeStreamsByMediaElementId(UUID mediaElementId) {
        try {
            mediaDatabase.getJdbcTemplate().update("DELETE FROM VideoStream WHERE MEID=?", new Object[] {mediaElementId});
            mediaDatabase.getJdbcTemplate().update("DELETE FROM AudioStream WHERE MEID=?", new Object[] {mediaElementId});
            mediaDatabase.getJdbcTemplate().update("DELETE FROM SubtitleStream WHERE MEID=?", new Object[] {mediaElementId});
        } catch (InvalidResultSetAccessException e) {
            return false;
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
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
}
