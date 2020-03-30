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

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Job.JobType;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.Session;
import com.scooter1556.sms.server.domain.StreamProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.VideoTranscode.VideoQuality;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.io.FileDownloadProcess;
import com.scooter1556.sms.server.io.SMSProcess;
import com.scooter1556.sms.server.service.AdaptiveStreamingService;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.ScannerService;
import com.scooter1556.sms.server.service.SessionService;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.service.TranscodeService;
import com.scooter1556.sms.server.utilities.MediaUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.scooter1556.sms.server.transcode.muxer.Muxer;
import org.apache.commons.io.FilenameUtils;

@Controller
@RequestMapping(value="/stream")
public class StreamController {

    private static final String CLASS_NAME = "StreamController";
        
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private TranscodeService transcodeService;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
    
    @Autowired
    private SessionService sessionService;
    
    @Autowired
    private ScannerService scannerService;
    
    @ApiOperation(value = "Get adaptive streaming playlist", hidden = true)
    @ResponseBody
    @RequestMapping(value="/playlist/{sid}/{meid}/{type}/{extra}/{extension}", method=RequestMethod.GET)
    public void getPlaylist(@PathVariable("sid") UUID sid,
                            @PathVariable("meid") UUID meid,
                            @PathVariable("type") String type,
                            @PathVariable("extra") Integer extra,
                            @PathVariable("extension") String extension,
                            HttpServletRequest request, 
                            HttpServletResponse response) {
        
        Session session;
        Job job;
        TranscodeProfile profile;
        
        try {
            // Retrieve session
            session = sessionService.getSessionById(sid);
            
            if(session == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Session invalid with ID: " + sid + ".");
                return;
            }
            
            if(session.getClientProfile() == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Client profile is not available for session with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Client profile is not available for session with ID: " + sid + ".");
                return;
            }
            
            // Retrieve Job
            job = session.getJobByMediaElementId(meid);
            
            if(job == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve job.", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Get transcode profile
            profile = job.getTranscodeProfile();
            
            if(profile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to retrieve transcode profile.", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode profile.");
                return;
            }
            
            // Check type and transcode profile
            switch(type) {
                case "audio":
                    if(profile.getAudioTranscodes() == null || profile.getAudioTranscodes().length <= extra) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Audio stream is out of range.", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Audio stream is out of range.");
                        return;
                    }
                    
                    break;
                    
                case "video":
                    if(profile.getVideoTranscodes() == null || extra >= TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).size()) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Video stream requested is out of range.", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Video stream requested is out of range.");
                        return;
                    }
                    
                    break;
                    
                case "subtitle":
                    if(profile.getSubtitleTranscodes() == null || profile.getSubtitleTranscodes().length <= extra) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Subtitles stream is out of range.", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Subtitle stream is out of range.");
                        return;
                    }
                    
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Playlist type is not recognised.", null);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Playlist type is not recognised.");
                    return;
            }
            
            // Return playlist
            switch (session.getClientProfile().getFormat()) {
                case SMS.Format.HLS_TS:
                case SMS.Format.HLS_FMP4:
                    adaptiveStreamingService.sendHLSPlaylist(job, session.getClientProfile(), type, extra, extension, false, response);
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format for job " + job.getId() + " is not compatible with adaptive streaming.", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Format is not supported for adaptive streaming.");
            }            
        } catch (IOException ex) {
            // Called if client closes the connection early.
        }
    }
    
    @ApiOperation(value = "Get adaptive streaming segment", hidden = true)
    @ResponseBody
    @RequestMapping(value="/segment/{sid}/{meid}/{type}/{extra}/{file}", method=RequestMethod.GET)
    public void getSegment(@PathVariable("sid") UUID sid,
                           @PathVariable("meid") UUID meid,
                           @PathVariable("type") String type,
                           @PathVariable("extra") Integer extra,
                           @PathVariable("file") String file,
                           HttpServletRequest request, 
                           HttpServletResponse response) {
        Session session;
        Job job = null;
        TranscodeProfile profile;
        AdaptiveStreamingProcess transcodeProcess;
        SMSProcess process = null;
        File segment;
                
        try {
            // Retrieve session
            session = sessionService.getSessionById(sid);
            
            if(session == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Session invalid with ID: " + sid + ".");
                return;
            }
            
            if(session.getClientProfile() == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Client profile is not available for session with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Client profile is not available for session with ID: " + sid + ".");
                return;
            }
            
            // Retrieve Job
            job = session.getJobByMediaElementId(meid);
            
            if(job == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve job.", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Get transcode profile
            profile = job.getTranscodeProfile();
            
            if(profile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to retrieve transcode profile.", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode profile.");
                return;
            }
            
            // Get associated process
            transcodeProcess = adaptiveStreamingService.getProcessById(job.getId());

            if(transcodeProcess == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to find adaptive streaming process for job " + job.getId() + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode process.");
                return;
            }
            
            // Initialise segment information
            segment = new File(SettingsService.getInstance().getCacheDirectory().getPath() + File.separator + "streams" + File.separator + job.getId() + File.separator + extra + "-" + type + "-" + file);
            
            if(!file.startsWith("init")) {
                // Update segment tracking
                int num = Integer.parseInt(FilenameUtils.getBaseName(file));
                int oldNum = transcodeProcess.getSegmentNum();
                transcodeProcess.setSegmentNum(num);
                
                // If segment requested is not the next chronologically check if we need to start a new transcode process
                if(num != oldNum && num != (oldNum + 1) && !segment.exists()) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Starting new transcode process.", null);
                    adaptiveStreamingService.initialise(job, num);
                }
            }
            
            
            
            // Check if segment is available and wait for it if not
            if(!segment.exists()) {
                // Watch work directory for segments
                WatchService watcher;
                watcher = FileSystems.getDefault().newWatchService();
                Path workdir = Paths.get(segment.getParent());
                workdir.register(watcher, ENTRY_CREATE);
                boolean isFound = false;
                
                while(!isFound) {
                    WatchKey key;
                    
                    // Do a simple check
                    if(segment.exists()) {
                        isFound = true;
                        break;
                    }
                    
                    try {
                        key = watcher.poll(TranscodeUtils.DEFAULT_SEGMENT_DURATION, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        return;
                    }
                    
                    // Check for timeout
                    if(key == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No activity for job " + job.getId() + " in more than " + TranscodeUtils.DEFAULT_SEGMENT_DURATION + " seconds!", null);
                        break;
                    }

                    for(WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                        Path filePath = pathEvent.context();

                        if(kind == ENTRY_CREATE) {
                            if(filePath.toString().equals(segment.getPath())) {
                                isFound = true;
                                break;
                            }
                        }
                    }

                    // Reset key
                    if(!key.reset()) {
                        break;
                    }
                }
                
                // Cancel watch service
                watcher.close();
                    
                if(!isFound) {
                    // Timed out waiting for segment
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Timed out waiting for segment " + file + " for job " + job.getId() + ".", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Requested segment is not available.");
                    return;
                }
            }
            
            // Get file type
            String mimeType = MediaUtils.getMimeType(MediaUtils.getType(type), MediaUtils.getFormatForExtension(FilenameUtils.getExtension(file)));
            
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Job ID=" + job.getId() + " Segment=" + file + " Type=" + type + " Extra=" + extra + " MimeType=" + mimeType, null);
            
            process = new FileDownloadProcess(segment.toPath(), mimeType, false, request, response);
            process.start();
                
        } catch (Exception ex) {
            // Called if client closes the connection early.
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Client closed connection early", ex);
        } finally {
            if(process != null && job != null) {
                job.setBytesTransferred(job.getBytesTransferred() + process.getBytesTransferred());
            }
        }
    }
    
    @ApiOperation(value = "Begin streaming media to a client")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_EXPECTATION_FAILED, message = "Session invalid or missing client profile"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media element or associated file not found"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Transcode request invalid"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to initialise stream")
    })
    @RequestMapping(value="/{sid}/{meid}", method={RequestMethod.GET, RequestMethod.HEAD})
    @ResponseBody
    public void getStream(
            @ApiParam(value = "Session ID", required = true) @PathVariable("sid") UUID sid,
            @ApiParam(value = "Media Element ID", required = true) @PathVariable("meid") UUID meid,
            HttpServletRequest request,
            HttpServletResponse response)
    {
        // Variables
        Job job;
        SMSProcess process = null;
        MediaElement mediaElement;
        Session session;
        ClientProfile clientProfile;
        TranscodeProfile transcodeProfile;
        
                
        /*********************** DEBUG: Get Request Headers *********************************/        
        String requestHeader = "\n***************\nRequest Header:\n***************\n";
	Enumeration requestHeaderNames = request.getHeaderNames();
        
	while (requestHeaderNames.hasMoreElements()) {
            String key = (String) requestHeaderNames.nextElement();
            String value = request.getHeader(key);
            requestHeader += key + ": " + value + "\n";
        }
        
        // Print Headers
        LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
        
        try {
            // Check session is valid
            session = sessionService.getSessionById(sid);

            if(session == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Session invalid with ID: " + sid + ".");
                return;
            }
            
            // Check client profile is valid
            clientProfile = session.getClientProfile();
            
            if(clientProfile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Client profile is not available for session with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Client profile is not available for session with ID: " + sid + ".");
                return;
            }
            
            // Retrieve job
            job = session.getJobByMediaElementId(meid);

            // Check if a job for this media element is already associated with the session
            if(job == null) {
                // Check media element
                mediaElement = mediaDao.getMediaElementByID(meid);

                if(mediaElement == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Requested media element not found.", null);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Requested media element not found.");
                    return;
                }

                // Check physical file is available
                if(!new File(mediaElement.getPath()).exists()) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "File not found for media element with ID " + meid + ".", null);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found for media element with ID " + meid + ".");
                    return;
                }

                // Create and populate a new job
                job = new Job(session.getId());

                // Determine job type, validate quality and fetch available streams
                if(mediaElement.getType() == MediaElementType.AUDIO && AudioQuality.isValid(clientProfile.getAudioQuality())) {
                    job.setType(JobType.AUDIO_STREAM);
                    mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(meid));
                } else if(mediaElement.getType() == MediaElementType.VIDEO && VideoQuality.isValid(clientProfile.getVideoQuality())) {
                    job.setType(JobType.VIDEO_STREAM);
                    mediaElement.setVideoStreams(mediaDao.getVideoStreamsByMediaElementId(meid));
                    mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(meid));
                    mediaElement.setSubtitleStreams(mediaDao.getSubtitleStreamsByMediaElementId(meid));
                } else {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Invalid transcode request.", null);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid transcode request.");
                    return;
                }

                // Set media element in job
                job.setMediaElement(mediaElement);

                // Populate transcode profile
                transcodeProfile = getTranscodeProfile(clientProfile, mediaElement);
                
                if(transcodeProfile == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to get transcode profile for media element " + mediaElement + " and client profile " + clientProfile + ".", null);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get transcode profile.");
                    return;
                }

                // Set transcode profile for job
                job.setTranscodeProfile(transcodeProfile);
                
                // Only do certain things if this IS NOT a HEAD request
                if(!request.getMethod().equals("HEAD")) {
                    // Update media element and parent media element if necessary
                    mediaDao.updateLastPlayed(mediaElement.getID());

                    MediaElement parentElement = mediaDao.getMediaElementByPath(mediaElement.getParentPath());
                    
                    if(parentElement != null) {
                        mediaDao.updateLastPlayed(parentElement.getID());
                    }
                    
                    // Add job to session
                    session.addJob(job);

                    // Stop deep scan if necessary
                    if(job.getType() == Job.JobType.VIDEO_STREAM) {
                        scannerService.stopDeepScan();
                    }
                
                    // If transcode is required start the transcode process
                    if(transcodeProfile.getType() > StreamType.DIRECT) {
                        if(adaptiveStreamingService.initialise(job, 0) == null) {
                            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to intialise adaptive streaming process for job " + job.getId() + ".", null);
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to intialise adaptive streaming process for job " + job.getId() + ".");
                            return;
                        }
                    }
                    
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, session.getUsername() + " streaming: " + mediaElement, null);
                    LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, session.getUsername() + " started streaming '" + mediaElement.getTitle() + "'.", null);
                }
            } else {
                // Populate variables
                transcodeProfile = job.getTranscodeProfile();
                mediaElement = job.getMediaElement();
            }
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Error encountered processing stream for session " + sid + ".", ex);
            return;
        }
            
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "SID: " + sid + " MEID: " + meid + " Client Profile=" + clientProfile.toString() + " Transcode Profile=" + transcodeProfile.toString(), null);

        try {
            switch(transcodeProfile.getType()) {
                case StreamType.LOCAL: case StreamType.REMOTE:
                    // HLS
                    if(clientProfile.getFormat() == SMS.Format.HLS_TS || clientProfile.getFormat() == SMS.Format.HLS_FMP4) {
                        adaptiveStreamingService.sendHLSPlaylist(job, clientProfile, null, null, null, request.getMethod().equals("HEAD"), response);
                    }
                    
                    // MPEG-Dash
                    if(clientProfile.getFormat() == SMS.Format.MPEG_DASH) {
                        adaptiveStreamingService.sendDashPlaylist(job, clientProfile, request.getMethod().equals("HEAD"), response);
                    }
                
                    break;
                    
                case StreamType.DIRECT:
                    if(mediaElement == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve media element for job " + job.getId() + ".", null);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find media element.");
                        return;
                    }
                    
                    process = new FileDownloadProcess(Paths.get(mediaElement.getPath()), transcodeProfile.getMimeType(), request.getMethod().equals("HEAD"), request, response);
                    process.start();
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to determine stream type for job " + job.getId() + ".", null);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot determine stream type.");
            }
        } catch (Exception ex) {
            // Called if client closes the connection early.
            if(process != null) {
                process.end();
            }
        } finally {            
            if(process != null) {          
                job.setBytesTransferred(job.getBytesTransferred() + process.getBytesTransferred());
            }
        }
    }
    
    @ApiOperation(value = "Get stream profile")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Stream profile returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_EXPECTATION_FAILED, message = "Session invalid or missing client profile"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media element or associated file not found"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Transcode request invalid"),
        @ApiResponse(code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message = "Failed to return stream profile")
    })
    @CrossOrigin
    @RequestMapping(value="/profile/{sid}/{meid}", method={RequestMethod.GET})
    public ResponseEntity<StreamProfile> getStreamProfile(
            @ApiParam(value = "Session ID", required = true) @PathVariable("sid") UUID sid,
            @ApiParam(value = "Media Element ID", required = true) @PathVariable("meid") UUID meid,
            HttpServletRequest request)
    {
        // Variables
        Job job;
        MediaElement mediaElement;
        Session session;
        ClientProfile clientProfile;
        TranscodeProfile transcodeProfile;
        StreamProfile streamProfile;
        
        // Check session is valid
        session = sessionService.getSessionById(sid);

        if(session == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sid, null);
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
        
        // Check client profile
        clientProfile = session.getClientProfile();

        if(clientProfile == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Client profile not found for session.", null);
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
        
        // Retrieve job
        job = session.getJobByMediaElementId(meid);

        // Check if a job for this media element is already associated with the session
        if(job == null) {
            // Check media element
            mediaElement = mediaDao.getMediaElementByID(meid);

            if(mediaElement == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Requested media element not found.", null);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Check physical file is available
            if(!new File(mediaElement.getPath()).exists()) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "File not found for media element with ID " + meid + ".", null);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // Create and populate a new job
            job = new Job(session.getId());

            // Determine job type, validate quality and fetch available streams
            if(mediaElement.getType() == MediaElementType.AUDIO && AudioQuality.isValid(clientProfile.getAudioQuality())) {
                job.setType(JobType.AUDIO_STREAM);
                mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(meid));
            } else if(mediaElement.getType() == MediaElementType.VIDEO && VideoQuality.isValid(clientProfile.getVideoQuality())) {
                job.setType(JobType.VIDEO_STREAM);
                mediaElement.setVideoStreams(mediaDao.getVideoStreamsByMediaElementId(meid));
                mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(meid));
                mediaElement.setSubtitleStreams(mediaDao.getSubtitleStreamsByMediaElementId(meid));
            } else {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Invalid transcode request.", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Set media element in job
            job.setMediaElement(mediaElement);

            // Populate transcode profile
            transcodeProfile = getTranscodeProfile(clientProfile, mediaElement);
            
            if(transcodeProfile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to get transcode profile for media element " + mediaElement + " and client profile " + clientProfile + ".", null);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            //  Add transcode Profile to job
            job.setTranscodeProfile(transcodeProfile);
        }
        
        // Convert transcode profile to stream profile
        streamProfile = TranscodeUtils.getStreamProfile(job.getMediaElement(), job.getTranscodeProfile());
        
        if(streamProfile == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to get stream profile for transcode profile: " + job.getTranscodeProfile(), null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(streamProfile, HttpStatus.OK);
    }
    
    //
    // Helper Functions
    //
    
    private TranscodeProfile getTranscodeProfile(ClientProfile clientProfile, MediaElement mediaElement) {
        // Create and populate transcode profile
        TranscodeProfile transcodeProfile = new TranscodeProfile();
        
        // Set stream type
        if(clientProfile.getLocal()) {
            transcodeProfile.setType(TranscodeProfile.StreamType.LOCAL);
        } else {
            transcodeProfile.setType(TranscodeProfile.StreamType.REMOTE);
        }

        // If the file type is supported and all codecs are supported without transcoding, stream the file directly
        Boolean transcodeRequired = TranscodeUtils.isTranscodeRequired(mediaElement, clientProfile);

        if(transcodeRequired == null) {
            return null;
        } else if(!transcodeRequired) {
            transcodeProfile.setType(StreamType.DIRECT);
            transcodeProfile.setMimeType(MediaUtils.getMimeType(mediaElement.getType(), mediaElement.getFormat()));
        }

        // If necessary process all streams ready for streaming and/or transcoding
        if(transcodeRequired) {
            // Get a suitable encoder
            Muxer muxer = TranscodeUtils.getTranscodeMuxer(clientProfile.getFormat());

            if(muxer == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to get a suitable muxer for format " + clientProfile.getFormat() + ".", null);
                return null;
            }
            
            // Set client in encoder
            muxer.setClient(clientProfile.getClient());

            // Set muxer in transcode profile
            transcodeProfile.setMuxer(muxer);
            
            // Set default segment duration
            transcodeProfile.setSegmentDuration(TranscodeUtils.DEFAULT_SEGMENT_DURATION);

            if(mediaElement.getType() == MediaElementType.VIDEO) {
                // If a suitable format was not given we can't continue
                if(clientProfile.getFormat() == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No suitable format found for client profile:" + clientProfile, null);
                    return null;
                }

                // Process subtitles
                if(!transcodeService.processSubtitles(transcodeProfile, mediaElement)) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process subtitle streams: " + ArrayUtils.toString(mediaElement.getSubtitleStreams()) + " " + clientProfile, null);
                    return null;
                }

                // Process video
                if(!transcodeService.processVideo(transcodeProfile, clientProfile, mediaElement)) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process video streams: " + ArrayUtils.toString(mediaElement.getVideoStreams()) + " " + clientProfile, null);
                    return null;
                }
                
                // Get segment duration
                int segmentDuration = TranscodeUtils.getSegmentDuration(mediaElement.getVideoStreams().get(0));
                
                if(segmentDuration > 0) {
                    transcodeProfile.setSegmentDuration(segmentDuration);
                }
            }

            // Process Audio
            if(!transcodeService.processAudio(transcodeProfile, clientProfile, mediaElement)) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process audio streams: " + ArrayUtils.toString(mediaElement.getAudioStreams()) + " " + clientProfile, null);
                return null;

            }

            // Set MIME Type
            if(clientProfile.getFormat() != null) {
                transcodeProfile.setMimeType(MediaUtils.getMimeType(mediaElement.getType(), clientProfile.getFormat()));
            }
        }
        
        return transcodeProfile;
    }
}
