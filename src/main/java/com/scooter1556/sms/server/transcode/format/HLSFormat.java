package com.scooter1556.sms.server.transcode.format;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.AudioTranscode;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class HLSFormat implements Format {    
    List<Integer> codecs = new ArrayList<>();
    
    // Client ID
    int client = SMS.Client.NONE;
    
    public HLSFormat(){
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
    public int getFormat() {
        return SMS.Format.HLS;
    }

    @Override
    public boolean isSupported(int codec) {
        return codecs.contains(codec);
    }

    @Override
    public int getVideoCodec(Integer[] codecs) {
        if(this.codecs.contains(SMS.Codec.HEVC_MAIN) && ArrayUtils.contains(codecs, SMS.Codec.HEVC_MAIN)) {
            return SMS.Codec.HEVC_MAIN;
        }
        
        if(this.codecs.contains(SMS.Codec.AVC_HIGH) && ArrayUtils.contains(codecs, SMS.Codec.AVC_HIGH)) {
            return SMS.Codec.AVC_HIGH;
        }
        
        if(this.codecs.contains(SMS.Codec.AVC_MAIN) && ArrayUtils.contains(codecs, SMS.Codec.AVC_MAIN)) {
            return SMS.Codec.AVC_MAIN;
        }
        
        if(this.codecs.contains(SMS.Codec.AVC_BASELINE) && ArrayUtils.contains(codecs, SMS.Codec.AVC_BASELINE)) {
            return SMS.Codec.AVC_BASELINE;
        }
        
        return SMS.Codec.UNSUPPORTED;
    }

    @Override
    public int getAudioCodec(Integer[] codecs, int channels, int quality) {
        
        switch(client) {
            // For Chromecast the codec needs to be the same for all streams...
            case SMS.Client.CHROMECAST:
                if(this.codecs.contains(SMS.Codec.EAC3) && ArrayUtils.contains(codecs, SMS.Codec.EAC3)) {
                    return SMS.Codec.EAC3;
                }

                if(this.codecs.contains(SMS.Codec.AC3) && ArrayUtils.contains(codecs, SMS.Codec.AC3)) {
                    return SMS.Codec.AC3;
                }

                if(this.codecs.contains(SMS.Codec.AAC) && ArrayUtils.contains(codecs, SMS.Codec.AAC)) {
                    return SMS.Codec.AAC;
                }
                
                break;
                
            default:
                // Lossless
                if(quality == AudioTranscode.AudioQuality.LOSSLESS && ArrayUtils.contains(codecs, SMS.Codec.FLAC) && this.codecs.contains(SMS.Codec.FLAC)) {
                    return SMS.Codec.FLAC;
                }
                    
                if(channels > 2) {
                    if(this.codecs.contains(SMS.Codec.EAC3) && ArrayUtils.contains(codecs, SMS.Codec.EAC3)) {
                        return SMS.Codec.EAC3;
                    }

                    if(this.codecs.contains(SMS.Codec.AC3) && ArrayUtils.contains(codecs, SMS.Codec.AC3)) {
                        return SMS.Codec.AC3;
                    }
                }

                if(this.codecs.contains(SMS.Codec.AAC) && ArrayUtils.contains(codecs, SMS.Codec.AAC)) {
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
        
        // Chromecast doesn't support HEVC in HLS
        if(client != SMS.Client.CHROMECAST) {
            codecs.add(SMS.Codec.HEVC_MAIN);
        }
    }
    
    @Override
    public int getClient() {
        return client;
    }
}
