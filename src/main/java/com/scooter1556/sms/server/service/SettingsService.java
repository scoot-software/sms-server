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

import com.scooter1556.sms.server.Project;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;
import org.springframework.scheduling.support.CronSequenceGenerator;

public final class SettingsService {
    
    private static final String CLASS_NAME = "SettingsService";
    
    // Config File
    public static final String CONFIG_FILE = "config.properties";
    
    // Configuration
    public static final String CONFIG_TRANSCODE_PATH = "transcode.path";
    public static final String CONFIG_PARSER_PATH = "parser.path";
    public static final String CONFIG_DEEPSCAN_SCHEDULE = "deepscan.schedule";
    
    // Default Values
    public static final String DEFAULT_DEEPSCAN_SCHEDULE = "0 0 0 * * *";
    Properties config;
    
    private static final SettingsService INSTANCE = new SettingsService();
    
    /**
     * Get the current settings service.
     * 
     * @return The current instance of SettingService.
     */
    public static SettingsService getInstance() {
        return INSTANCE;
    }
    
    // Load config
    public SettingsService() {
        config = new Properties();
        InputStream input = null;
        File configFile = getConfigFile();
        
        // Load config from file if it exists
        if(configFile.exists()) {
            try {
                input = new FileInputStream(configFile);
		config.load(input);
            } catch(IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to load configuration file!", ex);
            } finally {
		if(input != null) {
                    try { input.close(); } catch(IOException e) {}
		}
            }
        }
        
        initialiseConfig();
    }
    
    /**
    * Returns the data directory.
    *
    * @return The data directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public synchronized File getDataDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File dataDir = new File(dirs.getUserDataDir(Project.getArtifactId(), null, Project.getOrganisation()));

        // Attempt to create data directory if it doesn't exist.
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            boolean success = dataDir.mkdirs();
            
            if (success) {
                return dataDir;
            }
        } else if(dataDir.canWrite()) {
            return dataDir;
        }

        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "The directory '" + dataDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.", null);
        return null;
    }
    
    /**
    * Returns the config directory.
    *
    * @return The config directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public synchronized File getConfigDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File configDir = new File(dirs.getUserConfigDir(Project.getArtifactId(), null, Project.getOrganisation()));

        // Attempt to create config directory if it doesn't exist.
        if (!configDir.exists() || !configDir.isDirectory()) {
            boolean success = configDir.mkdirs();
            
            if (success) {
                return configDir;
            }
        } else if(configDir.canWrite()) {
            return configDir;
        }

        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "The directory '" + configDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.", null);
        return null;
    }
    
    /**
    * Returns the cache directory.
    *
    * @return The cache directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public synchronized File getCacheDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File cacheDir = new File(dirs.getUserCacheDir(Project.getArtifactId(), null, Project.getOrganisation()));

        // Attempt to create cache directory if it doesn't exist.
        if (!cacheDir.exists() || !cacheDir.isDirectory()) {
            boolean success = cacheDir.mkdirs();
            
            if (success) {
                return cacheDir;
            }
        } else if(cacheDir.canWrite()) {
            return cacheDir;
        }

        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "The directory '" + cacheDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.", null);
        return null;
    }
    
    /**
    * Returns the log directory.
    *
    * @return The log directory, if it exists.
    * @throws RuntimeException If directory doesn't exist.
    */
    public synchronized File getLogDirectory() {
        AppDirs dirs = AppDirsFactory.getInstance();
        File logDir = new File(dirs.getUserLogDir(Project.getArtifactId(), null, Project.getOrganisation()));

        // Attempt to create log directory if it doesn't exist.
        if (!logDir.exists() || !logDir.isDirectory()) {
            boolean success = logDir.mkdirs();
            
            if (success) {
                return logDir;
            }
        } else if(logDir.canWrite()) {
            return logDir;
        }

        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "The directory '" + logDir + "' does not exist or is not writable by user '" + System.getProperty("user.name") + "'. Please create it and make it writable.", null);
        return null;
    }
    
    private File getConfigFile() {
        return new File(getConfigDirectory() + File.separator + CONFIG_FILE);
    }
    
    private void initialiseConfig() {
        if(config == null) {
            return;
        }
        
        // Transcode Path
        if(!config.containsKey(CONFIG_TRANSCODE_PATH)) {
            config.setProperty(CONFIG_TRANSCODE_PATH, "");
        }
        
        // Parser Path
        if(!config.containsKey(CONFIG_PARSER_PATH)) {
            config.setProperty(CONFIG_PARSER_PATH, "");
        }
        
        // Deep Scan Schedule
        if(!config.containsKey(CONFIG_DEEPSCAN_SCHEDULE)) {
            config.setProperty(CONFIG_DEEPSCAN_SCHEDULE, DEFAULT_DEEPSCAN_SCHEDULE);
        }
    }
    
    private void saveConfig() {
        if(config == null) {
            return;
        }
        
        OutputStream output = null;

	try {
            output = new FileOutputStream(getConfigFile());
            config.store(output, null);
	} catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to save configuration to file!", ex);
	} finally {
            if(output != null) {
                try { output.close(); } catch(IOException e) {}
            }
	}
    }
    
    //
    // Configuration
    //
    
    public String getTranscodePath() {
        if(config == null) {
            return null;
        }
        
        String value = config.getProperty(CONFIG_TRANSCODE_PATH);
        
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
        
        saveConfig();
    }
    
    public String getParserPath() {
        if(config == null) {
            return null;
        }
        
        String value = config.getProperty(CONFIG_PARSER_PATH);
        
        if(value == null || value.isEmpty()) {
            return null;
        } else {
            return value;
        }
    }
    
    public void setParserPath(String value) {
        if(config == null || value == null) {
            return;
        }
        
        config.setProperty(CONFIG_PARSER_PATH, value);
        
        saveConfig();
    }
    
    public String getDeepScanSchedule() {
        if(config == null) {
            return null;
        }
        
        String value = config.getProperty(CONFIG_PARSER_PATH);
        
        if(value != null && !value.isEmpty()) {
            if(CronSequenceGenerator.isValidExpression(value)) {
                return value;
            } else {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, value + " is not a valid cron expression!", null);
            }
        }
        
        return DEFAULT_DEEPSCAN_SCHEDULE;
    }
    
    public void setDeepScanSchedule(String value) {
        if(config == null || value == null) {
            return;
        }
        
        if(!CronSequenceGenerator.isValidExpression(value)) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, value + " is not a valid cron expression!", null);
            return;
        }

        config.setProperty(CONFIG_DEEPSCAN_SCHEDULE, value);
        saveConfig();
    }
}
