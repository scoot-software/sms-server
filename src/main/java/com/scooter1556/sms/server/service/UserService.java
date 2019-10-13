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
package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.dao.UserDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.UserRule;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private static final String CLASS_NAME = "UserService";
    
    @Autowired
    private UserDao userDao;
    
    public boolean processMediaFolderForUser(@NonNull String user, @NonNull MediaFolder mediaFolder) {
        // Check parameters
        if(user.isEmpty() || mediaFolder.getPath() == null || mediaFolder.getPath().isEmpty()) {
            return false;
        }
        
        // Get rules for user
        List<UserRule> rules = userDao.getUserRulesByUsername(user);
        
        // If there are no rules nothing further to be done
        if(rules == null) {
            return true;
        }
        
        // Split rules
        List<UserRule> allowed = new ArrayList<>();
        List<UserRule> denied = new ArrayList<>();
        
        rules.forEach((rule) -> {
            if(rule.getRule() == SMS.Rule.ALLOWED) {
                allowed.add(rule);
            } else if(rule.getRule() == SMS.Rule.DENIED) {
                denied.add(rule);
            }
        });
        
        return processPath(allowed, denied, mediaFolder.getPath());
    }
    
    public List<MediaFolder> processMediaFoldersForUser(@NonNull String user, @NonNull List<MediaFolder> mediaFolders) {
        // Check parameters
        if(user.isEmpty() || mediaFolders.isEmpty()) {
            return null;
        }
        
        List<MediaFolder> processedList = new ArrayList<>();
        
        // Get rules for user
        List<UserRule> rules = userDao.getUserRulesByUsername(user);
        
        // If there are no rules nothing further to be done
        if(rules == null) {
            return mediaFolders;
        }
        
        // Split rules
        List<UserRule> allowed = new ArrayList<>();
        List<UserRule> denied = new ArrayList<>();
        
        rules.forEach((rule) -> {
            if(rule.getRule() == SMS.Rule.ALLOWED) {
                allowed.add(rule);
            } else if(rule.getRule() == SMS.Rule.DENIED) {
                denied.add(rule);
            }
        });
        
        // Process media elements
        mediaFolders.stream().filter((folder) -> (processPath(allowed, denied, folder.getPath()))).forEachOrdered((folder) -> {
            processedList.add(folder);
        }); // Add to processed list if allowed
        
        return processedList;
    }
    
    public boolean processMediaElementForUser(@NonNull String user, @NonNull MediaElement mediaElement) {
        // Check parameters
        if(user.isEmpty() || mediaElement.getPath() == null || mediaElement.getPath().isEmpty()) {
            return false;
        }
        
        // Get rules for user
        List<UserRule> rules = userDao.getUserRulesByUsername(user);
        
        // If there are no rules nothing further to be done
        if(rules == null) {
            return true;
        }
        
        // Split rules
        List<UserRule> allowed = new ArrayList<>();
        List<UserRule> denied = new ArrayList<>();
        
        rules.forEach((rule) -> {
            if(rule.getRule() == SMS.Rule.ALLOWED) {
                allowed.add(rule);
            } else if(rule.getRule() == SMS.Rule.DENIED) {
                denied.add(rule);
            }
        });
        
        return processPath(allowed, denied, mediaElement.getPath());
    }
    
    public List<MediaElement> processMediaElementsForUser(@NonNull String user, @NonNull List<MediaElement> mediaElements) {
        // Check parameters
        if(user.isEmpty() || mediaElements.isEmpty()) {
            return null;
        }
        
        List<MediaElement> processedList = new ArrayList<>();
        
        // Get rules for user
        List<UserRule> rules = userDao.getUserRulesByUsername(user);
        
        // If there are no rules nothing further to be done
        if(rules == null) {
            return mediaElements;
        }
        
        // Split rules
        List<UserRule> allowed = new ArrayList<>();
        List<UserRule> denied = new ArrayList<>();
        
        rules.forEach((rule) -> {
            if(rule.getRule() == SMS.Rule.ALLOWED) {
                allowed.add(rule);
            } else if(rule.getRule() == SMS.Rule.DENIED) {
                denied.add(rule);
            }
        });
        
        // Process media elements
        mediaElements.stream().filter((mediaElement) -> (processPath(allowed, denied, mediaElement.getPath()))).forEachOrdered((mediaElement) -> {
            processedList.add(mediaElement);
        });
        
        return processedList;
    }
    
    private boolean processPath(List<UserRule> allowed, List<UserRule> denied, String path) {
        boolean result = true;
        
        // Process allowed list
        if(allowed != null && !allowed.isEmpty()) {
            result = false;

            for(UserRule rule : allowed) {
                if(path.contains(rule.getPath())) {
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
}
