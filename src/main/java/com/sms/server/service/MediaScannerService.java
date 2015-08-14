package com.sms.server.service;

import com.sms.server.dao.MediaDao;
import com.sms.server.dao.SettingsDao;
import com.sms.server.domain.MediaElement;
import com.sms.server.domain.MediaElement.DirectoryMediaType;
import com.sms.server.domain.MediaElement.MediaElementType;
import com.sms.server.domain.MediaFolder;
import com.sms.server.service.parser.MetadataParser;
import com.sms.server.service.parser.NFOParser;
import java.io.File;
import java.sql.Timestamp;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

/**
 * Provides services for scanning media folders.
 *
 * @author Scott Ware
 */

@Service
public class MediaScannerService {

    private static final String CLASS_NAME = "MediaScannerService";
    
    @Autowired
    private SettingsDao settingsDao;
    
    @Autowired
    private MediaDao mediaDao;
    
    @Autowired
    private MetadataParser metadataParser;
    
    @Autowired
    private NFOParser nfoParser;
    
    private static final String AUDIO_FILE_TYPES = "ac3,mp3,ogg,oga,aac,m4a,flac,wav,dsf";
    private static final String VIDEO_FILE_TYPES = "avi,mpg,mpeg,mp4,m4v,mkv,mov,wmv,ogv,m2ts";
    private static final String EXCLUDED_FILE_NAMES = "Extras,extras";
    
    private static final Pattern FILE_NAME = Pattern.compile ("(.+)(\\s+[(\\[](\\d{4})[)\\]])$?");
    
    private boolean scanning = false;
    private int scanCount = 0, files = 0, folders = 0;
    private MediaFolder currentMediaFolder;

    //
    // Returns whether media folders are currently being scanned.
    //
    public synchronized boolean isScanning() {
        return scanning;
    }

    //
    // Returns the number of files scanned so far.
    //
    public int getScanCount() {
        return scanCount;
    }

