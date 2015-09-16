/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.service.AdaptiveStreamingService;
import com.scooter1556.sms.server.service.JobService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author scott2ware
 */

@RestController
@RequestMapping(value="/job")
public class JobController {
    
    private static final String CLASS_NAME = "JobController";

    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
    
    @RequestMapping(value="/active", method=RequestMethod.GET)
    public ResponseEntity<List<Job>> getActiveJobs()
    {
        List<Job> jobs = jobDao.getActiveJobs();
        
        if (jobs == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<Job> getJobByID(@PathVariable("id") Long id)
    {
        Job job = jobDao.getJobByID(id);
        
        if (job == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(job, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{username}/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<Job>> getJobsByUsername(@PathVariable("username") String username, @PathVariable("limit") Integer limit)
    {
        List<Job> jobs = jobDao.getJobsByUsername(username, limit);
        
        if (jobs == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}/end", method=RequestMethod.GET)
    public ResponseEntity<String> endJob(@PathVariable("id") Long id)
    {
        Job job = jobDao.getJobByID(id);
        
        if (job == null) {
            return new ResponseEntity<>("Unable to retrieve job with ID: " + id, HttpStatus.NOT_FOUND);
        }
        
        if(job.getType() == Job.JobType.ADAPTIVE_STREAM)
        {
            adaptiveStreamingService.endProcess(job.getID());
        }
        
        jobService.endJob(id);
        
        return new ResponseEntity<>("Ended job with ID: " + id, HttpStatus.OK);
    }
}