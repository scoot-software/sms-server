/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.MediaElement;
import com.scooter1556.sms.server.service.LogService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author scott2ware
 */
@Service
public class NFOParser {

    private static final String CLASS_NAME = "NFOParser";

    public NFOData parse(Path path) {
        // Get NFO file
        File nfoFile = path.toFile();

        // Check file exists
        if (!nfoFile.isFile()) {
            return null;
        }

        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing NFO file " + nfoFile.getPath(), null);

        // Parse File
        NFOData data = new NFOData();

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document document = builder.parse(nfoFile);

            // Optimise XML before proceeding to minimise errors
            document.getDocumentElement().normalize();

            if (document.getElementsByTagName("title").getLength() > 0) {
                if (!document.getElementsByTagName("title").item(0).getTextContent().equals("")) {
                    data.setTitle(document.getElementsByTagName("title").item(0).getTextContent());
                }
            }

            if (document.getElementsByTagName("rating").getLength() > 0) {
                if (!document.getElementsByTagName("rating").item(0).getTextContent().equals("")) {
                    Float rating = Float.valueOf(document.getElementsByTagName("rating").item(0).getTextContent());
                    data.setRating(rating);
                }
            }

            if (document.getElementsByTagName("year").getLength() > 0) {
                if (!document.getElementsByTagName("year").item(0).getTextContent().equals("")) {
                    Short year = Short.parseShort(document.getElementsByTagName("year").item(0).getTextContent());
                    data.setYear(year);
                }
            }

            if (document.getElementsByTagName("genre").getLength() > 0) {
                if (!document.getElementsByTagName("genre").item(0).getTextContent().equals("")) {
                    data.setGenre(document.getElementsByTagName("genre").item(0).getTextContent());
                }
            }

            if (document.getElementsByTagName("outline").getLength() > 0) {
                if (!document.getElementsByTagName("outline").item(0).getTextContent().equals("")) {
                    data.setDescription(document.getElementsByTagName("outline").item(0).getTextContent());
                }
            }

            if (document.getElementsByTagName("tagline").getLength() > 0) {
                if (!document.getElementsByTagName("tagline").item(0).getTextContent().equals("")) {
                    data.setTagline(document.getElementsByTagName("tagline").item(0).getTextContent());
                }
            }

            if (document.getElementsByTagName("mpaa").getLength() > 0) {
                if (!document.getElementsByTagName("mpaa").item(0).getTextContent().equals("")) {
                    data.setCertificate(document.getElementsByTagName("mpaa").item(0).getTextContent());
                }
            }

            if (document.getElementsByTagName("set").getLength() > 0) {
                if (!document.getElementsByTagName("set").item(0).getTextContent().equals("")) {
                    data.setCollection(document.getElementsByTagName("set").item(0).getTextContent());
                }
            }
            
            return data;
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse NFO file " + nfoFile.getPath(), e);
            return null;
        }
    }
    
    public MediaElement updateMediaElement(MediaElement element, NFOData data) {
        
        // Check media element
        if(element == null) {
            element = new MediaElement();
        }
        
        // Do not continue if there is no data to merge
        if(data == null) {
            return element;
        }
        
        //
        // Merge data
        //
        
        if(data.getTitle() != null) {
            element.setTitle(data.getTitle());
        }
        
        if(data.getYear() > 0) {
            if(element.getYear() <= 0) {
                element.setYear(data.getYear());
            }
        }
        
        if(data.getGenre() != null) {
                element.setGenre(data.getGenre());
        }
        
        if(data.getRating() != 0F) {
            if(element.getRating() == 0F) {
                element.setRating(data.getRating());
            }
        }
        
        if(data.getTagline() != null) {
            element.setTagline(data.getTagline());
        }
        
        if(data.getDescription() != null) {
            element.setDescription(data.getDescription());
        }
        
        if(data.getCertificate() != null) {
            element.setCertificate(data.getCertificate());
        }
        
        if(data.getCollection() != null) {
            element.setCollection(data.getCollection());
        }
        
        return element;
    }

    public class NFOData {

        private String title;
        private Short year = 0;
        private String genre;
        private Float rating = 0F;
        private String tagline;
        private String description;
        private String certificate;
        private String collection;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Short getYear() {
            return year;
        }

        public void setYear(Short year) {
            this.year = year;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public Float getRating() {
            return rating;
        }

        public void setRating(Float rating) {
            this.rating = rating;
        }

        public String getTagline() {
            return tagline;
        }

        public void setTagline(String tagline) {
            this.tagline = tagline;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCertificate() {
            return certificate;
        }

        public void setCertificate(String certificate) {
            this.certificate = certificate;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }
    }
}
