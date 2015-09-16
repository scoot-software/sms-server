/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.controller;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author scott2ware
 */

@RestController
@RequestMapping(value="/media")
public class MediaController {
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    private static final String CLASS_NAME = "MediaController";

    @RequestMapping(value="/folder", method=RequestMethod.GET)
    public ResponseEntity<List<MediaFolder>> getMediaFolders()
    {
        List<MediaFolder> mediaFolders = settingsDao.getMediaFolders();
        
        if (mediaFolders == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaFolders, HttpStatus.OK);
    }

    @RequestMapping(value="/folder/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaFolder> getMediaFolder(@PathVariable("id") Long id)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if (mediaFolder == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaFolder, HttpStatus.OK);
    }

    @RequestMapping(value="/{id}", method=RequestMethod.GET)
    public ResponseEntity<MediaElement> getMediaElement(@PathVariable("id") Long id)
    {
        MediaElement mediaElement = mediaDao.getMediaElementByID(id);
        
        if (mediaElement == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElement, HttpStatus.OK);
    }

    @RequestMapping(value="/folder/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByMediaFolderID(@PathVariable("id") Long id)
    {
        MediaFolder mediaFolder = settingsDao.getMediaFolderByID(id);
        
        if(mediaFolder == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        List<MediaElement> mediaElements = mediaDao.getAlphabeticalMediaElementsByParentPath(mediaFolder.getPath());
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/{id}/contents", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByParentID(@PathVariable("id") Long id)
    {
        MediaElement parentDirectory = mediaDao.getMediaElementByID(id);
        
        if(parentDirectory == null)
        {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        if(parentDirectory.getType() != MediaElementType.DIRECTORY)
        {
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
        }
        
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByParentPath(parentDirectory.getPath());
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
    public ResponseEntity<List<MediaElement>> getRecentlyAddedDirectoryMediaElements(@PathVariable("limit") Integer limit)
    {
        List<MediaElement> mediaElements = mediaDao.getRecentlyAddedDirectoryElements(limit);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }
    
    @RequestMapping(value="/recentlyplayed/{limit}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getRecentlyPlayedDirectoryMediaElements(@PathVariable("limit") Integer limit)
    {
        List<MediaElement> mediaElements = mediaDao.getRecentlyPlayedDirectoryElements(limit);
        
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
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtistAndAlbum(@PathVariable("artist") String artist, @PathVariable("album") String album)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtistAndAlbum(artist, album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/albumartist/{albumartist}/album/{album}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtistAndAlbum(@PathVariable("albumartist") String albumArtist, @PathVariable("album") String album)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtistAndAlbum(albumArtist, album);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/artist/{artist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByArtist(@PathVariable("artist") String artist)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByArtist(artist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        return new ResponseEntity<>(mediaElements, HttpStatus.OK);
    }

    @RequestMapping(value="/albumartist/{albumartist}", method=RequestMethod.GET)
    public ResponseEntity<List<MediaElement>> getMediaElementsByAlbumArtist(@PathVariable("albumartist") String albumArtist)
    {
        List<MediaElement> mediaElements = mediaDao.getMediaElementsByAlbumArtist(albumArtist);
        
        if (mediaElements == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
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
}