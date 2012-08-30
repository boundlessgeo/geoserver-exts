package org.geoserver.printng.spi;

import java.util.Map;

import org.geoserver.printng.api.PrintSpec;

public class MapPrintSpec implements PrintSpec {

    private final Map<String, ? extends Object> map;

    public MapPrintSpec(Map<String, ? extends Object> map) {
        this.map = map;
    }

    @Override
    public Integer getWidth() {
        return (Integer) map.get("width");
    }

    @Override
    public Integer getHeight() {
        return (Integer) map.get("height");
    }

    @Override
    public Integer getDotsPerPixel() {
        return (Integer) map.get("dpp");
    }

    @Override
    public String getBaseURL() {
        return (String) map.get("baseURL");
    }

}
