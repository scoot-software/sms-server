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

import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Session;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.UserStats;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SessionService implements DisposableBean {
    private static final String CLASS_NAME = "SessionService";
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
    
    @Autowired
    private UserDao userDao;
    
    private final ConcurrentSkipListSet<Session> sessions = new ConcurrentSkipListSet<>();
    
    public Session[] getSessions() {
        return sessions.toArray(new Session[sessions.size()]);
    }
    
    public int getNumSessions() {
        return sessions.size();
    }
    
    public int getNumJobs() {
        int jobs = 0;
        
        Iterator<Session> sIter = sessions.iterator();

        while (sIter.hasNext()) {
            Session session = sIter.next();
            jobs += session.getNumJobs();
        }
        
        return jobs;
    }
    
    public UUID addSession(UUID id, String username, ClientProfile profile) {
        // Check required parameters
        if(username == null) {
            return null;
        }
        
        // Generate ID if required
        if(id == null) {
            id = UUID.randomUUID();
        } else {
            // Check session doesn't already exist
            if(isSessionAvailable(id)) {
                return id;
            }
        }
        
        // Create a new session and add it to the list
        Session session = new Session(id, username, profile);
        sessions.add(session);
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "New session created: " + session.toString(), null);
        
        return id;
    }
    
    public void removeSessions(UUID id) {        
        Iterator<Session> sIter = sessions.iterator();

        while (sIter.hasNext()) {
            Session session = sIter.next();

            // Check if we are looking for a specific session 
            if(id != null) {
                if(session.getId().compareTo(id) != 0) {
                    continue;
                }
            }
            
            // Remove session
            endJobs(session, null);
            sIter.remove();            
        }
    }
    
    public void endJobs(Session session, UUID mediaElement) {
        // End jobs associated with sessions
        for(Job job : session.getJobs()) {
            // Check we are handling the correct job
            if(mediaElement != null) {
                if(job.getMediaElement().getID().compareTo(mediaElement) != 0) {
                    continue;
                }
            }
            
            // Stop transcode process
            if(job.getTranscodeProfile().getType() > TranscodeProfile.StreamType.DIRECT) {
                adaptiveStreamingService.endProcess(job.getId());
            }
            
            // Update User Stats
            UserStats userStats = userDao.getUserStatsByUsername(session.getUsername());
        
            if(userStats != null) {
                switch(job.getType()) {
                    case Job.JobType.VIDEO_STREAM: case Job.JobType.AUDIO_STREAM:
                        userStats.setStreamed(userStats.getStreamed() + job.getBytesTransferred());
                        break;

                    case Job.JobType.DOWNLOAD:
                        userStats.setDownloaded(userStats.getDownloaded() + job.getBytesTransferred());
                        break;
                }

                // Update database
                userDao.updateUserStats(userStats);            
            }

            // Remove job from session
            session.removeJobById(job.getId());
        }
    }
    
    public Session getSessionById(UUID id) {
        for(Session session : sessions) {
            if(session.getId().compareTo(id) == 0) {
                return session;
            }
        }
        
        return null;
    }
    
    public boolean isSessionAvailable(UUID id) {
        return getSessionById(id) != null;
    }
    
    // End all active sessions before the application exits
    @Override
    public void destroy() {
        // Check number of sessions
        if(sessions.isEmpty()) {
            return;
        }
            
        // End all sessions
        removeSessions(null);
    }
}
