/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author scott2ware
 */
public class StreamGobbler extends Thread
{
    InputStream stream;
    
    public StreamGobbler(InputStream stream)
    {
        this.stream = stream;
    }
    
    @Override
    public void run()
    {
        try
        {
            InputStreamReader streamReader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(streamReader);
            String line;
            
            while ((line = buffer.readLine()) != null) { /*System.out.println(line);*/ }
        }
        catch (IOException ex) {}
    }
}