package org.geoserver.printng.restlet;

import java.util.List;

import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

public class PrintResource extends Resource {

    private PrintngFacade facade;

    public PrintResource(PrintngFacade facade) {
        super(null, facade.getRequest(), facade.getResponse());
        this.facade = facade;
        List<Variant> allVariants = getVariants();
        allVariants.add(facade.getVariant());
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    @Override
    public void handlePost() {
        getResponse().setEntity(getRepresentation(getPreferredVariant()));
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        return facade.getRepresentation(variant);
    }
}
