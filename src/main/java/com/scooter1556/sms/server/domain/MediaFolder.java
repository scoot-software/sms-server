/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.domain;

import java.io.Serializable;

/**
 *
 * @author scott2ware
 */
public class MediaFolder implements Serializable {
    
    private Long id;
    private String name;
    private Byte type;
    private String path;    
    private Boolean enabled;


    public MediaFolder() {};
    
    public MediaFolder(Long id, String name, Byte type, String path, boolean enabled)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return String.format(
                "MediaFolder[ID=%s, Name=%s, Type=%s, Path=%s, Enabled=%s]",
                id == null ? "?" : id.toString(), name == null ? "N/A" : name, type == null ? "N/A" : type.toString(), path == null ? "N/A" : path, enabled == null ? "?" : enabled.toString());
    }

    public Long getID()  {
        return id;
    }
    
    public void setID(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Byte getType() {
        return type;
    }
    
    public void setType(Byte type) {
        this.type = type;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public static class ContentType {
        public static final byte AUDIO = 0;
        public static final byte VIDEO = 1;
    }
}
