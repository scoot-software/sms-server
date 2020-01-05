package com.scooter1556.sms.server.domain;

import com.scooter1556.sms.server.utilities.OpenCLUtils;

public class OpenCLDevice {
    String vendor;
    Long type;
    Integer platformId;
    Integer deviceId;
    
    public OpenCLDevice() {}
    
    public OpenCLDevice(String vendor,
                        Long type,
                        Integer platformId,
                        Integer deviceId) {
        this.vendor = vendor;
        this.type = type;
        this.platformId = platformId;
        this.deviceId = deviceId;
    }
    
    @Override
    public String toString() {
        return String.format(
            "{Vendor=%s, Type=%s, Platform ID=%s, Device ID=%s}",
            vendor == null ? "N/A" : vendor,
            type == null ? "N/A" : OpenCLUtils.deviceTypeToString(type),
            platformId == null ? "N/A" : platformId.toString(),
            deviceId == null ? "N/A" : deviceId.toString()
        );
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public Long getType() {
        return type;
    }
    
    public void setType(long type) {
        this.type = type;
    }
    
    public Integer getPlatformId() {
        return platformId;
    }
    
    public void setPlatformId(Integer platformId) {
        this.platformId = platformId;
    }
    
    public Integer getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }
    
    // Helper Functions
    
    public String getDescriptor() {
        return platformId + "." + deviceId;
    }
}
