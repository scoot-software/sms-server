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
package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.UUID;

public class Job implements Serializable {
    
    private UUID id, sid;
    private Byte type;
    private MediaElement mediaElement;
    private Timestamp startTime;
    private Timestamp endTime;
    private Timestamp lastActivity;
    private long bytesTransferred = 0;
    private TranscodeProfile profile;


    public Job(UUID sid) {
        // Set IDs
        id = UUID.randomUUID();
        this.sid = sid;
    };
    
    @Override
    public String toString() {
        return String.format(
                "Job[ID=%s, Session ID=%s, Type=%s, Media Element=%s, Start Time=%s, End Time=%s, Last Activity=%s, Bytes Transferred=%s]",
                id == null ? "N/A" : id.toString(),
                sid == null ? "N/A" : sid.toString(),
                type == null ? "N/A" : type.toString(),
                mediaElement == null ? "N/A" : mediaElement,
                startTime == null ? "N/A" : startTime.toString(),
                endTime == null ? "N/A" : endTime.toString(),
                lastActivity == null ? "N/A" : lastActivity.toString(),
                String.valueOf(bytesTransferred));
    }

    public UUID getId()  {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getSessionId()  {
        return sid;
    }
    
    public void setSessionId(UUID sid) {
        this.sid = sid;
    }
    
    public Byte getType() {
        return type;
    }
    
    public void setType(Byte type) {
        this.type = type;
    }
    
    public MediaElement getMediaElement()  {
        return mediaElement;
    }
    
    public void setMediaElement(MediaElement mediaElement) {
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
    
    public long getBytesTransferred() {
        return bytesTransferred;
    }
    
    public void setBytesTransferred(long bytesTransferred) {
        this.bytesTransferred = bytesTransferred;
    }
    
    public TranscodeProfile getTranscodeProfile() {
        return profile;
    }
    
    public void setTranscodeProfile(TranscodeProfile profile) {
        this.profile = profile;
    }

    public static class JobType {
        public static final byte AUDIO_STREAM = 0;
        public static final byte VIDEO_STREAM = 1;
        public static final byte DOWNLOAD = 2;
    }
}
