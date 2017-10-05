package org.geoserver.printng.rest;

import freemarker.template.SimpleHash;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Converts a JSON object or HTTP form into a SimpleHash to be used as a model by a Freemarker template
 */
@Component
public class FreemarkerModelConverter extends BaseMessageConverter<SimpleHash> {

    FormHttpMessageConverter formConverter;

    @Autowired
    public FreemarkerModelConverter() {
        super(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_FORM_URLENCODED);
        formConverter = new FormHttpMessageConverter();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return SimpleHash.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }

    @Override
    protected SimpleHash readInternal(Class<? extends SimpleHash> clazz,  HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

        SimpleHash simpleHash = new SimpleHash();

        if (MediaType.APPLICATION_JSON.equals(inputMessage.getHeaders().getContentType())) {
            JSONObject json = JSONObject.fromObject(IOUtils.toString(inputMessage.getBody()));
            addFromJSON(simpleHash, json);
        } else {
            MultiValueMap<String, String> map = formConverter.read(null, inputMessage);
            if (map != null) {
                addFromForm(simpleHash, map);
            }
        }

        return simpleHash;
    }

    private void addFromForm(SimpleHash hash, MultiValueMap<String, String> map) {
        for (String name : map.keySet()) {
            hash.put(name, map.getFirst(name));
        }
    }

    private void addFromJSON(SimpleHash parent, JSONObject json) {
        @SuppressWarnings("unchecked")
        Set<Map.Entry<Object, Object>> entries = json.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();
            Object obj = entry.getValue();
            if (obj instanceof JSONObject) {
                SimpleHash child = new SimpleHash();
                parent.put(obj.toString(), child);
                addFromJSON(child, (JSONObject) obj);
            } else {
                parent.put(key.toString(), obj);
            }
        }
    }

}
