package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.domain.Playlist;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class PlaylistUtils {
    
    public static final String[] SUPPORTED_PLAYLISTS = {"m3u","m3u8"};
    
    public static List<Playlist> scanForPlaylists(String path) {
        List<Playlist> playlists = new ArrayList<>();
        
        Collection files = FileUtils.listFiles(new File(path), SUPPORTED_PLAYLISTS, true);

        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            File file = (File) iterator.next();
            playlists.add(new Playlist(FilenameUtils.getBaseName(file.getName()), file.getAbsolutePath()));
        }
    
        return playlists;
    }
}
