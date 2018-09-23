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
package com.scooter1556.sms.server.service.parser;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.io.ParserProcess;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrameParser {
    
    private static final String CLASS_NAME = "FrameParser";
    
    @Autowired
    private MediaDao mediaDao;
    
    // Process for parsing streams
    ParserProcess parserProcess;
        
    public VideoStream parse(VideoStream stream) {
        // Use parser to parse frames of a video stream
        Path parser = ParserUtils.getMetadataParser();

        // Check transcoder exists
        if(parser == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Parser is not available but is required to parse frames.", null);
            return stream;
        }
        
        // Check stream parameters
        if(stream == null || stream.getBPS() == null || stream.getFPS() == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Video stream missing parameters required for parsing frames.", null);
            return stream;
        }
        
        // Get associated media element
        MediaElement element = mediaDao.getMediaElementByID(stream.getMediaElementId());
        
        // Check media element exists
        if(element == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "The media element associated with the stream to parse cannot be found!", null);
            return stream;
        }

        try {
            String[] command = new String[]{parser.toString(), "-threads", "0", "-v", "quiet", "-print_format", "json", "-select_streams", "0:" + stream.getStreamId(), "-show_entries", "frame=interlaced_frame,key_frame,pkt_size,pkt_duration_time", element.getPath()};
            
            // Run parser process for output
            parserProcess = new ParserProcess(command);
            parserProcess.start();
            
            // Wait for process to finish
            parserProcess.getProcess().waitFor();
            
            // Do some checks to make sure we got some data
            if(parserProcess == null || !parserProcess.hasEnded() || parserProcess.getOutput() == null) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to gather frame information for video stream!", null);
                return stream;
            }
            
            // Retrieve output from process
            String[] data = parserProcess.getOutput().toArray(new String[0]);
            
            // Parse JSON
            JsonValue json = Json.parse(StringUtils.arrayToDelimitedString(data, ""));
            JsonArray frames = json.asObject().get("frames").asArray();
            
            // Flags
            Boolean interlaced = false;
            long totalBitrate = 0L, totalGop = 0L;
            int maxBitrate = 0, frameCount = 0, intervalCount = 0, intervalTotal= 0, intervalGop = 0, gopCount = 0;
            double intervalDuration = 0;
            
            // Process Streams
            if(frames != null) {
                for(JsonValue frame : frames) {
                    // Bit Rate
                    int keyFrame = frame.asObject().getInt("key_frame", -1);
                    String sizeStr = frame.asObject().getString("pkt_size", "0");
                    int size = Integer.parseInt(sizeStr);
                    String durationStr = frame.asObject().getString("pkt_duration_time", "0");
                    double duration = Double.parseDouble(durationStr);

                    if(size > 0 && duration > 0) {
                        int bitrate = Double.valueOf(size * 0.001 * stream.getBPS() * stream.getFPS()).intValue();

                        // Add to bitrate total
                        if(bitrate > 0) {
                            // Check max bitrate
                            // We accumulate at least 1 seconds worth of frames and compare the average bitrate
                            // ensuring we are at a GOP boundary before processing
                            if(intervalDuration > 1.0 && keyFrame > 0) {                            
                                int test = intervalTotal / intervalCount;

                                if(test > maxBitrate) {
                                    maxBitrate = test;
                                }
                                
                                // Reset variables
                                intervalCount = 0;
                                intervalDuration = 0;
                                intervalTotal = 0;
                            }

                            totalBitrate += bitrate;
                            intervalTotal += bitrate;
                            intervalDuration += duration;
                            frameCount++;
                            intervalCount++;
                        }

                        
                    }
                       
                    // Interlaced
                    int interlacedFrame = frame.asObject().getInt("interlaced_frame", -1);

                    if(interlacedFrame != -1) {
                        interlaced = interlacedFrame > 0;
                    }
                    
                    // GOP Size
                    if(keyFrame > 0) {
                        if(intervalGop > 0) {                            
                            intervalGop++;

                            // Add to gop total and gop count
                            totalGop += intervalGop;
                            gopCount++;

                            // Reset interval
                            intervalGop = 0;
                        }
                    } else {
                        // Increment interval on non-key frames
                        intervalGop++;
                    }
                            
                }
                
                // Process result
                if(maxBitrate > 0) {
                    stream.setMaxBitrate(maxBitrate);
                }
                
                // Calculate average bitrate
                if(totalBitrate > 0 && (stream.getBitrate() == null || stream.getBitrate() == 0)) {
                    int avgBitrate = (int) Math.round(totalBitrate / frameCount);
                    stream.setBitrate(avgBitrate);
                }
                
                //  Calculate average GOP size
                if(totalGop > 0 && gopCount > 0) {
                    int gopSize = (int) Math.round(totalGop / gopCount);
                    stream.setGOPSize(gopSize);
                }
                
                // Interlaced
                stream.setInterlaced(interlaced);
            }
        } catch(NumberFormatException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse frames for file " + element.getPath(), ex);
        } catch(InterruptedException ex) {}

        return stream;
    }
    
    // Stop parser process if running
    public void stop() {
        if(parserProcess != null) {
            parserProcess.end();
        }
    }
}
