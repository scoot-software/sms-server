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

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.TranscodeService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetadataParser {
    
    private static final String CLASS_NAME = "MetadataParser";
    
    // Patterns
    private static final Pattern TITLE = Pattern.compile("^title\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIST = Pattern.compile("^(artist|band)\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALBUMARTIST = Pattern.compile("^album(\\s|_)artist\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALBUM = Pattern.compile("^album\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMENT = Pattern.compile("^comment\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENRE = Pattern.compile("^genre\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR = Pattern.compile("^(date|tyer|year)\\s*:\\s+(\\d{4})", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCNUMBER = Pattern.compile("^(disc|discnumber)\\s*:\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISCSUBTITLE = Pattern.compile("^discsubtitle\\s*:\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACK = Pattern.compile("^track\\s*:\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BITRATE = Pattern.compile("bitrate:\\s+(\\d+)\\s+kb/s");
    private static final Pattern DURATION = Pattern.compile("Duration:\\s+(\\d+):(\\d+):(\\d+).(\\d+),");
    private static final Pattern AUDIO_STREAM = Pattern.compile("Stream.*?([(](.+)[)])?:\\s+Audio:\\s*(\\S+).*?,\\s*(\\d+)\\s+Hz,\\s*([\\w\\.]+)"); 
    private static final Pattern VIDEO_STREAM = Pattern.compile("Stream.*?:\\s+Video:\\s*(\\S+).*?,\\s+(\\d+)x(\\d+)");
    private static final Pattern SUBTITLE_STREAM = Pattern.compile("Stream.*?([(](.+)[)])?:\\s+Subtitle:\\s*([^,\\s]+).*?([(]forced[)])?$");
    
    @Autowired
    private TranscodeService transcodeService;
    
    public MediaElement parse(MediaElement mediaElement) {
        // Use transcoder to parse file metadata
        Transcoder parser = transcodeService.getTranscoder();

        // Check transcoder exists
        if(parser == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Transcoder is not available but is required to parse metadata.", null);
            return mediaElement;
        }

        try {
            String[] command = new String[]{parser.getPath().toString(), "-i", mediaElement.getPath()};
            String[] metadata = ParserUtils.getProcessOutput(command);
            
            // Get Media Type
            Byte mediaType = getMediaType(metadata);
            mediaElement.setType(mediaType);
            
            // Flags
            boolean title = false;
            boolean comment = false;
                        
            // Begin Parsing
            for (int i = 0; i < metadata.length; i++)
            {
                Matcher matcher;
                String line = metadata[i];
                
                if(mediaType == MediaElementType.AUDIO || mediaType == MediaElementType.VIDEO)
                {
                    //
                    // Duration
                    //
                    matcher = DURATION.matcher(line);

                    if(matcher.find())
                    {                    
                        int hours = Integer.parseInt(matcher.group(1));
                        int minutes = Integer.parseInt(matcher.group(2));
                        int seconds = Integer.parseInt(matcher.group(3));
                        int ms = Integer.parseInt(matcher.group(4));
                        mediaElement.setDuration(hours * 3600 + minutes * 60 + seconds + Math.round((float)(ms * 0.01)));
                        
                        continue;
                    }
                    
                    //
                    // Bitrate
                    //
                    matcher = BITRATE.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setBitrate(Integer.parseInt(matcher.group(1)));
                        
                        continue;
                    }
                    
                    //
                    // Audio Stream
                    //
                    matcher = AUDIO_STREAM.matcher(line);

                    if (matcher.find()) 
                    {     
                        // Always set audio language for video elements
                        if(matcher.group(1) == null && mediaType == MediaElementType.VIDEO)
                        {
                            mediaElement.setAudioLanguage(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioLanguage(), "und"));
                        }
                        
                        // Set audio language if present
                        if(matcher.group(1) != null)
                        {
                            mediaElement.setAudioLanguage(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioLanguage(), String.valueOf(matcher.group(2))));
                        }
                        
                        // Codec
                        mediaElement.setAudioCodec(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioCodec(), String.valueOf(matcher.group(3))));
                        
                        //Sample Rate
                        mediaElement.setAudioSampleRate(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioSampleRate(), String.valueOf(matcher.group(4))));
                        
                        //Configuration
                        mediaElement.setAudioConfiguration(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioConfiguration(), String.valueOf(matcher.group(5))));
                    
                        // Title
                        if(metadata.length > (i+2)) {
                            matcher = TITLE.matcher(metadata[i+2]);
                            
                            if(matcher.find()) {
                                i = i+2;
                                mediaElement.setAudioName(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioName(), String.valueOf(matcher.group(1))));
                            } else if(mediaType == MediaElementType.VIDEO) {
                                mediaElement.setAudioName(ParserUtils.addToCommaSeparatedList(mediaElement.getAudioName(), " "));
                            }
                        }                        
                        
                        continue;
                    }
                }
                
                if(mediaType == MediaElementType.AUDIO)
                {
                    //
                    // Title
                    //
                    matcher = TITLE.matcher(line);

                    if(matcher.find() && !title)
                    {
                        mediaElement.setTitle(String.valueOf(matcher.group(1)));
                        title = true;
                        
                        continue;
                    }

                    //
                    // Artist
                    //
                    matcher = ARTIST.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setArtist(String.valueOf(matcher.group(2)));
                        
                        continue;
                    }

                    //
                    // Album Artist
                    //
                    matcher = ALBUMARTIST.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setAlbumArtist(String.valueOf(matcher.group(2)));
                        
                        continue;
                    }

                    //
                    // Album
                    //
                    matcher = ALBUM.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setAlbum(String.valueOf(matcher.group(1)));
                        
                        continue;
                    }

                    //
                    // Comment
                    //
                    matcher = COMMENT.matcher(line);

                    if(matcher.find() && !comment)
                    {
                        mediaElement.setDescription(String.valueOf(matcher.group(1)));
                        comment = true;
                        
                        continue;
                    }

                    //
                    // Date
                    //
                    matcher = YEAR.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setYear(Short.parseShort(matcher.group(2)));
                        
                        continue;
                    }

                    //
                    // Disc Number
                    //
                    matcher = DISCNUMBER.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setDiscNumber(Byte.parseByte(matcher.group(2)));
                        
                        continue;
                    }

                    //
                    // Disc Subtitle
                    //

                    matcher = DISCSUBTITLE.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setDiscSubtitle(String.valueOf(matcher.group(1)));
                        
                        continue;
                    }

                    //
                    // Track Number
                    //
                    matcher = TRACK.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setTrackNumber(Short.parseShort(matcher.group(1)));
                        
                        continue;
                    }
                    
                    //
                    // Genre
                    //
                    matcher = GENRE.matcher(line);

                    if(matcher.find())
                    {
                        mediaElement.setGenre(String.valueOf(matcher.group(1)));
                        
                        continue;
                    }
                }
                
                if(mediaType == MediaElementType.VIDEO)
                {
                    //
                    // Video Stream
                    //
                    matcher = VIDEO_STREAM.matcher(line);

                    // Only pull metadata for the first video stream (embedded images are also detected as video...)
                    if (matcher.find() && mediaElement.getVideoCodec() == null) 
                    {
                        // Codec
                        mediaElement.setVideoCodec(String.valueOf(matcher.group(1)));
                        
                        // Dimensions
                        short width = Short.parseShort(matcher.group(2));
                        short height = Short.parseShort(matcher.group(3));

                        if(width > 0 && height > 0)
                        {
                            mediaElement.setVideoWidth(width);
                            mediaElement.setVideoHeight(height);
                        }
                        
                        continue;
                    }
                    
                    //
                    // Subtitle Stream
                    //
                    matcher = SUBTITLE_STREAM.matcher(line);

                    if (matcher.find()) 
                    {
                        // Language
                        if(matcher.group(1) == null)
                        {
                            mediaElement.setSubtitleLanguage(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleLanguage(), "und"));
                        }
                        else
                        {
                            mediaElement.setSubtitleLanguage(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleLanguage(), String.valueOf(matcher.group(2))));
                        }
                        
                        // Format
                        mediaElement.setSubtitleFormat(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleFormat(), String.valueOf(matcher.group(3))));
                        
                        //Forced
                        if(matcher.group(4) == null)
                        {
                            mediaElement.setSubtitleForced(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleForced(), "false"));
                        }
                        else
                        {
                            mediaElement.setSubtitleForced(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleForced(), "true"));
                        }
                        
                        // Title
                        if(metadata.length > (i+2)) {
                            matcher = TITLE.matcher(metadata[i+2]);
                            
                            if(matcher.find()) {
                                i = i+2;
                                mediaElement.setSubtitleName(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleName(), String.valueOf(matcher.group(1))));
                            } else {
                                mediaElement.setSubtitleName(ParserUtils.addToCommaSeparatedList(mediaElement.getSubtitleName(), " "));
                            }
                        }                        
                    }
                }
            }            
        } 
        catch (IOException x) 
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse metadata for file " + mediaElement.getPath(), x);
        }

        return mediaElement;
    }
    
    private byte getMediaType(String[] metadata) {        
        for (String line : metadata) {
            if(VIDEO_STREAM.matcher(line).find()) {
                return MediaElementType.VIDEO;
            }
        }
        
        return MediaElementType.AUDIO;
    }
}
