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
package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

public class MediaUtils {
    
    public static final String[] SUPPORTED_FILE_EXTENSIONS = {"avi","dsf","flac","m4a","m4v","mka","mkv","mp3","mp4","mpeg","mpg","oga","ogg","wav"};
    
    public static int getSMSCodec(String codec, String profile, String pixelFormat, String cTransfer, String cPrimaries) {
        // Check codec
        if(codec == null) {
            return 0;
        }
        
        // Make sure we have no null parameters
        if(profile == null) profile = "";
        if(pixelFormat == null) pixelFormat = "";
        if(cTransfer == null) cTransfer = "";
        if(cPrimaries == null) cPrimaries = "";
        
        switch(codec) {
            //
            // Video
            //
            case "h264":
                if(!pixelFormat.contains("yuv420p")) {
                    return SMS.Codec.UNSUPPORTED;
                }
                
                switch(profile) {
                    case "Constrained Baseline":
                        return SMS.Codec.AVC_BASELINE;
                        
                    case "Main":
                        return SMS.Codec.AVC_MAIN;
                        
                    case "High":
                        return SMS.Codec.AVC_HIGH;
                        
                    case "High 10":
                        return SMS.Codec.AVC_HIGH10;
                        
                    default:
                        return SMS.Codec.UNSUPPORTED;
                        
                        
                }
                
            case "mpeg2video":
                if(!pixelFormat.contains("yuv420p")) {
                    return SMS.Codec.UNSUPPORTED;
                }
                
                return SMS.Codec.MPEG2;
                
            case "hevc":
                if(!pixelFormat.contains("yuv420p")) {
                    return SMS.Codec.UNSUPPORTED;
                }
                
                switch(profile) {
                    case "Main":
                        return SMS.Codec.HEVC_MAIN;
                        
                    case "Main 10":
                        if(cTransfer.equals("smpte2084") || cPrimaries.equals("bt2020")) {
                            return SMS.Codec.HEVC_HDR10;
                        } else {
                            return SMS.Codec.HEVC_MAIN10;
                        }
                        
                    default:
                        return SMS.Codec.UNSUPPORTED;
                }
                
            case "vc1":
                if(!pixelFormat.contains("yuv420p")) {
                    return SMS.Codec.UNSUPPORTED;
                }
                
                return SMS.Codec.VC1;
               
            //
            // Audio
            //
            case "aac":
                return SMS.Codec.AAC;
                
            case "mp3":
                return SMS.Codec.MP3;
                
            case "ac3":
                return SMS.Codec.AC3;
                
            case "eac3":
                return SMS.Codec.EAC3;
                
            case "dts":
                switch(profile) {
                    case "DTS-HD MA":
                        return SMS.Codec.DTSHD;
                        
                    default:
                        return SMS.Codec.DTS;
                }
                
            case "pcm_s16be":
            case "pcm_s16le":
            case "pcm_s24be":
            case "pcm_s24le":
            case "pcm_u16be":
            case "pcm_u16le":
            case "pcm_u24be":
            case "pcm_u24le":
                return SMS.Codec.PCM;
                
            case "truehd":
                return SMS.Codec.TRUEHD;
               
            case "dsd_lsbf":
            case "dsd_lsbf_planar":
            case "dsd_msbf":
            case "dsd_msbf_planar":
                return SMS.Codec.DSD;
                
                
            case "flac":
                return SMS.Codec.FLAC;
                
            case "alac":
                return SMS.Codec.ALAC;
                
            case "vorbis":
                return SMS.Codec.VORBIS;
                
            //
            // Subtitles
            //
            case "subrip":
                return SMS.Codec.SUBRIP;
                
            case "webvtt":
                return SMS.Codec.WEBVTT;
                
            case "dvb_subtitle":
                return SMS.Codec.DVB;
                
            case "dvd_subtitle":
                return SMS.Codec.DVD;
                 
            case "hdmv_pgs_subtitle":
                return SMS.Codec.PGS;
                
            default:
                return 0;
        }
    }
    
    public static int getSMSContainer(String extension) {
        // Check extension
        if(extension == null) {
            return SMS.Format.NONE;
        }
        
        switch(extension) {
            case "avi":
                return SMS.Format.AVI;
                
            case "dsf":
                return SMS.Format.DSF;
                
            case "flac":
                return SMS.Format.FLAC;
                
            case "mp3":
                return SMS.Format.MP3;
                
            case "m4v": case "m4a": case "mp4":
                return SMS.Format.MP4;
                
            case "mpg": case "mpeg":
                return SMS.Format.MPEG;
                
            case "ts":
                return SMS.Format.MPEGTS;
                
            case "mkv": case "mka":
                return SMS.Format.MATROSKA;
                
            case "ogg": case "oga":
                return SMS.Format.OGG;
                
            case "wav":
                return SMS.Format.WAV;
                
            default:
                return SMS.Format.UNSUPPORTED;
        }
    }
    
    public static int getFormatForCodec(int codec) {
        switch(codec) {
            case SMS.Codec.AAC:
                return SMS.Format.AAC;
                
            case SMS.Codec.AC3: case SMS.Codec.EAC3:
                return SMS.Format.AC3;
                
            case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH: case SMS.Codec.AVC_HIGH10:
                return SMS.Format.H264;
                
            case SMS.Codec.DTS: case SMS.Codec.DTSHD:
                return SMS.Format.DTS;
                
            case SMS.Codec.FLAC:
                return SMS.Format.FLAC;
                
            case SMS.Codec.HEVC_MAIN: case SMS.Codec.HEVC_MAIN10: case SMS.Codec.HEVC_HDR10:
                return SMS.Format.H265;
                
            case SMS.Codec.MP3:
                return SMS.Format.MP3;
                
            case SMS.Codec.MPEG2:
                return SMS.Format.MPEG;
                
            case SMS.Codec.PCM:
                return SMS.Format.WAV;
                
            case SMS.Codec.SUBRIP:
                return SMS.Format.SUBRIP;
                
            case SMS.Codec.VORBIS:
                return SMS.Format.OGG;
                
            case SMS.Codec.WEBVTT:
                return SMS.Format.WEBVTT;
                
            default:
                return SMS.Format.UNSUPPORTED;
        }
    }
    
