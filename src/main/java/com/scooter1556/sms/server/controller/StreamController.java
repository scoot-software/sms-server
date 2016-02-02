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
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Job.JobType;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.io.FileDownloadProcess;
import com.scooter1556.sms.server.io.SMSProcess;
import com.scooter1556.sms.server.io.StreamProcess;
import com.scooter1556.sms.server.service.AdaptiveStreamingService;
import com.scooter1556.sms.server.service.JobService;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.service.TranscodeService;
import com.scooter1556.sms.server.service.TranscodeService.StreamType;
import com.scooter1556.sms.server.service.TranscodeService.TranscodeProfile;
import com.scooter1556.sms.server.utilities.NetworkUtils;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
    private TranscodeService transcodeService;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;

    @RequestMapping(value="/initialise/{id}", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> initialiseStream(@PathVariable("id") Long id,
                                                 @RequestParam(value = "files", required = false) String files,
                                                 @RequestParam(value = "codecs", required = true) String codecs,
                                                 @RequestParam(value = "mchcodecs", required = false) String mchCodecs,
                                                 @RequestParam(value = "format", required = false) String format,
                                                 @RequestParam(value = "quality", required = true) Integer quality,
                                                 @RequestParam(value = "samplerate", required = false) Integer maxSampleRate,
                                                 @RequestParam(value = "atrack", required = false) Integer audioTrack,
                                                 @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                                                 @RequestParam(value = "direct", required = false) Boolean directPlay,
                                                 HttpServletRequest request) {
        MediaElement mediaElement;
        Job job;
        Byte jobType;
        TranscodeProfile profile;
        Boolean transcodeRequired = false;

        // Check media element
        mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null || codecs == null || quality == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Validate codecs
        for(String codec : codecs.split(",")) {
            if(!TranscodeService.isSupported(TranscodeService.getSupportedCodecs(), codec)) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        // Validate format
        if(format != null) {
            if(!TranscodeService.isSupported(TranscodeService.FORMATS, format)) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        // Determine job type & validate quality
        if(mediaElement.getType() == MediaElementType.AUDIO && TranscodeService.AudioQuality.isValid(quality)) {
            jobType = JobType.AUDIO_STREAM;
        } else if(mediaElement.getType() == MediaElementType.VIDEO && TranscodeService.VideoQuality.isValid(quality)) {
            jobType = JobType.VIDEO_STREAM;
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // Create a new job
        job = jobService.createJob(jobType, request.getUserPrincipal().getName(), mediaElement);
        
        if(job == null) {
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

                for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                    if(address.getAddress().equals(local)) {
                        int mask = address.getNetworkPrefixLength();
                        directPlay = NetworkUtils.isLocalIP(local, remote, mask);
                    }
                }
            } catch (UnknownHostException | SocketException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Failed to check IP adress of client", ex);
            }
        }
        
        //
        // Create transcode profile for job
        //
        
        profile = new TranscodeProfile(job.getID());
        
        profile.setMediaElement(mediaElement);
        
        if(files != null) {
            profile.setFiles(files.split(","));
        }
        
        profile.setCodecs(codecs.split(","));
        
        if(format != null) {
            profile.setFormat(format);
        }
        
        profile.setQuality(quality);
        
        if(maxSampleRate != null) {
            profile.setMaxSampleRate(maxSampleRate);
        }
        
        if(mchCodecs != null) {
            profile.setMchCodecs(mchCodecs.split(","));
        }
        
        if(audioTrack != null) {
            if(TranscodeService.isAudioStreamAvailable(audioTrack, mediaElement)) {
                profile.setAudioTrack(audioTrack);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
            
        if(subtitleTrack != null) {
            if(TranscodeService.isSubtitleStreamAvailable(subtitleTrack, mediaElement)) {
                profile.setSubtitleTrack(subtitleTrack);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        
        profile.setDirectPlayEnabled(directPlay);
        
        System.out.println(profile);
        
        // Test if we can stream the file directly without transcoding
        if(profile.getFiles() != null) {
            // If the file type is supported and all codecs are supported without transcoding stream the file directly
            if(TranscodeService.isSupported(profile.getFiles(), mediaElement.getFormat())) {
                transcodeRequired = TranscodeService.isTranscodeRequired(profile);
                
                if(transcodeRequired == null) {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                } else if(!transcodeRequired) {
                    profile.setType(StreamType.FILE);
                    profile.setMimeType(TranscodeService.getMimeType(mediaElement.getFormat(), mediaElement.getType()));
                    System.out.println(profile);
                }
            }
        }
        
        // If necessary process all streams ready for streaming and/or transcoding
        if(transcodeRequired) {
            System.out.println(profile);
            if(mediaElement.getType() == MediaElementType.VIDEO) {
                // If a suitable format was not given we can't continue
                if(profile.getFormat() == null) {
                    return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
                }
                
                // Process subtitles
                if(!TranscodeService.processSubtitles(profile)) {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                
                // Process video
                if(!TranscodeService.processVideo(profile)) {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
            
            // Process Audio
            if(!TranscodeService.processAudio(profile)) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
            
            // Set MIME Type
            if(profile.getFormat() != null) {
                profile.setMimeType(TranscodeService.getMimeType(profile.getFormat(), mediaElement.getType()));
            }
            
            // Set stream type
            switch(profile.getFormat()) {
                case "hls":
                    profile.setType(StreamType.ADAPTIVE);
                    
                default:
                    profile.setType(StreamType.TRANSCODE);
            }
        }
        
        // If this is an adaptive streaming job start the transcode process
        if(profile.getType() == StreamType.ADAPTIVE) {
            if(adaptiveStreamingService.initialise(profile) == null) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        
        // Add profile to transcode service
        transcodeService.addTranscodeProfile(profile);
        return new ResponseEntity<>(job.getID(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    @ResponseBody
    public void getStream(@PathVariable("id") Long id,
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
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
                
        try {
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Retrieve transcode profile
            profile = transcodeService.getTranscodeProfile(id);
            
            if(profile == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve transcode profile for job " + id + ".");
                return;
            }
            
            switch(profile.getType()) {
                case StreamType.ADAPTIVE:
                    if(profile.getFormat().equals("hls")) {
                        adaptiveStreamingService.sendHLSPlaylist(id, request, response);
                    }
                
                    break;
                    
                case StreamType.FILE:
                    if(profile.getMediaElement() == null) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find media element.");
                        return;
                    }
                    
                    process = new FileDownloadProcess(Paths.get(profile.getMediaElement().getPath()), profile.getMimeType(), request, response);
                    process.start();
                    break;
                    
                case StreamType.TRANSCODE:
                    // Update profile
                    if(offset != null) {
                        if(offset > profile.getMediaElement().getDuration()) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Offset out of range.");
                            return;
                        }
                        
                        profile.setOffset(offset);
                    }
                    
                    if(audioTrack != null) {
                        if(!TranscodeService.isAudioStreamAvailable(audioTrack, profile.getMediaElement())) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Audio track is not available.");
                            return;
                        }
                        
                        profile.setAudioTrack(audioTrack);
                    }
                    
                    if(subtitleTrack != null) {
                        if(!TranscodeService.isSubtitleStreamAvailable(subtitleTrack, profile.getMediaElement())) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Subtitle track is not available.");
                            return;
                        }
                        
                        profile.setSubtitleTrack(subtitleTrack);
                        
                        // Re-process video stream
                        if(!TranscodeService.processVideo(profile)) {
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process transcode properties for video stream.");
                            return;
                        }
                        
                    }
                    
                    // Get transcode command
                    List<String> command = transcodeService.getTranscodeCommand(profile);
        
                    if(command == null) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process transcode command.");
                        return;
                    }
                    
                    process = new StreamProcess(id, command, profile.getMimeType(), request, response);
                    process.start();
                    break;
                    
                default:
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot determine stream type.");
            }
        } catch (IOException ex) {
            // Called if client closes the connection early.
            if(process != null) {
                process.end();
            }
        } finally {
            if(job != null && process != null) {
                jobDao.updateBytesTransferred(id, process.getBytesTransferred());
            }
        }
    }
    
    @RequestMapping(value="/segment", method=RequestMethod.GET)
    public void getSegment(@RequestParam(value = "id", required = true) final Long id,
                           @RequestParam(value = "num", required = true) final Integer segmentNumber,
                           HttpServletRequest request, 
                           HttpServletResponse response) {
        Job job = null;
        FileDownloadProcess downloadProcess = null;
        AdaptiveStreamingProcess transcodeProcess;
        
        try {
            // Get associated job
            job = jobDao.getJobByID(id);

            // Get associated process
            transcodeProcess = adaptiveStreamingService.getProcessByID(id);

            // Check media element
            MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());

            if(transcodeProcess == null || mediaElement == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve transcode process.");
                return;
            }

            // Segment Files
            File segment = new File(SettingsService.getHomeDirectory().getPath() + "/stream" + String.format("%05d", segmentNumber) + ".ts");
            File nextSegment = new File(SettingsService.getHomeDirectory().getPath() + "/stream" + String.format("%05d", segmentNumber + 1) + ".ts");
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Job ID: " + id + " Segment Requested: " + segmentNumber, null);

            // Flags
            boolean isLastSegment = segmentNumber.equals(mediaElement.getDuration() / TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION);

            // Check if the segment already exists.
            // We check the next segment is available (if not the last segment) to make sure the requested segment is fully transcoded.
            boolean isAvailable = isLastSegment ? segment.exists() : segment.exists() && nextSegment.exists();

            if(!isAvailable) {            
                // Check if the user has seeked to an un-transcoded segment
                if(!(segmentNumber.equals(transcodeProcess.getSegmentNumber()) || segmentNumber.equals(transcodeProcess.getSegmentNumber() + 1))) {              
                    // Start a new transcode process starting with the requested segment
                    transcodeProcess = adaptiveStreamingService.startProcessWithOffset(id, segmentNumber);

                    // Check the new process started successfully
                    if(transcodeProcess == null) {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to start new transcode process.");
                        return;
                    }
                }

                // Wait for the segment to become available
                while(!isAvailable && !transcodeProcess.hasEnded()) {
                    isAvailable = isLastSegment ? segment.exists() : nextSegment.exists();
                    Thread.sleep(500);
                }
            }

            // Check that the segment is available
            if(!isAvailable) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Requested segment is not available.");
                return;
            }
            
            // Update process segment tracking
            transcodeProcess.setSegmentNumber(segmentNumber);
            
            // Return segment
            downloadProcess = new FileDownloadProcess(segment.toPath(), "video/MP2T", request, response);
            downloadProcess.start();
        } catch (IOException|InterruptedException ex) {
            // Called if client closes the connection early.
        } finally {
            if(job != null && downloadProcess != null) {
                jobDao.updateBytesTransferred(id, downloadProcess.getBytesTransferred());
            }
        }
    }
}
