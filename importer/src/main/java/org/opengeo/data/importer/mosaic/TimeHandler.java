package org.opengeo.data.importer.mosaic;

import java.util.Date;
import java.util.Map;

/**
 * Handles timestamps for granules in a mosaic. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class TimeHandler implements java.io.Serializable {

    /**
     * Initializes the handler with any properties. 
     * 
     */
    public void init(Map<String,Object> properties) {        
    }

    /**
     * Extracts a timestamp from a mosaic granule. 
     */
    public abstract Date computeTimestamp(Granule g);
}
