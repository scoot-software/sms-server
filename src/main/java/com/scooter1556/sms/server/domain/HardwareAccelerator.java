package com.scooter1556.sms.server.domain;

import java.nio.file.Path;

public class HardwareAccelerator {
    
    private String name;
    private Path device;
    private boolean streaming = true, decode = true, encode = true;
    
    public HardwareAccelerator(HardwareAccelerator accel) {
        if(accel == null) {
            return;
        }
        
        this.name = accel.getName();
        this.device = accel.getDevice();
        this.streaming = accel.isStreamingSupported();
        this.decode = accel.isDecodingSupported();
        this.encode = accel.isEncodingSupported();
    }
    
    public HardwareAccelerator(String name) { this.name = name; };
    
    @Override
    public String toString() {
        return String.format(
                "{Name=%s, Device=%s, Streaming=%s, Decoding=%s, Encoding=%s}",
                name == null ? "N/A" : name,
                device == null ? "N/A" : device.toString(),
                String.valueOf(streaming),
                String.valueOf(decode),
                String.valueOf(encode));
                
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
        return decode;
    }
    
    public void setDecodingSupported(boolean decode) {
        this.decode = decode;
    }
    
    public boolean isEncodingSupported() {
        return encode;
    }
    
    public void setEncodingSupported(boolean encode) {
        this.encode = encode;
    }
}