    //
    // Scans media in a separate thread.
    //
    public synchronized void startScanning()
    {
        // Check if media is already being scanned
        if (isScanning())
        {
            return;
        }
        
        scanning = true;

        Thread thread = new Thread("MediaScanner") {
            @Override
            public void run() {
                scanMedia();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private void scanMedia() 
    {
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Started to scan media.", null);

        try {
            
            Timestamp lastScanned = new Timestamp(new Date().getTime());
            
            // Helper variables
            scanCount = 0;
            files = 0;
            folders = 0;

            // Recurse through all files and folders on disk for each media folder
            for (MediaFolder mediaFolder : settingsDao.getMediaFolders()) 
            {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Scanning media folder " + mediaFolder.getPath(), null);
                currentMediaFolder = mediaFolder;
                scanDirectory(new File(mediaFolder.getPath()), lastScanned);
            }
        
            mediaDao.removeDeletedMediaElements(lastScanned);

            LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Media scan complete (Items Scanned: " + scanCount + " Folders Processed: " + folders + " Files Processed: " + files + ")", null);

        } 
        catch (Throwable x)
        {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to scan media.", x);
        }
        finally
        {
            scanning = false;
        }
    }

    private void scanDirectory(File directory, Timestamp lastScanned)
    {
        if(directory == null)
            return;
        
        // Recurse through the directory
        for(File currentItem : directory.listFiles())
        {   
            // Increment scan count
            scanCount++;
                        
            // If the current item is a directory, process it and scan it's contents
            if(currentItem.isDirectory())
            {
                // Recursively scan directories
                scanDirectory(currentItem, lastScanned);
                
                if(parseDirectory(currentItem, lastScanned))
                {
                    folders ++;
                }
            }
            
            // If the current item is a non-hidden supported file, process it
            else if(!currentItem.isHidden() && (getMediaType(currentItem) != null))
            {
                if(parseFile(currentItem, lastScanned))
                {
                    files ++;
                }
            }
        }
    }
    
    private boolean parseDirectory(File file, Timestamp lastScanned)
    {
        // Flags
        boolean isNew = false;
        boolean update = false;
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing directory " + file.getPath(), null);
        
        // Check if media file already has an associated media element
        MediaElement mediaElement = mediaDao.getMediaElementByPath(file.getPath());
        
        // Create a new media element if required
        if(mediaElement == null)
        {
            // Check if this directory contains any supported media
            if(!scanForSupportedMedia(file))
            {
                return false;
            }
            
            mediaElement = getMediaElementFromFile(file);            
            mediaElement.setType(MediaElementType.DIRECTORY);
            
            // Parse file name for media element attributes
            mediaElement = parseFileName(file.getName(), mediaElement);
            
            isNew = true;
        }
        
        // Check if existing media element requires updating
        else if(new Timestamp(file.lastModified()).after(mediaElement.getLastScanned()))
        {
            update = true;
        }
        
        // If an update is not required just update the last scanned attribute
        else
        {
            mediaDao.updateLastScanned(mediaElement.getID(), lastScanned);
            return false;
        }
        
        // Set directory parameters
        if(isNew || update)
        {
            // Determine directory media type
            mediaElement.setDirectoryType(getDirectoryMediaType(file.getPath()));
            
            // Determine if the directory should be excluded from categorised lists
            if(isExcluded(file.getName()))
            {
                mediaElement.setExcluded(true);
            }
            
            // Check for common attributes if the directory contains media
            if(!mediaElement.getDirectoryType().equals(DirectoryMediaType.NONE))
            {
                // Get year if not set
                if(mediaElement.getYear() == 0)
                {
                    mediaElement.setYear(getDirectoryYear(file.getPath()));
                }
                
                // Get common media attributes for the directory if available (artist, collection, TV series etc...)                
                if(mediaElement.getDirectoryType().equals(DirectoryMediaType.AUDIO) || mediaElement.getDirectoryType().equals(DirectoryMediaType.MIXED))
                {
                    // Get directory description if possible.
                    String description = getDirectoryDescription(file.getPath());

                    if(description != null)
                    {
                        mediaElement.setDescription(description);
                    }

                    // Get directory artist if possible.
                    String artist = getDirectoryArtist(file.getPath());

                    // Try album artist
                    if(artist == null)
                    {
                        artist = getDirectoryAlbumArtist(file.getPath());
                    }

                    // Try root directory name
                    if(artist == null)
                    {
                        artist = getDirectoryRoot(file.getPath());
                    }

                    // Set directory artist if found
                    if(artist != null)
                    {
                        mediaElement.setArtist(artist);
                    }
                }

                if(mediaElement.getDirectoryType().equals(DirectoryMediaType.VIDEO))
                {
                    // Get directory collection/series if possible.
                    String collection = getDirectoryCollection(file.getPath());

                    // Try root directory name
                    if(collection == null)
                    {
                        collection = getDirectoryRoot(file.getPath());
                    }

                    // Set directory collection if found
                    if(collection != null)
                    {
                        mediaElement.setCollection(collection);
                    }
                }
            }
            // If the directory only contains directories exclude it from categorised lists
            else
            {
                mediaElement.setExcluded(true);
            }
            
            mediaElement.setLastScanned(lastScanned);
        }
        
        // Update Database
        if(isNew)
        {
            mediaDao.createMediaElement(mediaElement);
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, mediaElement.toString(), null);
            return true;
        }
        
        if(update)
        {
            mediaDao.updateMediaElement(mediaElement);
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, mediaElement.toString(), null);
        }
        
        return false;
    }
    
    private boolean parseFile(File file, Timestamp lastScanned)
    {
        // Flags
        boolean isNew = false;
        boolean update = false;
        boolean reparse = false;
        
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing file " + file.getPath(), null);
        
        // Check if media file already has an associated media element
        MediaElement mediaElement = mediaDao.getMediaElementByPath(file.getPath());
        
        // Create a new media element if required
        if(mediaElement == null)
        {
            mediaElement = getMediaElementFromFile(file);
            mediaElement.setType(getMediaType(file));
            mediaElement.setFormat(getFileExtension(file.getName()));
            
            // Parse file name for media element attributes
            mediaElement = parseFileName(getFileNameWithoutExtension(file.getName()), mediaElement);
            
            isNew = true;
        }
        else
        {
            // Check if existing media element requires updating
            update = new Timestamp(file.lastModified()).after(mediaElement.getLastScanned());
            
            // Check if a reparse is required
            reparse = nfoParser.isUpdateRequired(file.getParent(), mediaElement.getLastScanned());
        }
        
        // If an update is not required just update the last scanned attribute
        if(isNew || update || reparse)
        {
            mediaElement.setLastScanned(lastScanned);
        }
        else
        {
            mediaDao.updateLastScanned(mediaElement.getID(), lastScanned);
            return false;
        }
        
        // Update
        if(isNew || update)
        {
            mediaElement.setSize(file.length());
            
            // If updating metadata reset stream information to avoid replication
            if(update)
            {
                mediaElement.resetStreams();
            }
            
            // Parse Metadata
            metadataParser.parse(mediaElement);
        }
        
        // Reparse
        if(isNew || reparse)
        {
            nfoParser.parse(mediaElement);
        }
                
        // Update Database
        if(isNew)
        {
            mediaDao.createMediaElement(mediaElement);
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, mediaElement.toString(), null);
            return true;
        }
        
        if(update || reparse)
        {
            mediaDao.updateMediaElement(mediaElement);
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, mediaElement.toString(), null);
        }
        
