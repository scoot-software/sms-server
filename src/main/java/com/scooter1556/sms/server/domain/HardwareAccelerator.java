package com.scooter1556.sms.server.domain;

import java.nio.file.Path;
import org.apache.commons.lang3.ArrayUtils;

public class HardwareAccelerator {
    
    private String name;
    private Path device;
    private boolean streaming = true;
    private int[] dCodecs, eCodecs;
    
    public HardwareAccelerator(HardwareAccelerator accel) {
        if(accel == null) {
            return;
        }
        
        this.name = accel.getName();
        this.device = accel.getDevice();
        this.streaming = accel.isStreamingSupported();
        this.dCodecs = accel.getDecodeCodecs();
        this.eCodecs = accel.getEncodeCodecs();
    }
    
    public HardwareAccelerator(String name) { this.name = name; };
    
    @Override
    public String toString() {
        return String.format(
                "{Name=%s, Device=%s, Streaming=%s, Decode=%s, Encode=%s}",
                name == null ? "N/A" : name,
                device == null ? "N/A" : device.toString(),
                String.valueOf(streaming),
                String.valueOf(ArrayUtils.toString(dCodecs)),
                String.valueOf(ArrayUtils.toString(eCodecs)));
                
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Path getDevice() {
        return device;
    }
    
    public void setDevice(Path device) {
        this.device = device;
    }
    
    public boolean isStreamingSupported() {
        return streaming;
    }
    
    public void setStreamingSupported(boolean streaming) {
        this.streaming = streaming;
    }
    
    public boolean isDecodingSupported() {
        return dCodecs != null && dCodecs.length > 0;
    }
    
    public boolean isDecodeCodecSupported(int codec) {
        return ArrayUtils.contains(dCodecs, codec);
    }
    
    public int[] getDecodeCodecs() {
        return eCodecs;
    }
    
    public void setDecodeCodecs(int[] dCodecs) {
        this.dCodecs = dCodecs;
    }
    
    public boolean isEncodingSupported() {
        return eCodecs != null && eCodecs.length > 0;
    }
    
    public boolean isEncodeCodecSupported(int codec) {
        return ArrayUtils.contains(eCodecs, codec);
    }
    
    public int[] getEncodeCodecs() {
        return eCodecs;
    }
    
    public void setEncodeCodecs(int[] eCodecs) {
        this.eCodecs = eCodecs;
    }
}
