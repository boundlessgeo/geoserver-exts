package org.geoserver.printng.rest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.geoserver.printng.GeoserverSupport;
import org.junit.Test;
import org.junit.AfterClass;
import static org.junit.Assert.*;

import freemarker.template.SimpleHash;
import org.apache.commons.io.FileUtils;

public class PrintControllerTemplateWriterTest {
    
    @AfterClass
    public static void cleanTempFiles() throws IOException {
        File templateDir = GeoserverSupport.getPrintngTemplateDirectory();
        FileUtils.cleanDirectory(templateDir);
        if (!templateDir.delete()) {
            throw new IOException("Failure removing template dir: " + templateDir.getPath());
        }
    }

    @Test
    public void testReaderNotFound() throws IOException {
        try {        
            PrintController.writeTemplate("doesnotexist", null);
            fail("Expecting IOException to be thrown");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testReaderFound() throws IOException {        
        createTemplate("foo", new StringReader("<div>foobar</div>"));

        String result = PrintController.writeTemplate("foo", null);
        assertEquals("Invalid template contents", "<div>foobar</div>", result);
    }

    @Test
    public void testReaderFoundWithParams() throws IOException {
        SimpleHash simpleHash = new SimpleHash();
        simpleHash.put("quux", "morx");
        createTemplate("foo", new StringReader("<div>${quux}</div>"));
        String result = PrintController.writeTemplate("foo", simpleHash);
        assertEquals("Invalid template interpoloation", "<div>morx</div>", result);
    }

    @Test
    public void testReaderFoundMissingParams() throws IOException {
        SimpleHash simpleHash = new SimpleHash();
        simpleHash.put("quux", "morx");
        createTemplate("foo", new StringReader("<div>${fleem}</div>"));
        try {
            PrintController.writeTemplate("foo", simpleHash);
            fail("Expected IOException thrown for processing bad template params");
        } catch (IOException e) {
        }
    }

    public static void createTemplate(String templateName, Reader inputReader) throws IOException {
        File templateDir = GeoserverSupport.getPrintngTemplateDirectory();
        if (!templateDir.exists() && !templateDir.mkdir()) {
            throw new IOException("Error creating template dir: " + templateDir.getPath());
        }
        File template = new File(templateDir, templateName);
        FileWriter fileWriter = new FileWriter(template);
        IOUtils.copy(inputReader, fileWriter);
        fileWriter.close();
    }

}
