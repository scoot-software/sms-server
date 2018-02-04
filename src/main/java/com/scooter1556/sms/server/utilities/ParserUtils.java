package com.scooter1556.sms.server.utilities;

import com.scooter1556.sms.server.service.SettingsService;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;

public class ParserUtils {
    
    private static final String PARSER = "ffprobe";
    
    public static final String[] PARSER_PATH_LINUX = {
        "/usr/bin/ffprobe"
    };
    
    public static final String[] PARSER_PATH_WINDOWS = {
        System.getenv("SystemDrive") + File.separator + "ffmpeg" + File.separator + "bin" + File.separator + "ffprobe.exe",
        System.getenv("ProgramFiles") + File.separator + "ffmpeg" + File.separator + "ffprobe.exe",
        System.getenv("%programfiles% (x86)") + File.separator + "ffmpeg" + File.separator + "ffprobe.exe",
    };
    
    public static Path getParser() {
        // Check user config transcode path
        if(SettingsService.getInstance().getParserPath() != null){
            File pFile = new File(SettingsService.getInstance().getParserPath());
            
            if(isValidParser(pFile)) {
                return pFile.toPath();
            }
        }
        
        // Search possible parser paths
        for(String path : getParserPaths()) {
            File test = new File(path);
            
            if(isValidParser(test)) {
                SettingsService.getInstance().setParserPath(path);
                return test.toPath();
            }
        }
        
        // Out of ideas
        return null;
    }
    
    public static String[] getParserPaths() {
        if(SystemUtils.IS_OS_WINDOWS) {
            return PARSER_PATH_WINDOWS;
        } else if(SystemUtils.IS_OS_LINUX) {
            return PARSER_PATH_LINUX;
        }
        
        return null;
    }
    
    public static boolean isValidParser(File parser) {
        // Check file exists and is executable
        if(parser == null || !parser.canExecute()) {
            return false;
        }
        
        // Check this is a supported transcoder
        String[] command = new String[]{parser.getAbsolutePath()};
        
        try {
            String[] result = ParserUtils.getProcessOutput(command);
            
            for (String line : result) {
                if(line.contains(PARSER)) {
                    return true;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        
        return false;
    }
    
    public static String[] getProcessOutput(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = processBuilder.start();
        
        List<String> result = null;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            result = new ArrayList<>();
            String line;
            while((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.length() > 0) {
                    result.add(line);
                }
            }
        } catch(Exception ex) {
            return null;
        } finally {
            // Close streams
            process.getInputStream().close();
            
            if(result != null && !result.isEmpty()) {
                return result.toArray(new String[result.size()]);
            }
        }

        return null;     
    }
}
