package org.opengeo.console.monitor.transport;

import java.util.Collection;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.monitor.RequestData;
import org.opengeo.console.monitor.ConsoleData;
import org.opengeo.console.monitor.ConsoleRequestData;
import org.opengeo.console.monitor.SystemStatSnapshot;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class MessageSerializer {

    public JSONObject serialize(String apiKey, Collection<ConsoleRequestData> data) {
        JSONObject json = new JSONObject();
        json.element("api", apiKey);
        json.element("messages", serializeMessages(data));
        return json;
    }

    private JSONArray serializeMessages(Collection<ConsoleRequestData> data) {
        JSONArray jsonArray = new JSONArray();
        for (ConsoleRequestData consoleRequestData : data) {
            jsonArray.add(serializeConsoleRequestData(consoleRequestData));
        }
        return jsonArray;
    }

    // consolidate request path and query string
    private String buildURL(RequestData requestData) {
        String path = requestData.getPath();
        String queryString = requestData.getQueryString();
        String url = path + (queryString == null ? "" : "?" + queryString);
        return url;
    }

    public JSONObject serializeConsoleRequestData(ConsoleRequestData consoleRequestData) {
        JSONObject json = new JSONObject();
        RequestData requestData = consoleRequestData.getRequestData();
        Optional<ConsoleData> optionalConsoleData = consoleRequestData.getConsoleData();
        SystemStatSnapshot systemStatSnapshot = consoleRequestData.getSystemStatSnapshot();

        json.element("id", requestData.internalid);

        json.element("url", buildURL(requestData));
        json.elementOpt("http_referer", requestData.getHttpReferer());

        json.element("request_method", requestData.getHttpMethod());
        json.element("request_length", requestData.getBodyContentLength());
        json.elementOpt("request_content_type", requestData.getBodyContentType());

        json.element("response_status", requestData.getResponseStatus());
        json.element("response_length", requestData.getResponseLength());
        json.element("response_content_type", requestData.getResponseContentType());

        json.element("category", requestData.getCategory().toString());
        json.elementOpt("service", requestData.getService());

        json.elementOpt("operation", requestData.getOperation());
        json.elementOpt("sub_operation", requestData.getSubOperation());
        json.elementOpt("ows_version", requestData.getOwsVersion());

        // start_time is seconds since epoch
        // duration is in milliseconds
        long startMillis = requestData.getStartTime().getTime();
        long endMillis = requestData.getEndTime().getTime();
        json.element("start_time", startMillis / 1000);
        json.element("duration", endMillis - startMillis);

        json.elementOpt("server_host", requestData.getHost());
        json.element("internal_server_host", requestData.getInternalHost());

        json.element("remote_address", requestData.getRemoteAddr());
        json.elementOpt("remote_host", requestData.getRemoteHost());
        json.elementOpt("remote_user_agent", requestData.getRemoteUserAgent());
        json.elementOpt("remote_user", requestData.getRemoteUser());

        // country only gets set if the ip lookup succeeded
        if (requestData.getRemoteCountry() != null) {
            json.element("remote_latitude", requestData.getRemoteLat());
            json.element("remote_longitude", requestData.getRemoteLon());
        }

        // encode the bounding box as a list of:
        // [minx, maxx, miny, maxy]
        BoundingBox bbox = requestData.getBbox();
        if (bbox != null) {
            Double minX = bbox.getMinX();
            Double maxX = bbox.getMaxX();
            Double minY = bbox.getMinY();
            Double maxY = bbox.getMaxY();
            List<Double> bboxList = ImmutableList.of(minX, maxX, minY, maxY);
            json.element("bbox", bboxList);
        }

        String errorMessage = requestData.getErrorMessage();
        if (errorMessage == null) {
            // unfortunately, we can have an error but no error message at this point
            // toString will at least give us something here
            Throwable error = requestData.getError();
            if (error != null) {
                errorMessage = error.toString();
            }
        }
        json.elementOpt("error", errorMessage);

        List<String> resources = requestData.getResources();
        if (resources != null && !resources.isEmpty()) {
            JSONArray jsonResources = new JSONArray();
            jsonResources.addAll(resources);
            json.element("resources", resources);
        }

        // track gwc cache hits
        if (optionalConsoleData.isPresent()) {
            ConsoleData consoleData = optionalConsoleData.get();

            boolean cacheHit = consoleData.isCacheHit();
            json.element("cache_hit", cacheHit);

            Optional<String> cacheMissReason = consoleData.getCacheMissReason();
            if (cacheMissReason.isPresent()) {
                json.element("cache_miss_reason", cacheMissReason.get());
            }
        }

        // system stats
        json.element("mem_usage", systemStatSnapshot.getTotalMemoryUsage());
        json.element("mem_total", systemStatSnapshot.getTotalMemoryMax());

        // we don't have access to system load averages on some systems
        // they return -1 in this scenario
        // we leave them off the message in this case
        double systemLoadAverage = systemStatSnapshot.getSystemLoadAverage();
        if (systemLoadAverage > 0) {
            json.element("load", systemLoadAverage);
        }

        return json;
    }

}
