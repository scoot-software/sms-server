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
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.utilities.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscodeAnalysisStream extends Thread {
    
    private static final String CLASS_NAME = "TranscodeAnalysisStream";
    
    // Patterns
    private static final Pattern FPS = Pattern.compile(".*?fps=\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    
    UUID id;
    String command;
    InputStream stream;
    
    long fps = 0;
    long fpsTotal = 0;
    long fpsReadings = 0;
    
    public TranscodeAnalysisStream(UUID id, String command, InputStream stream) {
        this.id = id;
        this.command = command;
        this.stream = stream;
    }
    
    public long getFps() {
        return fps;
    }
    
    @Override
    public void run() {
        try {
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(streamReader);
            String log = SettingsService.getInstance().getLogDirectory() + "/transcode-" + id + ".log";
            String line;
            
            // Write command to log
            LogUtils.writeToLog(log, command, Level.DEBUG);
            
            while ((line = buffer.readLine()) != null) {
                Matcher matcher;
                
                // Write to log
                LogUtils.writeToLog(log, line, Level.DEBUG);
                
                // FPS
                matcher = FPS.matcher(line);

                if(matcher.find()) {
                    fpsTotal += Integer.valueOf(matcher.group(1));
                    fpsReadings++;
                    
                    // Calculate average fps
                    fps = fpsTotal / fpsReadings;
                }
            }
        } catch (IOException ex) {}
    }
}