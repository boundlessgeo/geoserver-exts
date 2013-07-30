package org.geoserver.monitor;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.platform.ExtensionPriority;
import org.opengeo.console.monitor.ConsoleRequestData;
import org.opengeo.console.monitor.ConsoleRequestDataFactory;
import org.opengeo.console.monitor.transport.MessageTransport;

public class MessageTransportPostProcessor implements RequestPostProcessor, ExtensionPriority {

    private final MessageTransport transporter;

    private final ConsoleRequestDataFactory consoleRequestDataFactory;

    public MessageTransportPostProcessor(MessageTransport transporter,
            ConsoleRequestDataFactory consoleRequestDataFactory) {
        this.transporter = transporter;
        this.consoleRequestDataFactory = consoleRequestDataFactory;
    }

    @Override
    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ConsoleRequestData consoleRequestData = consoleRequestDataFactory.create(data);
        transporter.transport(Collections.singletonList(consoleRequestData));
    }

    @Override
    public int getPriority() {
        // we want this extension to run last
        // this allows all others to run first, and then the transport happens last
        return ExtensionPriority.HIGHEST;
    }

}
