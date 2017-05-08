package com.scooter1556.sms.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.springframework.util.DigestUtils;

public class FileUtils {
    
    public static boolean checkIntegrity(File file, String md5) {
        if(file == null || !file.exists()) {
            return false;
        }
        
        try {
            String checksum = DigestUtils.md5DigestAsHex(new FileInputStream(file));
            return checksum.equalsIgnoreCase(md5);
        } catch (IOException ex) {
            return false;
        }
    }
    
    public static List<String> readFileToList(File file) {
        ArrayList<String> list = new ArrayList<>();
        
        try {
            Scanner scan = new Scanner(file);
            
            while(scan.hasNextLine()){
                list.add(scan.nextLine());
            }
        } catch(FileNotFoundException ex) {
               return null; 
        }
        
        return list;
    }
}
