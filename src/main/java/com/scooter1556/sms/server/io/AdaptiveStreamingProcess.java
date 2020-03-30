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
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.SMS;
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.SubtitleTranscode;
import com.scooter1556.sms.server.domain.TranscodeProfile;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.media.FragmentedMp4Builder;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.utilities.MediaUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.mp4parser.Container;

public class AdaptiveStreamingProcess extends SMSProcess implements Runnable {
    
    private static final String CLASS_NAME = "AdaptiveStreamingProcess";
    
    File streamDirectory = null;
    int segmentNum = 0;
    TranscodeProfile profile = null;
    MediaElement mediaElement = null;
    Transcoder transcoder = null;
    
    Tailer tailer = null;
    ExecutorService postProcessExecutor = null;

    public AdaptiveStreamingProcess() {};
    
    public AdaptiveStreamingProcess(UUID id) {
        this.id = id;
    }
    
    public void initialise() {
        // Stop transcode process if one is already running
        if(process != null) {
            process.destroy();
        }
        
        // Stop segment tracking if re-initialising
        if(tailer != null) {
            tailer.stop();
        }
        
        // Stop post-processing execution if already running
        if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
            postProcessExecutor.shutdownNow();
        }
        
        // Determine stream directory
        streamDirectory = new File(SettingsService.getInstance().getCacheDirectory().getPath() + "/streams/" + id);
        
