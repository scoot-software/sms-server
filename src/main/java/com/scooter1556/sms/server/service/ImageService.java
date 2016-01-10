/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.io.StreamGobbler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Provides services for resizing and manipulating images.
 * 
 * @author scott2ware
 */

@Service
public class ImageService {
    
    private static final String CLASS_NAME = "ImageService";
    
    private static final String SUPPORTED_IMAGE_FORMATS = "jpg,jpeg,png,bmp";
    
    private static final String COVER_ART = "folder,cover";
    private static final String FAN_ART = "fanart";
    
    @Autowired
    private TranscodeService transcodeService;
    
    public byte[] getCoverArt(MediaElement element, Integer scale) {
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
        
        return processImage(imageFile, scale);
    }
    
    public byte[] getFanArt(MediaElement element, Integer scale) {
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
        
        return processImage(imageFile, scale);
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
    
    private byte[] processImage(File imageFile, Integer scale) {
        byte[] image;
        Process process = null;
        
        // Check required variables
        if(imageFile == null || scale == null) {
            return null;
        }
        
        // Check transcoder exists
        File transcoder = transcodeService.getTranscoder();
        
        if(transcoder == null) {
            return null;
        }
        
        // Build image scaling command
        List<String> command = new ArrayList<>();
        command.add(transcoder.getPath());
        command.add("-i");
        command.add(imageFile.getPath());
        command.add("-vf");
        command.add("scale=-1:" + scale.toString());
        command.add("-f");
        command.add("mjpeg");
        command.add("-");
        
        // Process image
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            process = processBuilder.start();
            InputStream input = process.getInputStream();
            new StreamGobbler(process.getErrorStream()).start();
            image = IOUtils.toByteArray(input);
            return image;
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to scale image file " + imageFile.getPath(), ex);
            if(process != null) { process.destroy(); }
            return null;
        }
    }
}
