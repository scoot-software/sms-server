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
package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.HardwareAccelerator;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.AudioStream;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaElement.SubtitleStream;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeCommand;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.utilities.MediaUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

@Service
public class TranscodeService {

    private static final String CLASS_NAME = "TranscodeService";
    
    private Transcoder transcoder = null;
    
    // Setup transcoder
    public TranscodeService() {
        // Attempt to find a transcoder
        this.transcoder = TranscodeUtils.getTranscoder();
        
        if(this.transcoder == null) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to find a suitable transcoder!", null);
        } else {
            LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Transcoder " + this.transcoder, null);
        }
        
        // Check all required codecs are supported
        if(!TranscodeUtils.checkTranscoder(transcoder)) {
            LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Transcoder is missing required codec support!", null);
        }
    }
    
    public Transcoder getTranscoder() {
        if(this.transcoder == null) {
            this.transcoder = TranscodeUtils.getTranscoder();
            return transcoder;
        }
        
        return this.transcoder;
    }
    
    public String[][] getTranscodeCommand(Job job) {
        ArrayList<TranscodeCommand> commands = new ArrayList<>();
        
        // Get transcode profile
        TranscodeProfile profile = job.getTranscodeProfile();
        
        // Number of potential transcode commands
        int transcodeCommands = 1;
        
        // Hardware accelerators
        List<HardwareAccelerator> accelerators = null;
        
        if(profile.getVideoTranscodes() != null) {
            // Some encoders don't support bitrate limiting so check if this is a requirement
            boolean bitrateLimit = false;
            
            for(VideoTranscode transcode : profile.getVideoTranscodes()) {
                if(transcode.getMaxBitrate() != null) {
                    bitrateLimit = true;
                }
            }

            // Get available hardware accelerators
            accelerators = transcoder.getHardwareAcceleratorOptions(bitrateLimit);
            
            // Determine number of potential transcode commands to generate
            if(accelerators != null) {
                transcodeCommands += accelerators.size();
            }
        }

        for(int i = 0; i < transcodeCommands; i++) {
            commands.add(new TranscodeCommand());
            
            boolean initialised = false;
            
            // Transcoder path
            commands.get(i).getCommands().add(transcoder.getPath().toString());
            commands.get(i).getCommands().add("-y");

            // Seek
            commands.get(i).getCommands().add("-ss");
            commands.get(i).getCommands().add(profile.getOffset().toString());

            // Video
            if(profile.getVideoTranscodes() != null && profile.getVideoStream() != null) {
                HardwareAccelerator hardwareAccelerator = null;
            
                // Software or hardware based transcoding
                if(accelerators != null && accelerators.size() > i) {
                    hardwareAccelerator = accelerators.get(i);
                }
                
                //  Get list of transcodes for the desired video stream
                List<VideoTranscode> vTranscodes = TranscodeUtils.getVideoTranscodesById(profile.getVideoTranscodes(), profile.getVideoStream());
                
                // Populate filters
                for(int v = 0; v < vTranscodes.size(); v++) {
                    // If we are copying the stream continue with the next transcode
                    if(vTranscodes.get(v).getCodec() == SMS.Codec.COPY) {
                        continue;
                    }
                    
                    // Add a filter list for video transcode
                    commands.get(i).getFilters().add(new ArrayList<>());
                    
                    if(hardwareAccelerator == null || !hardwareAccelerator.isEncodingSupported()) {
                        commands.get(i).getFilters().get(v).addAll(getSoftwareVideoEncodingFilters(vTranscodes.get(v).getResolution()));
                    } else {
                        commands.get(i).getFilters().get(v).addAll(getHardwareVideoEncodingFilters(vTranscodes.get(v).getResolution(), hardwareAccelerator));
                    }
                }
                
                // Hardware decoding
                if(hardwareAccelerator != null) {
                    commands.get(i).getCommands().addAll(getHardwareAccelerationCommands(hardwareAccelerator));
                }
                
                // Input media file
                initialised = true;
                commands.get(i).getCommands().add("-i");
                commands.get(i).getCommands().add(job.getMediaElement().getPath());
                
                // Enable experimental codecs
                commands.get(i).getCommands().add("-strict");
                commands.get(i).getCommands().add("experimental");

                // Remove metadata
                commands.get(i).getCommands().add("-map_metadata");
                commands.get(i).getCommands().add("-1");
                
                // Filter commands
                commands.get(i).getCommands().addAll(getFilterCommands(profile.getVideoStream(), commands.get(i).getFilters()));
                
                for(int v = 0; v < vTranscodes.size(); v++) {
                    VideoTranscode transcode = vTranscodes.get(v);
                    
                    // Stream copy
                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("0:" + transcode.getId());
                        
                        // Codec
                        commands.get(i).getCommands().addAll(getSoftwareVideoEncodingCommands(transcode.getCodec(), transcode.getMaxBitrate()));
                    } else {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("[v" + v + "]");

                        // Encoding
                        if(hardwareAccelerator == null || !hardwareAccelerator.isEncodingSupported()) {
                            commands.get(i).getCommands().addAll(getSoftwareVideoEncodingCommands(transcode.getCodec(), transcode.getMaxBitrate()));
                        } else {
                            commands.get(i).getCommands().addAll(getHardwareVideoEncodingCommands(hardwareAccelerator));
                        }

                        commands.get(i).getCommands().add("-force_key_frames");
                        commands.get(i).getCommands().add("expr:gte(t,n_forced*" + profile.getSegmentDuration()  + ")");
                    }
                }
                
                // Subtitles
                if(profile.getSubtitleTranscodes() != null) {
                    for(SubtitleTranscode transcode : profile.getSubtitleTranscodes()) {
                        // Transcode commands
                        commands.get(i).getCommands().addAll(getSubtitleCommands(transcode));
                    }
                }
            }

            // Audio
            if(profile.getAudioTranscodes() != null) {
                if(!initialised) {
                    // Input media file
                    commands.get(i).getCommands().add("-i");
                    commands.get(i).getCommands().add(job.getMediaElement().getPath());
                }
                
                for(AudioTranscode transcode : profile.getAudioTranscodes()) {
                    // Transcode commands
                    commands.get(i).getCommands().addAll(getAudioCommands(transcode));
                }
            }
            
            // Segmenter
            commands.get(i).getCommands().addAll(getSegmentCommands(job.getId(), profile.getOffset(), profile.getSegmentDuration()));
        }
        
        // Prepare result
        String[][] result = new String[commands.size()][];
        
        for(int r = 0; r < commands.size(); r++) {
            result[r] = commands.get(r).getCommands().toArray(new String[0]);
        }
        
        return result;
    }
    
    private Collection<String> getSegmentCommands(UUID id, Integer offset, Integer duration) {
        if(id == null || duration == null) {
            return null;
        }
        
        Collection<String> commands = new LinkedList<>();
        
        commands.add("-f");
        commands.add("segment");

        commands.add("-segment_time");
        commands.add(duration.toString());
        
        commands.add("-segment_time_delta");
        commands.add("0.0625");

        commands.add("-segment_format");
        commands.add("matroska");

        if(offset != null && offset > 0) {
            commands.add("-segment_start_number");
            commands.add(String.valueOf(offset / duration));
            
            commands.add("-initial_offset");
            commands.add(String.valueOf(offset));
        }

        commands.add("-segment_list_size");
        commands.add("0");
        
        commands.add("-segment_list_type");
        commands.add("flat");

        commands.add("-segment_list");
        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/segments.txt");
        
        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/%d");
        
        return commands;
    }
    
    private Collection<String> getHardwareAccelerationCommands(HardwareAccelerator hardwareAccelerator) {
        Collection<String> commands = new LinkedList<>();

        switch(hardwareAccelerator.getName()) {
            case "vaapi":
                if(hardwareAccelerator.isDecodingSupported()) {
                    commands.add("-hwaccel");
                    commands.add(hardwareAccelerator.getName());
                 
                    commands.add("-hwaccel_output_format");
                    commands.add("vaapi");
                }
                
                if(hardwareAccelerator.isDecodingSupported() || hardwareAccelerator.isEncodingSupported()) {
                    commands.add("-vaapi_device");
                    commands.add(hardwareAccelerator.getDevice().toString());
                }
                
                break;

            case "cuvid":
                if(hardwareAccelerator.isDecodingSupported()) {
                    commands.add("-hwaccel");
                    commands.add(hardwareAccelerator.getName());
                }
                
                break;
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a given hardware accelerator.
     */
    private Collection<String> getHardwareVideoEncodingCommands(HardwareAccelerator hardwareAccelerator) {
        Collection<String> commands = new LinkedList<>();
        
        if(hardwareAccelerator != null) {
            switch(hardwareAccelerator.getName()) {
                case "vaapi":
                    commands.add("-c:v");
                    commands.add("h264_vaapi");
                    commands.add("-qp");
                    commands.add("23");
                    break;

                case "cuvid":
                    commands.add("-c:v");
                    commands.add("h264_nvenc");
                    commands.add("-bf:v");
                    commands.add("4");
                    break;
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of filters for a given hardware accelerator.
     */
    private List<String> getHardwareVideoEncodingFilters(Dimension resolution, HardwareAccelerator hardwareAccelerator) {
        List<String> filters = new ArrayList<>();
        
        if(hardwareAccelerator != null) {
            switch(hardwareAccelerator.getName()) {
                case "vaapi":
                    filters.add("format=nv12|vaapi");
                    filters.add("hwupload");

                    if(resolution != null) {
                        filters.add("scale_vaapi=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    break;

                case "cuvid":
                    if(resolution != null) {
                        filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    break;
            }
        }
        
        return filters;
    }
    
    /*
     * Returns a list of commands for a given software video codec to optimise transcoding.
     */
    private Collection<String> getSoftwareVideoEncodingCommands(int codec, Integer maxrate) {
        Collection<String> commands = new LinkedList<>();
        
        // Video Codec
        commands.add("-c:v");

        switch(codec) {       
            case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH:
                commands.add("libx264");
                commands.add("-crf");
                commands.add("25");
                commands.add("-preset");
                commands.add("superfast");
                commands.add("-pix_fmt");
                commands.add("yuv420p");
                commands.add("-profile:v");
                
                //  Profile
                switch (codec) {
                    case SMS.Codec.AVC_BASELINE:
                        commands.add("baseline");
                        break;
                    case SMS.Codec.AVC_MAIN:
                        commands.add("main");
                        break;
                    case SMS.Codec.AVC_HIGH:
                        commands.add("high");
                        break;
                    default:
                        commands.add("high");
                        break;
                }

                if(maxrate != null) {
                    commands.add("-maxrate");
                    commands.add(maxrate.toString() + "k");
                    commands.add("-bufsize");
                    commands.add("2M");
                }

                break;
                
            case SMS.Codec.COPY:
                commands.add("copy");
                break;

            default:
                return null;
        }

        return commands;
    }
    
    /*
     * Returns a list of filters for software encoding.
     */
    private List<String> getSoftwareVideoEncodingFilters(Dimension resolution) {
        List<String> filters = new ArrayList<>();
        
        if(resolution != null) {
            filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
        }
        
        return filters;
    }
    
    /*
     * Returns a list of commands for an audio stream.
     */
    private Collection<String> getAudioCommands(AudioTranscode transcode) {
        Collection<String> commands = new LinkedList<>();
        
        if(transcode.getCodec() != null) {
            // Mapping
            commands.add("-map");
            commands.add("0:" + transcode.getId());
        
            // Codec
            commands.add("-c:a");
            commands.add(TranscodeUtils.getEncoderForCodec(transcode.getCodec()));
            
            // Quality
            if(transcode.getBitrate() > 0) {
                commands.add("-b:a");
                commands.add(String.valueOf(transcode.getBitrate()) + "k");
            }
            
            // Downmix
            if(transcode.getChannelCount() > 0) {
                commands.add("-ac");
                commands.add(String.valueOf(transcode.getChannelCount()));
            }
            
            // Sample rate
            if(transcode.getSampleRate() != null) {
                commands.add("-ar");
                commands.add(String.valueOf(transcode.getSampleRate()));
            }
            
            // Replaygain
            if(transcode.getReplaygain() != null && transcode.getReplaygain() != 0f) {
                commands.add("-af");
                commands.add("volume=volume="+ String.valueOf(transcode.getReplaygain()) + "dB");
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a subtitle stream.
     */
    private Collection<String> getSubtitleCommands(SubtitleTranscode transcode) {
        Collection<String> commands = new LinkedList<>();
        
        if(transcode.getCodec() != null) {
            // Mapping
            commands.add("-map");
            commands.add("0:" + transcode.getId());
        
            // Codec
            commands.add("-c:s");
            commands.add(TranscodeUtils.getEncoderForCodec(transcode.getCodec()));
        }
        
        return commands;
    }
    
    public Collection<String> getFilterCommands(int streamId, ArrayList<ArrayList<String>> filters) {
        Collection<String> commands = new LinkedList<>();
        
        if(!filters.isEmpty()) {
            commands.add("-filter_complex");
            
            StringBuilder filterBuilder = new StringBuilder();
            
            // Add each filter chain in turn
            for(int i = 0; i < filters.size(); i++) {
                filterBuilder.append("[0:").append(streamId).append("]");
                
                // If there are no filters to add utilise the 'null' filter
                if(filters.get(i).isEmpty()) {
                    filterBuilder.append("null");
                } else {
                    for(int f = 0; f < filters.get(i).size(); f++) {
                        filterBuilder.append(filters.get(i).get(f));
                        
                        if(f < (filters.get(i).size() - 1)) {
                            filterBuilder.append(",");
                        }
                    }
                }
            
                filterBuilder.append("[v").append(i).append("]");
                
                if(i < (filters.size() - 1)) {
                    filterBuilder.append(";");
                }
            }
            
            commands.add(filterBuilder.toString());
        }
        
        return commands;
    }
    
    public boolean processSubtitles(TranscodeProfile transcodeProfile, MediaElement mediaElement) {
        // Check variables
        if(mediaElement == null) {
            return false;
        }
        
        // Check this is a video element
        if(mediaElement.getType() != MediaElementType.VIDEO) {
            return false;
        }
        
        // If there are no streams to process we are done
        if(mediaElement.getSubtitleStreams() == null) {
            return true;
        }
        
        // Process each subtitle stream
        List<SubtitleTranscode> transcodes = new ArrayList<>();
        
        for(SubtitleStream stream : mediaElement.getSubtitleStreams()) {
            int codec;
            
            if(transcodeProfile.getEncoder().isSupported(stream.getCodec())) {
                codec = SMS.Codec.COPY;
            } else {
                switch(stream.getCodec()) {
                    // Text Based
                    case SMS.Codec.SUBRIP: case SMS.Codec.WEBVTT:
                        codec = SMS.Codec.WEBVTT;
                        break;

                    default:
                        codec = SMS.Codec.UNSUPPORTED;
                        break;
                }
            }
            
            // Check we can transcode this stream
            if(codec == SMS.Codec.UNSUPPORTED) {
                continue;
            }
            
            // Enable forced subtitles by default
            if(stream.isForced() && transcodeProfile.getSubtitleStream() == null) {
                transcodeProfile.setSubtitleStream(stream.getStreamId());
            }
            
            transcodes.add(new SubtitleTranscode(stream.getStreamId(), stream.getCodec(), codec));
        }
        
        // Update profile
        transcodeProfile.setSubtitleTranscodes(transcodes.toArray(new SubtitleTranscode[transcodes.size()]));
        
        return true;
    }
    
    public boolean processVideo(TranscodeProfile transcodeProfile, ClientProfile clientProfile, MediaElement mediaElement) {
        // Check variables
        if(mediaElement == null || clientProfile.getCodecs() == null || clientProfile.getFormat() == null || clientProfile.getVideoQuality() == null) {
            return false;
        }
        
        // Check this is a video element
        if(mediaElement.getType() != MediaElementType.VIDEO) {
            return false;
        }
        
        // Set video stream if necessary
        if(transcodeProfile.getVideoStream() == null) {
            if(mediaElement.getVideoStreams().size() > 0) {
                transcodeProfile.setVideoStream(mediaElement.getVideoStreams().get(0).getStreamId());
            } else {
                return true;
            }
        }
        
        // Process required number of video streams
        List<VideoTranscode> transcodes = new ArrayList<>();
        
        for(VideoStream stream : mediaElement.getVideoStreams()) {
            int streamCount = AdaptiveStreamingService.DEFAULT_STREAM_COUNT;
            int maxQuality = TranscodeUtils.getHighestVideoQuality(stream.getResolution());
            
            // Process quality
            if(maxQuality < 0 || maxQuality > clientProfile.getVideoQuality()) {
                maxQuality = clientProfile.getVideoQuality();
            }
            
            // Determine number of streams to transcode
            if(transcodeProfile.getType() < TranscodeProfile.StreamType.REMOTE || maxQuality == 0) {
                streamCount = 1;
            } else if(streamCount > clientProfile.getVideoQuality()) {
                streamCount = maxQuality;
            }
            
            // Test if transcoding is necessary
            int transcodeReason = TranscodeUtils.isTranscodeRequired(clientProfile, mediaElement, stream);
            
            // Test for missing required stream data
            if(transcodeReason == SMS.TranscodeReason.NONE) {
                if(stream.getGOPSize() == null || stream.getGOPSize() == 0 || stream.getFPS() == null || stream.getFPS() == 0) {
                    transcodeReason = SMS.TranscodeReason.MISSING_DATA;
                }
            }
            
            if(transcodeReason == SMS.TranscodeReason.NONE) {
                if(!transcodeProfile.getEncoder().isSupported(stream.getCodec())) {
                    transcodeReason = SMS.TranscodeReason.CODEC_UNSUPPORTED_BY_ENCODER;
                }
            }
            
            for(int i = 0; i < streamCount; i++) {
                Integer codec;
                Dimension resolution = null;
                Integer quality = maxQuality;
                Integer maxBitrate = clientProfile.getMaxBitrate();

                if(transcodeReason > SMS.TranscodeReason.NONE) {
                    // Determine quality for transcode
                    if(i > 0) {
                        quality = i - 1;
                    }

                    // Get suitable codec
                    codec = transcodeProfile.getEncoder().getVideoCodec(clientProfile.getCodecs());

                    // Check we got a suitable codec
                    if(codec == SMS.Codec.UNSUPPORTED) {
                        return false;
                    }

                    // Get suitable resolution (use native resolution if direct play is enabled)
                    if(!clientProfile.getDirectPlay()) {
                        resolution = TranscodeUtils.getVideoResolution(stream.getResolution(), quality);      
                    }
                    
                    // For remote streams set our default max bitrate
                    if(transcodeProfile.getType() == TranscodeProfile.StreamType.REMOTE) {
                        maxBitrate = TranscodeUtils.VIDEO_QUALITY_MAX_BITRATE[quality];
                    }
                } else {
                    codec = SMS.Codec.COPY;
                    quality = null;
                    maxBitrate = null;
                }

                // Add video transcode to array
                transcodes.add(new VideoTranscode(stream.getStreamId(), stream.getCodec(), codec, resolution, quality, maxBitrate, transcodeReason));
            }
        }
        
        // Update profile with video transcode properties
        transcodeProfile.setVideoTranscodes(transcodes.toArray(new VideoTranscode[transcodes.size()]));
        
        return true;
    }
    
    public boolean processAudio(TranscodeProfile transcodeProfile, ClientProfile clientProfile, MediaElement mediaElement) {
        // Check variables
        if(mediaElement == null || clientProfile.getCodecs() == null || clientProfile.getAudioQuality() == null) {
            return false;
        }
        
        // If there are no audio streams to process we are done
        if(mediaElement.getAudioStreams() == null || mediaElement.getAudioStreams().isEmpty()) {
            return true;
        }
        
        // Set default audio stream if necessary
        if(transcodeProfile.getAudioStream() == null) {
            boolean streamFound = false;
            
            for(AudioStream stream : mediaElement.getAudioStreams()) {
                if(stream.isDefault()) {
                    transcodeProfile.setAudioStream(stream.getStreamId());
                    streamFound = true;
                    break;
                }
            }
            
            // If we still don't have a default stream just pick the first...
            if(!streamFound) {
                transcodeProfile.setAudioStream(mediaElement.getAudioStreams().get(0).getStreamId());
            }
        }
        
        // Check a format is specified for video transcode
        if(mediaElement.getType() == MediaElementType.VIDEO && clientProfile.getFormat() == null) {
            return false;
        }
            
        // Process each audio stream
        List<AudioTranscode> transcodes = new ArrayList<>();

        mediaElement.getAudioStreams().forEach((AudioStream stream) -> {
            Integer codec;
            int bitrate = -1;
            Integer sampleRate = null;
            int numChannels = stream.getChannels();
            Float replaygain = null;
            
            // Check if transcoding is required
            boolean transcodeRequired = TranscodeUtils.isTranscodeRequired(clientProfile, mediaElement, stream);
            
            // Check the format supports this codec for video or that we can stream this codec for audio
            if(!transcodeRequired) {
                if(clientProfile.getFormat() != null) {
                    transcodeRequired = !transcodeProfile.getEncoder().isSupported(stream.getCodec());
                }
            }
            
            if(!transcodeRequired) {
                codec = SMS.Codec.COPY;
            } else {
                // Check multichannel codecs
                if(clientProfile.getMchCodecs() == null || clientProfile.getMchCodecs().length == 0) {
                    numChannels = 2;
                }
                
                // Combine supported codecs
                Integer[] codecs = ArrayUtils.addAll(clientProfile.getCodecs(), clientProfile.getMchCodecs());
                codec = transcodeProfile.getEncoder().getAudioCodec(codecs, numChannels, clientProfile.getAudioQuality());
                
                // Check audio parameters for codec
                if(codec != SMS.Codec.UNSUPPORTED) {
                    // Sample rate
                    if((stream.getSampleRate() > clientProfile.getMaxSampleRate()) || (stream.getSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec))) {
                        sampleRate = (clientProfile.getMaxSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec)) ? TranscodeUtils.getMaxSampleRateForCodec(codec) : clientProfile.getMaxSampleRate();
                    }
                    
                    // Channels
                    if(numChannels > TranscodeUtils.getMaxChannelsForCodec(codec)) {
                        numChannels = TranscodeUtils.getMaxChannelsForCodec(codec);
                    }
                    
                    // Bitrate
                    if(!MediaUtils.isLossless(codec)) {
                        if(mediaElement.getType() == MediaElementType.AUDIO) {
                            bitrate = TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[clientProfile.getAudioQuality()];
                        } else if (mediaElement.getType() == MediaElementType.VIDEO) {
                            bitrate = TranscodeUtils.VIDEO_QUALITY_AUDIO_BITRATE[clientProfile.getVideoQuality()];
                        }

                        bitrate = new Double(bitrate  * (numChannels * 0.5)).intValue();
                    }
                    
                    // Replaygain
                    if(clientProfile.getReplaygain() != null) {
                        if(clientProfile.getReplaygain() == SMS.ReplaygainMode.ALBUM || clientProfile.getReplaygain() == SMS.ReplaygainMode.NATIVE_ALBUM) {
                            if(mediaElement.getReplaygainAlbum() != null && mediaElement.getReplaygainAlbum() != 0f) {
                                replaygain = mediaElement.getReplaygainAlbum();
                            }
                        }

                        if(clientProfile.getReplaygain() == SMS.ReplaygainMode.TRACK || clientProfile.getReplaygain() == SMS.ReplaygainMode.NATIVE_TRACK) {
                            if(mediaElement.getReplaygainTrack() != null && mediaElement.getReplaygainTrack() != 0f) {
                                replaygain = mediaElement.getReplaygainTrack();
                            }
                        }
                    }
                }
            }
            
            // Add transcode properties to array
            transcodes.add(new AudioTranscode(stream.getStreamId(), stream.getCodec(), codec, bitrate, sampleRate, numChannels, replaygain));
        });
        
        // Update profile with audio transcode properties
        transcodeProfile.setAudioTranscodes(transcodes.toArray(new AudioTranscode[transcodes.size()]));
        
        return true;
    }
}