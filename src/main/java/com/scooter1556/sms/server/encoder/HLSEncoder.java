package com.scooter1556.sms.server.encoder;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.AudioTranscode;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class HLSEncoder implements Encoder {    
    List<Integer> codecs = new ArrayList<>();
    
    // Client ID
    int client = SMS.Client.NONE;
    
    public HLSEncoder(){
        // Populate default codecs
        codecs.add(SMS.Codec.AVC_BASELINE);
        codecs.add(SMS.Codec.AVC_MAIN);
        codecs.add(SMS.Codec.AVC_HIGH);
        codecs.add(SMS.Codec.AAC);
        codecs.add(SMS.Codec.AC3);
        codecs.add(SMS.Codec.EAC3);
        codecs.add(SMS.Codec.WEBVTT);
    };

    @Override
    public boolean isSupported(int codec) {
        return codecs.contains(codec);
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
    public int getAudioCodec(Integer[] codecs, int channels, int quality) {
        
        switch(client) {
            // For Chromecast the codec needs to be the same for all streams...
            case SMS.Client.CHROMECAST:
                if(ArrayUtils.contains(codecs, SMS.Codec.EAC3)) {
                    return SMS.Codec.EAC3;
                }

                if(ArrayUtils.contains(codecs, SMS.Codec.AC3)) {
                    return SMS.Codec.AC3;
                }

                if(ArrayUtils.contains(codecs, SMS.Codec.AAC)) {
                    return SMS.Codec.AAC;
                }
                
                break;
                
            default:
                // Lossless
                if(quality == AudioTranscode.AudioQuality.LOSSLESS && ArrayUtils.contains(codecs, SMS.Codec.FLAC) && this.codecs.contains(SMS.Codec.FLAC)) {
                    return SMS.Codec.FLAC;
                }
                    
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
                
                break;
        }
        
        return SMS.Codec.UNSUPPORTED;
    }
    
    @Override
    public void setClient(int client) {
        this.client = client;
        
        // Update codecs
        if(client == SMS.Client.KODI) {
            codecs.add(SMS.Codec.FLAC);
        }
    }
    
    @Override
    public int getClient() {
        return client;
    }
}
