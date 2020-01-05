
package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Transcoder implements Serializable {
    
    private final Path path;
    private Version version;
    private HardwareAccelerator[] hwaccels;
    private Integer[] decoders;
    private Integer[] encoders;
    private boolean zscale = false;
    private boolean npp = false;
    
    public Transcoder(Path path) {
        this.path = path;
    };
    
    @Override
    public String toString() {
        return String.format(
                "{Path=%s, Version=%s, Hardware Accelerators=%s, Decoders=%s, Encoders=%s, zscale=%s, NPP=%s}",
                path,
                version == null ? "N/A" : version.toString(),
                hwaccels == null ? "N/A" : Arrays.toString(hwaccels),
                decoders == null ? "N/A" : Arrays.toString(decoders),
                encoders == null ? "N/A" : Arrays.toString(encoders),
                String.valueOf(zscale),
                String.valueOf(npp)
        );
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
    
    public List<HardwareAccelerator> getHardwareAcceleratorOptions(boolean streaming) {
        if(hwaccels == null) {
            return null;
        }
        
        List<HardwareAccelerator> result = new ArrayList<>();
                
        for(HardwareAccelerator accelerator : hwaccels) {
            // Check the accelerator supports bitrate limiting
            if(streaming) {
                if(!accelerator.isStreamingSupported()) {
                    continue;
                }
            }
            
            // Add hardware accelerator
            if(accelerator.isDecodingSupported() || accelerator.isEncodingSupported()) {
                result.add(new HardwareAccelerator(accelerator));
            }
        }
        
        return result;
    }
    
    public Integer[] getDecoders() {
        return decoders;
    }
    
    public void setDecoders(Integer[] decoders) {
        this.decoders = decoders;
    }
    
    public boolean isDecoderSupported(int decoder) {
        if(decoders == null || decoders.length == 0) {
            return false;
        }
        
        for(int test : decoders) {
            if(decoder == test) {
                return true;
            }
        }
        
        return false;
    }
    
    public Integer[] getEncoders() {
        return encoders;
    }
    
    public void setEncoders(Integer[] encoders) {
        this.encoders = encoders;
    }
    
    public boolean isEncoderSupported(int encoder) {
        if(encoders == null || encoders.length == 0) {
            return false;
        }
        
        for(int test : encoders) {
            if(encoder == test) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean hasZscale() {
        return zscale;
    }
    
    public void setZscale(boolean zscale) {
        this.zscale = zscale;
    }
    
    public boolean hasNPP() {
        return npp;
    }
    
    public void setNPP(boolean npp) {
        this.npp = npp;
    }
}
