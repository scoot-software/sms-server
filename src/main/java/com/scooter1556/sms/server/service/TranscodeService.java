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
            if(profile.getVideoTranscodes() != null) {
                HardwareAccelerator hardwareAccelerator = null;
            
                // Software or hardware based transcoding
                if(accelerators != null && accelerators.size() > i) {
                    hardwareAccelerator = accelerators.get(i);
                }
                
                // Base filters
                byte memory = getVideoBaseFilters(hardwareAccelerator, profile.getMaxResolution(), profile.getVideoTranscodes()[0].getOriginalCodec(), profile.getTonemapping(), commands.get(i).getVideoBaseFilters());
                
                // Video encode filters
                for(int v = 0; v < profile.getVideoTranscodes().length; v++) {
                    // If we are copying the stream continue with the next transcode
                    if(profile.getVideoTranscodes()[v].getCodec() == SMS.Codec.COPY) {
                        continue;
                    }
                    
                    // Add a filter list for video transcode
                    commands.get(i).getVideoEncodeFilters().add(new ArrayList<>());
                    commands.get(i).getVideoEncodeFilters().get(v).addAll(getVideoEncodingFilters(hardwareAccelerator, profile.getVideoTranscodes()[v].getResolution(), profile.getVideoTranscodes()[v].getCodec(), memory));
                }
                
                // Hardware decoding
                if(hardwareAccelerator != null && hardwareAccelerator.isDecodeCodecSupported(profile.getVideoTranscodes()[0].getOriginalCodec())) {
                    commands.get(i).getCommands().addAll(getHardwareAccelerationCommands(hardwareAccelerator, profile.getVideoTranscodes()[0].getOriginalCodec(), profile.getTonemapping()));
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
                commands.get(i).getCommands().addAll(getFilterCommands(profile.getVideoStream(), commands.get(i).getVideoBaseFilters(), commands.get(i).getVideoEncodeFilters()));
                
                for(int v = 0; v < profile.getVideoTranscodes().length; v++) {
                    VideoTranscode transcode = profile.getVideoTranscodes()[v];
                    
                    // Stream copy
                    if(transcode.getCodec() == SMS.Codec.COPY) {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("0:" + transcode.getId());
                        
                        // Codec
                        commands.get(i).getCommands().addAll(getVideoEncodingCommands(null, transcode.getCodec(), null, null));
                    } else {
                        // Map video stream
                        commands.get(i).getCommands().add("-map");
                        commands.get(i).getCommands().add("[v" + v + "]");

                        // Encoding
                        commands.get(i).getCommands().addAll(getVideoEncodingCommands(hardwareAccelerator, transcode.getCodec(), transcode.getQuality(), transcode.getMaxBitrate()));

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
    
    private Collection<String> getHardwareAccelerationCommands(HardwareAccelerator hardwareAccelerator, int codec, boolean tonemapping) {
        Collection<String> commands = new LinkedList<>();

        switch(hardwareAccelerator.getType()) {
            case SMS.Accelerator.INTEL:
                if(hardwareAccelerator.isDecodeCodecSupported(codec)) {
                    commands.add("-init_hw_device");
                    commands.add("vaapi=va:" + hardwareAccelerator.getDevice());
                    
                    commands.add("-hwaccel");
                    commands.add("vaapi");
                    
                    commands.add("-hwaccel_device");
                    commands.add("va");
                 
                    commands.add("-hwaccel_output_format");
                    commands.add("vaapi");
                    
                    commands.add("-filter_hw_device");
                    commands.add("va");
                }
                
                break;

            case SMS.Accelerator.NVIDIA:
                if(hardwareAccelerator.isDecodeCodecSupported(codec)) {
                    commands.add("-hwaccel");
                    commands.add("nvdec");
                    
                    commands.add("-hwaccel_device");
                    commands.add(hardwareAccelerator.getDevice());
                    
                    commands.add("-hwaccel_output_format");
                    commands.add("cuda");
                }
                
                // Setup OpenCL if needed
                if(tonemapping && hardwareAccelerator.getTonemapping() == SMS.Tonemap.OPENCL && hardwareAccelerator.getOCLDevice() != null) {
                    commands.add("-init_hw_device");
                    commands.add("opencl=ocl:" + hardwareAccelerator.getOCLDevice());
                    commands.add("-filter_hw_device");
                    commands.add("ocl");
                }
                
                break;
        }
        
        return commands;
    }
    
    /*
     * Returns a list of commands for a given hardware accelerator.
     */
    private Collection<String> getVideoEncodingCommands(HardwareAccelerator hardwareAccelerator, int codec, Integer quality, Integer maxrate) {
        Collection<String> commands = new LinkedList<>();
        
        commands.add("-c:v");
        
        if(hardwareAccelerator == null || !hardwareAccelerator.isEncodeCodecSupported(codec)) {
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
                    commands.add("-maxrate:v");
                    commands.add(maxrate.toString() + "k");
                    commands.add("-bufsize");
                    commands.add("2M");
                }

                break;
                
            case SMS.Codec.HEVC_MAIN:
                commands.add("libx265");
                commands.add("-crf");
                commands.add("28");
                commands.add("-preset");
                commands.add("ultrafast");
                commands.add("-pix_fmt");
                commands.add("yuv420p");

                if(maxrate != null) {
                    commands.add("-maxrate:v");
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
        }
        
        else {
            switch(hardwareAccelerator.getType()) {
                case SMS.Accelerator.INTEL:                    
                    if(codec == SMS.Codec.AVC_BASELINE || codec == SMS.Codec.AVC_MAIN || codec == SMS.Codec.AVC_HIGH) {
                        
                        commands.add("h264_vaapi");
                        
                        if(quality != null) {
                            commands.add("-b:v");
                            commands.add(String.valueOf(TranscodeUtils.getAverageBitrateForCodec(codec, quality)) + "k");
                        }
                        
                        //  Profile
                        commands.add("-profile:v");

                        switch (codec) {
                            case SMS.Codec.AVC_BASELINE:
                                commands.add("constrained_baseline");
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
                            commands.add("-maxrate:v");
                            commands.add(maxrate.toString() + "k");
                            commands.add("-bufsize");
                            commands.add("2M");
                        }
                        
                        break;
                    }
                    
                    else if(codec == SMS.Codec.HEVC_MAIN) {
                        commands.add("hevc_vaapi");
                        
                        if(quality != null) {
                            commands.add("-b:v");
                            commands.add(String.valueOf(TranscodeUtils.getAverageBitrateForCodec(codec, quality)) + "k");
                        }
                        
                        if(maxrate != null) {
                            commands.add("-maxrate:v");
                            commands.add(maxrate.toString() + "k");
                            commands.add("-bufsize");
                            commands.add("2M");
                        }
                        
                        break;
                    }

                case SMS.Accelerator.NVIDIA:                    
                    if(codec == SMS.Codec.AVC_BASELINE || codec == SMS.Codec.AVC_MAIN || codec == SMS.Codec.AVC_HIGH) {
                        commands.add("h264_nvenc");
                        
                        //  Profile
                        commands.add("-profile:v");

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
                    
                        commands.add("-rc:v");
                        commands.add("vbr_hq");
                        commands.add("-cq:v");
                        commands.add("23");
                    }
                    
                    else if(codec == SMS.Codec.HEVC_MAIN) {
                        commands.add("hevc_nvenc");
                        commands.add("-rc:v");
                        commands.add("vbr_hq");
                        commands.add("-cq:v");
                        commands.add("25");
                    }
                    
                    commands.add("-gpu");
                    commands.add(hardwareAccelerator.getDevice());
                    
                    if(maxrate != null) {
                        commands.add("-maxrate:v");
                        commands.add(maxrate.toString() + "k");
                    }
                    
                    commands.add("-preset");
                    commands.add("fast");
                    
                    
                    break;
            }
        }
        
        return commands;
    }
    
    /*
     * Returns a list of base video filters.
     */
    private byte getVideoBaseFilters(HardwareAccelerator hardwareAccelerator, Dimension resolution, int codec, boolean tonemapping, List<String> filters) {
        // Track memory where frames are stored through filter chain
        byte memory = SMS.Memory.NONE;
        
        if(hardwareAccelerator == null) {
            memory = SMS.Memory.SYSTEM;
        } else if(hardwareAccelerator.isDecodeCodecSupported(codec)) {
            memory = SMS.Memory.HARDWARE;
        }

        // Resolution
        if(resolution != null) {
            if(memory == SMS.Memory.SYSTEM) {
                filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
            }

            else if(memory == SMS.Memory.HARDWARE) {
                switch(hardwareAccelerator.getType()) {
                    case SMS.Accelerator.INTEL:
                        filters.add("scale_vaapi=w=" + resolution.width + ":h=" + resolution.height);
                        break;
                        
                    case SMS.Accelerator.NVIDIA:
                        if(getTranscoder().hasCuda()) {
                            filters.add("scale_cuda=" + resolution.width + ":" + resolution.height);
                        } else {
                            filters.add("hwdownload");

                            switch(codec) {
                                case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH: case SMS.Codec.HEVC_MAIN:
                                    filters.add("format=nv12");
                                    break;

                                case SMS.Codec.AVC_HIGH10: case SMS.Codec.HEVC_MAIN10: case SMS.Codec.HEVC_HDR10:
                                    filters.add("format=p010");
                            }

                            filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);

                            memory = SMS.Memory.SYSTEM;
                        }

                        break;
                }
            }
        }
        
        // Tonemapping
        if(tonemapping) {
            if(hardwareAccelerator == null || hardwareAccelerator.getTonemapping() == SMS.Tonemap.NONE) {
                // Check we can software tonemap
                if(getTranscoder().hasZscale() && getTranscoder().hasTonemap()) {
                    if(memory == SMS.Memory.HARDWARE) {
                        filters.add("hwdownload");
                        filters.add("format=p010");
                    }

                    // Tonemap
                    filters.add("zscale=transfer=linear");
                    filters.add("tonemap=hable");
                    filters.add("zscale=transfer=bt709");
                    filters.add("format=nv12");

                    memory = SMS.Memory.SYSTEM;
                }
            }

            else {
                if(hardwareAccelerator.getTonemapping() == SMS.Tonemap.OPENCL) {
                    if(memory == SMS.Memory.HARDWARE) {
                        filters.add("hwdownload");
                    }

                    filters.add("format=p010");

                    // Tonemap OpenCL
                    filters.add("hwupload");
                    filters.add("tonemap_opencl=t=bt709:tonemap=hable:format=nv12");
                    filters.add("hwdownload");
                    filters.add("format=nv12");

                    memory = SMS.Memory.SYSTEM;
                }
            }
        }
        
        return memory;
    }
    
    /*
     * Returns a list of video filters for a given encode.
     */
    private List<String> getVideoEncodingFilters(HardwareAccelerator hardwareAccelerator, Dimension resolution, int codec, byte memory) {
        List<String> filters = new ArrayList<>();
        
        if(hardwareAccelerator == null || !hardwareAccelerator.isEncodeCodecSupported(codec)) {
            if(memory == SMS.Memory.HARDWARE) {
                filters.add("hwdownload");

                switch(codec) {
                    case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH: case SMS.Codec.HEVC_MAIN:
                        filters.add("format=nv12");
                        break;

                    case SMS.Codec.AVC_HIGH10: case SMS.Codec.HEVC_MAIN10: case SMS.Codec.HEVC_HDR10:
                        filters.add("format=p010");
                }

            }
            
            if(resolution != null) {
                filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
            }
        }
        
        else {
            switch(hardwareAccelerator.getType()) {
                case SMS.Accelerator.INTEL:
                    if(memory == SMS.Memory.SYSTEM) {
                        filters.add("hwupload");
                    }
                    
                    if(resolution != null) {
                        filters.add("scale_vaapi=w=" + resolution.width + ":h=" + resolution.height);
                    }
                    
                    break;

                case SMS.Accelerator.NVIDIA:
                    if(resolution != null) {
                        if(getTranscoder().hasCuda()) {
                            if(memory == SMS.Memory.SYSTEM) {
                                filters.add("hwupload_cuda");
                            }
                            
                            filters.add("scale_cuda=" + resolution.width + ":" + resolution.height);
                        } else {
                            if(memory == SMS.Memory.HARDWARE) {
                                filters.add("hwdownload");

                                switch(codec) {
                                    case SMS.Codec.AVC_BASELINE: case SMS.Codec.AVC_MAIN: case SMS.Codec.AVC_HIGH: case SMS.Codec.HEVC_MAIN:
                                        filters.add("format=nv12");
                                        break;

                                    case SMS.Codec.AVC_HIGH10: case SMS.Codec.HEVC_MAIN10: case SMS.Codec.HEVC_HDR10:
                                        filters.add("format=p010");
                                }

                            }
                            
                            filters.add("scale=w=" + resolution.width + ":h=" + resolution.height);
                        }
                    }
                    
                    break;
            }
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
    
    public Collection<String> getFilterCommands(int streamId, ArrayList<String> baseFilters, ArrayList<ArrayList<String>> videoEncodeFilters) {
        Collection<String> commands = new LinkedList<>();
        
        if(baseFilters.isEmpty() && videoEncodeFilters.isEmpty()) {
            return commands;
        }
        
        commands.add("-filter_complex");

        StringBuilder filterBuilder = new StringBuilder();
        
        filterBuilder.append("[0:").append(streamId).append("]");
        
        // Base filters
        if(baseFilters.size() > 0) {
            for(int i = 0; i < baseFilters.size(); i++) {
                filterBuilder.append(baseFilters.get(i));

                if(i < (baseFilters.size() - 1)) {
                    filterBuilder.append(",");
                }
            }
            
            filterBuilder.append("[base];[base]");
        }
        
        // Split stream
        filterBuilder.append("split=").append(videoEncodeFilters.size());
        
        for(int i = 0; i < videoEncodeFilters.size(); i++) {
            filterBuilder.append("[i").append(i).append("]");
        }
        
        filterBuilder.append(";");

        // Add each filter chain in turn
        for(int i = 0; i < videoEncodeFilters.size(); i++) {
            filterBuilder.append("[i").append(i).append("]");
            
            // If there are no filters to add utilise the 'null' filter
            if(videoEncodeFilters.get(i).isEmpty()) {
                filterBuilder.append("null");
            } else {
                for(int f = 0; f < videoEncodeFilters.get(i).size(); f++) {
                    filterBuilder.append(videoEncodeFilters.get(i).get(f));

                    if(f < (videoEncodeFilters.get(i).size() - 1)) {
                        filterBuilder.append(",");
                    }
                }
            }

            filterBuilder.append("[v").append(i).append("]");

            if(i < (videoEncodeFilters.size() - 1)) {
                filterBuilder.append(";");
            }
        }

        commands.add(filterBuilder.toString());
        
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
            
            if(transcodeProfile.getFormat().isSupported(stream.getCodec())) {
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
        
        VideoStream stream = mediaElement.getVideoStreams().get(transcodeProfile.getVideoStream());
        
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
            if(!transcodeProfile.getFormat().isSupported(stream.getCodec())) {
                transcodeReason = SMS.TranscodeReason.CODEC_NOT_SUPPORTED_BY_FORMAT;
            }
        }
        
        if(transcodeReason > SMS.TranscodeReason.NONE) {
            if(transcodeProfile.getType() == TranscodeProfile.StreamType.REMOTE || !clientProfile.getDirectPlay()) {
                transcodeProfile.setMaxResolution(TranscodeUtils.getVideoResolution(stream.getResolution(), maxQuality));
            }
        }
        
        // Check if tonemapping is required
        if(stream.getCodec() == SMS.Codec.HEVC_HDR10) {
            transcodeProfile.setTonemapping(true);
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
                codec = transcodeProfile.getFormat().getVideoCodec(clientProfile.getCodecs());

                // Check we got a suitable codec
                if(codec == SMS.Codec.UNSUPPORTED) {
                    return false;
                }

                // Get suitable resolution and bitrate if necessary (use native resolution if direct play is enabled)
                if(transcodeProfile.getType() == TranscodeProfile.StreamType.REMOTE || !clientProfile.getDirectPlay()) {
                    resolution = TranscodeUtils.getVideoResolution(stream.getResolution(), quality);
                    
                    // Check if the resolution for this stream is the same as our master resolution
                    if(resolution != null && transcodeProfile.getMaxResolution() != null) {
                        if(TranscodeUtils.compareDimensions(resolution, transcodeProfile.getMaxResolution()) == 2) {
                            resolution = null;
                        }
                    }
                    
                    // Set default max bitrate
                    maxBitrate = TranscodeUtils.getMaxBitrateForCodec(codec, quality);
                }
            } else {
                codec = SMS.Codec.COPY;
                quality = null;
                maxBitrate = null;
            }

            // Add video transcode to array
            transcodes.add(new VideoTranscode(stream.getStreamId(), stream.getCodec(), codec, resolution, quality, maxBitrate, transcodeReason));
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
                    transcodeRequired = !transcodeProfile.getFormat().isSupported(stream.getCodec());
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
                codec = transcodeProfile.getFormat().getAudioCodec(codecs, numChannels, clientProfile.getAudioQuality());
                
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