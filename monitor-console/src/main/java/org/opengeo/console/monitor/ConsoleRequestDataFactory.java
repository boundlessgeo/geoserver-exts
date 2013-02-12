package org.opengeo.console.monitor;

import java.util.concurrent.ConcurrentHashMap;

import org.geoserver.monitor.RequestData;

import com.google.common.base.Optional;

public class ConsoleRequestDataFactory {

    private final SystemMonitor systemMonitor;

    private final ConcurrentHashMap<Long, ConsoleData> consoleRequestDataMapping;

    public ConsoleRequestDataFactory(SystemMonitor systemMonitor,
            ConcurrentHashMap<Long, ConsoleData> consoleRequestDataMapping) {
        this.systemMonitor = systemMonitor;
        this.consoleRequestDataMapping = consoleRequestDataMapping;
    }

    public ConsoleRequestData create(RequestData requestData) {
        // currently ConsoleData only gets set in the map from the ows dispatcher
        // what this means is that if it's not an ows request, then we don't have any data for it
        long id = requestData.getId();
        ConsoleData consoleData = consoleRequestDataMapping.get(id);
        Optional<ConsoleData> optionalConsoleData = Optional.fromNullable(consoleData);

        // removing the id from the mapping is important to prevent memory leaks
        consoleRequestDataMapping.remove(id);

        SystemStatSnapshot systemStatSnapshot = systemMonitor.pollSystemStatSnapshot();

        return new ConsoleRequestData(requestData, optionalConsoleData, systemStatSnapshot);
    }

}
