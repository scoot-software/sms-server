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
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.service.ImageService;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public void getCoverArt(@PathVariable("id") Long id, @PathVariable("scale") Integer scale, HttpServletResponse response)
    {
        MediaElement mediaElement;
        File image;
        
        try {
            // Get corresponding media element
            mediaElement = mediaDao.getMediaElementByID(id);

            if(mediaElement == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media element with id " + id + ".");
                return;
            }

            // Get cover art
            image = imageService.getCoverArt(mediaElement);

            // Check if we were able to retrieve cover art
            if(image == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find cover art.");
                return;
            }
            
            // Send image if found
            imageService.sendImage(image, scale, response);
        } catch (IOException ex) {
            // Do nothing...
        }
    }
    
    @RequestMapping(value="/{id}/fanart/{scale}", method=RequestMethod.GET, produces = "image/jpeg")
    @ResponseBody
    public void getFanArt(@PathVariable("id") Long id, @PathVariable("scale") Integer scale, HttpServletResponse response) {
        MediaElement mediaElement;
        File image;
        
        try {
            // Get corresponding media element
            mediaElement = mediaDao.getMediaElementByID(id);

            if(mediaElement == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media element with id " + id + ".");
                return;
            }

            // Get fan art
            image = imageService.getFanArt(mediaElement);

            // Check if we were able to retrieve fan art
            if(image == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find fan art.");
                return;
            }
            
            // Send image if found
            imageService.sendImage(image, scale, response);
        } catch (IOException ex) {
            // Do nothing...
        }
    }
}
