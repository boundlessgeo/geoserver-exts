package org.geoserver.printng.spi;

import java.util.Set;

import org.geoserver.printng.api.PrintngReader;
import org.geoserver.printng.api.ReaderSource;
import org.geoserver.rest.RestletException;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Status;

import freemarker.template.SimpleHash;

public class FreemarkerReaderSource implements ReaderSource {

    @Override
    public PrintngReader printngReader(Request request) {
        String templateName = request.getAttributes().get("template").toString();
        if (templateName == null) {
            throw new RestletException("No template found", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        Form form = request.getResourceRef().getQueryAsForm();
        SimpleHash simpleHash = new SimpleHash();
        Set<String> names = form.getNames();
        for (String name : names) {
            String value = form.getFirst(name).getValue();
            simpleHash.put(name, value);
        }
        return new FreemarkerReader(templateName, simpleHash);
    }

}
