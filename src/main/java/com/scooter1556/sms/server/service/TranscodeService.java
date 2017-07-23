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
        
        if(profile.getVideoTranscode() != null && !profile.getVideoTranscode().getCodec().equals("copy")) {
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
            if(profile.getVideoTranscode() != null) {                
                // If there is the potential for hardcoded subtitles we need to maintain original resolution
                if(profile.getSubtitleTranscodes() != null) {
                    for(SubtitleTranscode transcode : profile.getSubtitleTranscodes()) {
                        if(transcode.isHardcoded()) {
                            profile.getVideoTranscode().setResolution(null);
                            break;
                        }
                    }
                }
                
                HardwareAccelerator hardwareAccelerator = null;
            
                // Check we are not simply copying the video stream
                if(!profile.getVideoTranscode().getCodec().equals("copy")) {
                    // Software or hardware based transcoding
                    if(transcoder.getHardwareAccelerators().length > i) {
                        hardwareAccelerator = transcoder.getHardwareAccelerators()[i];
                    }
                }
                
                // Hardware decoding
                if(hardwareAccelerator != null) {
                    commands.get(i).getCommands().addAll(getHardwareVideoDecodingCommands(hardwareAccelerator));
                }
                
                // Input media file
                initialised = true;
                commands.get(i).getCommands().add("-i");
                commands.get(i).getCommands().add(profile.getMediaElement().getPath());
                
                // Subtitle commands
                if(profile.getSubtitleTranscodes() != null) {
                    for(int s = 0; s < profile.getSubtitleTranscodes().length; s++) {
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("0:s:" + s);
                        commands.get(i).getCommands().add("-c:s");
                        commands.get(i).getCommands().add("copy");
                    }
                }

                // Hardware encoding
                if(hardwareAccelerator == null) {
                    commands.get(i).getCommands().addAll(getSoftwareVideoEncodingCommands(commands.get(i), profile.getVideoTranscode().getCodec(), profile.getVideoTranscode().getResolution()));
                } else {
                    commands.get(i).getCommands().addAll(getHardwareVideoEncodingCommands(commands.get(i), profile.getVideoTranscode().getResolution(), hardwareAccelerator));
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
                    commands.get(i).getCommands().addAll(getAudioCommands(a, profile.getAudioTranscodes()[a]));
                }
            }
            
            // Remove metadata
            commands.get(i).getCommands().add("-map_metadata");
            commands.get(i).getCommands().add("-1");

            // Format
            commands.get(i).getCommands().addAll(getFormatCommands(profile));
        }
        
        // Prepare result
        String[][] result = new String[commands.size()][];
        
        for(int r = 0; r < commands.size(); r++) {
            result[r] = commands.get(r).getCommands().toArray(new String[0]);
        }
        
        return result;
    }
    
    public String[][] getSegmentTranscodeCommand(String segment, TranscodeProfile profile, String type, Integer extra) {
        // Check variables
        if(segment == null || profile == null || type == null || extra == null) {
            return null;
        }
        
        TranscodeCommand command = new TranscodeCommand();
            
        // Transcoder path
        command.getCommands().add(getTranscoder().getPath().toString());
        command.getCommands().add("-y");

        switch(type) {

            case "video":
                // Flags
                boolean transcodeRequired = false;
                Dimension resolution = TranscodeUtils.getVideoResolution(profile.getMediaElement(), extra);


                // If the resolution isn't the max the media supports determine if we need to transcode
                if(resolution != null) {
                    Dimension origRes;

                    // Determine resolution to compare
                    if(profile.getVideoTranscode().getResolution() == null) {
                        origRes = profile.getMediaElement().getVideoStream().getResolution();
                    } else {
                        origRes = profile.getVideoTranscode().getResolution();
                    }

                    //  Check if transcode is required for the segment
                    if(origRes.width > resolution.width) {
                        transcodeRequired = true;
                    } else {
                        resolution = null;
                    }
                }

                // Check for hardcoded subtitles
                if(profile.getSubtitleTranscodes() != null && profile.getSubtitleTrack() != null) {
                    if(profile.getSubtitleTranscodes()[profile.getSubtitleTrack()].isHardcoded()) {
                        transcodeRequired = true;
                    }
                }

                // Input media file
                command.getCommands().add("-i");
                command.getCommands().add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/" + segment);

                if(profile.getVideoTranscode() != null) {                        
                    command.getCommands().add("-an");
                    command.getCommands().add("-sn");

                    // Get subtitle commands
                    command.getCommands().addAll(getSubtitleCommands(command, profile));

                    // If highest possible quality then copy stream
                    if(!transcodeRequired) {
                        command.getCommands().add("-c:v");
                        command.getCommands().add("copy");
                    } else {
                        command.getCommands().addAll(getSoftwareVideoEncodingCommands(command, "h264", resolution));
                    }

                    // Format
                    command.getCommands().add("-f");
                    command.getCommands().add("mpegts");                          
                }

                break;

            case "audio":
                // Input media file
                command.getCommands().add("-i");
                command.getCommands().add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/" + segment);

                // Audio
                if(profile.getAudioTranscodes() != null) {
                    if(profile.getAudioTranscodes().length > extra) {
                        // Mapping
                        command.getCommands().add("-map");
                        command.getCommands().add("0:a:" + extra);

                        // Codec
                        command.getCommands().add("-c:a");
                        command.getCommands().add("copy");
                    }
                }   

                // Format
                command.getCommands().add("-f");

                switch(profile.getClient()) {
                    case "chromecast":
                        command.getCommands().add(TranscodeUtils.getFormatForAudioCodec(profile.getAudioTranscodes()[extra].getCodec()));
                        break;

                    default:
                        command.getCommands().add("mpegts");
                }   

                break;

            case "subtitle":
                // Input media file
                command.getCommands().add("-i");
                command.getCommands().add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/" + segment);

                // Subtitle
                if(profile.getSubtitleTranscodes() != null) {
                    if(profile.getSubtitleTranscodes().length > extra) {
                        // Mapping
                        command.getCommands().add("-map");
                        command.getCommands().add("0:s:" + extra);

                        // Codec
                        command.getCommands().add("-c:s");
                        command.getCommands().add("webvtt");
                    }
                }   

                // Format
                command.getCommands().add("-f");
                command.getCommands().add("webvtt");

                break;

            default:
                break;
        }

        // Maintain timestamps
        command.getCommands().add("-copyts");

        command.getCommands().add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/" + segment + "-" + type + "-" + extra);           
        
        //Prepare result
        ArrayList<TranscodeCommand> commands = new ArrayList<>();
        commands.add(command);
        
        String[][] result = new String[commands.size()][];

        for(int i = 0; i < commands.size(); i++) {
            result[i] = commands.get(i).getCommands().toArray(new String[0]);
        }
            
        return result;
    }
    
    private Collection<String> getFormatCommands(TranscodeProfile profile) {
        Collection<String> commands = new LinkedList<>();
        
        if(profile.getFormat() != null) {
            switch(profile.getFormat()) {
                case "hls":                 
                    // Segments
                    commands.add("-f");
                    commands.add("segment");
                    
                    commands.add("-segment_time");
                    commands.add(AdaptiveStreamingService.HLS_SEGMENT_DURATION.toString());
                    
                    commands.add("-segment_format");
                    
                    if(profile.getVideoTranscode() == null) {
                        commands.add("mpegts");
                    } else {
                        commands.add("matroska");
                    }
                    
                    if(profile.getOffset() > 0) {
                        commands.add("-segment_start_number");
                        commands.add(String.valueOf(profile.getOffset() / AdaptiveStreamingService.HLS_SEGMENT_DURATION));
                        
                        commands.add("-initial_offset");
                        commands.add(profile.getOffset().toString());
                    }
                    
                    commands.add("-segment_list");
                    commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/segments.txt");
                    
                    commands.add("-segment_list_type");
                    commands.add("flat");
                    
                    commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/%d");
                    
                    break;
                    
                case "dash":
                    if(profile.getVideoTranscode() != null) {
                        // Reduce overhead with global header
                        commands.add("-flags");
                        commands.add("-global_header");                        
                    }
                    
                    // Segments
                    commands.add("-f");
                    commands.add("dash");
                                        
                    commands.add(SettingsService.getInstance().getDataDirectory().getPath() + "/streams/" + profile.getID() + "/playlist.mpd");
                    break;
                    
                default:
                    commands.add("-f");
                    commands.add(profile.getFormat());
                    commands.add("-");
                    break;
            }
        }
        
        return commands;
    }
    
    private Collection<String> getHardwareVideoDecodingCommands(HardwareAccelerator hardwareAccelerator) {
        Collection<String> commands = new LinkedList<>();

        switch(hardwareAccelerator.getName()) {
            case "vaapi":
                commands.add("-hwaccel");
                commands.add(hardwareAccelerator.getName());
                commands.add("-vaapi_device");
                commands.add(hardwareAccelerator.getDevice().toString());
                commands.add("-hwaccel_output_format");
                commands.add("vaapi");
                break;

            case "cuvid":
                commands.add("-hwaccel");
                commands.add(hardwareAccelerator.getName());
                break;
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a given hardware accelerator.
     */
    private Collection<String> getHardwareVideoEncodingCommands(TranscodeCommand command, Dimension resolution, HardwareAccelerator hardwareAccelerator) {
        Collection<String> commands = new LinkedList<>();
        
        if(hardwareAccelerator != null) {
            switch(hardwareAccelerator.getName()) {
                case "vaapi":
                    command.getFilters().add("format=nv12|vaapi");
                    command.getFilters().add("hwupload");

                    if(resolution != null) {
                        command.getFilters().add("scale_vaapi=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    // Get filter command
                    commands.addAll(getFilterCommands(command.getFilters()));

                    commands.add("-c:v");
                    commands.add("h264_vaapi");
                    commands.add("-qp");
                    commands.add("25");
                    break;

                case "cuvid":
                    if(resolution != null) {
                        command.getFilters().add("scale=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    // Get filter command
                    commands.addAll(getFilterCommands(command.getFilters()));

                    commands.add("-c:v");
                    commands.add("h264_nvenc");
                    break;
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a given software video codec to optimise transcoding.
     */
    private Collection<String> getSoftwareVideoEncodingCommands(TranscodeCommand command, String codec, Dimension resolution) {
        Collection<String> commands = new LinkedList<>();
                
        if(resolution != null) {
            command.getFilters().add("scale=w=" + resolution.width + ":h=" + resolution.height);
        }
                    
        // Get filter command
        commands.addAll(getFilterCommands(command.getFilters()));
        
        if(codec != null) {
            // Video Codec
            commands.add("-c:v");

            switch(codec) {       
                case "vp8":
                    commands.add("libvpx");
                    commands.add("-crf");
                    commands.add("25");
                    commands.add("-b:v");
                    commands.add("0");
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
                    
                    break;

                default:
                    commands.add(codec);
            }
        }
        
        return commands;
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
    
    /*
     * Returns a list of commands for burning picture based subtitles into the output video.
     */
    private Collection<String> getSubtitleCommands(TranscodeCommand command, TranscodeProfile profile) {  
        Collection<String> commands = new LinkedList<>();
        
        // Get subtitle transcode properties
        SubtitleTranscode transcode = null;
        
        if(profile.getSubtitleTranscodes() != null && profile.getSubtitleTrack() != null) {
            if(profile.getSubtitleTrack() < profile.getSubtitleTranscodes().length) {
                transcode = profile.getSubtitleTranscodes()[profile.getSubtitleTrack()];
            }
        }
        
        if(transcode != null && transcode.isHardcoded()) {
            command.getFilters().add("[0:s:" + profile.getSubtitleTrack() + "]overlay");
        }
        
        return commands;
    }
    
    public Collection<String> getFilterCommands(ArrayList<String> filters) {
        Collection<String> commands = new LinkedList<>();
        int i;
        
        if(filters.isEmpty()) {
            commands.add("-map");
            commands.add("0:v");
        } else {
            commands.add("-filter_complex");
            
            StringBuilder filterBuilder = new StringBuilder();
            filterBuilder.append("[0:v]");
            
            for(i = 0; i < filters.size(); i++) {
                if(i > 0) {
                    filterBuilder.append("[v").append(i - 1).append("]");
                }
                filterBuilder.append(filters.get(i));
                filterBuilder.append("[v").append(i).append("]");
                
                if(i < (filters.size() - 1)) {
                    filterBuilder.append(";");
                }
            }
            
            commands.add(filterBuilder.toString());
            commands.add("-map");
            commands.add("[v" + (i - 1) + "]");
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
        
        // Check bitrate
        if(profile.getMaxBitRate() != null) {
            if(profile.getMediaElement().getBitrate() > profile.getMaxBitRate()) {
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
        String codec = null;
        Dimension resolution = null;
        int quality = TranscodeUtils.getHighestVideoQuality(profile.getMediaElement());
        VideoStream stream = profile.getMediaElement().getVideoStream();
        
        if(stream == null) {
            return false;
        }
        
        // Process quality
        if(quality < 0 || quality < profile.getQuality()) {
            profile.setQuality(quality);
        }
        
        // Test if transcoding is necessary
        boolean transcodeRequired = isTranscodeRequired(profile, stream);
        
        // Test that the codec is supported by the given format
        if(!transcodeRequired) {
            transcodeRequired = !TranscodeUtils.isSupported(TranscodeUtils.getCodecsForFormat(profile.getFormat()), stream.getCodec());
        }
        
        if(transcodeRequired) {
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
                resolution = TranscodeUtils.getVideoResolution(profile.getMediaElement(), profile.getQuality());      
            }
        } else {
            codec = "copy";
        }
        
        // Update profile with video transcode properties
        profile.setVideoTranscode(new VideoTranscode(codec, resolution));
        
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
                    if(profile.getType() == StreamType.TRANSCODE) {
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