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
public class UserRole implements Serializable {
    
    private String username;    
    private String role;


    public UserRole() {};
    
    public UserRole(String username, String role)
    {
        this.username = username;
        this.role = role;
    }
    
    @Override
    public String toString() {
        return String.format(
                "User[Username=%s, Role=%s]",
                username == null ? "N/A" : username, role == null ? "N/A" : role);
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}
