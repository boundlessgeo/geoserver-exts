package org.opengeo.mapmeter.monitor.config;

import java.io.IOException;

import com.google.common.base.Optional;

public interface MessageTransportConfig {

    // this is where messages will be persisted
    String getStorageUrl();

    // this is where connection checks are made
    String getCheckUrl();

    // where system information gets sent
    String getSystemUpdateUrl();

    Optional<String> getApiKey();

    void setStorageUrl(String storageUrl);

    void setCheckUrl(String checkUrl);

    void setApiKey(String apiKey);

    void save() throws IOException;

    // the api key can be overridden by an environment variable or from the servlet context
    boolean isApiKeyOverridden();

}
