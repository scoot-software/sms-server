package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scooter1556.sms.server.SMS;
import java.awt.Dimension;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class VideoTranscode {
    private Integer id, quality, maxBitrate;
    private Integer oCodec, tCodec;
    private Dimension resolution;
    private Integer transcodeReason = SMS.TranscodeReason.NONE;

    public VideoTranscode(Integer id, Integer oCodec, Integer tCodec, Dimension resolution, Integer quality, Integer maxBitrate, Integer reason) {
        this.id = id;
        this.oCodec = oCodec;
        this.tCodec = tCodec;
        this.resolution = resolution;
        this.quality = quality;
        this.maxBitrate = maxBitrate;
        this.transcodeReason = reason;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Original Codec=%s, Transcode Codec=%s, Resolution=%s, Quality=%s, Max Bitrate=%s, Transcode Reason=%s}",
                id == null ? "null" : id.toString(),
                oCodec == null ? "null" : oCodec,
                tCodec == null ? "null" : tCodec,
                resolution == null ? "null" : String.format("%dx%d", resolution.width, resolution.height),
                quality == null ? "null" : quality.toString(),
                maxBitrate == null ? "null" : maxBitrate,
                transcodeReason == null ? "null" : transcodeReason);
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getOriginalCodec() {
        return oCodec;
    }
    
    public void setOriginalCodec(Integer codec) {
        this.oCodec = codec;
    }

    public Integer getCodec() {
        return tCodec;
    }

    public void setCodec(Integer codec) {
        this.tCodec = codec;
    }

    @JsonIgnore
    public Dimension getResolution() {
        return resolution;
    }

    public void setResolution(Dimension resolution) {
        this.resolution = resolution;
    }
    
    public Integer getQuality() {
        return this.quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
    
    public Integer getMaxBitrate() {
        return this.maxBitrate;
    }

    public void setMaxBitrate(int maxBitrate) {
        this.maxBitrate = maxBitrate;
    }
    
    public Integer getTrandscodeReaon() {
        return this.transcodeReason;
    }
    
    public void setTranscodeReason(int reason) {
        this.transcodeReason = reason;
    }
    
    public static class VideoQuality {
        public static final int LOW = 0;
        public static final int MEDIUM = 1;
        public static final int HIGH = 2;
        public static final int VERY_HIGH = 3;
        public static final int HD = 4;
        public static final int FULLHD = 5;
        
        public static boolean isValid(int quality) {
            return !(quality > 6 || quality < 0);
        }
        
        public static int getMax() {
            return 5;
        }
    }
}
