package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.AdaptiveStreamingService;
import com.scooter1556.sms.server.service.AdaptiveStreamingService.AdaptiveStreamingProfile;
import com.scooter1556.sms.server.service.AdaptiveStreamingService.StreamType;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller which produces HLS (Http Live Streaming) playlists.
 *
 * @author Scott Ware
 */

@Controller
@RequestMapping(value="/hls")
public class HLSController {

    private static final String CLASS_NAME = "HLSController";
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;

    @RequestMapping(method=RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Long> initialiseHLS(@RequestParam(value = "id", required = true) Long id,
                                              @RequestParam(value = "quality", required = true) Integer quality,
                                              @RequestParam(value = "atrack", required = false) Integer audioTrack,
                                              @RequestParam(value = "strack", required = false) Integer subtitleTrack,
                                              @RequestParam(value = "multichannel", required = false) Boolean multiChannel,
                                              HttpServletRequest request)
    {
        // Parameters for HLS playlist generation.
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
        
        profile.setType(StreamType.HLS);
        
        if(quality != null)
        {
            profile.setVideoQuality(quality);
        }
        
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
        
        // Initialise a new HLS transcode and get it's unique job number
        Long jobID = adaptiveStreamingService.initialise(request.getUserPrincipal().getName(), mediaElement, profile);
        
        if(jobID == null)
        {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(jobID, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}/playlist", method=RequestMethod.GET)
    @ResponseBody
    public void getHLSPlaylist(@PathVariable("id") Long id, HttpServletRequest request, HttpServletResponse response)
    {        
        try
        {
            // Get the request base URL so we can use it in our HLS playlist
            String baseUrl = request.getRequestURL().toString().replaceFirst("/hls(.*)", "");
            
            // Get playlist as a string array
            List<String> playlist = adaptiveStreamingService.generateHLSPlaylist(id, baseUrl);
            
            if(playlist == null)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate HLS playlist.");
                return;
            }
            
            // Write playlist to buffer so we can get the content length
            StringWriter playlistWriter = new StringWriter();
            for(String line : playlist) { playlistWriter.write(line + "\n"); }
            
            // Set Header Parameters
            response.setContentType("application/x-mpegurl");
            response.setContentLength(playlistWriter.toString().length());
            
            // Write playlist out to the client
            response.getWriter().write(playlistWriter.toString());
        } 
        catch (IOException ex)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error sending HLS playlist for job " + id + " to client.", ex);
        }
    }
    
    @RequestMapping(value="/stream.ts", method=RequestMethod.GET, produces = "video/MP2T")
    public @ResponseBody
    ResponseEntity<FileSystemResource> getHLSSegment(@RequestParam(value = "id", required = true) final Long id,
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
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to retrieve HLS process for job " + id, null);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        // Segment Files
        File segment = new File(process.getProfile().getOutputDirectory() + "/" + "stream" + String.format("%05d", segmentNumber) + ".ts");
        File nextSegment = new File(process.getProfile().getOutputDirectory() + "/" + "stream" + String.format("%05d", segmentNumber + 1) + ".ts");
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
            // Update HLS process segment tracking
            process.setSegmentNumber(segmentNumber);
            
            // Update bytes transferred
            Long bytesTransferred = job.getBytesTransferred() + segment.length();
            jobDao.updateBytesTransferred(id, bytesTransferred);

            return new ResponseEntity<>(new FileSystemResource(segment), HttpStatus.OK);
        }
        
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
