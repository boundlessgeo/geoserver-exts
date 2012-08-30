package org.geoserver.printng.api;

public interface PrintSpec {

    Integer getWidth();

    Integer getHeight();

    Integer getDotsPerPixel();

    String getBaseURL();
}
