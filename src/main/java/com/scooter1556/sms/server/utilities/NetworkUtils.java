/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.utilities;

import java.net.InetAddress;

/**
 * Utilities For Network Operations.
 * 
 * Created By Scott Ware
 */
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
}
