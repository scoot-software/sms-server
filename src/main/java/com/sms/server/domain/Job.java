/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sms.server.domain;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 *
 * @author scott2ware
 */
public class Job implements Serializable {
    
    private Long id;
    private Byte type;
    private String username;
    private Long mediaElement;
    private Timestamp startTime;
    private Timestamp endTime;
    private Timestamp lastActivity;
    private Long bytesTransferred;
    


    public Job() {};
    
    public Job(Long id, Byte type, String username, Long mediaElement, Timestamp startTime, Timestamp endTime, Timestamp lastActivity, Long bytesTransferred)
    {
        this.id = id;
        this.type = type;
        this.username = username;
        this.mediaElement = mediaElement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lastActivity = lastActivity;
        this.bytesTransferred = bytesTransferred;
    }
    
    @Override
    public String toString() {
        return String.format(
                "Job[ID=%s, Type=%s, User=%s, Media Element=%s, Start Time=%s, End Time=%s, Last Activity=%s, Bytes Transferred=%s]",
                id == null ? "N/A" : id.toString(), type == null ? "N/A" : type.toString(), username == null ? "N/A" : username, mediaElement == null ? "N/A" : mediaElement, startTime == null ? "N/A" : startTime.toString(), endTime == null ? "N/A" : endTime.toString(), lastActivity == null ? "N/A" : lastActivity.toString(), bytesTransferred == null ? "N/A" : bytesTransferred.toString());
    }

    public Long getID()  {
        return id;
    }
    
    public void setID(Long id) {
        this.id = id;
    }
    
    public Byte getType() {
        return type;
    }
    
    public void setType(Byte type) {
        this.type = type;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Long getMediaElement()  {
        return mediaElement;
    }
    
    public void setMediaElement(Long mediaElement) {
        this.mediaElement = mediaElement;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    public Timestamp getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(Timestamp lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public Long getBytesTransferred() {
        return bytesTransferred;
    }
    
    public void setBytesTransferred(Long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }

    public static class JobType {
        public static final byte AUDIO_STREAM = 0;
        public static final byte VIDEO_STREAM = 1;
        public static final byte DOWNLOAD = 2;
        public static final byte ADAPTIVE_STREAM = 3;
    }
}
