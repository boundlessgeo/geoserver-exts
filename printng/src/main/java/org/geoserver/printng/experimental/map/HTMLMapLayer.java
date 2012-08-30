/*
 */
package org.geoserver.printng.experimental.map;

import com.vividsolutions.jts.geom.Envelope;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Analagous to OpenLayers.Layer.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
abstract class HTMLMapLayer {
    
    final String url;
    final String[] layers;
    final String format;
    final float opacity;
    String units;
    double[] resolutions;
    HTMLMap map;
    
    public HTMLMapLayer(String url,String format,float opacity,String ... layers) {
        if (url == null) throw new NullPointerException("url");
        this.url = url;
        this.format = format == null ? "image/png" : format;
        this.layers = layers;
        this.opacity = opacity;
    }
    
    double[] getResolutions() {
        return resolutions;
    }
    
    abstract List<Tile> getTiles(int width,int height,String srs,Envelope bounds,double resolution);
    
    abstract String getFullURL(int width,int height,String srs,Envelope bounds);

    final void setMap(HTMLMap map) {
        this.map = map;
    }
            
    static class WMSLayer extends HTMLMapLayer {

        public WMSLayer(String url, String format, float opacity, String... layers) {
            super(url, format, opacity, layers);
        }
        
        String getFullURL(int width,int height,String srs,Envelope bounds) {
            StringBuilder buf = new StringBuilder(url);
            if (!url.endsWith("?")) {
                buf.append('?');
            }

            buf.append("LAYERS=");
            for (int i = 0; i < layers.length; i++) {
                buf.append(layers[i]);
                if (i + 1 < layers.length) {
                    buf.append(',');
                }
            }
            buf.append('&');

            buf.append("REQUEST=GetMap&");
            buf.append("STYLES=&");
            buf.append("FORMAT=").append(format).append('&');
            buf.append("SRS=").append(srs).append('&');

            buf.append("BBOX=");
            buf.append(bounds.getMinX()).append(',');
            buf.append(bounds.getMinY()).append(',');
            buf.append(bounds.getMaxX()).append(',');
            buf.append(bounds.getMaxY());
            buf.append('&');

            buf.append("WIDTH=").append(width).append('&');
            buf.append("HEIGHT=").append(height);

            return buf.toString();
        }

        @Override
        List<Tile> getTiles(int width, int height, String srs, Envelope bounds, double resolution) {
            return Collections.singletonList(new Tile(
                    new Point(0,0),
                    new Dimension(width,height),
                    bounds,
                    srs,
                    this
            ));
        }
    }
    
    static abstract class Grid extends HTMLMapLayer {

        Dimension tileSize;
        int buffer = 0;
        double tilelon;
        double tilelat;
        double tileoffsetlon;
        double tileoffsetlat;
        int tileoffsetx;
        int tileoffsety;

        Grid(String url, String format, float opacity, Dimension tileSize, String... layers) {
            super(url, format, opacity, layers);
            this.tileSize = tileSize;
        }

        void calculateGridLayout(Envelope bounds, Envelope extent, double resolution) {
            tilelon = resolution * this.tileSize.width;
            tilelat = resolution * this.tileSize.height;

            double offsetlon = bounds.getMinX() - extent.getMinX();
            double tilecol = Math.floor(offsetlon / tilelon) - this.buffer;
            double tilecolremain = offsetlon / tilelon - tilecol;
            tileoffsetx = (int) Math.round(-tilecolremain * this.tileSize.width);
            tileoffsetlon = extent.getMinX() + tilecol * tilelon;

            double offsetlat = bounds.getMaxY() - (extent.getMinY() + tilelat);
            double tilerow = Math.ceil(offsetlat / tilelat) + this.buffer;
            double tilerowremain = tilerow - offsetlat / tilelat;
            tileoffsety = (int) Math.round(-tilerowremain * this.tileSize.height);
            tileoffsetlat = extent.getMinY() + tilerow * tilelat;
        }

