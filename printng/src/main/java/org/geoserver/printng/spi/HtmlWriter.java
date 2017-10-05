package org.geoserver.printng.spi;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.printng.PrintSupport;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HtmlWriter extends PrintngWriter {

    @Override
    public String getExtension() {
        return "html";
    }

    @Override
    public void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        PrintSupport.write(spec.getDocument(), out, false);
    }
    
}
