/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.dao.JobDao;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.Job.JobType;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.service.TranscodeService.TranscodeProfile;
import com.scooter1556.sms.server.service.transcode.DASHTranscode;
import com.scooter1556.sms.server.service.transcode.HLSTranscode;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author scott2ware
 */

@Service
public class AdaptiveStreamingService {
    
    private static final String CLASS_NAME = "AdaptiveStreamingService";
        
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private JobService jobService;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private TranscodeService transcodeService;
    
    @Autowired
    private HLSTranscode hlsTranscode;
    
    @Autowired
    private DASHTranscode dashTranscode;
        
    private final ArrayList<AdaptiveStreamingProcess> processes = new ArrayList<>();
    
    public Long initialise(String username, MediaElement mediaElement, AdaptiveStreamingProfile profile)
    {
        // Create a new job
        Job job = jobService.createJob(JobType.ADAPTIVE_STREAM, username, mediaElement);
        
        if(job == null)
        {
            return null;
        }
        
        // Check we know which format we are working with
        if(profile.getType() == null)
        {
            return null;
        }
        
        // Update profile
        profile.setOutputDirectory(new File(SettingsService.getHomeDirectory().getPath() + "/stream/" + job.getID()));
        
        // Get transcode command
        List<String> command = null;
        
        if(profile.getType() >= StreamType.DASH)
        {
            profile.setLastSegment(mediaElement.getDuration() / DASHTranscode.DEFAULT_SEGMENT_DURATION);
            command = dashTranscode.createTranscodeCommand(mediaElement, profile);
        }
        else if(profile.getType() == StreamType.HLS)
        {
            profile.setLastSegment(mediaElement.getDuration() / HLSTranscode.DEFAULT_SEGMENT_DURATION);
            command = hlsTranscode.createTranscodeCommand(mediaElement, profile);
        }
        
        if(command == null)
        {
            return null;
        }
        
        // Start transcoding
        AdaptiveStreamingProcess process = new AdaptiveStreamingProcess(job.getID(), command, 0, profile);
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, command.toString(), null);
        
        process.start();
        processes.add(process);
        
