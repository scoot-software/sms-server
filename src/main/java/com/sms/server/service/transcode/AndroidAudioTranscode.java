/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service.transcode;

import com.sms.server.domain.MediaElement;
import com.sms.server.service.LogService;
import com.sms.server.service.TranscodeService;
import com.sms.server.service.TranscodeService.AudioQuality;
import com.sms.server.service.TranscodeService.TranscodeProfile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */
@Service
public class AndroidAudioTranscode {
    
    private static final String CLASS_NAME = "AndroidAudioTranscode";
    
    @Autowired
    private TranscodeService transcodeService;
    
    private static final String SUPPORTED_CODECS = "mp3,vorbis,aac,flac";
    private static final String LOSSY_CODECS = "vorbis,aac,mp3";
    private static final String LOSSLESS_CODECS = "flac";
    
    private static final Byte DEFAULT_QUALITY = AudioQuality.MEDIUM;
    private static final int MAX_SAMPLE_RATE = 48000;
        
    public List<String> createTranscodeCommand(MediaElement mediaElement, TranscodeProfile profile)
    {   
        List<String> command = new ArrayList<>();
        
        // Transcoder path
        command.add(transcodeService.getTranscoder().getPath());
        
        // Seek
        command.add("-ss");
        command.add(profile.getOffset().toString());
        
        // Input media file
        command.add("-i");
        command.add(mediaElement.getPath());
        
        // Test if we need to transcode
        if(profile.isAudioTranscodeRequired())
        {
            if(transcodeService.getAudioCommands(mediaElement, profile) == null) { return null; }
            command.addAll(transcodeService.getAudioCommands(mediaElement, profile));
        }
        else
        {
            command.add("-c:a");
            command.add("copy");
        }
        
        // Format
        command.add("-f");
        command.add(TranscodeService.getAudioFormatFromCodec(profile.getAudioCodec()));
        
        // Output Pipe
        command.add("-");
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, command.toString(), null);
        return command;
    }
    
    public TranscodeProfile processTranscodeProfile(MediaElement mediaElement, TranscodeProfile profile)
    {        
        // Quality
        profile.setAudioQuality(TranscodeService.validateAudioQuality(profile.getAudioQuality(), DEFAULT_QUALITY));
        
        // Get Source Channel Count
        int channelCount = TranscodeService.getAudioChannelCount(mediaElement.getAudioConfiguration());
        
        // Set max channel count to 2
        profile.setMaxChannelCount(2);
        
        // Lossless Audio
        if(profile.getAudioQuality().equals(AudioQuality.LOSSLESS))
        {
            if(mediaElement.getAudioCodec() != null)
            {
                if(TranscodeService.isSupported(SUPPORTED_CODECS, mediaElement.getAudioCodec()))
                { 
                    profile.setAudioCodec(TranscodeService.validateAudioCodec(mediaElement.getAudioCodec()));
                    profile.setMaxSampleRate(MAX_SAMPLE_RATE);
                    
                    // Test if we need to transcode
                    if(mediaElement.getAudioSampleRate() == null) { profile.setAudioTranscodeRequired(true); }
                    else if(Integer.valueOf(mediaElement.getAudioSampleRate()) > profile.getMaxSampleRate()) { profile.setAudioTranscodeRequired(true); }
                    else if(channelCount > profile.getMaxChannelCount()) { profile.setAudioTranscodeRequired(true); }
                    
                    return profile;
                }
            }
            
            return setProfileDefaultLossless(profile);  
        }
        
        // Lossy Audio
        else
        {
            if(mediaElement.getAudioCodec() != null)
            {
                if(TranscodeService.isSupported(LOSSY_CODECS, mediaElement.getAudioCodec()))
                { 
                    profile.setAudioCodec(TranscodeService.validateAudioCodec(mediaElement.getAudioCodec()));
                    profile.setMaxSampleRate(MAX_SAMPLE_RATE);
                    profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
                    
                    // Test if we need to transcode
                    if(mediaElement.getAudioSampleRate() == null) { profile.setAudioTranscodeRequired(true); }
                    else if(Integer.valueOf(mediaElement.getAudioSampleRate()) > profile.getMaxSampleRate()) { profile.setAudioTranscodeRequired(true); }
                    else if(channelCount > profile.getMaxChannelCount()) { profile.setAudioTranscodeRequired(true); }
                    else if(mediaElement.getBitrate() == null) { profile.setAudioTranscodeRequired(true); }
                    else if(mediaElement.getBitrate() > profile.getAudioBitrate()) { profile.setAudioTranscodeRequired(true); }
                    
                    return profile;
                }
            }
            
            return setProfileDefaultLossy(profile);  
        }        
    }
    
    private TranscodeProfile setProfileDefaultLossless(TranscodeProfile profile)
    {
        profile.setAudioCodec(TranscodeService.getDefault(LOSSLESS_CODECS));
        profile.setMaxSampleRate(MAX_SAMPLE_RATE);
        profile.setAudioTranscodeRequired(true);
            
        return profile;
    }
    
    private TranscodeProfile setProfileDefaultLossy(TranscodeProfile profile)
    {
        profile.setAudioCodec(TranscodeService.getDefault(LOSSY_CODECS));
        profile.setMaxSampleRate(MAX_SAMPLE_RATE);
        profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
        profile.setAudioTranscodeRequired(true);
            
        return profile;
    }
}
