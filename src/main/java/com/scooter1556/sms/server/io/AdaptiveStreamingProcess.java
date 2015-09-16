/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.service.AdaptiveStreamingService.AdaptiveStreamingProfile;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SettingsService;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.tomcat.util.http.fileupload.FileUtils;

/**
 *
 * @author scott2ware
 */
public class AdaptiveStreamingProcess extends JobProcess {
    
    private static final String CLASS_NAME = "AdaptiveStreamingProcess";

    AdaptiveStreamingProfile profile;
    
    // Stores the last segment requested to keep track of playback
    Integer segmentNumber = 0;
        
    public AdaptiveStreamingProcess() {};
    
    public AdaptiveStreamingProcess(Long id, List<String> command, Integer segmentNumber, AdaptiveStreamingProfile profile)
    {
        this.id = id;
        this.command = command;
        this.segmentNumber = segmentNumber;
        this.profile = profile;
    }
    
    @Override
    public void start()
    {        
        if (!profile.getOutputDirectory().exists())
        {
            boolean success = profile.getOutputDirectory().mkdirs();
            
            if(!success)
            {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to create directory " + profile.getOutputDirectory().getPath(), null);
                return;
            }
        }
        else
        {
            // Remove any existing segments
            for(File file : profile.getOutputDirectory().listFiles())
            {
                if(file.getName().startsWith("stream"))
                {
                    file.delete();
                }
            }
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try
        {
            process = processBuilder.start();
            new StreamGobbler(process.getInputStream()).start();
            new StreamGobbler(process.getErrorStream()).start();
        }
        catch (IOException ex)
        {
            if(process != null) { process.destroy(); }
            
            ended = true;
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error starting transcoding process.", ex);
        }
    }
    
    @Override
    public void end()
    {
        // Stop transcode process
        if(process != null)
        {
            process.destroy();
        }
        
        // Cleanup working directory
        File streamDirectory = new File(SettingsService.getHomeDirectory().getPath() + "/stream/" + id);
       
        try {
            FileUtils.deleteDirectory(streamDirectory);
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to remove working directory for Adaptive Streaming job " + id, ex);
        }
        
        ended = true;
    }
    
    public void setSegmentNumber(Integer segmentNumber)
    {
        this.segmentNumber = segmentNumber;
    }
    
    public Integer getSegmentNumber()
    {
        return segmentNumber;
    }
    
    public void setProfile(AdaptiveStreamingProfile profile)
    {
        this.profile = profile;
    }
    
    public AdaptiveStreamingProfile getProfile()
    {
        return profile;
    }
}
