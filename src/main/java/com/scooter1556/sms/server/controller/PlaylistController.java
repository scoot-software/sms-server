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
import com.scooter1556.sms.server.service.SessionService;
import com.scooter1556.sms.server.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/playlist")
public class PlaylistController {

    @Autowired
    private MediaDao mediaDao;

    @Autowired
    private UserService userService;

    private static final String CLASS_NAME = "PlaylistController";

    @ApiOperation(value = "Create a new playlist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Playlist created successfully"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Required parameters missing"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Error creating playlist")
    })
    @RequestMapping(method=RequestMethod.POST, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> createPlaylist(
            @ApiParam(value = "Playlist", required = true) @RequestBody Playlist playlist,
            HttpServletRequest request)
    {
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

    @ApiOperation(value = "Update a playlist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_ACCEPTED, message = "Playlist updated successfully"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Playlist doesn't exist"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Not authorised to update playlist"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Error updating playlist")
    })
    @RequestMapping(value="/{id}", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updatePlaylist(
            @ApiParam(value = "Playlist", required = true) @RequestBody Playlist update,
            @ApiParam(value = "ID of the playlist to update", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request)
    {
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

    @ApiOperation(value = "Update playlist contents")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_CREATED, message = "Playlist content updated successfully"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Playlist content is missing required data"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Not authorised to update playlist content"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Playlist does not exist"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Error updating playlist content")
    })
    @RequestMapping(value="/contents", method=RequestMethod.PUT, headers = {"Content-type=application/json"})
    @ResponseBody
    public ResponseEntity<String> updatePlaylistContent(
            @ApiParam(value = "Playlist content", required = true) @RequestBody PlaylistContent content,
            HttpServletRequest request) {
        // Check mandatory fields
        if(content.getID() == null || content.getMedia() == null) {
            return new ResponseEntity<>("Content is missing required fields.", HttpStatus.BAD_REQUEST);
        }

        // Check playlist exists
        Playlist playlist = mediaDao.getPlaylistByID(content.getID());

        if(playlist == null) {
            return new ResponseEntity<>("Playlist does not exist.", HttpStatus.NOT_FOUND);
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
        Set<UUID> tmp = new LinkedHashSet<>();

        content.getMedia().stream().filter((id) -> (!tmp.contains(id))).forEachOrdered((id) -> {
            tmp.add(id);
        });

        content.getMedia().clear();
        tmp.forEach((m) -> content.getMedia().add(m));

        // Check media elements
        content.getMedia().stream().filter((mediaElement) -> (mediaDao.getMediaElementByID(mediaElement) == null)).forEachOrdered((mediaElement) -> {
            content.getMedia().remove(mediaElement);
        });

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

    @ApiOperation(value = "Delete a playlist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Playlist deleted successfully"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Not authorised to delete playlist"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Playlist does not exist")
    })
    @RequestMapping(value="/{id}", method=RequestMethod.DELETE)
    public ResponseEntity<String> deletePlaylist(
            @ApiParam(value = "ID of the playlist to delete", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request) {
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

    @ApiOperation(value = "Get all playlists")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Playlists returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No playlists found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(method=RequestMethod.GET)
    public ResponseEntity<List<Playlist>> getPlaylists(HttpServletRequest request) {
        List<Playlist> playlists = mediaDao.getPlaylistsByUsername(request.getUserPrincipal().getName());

        if (playlists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        playlists = userService.processPlaylistsForUser(request.getUserPrincipal().getName(), playlists);

        if (playlists == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(playlists, HttpStatus.OK);
    }

    @ApiOperation(value = "Get playlist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Playlist returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Playlist does not exist"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Not authorised to retrieve playlist")
    })
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<Playlist> getPlaylist(
            @ApiParam(value = "ID of the playlist", required = true) @PathVariable("id") UUID id, HttpServletRequest request)
    {
        Playlist playlist = mediaDao.getPlaylistByID(id);

        if (playlist == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Check user has permission to get playlist
        if(!userService.processPlaylistForUser(request.getUserPrincipal().getName(), playlist)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(playlist, HttpStatus.OK);
    }

    @ApiOperation(value = "Get playlist contents")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Playlist content returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Playlist not found or is empty"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "Not authorised to retrieve playlist content")
    })
    @RequestMapping(value="/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getPlaylistContents(
            @ApiParam(value = "ID of the playlist", required = true) @PathVariable("id") UUID id,
            @ApiParam(value = "Return playlist content in a randomised order", defaultValue = "false", required = false) @RequestParam(value="random", required = false) Boolean random,
            HttpServletRequest request)
    {
        Playlist playlist = mediaDao.getPlaylistByID(id);

        if(playlist == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Check user has permission to get playlist content
        if(!userService.processPlaylistForUser(request.getUserPrincipal().getName(), playlist)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // Retrieve content
        List<MediaElement> mediaElements = mediaDao.getPlaylistContent(id);

        if(mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Process conents for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);

        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        // Check if we need to send a randomised playlist
        if(random != null) {
            if(random) {
                Collections.shuffle(mediaElements);
            }
        }

        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
}