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
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.JobDatabase;
import com.scooter1556.sms.server.domain.Job;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class JobDao {
    
    @Autowired
    private JobDatabase jobDatabase;
    
    private static final String CLASS_NAME = "JobDao";
    
    public boolean createJob(Job job) {
        try {
            jobDatabase.getJdbcTemplate().update("INSERT INTO Job (ID, Type, Username, MediaElement) " +
                                "VALUES (?,?,?,?)", new Object[] {job.getID(), job.getType(), job.getUsername(), job.getMediaElement()});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean updateEndTime(UUID id) {
        try {
            jobDatabase.getJdbcTemplate().update("UPDATE Job SET EndTime=NOW() WHERE ID=?", 
                                new Object[] {id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public boolean updateBytesTransferred(UUID id, Long bytesTransferred) {
        try {
            jobDatabase.getJdbcTemplate().update("UPDATE Job SET BytesTransferred=?,LastActivity=NOW() WHERE ID=?", 
                                new Object[] {bytesTransferred, id});
        } catch (DataAccessException e) {
            return false;
        }
        
        return true;
    }
    
    public List<Job> getJobs(int limit) {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {limit});
            return jobs;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<Job> getJobsByUsername(String username, int limit) {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE Username=? ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {username, limit});
            return jobs;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<Job> getJobsByType(Byte type, int limit) {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE Type=? ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {type, limit});
            return jobs;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public List<Job> getActiveJobs() {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE EndTime IS NULL", new JobMapper());
            return jobs;
        } catch (DataAccessException e) {
            return null;
        }
    }
    
    public Job getJobByID(UUID id) {
        Job job = null;

        try {
            if(id != null){
                List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE ID=?", new JobMapper(), new Object[] {id});

                if(jobs != null) {
                    if(jobs.size() > 0) {
                        job = jobs.get(0);
                    }
                }
            }
        } catch (DataAccessException e) {
            return null;
        }
        
        return job;
    }
    
    private static final class JobMapper implements RowMapper
    {
        @Override
        public Job mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            Job job = new Job();
            job.setID(UUID.fromString(rs.getString("ID")));
            job.setType(rs.getByte("Type"));
            job.setUsername(rs.getString("Username"));
            job.setMediaElement(rs.getLong("MediaElement"));
            job.setStartTime(rs.getTimestamp("StartTime"));
            job.setEndTime(rs.getTimestamp("EndTime"));
            job.setLastActivity(rs.getTimestamp("LastActivity"));
            job.setBytesTransferred(rs.getLong("BytesTransferred"));
            return job;
        }
    }
}