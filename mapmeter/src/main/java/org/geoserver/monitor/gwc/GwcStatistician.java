package org.geoserver.monitor.gwc;

import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WebMap;

import com.google.common.base.Optional;

public class GwcStatistician {

    public GwcStatistics getGwcStats(Optional<Operation> optionalOperation,
            Object owsResponseDispatchedResult) {
        if (!optionalOperation.isPresent()) {
            return emptyStats();
        }

        Operation operation = optionalOperation.get();
        GetMapRequest mapRequest = OwsUtils.parameter(operation.getParameters(),
                GetMapRequest.class);
        if (mapRequest == null) {
            return emptyStats();
        }

        boolean isTiled = mapRequest.isTiled();
        boolean isCacheHit = false;
        Optional<String> missReason = Optional.absent();

        if (isTiled && owsResponseDispatchedResult instanceof WebMap) {
            WebMap map = (WebMap) owsResponseDispatchedResult;
            String[][] responseHeaders = map.getResponseHeaders();
            if (responseHeaders != null) {
                for (String[] h : responseHeaders) {
                    if ("geowebcache-cache-result".equalsIgnoreCase(h[0])
                            && "HIT".equalsIgnoreCase(h[1])) {
                        isCacheHit = true;
                        break;
                    }
                    if ("geowebcache-miss-reason".equalsIgnoreCase(h[0])) {
                        String reason = h[1];
                        if (reason != null && !reason.isEmpty()) {
                            missReason = Optional.of(reason);
                        }
                    }
                }
            }
        }

        return new GwcStatistics(isTiled, isCacheHit, missReason);
    }

    private GwcStatistics emptyStats() {
        return new GwcStatistics(false, false, Optional.<String> absent());
    }

}
