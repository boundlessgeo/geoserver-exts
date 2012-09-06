package org.opengeo.gsr.core.feature;

import java.util.List;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class AttributeConverter implements Converter {

    @Override
    public boolean canConvert(Class clazz) {
        return clazz.equals(AttributeList.class);
    }

    @Override
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext context) {
        AttributeList attributes = (AttributeList) obj;
        List<Attribute> attrs = attributes.getAttributes();
        for (Attribute attr : attrs) {
            writer.startNode(attr.getName());
            // TODO: This encodes all values as Strings, which passes Schema validation but is not desirable
            writer.setValue(attr.getValue().toString());
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // TODO Auto-generated method stub
        return null;
    }

}
