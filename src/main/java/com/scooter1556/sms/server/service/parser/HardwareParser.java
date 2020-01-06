package com.scooter1556.sms.server.service.parser;

import com.scooter1556.sms.server.domain.GraphicsCard;
import com.scooter1556.sms.server.domain.HardwareAccelerator;
import com.scooter1556.sms.server.domain.OpenCLDevice;
import com.scooter1556.sms.server.service.LogService;
import com.scooter1556.sms.server.utilities.OpenCLUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;

import static org.jocl.CL.*;
import org.jocl.*;

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
                    
                    if((device.endsWith("nvidia") && vendor.equals(HardwareAccelerator.VENDOR_NVIDIA)) ||
                       (device.endsWith("i915") && vendor.equals(HardwareAccelerator.VENDOR_INTEL))) {
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
                                                                     function,
                                                                     false
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
    
    public static OpenCLDevice[] getOpenCLDevices() {
        // Get number of platforms
        int numPlatforms[] = new int[1];
        clGetPlatformIDs(0, null, numPlatforms);
        
        if(numPlatforms[0] == 0) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No OpenCL platforms found!", null);
            return null;
        }
        
        // Obtain the platform IDs
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms[0]];
        clGetPlatformIDs(platforms.length, platforms, null);
        
        List<OpenCLDevice> oclDevices = new ArrayList<>();
        
        for (int i = 0; i < platforms.length; i++) {
            // Obtain the number of devices for the current platform
            int numDevices[] = new int[1];
            clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, null, numDevices);

            if(numDevices[0] == 0) {
                continue;
            }

            cl_device_id devices[] = new cl_device_id[numDevices[0]];
            clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices[0], devices, null);

            // Process OpenCL devices
            for (int d = 0; d < devices.length; d++) {
                // Vendor
                Integer deviceVendor = OpenCLUtils.getInt(devices[d], CL_DEVICE_VENDOR_ID);
                String vendor = Integer.toString(deviceVendor, 16);

                // Type
                long deviceType = OpenCLUtils.getLong(devices[d], CL_DEVICE_TYPE);
                
                OpenCLDevice device = new OpenCLDevice(vendor, deviceType, i, d);                
                oclDevices.add(device);
            }
        }
        
        if(oclDevices.isEmpty()) {
            LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "No OpenCL devices found.", null);
            return null;
        }

        return oclDevices.toArray(new OpenCLDevice[oclDevices.size()]);
    }
}
