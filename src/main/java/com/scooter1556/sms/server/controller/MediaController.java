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
import com.scooter1556.sms.server.domain.Directory;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.service.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private UserService userService;
    
    private static final String CLASS_NAME = "MediaController";

    @ApiOperation(value = "Get a list of media folders")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media folders returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media folders found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/folder", method=RequestMethod.GET)
    public ResponseEntity<List<MediaFolder>> getMediaFolders(
            @ApiParam(value = "Content type", required = false, allowableValues = "0, 1, 2, 3") @RequestParam(value = "type", required = false) Byte type,
            HttpServletRequest request)
    {
        List<MediaFolder> mediaFolders = settingsDao.getMediaFolders(type);
        
        if (mediaFolders == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaFolders = userService.processMediaFoldersForUser(request.getUserPrincipal().getName(), mediaFolders);
        
        if (mediaFolders == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaFolders, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media folder")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media folder returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media folder not found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/folder/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaFolder> getMediaFolder(
            @ApiParam(value = "Media folder ID", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if (mediaFolder == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        if(!userService.processMediaFolderForUser(request.getUserPrincipal().getName(), mediaFolder)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaFolder, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media element")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media element returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media element not found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaElement> getMediaElement(
            @ApiParam(value = "Media element ID", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request)
    {
        MediaElement mediaElement = mediaDao.getMediaElementByID(id);
        
        if (mediaElement == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        if(!userService.processMediaElementForUser(request.getUserPrincipal().getName(), mediaElement)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
                
        return new ResponseEntity<>(mediaElement, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get random media elements")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/random/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRandomElements(
            @ApiParam(value = "Number of media elements to return", required = true) @PathVariable("limit") Integer limit,
            @ApiParam(value = "Media type", required = false) @RequestParam(value = "type", required = false) Byte type,
            HttpServletRequest request)
    {
        List<MediaElement> mediaElements = mediaDao.getRandomMediaElements(type);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        // Trim list to specified limit
        if(mediaElements.size() > limit) {
            mediaElements = mediaElements.subList(0, limit);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media folder contents")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Media folder not found"),
        @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/folder/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByMediaFolderID(
            @ApiParam(value = "ID of the media folder", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if(mediaFolder == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        List<MediaElement> mediaElements = mediaDao.getAlphabeticalMediaElementsByParentPath(mediaFolder.getPath());
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get directory element contents")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "Directory element not found"),
        @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_ACCEPTABLE, message = "Directory element cannot be processed"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByID(
            @ApiParam(value = "ID of the directory element", required = true) @PathVariable("id") UUID id,
            HttpServletRequest request)
    {
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
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get random directory elements")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Directory elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No directory elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/all/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getDirectoryMediaElements(
            @ApiParam(value = "Number of directory elements to return", required = true) @PathVariable("limit") Integer limit,
            HttpServletRequest request)
    {
        List<MediaElement> mediaElements = mediaDao.getDirectoryElements();
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        // Trim list to specified limit
        if(mediaElements.size() > limit) {
            mediaElements = mediaElements.subList(0, limit);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get recently added directory elements")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Directory elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No directory elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/recentlyadded/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRecentlyAddedDirectoryMediaElements(
            @ApiParam(value = "Number of directory elements to return", required = true) @PathVariable("limit") Integer limit,
            @ApiParam(value = "Directory media type", required = false) @RequestParam(value = "type", required = false) Byte type,
            HttpServletRequest request)
    {   
        List<MediaElement> mediaElements = null;
        
        if(type == null) {
            mediaElements = mediaDao.getRecentlyAddedDirectoryElements();
        } else {
            if(type == MediaElement.DirectoryMediaType.AUDIO) {
                mediaElements = mediaDao.getRecentlyAddedAudioDirectoryElements();
            } else if(type == MediaElement.DirectoryMediaType.VIDEO) {
                mediaElements = mediaDao.getRecentlyAddedVideoDirectoryElements();
            }
        }
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        // Trim list to specified limit
        if(mediaElements.size() > limit) {
            mediaElements = mediaElements.subList(0, limit);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get recently played directory elements")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Directory elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No directory elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/recentlyplayed/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRecentlyPlayedDirectoryMediaElements(
            @ApiParam(value = "Number of directory elements to return", required = true) @PathVariable("limit") Integer limit,
            @ApiParam(value = "Directory media type", required = false) @RequestParam(value = "type", required = false) Byte type,
            HttpServletRequest request)
    {   
        List<MediaElement> mediaElements = null;
        
        if(type == null) {
            mediaElements = mediaDao.getRecentlyPlayedDirectoryElements();
        } else {
            if(type == MediaElement.DirectoryMediaType.AUDIO) {
                mediaElements = mediaDao.getRecentlyPlayedAudioDirectoryElements();
            } else if(type == MediaElement.DirectoryMediaType.VIDEO) {
                mediaElements = mediaDao.getRecentlyPlayedVideoDirectoryElements();
            }
        }
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        // Trim list to specified limit
        if(mediaElements.size() > limit) {
            mediaElements = mediaElements.subList(0, limit);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get list of artists")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Artist list returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No artists found")
    })
    @RequestMapping(value="/artist", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getArtists()
    {
        List<String> artists = mediaDao.getArtists();
        
        if (artists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(artists, HttpStatus.OK);
    }

    @ApiOperation(value = "Get list of album artists")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Album artist list returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No album artists found")
    })
    @RequestMapping(value="/albumartist", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumArtists()
    {
        List<String> albumArtists = mediaDao.getAlbumArtists();
        
        if (albumArtists == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albumArtists, HttpStatus.OK);
    }

    @ApiOperation(value = "Get list of albums")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Album list returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No albums found")
    })
    @RequestMapping(value="/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbums()
    {
        List<String> albums = mediaDao.getAlbums();
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }

    @ApiOperation(value = "Get list of albums for artist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Album list returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No albums found")
    })
    @RequestMapping(value="/artist/{artist}/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumsByArtist(
            @ApiParam(value = "Artist", required = true) @PathVariable("artist") String artist)
    {
        List<String> albums = mediaDao.getAlbumsByArtist(artist);
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get list of albums for album artist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Album list returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No albums found")
    })
    @RequestMapping(value="/albumartist/{albumartist}/album", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getAlbumsByAlbumArtist(
            @ApiParam(value = "Album Artist", required = true) @PathVariable("albumartist") String albumArtist)
    {
        List<String> albums = mediaDao.getAlbumsByAlbumArtist(albumArtist);
        
        if (albums == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(albums, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media elements by artist and album")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/artist/{artist}/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtistAndAlbum(
            @ApiParam(value = "Artist", required = true) @PathVariable("artist") String artist,
            @ApiParam(value = "Album", required = true) @PathVariable("album") String album,
            HttpServletRequest request)
    {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for artist '" + artist + "' and album '" + album + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtistAndAlbum(artist, album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media elements by album artist and album")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/albumartist/{albumartist}/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtistAndAlbum(
            @ApiParam(value = "Album Artist", required = true) @PathVariable("albumartist") String albumArtist,
            @ApiParam(value = "Album", required = true) @PathVariable("album") String album,
            HttpServletRequest request)
    {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album artist '" + albumArtist + "' and album '" + album + "'", null);

        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtistAndAlbum(albumArtist, album);

        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
       return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media elements by artist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/artist/{artist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtist(
            @ApiParam(value = "Artist", required = true) @PathVariable("artist") String artist,
            HttpServletRequest request)
    {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for artist '" + artist + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtist(artist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media elements by album artist")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/albumartist/{albumartist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtist(
            @ApiParam(value = "Album Artist", required = true) @PathVariable("albumartist") String albumArtist,
            HttpServletRequest request)
    {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album artist '" + albumArtist + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtist(albumArtist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get media elements by album")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbum(
            @ApiParam(value = "Album", required = true) @PathVariable("album") String album,
            HttpServletRequest request)
    {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Fetching media elements for album '" + album + "'", null);
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbum(album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @ApiOperation(value = "Get list of collections")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "List of collections returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No collections found")
    })
    @RequestMapping(value="/collection", method=RequestMethod.GET)
    public ResponseEntity<List<String>> getCollections()
    {
        List<String> collections = mediaDao.getCollections();
        
        if (collections == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(collections, HttpStatus.OK);
    }

    @ApiOperation(value = "Get media elements by collection")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "Media elements returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No media elements found"),
        @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = "User is not permitted to access content")
    })
    @RequestMapping(value="/collection/{collection}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByCollection(
            @ApiParam(value = "Collection", required = true) @PathVariable("collection") String collection,
            HttpServletRequest request)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByCollection(collection);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        // Process for user
        mediaElements = userService.processMediaElementsForUser(request.getUserPrincipal().getName(), mediaElements);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @ApiOperation(value = "Get list of directories from filesystem")
    @ApiResponses(value = {
        @ApiResponse(code = HttpServletResponse.SC_OK, message = "List of directories returned successfully"),
        @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = "No directories found"),
        @ApiResponse(code = HttpServletResponse.SC_BAD_REQUEST, message = "Path is not a directory")
    })
    @RequestMapping(value="/files", method=RequestMethod.GET)
    public ResponseEntity<List<Directory>> getDirectoryList(
            @ApiParam(value = "Directory path", required = false) @RequestParam(value = "path", required = false) String path) {
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
}