/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

public class MediaFolder implements Serializable {
    
    private UUID id;
    private String name;
    private Byte type;
    private String path; 
    private Long folders;
    private Long files;
    private Timestamp lastScanned;
    private Boolean enabled;


    public MediaFolder() {};
    
    public MediaFolder(UUID id, String name, Byte type, String path, Long folders, Long files, Timestamp lastScanned, boolean enabled)
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

    public UUID getID()  {
        return id;
    }
    
    public void setID(UUID id) {
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
        public static final byte UNKNOWN = 0;
        public static final byte AUDIO = 1;
        public static final byte VIDEO = 2;
        public static final byte PLAYLIST = 3;
    }
}
