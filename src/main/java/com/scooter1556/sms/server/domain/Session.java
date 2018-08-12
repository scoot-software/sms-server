package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author scott2ware
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class Session implements Comparable {
    private final UUID id;
    private final String username;
    private ClientProfile profile;
    private final List<Job> jobs = new ArrayList<>();

    public Session(UUID id, String username, ClientProfile profile) {
        this.id = id;
        this.username = username;
        this.profile = profile;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Username=%s, Client Profile=%s}",
                id == null ? "null" : id,
                username == null ? "null" : username,
                profile == null ? "null" : profile);
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
    
    public ClientProfile getClientProfile() {
        return profile;
    }
    
    public void setClientProfile(ClientProfile profile) {
        this.profile = profile;
    }
    
    public void addJob(Job job) {
        jobs.add(job);
    }
    
    public Job[] getJobs() {
        return jobs.toArray(new Job[jobs.size()]);
    }
    
    public Job getJobById(UUID id) {
        for(Job job : jobs) {
            if(job.getId().compareTo(id) == 0) {
                return job;
            }
        }
        
        return null;
    }
     
    public void removeJobById(UUID id) {
        Iterator<Job> iter = jobs.iterator();

        while (iter.hasNext()) {
            Job job = iter.next();

            if(job.getId().compareTo(id) == 0) {
                iter.remove();
                break;
            }
        }
    }
    
    public Job getJobByMediaElementId(UUID id) {
        for(Job job : jobs) {
            if(job.getMediaElement().getID().compareTo(id) == 0) {
                return job;
            }
        }
        
        return null;
    }
     
    public void removeJobByMediaElementId(UUID id) {
        Iterator<Job> iter = jobs.iterator();

        while (iter.hasNext()) {
            Job job = iter.next();

            if(job.getMediaElement().getID().compareTo(id) == 0) {
                iter.remove();
                break;
            }
        }
    }

    @Override
    public int compareTo(Object t) {
        return ((Session) t).getId().compareTo(this.id);
    }
}
