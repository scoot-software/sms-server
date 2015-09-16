/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.service.LogService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 * @author scott2ware
 */
public class StreamProcess extends JobProcess {
    
    private static final String CLASS_NAME = "StreamProcess";
    
    private static final int BUFFER_SIZE = 4096;
    
    OutputStream output;
    
    Long bytesTransferred;
    
            
    public StreamProcess() {};
    
    public StreamProcess(Long id, List<String> command, OutputStream output, Long byteOffset)
    {
        this.id = id;
        this.command = command;
        this.output = output;
        this.bytesTransferred = byteOffset;
    }
    
    @Override
    public void start()
    {   
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            process = processBuilder.start();
            InputStream input = process.getInputStream();
            
            // Start reading streams
            new StreamGobbler(process.getErrorStream()).start();
            
            // Buffer
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            
            // Write stream to output
            while ((length = input.read(buffer)) != -1)
            {
                output.write(buffer, 0, length);
                bytesTransferred += length;
            }
            
            end();
            
        } catch (IOException ex) {
            // Thrown if the remote client closes the connection
            end();
        }
    }
    
    @Override
    public void end()
    {
        // Stop transcode process
        if(process != null)
        {
            process.destroy();
        }
        
        if(output != null)
        {
            try {
                output.close();
            } catch (IOException ex) {
                LogService.getInstance().addLogEntry(LogService.Level.ERROR, CLASS_NAME, "Error closing response output stream for job " + id, ex);
            }
        }
        
        ended = true;
    }
    
    public long getBytesTransferred()
    {
        return bytesTransferred;
    }
}
