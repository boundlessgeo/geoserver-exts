package org.geoserver.monitor;

import org.geoserver.platform.ExtensionFilter;

public class GeoServerMonitorFilter implements ExtensionFilter {

    @Override
    public boolean exclude(String beanId, Object bean) {
        // don't show the "Activity" and "Reports" pages in the "Monitor" category
        return "monitorActivityPage".equals(beanId) || "monitorReportPage".equals(beanId);
    }

}
