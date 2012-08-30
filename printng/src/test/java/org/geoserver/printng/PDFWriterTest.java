package org.geoserver.printng;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.DocumentParser;
import org.geoserver.printng.spi.MapPrintSpec;
import org.geoserver.printng.spi.PDFWriter;
import org.junit.Test;
import org.w3c.dom.Document;

public class PDFWriterTest {

    @Test
    public void testWrite() throws IOException {
        String input = "<div>foobar</div>";
        DocumentParser parser = new DocumentParser(new StringReader(input));
        Document document = parser.parse();
        PDFWriter pdfWriter = new PDFWriter();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Map<String, Object> options = Collections.emptyMap();
        PrintSpec printSpec = new MapPrintSpec(options);
        pdfWriter.write(document, printSpec, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        byte[] magicBytes = new byte[4];
        System.arraycopy(bytes, 0, magicBytes, 0, 4);
        String magic = new String(magicBytes);
        assertEquals("invalid pdf bytes", "%PDF", magic);
    }
}
