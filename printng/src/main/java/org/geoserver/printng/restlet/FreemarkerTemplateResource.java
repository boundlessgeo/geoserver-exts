package org.geoserver.printng.restlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import org.apache.xml.serialize.XMLSerializer;
import org.geoserver.printng.FreemarkerSupport;
import org.geoserver.printng.spi.DocumentParser;
import org.geoserver.rest.RestletException;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;
import org.w3c.dom.Document;

public class FreemarkerTemplateResource extends Resource {

    public FreemarkerTemplateResource(Request request, Response response) {
        super(null, request, response);
        getVariants().add(new Variant(MediaType.TEXT_HTML));
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        Request request = getRequest();
        String templateName = request.getAttributes().get("template").toString();
        if (!templateName.endsWith(".ftl")) {
            templateName = templateName + ".ftl";
        }
        InputStream input;
        InputStreamReader reader = null;
        Writer writer = null;

        try {
            try {
                input = request.getEntity().getStream();
            } catch (IOException e) {
                throw new RestletException("Failure reading template input",
                        Status.SERVER_ERROR_INTERNAL, e);
            }
            reader = new InputStreamReader(input);
            DocumentParser parser = new DocumentParser(reader);
            Document document;
            try {
                document = parser.parse();
            } catch (IOException e) {
                throw new RestletException("Error parsing invalid input",
                        Status.CLIENT_ERROR_BAD_REQUEST, e);
            }
            try {
                writer = FreemarkerSupport.newTemplateWriter(templateName);
                XMLSerializer xmlSerializer = new XMLSerializer(writer, null);
                xmlSerializer.serialize(document);
                getResponse().setEntity(
                        String.format("Template %s created succesffully%n", templateName),
                        MediaType.TEXT_PLAIN);
            } catch (IOException e) {
                throw new RestletException("Error writing new template",
                        Status.SERVER_ERROR_INTERNAL, e);
            }
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
            }
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
            }
        }
    }

}
