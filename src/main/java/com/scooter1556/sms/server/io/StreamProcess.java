/*
 * Author: Scott Ware <scoot.software@gmail.com>
 * Copyright (c) 2015 Scott Ware
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.scooter1556.sms.server.io;

import com.scooter1556.sms.server.service.LogService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;
import org.apache.commons.lang3.StringUtils;

public class StreamProcess extends SMSProcess {
    
    private static final String CLASS_NAME = "StreamProcess";
    
    private static final int BUFFER_SIZE = 4096;
    
    String contentType;
    HttpServletRequest request;
    HttpServletResponse response;    
            
    public StreamProcess() {};
    
    public StreamProcess(UUID id, String[][] commands, String contentType, HttpServletRequest request, HttpServletResponse response) {
        this.id = id;
        this.commands = commands;
        this.contentType = contentType;
        this.request = request;
        this.response = response;
    }
    
    @Override
    public void start() throws IOException {
        // Set response headers
        response.reset();
        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", id.toString());
        
        // Enable CORS
        response.setHeader(("Access-Control-Allow-Origin"), "*");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setIntHeader("Access-Control-Max-Age", 3600);

        // Set status code
        response.setStatus(SC_PARTIAL_CONTENT);
        
        /*********************** DEBUG: Response Headers *********************************/        
        String requestHeader = "\n***************\nResponse Header:\n***************\n";
	Collection<String> responseHeaderNames = response.getHeaderNames();
        
	for(int i = 0; i < responseHeaderNames.size(); i++) {
            String header = (String) responseHeaderNames.toArray()[i];
            String value = response.getHeader(header);
            requestHeader += header + ": " + value + "\n";
        }
        
        // Print Headers
        LogService.getInstance().addLogEntry(LogService.Level.INSANE, CLASS_NAME, requestHeader, null);
        
        /********************************************************************************/
        // Try available commands
        for (String[] command : commands) {
            // Start transcode process
            run(command);
            
            // Check for error
            if(bytesTransferred == 0) {
                LogService.getInstance().addLogEntry(LogService.Level.WARN, CLASS_NAME, "Transcode command failed for job " + id + ". Attempting alternatives if available...", null);
            } else {
                break;
            }
        }
    }
    
    private void run(String[] command) throws IOException {
        LogService.getInstance().addLogEntry(LogService.Level.DEBUG, CLASS_NAME, StringUtils.join(command, " "), null);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        process = processBuilder.start();
        InputStream input = process.getInputStream();
        OutputStream output = response.getOutputStream();
        new NullStream(process.getErrorStream()).start();

        // Buffer
        byte[] buffer = new byte[BUFFER_SIZE];
        int length;

        // Write stream to output
        while ((length = input.read(buffer)) != -1) {
            output.write(buffer, 0, length);
            bytesTransferred += length;
        }        
    }
}
