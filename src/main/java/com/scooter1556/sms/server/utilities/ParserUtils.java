package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.service.SettingsService;
import static com.scooter1556.sms.server.service.parser.MetadataParser.getParserPaths;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParserUtils {
    
    public static final String METADATA_PARSER = "ffprobe";
    
    public static final String LINUX_HARDWARE_PARSER = "lshw";
    
    public static final String[] METADATA_PARSER_PATH_LINUX = {
        "/usr/local/bin/ffprobe",
        "/usr/bin/ffprobe",
    };
    
    public static final String[] METADATA_PARSER_PATH_WINDOWS = {
        System.getenv("SystemDrive") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffprobe.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "ffprobe.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffprobe.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "ffprobe.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffprobe.exe",
    };
    
    public static Path getMetadataParser() {
        // Check user config transcode path
        if(SettingsService.getInstance().getParserPath() != null){
            File pFile = new File(SettingsService.getInstance().getParserPath());
            
            if(isValid(METADATA_PARSER, pFile)) {
                return pFile.toPath();
            }
        }
        
        // Search possible parser paths
        for(String path : getParserPaths()) {
            File test = new File(path);
            
            if(isValid(METADATA_PARSER, test)) {
                SettingsService.getInstance().setParserPath(path);
                return test.toPath();
            }
        }
        
        // Out of ideas
        return null;
    }
    
    public static String getHardwareParser() {
        if(SystemUtils.IS_OS_LINUX) {
            return LINUX_HARDWARE_PARSER;
        } else {
            return null;
        }
    }
    
    public static boolean isValid(String parser, File path) {
        // Check file exists and is executable
        if(path == null || !path.canExecute()) {
            return false;
        }
        
        // Check parser is valid
        String[] command = new String[]{path.getAbsolutePath()};
        
        try {
            String[] result = ParserUtils.getProcessOutput(command, true);
            
            for (String line : result) {
                if(line.contains(parser)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        
        return false;
    }
    
    public static String[] getProcessOutput(String[] command, boolean redirectErrorStream) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(redirectErrorStream);
        Process process = processBuilder.start();
        
        try {
            List<String> result = IOUtils.readLines(process.getInputStream(), "UTF-8");
            
            if(!result.isEmpty()) {
                return result.toArray(new String[result.size()]);
            }
        } catch(IOException ex) {
            return null;
        }

        return null;     
    }
    
    //
    // XML Helpers
    //
    public static Node getNode(String tagName, NodeList nodes) {
        for(int x = 0; x < nodes.getLength(); x++) {
            Node node = nodes.item(x);
            
            if (node.getNodeName().equalsIgnoreCase(tagName)) {
                return node;
            }
        }

        return null;
    }

    public static String getNodeValue(String tagName, NodeList nodes, String defaultValue) {
        for (int x = 0; x < nodes.getLength(); x++) {
            Node node = nodes.item(x);
            
            if (node.getNodeName().equalsIgnoreCase(tagName)) {
                NodeList childNodes = node.getChildNodes();
                
                for (int y = 0; y < childNodes.getLength(); y++ ) {
                    Node data = childNodes.item(y);
                    
                    if ( data.getNodeType() == Node.TEXT_NODE ) {
                        return data.getNodeValue();
                    }
                }
            }
        }
        
        return defaultValue;
    }
    
    public static String getNodeAttr(String attrName, Node node, String defaultValue) {
        NamedNodeMap attrs = node.getAttributes();
        
        if(attrs == null) {
            return defaultValue;
        }

        for (int y = 0; y < attrs.getLength(); y++ ) {
            Node attr = attrs.item(y);
            
            if (attr.getNodeName().equalsIgnoreCase(attrName)) {
                return attr.getNodeValue();
            }
        }

        return defaultValue;
    }
}
