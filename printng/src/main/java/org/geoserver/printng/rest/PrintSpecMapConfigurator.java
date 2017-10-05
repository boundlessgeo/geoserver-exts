package org.geoserver.printng.rest;

import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintSpecConfigurator;
import org.geoserver.printng.spi.PrintSpecException;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Torben Barsballe <tbarsballe@boundlessgeo.com>
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class PrintSpecMapConfigurator extends PrintSpecConfigurator<MultiValueMap<String, String>> {

    private PrintSpecMapConfigurator(MultiValueMap<String, String> map) {
        super(map);
    }

    private Integer parseInt(String key) {
        String val = source.getFirst(key);
        Integer res = null;
        if (val != null) {
            try {
                res = Integer.valueOf(val);
            } catch (NumberFormatException nfe) {
                messages.severe("Invalid number for '" + key + "' : " + val);
            }
        }
        return res;
    }

    public static PrintSpec configure(PrintSpec spec, MultiValueMap<String, String> map) throws PrintSpecException {
        return new PrintSpecMapConfigurator(map).configure(spec);
    }

    @Override
    protected void configureSpec(PrintSpec spec) {
        Integer val = parseInt("width");
        if (val != null) {
            messages.log(Level.FINE, "setting output width to {0}", val);
            spec.setOutputWidth(val);
        }
        val = parseInt("height");
        if (val != null) {
            messages.log(Level.FINE, "setting output height to {0}", val);
            spec.setOutputHeight(val);
        }
        for (String key : source.keySet()) {
            for (String value : source.get(key)) {
                if ("cookie".equals(key)) {
                    String[] parts = value.split(",");
                    if (parts.length != 3) {
                        messages.severe("Invalid cookie specification");
                    } else {
                        messages.log(Level.FINE, "setting cookie for {0} to {1}={2}", parts);
                        spec.addCookie(parts[0], parts[1], parts[2]);
                    }
                } else if ("auth".equals(key)) {
                    String[] parts = value.split(",");
                    if (parts.length != 3) {
                        messages.severe("Invalid auth specification");
                    } else {
                        messages.log(Level.FINE, "setting credentials for {1} on {0}", parts);
                        spec.addCredentials(parts[0], parts[1], parts[2]);
                    }
                }
            }
        }
    }
}
