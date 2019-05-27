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

import com.scooter1556.sms.server.database.UserDatabase;
import com.scooter1556.sms.server.database.UserDatabase.UserMapper;
import com.scooter1556.sms.server.database.UserDatabase.UserRoleMapper;
import com.scooter1556.sms.server.database.UserDatabase.UserStatsMapper;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.stereotype.Component;

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
}
