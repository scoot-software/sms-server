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

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.service.TranscodeService.StreamType;
import com.scooter1556.sms.server.service.TranscodeService.TranscodeProfile;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdaptiveStreamingService {
    
    private static final String CLASS_NAME = "AdaptiveStreamingService";
        
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private TranscodeService transcodeService;
        
    private final ArrayList<AdaptiveStreamingProcess> processes = new ArrayList<>();
    
    public AdaptiveStreamingProcess initialise(TranscodeProfile profile) {
        // Check that this is an adaptive streaming job
        if(profile.getType() != StreamType.ADAPTIVE) {
            return null;
        }
        
        // Get transcode command
        List<String> command = transcodeService.getTranscodeCommand(profile);
        
        if(command == null) {
            return null;
        }
        
        // Start transcoding
        AdaptiveStreamingProcess process = new AdaptiveStreamingProcess(profile.getID(), command, 0);
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, command.toString(), null);
        
        process.initialise();
        processes.add(process);
        
        return process;
    }
    
    public AdaptiveStreamingProcess startProcessWithOffset(long jobID, int segment) {
        // End existing process if found
        endProcess(jobID);
        
        // Get existing job
        Job job = jobDao.getJobByID(jobID);
        
        if(job == null) {
            return null;
        }
        
        // Retrieve original adaptive streaming profile so it can be reused
        TranscodeProfile profile = transcodeService.getTranscodeProfile(jobID);
        
        if(profile == null) {
            return null;
        }
        
        // Set offset
        profile.setOffset(segment);
        
        // Start a new transcoding process
        return initialise(profile);
    }
    
    public List<String> generateHLSPlaylist(long id, String baseUrl) {
        List<String> playlist = new ArrayList<>();
        
        Job job = jobDao.getJobByID(id);
        
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null) {
            return null;
        }
        
        playlist.add("#EXTM3U");
        playlist.add("#EXT-X-VERSION:3");
        playlist.add("#EXT-X-TARGETDURATION:" + TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION);
        playlist.add("#EXT-X-ALLOW-CACHE:YES");
        playlist.add("#EXT-X-MEDIA-SEQUENCE:0");
        playlist.add("#EXT-X-PLAYLIST-TYPE:VOD");

        // Get Video Segments
        for (int i = 0; i < (mediaElement.getDuration() / TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION); i++) {
            playlist.add("#EXTINF:" + TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION.floatValue() + ",");
            playlist.add(createSegmentUrl(baseUrl, job.getID(), i));
        }   

        // Determine the duration of the final segment.
        Integer remainder = mediaElement.getDuration() % TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION;
        if (remainder > 0) {
            playlist.add("#EXTINF:" + remainder.floatValue() + ",");
            playlist.add(createSegmentUrl(baseUrl, job.getID(), mediaElement.getDuration() / TranscodeService.ADAPTIVE_STREAMING_SEGMENT_DURATION));
        }

        playlist.add("#EXT-X-ENDLIST");
        
        return playlist;
    }
    
    public void sendHLSPlaylist(Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get the request base URL so we can use it in our HLS playlist
        String baseUrl = request.getRequestURL().toString().replaceFirst("/stream(.*)", "");

        // Get playlist as a string array
        List<String> playlist = generateHLSPlaylist(id, baseUrl);

        if(playlist == null) {
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
    
    private String createSegmentUrl(String baseUrl, Long id, Integer segment) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl).append("/stream/segment?id=").append(id).append("&num=").append(segment);
        
        return builder.toString();
    }
    
    public void addProcess(AdaptiveStreamingProcess process) {
        if(process != null) {
            processes.add(process);
        }
    }
    
    public AdaptiveStreamingProcess getProcessByID(long id) {
        for(AdaptiveStreamingProcess process : processes) {
            if(process.getID().compareTo(id) == 0) {
                return process;
            }
        }
        
        return null;
    }
    
    public void removeProcessByID(long id) {
        int index = 0;
        
        for (AdaptiveStreamingProcess process : processes) {
            if(process.getID().compareTo(id) == 0) {
                processes.remove(index);
                break;
            }
            
            index ++;
        }
    }
    
    public void endProcess(long jobID) {   
        AdaptiveStreamingProcess process = getProcessByID(jobID);

        if(process != null) {
            process.end();
            removeProcessByID(jobID);
        }        
    }
}
