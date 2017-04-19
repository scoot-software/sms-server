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
import java.io.IOException;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.stereotype.Service;

@Service
public class SettingsService {
    
    private static final String CLASS_NAME = "SettingsService";
    
    // Config File
    public static final String CONFIG_FILE = "config.properties";
    
    // Descriptors
    public static final String AUTHOR = "Scoot Software";
    public static final String NAME = "sms-server-dev";
    public static final String VERSION = "0.3.10";
    public static final Integer VERSION_INT = 40;
    
    // Configuration
    public static final String CONFIG_TRANSCODE_PATH = "transcode.path";
    
    Configuration config;
    
    // Load config
    public SettingsService() {
        Parameters params = new Parameters();
        File configFile = getConfigFile();
        
        // Check config file exists and create it if not
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create configuration file!", ex);
            }
        }
        
        // Load configuration from file
        FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
            new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
            .configure(params.properties()
                .setFile(configFile));
        
        // Automatically save config to file when modified
        builder.setAutoSave(true);

        try {
            config = builder.getConfiguration();
        } catch(ConfigurationException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to load configuration file!", ex);
        }
        
        initialiseConfig();
    }
    
    /**
    * Returns the data directory.
    *
    * @return The data directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public static synchronized File getDataDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File dataDir = new File(dirs.getUserDataDir(NAME, null, AUTHOR));

        // Attempt to create data directory if it doesn't exist.
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            boolean success = dataDir.mkdirs();
            
            if (success) {
                return dataDir;
            }
        } else if(dataDir.canWrite()) {
            return dataDir;
        }

        System.out.println("The directory '" + dataDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.");
        return null;
    }
    
    /**
    * Returns the config directory.
    *
    * @return The config directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public static synchronized File getConfigDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File configDir = new File(dirs.getUserConfigDir(NAME, null, AUTHOR));

        // Attempt to create config directory if it doesn't exist.
        if (!configDir.exists() || !configDir.isDirectory()) {
            boolean success = configDir.mkdirs();
            
            if (success) {
                return configDir;
            }
        } else if(configDir.canWrite()) {
            return configDir;
        }

        System.out.println("The directory '" + configDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.");
        return null;
    }
    
    /**
    * Returns the cache directory.
    *
    * @return The cache directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public static synchronized File getCacheDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File cacheDir = new File(dirs.getUserCacheDir(NAME, null, AUTHOR));

        // Attempt to create cache directory if it doesn't exist.
        if (!cacheDir.exists() || !cacheDir.isDirectory()) {
            boolean success = cacheDir.mkdirs();
            
            if (success) {
                return cacheDir;
            }
        } else if(cacheDir.canWrite()) {
            return cacheDir;
        }

        System.out.println("The directory '" + cacheDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.");
        return null;
    }
    
    /**
    * Returns the log directory.
    *
    * @return The log directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public static synchronized File getLogDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File logDir = new File(dirs.getUserLogDir(NAME, null, AUTHOR));

        // Attempt to create log directory if it doesn't exist.
        if (!logDir.exists() || !logDir.isDirectory()) {
            boolean success = logDir.mkdirs();
            
            if (success) {
                return logDir;
            }
        } else if(logDir.canWrite()) {
            return logDir;
        }

        System.out.println("The directory '" + logDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.");
        return null;
    }
    
    private static File getConfigFile() {
        return new File(getConfigDirectory() + File.separator + CONFIG_FILE);
    }
    
    private void initialiseConfig() {
        if(config == null) {
            return;
        }
        
        if(!config.containsKey(CONFIG_TRANSCODE_PATH)) {
            config.setProperty(CONFIG_TRANSCODE_PATH, "");
        }
    }
    
    //
    // Configuration
    //
    
    public String getTranscodePath() {
        if(config == null) {
            return null;
        }
        
        String value = config.getString(CONFIG_TRANSCODE_PATH);
        
        if(value == null || value.isEmpty()) {
            return null;
        } else {
            return value;
        }
    }
    
    public void setTranscodePath(String value) {
        if(config == null || value == null) {
            return;
        }
        
        config.setProperty(CONFIG_TRANSCODE_PATH, value);
    }

}
