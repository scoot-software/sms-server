package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 * @author scott2ware
 */
public class MediaFolder implements Serializable {
    
    private Long id;
    private String name;
    private Byte type;
    private String path; 
    private Long folders;
    private Long files;
    private Timestamp lastScanned;
    private Boolean enabled;


    public MediaFolder() {};
    
    public MediaFolder(Long id, String name, Byte type, String path, Long folders, Long files, Timestamp lastScanned, boolean enabled)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
        this.folders = folders;
        this.files = files;
        this.lastScanned = lastScanned;
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return String.format(
                "MediaFolder[ID=%s, Name=%s, Type=%s, Path=%s, Folders=%s, Files=%s, Enabled=%s]",
                id == null ? "?" : id.toString(), name == null ? "N/A" : name, type == null ? "N/A" : type.toString(), path == null ? "N/A" : path, folders == null ? "N/A" : folders.toString(), files == null ? "N/A" : files.toString(), enabled == null ? "?" : enabled.toString());
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
    
    public Long getFolders()  {
        return folders;
    }
    
    public void setFolders(Long folders) {
        this.folders = folders;
    }
    
    public Long getFiles()  {
        return files;
    }
    
    public void setFiles(Long files) {
        this.files = files;
    }
    
    @JsonIgnore
    public Timestamp getLastScanned() {
        return lastScanned;
    }
    
    public void setLastScanned(Timestamp lastScanned) {
        this.lastScanned = lastScanned;
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