        try {
            if(streamDirectory.exists()) {                
                // Wait for process to finish
                if(process != null) {
                    process.waitFor();
                }
                                
                FileUtils.cleanDirectory(streamDirectory);
            } else {
                boolean success = streamDirectory.mkdirs();

                if(!success) {
                    LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Unable to create directory " + streamDirectory.getPath(), null);
                    return;
                }
            }

            // Reset flags
            ended = false;
            
            // Setup thread pool for post-processing segments
            postProcessExecutor = Executors.newCachedThreadPool();
            
            // Setup tailer for segment list
            TailerListener listener = new SegmentListener();
            tailer = new Tailer(new File(streamDirectory + "/segments.txt"), listener);
            Thread thread = new Thread(tailer);
            thread.setDaemon(true);
            thread.start();
        
            // Start transcoding
            start();
        } catch(Exception ex) {
            if(process != null) {
                process.destroy();
            }
            
            ended = true;
            
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Error starting adaptive streaming process.", ex);
        }
    }
    
    @Override
    public void start() {
        new Thread(this).start();
    }
    
    @Override
    public void end() {
        // Stop transcode process
        if(process != null) {
            process.destroy();
        }
        
        //  Stop segment tracking
        tailer.stop();
        
        // Stop post-processing execution
        if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
            postProcessExecutor.shutdownNow();
        }
               
        try {
            // Wait for process to finish
            if(process != null) {
                process.waitFor();
            }
            
            // Cleanup working directory
            if(streamDirectory != null && streamDirectory.exists() && streamDirectory.isDirectory()) {
                FileUtils.deleteDirectory(streamDirectory);
            }
        } catch(InterruptedException ex) {
            // Do nothing...
        } catch(IOException ex) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to remove working directory for Adaptive Streaming job " + id, ex);
        }
        
        ended = true;
    }
    
    public class SegmentListener extends TailerListenerAdapter {
        @Override
        public void handle(String line) {
            // Check segment exists
            String segmentPath = streamDirectory + "/" + line;
            File segment = new File(segmentPath);
            
            if(!segment.exists()) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Segment does not exist: " + segmentPath, null);
                return;
            }
            
            postProcessExecutor.submit(() -> {
                postProcess(segment);
            });
      }
  }
    
    private void postProcess(File segment) {
        // Path to extracted stream segments
        List<String> segmentPaths = new ArrayList<>();
        
        // Process for transcoding
        Process postProcess = null;
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Post-processing segment: " + segment.getAbsolutePath(), null);
        
        try {
            // Determine format to use
            int vFormat = SMS.Format.UNSUPPORTED;
            int aFormat = SMS.Format.UNSUPPORTED;

            if(profile.getMuxer().getFormat() == SMS.Format.HLS_TS) {
                vFormat = SMS.Format.MPEGTS;
                aFormat = SMS.Format.MPEGTS;
            } else if(profile.getMuxer().getFormat() == SMS.Format.HLS_FMP4 || profile.getMuxer().getFormat() == SMS.Format.MPEG_DASH) {
                vFormat = SMS.Format.MP4;
                aFormat = SMS.Format.MP4;
            }

            // Generate post-process command
            List<String> command = new  ArrayList<>();
            
            if(profile.getVideoTranscodes() != null) {
                for(int i = 0; i < profile.getVideoTranscodes().length; i++) {
                    if(vFormat == SMS.Format.MPEGTS) {
                        if(command.isEmpty()) {
                            initialiseTranscode(command, segment.getAbsolutePath());
                        }
                        
                        command.add("-map");
                        command.add("0:v:" + i);

                        command.add("-c:v");
                        command.add("copy");
                    
                        command.add("-f");
                        command.add("mpegts");
                    
                        String path = segment.getParent() + "/" + i + "-video-" + segment.getName() + ".ts" + ".tmp";
                        command.add(path);
                        
                        // Add to segment list
                        segmentPaths.add(path);
                    } else if(vFormat == SMS.Format.MP4) {
                        File init = new File(segment.getParent() + "/" + i + "-video-init.mp4");
                        File tmpInit = new File(init.getPath() + ".tmp");
                        File newSegment = new File(segment.getParent() + "/" + i + "-video-" + segment.getName() + ".m4s.tmp");
                        
                        if(!init.exists()) {
                            FragmentedMp4Builder initBuilder = new FragmentedMp4Builder();
                            Container initContainer = initBuilder.build(segment.getAbsolutePath(), i, Integer.valueOf(segment.getName()), true);
                            FileOutputStream initfos = new FileOutputStream(tmpInit);
                            initContainer.writeContainer(initfos.getChannel());
                            initfos.close();
                            finaliseTmpFile(tmpInit);
                        }

                        FragmentedMp4Builder builder = new FragmentedMp4Builder();
                        Container container = builder.build(segment.getAbsolutePath(), i, Integer.valueOf(segment.getName()), false);
                        FileOutputStream fos = new FileOutputStream(newSegment);
                        container.writeContainer(fos.getChannel());
                        fos.close();
                        finaliseTmpFile(newSegment);
                    }
                }
            }
            
            if(profile.getSubtitleTranscodes() != null) {
                for(int i = 0; i < profile.getSubtitleTranscodes().length; i++) {
                    SubtitleTranscode transcode = profile.getSubtitleTranscodes()[i];
                    
                    // Determine format to use
                    int codec = transcode.getCodec();
                    
                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }
                    
                    int sFormat = MediaUtils.getFormatForCodec(codec);
                    
                    if(command.isEmpty()) {
                        initialiseTranscode(command, segment.getAbsolutePath());
                    }
                    
                    command.add("-map");
                    command.add("0:s:" + i);
                    
                    command.add("-c:s");
                    command.add(TranscodeUtils.getEncoderForCodec(codec));
                    
                    command.add("-f");
                    command.add(MediaUtils.getFormat(sFormat));
                    
                    String path = segment.getParent() + "/" + i + "-subtitle-" + segment.getName() + "." + MediaUtils.getExtensionForFormat(SMS.MediaType.SUBTITLE, sFormat) + ".tmp";
                    
                    command.add(path);
                    
                    // Add to segment list
                    segmentPaths.add(path);
                }
            }
            
            if(profile.getAudioTranscodes() != null) {
                for(int i = 0; i < profile.getAudioTranscodes().length; i++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[i];
                    
                    int codec = transcode.getCodec();
                    
                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }
                    
                    if(!MediaUtils.isCodecSupportedByFormat(aFormat, codec) || profile.getPackedAudio()) {
                        aFormat = MediaUtils.getFormatForCodec(codec);
                    }
                    
                    if(aFormat == SMS.Format.MP4) {
                        // Get track ID
                        int trackId = 
                                i 
                                + (profile.getVideoTranscodes() == null ? 0 : profile.getVideoTranscodes().length)
                                + (profile.getSubtitleTranscodes() == null ? 0 : profile.getSubtitleTranscodes().length);
                        
                        File init = new File(segment.getParent() + "/" + i + "-audio-init.mp4");
                        File tmpInit = new File(init.getPath() + ".tmp");
                        File newSegment = new File(segment.getParent() + "/" + i + "-audio-" + segment.getName() + ".m4s.tmp");
                        
                        if(!init.exists()) {
                            FragmentedMp4Builder initBuilder = new FragmentedMp4Builder();
                            Container initContainer = initBuilder.build(segment.getAbsolutePath(), trackId, Integer.valueOf(segment.getName()), true);
                            FileOutputStream initfos = new FileOutputStream(tmpInit);
                            initContainer.writeContainer(initfos.getChannel());
                            initfos.close();
                            finaliseTmpFile(tmpInit);
                        }

                        FragmentedMp4Builder builder = new FragmentedMp4Builder();
                        Container container = builder.build(segment.getAbsolutePath(), trackId, Integer.valueOf(segment.getName()), false);
                        FileOutputStream fos = new FileOutputStream(newSegment);
                        container.writeContainer(fos.getChannel());
                        fos.close();
                        finaliseTmpFile(newSegment);
                    } else {
                        if(command.isEmpty()) {
                            initialiseTranscode(command, segment.getAbsolutePath());
                        }

                        command.add("-map");
                        command.add("0:a:" + i);

                        command.add("-c:a");
                        command.add("copy");

                        command.add("-f");
                        command.add(MediaUtils.getFormat(aFormat));
                    
                        String path = segment.getParent() + "/" + i + "-audio-" + segment.getName() + "." + MediaUtils.getExtensionForFormat(SMS.MediaType.AUDIO, aFormat) + ".tmp";
                        command.add(path);
                        
                        // Add to segment list
                        segmentPaths.add(path);
                    }
                }
            }
            
            // Check if we need to start a transcode process
            if(!command.isEmpty()) {
                LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, StringUtils.join(command, " "), null);
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                postProcess = processBuilder.redirectErrorStream(true).start();
                new NullStream(postProcess.getInputStream()).start();
            
                // Wait for process to finish
                postProcess.waitFor();
            }
            
            // Rename temporary files once complete
            segmentPaths.stream().map((path) -> new File(path)).forEachOrdered((tmpSegment) -> {
                finaliseTmpFile(tmpSegment);
            });
            
            // Remove original segment
            if(segment.exists()) {
                segment.delete();
            }
        } catch(IOException ex) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to post-process segment: " + segment.getAbsolutePath(), ex);
        } catch(InterruptedException ex) {
            //Do nothing...
        } finally {
            if(postProcess != null) {
                postProcess.destroy();
            }
        }
    }
    
    private void initialiseTranscode(List<String> command, String path) {
        command.add(transcoder.getPath().toString());
        command.add("-y");

        command.add("-i");
        command.add(path);

        command.add("-copyts");
    }
    
    private void finaliseTmpFile(File tmp) {
        File finalised = new File(FilenameUtils.getFullPath(tmp.getPath()) + FilenameUtils.getBaseName(tmp.getPath()));

        if(tmp.exists()) {
            tmp.renameTo(finalised);
        } else {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to rename file: " + tmp.toString(), null);
        }
    }
    
    public void setSegmentNum(int num) {
        this.segmentNum = num;
    }
    
    public int getSegmentNum() {
        return this.segmentNum;
    }
    
    public void setTranscodeProfile(TranscodeProfile profile) {
        this.profile = profile;
    }
    
    public void setMediaElement(MediaElement element) {
        this.mediaElement = element;
    }
    
    public void setTranscoder(Transcoder transcoder) {
        this.transcoder = transcoder;
    }

    @Override
    public void run() {
        try {
            for(String[] command : commands) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, StringUtils.join(command, " "), null);
                
                // Clean stream directory
                FileUtils.cleanDirectory(streamDirectory);
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                process = processBuilder.start();
                new NullStream(process.getInputStream()).start();
                TranscodeAnalysisStream transcodeAnalysis = new TranscodeAnalysisStream(id, StringUtils.join(command, " "), process.getErrorStream());
                transcodeAnalysis.start();

                // Wait for process to finish
                int code = process.waitFor();
                
                LogService.getInstance().addLogEntry(Level.DEBUG, CLASS_NAME, "Transcode process exited with code " + code, null);

                // Check for error
                if(code == 0 || code == 255) {
                    LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Transcode finished for job " + id + " (fps=" + transcodeAnalysis.getFps() + ")", null);
                    break;
                } else {
                    LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Transcode command failed for job " + id + ". Attempting alternatives if available...", null);
                }
            }
        } catch(IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error occured whilst transcoding.", ex);
        } catch(InterruptedException ex) {
            // Do nothing...
        } finally {
            if(process != null) {
                process.destroy();
            }
                        
            ended = true;
        }
    }
}