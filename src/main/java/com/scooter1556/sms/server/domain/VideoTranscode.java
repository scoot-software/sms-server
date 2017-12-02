package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.awt.Dimension;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class VideoTranscode {
    private String codec;
    private Dimension resolution;
    private Integer quality;

    public VideoTranscode(String codec, Dimension resolution, Integer quality) {
        this.codec = codec;
        this.resolution = resolution;
        this.quality = quality;
    }

    @Override
    public String toString() {
        return String.format("{Codec=%s, Resolution=%s, Quality=%s}",
                codec == null ? "null" : codec,
                resolution == null ? "null" : String.format("%dx%d", resolution.width, resolution.height),
                quality == null ? "null" : quality.toString());
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
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
