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

@ApiModel(description = "User statistics")
public class UserStats implements Serializable {
    
    @ApiModelProperty(value = "Username associated with statistics", example = "smsuser")
    private String username;
    
    @ApiModelProperty(value = "Streaming usage in bytes", example = "2134563")
    private Long streamed;
    
    @ApiModelProperty(value = "Download usage in bytes", example = "4763579")
    private Long downloaded;


    public UserStats() {};
    
    public UserStats(String username, Long streamed, Long downloaded)
    {
        this.username = username;
        this.streamed = streamed;
        this.downloaded = downloaded;
    }
    
    @Override
    public String toString() {
        return String.format(
                "User[Username=%s, Streamed (Bytes)=%s, Downloaded (Bytes)=%s]",
                username == null ? "N/A" : username, streamed == null ? "?" : streamed.toString(), downloaded == null ? "?" : downloaded.toString());
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getStreamed() {
        return streamed;
    }
    
    public void setStreamed(Long streamed) {
        this.streamed = streamed;
    }
    
    public Long getDownloaded() {
        return downloaded;
    }
    
    public void setDownloaded(Long downloaded) {
        this.downloaded = downloaded;
    }
}
