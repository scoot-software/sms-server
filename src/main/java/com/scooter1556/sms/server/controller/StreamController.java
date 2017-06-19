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

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Job.JobType;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.VideoTranscode.VideoQuality;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.io.FileDownloadProcess;
import com.scooter1556.sms.server.io.SMSProcess;
import com.scooter1556.sms.server.service.AdaptiveStreamingService;
import com.scooter1556.sms.server.service.JobService;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.NetworkService;
import com.scooter1556.sms.server.service.SessionService;
import com.scooter1556.sms.server.service.SessionService.Session;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.service.TranscodeService;
import com.scooter1556.sms.server.utilities.FileUtils;
import com.scooter1556.sms.server.utilities.NetworkUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value="/stream")
public class StreamController {

    private static final String CLASS_NAME = "StreamController";
        
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private NetworkService networkService;
    
    @Autowired
    private TranscodeService transcodeService;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
    
    @Autowired
    private SessionService sessionService;

    @CrossOrigin
    @RequestMapping(value="/initialise/{session}/{id}", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TranscodeProfile> initialiseStream(@PathVariable("session") UUID sessionId,
                                                             @PathVariable("id") Long id,
                                                             @RequestParam(value = "client", required = false) String client,
                                                             @RequestParam(value = "files", required = false) String files,
                                                             @RequestParam(value = "codecs", required = true) String codecs,
                                                             @RequestParam(value = "mchcodecs", required = false) String mchCodecs,
                                                             @RequestParam(value = "format", required = false) String format,
                                                             @RequestParam(value = "quality", required = true) Integer quality,
                                                             @RequestParam(value = "samplerate", required = false) Integer maxSampleRate,
                                                             @RequestParam(value = "bitrate", required = false) Integer maxBitRate,
                                                             @RequestParam(value = "atrack", required = false) Integer audioTrack,
                                                             @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                                                             @RequestParam(value = "direct", required = false) Boolean directPlay,
                                                             @RequestParam(value = "update", required = false) Boolean update,
                                                             HttpServletRequest request) {
        MediaElement mediaElement;
        Session session;
        Job job;
        Byte jobType;
        TranscodeProfile profile;
        Boolean transcodeRequired = true;

        // Check session is valid
        session = sessionService.getSessionById(sessionId);
        
        if(session == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sessionId, null);
            return new ResponseEntity<>(HttpStatus.BAD_GATEWAY);
        }
        
        // Check media element
        mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null || codecs == null || quality == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Invalid transcode request.", null);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check physical file is available
        if(!new File(mediaElement.getPath()).exists()) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "File not found for media element with ID " + id + ".", null);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        // Validate codecs
        for(String codec : codecs.split(",")) {
            if(!TranscodeUtils.isSupported(TranscodeService.getSupportedCodecs(), codec)) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Codec '" + codec + "' not recognised.", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        // Validate format
        if(format != null) {
            if(!TranscodeUtils.isSupported(TranscodeUtils.FORMATS, format)) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format '" + format + "' is not recognised.", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        // Determine job type & validate quality
        if(mediaElement.getType() == MediaElementType.AUDIO && AudioQuality.isValid(quality)) {
            jobType = JobType.AUDIO_STREAM;
        } else if(mediaElement.getType() == MediaElementType.VIDEO && VideoQuality.isValid(quality)) {
            jobType = JobType.VIDEO_STREAM;
        } else {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Invalid transcode request.", null);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // Ensure our metadata update flag is set
        if(update == null) {
            update = true;
        }
        
        // Create a new job
        job = jobService.createJob(jobType, session.getUsername(), mediaElement, update);
        
        if(job == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to create job for trancode request.", null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Direct Play        
        if(directPlay == null) {
            directPlay = false;
        }
        
        // Determine if the device is on the local network
        if(directPlay) {
            try {
                InetAddress local = InetAddress.getByName(request.getLocalAddr());
                InetAddress remote = InetAddress.getByName(request.getRemoteAddr());
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(local);
                
                LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Client connected with IP " + remote.toString(), null);

                // Check if the remote device is on the same subnet as the server
                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if(address.getAddress().equals(local)) {
                        int mask = address.getNetworkPrefixLength();
                        directPlay = NetworkUtils.isLocalIP(local, remote, mask);
                    }
                }
                
                // Check if request came from public IP if subnet check was false
                if(!directPlay) {
                    String ip = networkService.getPublicIP();
                    
                    if(ip != null) {
                        directPlay = remote.toString().contains(ip);
                    }
                }
            } catch (UnknownHostException | SocketException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to check IP adress of client.", ex);
            }
        }
        
        //
        // Create transcode profile for job
        //
        
        profile = new TranscodeProfile(job.getID());
        
        profile.setMediaElement(mediaElement);
        
        profile.setUrl(request.getRequestURL().toString().replaceFirst("/stream(.*)", ""));
        
        if(files != null) {
            profile.setFiles(files.split(","));
        }
        
        profile.setCodecs(codecs.split(","));
        
        if(client != null) {
            profile.setClient(client);
        }
        
        if(format != null) {
            profile.setFormat(format);
        }
        
        profile.setQuality(quality);
        
        if(maxSampleRate != null) {
            profile.setMaxSampleRate(maxSampleRate);
        }
        
        if(maxBitRate != null) {
            profile.setMaxBitRate(maxBitRate);
        }
        
        if(mchCodecs != null) {
            profile.setMchCodecs(mchCodecs.split(","));
        }
        
        if(audioTrack != null) {
            if(TranscodeUtils.isAudioStreamAvailable(audioTrack, mediaElement)) {
                profile.setAudioTrack(audioTrack);
            } else {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Audio stream " + audioTrack + " is not available for media element " + mediaElement.getID() + ".", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
            
        if(subtitleTrack != null) {
            if(TranscodeUtils.isSubtitleStreamAvailable(subtitleTrack, mediaElement)) {
                profile.setSubtitleTrack(subtitleTrack);
            } else {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Subtitle stream " + subtitleTrack + " is not available for media element " + mediaElement.getID() + ".", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        profile.setDirectPlayEnabled(directPlay);
                
        // Test if we can stream the file directly without transcoding
        if(profile.getFiles() != null) {
            // If the file type is supported and all codecs are supported without transcoding stream the file directly
            if(TranscodeUtils.isSupported(profile.getFiles(), mediaElement.getFormat())) {
                transcodeRequired = TranscodeService.isTranscodeRequired(profile);
                
                if(transcodeRequired == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to determine trancode parameters for job " + job.getID() + ".", null);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                } else if(!transcodeRequired) {
                    profile.setType(StreamType.DIRECT);
                    profile.setMimeType(TranscodeUtils.getMimeType(mediaElement.getFormat(), mediaElement.getType()));
                }
            }
        }
        
        // If necessary process all streams ready for streaming and/or transcoding
        if(transcodeRequired) {
            if(mediaElement.getType() == MediaElementType.VIDEO) {
                // If a suitable format was not given we can't continue
                if(profile.getFormat() == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No suitable format given for job " + job.getID() + ".", null);
                    return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
                }
                
                // Process subtitles
                if(!TranscodeService.processSubtitles(profile)) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process subtitle streams for job " + job.getID() + ".", null);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                
                // Process video
                if(!TranscodeService.processVideo(profile)) {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process video stream for job " + job.getID() + ".", null);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Process Audio
            if(!TranscodeService.processAudio(profile)) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process audio streams for job " + job.getID() + ".", null);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Set MIME Type
            if(profile.getFormat() != null) {
                profile.setMimeType(TranscodeUtils.getMimeType(profile.getFormat(), mediaElement.getType()));
                profile.setType(StreamType.TRANSCODE);
            }
        }
        
        // If transcode is required start the transcode process
        if(profile.getType() == StreamType.TRANSCODE) {
            if(adaptiveStreamingService.initialise(profile, 0) == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to intialise adaptive streaming process for job " + job.getID() + ".", null);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
 
        // Add profile to transcode service
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, profile.toString(), null);
        transcodeService.addTranscodeProfile(profile);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }
    
    @ResponseBody
    @RequestMapping(value="/playlist/{id}/{type}/{extra}", method=RequestMethod.GET)
    public void getPlaylist(@PathVariable("id") UUID id,
                            @PathVariable("type") String type,
                            @PathVariable("extra") Integer extra,
                            HttpServletRequest request, 
                            HttpServletResponse response) {
        Job job;
        TranscodeProfile profile;
        
        try {
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve job with id " + id + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Get transcode profile
            profile = transcodeService.getTranscodeProfile(id);
            
            if(profile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to retrieve transcode profile for job " + id + ".", null);
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
                    if(profile.getVideoTranscode() == null && VideoQuality.isValid(extra)) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Video quality is not valid.", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Video quality is not valid.");
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
            switch (profile.getFormat()) {
                case "hls":
                    adaptiveStreamingService.sendHLSPlaylist(id, type, extra, request, response);
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format for job " + id + " is not compatible with adaptive streaming.", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Format is not supported for adaptive streaming.");
            }            
        } catch (IOException ex) {
            // Called if client closes the connection early.
        }
    }
    
    @ResponseBody
    @RequestMapping(value="/segment/{id}/{type}/{extra}/{file}", method=RequestMethod.GET)
    public void getSegment(@PathVariable("id") UUID id,
                           @PathVariable("type") String type,
                           @PathVariable("extra") Integer extra,
                           @PathVariable("file") String file,
                           HttpServletRequest request, 
                           HttpServletResponse response) {
        Job job = null;
        TranscodeProfile profile;
        AdaptiveStreamingProcess transcodeProcess;
        SMSProcess process = null;
        File segment;
                
        try {
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve job with id " + id + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Get transcode profile
            profile = transcodeService.getTranscodeProfile(id);
            
            if(profile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to retrieve transcode profile for job " + id + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode profile.");
                return;
            }
            
            // Get associated process
            transcodeProcess = adaptiveStreamingService.getProcessById(id);

            if(transcodeProcess == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to find adaptive streaming process for job " + id + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode process.");
                return;
            }
            
            String mimeType;
                    
            switch (profile.getFormat()) {
                case "hls":
                    mimeType = "video/MP2T";
                    break;
                    
                case "dash":
                    mimeType = "video/mp4";
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format for job " + id + " is not compatible with adaptive streaming.", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Format is not supported for adaptive streaming.");
                    return;
            }
            
            // Find Segment File
            segment = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/" + file);
            
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Job ID=" + id + " Segment=" + file + " Type=" + type + " Extra=" + extra, null);
            
            if(profile.getFormat().equals("hls")) {
                // Update segment tracking
                int num = Integer.parseInt(file);
                int oldNum = transcodeProcess.getSegmentNum();
                transcodeProcess.setSegmentNum(num);
                
                // If segment requested is not the next chronologically check if we need to start a new transcode process
                if(num != oldNum && num != (oldNum + 1) && !segment.exists()) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Starting new transcode process.", null);
                    adaptiveStreamingService.initialise(profile, num);
                }
            }
            
            File segmentList = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/segments.txt");
            
            // Check if segment list is available
            if(!segmentList.exists()) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to get segment list for job " + id + ".", null);
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to get segment list.");
                return;
            }
            
            List<String> segments = FileUtils.readFileToList(segmentList);
            int count = 0;

            while(!segments.contains(file) && (count < 10)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occured waiting for segment to become available.");
                }

                // Update segment list
                segments = FileUtils.readFileToList(segmentList);
                count++;
            }
            
            // Check if segment is definitely available
            if(count >= 10 ||  !segment.exists()) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to return segment " + file + " for job " + id + ".", null);
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Requested segment is not available.");
                return;
            }
            
            // Return file direct for audio and transcode as required for video
            if(profile.getMediaElement().getType().equals(MediaElementType.VIDEO)) {
                // Check if this is a request for a hardcoded subtitle segment
                if(type.equals("subtitle")) {
                    mimeType = "text/vtt";
                    
                    if(profile.getSubtitleTranscodes()[extra].isHardcoded()) {
                        // Set selected subtitle in transcode profile
                        profile.setSubtitleTrack(extra);
                    
                        // Return empty webvtt segment
                        adaptiveStreamingService.sendSubtitleSegment(response);
                        return;
                    }
                }
                
                segment = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/" + file + "-" + type + "-" + extra);
                
                if(!segment.exists()) {  
                    // Get transcode command
                    String[][] commands = transcodeService.getSegmentTranscodeCommand(file, profile, type, extra);

                    // Generate segment
                    boolean success = TranscodeUtils.runTranscodeCommands(commands);

                    if(!success || !segment.exists()) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to transcode segment " + file + " for job " + id + ".", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to transcode segment.");
                        return;
                    }
                }
                
                // Determine proper mimetype for audio
                if(type.equals("audio")) {
                    switch(profile.getClient()) {
                        case "chromecast":
                            mimeType = TranscodeUtils.getMimeType(profile.getAudioTranscodes()[extra].getCodec(), MediaElementType.AUDIO);
                            break;

                        default:
                            break; 
                    }
                }
            }
            
            process = new FileDownloadProcess(segment.toPath(), mimeType, request, response);
            process.start();
                
        } catch (Exception ex) {
            // Called if client closes the connection early.
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Client closed connection early (Job ID: " + id + " Segment: " + file + ")", null);
        } finally {
            if(process != null && job != null) {
                jobDao.updateBytesTransferred(id, job.getBytesTransferred() + process.getBytesTransferred());
            }
        }
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    @ResponseBody
    public void getStream(@PathVariable("id") UUID id,
                          @RequestParam(value = "atrack", required = false) Integer audioTrack,
                          @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                          @RequestParam(value = "offset", required = false) Integer offset,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        // Variables
        TranscodeProfile profile;
        Job job = null;
        SMSProcess process = null;
        
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
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve job with id " + id + ".", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Retrieve transcode profile
            profile = transcodeService.getTranscodeProfile(id);
            
            if(profile == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve transcode profile for job " + job.getID() + ".", null);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve transcode profile for job " + id + ".");
                return;
            }
            
            switch(profile.getType()) {
                case StreamType.TRANSCODE:
                    if(profile.getFormat().equals("hls")) {
                        adaptiveStreamingService.sendHLSPlaylist(id, null, null, request, response);
                    } else if(profile.getFormat().equals("dash")) {
                        adaptiveStreamingService.sendDashPlaylist(id, request, response);
                    }
                
                    break;
                    
                case StreamType.DIRECT:
                    if(profile.getMediaElement() == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve media element for job " + job.getID() + ".", null);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find media element.");
                        return;
                    }
                    
                    process = new FileDownloadProcess(Paths.get(profile.getMediaElement().getPath()), profile.getMimeType(), request, response);
                    process.start();
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to determine stream type for job " + job.getID() + ".", null);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot determine stream type.");
            }
        } catch (Exception ex) {
            // Called if client closes the connection early.
            if(process != null) {
                process.end();
            }
        } finally {
            if(job != null && process != null) {
                jobDao.updateBytesTransferred(id, job.getBytesTransferred() + process.getBytesTransferred());
            }
        }
    }
}