        return job.getID();
    }
    
    public List<String> generateHLSPlaylist(long id, String baseUrl) {
         
        List<String> playlist = new ArrayList<>();
        
        Job job = jobDao.getJobByID(id);
        
        if(job == null)
        {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null)
        {
            return null;
        }
        
        playlist.add("#EXTM3U");
        playlist.add("#EXT-X-VERSION:3");
        playlist.add("#EXT-X-TARGETDURATION:" + HLSTranscode.DEFAULT_SEGMENT_DURATION);
        playlist.add("#EXT-X-ALLOW-CACHE:YES");
        playlist.add("#EXT-X-MEDIA-SEQUENCE:0");
        playlist.add("#EXT-X-PLAYLIST-TYPE:VOD");

        // Get Video Segments
        for (int i = 0; i < (mediaElement.getDuration() / HLSTranscode.DEFAULT_SEGMENT_DURATION); i++)
        {
            playlist.add("#EXTINF:" + HLSTranscode.DEFAULT_SEGMENT_DURATION.floatValue() + ",");
            playlist.add(createHLSSegmentUrl(baseUrl, job.getID(), i));
        }   

        // Determine the duration of the final segment.
        Integer remainder = mediaElement.getDuration() % HLSTranscode.DEFAULT_SEGMENT_DURATION;
        if (remainder > 0)
        {
            playlist.add("#EXTINF:" + remainder.floatValue() + ",");
            playlist.add(createHLSSegmentUrl(baseUrl, job.getID(), mediaElement.getDuration() / HLSTranscode.DEFAULT_SEGMENT_DURATION));
        }

        playlist.add("#EXT-X-ENDLIST");
        
        return playlist;
    }
    
    private String createHLSSegmentUrl(String baseUrl, Long id, Integer segment)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl).append("/hls/stream.ts?id=").append(id).append("&segment=").append(segment);
        
        return builder.toString();
    }
    
    public Document generateDASHPlaylist(long id, String baseUrl) {
        
        Job job = jobDao.getJobByID(id);
        
        if(job == null)
        {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null)
        {
            return null;
        }
        
        AdaptiveStreamingProfile profile = getProfileByID(id);
        
        if(profile == null)
        {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to get transcode profile to generate manifest for job " + id, null);
            return null;
        }
        
        // Transcode Parameters
        Dimension resolution = transcodeService.getVideoResolution(profile.getVideoQuality(), mediaElement);
        String codec;
        String mimeType;
        
        switch(profile.getType())
        {
            case StreamType.DASH:
                codec = "avc1";
                mimeType = "video/MP2T";
                break;
                
            case StreamType.DASH_MP4:
                codec = "avc1";
                mimeType = "video/mp4";
                break;
                
            case StreamType.DASH_WEBM:
                codec = "vp8";
                mimeType = "video/webm";
                break;
                
            default:
                return null;
        }
        
        try
        {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // Root MPD Element
            Document mpd = docBuilder.newDocument();
            Element root = mpd.createElement("MPD");
            root.setAttribute("xmnls", "urn:mpeg:dash:schema:mpd:2011");
            root.setAttribute("profiles", "urn:mpeg:dash:profile:isoff-on-demand:2011");
            root.setAttribute("type", "static");
            root.setAttribute("mediaPresentationDuration", "PT" + mediaElement.getDuration() + "S");
            root.setAttribute("minBufferTime", "PT2.0S");
            mpd.appendChild(root);
            
            // Program Information
            Element programInformation = mpd.createElement("ProgramInformation");
            root.appendChild(programInformation);
            
            // Title
            Element title = mpd.createElement("Title");
            title.appendChild(mpd.createTextNode(mediaElement.getTitle()));
            programInformation.appendChild(title);
            
            // Period
            Element period = mpd.createElement("Period");
            period.setAttribute("duration", "PT" + mediaElement.getDuration() + "S");
            period.setAttribute("start", "PT0S");
            root.appendChild(period);
            
            // Adaptation Set
            Element adaptationSet = mpd.createElement("AdaptationSet");
            adaptationSet.setAttribute("bitstreamSwitching", "false");
            period.appendChild(adaptationSet);
            
            // Representation
            Element representation = mpd.createElement("Representation");
            representation.setAttribute("id", "1");
            representation.setAttribute("codecs", codec);
            representation.setAttribute("mimeType", mimeType);
            representation.setAttribute("width", String.valueOf(resolution.width));
            representation.setAttribute("height", String.valueOf(resolution.height));
            adaptationSet.appendChild(representation);
            
            // Segment List
            Element segmentList = mpd.createElement("SegmentList");
            segmentList.setAttribute("timescale", "1000");
            segmentList.setAttribute("duration", Integer.toString(DASHTranscode.DEFAULT_SEGMENT_DURATION * 1000));
            representation.appendChild(segmentList);
            
            // Initialisation
            Element initialisation = mpd.createElement("Initialization");
            initialisation.setAttribute("sourceURL", createDASHSegmentUrl(baseUrl, job.getID(), 0));
            segmentList.appendChild(initialisation);
            
            // Segment URLs
            for (int i = 1; i < (mediaElement.getDuration() / DASHTranscode.DEFAULT_SEGMENT_DURATION); i++)
            {
                Element segmentUrl = mpd.createElement("SegmentURL");
                segmentUrl.setAttribute("media", createDASHSegmentUrl(baseUrl, job.getID(), i));
                segmentList.appendChild(segmentUrl);
            }   

            // Determine the duration of the final segment.
            Integer remainder = mediaElement.getDuration() % DASHTranscode.DEFAULT_SEGMENT_DURATION;
            if (remainder > 0)
            {
                Element segmentUrl = mpd.createElement("SegmentURL");
                segmentUrl.setAttribute("media", createDASHSegmentUrl(baseUrl, job.getID(), mediaElement.getDuration() / HLSTranscode.DEFAULT_SEGMENT_DURATION));
                segmentList.appendChild(segmentUrl);
            }
            
            mpd.normalizeDocument();
        
            return mpd;
        } 
        catch (ParserConfigurationException ex) {
        }
        
        return null;
    }
    
    private String createDASHSegmentUrl(String baseUrl, Long id, Integer segment)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl).append("/dash/stream?id=").append(id).append("&segment=").append(segment);
        
        return builder.toString();
    }
    
    public AdaptiveStreamingProcess startProcessWithOffset(long jobID, int segment)
    {
        // Retrieve original adaptive streaming profile so it can be reused
        AdaptiveStreamingProfile profile = getProcessByID(jobID).getProfile();
        
        // End existing process if found
        endProcess(jobID);
        
        // Get existing job
        Job job = jobDao.getJobByID(jobID);
        
        if(job == null)
        {
            return null;
        }
        
        // Get media element
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null)
        {
            return null;
        }
        
        // Set offset in transcode profile
        profile.setSegmentOffset(segment);
        
        // Get transcode command
        List<String> command = null;
        
        if(profile.getType() >= StreamType.DASH)
        {
            profile.setLastSegment(mediaElement.getDuration() / DASHTranscode.DEFAULT_SEGMENT_DURATION);
            command = dashTranscode.createTranscodeCommand(mediaElement, profile);
        }
        else if(profile.getType() == StreamType.HLS)
        {
            profile.setLastSegment(mediaElement.getDuration() / HLSTranscode.DEFAULT_SEGMENT_DURATION);
            command = hlsTranscode.createTranscodeCommand(mediaElement, profile);
        }
        
        if(command == null)
        {
            return null;
        }
        
        // Start transcoding
        AdaptiveStreamingProcess process = new AdaptiveStreamingProcess(jobID, command, segment, profile);
        process.start();
        processes.add(process);
        
        return process;
    }
    
    public AdaptiveStreamingProcess getProcessByID(long id)
    {
        for(AdaptiveStreamingProcess process : processes)
        {
            if(process.getID().compareTo(id) == 0)
            {
                return process;
            }
        }
        
        return null;
    }
    
    public void removeProcessByID(long id)
    {
        int index = 0;
        
        for (AdaptiveStreamingProcess process : processes)
        {
            if(process.getID().compareTo(id) == 0)
            {
                break;
            }
            
            index ++;
        }
        
        processes.remove(index);
    }
    
    public void endProcess(long jobID)
    {   
        AdaptiveStreamingProcess process = getProcessByID(jobID);

        if(process != null)
        {
            process.end();
            removeProcessByID(jobID);
        }        
    }
    
    public AdaptiveStreamingProfile getProfileByID(long id)
    {
        AdaptiveStreamingProcess process = getProcessByID(id);
        
        if(process == null)
        {
            return null;
        }
        
        return process.getProfile();
    }
    
    public static class AdaptiveStreamingProfile extends TranscodeProfile
    {
        private Byte type;
        private Integer segmentOffset = 0, lastSegment = 0;
        private File outputDirectory;
        
        public Byte getType()
        {
            return type;
        }
        
        public void setType(byte type)
        {
            this.type = type;
        }
        
        public Integer getSegmentOffset()
        {
            return segmentOffset;
        }
        
        public void setSegmentOffset(int segmentOffset)
        {
            this.segmentOffset = segmentOffset;
        }
        
        public Integer getLastSegment()
        {
            return lastSegment;
        }
        
        public void setLastSegment(int lastSegment)
        {
            this.lastSegment = lastSegment;
        }
        
        public File getOutputDirectory()
        {
            return outputDirectory;
        }
        
        public void setOutputDirectory(File outputDirectory)
        {
            this.outputDirectory = outputDirectory;
        }
    }
    
    public static class StreamType {
        public static final byte HLS = 0;
        public static final byte DASH = 1;
        public static final byte DASH_WEBM = 2;
        public static final byte DASH_MP4 = 3;
    }
}
