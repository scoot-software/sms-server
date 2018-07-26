package com.scooter1556.sms.server.encoder;

import com.scooter1556.sms.server.SMS;
import org.apache.commons.lang3.ArrayUtils;

public class HLSEncoder implements Encoder {
    int[] codecs = {
        SMS.Codec.AVC_BASELINE,
        SMS.Codec.AVC_MAIN,
        SMS.Codec.AVC_HIGH,
        SMS.Codec.AAC,
        SMS.Codec.AC3,
        SMS.Codec.EAC3,
        SMS.Codec.WEBVTT
    };

    @Override
    public boolean isSupported(int codec) {
        return ArrayUtils.contains(codecs, codec);
    }

    @Override
    public int getVideoCodec(Integer[] codecs) {
        if(ArrayUtils.contains(codecs, SMS.Codec.AVC_HIGH)) {
            return SMS.Codec.AVC_HIGH;
        }
        
        if(ArrayUtils.contains(codecs, SMS.Codec.AVC_MAIN)) {
            return SMS.Codec.AVC_MAIN;
        }
        
        if(ArrayUtils.contains(codecs, SMS.Codec.AVC_BASELINE)) {
            return SMS.Codec.AVC_BASELINE;
        }
        
        return SMS.Codec.UNSUPPORTED;
    }

    @Override
    public int getAudioCodec(Integer[] codecs, int channels) {
        if(channels > 2) {
            if(ArrayUtils.contains(codecs, SMS.Codec.EAC3)) {
                return SMS.Codec.EAC3;
            }
            
            if(ArrayUtils.contains(codecs, SMS.Codec.AC3)) {
                return SMS.Codec.AC3;
            }
        }
        
        if(ArrayUtils.contains(codecs, SMS.Codec.AAC)) {
            return SMS.Codec.AAC;
        }
        
        return SMS.Codec.UNSUPPORTED;
    }
    
}
