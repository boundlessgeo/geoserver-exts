package org.geotools.ysld.validate;

import org.geotools.ysld.parse.ZoomContextFinder;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

public class YsldValidator {
    
    List<ZoomContextFinder> zCtxtFinders = Collections.emptyList();
    
    public List<MarkedYAMLException> validate(Reader input) throws IOException {
        YsldValidateContext context = new YsldValidateContext();
        context.zCtxtFinders = this.zCtxtFinders;
        context.push(new RootValidator());

        try {
            Yaml yaml = new Yaml();

            for (Event evt : yaml.parse(input)) {
                YsldValidateHandler h = context.peek();

                if (evt instanceof MappingStartEvent) {
                    h.mapping((MappingStartEvent) evt, context);
                }
                else if (evt instanceof MappingEndEvent) {
                    h.endMapping((MappingEndEvent) evt, context);
                }
                else if (evt instanceof SequenceStartEvent) {
                    h.sequence((SequenceStartEvent) evt, context);
                }
                else if (evt instanceof SequenceEndEvent) {
                    h.endSequence((SequenceEndEvent) evt, context);
                }
                else if (evt instanceof ScalarEvent) {
                    h.scalar((ScalarEvent) evt, context);
                }
                else if (evt instanceof AliasEvent) {

                }
            };
        }
        catch(Exception e) {
            if (e instanceof MarkedYAMLException) {
                context.error((MarkedYAMLException)e);
            }
            else {
                throw new RuntimeException(e);
            }
        }

        return context.errors();
    }

    public void setZCtxtFinders(List<ZoomContextFinder> zCtxtFinders) {
        if(zCtxtFinders==null) throw new NullPointerException("zCtxtFinders can not be null");
        this.zCtxtFinders = zCtxtFinders;
    }
}
