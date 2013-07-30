package org.opengeo.console.monitor.config;

import java.io.IOException;

import com.google.common.base.Optional;

public interface MessageTransportConfig {

    // this is where messages will be persisted
    String getStorageUrl();

    // this is where connection checks are made
    String getCheckUrl();

    Optional<String> getApiKey();

    void setStorageUrl(String storageUrl);

    void setCheckUrl(String checkUrl);

    void setApiKey(String apiKey);

    void save() throws IOException;

}
