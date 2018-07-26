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
package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scooter1556.sms.server.SMS;
import java.awt.Dimension;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class MediaElement implements Serializable {
    private UUID id;
    private Byte type;
    private Byte directoryType = DirectoryMediaType.NONE;
    private String path;
    private String parentPath;
    private Timestamp created;
    private Timestamp lastPlayed;
    private Timestamp lastScanned;
    private Boolean excluded = false;
    private Integer format = SMS.Format.NONE;
    private Long size = 0L;
    private Double duration = 0d;
    private Integer bitrate = 0;
    private String title;
    private String artist;
    private String albumArtist;
    private String album;
    private Short year = 0;
    private Short discNumber = 0;
    private String discSubtitle;
    private Short trackNumber = 0;
    private String genre;
    private Float rating = 0f;
    private String tagline;
    private String description;
    private String certificate;
    private String collection;
    
    List<VideoStream> videoStreams;
    List<AudioStream> audioStreams;
    List<SubtitleStream> subtitleStreams;

    public MediaElement() {};
    
    public MediaElement(UUID id,
                        Byte type,
                        Byte directoryType,
                        String path,
                        String parentPath,
                        Timestamp created,
                        Timestamp lastPlayed,
                        Timestamp lastScanned,
                        Boolean excluded,
                        Integer format,
                        Long size,
                        Double duration,
                        Integer bitrate,
                        String title,
                        String artist,
                        String albumArtist,
                        String album,
                        Short year,
                        Short discNumber,
                        String discSubtitle,
                        Short trackNumber,
                        String genre,
                        Float rating,
                        String tagline,
                        String description,
                        String certificate,
                        String collection) {
        this.id = id;
        this.type = type;
        this.directoryType = directoryType;
        this.path = path;
        this.parentPath = parentPath;
        this.created = created;
        this.lastPlayed = lastPlayed;
        this.lastScanned = lastScanned;
        this.excluded = excluded;
        this.format = format;
        this.size = size;
        this.duration = duration;
        this.bitrate = bitrate;
        this.title = title;
        this.artist = artist;
        this.albumArtist = albumArtist;
        this.album = album;
        this.year = year;
        this.discNumber = discNumber;
        this.discSubtitle = discSubtitle;
        this.trackNumber = trackNumber;
        this.genre = genre;
        this.rating = rating;
        this.tagline = tagline;
        this.description = description;
        this.certificate = certificate;
        this.collection = collection;
    }
    
    @Override
    public String toString() {
        // Return 'type' specific string
        switch(type) {
            case MediaElementType.DIRECTORY:
                return String.format(
                    "DirectoryElement[ID=%s, Directory Type=%s, Title=%s, Path=%s, Excluded=%s, Created=%s, LastPlayed=%s]",
                    id == null ? "?" : id.toString(),
                    directoryType.toString(),
                    title == null ? "N/A" : title,
                    path == null ? "N/A" : path,
                    excluded.toString(),
                    created == null ? "Unknown" : created.toString(),
                    lastPlayed == null ? "Never" : lastPlayed.toString());
            case MediaElementType.VIDEO:
                return String.format(
                    "VideoElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Year=%s, Genre=%s, Rating=%s, Tagline=%s, Description=%s, Certificate=%s, Collection=%s, Video Streams=%s, Audio Streams=%s, Subtitle Streams=%s]",
                    id == null ? "?" : id.toString(),
                    title == null ? "N/A" : title,
                    path == null ? "N/A" : path,
                    created == null ? "Unknown" : created.toString(),
                    lastPlayed == null ? "Never" : lastPlayed.toString(),
                    duration.toString(),
                    bitrate.toString(),
                    year.toString(),
                    genre == null ? "N/A" : genre,
                    rating.toString(),
                    tagline == null ? "N/A" : tagline,
                    description == null ? "N/A" : description,
                    certificate == null ? "N/A" : certificate,
                    collection == null ? "N/A" : collection,
                    videoStreams == null ? "N/A" : videoStreams.toString(),
                    audioStreams == null ? "N/A" : audioStreams.toString(),
                    subtitleStreams == null ? "N/A" : subtitleStreams.toString());
            case MediaElementType.AUDIO:
                return String.format(
                    "AudioElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Artist=%s, Album Artist=%s, Album=%s, Track Number=%s, Disc Number=%s, Disc Subtitle=%s, Year=%s, Genre=%s, Description=%s, Audio Streams=%s]",
                    id == null ? "?" : id.toString(),
                    title == null ? "N/A" : title,
                    path == null ? "N/A" : path,
                    created == null ? "Unknown" : created.toString(),
                    lastPlayed == null ? "Never" : lastPlayed.toString(),
                    duration.toString(),
                    bitrate.toString(),
                    artist == null ? "N/A" : artist,
                    albumArtist == null ? "N/A" : albumArtist,
                    album == null ? "N/A" : album,
                    trackNumber.toString(),
                    discNumber.toString(),
                    discSubtitle == null ? "N/A" : discSubtitle,
                    year.toString(),
                    genre == null ? "N/A" : genre,
                    description == null ? "N/A" : description,
                    audioStreams == null ? "N/A" : audioStreams.toString());
                
            default:
                return String.format(
                        "MediaElement[ID=%s, Path=%s, Created=%s]",
                        id == null ? "?" : id.toString(), path == null ? "N/A" : path, created == null ? "Unknown" : created.toString());
        }
    }

    public UUID getID()  {
        return id;
    }
    
    public void setID(UUID id) {
        this.id = id;
    }
    
    public Byte getType() {
        return type;
    }
    
    public void setType(Byte type) {
        this.type = type;
    }
    
    public Byte getDirectoryType() {
        return directoryType;
    }
    
    public void setDirectoryType(Byte directoryType) {
        this.directoryType = directoryType;
    }
    
    @JsonIgnore
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @JsonIgnore
    public String getParentPath() {
        return parentPath;
    }
    
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
    
    @JsonIgnore
    public Timestamp getCreated() {
        return created;
    }
    
    public void setCreated(Timestamp created) {
        this.created = created;
    }
    
    @JsonIgnore
    public Timestamp getLastPlayed() {
        return lastPlayed;
    }
    
    public void setLastPlayed(Timestamp lastPlayed) {
        this.lastPlayed = lastPlayed;
    }
    
    @JsonIgnore
    public Timestamp getLastScanned() {
        return lastScanned;
    }
    
    public void setLastScanned(Timestamp lastScanned) {
        this.lastScanned = lastScanned;
    }
    
    @JsonIgnore
    public Boolean getExcluded() {
        return excluded;
    }
    
    public void setExcluded(Boolean excluded) {
        this.excluded = excluded;
    }
    
    @JsonIgnore
    public Integer getFormat() {
        return format;
    }
    
    public void setFormat(Integer format) {
        this.format = format;
    }
    
    @JsonIgnore
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public Double getDuration() {
        return duration;
    }
    
    public void setDuration(Double duration) {
        this.duration = duration;
    }
    
    @JsonIgnore
    public Integer getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getAlbumArtist() {
        return albumArtist;
    }
    
    public void setAlbumArtist(String albumArtist) {
        this.albumArtist = albumArtist;
    }
    
    public String getAlbum() {
        return album;
    }
    
    public void setAlbum(String album) {
        this.album = album;
    }
    
    public Short getYear() {
        return year;
    }
    
    public void setYear(Short year) {
        this.year = year;
    }
    
    public Short getDiscNumber() {
        return discNumber;
    }
    
    public void setDiscNumber(Short discNumber) {
        this.discNumber = discNumber;
    }
    
    public String getDiscSubtitle() {
        return discSubtitle;
    }
    
    public void setDiscSubtitle(String discSubtitle) {
        this.discSubtitle = discSubtitle;
    }
    
    public Short getTrackNumber() {
        return trackNumber;
    }
    
    public void setTrackNumber(Short trackNumber) {
        this.trackNumber = trackNumber;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public void setGenre(String genre) {
        this.genre = genre;
    }
    
    public Float getRating() {
        return rating;
    }
    
    public void setRating(Float rating) {
        this.rating = rating;
    }
    
    public String getTagline() {
        return tagline;
    }
    
    public void setTagline(String tagline) {
        this.tagline = tagline;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCertificate() {
        return certificate;
    }
    
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
    
    public String getCollection() {
        return collection;
    }
    
    public void setCollection(String collection) {
        this.collection = collection;
    }
    
    @JsonIgnore
    public List<VideoStream> getVideoStreams() {
        return videoStreams;
    }
    
    public void setVideoStreams(List<VideoStream> videoStreams) {
        this.videoStreams = videoStreams;
    }
    
    @JsonIgnore
    public List<AudioStream> getAudioStreams() {
        return audioStreams;
    }
    
    public void setAudioStreams(List<AudioStream> audioStreams) {
        this.audioStreams = audioStreams;
    }
    
    @JsonIgnore
    public List<SubtitleStream> getSubtitleStreams() {
        return subtitleStreams;
    }
    
    public void setSubtitleStreams(List<SubtitleStream> subtitleStreams) {
        this.subtitleStreams = subtitleStreams;
    }
    
    public static class Stream {
        UUID mediaElementId;
        Integer streamId, codec;
        String title, language;
        Boolean isDefault, isForced;
        
        public Stream() {};
        
        public UUID getMediaElementId() {
            return mediaElementId;
        }
        
        public void setMediaElementId(UUID mediaElementId) {
            this.mediaElementId = mediaElementId;
        }
        
        public Integer getStreamId() {
            return streamId;
        }
        
        public void setStreamId(Integer streamId) {
            this.streamId = streamId;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public Integer getCodec() {
            return codec;
        }
        
        public void setCodec(Integer codec) {
            this.codec = codec;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public void setLanguage(String language) {
            this.language = language;
        }
        
        public Boolean isDefault() {
            return isDefault;
        }
        
        public void setDefault(Boolean isDefault) {
            this.isDefault = isDefault;
        }
        
        public Boolean isForced() {
            return isForced;
        }
        
        public void setForced(Boolean isForced) {
            this.isForced = isForced;
        }
    }
    
    public static class VideoStream extends Stream {
        private Double fps;
        private Integer width, height, bitrate, maxBitrate, bps;
        private Boolean interlaced;
        
        public VideoStream() {};
        
        public VideoStream(UUID mediaElementId,
                           Integer streamId,
                           String title,
                           Integer codec,
                           Integer width,
                           Integer height,
                           Boolean interlaced,
                           Double fps,
                           Integer bitrate,
                           Integer maxBitrate,
                           Integer bps,
                           String language,
                           Boolean isDefault,
                           Boolean isForced) {
            this.mediaElementId = mediaElementId;
            this.streamId = streamId;
            this.title = title;
            this.codec = codec;
            this.width = width;
            this.height = height;
            this.interlaced = interlaced;
            this.fps = fps;
            this.bitrate = bitrate;
            this.maxBitrate = maxBitrate;
            this.bps = bps;
            this.language = language;
            this.isDefault = isDefault;
            this.isForced = isForced;
        }
        
        @Override
        public String toString() {
            return String.format(
                        "{Media Element ID=%s, Stream ID=%s, Title=%s, Codec=%s, Width=%s, Height=%s, Interlaced=%s, FPS=%s, Bitrate=%s kb/s, Max Bitrate=%s kb/s, Bits Per Sample=%s, Language=%s, Default=%s, Forced=%s}",
                        mediaElementId == null ? "N/A" : mediaElementId.toString(),
                        streamId == null ? "N/A" : streamId.toString(),
                        title == null ? "N/A" : title,
                        codec == null ? "N/A" : codec,
                        width == null ? "N/A" : width.toString(),
                        height == null ? "N/A" : height.toString(),
                        interlaced == null ? "N/A" : interlaced.toString(),
                        fps == null ? "N/A" : fps.toString(),
                        bitrate == null ? "N/A" : bitrate.toString(),
                        maxBitrate == null ? "N/A" : maxBitrate.toString(),
                        bps == null ? "N/A" : bps.toString(),
                        language == null ? "N/A" : language,
                        isDefault == null ? "False" : isDefault.toString(),
                        isForced == null ? "False" : isForced.toString());
        }
        
        public Integer getWidth() {
            return width;
        }
        
        public void setWidth(Integer width) {
            this.width = width;
        }
        
        public Integer getHeight() {
            return height;
        }
        
        public void setHeight(Integer height) {
            this.height = height;
        }
        
        public Dimension getResolution() {
            return new Dimension(width, height);
        }
        
        public Boolean isInterlaced() {
            return interlaced;
        }
        
        public void setInterlaced(Boolean interlaced) {
            this.interlaced = interlaced;
        }
        
        public Double getFPS() {
            return fps;
        }
        
        public void setFPS(Double fps) {
            this.fps = fps;
        }
        
        public Integer getBitrate() {
            return bitrate;
        }
        
        public void setBitrate(Integer bitrate) {
            this.bitrate = bitrate;
        }
        
        public Integer getMaxBitrate() {
            return maxBitrate;
        }
        
        public void setMaxBitrate(Integer maxBitrate) {
            this.maxBitrate = maxBitrate;
        }
        
        public Integer getBPS() {
            return bps;
        }
        
        public void setBPS(Integer bps) {
            this.bps = bps;
        }
    }
    
    public static class AudioStream extends Stream {
        private Integer sampleRate, channels, bitrate, bps;
        
        public AudioStream() {};
        
        public AudioStream(UUID mediaElementId,
                           Integer streamId,
                           String title,
                           Integer codec,
                           Integer sampleRate,
                           Integer channels,
                           Integer bitrate,
                           Integer bps,
                           String language,
                           Boolean isDefault,
                           Boolean isForced) {
            
            this.mediaElementId = mediaElementId;
            this.streamId = streamId;
            this.title = title;
            this.codec = codec;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.bitrate = bitrate;
            this.bps = bps;
            this.language = language;
            this.isDefault = isDefault;
            this.isForced = isForced;
        }
        
        @Override
        public String toString() {
            return String.format(
                        "{Media Element ID=%s, Stream ID=%s, Title=%s, Codec=%s, Sample Rate=%s Hz, Channels=%s, Bitrate=%s kb/s, Bits Per Sample=%s, Language=%s, Default=%s, Forced=%s}",
                        mediaElementId == null ? "N/A" : mediaElementId.toString(),
                        streamId == null ? "N/A" : streamId.toString(),
                        title == null ? "N/A" : title,
                        codec == null ? "N/A" : codec,
                        sampleRate == null ? "N/A" : sampleRate.toString(),
                        channels == null ? "N/A" : channels.toString(),
                        bitrate == null ? "N/A" : bitrate.toString(),
                        bps == null ? "N/A" : bps.toString(),
                        language == null ? "N/A" : language,
                        isDefault == null ? "False" : isDefault.toString(),
                        isForced == null ? "False" : isForced.toString());
        }
        
        public Integer getSampleRate() {
            return sampleRate;
        }
        
        public void setSampleRate(Integer sampleRate) {
            this.sampleRate = sampleRate;
        }
        
        public Integer getChannels() {
            return channels;
        }
        
        public void setChannels(Integer channels) {
            this.channels = channels;
        }
        
        public Integer getBitrate() {
            return bitrate;
        }
        
        public void setBitrate(Integer bitrate) {
            this.bitrate = bitrate;
        }
        
        public Integer getBPS() {
            return bps;
        }
        
        public void setBPS(Integer bps) {
            this.bps = bps;
        }
    }
    
    public static class SubtitleStream extends Stream {
        
        public SubtitleStream() {};
        
        public SubtitleStream(UUID mediaElementId,
                              Integer streamId,
                              String title,
                              Integer codec,
                              String language,
                              Boolean isDefault,
                              Boolean isForced) {
            this.mediaElementId = mediaElementId;
            this.streamId = streamId;
            this.title = title;
            this.codec = codec;
            this.language = language;
            this.isDefault = isDefault;
            this.isForced = isForced;
        }
        
        @Override
        public String toString() {
            return String.format(
                        "{Media Element ID=%s, Stream ID=%s, Title=%s, Codec=%s, Language=%s, Default=%s, Forced=%s}",
                        mediaElementId == null ? "N/A" : mediaElementId.toString(),
                        streamId == null ? "N/A" : streamId.toString(),
                        title == null ? "N/A" : title,
                        codec == null ? "N/A" : codec,
                        language == null ? "N/A" : language,
                        isDefault == null ? "False" : isDefault.toString(),
                        isForced == null ? "False" : isForced.toString());
        }
    }

    public static class MediaElementType {
        public static final byte NONE = 0;
        public static final byte AUDIO = 1;
        public static final byte VIDEO = 2;
        public static final byte SUBTITLE = 3;
        public static final byte DIRECTORY = 4;
    }
    
    public static class DirectoryMediaType {
        public static final byte NONE = 0;
        public static final byte AUDIO = 1;
        public static final byte VIDEO = 2;
        public static final byte MIXED = 3;
    }
}