package com.scooter1556.sms.server.domain;

import java.nio.file.Path;

public class HardwareAccelerator {
    
    private String name;
    private Path device;
    private boolean streaming = true;
    
    public HardwareAccelerator(String name) { this.name = name; };
    
    @Override
    public String toString() {
        return String.format(
                "{Name=%s, Device=%s}",
                name == null ? "N/A" : name,
                device == null ? "N/A" : device.toString());
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
}
