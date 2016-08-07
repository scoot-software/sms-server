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
package com.scooter1556.sms.server.service;

import java.io.File;

public class SettingsService {
    
    private static final String CLASS_NAME = "SettingsService";
    
    // SMS Version
    private static final String VERSION = "0.3.10";
    private static final Integer VERSION_INT = 40;
    
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
