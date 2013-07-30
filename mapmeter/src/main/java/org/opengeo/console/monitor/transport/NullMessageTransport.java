package org.opengeo.console.monitor.transport;

import java.util.Collection;

import org.opengeo.console.monitor.ConsoleRequestData;

public class NullMessageTransport implements MessageTransport {

    @Override
    public void transport(Collection<ConsoleRequestData> data) {
    }

    @Override
    public void destroy() {
    }

}
