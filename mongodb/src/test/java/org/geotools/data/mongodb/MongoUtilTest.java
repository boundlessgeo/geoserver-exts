/*
 * 
 */

package org.geotools.data.mongodb;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import static org.hamcrest.CoreMatchers.*;
import org.hamcrest.CoreMatchers;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoUtilTest {
    
    public MongoUtilTest() {
    }

    @Test
    public void set() {
        DBObject dbo = new BasicDBObject();
        Object result;
        
        MongoUtil.setDBOValue(dbo, "root.child1", 1d);
        result = MongoUtil.getDBOValue(dbo, "root.child1");
        
        assertThat(result, is(CoreMatchers.instanceOf(Double.class)));
        assertThat((Double)result, is(equalTo(1d)));
        
        MongoUtil.setDBOValue(dbo, "root.child2", "one");
        result = MongoUtil.getDBOValue(dbo, "root.child2");
        
        assertThat(result, is(CoreMatchers.instanceOf(String.class)));
        assertThat((String)result, is(equalTo("one")));
        
        result = MongoUtil.getDBOValue(dbo, "root.doesnotexists");
        assertThat(result, is(nullValue()));
        
        result = MongoUtil.getDBOValue(dbo, "bugusroot.neglectedchild");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void findMappableFieldsSkipsArraysOfObjects() {
        DBObject dbo = new BasicDBObject();
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("listItemKey", "value"));
        dbo.put("unsupportedList", list);

        Map<String, Class<?>> fields = MongoUtil.findMappableFields(dbo);

        assertThat(fields.size(), equalTo(0));
    }

    @Test
    public void getDBValueIgnoresArrayKeyPathComponents() {
        DBObject dbo = new BasicDBObject();
        BasicDBList list = new BasicDBList();
        list.add(new BasicDBObject("listItemKey", "value"));
        dbo.put("unsupportedList", list);

        assertThat(MongoUtil.getDBOValue(dbo, "unsupportedList"), nullValue());
        assertThat(MongoUtil.getDBOValue(dbo, "unsupportedList.objectInList"), nullValue());
        assertThat(MongoUtil.getDBOValue(dbo, Arrays.asList("unsupportedList", "objectInList").iterator()), nullValue());
    }
    
}
