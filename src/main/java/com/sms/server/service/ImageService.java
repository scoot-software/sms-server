/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.service;

import com.sms.server.domain.MediaElement;
import com.sms.server.domain.MediaElement.MediaElementType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;
import org.springframework.stereotype.Service;

/**
 * Provides services for resizing and manipulating images.
 * 
 * @author scott2ware
 */

@Service
public class ImageService {
    
    private static final String CLASS_NAME = "ImageService";
    
    private static final String COVER_ART = "folder.jpg,cover.jpg";
    private static final String FAN_ART = "fanart.jpg";
    
    public BufferedImage getCoverArt(MediaElement element, Integer scale)
    {
        File imageFile;
        BufferedImage image;
        
        // Get directory or file parent directory
        if(element.getType() == MediaElementType.DIRECTORY)
        {
            imageFile = findCoverArt(new File(element.getPath()));
        }
        else
        {
            imageFile = findCoverArt(new File(element.getParentPath()));
        }
        
        // If cover art is not found return.
        if(imageFile == null)
        {
            return null;
        }
        
        // Read image file into buffer
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to read image file " + imageFile.getPath(), ex);
            return null;
        }
        
        // Scale image
        image = Scalr.resize(image, scale);
        
        return image;
    }
    
    public BufferedImage getFanArt(MediaElement element, Integer scale)
    {
        File imageFile;
        BufferedImage image;
        
        // Get directory or file parent directory
        if(element.getType() == MediaElementType.DIRECTORY)
        {
            imageFile = findFanArt(new File(element.getPath()));
        }
        else
        {
            imageFile = findFanArt(new File(element.getParentPath()));
        }
        
        // If fan art is not found return.
        if(imageFile == null)
        {
            return null;
        }
        
        // Read image file into buffer
        try {
            image = ImageIO.read(imageFile);
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to read image file " + imageFile.getPath(), ex);
            return null;
        }
        
        // Scale image
        image = Scalr.resize(image, scale);
        
        return image;
    }
    
    private File findCoverArt(File directory)
    {
        if(!directory.isDirectory())
        {
            return null;
        }
        
        for(File file : directory.listFiles()) 
        {
            for (String name : COVER_ART.split(","))
            {
                if(file.getPath().endsWith(name))
                {
                    return file;
                }
            }
        }
        
        return null;
    }
    
    private File findFanArt(File directory)
    {
        if(!directory.isDirectory())
        {
            return null;
        }
        
        for(File file : directory.listFiles()) 
        {            
            for (String name : FAN_ART.split(","))
            {
                if(file.getPath().endsWith(name))
                {
                    return file;
                }
            }
        }
        
        return null;
    }
}
