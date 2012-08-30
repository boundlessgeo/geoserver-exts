package org.geoserver.printng.restlet;

import org.geoserver.printng.api.PrintSpec;
import org.restlet.data.Form;
import org.restlet.data.Parameter;
import org.restlet.data.Request;

public class RequestPrintSpec implements PrintSpec {

    private Form form;

    public RequestPrintSpec(Request request) {
        form = request.getResourceRef().getQueryAsForm();
    }

    private Integer getIntAttribute(String attr) {
        Parameter parameter = form.getFirst(attr);
        if (parameter == null) {
            return null;
        }
        try {
            return Integer.parseInt(parameter.getValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public Integer getWidth() {
        return getIntAttribute("width");
    }

    @Override
    public Integer getHeight() {
        return getIntAttribute("height");
    }

    @Override
    public Integer getDotsPerPixel() {
        return getIntAttribute("dpp");
    }

    @Override
    public String getBaseURL() {
        Parameter parameter = form.getFirst("baseURL");
        return parameter == null ? null : parameter.getValue();
    }

}
