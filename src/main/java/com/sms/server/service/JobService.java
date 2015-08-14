/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service;

import com.sms.server.dao.JobDao;
import com.sms.server.dao.MediaDao;
import com.sms.server.dao.UserDao;
import com.sms.server.domain.Job;
import com.sms.server.domain.Job.JobType;
import com.sms.server.domain.MediaElement;
import com.sms.server.domain.UserStats;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
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
public class JobService {
    
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
        
        mediaElement = mediaDao.getMediaElementByPath(mediaElement.getParentPath());
        
        if(mediaElement != null)
        {
            mediaDao.updateLastPlayed(mediaElement.getID());
        }
        
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
        }
    }
    
    // Check for and end inactive jobs every 15 minutes
    @Scheduled(fixedDelay=900000)
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
}
