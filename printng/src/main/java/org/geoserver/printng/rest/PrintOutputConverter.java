package org.geoserver.printng.rest;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.printng.spi.*;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Writes a {@link PrintSpec}
 */
@Component
public class PrintOutputConverter extends BaseMessageConverter<PrintSpec> {

    @Autowired
    public PrintOutputConverter() {
        super(MediaType.APPLICATION_PDF, MediaType.IMAGE_GIF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG,
                MediaType.APPLICATION_JSON, MediaType.TEXT_HTML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return PrintSpec.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    @Override
    protected void writeInternal(PrintSpec printSpec, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        PrintngWriter writer = getWriter(outputMessage.getHeaders().getContentType());

        try {
            Logging.getLogger(getClass()).info("printing with " + printSpec);
            writer.write(printSpec, outputMessage.getBody());
        } catch (PrintSpecException e) {
            throw new RestException(e.getMessage(), HttpStatus.BAD_REQUEST, e);
        }
    }

    protected static MediaType mediaType(String extension) {

        extension = extension.toLowerCase();
        if ("pdf".equals(extension)) {
            return MediaType.APPLICATION_PDF;
        } else if ("jpg".equals(extension)) {
            return MediaType.IMAGE_JPEG;
        } else if ("png".equals(extension)) {
            return MediaType.IMAGE_PNG;
        } else if ("gif".equals(extension)) {
            return MediaType.IMAGE_GIF;
        } else if ("html".equals(extension)) {
            return MediaType.TEXT_HTML;
        } else if ("json".equals(extension)) {
            return MediaType.APPLICATION_JSON;
        } else {
            String error = String.format("invalid format \"%s\"", extension);
            throw new RestException(error, HttpStatus.BAD_REQUEST);
        }
    }

    protected static PrintngWriter getWriter(MediaType contentType) {
        if (MediaType.APPLICATION_PDF.equals(contentType)) {
            return new PDFWriter();
        } else if (MediaType.IMAGE_JPEG.equals(contentType)) {
            return new ImageWriter("jpg");
        } else if (MediaType.IMAGE_PNG.equals(contentType)) {
            return new ImageWriter("png");
        } else if (MediaType.IMAGE_GIF.equals(contentType)) {
            return new ImageWriter("gif");
        } else if (MediaType.TEXT_HTML.equals(contentType)) {
            return new HtmlWriter();
        } else if (MediaType.APPLICATION_JSON.equals(contentType)) {

            // the json response type requires a format parameter that will
            // drive the actual output format
            RequestInfo requestInfo = RequestInfo.get();
            Map<String, String[]> queryMap = requestInfo.getQueryMap();
            if (queryMap != null) {
                String[] format = queryMap.get("format");
                if (format != null && format.length > 0) {
                    return  new JSONWriter(getWriter(mediaType(format[0])), requestInfo.getBaseURL());
                }
            }
            throw new RestException("json response requires additional 'format' parameter", HttpStatus.BAD_REQUEST);
        }
        String error = String.format("invalid format \"%s\"", contentType.getSubtype());
        throw new RestException(error, HttpStatus.BAD_REQUEST);
    }
}
