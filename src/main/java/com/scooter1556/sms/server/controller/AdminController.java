/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.MediaScannerService;
import java.io.File;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author scott2ware
 */

@RestController
@RequestMapping(value="/admin")
public class AdminController {
    
    private static final String CLASS_NAME = "AdminController";
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private MediaScannerService mediaScannerService;
    
    //
    // User
    //
    @RequestMapping(value="/user", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createUser(@RequestBody User user)
    {
        // Check mandatory fields.
        if(user.getUsername() == null || user.getPassword() == null)
        {
            return new ResponseEntity<>("Missing required parameter.", HttpStatus.BAD_REQUEST);
        }
        
        // Check unique fields
        if(userDao.getUserByUsername(user.getUsername()) != null)
        {
            return new ResponseEntity<>("Username already exists.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // Add user to the database.
        if(!userDao.createUser(user))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error adding user '" + user.getUsername() + "' to database.", null);
            return new ResponseEntity<>("Error adding user to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "User '" + user.getUsername() + "' created successfully.", null);
        return new ResponseEntity<>("User created successfully.", HttpStatus.CREATED);
    }
    
    @RequestMapping(value="/user/role", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createUserRole(@RequestBody UserRole userRole)
    {
        // Check mandatory fields.
        if(userRole.getUsername() == null || userRole.getRole() == null)
        {
            return new ResponseEntity<>("Missing required parameter.", HttpStatus.BAD_REQUEST);
        }
        
        // Check unique fields
        if(userDao.getUserByUsername(userRole.getUsername()) == null)
        {
            return new ResponseEntity<>("Username is not registered.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // Add user role to the database.
        if(!userDao.createUserRole(userRole))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error adding role'" + userRole.getRole() + "' to user '" + userRole.getUsername() + "'.", null);
            return new ResponseEntity<>("Error adding user role to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Added role'" + userRole.getRole() + "' to user '" + userRole.getUsername() + "'.", null);
        return new ResponseEntity<>("User role added successfully.", HttpStatus.CREATED);
    }
    
    @RequestMapping(value="/user/{username}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("username") String username) {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed user '" + username + "'.", null);
        userDao.removeUser(username);
    }

    @RequestMapping(value="/user/{username}/role/{role}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRole(@PathVariable("username") String username, @PathVariable("role") String role) {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed role '" + role + "' form user '" + username + "'.", null);
        userDao.removeUserRole(username, role);
    }
    
    @RequestMapping(value="/user", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updateDefaultUser(@RequestBody User update)
    {
        User user = userDao.getUserByUsername("admin");
        
        if(user == null)
        {
            return new ResponseEntity<>("Default user does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        if(update.getPassword() != null)
        {
            user.setPassword(update.getPassword());
        }
        
        // Update database
        if(!userDao.updateUser(user, "admin"))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error updating user '" + user.getUsername() + "'.", null);
            return new ResponseEntity<>("Error updating user details.",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "User '" + user.getUsername() + "' updated successfully.", null);
        return new ResponseEntity<>("User details updated successfully.", HttpStatus.ACCEPTED);
    }
    
    @RequestMapping(value="/user", method=RequestMethod.GET)
    @ResponseBody
    public List<User> getAllUsers() {
        return userDao.getUsers();
    }

    @RequestMapping(value="/user/{username}", method=RequestMethod.GET)
    public ResponseEntity<User> getUser(@PathVariable("username") String username)
    {
        User user = userDao.getUserByUsername(username);
        
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    
    @RequestMapping(value="/user/role", method=RequestMethod.GET)
    public ResponseEntity<List<UserRole>> getUserRoles()
    {
        List<UserRole> userRoles = userDao.getUserRoles();
        
        if (userRoles == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userRoles, HttpStatus.OK);
    }
    
    @RequestMapping(value="/user/stats", method=RequestMethod.GET)
    public ResponseEntity<List<UserStats>> getUserStats()
    {
        List<UserStats> userStats = userDao.getUserStats();
        
        if (userStats == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userStats, HttpStatus.OK);
    }
    
    //
    // Media
    //
    
    @RequestMapping(value="/media/folder", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createMediaFolder(@RequestBody MediaFolder mediaFolder)
    {
        // Check mandatory fields.
        if(mediaFolder.getName() == null || mediaFolder.getType() == null || mediaFolder.getPath() == null)
        {
            return new ResponseEntity<>("Missing required parameter.", HttpStatus.BAD_REQUEST);
        }
        
        // Check unique fields
        if(settingsDao.getMediaFolderByPath(mediaFolder.getPath()) != null)
        {
            return new ResponseEntity<>("Media folder path already exists.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // Check path is readable
        if(!new File(mediaFolder.getPath()).isDirectory())
        {
            return new ResponseEntity<>("Media folder path does not exist or is not readable.", HttpStatus.FAILED_DEPENDENCY);
        }
        
        // Add Media Folder to the database.
        if(!settingsDao.createMediaFolder(mediaFolder))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error adding media folder with path '" + mediaFolder.getPath() + "' to database.", null);
            return new ResponseEntity<>("Error adding media folder to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Media folder with path '" + mediaFolder.getPath() + "' added successfully.", null);
        return new ResponseEntity<>("Media Folder added successfully.", HttpStatus.CREATED);
    }

    @RequestMapping(value="/media/folder/{id}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMediaFolder(@PathVariable("id") Long id) {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed media folder with ID '" + id.toString() + "'.", null);
        settingsDao.removeMediaFolder(id);
    }
    
    @RequestMapping(value="/media/folder", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updateMediaFolder(@RequestBody MediaFolder update)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(update.getID());
        
        if(mediaFolder == null)
        {
            return new ResponseEntity<>("Media folder does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        if(update.getName() != null)
        {
            mediaFolder.setName(update.getName());
        }
        
        if(update.getType() != null)
        {
            mediaFolder.setType(update.getType());
        }
        
        if(update.getPath() != null)
        {
            // Check unique fields
            if(settingsDao.getMediaFolderByPath(update.getPath()) != null)
            {
                return new ResponseEntity<>("New media folder path already exists.", HttpStatus.NOT_ACCEPTABLE);
            }

            // Check path is readable
            if(!new File(update.getPath()).isDirectory())
            {
                return new ResponseEntity<>("New media folder path does not exist or is not readable.", HttpStatus.FAILED_DEPENDENCY);
            }
            
            mediaFolder.setPath(update.getPath());
        }
        
        if(update.getEnabled() != null)
        {
            mediaFolder.setEnabled(update.getEnabled());
        }
        
        // Update database
        if(!settingsDao.updateMediaFolder(mediaFolder))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error updating media folder with ID '" + mediaFolder.getID() + "'.", null);
            return new ResponseEntity<>("Error updating media folder.",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Media folder with ID '" + mediaFolder.getID() + "' updated successfully.", null);
        return new ResponseEntity<>("Media folder updated successfully.", HttpStatus.ACCEPTED);
    }
    
    @RequestMapping(value="/media/{id}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMediaElement(@PathVariable("id") Long id) {
        mediaDao.removeMediaElement(id);
    }

    @RequestMapping(value="/media/all", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMediaElements() {
        mediaDao.removeAllMediaElements();
    }
    
    @RequestMapping(value="/media/scan", method=RequestMethod.GET)
    public ResponseEntity<String> scanMedia(@RequestParam(value = "id", required = false) Long id,
                                            @RequestParam(value = "forcerescan", required = false) Boolean forceRescan)
    {        
        if (mediaScannerService.isScanning()) {
            return new ResponseEntity<>("Media is already being scanned.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        if(id != null) {
            if(settingsDao.getMediaFolderByID(id) == null) {
                return new ResponseEntity<>("Media folder does not exist.", HttpStatus.BAD_REQUEST);
            }
        }
        
        if(forceRescan != null) {
            if(forceRescan) {
                settingsDao.forceRescan(id);
            }
        }
        
        // Start scanning media
        mediaScannerService.startScanning(id);
        
        return new ResponseEntity<>("Media scanning started.", HttpStatus.OK);
    }
    
    @RequestMapping(value="/media/scan/count", method=RequestMethod.GET)
    public ResponseEntity<Long> getMediaScanCount()
    {   
        return new ResponseEntity<>(mediaScannerService.getScanCount(), HttpStatus.OK);
    }
    
    //
    // Job
    //
    
    @RequestMapping(value="/job/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<Job>> getJobs(@PathVariable("limit") Integer limit)
    {
        List<Job> jobs = jobDao.getJobs(limit);
        
        if (jobs == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }
    
    //
    // Log
    //
    
    @RequestMapping(value="/log",method=RequestMethod.GET)
    public ResponseEntity<List<LogService.LogEntry>> getLogEntries()
    { 
        if (LogService.getInstance().getLatestLogEntries() == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(LogService.getInstance().getLatestLogEntries(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/log/enabledebug", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void enableDebug() {
        LogService.getInstance().enableDebug(true);
    }
    
    @RequestMapping(value="/log/disabledebug", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void disableDebug() {
        LogService.getInstance().enableDebug(false);
    }
}
