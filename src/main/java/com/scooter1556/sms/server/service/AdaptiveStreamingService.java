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

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.io.AdaptiveStreamingProcess;
import com.scooter1556.sms.server.utilities.MediaUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdaptiveStreamingService {
    
    private static final String CLASS_NAME = "AdaptiveStreamingService";
    
    public static final Integer DASH_SEGMENT_DURATION = 5;
    
    // The number of stream alternatives to transcode by default
    public static final Integer DEFAULT_STREAM_COUNT = 2;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private TranscodeService transcodeService;
    
    private final ArrayList<AdaptiveStreamingProcess> processes = new ArrayList<>();

    public AdaptiveStreamingProcess initialise(Job job, int num) {        
        // Get offset
        if(num > 0) {
            // Start transcoding from the previous segment
            num -= 1;
            job.getTranscodeProfile().setOffset(num * job.getTranscodeProfile().getSegmentDuration());
        }
        
        // Get transcode command
        String[][] commands = transcodeService.getTranscodeCommand(job);
        
        if(commands == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to get transcode command for profile: " + job.getTranscodeProfile(), null);
            return null;
        }
        
        // Start transcoding
        AdaptiveStreamingProcess process = getProcessById(job.getId());
        
        if(process == null) {
            process = new AdaptiveStreamingProcess(job.getId());
            processes.add(process);
        }
        
        // Update process with required information
        process.setCommands(commands);
        process.setMediaElement(job.getMediaElement());
        process.setTranscodeProfile(job.getTranscodeProfile());
        process.setTranscoder(transcodeService.getTranscoder());
            
        process.initialise();
        
        return process;
    }
    
    public List<String> generateHLSVariantPlaylist(Job job, ClientProfile clientProfile) {
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = job.getMediaElement();
        
        if(mediaElement == null) {
            return null;
        }
        
        TranscodeProfile profile = job.getTranscodeProfile();
        
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
                
                if(clientProfile.getAudioQuality() != null) {
                    bandwidth = (TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[clientProfile.getAudioQuality()] * 1000);
                }
                
                if(bandwidth < 0) {
                    bandwidth = 384000;
                }
                
                playlist.add("#EXT-X-STREAM-INF:PROGRAM-ID=1, BANDWIDTH=" + bandwidth + ", CODECS=\"" + TranscodeUtils.getIsoSpecForCodec(transcode.getCodec()) + "\"");
                playlist.add(clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/audio/" + i);
            }
        } else if(mediaElement.getType() == MediaElementType.VIDEO && profile.getVideoTranscodes() != null) {
            String audio = "";
            boolean subtitles = false;
            
            // Process audio streams
            if(profile.getAudioTranscodes() != null) {
                for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[a];
                    AudioStream stream = TranscodeUtils.getAudioStreamById(mediaElement.getAudioStreams(), transcode.getId());
                    String isDefault = "NO";
                    
                    if(transcode.getId().equals(profile.getAudioStream())) {
                        isDefault = "YES";
                    }

                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        audio = TranscodeUtils.getIsoSpecForCodec(stream.getCodec());
                    } else {
                        audio = TranscodeUtils.getIsoSpecForCodec(transcode.getCodec());
                    }
                    
                    playlist.add("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",LANGUAGE=\"" + stream.getLanguage() + "\",NAME=\"" + MediaUtils.getTitleForStream(stream.getTitle(), stream.getLanguage()) + "\",AUTOSELECT=YES,DEFAULT=" + isDefault + ",URI=\"" + clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/audio/" + a + "\"");
                }                
            }
            
            // Process subtitle streams
            if(profile.getSubtitleTranscodes() != null) {
                for(int s = 0; s < profile.getSubtitleTranscodes().length; s++) {
                    SubtitleTranscode transcode = profile.getSubtitleTranscodes()[s];
                    SubtitleStream stream = TranscodeUtils.getSubtitleStreamById(mediaElement.getSubtitleStreams(), transcode.getId());
                    String isDefault = "NO";
                    
                    if(profile.getSubtitleStream() != null) {
                        if(transcode.getId().equals(profile.getSubtitleStream())) {
                            isDefault = "YES";
                        }
                    }
                    
                    playlist.add("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"subs\",LANGUAGE=\"" + stream.getLanguage() + "\",NAME=\"" + MediaUtils.getTitleForStream(stream.getTitle(), stream.getLanguage()) + "\",AUTOSELECT=YES,DEFAULT=" + isDefault + ",URI=\"" + clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/subtitle/" + s + "\"");
                    
                    subtitles = true;
                }                
            }
            
            for(int i = 0; i < TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).size(); i++) {
                VideoTranscode transcode = TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).get(i);
                
                // Get video stream
                VideoStream videoStream = MediaUtils.getVideoStreamById(mediaElement.getVideoStreams(), transcode.getId());
                
                // Determine bitrate
                int bitrate = -1;
                
                if(transcode.getQuality() != null) {
                    bitrate = TranscodeUtils.VIDEO_QUALITY_BITRATE[transcode.getQuality()];
                }
                
                if(bitrate < 0) {
                    bitrate = MediaUtils.getAverageBitrate(videoStream, mediaElement.getBitrate());
                }
                                
                // Determine resolution
                Dimension resolution = transcode.getResolution();
                
                if(resolution == null) {
                    resolution = videoStream.getResolution();
                }
                
                StringBuilder builder = new StringBuilder();
                builder.append("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=");
                builder.append(String.valueOf(bitrate * 1000));
                builder.append(",RESOLUTION=").append(String.format("%dx%d", resolution.width, resolution.height));
                builder.append(",CLOSED-CAPTIONS=NONE");
                builder.append(",CODECS=\"");
                
                if(transcode.getCodec() == SMS.Codec.COPY) {
                    builder.append(TranscodeUtils.getIsoSpecForCodec(videoStream.getCodec()));
                } else {
                    builder.append(TranscodeUtils.getIsoSpecForCodec(transcode.getCodec()));
                }
                
                if(!audio.isEmpty()) {
                    builder.append(",").append(audio).append("\",AUDIO=\"audio\"");
                }
                
                
                if(subtitles) {
                    builder.append(",SUBTITLES=\"subs\"");
                }
                                
                playlist.add(builder.toString());
                
                // Url
                playlist.add(clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/video/" + i);
            }
        } else {
            return null;
        }
        
        return playlist;
    }
    
    public List<String> generateHLSPlaylist(Job job, ClientProfile clientProfile, String type, Integer extra) {
        // Check variables
        if(type == null || extra == null) {
            return null;
        }
                
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = job.getMediaElement();
        
        if(mediaElement == null) {
            return null;
        }
        
        List<String> playlist = new ArrayList<>();
        
        playlist.add("#EXTM3U");
        playlist.add("#EXT-X-VERSION:4");
        playlist.add("#EXT-X-TARGETDURATION:" + String.valueOf(job.getTranscodeProfile().getSegmentDuration() + 1));
        playlist.add("#EXT-X-MEDIA-SEQUENCE:0");
        playlist.add("#EXT-X-PLAYLIST-TYPE:VOD");
        
        // Get Video Segments
        for (int i = 0; i < Math.floor(mediaElement.getDuration() / job.getTranscodeProfile().getSegmentDuration()); i++) {
            playlist.add("#EXTINF:" + job.getTranscodeProfile().getSegmentDuration().floatValue() + ",");
            playlist.add(clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/" + type + "/" + extra + "/" + i);
        }   

        // Determine the duration of the final segment.
        double remainder = mediaElement.getDuration() % job.getTranscodeProfile().getSegmentDuration();
        if (remainder > 0) {
            long i = Double.valueOf(Math.floor(mediaElement.getDuration() / job.getTranscodeProfile().getSegmentDuration())).longValue();
            
            playlist.add("#EXTINF:" + Precision.round(remainder, 1, BigDecimal.ROUND_HALF_UP) + ",");
            playlist.add(clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/" + type + "/" + extra + "/" + i);
        }

        playlist.add("#EXT-X-ENDLIST");
        
        return playlist;
    }
    
    public void sendHLSPlaylist(Job job, ClientProfile clientProfile, String type, Integer extra, boolean head, HttpServletResponse response) throws IOException {
        List<String> playlist;
        
        // Get playlist as a string array
        if(type == null) {
            playlist = generateHLSVariantPlaylist(job, clientProfile);
        } else {
            playlist = generateHLSPlaylist(job, clientProfile, type, extra);
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
        
        // If this is a HEAD request we are done
        if(head) {
            return;
        }

        // Write playlist out to the client
        response.getWriter().write(playlistWriter.toString());
        
        // Log playlist
        LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, "\n************\nHLS Playlist\n************\n" + playlistWriter.toString(), null);
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
    
    public boolean isProcessAvailable(UUID id) {
        return getProcessById(id) != null;
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
