package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import com.scooter1556.sms.server.transcode.format.Format;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class TranscodeProfile {
    private byte type;
    private String mimeType;
    private Format format;
    private VideoTranscode[] videoTranscodes;
    private AudioTranscode[] audioTranscodes;
    private SubtitleTranscode[] subtitleTranscodes;
    private Integer videoStream, audioStream, subtitleStream;
    private Integer offset = 0;
    private Integer segmentDuration;
    private boolean active = true, packedAudio = false, tonemapping = false;

    public TranscodeProfile() {}

    @Override
    public String toString() {
        return String.format("TranscodeProfile[Type=%s, Format=%s, Mime Type=%s, Video Transcodes=%s, Audio Transcodes=%s, Subtitle Transcodes=%s, Video Stream=%s, Audio Stream=%s, Subtitle Stream=%s, Offset=%s, Segment Duration=%s, Packed Audio=%s, Tonemapping=%s",
                String.valueOf(type),
                format == null ? "null" : format.toString(),
                mimeType == null ? "null" : mimeType,
                videoTranscodes == null ? "null" : Arrays.toString(videoTranscodes),
                audioTranscodes == null ? "null" : Arrays.toString(audioTranscodes),
                subtitleTranscodes == null ? "null" : Arrays.toString(subtitleTranscodes),
                videoStream == null ? "null" : videoStream.toString(),
                audioStream == null ? "null" : audioStream.toString(),
                subtitleStream == null ? "null" : subtitleStream.toString(),
                offset == null ? "null" : offset.toString(),
                segmentDuration == null ? "null" : segmentDuration,
                Boolean.toString(packedAudio),
                Boolean.toString(tonemapping)
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
    public Format getFormat() {
        return format;
    }
    
    public void setFormat(Format format) {
        this.format = format;
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

    public Integer getSegmentDuration() {
        return segmentDuration;
    }

    public void setSegmentDuration(int segmentDuration) {
        this.segmentDuration = segmentDuration;
    }
    
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean getPackedAudio() {
        return packedAudio;
    }

    public void setPackedAudio(boolean packedAudio) {
        this.packedAudio = packedAudio;
    }
    
    public boolean getTonemapping() {
        return tonemapping;
    }

    public void setTonemapping(boolean tonemapping) {
        this.tonemapping = tonemapping;
    }
    
    public static class StreamType {
        public static final byte DIRECT = 0;
        public static final byte LOCAL = 1;
        public static final byte REMOTE = 2;
    }
}
