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

import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
import com.scooter1556.sms.server.domain.HardwareAccelerator;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeCommand;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.parser.TranscoderParser;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TranscodeService {

    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
 
    private static final String CLASS_NAME = "TranscodeService";
    
    private Transcoder transcoder = null;
    private final List<TranscodeProfile> transcodeProfiles = new ArrayList<>();
    
    // Setup transcoder
    public TranscodeService() {
        // Attempt to find a transcoder
        this.transcoder = getTranscoder();
        
        if(this.transcoder == null) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to find a suitable transcoder!", null);
        } else {
            LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Transcoder " + this.transcoder, null);
        }
        
        // Check all required codecs are supported
        if(!TranscodeUtils.checkTranscoder(transcoder)) {
            LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Transcoder is missing required codec support!", null);
        }
    }
    
    public final Transcoder getTranscoder() {
        // First check we haven't already found the transcoder
        if(this.transcoder != null) {
            return this.transcoder;
        }
        
        // Check user config transcode path
        if(SettingsService.getInstance().getTranscodePath() != null){
            File tFile = new File(SettingsService.getInstance().getTranscodePath());
            
            if(TranscodeUtils.isValidTranscoder(tFile)) {
                return TranscoderParser.parse(new Transcoder(tFile.toPath()));
            }
        }
        
        // Search possible transcoder paths
        for(String path : TranscodeUtils.getTranscoderPaths()) {
            File test = new File(path);
            
            if(TranscodeUtils.isValidTranscoder(test)) {
                SettingsService.getInstance().setTranscodePath(path);
                return TranscoderParser.parse(new Transcoder(test.toPath()));
            }
        }
        
        // Out of ideas
        return null;
    }
    
    public static String[] getSupportedCodecs() {
        List<String> codecs = new ArrayList<>();
        codecs.addAll(Arrays.asList(TranscodeUtils.SUPPORTED_VIDEO_CODECS));
        codecs.addAll(Arrays.asList(TranscodeUtils.SUPPORTED_AUDIO_CODECS));
        codecs.addAll(Arrays.asList(TranscodeUtils.SUPPORTED_SUBTITLE_CODECS));
        return codecs.toArray(new String[codecs.size()]);
    }
    
    public static String[] getTranscodeCodecs() {
        List<String> codecs = new ArrayList<>();
        codecs.addAll(Arrays.asList(TranscodeUtils.TRANSCODE_VIDEO_CODECS));
        codecs.addAll(Arrays.asList(TranscodeUtils.TRANSCODE_AUDIO_CODECS));
        return codecs.toArray(new String[codecs.size()]);
    }
    
    public String[][] getTranscodeCommand(TranscodeProfile profile) {
        ArrayList<TranscodeCommand> commands = new ArrayList<>();
        
        // Determine number of potential transcode commands to generate
        int transcodeCommands = 1; 
        
        if(profile.getVideoTranscodes() != null) {
            transcodeCommands += transcoder.getHardwareAccelerators().length;
        }
        
        for(int i = 0; i < transcodeCommands; i++) {
            commands.add(new TranscodeCommand());
            
            boolean initialised = false;
            
            // Transcoder path
            commands.get(i).getCommands().add(getTranscoder().getPath().toString());
            commands.get(i).getCommands().add("-y");

            // Seek
            commands.get(i).getCommands().add("-ss");
            commands.get(i).getCommands().add(profile.getOffset().toString());

            // Video
            if(profile.getVideoTranscodes() != null) {
                HardwareAccelerator hardwareAccelerator = null;
                boolean hardcodedSubtitles = false;
                boolean hardwareEncoding = !hardcodedSubtitles && profile.getMaxBitRate() == null;
            
                // Software or hardware based transcoding
                if(transcoder.getHardwareAccelerators().length > i) {
                    hardwareAccelerator = transcoder.getHardwareAccelerators()[i];
                }
                
                // Check for hardcoded subtitles
                if(profile.getSubtitleTrack() != null) {
                    hardcodedSubtitles = profile.getSubtitleTranscodes()[profile.getSubtitleTrack()].isHardcoded();
                }
                
                // Populate filters
                for(int v = 0; v < profile.getVideoTranscodes().length; v++) {
                    // If we are copying the stream continue with the next transcode
                    if(profile.getVideoTranscodes()[v].getCodec().equals("copy")) {
                        continue;
                    }
                    
                    // Add a filter list for video transcode
                    commands.get(i).getFilters().add(new ArrayList<String>());
                    
                    // Burn in subtitles if required
                    if(hardcodedSubtitles) {
                        commands.get(i).getFilters().get(v).add("[0:s:" + profile.getSubtitleTrack() + "]overlay");
                    }
                    
                    if(hardwareAccelerator == null) {
                        commands.get(i).getFilters().get(v).addAll(getSoftwareVideoEncodingFilters(profile.getVideoTranscodes()[v].getResolution()));
                    } else {
                        commands.get(i).getFilters().get(v).addAll(getHardwareVideoEncodingFilters(profile.getVideoTranscodes()[v].getResolution(), hardwareAccelerator));
                    }
                }
                
                // Hardware decoding
                if(hardwareAccelerator != null) {
                    commands.get(i).getCommands().addAll(getHardwareAccelerationCommands(hardwareAccelerator, hardwareEncoding));
                }
                
                // Input media file
                initialised = true;
                commands.get(i).getCommands().add("-i");
                commands.get(i).getCommands().add(profile.getMediaElement().getPath());

                // Remove metadata
                commands.get(i).getCommands().add("-map_metadata");
                commands.get(i).getCommands().add("-1");
                
                // Filter commands
                commands.get(i).getCommands().addAll(getFilterCommands(commands.get(i).getFilters()));
                
                for(int v = 0; v < profile.getVideoTranscodes().length; v++) {
                    // Stream copy
                    if(profile.getVideoTranscodes()[v].getCodec().equals("copy")) {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("0:v");
                        
                        // Codec
                        commands.get(i).getCommands().addAll(getSoftwareVideoEncodingCommands(profile.getVideoTranscodes()[v].getCodec(), profile.getMaxBitRate()));
                    } else {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("[v" + v + "]");

                        // Encoding
                        if(hardwareAccelerator == null || !hardwareEncoding) {
                            commands.get(i).getCommands().addAll(getSoftwareVideoEncodingCommands(profile.getVideoTranscodes()[v].getCodec(), profile.getMaxBitRate()));
                        } else {
                            commands.get(i).getCommands().addAll(getHardwareVideoEncodingCommands(hardwareAccelerator));
                        }

                        commands.get(i).getCommands().add("-force_key_frames");
                        commands.get(i).getCommands().add("expr:gte(t,n_forced*" + AdaptiveStreamingService.HLS_SEGMENT_DURATION.toString() + ")");
                    }
                    
                    // Segment
                    commands.get(i).getCommands().addAll(getHlsCommands(profile.getID(), null, "video-" + v, profile.getOffset()));
                }
            }

            // Audio
            if(profile.getAudioTranscodes() != null) {
                if(!initialised) {
                    // Input media file
                    commands.get(i).getCommands().add("-i");
                    commands.get(i).getCommands().add(profile.getMediaElement().getPath());
                }
                
                // Enable experimental codecs
                commands.get(i).getCommands().add("-strict");
                commands.get(i).getCommands().add("experimental");
                
                for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[a];
                    String format = null;
                    
                    if(profile.getMediaElement().getType().equals(MediaElementType.VIDEO)) {
                        switch(profile.getClient()) {
                            case "chromecast":
                                format = TranscodeUtils.getFormatForAudioCodec(transcode.getCodec());
                                break;

                            default:
                                break; 
                        }
                    }
                    
                    // Transcode commands
                    commands.get(i).getCommands().addAll(getAudioCommands(a, transcode));
                    
                    // Segment commands
                    commands.get(i).getCommands().addAll(getHlsCommands(profile.getID(), format, "audio-" + a, profile.getOffset()));
                }
            }
        }
        
        // Prepare result
        String[][] result = new String[commands.size()][];
        
        for(int r = 0; r < commands.size(); r++) {
            result[r] = commands.get(r).getCommands().toArray(new String[0]);
        }
        
        return result;
    }
    
    private Collection<String> getHlsCommands(UUID id, String format, String name, Integer offset) {
        if(id == null || name == null) {
            return null;
        }
        
        Collection<String> commands = new LinkedList<>();
        
        commands.add("-f");
        commands.add("segment");

        commands.add("-segment_time");
        commands.add(AdaptiveStreamingService.HLS_SEGMENT_DURATION.toString());

        commands.add("-segment_format");
        
        if(format == null) {
            commands.add("mpegts");
        } else {
            commands.add(format);
        }

        if(offset != null && offset > 0) {
            commands.add("-segment_start_number");
            commands.add(String.valueOf(offset / AdaptiveStreamingService.HLS_SEGMENT_DURATION));

            commands.add("-initial_offset");
            commands.add(offset.toString());
        }

        commands.add("-segment_list");
        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/" + name + ".txt");

        commands.add("-segment_list_type");
        commands.add("flat");

        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/%d-" + name);
        
        return commands;
    }
    
    private Collection<String> getHardwareAccelerationCommands(HardwareAccelerator hardwareAccelerator, boolean decode) {
        Collection<String> commands = new LinkedList<>();

        switch(hardwareAccelerator.getName()) {
            case "vaapi":
                if(decode) {
                    commands.add("-hwaccel");
                    commands.add(hardwareAccelerator.getName());
                    commands.add("-hwaccel_output_format");
                    commands.add("vaapi");
                }
                
                commands.add("-vaapi_device");
                commands.add(hardwareAccelerator.getDevice().toString());
                
                break;

            case "cuvid":
                if(decode) {
                    commands.add("-hwaccel");
                    commands.add(hardwareAccelerator.getName());
                }
                
                break;
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a given hardware accelerator.
     */
    private Collection<String> getHardwareVideoEncodingCommands(HardwareAccelerator hardwareAccelerator) {
        Collection<String> commands = new LinkedList<>();
        
        if(hardwareAccelerator != null) {
            switch(hardwareAccelerator.getName()) {
                case "vaapi":
                    commands.add("-c:v");
                    commands.add("h264_vaapi");
                    commands.add("-qp");
                    commands.add("23");
                    break;

                case "cuvid":
                    commands.add("-c:v");
                    commands.add("h264_nvenc");
                    commands.add("-bf:v");
                    commands.add("4");
                    break;
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of filters for a given hardware accelerator.
     */
    private List<String> getHardwareVideoEncodingFilters(Dimension resolution, HardwareAccelerator hardwareAccelerator) {
        List<String> filters = new ArrayList<>();
        
        if(hardwareAccelerator != null) {
            switch(hardwareAccelerator.getName()) {
                case "vaapi":
                    filters.add("format=nv12|vaapi");
                    filters.add("hwupload");

                    if(resolution != null) {
                        filters.add("scale_vaapi=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    break;

                case "cuvid":
                    if(resolution != null) {
                        filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    break;
            }
        }
        
        return filters;
    }
    
    /*
     * Returns a list of commands for a given software video codec to optimise transcoding.
     */
    private Collection<String> getSoftwareVideoEncodingCommands(String codec, Integer maxrate) {
        Collection<String> commands = new LinkedList<>();
        
        if(codec != null) {
            // Video Codec
            commands.add("-c:v");

            switch(codec) {       
                case "vp8":
                    commands.add("libvpx");
                    commands.add("-crf");
                    commands.add("25");
                    commands.add("-b:v");
                    
                    if(maxrate != null) {
                        commands.add(maxrate.toString() + "k");
                    } else {
                        commands.add("0");
                    }
                    
                    commands.add("-quality");
                    commands.add("realtime");
                    commands.add("-cpu-used");
                    commands.add("5");
                    break;

                case "h264":
                    commands.add("libx264");
                    commands.add("-crf");
                    commands.add("25");
                    commands.add("-preset");
                    commands.add("superfast");
                    commands.add("-pix_fmt");
                    commands.add("yuv420p");
                    commands.add("-profile:v");
                    commands.add("high");
                    
                    if(maxrate != null) {
                        commands.add("-maxrate");
                        commands.add(maxrate.toString() + "k");
                        commands.add("-bufsize");
                        commands.add("2M");
                    }
                    
                    break;

                default:
                    commands.add(codec);
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of filters for software encoding.
     */
    private List<String> getSoftwareVideoEncodingFilters(Dimension resolution) {
        List<String> filters = new ArrayList<>();
        
        if(resolution != null) {
            filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
        }
        
        return filters;
    }
    
    /*
     * Returns a list of commands for an audio stream.
     */
    private Collection<String> getAudioCommands(int track, AudioTranscode transcode) {
        Collection<String> commands = new LinkedList<>();
        
        if(transcode.getCodec() != null) {
            // Mapping
            commands.add("-map");
            commands.add("0:a:" + track);
        
            // Codec
            commands.add("-c:a");
            commands.add(transcode.getCodec());
            
            // Quality
            if(transcode.getQuality() != null) {
                commands.add("-q:a");
                commands.add(String.valueOf(transcode.getQuality()));
            }
            
            // Downmix
            if(transcode.isDownmixed()) {
                commands.add("-ac");
                commands.add("2");
                commands.add("-clev");
                commands.add("3dB");
                commands.add("-slev");
                commands.add("-3dB");
            }
            
            // Sample rate
            if(transcode.getSampleRate() != null) {
                commands.add("-ar");
                commands.add(String.valueOf(transcode.getSampleRate()));
            }
        }
        
        return commands;
    }
    
    public Collection<String> getFilterCommands(ArrayList<ArrayList<String>> filters) {
        Collection<String> commands = new LinkedList<>();
        
        if(!filters.isEmpty()) {
            commands.add("-filter_complex");
            
            StringBuilder filterBuilder = new StringBuilder();
            
            // Add each filter chain in turn
            for(int i = 0; i < filters.size(); i++) {
                filterBuilder.append("[0:v]");
                
                // If there are no filters to add utilise the 'null' filter
                if(filters.get(i).isEmpty()) {
                    filterBuilder.append("null");
                } else {
                    for(int f = 0; f < filters.get(i).size(); f++) {
                        filterBuilder.append(filters.get(i).get(f));
                        
                        if(f < (filters.get(i).size() - 1)) {
                            filterBuilder.append(",");
                        }
                    }
                }
            
                filterBuilder.append("[v").append(i).append("]");
                
                if(i < (filters.size() - 1)) {
                    filterBuilder.append(";");
                }
            }
            
            commands.add(filterBuilder.toString());
        }
        
        return commands;
    }
    
    public static Boolean isTranscodeRequired(TranscodeProfile profile) {
        // Make sure we have the information we require
        if(profile.getMediaElement() == null || profile.getQuality() == null || profile.getCodecs() == null) {
            return null;
        }
        
        // Check video codec
        if(profile.getMediaElement().getVideoStream() != null) {
            if(isTranscodeRequired(profile, profile.getMediaElement().getVideoStream())) {
                return true;
            }
        }
        
        // Check audio streams
        if(profile.getMediaElement().getAudioStreams() != null) {
            for(AudioStream stream : profile.getMediaElement().getAudioStreams()) {
                if(isTranscodeRequired(profile, stream)) {
                    return true;
                }
            }
        }

        // Check subtitle streams
        if(profile.getMediaElement().getSubtitleStreams() != null) {
            for(SubtitleStream stream : profile.getMediaElement().getSubtitleStreams()) {
                if(!TranscodeUtils.isSupported(profile.getCodecs(), stream.getFormat())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static boolean isTranscodeRequired(TranscodeProfile profile, VideoStream stream) {
        if(!TranscodeUtils.isSupported(profile.getCodecs(), stream.getCodec())) {
            return true;
        }
        
        // Check maximum bitrate
        // We are comparing against the original streams average bitrate 
        // so to allow for this we unscientifically half the max bitrate for comparison
        if(profile.getMaxBitRate() != null) {
            if(profile.getMediaElement().getBitrate() > (profile.getMaxBitRate() * 0.5)) {
                return true;
            }
        }

        // If direct play is not enabled check stream parameters
        if(!profile.isDirectPlayEnabled()) {
            // Check bitrate
            if(profile.getMediaElement().getBitrate() > TranscodeUtils.VIDEO_QUALITY_MAX_BITRATE[profile.getQuality()]) {
                return true;
            }

            // Check resolution
            if(TranscodeUtils.compareDimensions(new Dimension(stream.getWidth(), stream.getHeight()), TranscodeUtils.VIDEO_QUALITY_RESOLUTION[profile.getQuality()]) == 1) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isTranscodeRequired(TranscodeProfile profile, AudioStream stream) {
        // Check audio codec
        if(TranscodeUtils.getAudioChannelCount(stream.getConfiguration()) > 2) {
            if(profile.getMchCodecs() == null || !TranscodeUtils.isSupported(profile.getMchCodecs(), stream.getCodec())) {
                return true;
            }
        } else {
            if(!TranscodeUtils.isSupported(profile.getCodecs(), stream.getCodec())) {
                return true;
            }
        }

        // Check audio sample rate
        if(stream.getSampleRate() > profile.getMaxSampleRate() && !stream.getCodec().contains("dsd")) {
            return true;
        }

        // If direct play is not enabled check stream parameters
        if(!profile.isDirectPlayEnabled()) {
            // Check bitrate for audio elements
            if(profile.getMediaElement().getType() == MediaElementType.AUDIO) {
                int bitrate = (TranscodeUtils.getAudioChannelCount(stream.getConfiguration()) * TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[profile.getQuality()]);

                if(bitrate > 0 && profile.getMediaElement().getBitrate() > bitrate) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static boolean processSubtitles(TranscodeProfile profile) {
        // Check variables
        if(profile.getMediaElement() == null || profile.getCodecs() == null || profile.getFormat() == null) {
            return false;
        }
        
        // Check this is a video element
        if(profile.getMediaElement().getType() != MediaElementType.VIDEO) {
            return false;
        }
        
        // If there are no streams to process we are done
        if(profile.getMediaElement().getSubtitleStreams() == null) {
            return true;
        }
        
        // Process each subtitle stream
        List<SubtitleTranscode> transcodes = new ArrayList<>();
        
        for(SubtitleStream stream : profile.getMediaElement().getSubtitleStreams()) {
            String codec = null;
            boolean hardcode = false;
            
            if(TranscodeUtils.isSupported(profile.getCodecs(), stream.getFormat()) && TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), stream.getFormat())) {
                codec = "copy";
            } else if(TranscodeUtils.isSupported(TranscodeUtils.SUPPORTED_SUBTITLE_CODECS, stream.getFormat())){
                switch(stream.getFormat()) {
                    // Text Based
                    case "subrip": case "srt": case "webvtt":
                        codec = "webvtt";
                        break;

                    // Picture Based
                    case "dvd_subtitle": case "dvb_subtitle": case "hdmv_pgs_subtitle":
                        codec = stream.getFormat();
                        hardcode = true;
                        break;

                    default:
                        codec = "copy";
                        break;
                }
            }
            
            // Enable forced subtitles by default
            if(stream.isForced() && profile.getSubtitleTrack() == null) {
                profile.setSubtitleTrack(stream.getStream());
            }
            
            transcodes.add(new SubtitleTranscode(codec, hardcode));
        }
        
        // Update profile
        profile.setSubtitleTranscodes(transcodes.toArray(new SubtitleTranscode[transcodes.size()]));
        
        return true;
    }
    
    public static boolean processVideo(TranscodeProfile profile) {
        // Check variables
        if(profile.getMediaElement() == null || profile.getCodecs() == null || profile.getFormat() == null || profile.getQuality() == null) {
            return false;
        }
        
        // Check this is a video element
        if(profile.getMediaElement().getType() != MediaElementType.VIDEO) {
            return false;
        }
        
        // Variables
        int maxQuality = TranscodeUtils.getHighestVideoQuality(profile.getMediaElement());
        int streamCount = AdaptiveStreamingService.DEFAULT_STREAM_COUNT;
        VideoStream stream = profile.getMediaElement().getVideoStream();
        
        if(stream == null) {
            return false;
        }
        
        // Process quality
        if(maxQuality < 0 || maxQuality < profile.getQuality()) {
            profile.setQuality(maxQuality);
        }
        
        // Determine number of streams to transcode
        if(profile.isDirectPlayEnabled() || profile.getQuality() == 0) {
            streamCount = 1;
        } else if(streamCount > profile.getQuality()) {
            streamCount = profile.getQuality();
        }
        
        // Test if transcoding is necessary
        boolean transcodeRequired = streamCount > 1;
        
        if(!transcodeRequired) {
            transcodeRequired = isTranscodeRequired(profile, stream);
        }
        
        if(!transcodeRequired) {
            transcodeRequired = !TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), stream.getCodec());
        }
        
        // Check for hardcoded subtitles
        if(profile.getSubtitleTrack() != null) {
            transcodeRequired = profile.getSubtitleTranscodes()[profile.getSubtitleTrack()].isHardcoded();
        }
        
        // Process required number of video streams
        List<VideoTranscode> transcodes = new ArrayList<>();
        
        for(int i = 0; i < streamCount; i++) {
            String codec = null;
            Dimension resolution = null;
            Integer quality = profile.getQuality();
            
            if(transcodeRequired) {
                // Determine quality for transcode
                if(i > 0) {
                    quality = i - 1;
                }
                
                // Get suitable codec
                for(String test : profile.getCodecs()) {
                    if(TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), test) && TranscodeUtils.isSupported(TranscodeUtils.TRANSCODE_VIDEO_CODECS, test)) {
                        codec = test;
                        break;
                    }
                }

                // Check we got a suitable codec
                if(codec == null) {
                    return false;
                }

                // Get suitable resolution (use native resolution if direct play is enabled)
                if(!profile.isDirectPlayEnabled()) {
                    resolution = TranscodeUtils.getVideoResolution(profile.getMediaElement(), quality);      
                }
            } else {
                codec = "copy";
                quality = null;
            }
            
            // Add video transcode to array
            transcodes.add(new VideoTranscode(codec, resolution, quality));
        }
        
        // Update profile with video transcode properties
        profile.setVideoTranscodes(transcodes.toArray(new VideoTranscode[transcodes.size()]));
        
        return true;
    }
    
    public static boolean processAudio(TranscodeProfile profile) {
        // Check variables
        if(profile.getMediaElement() == null || profile.getCodecs() == null || profile.getQuality() == null) {
            return false;
        }
        
        // If there are no audio streams to process we are done
        if(profile.getMediaElement().getAudioStreams() == null) {
            return true;
        }
        
        // Set audio track if necessary
        if(profile.getAudioTrack() == null) {
            if(profile.getMediaElement().getAudioStreams().size() > 0) {
                profile.setAudioTrack(0);
            } else {
                return true;
            }
        } 
        
        // Check a format is specified for video transcode
        if(profile.getMediaElement().getType() == MediaElementType.VIDEO && profile.getFormat() == null) {
            return false;
        }
            
        // Process each audio stream
        List<AudioTranscode> transcodes = new ArrayList<>();

        for(AudioStream stream : profile.getMediaElement().getAudioStreams()) {
            String codec = null;
            Integer quality = null;
            Integer sampleRate = null;
            boolean downmix = false;
            
            // Check if transcoding is required
            boolean transcodeRequired = isTranscodeRequired(profile, stream);
            
            // Check the format supports this codec for video or that we can stream this codec for audio
            if(!transcodeRequired) {
                if(profile.getFormat() != null) {
                    transcodeRequired = !TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), stream.getCodec());
                } else {
                    transcodeRequired = !TranscodeUtils.isSupported(TranscodeUtils.TRANSCODE_AUDIO_CODECS, stream.getCodec());
                }
            }
            
            if(!transcodeRequired) {
                // Work around transcoder bug where flac files have the wrong duration if the stream is copied
                codec = "copy";
                
                // Get format if required
                if(profile.getFormat() == null) {
                    profile.setFormat(TranscodeUtils.getFormatForAudioCodec(stream.getCodec()));
                }
            } else {
                // Test if lossless codecs should be prioritised
                if(profile.getMediaElement().getType() == MediaElementType.AUDIO && (profile.getQuality() == AudioQuality.LOSSLESS || profile.isDirectPlayEnabled()) && TranscodeUtils.isSupported(TranscodeUtils.LOSSLESS_CODECS, stream.getCodec())) {
                    profile.setCodecs(TranscodeUtils.sortStringList(profile.getCodecs(), TranscodeUtils.LOSSLESS_CODECS));
                    
                    if(profile.getMchCodecs() != null) {
                        profile.setMchCodecs(TranscodeUtils.sortStringList(profile.getMchCodecs(), TranscodeUtils.LOSSLESS_CODECS));
                    }
                }
                
                // Check for multichannel codecs if this is a multichannel stream
                if(TranscodeUtils.getAudioChannelCount(stream.getConfiguration()) > 2) {
                    // Try to get a suitable multichannel codec
                    if(profile.getMchCodecs() != null) {
                        for(String test : profile.getMchCodecs()) {
                            if(TranscodeUtils.isSupported(TranscodeUtils.TRANSCODE_AUDIO_CODECS, test)) {
                                if(profile.getFormat() != null) {
                                    if(TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), test)) {
                                        codec = test;
                                        break;
                                    }
                                } else {
                                    codec = test;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // If a codec couldn't be found we need to downmix to stereo
                    if(codec == null) {
                        downmix = true;
                    }
                }
                
                // If we still don't have a codec just try anything...
                if(codec == null) {
                    for(String test : profile.getCodecs()) {
                        if(TranscodeUtils.isSupported(TranscodeUtils.TRANSCODE_AUDIO_CODECS, test)) {
                            if(profile.getFormat() != null) {
                                if(TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), test)) {
                                    codec = test;
                                    break;
                                }
                            } else {
                                codec = test;
                                break;
                            }
                        }
                    }
                }
                
                // Check audio parameters for codec
                if(codec != null) {
                    
                    // Sample rate
                    if((stream.getSampleRate() > profile.getMaxSampleRate()) || (stream.getSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec))) {
                        sampleRate = (profile.getMaxSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec)) ? TranscodeUtils.getMaxSampleRateForCodec(codec) : profile.getMaxSampleRate();
                    }
                    
                    // Quality
                    if(profile.getMediaElement().getType() == MediaElementType.AUDIO) {
                        quality = TranscodeUtils.getAudioQualityForCodec(codec, profile.getQuality());
                    } else if (profile.getMediaElement().getType() == MediaElementType.VIDEO) {
                        quality = TranscodeUtils.getAudioQualityForCodec(codec, TranscodeUtils.VIDEO_QUALITY_AUDIO_QUALITY[profile.getQuality()]);
                    }
                    
                    // Get format if required
                    if(profile.getFormat() == null) {
                        profile.setFormat(TranscodeUtils.getFormatForAudioCodec(codec));
                    }
                    
                    // Update codec
                    codec = TranscodeUtils.getEncoderForAudioCodec(codec);
                }
            }
            
            // Add transcode properties to array
            transcodes.add(new AudioTranscode(codec, quality, sampleRate, downmix));
        }
        
        // Update profile with audio transcode properties
        profile.setAudioTranscodes(transcodes.toArray(new AudioTranscode[transcodes.size()]));
        
        return true;
    }
    
    //
    // Transcode Profile
    //
    
    public void addTranscodeProfile(TranscodeProfile profile) {
        transcodeProfiles.add(profile);
    }
    
    public TranscodeProfile getTranscodeProfile(UUID id) {
        for(TranscodeProfile profile : transcodeProfiles) {
            if(profile.getID() != null) {
                if(profile.getID().compareTo(id) == 0) {
                    return profile;
                }
            }
        }
        
        return null;
    }
    
    public void removeTranscodeProfile(UUID id) {
        int index = 0;
        
        for(TranscodeProfile profile : transcodeProfiles) {
            if(profile.getID() != null) {
                if(profile.getID().compareTo(id) == 0) {
                    // Stop transcode process
                    if(profile.getType() > StreamType.DIRECT) {
                        adaptiveStreamingService.endProcess(id);
                    }
                    
                    transcodeProfiles.remove(index);
                    break;
                }
            }
            
            index ++;
        }
    }
}