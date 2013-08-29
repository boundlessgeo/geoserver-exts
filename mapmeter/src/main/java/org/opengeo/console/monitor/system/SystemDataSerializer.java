package org.opengeo.console.monitor.system;

import net.sf.json.JSONObject;

public class SystemDataSerializer {

    public String serializeSystemData(SystemData systemData) {
        JSONObject json = new JSONObject();

        json.element("apiKey", systemData.getApiKey());
        json.element("osArch", systemData.getOsArch());
        json.element("osName", systemData.getOsName());
        json.element("osVersion", systemData.getOsVersion());
        json.element("javaVendor", systemData.getJavaVendor());
        json.element("javaVersion", systemData.getJavaVersion());
        json.element("type", systemData.getType());

        json.elementOpt("serverVersion", systemData.getServerVersion().orNull());

        return json.toString();
    }

}
