package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.HardwareAccelerator;
import com.scooter1556.sms.server.domain.Transcoder;
import com.scooter1556.sms.server.domain.Version;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.ParserUtils;
import com.scooter1556.sms.server.utilities.TranscodeUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jutils.jhardware.HardwareInfo;
import org.jutils.jhardware.model.GraphicsCardInfo;

public class TranscoderParser {
    
    private static final String CLASS_NAME = "TranscodeParser";
    
    // Keywords
    private static final String HWACCEL = "Hardware acceleration methods:";
    
    // Patterns
    private static final Pattern VERSION = Pattern.compile(".*?version\\s+(\\d+\\.\\d+\\.?\\d*)", Pattern.CASE_INSENSITIVE);
    
    public static Transcoder parse(Transcoder transcoder) {
        try {
            // Parse version
            transcoder.setVersion(parseVersion(transcoder));
            
            // Parse hardware acceleration methods
            transcoder.setHardwareAccelerators(parseHardwareAccelerators(transcoder));
            
        } catch (IOException ex) {
            LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Failed to parse transcoder!", ex);
        }
        
        return transcoder;
    }
    
    private static Version parseVersion(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString()};
        String[] result = ParserUtils.getProcessOutput(command);

        for(String line : result) {
            Matcher matcher;

            // Version
            matcher = VERSION.matcher(line);

            if(matcher.find()) {
                return Version.parse(String.valueOf(matcher.group(1)));
            }
        }
        
        return null;
    }
    
    private static HardwareAccelerator[] parseHardwareAccelerators(Transcoder transcoder) throws IOException {
        String[] command = {transcoder.getPath().toString(), "-hwaccels"};
        String[] result = ParserUtils.getProcessOutput(command);
        List<HardwareAccelerator> hwaccels = new ArrayList<>();
        
        boolean enable = false;
        
        // Get available graphics cards
        GraphicsCardInfo gpus = HardwareInfo.getGraphicsCardInfo();

        for(String line : result) {
            if(enable) {
                if(TranscodeUtils.isSupported(TranscodeUtils.SUPPORTED_HARDWARE_ACCELERATORS, line)) {
                    switch(line) {
                        case "vaapi":
                            // Get render devices
                            Path[] devices = TranscodeUtils.getRenderDevices();
                            
                            // Determine if we have a GPU which supports VAAPI
                            for(int i = 0; i < gpus.getGraphicsCards().size(); i++) {
                                if(gpus.getGraphicsCards().get(i).getManufacturer().contains("Intel")) {
                                    if(i < devices.length) {
                                        HardwareAccelerator hwaccel = new HardwareAccelerator(line);
                                        hwaccel.setDevice(devices[i]);
                                        hwaccels.add(hwaccel);
                                    }
                                }
                            }
                            
                            break;
                            
                        case "cuvid":
                            // Determine if we have a GPU which supports CUVID
                            for(int i = 0; i < gpus.getGraphicsCards().size(); i++) {
                                if(gpus.getGraphicsCards().get(i).getManufacturer().contains("NVIDIA")) {
                                    HardwareAccelerator hwaccel = new HardwareAccelerator(line);
                                    hwaccels.add(0, hwaccel);
                                }
                            }
                            
                            break;
                    }
                }
            } else {
                if(line.contains(HWACCEL)) {
                    enable = true;
                }
            }
        }
        
        return hwaccels.toArray(new HardwareAccelerator[0]);
    }
}
