/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.utilities;

import java.nio.file.Path;

/**
 *
 * @author scott2ware
 */
public class FileUtils {
    
    public static String getFileExtension(Path path) {
        int extensionIndex = path.getFileName().toString().lastIndexOf(".");
        return extensionIndex == -1 ? null : path.getFileName().toString().substring(extensionIndex + 1).toLowerCase().trim();
    }
    
}
