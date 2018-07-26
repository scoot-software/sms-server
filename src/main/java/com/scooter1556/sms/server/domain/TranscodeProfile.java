package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scooter1556.sms.server.encoder.Encoder;
import java.util.Arrays;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TranscodeProfile {
    private UUID id;
    private byte type;
    private MediaElement element;
    private Integer[] formats, codecs, mchCodecs;
    private Integer format, quality, maxBitRate, maxSampleRate = 48000;
    private String url, mimeType, client;
    private Encoder encoder;
    private VideoTranscode[] videoTranscodes;
    private AudioTranscode[] audioTranscodes;
    private SubtitleTranscode[] subtitleTranscodes;
    private Integer videoStream, audioStream, subtitleStream;
    private Integer offset = 0;
    private boolean directPlay = false;
    private boolean active = true;

    public TranscodeProfile() {}

    public TranscodeProfile(UUID id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("TranscodeProfile[ID=%s, Type=%s, %s, Client=%s, Supported Formats=%s, Supported Codecs=%s, Supported Multichannel Codecs=%s, Quality=%s, Max Sample Rate=%s, Max Bit Rate=%s, Encoder=%s, Format=%s, Mime Type=%s, Video Transcodes=%s, Audio Transcodes=%s, Subtitle Transcodes=%s, Video Stream=%s, Audio Stream=%s, Subtitle Stream=%s, Offset=%s, Direct Play=%s",
                id == null ? "null" : id.toString(),
                String.valueOf(type),
                element == null ? "null" : element.toString(),
                client == null ? "null" : client,
                formats == null ? "null" : Arrays.toString(formats),
                codecs == null ? "null" : Arrays.toString(codecs),
                mchCodecs == null ? "null" : Arrays.toString(mchCodecs),
                quality == null ? "null" : quality.toString(),
                maxSampleRate == null ? "null" : maxSampleRate.toString(),
                maxBitRate == null ? "null" : maxBitRate.toString(),
                encoder == null ? "null" : encoder.toString(),
                format == null ? "null" : format,
                mimeType == null ? "null" : mimeType,
                videoTranscodes == null ? "null" : Arrays.toString(videoTranscodes),
                audioTranscodes == null ? "null" : Arrays.toString(audioTranscodes),
                subtitleTranscodes == null ? "null" : Arrays.toString(subtitleTranscodes),
                videoStream == null ? "null" : videoStream.toString(),
                audioStream == null ? "null" : audioStream.toString(),
                subtitleStream == null ? "null" : subtitleStream.toString(),
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

    public MediaElement getMediaElement() {
        return element;
    }

    public void setMediaElement(MediaElement element) {
        this.element = element;
    }

    public boolean isDirectPlayEnabled() {
        return directPlay;
    }

    public void setDirectPlayEnabled(boolean directPlay) {
        this.directPlay = directPlay;
    }

    public Integer[] getFormats() {
        return formats;
    }

    public void setFormats(Integer[] formats) {
        this.formats = formats;
    }

    public Integer[] getCodecs() {
        return codecs;
    }

    public void setCodecs(Integer[] codecs) {
        this.codecs = codecs;
    }

    public Integer[] getMchCodecs() {
        return mchCodecs;
    }

    public void setMchCodecs(Integer[] mchCodecs) {
        this.mchCodecs = mchCodecs;
    }

    @JsonIgnore
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getFormat() {
        return format;
    }

    public void setFormat(Integer format) {
        this.format = format;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }
    
    @JsonIgnore
    public Encoder getEncoder() {
        return encoder;
    }
    
    public void setEncoder(Encoder encoder) {
        this.encoder = encoder;
    }

    public VideoTranscode[] getVideoTranscodes() {
        return videoTranscodes;
    }

    public void setVideoTranscodes(VideoTranscode[] videoTranscodes) {
        this.videoTranscodes = videoTranscodes;
    }

    public AudioTranscode[] getAudioTranscodes() {
        return audioTranscodes;
    }

    public void setAudioTranscodes(AudioTranscode[] audioTranscodes) {
        this.audioTranscodes = audioTranscodes;
    }

    public SubtitleTranscode[] getSubtitleTranscodes() {
        return subtitleTranscodes;
    }

    public void setSubtitleTranscodes(SubtitleTranscode[] subtitleTranscodes) {
        this.subtitleTranscodes = subtitleTranscodes;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    public Integer getVideoStream() {
        return videoStream;
    }

    public void setVideoStream(Integer videoStream) {
        this.videoStream = videoStream;
    }

    public Integer getAudioStream() {
        return audioStream;
    }

    public void setAudioStream(Integer audioStream) {
        this.audioStream = audioStream;
    }

    public Integer getSubtitleStream() {
        return subtitleStream;
    }

    public void setSubtitleStream(Integer subtitleStream) {
        this.subtitleStream = subtitleStream;
    }

    public Integer getMaxSampleRate() {
        return maxSampleRate;
    }

    public void setMaxSampleRate(int maxSampleRate) {
        this.maxSampleRate = maxSampleRate;
    }

    public Integer getMaxBitRate() {
        return maxBitRate;
    }

    public void setMaxBitRate(int maxBitRate) {
        this.maxBitRate = maxBitRate;
    }

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
