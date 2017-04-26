package com.scooter1556.sms.server.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ParserUtils {
    
    public static String[] getProcessOutput(String[] command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);
        Process process = processBuilder.start();
        
        List<String> result;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            result = new ArrayList<>();
            String line;
            while((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.length() > 0) {
                    result.add(line);
                }
            }
        } finally {
            // Close streams
            process.getInputStream().close();
        }

        return result.toArray(new String[result.size()]);        
    }
    
    public static String addToCommaSeparatedList(String list, String entryToAdd) {
        if(entryToAdd == null) {
            return list;
        } else if(entryToAdd.equals("")) {
            return list;
        }
        
        if(list == null) {
            list = entryToAdd;
        } else if(list.equals("")) {
            list = entryToAdd;
        } else {
            list = list + "," + entryToAdd;
        }
            
        return list;
    }
}