        @Override
        List<Tile> getTiles(int width, int height, String srs, Envelope bounds, double resolution) {
            List<Tile> tiles = new ArrayList<Tile>();
            // work out mininum number of rows and columns; this is the number of
            // tiles required to cover the viewport plus at least one for panning

            Dimension viewSize = new Dimension(width, height);
            int minRows = (int) Math.ceil((double)viewSize.height / (double)this.tileSize.height)
                    + Math.max(1, 2 * this.buffer);
            int minCols = (int) Math.ceil((double)viewSize.width / (double)this.tileSize.width)
                    + Math.max(1, 2 * this.buffer);

            //var extent = this.getMaxExtent();
            //var resolution = this.map.getResolution();

            calculateGridLayout(bounds, /*
                     * extent
                     */ bounds, resolution);

//        int tileoffsetx = Math.round(tileoffsetx); // heaven help us
//        int tileoffsety = Math.round(tileoffsety);

//        var tileoffsetlon = tileLayout.tileoffsetlon;
//        var tileoffsetlat = tileLayout.tileoffsetlat;
//        
//        var tilelon = tileLayout.tilelon;
//        var tilelat = tileLayout.tilelat;

            // this.origin = new OpenLayers.Pixel(tileoffsetx, tileoffsety);

            int startX = tileoffsetx;
            double startLon = tileoffsetlon;

            int rowidx = 0;

//        var layerContainerDivLeft = parseInt(this.map.layerContainerDiv.style.left);
//        var layerContainerDivTop = parseInt(this.map.layerContainerDiv.style.top);
            int layerContainerDivLeft = 0;
            int layerContainerDivTop = 0;


            do {
                rowidx++;
                tileoffsetlon = startLon;
                tileoffsetx = startX;
                int colidx = 0;

                do {
                    Envelope tileBounds = new Envelope(tileoffsetlon, tileoffsetlon + tilelon,
                            tileoffsetlat, tileoffsetlat + tilelat);
//                    new Envelope(tileoffsetlon, 
//                                          tileoffsetlat, 
//                                          tileoffsetlon + tilelon,
//                                          tileoffsetlat + tilelat);

                    int x = tileoffsetx;
                    x -= layerContainerDivLeft;

                    int y = tileoffsety;
                    y -= layerContainerDivTop;

                    colidx++;
//                var px = new OpenLayers.Pixel(x, y);
//                var tile = row[colidx++];
//                if (!tile) {
//                    tile = this.addTile(tileBounds, px);
//                    this.addTileMonitoringHooks(tile);
//                    row.push(tile);
//                } else {
//                    tile.moveTo(tileBounds, px, false);
//                }
                    tiles.add(new Tile(new Point(x, y), tileSize, tileBounds, srs, this));

                    tileoffsetlon += tilelon;
                    tileoffsetx += this.tileSize.width;
                } while ((tileoffsetlon <= bounds.getMaxX() + tilelon * this.buffer)
                        || colidx < minCols);

                tileoffsetlat -= tilelat;
                tileoffsety += this.tileSize.height;
            } while ((tileoffsetlat >= bounds.getMinY() - tilelat * this.buffer)
                    || rowidx < minRows);

            return tiles;
        }
    }
    
    static class XYZ extends Grid {
//        private double resolution;
//        maxExtent: new OpenLayers.Bounds(
//                    -128 * 156543.0339,
//                    -128 * 156543.0339,
//                    128 * 156543.0339,
//                    128 * 156543.0339
//                )
        public XYZ(String url, String format, float opacity, Dimension tileSize, String... layers) {
            super(url, format, opacity, tileSize, layers);
            resolutions = new double[] {
              156543.03390625, 78271.516953125, 39135.7584765625, 19567.87923828125, 9783.939619140625, 
              4891.9698095703125, 2445.9849047851562, 1222.9924523925781, 611.4962261962891, 305.74811309814453, 
              152.87405654907226, 76.43702827453613, 38.218514137268066, 19.109257068634033, 9.554628534317017, 
              4.777314267158508, 2.388657133579254, 1.194328566789627, 0.5971642833948135
            };
        }
        
        @Override
        List<Tile> getTiles(int width, int height, String srs, Envelope bounds, double resolution) {
            // @hack
//            this.resolution = resolution;
            return super.getTiles(width, height, srs, bounds, resolution);
        }

        @Override
        String getFullURL(int width, int height, String srs, Envelope bounds) {
//            double res = resolution;
//            int x = Math.round((bounds.getMinX() - this.maxExtent.left) 
//                / (res * this.tileSize.w));
//            int y = Math.round((this.maxExtent.top - bounds.top) 
//                / (res * this.tileSize.h));
//            int z = this.map.getZoom() + this.zoomOffset;
//
//            var url = this.url;
//            var s = '' + x + y + z;
//            if (url instanceof Array)
//            {
//                url = this.selectUrl(s, url);
//            }
//
//            var path = OpenLayers.String.format(url, {'x': x, 'y': y, 'z': z});
//
//            return path;
            throw new UnsupportedOperationException();
        }
        
        
    }
    
//    static class OSM extends XYZ {
//
//    }
    
    static class Tile {
        
        Point position;
        Dimension size;
        Envelope bounds;
        HTMLMapLayer layer;
        String srs;

        Tile(Point position, Dimension size,Envelope bounds,String srs, HTMLMapLayer layer) {
            this.position = position;
            this.size = size;
            this.layer = layer;
            this.bounds = bounds;
            this.srs = srs;
        }

        void write(XMLStreamWriter out) throws XMLStreamException {
                    // skip the outer wrapper
            out.writeStartElement("img");
            HTMLMap.writeStyle(out,
                    "width",size.width + "px",
                    "height",size.height + "px",
                    "position","absolute",
                    "top","0",
                    "left","0",
                    "opacity","" + layer.opacity
                    );
            out.writeAttribute("src", layer.getFullURL(size.width,size.height,srs,bounds));
            out.writeEndElement();
        }
    }
    
}
