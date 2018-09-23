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

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.ScannerService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private ScannerService scannerService;
    
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
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed role '" + role + "' from user '" + username + "'.", null);
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
        if(mediaFolder.getName() == null || mediaFolder.getPath() == null)
        {
            return new ResponseEntity<>("Missing required parameter.", HttpStatus.BAD_REQUEST);
        }
        
        // Create file for testing
        File file = new File(mediaFolder.getPath());
        
        // Check unique fields
        if(settingsDao.getMediaFolderByPath(file.getPath()) != null)
        {
            return new ResponseEntity<>("Media folder path already exists.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // Check path is readable
        if(!file.isDirectory())
        {
            return new ResponseEntity<>("Media folder path does not exist or is not readable.", HttpStatus.FAILED_DEPENDENCY);
        }
        
        // Ensure path is formatted correctly
        mediaFolder.setPath(file.getPath());
        
        // Generate ID
        mediaFolder.setID(UUID.randomUUID());
        
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
    public void deleteMediaFolder(@PathVariable("id") UUID id) {
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
            // Create file for testing
            File file = new File(update.getPath());
            
            // Check unique fields
            if(settingsDao.getMediaFolderByPath(file.getPath()) != null)
            {
                return new ResponseEntity<>("New media folder path already exists.", HttpStatus.NOT_ACCEPTABLE);
            }

            // Check path is readable
            if(!file.isDirectory())
            {
                return new ResponseEntity<>("New media folder path does not exist or is not readable.", HttpStatus.FAILED_DEPENDENCY);
            }
            
            mediaFolder.setPath(file.getPath());
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
    public void deleteMediaElement(@PathVariable("id") UUID id) {
        mediaDao.removeMediaElement(id);
    }

    @RequestMapping(value="/media/all", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMediaElements() {
        mediaDao.removeAllMediaElements();
    }
    
    @RequestMapping(value="/media/scan", method=RequestMethod.GET)
    public ResponseEntity<String> scanMedia(@RequestParam(value = "id", required = false) UUID id,
                                            @RequestParam(value = "forcerescan", required = false) Boolean forceRescan) {        
        // Check a media scanning process is not already active
        if (scannerService.isScanning()) {
            return new ResponseEntity<>("A scanning process is already running.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // List of media folders to scan
        List<MediaFolder> mediaFolders = new ArrayList<>();
        
        if(id == null) {
            mediaFolders.addAll(settingsDao.getMediaFolders(null));
        } else {
            MediaFolder folder = settingsDao.getMediaFolderByID(id);
            
            if(folder == null) {
                return new ResponseEntity<>("Media folder does not exist.", HttpStatus.BAD_REQUEST);
            } else {
                mediaFolders.add(folder);
            }
        }
        
        // Check we have media folders to scan
        if(mediaFolders.isEmpty()) {
            return new ResponseEntity<>("No media folders to scan for this request.", HttpStatus.NOT_FOUND);
        }
        
        // Reset last scanned timestamp if force rescan is set
        if(forceRescan != null) {
            if(forceRescan) {
                for(MediaFolder folder : mediaFolders) {
                    folder.setLastScanned(null);
                }
            }
        }
        
        // Start scanning media
        scannerService.startMediaScanning(mediaFolders);
        
        return new ResponseEntity<>("Media scanning started.", HttpStatus.OK);
    }
    
    @RequestMapping(value="/media/scan/count", method=RequestMethod.GET)
    public ResponseEntity<Long> getMediaScanCount()
    {   
        return new ResponseEntity<>(scannerService.getScanCount(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/playlist/scan", method=RequestMethod.GET)
    public ResponseEntity<String> scanPlaylist(@RequestParam(value = "id", required = false) UUID id) {        
        // Check a scanning process is not already active
        if (scannerService.isScanning()) {
            return new ResponseEntity<>("A scanning process is already running.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // List of playlists to scan
        List<Playlist> playlists = new ArrayList<>();
        
        if(id == null) {
            playlists.addAll(mediaDao.getPlaylists());
        } else {
            Playlist playlist = mediaDao.getPlaylistByID(id);
            
            if(playlist == null) {
                return new ResponseEntity<>("Playlist does not exist.", HttpStatus.BAD_REQUEST);
            } else {
                playlists.add(playlist);
            }
        }
        
        // Check we have media folders to scan
        if(playlists.isEmpty()) {
            return new ResponseEntity<>("No playlists to scan for this request.", HttpStatus.NOT_FOUND);
        }
        
        // Start scanning playlists
        scannerService.startPlaylistScanning(playlists);
        
        return new ResponseEntity<>("Playlist scanning started.", HttpStatus.OK);
    }
    
    @RequestMapping(value="/deep/scan", method=RequestMethod.GET)
    public ResponseEntity<String> deepScan() {
        // Start scanning playlists
        int status = scannerService.startDeepScan();
        
        // Check status
        switch(status) {
            case SMS.Status.NOT_ALLOWED:
                return new ResponseEntity<>("Deep scan cannot be run at this time.", HttpStatus.NOT_ACCEPTABLE);
                
            case SMS.Status.NOT_REQUIRED:
                return new ResponseEntity<>("No media requires scanning.", HttpStatus.NOT_FOUND);
                
            default:
                return new ResponseEntity<>("Deep scanning started.", HttpStatus.OK);
        }
    }
    
    @RequestMapping(value="/deep/scan/stop", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void deepScanStop() {   
        scannerService.stopDeepScan();
    }
    
    @RequestMapping(value="/deep/scan/count", method=RequestMethod.GET)
    public ResponseEntity<Long> getDeepScanCount()
    {   
        return new ResponseEntity<>(scannerService.getDeepScanCount(), HttpStatus.OK);
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
    
    @RequestMapping(value="/log/level/{value}", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void setLogLevel(@PathVariable("value") byte level) {
        LogService.getInstance().setLogLevel(level);
    }
}
