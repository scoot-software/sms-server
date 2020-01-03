package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.GraphicsCard;
import com.scooter1556.sms.server.service.LogService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;

public class HardwareParser {
    
    private static final String CLASS_NAME = "HardwareParser";
    
    private static final String LINUX_PCI_BUS_PATH = "/proc/bus/pci/devices";
    
    public static GraphicsCard[] getGraphicsCards() {
        // Linux
        if(SystemUtils.IS_OS_LINUX) {
            try {
                // Read PCI bus
                File fDevices = new File(LINUX_PCI_BUS_PATH);
                
                if(!fDevices.exists()) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "PCI bus device descriptor not found!", null);
                    return null;
                }
                
                List<String> devices = Files.readAllLines(Paths.get(fDevices.toURI()));
                
                if(devices.isEmpty()) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No devices found on PCI bus.", null);
                    return null;
                }
                
                List<GraphicsCard> graphicsCards = new ArrayList<>();
                
                for(String device : devices) {
                    // Remove whitespace
                    device = device.replaceAll("\\s+","");
                    
                    // Process device info
                    String bus = device.substring(0, 2);
                    String devfn = device.substring(2, 4);
                    String vendor = device.substring(4, 8);
                    String id = device.substring(8, 12);
                    
                    if((device.endsWith("nvidia") && vendor.equals("10de")) ||
                       (device.endsWith("i915") && vendor.equals("8086"))) {
                        // Process device function
                        String binary = Integer.toBinaryString(Integer.parseInt(devfn, 16));
                        
                        // Pad binary string if necessary
                        if(binary.length() < 8) {
                            binary = String.format("%8s", binary).replace(' ', '0');
                        }
                        
                        String bDev = String.format("%8s", binary.substring(0, 5)).replace(' ', '0');
                        String bFunction = String.format("%8s", binary.substring(5)).replace(' ', '0');
                        
                        String dev = String.format("%2s", Integer.parseInt(bDev, 2)).replace(' ', '0');
                        String function  = String.format("%1s", Integer.parseInt(bFunction, 2));
                                                
                        // Create new graphics card instance
                        GraphicsCard graphicsCard = new GraphicsCard(id,
                                                                     vendor,
                                                                     bus,
                                                                     dev,
                                                                     function
                        );
                                                
                        // Add to array
                        graphicsCards.add(graphicsCard);
                    }
                }
                
                if(graphicsCards.isEmpty()) {
                    LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No graphics cards found.", null);
                    return null;
                }

                return graphicsCards.toArray(new GraphicsCard[graphicsCards.size()]);
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Parsing of hardware failed.", ex);
                return null;
            }

        }
        
        return null;
    }
}
