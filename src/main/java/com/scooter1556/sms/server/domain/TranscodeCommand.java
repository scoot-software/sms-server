package com.scooter1556.sms.server.domain;

import java.util.ArrayList;

public class TranscodeCommand {
    private final ArrayList<String> commands = new ArrayList<>();
    private final ArrayList<String> videoBaseFilters = new ArrayList<>();
    private final ArrayList<ArrayList<String>> videoEncodeFilters = new ArrayList<>();
    
    public TranscodeCommand() {}
    
    public ArrayList<String> getCommands() {
        return commands;
    }
    
    public ArrayList<String> getVideoBaseFilters() {
        return videoBaseFilters;
    }
    
    public ArrayList<ArrayList<String>> getVideoEncodeFilters() {
        return videoEncodeFilters;
    }
}
