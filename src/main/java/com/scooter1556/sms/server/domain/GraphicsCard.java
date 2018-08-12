package com.scooter1556.sms.server.domain;

public class GraphicsCard {
    String id;
    String vendor;
    String product;
    String driver;
    
    public GraphicsCard() {}
    
    @Override
    public String toString() {
        return String.format(
            "{ID=%s, Vendor=%s, Product=%s, Driver=%s}",
            id == null ? "N/A" : id,
            vendor == null ? "N/A" : vendor,
            product == null ? "N/A" : product,
            driver == null ? "N/A" : driver
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
    
    public String getProduct() {
        return product;
    }
    
    public void setProduct(String product) {
        this.product = product;
    }
    
    public String getDriver() {
        return driver;
    }
    
    public void setDriver(String driver) {
        this.driver = driver;
    }
}
