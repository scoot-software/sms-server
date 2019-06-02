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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.VideoStream;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import java.io.IOException;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class FrameParser {
    
    private static final String CLASS_NAME = "FrameParser";
    
    @Autowired
    private MediaDao mediaDao;
    
    // Process for parsing streams
    Process process;
    JsonParser jsonParser;
        
    public VideoStream parse(@NonNull VideoStream stream) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "parse() -> " + stream.getMediaElementId() + "(" + stream.getStreamId() + ")", null);
        
        // Use parser to parse frames of a video stream
        Path parser = ParserUtils.getMetadataParser();

        // Check transcoder exists
        if(parser == null) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Parser is not available but is required to parse frames.", null);
            return stream;
        }
        
        // Check stream parameters
        if(stream.getBPS() == null || stream.getFPS() == null) {
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
            String[] command = new String[]{parser.toString(), "-threads", "0", "-v", "quiet", "-print_format", "json", "-select_streams", "v:" + stream.getStreamId(), "-show_entries", "frame=interlaced_frame,key_frame,pkt_size,pkt_duration_time", element.getPath()};
            
            // Start process
            ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
            this.process = processBuilder.start();
            
            // Start Json Parser
            JsonFactory factory = new JsonFactory();
            this.jsonParser  = factory.createParser(this.process.getInputStream());
            
            JsonToken jsonToken = this.jsonParser.nextToken();
            
            // Stream Variables    
            boolean interlaced = false;
            long totalBitrate = 0L, totalGop = 0L;
            int maxBitrate = 0, frameCount = 0, intervalCount = 0, intervalTotal= 0, intervalGop = 0, gopCount = 0;
            double intervalDuration = 0;
            
            // Frame Variables
            boolean keyFrame = false;
            int size = 0;
            double duration = 0;
            
            while(!this.jsonParser.isClosed()){
                if(jsonToken == null) {
                    break;
                }
                
                if(jsonToken.equals(JsonToken.FIELD_NAME)) {
                    String fieldName = this.jsonParser.getCurrentName();

                    // Get next token which should be the field value
                    this.jsonParser.nextToken();
                    
                    // Process fields
                    switch(fieldName) {
                        case "key_frame":
                            keyFrame = this.jsonParser.getValueAsBoolean(false);
                            
                            // GOP Size
                            if(keyFrame) {                                
                                if(intervalGop > 0) {                            
                                    intervalGop++;

                                    // Add to gop total and gop count
                                    totalGop += intervalGop;
                                    gopCount++;
                                }
                                
                                // Reset interval
                                intervalGop = 0;
                            } else {
                                // Increment interval on non-key frames
                                intervalGop++;
                            }
                                
                            break;
                            
                        case "interlaced_frame":
                            boolean interlacedFrame = this.jsonParser.getValueAsBoolean(false);
                            
                            if(interlacedFrame) {
                                interlaced = true;
                            }
                            
                            break;
                            
                        case "pkt_size":
                            size = this.jsonParser.getValueAsInt(0);
                            break;
                            
                        case "pkt_duration_time":
                            duration = this.jsonParser.getValueAsDouble(0);
                            break;
                    }
                    
                }
                
                // Check if we have complete frame data
                if(size > 0 && duration > 0) {
                    int bitrate = Double.valueOf(size * 0.001 * stream.getBPS() * stream.getFPS()).intValue();

                    // Add to bitrate total
                    if(bitrate > 0) {
                        // Check max bitrate
                        // We accumulate at least 1 seconds worth of frames and compare the average bitrate
                        // ensuring we are at a GOP boundary before processing
                        if(intervalDuration > 1.0 && keyFrame) {                            
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
                    
                    // Reset frame variables
                    size = 0;
                    duration = 0;
                }
                
                jsonToken = this.jsonParser.nextToken();
            }
            
            // Close streams
            this.process.getInputStream().close();
            this.jsonParser.close();
                
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
            
        } catch(JsonEOFException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "parse() -> " + ex.getClass().getName(), null);
        } catch(RuntimeException | IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse frames for file " + element.getPath(), ex);
        } finally {
            this.stop();
        }
        
        return stream;
    }
        
    // Stop Json parser if running
    public void stop() {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "stop()", null);
        
        try {
            if (this.jsonParser != null && !this.jsonParser.isClosed()) {
                this.jsonParser.close();
            }
            
            if(this.process != null) {
                this.process.destroy();
            }
            
            this.process = null;
            this.jsonParser = null;
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to stop frame parser.", ex);
        }
    }
}
