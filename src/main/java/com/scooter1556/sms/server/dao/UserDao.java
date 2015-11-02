/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.dao;

import com.scooter1556.sms.server.database.UserDatabase;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/**
 *
 * @author scott2ware
 */

@Component
public class UserDao {
    
    @Autowired
    private UserDatabase userDatabase;
    
    private static final String CLASS_NAME = "UserDao";
    
    //
    // User
    //
    
    public boolean createUser(User user)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("INSERT INTO User (Username,Password) " +
                                "VALUES (?,?)", new Object[] {user.getUsername(), user.getPassword()});
            
            userDatabase.getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) " +
                                "VALUES (?,?)", new Object[] {user.getUsername(), "ROLE_USER"});
            
            userDatabase.getJdbcTemplate().update("INSERT INTO UserStats (Username) " +
                                "VALUES (?)", new Object[] {user.getUsername()});
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
    
    public boolean removeUser(String username)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("DELETE FROM User WHERE Username=?", username);
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
    
    public boolean updateUser(User user, String username)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("UPDATE User SET Username=?, Password=?, Enabled=? WHERE Username=?", 
                                new Object[] {user.getUsername(), user.getPassword(), user.getEnabled(),
                                username});
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
    
    public List<User> getUsers()
    {
        try {
            List<User> users = userDatabase.getJdbcTemplate().query("SELECT * FROM User", new UserMapper());
            return users;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public User getUserByUsername(String username)
    {
        User user = null;

        try
        {
            if(!username.equals(""))
            {

                List<User> users = userDatabase.getJdbcTemplate().query("SELECT * FROM User WHERE Username=?", new UserMapper(), new Object[] {username});

                if(users != null)
                {
                    if(users.size() > 0)
                    {
                        user = users.get(0);
                    }
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        
        return user;
    }
    
    private static final class UserMapper implements RowMapper
    {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            User user = new User();
            user.setUsername(rs.getString("Username"));
            user.setPassword(rs.getString("Password"));
            user.setEnabled(rs.getBoolean("Enabled"));
            return user;
        }
    }
    
    //
    // User Roles
    //
    
    public boolean createUserRole(UserRole userRole)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("INSERT INTO UserRole (Username,Role) " +
                                "VALUES (?,?)", new Object[] {userRole.getUsername(), userRole.getRole()});
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
    
    public boolean removeUserRole(String username, String role)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("DELETE FROM UserRole WHERE Username=? AND Role=?", username, role);
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
    
    public List<UserRole> getUserRoles()
    {
        try {
            List<UserRole> userRoles = userDatabase.getJdbcTemplate().query("SELECT * FROM UserRole", new UserRoleMapper());
            return userRoles;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public List<UserRole> getUserRolesByUsername(String username)
    {
        try {
            List<UserRole> userRoles = userDatabase.getJdbcTemplate().query("SELECT * FROM UserRole WHERE Username=?", new UserRoleMapper(), new Object[] {username});
            return userRoles;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    private static final class UserRoleMapper implements RowMapper
    {
        @Override
        public UserRole mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            UserRole userRole = new UserRole();
            userRole.setUsername(rs.getString("Username"));
            userRole.setRole(rs.getString("Role"));
            return userRole;
        }
    }
    
    //
    // User Statistics
    //
    
    public boolean updateUserStats(UserStats userStats)
    {
        try
        {
            userDatabase.getJdbcTemplate().update("UPDATE UserStats SET Streamed=?, Downloaded=? WHERE Username=?", 
                                new Object[] {userStats.getStreamed(), userStats.getDownloaded(), userStats.getUsername()});
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
    
    public List<UserStats> getUserStats()
    {
        try {
            List<UserStats> userStats = userDatabase.getJdbcTemplate().query("SELECT * FROM UserStats", new UserStatsMapper());
            return userStats;
        }
        catch (DataAccessException e)
        {
            return null;
        }
    }
    
    public UserStats getUserStatsByUsername(String username)
    {
        UserStats userStats = null;

        try
        {
            if(!username.equals(""))
            {

                List<UserStats> results = userDatabase.getJdbcTemplate().query("SELECT * FROM UserStats WHERE Username=?", new UserStatsMapper(), new Object[] {username});

                if(results != null)
                {
                    if(results.size() > 0)
                    {
                        userStats = results.get(0);
                    }
                }
            }
        }
        catch (DataAccessException e)
        {
            return null;
        }
        
        return userStats;
    }
    
    private static final class UserStatsMapper implements RowMapper
    {
        @Override
        public UserStats mapRow(ResultSet rs, int rowNum) throws SQLException
        {
            UserStats userStats = new UserStats();
            userStats.setUsername(rs.getString("Username"));
            userStats.setStreamed(rs.getLong("Streamed"));
            userStats.setDownloaded(rs.getLong("Downloaded"));
            return userStats;
        }
    }
}
