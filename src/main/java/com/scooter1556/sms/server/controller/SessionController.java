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
package com.scooter1556.sms.server.controller;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SessionService;
import com.scooter1556.sms.server.service.SessionService.Session;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/session")
public class SessionController {
    private static final String CLASS_NAME = "SessionController";
    
    @Autowired
    private SessionService sessionService;
    
    @RequestMapping(value="/active", method=RequestMethod.GET)
    public ResponseEntity<List<Session>> getActiveSessions() {
        List<Session> sessions = sessionService.getActiveSessions();
        
        if (sessions == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(sessions, HttpStatus.OK);
    }
    
    @RequestMapping(value="/create", method=RequestMethod.GET)
    public ResponseEntity<String> createSession(HttpServletRequest request) {
        UUID id = sessionService.createSession(request.getUserPrincipal().getName());
        
        if (id == null) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(id.toString(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/add/{id}", method=RequestMethod.GET)
    public ResponseEntity<String> addSession(@PathVariable("id") UUID id, 
                                             HttpServletRequest request) {
        int result = sessionService.addSession(id, request.getUserPrincipal().getName());
        
        if (result < 0) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else if(result == 0) {
            return new ResponseEntity<>("Session already exists with ID: " + id, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Session added with ID: " + id, HttpStatus.OK);
        }
    }
    
    @RequestMapping(value="/end/{id}", method=RequestMethod.GET)
    public ResponseEntity<String> endSession(@PathVariable("id") UUID id) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Ending session with ID: " + id, null);
        
        // Check session is valid
        if (!sessionService.isSessionValid(id)) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session does not exist with ID: " + id, null);
            return new ResponseEntity<>("Session does not exist with ID: " + id, HttpStatus.NOT_FOUND);
        }
        
        // Remove session
        sessionService.removeSessionById(id);
        
        return new ResponseEntity<>("Ended session with ID: " + id, HttpStatus.OK);
    }
}