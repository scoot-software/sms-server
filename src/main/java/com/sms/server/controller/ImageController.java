/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.controller;

import com.sms.server.dao.MediaDao;
import com.sms.server.domain.MediaElement;
import com.sms.server.service.ImageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author scott2ware
 */

@Controller
@RequestMapping(value="/image")
public class ImageController {
    
    private static final String CLASS_NAME = "ImageController";
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private ImageService imageService;
    
    @RequestMapping(value="/{id}/cover/{scale}", method=RequestMethod.GET, produces = "image/jpeg")
    @ResponseBody
    public ResponseEntity getCoverArt(@PathVariable("id") Long id, @PathVariable("scale") Integer scale)
    {
        MediaElement mediaElement;
        BufferedImage image;
        
        // Get corresponding media element
        mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Get scaled cover art
        image = imageService.getCoverArt(mediaElement, scale);
        
        // Check if we were able to retrieve a cover
        if(image == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Convert image to bytes to send
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Return cover art if found
            return new ResponseEntity(imageBytes, HttpStatus.OK);
            
        } catch (IOException ex) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @RequestMapping(value="/{id}/fanart/{scale}", method=RequestMethod.GET, produces = "image/jpeg")
    @ResponseBody
    public ResponseEntity getFanArt(@PathVariable("id") Long id, @PathVariable("scale") Integer scale)
    {
        MediaElement mediaElement;
        BufferedImage image;
        
        // Get corresponding media element
        mediaElement = mediaDao.getMediaElementByID(id);
        
        if(mediaElement == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Get scaled fan art
        image = imageService.getFanArt(mediaElement, scale);
        
        // Check if we were able to retrieve fan art
        if(image == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Convert image to bytes to send
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();
            
            // Return cover art if found
            return new ResponseEntity(imageBytes, HttpStatus.OK);
            
        } catch (IOException ex) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
