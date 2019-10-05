package com.scooter1556.sms.server.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Arrays;

@ApiModel(description = "Client Profile")
public class ClientProfile implements Serializable {
    
    @ApiModelProperty(value = "Formats supported by the client", required = true, allowableValues = "range[1, 18]", example = "[1,2,3]")
    private Integer[] formats;
    
    @ApiModelProperty(value = "Codecs supported by the client", required = true, allowableValues = "10,11,12,13,20,30,31,32,40,1000,1001,1002,1003,1004,1005,1006,1007,1008,1009,1010,1011,2000,2001,2002,2003,2004", example = "[10,1000,2000]")
    private Integer[] codecs;
    
    @ApiModelProperty(value = "Formats supported by the client", required = true, allowableValues = "range[1000, 1011]", example = "[1000,1001]")
    private Integer[] mchCodecs;
    
    @ApiModelProperty(value = "URL the client uses to connect to the server", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "http://mysmsserver.com")
    private String url;
    
    @ApiModelProperty(value = "Client type", required = true, allowableValues = "range[1, 4]", example = "1")
    private Integer client;
    
    @ApiModelProperty(value = "Transcode format", required = true, allowableValues = "range[1, 18]", example = "9")
    private Integer format;
    
    @ApiModelProperty(value = "Video quality setting", required = true, allowableValues = "range[0, 5]", example = "4")
    private Integer videoQuality;
    
    @ApiModelProperty(value = "Audio quality setting", required = true, allowableValues = "range[0, 3]", example = "1")
    private Integer audioQuality;
    
    @ApiModelProperty(value = "Maximum bitrate supported by the client", required = false, allowableValues = "range[0, 3]", example = "1")
    private Integer maxBitrate;
    
    @ApiModelProperty(value = "Maximum sample rate supported by the client", required = false, example = "48000")
    private Integer maxSampleRate = 48000;
    
    @ApiModelProperty(value = "Replaygain setting", required = false, allowableValues = "range[0, 4]", example = "0")
    private Integer replaygain;
    
    @ApiModelProperty(value = "Direct play on the local network", required = false, example = "true")
    private Boolean directPlay = false;
    
    @ApiModelProperty(value = "Whether the device is on the local network", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "true")
    private Boolean local = false;
    
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
