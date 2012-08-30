package org.geoserver.printng.api;

import org.restlet.data.Request;

public interface ReaderSource {

    PrintngReader printngReader(Request request);

}
