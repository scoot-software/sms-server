package com.sms.server.controller;

import com.sms.server.dao.JobDao;
import com.sms.server.dao.MediaDao;
import com.sms.server.domain.Job;
import com.sms.server.domain.Job.JobType;
import com.sms.server.domain.MediaElement;
import com.sms.server.domain.MediaElement.MediaElementType;
import com.sms.server.io.StreamGobbler;
import com.sms.server.service.JobService;
import com.sms.server.service.LogService;
import com.sms.server.service.TranscodeService;
import com.sms.server.service.TranscodeService.TranscodeProfile;
import com.sms.server.service.transcode.AndroidAudioTranscode;
import com.sms.server.service.transcode.AndroidVideoTranscode;
import com.sms.server.service.transcode.KodiAudioTranscode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller which produces audio and video streams.
 *
 * @author Scott Ware
 */

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
    private KodiAudioTranscode kodiAudioTranscode;
    
    @Autowired
    private AndroidAudioTranscode androidAudioTranscode;
    
    @Autowired
    private AndroidVideoTranscode androidVideoTranscode;

    @RequestMapping(value="/initialise/{id}", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> initialiseStream(@PathVariable("id") Long id, HttpServletRequest request)
    {
        MediaElement mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Determine job type
        Byte jobType;
        
        if(mediaElement.getType().equals(MediaElementType.AUDIO)) { jobType = JobType.AUDIO_STREAM; }
        else if(mediaElement.getType().equals(MediaElementType.VIDEO)) { jobType = JobType.VIDEO_STREAM; }
        else { return new ResponseEntity<>(HttpStatus.BAD_REQUEST); }
        
        // Create a new job
        Job job = jobService.createJob(jobType, request.getUserPrincipal().getName(), mediaElement);
        
        if(job == null)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
                
        return new ResponseEntity<>(job.getID(), HttpStatus.OK);
    }
    
    @RequestMapping(value="/video", method=RequestMethod.GET)
    @ResponseBody
    public void getVideoStream(@RequestParam(value = "id", required = true) Long id, 
                               @RequestParam(value = "client", required = true) String client,
                               @RequestParam(value = "quality", required = true) Integer quality,
                               @RequestParam(value = "multichannel", required = false) Boolean multiChannel,
                               @RequestParam(value = "atrack", required = false) Integer audioTrack,
                               @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                               @RequestParam(value = "offset", required = false) Integer offset,
                               HttpServletRequest request,
                               HttpServletResponse response)
    {
        // Variables
        MediaElement mediaElement;
        Job job = null;
        long bytesTransferred = 0;
        boolean headerOnly = false;
        String[] ranges = {};
        Process process = null;
        
        /*********************** DEBUG: Get Request Headers *********************************/        
        String requestHeader = "\n***************\nRequest Header:\n***************\n";
	Enumeration requestHeaderNames = request.getHeaderNames();
        
	while (requestHeaderNames.hasMoreElements())
        {
            String key = (String) requestHeaderNames.nextElement();
            String value = request.getHeader(key);
            requestHeader += key + ": " + value + "\n";
            
            // Store certain headers for later use
            if(key.equals("range")) { ranges = value.substring("bytes=".length()).split("-"); }
	}
        
        // Print Headers
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
        
        // Determine if this is a content type request (request for 0 bytes)
        if(ranges.length > 1) { if(Integer.valueOf(ranges[1]).equals(0)) { headerOnly = true; LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "** Header Only Request **", null); } }
        
        try
        {
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Retrieve and check media element
            mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());

            if(mediaElement == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Media Element cannot be retrieved.");
                return;
            }
            
            // Build Transcode Profile
            TranscodeProfile profile = new TranscodeProfile();
            profile.setVideoQuality(quality);
            if(multiChannel != null) { profile.setMultiChannelEnabled(multiChannel); }
            if(audioTrack != null) { profile.setAudioTrack(audioTrack); }
            if(subtitleTrack != null) { profile.setSubtitleTrack(subtitleTrack); }
            if(offset != null) { profile.setOffset(offset); }
            
            List<String> command;
            
            // Determine which client we are transcoding for
            switch(client)
            {       
                case "android":
                    command = androidVideoTranscode.createTranscodeCommand(mediaElement, profile);
                    break;
                    
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognised or unsupported client.");
                    return;
            }
            
            if(command == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create transcoding command.");
                return;
            }
            
            // Set response header
            profile.setMimeType(TranscodeService.getAudioMimeType(TranscodeService.getAudioFormatFromCodec(profile.getAudioCodec())));
            response.setContentType(profile.getMimeType());
            
            // Tell client we don't support byte range requests.
            response.setIntHeader("ETag", mediaElement.getID().intValue());
            response.setHeader("Accept-Ranges", "none");
            
            /*********************** DEBUG: Get Response Headers *********************************/ 
            // Get Response Headers
            String responseHeader = "\n***************\nResponse Header:\n***************\n";
            Collection<String> responseHeaderNames = response.getHeaderNames();

            for(String header : responseHeaderNames)
            {
                String value = response.getHeader(header);
                responseHeader += header + ": " + value + "\n";
            }

            // Print Response Headers
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, responseHeader, null);
            
            /********************************************************************************/
            
            // If we only want to respond with a header we are done
            if(headerOnly) { response.setStatus(SC_OK); return; }
            
            //
            // Start transcoding
            //
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            process = processBuilder.start();
            InputStream input = process.getInputStream();
            OutputStream output = response.getOutputStream();

            // Set status code
            response.setStatus(SC_PARTIAL_CONTENT);
            
            // Start reading streams
            new StreamGobbler(process.getErrorStream()).start();

            // Buffer
            byte[] buffer = new byte[4096];
            int length;
            int count = 0;

            // Write stream to output
            while ((length = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, length);
                bytesTransferred += length;
                
                // Update job statistics
                count ++;
                
                if(count >= 200)
                {
                    jobDao.updateBytesTransferred(job.getID(), bytesTransferred);
                    count = 0;
                }
            }
        }
        catch (IOException ex) {
            // Called if client closes the connection early.
            if(process != null) { process.destroy(); }
        }
        finally
        {
            if(job != null && !headerOnly)
            {
                jobDao.updateBytesTransferred(id, bytesTransferred);
            }
        }
    }
    
    @RequestMapping(value="/audio/profile", method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<TranscodeProfile> getAudioTranscodeProfile(@RequestParam(value = "id", required = true) Long id,
                               @RequestParam(value = "client", required = true) String client,
                               @RequestParam(value = "quality", required = true) Integer quality,
                               @RequestParam(value = "multichannel", required = false) Boolean multiChannel,
                               @RequestParam(value = "maxsamplerate", required = false) Integer maxSampleRate)
    {
        // Retrieve and check media element
        MediaElement mediaElement = mediaDao.getMediaElementByID(id);

        if(mediaElement == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // Build Transcode Profile
        TranscodeProfile profile = new TranscodeProfile();
        profile.setAudioQuality(quality);
        if(multiChannel != null) { profile.setMultiChannelEnabled(multiChannel); }
        if(maxSampleRate != null) { profile.setMaxSampleRate(maxSampleRate); }

        // Determine which client we are transcoding for
        switch(client)
        {
            case "kodi":
                profile = kodiAudioTranscode.processTranscodeProfile(mediaElement, profile);
                break;
                
            case "android":
                profile = androidAudioTranscode.processTranscodeProfile(mediaElement, profile);
                break;

            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Return transcoded mime type
        profile.setMimeType(TranscodeService.getAudioMimeType(TranscodeService.getAudioFormatFromCodec(profile.getAudioCodec())));
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Content-Type: " + profile.getMimeType(), null);
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }
    
    @RequestMapping(value="/audio", method=RequestMethod.GET)
    @ResponseBody
    public void getAudioStream(@RequestParam(value = "id", required = true) Long id,
                               @RequestParam(value = "client", required = true) String client,
                               @RequestParam(value = "quality", required = true) Integer quality,
                               @RequestParam(value = "multichannel", required = false) Boolean multiChannel,
                               @RequestParam(value = "maxsamplerate", required = false) Integer maxSampleRate,
                               @RequestParam(value = "offset", required = false) Integer offset,
                               HttpServletRequest request,
                               HttpServletResponse response)
    {
        // Variables
        MediaElement mediaElement;
        Job job = null;
        long bytesTransferred = 0;
        boolean headerOnly = false;
        boolean autoEnd = false;
        String[] ranges = {};
        Process process = null;
        
        /*********************** DEBUG: Get Request Headers *********************************/        
        String requestHeader = "\n***************\nRequest Header:\n***************\n";
	Enumeration requestHeaderNames = request.getHeaderNames();
        
	while (requestHeaderNames.hasMoreElements())
        {
            String key = (String) requestHeaderNames.nextElement();
            String value = request.getHeader(key);
            requestHeader += key + ": " + value + "\n";
            
            // Store certain headers for later use
            if(key.equals("range")) { ranges = value.substring("bytes=".length()).split("-"); }
	}
        
        // Print Headers
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
        
        // Determine if this is a content type request (request for 0 bytes)
        if(ranges.length > 1) { if(Integer.valueOf(ranges[1]).equals(0)) { headerOnly = true; LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "** Header Only Request **", null); } }
        
        try
        {
            // Retrieve Job
            job = jobDao.getJobByID(id);
            
            if(job == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve job.");
                return;
            }
            
            // Retrieve and check media element
            mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());

            if(mediaElement == null)
            {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Media Element cannot be retrieved.");
                return;
            }
            
            // Build Transcode Profile
            TranscodeProfile profile = new TranscodeProfile();
            profile.setAudioQuality(quality);
            if(multiChannel != null) { profile.setMultiChannelEnabled(multiChannel); }
            if(maxSampleRate != null) { profile.setMaxSampleRate(maxSampleRate); }
            if(offset != null) { profile.setOffset(offset); }
            
            List<String> command;
            
            // Determine which client we are transcoding for
            switch(client)
            {
                case "kodi":
                    profile = kodiAudioTranscode.processTranscodeProfile(mediaElement, profile);
                    command = kodiAudioTranscode.createTranscodeCommand(mediaElement, profile);
                    autoEnd = true;
                    break;
                    
                case "android":
                    profile = androidAudioTranscode.processTranscodeProfile(mediaElement, profile);
                    command = androidAudioTranscode.createTranscodeCommand(mediaElement, profile);
                    autoEnd = false;
                    break;
                    
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognised or unsupported client.");
                    return;
            }
            
            if(command == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create transcoding command.");
                return;
            }
            
            // Set response header
            profile.setMimeType(TranscodeService.getAudioMimeType(TranscodeService.getAudioFormatFromCodec(profile.getAudioCodec())));
            response.setContentType(profile.getMimeType());
            
            // Tell client we don't support byte range requests.
            response.setIntHeader("ETag", mediaElement.getID().intValue());
            response.setHeader("Accept-Ranges", "none");
            
            if (mediaElement.getDuration() != null)
            {
                response.setHeader("X-Content-Duration", String.format("%.1f", mediaElement.getDuration().doubleValue()));
            }
            
            /*********************** DEBUG: Get Response Headers *********************************/ 
            // Get Response Headers
            String responseHeader = "\n***************\nResponse Header:\n***************\n";
            Collection<String> responseHeaderNames = response.getHeaderNames();

            for(String header : responseHeaderNames)
            {
                String value = response.getHeader(header);
                responseHeader += header + ": " + value + "\n";
            }

            // Print Response Headers
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, responseHeader, null);
            
            /********************************************************************************/
            
            // If we only want to respond with a header we are done
            if(headerOnly) { response.setStatus(SC_OK); return; }
            
            //
            // Start transcoding
            //
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            process = processBuilder.start();
            InputStream input = process.getInputStream();
            OutputStream output = response.getOutputStream();

            // Set status code
            response.setStatus(SC_PARTIAL_CONTENT);
            
            // Start reading streams
            new StreamGobbler(process.getErrorStream()).start();

            // Buffer
            byte[] buffer = new byte[4096];
            int length;
            int count = 0;

            // Write stream to output
            while ((length = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, length);
                bytesTransferred += length;
                
                // Update job statistics
                count ++;
                
                if(count >= 200)
                {
                    jobDao.updateBytesTransferred(job.getID(), bytesTransferred);
                    count = 0;
                }
            }
        }
        catch (IOException ex) {
            // Called if client closes the connection early.
            if(process != null) { process.destroy(); }
        }
        finally
        {
            if(job != null && !headerOnly)
            {
                jobDao.updateBytesTransferred(id, bytesTransferred);
                
                // If this isn't a manual seeking client end job automatically.
                if(autoEnd) { jobService.endJob(id); }
            }
        }
    }
}
