package com.sms.server.controller;

import com.sms.server.dao.JobDao;
import com.sms.server.dao.MediaDao;
import com.sms.server.domain.Job;
import com.sms.server.domain.MediaElement;
import com.sms.server.domain.MediaElement.MediaElementType;
import com.sms.server.io.AdaptiveStreamingProcess;
import com.sms.server.service.LogService;
import com.sms.server.service.AdaptiveStreamingService;
import com.sms.server.service.AdaptiveStreamingService.AdaptiveStreamingProfile;
import com.sms.server.service.AdaptiveStreamingService.StreamType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.w3c.dom.Document;

/**
 * Controller which produces MPEG-DASH playlists.
 *
 * @author Scott Ware
 */

@Controller
@RequestMapping(value="/dash")
public class DASHController {

    private static final String CLASS_NAME = "DASHController";
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;

    @RequestMapping(method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> initialiseDASH(@RequestParam(value = "id", required = true) Long id,
                                              @RequestParam(value = "type", required = true) String type,
                                              @RequestParam(value = "quality", required = true) Integer quality,
                                              @RequestParam(value = "atrack", required = false) Integer audioTrack,
                                              @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                                              @RequestParam(value = "multichannel", required = false) Boolean multiChannel,
                                              HttpServletRequest request)
    {
        // Parameters for MPEG-DASH playlist generation.
        MediaElement mediaElement;

        mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
            
        // Check this is a video.
        if(mediaElement.getType() != MediaElementType.VIDEO)
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        // Create a new Adaptive Streaming profile
        AdaptiveStreamingProfile profile = new AdaptiveStreamingProfile();
        
        // Set Type
        switch(type)
        {
            case "mpegts":
                profile.setType(StreamType.DASH);
                break;
                
            case "webm":
                profile.setType(StreamType.DASH_WEBM);
                break;
                
            case "mp4":
                profile.setType(StreamType.DASH_MP4);
                break;
                
            default:
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Requested stream type not recognised.", null);
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        
        profile.setVideoQuality(quality);
        
        if(audioTrack != null)
        {
            profile.setAudioTrack(audioTrack);
        }
        
        if(subtitleTrack != null)
        {
            profile.setSubtitleTrack(subtitleTrack);
        }
        
        if(multiChannel != null)
        {
            profile.setMultiChannelEnabled(multiChannel);
        }
        
        // Initialise a new MPEG-DASH transcode and get it's unique job number
        Long jobID = adaptiveStreamingService.initialise(request.getUserPrincipal().getName(), mediaElement, profile);
        
        if(jobID == null)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to initialise new DASH job.", null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(jobID, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}/playlist", method=RequestMethod.GET)
    @ResponseBody
    public void getDASHPlaylist(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response)
    {          
        try {
            // Get the request base URL so we can use it in our DASH playlist
            String baseUrl = request.getRequestURL().toString().replaceFirst("/dash(.*)", "");
            
            // Get playlist
            Document playlist = adaptiveStreamingService.generateDASHPlaylist(id, baseUrl);
            
            if(playlist == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate DASH playlist.");
                return;
            }
            
            DOMSource input = new DOMSource(playlist);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Result result = new StreamResult(output);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(input,result);
            
            // Set Header Parameters
            response.setContentType("application/dash+xml");
            response.setContentLength(output.toString().length());
            
            response.getWriter().write(output.toString());
            response.getWriter().close();
            
        } catch (TransformerConfigurationException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error sending DASH playlist for job " + id, ex);
        } catch (TransformerException | IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error sending DASH playlist for job " + id, ex);
        }
        
    }
    
    @RequestMapping(value="/stream", method=RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<FileSystemResource> getDASHSegment(@RequestParam(value = "id", required = true) final Long id,
                                            @RequestParam(value = "segment", required = true) final Integer segmentNumber) throws InterruptedException
    {
        // Get associated job
        Job job = jobDao.getJobByID(id);
        
        // Get associated process
        AdaptiveStreamingProcess process = adaptiveStreamingService.getProcessByID(id);
        
        if(job == null)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to retrieve job " + id, null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        if(process == null)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to retrieve DASH process for job " + id, null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Get parameters from profile
        String extension;
        HttpHeaders responseHeaders = new HttpHeaders();
        
        switch(process.getProfile().getType())
        {
            case StreamType.DASH:
                extension = ".ts";
                responseHeaders.setContentType(new MediaType("video","MP2T"));
                break;
                
            case StreamType.DASH_WEBM:
                extension = ".webm";
                responseHeaders.setContentType(new MediaType("video","webm"));
                break;
                
            case StreamType.DASH_MP4:
                extension = ".mp4";
                responseHeaders.setContentType(new MediaType("video","mp4"));
                break;
                
            default:
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Stream type cannot be determined from profile.", null);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Segment Files
        File segment = new File(process.getProfile().getOutputDirectory() + "/" + "stream" + String.format("%05d", segmentNumber) + extension);
        File nextSegment = new File(process.getProfile().getOutputDirectory() + "/" + "stream" + String.format("%05d", segmentNumber + 1) + extension);
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Job ID: " + id + " Segment Requested: " + segmentNumber, null);
        
        // Flags
        boolean isLastSegment = segmentNumber.equals(process.getProfile().getLastSegment());
        
        // Check if the segment already exists.
        // We check the next segment is available (if not the last segment) to make sure the requested segment is fully transcoded.
        boolean isAvailable = isLastSegment ? segment.exists() : segment.exists() && nextSegment.exists();
        
        if(!isAvailable)
        {            
            // Check if the user has seeked to an un-transcoded segment
            if(!(segmentNumber.equals(process.getSegmentNumber()) || segmentNumber.equals(process.getSegmentNumber() + 1)))
            {              
                // Start a new transcode process starting with the requested segment
                process = adaptiveStreamingService.startProcessWithOffset(id, segmentNumber);

                // Check the new process started successfully
                if(process == null)
                {
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
                
            // Wait for the segment to become available
            while(!isAvailable && !process.hasEnded())
            {
                isAvailable = isLastSegment ? segment.exists() : nextSegment.exists();
                Thread.sleep(500);
            }
        }
        
        // If the segment is available return it to the client
        if(isAvailable)
        {
            // Update process segment tracking
            process.setSegmentNumber(segmentNumber);
            
            // Update bytes transferred
            Long bytesTransferred = job.getBytesTransferred() + segment.length();
            jobDao.updateBytesTransferred(id, bytesTransferred);

            return new ResponseEntity<>(new FileSystemResource(segment), responseHeaders, HttpStatus.OK);
        }
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
