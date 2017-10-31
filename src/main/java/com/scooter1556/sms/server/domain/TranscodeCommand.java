package com.scooter1556.sms.server.domain;

import java.util.ArrayList;

public class TranscodeCommand {
    private final ArrayList<String> commands = new ArrayList<>();
    private final ArrayList<ArrayList<String>> filters = new ArrayList<>();
    
    public TranscodeCommand() {}
    
    public ArrayList<String> getCommands() {
        return commands;
    }
    
    public ArrayList<ArrayList<String>> getFilters() {
        return filters;
    }
}
