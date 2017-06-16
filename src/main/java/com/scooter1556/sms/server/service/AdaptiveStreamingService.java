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
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.VideoTranscode.VideoQuality;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class AdaptiveStreamingService {
    
    private static final String CLASS_NAME = "AdaptiveStreamingService";
    
    public static final Integer HLS_SEGMENT_DURATION = 10;
    public static final Integer DASH_SEGMENT_DURATION = 5;
        
    @Autowired
    private JobDao jobDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private TranscodeService transcodeService;
    
    private final ArrayList<AdaptiveStreamingProcess> processes = new ArrayList<>();

    public AdaptiveStreamingProcess initialise(TranscodeProfile profile, int num) {
        // Check that this is an adaptive streaming job
        if(profile.getType() != StreamType.TRANSCODE) {
            return null;
        }
        
        // Get offset
        if(num > 0) {
            // Start transcoding from the previous segment
            num -= 1;
            profile.setOffset(num * HLS_SEGMENT_DURATION);
        }
        
        // Get transcode command
        String[][] commands = transcodeService.getTranscodeCommand(profile);
        
        if(commands == null) {
            return null;
        }
        
        // Start transcoding
        AdaptiveStreamingProcess process = getProcessById(profile.getID());
        
        if(process == null) {
            process = new AdaptiveStreamingProcess(profile.getID());
            processes.add(process);
        }
        
        process.setCommands(commands);
        process.initialise();
        
        return process;
    }
    
    public DOMSource generateDashPlaylist(UUID id, String baseUrl) {
        Job job = jobDao.getJobByID(id);
        
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null) {
            return null;
        }
        
        baseUrl = baseUrl + "/stream/segment/" + id + "/";
        
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;
            docBuilder = docFactory.newDocumentBuilder();

            // Root elements
            Document playlist = docBuilder.newDocument();
            Element mpd = playlist.createElement("MPD");
            playlist.appendChild(mpd);

            mpd.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            mpd.setAttribute("xmlns", "urn:mpeg:dash:schema:mpd:2011");
            mpd.setAttribute("xsi:schemaLocation", "urn:mpeg:DASH:schema:MPD:2011 DASH-MPD.xsd");
            //mpd.setAttribute("profiles", "urn:mpeg:dash:profile:full:2011");
            mpd.setAttribute("profiles", "urn:mpeg:dash:profile:isoff-live:2011");
            mpd.setAttribute("minBufferTime", "PT"+ DASH_SEGMENT_DURATION + "S");
            mpd.setAttribute("type", "static");
            mpd.setAttribute("mediaPresentationDuration", "PT" + mediaElement.getDuration() + "S");

            Element period = playlist.createElement("Period");
            mpd.appendChild(period);

            period.setAttribute("duration", "PT" + mediaElement.getDuration() + "S");

            Element adaptationSet = playlist.createElement("AdaptationSet");
            period.appendChild(adaptationSet);

            adaptationSet.setAttribute("segmentAlignment", "true");
            adaptationSet.setAttribute("contentType", "audio");

            Element representation = playlist.createElement("Representation");
            adaptationSet.appendChild(representation);

            representation.setAttribute("id", "audio");
            representation.setAttribute("mimeType", "audio/mp4");
            representation.setAttribute("codecs", "mp4a.40.2");
            representation.setAttribute("audioSamplingRate", "44100");
            representation.setAttribute("bandwidth", "128000");
            
            Element audioChannelConfig = playlist.createElement("AudioChannelConfiguration");
            representation.appendChild(audioChannelConfig);
            
            audioChannelConfig.setAttribute("schemeIdUri", "urn:mpeg:dash:23003:3:audio_channel_configuration:2011");
            audioChannelConfig.setAttribute("value", "2");
            
            Element segmentTemplate = playlist.createElement("SegmentTemplate");
            representation.appendChild(segmentTemplate);

            segmentTemplate.setAttribute("duration", "5000");
            segmentTemplate.setAttribute("initialization", baseUrl + "init-stream0.m4s");
            segmentTemplate.setAttribute("media", baseUrl + "chunk-stream0-$Number%05d$.m4s");
            segmentTemplate.setAttribute("startNumber", "1");
            
            DOMSource result = new DOMSource(playlist);
            return result;
        } catch (ParserConfigurationException ex) {
            return null;
        }
    }
    
    public void sendDashPlaylist(UUID id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Get the request base URL so we can use it in our playlist
            String baseUrl = request.getRequestURL().toString().replaceFirst("/stream(.*)", "");
            
            // Get playlist
            DOMSource playlist = generateDashPlaylist(id, baseUrl);
            
            if(playlist == null) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate Dash playlist.");
                return;
            }
            
            // Write playlist to buffer
            StringWriter playlistWriter = new StringWriter();
            StreamResult result = new StreamResult(playlistWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(playlist, result);
            
            // Set Header Parameters
            response.setContentType("application/dash+xml");
            response.setContentLength(playlistWriter.toString().length());

            // Write playlist out to the client
            response.getWriter().write(playlistWriter.toString());
        } catch (TransformerException ex) {
            Logger.getLogger(AdaptiveStreamingService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public List<String> generateHLSVariantPlaylist(UUID id, String baseUrl) {
        Job job = jobDao.getJobByID(id);
        
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null) {
            return null;
        }
        
        TranscodeProfile profile = transcodeService.getTranscodeProfile(id);
        
        if(profile == null) {
            return null;
        }        
        
        List<String> playlist = new ArrayList<>();
        
        playlist.add("#EXTM3U");
        
        if(mediaElement.getType() == MediaElementType.AUDIO && profile.getAudioTranscodes() != null) {
            for(int i = 0; i < profile.getAudioTranscodes().length; i++) {
                AudioTranscode transcode = profile.getAudioTranscodes()[i];
                
                // Get audio bandwidth
                int bandwidth = -1;
                
                if(profile.getQuality() != null) {
                    bandwidth = (TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[profile.getQuality()] * 1000);
                }
                
                if(bandwidth < 0) {
                    bandwidth = 384000;
                }
                
                playlist.add("#EXT-X-STREAM-INF:PROGRAM-ID=1, BANDWIDTH=" + bandwidth + ", CODECS=\"" + TranscodeUtils.getIsoSpecForAudioCodec(transcode.getCodec()) + "\"");
                playlist.add(baseUrl + "/stream/playlist/" + id + "/audio/" + i + ".m3u8");
            }
        } else if(mediaElement.getType() == MediaElementType.VIDEO && profile.getVideoTranscode() != null) {
            String audio = "";
            boolean subtitles = false;
            
            // Process audio streams
            if(profile.getAudioTranscodes() != null) {
                for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[a];
                    AudioStream stream = mediaElement.getAudioStreams().get(a);
                    String selected = "NO";
                    
                    if(profile.getAudioTrack() != null) {
                        if(profile.getAudioTrack().equals(a)) {
                            selected = "YES";
                        }
                    }

                    if(transcode.getCodec().equals("copy")) {
                        audio = TranscodeUtils.getIsoSpecForAudioCodec(mediaElement.getAudioStreams().get(a).getCodec());
                    } else {
                        audio = TranscodeUtils.getIsoSpecForAudioCodec(transcode.getCodec());
                    }
                    
                    playlist.add("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",LANGUAGE=\"" + stream.getLanguage() + "\",NAME=\"" + stream.getName() + "\",AUTOSELECT=YES,DEFAULT=" + selected + ",URI=\"" + baseUrl + "/stream/playlist/" + id + "/audio/" + a + ".m3u8\"");
                }                
            }
            
            // Process subtitle streams
            if(profile.getSubtitleTranscodes() != null) {
                for(int s = 0; s < profile.getSubtitleTranscodes().length; s++) {
                    SubtitleTranscode transcode = profile.getSubtitleTranscodes()[s];
                    
                    // If this subtitle needs to be hardcoded skip it
                    if(transcode.isHardcoded()) {
                        continue;
                    }
                    
                    SubtitleStream stream = mediaElement.getSubtitleStreams().get(s);
                    String selected = "NO";
                    
                    if(profile.getSubtitleTrack() != null) {
                        if(profile.getSubtitleTrack().equals(s)) {
                            selected = "YES";
                        }
                    }
                    
                    playlist.add("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"subs\",LANGUAGE=\"" + stream.getLanguage() + "\",NAME=\"" + stream.getName() + "\",AUTOSELECT=YES,DEFAULT=" + selected + ",URI=\"" + baseUrl + "/stream/playlist/" + id + "/subtitle/" + s + ".m3u8\"");
                    
                    subtitles = true;
                }                
            }
            
            // If client doesn't support bitrate switching just give them one variant  
            int offset;
            
            if(profile.getClient() == null) {
                offset = 0;
            } else {
                switch(profile.getClient()) {
                    case "kodi":
                        offset = profile.getQuality();
                        break;

                    default:
                        offset = 0;
                        break;
                }
            }
            
            for(int i = offset; i <= profile.getQuality(); i++) {
                Dimension resolution = TranscodeUtils.VIDEO_QUALITY_RESOLUTION[i];
                
                StringBuilder builder = new StringBuilder();
                builder.append("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=");
                builder.append(String.valueOf(TranscodeUtils.VIDEO_QUALITY_MAX_BITRATE[i] * 1000));
                builder.append(",RESOLUTION=").append(String.format("%dx%d", resolution.width, resolution.height));
                builder.append(",CLOSED-CAPTIONS=NONE");
                builder.append(",CODECS=\"");
                
                if(profile.getQuality() > VideoQuality.HIGH) {
                    builder.append("avc1.640028");
                } else {
                    builder.append("avc1.42e01e");
                }
                
                if(!audio.isEmpty()) {
                    builder.append(",").append(audio).append("\",AUDIO=\"audio\"");
                }
                
                if(subtitles) {
                    builder.append(",SUBTITLES=\"subs\"");
                }
                
                playlist.add(builder.toString());
                
                // Url
                playlist.add(baseUrl + "/stream/playlist/" + id + "/video/" + i + ".m3u8");
            }
        } else {
            return null;
        }
        
        return playlist;
    }
    
    public List<String> generateHLSPlaylist(UUID id, String baseUrl, String type, Integer extra) {
        // Check variables
        if(type == null || extra == null) {
            return null;
        }
        
        Job job = jobDao.getJobByID(id);
        
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = mediaDao.getMediaElementByID(job.getMediaElement());
        
        if(mediaElement == null) {
            return null;
        }
        
        List<String> playlist = new ArrayList<>();
        
        playlist.add("#EXTM3U");
        playlist.add("#EXT-X-VERSION:4");
        playlist.add("#EXT-X-TARGETDURATION:" + String.valueOf(HLS_SEGMENT_DURATION + 1));
        playlist.add("#EXT-X-MEDIA-SEQUENCE:0");
        playlist.add("#EXT-X-PLAYLIST-TYPE:VOD");
        
        // Get Video Segments
        for (int i = 0; i < (mediaElement.getDuration() / HLS_SEGMENT_DURATION); i++) {
            playlist.add("#EXTINF:" + HLS_SEGMENT_DURATION.floatValue() + ",");
            playlist.add(baseUrl + "/stream/segment/" + id + "/" + type + "/" + extra + "/" + i);
        }   

        // Determine the duration of the final segment.
        Integer remainder = mediaElement.getDuration() % HLS_SEGMENT_DURATION;
        if (remainder > 0) {
            int i = mediaElement.getDuration() / HLS_SEGMENT_DURATION;
            
            playlist.add("#EXTINF:" + remainder.floatValue() + ",");
            playlist.add(baseUrl + "/stream/segment/" + id + "/" + type + "/" + extra + "/" + i);
        }

        playlist.add("#EXT-X-ENDLIST");
        
        return playlist;
    }
    
    public void sendHLSPlaylist(UUID id, String type, Integer extra, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Get the request base URL so we can use it in our playlist
        String baseUrl = request.getRequestURL().toString().replaceFirst("/stream(.*)", "");

        List<String> playlist;
        
        // Get playlist as a string array
        if(type == null) {
            playlist = generateHLSVariantPlaylist(id, baseUrl);
        } else {
            playlist = generateHLSPlaylist(id, baseUrl, type, extra);
        }

        if(playlist == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to generate HLS playlist.", null);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate HLS playlist.");
            return;
        }

        // Write playlist to buffer so we can get the content length
        StringWriter playlistWriter = new StringWriter();
        for(String line : playlist) {
            playlistWriter.write(line + "\n");
        }

        // Set Header Parameters
        response.reset();
        response.setContentType("application/x-mpegurl");
        response.setContentLength(playlistWriter.toString().length());
        
        // Enable CORS
        response.setHeader(("Access-Control-Allow-Origin"), "*");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setIntHeader("Access-Control-Max-Age", 3600);

        // Write playlist out to the client
        response.getWriter().write(playlistWriter.toString());
        
        /*********************** DEBUG: Response Headers *********************************/        
        String requestHeader = "\n***************\nResponse Header:\n***************\n";
	Collection<String> responseHeaderNames = response.getHeaderNames();
        
	for(int i = 0; i < responseHeaderNames.size(); i++) {
            String header = (String) responseHeaderNames.toArray()[i];
            String value = response.getHeader(header);
            requestHeader += header + ": " + value + "\n";
        }
        
        // Log Headers
        LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
        
        // Log playlist
        LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, "\n************\nHLS Playlist\n************\n" + playlistWriter.toString(), null);
    }
    
    public void sendSubtitleSegment(HttpServletResponse response) throws IOException {
        List<String> segment = new ArrayList<>();
        segment.add("WEBVTT");
 
        // Write segment to buffer so we can get the content length
        StringWriter segmentWriter = new StringWriter();
        for(String line : segment) {
            segmentWriter.write(line + "\n");
        }

        // Set Header Parameters
        response.reset();
        response.setContentType("text/vtt");
        response.setContentLength(segmentWriter.toString().length());
        
        // Enable CORS
        response.setHeader(("Access-Control-Allow-Origin"), "*");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setIntHeader("Access-Control-Max-Age", 3600);

        // Write segment out to the client
        response.getWriter().write(segmentWriter.toString());
    }
    
    public void addProcess(AdaptiveStreamingProcess process) {
        if(process != null) {
            processes.add(process);
        }
    }
    
    public AdaptiveStreamingProcess getProcessById(UUID id) {
        for(AdaptiveStreamingProcess process : processes) {
            if(process.getId().compareTo(id) == 0) {
                return process;
            }
        }
        
        return null;
    }
     
    public void removeProcessById(UUID id) {
        int index = 0;
        
        for (AdaptiveStreamingProcess process : processes) {
            if(process.getId().compareTo(id) == 0) {
                processes.remove(index);
                break;
            }
            
            index ++;
        }
    }
     
    public void endProcess(UUID id) {   
        AdaptiveStreamingProcess process = getProcessById(id);
 
        if(process != null) {
            process.end();
            removeProcessById(id);
        }        
    }
}
