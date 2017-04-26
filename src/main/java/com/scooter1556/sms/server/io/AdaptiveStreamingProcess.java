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
import com.scooter1556.sms.server.service.SettingsService;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.apache.tomcat.util.http.fileupload.FileUtils;

public class AdaptiveStreamingProcess extends SMSProcess {
    
    private static final String CLASS_NAME = "AdaptiveStreamingProcess";
    
    int segmentNum = 0;
        
    public AdaptiveStreamingProcess() {};
    
    public AdaptiveStreamingProcess(UUID id) {
        this.id = id;
    }
    
    public void initialise() {
        // Stop transcode process if one is already running
        if(process != null) {
            process.destroy();
        }
        
        File streamDirectory = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id);
        
        try {
            if(streamDirectory.exists()) {
                // Wait for process to finish
                if(process != null) {
                    process.waitFor();
                }
                
                FileUtils.cleanDirectory(streamDirectory);
            } else {
                boolean success = streamDirectory.mkdirs();

                if(!success) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to create directory " + streamDirectory.getPath(), null);
                    return;
                }
            }

            // Reset flags
            ended = false;
        
            // Start transcoding
            start();
        } catch(IOException | InterruptedException ex) {
            if(process != null) {
                process.destroy();
            }
            
            ended = true;
            
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error starting transcoding process.", ex);
        }
    }
    
    @Override
    public void start() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        process = processBuilder.start();
        new NullStream(process.getInputStream()).start();
        new NullStream(process.getErrorStream()).start();
    }
    
    @Override
    public void end() {
        // Stop transcode process
        if(process != null) {
            process.destroy();
        }
        
        // Cleanup working directory
        File streamDirectory = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id);
       
        try {
            // Wait for process to finish
            if(process != null) {
                process.waitFor();
            }
            
            FileUtils.deleteDirectory(streamDirectory);
        } catch (IOException | InterruptedException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to remove working directory for Adaptive Streaming job " + id, ex);
        }
        
        ended = true;
    }
    
    public void setSegmentNum(int num) {
        this.segmentNum = num;
    }
    
    public int getSegmentNum() {
        return this.segmentNum;
    }
}