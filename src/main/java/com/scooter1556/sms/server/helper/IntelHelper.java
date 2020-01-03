package com.scooter1556.sms.server.helper;

import com.scooter1556.sms.server.SMS;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;

public class IntelHelper {
    
    public static Integer[] getDecodeCodecs(@NonNull String id) {
        // Check input
        if(id.isEmpty()) {
            return null;
        }
        
        // Create array for codecs
        List<Integer> codecs = new ArrayList<>();
        
        if(id.startsWith("010") ||
           id.startsWith("011") ||
           id.startsWith("012") ||
           id.startsWith("015") ||
           id.startsWith("016") ||
           id.startsWith("017") ||
           id.startsWith("040") ||
           id.startsWith("041") ||
           id.startsWith("0a0") ||
           id.startsWith("0a1") ||
           id.startsWith("0a2") ||
           id.startsWith("0d1") ||
           id.startsWith("0d2") ||
           id.startsWith("0d3") ||
           id.startsWith("160") ||
           id.startsWith("161") ||
           id.startsWith("162") ||
           id.startsWith("163")) {
            codecs.add(SMS.Codec.MPEG2);
            codecs.add(SMS.Codec.VC1);
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        if(id.startsWith("190") ||
           id.startsWith("191") ||
           id.startsWith("192") ||
           id.startsWith("193") ||
           id.startsWith("22b") ||
           id.startsWith("5a8")) {
            codecs.add(SMS.Codec.MPEG2);
            codecs.add(SMS.Codec.VC1);
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
        }
        
        if(id.startsWith("318") ||
           id.startsWith("3e9") ||
           id.startsWith("3ea") ||
           id.startsWith("590") ||
           id.startsWith("591") ||
           id.startsWith("592") ||
           id.startsWith("87c") ||
           id.startsWith("8a5") ||
           id.startsWith("9b4")) {
            codecs.add(SMS.Codec.MPEG2);
            codecs.add(SMS.Codec.VC1);
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
            codecs.add(SMS.Codec.HEVC_MAIN10);
            codecs.add(SMS.Codec.HEVC_HDR10);
        }
        
        // Check we found any supported codecs
        if(codecs.isEmpty()) {
            return null;
        }
        
        return codecs.toArray(new Integer[0]);
    }
    
    public static Integer[] getEncodeCodecs(@NonNull String id) {
        // Check input
        if(id.isEmpty()) {
            return null;
        }
        
        // Create array for codecs
        List<Integer> codecs = new ArrayList<>();
        
        if(id.startsWith("010") ||
           id.startsWith("011") ||
           id.startsWith("012") ||
           id.startsWith("015") ||
           id.startsWith("016") ||
           id.startsWith("017") ||
           id.startsWith("040") ||
           id.startsWith("041") ||
           id.startsWith("0a0") ||
           id.startsWith("0a1") ||
           id.startsWith("0a2") ||
           id.startsWith("160") ||
           id.startsWith("161") ||
           id.startsWith("162") ||
           id.startsWith("163") ||
           id.startsWith("22b")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        else if(id.startsWith("190") ||
                id.startsWith("191") ||
                id.startsWith("192") ||
                id.startsWith("193") ||
                id.startsWith("5a8")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
        }
        
        else if(id.startsWith("318") ||
                id.startsWith("3e9") ||
                id.startsWith("3ea") ||
                id.startsWith("590") ||
                id.startsWith("591") ||
                id.startsWith("592") ||
                id.startsWith("87c") ||
                id.startsWith("8a5") ||
                id.startsWith("9b4")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
            codecs.add(SMS.Codec.HEVC_MAIN10);
            codecs.add(SMS.Codec.HEVC_HDR10);
        }
        
        // Check we found any supported codecs
        if(codecs.isEmpty()) {
            return null;
        }
        
        return codecs.toArray(new Integer[0]);
    }
}
