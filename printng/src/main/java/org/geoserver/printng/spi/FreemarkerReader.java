package org.geoserver.printng.spi;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.geoserver.printng.FreemarkerSupport;
import org.geoserver.printng.api.PrintngReader;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class FreemarkerReader implements PrintngReader {

    private final String templateName;

    private final SimpleHash templateModel;

    public FreemarkerReader(String templateName, SimpleHash templateModel) {
        this.templateName = templateName;
        this.templateModel = templateModel;
    }

    @Override
    public Reader reader() throws IOException {
        Template template = findTemplate(this.templateName);
        StringWriter writer = new StringWriter();
        try {
            template.process(templateModel, writer);
        } catch (TemplateException e) {
            throw new IOException("Error processing template: " + templateName, e);
        }
        String result = writer.toString();
        return new StringReader(result);
    }

    private Template findTemplate(String templateName) throws IOException {
        File templateDirectory = FreemarkerSupport.getPrintngTemplateDirectory();
        Configuration configuration = new Configuration();
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

}
