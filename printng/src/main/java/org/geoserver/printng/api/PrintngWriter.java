package org.geoserver.printng.api;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.printng.PrintUserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;

public abstract class PrintngWriter {
    
    private PrintUserAgentCallback callback;

    public final void write(PrintSpec spec, OutputStream out) throws IOException {
        writeInternal(spec, out);
        // html writer won't use configure
        if (callback != null) {
            callback.cleanup();
        }
    }

    public abstract String getExtension();
    
    protected final void configure(SharedContext context, PrintSpec spec) throws IOException {
        String baseURL = spec.getBaseURL();
        if (baseURL != null && !baseURL.isEmpty()) {
            context.setBaseURL(baseURL);
        }
        int dotsPerPixel = spec.getDotsPerPixel();
        if (dotsPerPixel > 0) {
            context.setDotsPerPixel(dotsPerPixel);
        }
        callback = new PrintUserAgentCallback(spec, context.getUserAgentCallback());
        callback.preload();
        context.setUserAgentCallback(callback);
        configureInternal(context, callback);
    }

    protected abstract void writeInternal(PrintSpec spec, OutputStream out) throws IOException;

    protected void configureInternal(SharedContext context, PrintUserAgentCallback callback) {
        
    }

}
