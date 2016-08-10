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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT;

public class StreamProcess extends SMSProcess {
    
    private static final String CLASS_NAME = "StreamProcess";
    
    private static final int BUFFER_SIZE = 4096;
    
    String contentType;
    HttpServletRequest request;
    HttpServletResponse response;    
            
    public StreamProcess() {};
    
    public StreamProcess(UUID id, List<String> command, String contentType, HttpServletRequest request, HttpServletResponse response) {
        this.id = id;
        this.command = command;
        this.contentType = contentType;
        this.request = request;
        this.response = response;
    }
    
    @Override
    public void start() throws IOException {
        // Set response headers
        response.setContentType(contentType);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("ETag", id.toString());

        // Start transcoding
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        process = processBuilder.start();
        InputStream input = process.getInputStream();
        OutputStream output = response.getOutputStream();

        // Set status code
        response.setStatus(SC_PARTIAL_CONTENT);

        // Start reading streams
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
