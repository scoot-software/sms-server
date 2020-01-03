package com.scooter1556.sms.server.domain;

public class GraphicsCard {
    String id;
    String vendor;
    String bus;
    String device;
    String function;
    
    public GraphicsCard() {}
    
    public GraphicsCard(String id,
                        String vendor,
                        String bus,
                        String device,
                        String function) {
        this.id = id;
        this.vendor = vendor;
        this.bus = bus;
        this.device = device;
        this.function = function;
    }
    
    @Override
    public String toString() {
        return String.format(
            "{ID=%s, Vendor=%s, Bus=%s, Device=%s, Function=%s}",
            id == null ? "N/A" : id,
            vendor == null ? "N/A" : vendor,
            bus == null ? "N/A" : bus,
            device == null ? "N/A" : device,
            function == null ? "N/A" : function
        );
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    public String getBus() {
        return bus;
    }
    
    public void setBus(String bus) {
        this.bus = bus;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public String getFunction() {
        return function;
    }
    
    public void setFunction(String function) {
        this.function = function;
    }
    
    // Helper Functions
    
    public String toBDF() {
        return "0000" + ":" + bus + ":" + device + "." + function;
    }
}
