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
import com.scooter1556.sms.server.domain.AudioTranscode;
import com.scooter1556.sms.server.domain.ClientProfile;
import com.scooter1556.sms.server.domain.Directory;
import com.scooter1556.sms.server.domain.Job;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.domain.Session;
import com.scooter1556.sms.server.domain.VideoTranscode;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.SessionService;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/media")
public class MediaController {
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private SessionService sessionService;
    
    private static final String CLASS_NAME = "MediaController";

    @RequestMapping(value="/folder", method=RequestMethod.GET)
    public ResponseEntity<List<MediaFolder>> getMediaFolders(@RequestParam(value = "type", required = false) Byte type)
    {
        List<MediaFolder> mediaFolders = settingsDao.getMediaFolders(type);
        
        if (mediaFolders == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaFolders, HttpStatus.OK);
    }

    @RequestMapping(value="/folder/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaFolder> getMediaFolder(@PathVariable("id") UUID id)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if (mediaFolder == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaFolder, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaElement> getMediaElement(@PathVariable("id") UUID id,
                                                        @RequestParam(value = "sid", required = false) UUID sid)
    {
        MediaElement mediaElement = mediaDao.getMediaElementByID(id);
        
        if (mediaElement == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        List<MediaElement> mediaElements = Arrays.asList(mediaElement);
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements.get(0), HttpStatus.OK);
    }
    
