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
import com.scooter1556.sms.server.domain.AudioTranscode.AudioQuality;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TranscodeService {

    @Autowired
    private AdaptiveStreamingService adaptiveStreamingService;
 
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
        
        // Get available hardware accelerators
        List<HardwareAccelerator> accelerators = transcoder.getHardwareAcceleratorOptions(profile.getType() == TranscodeProfile.StreamType.REMOTE);
        
        // Determine number of potential transcode commands to generate
        int transcodeCommands = 1; 
        
        if(accelerators != null && profile.getVideoTranscodes() != null) {
            transcodeCommands += accelerators.size();
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
                boolean hardcodedSubtitles = false;
            
                // Software or hardware based transcoding
                if(accelerators != null && accelerators.size() > i) {
                    hardwareAccelerator = accelerators.get(i);
                }
                
                // Check for hardcoded subtitles
                if(profile.getSubtitleStream() != null) {
                    SubtitleTranscode transcode = TranscodeUtils.getSubtitleTranscodeById(profile.getSubtitleTranscodes(), profile.getSubtitleStream());
                    
                    if(transcode != null) {
                        hardcodedSubtitles = transcode.isHardcoded();
                        
                        // Don't decode in hardware if burning in subtitles
                        if(hardwareAccelerator != null) {
                            hardwareAccelerator.setDecodingSupported(!hardcodedSubtitles);
                        }
                    }
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
                    commands.get(i).getFilters().add(new ArrayList<String>());
                    
                    // Burn in subtitles if required
                    if(hardcodedSubtitles) {
                        commands.get(i).getFilters().get(v).add("[0:" + profile.getSubtitleStream() + "]overlay");
                    }
                    
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

                // Remove metadata
                commands.get(i).getCommands().add("-map_metadata");
                commands.get(i).getCommands().add("-1");
                
                // Copy Timestamps
                commands.get(i).getCommands().add("-copyts");
                
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
                        commands.get(i).getCommands().add("expr:gte(t,n_forced*2)");
                    }
                    
                    // Segment
                    commands.get(i).getCommands().addAll(getHlsCommands(job.getId(), "video-" + v, profile.getOffset()));
                }
            }

            // Audio
            if(profile.getAudioTranscodes() != null) {
                if(!initialised) {
                    // Input media file
                    commands.get(i).getCommands().add("-i");
                    commands.get(i).getCommands().add(job.getMediaElement().getPath());
                }
                
                // Enable experimental codecs
                commands.get(i).getCommands().add("-strict");
                commands.get(i).getCommands().add("experimental");
                
                for(int a = 0; a < profile.getAudioTranscodes().length; a++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[a];
                    String format = null;
                    
                    // Transcode commands
                    commands.get(i).getCommands().addAll(getAudioCommands(transcode));
                    
                    // Segment commands
                    commands.get(i).getCommands().addAll(getHlsCommands(job.getId(), "audio-" + a, profile.getOffset()));
                }
            }
        }
        
        // Prepare result
        String[][] result = new String[commands.size()][];
        
        for(int r = 0; r < commands.size(); r++) {
            result[r] = commands.get(r).getCommands().toArray(new String[0]);
        }
        
        return result;
    }
    
    private Collection<String> getHlsCommands(UUID id, String name, Integer offset) {
        if(id == null || name == null) {
            return null;
        }
        
        Collection<String> commands = new LinkedList<>();
        
        commands.add("-f");
        commands.add("hls");

        commands.add("-hls_time");
        commands.add(AdaptiveStreamingService.HLS_SEGMENT_DURATION.toString());

        commands.add("-hls_segment_type");
        commands.add("mpegts");

        if(offset != null && offset > 0) {
            commands.add("-start_number");
            commands.add(String.valueOf(offset / AdaptiveStreamingService.HLS_SEGMENT_DURATION));
        }

        commands.add("-hls_list_size");
        commands.add("0");

        commands.add("-hls_flags");
        commands.add("temp_file");

        commands.add("-hls_segment_filename");
        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/%d-" + name + ".ts");

        commands.add(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id + "/" + name + ".m3u8");
        
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
                if(codec == SMS.Codec.AVC_BASELINE) {
                    commands.add("baseline");
                } else if(codec == SMS.Codec.AVC_MAIN) {
                    commands.add("main");
                } else if(codec == SMS.Codec.AVC_HIGH){
                    commands.add("high");
                } else {
                    commands.add("high");
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
    
    public Boolean isTranscodeRequired(MediaElement mediaElement, ClientProfile profile) {
        // Make sure we have the information we require
        if(mediaElement == null || profile.getCodecs() == null) {
            return null;
        }
        
        // Check video codec
        if(mediaElement.getVideoStreams() != null) {
            // Check video quality is set
            if(profile.getVideoQuality() == null) {
                return null;
            }
            
            for(VideoStream stream : mediaElement.getVideoStreams()) {
                if(isTranscodeRequired(profile, mediaElement, stream)) {
                    return true;
                }
            }
        }
        
        // Check audio streams
        if(mediaElement.getAudioStreams() != null) {
            for(AudioStream stream : mediaElement.getAudioStreams()) {
                if(isTranscodeRequired(profile, mediaElement, stream)) {
                    return true;
                }
            }
        }

        // Check subtitle streams
        if(mediaElement.getSubtitleStreams() != null) {
            for(SubtitleStream stream : mediaElement.getSubtitleStreams()) {
                if(!getTranscoder().isCodecSupported(stream.getCodec())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean isTranscodeRequired(ClientProfile profile, MediaElement mediaElement, VideoStream stream) {
        if(!getTranscoder().isCodecSupported(stream.getCodec())) {
            return true;
        }
        
        // Check maximum bitrate
        if(profile.getMaxBitrate() != null) {
            if(MediaUtils.getMaxBitrate(stream, mediaElement.getBitrate()) > profile.getMaxBitrate()) {
                return true;
            }
        }

        // If client is not on the local network check stream parameters
        if(!profile.getLocal()) {
            // Check resolution
            if(TranscodeUtils.compareDimensions(new Dimension(stream.getWidth(), stream.getHeight()), TranscodeUtils.VIDEO_QUALITY_RESOLUTION[profile.getVideoQuality()]) == 1) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isTranscodeRequired(ClientProfile profile, MediaElement mediaElement, AudioStream stream) {
        // Check audio codec
        if(stream.getChannels() > 2) {
            if(profile.getMchCodecs() == null || !getTranscoder().isCodecSupported(stream.getCodec())) {
                return true;
            }
        } else {
            if(!getTranscoder().isCodecSupported(stream.getCodec())) {
                return true;
            }
        }

        // Check audio sample rate
        if(stream.getSampleRate() > profile.getMaxSampleRate() && stream.getCodec() != SMS.Codec.DSD) {
            return true;
        }

        // If client is not on the local network check stream parameters
        if(!profile.getLocal() || !profile.getDirectPlay()) {
            // Check bitrate
            int bitrate;
            
            if(mediaElement.getType() == MediaElementType.VIDEO) {
                bitrate = TranscodeUtils.VIDEO_QUALITY_AUDIO_BITRATE[profile.getVideoQuality()];
            } else {
                bitrate = TranscodeUtils.AUDIO_QUALITY_MAX_BITRATE[profile.getAudioQuality()];
            }
            
            //  Calculate overall bitrate to compare
            if(bitrate > 0) {
                bitrate = new Double(bitrate * (stream.getChannels() * 0.5)).intValue();
            }

            if(bitrate > 0 && stream.getBitrate() > 0 && stream.getBitrate() > bitrate) {
                return true;
            }
        }
        
        return false;
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
            Integer codec = null;
            boolean hardcode = false;
            
            if(transcodeProfile.getEncoder().isSupported(stream.getCodec())) {
                codec = SMS.Codec.COPY;
            } else {
                switch(stream.getCodec()) {
                    // Text Based
                    case SMS.Codec.SUBRIP: case SMS.Codec.WEBVTT:
                        codec = SMS.Codec.WEBVTT;
                        break;

                    // Picture Based
                    case SMS.Codec.DVD: case SMS.Codec.DVB: case SMS.Codec.PGS:
                        codec = stream.getCodec();
                        hardcode = true;
                        break;

                    default:
                        codec = SMS.Codec.COPY;
                        break;
                }
            }
            
            // Enable forced subtitles by default
            if(stream.isForced() && transcodeProfile.getSubtitleStream() == null) {
                transcodeProfile.setSubtitleStream(stream.getStreamId());
            }
            
            transcodes.add(new SubtitleTranscode(stream.getStreamId(), codec, hardcode));
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
            boolean transcodeRequired = isTranscodeRequired(clientProfile, mediaElement, stream);

            if(!transcodeRequired) {
                transcodeRequired = !transcodeProfile.getEncoder().isSupported(stream.getCodec());
            }

            // Check for hardcoded subtitles
            if(transcodeProfile.getSubtitleStream() != null) {
                SubtitleTranscode transcode = TranscodeUtils.getSubtitleTranscodeById(transcodeProfile.getSubtitleTranscodes(), transcodeProfile.getSubtitleStream());

                if(transcode != null) {
                    transcodeRequired = transcode.isHardcoded();
                }
            }

            for(int i = 0; i < streamCount; i++) {
                Integer codec = null;
                Dimension resolution = null;
                Integer quality = maxQuality;
                Integer maxBitrate = clientProfile.getMaxBitrate();
                
                // Determine max bitrate
                if(maxBitrate == null) {
                    maxBitrate = TranscodeUtils.VIDEO_QUALITY_MAX_BITRATE[quality];
                }

                if(transcodeRequired) {
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
                transcodes.add(new VideoTranscode(stream.getStreamId(), codec, resolution, quality, maxBitrate));
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

        for(AudioStream stream : mediaElement.getAudioStreams()) {
            Integer codec = null;
            int bitrate = -1;
            Integer sampleRate = null;
            int numChannels = stream.getChannels();
            
            // Check if transcoding is required
            boolean transcodeRequired = isTranscodeRequired(clientProfile, mediaElement, stream);
            
            // Check the format supports this codec for video or that we can stream this codec for audio
            if(!transcodeRequired) {
                if(clientProfile.getFormat() != null) {
                    transcodeRequired = !transcodeProfile.getEncoder().isSupported(stream.getCodec());
                }
            }
            
            if(!transcodeRequired) {
                // Work around transcoder bug where flac files have the wrong duration if the stream is copied
                codec = SMS.Codec.COPY;
            } else {
                // Check multichannel codecs
                if(clientProfile.getMchCodecs() == null) {
                    numChannels = 2;
                }
                
                codec = transcodeProfile.getEncoder().getAudioCodec(clientProfile.getCodecs(), numChannels);
                
                // Check audio parameters for codec
                if(codec != SMS.Codec.UNSUPPORTED) {
                    
                    // Sample rate
                    if((stream.getSampleRate() > clientProfile.getMaxSampleRate()) || (stream.getSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec))) {
                        sampleRate = (clientProfile.getMaxSampleRate() > TranscodeUtils.getMaxSampleRateForCodec(codec)) ? TranscodeUtils.getMaxSampleRateForCodec(codec) : clientProfile.getMaxSampleRate();
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
                }
            }
            
            // Add transcode properties to array
            transcodes.add(new AudioTranscode(stream.getStreamId(), stream.getCodec(), codec, bitrate, sampleRate, numChannels));
        }
        
        // Update profile with audio transcode properties
        transcodeProfile.setAudioTranscodes(transcodes.toArray(new AudioTranscode[transcodes.size()]));
        
        return true;
    }
}