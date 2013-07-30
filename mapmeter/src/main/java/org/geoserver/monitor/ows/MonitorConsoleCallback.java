package org.geoserver.monitor.ows;

import java.util.concurrent.ConcurrentHashMap;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.gwc.GwcStatistician;
import org.geoserver.monitor.gwc.GwcStatistics;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.opengeo.console.monitor.ConsoleData;

import com.google.common.base.Optional;

public class MonitorConsoleCallback implements DispatcherCallback {

    private final ConcurrentHashMap<Long, ConsoleData> consoleRequestDataMapping;

    private final GwcStatistician gwcStatistician;

    private final Monitor monitor;

    public MonitorConsoleCallback(Monitor monitor,
            ConcurrentHashMap<Long, ConsoleData> consoleRequestDataMapping,
            GwcStatistician gwcStatistician) {
        this.monitor = monitor;
        this.consoleRequestDataMapping = consoleRequestDataMapping;
        this.gwcStatistician = gwcStatistician;
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        return operation;
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(Request request, Operation operation, Object result,
            Response response) {
        // grab the current request data from the thread local in the monitor
        RequestData current = monitor.current();
        if (current == null) {
            return response;
        }

        // compute gwc stats
        GwcStatistics gwcStats = gwcStatistician.getGwcStats(Optional.fromNullable(operation),
                result);
        ConsoleData consoleData = new ConsoleData(gwcStats.isCacheHit(), gwcStats.getMissReason());

        // store data into a map keyed off the request data id
        // this map is used later to lookup the console data for the requestdata from the post processor
        // note that this assumes that the id can uniquely identify a request data object
        long id = current.getId();
        consoleRequestDataMapping.put(id, consoleData);

        return response;
    }

    @Override
    public void finished(Request request) {
    }

}
