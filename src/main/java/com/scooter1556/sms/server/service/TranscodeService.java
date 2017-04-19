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
import org.apache.commons.lang3.SystemUtils;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.TranscodeProfile.StreamType;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.domain.VideoTranscode.VideoQuality;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.utilities.FileUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
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
    
    private static final String TRANSCODER_URL = "https://github.com/scoot-software/sms-resources/raw/master/transcode/";
    private static final String LINUX_32_MD5SUM = "5ef7615e8c647f7fe6ea8596a43d3bdf";
    private static final String LINUX_64_MD5SUM = "d247ba4d31c12eddd5adc77c6fc3ed8f";
    private static final String WINDOWS_32_MD5SUM = "a97326fc4348e24ed80ca41ba44c39c4";
    private static final String WINDOWS_64_MD5SUM = "100d2dd55a682d16f845d36f215fc717";
    
    private final String TRANSCODER_FILE = SettingsService.getDataDirectory() + "/transcoder";
            
    private final List<TranscodeProfile> transcodeProfiles = new ArrayList<>();
    
    // Setup transcoder
    public TranscodeService() {
        File transcodeFile = new File(TRANSCODER_FILE);
        boolean is64bit = (SystemUtils.OS_ARCH.contains("64"));
        boolean download;
        String md5sum;
        String os;
        
        // Get OS information
        if(SystemUtils.IS_OS_WINDOWS) {
            os = "windows";
            md5sum = is64bit ? WINDOWS_64_MD5SUM : WINDOWS_32_MD5SUM;
        } else if(SystemUtils.IS_OS_LINUX) {
            os = "linux";
            md5sum = is64bit ? LINUX_64_MD5SUM : LINUX_32_MD5SUM;
        } else {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "OS (" + SystemUtils.OS_NAME + ") is not supported", null);
            return;
        }
        
        if(!transcodeFile.exists()) {
            download = true;
        } else {
            download = !FileUtils.checkIntegrity(transcodeFile, md5sum);
        }
        
        if(download) {
            LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Downloading transcoder...", null);
            
            // Remove current transcoder
            transcodeFile.delete();
            
            // Download new transcoder
            try {
                URL url = new URL(TRANSCODER_URL + os + "-" + (is64bit ? "64" : "32") + "/transcoder");
                org.apache.commons.io.FileUtils.copyURLToFile(url, transcodeFile);
                
                // Make sure file is executable
                transcodeFile.setExecutable(true);
                
                LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Transcoder initialised successfully.", null);
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to download transcoder!", ex);
            }
        }
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
    
    /*
     * Returns a file reference to the transcoder.
     */
    public File getTranscoder() 
    {
        File transcoder = new File(TRANSCODER_FILE);
        
        // Check if the transcoder binary exists
        if(!transcoder.exists()) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Transcoder not found!", null);
            return null;
        }
        
        return transcoder;
    }
    
    public List<String> getTranscodeCommand(TranscodeProfile profile) {
        List<String> command = new ArrayList<>();
        
        // Transcoder path
        command.add(getTranscoder().getPath());
        
        // Seek
        command.add("-ss");
        command.add(profile.getOffset().toString());
        
        // Ensure PTS is set
        if(profile.getFormat().equals("hls")) {
            command.add("-fflags");
            command.add("+genpts");
            command.add("-y");
        }
       
        // Input media file
        command.add("-i");
        command.add(profile.getMediaElement().getPath());
        
        // Enable experimental codecs
        command.add("-strict");
        command.add("experimental");
        
        // Video
        if(profile.getVideoTranscode() != null) {
            // Subtitle commands
            if(profile.getSubtitleTranscodes() != null) {
                if(profile.getType() == StreamType.ADAPTIVE) {
                    for(int i = 0; i < profile.getSubtitleTranscodes().length; i++) {
                        command.add("-map");
                        command.add("0:s:" + i);
                        command.add("-c:s");
                        command.add("copy");
                    }
                    
                    command.add("-map");
                    command.add("0:v");
                    
                    
                } else if(profile.getSubtitleTrack() != null) {
                    command.addAll(getSubtitleCommands(profile));
                }
            } else {
                    command.add("-map");
                    command.add("0:v");
            }
            
            if(profile.getType() == StreamType.ADAPTIVE) {
                command.addAll(getVideoCommands(profile));
                
                if(profile.getMediaElement().getVideoCodec().equals("h264")) {
                    command.add("-bsf:v");
                    command.add("h264_mp4toannexb");
                }
            } else {
                command.addAll(getVideoCommands(profile));
            }
        }
        
        // Audio
        if(profile.getAudioTranscodes() != null) {
            if(profile.getType() == StreamType.ADAPTIVE) {
                for(int i = 0; i < profile.getAudioTranscodes().length; i++) {
                    command.addAll(getAudioCommands(i, profile.getAudioTranscodes()[i]));
                }
            } else if(profile.getAudioTrack() != null) {
                command.addAll(getAudioCommands(profile.getAudioTrack(), profile.getAudioTranscodes()[profile.getAudioTrack()]));
            }
        }
                
        // Use all CPU cores
        command.add("-threads");
        command.add("0");
        
        // Format
        command.addAll(getFormatCommands(profile));
        
        return command;
    }
    
    public List<String> getAdaptiveSegmentTranscodeCommand(Path segment, TranscodeProfile profile, String type, Integer extra) {
        // Check variables
        if(segment == null || profile == null || type == null || extra == null) {
            return null;
        }
        
        List<String> command = new ArrayList<>();
        
        // Transcoder path
        command.add(getTranscoder().getPath());
       
        // Input media file
        command.add("-i");
        command.add(segment.toString());
        
        if(type.equals("video")) {
            // Check profile
            if(!extra.equals(profile.getQuality())) {
                Dimension resolution = TranscodeUtils.getVideoResolution(profile.getMediaElement(), extra);
                
                if(resolution != null) {
                    profile.getVideoTranscode().setResolution(resolution);
                }
            }
            
            if(profile.getVideoTranscode() != null) {
                command.add("-map");
                command.add("0:v");

                // If highest possible quality then copy stream
                if(extra.equals(profile.getQuality())) {
                    command.add("-c:v");
                    command.add("copy");
                } else {
                    command.addAll(getVideoCommands(profile));
                }
                
                // Format
                command.add("-f");
                command.add("mpegts");
                
                command.add("-mpegts_copyts");
                command.add("1");
            }
        } else if(type.equals("audio")) {
            // Audio
            if(profile.getAudioTranscodes() != null) {
                if(profile.getAudioTranscodes().length > extra) {
                    // Mapping
                    command.add("-map");
                    command.add("0:a:" + extra);

                    // Codec
                    command.add("-c:a");
                    command.add("copy");
                }
            }
            
            // Format
            command.add("-f");
            
            switch(profile.getClient()) {
                case "chromecast":
                    command.add(TranscodeUtils.getFormatForAudioCodec(profile.getAudioTranscodes()[extra].getCodec()));
                    break;
                    
                default:
                    command.add("mpegts");
                    command.add("-mpegts_copyts");
                    command.add("1");  
            }
        }
                
        // Use all CPU cores
        command.add("-threads");
        command.add("0");
        
        // Maintain timestamps
        command.add("-copyts");
        
        command.add("-");
        
        return command;
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
                    
                    if(profile.getOffset() > 0) {
                        commands.add("-segment_start_number");
                        commands.add(String.valueOf(profile.getOffset() / AdaptiveStreamingService.HLS_SEGMENT_DURATION));
                        
                        commands.add("-initial_offset");
                        commands.add(profile.getOffset().toString());
                    }
                    
                    commands.add(SettingsService.getCacheDirectory().getPath() + "/streams/" + profile.getID() + "/%d.ts");
                    
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
                                        
                    commands.add(SettingsService.getDataDirectory().getPath() + "/streams/" + profile.getID() + "/playlist.mpd");
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
    
    /*
     * Returns a list of commands for a given video codec to optimise transcoding.
     */
    private Collection<String> getVideoCommands(TranscodeProfile profile) {
        Collection<String> commands = new LinkedList<>();
        
        VideoTranscode transcode = profile.getVideoTranscode();
        
        if(transcode.getResolution() != null) {
            commands.add("-vf");
            commands.add("scale=w=" + transcode.getResolution().width + ":h=" + transcode.getResolution().height);
        }
        
        if(transcode.getCodec() != null) {
            // Video Codec
            commands.add("-c:v");

            switch(transcode.getCodec()) {       
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
                    commands.add("23");
                    commands.add("-preset");
                    commands.add("superfast");
                    commands.add("-pix_fmt");
                    commands.add("yuv420p");
                    commands.add("-profile:v");
                    commands.add("baseline");
                    
                    break;

                default:
                    commands.add(transcode.getCodec());
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
     * Returns a list of commands for burning text and picture based subtitles into the output video.
     */
    private Collection<String> getSubtitleCommands(TranscodeProfile profile) {   
        Collection<String> commands = new LinkedList<>();
        
        // Get subtitle transcode properties
        SubtitleTranscode transcode = profile.getSubtitleTranscodes()[profile.getSubtitleTrack()];
        
        if(transcode == null || transcode.getCodec() == null) {
            commands.add("-map");
            commands.add("0:v");
            commands.add("-sn");
            return commands;
        }
        
        // Hardcoded subtitles
        if(transcode.isHardcoded()) {
            switch(transcode.getCodec()) {
                // Text Based
                case "subrip": case "srt": case "webvtt":
                    commands.add("-map");
                    commands.add("0:v");
                    commands.add("-vf");
                    commands.add("setpts=PTS+" + profile.getOffset() + "/TB,subtitles=" + profile.getMediaElement().getPath() + ":si=" + profile.getSubtitleTrack() + ",setpts=PTS-STARTPTS");
                    break;

                // Picture Based
                case "dvd_subtitle": case "dvb_subtitle": case "hdmv_pgs_subtitle":
                    commands.add("-filter_complex");
                    commands.add("[0:v][0:s:" + profile.getSubtitleTrack() + "]overlay[v]");
                    commands.add("-map");
                    commands.add("[v]");
                    break;

                default:
                    commands.add("-map");
                    commands.add("0:v");
                    commands.add("-sn");
                    break;
            }
        } else {
            commands.add("-map");
            commands.add("0:v");
            commands.add("-map");
            commands.add("0:s:" + profile.getSubtitleTrack());
            commands.add("-c:s");
            commands.add(transcode.getCodec());
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
                codec = stream.getFormat();
                hardcode = true;
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
        boolean transcodeRequired = false;
        int quality = TranscodeUtils.getHighestVideoQuality(profile.getMediaElement());
        VideoStream stream = profile.getMediaElement().getVideoStream();
        
        if(stream == null) {
            return false;
        }
        
        // Process quality
        if(quality < 0 || quality < profile.getQuality()) {
            profile.setQuality(quality);
        }
        
        // Check if subtitles require transcode
        if(profile.getSubtitleTrack() != null && profile.getSubtitleTranscodes() != null) {
            if(profile.getSubtitleTranscodes()[profile.getSubtitleTrack()].isHardcoded()) {
                transcodeRequired = true;
            }
        }
        
        // Test if transcoding is necessary
        if(!transcodeRequired) {
            transcodeRequired = isTranscodeRequired(profile, stream);
        }
        
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
                    if(profile.getType() == StreamType.ADAPTIVE) {
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