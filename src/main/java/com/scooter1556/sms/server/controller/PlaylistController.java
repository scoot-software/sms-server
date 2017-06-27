/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.domain.PlaylistContent;
import com.scooter1556.sms.server.service.LogService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
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
@RequestMapping(value="/playlist")
public class PlaylistController {
    
    @Autowired
    private MediaDao mediaDao;
    
    private static final String CLASS_NAME = "PlaylistController";
    
    @RequestMapping(method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createPlaylist(@RequestBody Playlist playlist, HttpServletRequest request) {
        // Check ID
        if(playlist.getID() == null) {
            playlist.setID(UUID.randomUUID());
        }

        // Check mandatory fields.
        if(playlist.getName() == null) {
            return new ResponseEntity<>("Missing required parameter.", HttpStatus.BAD_REQUEST);
        }
        
        // Set playlist user
        playlist.setUsername(request.getUserPrincipal().getName());
        
        // Add playlist to the database.
        if(!mediaDao.createPlaylist(playlist)) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error adding playlist to database.", null);
            return new ResponseEntity<>("Error adding playlist to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Playlist '" + playlist.getName() + "' for user '" + playlist.getUsername() + "' created successfully.", null);
        return new ResponseEntity<>("Playlist created successfully.", HttpStatus.CREATED);
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updatePlaylist(@RequestBody Playlist update, @PathVariable("id") UUID id, HttpServletRequest request) {
        Playlist playlist = mediaDao.getPlaylistByID(id);
        
        if(playlist == null) {
            return new ResponseEntity<>("Playlist does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        // Check user owns the playlist
        if(!playlist.getUsername().equals(request.getUserPrincipal().getName())) {
            return new ResponseEntity<>("You are not authorised to update this playlist.", HttpStatus.FORBIDDEN);
        }
        
        // Update playlist details
        if(update.getName() != null) {
            playlist.setName(update.getName());
        }
        
        if(update.getDescription() == null) {
            playlist.setDescription(update.getDescription());
        }
        
        // Update database
        if(!mediaDao.updatePlaylist(playlist)) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error updating playlist with ID: " + playlist.getID(), null);
            return new ResponseEntity<>("Error updating playlist.",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Playlist '" + playlist.getName() + "' for user '" + playlist.getUsername() + "' updated successfully.", null);
        return new ResponseEntity<>("Playlist updated successfully.", HttpStatus.ACCEPTED);
    }
    
    @RequestMapping(value="/contents", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updatePlaylistContent(@RequestBody PlaylistContent content, HttpServletRequest request) {
        // Check mandatory fields
        if(content.getID() == null || content.getMedia() == null) {
            return new ResponseEntity<>("Content is missing required fields.", HttpStatus.BAD_REQUEST);
        }
        
        // Check playlist exists
        Playlist playlist = mediaDao.getPlaylistByID(content.getID());
        
        if(playlist == null) {
            return new ResponseEntity<>("Playlist does not exist.", HttpStatus.BAD_REQUEST);
        }
        
        // Check user owns the playlist
        if(!playlist.getUsername().equals(request.getUserPrincipal().getName())) {
            return new ResponseEntity<>("You are not authorised to update this playlist.", HttpStatus.FORBIDDEN);
        }
        
        // Check content
        if(content.getMedia().isEmpty()) {
            return new ResponseEntity<>("Playlist content is empty.", HttpStatus.NO_CONTENT);
        }
        
        // Remove duplicate entries
        LinkedHashSet<Long> tmp = new LinkedHashSet<>();
        tmp.addAll(content.getMedia());
        content.getMedia().clear();
        content.getMedia().addAll(tmp);
        
        // Check media elements
        for(Long mediaElement : content.getMedia()) {
            if(mediaDao.getMediaElementByID(mediaElement) == null) {
                content.getMedia().remove(mediaElement);
            }
        }
        
        // Remove existing content for playlist
        if(!mediaDao.removePlaylistContent(content.getID())) {
            return new ResponseEntity<>("Failed to remove existing content for playlist.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Add new content for playlist
        if(!mediaDao.setPlaylistContentFromIds(content.getID(), content.getMedia())) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error adding playlist content to database.", null);
            return new ResponseEntity<>("Error adding playlist content to database.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Content updated for playlist with ID: " + playlist.getID(), null);
        return new ResponseEntity<>("Playlist content updated successfully", HttpStatus.CREATED);
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<String> deletePlaylist(@PathVariable("id") UUID id, HttpServletRequest request) {
        Playlist playlist = mediaDao.getPlaylistByID(id);
        
        if(playlist == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check user is allowed to delete the playlist
        if(!playlist.getUsername().equals(request.getUserPrincipal().getName())) {
            return new ResponseEntity<>("You are not authorised to delete this playlist.", HttpStatus.FORBIDDEN);
        }
        
        // Remove playlist from database
        mediaDao.removePlaylist(id);
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Removed playlist '" + playlist.getName() + "' for user '" + playlist.getUsername() + "'.", null);
        
        return new ResponseEntity<>("Playlist deleted.", HttpStatus.OK);
    }
    
    @RequestMapping(method=RequestMethod.GET)
    public ResponseEntity<List<Playlist>> getPlaylists(HttpServletRequest request) {
        List<Playlist> playlists = mediaDao.getPlaylistsByUsername(request.getUserPrincipal().getName());
        
        if (playlists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(playlists, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<Playlist> getPlaylist(@PathVariable("id") UUID id) {
        Playlist playlist = mediaDao.getPlaylistByID(id);
        
        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(playlist, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getPlaylistContents(@PathVariable("id") UUID id, HttpServletRequest request) {
        Playlist playlist = mediaDao.getPlaylistByID(id);
        
        if(playlist == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check user has permission to get playlist content
        if(playlist.getUsername() != null) {
            if(!playlist.getUsername().equals(request.getUserPrincipal().getName())) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }
        
        // Retrieve content
        List<MediaElement> elements = mediaDao.getPlaylistContent(id);
        
        if(elements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(elements, HttpStatus.OK);
    }
}