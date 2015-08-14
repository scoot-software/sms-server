/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.domain;

import java.io.Serializable;

/**
 *
 * @author scott2ware
 */
public class UserStats implements Serializable {
    
    private String username;
    private Long streamed;    
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
