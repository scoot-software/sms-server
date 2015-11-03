/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */

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
    private AdaptiveStreamingService adaptiveStreamingService;
    
    private static final Integer INACTIVE_PERIOD_IN_MINUTES = 60;
    
    // Creates a new job in the database and updates media statistics
    public Job createJob(byte type, String username, MediaElement mediaElement)
    {
        // Check parameters
        if(username == null || mediaElement == null)
        {
            return null;
        }
        
        // Create new job in database
        Job job = new Job(null, type, username, mediaElement.getID(), null, null, null, null);
        jobDao.createJob(job);
        job = jobDao.getLatestJobByUsername(username);
        
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
    public void endJob(long id)
    {
        // Retrieve job from database
        Job job = jobDao.getJobByID(id);
        
        // Don't continue if the job cannot be found
        if(job == null) { return; }
        
        // Update end time
        jobDao.updateEndTime(job.getID());
        
        // Update User Stats
        UserStats userStats = userDao.getUserStatsByUsername(job.getUsername());
        
        if(userStats != null)
        {
            switch(job.getType())
            {
                case JobType.ADAPTIVE_STREAM: case JobType.AUDIO_STREAM: case JobType.VIDEO_STREAM:
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
    
    // Check for and end inactive jobs
    private void cleanupJobs()
    {
        List<Job> jobs = jobDao.getActiveJobs();
        
        // If there are no jobs to process don't bother going any further
        if(jobs == null)
        {
            return;
        }
        
        // Get current time minus 60 minutes
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -INACTIVE_PERIOD_IN_MINUTES);
        
        // Timestamp to compare to a jobs last activity
        Timestamp inactivePeriod = new java.sql.Timestamp(calendar.getTime().getTime());
            
        // Check for inactive jobs which have not been ended
        for(Job job : jobs)
        {   
            if(job.getLastActivity().before(inactivePeriod))
            {
                if(job.getType() == JobType.ADAPTIVE_STREAM)
                {
                    adaptiveStreamingService.endProcess(job.getID());
                }
                
                jobDao.updateEndTime(job.getID());
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
        if(jobs == null)
        {
            return;
        }
            
        // End all active jobs
        for(Job job : jobs)
        {   
            if(job.getType() == JobType.ADAPTIVE_STREAM)
            {
                adaptiveStreamingService.endProcess(job.getID());
            }

            jobDao.updateEndTime(job.getID());
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
            
            case JobType.ADAPTIVE_STREAM: case JobType.AUDIO_STREAM: case JobType.VIDEO_STREAM:
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
