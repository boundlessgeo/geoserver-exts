package org.geoserver.printng.restlet;

import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class FreemarkerTemplateFinder extends Finder {

    @Override
    public Resource findTarget(Request request, Response response) {
        return new FreemarkerTemplateResource(request, response);
    }

}
