/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service.transcode;

import com.sms.server.domain.MediaElement;
import com.sms.server.service.AdaptiveStreamingService.AdaptiveStreamingProfile;
import com.sms.server.service.AdaptiveStreamingService.StreamType;
import com.sms.server.service.TranscodeService;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */
@Service
public class DASHTranscode {
    
    private static final String CLASS_NAME = "DASHTranscode";
    
    @Autowired
    private TranscodeService transcodeService;
    
    private static final String VIDEO_CODEC = "h264";
    private static final String AUDIO_CODEC = "aac";
    private static final String MULTI_CHANNEL_CODEC = "ac3";
    
    private static final String VIDEO_CODEC_WEBM = "vp8";
    private static final String AUDIO_CODEC_WEBM = "vorbis";
    
    private static final Integer DEFAULT_QUALITY = 360;
    public static final Integer DEFAULT_SEGMENT_DURATION = 10;
    
    private static final Integer MAX_SAMPLE_RATE = 48000;
    
    public List<String> createTranscodeCommand(MediaElement mediaElement, AdaptiveStreamingProfile profile)
    {
        List<String> command = new ArrayList<>();
                
        //
        // Transcode Parameters
        //
        
        // Quality Setting
        profile.setVideoQuality(TranscodeService.validateVideoQuality(profile.getVideoQuality(), DEFAULT_QUALITY));
        Integer sourceQuality = transcodeService.getVideoSourceQuality(mediaElement);
        
        // Quality setting used is limited by the source file regardless of clients capabilities
        if(sourceQuality != null)
        {
            if(sourceQuality < profile.getVideoQuality()) { profile.setVideoQuality(sourceQuality); }
        }
                
        // Resolution
        Dimension resolution = transcodeService.getVideoResolution(profile.getVideoQuality(), mediaElement);
        
        // Subtitles        
        if(profile.getSubtitleTrack() == null) { profile.setSubtitleTrack(transcodeService.getForcedSubtitleIndex(mediaElement)); }
        
        if(profile.getSubtitleTrack() != null)
        {
            if(transcodeService.isSubtitleStreamAvailable(profile.getSubtitleTrack(), mediaElement))
            {
                // Check subtitle stream is supported
                if(!TranscodeService.isSupportedSubtitleCodec(mediaElement.getSubtitleStreams().get(profile.getSubtitleTrack()).getFormat())) { profile.setSubtitleTrack(null); }
            }
        }
        
        // Audio Stream
        if(profile.getAudioTrack() == null)
        {
            // Default to the first audio stream if available
            if(transcodeService.isAudioStreamAvailable(0, mediaElement)) { profile.setAudioTrack(0); }
        }
        else
        {
            // Check requested audio stream is available
            if(!transcodeService.isAudioStreamAvailable(profile.getAudioTrack(), mediaElement))
            {
                if(transcodeService.isAudioStreamAvailable(0, mediaElement)) { profile.setAudioTrack(0); }
                else { profile.setAudioTrack(null); }
            }
        }
        
        // Audio Quality
        profile.setAudioQuality(TranscodeService.getAudioQualityForVideo(profile.getVideoQuality()));
        
        // Sample Rate
        profile.setMaxSampleRate(MAX_SAMPLE_RATE);
        
        // Segmenter Options
        String fileExtension = null;
        String segmentFormat = null;
        
        switch(profile.getType())
        {
            case StreamType.DASH:
                fileExtension = ".ts";
                segmentFormat = "mpegts";
                break;
                
            case StreamType.DASH_WEBM:
                fileExtension = ".webm";
                segmentFormat = "webm";
                break;
                
            case StreamType.DASH_MP4:
                fileExtension = ".mp4";
                segmentFormat = "mp4";
                break;
        }
        
        //
        // Transcode Command
        //
        
        // Transcoder path
        command.add(transcodeService.getTranscoder().getPath());
        
        // Offset
        Integer offset = 0;
        
        if(profile.getSegmentOffset() > 0)
        {
            offset = DEFAULT_SEGMENT_DURATION * profile.getSegmentOffset();
        }
        
        if(offset > 0)
        {
            command.add("-ss");
            command.add(offset.toString());
        }
        
        // Input media file
        command.add("-i");
        command.add(mediaElement.getPath());
        
        // Don't embed subtitles
        command.add("-sn");
        
        // Map video (only necessary if subtitles are not enabled)
        if(profile.getSubtitleTrack() == null)
        {
            command.add("-map");
            command.add("0:v");
        }
        
        //        
        // Subtitles
        //
        
        if(profile.getSubtitleTrack() != null)
        {
            command.addAll(transcodeService.getSubtitleCommands(profile.getSubtitleTrack(), mediaElement, offset));
        }
        
        // Video Bit Rate
        command.add("-b:v");
        command.add(TranscodeService.getVideoBitrate(profile.getVideoQuality()) + "k");
        
        // Video Resolution
        command.add("-s");
        command.add(String.valueOf(resolution.width) + "x" + String.valueOf(resolution.height));
        
        // Video codec
        if(profile.getType().equals(StreamType.DASH_WEBM))
        {
            command.addAll(transcodeService.getVideoCodecCommands(VIDEO_CODEC_WEBM));
        }
        else
        {
            command.addAll(transcodeService.getVideoCodecCommands(VIDEO_CODEC));
        }
        
        // Fix issues with mp4 to mpegts
        if(profile.getType().equals(StreamType.DASH))
        {
            command.add("-bsf");
            command.add("h264_mp4toannexb");
        }
        
        // DASH Compliant Output
        command.add("-dash");
        command.add("1");
        
        // Audio
        if(profile.getAudioTrack() != null)
        {
            // Map Audio
            command.add("-map");
            command.add("0:a:" + profile.getAudioTrack());
            
            // Multi-channel
            if(!profile.getType().equals(StreamType.DASH_WEBM) && profile.isMultiChannelEnabled() && transcodeService.getAudioStreamChannelCount(profile.getAudioTrack(), mediaElement) > 2)
            {
                profile.setAudioCodec(MULTI_CHANNEL_CODEC);
                profile.setMaxChannelCount(TranscodeService.getMaxChannelsForCodec(profile.getAudioCodec()));
                profile.setAudioBitrate(TranscodeService.getMultiChannelAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));                
            }
            // Stereo
            else
            {
                // Set channel configuration to stereo
                profile.setMaxChannelCount(2);
                
                if(profile.getType().equals(StreamType.DASH_WEBM))
                {
                    profile.setAudioCodec(AUDIO_CODEC_WEBM);
                    profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
                }
                else
                {
                    profile.setAudioCodec(AUDIO_CODEC);
                    profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));                    
                }
            }
            
            command.addAll(transcodeService.getAudioCommands(mediaElement, profile));
        }
        
        // Use all CPU cores
        command.add("-threads");
        command.add("0");
        
        // Reduce overhead with global header for mpegts
        if(profile.getType().equals(StreamType.DASH))
        {
            command.add("-flags");
            command.add("-global_header");
        }
        
        //
        // Segmenter options
        //
        
        command.add("-f");
        command.add("segment");
        
        command.add("-segment_time");
        command.add(DEFAULT_SEGMENT_DURATION.toString());
        
        command.add("-segment_format");
        command.add(segmentFormat);
        
        if(profile.getType().equals(StreamType.DASH_MP4))
        {
            command.add("-segment_format_options");
            command.add("movflags=+faststart");
        }
        
        if(profile.getSegmentOffset() > 0)
        {
            command.add("-segment_start_number");
            command.add(profile.getSegmentOffset().toString());
        }
        
        if(offset > 0)
        {
            command.add("-initial_offset");
            command.add(offset.toString());
        }
        
        // Output Directory
        command.add(profile.getOutputDirectory().getPath() + "/stream%05d" + fileExtension);
                
        return command;
    }
}