/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service;

import java.io.File;

/**
 *
 * @author scott2ware
 */

public class SettingsService {
    
    private static final String CLASS_NAME = "SettingsService";
    
    // SMS Version
    private static final String VERSION = "0.3.5";
    private static final Integer VERSION_INT = 35;
    
    // SMS home directory
    private static final File HOME_DIRECTORY = new File(System.getProperty( "user.home" ) + "/.sms/server");
    private static File homeDirectory;
    
    /**
    * Returns the SMS home directory.
    *
    * @return The SMS home directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public static synchronized File getHomeDirectory() {

        if (homeDirectory != null) {
            return homeDirectory;
        }

        // Attempt to create home directory if it doesn't exist.
        if (!HOME_DIRECTORY.exists() || !HOME_DIRECTORY.isDirectory())
        {
            boolean success = HOME_DIRECTORY.mkdirs();
            if (success) 
            {
                return HOME_DIRECTORY;
            } 
            else
            {
                System.out.println("The directory " + HOME_DIRECTORY + " does not exist. Please create it and make it writable.");
            }
        } 
        else 
        {
            return HOME_DIRECTORY;
        }

        return null;
    }
    
    public static Integer getVersion()
    {
        return VERSION_INT;
    }
}
