/*
 * 
 */
package org.geotools.data.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tkunicki@boundlessgeo.com
 */
public class MongoUtil {
    
    public static Object getDBOValue(DBObject dbo, String path) {
        return getDBOValue(dbo, Arrays.asList(path.split("\\.")).iterator());
    }
    
    public static Object getDBOValue(DBObject dbo, Iterator<String> path) {
        return getDBOValueInternal(path, dbo);
    }
    
    private static Object getDBOValueInternal(Iterator<String> path, Object current) {
        if (current instanceof List) {
            // TODO: add support for nested arrays in documents
            return null;
        }

        if (path.hasNext()) {
            if (current instanceof DBObject) {
                String key = path.next();
                Object value = ((DBObject)current).get(key);
                return getDBOValueInternal(path, value);
            }
            return null;
        } else {
            return current;
        }
    }
    
    public static void setDBOValue(DBObject dbo, String path, Object value) {
        setDBOValue(dbo, Arrays.asList(path.split("\\.")).iterator(), value);
    }
    
    public static void setDBOValue(DBObject dbo, Iterator<String> path, Object value) {
        setDBOValueInternal(dbo, path, value);
    }
    
    private static void setDBOValueInternal(DBObject currentDBO, Iterator<String> path, Object value) {
        String key = path.next();
        if (path.hasNext()) {
            Object next = currentDBO.get(key);
            DBObject nextDBO;
            if (next instanceof DBObject) {
                nextDBO = (DBObject)next;
            } else {
                currentDBO.put(key, nextDBO = new BasicDBObject());
            }
            setDBOValueInternal(nextDBO, path, value);
        } else {
            currentDBO.put(key, value);
        }
    }
    
    public static Set<String> findIndexedGeometries(DBCollection dbc) {
        return findIndexedFields(dbc, "2dsphere");
    } 
    
    public static Set<String> findIndexedFields(DBCollection dbc) {
        return findIndexedFields(dbc, null);
    }
    
    public static Set<String> findIndexedFields(DBCollection dbc, String type) {
        Set<String> fields = new LinkedHashSet<String>();
        List<DBObject> indices = dbc.getIndexInfo();
        for (DBObject index : indices) {
            Object key = index.get("key");
            if (key instanceof DBObject) {
                for (Map.Entry entry : ((Map<?,?>)((DBObject)key).toMap()).entrySet()) {
                    if (type == null || type.equals(entry.getValue())) {
                        fields.add(entry.getKey().toString());
                    }
                }
            }
        }
        fields.remove("_id");
        return fields;
    }
    
    public static Map<String, Class<?>> findMappableFields(DBCollection dbc) {
        return findMappableFields(dbc.findOne());
    }
    
    public static Map<String, Class<?>> findMappableFields(DBObject dbo) {
        if (dbo == null) {
            return  Collections.EMPTY_MAP;
        }
        Map<String, Class<?>> map = doFindMappableFields(dbo);
        map.remove("_id");
        return map;
    }
    
    private static Map<String, Class<?>> doFindMappableFields(DBObject dbo) {
        if (dbo == null) {
            return Collections.EMPTY_MAP;
        }
        Map<String, Class<?>> map = new LinkedHashMap<String, Class<?>>();
        for (Map.Entry e : ((Map<?,?>)dbo.toMap()).entrySet()) {
            Object k = e.getKey();
            if (k instanceof String) {
                String field = (String)k;
                Object v = e.getValue();
                if (v instanceof List) {
                    // this is here as documentation/placeholder.  no array/list support yet.
                } else if (v instanceof DBObject) {
                    for (Map.Entry<String, Class<?>> childEntry : doFindMappableFields((DBObject)v).entrySet()) {
                        map.put(field + "." + childEntry.getKey(), childEntry.getValue());
                    }
                } else {
                    Class<?> binding = mapBSONObjectToJavaType(v);
                    if (binding != null) {
                        map.put(field, binding);
                    }
                }
            }
        }
        return map;
    }
    
    public static Class<?> mapBSONObjectToJavaType(Object o) {
        if (o instanceof String || 
            o instanceof Double ||
            o instanceof Long ||
            o instanceof Integer ||
            o instanceof Boolean || 
            o instanceof Date) {
            return o.getClass();
        }
        return null;
    } 
}
