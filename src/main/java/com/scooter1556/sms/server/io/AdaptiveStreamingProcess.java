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
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.utilities.MediaUtils;
import java.io.File;
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

public class AdaptiveStreamingProcess extends SMSProcess implements Runnable {
    
    private static final String CLASS_NAME = "AdaptiveStreamingProcess";
    
    File streamDirectory = null;
    int segmentNum = 0;
    int subtitleNum = 0;
    boolean subtitlesEnabled = false;
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
            
            //  Setup thread pool for post-processing segments
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
        
        // Stop post-processing pool
        if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
            postProcessExecutor.shutdownNow();
        }
               
        try {
            // Wait for process to finish
            if(process != null) {
                process.waitFor();
            }
            
            // Cleanup working directory
            if(streamDirectory != null && streamDirectory.isDirectory()) {
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
            // Generate post-process command
            List<String> command = new  ArrayList<>();
            command.add(transcoder.getPath().toString());
            command.add("-i");
            command.add(segment.getAbsolutePath());
            command.add("-copyts");
            
            if(profile.getVideoTranscodes() != null) {
                for(int i = 0; i < profile.getVideoTranscodes().length; i++) {                    
                    command.add("-map");
                    command.add("0:v:" + i);
                    
                    command.add("-c");
                    command.add("copy");
                    
                    command.add("-f");
                    command.add("mpegts");
                    
                    String path = segment.getAbsolutePath() + "-video-" + i + ".tmp";
                    
                    command.add(path);
                    
                    // Add to segment list
                    segmentPaths.add(path);
                }
            }
            
            if(profile.getAudioTranscodes() != null) {
                for(int i = 0; i < profile.getAudioTranscodes().length; i++) {
                    AudioTranscode transcode = profile.getAudioTranscodes()[i];
                    
                    // Determine format to use
                    int format = SMS.Format.MPEGTS;
                    int codec = transcode.getCodec();
                    
                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }
                    
                    if(!MediaUtils.isCodecSupportedByFormat(SMS.Format.MPEGTS, codec) || profile.getPackedAudio()) {
                        format = MediaUtils.getFormatForCodec(codec);
                    }
                    
                    command.add("-map");
                    command.add("0:a:" + i);
                    
                    command.add("-c");
                    command.add("copy");
                    
                    command.add("-f");
                    command.add(MediaUtils.getFormat(format));
                    
                    String path = segment.getAbsolutePath() + "-audio-" + i + ".tmp";
                    
                    command.add(path);
                    
                    // Add to segment list
                    segmentPaths.add(path);
                }
            }
            
            /*
            if(profile.getSubtitleTranscodes() != null) {
                for(int i = 0; i < profile.getSubtitleTranscodes().length; i++) {
                    SubtitleTranscode transcode = profile.getSubtitleTranscodes()[i];
                    
                    // Determine format to use
                    int codec = transcode.getCodec();
                    
                    if(codec == SMS.Codec.HARDCODED) {
                        continue;
                    }
                    
                    if(codec == SMS.Codec.COPY) {
                        codec = transcode.getOriginalCodec();
                    }
                    
                    int format = MediaUtils.getFormatForCodec(codec);
                    
                    command.add("-map");
                    command.add("0:s:" + i);
                    
                    command.add("-c");
                    command.add("copy");
                    
                    command.add("-f");
                    command.add(MediaUtils.getFormat(format));
                    
                    String path = segment.getAbsolutePath() + "-subtitle-" + i + ".tmp";
                    
                    command.add(path);
                    
                    // Add to segment list
                    segmentPaths.add(path);
                }
            }*/
            
            LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, StringUtils.join(command, " "), null);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            postProcess = processBuilder.start();
            new NullStream(postProcess.getInputStream()).start();
            
            // Wait for process to finish
            postProcess.waitFor();
            
            // Rename temporary files once complete
            segmentPaths.stream().map((path) -> new File(path)).forEachOrdered((tmpSegment) -> {
                File finalSegment = new File(FilenameUtils.getFullPath(tmpSegment.getPath()) + FilenameUtils.getBaseName(tmpSegment.getPath()));

                if(tmpSegment.exists()) {
                    tmpSegment.renameTo(finalSegment);
                } else {
                    LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to rename post-processed segment: " + tmpSegment.toString(), null);
                }
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
    
    public void setSegmentNum(int num) {
        this.segmentNum = num;
    }
    
    public int getSegmentNum() {
        return this.segmentNum;
    }
    
    public void setSubtitleNum(int num) {
        this.subtitleNum = num;
    }
    
    public int getSubtitleNum() {
        return this.subtitleNum;
    }
    
    public void setSubtitlesEnabled(boolean enabled) {
        this.subtitlesEnabled = enabled;
    }
    
    public boolean getSubtitlesEnabled() {
        return this.subtitlesEnabled;
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
            
            //  Stop segment tracking
            tailer.stop();
            
            if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
                postProcessExecutor.shutdownNow();
            }
                        
            ended = true;
        }
    }
}