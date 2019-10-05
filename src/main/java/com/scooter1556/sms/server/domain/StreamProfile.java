package com.scooter1556.sms.server.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Arrays;

@ApiModel(description = "Stream Profile")
public class StreamProfile implements Serializable {
    
    @ApiModelProperty(value = "MIME Type", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "application/x-mpegurl")
    private String mimeType;
    
    @ApiModelProperty(value = "Codecs", readOnly = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "[10,1000,2000]")
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
