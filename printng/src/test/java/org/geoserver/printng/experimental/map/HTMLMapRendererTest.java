/*
 */
package org.geoserver.printng.experimental.map;

import junit.framework.TestCase;

import org.geoserver.printng.experimental.map.HTMLMap;
import org.geoserver.printng.experimental.map.HTMLMapLayer;


/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLMapRendererTest extends TestCase {
    
    public HTMLMapRendererTest() {
    }

    public void testRenderer() throws Exception {
        HTMLMap r = new HTMLMap("EPSG:4326",
                40,-101, 34879630);
        r.setSize(660, 330);
        r.addLayer(new HTMLMapLayer.WMSLayer("http://demo.opengeo.org/geoserver/topp/wms",
                "image/png",1,"topp:naturalearth"));
        r.addLayer(new HTMLMapLayer.WMSLayer("http://demo.opengeo.org/geoserver/topp/wms",
                "image/png",.5f,"topp:states"));
        System.out.println(r.render());
    }
}
