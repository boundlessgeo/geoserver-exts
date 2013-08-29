package org.opengeo.console.monitor.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

public class MessageTransportConfigProperties implements MessageTransportConfig {

    private final static Logger LOGGER = Logging.getLogger(MessageTransportConfigProperties.class);

    private final String defaultStorageUrl;

    private final String defaultCheckUrl;

    private final String defaultSystemUpdateUrl;

    private final String controllerPropertiesRelPath;

    private final GeoServerResourceLoader loader;

    private Optional<String> storageUrl;

    private Optional<String> checkUrl;

    private Optional<String> apiKey;

    private Optional<String> systemUpdateUrl;

    public MessageTransportConfigProperties(String monitoringDataDirName,
            String controllerPropertiesName, String defaultStorageUrl, String defaultCheckUrl,
            String defaultSystemUpdateUrl, GeoServerResourceLoader loader) {

        this.defaultStorageUrl = defaultStorageUrl;
        this.defaultCheckUrl = defaultCheckUrl;
        this.defaultSystemUpdateUrl = defaultSystemUpdateUrl;
        this.loader = loader;
        this.controllerPropertiesRelPath = monitoringDataDirName + File.separatorChar
                + controllerPropertiesName;

        Optional<String> storageUrl = Optional.absent();
        Optional<String> checkUrl = Optional.absent();
        Optional<String> systemUpdateUrl = Optional.absent();
        Optional<String> apiKey = Optional.absent();

        BufferedReader fileReader = null;

        try {
            Optional<File> propFile = findControllerPropertiesFile();

            if (propFile.isPresent()) {
                Properties properties = new Properties();
                fileReader = Files.newReader(propFile.get(), Charsets.UTF_8);
                properties.load(fileReader);

                String storageUrlString = (String) properties.get("url");
                String checkUrlString = (String) properties.get("checkurl");
                String systemUpdateUrlString = (String) properties.get("systemupdateurl");
                String apiKeyString = (String) properties.get("apikey");

                if (apiKeyString != null) {
                    apiKey = Optional.of(apiKeyString.trim());
                } else {
                    LOGGER.severe("Failure reading 'apikey' property from "
                            + controllerPropertiesName);
                }
                if (storageUrlString != null) {
                    storageUrl = Optional.of(storageUrlString.trim());
                }
                if (checkUrlString != null) {
                    checkUrl = Optional.of(checkUrlString.trim());
                }
                if (systemUpdateUrlString != null) {
                    systemUpdateUrl = Optional.of(systemUpdateUrlString.trim());
                }
            }
        } catch (IOException e) {
            LOGGER.severe("Failure reading: " + controllerPropertiesRelPath + " from data dir");
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(Throwables.getStackTraceAsString(e));
            }
        } finally {
            Closeables.closeQuietly(fileReader);
        }
        this.storageUrl = storageUrl;
        this.checkUrl = checkUrl;
        this.systemUpdateUrl = systemUpdateUrl;
        this.apiKey = apiKey;
    }

    public Optional<File> findControllerPropertiesFile() throws IOException {
        File propFile = loader.find(controllerPropertiesRelPath);
        if (propFile == null) {
            String msg = "Could not find controller properties file in data dir. Expected data dir location: "
                    + controllerPropertiesRelPath;
            LOGGER.warning(msg);
            return Optional.absent();
        } else {
            return Optional.of(propFile);
        }
    }

    @Override
    public String getStorageUrl() {
        return storageUrl.or(defaultStorageUrl);
    }

    @Override
    public String getCheckUrl() {
        return checkUrl.or(defaultCheckUrl);
    }

    @Override
    public Optional<String> getApiKey() {
        return apiKey;
    }

    @Override
    public String getSystemUpdateUrl() {
        return systemUpdateUrl.or(defaultSystemUpdateUrl);
    }

    @Override
    public void setStorageUrl(String storageUrl) {
        this.storageUrl = Optional.of(storageUrl);
    }

    @Override
    public void setCheckUrl(String checkUrl) {
        this.checkUrl = Optional.of(checkUrl);
    }

    @Override
    public void setApiKey(String apiKey) {
        this.apiKey = Optional.of(apiKey);
    }

    public void setSystemUpdateUrl(String systemUpdateUrl) {
        this.systemUpdateUrl = Optional.of(systemUpdateUrl);
    }

    @Override
    public void save() throws IOException {
        Properties properties = new Properties();
        if (!apiKey.isPresent()) {
            throw new IllegalStateException("need api key to save: " + controllerPropertiesRelPath);
        }
        properties.setProperty("apikey", apiKey.get());
        // only persist the storage/check urls if they are set
        if (storageUrl.isPresent()) {
            properties.setProperty("url", storageUrl.get());
        }
        if (checkUrl.isPresent()) {
            properties.setProperty("checkurl", checkUrl.get());
        }
        if (systemUpdateUrl.isPresent()) {
            properties.setProperty("systemupdateurl", systemUpdateUrl.get());
        }

        File propFile = null;
        Optional<File> maybePropFile = findControllerPropertiesFile();
        if (maybePropFile.isPresent()) {
            propFile = maybePropFile.get();
        } else {
            LOGGER.warning("Creating controller properties: " + controllerPropertiesRelPath);
            propFile = loader.createFile(controllerPropertiesRelPath);
        }

        BufferedWriter out = null;
        try {
            out = Files.newWriter(propFile, Charsets.UTF_8);
            properties.store(out, null);
        } finally {
            Closeables.closeQuietly(out);
        }
    }

}
