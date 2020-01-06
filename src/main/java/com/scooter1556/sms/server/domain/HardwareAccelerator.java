package com.scooter1556.sms.server.domain;

import com.scooter1556.sms.server.SMS;
import org.apache.commons.lang3.ArrayUtils;

public class HardwareAccelerator {
    
    public static final String VENDOR_INTEL = "8086";
    public static final String VENDOR_NVIDIA = "10de";
    
    private Byte type;
    private String device;
    private String oclDevice;
    private boolean streaming = true;
    private int[] dCodecs, eCodecs;
    private byte tonemapping = SMS.Tonemap.NONE;
    
    public HardwareAccelerator(HardwareAccelerator accel) {
        if(accel == null) {
            return;
        }
        
        this.type = accel.getType();
        this.device = accel.getDevice();
        this.oclDevice = accel.getOCLDevice();
        this.streaming = accel.isStreamingSupported();
        this.tonemapping = accel.getTonemapping();
        this.dCodecs = accel.getDecodeCodecs();
        this.eCodecs = accel.getEncodeCodecs();
    }
    
    public HardwareAccelerator(Byte type) { this.type = type; };
    
    @Override
    public String toString() {
        return String.format(
                "{Type=%s, Device=%s, OpenCL Device=%s, Streaming=%s, Tonemapping=%s, Decode=%s, Encode=%s}",
                type == null ? "N/A" : SMS.Accelerator.toString(type),
                device == null ? "N/A" : device,
                oclDevice == null ? "N/A" : oclDevice,
                String.valueOf(streaming),
                SMS.Tonemap.toString(tonemapping),
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
    
    public String getOCLDevice() {
        return oclDevice;
    }
    
    public void setOCLDevice(String oclDevice) {
        this.oclDevice = oclDevice;
    }
    
    public boolean isStreamingSupported() {
        return streaming;
    }
    
    public void setStreamingSupported(boolean streaming) {
        this.streaming = streaming;
    }
    
    public byte getTonemapping() {
        return tonemapping;
    }
    
    public void setTonemapping(byte tonemapping) {
        this.tonemapping = tonemapping;
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
