package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.Stream;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.StreamProfile;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.transcode.muxer.HLSMuxer;
import com.scooter1556.sms.server.io.NullStream;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.service.parser.TranscoderParser;
import com.scooter1556.sms.server.transcode.muxer.DashMuxer;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import com.scooter1556.sms.server.transcode.muxer.Muxer;

public class TranscodeUtils {
    
    private static final String CLASS_NAME = "TranscodeUtils";
    
    private static final String TRANSCODER = "ffmpeg";
    
    public static final String[] TRANSCODER_PATH_LINUX = {
        "/usr/local/bin/ffmpeg",
        "/usr/bin/ffmpeg",
    };
    
    public static final String[] TRANSCODER_PATH_WINDOWS = {
        System.getenv("SystemDrive") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffmpeg.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "ffmpeg.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffmpeg.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "ffmpeg.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffmpeg.exe",
    };
    
    public static final String ISO_AVC_BASELINE = "avc1.42E01E";
    public static final String ISO_AVC_MAIN = "avc1.4D401F";
    public static final String ISO_AVC_HIGH = "avc1.640029";
    public static final String ISO_HEVC_MAIN = "hev1.1.6.L150";
    public static final String ISO_HEVC_MAIN10 = "hev1.2.6.L150";
    public static final String ISO_MP3 = "mp4a.69";
    public static final String ISO_AAC = "mp4a.40.2";
    public static final String ISO_AC3 = "ac-3";
    public static final String ISO_EAC3 = "ec-3";
    public static final String ISO_VORBIS = "vorbis";
    public static final String ISO_PCM = "1";
    
    public static final Integer DEFAULT_SEGMENT_DURATION = 10;

    public static final String[][] AUDIO_CODEC_FORMAT = {
        {"aac", "adts"},
        {"ac3", "ac3"},
        {"eac3", "eac3"},
        {"flac", "flac"},
        {"mp3", "mp3"},
        {"vorbis", "oga"},
        {"pcm", "wav"}
    };
    
    public static final String[][] SUBTITLE_MIME_TYPES = {
        {"webvtt", "text/vtt"}
    };
    
    public static final String[][] AUDIO_CODEC_QUALITY = {
        {"mp3", "6", "4", "0", "0"},
        {"vorbis", "2", "5", "8", "10"},
        {"aac", "1", "2", "3", "10"}
    };
    
    public static final int[] AUDIO_QUALITY_MAX_BITRATE = {96,196,320,320};
    
    public static final int[] AVC_VIDEO_QUALITY_MAX_BITRATE = {1000,1500,2000,3000,4000,6000};
    public static final int[] AVC_VIDEO_QUALITY_AVERAGE_BITRATE = {400,800,1000,1800,2500,4500};
    public static final int[] HEVC_VIDEO_QUALITY_MAX_BITRATE = {500,700,1000,1500,2000,4000,15000};
    public static final int[] HEVC_VIDEO_QUALITY_AVERAGE_BITRATE = {200,400,500,900,1500,2000,8000};
    
    public static final int[] VIDEO_QUALITY_AUDIO_BITRATE = {64,
                                                             64,
                                                             64,
                                                             128,
                                                             128,
                                                             192,
                                                             192
    };
    
    public static final Dimension[] VIDEO_QUALITY_RESOLUTION = {new Dimension(426,240),
                                                                new Dimension(640,360),
                                                                new Dimension(848,480),
                                                                new Dimension(1024,576),
                                                                new Dimension(1280,720),
                                                                new Dimension(1920,1080),
                                                                new Dimension(3840,2160)
    };
    
