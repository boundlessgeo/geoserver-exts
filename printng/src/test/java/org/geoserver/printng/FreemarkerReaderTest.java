package org.geoserver.printng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.printng.spi.FreemarkerReader;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;

import freemarker.template.SimpleHash;

public class FreemarkerReaderTest extends GeoServerTestSupport {

    @Test
    public void testReaderNotFound() throws IOException {
        FreemarkerReader templateReader = new FreemarkerReader("foo", null);
        try {
            templateReader.reader();
        } catch (IOException e) {
            assertTrue(true);
            return;
        }
        fail("Expecting IOException to be thrown");
    }

    @Test
    public void testReaderFound() throws IOException {
        FreemarkerReader freemarkerReader = new FreemarkerReader("foo", null);
        createTemplate("foo", new StringReader("<div>foobar</div>"));
        Reader reader = freemarkerReader.reader();
        String result = IOUtils.toString(reader);
        assertEquals("Invalid template contents", "<div>foobar</div>", result);
    }

    @Test
    public void testReaderFoundWithParams() throws IOException {
        SimpleHash simpleHash = new SimpleHash();
        simpleHash.put("quux", "morx");
        FreemarkerReader freemarkerTemplateReader = new FreemarkerReader("foo",
                simpleHash);
        createTemplate("foo", new StringReader("<div>${quux}</div>"));
        Reader reader = freemarkerTemplateReader.reader();
        String result = IOUtils.toString(reader);
        assertEquals("Invalid template interpoloation", "<div>morx</div>", result);
    }

    @Test
    public void testReaderFoundMissingParams() throws IOException {
        SimpleHash simpleHash = new SimpleHash();
        simpleHash.put("quux", "morx");
        FreemarkerReader freemarkerTemplateReader = new FreemarkerReader("foo",
                simpleHash);
        createTemplate("foo", new StringReader("<div>${fleem}</div>"));
        try {
            freemarkerTemplateReader.reader();
        } catch (IOException e) {
            assertTrue(true);
            return;
        }
        fail("Expected IOException thrown for processing bad template params");
    }

    private void createTemplate(String templateName, Reader inputReader) throws IOException {
        GeoServerDataDirectory geoServerDataDirectory = GeoServerExtensions
                .bean(GeoServerDataDirectory.class);
        File templateDir = geoServerDataDirectory.findOrCreateDir("printng", "templates");
        File template = new File(templateDir, templateName);
        FileWriter fileWriter = new FileWriter(template);
        IOUtils.copy(inputReader, fileWriter);
        fileWriter.close();
    }

}
