/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author scott2ware
 */

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
    private String audioCodec;
    private String audioSampleRate;
    private String audioConfiguration;
    private String audioLanguage;
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
                        String audioCodec,
                        String audioSampleRate,
                        String audioConfiguration,
                        String audioLanguage,
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
        this.audioCodec = audioCodec;
        this.audioSampleRate = audioSampleRate;
        this.audioConfiguration = audioConfiguration;
        this.audioLanguage = audioLanguage;
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
                    "VideoElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Resolution=%sx%s, Video Codec=%s, Audio Language(s)=%s, Audio Codec(s)=%s, Audio Configuration(s)=%s, Subtitles=%s, Subtitle Format(s)=%s, Year=%s, Genre=%s, Rating=%s, Tagline=%s, Description=%s, Certificate=%s, Collection=%s]",
                    id == null ? "?" : id.toString(), title == null ? "N/A" : title, path == null ? "N/A" : path, created == null ? "Unknown" : created.toString(), lastPlayed == null ? "Never" : lastPlayed.toString(), duration.toString(), bitrate.toString(), videoWidth.toString(), videoHeight.toString(), videoCodec == null ? "N/A" : videoCodec, audioLanguage == null ? "N/A" : audioLanguage, audioCodec == null ? "N/A" : audioCodec, audioConfiguration == null ? "N/A" : audioConfiguration, subtitleLanguage == null ? "N/A" : subtitleLanguage, subtitleFormat == null ? "N/A" : subtitleFormat, year.toString(), genre == null ? "N/A" : genre, rating.toString(), tagline == null ? "N/A" : tagline, description == null ? "N/A" : description, certificate == null ? "N/A" : certificate, collection == null ? "N/A" : collection);
            }
            else if(type == MediaElementType.AUDIO)
            {
                return String.format(
                    "AudioElement[ID=%s, Title=%s, Path=%s, Created=%s, LastPlayed=%s, Duration=%s seconds, Bitrate=%s kb/s, Audio Codec(s)=%s, Sample Rate=%s Hz, Audio Configuration(s)=%s, Artist=%s, Album Artist=%s, Album=%s, Track Number=%s, Disc Number=%s, Disc Subtitle=%s, Year=%s, Genre=%s, Description=%s]",
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
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
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
    
    public Integer getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }
    
    public Short getVideoWidth() {
        return videoWidth;
    }
    
    public void setVideoWidth(Short videoWidth) {
        this.videoWidth = videoWidth;
    }
    
    public Short getVideoHeight() {
        return videoHeight;
    }
    
    public void setVideoHeight(Short videoHeight) {
        this.videoHeight = videoHeight;
    }
    
    public String getVideoCodec() {
        return videoCodec;
    }
    
    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }
    
    public String getAudioCodec() {
        return audioCodec;
    }
    
    public void setAudioCodec(String audioCodec) {
        this.audioCodec = audioCodec;
    }
    
    public String getAudioSampleRate() {
        return audioSampleRate;
    }
    
    public void setAudioSampleRate(String audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }
    
    public String getAudioConfiguration() {
        return audioConfiguration;
    }
    
    public void setAudioConfiguration(String audioConfiguration) {
        this.audioConfiguration = audioConfiguration;
    }
    
    public String getAudioLanguage() {
        return audioLanguage;
    }
    
    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }
    
    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }
    
    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }
    
    public String getSubtitleFormat() {
        return subtitleFormat;
    }
    
    public void setSubtitleFormat(String subtitleFormat) {
        this.subtitleFormat = subtitleFormat;
    }
    
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
        this.audioCodec = null;
        this.audioConfiguration = null;
        this.audioLanguage = null;
        this.audioSampleRate = null;
        
        this.subtitleForced = null;
        this.subtitleFormat = null;
        this.subtitleLanguage = null;
    }
    
    @JsonIgnore
    public List<AudioStream> getAudioStreams()
    {
        // Check parameters are available
        if(audioLanguage == null || audioCodec == null || audioSampleRate == null || audioConfiguration == null)
        { 
            return null;
        }
        
        // Retrieve parameters as arrays
        String[] audioLanguages = getAudioLanguage().split(",");
        String[] audioCodecs = getAudioCodec().split(",");
        String[] audioSampleRates = getAudioSampleRate().split(",");
        String[] audioConfigurations = getAudioConfiguration().split(",");
        
        // Get the number of audio streams
        Integer streams = audioLanguages.length;
        
        // Check parameters are present for all streams
        if(audioCodecs.length != streams || audioSampleRates.length != streams || audioConfigurations.length != streams)
        {
            return null;
        }
        
        // Accumalate list of audio streams
        int count = 0;
        List<AudioStream> audioStreams = new ArrayList<>();
        
        while(count < streams)
        {
            audioStreams.add(new AudioStream(count, audioLanguages[count], audioCodecs[count], Integer.parseInt(audioSampleRates[count]), audioConfigurations[count]));
            count ++;
        }
        
        return audioStreams;
    }
    
    @JsonIgnore
    public List<SubtitleStream> getSubtitleStreams()
    {
        // Check parameters are available
        if(subtitleLanguage == null || subtitleFormat == null || subtitleForced == null)
        { 
            return null;
        }
        
        // Retrieve parameters as arrays
        String[] subtitleLanguages = getSubtitleLanguage().split(",");
        String[] subtitleFormats = getSubtitleFormat().split(",");
        String[] subtitleForcedFlags = getSubtitleForced().split(",");
        
        // Get the number of subtitle streams
        Integer streams = subtitleLanguages.length;
        
        // Check parameters are present for all streams
        if(subtitleFormats.length != streams || subtitleForcedFlags.length != streams)
        {
            return null;
        }
        
        // Accumalate list of subtitle streams
        int count = 0;
        List<SubtitleStream> subtitleStreams = new ArrayList<>();
        
        while(count < streams)
        {
            subtitleStreams.add(new SubtitleStream(count, subtitleLanguages[count], subtitleFormats[count], Boolean.parseBoolean(subtitleForcedFlags[count])));
            count ++;
        }
        
        return subtitleStreams;
    }
    
    public static class AudioStream
    {
        private final Integer stream;
        private final String language;
        private final String codec;
        private final Integer sampleRate;
        private final String configuration;
        
        public AudioStream(Integer stream, String language, String codec, Integer sampleRate, String configuration)
        {
            this.stream = stream;
            this.language = language;
            this.codec = codec;
            this.sampleRate = sampleRate;
            this.configuration = configuration;
        }

        public Integer getStream() {
            return stream;
        }
        
        public String getLanguage() {
            return language;
        }

        public String getCodec() {
            return codec;
        }
        
        public Integer getSampleRate() {
            return sampleRate;
        }
        
        public String getConfiguration() {
            return configuration;
        }      
    }
    
    public static class SubtitleStream
    {
        private final Integer stream;
        private final String language;
        private final String format;
        private final Boolean forced;
        
        public SubtitleStream(Integer stream, String language, String format, Boolean forced)
        {
            this.stream = stream;
            this.language = language;
            this.format = format;
            this.forced = forced;
        }

        public Integer getStream() {
            return stream;
        }
        
        public String getLanguage() {
            return language;
        }

        public String getFormat() {
            return format;
        }
        
        public Boolean isForced() {
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