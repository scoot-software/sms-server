package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.util.Arrays;

public class ClientProfile implements Serializable {
    private Integer[] formats, codecs, mchCodecs;
    private String url;
    private Integer client, format, videoQuality, audioQuality, maxBitrate, maxSampleRate = 48000, replaygain;
    private Boolean directPlay = false, local = false;
    
    public void ClientProfile() {} ;
    
    @Override
    public String toString() {
        return String.format("{Client=%s, URL=%s, Format=%s, Supported Formats=%s, Supported Codecs=%s, Supported Multichannel Codecs=%s, Video Quality=%s, Audio Quality=%s, Max Bitrate=%s, Max Sample Rate=%s, Replaygain Mode=%s, Direct Play=%s, Local=%s}",
                client == null ? "null" : client.toString(),
                url == null ? "null" : url,
                format == null ? "null" : format.toString(),
                formats == null ? "null" : Arrays.toString(formats),
                codecs == null ? "null" : Arrays.toString(codecs),
                mchCodecs == null ? "null" : Arrays.toString(mchCodecs),
                videoQuality == null ? "null" : videoQuality.toString(),
                audioQuality == null ? "null" : audioQuality.toString(),
                maxBitrate == null ? "null" : maxBitrate.toString(),
                maxSampleRate == null ? "null" : maxSampleRate.toString(),
                replaygain == null ? "null" : replaygain.toString(),
                directPlay == null ? "null" : directPlay.toString(),
                local == null ? "null" : local.toString()
        );
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
    
    public Integer getClient() {
        return client;
    }

    public void setClient(Integer client) {
        this.client = client;
    }
    
    public Integer getAudioQuality() {
        return audioQuality;
    }

    public void setAudioQuality(int quality) {
        this.audioQuality = quality;
    }
        
    public Integer getVideoQuality() {
        return videoQuality;
    }

    public void setVideoQuality(int quality) {
        this.videoQuality = quality;
    }
    
    public Integer getMaxSampleRate() {
        return maxSampleRate;
    }

    public void setMaxSampleRate(int maxSampleRate) {
        this.maxSampleRate = maxSampleRate;
    }

    public Integer getMaxBitrate() {
        return maxBitrate;
    }

    public void setMaxBitrate(int maxBitrate) {
        this.maxBitrate = maxBitrate;
    }
    
    public Integer getReplaygain() {
        return replaygain;
    }

    public void setReplaygain(int replaygain) {
        this.replaygain = replaygain;
    }
    
    public boolean getDirectPlay() {
        return directPlay;
    }

    public void setDirectPlay(boolean directPlay) {
        this.directPlay = directPlay;
    }
    
    public Boolean getLocal() {
        return local;
    }
    
    public void setLocal(boolean local) {
        this.local = local;
    }
}