    public static String getFormat(int format) {
        switch(format) {
            case SMS.Format.AVI:
                return "avi";
                
            case SMS.Format.FLAC:
                return "flac";
                
            case SMS.Format.MATROSKA:
                return "matroska";
                
            case SMS.Format.MP3:
                return "mp3";
                
            case SMS.Format.MP4:
                return "mp4";
                
            case SMS.Format.MPEGTS:
                return "mpegts";
                
            case SMS.Format.OGG:
                return "ogg";
                
            case SMS.Format.WAV:
                return "wav";
                
            case SMS.Format.AAC:
                return "adts";
                
            case SMS.Format.AC3:
                return "ac3";
                
            case SMS.Format.SUBRIP:
                return "srt";
                
            case SMS.Format.WEBVTT:
                return "webvtt";
                
            default:
                return null;
        }
    }
    
    public static String getMimeType(int type, int format) {        
        StringBuilder mimeType = new StringBuilder();
        
        // Handle special cases
        switch(format) {
            case SMS.Format.HLS:
                mimeType.append("application/x-mpegurl");
                return mimeType.toString();
        }
        
        switch(type) {
            case SMS.MediaType.AUDIO:
                mimeType.append("audio/");
                break;
                
            case SMS.MediaType.VIDEO:
                mimeType.append("video/");
                break;
                
            case SMS.MediaType.SUBTITLE:
                mimeType.append("text/");
                break;
                
            default:
                return null;
        }
     
        switch(format) {
            case SMS.Format.AVI:
                mimeType.append("avi");
                break;
                
            case SMS.Format.DSF:
                mimeType.append("dsf");
                break;
                
            case SMS.Format.FLAC:
                mimeType.append("flac");
                break;
                
            case SMS.Format.MATROSKA:
                mimeType.append("x-matroska");
                break;
                
            case SMS.Format.MP3: case SMS.Format.MPEG:
                mimeType.append("mpeg");
                break;
                
            case SMS.Format.MP4:
                mimeType.append("mp4");
                break;
                
            case SMS.Format.MPEGTS:
                mimeType.append("MP2T");
                break;
                
            case SMS.Format.OGG:
                mimeType.append("ogg");
                break;
                
            case SMS.Format.WAV:
                mimeType.append("wav");
                break;
                
            case SMS.Format.AAC:
                mimeType.append("aac");
                
            case SMS.Format.AC3:
                mimeType.append("ac3");
                                
            default:
                return null;
        }
        
        return mimeType.toString();
    }
    
    public static boolean isCodecSupportedByFormat(int format, int codec) {
        switch(format) {
            case SMS.Format.MPEGTS:
                switch(codec) {
                        case SMS.Codec.AVC_BASELINE:
                        case SMS.Codec.AVC_HIGH:
                        case SMS.Codec.AVC_HIGH10:
                        case SMS.Codec.AVC_MAIN:
                        case SMS.Codec.MPEG2:
                        case SMS.Codec.AAC:
                        case SMS.Codec.AC3:
                        case SMS.Codec.EAC3:
                        case SMS.Codec.TRUEHD:
                        case SMS.Codec.DTS:
                        case SMS.Codec.DTSHD:
                        case SMS.Codec.MP3:
                        case SMS.Codec.DVB:
                            return true;
                            
                        default:
                            return false;
                }
                                
            default:
                return false;
        }
    }
    
    public static boolean isLossless(int codec) {
        switch(codec) {
            case SMS.Codec.ALAC:
            case SMS.Codec.DSD:
            case SMS.Codec.DTSHD:
            case SMS.Codec.FLAC:
            case SMS.Codec.PCM:
            case SMS.Codec.TRUEHD:
                return true;
                
            default:
                return false;
        }
    }
    
    public static boolean isMediaFile(Path path) {
        return FilenameUtils.isExtension(path.getFileName().toString().toLowerCase(), SUPPORTED_FILE_EXTENSIONS);
    }
    
    // Determines if a directory contains media
    public static boolean containsMedia(File directory, boolean includeDir) {
        for (File file : directory.listFiles()) {
            if(!file.isHidden()) {
                if(includeDir && file.isDirectory()) {
                    return true;
                }

                if(isMediaFile(file.toPath())) {
                    return true;
                }
            }                    
        }

        return false;
    }
    
    // Returns the first video stream with the requested ID
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
    
    // Returns the first audio stream with the requested ID
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
    
    // Returns the first subtitle stream with the requested ID
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
    
    // Returns a max bitrate for a given media element
    public static int getAverageBitrate(VideoStream stream, int defaultValue) {
        // Return stream bitrate if it is available
        if(stream.getBitrate() != null && stream.getBitrate() > 0) {
            return stream.getBitrate();
        }
        
        return defaultValue;
    }
    
    public static int getStreamCount(MediaElement mediaElement) {
        int count = 0;
        
        if(mediaElement != null) {
            if(mediaElement.getVideoStreams() != null) {
                count += mediaElement.getVideoStreams().size();
            }
            
            if(mediaElement.getAudioStreams() != null) {
                count += mediaElement.getAudioStreams().size();
            }
        }
        
        return count;
    }
}
