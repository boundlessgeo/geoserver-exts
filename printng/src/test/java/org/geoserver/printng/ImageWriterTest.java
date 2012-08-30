package org.geoserver.printng;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.spi.DocumentParser;
import org.geoserver.printng.spi.ImageWriter;
import org.geoserver.printng.spi.MapPrintSpec;
import org.geoserver.printng.spi.PrintSpecException;
import org.junit.Test;
import org.w3c.dom.Document;

import com.google.common.collect.ImmutableMap;

public class ImageWriterTest {

    @Test
    public void testWritePng() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "png", "89504e470d0a1a0a");
    }

    @Test
    public void testWriteJpg() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "jpg", "ffd8");
    }

    @Test
    public void testWriteGif() throws IOException, DecoderException {
        String input = "<div>foobar</div>";
        checkImageWrite(input, "gif", "474946383961");
    }

    private void checkImageWrite(String input, String format, String magic) throws IOException,
            DecoderException {
        DocumentParser parser = new DocumentParser(new StringReader(input));
        Document document = parser.parse();
        Map<String, Integer> options = ImmutableMap.of("width", 100, "height", 50);
        PrintSpec printSpec = new MapPrintSpec(options);
        ImageWriter imageWriter = new ImageWriter(format);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        imageWriter.write(document, printSpec, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(byteArrayInputStream);
        assertEquals("invalid width", 100, image.getWidth());
        assertEquals("invalid height", 50, image.getHeight());
        byte[] expectedBytes = Hex.decodeHex(magic.toCharArray());
        int nMagicBytes = magic.length() / 2;
        byte[] magicBytes = new byte[nMagicBytes];
        System.arraycopy(bytes, 0, magicBytes, 0, nMagicBytes);
        assertArrayEquals("Invalid magic bytes", expectedBytes, magicBytes);
    }

    @Test
    public void testBadPrintSpec() throws Exception {
        ImageWriter imageWriter = new ImageWriter("foo");
        Map<String, Object> map = Collections.emptyMap();
        PrintSpec printSpec = new MapPrintSpec(map);
        try {
            imageWriter.write(null, printSpec, null);
            fail("exception should have been thrown");
        } catch (PrintSpecException e) {
            assertTrue(true);
        }
    }

}
