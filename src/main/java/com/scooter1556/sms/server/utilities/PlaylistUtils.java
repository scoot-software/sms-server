package com.scooter1556.sms.server.utilities;

import static com.scooter1556.sms.server.utilities.MediaUtils.isMediaFile;
import java.io.File;
import java.nio.file.Path;
import org.apache.commons.io.FilenameUtils;

public class PlaylistUtils {
    
    public static final String[] SUPPORTED_PLAYLISTS = {"m3u","m3u8"};
    
    public static boolean isPlaylist(Path path) {
        return FilenameUtils.isExtension(path.getFileName().toString().toLowerCase(), SUPPORTED_PLAYLISTS);
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
