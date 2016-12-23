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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.SystemUtils;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.service.LogService.Level;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    
    private final String TRANSCODER_FILE = SettingsService.getHomeDirectory() + "/transcoder";
        
    private final List<TranscodeProfile> transcodeProfiles = new ArrayList<>();
    
    public static final String[] FORMATS = {"hls","dash","matroska","webm"};
    public static final String[] SUPPORTED_FILE_EXTENSIONS = {"3gp","aac","avi","dsf","flac","m4a","m4v","mka","mkv","mp3","mp4","mpeg","mpg","oga","ogg","opus","wav","webm"};
    public static final String[] SUPPORTED_VIDEO_CODECS = {"h264","mpeg2video","vc1","vp8"};
    public static final String[] SUPPORTED_AUDIO_CODECS = {"aac","ac3","alac","dsd","dts","flac","mp3","opus","pcm","truehd","vorbis"};
    public static final String[] SUPPORTED_SUBTITLE_CODECS = {"subrip","webvtt","dvb","dvd","pgs"};
    public static final String[] TRANSCODE_VIDEO_CODECS = {"h264","vp8"};
    public static final String[] TRANSCODE_AUDIO_CODECS = {"aac","ac3","flac","mp3","pcm","vorbis"};
    
    public static final String[] LOSSLESS_CODECS= {"flac","pcm","alac","dsd"};
        
    private static final String[][] AUDIO_CODEC_FORMAT = {
        {"aac", "adts"},
        {"ac3", "ac3"},
        {"flac", "flac"},
        {"mp3", "mp3"},
        {"vorbis", "oga"},
        {"pcm", "wav"}
    };
    
    private static final String[][] FORMAT_CODECS = {
        {"hls", "h264,mp3,aac,ac3"},
        {"dash", "h264,aac"},
        {"matroska", "h264,vc1,mpeg2video,mp3,vorbis,aac,flac,pcm,ac3,dts,truehd,srt,subrip,webvtt,dvb,dvd,pgs"},
        {"webm", "vp8,vorbis,opus"},
    };
    
    private static final String[][] AUDIO_MIME_TYPES = {
        {"aac", "audio/aac"},
        {"adts", "audio/aac"},
        {"aiff", "audio/aiff"},
        {"dash", "application/dash+xml"},
        {"dsf", "audio/dsf"},
        {"flac", "audio/flac"},
        {"hls", "application/x-mpegurl"},
        {"m4a", "audio/mp4"},
        {"mka", "audio/x-matroska"},
        {"matroska", "audio/x-matroska"},
        {"mp3", "audio/mpeg"},
        {"mp4", "audio/mp4"},
        {"oga", "audio/ogg"},
        {"ogg", "audio/ogg"},
        {"opus", "audio/opus"},        
        {"wav", "audio/wav"},
        {"webm", "audio/webm"}
    };
    
    private static final String[][] VIDEO_MIME_TYPES = {
        {"3gp", "video/3gpp"},
        {"avi", "video/avi"},
        {"dash", "application/dash+xml"},
        {"hls", "application/x-mpegurl"},
        {"m4v", "video/mp4"},
        {"matroska", "video/x-matroska"},
        {"mkv", "video/x-matroska"},
        {"mp4", "video/mp4"},
        {"mpeg", "video/mpeg"},
        {"mpg", "video/mpeg"},
        {"ogg", "video/ogg"},
        {"ogv", "video/ogg"},
        {"ts", "video/MP2T"},
        {"webm", "video/webm"}
    };
    
    private static final String[][] AUDIO_CODEC_ENCODER = {
        {"mp3", "libmp3lame"},
        {"vorbis", "libvorbis"},
        {"aac", "aac"},
        {"flac", "flac"},
        {"pcm", "pcm_s16le"},
        {"ac3", "ac3"}
    };
    
    public static final String[][] AUDIO_CODEC_ISO_SPEC = {
        {"mp3", "mp4a.69"},
        {"aac", "mp4a.40.2"},
        {"ac3", "ac-3"},
        {"eac3", "ec-3"},
    };
    
    private static final String[][] AUDIO_CODEC_QUALITY = {
        {"mp3", "6", "4", "0", "0"},
        {"vorbis", "2", "5", "8", "10"},
        {"aac", "1", "2", "3", "10"}
    };
    
    public static final int[] AUDIO_QUALITY_MAX_BITRATE = {64,96,160,-1};
    
    public static final int[] VIDEO_QUALITY_MAX_BITRATE = {250,375,600,1000,1500,2500,4500};
    
    public static final int[] VIDEO_QUALITY_AUDIO_QUALITY = {AudioQuality.LOW,
                                                             AudioQuality.LOW,
                                                             AudioQuality.LOW,
                                                             AudioQuality.LOW,
                                                             AudioQuality.MEDIUM,
                                                             AudioQuality.MEDIUM,
                                                             AudioQuality.HIGH
    };
    
    public static final Dimension[] VIDEO_QUALITY_RESOLUTION = {new Dimension(320,240),
                                                                new Dimension(384,288),
                                                                new Dimension(512,384),
                                                                new Dimension(640,480),
                                                                new Dimension(720,480),
                                                                new Dimension(1280,720),
                                                                new Dimension(1920,1080)
    };
    
    private static final String[][] AUDIO_CODEC_MAX_SAMPLE_RATE = {
        {"mp3", "48000"},
        {"vorbis", "48000"},
        {"aac", "96000"},
        {"flac", "192000"},
        {"pcm", "192000"},
        {"ac3", "48000"}
    };
    
    private static final String[][] CHANNEL_CONFIGURATION = {
        {"1", "1"},
        {"mono", "1"},
        {"2", "2"},
        {"stereo", "2"},
        {"4", "4"},
        {"quad", "4"},
        {"5", "5"},
        {"5.0", "5"},
        {"6", "6"},
        {"5.1", "6"},
        {"7", "7"},
        {"6.1", "7"},
        {"8", "8"},
        {"7.1", "8"}
    };
    
    // Extract transcoder to data directory
    public TranscodeService() {
        InputStream inputStream = null;
        OutputStream outputStream;
                
        // Get transcoder
        if(SystemUtils.IS_OS_WINDOWS) {
            inputStream = getClass().getResourceAsStream("ffmpeg.exe");
        } else if(SystemUtils.IS_OS_LINUX) {
            inputStream = getClass().getResourceAsStream("ffmpeg");
        }

        // Check we found the transcoder
        if(inputStream == null) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Transcoder not found!", null);
            return;
        }
            
        // Copy transcoder to filesystem
        try {
            LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Preparing transcoder.", null);
            File file = new File(TRANSCODER_FILE);

            int readBytes;
            byte[] buffer = new byte[4096];

            outputStream = new FileOutputStream(file);
            
            while ((readBytes = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readBytes);
            }
            
            // Close streams
            inputStream.close();
            outputStream.close();

            // Check file copied successfully
            if(!file.exists()) {
                LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to extract transcoder!", null);
                return;
            }
            
            // Make sure file is executable
            file.setExecutable(true);
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to extract transcoder!", ex);
        }
    }
    
    public static String[] getSupportedCodecs() {
        List<String> codecs = new ArrayList<>();
        codecs.addAll(Arrays.asList(SUPPORTED_VIDEO_CODECS));
        codecs.addAll(Arrays.asList(SUPPORTED_AUDIO_CODECS));
        codecs.addAll(Arrays.asList(SUPPORTED_SUBTITLE_CODECS));
        return codecs.toArray(new String[codecs.size()]);
    }
    
    public static String[] getTranscodeCodecs() {
        List<String> codecs = new ArrayList<>();
        codecs.addAll(Arrays.asList(TRANSCODE_VIDEO_CODECS));
        codecs.addAll(Arrays.asList(TRANSCODE_AUDIO_CODECS));
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
                command.add("-c:v");
                command.add("copy");
                
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
                profile.setQuality(extra);
                processVideo(profile);
            }
            
            if(profile.getVideoTranscode() != null) {
                command.add("-map");
                command.add("0:v");

                command.addAll(getVideoCommands(profile));
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
        }
                
        // Use all CPU cores
        command.add("-threads");
        command.add("0");
        
        // Maintain timestamps
        command.add("-copyts");
        
        // Format
        command.add("-f");
        command.add("mpegts");
        
        command.add("-mpegts_copyts");
        command.add("1");
        
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
                    
                    commands.add(SettingsService.getHomeDirectory().getPath() + "/stream/" + profile.getID() + "/%d.ts");
                    
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
                                        
                    commands.add(SettingsService.getHomeDirectory().getPath() + "/stream/" + profile.getID() + "/playlist.mpd");
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
        
        if(transcode.getCodec() != null) {
            // De-interlace for all codecs (auto-detect)
            commands.add("-deinterlace");

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
                    commands.add("veryfast");
                    commands.add("-pix_fmt");
                    commands.add("yuv420p");
                    commands.add("-profile:v");
                    
                    if(profile.getQuality() < 4) {
                        commands.add("baseline");
                    } else {
                        commands.add("high");
                    }
                    
                    break;

                default:
                    commands.add(transcode.getCodec());
            }
        }
        
        if(transcode.getResolution() != null) {
            commands.add("-s");
            commands.add(transcode.getResolution().width + "x" + transcode.getResolution().height);
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
    
    /*
     * Returns a suitable video resolution based on the video quality requested.
     */
    public static Dimension getVideoResolution(MediaElement element, int quality) {
        // Check variables
        if(element == null || !VideoQuality.isValid(quality)) {
            return null;
        }
        
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) {
            return null;
        }
        
        // Determine resolution based on the requested quality.
        Dimension requested = VIDEO_QUALITY_RESOLUTION[quality];

        // Make sure we managed to set a resolution based on the requested quality
        if(requested == null || element.getVideoHeight() == null || element.getVideoWidth() == null) {
            return null;
        }
        
        // If the original resolution is less than the requested we don't need to continue
        Dimension original = new Dimension(element.getVideoWidth(), element.getVideoHeight());
        
        if (original.width < requested.width || original.height < requested.height) {
            return null;
        }

        // Calculate the aspect ratio of the original video.
        double aspectRatio = new Integer(original.width).doubleValue() / new Integer(original.height).doubleValue();
        requested.height = (int) Math.round(requested.width / aspectRatio);

        return new Dimension(even(requested.width), even(requested.height));
    }
    
    
    /*
     * Make sure width and height are multiples of two for the transcoder.
     */
    private static int even(int size) {
        return size + (size % 2);
    }
    
    /*
     * Returns the highest possible transcode quality for a given video element
     */
    public static int getHighestVideoQuality(MediaElement element) {
        // Check variables
        if(element == null || element.getType() != MediaElementType.VIDEO) {
            return -1;
        }
        
        // Loop through possible qualities until we find the highest
        for(int i = VideoQuality.getMax(); i >= 0; i--) {
            if(getVideoResolution(element, i) != null) {
                return i;
            }
        }

        return -1;
    }
    
    public static boolean isAudioStreamAvailable(int streamNum, MediaElement element) {        
        // Get list of audio streams for element
        List<AudioStream> audioStreams = element.getAudioStreams();
        
        if(audioStreams == null) {
            return false;
        }
        
        return audioStreams.size() >= streamNum;
    }
    
    public static boolean isSubtitleStreamAvailable(int streamNum, MediaElement element) {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) {
            return false;
        }
        
        // Get list of subtitle streams for video file
        List<SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) {
            return false;
        }
        
        return subtitles.size() >= streamNum;
    }
    
    public Integer getForcedSubtitleIndex(MediaElement element) {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) {
            return null;
        }
        
        // Get list of subtitle streams for video file
        List<SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) {
            return null;
        }
        
        // Scan subtitles for forced streams
        for(SubtitleStream subtitle : subtitles) {
            if(subtitle.isForced()) {
                return subtitle.getStream();
            }
        }
        
        return null;
    }
    
    public static String getMimeType(String format, byte type) {
        if(type == MediaElementType.AUDIO) {
            for (String[] map : AUDIO_MIME_TYPES) {
                if (map[0].equalsIgnoreCase(format)) { return map[1]; }
            }
        } else if(type == MediaElementType.VIDEO) {
            for (String[] map : VIDEO_MIME_TYPES) {
                if (map[0].equalsIgnoreCase(format)) { return map[1]; }
            }
        }
        
        return null;
    }
    
    public static String getFormatForAudioCodec(String codec) {
        for (String[] map : AUDIO_CODEC_FORMAT) {
            if (map[0].contains(codec)) { return map[1]; }
        }
        
        return null;
    }
    
    //
    // Return the codecs supported by a given format
    //
    public static String[] getCodecsForFormat(String test) {
        if(test == null) {
            return null;
        }
        
        for(String[] format : FORMAT_CODECS) {
            if(format[0].contains(test)) {
                return format[1].split(",");
            }
        }
        
        return null;
    }
    
    public static int validateAudioQuality(int quality) {
        if(quality >= AudioQuality.LOW && quality <= AudioQuality.LOSSLESS) {
            return quality;
        } else {
            return AudioQuality.MEDIUM;
        }
    }
    
    public static int getAudioChannelCount(String audioConfiguration) {
        for (String[] map : CHANNEL_CONFIGURATION) {
            if (map[0].equalsIgnoreCase(audioConfiguration)) {
                return Integer.valueOf(map[1]);
            }
        }
        
        // Default to stereo
        return 2;
    }
    
    public static Integer getAudioQualityForCodec(String codec, int quality) {
        if(quality >= AudioQuality.LOW && quality <= AudioQuality.LOSSLESS) {
            for (String[] map : AUDIO_CODEC_QUALITY) {
                if (codec.contains(map[0])) {
                    return Integer.valueOf(map[quality + 1]);
                }
            }
        }
        
        return null;
    }
    
    public static String getEncoderForAudioCodec(String codec) {
        for (String[] map : AUDIO_CODEC_ENCODER) {
            if (map[0].contains(codec)) {
                return map[1];
            }
        }
        
        return null;
    }
    
    public static String getIsoSpecForAudioCodec(String codec) {
        for (String[] map : AUDIO_CODEC_ISO_SPEC) {
            if (codec.contains(map[0])) {
                return map[1];
            }
        }
        
        return null;
    }
    
    public static Integer getMaxSampleRateForCodec(String codec) {
        for (String[] map : AUDIO_CODEC_MAX_SAMPLE_RATE) {
            if (codec.contains(map[0])) {
                return Integer.valueOf(map[1]);
            }
        }
        
        return null;
    }
    
    public static int compareDimensions(Dimension d1, Dimension d2) {
        int a = d1.height * d1.width;
        int b = d2.height * d2.width;
        
        if(a < b) {
            return 0;
        } else if(a > b) {
            return 1;
        } else if(a == b) {
            return 2;
        }
        
        return -1;
    }
    
    public static String[] sortStringList(String[] listToSort, String[] priorityList) {
        List<String> list = new ArrayList<>();
        
        // First parse
        for(String item : listToSort) {
            if(isSupported(priorityList, item)) {
                list.add(item);
            }
        }
        
        // Second parse
        for(String item : listToSort) {
            if(!isSupported(priorityList, item)) {
                list.add(item);
            }
        }
        
        return list.toArray(new String[list.size()]);
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
                if(!isSupported(profile.getCodecs(), stream.getFormat())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public static boolean isTranscodeRequired(TranscodeProfile profile, VideoStream stream) {
        if(!isSupported(profile.getCodecs(), stream.getCodec())) {
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
            if(profile.getMediaElement().getBitrate() > VIDEO_QUALITY_MAX_BITRATE[profile.getQuality()]) {
                return true;
            }

            // Check resolution
            if(compareDimensions(new Dimension(stream.getWidth(), stream.getHeight()), VIDEO_QUALITY_RESOLUTION[profile.getQuality()]) == 1) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isTranscodeRequired(TranscodeProfile profile, AudioStream stream) {
        // Check audio codec
        if(getAudioChannelCount(stream.getConfiguration()) > 2) {
            if(profile.getMchCodecs() == null || !isSupported(profile.getMchCodecs(), stream.getCodec())) {
                return true;
            }
        } else {
            if(!isSupported(profile.getCodecs(), stream.getCodec())) {
                return true;
            }
        }

        // Check audio sample rate
        if(stream.getSampleRate() > profile.getMaxSampleRate()) {
            return true;
        }

        // If direct play is not enabled check stream parameters
        if(!profile.isDirectPlayEnabled()) {
            // Check bitrate for audio elements
            if(profile.getMediaElement().getType() == MediaElementType.AUDIO) {
                int bitrate = (getAudioChannelCount(stream.getConfiguration()) * AUDIO_QUALITY_MAX_BITRATE[profile.getQuality()]);

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
            
            if(isSupported(profile.getCodecs(), stream.getFormat()) && isSupported(getCodecsForFormat(profile.getFormat()), stream.getFormat())) {
                codec = "copy";
            } else if(isSupported(SUPPORTED_SUBTITLE_CODECS, stream.getFormat())){
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
        VideoStream stream = profile.getMediaElement().getVideoStream();
        
        if(stream == null) {
            return false;
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
            transcodeRequired = !isSupported(getCodecsForFormat(profile.getFormat()), stream.getCodec());
        }
        
        if(transcodeRequired) {
            // Get suitable codec
            for(String test : profile.getCodecs()) {
                if(isSupported(getCodecsForFormat(profile.getFormat()), test) && isSupported(TRANSCODE_VIDEO_CODECS, test)) {
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
                resolution = getVideoResolution(profile.getMediaElement(), profile.getQuality());      
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
                    transcodeRequired = !isSupported(getCodecsForFormat(profile.getFormat()), stream.getCodec());
                } else {
                    transcodeRequired = !isSupported(TRANSCODE_AUDIO_CODECS, stream.getCodec());
                }
            }
            
            if(!transcodeRequired) {
                // Work around transcoder bug where flac files have the wrong duration if the stream is copied
                codec = "copy";
                
                // Get format if required
                if(profile.getFormat() == null) {
                    profile.setFormat(getFormatForAudioCodec(stream.getCodec()));
                }
            } else {
                // Test if lossless codecs should be prioritised
                if(profile.getMediaElement().getType() == MediaElementType.AUDIO && (profile.getQuality() == AudioQuality.LOSSLESS || profile.isDirectPlayEnabled()) && isSupported(LOSSLESS_CODECS, stream.getCodec())) {
                    profile.setCodecs(sortStringList(profile.getCodecs(), LOSSLESS_CODECS));
                    
                    if(profile.mchCodecs != null) {
                        profile.setMchCodecs(sortStringList(profile.getMchCodecs(), LOSSLESS_CODECS));
                    }
                }
                
                // Check for multichannel codecs if this is a multichannel stream
                if(getAudioChannelCount(stream.getConfiguration()) > 2) {
                    // Try to get a suitable multichannel codec
                    if(profile.getMchCodecs() != null) {
                        for(String test : profile.getMchCodecs()) {
                            if(isSupported(TRANSCODE_AUDIO_CODECS, test)) {
                                if(profile.getFormat() != null) {
                                    if(isSupported(getCodecsForFormat(profile.getFormat()), test)) {
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
                        if(isSupported(TRANSCODE_AUDIO_CODECS, test)) {
                            if(profile.getFormat() != null) {
                                if(isSupported(getCodecsForFormat(profile.getFormat()), test)) {
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
                    if((stream.getSampleRate() > profile.getMaxSampleRate()) || (stream.getSampleRate() > getMaxSampleRateForCodec(codec))) {
                        sampleRate = (profile.getMaxSampleRate() > getMaxSampleRateForCodec(codec)) ? getMaxSampleRateForCodec(codec) : profile.getMaxSampleRate();
                    }
                    
                    // Quality
                    if(profile.getMediaElement().getType() == MediaElementType.AUDIO) {
                        quality = getAudioQualityForCodec(codec, profile.getQuality());
                    } else if (profile.getMediaElement().getType() == MediaElementType.VIDEO) {
                        quality = getAudioQualityForCodec(codec, VIDEO_QUALITY_AUDIO_QUALITY[profile.getQuality()]);
                    }
                    
                    // Get format if required
                    if(profile.getFormat() == null) {
                        profile.setFormat(getFormatForAudioCodec(codec));
                    }
                    
                    // Update codec
                    codec = getEncoderForAudioCodec(codec);
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
    // Helper Functions For Child Transcode Services
    //
    public static boolean isSupported(String[] list, String test) {
        for (String item : list) {
            if(test.contains(item)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static class AudioQuality {
        public static final int LOW = 0;
        public static final int MEDIUM = 1;
        public static final int HIGH = 2;
        public static final int LOSSLESS = 3;
        
        public static boolean isValid(int quality) {
            return !(quality > 3 || quality < 0);
        }
    }
    
    public static class VideoQuality {
        public static final int VERY_LOW = 0;
        public static final int LOW = 1;
        public static final int MEDIUM = 2;
        public static final int HIGH = 3;
        public static final int VERY_HIGH = 4;
        public static final int HD = 5;
        public static final int FULLHD = 6;
        
        public static boolean isValid(int quality) {
            return !(quality > 6 || quality < 0);
        }
        
        public static int getMax() {
            return 6;
        }
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
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class TranscodeProfile {
        private UUID id;
        private byte type;
        private MediaElement element;
        private String[] files, codecs, mchCodecs;
        private Integer quality, maxBitRate, maxSampleRate = 48000;
        private String url, format, mimeType, client;
        private VideoTranscode videoTranscode;
        private AudioTranscode[] audioTranscodes;
        private SubtitleTranscode[] subtitleTranscodes;
        private Integer audioTrack, subtitleTrack;
        private Integer offset = 0;
        private boolean directPlay = false;
        private boolean active = true;
        
        public TranscodeProfile() {}
        
        public TranscodeProfile(UUID id) {
            this.id = id;
        }
        
        @Override
        public String toString() {
            return String.format("TranscodeProfile[ID=%s, Type=%s, MediaElement=%s, Client=%s, Supported Files=%s, Supported Codecs=%s, Supported Multichannel Codecs=%s, Quality=%s, Max Sample Rate=%s, Max Bit Rate=%s, Format=%s, Mime Type=%s, Video Transcode=%s, Audio Transcodes=%s, Subtitle Transcodes=%s, Audio Track=%s, Subtitle Track=%s, Offset=%s, Direct Play=%s",
                    id == null ? "null" : id.toString(),
                    String.valueOf(type),
                    element == null ? "null" : element.getID().toString(),
                    client == null ? "null" : client,
                    files == null ? "null" : Arrays.toString(files),
                    codecs == null ? "null" : Arrays.toString(codecs),
                    mchCodecs == null ? "null" : Arrays.toString(mchCodecs),
                    quality == null ? "null" : quality.toString(),
                    maxSampleRate == null ? "null" : maxSampleRate.toString(),
                    maxBitRate == null ? "null" : maxBitRate.toString(),
                    format == null ? "null" : format,
                    mimeType == null ? "null" : mimeType,
                    videoTranscode == null ? "null" : videoTranscode.toString(),
                    audioTranscodes == null ? "null" : Arrays.toString(audioTranscodes),
                    subtitleTranscodes == null ? "null" : Arrays.toString(subtitleTranscodes),
                    audioTrack == null ? "null" : audioTrack.toString(),
                    subtitleTrack == null ? "null" : subtitleTrack.toString(),
                    offset == null ? "null" : offset.toString(),
                    String.valueOf(directPlay));
        }
        
        public UUID getID() {
            return id;
        }
        
        public void setID(UUID id) {
            this.id = id;
        }
        
        public byte getType() {
            return type;
        }
        
        public void setType(byte type) {
            this.type = type;
        }
        
        @JsonIgnore
        public MediaElement getMediaElement() {
            return element;
        }
        
        public void setMediaElement(MediaElement element) {
            this.element = element;
        }
        
        @JsonIgnore
        public boolean isDirectPlayEnabled() {
            return directPlay;
        }
        
        public void setDirectPlayEnabled(boolean directPlay) {
            this.directPlay = directPlay;
        }

        @JsonIgnore
        public String[] getFiles() {
            return files;
        }
        
        public void setFiles(String[] files) {
            this.files = files;
        }
        
        @JsonIgnore
        public String[] getCodecs() {
            return codecs;
        }
        
        public void setCodecs(String[] codecs) {
            this.codecs = codecs;
        }
        
        @JsonIgnore
        public String[] getMchCodecs() {
            return mchCodecs;
        }
        
        public void setMchCodecs(String[] mchCodecs) {
            this.mchCodecs = mchCodecs;
        }
        
        @JsonIgnore
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        @JsonIgnore
        public String getFormat() {
            return format;
        }
        
        public void setFormat(String format) {
            this.format = format;
        }
        
        public String getMimeType() {
            return mimeType;
        }
        
        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }
        
        @JsonIgnore
        public String getClient() {
            return client;
        }
        
        public void setClient(String client) {
            this.client = client;
        }
        
        @JsonIgnore
        public VideoTranscode getVideoTranscode() {
            return videoTranscode;
        }
        
        public void setVideoTranscode(VideoTranscode videoTranscode) {
            this.videoTranscode = videoTranscode;
        }
        
        @JsonIgnore
        public AudioTranscode[] getAudioTranscodes() {
            return audioTranscodes;
        }
        
        public void setAudioTranscodes(AudioTranscode[] audioTranscodes) {
            this.audioTranscodes = audioTranscodes;
        }
        
        @JsonIgnore
        public SubtitleTranscode[] getSubtitleTranscodes() {
            return subtitleTranscodes;
        }
        
        public void setSubtitleTranscodes(SubtitleTranscode[] subtitleTranscodes) {
            this.subtitleTranscodes = subtitleTranscodes;
        }
        
        @JsonIgnore
        public Integer getQuality() {
            return quality;
        }
        
        public void setQuality(int quality) {
            this.quality = quality;
        }
        
        @JsonIgnore
        public Integer getAudioTrack() {
            return audioTrack;
        }
        
        public void setAudioTrack(Integer audioTrack) {
            this.audioTrack = audioTrack;
        }
        
        @JsonIgnore
        public Integer getSubtitleTrack() {
            return subtitleTrack;
        }
        
        public void setSubtitleTrack(Integer subtitleTrack) {
            this.subtitleTrack = subtitleTrack;
        }
        
        @JsonIgnore
        public Integer getMaxSampleRate() {
            return maxSampleRate;
        }
        
        public void setMaxSampleRate(int maxSampleRate) {
            this.maxSampleRate = maxSampleRate;
        }
        
        @JsonIgnore
        public Integer getMaxBitRate() {
            return maxBitRate;
        }
        
        public void setMaxBitRate(int maxBitRate) {
            this.maxBitRate = maxBitRate;
        }
        
        @JsonIgnore
        public Integer getOffset() {
            return offset;
        }
        
        public void setOffset(int offset) {
            this.offset = offset;
        }
        
        @JsonIgnore
        public boolean isActive() {
            return active;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class VideoTranscode {
        private final String codec;
        private final Dimension resolution;
        
        public VideoTranscode(String codec, Dimension resolution) {
            this.codec = codec;
            
            this.resolution = resolution;
        }
        
        @Override
        public String toString() {
            return String.format("{Codec=%s, Resolution=%s}",
                    codec == null ? "null" : codec,
                    resolution == null ? "null" : String.format("%dx%d", resolution.width, resolution.height));
        }
        
        public String getCodec() {
            return codec;
        }
        
        @JsonIgnore
        public Dimension getResolution() {
            return resolution;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class AudioTranscode {
        private final String codec;
        private final Integer quality;
        private final Integer sampleRate;
        private final boolean downmix;
        
        public AudioTranscode(String codec, Integer quality, Integer sampleRate, boolean downmix) {
            this.codec = codec;
            this.quality = quality;
            this.sampleRate = sampleRate;
            this.downmix = downmix;
        }
        
        @Override
        public String toString() {
            return String.format("{Codec=%s, Quality=%s, Sample Rate=%s, Downmix=%s}",
                    codec == null ? "null" : codec,
                    quality == null ? "null" : quality.toString(),
                    sampleRate == null ? "null" : sampleRate.toString(),
                    String.valueOf(downmix));
        }
        
        public String getCodec() {
            return codec;
        }
        
        public Integer getQuality() {
            return quality;
        }
        
        public Integer getSampleRate() {
            return sampleRate;
        }
        
        public boolean isDownmixed() {
            return downmix;
        }
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class SubtitleTranscode {
        private final String codec;
        private boolean hardcode = false;
        
        public SubtitleTranscode(String codec, boolean hardcode) {
            this.codec = codec;
            this.hardcode = hardcode;
        }
        
        @Override
        public String toString() {
            return String.format("{Codec=%s, Hardcoded=%s}",
                    codec == null ? "null" : codec,
                    String.valueOf(hardcode));
        }
        
        public String getCodec() {
            return codec;
        }
        
        public boolean isHardcoded() {
            return hardcode;
        }
    }
    
    public static class StreamType {
        public static final byte TRANSCODE = 0;
        public static final byte ADAPTIVE = 1;
        public static final byte FILE = 2;
    }
}