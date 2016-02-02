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
import com.scooter1556.sms.server.io.ImageProcess;
import com.scooter1556.sms.server.io.SMSProcess;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
    
    public void sendImage(File imageFile, int scale, HttpServletResponse response) throws IOException {
        SMSProcess process;
        
        // Check transcoder exists
        File transcoder = transcodeService.getTranscoder();

        if(transcoder == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to get image processor.");
            return;
        }

        // Build image scaling command
        List<String> command = new ArrayList<>();
        command.add(transcoder.getPath());
        command.add("-i");
        command.add(imageFile.getPath());
        command.add("-vf");
        command.add("scale=-1:" + scale);
        command.add("-f");
        command.add("mjpeg");
        command.add("-");

        // Process image
        process = new ImageProcess(command, response);
        process.start();
    }
}
