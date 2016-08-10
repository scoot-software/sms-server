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

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Job.JobType;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.UserStats;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class JobService implements DisposableBean {
    
    private static final String CLASS_NAME = "JobService";
    
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private TranscodeService transcodeService;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
    
    private static final Integer INACTIVE_PERIOD_IN_MINUTES = 60;
    
    // Creates a new job in the database and updates media statistics
    public Job createJob(byte type, String username, MediaElement mediaElement) {
        // Check parameters
        if(username == null || mediaElement == null) {
            return null;
        }
        
        // Create new job and add it to database
        Job job = new Job(UUID.randomUUID(), type, username, mediaElement.getID(), null, null, null, null);
        jobDao.createJob(job);
        
        // Update media element and parent media element if necessary
        mediaDao.updateLastPlayed(mediaElement.getID());
        
        MediaElement parentElement = mediaDao.getMediaElementByPath(mediaElement.getParentPath());
        
        if(parentElement != null)
        {
            mediaDao.updateLastPlayed(parentElement.getID());
        }
        
        // Log event
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, getLogMessage(false, job.getType(), job.getUsername(), mediaElement), null);
        
        return job;
    }
    
    // End a job and update user stats
    public void endJob(UUID id) {
        // Retrieve job from database
        Job job = jobDao.getJobByID(id);
        
        // Don't continue if the job cannot be found
        if(job == null) {
            return;
        }
        
        // Do initial cleanup process
        endJob(job);
        
        // Update User Stats
        UserStats userStats = userDao.getUserStatsByUsername(job.getUsername());
        
        if(userStats != null) {
            switch(job.getType()) {
                case JobType.VIDEO_STREAM:
                    userStats.setStreamed(userStats.getStreamed() + job.getBytesTransferred());
                    break;
                    
                case JobType.DOWNLOAD:
                    userStats.setDownloaded(userStats.getDownloaded() + job.getBytesTransferred());
                    break;
            }
            
            // Update database
            userDao.updateUserStats(userStats);
            
            // Log event
            LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, getLogMessage(true, job.getType(), job.getUsername(), mediaDao.getMediaElementByID(job.getMediaElement())), null);
        }
    }
    
    public void endJob(Job job) {
        if(job.getType() == JobType.AUDIO_STREAM || job.getType() == JobType.VIDEO_STREAM) {
            transcodeService.removeTranscodeProfile(job.getID());
        }

        jobDao.updateEndTime(job.getID());
    }
    
    // Check for and end inactive jobs
    private void cleanupJobs() {
        List<Job> jobs = jobDao.getActiveJobs();
        
        // If there are no jobs to process don't bother going any further
        if(jobs == null) {
            return;
        }
        
        // Get current time minus 60 minutes
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -INACTIVE_PERIOD_IN_MINUTES);
        
        // Timestamp to compare to a jobs last activity
        Timestamp inactivePeriod = new java.sql.Timestamp(calendar.getTime().getTime());
            
        // Check for inactive jobs which have not been ended
        for(Job job : jobs) {   
            if(job.getLastActivity().before(inactivePeriod)) {
                endJob(job);
            }
        }
    }
    
    // Check for inactive jobs every 15 minutes
    @Scheduled(fixedDelay=900000)
    private void monitorJobs() {
        cleanupJobs();
    }

    // End all active jobs before the application exits
    @Override
    public void destroy() {
        List<Job> jobs = jobDao.getActiveJobs();
        
        // If there are no jobs to process don't bother going any further
        if(jobs == null) {
            return;
        }
            
        // End all active jobs
        for(Job job : jobs) {   
            endJob(job);
        }
    }
    
    private String getLogMessage(boolean ended, byte type, String username, MediaElement element) {
        
        if(element == null || username == null) {
            return null;
        }
        
        StringBuilder message = new StringBuilder();
        
        message.append(username);
        
        if(ended) {
            message.append(" finished ");
        } else {
           message.append(" started "); 
        }
        
        switch(type) {
            
            case JobType.AUDIO_STREAM: case JobType.VIDEO_STREAM:
                message.append("streaming ");
                break;
                
            case JobType.DOWNLOAD:
                message.append("downloading ");
                break;
                
            default:
                return null;
        }
        
        switch(element.getType()) {
            case MediaElementType.AUDIO:
                message.append("'").append(element.getTitle()).append("'");
                message.append(" by ");
                message.append("'").append(element.getArtist()).append("'");
                break;
                
            case MediaElementType.VIDEO:
                message.append("'").append(element.getTitle()).append("'");
                break;
                
            default:
                return null;
        }
        
        return message.toString();
    }
}
