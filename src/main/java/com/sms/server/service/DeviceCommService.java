/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service;

import com.sms.server.dao.UserDao;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */

@Service
public class DeviceCommService {
    
    private static final String CLASS_NAME = "DeviceCommService";
    
    @Autowired
    private UserDao userDao;
    
    @PostConstruct
    public void init() {
    }
    
}