    public static final String[][] CHANNEL_CONFIGURATION = {
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
    
    public static String[] getTranscoderPaths() {
        if(SystemUtils.IS_OS_WINDOWS) {
            return TRANSCODER_PATH_WINDOWS;
        } else if(SystemUtils.IS_OS_LINUX) {
            return TRANSCODER_PATH_LINUX;
        }
        
        return null;
    }
    
    public static Transcoder getTranscoder() {
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
    
    public static boolean isValidTranscoder(File transcoder) {
        // Check file exists and is executable
        if(transcoder == null || !transcoder.canExecute()) {
            return false;
        }
        
        // Check this is a supported transcoder
        String[] command = new String[]{transcoder.getAbsolutePath()};
        
        try {
            String[] result = ParserUtils.getProcessOutput(command, true);
            
            for (String line : result) {
                if(line.contains(TRANSCODER)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        
        return false;
    }
    
    public static boolean isSupported(String[] list, String test) {
        for (String item : list) {
            if(test.contains(item)) {
                return true;
            }
        }
        
        return false;
    }
    
    /*
     * Returns a suitable video resolution based on the video quality requested.
     */
    public static Dimension getVideoResolution(Dimension resolution, int quality) {
        // Check variables
        if(resolution == null || !VideoTranscode.VideoQuality.isValid(quality)) {
            return null;
        }
        
        // Determine resolution based on the requested quality.
        Dimension requested = TranscodeUtils.VIDEO_QUALITY_RESOLUTION[quality];

        // Make sure we managed to set a resolution based on the requested quality
        if(requested == null) {
            return null;
        }
        
        // If the original resolution is less than or equal to the requested resolution we don't need to continue
        if (resolution.width <= requested.width || resolution.height <= requested.height) {
            return null;
        }

        // Calculate the aspect ratio of the original video.
        double aspectRatio = new Integer(resolution.width).doubleValue() / new Integer(resolution.height).doubleValue();
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
    public static int getHighestVideoQuality(Dimension resolution) {
        // Check required variables
        if(resolution == null) {
            return -1;
        }
        
        // Loop through possible qualities until we find the highest
        for(int i = VideoTranscode.VideoQuality.getMax(); i >= 0; i--) {
            // Determine resolution based on quality.
            Dimension requested = TranscodeUtils.VIDEO_QUALITY_RESOLUTION[i];
            
            if(requested != null) {
                // Check if this is the closest match to the original resolution
                if (requested.width <= resolution.width || requested.height <= resolution.height) {
                    return i;
                }
            }
        }

        return -1;
    }
    
    /*
     * Returns the highest possible transcode quality based on bitrate
     */
    public static int getVideoQualityFromMaxBitrate(int codec, int maxBitrate) {
        // Loop through possible qualities until we find the highest
        for(int i = VideoTranscode.VideoQuality.getMax(); i >= 0; i--) {
            // Determine max bitrate based on quality
            int requested;
            
            switch(codec) {
                case SMS.Codec.AVC_BASELINE:
                case SMS.Codec.AVC_MAIN:
                case SMS.Codec.AVC_HIGH:
                    requested = TranscodeUtils.AVC_VIDEO_QUALITY_MAX_BITRATE[i];
                    break;
                    
                case SMS.Codec.HEVC_MAIN:
                    requested = TranscodeUtils.HEVC_VIDEO_QUALITY_MAX_BITRATE[i];
                    break;
                    
                default:
                    return 0;
            }
                        
            if(requested > 0 && maxBitrate >= requested) {
                return i;
            }
        }
        
        // Return the lowest quality by default
        return 0;
    }
    
    /*
     * Returns the max bitrate for a given codec
     */
    public static int getMaxBitrateForCodec(int codec, int quality) {
        // Check quality
        if(!VideoTranscode.VideoQuality.isValid(quality)) {
            return 0;
        }
        
        // Determine max bitrate based on quality
        switch(codec) {
            case SMS.Codec.AVC_BASELINE:
            case SMS.Codec.AVC_MAIN:
            case SMS.Codec.AVC_HIGH:
                return TranscodeUtils.AVC_VIDEO_QUALITY_MAX_BITRATE[quality];

            case SMS.Codec.HEVC_MAIN:
                return TranscodeUtils.HEVC_VIDEO_QUALITY_MAX_BITRATE[quality];

            default:
                return 0;
        }
    }
    
    /*
     * Returns the average bitrate for a given codec
     */
    public static int getAverageBitrateForCodec(int codec, int quality) {
        // Check quality
        if(!VideoTranscode.VideoQuality.isValid(quality)) {
            return 0;
        }
        
        // Determine average bitrate based on quality
        switch(codec) {
            case SMS.Codec.AVC_BASELINE:
            case SMS.Codec.AVC_MAIN:
            case SMS.Codec.AVC_HIGH:
                return TranscodeUtils.AVC_VIDEO_QUALITY_AVERAGE_BITRATE[quality];

            case SMS.Codec.HEVC_MAIN:
                return TranscodeUtils.HEVC_VIDEO_QUALITY_AVERAGE_BITRATE[quality];

            default:
                return 0;
        }
    }
    
    public static Integer getForcedSubtitleId(List<SubtitleStream> streams) {
        if(streams == null) {
            return null;
        }
        
        // Scan subtitles for forced streams
        for(SubtitleStream stream : streams) {
            if(stream.isForced()) {
                return stream.getStreamId();
            }
        }
        
        return null;
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
    
    public static String getFormatForAudioCodec(String codec) {
        for (String[] map : AUDIO_CODEC_FORMAT) {
            if (map[0].contains(codec)) { return map[1]; }
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
    
    public static String getEncoderForCodec(int codec) {
        switch(codec) {
            case SMS.Codec.AAC:
                return "aac";
                
            case SMS.Codec.AC3:
                return "ac3";
                
            case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH: case SMS.Codec.AVC_HIGH10:
                return "libx264";
                
            case SMS.Codec.COPY:
                return "copy";
                
            case SMS.Codec.EAC3:
                return "eac3";
                
            case SMS.Codec.FLAC:
                return "flac";
                
            case SMS.Codec.MP3:
                return "libmp3lame";
                
            case SMS.Codec.PCM:
                return "pcm_s16le";
                
            case SMS.Codec.SUBRIP:
                return "subrip";
                
            case SMS.Codec.WEBVTT:
                return "webvtt";
                
            case SMS.Codec.VORBIS:
                return "libvorbis";
                
            default:
                return null;
        }
    }
    
    public static String getIsoSpecForCodec(int codec) {
        switch(codec) {
            case SMS.Codec.AVC_BASELINE:
                return ISO_AVC_BASELINE;
                
            case SMS.Codec.AVC_MAIN:
                return ISO_AVC_MAIN;
                
            case SMS.Codec.AVC_HIGH:
                return ISO_AVC_HIGH;
                
            case SMS.Codec.HEVC_MAIN:
                return ISO_HEVC_MAIN;
                
            case SMS.Codec.HEVC_MAIN10:
                return ISO_HEVC_MAIN10;
                
            case SMS.Codec.AAC:
                return ISO_AAC;
                
            case SMS.Codec.MP3:
                return ISO_MP3;
                
            case SMS.Codec.VORBIS:
                return ISO_VORBIS;
                
            case SMS.Codec.AC3:
                return ISO_AC3;
                
            case SMS.Codec.EAC3:
                return ISO_EAC3;
                
            case SMS.Codec.PCM:
                return ISO_PCM;
                
            default:
                return "";
        }        
    }
    
    public static int getMaxChannelsForCodec(int codec) {
        switch(codec) {
            case SMS.Codec.AAC: case SMS.Codec.ALAC: case SMS.Codec.FLAC: case SMS.Codec.PCM: case SMS.Codec.DTSHD: case SMS.Codec.TRUEHD: case SMS.Codec.VORBIS:
                return 8;
                
            case SMS.Codec.AC3: case SMS.Codec.EAC3: case SMS.Codec.DTS:
                return 6;
                
            case SMS.Codec.MP3:
                return 2;
                
            default:
                return -1;
        }        
    }
    
    public static int getMaxSampleRateForCodec(int codec) {
        switch(codec) {
            case SMS.Codec.AC3: case SMS.Codec.EAC3: case SMS.Codec.DTS: case SMS.Codec.MP3: case SMS.Codec.VORBIS:
                return 48000;
                
            case SMS.Codec.AAC:
                return 96000;
                
            case SMS.Codec.ALAC: case SMS.Codec.FLAC: case SMS.Codec.PCM: case SMS.Codec.DTSHD: case SMS.Codec.TRUEHD:
                return 192000;
                
            default:
                return -1;
        }        
    }
    
    public static boolean isValidVideoStream(List<VideoStream> videoStreams, int streamId) {
        if(videoStreams == null || videoStreams.isEmpty()) {
            return false;
        }
        
        List<Stream> streams = new ArrayList<>();
        
        for(VideoStream stream : videoStreams) {
            streams.add(stream);
        }
        
        return isValidStream(streams, streamId);
    }
    
    public static boolean isValidAudioStream(List<AudioStream> audioStreams, int streamId) {
        if(audioStreams == null || audioStreams.isEmpty()) {
            return false;
        }
        
        List<Stream> streams = new ArrayList<>();
        
        for(AudioStream stream : audioStreams) {
            streams.add(stream);
        }
        
        return isValidStream(streams, streamId);
    }
    
    public static boolean isValidSubtitleStream(List<SubtitleStream> subtitleStreams, int streamId) {
        if(subtitleStreams == null || subtitleStreams.isEmpty()) {
            return false;
        }
        
        List<Stream> streams = new ArrayList<>();
        
        for(SubtitleStream stream : subtitleStreams) {
            streams.add(stream);
        }
        
        return isValidStream(streams, streamId);
    }
    
    public static boolean isValidStream(List<Stream> streams, int streamId) {
        for(Stream stream : streams) {
            Integer id = stream.getStreamId();
            
            if(id == null) {
                continue;
            }
            
            if(stream.getStreamId() == streamId) {
                return true;
            }
        }
        
        return false;
    }
    
    public static List<VideoTranscode> getVideoTranscodesById(VideoTranscode[] transcodes, int id) {
        List<VideoTranscode> result = new ArrayList<>();
        
        if(transcodes != null && transcodes.length > 0) {
            for(VideoTranscode transcode : transcodes) {
                if(transcode.getId() == id) {
                    result.add(transcode);
                }
            }
        }
        
        return result;
    }
    
    public static AudioTranscode getAudioTranscodeById(AudioTranscode[] transcodes, int id) {
        if(transcodes == null || transcodes.length == 0) {
            return null;
        }
        
        for(AudioTranscode transcode : transcodes) {
            if(transcode.getId() == id) {
                return transcode;
            }
        }
        
        return null;
    }
    
    public static SubtitleTranscode getSubtitleTranscodeById(SubtitleTranscode[] transcodes, int id) {
        if(transcodes == null || transcodes.length == 0) {
            return null;
        }
        
        for(SubtitleTranscode transcode : transcodes) {
            if(transcode.getId() == id) {
                return transcode;
            }
        }
        
        return null;
    }
    
    public static VideoStream getVideoStreamById(List<VideoStream> streams, int id) {
        if(streams == null || streams.isEmpty()) {
            return null;
        }
        
        for(VideoStream stream : streams) {
            if(stream.getStreamId() == id) {
                return stream;
            }
        }
        
        return null;
    }
    
    public static SubtitleStream getSubtitleStreamById(List<SubtitleStream> streams, int id) {
        if(streams == null || streams.isEmpty()) {
            return null;
        }
        
        for(SubtitleStream stream : streams) {
            if(stream.getStreamId() == id) {
                return stream;
            }
        }
        
        return null;
    }
    
    public static AudioStream getAudioStreamById(List<AudioStream> streams, int id) {
        if(streams == null || streams.isEmpty()) {
            return null;
        }
        
        for(AudioStream stream : streams) {
            if(stream.getStreamId() == id) {
                return stream;
            }
        }
        
        return null;
    }
    
    public static Muxer getTranscodeMuxer(int mode, int format) {
        switch(format) {
            case SMS.Format.HLS_TS:
            case SMS.Format.HLS_FMP4:
                return new HLSMuxer(mode, format);
                
            case SMS.Format.MPEG_DASH:
                return new DashMuxer(mode);
                
            default:
                return null;
        }
    }
    
    public static int getMuxerMode(int type) {
        switch(type) {
            case MediaElement.MediaElementType.VIDEO:
                return SMS.MuxerMode.VIDEO;
                
            case MediaElement.MediaElementType.AUDIO:
                return SMS.MuxerMode.AUDIO;
                
            default:
                return SMS.MuxerMode.UNSUPPORTED;
        }
    }

    public static StreamProfile getStreamProfile(MediaElement mediaElement, TranscodeProfile transcodeProfile) {
        // Checks
        if(mediaElement == null || transcodeProfile == null) {
            return null;
        }
        
        // Create new stream profile
        StreamProfile streamProfile = new StreamProfile();
        
        // Populate MIME type
        streamProfile.setMimeType(transcodeProfile.getMimeType());
        
        // Determine codecs
        List<Integer> codecs = new ArrayList<>();
        
        // If this is a direct stream populate codecs from media element
        if(transcodeProfile.getType() == TranscodeProfile.StreamType.DIRECT) {
            if(mediaElement.getVideoStreams() != null && !mediaElement.getVideoStreams().isEmpty()) {
                for(VideoStream stream : mediaElement.getVideoStreams()) {
                    codecs.add(stream.getCodec());
                }
            }
            
            if(mediaElement.getAudioStreams() != null && !mediaElement.getAudioStreams().isEmpty()) {
                for(AudioStream stream : mediaElement.getAudioStreams()) {
                    codecs.add(stream.getCodec());
                }
            }
            
            if(mediaElement.getSubtitleStreams() != null && !mediaElement.getSubtitleStreams().isEmpty()) {
                for(SubtitleStream stream : mediaElement.getSubtitleStreams()) {
                    codecs.add(stream.getCodec());
                }
            }
        } else {
            if(transcodeProfile.getVideoTranscodes() != null && transcodeProfile.getVideoTranscodes().length > 0) {
                for(VideoTranscode transcode : transcodeProfile.getVideoTranscodes()) {
                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        codecs.add(getVideoStreamById(mediaElement.getVideoStreams(), transcode.getId()).getCodec());
                    } else {
                        codecs.add(transcode.getCodec());
                    }
                }
            }
            
            if(transcodeProfile.getAudioTranscodes() != null && transcodeProfile.getAudioTranscodes().length > 0) {
                for(AudioTranscode transcode : transcodeProfile.getAudioTranscodes()) {
                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        codecs.add(getAudioStreamById(mediaElement.getAudioStreams(), transcode.getId()).getCodec());
                    } else {
                        codecs.add(transcode.getCodec());
                    }
                }
            }
            
            if(transcodeProfile.getSubtitleTranscodes() != null && transcodeProfile.getSubtitleTranscodes().length > 0) {
                for(SubtitleTranscode transcode : transcodeProfile.getSubtitleTranscodes()) {
                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        codecs.add(getSubtitleStreamById(mediaElement.getSubtitleStreams(), transcode.getId()).getCodec());
                    } else {
                        codecs.add(transcode.getCodec());
                    }
                }
            }
        }
        
        // Remove duplicate codecs
        Set<Integer> deDup = new HashSet<>();
        deDup.addAll(codecs);
        codecs.clear();
        codecs.addAll(deDup);
        
        // Add codecs to stream profile
        streamProfile.setCodecs(codecs.toArray(new Integer[codecs.size()]));
        
        return streamProfile;
    }
    
    public static Boolean isTranscodeRequired(MediaElement mediaElement, ClientProfile profile) {
        // Make sure we have the information we require
        if(mediaElement == null || profile.getCodecs() == null || profile.getFormats() == null) {
            return null;
        }
        
        // Check this is not a directory or otherwise unsupported
        if(mediaElement.getType() == MediaElementType.NONE || mediaElement.getType() == MediaElementType.DIRECTORY) {
            return null;
        }
        
        // Check format
        if(!ArrayUtils.contains(profile.getFormats(), mediaElement.getFormat())) {
            return true;
        }
        
        // Check video codec
        if(mediaElement.getVideoStreams() != null) {
            // Check video quality is set
            if(profile.getVideoQuality() == null) {
                return null;
            }
            
            if (mediaElement.getVideoStreams().stream().anyMatch((stream) -> (isTranscodeRequired(profile, mediaElement, stream) > SMS.TranscodeReason.NONE))) {
                return true;
            }
        }
        
        // Check audio streams
        if(mediaElement.getAudioStreams() != null) {
            if (mediaElement.getAudioStreams().stream().anyMatch((stream) -> (isTranscodeRequired(profile, mediaElement, stream)))) {
                return true;
            }
        }

        // Check subtitle streams
        if(mediaElement.getSubtitleStreams() != null) {
            if (mediaElement.getSubtitleStreams().stream().anyMatch((stream) -> (!ArrayUtils.contains(profile.getCodecs(), stream.getCodec())))) {
                return true;
            }
        }
        
        return false;
    }
    
    public static int isTranscodeRequired(ClientProfile profile, MediaElement mediaElement, VideoStream stream) {
        if(!ArrayUtils.contains(profile.getCodecs(), stream.getCodec())) {
            return SMS.TranscodeReason.CODEC_NOT_SUPPORTED_BY_CLIENT;
        }
        
        // Check maximum bitrate
        if(profile.getMaxBitrate() != null && profile.getMaxBitrate() > 0) {
            if(stream.getMaxBitrate() == null || stream.getMaxBitrate() == 0 || (stream.getMaxBitrate() > profile.getMaxBitrate())) {
                return SMS.TranscodeReason.BITRATE;
            }
        }

        // If client is not on the local network check stream parameters
        if(!profile.getLocal() || !profile.getDirectPlay()) {
            // Check resolution
            if(TranscodeUtils.compareDimensions(new Dimension(stream.getWidth(), stream.getHeight()), TranscodeUtils.VIDEO_QUALITY_RESOLUTION[profile.getVideoQuality()]) == 1) {
                return SMS.TranscodeReason.RESOLUTION;
            }
        }
        
        return SMS.TranscodeReason.NONE;
    }
    
    public static boolean isTranscodeRequired(ClientProfile profile, MediaElement mediaElement, AudioStream stream) {
        // Check audio codec
        if(stream.getChannels() > 2) {
            if(profile.getMchCodecs() == null || !ArrayUtils.contains(profile.getMchCodecs(), stream.getCodec())) {
                return true;
            }
        } else {
            if(!ArrayUtils.contains(profile.getCodecs(), stream.getCodec())) {
                return true;
            }
        }

        // Check audio sample rate
        if((stream.getSampleRate() > profile.getMaxSampleRate()) && (stream.getCodec() != SMS.Codec.DSD)) {
            return true;
        }
        
        // Replaygain
        if(profile.getReplaygain() != null) {
            if(profile.getReplaygain() == SMS.ReplaygainMode.TRACK 
                    && mediaElement.getReplaygainTrack() != null 
                    && mediaElement.getReplaygainTrack() != 0f) {
                return true;
            }
            
            if(profile.getReplaygain() == SMS.ReplaygainMode.ALBUM
                    && mediaElement.getReplaygainAlbum() != null 
                    && mediaElement.getReplaygainAlbum() != 0f) {
                return true;
            }
        }

        // If client is not on the local network check stream parameters
        if(!profile.getLocal() || !profile.getDirectPlay()) {
            // Check bitrate
            int bitrate;
            
            if(mediaElement.getType() == MediaElement.MediaElementType.VIDEO) {
                bitrate = TranscodeUtils.VIDEO_QUALITY_AUDIO_BITRATE[profile.getVideoQuality()];
            } else {
                bitrate = TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[profile.getAudioQuality()];
            }
            
            //  Calculate overall bitrate to compare
            if(bitrate > 0) {
                bitrate = new Double(bitrate * (stream.getChannels() * 0.5)).intValue();
            }

            if(bitrate > 0 && stream.getBitrate() > 0 && stream.getBitrate() > bitrate) {
                return true;
            }
        }
        
        return false;
    }
    
    public static void processMediaElementsForClient(List<MediaElement> mediaElements, ClientProfile profile) {
        // Do some basic checks
        if(profile == null || mediaElements == null || mediaElements.isEmpty()) {
            return;
        }
        
        // Process each media element to determine transcoded format for client
        mediaElements.forEach((MediaElement mediaElement) -> {
            Boolean transcodeRequired = isTranscodeRequired(mediaElement, profile);
            if (!(transcodeRequired == null)) {
                if (transcodeRequired) {
                    mediaElement.setFormat(profile.getFormat());
                }
            }
        });
    }
    
    public static int getSegmentDuration(VideoStream stream) {
        // If we don't have the data we need return the default  value
        if(stream == null || stream.getFPS() == null || stream.getGOPSize() == null || stream.getFPS() <= 0 || stream.getGOPSize() <= 0) {
            return DEFAULT_SEGMENT_DURATION;
        }
        
        // Calculate duration between key frames
        int interval = (int) Math.round(stream.getGOPSize() / stream.getFPS());
        
        // If interval is 1 or less return our default value
        if(interval <= 1) {
            return DEFAULT_SEGMENT_DURATION;
        }
        
        // If interval is greater than our default, return the calculated interval value
        if(interval >= DEFAULT_SEGMENT_DURATION) {
            return interval;
        }
        
        // Return a multiple of the calculated interval
        int multiplier = (int) Math.floor(DEFAULT_SEGMENT_DURATION / interval);
        return interval * multiplier;
    }
    
    public static Path[] getRenderDevices() {
        List<Path> devices = new ArrayList<>();
        
        if(!SystemUtils.IS_OS_LINUX) {
            return null;
        }
        
        File dir = new File("/dev/dri");
        
        // Check device directory exists
        if(!dir.isDirectory()) {
            return null;
        }
        
        for(File file : dir.listFiles()) {
            if(file.toString().contains("render")){
                devices.add(file.toPath());
            }
        }
        
        Path[] result = devices.toArray(new Path[0]);
        Arrays.sort(result);
        return result;
    }
    
    public static boolean runTranscodeCommands(String[][] commands) {
        Process process = null;
        
        if(commands == null || commands.length == 0) {
            return false;
        }
                
        try {
            for(String[] command : commands) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, StringUtils.join(command, " "), null);
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                process = processBuilder.start();
                new NullStream(process.getInputStream()).start();
                new NullStream(process.getErrorStream()).start();

                // Wait for process to finish
                int code = process.waitFor();

                // Check for error
                if(code != 1) {
                    return true;
                } else {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, StringUtils.join(command, " "), null);
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Transcode command failed. Attempting alternatives if available...", null);
                }
            }
        } catch(IOException | InterruptedException ex) {
            if(process != null) {
                process.destroy();
            }
            
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error occured whilst transcoding.", ex);
        }
        
        return false;
    }
}
