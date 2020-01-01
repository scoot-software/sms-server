package com.scooter1556.sms.server.helper;

import com.scooter1556.sms.server.SMS;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;

public class NvidiaHelper {
    
    public static Integer[] getDecodeCodecs(@NonNull String product) {
        // Check input
        if(product.isEmpty()) {
            return null;
        }
        
        // Process input
        product = product.trim().toLowerCase();
        
        // Create array for codecs
        List<Integer> codecs = new ArrayList<>();
        
        // VP4/5/6
        if(product.contains("gt21") ||
           product.contains("gf1") ||
           product.contains("gk1") ||
           product.contains("gk2") ||
           product.contains("gm10") ||
           product.contains("gm20")) {
            codecs.add(SMS.Codec.MPEG2);
            codecs.add(SMS.Codec.VC1);
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        // GM206
        if(product.contains("gm206")) {
            codecs.add(SMS.Codec.HEVC_MAIN);
            codecs.add(SMS.Codec.HEVC_MAIN10);
            codecs.add(SMS.Codec.HEVC_HDR10);
        }
        
        // VP7/8/9/10
        if(product.contains("gp10") ||
           product.contains("gv10") ||
           product.contains("tu1")) {
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
    
    public static Integer[] getEncodeCodecs(@NonNull String product) {
        // Check input
        if(product.isEmpty()) {
            return null;
        }
        
        // Process input
        product = product.trim().toLowerCase();
        
        // Create array for codecs
        List<Integer> codecs = new ArrayList<>();
        
        // GK10x, GM107
        if(product.contains("gk1") ||
           product.contains("gk2") ||
           product.contains("gm107")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        // GM20x
        if(product.contains("gm20")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
        }
        
        // GP10x
        if((product.contains("gp10") && !product.contains("gp108")) ||
            product.contains("gv10") ||
            product.contains("tu1")) {
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
