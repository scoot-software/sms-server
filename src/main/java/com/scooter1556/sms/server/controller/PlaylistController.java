/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.domain.Directory;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.MediaFolder.ContentType;
import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.service.PlaylistService;
import com.scooter1556.sms.server.utilities.PlaylistUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/playlist")
public class PlaylistController {
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private PlaylistService playlistService;
    
    private static final String CLASS_NAME = "PlaylistController";
    
    @RequestMapping(value="/global", method=RequestMethod.GET)
    public ResponseEntity<List<Playlist>> getGlobalPlaylists() {
        List<MediaFolder> folders = settingsDao.getMediaFolders();
        List<Playlist> playlists = new ArrayList<>();
        
        if (folders == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Scan folders for playlists
        for(MediaFolder folder : folders) {
            // Check this media folder is supposed to contain playlists
            if(!folder.getType().equals(ContentType.PLAYLIST)) {
                continue;
            }
            
            // Scan folder for playlists
            playlists.addAll(PlaylistUtils.scanForPlaylists(folder.getPath()));
        }
        
        return new ResponseEntity<>(playlists, HttpStatus.OK);
    }

    @RequestMapping(value="/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getPlaylistContents(@RequestParam(value = "path", required = true) String path) {
        List<MediaElement> elements = new ArrayList<>();
        File playlist = new File(path);
        
        // Check file exists
        if(!playlist.isFile()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        elements.addAll(playlistService.parsePlaylist(path));
        
        return new ResponseEntity<>(elements, HttpStatus.OK);
    }
}