    @RequestMapping(value="/random/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRandomElements(@PathVariable("limit") Integer limit,
                                                                @RequestParam(value="sid", required = false) UUID sid,
                                                                @RequestParam(value = "type", required = false) Byte type) {
        List<MediaElement> mediaElements = mediaDao.getRandomMediaElements(limit, type);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/folder/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByMediaFolderID(@PathVariable("id") UUID id,
                                                                              @RequestParam(value="sid", required = false) UUID sid) {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if(mediaFolder == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        List<MediaElement> mediaElements = mediaDao.getAlphabeticalMediaElementsByParentPath(mediaFolder.getPath());
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByID(@PathVariable("id") UUID id,
                                                                   @RequestParam(value="sid", required = false) UUID sid) {
        MediaElement element = mediaDao.getMediaElementByID(id);
        
        if(element == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        String parentPath;
        Byte type = element.getType();
        
        switch(element.getType()) {
            case MediaElementType.DIRECTORY:
                parentPath = element.getPath();
                type = null;
                break;
                
            case MediaElementType.AUDIO: case MediaElementType.VIDEO:
                parentPath = element.getParentPath();
                break;
                
            default:
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByParentPath(parentPath, type);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/all/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getDirectoryMediaElements(@PathVariable("limit") Integer limit)
    {
        List<MediaElement> mediaElements = mediaDao.getDirectoryElements(limit);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/recentlyadded/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRecentlyAddedDirectoryMediaElements(@PathVariable("limit") Integer limit,
                                                                                     @RequestParam(value = "type", required = false) Byte type)
    {   
        List<MediaElement> mediaElements = null;
        
        if(type == null) {
            mediaElements = mediaDao.getRecentlyAddedDirectoryElements(limit);
        } else if(type == MediaElement.DirectoryMediaType.AUDIO) {
            mediaElements = mediaDao.getRecentlyAddedAudioDirectoryElements(limit);
        } else if(type == MediaElement.DirectoryMediaType.VIDEO) {
            mediaElements = mediaDao.getRecentlyAddedVideoDirectoryElements(limit);
        }
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/recentlyplayed/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRecentlyPlayedDirectoryMediaElements(@PathVariable("limit") Integer limit,
                                                                                      @RequestParam(value = "type", required = false) Byte type)
    {   
        List<MediaElement> mediaElements = null;
        
        if(type == null) {
            mediaElements = mediaDao.getRecentlyPlayedDirectoryElements(limit);
        } else if(type == MediaElement.DirectoryMediaType.AUDIO) {
            mediaElements = mediaDao.getRecentlyPlayedAudioDirectoryElements(limit);
        } else if(type == MediaElement.DirectoryMediaType.VIDEO) {
            mediaElements = mediaDao.getRecentlyPlayedVideoDirectoryElements(limit);
        }
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/artist", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getArtists()
    {
        List<String> artists = mediaDao.getArtists();
        
        if (artists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(artists, HttpStatus.OK);
    }

    @RequestMapping(value="/albumartist", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumArtists()
    {
        List<String> albumArtists = mediaDao.getAlbumArtists();
        
        if (albumArtists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albumArtists, HttpStatus.OK);
    }

    @RequestMapping(value="/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbums()
    {
        List<String> albums = mediaDao.getAlbums();
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }

    @RequestMapping(value="/artist/{artist}/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumsByArtist(@PathVariable("artist") String artist)
    {
        List<String> albums = mediaDao.getAlbumsByArtist(artist);
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }
    
    @RequestMapping(value="/albumartist/{albumartist}/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumsByAlbumArtist(@PathVariable("albumartist") String albumArtist)
    {
        List<String> albums = mediaDao.getAlbumsByAlbumArtist(albumArtist);
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }

    @RequestMapping(value="/artist/{artist}/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtistAndAlbum(@PathVariable("artist") String artist,
                                                                               @PathVariable("album") String album,
                                                                               @RequestParam(value="sid", required = false) UUID sid) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for artist '" + artist + "' and album '" + album + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtistAndAlbum(artist, album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/albumartist/{albumartist}/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtistAndAlbum(@PathVariable("albumartist") String albumArtist,
                                                                                    @PathVariable("album") String album,
                                                                                    @RequestParam(value="sid", required = false) UUID sid) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album artist '" + albumArtist + "' and album '" + album + "'", null);

        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtistAndAlbum(albumArtist, album);

        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
       
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
       return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/artist/{artist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtist(@PathVariable("artist") String artist,
                                                                       @RequestParam(value="sid", required = false) UUID sid) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for artist '" + artist + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtist(artist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/albumartist/{albumartist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtist(@PathVariable("albumartist") String albumArtist,
                                                                            @RequestParam(value="sid", required = false) UUID sid) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album artist '" + albumArtist + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtist(albumArtist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbum(@PathVariable("album") String album,
                                                                      @RequestParam(value="sid", required = false) UUID sid) {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album '" + album + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbum(album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Check if we need to process media elements prior to sending
        if(sid != null) {
            processMediaElements(sid, mediaElements);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/collection", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getCollections()
    {
        List<String> collections = mediaDao.getCollections();
        
        if (collections == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(collections, HttpStatus.OK);
    }

    @RequestMapping(value="/collection/{collection}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByCollection(@PathVariable("collection") String collection)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByCollection(collection);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/files", method=RequestMethod.GET)
    public ResponseEntity<List<Directory>> getDirectoryList(@RequestParam(value = "path", required = false) String path) {
        List<Directory> directories = new ArrayList<>();
        File[] files = null;
        
        // If no path is specified return roots
        if (path == null) {
            if(SystemUtils.IS_OS_WINDOWS) {
                files = File.listRoots();
            } else if(SystemUtils.IS_OS_LINUX) {
                files = new File("/").listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
            }
        } else {
            // Return directory list for given path
            File file = new File(path);
            
            if(!file.isDirectory()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
        }
        
        // Check if there is anything to return
        if(files == null || files.length == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Convert files to directory list
        for(File file : files) {
            directories.add(new Directory(file.getName(), file.getAbsolutePath()));
        }
        
        return new ResponseEntity<>(directories, HttpStatus.OK);
    }
    
    /**
     * Helper Functions
     */
    
    private void processMediaElements(UUID sid, List<MediaElement> mediaElements) {
        // Check session is valid
        Session session = sessionService.getSessionById(sid);

        if(session != null && session.getClientProfile() != null) {
            // Populate streams for media elements
            mediaElements.forEach((mediaElement) -> {
                if(mediaElement.getType() == MediaElementType.AUDIO) {
                    mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(mediaElement.getID()));
                } else if(mediaElement.getType() == MediaElementType.VIDEO) {
                    mediaElement.setVideoStreams(mediaDao.getVideoStreamsByMediaElementId(mediaElement.getID()));
                    mediaElement.setAudioStreams(mediaDao.getAudioStreamsByMediaElementId(mediaElement.getID()));
                    mediaElement.setSubtitleStreams(mediaDao.getSubtitleStreamsByMediaElementId(mediaElement.getID()));
                }
            });
        
            TranscodeUtils.processMediaElementsForClient(mediaElements, session.getClientProfile());
        }
    }
    
}