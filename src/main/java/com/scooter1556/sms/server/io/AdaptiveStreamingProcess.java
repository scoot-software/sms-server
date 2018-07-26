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
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.utilities.DirectoryWatcher;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.LogService.Level;
import com.scooter1556.sms.server.service.SettingsService;
import com.scooter1556.sms.server.utilities.MediaUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;

public class AdaptiveStreamingProcess extends SMSProcess implements Runnable {
    
    private static final String CLASS_NAME = "AdaptiveStreamingProcess";
    
    File streamDirectory = null;
    int segmentNum = 0;
    int subtitleNum = 0;
    boolean subtitlesEnabled = false;
    boolean postProcessEnabled = false;
    DirectoryWatcher watcher;
    ExecutorService postProcessExecutor = null;
    AudioTranscode[] audioTranscodes = null;
    MediaElement mediaElement = null;
    Transcoder transcoder = null;
        
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
            
            // Setup post-processing of audio segments if required
            if(postProcessEnabled && audioTranscodes != null && mediaElement != null && transcoder != null) {                
                //  Setup thread pool for post-processing segments
                postProcessExecutor = Executors.newCachedThreadPool();
                
                // Setup directory watcher
                watcher = new DirectoryWatcher.Builder()
                    .addDirectories(streamDirectory.getPath())
                    .setPreExistingAsCreated(true)
                    .build(new DirectoryWatcher.Listener() {
                        
                        
                        @Override
                        public void onEvent(DirectoryWatcher.Event event, final Path path) {
                            switch (event) {
                                case ENTRY_CREATE:
                                    // Check if we are interested in this file
                                    if(!FilenameUtils.getExtension(path.toString()).isEmpty() || !path.getFileName().toString().contains("audio")) {
                                        break;
                                    }
                                                                        
                                    // Get the information we require
                                    String[] segmentData = FilenameUtils.getBaseName(path.getFileName().toString()).split("-");
                                    
                                    if(segmentData.length < 3) {
                                        break;
                                    }
                                    
                                    // Variables
                                    final int transcode = Integer.parseInt(segmentData[2]);
                                    
                                    // Retrive transcode format
                                    if(audioTranscodes.length < transcode || mediaElement == null) {
                                        break;
                                    }
                                    
                                    // Determine codec
                                    AudioTranscode aTranscode = audioTranscodes[transcode];
                                    Integer codec = aTranscode.getCodec();

                                    if(codec == SMS.Codec.COPY) {
                                        codec = MediaUtils.getAudioStreamById(mediaElement.getAudioStreams(), aTranscode.getId()).getCodec();
                                    }
                                    
                                    final int format = MediaUtils.getFormatForCodec(codec);
                                    
                                    // Transcode
                                    postProcessExecutor.submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            postProcess(path.toString(), format);
                                        }
                                    });
                                    break;

                                case ENTRY_MODIFY:                                    
                                    break;

                                case ENTRY_DELETE:
                                    break;
                            }
                        }
                    });
                
                // Start directory watcher
                watcher.start();
            }
        
            // Start transcoding
            start();
        } catch(Exception ex) {
            if(process != null) {
                process.destroy();
            }
            
            if(watcher != null) {
                watcher.stop();
            }
            
            if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
                postProcessExecutor.shutdownNow();
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
        
        // Check if directory watcher is active
        if(watcher != null) {
            watcher.stop();
        }
        
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
    
    private void postProcess(String path, Integer format) {
        // Process for transcoding
        Process postProcess = null;
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Post processing segment " + path + " with format '" + format + "'", null);
        
        try {
            // Generate post-process command
            List<String> command = new  ArrayList<>();
            command.add(transcoder.getPath().toString());
            command.add("-i");
            command.add(path);
            command.add("-c:a");
            command.add("copy");
            command.add(path + "." + format + ".tmp");
            
            LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, StringUtils.join(command, " "), null);
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            postProcess = processBuilder.start();
            new NullStream(postProcess.getInputStream()).start();
            
            // Wait for process to finish
            postProcess.waitFor();
            
            // Rename file once complete
            File temp = new File(path + "." + format + ".tmp");
            File segment = new File(path + "." + format);
            
            if(temp.exists()) {
                temp.renameTo(segment);
            }
        } catch(IOException ex) {
            LogService.getInstance().addLogEntry(Level.ERROR, CLASS_NAME, "Failed to post-process file " + path, ex);
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
    
    public void setPostProcessEnabled(boolean enabled) {
        this.postProcessEnabled = enabled;
    }
    
    public boolean getPostProcessEnabled() {
        return this.postProcessEnabled;
    }
    
    public void setAudioTranscodes(AudioTranscode[] audioTranscodes) {
        this.audioTranscodes = audioTranscodes;
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

                // Check for error
                if(code == 1) {
                    LogService.getInstance().addLogEntry(Level.WARN, CLASS_NAME, "Transcode command failed for job " + id + ". Attempting alternatives if available...", null);
                } else {
                    LogService.getInstance().addLogEntry(Level.INFO, CLASS_NAME, "Transcode finished for job " + id + " (fps=" + transcodeAnalysis.getFps() + ")", null);
                    break;
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
            
            if(watcher != null) {
                watcher.stop();
            }
            
            if(postProcessExecutor != null && !postProcessExecutor.isTerminated()) {
                postProcessExecutor.shutdownNow();
            }
            
            ended = true;
        }
    }
}