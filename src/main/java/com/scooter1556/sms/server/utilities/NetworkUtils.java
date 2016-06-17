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
package com.scooter1556.sms.server.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;

public class NetworkUtils {
    
    public static boolean isLocalIP(final InetAddress ip1, final InetAddress ip2, final int mask) {
        byte[] x = ip1.getAddress();
        byte[] y = ip2.getAddress();
        
        if(x == y) {
            return true;
        }
        
        if(x == null || y == null) {
            return false;
        }
        
        if(x.length != y.length) {
            return false;
        }
        
        final int bits  = mask &   7;
        final int bytes = mask >>> 3;
        
        for(int i = 0; i < bytes; i++) {
            if(x[i] != y[i]) {
                return false;
            }
        }
        
        final int shift = 8 - bits;
        
        return !(bits != 0 && x[bytes]>>>shift != y[bytes]>>>shift);
    }
    
    public static int ping(final InetAddress address) {
        Long start = System.currentTimeMillis();
        
        try {
            if (!address.isReachable(2000)) return -1;
            return (int) (System.currentTimeMillis() - start);
        } catch (IOException ex) {
            return -1;
        }
            
    }
    
    public static String getPublicIP() {

        try {
            URL connection = new URL("http://checkip.amazonaws.com/");
            URLConnection con = connection.openConnection();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            
            String str = reader.readLine();
            return str;
        } catch (IOException ex) {
            return null;
        }
    }
}
