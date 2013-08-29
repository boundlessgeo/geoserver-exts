package org.opengeo.console.monitor.system;

import com.google.common.base.Optional;

public class SystemData {

    private final String apiKey;

    private final String osArch;

    private final String osName;

    private final String osVersion;

    private final String javaVendor;

    private final String javaVersion;

    private final String type;

    private final Optional<String> serverVersion;

    public SystemData(String apiKey, String osArch, String osName, String osVersion,
            String javaVendor, String javaVersion, String type, Optional<String> serverVersion) {
        this.apiKey = apiKey;
        this.osArch = osArch;
        this.osName = osName;
        this.osVersion = osVersion;
        this.javaVendor = javaVendor;
        this.javaVersion = javaVersion;
        this.type = type;
        this.serverVersion = serverVersion;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getType() {
        return type;
    }

    public Optional<String> getServerVersion() {
        return serverVersion;
    }

}
