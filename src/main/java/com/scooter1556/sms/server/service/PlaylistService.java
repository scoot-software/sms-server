package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.utilities.FileUtils;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlaylistService {
    
    private static final String CLASS_NAME = "PlaylistService";
    
    @Autowired
    private MediaDao mediaDao;
    
    public List<MediaElement> parsePlaylist(String playlistPath) {
        List<String> contents = null;
        List<MediaElement> mediaElements = new ArrayList<>();
        
        File playlist = new File(playlistPath);
        
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
            
            Path path = Paths.get(FilenameUtils.separatorsToSystem(line));                       
            List<MediaElement> results = mediaDao.getMediaElementsByName(path.getFileName().toString(), MediaElementType.AUDIO);
            
            // If nothing is found continue
            if(results == null || results.isEmpty()) {
                continue;
            }
            
            // If more than one media element is found attempt to narrow it down further
            if(results.size() > 1) {
                if(path.getNameCount() > 1) {
                    for(MediaElement mediaElement : results) {
                        if(mediaElement.getPath().contains(path.getName(path.getNameCount() - 2).toString())) {
                            mediaElements.add(mediaElement);
                            break;
                        }
                    }
                }
            } else {
                mediaElements.add(results.get(0));
            }
        }
        
        // Check for duplicates
        Set<MediaElement> tmp = new LinkedHashSet<>();
        
        mediaElements.stream().filter((mediaElement) -> (!tmp.contains(mediaElement))).forEachOrdered((mediaElement) -> {
            tmp.add(mediaElement);
        });
        
        mediaElements.clear();
        tmp.forEach((m) -> mediaElements.add(m));
                
        return mediaElements;
    }
}
