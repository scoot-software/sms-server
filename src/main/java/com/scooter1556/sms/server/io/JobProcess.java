/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.scooter1556.sms.server.io;

import java.util.List;

/**
 *
 * @author scott2ware
 */
public class JobProcess {

    Long id;
    Process process;
    List<String> command;
    boolean ended = false;
    
    public JobProcess() {};
    
    void start(){}

    public void end()
    {
        if(process != null)
        {
            process.destroy();
        }
        
        ended = true;
    }
    
    public Long getID()  {
        return id;
    }
    
    public void setID(Long id) {
        this.id = id;
    }
    
    public boolean hasEnded()
    {
        return ended;
    }
}
