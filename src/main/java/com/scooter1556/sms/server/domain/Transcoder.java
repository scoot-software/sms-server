package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;

public class Transcoder implements Serializable {
    
    private final Path path;
    private Version version;
    private HardwareAccelerator[] hwaccels;
    
    public Transcoder(Path path) {
        this.path = path;
    };
    
    @Override
    public String toString() {
        return String.format(
                "{Path=%s, Version=%s, Hardware Accelerators=%s}",
                path,
                version == null ? "N/A" : version.toString(),
                hwaccels == null ? "N/A" : Arrays.toString(hwaccels));
    }
    
    public Path getPath() {
        return path;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void setVersion(Version version) {
        this.version = version;
    }
    
    public HardwareAccelerator[] getHardwareAccelerators() {
        return hwaccels;
    }
    
    public void setHardwareAccelerators(HardwareAccelerator[] hwaccels) {
        this.hwaccels = hwaccels;
    }
}
