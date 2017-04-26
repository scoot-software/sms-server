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

public final class LogService {
    
    private final String LOG_FILE = SettingsService.getInstance().getLogDirectory() + "/" + Project.getArtifactId() + ".log";
    private static final int LOG_BUFFER_SIZE = 50;
    
    private final List<LogEntry> logEntries = new ArrayList<>();
    
    private boolean enableDebug = false;
    
    public LogService() {
        removeLog();
    }
    
    private static final LogService INSTANCE = new LogService();
    
    /**
     * Get the current log service.
     * 
     * @return The current instance of LogService.
     */
    public static LogService getInstance() {
        return INSTANCE;
    }

    /**
     * Add a new entry to the log.
     * 
     * @param level Level of the new entry.
     * @param category Category this entry belongs to.
     * @param message The message to accompany this entry.
     * @param exception Exception stack trace (Optional).
     */
    public void addLogEntry(Level level, String category, String message, Throwable exception) {
        
        if(message == null || category == null || level == null) {
            return;
        }
        
        LogEntry entry = new LogEntry(level, category, message);
        
        // If debugging is not enabled and this is a debug entry, ignore it.
        if(entry.getLevel() == Level.DEBUG && !enableDebug) {
            return;
        }
        
        // Output to console
        System.out.println(entry.toString());
        
        // Check log buffer size
        if(logEntries.size() == LOG_BUFFER_SIZE) {
            logEntries.remove(0);
        }
        
        // Add to our recent entry buffer
        logEntries.add(entry);
        
        // Write to log file
        try {
            PrintWriter out;
            out = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)));
            out.println(entry.toString());
            
            // Print stack trace if necessary
            if(exception != null) {
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
    public void removeLog() {
        File log = new File(LOG_FILE);
        
        if(log.exists()) {
            log.renameTo(new File(LOG_FILE + ".old"));
        }
    }
    
    /**
     * Returns the last few log entries.
     *
     * @return The last few log entries.
     */
    public List<LogEntry> getLatestLogEntries() {
        return logEntries;
    }
    
    /**
     * Enables debug logging.
     * 
     * @param enable Enable or disable debugging.
     */
    public void enableDebug(boolean enable) {
        enableDebug = enable;
    }
    
    /**
     * Log level
     */
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    /**
     * Log entry
     */
    public static class LogEntry {
        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        private final Date date;
        private final Level level;
        private final String category;        
        private final String message;

        public LogEntry(Level level, String category, String message) {
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
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(DATE_FORMAT.format(date)).append(" ");
            buf.append(level).append(" ");
            buf.append(category).append(": ");
            buf.append(message);

            return buf.toString();
        }
    }
}
