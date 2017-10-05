package testsupport;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.geoserver.printng.GeoserverSupport;
import org.geotools.util.logging.Logging;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public final class PrintTestSupport {
    
    private PrintTestSupport() {}
    
    public static MultiValueMap<String, String> map(String... kvp) {
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (int i = 0; i < kvp.length; i += 2) {
            map.add(kvp[i], kvp[i + 1]);
        }
        return map;
    }
    
    public static void assertTemplateExists(String path) throws IOException {
        File f = new File(GeoserverSupport.getPrintngTemplateDirectory(), path);
        assertTrue("expected template : " + f.getPath(), f.exists());
    } 
    
    public static void assertPNG(InputStream bytes, int width, int height) {
        BufferedImage read = null;
        try {
            read = ImageIO.read(bytes);
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("Error reading image");
        }
        if (read == null) {
            fail("Expected image to be read - must not be image content");
        }
        assertEquals(width, read.getWidth());
        assertEquals(height, read.getHeight());
    }
    
    public static void assertPDF(ByteArrayOutputStream baos) throws IOException {
        assertPDF(new ByteArrayInputStream(baos.toByteArray()));
    }
    
    public static void assertPDF(InputStream bytes) throws IOException {
        byte[] magicBytes = new byte[4];
        int read = bytes.read(magicBytes);
        assertEquals("expected at least 4 bytes for PDF response", 4, read);
        String magic = new String(magicBytes);
        assertEquals("invalid pdf bytes", "%PDF", magic);
    }
    
    public static class LogCollector extends Handler {
        
        public List<LogRecord> records = new ArrayList<LogRecord>();
        private final Logger logger;
        private final Level returnLevel;
        
        private LogCollector(Logger logger) {
            this.logger = logger;
            this.returnLevel = logger.getLevel();
        }
        
        public void detach() {
            logger.removeHandler(this);
            logger.setLevel(returnLevel == null ? Level.INFO : returnLevel);
        }
        
        public static LogCollector attach(Logger logger, Level level) {
            LogCollector lc = new LogCollector(logger);
            lc.setLevel(level);
            logger.addHandler(lc);
            logger.setLevel(level);
            return lc;
        }
        
        public static LogCollector attach(Class<?> logClass, Level level) {
            return attach(Logging.getLogger(logClass), level);
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
        
    }
}
