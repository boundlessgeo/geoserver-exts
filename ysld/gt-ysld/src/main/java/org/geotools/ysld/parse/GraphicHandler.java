package org.geotools.ysld.parse;

import org.geotools.styling.*;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;

public class GraphicHandler extends YsldParseHandler {

    Graphic g;

    GraphicHandler(Factory factory) {
        this(factory, factory.styleBuilder.createGraphic(null, null, null));
    }

    GraphicHandler(Factory factory, Graphic g) {
        super(factory);
        this.g = g;
    }

    @Override
    public void scalar(ScalarEvent evt, YamlParseContext context) {
        String val = evt.getValue();

        if ("anchor".equals(val)) {
            context.push(new AnchorHandler(factory) {
                protected void anchor(AnchorPoint anchor) {
                    g.setAnchorPoint(anchor);
                }
            });
        }
        if ("opacity".equals(val)) {
            context.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setOpacity(expr);
                }
            });
        }
        if ("size".equals(val)) {
            context.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setSize(expr);
                }
            });
        }
        if ("displacement".equals(val)) {
            context.push(new DisplacementHandler(factory) {
                @Override
                protected void displace(Displacement displacement) {
                    g.setDisplacement(displacement);
                }
            });
        }
        if ("rotation".equals(val)) {
            context.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setRotation(expr);
                }
            });
        }
        if ("gap".equals(val)) {
            context.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setGap(expr);
                }
            });
        }
        if ("initial-gap".equals(val)) {
            context.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setInitialGap(expr);
                }
            });
        }
        if ("symbols".equals(val)) {
            context.push(new SymbolsHandler());
        }
    }

    @Override
    public void endMapping(MappingEndEvent evt, YamlParseContext context) {
        super.endMapping(evt, context);
        graphic(g);
        context.pop();
    }

    protected void graphic(Graphic graphic) {
    }

    class SymbolsHandler extends YsldParseHandler {

        protected SymbolsHandler() {
            super(GraphicHandler.this.factory);
        }

        @Override
        public void scalar(ScalarEvent evt, YamlParseContext context) {
            String val = evt.getValue();
            if ("mark".equals(val)) {
                context.push(new MarkHandler(factory) {
                    @Override
                    protected void mark(Mark mark) {
                        g.graphicalSymbols().add(mark);
                    }
                });
            }
            else if ("external".equals(val)) {
                context.push(new ExternalGraphicHandler(factory) {
                    @Override
                    protected void externalGraphic(ExternalGraphic externalGraphic) {
                        g.graphicalSymbols().add(externalGraphic);
                    }
                });
            }
        }

        @Override
        public void endSequence(SequenceEndEvent evt, YamlParseContext context) {
            context.pop();
        }
    }
}