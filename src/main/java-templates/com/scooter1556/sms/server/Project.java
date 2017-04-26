package com.scooter1556.sms.server;

public final class Project {

    private static final String VERSION = "${project.version}";
    private static final String ARTIFACTID = "${project.artifactId}";
    private static final String ORGANISATION = "${project.organization.name}";

    public static String getVersion() {
        return VERSION;
    }

    public static String getArtifactId() {
        return ARTIFACTID;
    }
    
    public static String getOrganisation() {
        return ORGANISATION;
    }
}
