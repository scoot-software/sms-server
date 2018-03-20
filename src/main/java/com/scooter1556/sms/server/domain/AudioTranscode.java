package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AudioTranscode {
    private Integer id;
    private String codec;
    private Integer bitrate, sampleRate;
    private Integer channels;

    public AudioTranscode(Integer id, String codec, Integer bitrate, Integer sampleRate, Integer channels) {
        this.id = id;
        this.codec = codec;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Codec=%s, Bitrate=%s, Sample Rate=%s, Channels=%s}",
                id == null ? "null" : id.toString(),
                codec == null ? "null" : codec,
                bitrate == null ? "null" : bitrate.toString(),
                sampleRate == null ? "null" : sampleRate.toString(),
                channels == null ? "null" : channels.toString());
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

    public Integer getBitrate() {
        return bitrate;
    }
    
    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }
    
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Integer getChannelCount() {
        return channels;
    }
    
    public void setChannelCount(int channels) {
        this.channels = channels;
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
