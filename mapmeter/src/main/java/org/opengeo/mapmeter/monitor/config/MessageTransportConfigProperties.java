package org.opengeo.mapmeter.monitor.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

public class MessageTransportConfigProperties implements MessageTransportConfig,
        ApplicationContextAware {

    private static final String MAPMETER_ENV_NAME = "MAPMETER_API_KEY";

    private final static Logger LOGGER = Logging.getLogger(MessageTransportConfigProperties.class);

    private final String defaultStorageUrl;

    private final String defaultCheckUrl;

    private final String defaultSystemUpdateUrl;

    private final String controllerPropertiesRelPath;

    private final GeoServerResourceLoader loader;

    private Optional<String> storageUrl;

    private Optional<String> checkUrl;

    private Optional<String> systemUpdateUrl;

    private Optional<String> apiKeyProperties;

    private Optional<String> apiKeyWebContext;

    private Optional<String> apiKeyEnvironmentVariable;

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

        // Check for the mapmeter key in both the java properties and environment variables
        // environment variables trump java properties
        String envVar = System.getenv(MAPMETER_ENV_NAME);
        String javaProperty = System.getProperty(MAPMETER_ENV_NAME);
        apiKeyEnvironmentVariable = Optional.fromNullable(envVar).or(
                Optional.fromNullable(javaProperty));

        // the logic for this happens in setApplicationContext
        // setting it here for now though just so it always has a value
        this.apiKeyWebContext = Optional.absent();

        this.storageUrl = storageUrl;
        this.checkUrl = checkUrl;
        this.systemUpdateUrl = systemUpdateUrl;
        this.apiKeyProperties = apiKey;
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
        // env trumps web.xml trumps config properties
        return apiKeyEnvironmentVariable.or(apiKeyWebContext).or(apiKeyProperties);
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
        this.apiKeyProperties = Optional.of(apiKey);
    }

    public void setSystemUpdateUrl(String systemUpdateUrl) {
        this.systemUpdateUrl = Optional.of(systemUpdateUrl);
    }

    @Override
    public void save() throws IOException {
        Properties properties = new Properties();
        // it's possible that the api key is coming from an environment variable or servlet context (web.xml)
        // only set the api key if it's meant to be set in the properties file
        if (apiKeyProperties.isPresent()) {
            properties.setProperty("apikey", apiKeyProperties.get());
        }
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        synchronized (this) {
            apiKeyWebContext = Optional.absent();
            if (applicationContext instanceof WebApplicationContext) {
                WebApplicationContext webApplicationContext = (WebApplicationContext) applicationContext;
                ServletContext servletContext = webApplicationContext.getServletContext();
                String apiKeyParam = servletContext.getInitParameter(MAPMETER_ENV_NAME);
                apiKeyWebContext = Optional.fromNullable(apiKeyParam);
            }
        }
    }

    @Override
    public MessageTransportConfigApiKeySource getApiKeySource() {
        if (apiKeyEnvironmentVariable.isPresent()) {
            return MessageTransportConfigApiKeySource.ENVIRONMENT;
        } else if (apiKeyWebContext.isPresent()) {
            return MessageTransportConfigApiKeySource.WEB_CONTEXT;
        } else if (apiKeyProperties.isPresent()) {
            return MessageTransportConfigApiKeySource.PROPERTIES;
        } else {
            return MessageTransportConfigApiKeySource.NO_KEY;
        }
    }

}
