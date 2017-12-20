package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AudioTranscode {
    private Integer id;
    private String codec;
    private Integer quality, sampleRate;
    private boolean downmix;

    public AudioTranscode(Integer id, String codec, Integer quality, Integer sampleRate, boolean downmix) {
        this.id = id;
        this.codec = codec;
        this.quality = quality;
        this.sampleRate = sampleRate;
        this.downmix = downmix;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Codec=%s, Quality=%s, Sample Rate=%s, Downmix=%s}",
                id == null ? "null" : id.toString(),
                codec == null ? "null" : codec,
                quality == null ? "null" : quality.toString(),
                sampleRate == null ? "null" : sampleRate.toString(),
                String.valueOf(downmix));
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodec() {
        return codec;
    }
    
    public void setCodec(String codec) {
        this.codec = codec;
    }

    public Integer getQuality() {
        return quality;
    }
    
    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }
    
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public boolean isDownmixed() {
        return downmix;
    }
    
    public void setDownmixed(boolean downmix) {
        this.downmix = downmix;
    }
    
    public static class AudioQuality {
        public static final int LOW = 0;
        public static final int MEDIUM = 1;
        public static final int HIGH = 2;
        public static final int LOSSLESS = 3;
        
        public static boolean isValid(int quality) {
            return !(quality > 3 || quality < 0);
        }
    }
}
