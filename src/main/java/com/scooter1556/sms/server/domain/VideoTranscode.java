package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.awt.Dimension;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class VideoTranscode {
    private Integer id, quality, maxBitrate;
    private Integer codec;
    private Dimension resolution;

    public VideoTranscode(Integer id, Integer codec, Dimension resolution, Integer quality, Integer maxBitrate) {
        this.id = id;
        this.codec = codec;
        this.resolution = resolution;
        this.quality = quality;
        this.maxBitrate = maxBitrate;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Codec=%s, Resolution=%s, Quality=%s, Max Bitrate=%s}",
                id == null ? "null" : id.toString(),
                codec == null ? "null" : codec,
                resolution == null ? "null" : String.format("%dx%d", resolution.width, resolution.height),
                quality == null ? "null" : quality.toString(),
                maxBitrate == null ? "null" : maxBitrate);
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCodec() {
        return codec;
    }

    public void setCodec(Integer codec) {
        this.codec = codec;
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
