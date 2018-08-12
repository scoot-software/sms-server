package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scooter1556.sms.server.encoder.Encoder;
import java.util.Arrays;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TranscodeProfile {
    private byte type;
    private String mimeType;
    private Encoder encoder;
    private VideoTranscode[] videoTranscodes;
    private AudioTranscode[] audioTranscodes;
    private SubtitleTranscode[] subtitleTranscodes;
    private Integer videoStream, audioStream, subtitleStream;
    private Integer offset = 0;
    private boolean active = true;

    public TranscodeProfile() {}

    @Override
    public String toString() {
        return String.format("TranscodeProfile[Type=%s, Encoder=%s, Mime Type=%s, Video Transcodes=%s, Audio Transcodes=%s, Subtitle Transcodes=%s, Video Stream=%s, Audio Stream=%s, Subtitle Stream=%s, Offset=%s",
                String.valueOf(type),
                encoder == null ? "null" : encoder.toString(),
                mimeType == null ? "null" : mimeType,
                videoTranscodes == null ? "null" : Arrays.toString(videoTranscodes),
                audioTranscodes == null ? "null" : Arrays.toString(audioTranscodes),
                subtitleTranscodes == null ? "null" : Arrays.toString(subtitleTranscodes),
                videoStream == null ? "null" : videoStream.toString(),
                audioStream == null ? "null" : audioStream.toString(),
                subtitleStream == null ? "null" : subtitleStream.toString(),
                offset == null ? "null" : offset.toString()
                );
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

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
