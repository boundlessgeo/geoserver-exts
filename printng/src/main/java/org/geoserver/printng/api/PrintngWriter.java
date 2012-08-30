package org.geoserver.printng.api;

import java.io.IOException;
import java.io.OutputStream;

import org.w3c.dom.Document;

public interface PrintngWriter {

    void write(Document document, PrintSpec spec, OutputStream out) throws IOException;

}
