package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AudioTranscode {
    private final String codec;
    private final Integer quality;
    private final Integer sampleRate;
    private final boolean downmix;

    public AudioTranscode(String codec, Integer quality, Integer sampleRate, boolean downmix) {
        this.codec = codec;
        this.quality = quality;
        this.sampleRate = sampleRate;
        this.downmix = downmix;
    }

    @Override
    public String toString() {
        return String.format("{Codec=%s, Quality=%s, Sample Rate=%s, Downmix=%s}",
                codec == null ? "null" : codec,
                quality == null ? "null" : quality.toString(),
                sampleRate == null ? "null" : sampleRate.toString(),
                String.valueOf(downmix));
    }

    public String getCodec() {
        return codec;
    }

    public Integer getQuality() {
        return quality;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public boolean isDownmixed() {
        return downmix;
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
