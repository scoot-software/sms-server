package com.scooter1556.sms.server;

public class SMS {
    public static class Status {
        public static final int ERROR = -1;
        public static final int OK = 0;
        public static final int NOT_ALLOWED = 1;
        public static final int NOT_REQUIRED = 2;
        public static final int REQUIRED_DATA_MISSING = 3;
    }
    
    public static class Codec {
        public static final int UNSUPPORTED = -1;
        
        public static final int COPY = 0;
        
        public static final int AVC_BASELINE = 10;
        public static final int AVC_MAIN = 11;
        public static final int AVC_HIGH = 12;
        public static final int AVC_HIGH10 = 13;
        
        public static final int MPEG2 = 20;
        
        public static final int HEVC_MAIN = 30;
        public static final int HEVC_MAIN10 = 31;
        public static final int HEVC_HDR10 = 32;
        
        public static final int VC1 = 40;
        
        public static final int AAC = 1000;
        public static final int AC3 = 1001;
        public static final int EAC3 = 1002;
        public static final int DTS = 1003;
        public static final int DTSHD = 1004;
        public static final int PCM = 1005;
        public static final int TRUEHD = 1006;
        public static final int MP3 = 1007;
        public static final int DSD = 1008;
        public static final int FLAC = 1009;
        public static final int ALAC = 1010;
        public static final int VORBIS = 1011;
        
        public static final int SUBRIP = 2000;
        public static final int WEBVTT = 2001;
        public static final int PGS = 2002;
        public static final int DVB = 2003;
        public static final int DVD = 2004;
    }
    
    public static class Format {
        public static final int UNSUPPORTED = -1;
        public static final int NONE = 0;
        public static final int AAC = 1;
        public static final int AC3 = 2;
        public static final int AVI = 3;
        public static final int DSF = 4;
        public static final int DTS = 5;
        public static final int FLAC= 6;
        public static final int H264 = 7;
        public static final int H265 = 8;
        public static final int HLS = 9;
        public static final int MATROSKA = 10;
        public static final int MP3 = 11;
        public static final int MP4 = 12;
        public static final int MPEG = 13;
        public static final int MPEGTS = 14;
        public static final int OGG = 15;
        public static final int SUBRIP = 16;
        public static final int WAV = 17;
        public static final int WEBVTT = 18;
    }
    
    public static class MediaType {
        public static final int AUDIO = 0;
        public static final int VIDEO = 1;
        public static final int SUBTITLE = 2;
    }
    
    public static class Client {
        public static final int UNSUPPORTED = -1;
        public static final int NONE = 0;
        public static final int ANDROID = 1;
        public static final int ANDROID_TV = 2;
        public static final int CHROMECAST = 3;
        public static final int KODI = 4;
    }
    
    public static class ReplaygainMode {
        public static final int OFF = 0;
        public static final int NATIVE_TRACK = 1;
        public static final int NATIVE_ALBUM = 2;
        public static final int TRACK = 3;
        public static final int ALBUM = 4;
    }
    
    public static class TranscodeReason {
        public static final int UNKNOWN = -1;
        public static final int NONE = 0;
        public static final int CODEC_UNSUPPORTED_BY_CLIENT = 1;
        public static final int CODEC_UNSUPPORTED_BY_ENCODER = 2;
        public static final int BITRATE = 3;
        public static final int RESOLUTION = 4;
        public static final int SUBTITLES = 5;
        public static final int MISSING_DATA = 6;
    }
}
