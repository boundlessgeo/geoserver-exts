package org.geoserver.printng.rest;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.FilenameUtils;
import org.apache.xml.serialize.XMLSerializer;
import org.geoserver.printng.GeoserverSupport;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.ParsedDocument;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import static org.geoserver.rest.RestBaseController.ROOT_PATH;

@RestController
@RequestMapping(path = {ROOT_PATH + "/printng"})
public class PrintController extends RestBaseController {

    @Autowired
    public PrintController() { }

    /**
     * Workaround to support extension as sole determinator of acceptable media type
     */
    @Configuration
    static class PrintControllerConfiguration {
        @Bean
        ContentNegotiationStrategy printOutputTypeContentNegotiationStrategy() {
            return webRequest -> {
                HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
                if (new PatternsRequestCondition("/printng/render.{ext}", "/printng/freemarker/{template}.{ext}").getMatchingCondition(request) != null) {
                    String extension = FilenameUtils.getExtension(request.getPathInfo());
                    int queryPos = extension.indexOf('?');
                    if (queryPos >= 0) {
                        extension = extension.substring(0, queryPos);
                    }
                    return Collections.singletonList(PrintOutputConverter.mediaType(extension));
                }
                return new ArrayList<>();
            };
        }
    }

    @PostMapping(path = {"render.{ext}"},
            consumes = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE,
                        MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE} )
    public PrintSpec renderHtmlPost(InputStream inputStream,
                                    @RequestParam MultiValueMap<String,String> params) throws IOException {

        return getPrintSpec(new InputStreamReader(inputStream), params);
    }

    @PostMapping(path = {"freemarker/{template}.{ext}"},
            consumes = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE},
            produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE,
                        MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE} )
    public PrintSpec renderFreemarkerPost(@RequestBody SimpleHash simpleHash,
                                          @PathVariable(name = "template") String templateName,
                                          @RequestParam MultiValueMap<String,String> params) throws IOException {
        //From FreeMarkerReaderSource
        if (templateName == null) {
            throw new RestException("No template found", HttpStatus.BAD_REQUEST);
        }
        for (String key : params.keySet()) {
            for (String value : params.get(key)) {
                simpleHash.put(key, value);
            }
        }

        return getPrintSpec(new StringReader(writeTemplate(templateName, simpleHash)), params);
    }

    protected static String writeTemplate(String templateName, SimpleHash model) throws IOException {
        Template template = findTemplate(templateName);
        if (template == null) {
            throw new IOException("Template not found " + templateName);
        }
        StringWriter writer = new StringWriter();
        try {
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new IOException("Error processing template: " + templateName, e);
        }
        return writer.toString();
    }

    private static Template findTemplate(String templateName) throws IOException {
        File templateDirectory = GeoserverSupport.getPrintngTemplateDirectory();
        freemarker.template.Configuration configuration = new freemarker.template.Configuration();
        configuration.setDirectoryForTemplateLoading(templateDirectory);
        Template template;
        try {
            template = configuration.getTemplate(templateName);
        } catch (IOException e) {
            if (!templateName.endsWith(".ftl")) {
                return findTemplate(templateName + ".ftl");
            } else {
                throw e;
            }
        }
        return template;
    }

    private static PrintSpec getPrintSpec(Reader reader, MultiValueMap<String, String> queryParams) throws IOException {
        PrintSpec spec =  new PrintSpec(ParsedDocument.parse(reader));
        PrintSpecMapConfigurator.configure(spec, queryParams);
        return spec;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = {"freemarker/{template}"},
            consumes = {MediaType.TEXT_HTML_VALUE},
            produces = {MediaType.TEXT_PLAIN_VALUE})
    public String modifyFreemarkerPost(InputStream input,
                                       @PathVariable(name = "template") String templateName) {
        if (!templateName.endsWith(".ftl")) {
            templateName = templateName + ".ftl";
        }

        try (InputStreamReader reader = new InputStreamReader(input)) {
            ParsedDocument parsed = ParsedDocument.parse(reader);
            try (Writer writer = GeoserverSupport.newTemplateWriter(templateName)) {
                XMLSerializer xmlSerializer = new XMLSerializer(writer, null);
                xmlSerializer.serialize(parsed.getDocument());
                writer.flush();
                return String.format("Template %s created succesfully%n", templateName);
            } catch (IOException e) {
                throw new RestException("Error writing new template",
                        HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        } catch (IOException e) {
            throw new RestException("Error parsing invalid input",
                    HttpStatus.BAD_REQUEST, e);
        }
    }
}
