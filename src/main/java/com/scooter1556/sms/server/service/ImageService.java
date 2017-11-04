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

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.io.ImageProcess;
import com.scooter1556.sms.server.io.SMSProcess;
import java.io.File;
import java.util.ArrayList;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
    
    private static final String CLASS_NAME = "ImageService";
    
    private static final String SUPPORTED_IMAGE_FORMATS = "jpg,jpeg,png,bmp";
    
    private static final String COVER_ART = "folder,cover";
    private static final String FAN_ART = "fanart";
    
    @Autowired
    private TranscodeService transcodeService;
    
    public File getCoverArt(MediaElement element) {
        File imageFile;
        
        // Get directory or file parent directory
        if(element.getType() == MediaElementType.DIRECTORY) {
            imageFile = findCoverArt(new File(element.getPath()));
        } else {
            imageFile = findCoverArt(new File(element.getParentPath()));
        }
        
        // If cover art is not found return.
        if(imageFile == null) {
            return null;
        }
        
        return imageFile;
    }
    
    public File getCoverArt(MediaFolder folder) {
        File imageFile;
        
        imageFile = findCoverArt(new File(folder.getPath()));
        
        // If cover art is not found return.
        if(imageFile == null) {
            return null;
        }
        
        return imageFile;
    }
    
    public File getFanArt(MediaElement element) {
        File imageFile;
        
        // Get directory or file parent directory
        if(element.getType() == MediaElementType.DIRECTORY) {
            imageFile = findFanArt(new File(element.getPath()));
            
            // Try parent path
            if(imageFile == null) {
                imageFile = findFanArt(new File(element.getParentPath()));
            }
        } else {
            imageFile = findFanArt(new File(element.getParentPath()));
        }
        
        return imageFile;
    }
    
    public File getFanArt(MediaFolder folder) {
        File imageFile = findFanArt(new File(folder.getPath()));
        return imageFile;
    }
    
    private File findCoverArt(File directory) {
        if(!directory.isDirectory()) {
            return null;
        }
        
        for(File file : directory.listFiles()) {
            for (String name : COVER_ART.split(",")) {
                if(file.getName().toLowerCase().contains(name) && isSupportedFormat(file)) {
                    return file;
                }
            }
        }
        
        return null;
    }
    
    private File findFanArt(File directory) {
        if(!directory.isDirectory()) {
            return null;
        }
        
        for(File file : directory.listFiles()) {            
            for (String name : FAN_ART.split(",")){
                if(file.getName().toLowerCase().contains(name) && isSupportedFormat(file)){
                    return file;
                }
            }
        }
        
        return null;
    }
    
    private boolean isSupportedFormat(File file) {
        for (String type : SUPPORTED_IMAGE_FORMATS.split(",")) {
            if (file.getName().toLowerCase().endsWith("." + type)) {
                return true;
            }
        }

        return false;
    }
    
    public void sendImage(File imageFile, int scale, HttpServletResponse response) throws Exception {
        SMSProcess process;
        
        // Check transcoder exists
        Transcoder transcoder = transcodeService.getTranscoder();

        if(transcoder == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get image processor.");
            return;
        }

        // Build image scaling command
        ArrayList<ArrayList<String>> commands = new ArrayList<>();
        commands.add(new ArrayList<String>());
        
        commands.get(0).add(transcoder.getPath().toString());
        commands.get(0).add("-i");
        commands.get(0).add(imageFile.getPath());
        commands.get(0).add("-vf");
        commands.get(0).add("scale=-1:" + scale);
        commands.get(0).add("-f");
        commands.get(0).add("mjpeg");
        commands.get(0).add("-");
        
        String[][] result = new String[commands.size()][];
        result[0] = commands.get(0).toArray(new String[0]);
        
        // Set content type
        response.setContentType("image/jpeg");

        // Set status code
        response.setStatus(SC_PARTIAL_CONTENT);
        
        // Process image
        process = new ImageProcess(result, response);
        process.start();
    }
    
    public void sendThumbnail(File file, int offset, int scale, HttpServletResponse response) throws Exception {
        SMSProcess process;
        
        // Check transcoder exists
        Transcoder transcoder = transcodeService.getTranscoder();

        if(transcoder == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get image processor.");
            return;
        }

        // Build image scaling command
        ArrayList<ArrayList<String>> commands = new ArrayList<>();
        commands.add(new ArrayList<String>());
        
        commands.get(0).add(transcoder.getPath().toString());
        commands.get(0).add("-ss");
        commands.get(0).add(String.valueOf(offset));
        commands.get(0).add("-i");
        commands.get(0).add(file.getPath());
        commands.get(0).add("-vframes");
        commands.get(0).add("1");
        commands.get(0).add("-vf");
        commands.get(0).add("scale=-1:" + scale);
        commands.get(0).add("-f");
        commands.get(0).add("mjpeg");
        commands.get(0).add("-");
        
        String[][] result = new String[commands.size()][];
        result[0] = commands.get(0).toArray(new String[0]);
        
        // Set content type
        response.setContentType("image/jpeg");

        // Set status code
        response.setStatus(SC_PARTIAL_CONTENT);
        
        // Process image
        process = new ImageProcess(result, response);
        process.start();
    }
}
