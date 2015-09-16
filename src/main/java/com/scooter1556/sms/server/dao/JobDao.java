/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.JobDatabase;
import com.scooter1556.sms.server.domain.Job;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 *
 * @author scott2ware
 */

@Repository
public class JobDao {
    
    @Autowired
    private JobDatabase jobDatabase;
    
    private static final String CLASS_NAME = "JobDao";
    
    public boolean createJob(Job job)
    {
        try
        {
            jobDatabase.getJdbcTemplate().update("INSERT INTO Job (Type, Username, MediaElement) " +
                                "VALUES (?,?,?)", new Object[] {job.getType(), job.getUsername(), job.getMediaElement()});
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public boolean updateEndTime(Long id)
    {
        try
        {
            jobDatabase.getJdbcTemplate().update("UPDATE Job SET EndTime=NOW() WHERE ID=?", 
                                new Object[] {id});
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public boolean updateBytesTransferred(Long id, Long bytesTransferred)
    {
        try
        {
            jobDatabase.getJdbcTemplate().update("UPDATE Job SET BytesTransferred=?,LastActivity=NOW() WHERE ID=?", 
                                new Object[] {bytesTransferred, id});
        }
        catch (InvalidResultSetAccessException e) 
        {
            return false;
        } 
        catch (DataAccessException e)
        {
            return false;
        }
        
        return true;
    }
    
    public List<Job> getJobs(int limit)
    {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {limit});
            return jobs;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<Job> getJobsByUsername(String username, int limit)
    {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE Username=? ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {username, limit});
            return jobs;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<Job> getJobsByType(Byte type, int limit)
    {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE Type=? ORDER BY StartTime DESC LIMIT ?", new JobMapper(), new Object[] {type, limit});
            return jobs;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<Job> getActiveJobs()
    {
        try {
            List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE EndTime IS NULL", new JobMapper());
            return jobs;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public Job getJobByID(Long id)
    {
        Job job = null;

        try
        {
            if(id != null)
            {

                List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE ID=?", new JobMapper(), new Object[] {id});

                if(jobs != null)
                {
                    if(jobs.size() > 0)
                    {
                        job = jobs.get(0);
                    }
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        
        return job;
    }
    
    public Job getLatestJobByUsername(String username)
    {
        Job job = null;

        try
        {
            if(username != null)
            {

                List<Job> jobs = jobDatabase.getJdbcTemplate().query("SELECT * FROM Job WHERE Username=? ORDER BY StartTime DESC LIMIT 1", new JobMapper(), new Object[] {username});

                if(jobs != null)
                {
                    if(jobs.size() > 0)
                    {
                        job = jobs.get(0);
                    }
                }
            }
        }
        catch (DataAccessException e)
        {
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
            job.setID(rs.getLong("ID"));
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