package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.utilities.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaylistService {
    
    private static final String CLASS_NAME = "PlaylistService";
    
    @Autowired
    private MediaDao mediaDao;
    
    public List<MediaElement> parsePlaylist(String path) {
        List<String> contents = null;
        List<MediaElement> elements = new ArrayList<>();
        
        File playlist = new File(path);
        
        // Read contents of playlist
        if(playlist.canRead()) {
            contents = FileUtils.readFileToList(playlist);
        }
        
        // Check the playlist is not empty
        if(contents == null || contents.isEmpty()) {
            return null;
        }
        
        // Parse playlist contents
        for(String line : contents) {
            // Ignore extended M3U content
            if(line.startsWith("#")) {
                continue;
            }
            
            File file = new File(FilenameUtils.separatorsToSystem(line));                       
            List<MediaElement> results = mediaDao.getMediaElementsByName(file.getName(), MediaElementType.AUDIO);
            
            if(results == null || results.isEmpty()) {
                continue;
            }
            
            elements.addAll(results);
            
        }
        
        return elements;
    }
}
