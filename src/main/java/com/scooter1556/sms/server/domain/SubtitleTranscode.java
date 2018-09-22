package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SubtitleTranscode {
    private Integer id;
    private Integer oCodec, tCodec;

    public SubtitleTranscode(Integer id, Integer oCodec, Integer tCodec) {
        this.id = id;
        this.oCodec = oCodec;
        this.tCodec = tCodec;
    }

    @Override
    public String toString() {
        return String.format("{ID=%s, Original Codec=%s, Transcode Codec=%s}",
                id == null ? "null" : id.toString(),
                oCodec == null ? "null" : oCodec,
                tCodec == null ? "null" : tCodec
        );
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getOriginalCodec() {
        return oCodec;
    }
    
    public void setOriginalCodec(Integer codec) {
        this.oCodec = codec;
    }

    public Integer getCodec() {
        return tCodec;
    }
    
    public void setCodec(Integer codec) {
        this.tCodec = codec;
    }
}
