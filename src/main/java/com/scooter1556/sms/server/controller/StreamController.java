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
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.VideoTranscode.VideoQuality;
import com.scooter1556.sms.server.encoder.Encoder;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
    
    @ResponseBody
    @RequestMapping(value="/playlist/{sid}/{meid}/{type}/{extra}", method=RequestMethod.GET)
    public void getPlaylist(@PathVariable("sid") UUID sid,
                            @PathVariable("meid") UUID meid,
                            @PathVariable("type") String type,
                            @PathVariable("extra") Integer extra,
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
                case SMS.Format.HLS:
                    adaptiveStreamingService.sendHLSPlaylist(job, session.getClientProfile(), type, extra, response);
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format for job " + job.getId() + " is not compatible with adaptive streaming.", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Format is not supported for adaptive streaming.");
            }            
        } catch (IOException ex) {
            // Called if client closes the connection early.
        }
    }
    
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
        File segment = null;
                
        try {
            // Retrieve session
            session = sessionService.getSessionById(sid);
            
            if(session == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Session invalid with ID: " + sid, null);
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Session invalid with ID: " + sid + ".");
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
            
            String mimeType;
            
            switch (session.getClientProfile().getFormat()) {
                case SMS.Format.HLS:
                    mimeType = "video/MP2T";
                    break;
                    
                default:
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Format for job " + job.getId() + " is not compatible with adaptive streaming.", null);
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Format is not supported for adaptive streaming.");
                    return;
            }
            
            /*
            // Check for special cases such as Chromecast
            if(profile.getMediaElement().getType().equals(MediaElementType.VIDEO)) {
                // Determine proper mimetype for audio
                if(type.equals("audio")) {
                    switch(profile.getClient()) {
                        case "chromecast":
                            AudioTranscode transcode = profile.getAudioTranscodes()[extra];
                            Integer codec = transcode.getCodec();
                            
                            if(codec == SMS.Codec.COPY) {
                                codec = MediaUtils.getAudioStreamById(profile.getMediaElement().getAudioStreams(), transcode.getId()).getCodec();
                            }
                            
                            segment = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/" + file + "-" + type + "-" + extra + "." + TranscodeUtils.getFormatForAudioCodec(codec));
                            
                            mimeType = TranscodeUtils.getMimeType(codec, MediaElementType.AUDIO);
                            break;

                        default:
                            break; 
                    }
                }
            }
            */
            
            // Set default segment if not already set
            if(segment == null) {
                segment = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + job.getId() + "/" + file + "-" + type + "-" + extra + ".ts");
            }
            
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Job ID=" + job.getId() + " Segment=" + file + " Type=" + type + " Extra=" + extra, null);
            
            if(session.getClientProfile().getFormat() == SMS.Format.HLS) {
                // Update segment tracking
                int num = Integer.parseInt(file);
                int oldNum = transcodeProcess.getSegmentNum();
                transcodeProcess.setSegmentNum(num);
                
                // If segment requested is not the next chronologically check if we need to start a new transcode process
                if(num != oldNum && num != (oldNum + 1) && !segment.exists()) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Starting new transcode process.", null);
                    adaptiveStreamingService.initialise(job, num);
                }
            }
            
            // Check how we detect the segment is available
            int count = 0;
            
            // Wait for segment to become available using necessary method
            while(!segment.exists() && count < 20) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error occured waiting for segment to become available.");
                }

                count++;
            }
            
            // Check if segment is definitely available
            if(count >= 20 || !segment.exists()) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to return segment " + file + " for job " + job.getId() + ".", null);
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Requested segment is not available.");
                return;
            }
            
            process = new FileDownloadProcess(segment.toPath(), mimeType, request, response);
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
    
    @RequestMapping(value="/{sid}/{meid}", method=RequestMethod.GET)
    @ResponseBody
    public void getStream(@PathVariable("sid") UUID sid,
                          @PathVariable("meid") UUID meid,
                          HttpServletRequest request,
                          HttpServletResponse response) {
        // Variables
        Job job = null;
        SMSProcess process = null;
        MediaElement mediaElement;
        Session session;
        ClientProfile clientProfile;
        TranscodeProfile transcodeProfile;
        Boolean transcodeRequired = true;
        
                
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
                response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Session invalid with ID: " + sid + ".");
                return;
            }

            // Check if a job for this media element is already associated with the session
            if(session.getJobByMediaElementId(meid) == null) {
                // Get client profile
                clientProfile = session.getClientProfile();

                if(clientProfile == null) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Client profile not found for session.", null);
                    response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED, "Client profile not found for session.");
                    return;
                }

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
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "File not found for media element with ID " + meid + ".");
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

                // Update media element and parent media element if necessary
                mediaDao.updateLastPlayed(mediaElement.getID());

                MediaElement parentElement = mediaDao.getMediaElementByPath(mediaElement.getParentPath());

                if(parentElement != null) {
                    mediaDao.updateLastPlayed(parentElement.getID());
                }

                // Create and populate transcode profile
                transcodeProfile = new TranscodeProfile();

                // Set stream type
                if(clientProfile.getLocal()) {
                    transcodeProfile.setType(TranscodeProfile.StreamType.LOCAL);
                } else {
                    transcodeProfile.setType(TranscodeProfile.StreamType.REMOTE);
                }

                // Test if we can stream the file directly without transcoding
                if(clientProfile.getFormats() != null) {
                    // If the file type is supported and all codecs are supported without transcoding, stream the file directly
                    if(ArrayUtils.contains(clientProfile.getFormats(), mediaElement.getFormat())) {
                        transcodeRequired = transcodeService.isTranscodeRequired(mediaElement, clientProfile);

                        if(transcodeRequired == null) {
                            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to determine trancode parameters for job " + job.getId() + ".", null);
                            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Failed to determine trancode parameters for job " + job.getId() + ".");
                            return;
                        } else if(!transcodeRequired) {
                            transcodeProfile.setType(StreamType.DIRECT);
                            transcodeProfile.setMimeType(MediaUtils.getMimeType(mediaElement.getType(), mediaElement.getFormat()));
                        }
                    }
                }

                // If necessary process all streams ready for streaming and/or transcoding
                if(transcodeRequired) {
                    // Get a suitable encoder
                    Encoder encoder = TranscodeUtils.getEncoderForFormat(clientProfile.getFormat());
                    
                    if(encoder == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to get a suitable encoder for format " + clientProfile.getFormat() + ".", null);
                        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "Failed to get a suitable encoder for format " + clientProfile.getFormat() + ".");
                    }
                    
                    // Set encoder in transcode profile
                    transcodeProfile.setEncoder(encoder);
                    
                    if(mediaElement.getType() == MediaElementType.VIDEO) {
                        // If a suitable format was not given we can't continue
                        if(clientProfile.getFormat() == null) {
                            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No suitable format given for job " + job.getId() + ".", null);
                            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "No suitable format given for job " + job.getId() + ".");
                            return;
                        }

                        // Process subtitles
                        if(!transcodeService.processSubtitles(transcodeProfile, mediaElement)) {
                            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process subtitle streams for job " + job.getId() + ".", null);
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process subtitle streams for job " + job.getId() + ".");
                            return;
                        }

                        // Process video
                        if(!transcodeService.processVideo(transcodeProfile, clientProfile, mediaElement)) {
                            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process video streams for job " + job.getId() + ".", null);
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process video streams for job " + job.getId() + ".");
                            return;
                        }
                    }

                    // Process Audio
                    if(!transcodeService.processAudio(transcodeProfile, clientProfile, mediaElement)) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to process audio streams for job " + job.getId() + ".", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to process audio streams for job " + job.getId() + ".");
                        return;
                        
                    }

                    // Set MIME Type
                    if(clientProfile.getFormat() != null) {
                        transcodeProfile.setMimeType(MediaUtils.getMimeType(mediaElement.getType(), clientProfile.getFormat()));
                    }
                }

                // Set transcode profile for job
                job.setTranscodeProfile(transcodeProfile);

                // If transcode is required start the transcode process
                if(transcodeProfile.getType() > StreamType.DIRECT) {
                    if(adaptiveStreamingService.initialise(job, 0) == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to intialise adaptive streaming process for job " + job.getId() + ".", null);
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to intialise adaptive streaming process for job " + job.getId() + ".");
                        return;
                    }
                }
                
                //  Add job to session
                session.addJob(job);

                // Stop deep scan if necessary
                if(job.getType() == Job.JobType.VIDEO_STREAM) {
                    scannerService.stopDeepScan();
                }
            } else {
                // Populate variables
                job = session.getJobByMediaElementId(meid);
                clientProfile = session.getClientProfile();
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
                    if(clientProfile.getFormat() == SMS.Format.HLS) {
                        adaptiveStreamingService.sendHLSPlaylist(job, clientProfile, null, null, response);
                    }
                
                    break;
                    
                case StreamType.DIRECT:
                    if(mediaElement == null) {
                        LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Failed to retrieve media element for job " + job.getId() + ".", null);
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find media element.");
                        return;
                    }
                    
                    process = new FileDownloadProcess(Paths.get(mediaElement.getPath()), transcodeProfile.getMimeType(), request, response);
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
}
