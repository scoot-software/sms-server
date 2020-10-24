package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.domain.Playlist;
import com.scooter1556.sms.server.domain.UserRule;
import java.util.List;

public class UserUtils {
    
    public static boolean isPathAllowed(List<UserRule> allowed, List<UserRule> denied, String path) {
        boolean result = true;
        
        // Check path
        if(path == null) {
            return true;
        }
        
        // Process allowed list
        if(allowed != null && !allowed.isEmpty()) {
            result = false;

            for(UserRule rule : allowed) {
                if(rule.getPath().contains(path) || path.contains(rule.getPath())) {
                    result = true;
                    break;
                }
            }
        }

        // If not allowed we are done
        if(!result) {
            return result;
        }

        // Check denied list
        if(denied != null && !denied.isEmpty()) {
            for(UserRule rule : denied) {
                if(path.contains(rule.getPath())) {
                    return false;
                }
            }
        }
        
        return result;
    }
    
    public static boolean isPlaylistAllowed(Playlist playlist, List<UserRule> allowed, List<UserRule> denied, String user) {
        // Check user has permission to get playlist
        if(playlist.getUsername() != null) {
            if(!playlist.getUsername().equals(user)) {
                return false;
            }
        }
        
        // Check path
        if(playlist.getPath() == null || playlist.getPath().isEmpty()) {
            return true;
        }
        
        return isPathAllowed(allowed, denied, playlist.getPath());
    }
    
}
