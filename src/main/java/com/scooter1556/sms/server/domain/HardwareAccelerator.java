package com.scooter1556.sms.server.domain;

import com.scooter1556.sms.server.SMS;
import org.apache.commons.lang3.ArrayUtils;

public class HardwareAccelerator {
    
    private Byte type;
    private String device;
    private boolean streaming = true;
    private int[] dCodecs, eCodecs;
    
    public HardwareAccelerator(HardwareAccelerator accel) {
        if(accel == null) {
            return;
        }
        
        this.type = accel.getType();
        this.device = accel.getDevice();
        this.streaming = accel.isStreamingSupported();
        this.dCodecs = accel.getDecodeCodecs();
        this.eCodecs = accel.getEncodeCodecs();
    }
    
    public HardwareAccelerator(Byte type) { this.type = type; };
    
    @Override
    public String toString() {
        return String.format(
                "{Type=%s, Device=%s, Streaming=%s, Decode=%s, Encode=%s}",
                type == null ? "N/A" : SMS.Accelerator.toString(type),
                device == null ? "N/A" : device,
                String.valueOf(streaming),
                String.valueOf(ArrayUtils.toString(dCodecs)),
                String.valueOf(ArrayUtils.toString(eCodecs)));
                
    }
    
    public Byte getType() {
        return type;
    }
    
    public void setType(Byte type) {
        this.type = type;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
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
