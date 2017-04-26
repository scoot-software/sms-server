package com.scooter1556.sms.server.domain;

import java.io.Serializable;
import java.nio.file.Path;

public class Transcoder implements Serializable {
    
    private final Path path;
    private Version version;
    
    public Transcoder(Path path) {
        this.path = path;
    };
    
    @Override
    public String toString() {
        return String.format(
                "[Path=%s, Version=%s]",
                path,
                version == null ? "N/A" : version.toString());
    }
    
    public Path getPath() {
        return path;
    }
    
    public Version getVersion() {
        return version;
    }
    
    public void setVersion(Version version) {
        this.version = version;
    }
}
