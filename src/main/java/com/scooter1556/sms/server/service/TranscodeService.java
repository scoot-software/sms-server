package com.scooter1556.sms.server.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.service.LogService.Level;
import java.awt.Dimension;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */

@Service
public class TranscodeService {
    
    private static final String CLASS_NAME = "TranscodeService";
    
    private static final String TRANSCODER = "ffmpeg";
    
    private static final String SUPPORTED_VIDEO_CODECS = "h264,vp8";
    private static final String SUPPORTED_AUDIO_CODECS = "mp3,vorbis,aac,flac,pcm,ac3,dsd,alac";
    private static final String SUPPORTED_SUBTITLE_CODECS = "srt,subrip,webvtt,dvb,dvd,pgs";
    private static final String LOSSLESS_AUDIO_CODECS = "flac,alac,pcm,dsd";
    
    private static final String[][] VIDEO_FORMATS = {
        {"h264", "matroska"},
        {"vp8", "webm"}};
    
    private static final String[][] AUDIO_FORMATS = {
        {"mp3", "mp3"},
        {"vorbis", "ogg"},
        {"opus", "ogg"},
        {"aac", "adts"},
        {"flac", "flac"},
        {"pcm", "wav"},
        {"ac3", "ac3"}};
    
    private static final String[][] VIDEO_CODEC = {
        {"h264", "libx264"},
        {"vp8", "libvpx"}};
    
    private static final String[][] AUDIO_CODEC = {
        {"mp3", "libmp3lame"},
        {"vorbis", "libvorbis"},
        {"opus", "libopus"},
        {"aac", "libfdk_aac"},
        {"flac", "flac"},
        {"pcm", "pcm_s16le"},
        {"ac3", "ac3"}};
    
    private static final String[][] AUDIO_BITRATES = {
        {"mp3", "128", "192", "256", "320"},
        {"vorbis", "96", "160", "256", "320"},
        {"opus", "96", "128", "256", "256"},
        {"aac", "96", "160", "256", "256"},
        {"ac3", "160", "192", "224", "224"}};
    
    private static final String[][] MULTI_CHANNEL_AUDIO_BITRATES = {
        {"vorbis", "288", "384", "448", "640"},
        {"opus", "240", "384", "448", "448"},
        {"aac", "240", "384", "448", "448"},
        {"ac3", "384", "448", "640", "640"}};
    
    private static final int[][] VIDEO_QUALITY_AUDIO_MAPPING = {
        {240, AudioQuality.LOW},
        {360, AudioQuality.LOW},
        {480, AudioQuality.MEDIUM},
        {720, AudioQuality.MEDIUM},
        {1080, AudioQuality.HIGH}};
    
    
    private static final String[][] MAX_SAMPLE_RATES = {
        {"mp3", "48000"},
        {"vorbis", "48000"},
        {"opus", "48000"},
        {"aac", "96000"},
        {"flac", "192000"},
        {"pcm", "192000"},
        {"ac3", "48000"}};
    
