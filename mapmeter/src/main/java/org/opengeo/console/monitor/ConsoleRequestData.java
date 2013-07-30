package org.opengeo.console.monitor;

import org.geoserver.monitor.RequestData;

import com.google.common.base.Optional;

public class ConsoleRequestData {

    private final RequestData requestData;

    private final Optional<ConsoleData> consoleData;

    private final SystemStatSnapshot systemStatSnapshot;

    public ConsoleRequestData(RequestData requestData, Optional<ConsoleData> consoleData,
            SystemStatSnapshot systemStatSnapshot) {
        this.requestData = requestData;
        this.consoleData = consoleData;
        this.systemStatSnapshot = systemStatSnapshot;
    }

    public RequestData getRequestData() {
        return requestData;
    }

    public Optional<ConsoleData> getConsoleData() {
        return consoleData;
    }

    public SystemStatSnapshot getSystemStatSnapshot() {
        return systemStatSnapshot;
    }

}
