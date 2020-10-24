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
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.domain.User;
import com.scooter1556.sms.server.domain.UserStats;
import com.scooter1556.sms.server.domain.UserRole;
import com.scooter1556.sms.server.domain.UserRule;
import com.scooter1556.sms.server.domain.UserRuleRequest;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.ScannerService;
import com.scooter1556.sms.server.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private UserService userService;
    
    @Autowired
    private ScannerService scannerService;
    
    //
    // User
    //
    @ApiOperation(value = "Create a new user")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Successfully created new user"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Reqired parameter is missing"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "Username already exists"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to add new user to database")
    })
    @RequestMapping(value="/user", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createUser(
            @ApiParam(value = "User to create", required = true) @RequestBody User user)
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
        
        // Encrypt password
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        
        // Add user to the database.
        if(!userDao.createUser(user))
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error adding user '" + user.getUsername() + "' to database.", null);
            return new ResponseEntity<>("Error adding user to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "User '" + user.getUsername() + "' created successfully.", null);
        return new ResponseEntity<>("User created successfully.", HttpStatus.CREATED);
    }
    
    @ApiOperation(value = "Update user roles")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Successfully updated user roles"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Required parameter is missing"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "Username doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to update user roles")
    })
    @RequestMapping(value="/user/role", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createUserRole(
            @ApiParam(value = "Role to add to user", required = true) @RequestBody UserRole userRole)
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
    
    @ApiOperation(value = "Delete user")
    @RequestMapping(value="/user/{username}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @ApiParam(value = "Username of user to be deleted", required = true) @PathVariable("username") String username) {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed user '" + username + "'.", null);
        userDao.removeUser(username);
    }

    @ApiOperation(value = "Remove role from user")
    @RequestMapping(value="/user/{username}/role/{role}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserRole(
            @ApiParam(value = "Username of user for which role is to be removed", required = true) @PathVariable("username") String username,
            @ApiParam(value = "Role to remove from user", required = true) @PathVariable("role") String role) {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed role '" + role + "' from user '" + username + "'.", null);
        userDao.removeUserRole(username, role);
    }
    
    @ApiOperation(value = "Update the default 'admin' user")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Successfully updated default user"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Default user does not exist"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to update default user")
    })
    @RequestMapping(value="/user", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updateDefaultUser(
            @ApiParam(value = "Updated user details", required = true) @RequestBody User update)
    {
        User user = userDao.getUserByUsername("admin");
        
        if(user == null)
        {
            return new ResponseEntity<>("Default user does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        if(update.getPassword() != null)
        {
            user.setPassword(new BCryptPasswordEncoder().encode(update.getPassword()));
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
    
    @ApiOperation(value = "Get all users")
    @RequestMapping(value="/user", method=RequestMethod.GET)
    @ResponseBody
    public List<User> getAllUsers() {
        return userDao.getUsers();
    }

    @ApiOperation(value = "Get user")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully returned user"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "User does not exist")
    })
    @RequestMapping(value="/user/{username}", method=RequestMethod.GET)
    public ResponseEntity<User> getUser(
            @ApiParam(value = "Username of the user to retrieve", required = true) @PathVariable("username") String username)
    {
        User user = userDao.getUserByUsername(username);
        
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get roles associated with all users")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully returned user roles"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No user roles found")
    })
    @RequestMapping(value="/user/role", method=RequestMethod.GET)
    public ResponseEntity<List<UserRole>> getUserRoles()
    {
        List<UserRole> userRoles = userDao.getUserRoles();
        
        if (userRoles == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userRoles, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get statistics for all users")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully returned user statistics"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No user statistics available")
    })
    @RequestMapping(value="/user/stats", method=RequestMethod.GET)
    public ResponseEntity<List<UserStats>> getUserStats()
    {
        List<UserStats> userStats = userDao.getUserStats();
        
        if (userStats == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userStats, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get rules associated with user(s)")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully returned user rules"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No user rules found")
    })
    @RequestMapping(value="/user/rule", method=RequestMethod.GET)
    public ResponseEntity<List<UserRule>> getUserRules(
            @ApiParam(value = "Username", required = false) @RequestParam(value = "username", required = false) String username)
    {
        List<UserRule> userRules = null;
        
        if(username == null) {
            userRules = userDao.getUserRules();
        } else {
            userRules = userDao.getUserRulesByUsername(username);
        }
        
        if (userRules == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(userRules, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Update user rules")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully updated user rules"),
        @ApiResponse(code = HttpServletResponse.SC_EXPECTATION_FAILED, message = "Required data is missing or invalid"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Username provided is invalid"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Unable to add rules for administrators"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to update user rules")
    })
    @RequestMapping(value="/user/rule", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createUserRule(
            @ApiParam(value = "User rule", required = true) @RequestBody UserRuleRequest userRule)
    {
        // Check required parameters
        if(userRule.getUsername() == null || userRule.getID() == null || userRule.getRule() == null || !SMS.Rule.isValid(userRule.getRule())) {
            return new ResponseEntity<>("Required data is missing or invalid.", HttpStatus.EXPECTATION_FAILED);
        }
        
        // Check user
        User user = userDao.getUserByUsername(userRule.getUsername());
        if(user == null) {
            return new ResponseEntity<>("User doesn't exist.", HttpStatus.BAD_REQUEST);
        }
        
        // Check user roles
        List<UserRole> roles = userDao.getUserRolesByUsername(userRule.getUsername());
        
        // Check for ADMIN role
        for(UserRole role : roles) {
            if(role.getRole().equals("ADMIN")) {
                return new ResponseEntity<>("Cannot add rules for administrators.", HttpStatus.FORBIDDEN);
            }
        }

        // Fetch media folder, element or playlist
        String path = null;

        if(userRule.getFolder() == null) {
            userRule.setFolder(false);
        }

        if(userRule.getPlaylist() == null) {
            userRule.setPlaylist(false);
        }

        if(userRule.getFolder()) {
            MediaFolder folder = settingsDao.getMediaFolderByID(userRule.getID());
            
            if(folder == null) {
                return new ResponseEntity<>("Media folder not found.", HttpStatus.NOT_FOUND);
            }
            
            path = folder.getPath();
        } else if(userRule.getPlaylist()) {
            Playlist playlist = mediaDao.getPlaylistByID(userRule.getID());

            if(playlist == null) {
                return new ResponseEntity<>("Playlist not found.", HttpStatus.NOT_FOUND);
            }

            if(playlist.getPath() == null) {
                return new ResponseEntity<>("Playlist not supported.", HttpStatus.NOT_FOUND);
            }

            path = playlist.getPath();
        } else {
            MediaElement mediaElement = mediaDao.getMediaElementByID(userRule.getID());

            if(mediaElement == null) {
                return new ResponseEntity<>("Media element not found.", HttpStatus.NOT_FOUND);
            }

            path = mediaElement.getPath();
        }
        
        // Remove existing matching rules
        userDao.removeUserRule(userRule.getUsername(), path);
        
        if(!userDao.createUserRule(new UserRule(userRule.getUsername(), path, userRule.getRule()))) {
            return new ResponseEntity<>("Failed to add user rule to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Added rule for user '" + userRule.getUsername() + "': Path=" + path + " Rule="+ SMS.Rule.toString(userRule.getRule()), null);
        return new ResponseEntity<>("User rule added successfully.", HttpStatus.OK);
    }
    
    @ApiOperation(value = "Remove user rule(s)")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully removed user rule(s)"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media element or folder not found")
    })
    @RequestMapping(value="/user/{username}/rule/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<String> deleteUserRule(
            @ApiParam(value = "Username", required = true) @PathVariable("username") String username,
            @ApiParam(value = "ID of media element or folder (or 'all')", required = true) @PathVariable("id") String id,
            @ApiParam(value = "Whether provided ID is a media folder", required = false) @RequestParam(value = "folder", required = false) Boolean isFolder)
    {
        // Remove all user rules
        if(id.equals("all")) {
            userDao.removeUserRuleByUsername(username);
            return new ResponseEntity<>("Successfully removed all rules for user.", HttpStatus.OK);
        }
        
        // Get media element or folder
        if(isFolder == null) {
            isFolder = false;
        }

        if(isFolder) {
            MediaFolder folder = settingsDao.getMediaFolderByID(UUID.fromString(id));
            
            if(folder == null) {
                return new ResponseEntity<>("Media folder not found.", HttpStatus.NOT_FOUND);
            }
            
            userDao.removeUserRule(username, folder.getPath());
        } else {
            MediaElement mediaElement = mediaDao.getMediaElementByID(UUID.fromString(id));
            
            if(mediaElement == null) {
                return new ResponseEntity<>("Media element not found.", HttpStatus.NOT_FOUND);
            }
            
            userDao.removeUserRule(username, mediaElement.getPath());
        }
        
        return new ResponseEntity<>("Successfully removed user rule(s).", HttpStatus.OK);
    }
    
    @ApiOperation(value = "Cleanup user rules")
    @RequestMapping(value="/user/rule/clean", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void cleanupUserRules() {
        // Begin pruning user rules
        userService.pruneUserRules();
    }
    
    //
    // Media
    //
    
    @ApiOperation(value = "Add a new media folder")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Successfully added media folder"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Required parameter is missing"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "A media folder with the same path already exists"),
        @ApiResponse(code = HttpServletResponse.SC_EXPECTATION_FAILED, message = "The path given does not exist or is not readable"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to add media folder")
    })
    @RequestMapping(value="/media/folder", method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createMediaFolder(
            @ApiParam(value = "Media folder to add", required = true) @RequestBody MediaFolder mediaFolder)
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
            return new ResponseEntity<>("Media folder path does not exist or is not readable.", HttpStatus.EXPECTATION_FAILED);
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

    @ApiOperation(value = "Remove media folder")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Successfully removed media folder"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media folder not found")
    })
    @RequestMapping(value="/media/folder/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<String> deleteMediaFolder(
            @ApiParam(value = "ID of the media folder", required = true) @PathVariable("id") UUID id)
    {
        LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Removed media folder with ID '" + id.toString() + "'.", null);
        
        // Fetch media folder
        MediaFolder folder = settingsDao.getMediaFolderByID(id);
        
        if(folder == null) {
            return new ResponseEntity<>("Media Folder not found.", HttpStatus.NOT_FOUND);
        }
        
        // Remove media folder from database
        settingsDao.removeMediaFolder(id);
                
        return new ResponseEntity<>("Media Folder removed successfully.", HttpStatus.OK);
    }
    
    @ApiOperation(value = "Update a media folder")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_ACCEPTED, message = "Successfully updated media folder"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Media folder doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "A media folder with the same path already exists"),
        @ApiResponse(code = HttpServletResponse.SC_EXPECTATION_FAILED, message = "The path given does not exist or is not readable"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to update media folder")
    })
    @RequestMapping(value="/media/folder", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updateMediaFolder(
            @ApiParam(value = "Updated media folder details", required = true) @RequestBody MediaFolder update)
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
    
    @ApiOperation(value = "Remove media element")
    @RequestMapping(value="/media/{id}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMediaElement(
            @ApiParam(value = "ID of the media element", required = true) @PathVariable("id") UUID id) {
        mediaDao.removeMediaElement(id);
    }

    @ApiOperation(value = "Remove all media elements")
    @RequestMapping(value="/media/all", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMediaElements() {
        mediaDao.removeAllMediaElements();
    }
    
    @ApiOperation(value = "Start a media scan")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media scan started"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Specified media folder doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "A scanning process is already running"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media folders found")
    })
    @RequestMapping(value="/media/scan", method=RequestMethod.GET)
    public ResponseEntity<String> scanMedia(
            @ApiParam(value = "Specify ID of media folder to scan", required = false) @RequestParam(value = "id", required = false) UUID id,
            @ApiParam(value = "Rescan all media regardless of whether it has changed on the filesystem", required = false) @RequestParam(value = "forcerescan", required = false) Boolean forceRescan) {        
        // Check a media scanning process is not already active
        if (scannerService.isScanning()) {
            return new ResponseEntity<>("A scanning process is already running.", HttpStatus.NOT_ACCEPTABLE);
        }
        
        // List of media folders to scan
        List<MediaFolder> mediaFolders = new ArrayList<>();
        
        if(id == null) {
            mediaFolders = settingsDao.getMediaFolders(null);
        } else {
            MediaFolder folder = settingsDao.getMediaFolderByID(id);
            
            if(folder == null) {
                return new ResponseEntity<>("Media folder does not exist.", HttpStatus.BAD_REQUEST);
            } else {
                mediaFolders.add(folder);
            }
        }
        
        // Check we have media folders to scan
        if(mediaFolders == null || mediaFolders.isEmpty()) {
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
    
    @ApiOperation(value = "Get current media scan count")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Scan count returned successfully")
    })
    @RequestMapping(value="/media/scan/count", method=RequestMethod.GET)
    public ResponseEntity<Long> getMediaScanCount()
    {   
        return new ResponseEntity<>(scannerService.getScanCount(), HttpStatus.OK);
    }
    
    @ApiOperation(value = "Start a playlist scan")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Playlist scan started"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Specified playlist doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "A scanning process is already running"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No playlists found")
    })
    @RequestMapping(value="/playlist/scan", method=RequestMethod.GET)
    public ResponseEntity<String> scanPlaylist(
            @ApiParam(value = "Specify ID of playlist to scan", required = false) @RequestParam(value = "id", required = false) UUID id) {        
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
    
    @ApiOperation(value = "Start a deep scan of media streams")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Deep scan started"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to retrieve media streams to scan"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "A deep scan cannot run at this time"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media streams require scanning")
    })
    @RequestMapping(value="/deep/scan", method=RequestMethod.GET)
    public ResponseEntity<String> deepScan() {
        // Start scanning playlists
        int status = scannerService.startDeepScan();
        
        // Check status
        switch(status) {
            case SMS.Status.NOT_ALLOWED:
                return new ResponseEntity<>("Deep scan cannot be run at this time.", HttpStatus.NOT_ACCEPTABLE);
                
            case SMS.Status.REQUIRED_DATA_MISSING:
                return new ResponseEntity<>("Failed to retrieve media for scanning.", HttpStatus.INTERNAL_SERVER_ERROR);
                
            case SMS.Status.NOT_REQUIRED:
                return new ResponseEntity<>("No media requires scanning.", HttpStatus.NOT_FOUND);
                
            default:
                return new ResponseEntity<>("Deep scanning started.", HttpStatus.OK);
        }
    }
    
    @ApiOperation(value = "Stop deep scanning process")
    @RequestMapping(value="/deep/scan/stop", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void deepScanStop() {   
        scannerService.stopDeepScan();
    }
    
    @ApiOperation(value = "Get current deep scan count")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Scan count returned successfully")
    })
    @RequestMapping(value="/deep/scan/count", method=RequestMethod.GET)
    public ResponseEntity<Long> getDeepScanCount()
    {   
        return new ResponseEntity<>(scannerService.getDeepScanCount(), HttpStatus.OK);
    }

    //
    // Log
    //
    
    @ApiOperation(value = "Get most recent log entries")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Log entries returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to fetch most recent log entries")
    })
    @RequestMapping(value="/log",method=RequestMethod.GET)
    public ResponseEntity<List<LogService.LogEntry>> getLogEntries()
    { 
        if (LogService.getInstance().getLatestLogEntries() == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(LogService.getInstance().getLatestLogEntries(), HttpStatus.OK);
    }
    
    @ApiOperation(value = "Set log level")
    @RequestMapping(value="/log/level/{value}", method=RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void setLogLevel(
            @ApiParam(value = "Log level to set", required = true, allowableValues = "0,1,2,3,4") @PathVariable("value") byte level) {
        LogService.getInstance().setLogLevel(level);
    }
}
