/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 * @author scott2ware
 */

public final class LogService {
    
    private final String LOG_FILE = SettingsService.getHomeDirectory() + "/log";
    private static final int LOG_BUFFER_SIZE = 50;
    
    private static final List<LogEntry> logEntries = new ArrayList<>();
    
    private boolean enableDebug = false;
    
    public LogService()
    {
        removeLog();
    }
    
    public static final LogService instance = new LogService();
    
    /**
     * Get the current log service.
     * 
     * @return The current instance of LogService.
     */
    public static LogService getInstance() {
        return instance;
    }

    /**
     * Add a new entry to the log.
     * 
     * @param level Level of the new entry.
     * @param category Category this entry belongs to.
     * @param message The message to accompany this entry.
     * @param exception Exception stack trace (Optional).
     */
    public void addLogEntry(Level level, String category, String message, Throwable exception)
    {
        LogEntry entry = new LogEntry(level, category, message);
        
        // If debugging is not enabled and this is a debug entry, ignore it.
        if(entry.getLevel() == Level.DEBUG && !enableDebug)
        {
            return;
        }
        
        // Output to console
        System.out.println(entry.toString());
        
        // Check log buffer size
        if(logEntries.size() == LOG_BUFFER_SIZE)
        {
            logEntries.remove(0);
        }
        
        // Add to our recent entry buffer
        logEntries.add(entry);
        
        // Write to log file
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)));
            out.println(entry.toString());
            
            // Print stack trace if necessary
            if(exception != null)
            {
                StringWriter exceptionStackTrace = new StringWriter();
                exception.printStackTrace(new PrintWriter(exceptionStackTrace));
                out.println(exceptionStackTrace.toString());
            }
            
            out.close();
            
        } catch (IOException e) {
            System.out.println("LogService: Unable to add entry to log file.");
        }
    }
    
    /**
     * Renames the old log file so a new one can be created.
     */
    public void removeLog()
    {
        File log = new File(LOG_FILE);
        
        if(log.exists())
        {
            log.renameTo(new File(LOG_FILE + ".old"));
        }
    }
    
    /**
     * Returns the last few log entries.
     *
     * @return The last few log entries.
     */
    public List<LogEntry> getLatestLogEntries()
    {
        return logEntries;
    }
    
    /**
     * Enables debug logging.
     * 
     * @param enable Enable or disable debugging.
     */
    public void enableDebug(boolean enable)
    {
        enableDebug = enable;
    }
    
    /**
     * Log level
     */
    public enum Level
    {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * Log entry
     */
    public static class LogEntry
    {
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        private final Date date;
        private final Level level;
        private final String category;        
        private final String message;

        public LogEntry(Level level, String category, String message)
        {
            this.date = new Date();
            this.category = category;
            this.level = level;
            this.message = message;
        }

        public String getCategory() {
            return category;
        }

        public Date getDate() {
            return date;
        }

        public Level getLevel() {
            return level;
        }

        public Object getMessage() {
            return message;
        }

        @Override
        public String toString()
        {
            StringBuilder buf = new StringBuilder();
            buf.append(DATE_FORMAT.format(date)).append(" ");
            buf.append(level).append(" ");
            buf.append(category).append(": ");
            buf.append(message);

            return buf.toString();
        }
    }
}
