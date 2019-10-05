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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@ApiModel(description = "Playlist Content")
public class PlaylistContent implements Serializable {
    
    @ApiModelProperty(value = "ID of the playlist", required = true, example = "8fedcef6-ecd2-4ca0-91af-0da4d6dc452d")
    private UUID id;
    
    @ApiModelProperty(value = "Playlist content", required = true, example = "[ \"813b4c8b-e43e-4bf6-8623-1587f894fd88\", \"820d0924-b9fe-4ff5-926e-e2c186875664\" ]")
    private List<UUID> media;
    
    public PlaylistContent() {};
    
    public PlaylistContent(UUID id, List<UUID> media) {
        this.id = id;
        this.media = media;
    }
    
    @Override
    public String toString() {
        return String.format(
                "Playlist Content[ID=%s, Media Elements=%s]",
                id == null ? "N/A" : id,
                media == null ? "N/A" : media);
    }
    
    public UUID getID()  {
        return id;
    }
    
    public void setID(UUID id) {
        this.id = id;
    }
    
    public List<UUID> getMedia() {
        return this.media;
    }
    
    public void setMedia(List<UUID> media) {
        this.media =  media;
    }
}
