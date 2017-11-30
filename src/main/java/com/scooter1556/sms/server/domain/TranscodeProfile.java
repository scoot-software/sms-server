package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TranscodeProfile {
    private UUID id;
    private byte type;
    private MediaElement element;
    private String[] files, codecs, mchCodecs;
    private Integer quality, maxBitRate, maxSampleRate = 48000;
    private String url, format, mimeType, client;
    private VideoTranscode[] videoTranscodes;
    private AudioTranscode[] audioTranscodes;
    private SubtitleTranscode[] subtitleTranscodes;
    private Integer audioTrack, subtitleTrack;
    private Integer offset = 0;
    private boolean directPlay = false;
    private boolean active = true;

    public TranscodeProfile() {}

    public TranscodeProfile(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("TranscodeProfile[ID=%s, Type=%s, MediaElement=%s, Client=%s, Supported Files=%s, Supported Codecs=%s, Supported Multichannel Codecs=%s, Quality=%s, Max Sample Rate=%s, Max Bit Rate=%s, Format=%s, Mime Type=%s, Video Transcodes=%s, Audio Transcodes=%s, Subtitle Transcodes=%s, Audio Track=%s, Subtitle Track=%s, Offset=%s, Direct Play=%s",
                id == null ? "null" : id.toString(),
                String.valueOf(type),
                element == null ? "null" : element.getID().toString(),
                client == null ? "null" : client,
                files == null ? "null" : Arrays.toString(files),
                codecs == null ? "null" : Arrays.toString(codecs),
                mchCodecs == null ? "null" : Arrays.toString(mchCodecs),
                quality == null ? "null" : quality.toString(),
                maxSampleRate == null ? "null" : maxSampleRate.toString(),
                maxBitRate == null ? "null" : maxBitRate.toString(),
                format == null ? "null" : format,
                mimeType == null ? "null" : mimeType,
                videoTranscodes == null ? "null" : Arrays.toString(videoTranscodes),
                audioTranscodes == null ? "null" : Arrays.toString(audioTranscodes),
                subtitleTranscodes == null ? "null" : Arrays.toString(subtitleTranscodes),
                audioTrack == null ? "null" : audioTrack.toString(),
                subtitleTrack == null ? "null" : subtitleTrack.toString(),
                offset == null ? "null" : offset.toString(),
                String.valueOf(directPlay));
    }

    public UUID getID() {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    @JsonIgnore
    public MediaElement getMediaElement() {
        return element;
    }

    public void setMediaElement(MediaElement element) {
        this.element = element;
    }

    @JsonIgnore
    public boolean isDirectPlayEnabled() {
        return directPlay;
    }

    public void setDirectPlayEnabled(boolean directPlay) {
        this.directPlay = directPlay;
    }

    @JsonIgnore
    public String[] getFiles() {
        return files;
    }

    public void setFiles(String[] files) {
        this.files = files;
    }

    @JsonIgnore
    public String[] getCodecs() {
        return codecs;
    }

    public void setCodecs(String[] codecs) {
        this.codecs = codecs;
    }

    @JsonIgnore
    public String[] getMchCodecs() {
        return mchCodecs;
    }

    public void setMchCodecs(String[] mchCodecs) {
        this.mchCodecs = mchCodecs;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @JsonIgnore
    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @JsonIgnore
    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    @JsonIgnore
    public VideoTranscode[] getVideoTranscodes() {
        return videoTranscodes;
    }

    public void setVideoTranscodes(VideoTranscode[] videoTranscodes) {
        this.videoTranscodes = videoTranscodes;
    }

    @JsonIgnore
    public AudioTranscode[] getAudioTranscodes() {
        return audioTranscodes;
    }

    public void setAudioTranscodes(AudioTranscode[] audioTranscodes) {
        this.audioTranscodes = audioTranscodes;
    }

    @JsonIgnore
    public SubtitleTranscode[] getSubtitleTranscodes() {
        return subtitleTranscodes;
    }

    public void setSubtitleTranscodes(SubtitleTranscode[] subtitleTranscodes) {
        this.subtitleTranscodes = subtitleTranscodes;
    }

    @JsonIgnore
    public Integer getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    @JsonIgnore
    public Integer getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(Integer audioTrack) {
        this.audioTrack = audioTrack;
    }

    @JsonIgnore
    public Integer getSubtitleTrack() {
        return subtitleTrack;
    }

    public void setSubtitleTrack(Integer subtitleTrack) {
        this.subtitleTrack = subtitleTrack;
    }

    @JsonIgnore
    public Integer getMaxSampleRate() {
        return maxSampleRate;
    }

    public void setMaxSampleRate(int maxSampleRate) {
        this.maxSampleRate = maxSampleRate;
    }

    @JsonIgnore
    public Integer getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(int maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

    @JsonIgnore
    public Integer getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @JsonIgnore
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public static class StreamType {
        public static final byte DIRECT = 0;
        public static final byte LOCAL = 1;
        public static final byte REMOTE = 2;
    }
}
