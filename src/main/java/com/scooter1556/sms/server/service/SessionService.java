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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private static final String CLASS_NAME = "SessionService";
    
    private final List<Session> sessions = new ArrayList<>();
    
    public UUID createSession(String username) {
        // Checks
        if(username == null) {
            return null;
        }
        
        // Create a new session and add it to the list
        Session session = new Session(UUID.randomUUID(), username);
        sessions.add(session);
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "New session created: " + session.toString(), null);
        
        return session.getId();
    }
    
    public Session getSessionById(UUID id) {
        for(Session session : sessions) {
            if(session.getId().compareTo(id) == 0) {
                return session;
            }
        }
        
        return null;
    }
    
    public boolean isSessionValid(UUID id) {
        return getSessionById(id) != null;
    }
     
    public void removeSessionById(UUID id) {
        int index = 0;
        
        for (Session session : sessions) {
            if(session.getId().compareTo(id) == 0) {
                sessions.remove(index);
                break;
            }
            
            index ++;
        }
    }
    
    public List<Session> getActiveSessions() {
        return sessions;
    }
        
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class Session {
        private final UUID id;
        private final String username;
        
        public Session(UUID id, String username) {
            this.id = id;
            this.username = username;
        }
        
        @Override
        public String toString() {
            return String.format("{ID=%s, Username=%s}",
                    id == null ? "null" : id,
                    username == null ? "null" : username);
        }
        
        public UUID getId() {
            return id;
        }
        
        public String getUsername() {
            return username;
        }
    }
}
