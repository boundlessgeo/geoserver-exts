package org.opengeo.console.monitor.system;

import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.opengeo.console.monitor.config.MessageTransportConfig;

public class SystemDataTransport {

    private final SystemDataSerializer serializer;

    private MessageTransportConfig config;

    public SystemDataTransport(SystemDataSerializer serializer, MessageTransportConfig config) {
        this.serializer = serializer;
        this.config = config;
    }

    public SystemDataTransportResult transport(SystemData systemData) {
        String json = serializer.serializeSystemData(systemData);
        String systemUpdateUrl;
        synchronized (config) {
            systemUpdateUrl = config.getSystemUpdateUrl();
        }
        HttpClient client = new HttpClient();
        PostMethod postMethod = new PostMethod(systemUpdateUrl);
        StringRequestEntity requestEntity;
        try {
            requestEntity = new StringRequestEntity(json, "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        postMethod.setRequestEntity(requestEntity);
        try {
            int statusCode = client.executeMethod(postMethod);
            if (statusCode == 200) {
                return SystemDataTransportResult.success();
            } else {
                return SystemDataTransportResult.errorResponse(statusCode,
                        postMethod.getResponseBodyAsString());
            }
        } catch (Exception e) {
            return SystemDataTransportResult.transferError(e);
        } finally {
            postMethod.releaseConnection();
        }
    }

}
