package org.geoserver.printng.experimental.map;

import com.vividsolutions.jts.geom.Envelope;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geoserver.printng.experimental.map.HTMLMapLayer.Tile;

/**
 * A 'port' of OpenLayers.Map. Not trying to be complete.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class HTMLMap {
    
    static final Map<String,Double> INCHES_PER_UNIT = new HashMap<String,Double>();
    
    static {
        INCHES_PER_UNIT.put("inches", 1.);
        INCHES_PER_UNIT.put("ft", 12.);
        INCHES_PER_UNIT.put("mi", 63360.);
        INCHES_PER_UNIT.put("m", 39.3701);
        INCHES_PER_UNIT.put("km", 39370.1);
        INCHES_PER_UNIT.put("dd", 4374754.);
        INCHES_PER_UNIT.put("yd", 36.);
        // @todo aliases
    }
    
    static double getResolutionFromScale(double scale,String units,int dpi) {
        assert INCHES_PER_UNIT.containsKey(units);
        return 1 / (normalizeScale(scale) * INCHES_PER_UNIT.get(units) * dpi);
    }
    
    static double normalizeScale(double scale) {
        return (scale > 1.0) ? (1.0 / scale) 
                                  : scale;
    }
    
    private final double centerLat;
    private final double centerLon;
    private int width = 400;
    private int height = 400;
    private Double resolution;
    private final double scale;
    private String units = "dd";
    private int dpi = 72;
    private final String srs;
    private List<HTMLMapLayer> layers;
    private String mapId = "map";
    private Envelope bounds;
    
    public HTMLMap(String srs,double centerLat, double centerLon, double scale) throws Exception {
        this.srs = srs;
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.scale = scale;
        layers = new ArrayList<HTMLMapLayer>();
    }
    
    public void setMapID(String id) {
        this.mapId = id;
    }
    
    public void setDPI(int dpi) {
        this.dpi = dpi;
        resolution = null;
    }
    
    public void setUnits(String units) throws IllegalArgumentException {
        String lunits = units.toLowerCase();
        if (! INCHES_PER_UNIT.containsKey(lunits)) {
            throw new IllegalArgumentException(units + " not a supported unit like : " + INCHES_PER_UNIT.keySet());
        }
        this.units = lunits;
        resolution = null;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    double getResolution() {
        if (resolution == null) {
            resolution = getResolutionFromScale(scale, units, dpi);
        }
        return resolution;
    }
    
    Envelope getBounds() {
        if (bounds == null) {
            double r = getResolution();
            double w_deg = width * r;
            double h_deg = height * r;
            bounds = new Envelope(
                    centerLon - w_deg / 2,
                    centerLon + w_deg / 2,
                    centerLat - h_deg / 2,
                    centerLat + h_deg / 2
            );
        }
        return bounds;
    }
    
    public String render() throws XMLStreamException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLStreamWriter out = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
        render(out);
        out.flush();
        return new String(baos.toByteArray());
    }
    
    void prepareForRender() {
        
    }
    
    public void render(XMLStreamWriter out) throws XMLStreamException {
        prepareForRender();
        
        // map div and viewport combined
        out.writeStartElement("div");
        out.writeAttribute("id", mapId);
        writeStyle(out,
            "width",width + "px",
            "height", height + "px",
            "position","relative",
            "overflow","hidden"
        );
        
        // container
        out.writeStartElement("div");
        writeStyle(out,
            "left","0",
            "top","0"
        );
        
        renderLayers(out);
        
        // end container
        out.writeEndElement();
        // end map div
        out.writeEndElement();
    }

    private void renderLayers(XMLStreamWriter out) throws XMLStreamException {
        for (int i = 0; i < layers.size(); i++) {
            renderLayer(layers.get(i),out);
        }
    }

    private void renderLayer(HTMLMapLayer layer, XMLStreamWriter out) throws XMLStreamException {
        List<Tile> tiles = layer.getTiles(width, height, srs, getBounds(),getResolution());
        for (Tile t: tiles) {
            t.write(out);
        }
    }

    static void writeStyle(XMLStreamWriter out, String ... styles) throws XMLStreamException {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < styles.length; i+=2) {
            builder.append(styles[i]).append(':');
            builder.append(styles[i+1]);
            if (i + 2 < styles.length) {
                builder.append(';');
            }
        }
        out.writeAttribute("style", builder.toString());
    }

    void addLayer(HTMLMapLayer layer) {
        layers.add(layer);
        layer.setMap(this);
    }
    
}
