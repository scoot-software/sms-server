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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

@ApiModel(description = "Playlist")
public class Playlist implements Serializable {
    
    @ApiModelProperty(value = "ID of the playlist", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "8fedcef6-ecd2-4ca0-91af-0da4d6dc452d")
    private UUID id;
    
    @ApiModelProperty(value = "Name of the playlist", required = true, example = "Best Of SMS")
    private String name;
    
    @ApiModelProperty(value = "Playlist description", required = false, example = "A collection of the best songs around...")
    private String description;
    
    @ApiModelProperty(value = "Playlist owner", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "smsuser")
    private String username;
    
    @ApiModelProperty(hidden = true)
    private String path;
    
    @ApiModelProperty(hidden = true)
    private String parentPath;
    
    @ApiModelProperty(hidden = true)
    private Timestamp lastScanned;
    
    public Playlist() {};
    
    public Playlist(UUID id,
                    String name,
                    String description,
                    String username,
                    String path,
                    String parentPath,
                    Timestamp lastScanned) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.username = username;
        this.path = path;
        this.parentPath = parentPath;
        this.lastScanned = lastScanned;
    }
    
    @Override
    public String toString() {
        return String.format(
                "Playlist[ID=%s, Name=%s, Description=%s, User=%s, Path=%s, Parent Path=%s, Last Scanned=%s]",
                id == null ? "N/A" : id,
                name == null ? "N/A" : name,
                description == null ? "N/A" : description,
                username == null ? "N/A" : username,
                path == null ? "N/A" : path,
                parentPath == null ? "N/A" : parentPath,
                lastScanned == null ? "N/A" : lastScanned);
    }
    
    public UUID getID()  {
        return id;
    }
    
    public void setID(UUID id) {
        this.id = id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    @JsonIgnore
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @JsonIgnore
    public String getParentPath() {
        return parentPath;
    }
    
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }
    
    @JsonIgnore
    public Timestamp getLastScanned() {
        return lastScanned;
    }
    
    public void setLastScanned(Timestamp lastScanned) {
        this.lastScanned = lastScanned;
    }
}
