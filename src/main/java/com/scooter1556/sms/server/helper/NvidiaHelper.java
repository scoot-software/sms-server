package com.scooter1556.sms.server.helper;

import com.scooter1556.sms.server.SMS;
import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.NonNull;

public class NvidiaHelper {
    
    public static Integer[] getDecodeCodecs(@NonNull String id) {
        // Check input
        if(id.isEmpty()) {
            return null;
        }
        
        // Create array for codecs
        List<Integer> codecs = new ArrayList<>();
        
        // VP4/5/6
        if(id.startsWith("0a2") ||
           id.startsWith("0a3") ||
           id.startsWith("0a6") ||
           id.startsWith("0a7") ||
           id.startsWith("0be") ||
           id.startsWith("0ca") ||
           id.startsWith("0cb") ||
           id.startsWith("0dc") ||
           id.startsWith("0dd") ||
           id.startsWith("0de") ||
           id.startsWith("0df") ||
           id.startsWith("0e2") ||
           id.startsWith("0e3") ||
           id.startsWith("0f0") ||
           id.startsWith("0fc") ||
           id.startsWith("0fd") ||
           id.startsWith("0fe") ||
           id.startsWith("0ff") ||
           id.startsWith("100") ||
           id.startsWith("101") ||
           id.startsWith("102") ||
           id.startsWith("103") ||
           id.startsWith("104") ||
           id.startsWith("105") ||
           id.startsWith("107") ||
           id.startsWith("108") ||
           id.startsWith("109") ||
           id.startsWith("10c") ||
           id.startsWith("10d") ||
           id.startsWith("114") ||
           id.startsWith("118") ||
           id.startsWith("119") ||
           id.startsWith("11a") ||
           id.startsWith("11b") ||
           id.startsWith("11c") ||
           id.startsWith("11e") ||
           id.startsWith("11f") ||
           id.startsWith("120") ||
           id.startsWith("121") ||
           id.startsWith("124") ||
           id.startsWith("125") ||
           id.startsWith("128") ||
           id.startsWith("129") ||
           id.startsWith("12a") ||
           id.startsWith("12b") ||
           id.startsWith("134") ||
           id.startsWith("137") ||
           id.startsWith("138") ||
           id.startsWith("139") ||
           id.startsWith("13b") ||
           id.startsWith("13c") ||
           id.startsWith("13d") ||
           id.startsWith("13e") ||
           id.startsWith("13f") ||
           id.startsWith("161") ||
           id.startsWith("166") ||
           id.startsWith("174") ||
           id.startsWith("179") ||
           id.startsWith("17c") ||
           id.startsWith("17f")) {
            codecs.add(SMS.Codec.MPEG2);
            codecs.add(SMS.Codec.VC1);
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        // VP7/8/9/10
        if(id.startsWith("140") ||
           id.startsWith("142") ||
           id.startsWith("143") ||
           id.startsWith("15f") ||
           id.startsWith("172") ||
           id.startsWith("1ae") ||
           id.startsWith("1b0") ||
           id.startsWith("1b3") ||
           id.startsWith("1b7") ||
           id.startsWith("1b8") ||
           id.startsWith("1ba") ||
           id.startsWith("1bb") ||
           id.startsWith("1bc") ||
           id.startsWith("1be") ||
           id.startsWith("1c0") ||
           id.startsWith("1c2") ||
           id.startsWith("1c3") ||
           id.startsWith("1c6") ||
           id.startsWith("1c7") ||
           id.startsWith("1c8") ||
           id.startsWith("1c9") ||
           id.startsWith("1ca") ||
           id.startsWith("1cb") ||
           id.startsWith("1cc") ||
           id.startsWith("1d0") ||
           id.startsWith("1d1") ||
           id.startsWith("1d3") ||
           id.startsWith("1d5") ||
           id.startsWith("1d8") ||
           id.startsWith("1db") ||
           id.startsWith("1df") ||
           id.startsWith("1e0") ||
           id.startsWith("1e2") ||
           id.startsWith("1e3") ||
           id.startsWith("1e7") ||
           id.startsWith("1e8") ||
           id.startsWith("1e9") ||
           id.startsWith("1ea") ||
           id.startsWith("1eb") ||
           id.startsWith("1ec") ||
           id.startsWith("1ed") ||
           id.startsWith("1f0") ||
           id.startsWith("1f1") ||
           id.startsWith("1f2") ||
           id.startsWith("1f3") ||
           id.startsWith("1f4") ||
           id.startsWith("1f5") ||
           id.startsWith("1f8") ||
           id.startsWith("1f9") ||
           id.startsWith("1fa") ||
           id.startsWith("1fb") ||
           id.startsWith("218") ||
           id.startsWith("219") ||
           id.startsWith("21a") ||
           id.startsWith("21b") ||
           id.startsWith("21c") ||
           id.startsWith("21d")) {
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
        
        if(id.startsWith("0f0") ||
           id.startsWith("0fc") ||
           id.startsWith("0fd") ||
           id.startsWith("0fe") ||
           id.startsWith("0ff") ||
           id.startsWith("100") ||
           id.startsWith("101") ||
           id.startsWith("102") ||
           id.startsWith("103") ||
           id.startsWith("114") ||
           id.startsWith("118") ||
           id.startsWith("119") ||
           id.startsWith("11a") ||
           id.startsWith("11b") ||
           id.startsWith("11c") ||
           id.startsWith("11e") ||
           id.startsWith("11f") ||
           id.startsWith("138") ||
           id.startsWith("139") ||
           id.startsWith("13b")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
        }
        
        if(id.startsWith("13c") ||
           id.startsWith("13d") ||
           id.startsWith("13e") ||
           id.startsWith("13f") ||
           id.startsWith("140") ||
           id.startsWith("142") ||
           id.startsWith("143") ||
           id.startsWith("161") ||
           id.startsWith("166") ||
           id.startsWith("17c") ||
           id.startsWith("17f")) {
            codecs.add(SMS.Codec.AVC_BASELINE);
            codecs.add(SMS.Codec.AVC_MAIN);
            codecs.add(SMS.Codec.AVC_HIGH);
            codecs.add(SMS.Codec.HEVC_MAIN);
        }
        
        if(id.startsWith("15f") ||
           id.startsWith("172") ||
           id.startsWith("1b0") ||
           id.startsWith("1b3") ||
           id.startsWith("1b7") ||
           id.startsWith("1b8") ||
           id.startsWith("1ba") ||
           id.startsWith("1bb") ||
           id.startsWith("1bc") ||
           id.startsWith("1be") ||
           id.startsWith("1c0") ||
           id.startsWith("1c2") ||
           id.startsWith("1c3") ||
           id.startsWith("1c6") ||
           id.startsWith("1c7") ||
           id.startsWith("1c8") ||
           id.startsWith("1c9") ||
           id.startsWith("1ca") ||
           id.startsWith("1cb") ||
           id.startsWith("1cc") ||
           id.startsWith("1d8") ||
           id.startsWith("1db") ||
           id.startsWith("1df") ||
           id.startsWith("1e0") ||
           id.startsWith("1e2") ||
           id.startsWith("1e3") ||
           id.startsWith("1e7") ||
           id.startsWith("1e8") ||
           id.startsWith("1e9") ||
           id.startsWith("1ea") ||
           id.startsWith("1eb") ||
           id.startsWith("1ec") ||
           id.startsWith("1ed") ||
           id.startsWith("1f0") ||
           id.startsWith("1f1") ||
           id.startsWith("1f2") ||
           id.startsWith("1f3") ||
           id.startsWith("1f4") ||
           id.startsWith("1f5") ||
           id.startsWith("1f8") ||
           id.startsWith("1f9") ||
           id.startsWith("1fa") ||
           id.startsWith("1fb") ||
           id.startsWith("218") ||
           id.startsWith("219") ||
           id.startsWith("21a") ||
           id.startsWith("21b") ||
           id.startsWith("21c") ||
           id.startsWith("21d")) {
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
