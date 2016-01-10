/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service.transcode;

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.TranscodeService;
import com.scooter1556.sms.server.service.TranscodeService.AudioQuality;
import com.scooter1556.sms.server.service.TranscodeService.TranscodeProfile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author scott2ware
 */
@Service
public class KodiAudioTranscode {
    
    private static final String CLASS_NAME = "KodiAudioTranscode";
    
    @Autowired
    private TranscodeService transcodeService;
    
    private static final String SUPPORTED_CODECS = "mp3,vorbis,aac,flac,ac3";
    private static final String LOSSY_CODECS = "vorbis,aac,mp3,ac3";
    private static final String LOSSLESS_CODECS = "flac";
    private static final String MULTI_CHANNEL_CODECS = "ac3";
    
    private static final Byte DEFAULT_QUALITY = AudioQuality.HIGH;
        
    public List<String> createTranscodeCommand(MediaElement mediaElement, TranscodeProfile profile)
    {   
        List<String> command = new ArrayList<>();
        
        // Transcoder path
        command.add(transcodeService.getTranscoder().getPath());
        
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
        
        int channelCount = TranscodeService.getAudioChannelCount(mediaElement.getAudioConfiguration());
        
        // Multichannel Audio
        if(profile.getMaxChannelCount() > 2 && channelCount > 2)
        {
            if(mediaElement.getAudioCodec() != null)
            {
                if(TranscodeService.isSupported(MULTI_CHANNEL_CODECS, mediaElement.getAudioCodec()))
                {
                    profile.setAudioCodec(TranscodeService.validateAudioCodec(mediaElement.getAudioCodec()));
                    if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }
                    profile.setMaxChannelCount(TranscodeService.getMaxChannelsForCodec(profile.getAudioCodec()));
                    
                    // Test if we need to transcode
                    if(mediaElement.getAudioSampleRate() == null) { profile.setAudioTranscodeRequired(true); }
                    else if(Integer.valueOf(mediaElement.getAudioSampleRate()) > profile.getMaxSampleRate()) { profile.setAudioTranscodeRequired(true); }
                    else if(channelCount > profile.getMaxChannelCount()) { profile.setAudioTranscodeRequired(true); }
                    return profile;
                }
            }
            
            return setProfileDefaultMultichannel(profile);
        }
        
        // Lossless Audio
        else if(profile.getAudioQuality().equals(AudioQuality.LOSSLESS))
        {
            // Set max channels to stereo
            profile.setMaxChannelCount(2);
            
            if(mediaElement.getAudioCodec() != null)
            {
                if(TranscodeService.isSupported(SUPPORTED_CODECS, mediaElement.getAudioCodec()))
                {
                    profile.setAudioCodec(TranscodeService.validateAudioCodec(mediaElement.getAudioCodec()));
                    if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }

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
            // Set max channels to stereo
            profile.setMaxChannelCount(2);
            
            if(mediaElement.getAudioCodec() != null)
            {
                if(TranscodeService.isSupported(LOSSY_CODECS, mediaElement.getAudioCodec()))
                { 
                    profile.setAudioCodec(TranscodeService.validateAudioCodec(mediaElement.getAudioCodec()));
                    if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }
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
    
    private TranscodeProfile setProfileDefaultMultichannel(TranscodeProfile profile)
    {
        profile.setAudioCodec(TranscodeService.getDefault(MULTI_CHANNEL_CODECS));
        if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }
        profile.setMaxChannelCount(TranscodeService.getMaxChannelsForCodec(profile.getAudioCodec()));
        profile.setAudioBitrate(TranscodeService.getMultiChannelAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
        profile.setAudioTranscodeRequired(true);
            
        return profile;
    }
    
    private TranscodeProfile setProfileDefaultLossless(TranscodeProfile profile)
    {
        profile.setAudioCodec(TranscodeService.getDefault(LOSSLESS_CODECS));
        if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }
        profile.setAudioTranscodeRequired(true);
            
        return profile;
    }
    
    private TranscodeProfile setProfileDefaultLossy(TranscodeProfile profile)
    {
        profile.setAudioCodec(TranscodeService.getDefault(LOSSY_CODECS));
        if(profile.getMaxSampleRate() == null) { profile.setMaxSampleRate(TranscodeService.getMaxSampleRateForCodec(profile.getAudioCodec())); }
        profile.setAudioBitrate(TranscodeService.getAudioBitrateForCodec(profile.getAudioCodec(), profile.getAudioQuality()));
        profile.setAudioTranscodeRequired(true);
            
        return profile;
    }
}
