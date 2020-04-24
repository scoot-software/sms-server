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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class AdaptiveStreamingService {
    
    private static final String CLASS_NAME = "AdaptiveStreamingService";
        
    // The number of stream alternatives to transcode by default
    public static final Integer DEFAULT_STREAM_COUNT = 2;
    
    @Autowired
    private TranscodeService transcodeService;
    
    private final ArrayList<AdaptiveStreamingProcess> processes = new ArrayList<>();

    public AdaptiveStreamingProcess initialise(Job job, int num) {        
        // Set offset
        if(num > 0) {
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
    
    public DOMSource generateDashPlaylist(Job job, ClientProfile clientProfile) {        
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
            mpd.setAttribute("profiles", "urn:mpeg:dash:profile:isoff-on-demand:2011");
            mpd.setAttribute("minBufferTime", "PT"+ String.valueOf(job.getTranscodeProfile().getSegmentDuration()) + "S");
            mpd.setAttribute("type", "static");
            mpd.setAttribute("mediaPresentationDuration", "PT" + mediaElement.getDuration() + "S");

            Element period = playlist.createElement("Period");
            mpd.appendChild(period);

            period.setAttribute("duration", "PT" + mediaElement.getDuration() + "S");
            
            Element baseUrl = playlist.createElement("BaseURL");
            period.appendChild(baseUrl);
            
            baseUrl.appendChild(playlist.createTextNode(clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/"));
            
            // Video Adaptation Set
            if(profile.getVideoTranscodes() != null && profile.getVideoTranscodes().length > 0) {
                Element vAdaptationSet = playlist.createElement("AdaptationSet");
                period.appendChild(vAdaptationSet);
                
                vAdaptationSet.setAttribute("segmentAlignment", "true");
                vAdaptationSet.setAttribute("mimeType", "video/mp4");
                
                for(int v = 0; v < TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).size(); v++) {
                    VideoTranscode transcode = TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).get(v);
                    VideoStream stream = MediaUtils.getVideoStreamById(mediaElement.getVideoStreams(), transcode.getId());

                    // Bitrate
                    int bitrate = -1;

                    if(transcode.getQuality() != null) {
                        bitrate = TranscodeUtils.getMaxBitrateForCodec(transcode.getCodec(), transcode.getQuality());
                    }

                    if(bitrate < 0) {
                        bitrate = MediaUtils.getAverageBitrate(stream, mediaElement.getBitrate());
                    }
                    
                    // Codec
                    int codec = transcode.getCodec();

                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }
                    
                    // Resolution
                    Dimension resolution = transcode.getResolution();

                    if(resolution == null) {
                        resolution = stream.getResolution();
                    }

                    Element representation = playlist.createElement("Representation");
                    vAdaptationSet.appendChild(representation);
                    
                    representation.setAttribute("bandwidth", String.valueOf(bitrate * 1000));
                    representation.setAttribute("codecs", TranscodeUtils.getIsoSpecForCodec(codec));
                    representation.setAttribute("width", String.valueOf(resolution.width));
                    representation.setAttribute("height", String.valueOf(resolution.height));
                    
                    if(stream.getFPS() != null) {
                        representation.setAttribute("frameRate", String.valueOf(Math.round(stream.getFPS())));
                    }
                    
                    representation.setAttribute("id", "video/" + String.valueOf(v));
                    
                    Element vBaseUrl = playlist.createElement("BaseURL");
                    representation.appendChild(vBaseUrl);
                    
                    vBaseUrl.appendChild(playlist.createTextNode("video/" + String.valueOf(v) + "/"));
                    
                    Element segmentTemplate = playlist.createElement("SegmentTemplate");
                    representation.appendChild(segmentTemplate);

                    segmentTemplate.setAttribute("startNumber", "0");
                    segmentTemplate.setAttribute("duration", String.valueOf(Math.round(profile.getSegmentDuration() * 1000)));
                    segmentTemplate.setAttribute("timescale", "1000");
                    segmentTemplate.setAttribute("initialization", "init.mp4");
                    segmentTemplate.setAttribute("media", "$Number$.m4s");
                }
            }
            
            
            // Audio Adaptation Sets
            int audioId = -1;
            
            if(profile.getAudioTranscodes() != null && profile.getAudioTranscodes().length > 0) {
                for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[a];
                    AudioStream stream = MediaUtils.getAudioStreamById(mediaElement.getAudioStreams(), transcode.getId());

                    // Get audio bandwidth
                    int bandwidth = -1;

                    if(clientProfile.getAudioQuality() != null) {
                        bandwidth = (TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[clientProfile.getAudioQuality()] * 1000);
                    }

                    if(bandwidth < 0) {
                        bandwidth = 384000;
                    }
                
                    // Codec
                    int codec = transcode.getCodec();

                    if(codec == SMS.Codec.COPY) {
                        codec = stream.getCodec();
                    }
                    
                    // New adaptation set
                    Element aAdaptationSet = null;
                    
                    if(audioId != transcode.getId()) {
                        audioId = transcode.getId();
                        
                        aAdaptationSet = playlist.createElement("AdaptationSet");
                        period.appendChild(aAdaptationSet);

                        aAdaptationSet.setAttribute("segmentAlignment", "true");
                        aAdaptationSet.setAttribute("mimeType", "audio/mp4");
                        aAdaptationSet.setAttribute("lang", stream.getLanguage());
                    }
                    
                    if(aAdaptationSet == null) {
                        continue;
                    }
                    
                    Element representation = playlist.createElement("Representation");
                    aAdaptationSet.appendChild(representation);
                    
                    representation.setAttribute("bandwidth", String.valueOf(bandwidth));
                    representation.setAttribute("codecs", TranscodeUtils.getIsoSpecForCodec(codec));
                    
                    representation.setAttribute("id", "audio/" + String.valueOf(a));
                    
                    Element aBaseUrl = playlist.createElement("BaseURL");
                    representation.appendChild(aBaseUrl);
                    
                    aBaseUrl.appendChild(playlist.createTextNode("audio/" + String.valueOf(a) + "/"));
                    
                    Element segmentTemplate = playlist.createElement("SegmentTemplate");
                    representation.appendChild(segmentTemplate);

                    segmentTemplate.setAttribute("startNumber", "0");
                    segmentTemplate.setAttribute("duration", String.valueOf(Math.round(profile.getSegmentDuration() * 1000)));
                    segmentTemplate.setAttribute("timescale", "1000");
                    segmentTemplate.setAttribute("initialization", "init.mp4");
                    segmentTemplate.setAttribute("media", "$Number$.m4s");
                }
            }
            
            // Subtitle Adaptation Sets
            if(profile.getSubtitleTranscodes() != null && profile.getSubtitleTranscodes().length > 0) {
                for(int s = 0; s < profile.getSubtitleTranscodes().length; s++) {
                    SubtitleTranscode transcode = profile.getSubtitleTranscodes()[s];
                    SubtitleStream stream = TranscodeUtils.getSubtitleStreamById(mediaElement.getSubtitleStreams(), transcode.getId());
                    
                    // Determine format to use
                    int codec = transcode.getCodec();

                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }

                    int format = MediaUtils.getFormatForCodec(codec);

                    // Determine extension for segment
                    String extension = MediaUtils.getExtensionForFormat(SMS.MediaType.SUBTITLE, format);
                    
                    Element sAdaptationSet = playlist.createElement("AdaptationSet");
                    period.appendChild(sAdaptationSet);

                    sAdaptationSet.setAttribute("contentType", "text");
                    sAdaptationSet.setAttribute("mimeType", MediaUtils.getMimeType(SMS.MediaType.SUBTITLE, format));
                    sAdaptationSet.setAttribute("lang", stream.getLanguage());
                    
                    Element representation = playlist.createElement("Representation");
                    sAdaptationSet.appendChild(representation);
                    
                    representation.setAttribute("bandwidth", "0");                    
                    representation.setAttribute("id", "subtitle/" + String.valueOf(s));
                    
                    Element sBaseUrl = playlist.createElement("BaseURL");
                    representation.appendChild(sBaseUrl);
                    
                    sBaseUrl.appendChild(playlist.createTextNode("subtitle/" + String.valueOf(s) + "/"));
                    
                    Element segmentTemplate = playlist.createElement("SegmentTemplate");
                    representation.appendChild(segmentTemplate);

                    segmentTemplate.setAttribute("startNumber", "0");
                    segmentTemplate.setAttribute("duration", String.valueOf(Math.round(profile.getSegmentDuration() * 1000)));
                    segmentTemplate.setAttribute("timescale", "1000");
                    segmentTemplate.setAttribute("media", "$Number$" + "." + extension);
                }                
            }
            
            DOMSource result = new DOMSource(playlist);
            return result;
        } catch (ParserConfigurationException ex) {
            return null;
        }
    }
    
    public void sendDashPlaylist(Job job, ClientProfile clientProfile, boolean head, HttpServletResponse response) throws IOException {
        try {
            // Get playlist
            DOMSource playlist = generateDashPlaylist(job, clientProfile);
            
            if(playlist == null) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to generate MPEG-Dash playlist", null);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate MPEG-Dash playlist");
                return;
            }

            // Write playlist to buffer
            StringWriter playlistWriter = new StringWriter();
            StreamResult result = new StreamResult(playlistWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(playlist, result);

            // Set Header Parameters
            response.reset();
            response.setContentType("application/dash+xml");
            response.setContentLength(playlistWriter.toString().length());

            // Enable CORS
            response.setHeader(("Access-Control-Allow-Origin"), "*");
            response.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range");
            response.setHeader("Access-Control-Expose-Headers", "Content-Length,Content-Range");
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
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "\n************\nPlaylist\n************\n" + playlistWriter.toString(), null);
        } catch (TransformerException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "An error occured returning MPEG-Dash playlist", null);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occured returning MPEG-Dash playlist");
        }
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
        
        //
        // Audio
        //
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
                
                // Determine format to use
                int format = -1;
                
                if(clientProfile.getFormat() == SMS.Format.HLS_TS) {
                    format = SMS.Format.MPEGTS;
                } else if(clientProfile.getFormat() == SMS.Format.HLS_FMP4) {
                    format = SMS.Format.MP4;
                }
                
                int codec = transcode.getCodec();

                if(codec == SMS.Codec.COPY) {
                    codec = transcode.getOriginalCodec();
                }

                if(!MediaUtils.isCodecSupportedByFormat(format, codec) || profile.getPackedAudio()) {
                    format = MediaUtils.getFormatForCodec(codec);
                }

                // Determine extension for segment
                String extension = MediaUtils.getExtensionForFormat(SMS.MediaType.AUDIO, format);

                playlist.add("#EXT-X-STREAM-INF:PROGRAM-ID=1, BANDWIDTH=" + bandwidth + ", CODECS=\"" + TranscodeUtils.getIsoSpecForCodec(codec) + "\"");
                playlist.add(clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/audio/" + i + "/" + extension);
            }

            return playlist;
        }

        //
        // Video
        //
        if(mediaElement.getType() == MediaElementType.VIDEO && profile.getVideoTranscodes() != null && profile.getAudioTranscodes() != null) {
            // Global flags
            int aCodec = SMS.Codec.UNSUPPORTED;
            
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
                    
                    // Determine format to use
                    int codec = transcode.getCodec();

                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }

                    int format = MediaUtils.getFormatForCodec(codec);

                    // Determine extension for segment
                    String extension = MediaUtils.getExtensionForFormat(SMS.MediaType.SUBTITLE, format);

                    playlist.add("#EXT-X-MEDIA:TYPE=SUBTITLES,GROUP-ID=\"subs\",LANGUAGE=\"" + stream.getLanguage() + "\",NAME=\"" + MediaUtils.getTitleForStream(stream.getTitle(), stream.getLanguage()) + "\",AUTOSELECT=YES,DEFAULT=" + isDefault + ",URI=\"" + clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/subtitle/" + s + "/" + extension + "\"");
                }                
            }
            
            // Process audio streams
            for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                AudioTranscode transcode = profile.getAudioTranscodes()[a];
                AudioStream stream = TranscodeUtils.getAudioStreamById(mediaElement.getAudioStreams(), transcode.getId());
                String isDefault = "NO";

                if(transcode.getId().equals(profile.getAudioStream())) {
                    isDefault = "YES";
                }

                // Determine format to use
                int format = -1;

                if(clientProfile.getFormat() == SMS.Format.HLS_TS) {
                    format = SMS.Format.MPEGTS;
                } else if(clientProfile.getFormat() == SMS.Format.HLS_FMP4) {
                    format = SMS.Format.MP4;
                }

                int codec = transcode.getCodec();

                if(codec == SMS.Codec.COPY) {
                    codec = stream.getCodec();
                }
                
                // Set global flag
                if(aCodec == SMS.Codec.UNSUPPORTED) {
                    aCodec = codec;
                }

                if(!MediaUtils.isCodecSupportedByFormat(format, codec) || profile.getPackedAudio()) {
                    format = MediaUtils.getFormatForCodec(codec);
                }

                // Determine extension for segment
                String extension = MediaUtils.getExtensionForFormat(SMS.MediaType.AUDIO, format);

                playlist.add("#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",LANGUAGE=\"" + stream.getLanguage() + "\",CHANNELS=\"" + transcode.getChannelCount() + "\",NAME=\"" + MediaUtils.getTitleForStream(stream.getTitle(), stream.getLanguage()) + "\",AUTOSELECT=YES,DEFAULT=" + isDefault + ",URI=\"" + clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/audio/" + a + "/" + extension + "\"");
            }
            
            //
            // Process Variants
            //
            for(int i = 0; i < TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).size(); i++) {
                VideoTranscode transcode = TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream()).get(i);
                VideoStream stream = MediaUtils.getVideoStreamById(mediaElement.getVideoStreams(), transcode.getId());

                // Determine bitrate
                int bitrate = -1;

                if(transcode.getQuality() != null) {
                    bitrate = TranscodeUtils.getMaxBitrateForCodec(transcode.getCodec(), transcode.getQuality());
                }

                if(bitrate < 0) {
                    bitrate = MediaUtils.getAverageBitrate(stream, mediaElement.getBitrate());
                }

                // Determine extension for segment
                String extension = "";

                if(clientProfile.getFormat() == SMS.Format.HLS_TS) {
                    extension = MediaUtils.getExtensionForFormat(SMS.MediaType.VIDEO, SMS.Format.MPEGTS);
                } else if(clientProfile.getFormat() == SMS.Format.HLS_FMP4) {
                    extension = MediaUtils.getExtensionForFormat(SMS.MediaType.VIDEO, SMS.Format.MP4);
                }

                // Determine resolution
                Dimension resolution = transcode.getResolution();

                if(resolution == null) {
                    resolution = stream.getResolution();
                }

                StringBuilder builder = new StringBuilder();
                builder.append("#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=");
                builder.append(String.valueOf(bitrate * 1000));
                builder.append(",RESOLUTION=").append(String.format("%dx%d", resolution.width, resolution.height));
                builder.append(",CLOSED-CAPTIONS=NONE");
                builder.append(",CODECS=\"");

                // Video codec
                if(transcode.getCodec() == SMS.Codec.COPY) {
                    builder.append(TranscodeUtils.getIsoSpecForCodec(stream.getCodec()));
                } else {
                    builder.append(TranscodeUtils.getIsoSpecForCodec(transcode.getCodec()));
                }

                // Audio codec
                builder.append(",").append(TranscodeUtils.getIsoSpecForCodec(aCodec));

                // Audio group
                builder.append("\",AUDIO=\"audio\"");

                if(profile.getSubtitleTranscodes() != null) {
                    builder.append(",SUBTITLES=\"subs\"");
                }

                playlist.add(builder.toString());

                // Url
                playlist.add(clientProfile.getUrl() + "/stream/playlist/" + job.getSessionId() + "/" + mediaElement.getID() + "/video/" + i + "/" + extension);
            }
            
            return playlist;
        }
        
        return null;
    }
    
    public List<String> generateHLSPlaylist(Job job, ClientProfile clientProfile, String type, Integer extra, String extension) {
        // Check variables
        if(type == null || extra == null || extension == null) {
            return null;
        }

        // Check job
        if(job == null) {
            return null;
        }
        
        MediaElement mediaElement = job.getMediaElement();
        
        if(mediaElement == null) {
            return null;
        }
        
        List<String> playlist = new ArrayList<>();
        
        playlist.add("#EXTM3U");
        playlist.add("#EXT-X-VERSION:7");
        playlist.add("#EXT-X-TARGETDURATION:" + String.valueOf(job.getTranscodeProfile().getSegmentDuration() + 1));
        playlist.add("#EXT-X-MEDIA-SEQUENCE:0");
        playlist.add("#EXT-X-PLAYLIST-TYPE:VOD");
        
        if(clientProfile.getFormat() == SMS.Format.HLS_FMP4 && extension.equals("mp4")) {
            playlist.add("#EXT-X-MAP:URI=\"" + clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/" + type + "/" + extra + "/init.mp4" + "\"");
            
            // Update extension for segments
            extension = "m4s";
        }
        
        // Get Video Segments
        for (int i = 0; i < Math.floor(mediaElement.getDuration() / job.getTranscodeProfile().getSegmentDuration()); i++) {
            playlist.add("#EXTINF:" + job.getTranscodeProfile().getSegmentDuration().floatValue() + ",");
            playlist.add(clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/" + type + "/" + extra + "/" + i + "." + extension);
        }   

        // Determine the duration of the final segment.
        double remainder = mediaElement.getDuration() % job.getTranscodeProfile().getSegmentDuration();
        if (remainder > 0) {
            long i = Double.valueOf(Math.floor(mediaElement.getDuration() / job.getTranscodeProfile().getSegmentDuration())).longValue();
            
            playlist.add("#EXTINF:" + Precision.round(remainder, 1, BigDecimal.ROUND_HALF_UP) + ",");
            playlist.add(clientProfile.getUrl() + "/stream/segment/" + job.getSessionId() + "/" + mediaElement.getID() + "/" + type + "/" + extra + "/" + i + "." + extension);
        }

        playlist.add("#EXT-X-ENDLIST");
        
        return playlist;
    }
    
    public void sendHLSPlaylist(Job job, ClientProfile clientProfile, String type, Integer extra, String extension, boolean head, HttpServletResponse response) throws IOException {
        List<String> playlist;
                
        if(type == null) {
            playlist = generateHLSVariantPlaylist(job, clientProfile);
        } else {
            playlist = generateHLSPlaylist(job, clientProfile, type, extra, extension);
        }

        if(playlist == null) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Unable to generate HLS playlist.", null);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to generate HLS playlist.");
            return;
        }

        // Write playlist to buffer so we can get the content length
        StringWriter playlistWriter = new StringWriter();
        playlist.forEach((line) -> {
            playlistWriter.write(line + "\n");
        });

        // Set Header Parameters
        response.reset();
        response.setContentType("application/vnd.apple.mpegurl");
        response.setContentLength(playlistWriter.toString().length());
        
        // Enable CORS
        response.setHeader(("Access-Control-Allow-Origin"), "*");
        response.setHeader("Access-Control-Allow-Methods", "GET,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range");
        response.setHeader("Access-Control-Expose-Headers", "Content-Length,Content-Range");
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
        LogService.getInstance().addLogEntry(type == null ? LogService.Level.DEBUG : LogService.Level.INSANE, CLASS_NAME, "\n************\nPlaylist\n************\n" + playlistWriter.toString(), null);
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
        Iterator pItr = processes.iterator(); 
        
        while (pItr.hasNext()) { 
            AdaptiveStreamingProcess process = (AdaptiveStreamingProcess) pItr.next();
            
            if(process.getId().compareTo(id) == 0) {
                pItr.remove();
                break;
            }
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
