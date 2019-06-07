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
import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.service.ImageService;
import java.io.File;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value="/image")
public class ImageController {
    
    private static final String CLASS_NAME = "ImageController";
    
    private static final int DEFAULT_COVER_SCALE = 500;
    private static final int DEFAULT_FANART_SCALE = 1280;
    private static final int DEFAULT_THUMBNAIL_SCALE = 640;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private ImageService imageService;
    
    @RequestMapping(value="/{id}/cover", method=RequestMethod.GET)
    @ResponseBody
    public void getCoverArt(@PathVariable("id") UUID id, 
                            @RequestParam(value = "scale", required = false) Integer scale,
                            @RequestParam(value = "folder", required = false) Boolean isFolder,
                            HttpServletResponse response) {
        MediaFolder folder = null;
        MediaElement mediaElement = null;
        File image = null;
        
        try {
            // Ensure folder flag is set
            if(isFolder == null) {
                isFolder = false;
            }
            
            // Get corresponding media
            if(isFolder) {
                folder = settingsDao.getMediaFolderByID(id);
            } else {
                mediaElement = mediaDao.getMediaElementByID(id);
            }
            
            if(mediaElement == null && folder == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media "+ (isFolder ? "folder" : "element") + "with id " + id + ".");
                return;
            }
            
            // Ensure scale is set
            if(scale == null || scale <= 0) {
                scale = DEFAULT_COVER_SCALE;
            }
            
            // Get cover art
            if(folder != null) {
                image = imageService.findCoverArt(new File(folder.getPath()));
            } else if(mediaElement != null) {
                image = imageService.getCoverArt(mediaElement);
            }

            // Check if we were able to retrieve cover art
            if(image == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to find cover art.");
                return;
            }
            
            // Send image if found
            imageService.sendImage(image, scale, response);
        } catch (Exception ex) {
            // Do nothing...
        }
    }
    
    @RequestMapping(value="/{id}/random", method=RequestMethod.GET)
    @ResponseBody
    public void getRandomCoverArt(@PathVariable("id") UUID id,
                                  @RequestParam(value = "scale", required = false) Integer scale,
                                  @RequestParam(value = "folder", required = false) Boolean isFolder,
                                  HttpServletResponse response) {
        MediaFolder folder = null;
        MediaElement element = null;
        String path = null;
        File image = null;
        
        try {
            // Ensure folder flag is set
            if(isFolder == null) {
                isFolder = false;
            }
            
            // Ensure scale is set
            if(scale == null || scale <= 0) {
                scale = DEFAULT_COVER_SCALE;
            }
            
            // Get corresponding media item
            if(isFolder) {
                folder = settingsDao.getMediaFolderByID(id);
            } else {
                element = mediaDao.getMediaElementByID(id);
            }
            
            // Check we have retrieved media
            if(folder == null && element == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media " + (isFolder ? "folder" : "element") + " with id " + id + ".");
                return;
            }
            
            // Get media path
            if(folder != null) {
                path = folder.getPath();
            } else if(element != null) {
                path = element.getPath();
            }
            
            // Loop through media elements for cover art
            List<MediaElement> mediaElements = mediaDao.getRandomMediaElementsByParentPath(path);

            if(mediaElements == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to retrieve child media elements for " + (isFolder ? "folder" : "media element") + " with id " + id + ".");
                return;
            }
            
            for(MediaElement mediaElement : mediaElements) {
                // Get cover art
                image = imageService.getCoverArt(mediaElement);
                
                if(image != null) {
                    break;
                }
            }

            // Check if we were able to retrieve cover art
            if(image == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to find cover art.");
                return;
            }
            
            // Send image if found
            imageService.sendImage(image, scale, response);
        } catch (Exception ex) {
            // Do nothing...
        }
    }
    
    @RequestMapping(value="/{id}/fanart", method=RequestMethod.GET)
    @ResponseBody
    public void getFanArt(@PathVariable("id") UUID id,
                          @RequestParam(value = "scale", required = false) Integer scale,
                          @RequestParam(value = "folder", required = false) Boolean isFolder,
                          HttpServletResponse response) {
        MediaElement mediaElement = null;
        MediaFolder folder = null;
        File image = null;
        
        try {
            // Ensure folder flag is set
            if(isFolder == null) {
                isFolder = false;
            }
            
            // Get corresponding media
            if(isFolder) {
                folder = settingsDao.getMediaFolderByID(id);
            } else {
                mediaElement = mediaDao.getMediaElementByID(id);
            }
            

            if(mediaElement == null && folder == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media "+ (isFolder ? "folder" : "element") + "with id " + id + ".");
                return;
            }
            
            // Ensure scale is set
            if(scale == null || scale <= 0) {
                scale = DEFAULT_FANART_SCALE;
            }

            // Get fan art
            if(folder != null) {
                image = imageService.findFanArt(new File(folder.getPath()));
            } else if(mediaElement != null) {
                image = imageService.getFanArt(mediaElement);
            }

            // Check if we were able to retrieve fan art
            if(image == null) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Unable to find fan art.");
                return;
            }
            
            // Send image if found
            imageService.sendImage(image, scale, response);
        } catch (Exception ex) {
            // Do nothing...
        }
    }
    
    @RequestMapping(value="/{id}/thumbnail", method=RequestMethod.GET)
    @ResponseBody
    public void getThumbnail(@PathVariable("id") UUID id,
                             @RequestParam(value = "scale", required = false) Integer scale,
                             @RequestParam(value = "offset", required = false) Integer offset,
                             HttpServletResponse response) {
        MediaElement mediaElement;
        File file;
        
        try {
            // Get corresponding media element
            mediaElement = mediaDao.getMediaElementByID(id);

            if(mediaElement == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to retrieve media element with id " + id + ".");
                return;
            }
            
            // Check this is a video element. We can't get thumbnails from any other type of file.
            if(!mediaElement.getType().equals(MediaElementType.VIDEO)) {
                response.sendError(HttpServletResponse.SC_NO_CONTENT, "Media element with id " + id + " is not a video element.");
                return;
            }
            
            // Ensure scale is set
            if(scale == null || scale <= 0) {
                scale = DEFAULT_THUMBNAIL_SCALE;
            }
            
            // Check offset
            if(offset != null) {
                if(offset > mediaElement.getDuration()) {
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Offset " + offset + " is out of range for the given media element.");
                    return;
                }
            } else {
                // Calculate default offset
                offset = (int) (mediaElement.getDuration() * 0.2);
            }

            // Get file
            file = new File(mediaElement.getPath());
            
            // Send thumbnail
            imageService.sendThumbnail(file, offset, scale, response);
            
        } catch (Exception ex) {
            // Do nothing...
        }
    }
}
