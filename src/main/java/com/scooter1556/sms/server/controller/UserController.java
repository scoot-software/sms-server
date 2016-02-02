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

import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import com.scooter1556.sms.server.service.LogService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/user")
public class UserController {
    
    @Autowired
    private UserDao userDao;
    
    private static final String CLASS_NAME = "UserController";

    @RequestMapping(value="/{username}", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updateUser(@RequestBody User update, @PathVariable("username") String username)
    {
        User user = userDao.getUserByUsername(username);
        
        if(user == null)
        {
            return new ResponseEntity<>("Username does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        if(username.equals("admin"))
        {
            return new ResponseEntity<>("You are not authenticated to perform this operation.", HttpStatus.FORBIDDEN);
        }
        
        // Update user details
        if(update.getUsername() != null)
        {
            // Check username is available
            if(userDao.getUserByUsername(user.getUsername()) != null)
            {
                return new ResponseEntity<>("Username already exists.", HttpStatus.NOT_ACCEPTABLE);
            }
            else
            {
                user.setUsername(update.getUsername());
            }
        }
        
        if(update.getPassword() != null)
        {
            user.setPassword(update.getPassword());
        }
        
        if(update.getEnabled() != null)
        {
            user.setEnabled(update.getEnabled());
        }
        
        // Update database
        if(!userDao.updateUser(user, username))
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error updating user '" + user.getUsername() + "'.", null);
            return new ResponseEntity<>("Error updating user details.",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "User '" + user.getUsername() + "' updated successfully.", null);
        return new ResponseEntity<>("User details updated successfully.", HttpStatus.ACCEPTED);
    }

    @RequestMapping(value="/{username}/role", method=RequestMethod.GET)
    public ResponseEntity<List<UserRole>> getUserRoles(@PathVariable("username") String username)
    {
        List<UserRole> userRoles = userDao.getUserRolesByUsername(username);
        
        if (userRoles == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userRoles, HttpStatus.OK);
    }

    @RequestMapping(value="/{username}/stats", method=RequestMethod.GET)
    public ResponseEntity<UserStats> getUserStats(@PathVariable("username") String username)
    {
        UserStats userStats = userDao.getUserStatsByUsername(username);
        
        if (userStats == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userStats, HttpStatus.OK);
    }
}