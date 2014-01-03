package org.opengeo.mapmeter.monitor.config;

public enum MessageTransportConfigApiKeySource {

    ENVIRONMENT, WEB_CONTEXT, PROPERTIES, NO_KEY;

    public boolean isSourceExternal() {
        return this == ENVIRONMENT || this == WEB_CONTEXT;
    }

}
