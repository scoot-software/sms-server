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
        
        jobService.endJob(id);
        
        return new ResponseEntity<>("Ended job with ID: " + id, HttpStatus.OK);
    }
}