        return false;       
    }
    
    private MediaElement parseFileName(String name, MediaElement mediaElement)
    {
        // Parse file name for title and year
        Matcher matcher = FILE_NAME.matcher(name);

        if(matcher.find())
        {
            mediaElement.setTitle(String.valueOf(matcher.group(1)));
            
            if(matcher.group(2) != null)
            {
                mediaElement.setYear(Short.parseShort(matcher.group(3)));
            }
        }
        else
        {
            mediaElement.setTitle(name);
        }
        
        return mediaElement;
    }
    
    private MediaElement getMediaElementFromFile(File file)
    {
        MediaElement mediaElement = new MediaElement();

        // Set common attributes
        mediaElement.setPath(file.getPath());
        mediaElement.setParentPath(file.getParent());
        
        return mediaElement;
    }
    
    //
    // Helper Function
    //
    
    private Byte getMediaType(File file)
    {
        if(file.isFile())
        {
            if(isAudioFile(file.getPath()))
            {
                return MediaElementType.AUDIO;
            }
            else if(isVideoFile(file.getPath()))
            {
                return MediaElementType.VIDEO;
            }
            else
            {
                return null;
            }
        }
        
        return null;
    }
    
    private Byte getDirectoryMediaType(String path)
    {
        Byte type = DirectoryMediaType.NONE;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path)) 
        {
            if(child.getType() == MediaElementType.AUDIO || child.getType() == MediaElementType.VIDEO)
            {
                // Set an initial media type
                if(type == DirectoryMediaType.NONE)
                {
                    type = child.getType();
                }
                else if(child.getType().compareTo(type) != 0)
                {
                    return DirectoryMediaType.MIXED;
                }
            }
        }
        
        return type;
    }
    
    private boolean isAudioFile(String path) 
    {
        for (String type : AUDIO_FILE_TYPES.split(","))
        {
            if (path.toLowerCase().endsWith("." + type))
            {
                return true;
            }
        }
        
        return false;
    }

    private boolean isVideoFile(String path) 
    {
        for (String type : VIDEO_FILE_TYPES.split(","))
        {
            if (path.toLowerCase().endsWith("." + type))
            {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isExcluded(String fileName)
    {
        for (String name : EXCLUDED_FILE_NAMES.split(","))
        {
            if (fileName.equals(name))
            {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean containsMedia(File directory)
    {
        for(MediaElement mediaElement : mediaDao.getMediaElementsByParentPath(directory.getPath())) 
        {
            Byte type = mediaElement.getType();
            
            if(type == MediaElementType.AUDIO || type == MediaElementType.VIDEO)
            {
                return true;
            }
        }
        
        return false;
    }
    
    // Recursively scans a directory for supported media.
    private boolean scanForSupportedMedia(File directory)
    {
        // Scan the root directory first
        if(containsMedia(directory))
        {
            return true;
        }
        
        // Scan child directories for supported media
        for(File candidate : directory.listFiles())
        {
            if(candidate.isDirectory())
            {
                if(scanForSupportedMedia(candidate))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private String getFileExtension(String name)
    {
        int extensionIndex = name.lastIndexOf(".");
        
        return extensionIndex == -1 ? null : name.substring(extensionIndex + 1).toLowerCase().trim();
    }
    
    // Returns the file name without extension
    private String getFileNameWithoutExtension(String name)
    {        
        int extensionIndex = name.lastIndexOf(".");
        
        return extensionIndex == -1 ? name : name.substring(0, extensionIndex).trim();
    }
    
    // Get directory year from child media elements
    private Short getDirectoryYear(String path)
    {
        Short year = 0;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path))
        {
            if(child.getType() != MediaElementType.DIRECTORY)
            {
                if(child.getYear() > 0)
                {
                    // Set an initial year
                    if(year == 0)
                    {
                        year = child.getYear();
                    }
                    else if(child.getYear().intValue() != year.intValue())
                    {
                        return 0;
                    }
                }
            }
        }
        
        return year;
    }
    
    // Get directory artist from child media elements
    private String getDirectoryArtist(String path)
    {
        String artist = null;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path))
        {
            if(child.getType() == MediaElementType.AUDIO)
            {
                if(child.getArtist() != null)
                {
                    // Set an initial artist
                    if(artist == null)
                    {
                        artist = child.getArtist();
                    }
                    else if(!child.getArtist().equals(artist))
                    {
                        return null;
                    }
                }
            }
        }
        
        return artist;
    }
    
    // Get directory album artist from child media elements
    private String getDirectoryAlbumArtist(String path)
    {
        String albumArtist = null;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path))
        {
            if(child.getType() == MediaElementType.AUDIO)
            {
                if(child.getAlbumArtist() != null)
                {
                    // Set an initial album artist
                    if(albumArtist == null)
                    {
                        albumArtist = child.getAlbumArtist();
                    }
                    else if(!child.getAlbumArtist().equals(albumArtist))
                    {
                        return null;
                    }
                }
            }
        }
        
        return albumArtist;
    }
    
    // Get directory collection from child media elements
    private String getDirectoryCollection(String path)
    {
        String collection = null;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path))
        {
            if(child.getType() == MediaElementType.VIDEO)
            {
                if(child.getCollection() != null)
                {
                    // Set an initial collection
                    if(collection == null)
                    {
                        collection = child.getCollection();
                    }
                    else if(!child.getCollection().equals(collection))
                    {
                        return null;
                    }
                }
            }
        }
        
        return collection;
    }
    
    // Get directory description from child media elements (audio only)
    private String getDirectoryDescription(String path)
    {
        String description = null;
        
        for(MediaElement child : mediaDao.getMediaElementsByParentPath(path))
        {
            if(child.getType() == MediaElementType.AUDIO)
            {
                if(child.getDescription() != null)
                {
                    // Set an initial description
                    if(description == null)
                    {
                        description = child.getDescription();
                    }
                    else if(!child.getDescription().equals(description))
                    {
                        return null;
                    }
                }
            }
        }
        
        return description;
    }
    
    // Depending on directory structure this could return artist, series or collection based on the parent directory name.
    private String getDirectoryRoot(String path)
    {        
        File directory = new File(path);
        
        // Check this is in fact a directory path
        if(!directory.isDirectory())
        {
            return null;
        }
        
        // If the parent directory is the current media folder forget it
        if(directory.getParent().equals(currentMediaFolder.getPath()))
        {
            return null;
        }
        
        // Check if the root directory contains media, if so forget it
        if(containsMedia(directory.getParentFile()))
        {
            return null;
        }
        
        return directory.getParentFile().getName();
    }
}