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
public class User implements Serializable {
    
    private String username;
    private String password;
    private Boolean enabled;


    public User() {};
    
    public User(String username, String password, boolean enabled)
    {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return String.format(
                "User[Username=%s, Enabled=%s]",
                username == null ? "N/A" : username, enabled == null ? "?" : enabled.toString());
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
