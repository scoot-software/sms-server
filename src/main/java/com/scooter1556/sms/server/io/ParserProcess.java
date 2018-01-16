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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ParserProcess {
    
    private static final String CLASS_NAME = "ParserProcess";
    
    private static final int BUFFER_SIZE = 4096;  
    
    Process process;
    String command[];
    boolean ended = false;
    List<String> output;
            
    public ParserProcess() {};
    
    public ParserProcess(String[] command) {
        this.command = command;
    }
    
    public void start() {
        try {
            // Start process
            ProcessBuilder processBuilder = new ProcessBuilder(command).redirectErrorStream(true);

            process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = new ArrayList<>();
                String line;
                while((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.length() > 0) {
                        output.add(line);
                    }
                }
            } finally {
                // Close streams
                process.getInputStream().close();
                ended = true;
            }
        } catch (IOException ex) {
            if(process != null) {
                process.destroy();
            }
        }
    }
    
    public void end()
    {
        if(process != null) {
            process.destroy();
        }
        
        ended = true;
    }
    
    public List<String> getOutput() {
        return output;
    }
    
    public Process getProcess() {
        return process;
    }
    
    public boolean hasEnded() {
        return ended;
    }
}
