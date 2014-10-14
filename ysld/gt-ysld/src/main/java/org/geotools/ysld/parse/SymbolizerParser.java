package org.geotools.ysld.parse;

import java.util.Iterator;

import org.geotools.styling.Rule;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UomOgcMapping;
import org.geotools.ysld.YamlMap;
import org.geotools.ysld.YamlObject;
import org.geotools.ysld.encode.YsldEncodeHandler;

public class SymbolizerParser<T extends Symbolizer> extends YsldParseHandler {

    protected T sym;

    protected SymbolizerParser(Rule rule, T sym, Factory factory) {
        super(factory);
        rule.symbolizers().add(this.sym = sym);
    }

    @Override
    public void handle(YamlObject<?> obj, YamlParseContext context) {
        YamlMap map = obj.map();
        sym.setName(map.str("name"));
        if (map.has("geometry")) {
            sym.setGeometry(Util.expression(map.str("geometry"), factory));
        }
        if (map.has("uom")) {
            sym.setUnitOfMeasure(UomOgcMapping.get(map.str("uom")).getUnit());
        }
        if(map.has("options")){
            YamlMap options = map.map("options");
            for(String key : options){
                sym.getOptions().put(key, options.str(key));
            }
        }
        else {
            for(String key : map){
                if( key.startsWith(YsldEncodeHandler.OPTION_PREFIX)){
                    String option = key.substring(YsldEncodeHandler.OPTION_PREFIX.length());
                    sym.getOptions().put(option, map.str(key));
                }
            }
        }
    }
    
}
