package com.scooter1556.sms.server.utilities;

import static com.scooter1556.sms.server.utilities.MediaUtils.isMediaFile;
import java.io.File;
import java.nio.file.Path;

public class PlaylistUtils {
    
    public static final String[] SUPPORTED_PLAYLISTS = {"m3u","m3u8"};
    
    public static boolean isPlaylist(Path path) {
        for (String type : SUPPORTED_PLAYLISTS) {
            if (path.getFileName().toString().toLowerCase().endsWith("." + type)) {
                return true;
            }
        }

        return false;
    }
    
    // Determines if a directory contains playlists
    public static boolean containsPlaylists(File directory) {
        for (File file : directory.listFiles()) {
            if(!file.isHidden()) {
                if(isPlaylist(file.toPath())) {
                    return true;
                }
            }                    
        }

        return false;
    }
}
