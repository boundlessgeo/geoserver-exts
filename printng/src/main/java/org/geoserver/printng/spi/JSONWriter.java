package org.geoserver.printng.spi;

import org.geoserver.printng.GeoserverSupport;
import org.geoserver.printng.api.PrintSpec;
import org.geoserver.printng.api.PrintngWriter;
import org.geoserver.rest.RestException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;

import java.io.*;

/**
 * The JSON response is a single object that points to a URL where the client
 * can retrieve the rendered output.
 * 
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class JSONWriter extends PrintngWriter {
    
    private final PrintngWriter delegate;
    private final String baseURL;

    public JSONWriter(PrintngWriter delegate, String baseURL) {
        this.delegate = delegate;
        this.baseURL = baseURL;
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    protected void writeInternal(PrintSpec spec, OutputStream out) throws IOException {
        File output = GeoserverSupport.getOutputFile(delegate.getExtension());
        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(output));
        IOException error = null;
        try {
            delegate.write(spec, bout);
            String uri = baseURL + GeoserverSupport.getOutputFileURI(output.getAbsolutePath());
            String response = render(uri);
            out.write(response.getBytes());
        } catch (IOException ioe) {
            error = ioe;
        } finally {
            try {
                bout.close();
            } catch (IOException ioe) {
                // pass
            }
        }
        if (error != null) {
            output.delete(); // try now, it will get cleaned up later otherwise
            
            // the existing mapfish print protocol likes a 500
            throw new RestException(error.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private String render(String output) {
        JSONObject resp = new JSONObject();
        try {
            resp.put("getURL", output);
        } catch (JSONException ex) {
            // this shouldn't happen
            throw new RuntimeException(ex);
        }
        return resp.toString();
    }
}
