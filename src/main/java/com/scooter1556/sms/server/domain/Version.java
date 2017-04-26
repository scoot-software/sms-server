package com.scooter1556.sms.server.domain;

import java.io.Serializable;

public class Version implements Serializable {
    
    private short major;
    private short minor;
    private short patch;
    private String extra;
    
    public Version() {};
    
    public Version(short major, short minor, short patch, String extra) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.extra = extra;
    }
    
    public static Version parse(String version) {
        if(version == null || version.isEmpty()) {
            return null;
        }
        
        Version result = new Version();
        
        //  Check for extra
        String[] ext = version.split("-");
        
        if(ext.length == 0) {
            return null;
        } else if(ext.length > 1) {
            result.setExtra(ext[1]);
        }
        
        // Parse version
        String[] num = ext[0].split("\\.");
        
        // We expect at least major and minor
        if(num.length < 2) {
            return null;
        }
        
        // Set major and minor
        try {
            result.setMajor(Short.valueOf(num[0]));
            result.setMinor(Short.valueOf(num[1]));
        } catch(NumberFormatException ex) {
            return null;
        }
        
        if(num.length > 2) {
            result.setPatch(Short.valueOf(num[2]));
        } else {
            result.setPatch((short) 0);
        }
        
        return result;
    }
    
    @Override
    public String toString() {
        return String.format(
                "%d.%d.%d%s",
                major, minor, patch, extra == null ? "" : "-" + extra);
    }
    
    public int toInt() {
        return (major * 100) + (minor * 10) + patch;
    }
    
    public short getMajor() {
        return this.major;
    }
    
    public void setMajor(short major) {
        this.major = major;
    }
    
    public short getMinor() {
        return this.minor;
    }
    
    public void setMinor(short minor) {
        this.minor = minor;
    }
    
    public short getPatch() {
        return this.patch;
    }
    
    public void setPatch(short patch) {
        this.patch = patch;
    }
    
    public String getExtra() {
        return this.extra;
    }
    
    public void setExtra(String extra) {
        this.extra = extra;
    }
}
