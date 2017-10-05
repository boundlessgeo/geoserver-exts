package org.geoserver.printng.rest;

import freemarker.template.SimpleHash;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.hsqldb.lib.StringInputStream;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class FreemarkerModelConverterTest {
    
    @Test
    public void testJSON() throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("title", "TITLE");
        JSONArray stuff = new JSONArray();
        for (int i = 0; i < 10; i++) {
            stuff.add(i);
        }
        obj.put("stuff", stuff);
        stuff = new JSONArray();
        for (int i = 0; i < 10; i++) {
            JSONObject s = new JSONObject();
            s.put("name", i);
            stuff.add(s);
        }
        obj.put("moreStuff", stuff);
        obj.put("missing", null);
        
        PrintControllerTemplateWriterTest.createTemplate("farby.ftl", new StringReader("${title}"
                + " <#list stuff as x>${x}</#list> "
                + " <#list moreStuff as s>${s.name}</#list>"
                + " ${missing!\"MISSING\"}"));

        FreemarkerModelConverter converter = new FreemarkerModelConverter();
        MockHttpInputMessage req = new MockHttpInputMessage(new ByteArrayInputStream(obj.toString().getBytes()));
        req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        SimpleHash simpleHash = converter.readInternal(null, req);

        String text = new Scanner(PrintController.writeTemplate("farby", simpleHash)).useDelimiter("\\Z").next();
        assertEquals("TITLE 0123456789  0123456789 MISSING", text);
    }
}
