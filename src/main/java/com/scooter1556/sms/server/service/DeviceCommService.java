/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.dao.UserDao;
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
