package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AudioTranscode {
    private Integer id;
    private Integer oCodec, tCodec;
    private Integer bitrate, sampleRate;
    private Integer channels;
    private Float replaygain;

    public AudioTranscode(Integer id, Integer oCodec, Integer tCodec, Integer bitrate, Integer sampleRate, Integer channels, Float replaygain) {
        this.id = id;
        this.oCodec = oCodec;
        this.tCodec = tCodec;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.replaygain = replaygain;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Original Codec=%s, Transcode Codec=%s, Bitrate=%s, Sample Rate=%s, Channels=%s, Replaygain=%s}",
                id == null ? "null" : id.toString(),
                oCodec == null ? "null" : oCodec.toString(),
                tCodec == null ? "null" : tCodec.toString(),
                bitrate == null ? "null" : bitrate.toString(),
                sampleRate == null ? "null" : sampleRate.toString(),
                channels == null ? "null" : channels.toString(),
                replaygain == null ? "null" : replaygain.toString());
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
    
    public void setOriginalCodec(Integer oCodec) {
        this.oCodec = oCodec;
    }
    
    public Integer getCodec() {
        return tCodec;
    }
    
    public void setCodec(Integer tCodec) {
        this.tCodec = tCodec;
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
    
    public Float getReplaygain() {
        return replaygain;
    }
    
    public void setReplaygain(float replaygain) {
        this.replaygain = replaygain;
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
