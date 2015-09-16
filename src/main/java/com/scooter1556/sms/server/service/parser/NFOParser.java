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
import java.sql.Timestamp;
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
    
    public MediaElement parse(MediaElement mediaElement)
    {
        // Get XML file
        File nfoFile = getNFOFile(mediaElement.getParentPath());
        
        if(nfoFile != null)
        {
            LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, "Parsing NFO file " + nfoFile.getPath(), null);
            
            try
            {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = dbFactory.newDocumentBuilder();
                Document document = builder.parse(nfoFile);

                // Optimise XML before proceeding to minimise errors
                document.getDocumentElement().normalize();

                if(document.getElementsByTagName("title").getLength() > 0)
                {
                    if(!document.getElementsByTagName("title").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setTitle(document.getElementsByTagName("title").item(0).getTextContent());
                    }
                }

                if(document.getElementsByTagName("rating").getLength() > 0)
                {
                    if(!document.getElementsByTagName("rating").item(0).getTextContent().equals(""))
                    {
                       Float rating = Float.valueOf(document.getElementsByTagName("rating").item(0).getTextContent());
                       mediaElement.setRating(rating);
                    }
                }

                if(document.getElementsByTagName("year").getLength() > 0)
                {
                    if(!document.getElementsByTagName("year").item(0).getTextContent().equals(""))
                    {
                       Short year = Short.parseShort(document.getElementsByTagName("year").item(0).getTextContent());
                       mediaElement.setYear(year);
                    }
                }
                
                if(document.getElementsByTagName("genre").getLength() > 0)
                {
                    if(!document.getElementsByTagName("genre").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setGenre(document.getElementsByTagName("genre").item(0).getTextContent());
                    }
                }

                if(document.getElementsByTagName("outline").getLength() > 0)
                {
                    if(!document.getElementsByTagName("outline").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setDescription(document.getElementsByTagName("outline").item(0).getTextContent());
                    }
                }

                if(document.getElementsByTagName("tagline").getLength() > 0)
                {
                    if(!document.getElementsByTagName("tagline").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setTagline(document.getElementsByTagName("tagline").item(0).getTextContent());
                    }
                }

                if(document.getElementsByTagName("mpaa").getLength() > 0)
                {
                    if(!document.getElementsByTagName("mpaa").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setCertificate(document.getElementsByTagName("mpaa").item(0).getTextContent());
                    }
                }
                
                if(document.getElementsByTagName("set").getLength() > 0)
                {
                    if(!document.getElementsByTagName("set").item(0).getTextContent().equals(""))
                    {
                       mediaElement.setCollection(document.getElementsByTagName("set").item(0).getTextContent());
                    }
                }
            }
            catch (ParserConfigurationException | SAXException | IOException e)
            {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Unable to parse NFO file " + nfoFile.getPath(), e);
            }
        }

        return mediaElement;
    }

    // Returns an NFO file in the parent directory if present.
    private File getNFOFile(String path)
    {
        // Recursively check for nfo file in given directory path
        for (File candidate : new File(path).listFiles()) 
        {
            if (candidate.isFile() && !candidate.isHidden() && candidate.getName().toLowerCase().endsWith("nfo")) 
            {
                return candidate;
            }
        }
        
        return null;
    }
    
    public boolean isUpdateRequired(String path, Timestamp lastScanned)
    {
        File nfoFile = getNFOFile(path);
        
        if(nfoFile == null)
        {
            return false;
        }
        
        return new Timestamp(nfoFile.lastModified()).after(lastScanned);
    }
}
