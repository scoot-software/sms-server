package com.scooter1556.sms.server.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class SubtitleTranscode {
    private final String codec;
    private boolean hardcode = false;

    public SubtitleTranscode(String codec, boolean hardcode) {
        this.codec = codec;
        this.hardcode = hardcode;
    }

    @Override
    public String toString() {
        return String.format("{Codec=%s, Hardcoded=%s}",
                codec == null ? "null" : codec,
                String.valueOf(hardcode));
    }

    public String getCodec() {
        return codec;
    }

    public boolean isHardcoded() {
        return hardcode;
    }
}
