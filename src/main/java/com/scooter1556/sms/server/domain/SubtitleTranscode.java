package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SubtitleTranscode {
    private Integer id;
    private Integer codec;
    private boolean hardcode = false;

    public SubtitleTranscode(Integer id, Integer codec, boolean hardcode) {
        this.id = id;
        this.codec = codec;
        this.hardcode = hardcode;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Codec=%s, Hardcoded=%s}",
                id == null ? "null" : id.toString(),
                codec == null ? "null" : codec,
                String.valueOf(hardcode));
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCodec() {
        return codec;
    }
    
    public void setCodec(Integer codec) {
        this.codec = codec;
    }

    public boolean isHardcoded() {
        return hardcode;
    }
    
    public void setHardcoded(boolean hardcode) {
        this.hardcode = hardcode;
    }
}
