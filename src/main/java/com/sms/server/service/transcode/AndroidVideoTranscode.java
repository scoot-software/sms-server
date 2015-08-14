/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service.transcode;

import com.sms.server.domain.MediaElement;
import com.sms.server.service.LogService;
import com.sms.server.service.TranscodeService;
import com.sms.server.service.TranscodeService.TranscodeProfile;
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
public class AndroidVideoTranscode {
    
    private static final String CLASS_NAME = "AndroidVideoTranscode";
    
    @Autowired
    private TranscodeService transcodeService;
    
    private static final String SUPPORTED_VIDEO_CODECS = "vp8,h264";
    private static final String SUPPORTED_AUDIO_CODECS = "vorbis,mp3,aac";
        
    private static final Integer DEFAULT_QUALITY = 360;
    private static final Integer MAX_SAMPLE_RATE = 48000;
    
    public List<String> createTranscodeCommand(MediaElement mediaElement, TranscodeProfile profile)
    {
        List<String> command = new ArrayList<>();
        
        ///////////////////////////////////////////////////////////////////////////////
        
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
                
        // Transcoder path
        command.add(transcodeService.getTranscoder().getPath());
        
        // Seek
        command.add("-ss");
        command.add(profile.getOffset().toString());
        
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
            command.addAll(transcodeService.getSubtitleCommands(profile.getSubtitleTrack(), mediaElement, profile.getOffset()));
        }
        
        // Video Bit Rate
        command.add("-b:v");
        command.add(TranscodeService.getVideoBitrate(profile.getVideoQuality()) + "k");
        
        // Video Resolution
        command.add("-s");
        command.add(String.valueOf(resolution.width) + "x" + String.valueOf(resolution.height));
        
        // Video codec
        command.addAll(transcodeService.getVideoCodecCommands(TranscodeService.getDefault(SUPPORTED_VIDEO_CODECS)));
        
        // Fixes some issues with mpegts
        command.add("-bsf");
        command.add("h264_mp4toannexb");
        
        // Audio
        if(profile.getAudioTrack() != null)
        {
            // Map Audio
            command.add("-map");
            command.add("0:a:" + profile.getAudioTrack());
            
            // Stereo Audio
            profile.setAudioCodec(TranscodeService.getDefault(SUPPORTED_AUDIO_CODECS));
            profile.setMaxChannelCount(2);
            profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
            
            command.addAll(transcodeService.getAudioCommands(mediaElement, profile));
        }
        
        // Use all CPU cores
        command.add("-threads");
        command.add("0");
        
        //
        // Segmenter options
        //
        
        command.add("-f");
        command.add(TranscodeService.getVideoFormatFromCodec(TranscodeService.getDefault(SUPPORTED_VIDEO_CODECS)));
        
        // Output Pipe
        command.add("-");
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, command.toString(), null);
        return command;
    }
    
    public TranscodeProfile processTranscodeProfile(MediaElement mediaElement, TranscodeProfile profile)
    {   
        return profile;
    }
}