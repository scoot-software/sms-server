/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author scott2ware
 */

@RestController
@RequestMapping(value="/settings")
public class SettingsController {
    
    private static final String CLASS_NAME = "SettingsController";

    @RequestMapping(value="/version", method=RequestMethod.GET)
    public ResponseEntity<String> getVersion()
    {
        return new ResponseEntity<>(SettingsService.getVersion().toString(), HttpStatus.OK);
    }
}