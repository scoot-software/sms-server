package com.scooter1556.sms.server.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
}
