package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.VideoTranscode;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TranscodeUtils {
    
    public static final String[] TRANSCODE_PATH_LINUX = {
        "/usr/bin/ffmpeg"
    };
    
    public static final String[] TRANSCODE_PATH_WINDOWS = {
        System.getenv("SystemDrive") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffmpeg.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "ffmpeg.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "ffmpeg.exe",
    };
    
    public static final String[] FORMATS = {"hls","dash","matroska","webm"};
    public static final String[] SUPPORTED_FILE_EXTENSIONS = {"3gp","aac","avi","dsf","flac","m4a","m4v","mka","mkv","mp3","mp4","mpeg","mpg","oga","ogg","opus","wav","webm"};
    public static final String[] SUPPORTED_VIDEO_CODECS = {"h264","mpeg2video","vc1","vp8"};
    public static final String[] SUPPORTED_AUDIO_CODECS = {"aac","ac3","alac","dsd","dts","flac","mp3","opus","pcm","truehd","vorbis"};
    public static final String[] SUPPORTED_SUBTITLE_CODECS = {"subrip","webvtt","dvb","dvd","pgs"};
    public static final String[] TRANSCODE_VIDEO_CODECS = {"h264","vp8"};
    public static final String[] TRANSCODE_AUDIO_CODECS = {"aac","ac3","flac","mp3","pcm","vorbis"};
    
    public static final String[] LOSSLESS_CODECS= {"flac","pcm","alac","dsd"};
        
    public static final String[][] AUDIO_CODEC_FORMAT = {
        {"aac", "adts"},
        {"ac3", "ac3"},
        {"flac", "flac"},
        {"mp3", "mp3"},
        {"vorbis", "oga"},
        {"pcm", "wav"}
    };
    
    public static final String[][] FORMAT_CODECS = {
        {"hls", "h264,mp3,aac,ac3"},
        {"dash", "h264,aac"},
        {"matroska", "h264,vc1,mpeg2video,mp3,vorbis,aac,flac,pcm,ac3,dts,truehd,srt,subrip,webvtt,dvb,dvd,pgs"},
        {"webm", "vp8,vorbis,opus"},
    };
    
    public static final String[][] AUDIO_MIME_TYPES = {
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
    
    public static final String[][] VIDEO_MIME_TYPES = {
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
    
    public static final String[][] AUDIO_CODEC_ENCODER = {
        {"mp3", "libmp3lame"},
        {"vorbis", "libvorbis"},
        {"aac", "aac"},
        {"flac", "flac"},
        {"pcm", "pcm_s16le"},
        {"ac3", "ac3"}
    };
    
    public static final String[][] AUDIO_CODEC_ISO_SPEC = {
        {"mp3", "mp4a.40.34"},
        {"aac", "mp4a.40.2"},
        {"ac3", "ac-3"},
        {"eac3", "ec-3"},
    };
    
    public static final String[][] AUDIO_CODEC_QUALITY = {
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
    
    public static final String[][] AUDIO_CODEC_MAX_SAMPLE_RATE = {
        {"mp3", "48000"},
        {"vorbis", "48000"},
        {"aac", "96000"},
        {"flac", "192000"},
        {"pcm", "192000"},
        {"ac3", "48000"}
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
    public static Dimension getVideoResolution(MediaElement element, int quality) {
        // Check variables
        if(element == null || !VideoTranscode.VideoQuality.isValid(quality)) {
            return null;
        }
        
        // Check this is a video element
        if(element.getType() != MediaElement.MediaElementType.VIDEO) {
            return null;
        }
        
        // Determine resolution based on the requested quality.
        Dimension requested = TranscodeUtils.VIDEO_QUALITY_RESOLUTION[quality];

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
        if(element == null || element.getType() != MediaElement.MediaElementType.VIDEO) {
            return -1;
        }
        
        // Loop through possible qualities until we find the highest
        for(int i = VideoTranscode.VideoQuality.getMax(); i >= 0; i--) {
            if(getVideoResolution(element, i) != null) {
                return i;
            }
        }

        return -1;
    }
    
    public static boolean isAudioStreamAvailable(int streamNum, MediaElement element) {        
        // Get list of audio streams for element
        List<MediaElement.AudioStream> audioStreams = element.getAudioStreams();
        
        if(audioStreams == null) {
            return false;
        }
        
        return audioStreams.size() >= streamNum;
    }
    
    public static boolean isSubtitleStreamAvailable(int streamNum, MediaElement element) {
        // Check this is a video element
        if(element.getType() != MediaElement.MediaElementType.VIDEO) {
            return false;
        }
        
        // Get list of subtitle streams for video file
        List<MediaElement.SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) {
            return false;
        }
        
        return subtitles.size() >= streamNum;
    }
    
    public static Integer getForcedSubtitleIndex(MediaElement element) {
        // Check this is a video element
        if(element.getType() != MediaElement.MediaElementType.VIDEO) {
            return null;
        }
        
        // Get list of subtitle streams for video file
        List<MediaElement.SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) {
            return null;
        }
        
        // Scan subtitles for forced streams
        for(MediaElement.SubtitleStream subtitle : subtitles) {
            if(subtitle.isForced()) {
                return subtitle.getStream();
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
    
    public static String getMimeType(String format, byte type) {
        if(type == MediaElement.MediaElementType.AUDIO) {
            for (String[] map : AUDIO_MIME_TYPES) {
                if (map[0].equalsIgnoreCase(format)) { return map[1]; }
            }
        } else if(type == MediaElement.MediaElementType.VIDEO) {
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
}
