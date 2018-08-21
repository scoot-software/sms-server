package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.util.Arrays;

public class StreamProfile implements Serializable {
    private String mimeType;
    private Integer[] codecs;

    public StreamProfile() {}

    @Override
    public String toString() {
        return String.format("StreamProfile[Mime Type=%s, Codecs=%s",
                mimeType == null ? "null" : mimeType,
                codecs == null ? "null" : Arrays.toString(codecs)
                );
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer[] getCodecs() {
        return codecs;
    }

    public void setCodecs(Integer[] codecs) {
        this.codecs = codecs;
    }
}
