package com.scooter1556.sms.server.service;

import com.scooter1556.sms.server.dao.MediaDao;
import com.scooter1556.sms.server.dao.SettingsDao;
import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.domain.MediaElement.DirectoryMediaType;
import com.scooter1556.sms.server.domain.MediaElement.MediaElementType;
import com.scooter1556.sms.server.domain.MediaFolder;
import com.scooter1556.sms.server.service.parser.MetadataParser;
import com.scooter1556.sms.server.service.parser.NFOParser;
import com.scooter1556.sms.server.service.parser.NFOParser.NFOData;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private static final String INFO_FILE_TYPES = "nfo";
    private static final String EXCLUDED_FILE_NAMES = "Extras,extras";

    private static final Pattern FILE_NAME = Pattern.compile("(.+)(\\s+[(\\[](\\d{4})[)\\]])$?");

    private boolean scanning = false;
    private long tCount = 0, tFolders = 0, tFiles = 0;
    private long count = 0, files = 0, folders = 0;
    private List<MediaFolder> mediaFolders;

    //
    // Returns whether media folders are currently being scanned.
    //
    public synchronized boolean isScanning() {
        return scanning;
    }

    //
    // Returns the number of files scanned so far.
    //
    public long getScanCount() {
        return tCount;
    }

    //
    // Scans media in a separate thread.
    //
    public synchronized void startScanning(Long id) {

        // Check if media is already being scanned
        if (isScanning()) {
            return;
        }

        // Determine which folders to scan
        if(id == null) {
            mediaFolders = settingsDao.getMediaFolders();
        } else {
            MediaFolder folder = settingsDao.getMediaFolderByID(id);

            if (folder == null) {
                return;
            }

            mediaFolders = new ArrayList<>();
            mediaFolders.add(folder);
        }
        
        // Start scanning media folders in a new thread
        Thread thread = new Thread("MediaScanner") {
            @Override
            public void run() {
                scanMedia();
            }
        };

        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    
    private void scanMedia() {
        // Start scanning
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Started to scan media.", null);

        tCount = 0;
        tFolders = 0;
        tFiles = 0;
        scanning = true;

        for (MediaFolder folder : mediaFolders) {

            Path path = FileSystems.getDefault().getPath(folder.getPath());
            ParseFiles fileParser = new ParseFiles(folder);

            try {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Scanning media folder " + folder.getPath(), null);
                Files.walkFileTree(path, fileParser);
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error scanning media folder " + folder.getPath(), ex);
            }

            // Update folder statistics
            folder.setFolders(folders);
            folder.setFiles(files);
            settingsDao.updateMediaFolder(folder);

            // Remove files which no longer exist
            mediaDao.removeDeletedMediaElements(folder.getPath(), fileParser.getScanTime());
        }
        
        // Scanning finished
        scanning = false;
        
        LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Media scan complete (Items Scanned: " + tCount + " Folders Processed: " + tFolders + " Files Processed: " + tFiles + ")", null);
    }

    private class ParseFiles extends SimpleFileVisitor<Path> {
        
        boolean directoryChanged = false;

        Timestamp scanTime = new Timestamp(new Date().getTime());;
        private NFOData data;
        private MediaFolder folder;
        private MediaElement directory;
        private List<MediaElement> elements;
        
        public ParseFiles(MediaFolder folder) {
            this.folder = folder;
            
            count = 0;
            folders = 0;
            files = 0;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing directory " + dir.toString(), null);
            
            // Check if we need to scan this directory
            if(!containsMedia(dir.toFile())) {
                return SKIP_SUBTREE;
            }
            
            // Clear variables
            directoryChanged = false;
            directory = null;
            elements = new ArrayList<>();
            data = null;
            
            // If this is the root directory procede without processing
            if(dir.toString().equals(folder.getPath())) {
                return CONTINUE;
            }

            // Check if media file already has an associated media element
            directory = mediaDao.getMediaElementByPath(dir.toString());

            if (directory == null) {
                directory = getMediaElementFromPath(dir);
                directory.setType(MediaElementType.DIRECTORY);

                // Parse file name for media element attributes
                directory = parseFileName(dir, directory);
                
                // Add new media element to database
                mediaDao.createMediaElement(directory);
                directory = mediaDao.getMediaElementByPath(dir.toString());
            }
            
            if(folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(directory.getLastScanned())) {
                // Determine if the directory should be excluded from categorised lists
                if (isExcluded(dir.getFileName())) {
                    directory.setExcluded(true);
                }
                
                directoryChanged = true;
            }
            
            // Update timestamp
            directory.setLastScanned(scanTime);

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing file " + file.toString(), null);
            
            // Update statistics
            count++;
            tCount++;
            
            // Determine type of file and how to process it
            if(isAudioFile(file) || isVideoFile(file)) {
                // Update statistics
                files++;
                tFiles++;
                
                // Check if media file already has an associated media element
                MediaElement mediaElement = mediaDao.getMediaElementByPath(file.toString());

                if (mediaElement == null) {
                    mediaElement = getMediaElementFromPath(file);
                    mediaElement.setType(getMediaType(file));
                    mediaElement.setFormat(getFileExtension(file.getFileName()));

                    // Parse file name for media element attributes
                    mediaElement = parseFileName(file.getFileName(), mediaElement);
                    
                    // Add new media element to database
                    mediaDao.createMediaElement(mediaElement);
                    mediaElement = mediaDao.getMediaElementByPath(file.toString());
                }
                
                // Determine if we need to parse the file
                if (folder.getLastScanned() == null || mediaElement.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).before(mediaElement.getLastScanned())) {
                    mediaElement.setSize(attr.size());

                    // Parse Metadata
                    mediaElement.resetStreams();
                    metadataParser.parse(mediaElement);
                }
                
                // Update timestamp
                mediaElement.setLastScanned(scanTime);
                
                // Add media element to list
                elements.add(mediaElement);
                
            } else if(isInfoFile(file)) {
                // Determine if we need to parse this file
                if(directoryChanged || folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(folder.getLastScanned())) {
                    data = nfoParser.parse(file);
                }
            }
            
            return CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            // Update statistics
            count++;
            tCount++;
            folders++;
            tFolders++;
            
            // Process child media elements
            for(MediaElement element : elements) {
                if(data != null) {
                    nfoParser.updateMediaElement(element, data);
                }
                
                mediaDao.updateMediaElement(element);
            }
            
            // Update directory element if necessary
            if(directoryChanged) {
                if(data != null) {
                    nfoParser.updateMediaElement(directory, data);
                }
                
                // Determine directory media type
                directory.setDirectoryType(getDirectoryMediaType(elements));
                
                // Check for common attributes if the directory contains media
                if (!directory.getDirectoryType().equals(DirectoryMediaType.NONE)) {
                    // Get year if not set
                    if (directory.getYear() == 0) {
                        directory.setYear(getDirectoryYear(elements));
                    }

                    // Get common media attributes for the directory if available (artist, collection, TV series etc...)                
                    if (directory.getDirectoryType().equals(DirectoryMediaType.AUDIO) || directory.getDirectoryType().equals(DirectoryMediaType.MIXED)) {
                        // Get directory description if possible.
                        String description = getDirectoryDescription(elements);

                        if (description != null) {
                            directory.setDescription(description);
                        }

                        // Get directory artist if possible.
                        String artist = getDirectoryArtist(elements);

                        // Try album artist
                        if (artist == null) {
                            artist = getDirectoryAlbumArtist(elements);
                        }

                        // Try root directory name
                        if (artist == null) {
                            artist = getDirectoryRoot(dir, folder.getPath());
                        }

                        // Set directory artist if found
                        if (artist != null) {
                            directory.setArtist(artist);
                        }
                    }

                    if (directory.getDirectoryType().equals(DirectoryMediaType.VIDEO)) {
                        // Get directory collection/series if possible.
                        String collection = getDirectoryCollection(elements);

                        // Try root directory name
                        if (collection == null) {
                            collection = getDirectoryRoot(dir, folder.getPath());
                        }

                        // Set directory collection if found
                        if (collection != null) {
                            directory.setCollection(collection);
                        }
                    }
                } else {
                    // Exclude directories from categorised lists which do not directly contain media
                    directory.setExcluded(true);
                }
            }
            
            // Update database
            if(directory != null) {
                mediaDao.updateMediaElement(directory);
            }

            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Finished parsing directory " + dir.toString(), null);
            
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error parsing file " + file.toString(), exc);
            return CONTINUE;
        }
        
        //
        // Helper Functions
        //

        private boolean isAudioFile(Path path) {
            for (String type : AUDIO_FILE_TYPES.split(",")) {
                if (path.getFileName().toString().toLowerCase().endsWith("." + type)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isVideoFile(Path path) {
            for (String type : VIDEO_FILE_TYPES.split(",")) {
                if (path.getFileName().toString().toLowerCase().endsWith("." + type)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isInfoFile(Path path) {
            for (String type : INFO_FILE_TYPES.split(",")) {
                if (path.getFileName().toString().toLowerCase().endsWith("." + type)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isExcluded(Path path) {
            for (String name : EXCLUDED_FILE_NAMES.split(",")) {
                if (path.getFileName().toString().equals(name)) {
                    return true;
                }
            }

            return false;
        }

        // Return a new media element for a given file
        private MediaElement getMediaElementFromPath(Path path) {
            MediaElement mediaElement = new MediaElement();

            // Set common attributes
            mediaElement.setPath(path.toString());
            mediaElement.setParentPath(path.getParent().toString());
            mediaElement.setLastScanned(scanTime);

            return mediaElement;
        }

        // Get title and other information from file name
        private MediaElement parseFileName(Path path, MediaElement mediaElement) {
            // Parse file name for title and year
            Matcher matcher = FILE_NAME.matcher(path.getFileName().toString());

            if (matcher.find()) {
                mediaElement.setTitle(String.valueOf(matcher.group(1)));

                if (matcher.group(2) != null) {
                    mediaElement.setYear(Short.parseShort(matcher.group(3)));
                }
            } else {
                mediaElement.setTitle(path.getFileName().toString());
            }

            return mediaElement;
        }

        // Returns media type for a given file
        private Byte getMediaType(Path path) {
            if (isAudioFile(path)) {
                return MediaElementType.AUDIO;
            } else if (isVideoFile(path)) {
                return MediaElementType.VIDEO;
            }

            return null;
        }

        // Determines if a directory should be scanned
        private boolean containsMedia(File directory) {
            for (File file : directory.listFiles()) {
                if (!file.isHidden() && (file.isDirectory() || isAudioFile(file.toPath()) || isVideoFile(file.toPath()))) {
                    return true;
                }
            }

            return false;
        }

        private String getFileExtension(Path path) {
            int extensionIndex = path.getFileName().toString().lastIndexOf(".");
            return extensionIndex == -1 ? null : path.getFileName().toString().substring(extensionIndex + 1).toLowerCase().trim();
        }

        private Byte getDirectoryMediaType(List<MediaElement> mediaElements) {
            Byte type = DirectoryMediaType.NONE;

            for (MediaElement child : mediaElements) {
                if (child.getType() == MediaElementType.AUDIO || child.getType() == MediaElementType.VIDEO) {
                    // Set an initial media type
                    if (type == DirectoryMediaType.NONE) {
                        type = child.getType();
                    } else if (child.getType().compareTo(type) != 0) {
                        return DirectoryMediaType.MIXED;
                    }
                }
            }

            return type;
        }

        // Get directory year from child media elements
        private Short getDirectoryYear(List<MediaElement> mediaElements) {
            Short year = 0;

            for (MediaElement child : mediaElements) {
                if (child.getType() != MediaElementType.DIRECTORY) {
                    if (child.getYear() > 0) {
                        // Set an initial year
                        if (year == 0) {
                            year = child.getYear();
                        } else if (child.getYear().intValue() != year.intValue()) {
                            return 0;
                        }
                    }
                }
            }

            return year;
        }

        // Get directory artist from child media elements
        private String getDirectoryArtist(List<MediaElement> mediaElements) {
            String artist = null;

            for (MediaElement child : mediaElements) {
                if (child.getType() == MediaElementType.AUDIO) {
                    if (child.getArtist() != null) {
                        // Set an initial artist
                        if (artist == null) {
                            artist = child.getArtist();
                        } else if (!child.getArtist().equals(artist)) {
                            return null;
                        }
                    }
                }
            }

            return artist;
        }

        // Get directory album artist from child media elements
        private String getDirectoryAlbumArtist(List<MediaElement> mediaElements) {
            String albumArtist = null;

            for (MediaElement child : mediaElements) {
                if (child.getType() == MediaElementType.AUDIO) {
                    if (child.getAlbumArtist() != null) {
                        // Set an initial album artist
                        if (albumArtist == null) {
                            albumArtist = child.getAlbumArtist();
                        } else if (!child.getAlbumArtist().equals(albumArtist)) {
                            return null;
                        }
                    }
                }
            }

            return albumArtist;
        }

        // Get directory collection from child media elements
        private String getDirectoryCollection(List<MediaElement> mediaElements) {
            String collection = null;

            for (MediaElement child : mediaElements) {
                if (child.getType() == MediaElementType.VIDEO) {
                    if (child.getCollection() != null) {
                        // Set an initial collection
                        if (collection == null) {
                            collection = child.getCollection();
                        } else if (!child.getCollection().equals(collection)) {
                            return null;
                        }
                    }
                }
            }

            return collection;
        }

        // Get directory description from child media elements (audio only)
        private String getDirectoryDescription(List<MediaElement> mediaElements) {
            String description = null;

            for (MediaElement child : mediaElements) {
                if (child.getType() == MediaElementType.AUDIO) {
                    if (child.getDescription() != null) {
                        // Set an initial description
                        if (description == null) {
                            description = child.getDescription();
                        } else if (!child.getDescription().equals(description)) {
                            return null;
                        }
                    }
                }
            }

            return description;
        }

        // Depending on directory structure this could return artist, series or collection based on the parent directory name.
        private String getDirectoryRoot(Path path, String mediaFolderPath) {
            File dir = path.toFile();

            // Check variables
            if (!dir.isDirectory()) {
                return null;
            }

            // If the parent directory is the current media folder forget it
            if (dir.getParent().equals(mediaFolderPath)) {
                return null;
            }

            // Check if the root directory contains media, if so forget it
            if (containsMedia(dir.getParentFile())) {
                return null;
            }

            return dir.getParentFile().getName();
        }

        public long getFiles() {
            return files;
        }

        public long getFolders() {
            return folders;
        }

        public long getCount() {
            return count;
        }

        public Timestamp getScanTime() {
            return scanTime;
        }
    }
}