    private static final String[][] MAX_CHANNELS = {
        {"mp3", "2"},
        {"vorbis", "6"},
        {"opus", "6"},
        {"aac", "6"},
        {"flac", "8"},
        {"pcm", "8"},
        {"ac3", "6"}};
    
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
        {"7.1", "8"}};
    
    private static final int[][] VIDEO_RESOLUTION = {
        {240, 426, 240},
        {360, 640, 360},
        {480, 854, 480},
        {720, 1280, 720},
        {1080, 1920, 1080}};
    
    private static final int[][] VIDEO_BITRATE = {
        {240, 400},
        {360, 750},
        {480, 1000},
        {720, 2500},
        {1080, 4500}};
    
    private static final String[][] VIDEO_MIME_TYPES = {
        {"webm", "video/webm"},
        {"mpeg", "video/mpeg"},
        {"mp4", "video/mp4"},
        {"matroska", "video/x-matroska"},
        {"mkv", "video/x-matroska"},};
    
    private static final String[][] AUDIO_MIME_TYPES = {
        {"mp3", "audio/mpeg"},
        {"ogg", "audio/ogg"},
        {"oga", "audio/ogg"},
        {"adts", "audio/aac"},
        {"flac", "audio/flac"},
        {"wav", "audio/x-wav"},
        {"ac3", "audio/ac3"}};
    
    private static final String[][] SUBTITLE_MIME_TYPES = {
        {"vtt", "text/vtt"},
        {"srt", "text/plain"},};
    
    /*
     * Returns a file reference to the transcoder.
     */
    public File getTranscoder() 
    {       
        File transcoder = new File(getClass().getResource(TRANSCODER).getPath());
        
        // Check if the transcoder binary exists.
        if(!transcoder.exists())
        {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Transcoder not found!", null);
            return null;
        }
        
        // Make sure transcoder is executable.
        if(!transcoder.canExecute()) { transcoder.setExecutable(true); }
        
        return transcoder;
    }
    
    /*
     * Returns a list of commands for a given video codec to optimise transcoding.
     */
    public Collection<String> getVideoCodecCommands(String codec)
    {
        Collection<String> commands = new LinkedList<>();
        
        // De-interlace for all codecs (auto-detect)
        commands.add("-deinterlace");
        
        // Video Codec
        commands.add("-c:v");
        
        switch(codec)
        {       
            case "vp8":
                commands.add("libvpx");
                commands.add("-quality");
                commands.add("realtime");
                commands.add("-cpu-used");
                commands.add("5");
                break;
                
            case "h264":
                commands.add("libx264");
                commands.add("-preset");
                commands.add("superfast");
                commands.add("-tune");
                commands.add("zerolatency");
                commands.add("-profile:v");
                commands.add("baseline");
                break;
                
            default:
                return null;
        }
        
        return commands;
    }
    
    /*
     * Returns a suitable video resolution based on the video quality requested.
     */
    public Dimension getVideoResolution(int quality, MediaElement element)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return null; }
        
        // Determine resolution based on the requested quality.
        Dimension requested = null;
        
        for (int[] map : VIDEO_RESOLUTION)
        {
            if(map[0] == quality) { requested = new Dimension(map[1], map[2]); }
        }

        // Make sure we managed to set a resolution based on the requested quality
        if(requested == null) { return null; }
        
        // Check resolution is available, if not return default values for given quality
        if(element.getVideoHeight() == null || element.getVideoWidth() == null) { return requested; }
        
        // If the original resolution is less than the requested, return original resolution.
        Dimension original = new Dimension(element.getVideoWidth(), element.getVideoHeight());
        
        if (original.width < requested.width || original.height < requested.height)
        {
            return new Dimension(even(original.width), even(original.height));
        }

        // Calculate the aspect ratio of the original video.
        double aspectRatio = new Integer(original.width).doubleValue() / new Integer(original.height).doubleValue();
        requested.height = (int) Math.round(requested.width / aspectRatio);

        return new Dimension(even(requested.width), even(requested.height));
    }
    
    /*
     * Make sure width and height are multiples of two for the transcoder.
     */
    private int even(int size)
    {
        return size + (size % 2);
    }
    
    /*
     * Returns the maximum quality setting applicable for the source.
     */
    public Integer getVideoSourceQuality(MediaElement element)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return null; }
        
        // Check resolution is available
        if(element.getVideoHeight() == null || element.getVideoWidth() == null) { return null; }
        
        // Get source resolution
        Dimension resolution = new Dimension(element.getVideoWidth(), element.getVideoHeight());
        
        for (int[] map : VIDEO_RESOLUTION)
        {
            if (resolution.width <= map[1] || resolution.height <= map[2]) { return map[0]; }
        }
        
        return null;
    }
    
    public static Integer getVideoBitrate(int quality)
    {
        for (int[] map : VIDEO_BITRATE)
        {
            if (map[0] == quality) { return map[1]; }
        }
        
        return null;
    }
    
    public static int validateVideoQuality(int quality, int defaultQuality)
    {
        for (int[] map : VIDEO_RESOLUTION)
        {
            if (map[0] == quality) { return quality; }
        }
        
        return defaultQuality;
    }
    
    /*
     * Returns a list of commands for an audio stream.
     */
    public Collection<String> getAudioCommands(MediaElement element, TranscodeProfile profile)
    {
        // Check this is a media element
        if(element.getType().equals(MediaElementType.DIRECTORY)) { return null; }
        
        List<AudioStream> audioStreams;
        int channelCount;
        int sampleRate;
        
         
        // Check if this is a video element
        if(element.getType().equals(MediaElementType.VIDEO))
        {
            // Check we know which stream to process
            if(profile.getAudioTrack() == null) { return null; }
            
            // Get list of audio streams for video file
            audioStreams = element.getAudioStreams();

            if(audioStreams == null) { return null; }

            // Check that the requested stream number exists
            if(audioStreams.size() < profile.getAudioTrack()) { return null; }
            
            // Store details locally
            channelCount = getAudioChannelCount(audioStreams.get(profile.getAudioTrack()).getConfiguration());
            sampleRate = audioStreams.get(profile.getAudioTrack()).getSampleRate();
        }
        else
        {
            channelCount = getAudioChannelCount(element.getAudioConfiguration());
            sampleRate = Integer.valueOf(element.getAudioSampleRate());
        }
        
        Collection<String> commands = new LinkedList<>();
        
        // Codec
        commands.add("-c:a");
        
        for (String[] map : AUDIO_CODEC)
        {
            if (map[0].equalsIgnoreCase(profile.getAudioCodec())) { commands.add(map[1]); }
        }
        
        // Bitrate
        if(!isLosslessAudioCodec(profile.getAudioCodec()) && profile.getAudioBitrate() != null)
        {
            commands.add("-b:a");
            commands.add(profile.getAudioBitrate() + "k");
        }
        
        // Channel Count        
        if(channelCount > profile.getMaxChannelCount())
        {
            commands.add("-ac");
            commands.add(String.valueOf(profile.getMaxChannelCount()));
            
            // Downmix Levels
            if(profile.getMaxChannelCount() <= 2) {
                commands.add("-clev");
                commands.add("3dB");
                commands.add("-slev");
                commands.add("-3dB");
            }
        }
        
        // Sample Rate
        if(sampleRate > profile.getMaxSampleRate())
        {
            commands.add("-ar");
            commands.add(String.valueOf(profile.getMaxSampleRate()));
        }
        
        return commands;
    }
    
    public Integer getAudioStreamChannelCount(int streamNum, MediaElement element)
    {
        // Check the audio stream is available
        if(!isAudioStreamAvailable(streamNum, element)) { return null; }
        
        // Get list of audio streams for video file
        List<AudioStream> audioStreams = element.getAudioStreams();
        
        return getAudioChannelCount(audioStreams.get(streamNum).getConfiguration());
    }
    
    public static int validateAudioQuality(int quality, int defaultQuality)
    {
        if(quality >= AudioQuality.LOW && quality <= AudioQuality.LOSSLESS) { return quality; }
        else { return defaultQuality; }
    }
    
    public static String validateAudioCodec(String originalCodec)
    {
        for (String codec : SUPPORTED_AUDIO_CODECS.split(","))
        {
            if(originalCodec.contains(codec)) { return codec; }
        }
        
        return null;
    }
    
    public boolean isAudioStreamAvailable(int streamNum, MediaElement element)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return false; }
        
        // Get list of audio streams for video file
        List<AudioStream> audioStreams = element.getAudioStreams();
        if(audioStreams == null) { return false; }
        
        return audioStreams.size() >= streamNum;
    }
    
    /*
     * Return number of channels present.
     */
    public static int getAudioChannelCount(String audioConfiguration)
    {
        for (String[] map : CHANNEL_CONFIGURATION)
        {
            if (map[0].equalsIgnoreCase(audioConfiguration)) { return Integer.valueOf(map[1]); }
        }
        
        // Default to stereo
        return 2;
    }
    
    public static boolean isLosslessAudioCodec(String testCodec)
    {
        for (String codec : LOSSLESS_AUDIO_CODECS.split(","))
        {
            if(testCodec.contains(codec)) { return true; }
        }
        
        return false;
    }
    
    /*
     * Returns a list of commands for burning text and picture based subtitles into the output video.
     */
    public Collection<String> getSubtitleCommands(int streamNum, MediaElement element, int offset)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return null; }
        
        // Get list of subtitle streams for video file
        List<SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) { return null; }
        
        // Check that the requested stream number exists
        if(subtitles.size() < streamNum) { return null; }
        
        Collection<String> commands = new LinkedList<>();
                
        switch(subtitles.get(streamNum).getFormat())
        {
            // Text Based
            case "subrip": case "srt": case "webvtt":
                commands.add("-map");
                commands.add("0:v");
                commands.add("-vf");
                commands.add("setpts=PTS+" + offset + "/TB,subtitles=" + element.getPath() + ":si=" + streamNum + ",setpts=PTS-STARTPTS");
                break;
            
            // Picture Based
            case "dvd_subtitle": case "dvb_subtitle": case "hdmv_pgs_subtitle":
                commands.add("-filter_complex");
                commands.add("[0:v][0:s:" + streamNum + "]overlay[v]");
                commands.add("-map");
                commands.add("[v]");
                break;
                
            default:
                return null;
        }
        
        return commands;
    }
    
    public Integer getForcedSubtitleIndex(MediaElement element)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return null; }
        
        // Get list of subtitle streams for video file
        List<SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) { return null; }
        
        // Scan subtitles for forced streams
        for(SubtitleStream subtitle : subtitles)
        {
            if(subtitle.isForced()) { return subtitle.getStream(); }
        }
        
        return null;
    }
    
    public boolean isSubtitleStreamAvailable(int streamNum, MediaElement element)
    {
        // Check this is a video element
        if(element.getType() != MediaElementType.VIDEO) { return false; }
        
        // Get list of subtitle streams for video file
        List<SubtitleStream> subtitles = element.getSubtitleStreams();
        
        if(subtitles == null) { return false; }
        
        return subtitles.size() >= streamNum;
    }
    
    public static boolean isSupportedSubtitleCodec(String testCodec)
    {
        for (String codec : SUPPORTED_SUBTITLE_CODECS.split(","))
        {
            if(testCodec.contains(codec)) { return true; }
        }
        
        return false;
    }
    
    /**
     * Returns the proper MIME type for the given file format.
     *
     * @param format The file format requested.
     * @return The corresponding MIME type. If no MIME type
     * is found, we assume that this file format is not supported.
     */
    public static String getVideoMimeType(String format) {
        for (String[] map : VIDEO_MIME_TYPES)
        {
            if (map[0].equalsIgnoreCase(format)) { return map[1]; }
        }
        
        return null;
    }
    
    /**
     * Returns the proper MIME type for the given file format.
     *
     * @param format The file format requested.
     * @return The corresponding MIME type. If no MIME type
     * is found, we assume that this file format is not supported.
     */
    public static String getAudioMimeType(String format) {
        for (String[] map : AUDIO_MIME_TYPES)
        {
            if (map[0].equalsIgnoreCase(format)) { return map[1]; }
        }
        
        return null;
    }
    
    /**
     * Returns the proper MIME type for the given file format.
     *
     * @param format The file format requested.
     * @return The corresponding MIME type. If no MIME type
     * is found, we assume that this file format is not supported.
     */
    public static String getSubtitleMimeType(String format) {
        for (String[] map : SUBTITLE_MIME_TYPES)
        {
            if (map[0].equalsIgnoreCase(format)) { return map[1]; }
        }
        
        return null;
    }
    
    /**
     * Returns the format to use for a given codec.
     *
     * @param codec The codec.
     * @return A suitable format.
     */
    public static String getVideoFormatFromCodec(String codec) {
        for (String[] map : VIDEO_FORMATS)
        {
            if (codec.contains(map[0])) { return map[1]; }
        }
        
        return null;
    }
    
    public static String getAudioFormatFromCodec(String codec) {
        for (String[] map : AUDIO_FORMATS)
        {
            if (codec.contains(map[0])) { return map[1]; }
        }
        
        return null;
    }
    
    private static boolean isSupportedVideoCodec(String testCodec)
    {
        for (String codec : SUPPORTED_VIDEO_CODECS.split(","))
        {
            if(testCodec.contains(codec)) { return true; }
        }
        
        return false;
    }
    
    private static boolean isSupportedAudioCodec(String testCodec)
    {
        for (String codec : SUPPORTED_AUDIO_CODECS.split(","))
        {
            if(testCodec.contains(codec)) { return true; }
        }
        
        return false;
    }
    
    public static Integer getAudioQualityForVideo(int videoQuality)
    {
        for (int[] map : VIDEO_QUALITY_AUDIO_MAPPING)
        {
            if (videoQuality == map[0]) { return map[1]; }
        }
        
        return null;
    }
    
    public static Integer getAudioBitrateForCodec(String codec, int quality)
    {
        if(quality >= AudioQuality.LOW && quality <= AudioQuality.LOSSLESS)
        {
            for (String[] map : AUDIO_BITRATES)
            {
                if (codec.contains(map[0])) { return Integer.valueOf(map[quality + 1]); }
            }
        }
        
        return null;
    }
    
    public static Integer getMultiChannelAudioBitrateForCodec(String codec, int quality)
    {
        if(quality >= AudioQuality.LOW && quality <= AudioQuality.LOSSLESS)
        {
            for (String[] map : MULTI_CHANNEL_AUDIO_BITRATES)
            {
                if (codec.contains(map[0])) { return Integer.valueOf(map[quality + 1]); }
            }
        }
        
        return null;
    }
    
    public static Integer getMaxSampleRateForCodec(String codec)
    {
        for (String[] map : MAX_SAMPLE_RATES)
        {
            if (codec.contains(map[0])) { return Integer.valueOf(map[1]); }
        }
        
        return null;
    }
    
    public static Integer getMaxChannelsForCodec(String codec)
    {
        for (String[] map : MAX_CHANNELS)
        {
            if (codec.contains(map[0])) { return Integer.valueOf(map[1]); }
        }
        
        return null;
    }
    
    //
    // Helper Functions For Child Transcode Services
    //
    public static boolean isSupported(String list, String test)
    {
        for (String item : list.split(","))
        {
            if(test.contains(item)) { return true; }
        }
        
        return false;
    }
    
    public static String getDefault(String list)
    {
        for (String item : list.split(",")) { return item; }
        
        return null;
    }
    
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static class TranscodeProfile {
        
        private String videoCodec, audioCodec, mimeType;
        private Integer videoQuality, audioQuality, audioBitrate, audioTrack, subtitleTrack, maxSampleRate, maxChannelCount;
        private Integer offset = 0;
        private Boolean multiChannel = false;
        private Boolean videoTranscodeRequired = false;
        private Boolean audioTranscodeRequired = false;
        
        public String getVideoCodec()
        {
            return videoCodec;
        }
        
        public void setVideoCodec(String videoCodec)
        {
            this.videoCodec = videoCodec;
        }
        
        public String getAudioCodec()
        {
            return audioCodec;
        }
        
        public void setAudioCodec(String audioCodec)
        {
            this.audioCodec = audioCodec;
        }
        
        public String getMimeType()
        {
            return mimeType;
        }
        
        public void setMimeType(String mimeType)
        {
            this.mimeType = mimeType;
        }
        
        @JsonIgnore
        public Integer getVideoQuality()
        {
            return videoQuality;
        }
        
        public void setVideoQuality(int videoQuality)
        {
            this.videoQuality = videoQuality;
        }
        
        @JsonIgnore
        public Integer getAudioQuality()
        {
            return audioQuality;
        }
        
        public void setAudioQuality(int audioQuality)
        {
            this.audioQuality = audioQuality;
        }
        
        public Integer getAudioBitrate()
        {
            return audioBitrate;
        }
        
        public void setAudioBitrate(int audioBitrate)
        {
            this.audioBitrate = audioBitrate;
        }
        
        @JsonIgnore
        public Integer getAudioTrack()
        {
            return audioTrack;
        }
        
        public void setAudioTrack(Integer audioTrack)
        {
            this.audioTrack = audioTrack;
        }
        
        @JsonIgnore
        public Integer getSubtitleTrack()
        {
            return subtitleTrack;
        }
        
        public void setSubtitleTrack(Integer subtitleTrack)
        {
            this.subtitleTrack = subtitleTrack;
        }
        
        @JsonIgnore
        public Integer getMaxSampleRate()
        {
            return maxSampleRate;
        }
        
        public void setMaxSampleRate(int maxSampleRate)
        {
            this.maxSampleRate = maxSampleRate;
        }
        
        public Integer getMaxChannelCount()
        {
            return maxChannelCount;
        }
        
        public void setMaxChannelCount(int maxChannelCount)
        {
            this.maxChannelCount = maxChannelCount;
        }
        
        @JsonIgnore
        public Integer getOffset()
        {
            return offset;
        }
        
        public void setOffset(int offset)
        {
            this.offset = offset;
        }
        
        @JsonIgnore
        public Boolean isMultiChannelEnabled()
        {
            return multiChannel;
        }
        
        public void setMultiChannelEnabled(boolean multiChannel)
        {
            this.multiChannel = multiChannel;
        }
        
        @JsonIgnore
        public Boolean isVideoTranscodeRequired()
        {
            return videoTranscodeRequired;
        }
        
        public void setVideoTranscodeRequired(boolean transcodeRequired)
        {
            this.videoTranscodeRequired = transcodeRequired;
        }
        
        @JsonIgnore
        public Boolean isAudioTranscodeRequired()
        {
            return audioTranscodeRequired;
        }
        
        public void setAudioTranscodeRequired(boolean transcodeRequired)
        {
            this.audioTranscodeRequired = transcodeRequired;
        }
    }
    
    public static class AudioQuality {
        public static final int LOW = 0;
        public static final int MEDIUM = 1;
        public static final int HIGH = 2;
        public static final int LOSSLESS = 3;
    }
}
