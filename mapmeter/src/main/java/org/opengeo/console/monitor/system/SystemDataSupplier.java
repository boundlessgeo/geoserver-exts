package org.opengeo.console.monitor.system;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.geoserver.ManifestLoader;
import org.geoserver.ManifestLoader.AboutModel;
import org.geoserver.ManifestLoader.AboutModel.ManifestModel;
import org.opengeo.console.monitor.config.MessageTransportConfig;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

public class SystemDataSupplier implements Supplier<Optional<SystemData>> {

    private final MessageTransportConfig config;

    // takes a manifest loader to ensure that the constructor has already been called to set internal state
    public SystemDataSupplier(MessageTransportConfig config, ManifestLoader manifestLoader) {
        this.config = config;
    }

    @Override
    public Optional<SystemData> get() {

        Optional<String> maybeApiKey = config.getApiKey();

        if (!maybeApiKey.isPresent()) {
            return Optional.absent();
        }

        String apiKey = maybeApiKey.get();

        String osArch = System.getProperty("os.arch");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersion = System.getProperty("java.version");

        String type = getType();
        Optional<String> serverVersion = getServerVersion();

        SystemData systemData = new SystemData(apiKey, osArch, osName, osVersion, javaVendor,
                javaVersion, type, serverVersion);
        return Optional.of(systemData);
    }

    public Optional<String> getServerVersion() {
        Optional<String> serverVersion = Optional.absent();

        AboutModel versions = ManifestLoader.getVersions();
        Set<ManifestModel> manifests = versions.getManifests();
        for (ManifestModel manifestModel : manifests) {
            String name = manifestModel.getName();
            if ("GeoServer".equals(name)) {
                Map<String, String> entries = manifestModel.getEntries();
                for (Entry<String, String> entry : entries.entrySet()) {
                    if ("Version".equals(entry.getKey())) {
                        serverVersion = Optional.of(entry.getValue());
                        break;
                    }
                }
                break;
            }
        }
        return serverVersion;
    }

    public String getType() {
        String markerClass = "org.opengeo.OpenGeoTheme";
        try {
            Class.forName(markerClass);
        } catch (ClassNotFoundException e) {
            return "community";
        }
        return "suite";
    }

}
