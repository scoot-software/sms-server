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
package com.scooter1556.sms.server.service.parser;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MetadataParser {
    
    private static final String CLASS_NAME = "MetadataParser";
        
    public MediaElement parse(MediaElement mediaElement) {
        // Use parser to parse file metadata
        Path parser = ParserUtils.getParser();

        // Check transcoder exists
        if(parser == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Parser is not available but is required to parse metadata.", null);
            return mediaElement;
        }

        try {
            String[] command = new String[]{parser.toString(), "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams", mediaElement.getPath()};
            String[] metadata = ParserUtils.getProcessOutput(command);
            
            // Parse JSON
            JsonValue json = Json.parse(StringUtils.arrayToDelimitedString(metadata, ""));
            JsonArray streams = json.asObject().get("streams").asArray();
            JsonValue format = json.asObject().get("format").asObject();
            
            // Initialise stream lists
            List<VideoStream> videoStreams = new ArrayList<>();
            List<AudioStream> audioStreams = new ArrayList<>();
            List<SubtitleStream> subtitleStreams = new ArrayList<>();
            
            // Process Streams
            if(streams != null && mediaElement.getID() != null) {
                for(JsonValue stream : streams) {     
                    JsonValue disposition = stream.asObject().get("disposition");
                    JsonValue tags = stream.asObject().get("tags");
                    String type = stream.asObject().getString("codec_type", "Unknown");
                    String codec;
                    String bitrate;
                    String bps;
                    
                    switch(type) {
                        case "video":
                            VideoStream vStream = new VideoStream();
                            
                            // Check codec
                            codec = stream.asObject().getString("codec_name", "Unknown");
                            
                            if(!TranscodeUtils.isSupported(TranscodeUtils.SUPPORTED_VIDEO_CODECS, codec)) {
                                continue;
                            } else {
                                vStream.setCodec(codec);
                            }
                            
                            // Media Element ID
                            vStream.setMediaElementId(mediaElement.getID());
                            
                            // Stream ID
                            vStream.setStreamId(stream.asObject().getInt("index", 0));
                            
                            // Profile
                            vStream.setProfile(stream.asObject().getString("profile", ""));
                            
                            // Resolution
                            vStream.setWidth(stream.asObject().getInt("width", 0));
                            vStream.setHeight(stream.asObject().getInt("height", 0));
                            
                            // Format
                            vStream.setPixelFormat(stream.asObject().getString("pix_fmt", ""));
                            vStream.setColorSpace(stream.asObject().getString("color_space", ""));
                            vStream.setColorTransfer(stream.asObject().getString("color_transfer", ""));
                            vStream.setColorPrimaries(stream.asObject().getString("color_primaries", ""));
                            
                            // Interlaced
                            String fieldOrder = stream.asObject().getString("field_order", "");
                            vStream.setInterlaced(!fieldOrder.contains("progressive"));
                            
                            // Bit Rate
                            bitrate = stream.asObject().getString("bit_rate", "0");
                            vStream.setBitrate(Integer.parseInt(bitrate));
                            String maxBitrate = stream.asObject().getString("max_bit_rate", "0");
                            vStream.setMaxBitrate(Integer.parseInt(maxBitrate));
                            
                            // Frame Rate
                            String strFps = stream.asObject().getString("r_frame_rate", "");
                            
                            // Process Frame Rate
                            if(!strFps.isEmpty()) {
                                String[] splitFps = strFps.split("/");
                                                                
                                if(splitFps.length == 2) {
                                    double a = Double.parseDouble(splitFps[0]);
                                    double b = Double.parseDouble(splitFps[1]);
                                                                        
                                    if(a > 0 && b > 0) {
                                        vStream.setFPS(a / b);
                                    }
                                }
                            }
                            
                            // Bits Per Sample (Default to 8)
                            bps = stream.asObject().getString("bits_per_raw_sample", "8");
                            vStream.setBPS(Integer.parseInt(bps));
                            
                            // Tags
                            if(tags != null && tags.isObject()) {
                                // Title
                                vStream.setTitle(tags.asObject().getString("title", ""));
                            
                                // Language
                                vStream.setLanguage(tags.asObject().getString("language", "und"));
                            }
                            
                            // Flags
                            if(disposition != null && disposition.isObject()) {
                                int d = disposition.asObject().getInt("default", 0);
                                vStream.setDefault((d > 0));
                                
                                int f = disposition.asObject().getInt("forced", 0);
                                vStream.setForced(f > 0);
                            }
                            
                            videoStreams.add(vStream);
                            break;
                            
                        case "audio":
                            AudioStream aStream = new AudioStream();
                            
                            // Check codec
                            codec = stream.asObject().getString("codec_name", "Unknown");
                            
                            if(!TranscodeUtils.isSupported(TranscodeUtils.SUPPORTED_AUDIO_CODECS, codec)) {
                                continue;
                            } else {
                                aStream.setCodec(codec);
                            }
                            
                            // Media Element ID
                            aStream.setMediaElementId(mediaElement.getID());
                            
                            // Stream ID
                            aStream.setStreamId(stream.asObject().getInt("index", 0));
                            
                            // Sample Rate
                            String sampleRate = stream.asObject().getString("sample_rate", "0");
                            aStream.setSampleRate(Integer.parseInt(sampleRate));
                            
                            // Channels
                            aStream.setChannels(stream.asObject().getInt("channels", 0));
                            
                            // Bit Rate
                            bitrate = format.asObject().getString("bit_rate", "0");
                            aStream.setBitrate(Integer.parseInt(bitrate));
                            
                            // Bits Per Sample
                            bps = stream.asObject().getString("bits_per_raw_sample", "0");
                            aStream.setBPS(Integer.parseInt(bps));
                            
                            // Tags
                            if(tags != null && tags.isObject()) {
                                // Title
                                aStream.setTitle(tags.asObject().getString("title", ""));
                            
                                // Language
                                aStream.setLanguage(tags.asObject().getString("language", "und"));
                            }
                            
                            // Flags
                            if(disposition != null && disposition.isObject()) {
                                int d = disposition.asObject().getInt("default", 0);
                                aStream.setDefault((d > 0));
                                
                                int f = disposition.asObject().getInt("forced", 0);
                                aStream.setForced(f > 0);
                            }
                            
                            audioStreams.add(aStream);
                            break;
                            
                        case "subtitle":
                            SubtitleStream sStream = new SubtitleStream();
                            
                            // Check codec
                            codec = stream.asObject().getString("codec_name", "Unknown");
                            
                            if(!TranscodeUtils.isSupported(TranscodeUtils.SUPPORTED_SUBTITLE_CODECS, codec)) {
                                continue;
                            } else {
                                sStream.setCodec(codec);
                            }
                            
                            // Media Element ID
                            sStream.setMediaElementId(mediaElement.getID());
                            
                            // Stream ID
                            sStream.setStreamId(stream.asObject().getInt("index", 0));
                            
                            // Tags
                            if(tags != null && tags.isObject()) {
                                // Title
                                sStream.setTitle(tags.asObject().getString("title", ""));
                            
                                // Language
                                sStream.setLanguage(tags.asObject().getString("language", "und"));
                            }
                            
                            // Flags
                            if(disposition != null && disposition.isObject()) {
                                int d = disposition.asObject().getInt("default", 0);
                                sStream.setDefault((d > 0));
                                
                                int f = disposition.asObject().getInt("forced", 0);
                                sStream.setForced(f > 0);
                            }
                            
                            subtitleStreams.add(sStream);
                            break;
                    }
                }
            }
            
            // Set streams
            mediaElement.setVideoStreams(videoStreams);
            mediaElement.setAudioStreams(audioStreams);
            mediaElement.setSubtitleStreams(subtitleStreams);
            
            // Determine media type
            if(videoStreams.size() > 0) {
                mediaElement.setType(MediaElementType.VIDEO);
            } else if(audioStreams.size() > 0) {
                mediaElement.setType(MediaElementType.AUDIO);
            }
            
            // Process Format
            if(format != null) {
                JsonValue tags = format.asObject().get("tags");
                
                // Duration
                String duration = format.asObject().getString("duration", "0");
                mediaElement.setDuration(Double.parseDouble(duration));
                
                // Bit Rate
                String bitrate = format.asObject().getString("bit_rate", "0");
                mediaElement.setBitrate(Integer.parseInt(bitrate));
                
                // Read tags for audio files
                if(tags != null && mediaElement.getType() == MediaElementType.AUDIO) {
                    // Title
                    if(tags.asObject().get("title") != null) {
                        mediaElement.setTitle(tags.asObject().getString("title", mediaElement.getTitle()));
                    } else if(tags.asObject().get("TITLE") != null) {
                        mediaElement.setTitle(tags.asObject().getString("TITLE", mediaElement.getTitle()));
                    }
                    
                    // Artist
                    if(tags.asObject().get("artist") != null) {
                        mediaElement.setArtist(tags.asObject().getString("artist", ""));
                    } else if(tags.asObject().get("ARTIST") != null) {
                        mediaElement.setArtist(tags.asObject().getString("ARTIST", ""));
                    } else if(tags.asObject().get("band") != null) {
                        mediaElement.setArtist(tags.asObject().getString("band", ""));
                    } else if(tags.asObject().get("BAND") != null) {
                        mediaElement.setArtist(tags.asObject().getString("BAND", ""));
                    }
                    
                    // Album Artist
                    if(tags.asObject().get("album_artist") != null) {
                        mediaElement.setAlbumArtist(tags.asObject().getString("album_artist", ""));
                    } else if(tags.asObject().get("ALBUM_ARTIST") != null) {
                        mediaElement.setAlbumArtist(tags.asObject().getString("ALBUM_ARTIST", ""));
                    }
                    
                    // Album
                    if(tags.asObject().get("album") != null) {
                        mediaElement.setAlbum(tags.asObject().getString("album", ""));
                    } else if(tags.asObject().get("ALBUM") != null) {
                        mediaElement.setAlbum(tags.asObject().getString("ALBUM", ""));
                    }
                    
                    // Comment
                    if(tags.asObject().get("comment") != null) {
                        mediaElement.setDescription(tags.asObject().getString("comment", ""));
                    } else if(tags.asObject().get("COMMENT") != null) {
                        mediaElement.setDescription(tags.asObject().getString("COMMENT", ""));
                    }
                    
                    // Genre
                    if(tags.asObject().get("genre") != null) {
                        mediaElement.setGenre(tags.asObject().getString("genre", ""));
                    } else if(tags.asObject().get("GENRE") != null) {
                        mediaElement.setGenre(tags.asObject().getString("GENRE", ""));
                    }
                    
                    // Year
                    String date = null;
                    
                    if(tags.asObject().get("date") != null) {
                        date = tags.asObject().getString("date", "0");
                    } else if(tags.asObject().get("DATE") != null) {
                        date = tags.asObject().getString("DATE", "0");
                    } else if(tags.asObject().get("year") != null) {
                        date = tags.asObject().getString("year", "0");
                    } else if(tags.asObject().get("YEAR") != null) {
                        date = tags.asObject().getString("YEAR", "0");
                    } else if(tags.asObject().get("tyer") != null) {
                        date = tags.asObject().getString("tyer", "0");
                    } else if(tags.asObject().get("TYER") != null) {
                        date = tags.asObject().getString("TYER", "0");
                    }
                    
                    if(date != null && date.length() > 4) {
                        String[] split = null;
                      
                        if(date.length() == 4) {
                            split = new String[] {date};
                        } else if(date.contains("-")) {
                            split = date.split("-");
                        } else if(date.contains(".")) {
                            split = date.split(".");
                        } else if(date.contains("/")) {
                            split = date.split("/");
                        }
                        
                        if(split != null) {
                            for(String i : split) {
                                short year = Short.parseShort(i);
                                
                                if(year > 1000) {
                                    mediaElement.setYear(year);
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Disc Number
                    String discNumber = null;
                    
                    if(tags.asObject().get("discnumber") != null) {
                        discNumber = tags.asObject().getString("discnumber", "0");
                    } else if(tags.asObject().get("DISCNUMBER") != null) {
                        discNumber = tags.asObject().getString("DISCNUMBER", "0");
                    } else if(tags.asObject().get("disc") != null) {
                        discNumber = tags.asObject().getString("disc", "0");
                    } else if(tags.asObject().get("DISC") != null) {
                        discNumber = tags.asObject().getString("DISC", "0");
                    }
                    
                    if(discNumber != null) {
                        String[] split = discNumber.split("/");
                        
                        if(split.length > 0) {
                            short val = Short.parseShort(split[0]);
                            
                            if(val > 0) {
                                mediaElement.setDiscNumber(val);
                            }
                        }
                    }
                    
                    // Disc Subtitle
                    if(tags.asObject().get("discsubtitle") != null) {
                        mediaElement.setDiscSubtitle(tags.asObject().getString("discsubtitle", ""));
                    } else if(tags.asObject().get("DISCSUBTITLE") != null) {
                        mediaElement.setDiscSubtitle(tags.asObject().getString("DISCSUBTITLE", ""));
                    }
                    
                    // Track Number
                    String track = null;
                    
                    if(tags.asObject().get("track") != null) {
                        track = tags.asObject().getString("track", "0");
                    } else if(tags.asObject().get("TRACK") != null) {
                        track = tags.asObject().getString("TRACK", "0");
                    }
                    
                    if(track != null) {
                        String[] split = track.split("/");
                        
                        if(split.length > 0) {
                            short val = Short.parseShort(split[0]);
                            
                            if(val > 0) {
                                mediaElement.setTrackNumber(val);
                            }
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException x) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse metadata for file " + mediaElement.getPath(), x);
        }

        return mediaElement;
    }
}
