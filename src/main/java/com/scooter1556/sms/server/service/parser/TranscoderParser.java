package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.Version;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscoderParser {
    
    private static final String CLASS_NAME = "TranscodeParser";
    
    // Patterns
    private static final Pattern VERSION = Pattern.compile(".*?version\\s+(\\d+\\.\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE);
    
    public static Transcoder parse(Transcoder transcoder) {
        try{
            // Parse version
            String[] command = {transcoder.getPath().toString()};
            String[] result = ParserUtils.getProcessOutput(command);
            
            for(String line : result) {
                Matcher matcher;
                
                // Version
                matcher = VERSION.matcher(line);
                
                if(matcher.find()) {
                    transcoder.setVersion(Version.parse(String.valueOf(matcher.group(1))));
                }
            }
            
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to parse transcoder!", ex);
        }
        
        return transcoder;
    }
}
