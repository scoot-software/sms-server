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
import com.scooter1556.sms.server.utilities.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SIBLINGS;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashSet;
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

    private static final String INFO_FILE_TYPES = "nfo";
    private static final String EXCLUDED_FILE_NAMES = "Extras,extras";

    private static final Pattern FILE_NAME = Pattern.compile("(.+)(\\s+[(\\[](\\d{4})[)\\]])$?");

    private boolean scanning = false;
    private long tCount = 0, count = 0, files = 0, folders = 0;
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
        tCount = 0;
        scanning = true;

        for (MediaFolder folder : mediaFolders) {

            Path path = FileSystems.getDefault().getPath(folder.getPath());
            ParseFiles fileParser = new ParseFiles(folder);

            try {
                // Start Scan directory
                LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Scanning media folder " + folder.getPath(), null);
                Files.walkFileTree(path, fileParser);
                
                // Update folder statistics
                folder.setFolders(folders);
                folder.setFiles(files);
                folder.setLastScanned(fileParser.getScanTime());
                settingsDao.updateMediaFolder(folder);

                // Remove files which no longer exist
                mediaDao.removeDeletedMediaElements(folder.getPath(), fileParser.getScanTime());
                
                LogService.getInstance().addLogEntry(LogService.Level.INFO, CLASS_NAME, "Finished scanning media folder " + folder.getPath() + " (Items Scanned: " + count + " Folders Processed: " + folders + " Files Processed: " + files + ")", null);
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error scanning media folder " + folder.getPath(), ex);
            }
        }
        
        // Scanning finished
        scanning = false;        
    }

    private class ParseFiles extends SimpleFileVisitor<Path> {
        
        boolean directoryChanged = false;

        Timestamp scanTime = new Timestamp(new Date().getTime());
        private final MediaFolder folder;
        private final Deque<MediaElement> directories = new ArrayDeque<>();
        private final Deque<Deque<MediaElement>> directoryElements = new ArrayDeque<>();
        private final Deque<NFOData> nfoData = new ArrayDeque<>();
        private final HashSet<Path> directoriesToUpdate = new HashSet<>();
        
        public ParseFiles(MediaFolder folder) {
            this.folder = folder;
            
            count = 0;
            folders = 0;
            files = 0;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) {            
            // Check if we need to scan this directory
            if(!containsMedia(dir.toFile())) {
                return SKIP_SIBLINGS;
            }
            
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing directory " + dir.toString(), null);
            
            // Initialise variables
            directoryChanged = false;
            directoryElements.add(new ArrayDeque<MediaElement>());
            
            // Determine if this directory has changed
            directoryChanged = folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(folder.getLastScanned());
            
            // If this is the root directory procede without processing
            if(dir.toString().equals(folder.getPath())) {
                return CONTINUE;
            }
                
            // Check if directory already has an associated media element
            MediaElement directory = mediaDao.getMediaElementByPath(dir.toString());

            if (directory == null) {
                directory = getMediaElementFromPath(dir);
                directory.setType(MediaElementType.DIRECTORY);
            }

            if(directory.getID() == null || folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(folder.getLastScanned())) {
                // Add directory to update list
                directoriesToUpdate.add(dir);
                
                // Parse file name for media element attributes
                directory = parseFileName(dir, directory);
                
                // Determine if the directory should be excluded from categorised lists
                if (isExcluded(dir.getFileName())) {
                    directory.setExcluded(true);
                }
            }

            // Update timestamp
            directory.setLastScanned(scanTime);
            
            // Add directory to list
            directories.add(directory);

            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
            // Determine type of file and how to process it
            if(isAudioFile(file) || isVideoFile(file)) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing file " + file.toString(), null);
                
                // Update statistics
                count++;
                tCount++;
                files++;
                
                // Check if media file already has an associated media element
                MediaElement mediaElement = mediaDao.getMediaElementByPath(file.toString());

                if (mediaElement == null) {
                    mediaElement = getMediaElementFromPath(file);
                    mediaElement.setType(getMediaType(file));
                    mediaElement.setFormat(FileUtils.getFileExtension(file.getFileName()));
                }
                
                // Determine if we need to process the file
                if(mediaElement.getID() == null || folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(folder.getLastScanned())) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Processing file " + file.toString(), null);
                    
                    // Add parent directory to update list
                    directoriesToUpdate.add(file.getParent());
                    
                    // Parse file name for media element attributes
                    mediaElement = parseFileName(file.getFileName(), mediaElement);
                    
                    mediaElement.setSize(attr.size());

                    // Parse Metadata
                    mediaElement.resetStreams();
                    metadataParser.parse(mediaElement);
                }
                
                // Update timestamp
                mediaElement.setLastScanned(scanTime);
                
                // Add media element to list
                directoryElements.getLast().add(mediaElement);
                
            } else if(isInfoFile(file)) {
                // Determine if we need to parse this file
                if(directoryChanged || folder.getLastScanned() == null || new Timestamp(attr.lastModifiedTime().toMillis()).after(folder.getLastScanned())) {
                    LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Processing file " + file.toString(), null);
                    
                    nfoData.add(nfoParser.parse(file));
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
            
            MediaElement directory = null;
            Deque<MediaElement> dirElements;
            NFOData dirData = null;
            
            // Retrieve directory from list
            if(!directories.isEmpty()) {
                directory = directories.removeLast();
            }
            
            // Get child elements for directory
            dirElements = directoryElements.removeLast();
            
            // Get NFO data for directory
            if(!nfoData.isEmpty()) {
                if(nfoData.getLast().getPath().equals(dir)) {
                    dirData = nfoData.removeLast();
                }
            }
            
            // Process child media elements
            for(MediaElement element : dirElements) {
                if(dirData != null) {
                    nfoParser.updateMediaElement(element, dirData);
                }
                
                if(element.getID() == null) {
                    mediaDao.createMediaElement(element);
                } else {
                    mediaDao.updateMediaElementByPath(element);
                }
            }
            
            // Update directory element if necessary
            if(directory != null && directoriesToUpdate.contains(dir)) {
                LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Processing directory " + dir.toString(), null);
                
                if(dirData != null) {
                    nfoParser.updateMediaElement(directory, dirData);
                }
                
                // Determine directory media type
                directory.setDirectoryType(getDirectoryMediaType(dirElements));
                
                // Check for common attributes if the directory contains media
                if (!directory.getDirectoryType().equals(DirectoryMediaType.NONE)) {
                    // Get year if not set
                    if (directory.getYear() == 0) {
                        directory.setYear(getDirectoryYear(dirElements));
                    }

                    // Get common media attributes for the directory if available (artist, collection, TV series etc...)                
                    if (directory.getDirectoryType().equals(DirectoryMediaType.AUDIO) || directory.getDirectoryType().equals(DirectoryMediaType.MIXED)) {
                        // Get directory description if possible.
                        String description = getDirectoryDescription(dirElements);

                        if (description != null) {
                            directory.setDescription(description);
                        }

                        // Get directory artist if possible.
                        String artist = getDirectoryArtist(dirElements);

                        // Try album artist
                        if (artist == null) {
                            artist = getDirectoryAlbumArtist(dirElements);
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
                        String collection = getDirectoryCollection(dirElements);

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
                if(directory.getID() == null) {
                    mediaDao.createMediaElement(directory);
                } else {
                    mediaDao.updateMediaElementByPath(directory);
                }
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
            for (String type : TranscodeService.SUPPORTED_AUDIO_CONTAINER_FORMATS.split(",")) {
                if (path.getFileName().toString().toLowerCase().endsWith("." + type)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isVideoFile(Path path) {
            for (String type : TranscodeService.SUPPORTED_VIDEO_CONTAINER_FORMATS.split(",")) {
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
            } else if(path.toFile().isDirectory()){
                mediaElement.setTitle(path.getFileName().toString());
            } else {
                int extensionIndex = path.getFileName().toString().lastIndexOf(".");
                mediaElement.setTitle(extensionIndex == -1 ? path.getFileName().toString() : path.getFileName().toString().substring(0, extensionIndex));
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

        private Byte getDirectoryMediaType(Deque<MediaElement> mediaElements) {
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
        private Short getDirectoryYear(Deque<MediaElement> mediaElements) {
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
        private String getDirectoryArtist(Deque<MediaElement> mediaElements) {
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
        private String getDirectoryAlbumArtist(Deque<MediaElement> mediaElements) {
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
        private String getDirectoryCollection(Deque<MediaElement> mediaElements) {
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
        private String getDirectoryDescription(Deque<MediaElement> mediaElements) {
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
