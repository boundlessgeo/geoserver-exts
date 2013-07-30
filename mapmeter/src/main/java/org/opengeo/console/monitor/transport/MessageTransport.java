package org.opengeo.console.monitor.transport;

import java.util.Collection;

import org.opengeo.console.monitor.ConsoleRequestData;

/**
 * Takes request data from monitoring, and pushes it to where it needs to go
 * 
 */
public interface MessageTransport {

    /**
     * 
     * @param data request data to transport
     */
    void transport(Collection<ConsoleRequestData> data);

    /**
     * Hook to perform any cleanup, ie shut down thread pools
     */
    void destroy();
}
