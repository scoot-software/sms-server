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
import java.awt.Dimension;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class MediaElement implements Serializable {
    
    private Long id;
    private Byte type;
    private Byte directoryType = -1;
    private String path;
    private String parentPath;
    private Timestamp created;
    private Timestamp lastPlayed;
    private Timestamp lastScanned;
    private Boolean excluded = false;
    private String format;
    private Long size = 0L;
    private Integer duration = 0;
    private Integer bitrate = 0;
    private Short videoWidth = 0;
    private Short videoHeight = 0;
    private String videoCodec;
    private String audioName;
    private String audioCodec;
    private String audioSampleRate;
    private String audioConfiguration;
    private String audioLanguage;
    private String subtitleName;
    private String subtitleLanguage;
    private String subtitleFormat;
    private String subtitleForced;
    private String title;
    private String artist;
    private String albumArtist;
    private String album;
    private Short year = 0;
    private Byte discNumber = 0;
    private String discSubtitle;
    private Short trackNumber = 0;
    private String genre;
    private Float rating = 0F;
    private String tagline;
    private String description;
    private String certificate;
    private String collection;

    public MediaElement() {};
    
    public MediaElement(Long id,
                        Byte type,
                        Byte directoryType,
                        String path,
                        String parentPath,
                        Timestamp created,
                        Timestamp lastPlayed,
                        Timestamp lastScanned,
                        Boolean excluded,
                        String format,
                        Long size,
                        Integer duration,
                        Integer bitrate,
                        Short videoWidth,
                        Short videoHeight,
                        String videoCodec,
                        String audioName,
                        String audioCodec,
                        String audioSampleRate,
                        String audioConfiguration,
                        String audioLanguage,
                        String subtitleName,
                        String subtitleLanguage,
                        String subtitleFormat,
                        String subtitleForced,
                        String title,
                        String artist,
                        String albumArtist,
                        String album,
                        Short year,
                        Byte discNumber,
                        String discSubtitle,
                        Short trackNumber,
                        String genre,
                        Float rating,
                        String tagline,
                        String description,
                        String certificate,
                        String collection)
    {
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
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.videoCodec = videoCodec;
        this.audioName = audioName;
        this.audioCodec = audioCodec;
        this.audioSampleRate = audioSampleRate;
        this.audioConfiguration = audioConfiguration;
        this.audioLanguage = audioLanguage;
        this.subtitleName = subtitleName;
        this.subtitleLanguage = subtitleLanguage;
        this.subtitleFormat = subtitleFormat;
        this.subtitleForced = subtitleForced;
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
        if(type != null)
        {
            if(type == MediaElementType.DIRECTORY)
            {
                return String.format(
                    "DirectoryElement[ID=%s, Directory Type=%s, Title=%s, Path=%s, Excluded=%s, Created=%s, LastPlayed=%s]",
                    id == null ? "?" : id.toString(), directoryType.toString(), title == null ? "N/A" : title, path == null ? "N/A" : path, excluded.toString(), created == null ? "Unknown" : created.toString(), lastPlayed == null ? "Never" : lastPlayed.toString());
            }
            else if(type == MediaElementType.VIDEO)
            {
                return String.format(
                    "VideoElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Resolution=%sx%s, Video Codec=%s, Audios=%s, Audio Languages=%s, Audio Codecs=%s, Audio Configurations=%s, Subtitles=%s, Subtitle Languages=%s, Subtitle Formats=%s, Year=%s, Genre=%s, Rating=%s, Tagline=%s, Description=%s, Certificate=%s, Collection=%s]",
                    id == null ? "?" : id.toString(), title == null ? "N/A" : title, path == null ? "N/A" : path, created == null ? "Unknown" : created.toString(), lastPlayed == null ? "Never" : lastPlayed.toString(), duration.toString(), bitrate.toString(), videoWidth.toString(), videoHeight.toString(), videoCodec == null ? "N/A" : videoCodec, audioName == null ? "N/A" : audioName, audioLanguage == null ? "N/A" : audioLanguage, audioCodec == null ? "N/A" : audioCodec, audioConfiguration == null ? "N/A" : audioConfiguration, subtitleName == null ? "N/A" : subtitleName, subtitleLanguage == null ? "N/A" : subtitleLanguage, subtitleFormat == null ? "N/A" : subtitleFormat, year.toString(), genre == null ? "N/A" : genre, rating.toString(), tagline == null ? "N/A" : tagline, description == null ? "N/A" : description, certificate == null ? "N/A" : certificate, collection == null ? "N/A" : collection);
            }
            else if(type == MediaElementType.AUDIO)
            {
                return String.format(
                    "AudioElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Audio Codecs=%s, Sample Rate=%s Hz, Audio Configurations=%s, Artist=%s, Album Artist=%s, Album=%s, Track Number=%s, Disc Number=%s, Disc Subtitle=%s, Year=%s, Genre=%s, Description=%s]",
                    id == null ? "?" : id.toString(), title == null ? "N/A" : title, path == null ? "N/A" : path, created == null ? "Unknown" : created.toString(), lastPlayed == null ? "Never" : lastPlayed.toString(), duration.toString(), bitrate.toString(), audioCodec == null ? "N/A" : audioCodec, audioSampleRate == null ? "?" : audioSampleRate, audioConfiguration == null ? "N/A" : audioConfiguration, artist == null ? "N/A" : artist, albumArtist == null ? "N/A" : albumArtist, album == null ? "N/A" : album, trackNumber.toString(), discNumber.toString(), discSubtitle == null ? "N/A" : discSubtitle, year.toString(), genre == null ? "N/A" : genre, description == null ? "N/A" : description);
            }
        }
        
        // Default
        return String.format(
                "MediaElement[ID=%s, Path=%s, Created=%s]",
                id == null ? "?" : id.toString(), path == null ? "N/A" : path, created == null ? "Unknown" : created.toString());
    }

    public Long getID()  {
        return id;
    }
    
    public void setID(Long id) {
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
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    @JsonIgnore
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    @JsonIgnore
    public Integer getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }
    
    @JsonIgnore
    public Short getVideoWidth() {
        return videoWidth;
    }
    
    public void setVideoWidth(Short videoWidth) {
        this.videoWidth = videoWidth;
    }
    
    @JsonIgnore
    public Short getVideoHeight() {
        return videoHeight;
    }
    
    public void setVideoHeight(Short videoHeight) {
        this.videoHeight = videoHeight;
    }
    
    @JsonIgnore
    public String getVideoCodec() {
        return videoCodec;
    }
    
    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }
    
    @JsonIgnore
    public String getAudioName() {
        return audioName;
    }
    
    public void setAudioName(String audioName) {
        this.audioName = audioName;
    }
    
    @JsonIgnore
    public String getAudioCodec() {
        return audioCodec;
    }
    
    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }
    
    @JsonIgnore
    public String getAudioSampleRate() {
        return audioSampleRate;
    }
    
    public void setAudioSampleRate(String audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }
    
    @JsonIgnore
    public String getAudioConfiguration() {
        return audioConfiguration;
    }
    
    public void setAudioConfiguration(String audioConfiguration) {
        this.audioConfiguration = audioConfiguration;
    }
    
    @JsonIgnore
    public String getAudioLanguage() {
        return audioLanguage;
    }
    
    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }
    
    @JsonIgnore
    public String getSubtitleName() {
        return subtitleName;
    }
    
    public void setSubtitleName(String subtitleName) {
        this.subtitleName = subtitleName;
    }
    
    @JsonIgnore
    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }
    
    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }
    
    @JsonIgnore
    public String getSubtitleFormat() {
        return subtitleFormat;
    }
    
    public void setSubtitleFormat(String subtitleFormat) {
        this.subtitleFormat = subtitleFormat;
    }
    
    @JsonIgnore
    public String getSubtitleForced() {
        return subtitleForced;
    }
    
    public void setSubtitleForced(String subtitleForced) {
        this.subtitleForced = subtitleForced;
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
    
    public Byte getDiscNumber() {
        return discNumber;
    }
    
    public void setDiscNumber(Byte discNumber) {
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
    
    //
    // Helper Functions
    //
    
    // Clear all attributes relating to Audio and Subtitle streams
    public void resetStreams()
    {
        this.audioName = null;
        this.audioCodec = null;
        this.audioConfiguration = null;
        this.audioLanguage = null;
        this.audioSampleRate = null;
        
        this.subtitleName = null;
        this.subtitleForced = null;
        this.subtitleFormat = null;
        this.subtitleLanguage = null;
    }
    
    @JsonIgnore
    public VideoStream getVideoStream() {
        // Check parameters are available
        if(videoCodec == null || videoWidth == null || videoHeight == null) { 
            return null;
        }
        
        return new VideoStream(videoCodec, videoWidth, videoHeight);
    }
    
    @JsonIgnore
    public List<AudioStream> getAudioStreams() {
        // Check parameters are available
        if(audioCodec == null || audioSampleRate == null || audioConfiguration == null) { 
            return null;
        }
        
        String[] audioNames = null;
        String[] audioLanguages = null;
        String[] audioCodecs;
        String[] audioSampleRates;
        String[] audioConfigurations;
        
        // Retrieve parameters as arrays
        if(audioLanguage != null) {
             audioLanguages = getAudioLanguage().split(",");
        }
        
        if(audioName != null) {
            audioNames = getAudioName().split(",");
        }
        
        audioCodecs = getAudioCodec().split(",");
        audioSampleRates = getAudioSampleRate().split(",");
        audioConfigurations = getAudioConfiguration().split(",");
        
        // Get the number of audio streams
        Integer streams = audioCodecs.length;
        
        // Check parameters are present for all streams
        if(audioSampleRates.length != streams || audioConfigurations.length != streams) {
            return null;
        }
        
        if(audioLanguages != null && audioLanguages.length != streams) {
            return null;
        }
        
        if(audioNames != null && audioNames.length != streams) {
            return null;
        }
        
        // Accumalate list of audio streams
        int count = 0;
        List<AudioStream> audioStreams = new ArrayList<>();
        
        while(count < streams) {
            audioStreams.add(new AudioStream(count, audioNames == null ? "null" : audioNames[count], audioLanguages == null ? "null" : audioLanguages[count], audioCodecs[count], Integer.parseInt(audioSampleRates[count]), audioConfigurations[count]));
            count ++;
        }
        
        return audioStreams;
    }
    
    @JsonIgnore
    public List<SubtitleStream> getSubtitleStreams() {
        // Check parameters are available
        if(subtitleLanguage == null || subtitleFormat == null || subtitleForced == null) { 
            return null;
        }
        
        String[] subtitleNames = null;
        String[] subtitleLanguages;
        String[] subtitleFormats;
        String[] subtitleForcedFlags;
        
        // Retrieve parameters as arrays
        if(subtitleName != null) {
            subtitleNames = getSubtitleName().split(",");
        }
        
        subtitleLanguages = getSubtitleLanguage().split(",");
        subtitleFormats = getSubtitleFormat().split(",");
        subtitleForcedFlags = getSubtitleForced().split(",");
        
        // Get the number of subtitle streams
        Integer streams = subtitleLanguages.length;
        
        // Check parameters are present for all streams
        if(subtitleFormats.length != streams || subtitleForcedFlags.length != streams) {
            return null;
        }
        
        if(subtitleNames != null && subtitleNames.length != streams) {
            return null;
        }
        
        // Accumalate list of subtitle streams
        int count = 0;
        List<SubtitleStream> subtitleStreams = new ArrayList<>();
        
        while(count < streams) {
            subtitleStreams.add(new SubtitleStream(count, subtitleNames == null ? "null" : subtitleNames[count], subtitleLanguages[count], subtitleFormats[count], Boolean.parseBoolean(subtitleForcedFlags[count])));
            count ++;
        }
        
        return subtitleStreams;
    }
    
    public static class VideoStream {
        private final String codec;
        private final short width, height;
        
        public VideoStream(String codec, short width, short height) {
            this.codec = codec;
            this.width = width;
            this.height = height;
        }

        public String getCodec() {
            return codec;
        }
        
        public short getWidth() {
            return width;
        }
        
        public short getHeight() {
            return height;
        }
        
        public Dimension getResolution() {
            return new Dimension(width, height);
        }
    }
    
    public static class AudioStream {
        private final int stream;
        private final String name;
        private final String language;
        private final String codec;
        private final int sampleRate;
        private final String configuration;
        
        public AudioStream(int stream, String name, String language, String codec, int sampleRate, String configuration) {
            this.stream = stream;
            this.name = name;
            this.language = language;
            this.codec = codec;
            this.sampleRate = sampleRate;
            this.configuration = configuration;
        }

        public int getStream() {
            return stream;
        }
        
        public String getName() {
            return name;
        }
        
        public String getLanguage() {
            return language;
        }

        public String getCodec() {
            return codec;
        }
        
        public int getSampleRate() {
            return sampleRate;
        }
        
        public String getConfiguration() {
            return configuration;
        }      
    }
    
    public static class SubtitleStream {
        private final int stream;
        private final String name;
        private final String language;
        private final String format;
        private final boolean forced;
        
        public SubtitleStream(int stream, String name, String language, String format, boolean forced) {
            this.stream = stream;
            this.name = name;
            this.language = language;
            this.format = format;
            this.forced = forced;
        }

        public int getStream() {
            return stream;
        }
        
        public String getName() {
            return name;
        }
        
        public String getLanguage() {
            return language;
        }

        public String getFormat() {
            return format;
        }
        
        public boolean isForced() {
            return forced;
        }       
    }

    public static class MediaElementType {
        public static final byte AUDIO = 0;
        public static final byte VIDEO = 1;
        public static final byte DIRECTORY = 2;
    }
    
    public static class DirectoryMediaType {
        public static final byte AUDIO = 0;
        public static final byte VIDEO = 1;
        public static final byte MIXED = 2;
        public static final byte NONE = 3;
    }
}