var fin = function() {
    var i = 0, ii = this.y.length;
    this.tags = r.set();
    for (;i < ii; i++) {
        if (this.values[i] > 0) {
            this.tags.push(r.g.popup(this.x, this.y[i], this.values[i])
            .insertBefore(this).attr([{fill: "#fff"}, {fill: this.symbols[i].attr("fill")}]));
        }
    }
};

var fout = function () {
    this.tags && this.tags.remove();
}

labelBreaks = function(r,opts) {
    var w = opts.width - 2 * opts.gut,
        dx = w / opts.xsteps,
        i,x,y = 270;
    for (i = 0; i < opts.breaks.length; i++) {
        var b = opts.breaks[i];
        x = opts.gut + 10 + (dx * b[0]) + (dx*b[1]) / b[2];
        r.text(x,y,b[3]).attr({"text-anchor":"left", "opacity": "0.75"});
    }
}

maxYLabelLength = function(ydata) {
    var maxy = 0, i;
    for (i = 0; i < ydata.length; i++) {
        maxy = Math.max(Math.max.apply(Math, ydata[i]),maxy);
    }
    return Math.ceil(Math.log(maxy)/(Math.log(10))) + 1;
}

analyticsCharts = {
    line : function(opts) {
        var r = Raphael(opts.container),
        line,
        xo = maxYLabelLength(opts.ydata) * 8,
        gut = 'gut' in opts ? opts.gut : 20;
        opts.gut = gut;
        r.g.text(xo + opts.width/9, gut, opts.ylabel).attr({"text-anchor":"start"});

        if (opts.xlen > 0) {
            line = r.g.linechart(xo, 0, opts.width, opts.height, opts.xdata, opts.ydata, { 
              axis: "0 0 0 1", axisystep: 5, gutter: gut, smooth: true,  symbol: "o", 
              colors: opts.colors
            });
            if (opts.xlen < 25) {
              line.hoverColumn(fin, fout);
              line.symbols.attr({r:3});
            }
            else {
              line.symbols.attr({r:0});
            }
        } else {
            opts.xlen = 0;
            r.g.linechart(xo, 0, opts.width, opts.height, [0], [0], {axis: "0 0 0 1", gutter: gut, smooth: true});
            r.g.txtattr.font = "12px 'Fontin Sans', Fontin-Sans, sans-serif";
            r.g.text(opts.width/2, opts.height/2, opts.ylabel);
        }
        
        r.g.axis(gut+ xo, opts.height-gut, opts.width - 2*gut, 0, opts.xlen, opts.xsteps, 0, opts.labels);
        
        labelBreaks(r,opts);
    },
    performanceLine : function(opts) {
        var r = Raphael(opts.container),
        line,
        xo = maxYLabelLength([opts.timeData,opts.thruData]) * 8,
        gut = 'gut' in opts ? opts.gut : 20;
        opts.gut = gut;
        if (opts.xlen > 0) {
            line = r.g.linechart(xo, 0, opts.width, opts.height, opts.xdata, opts.timeData, { 
              axis: "0 0 0 1", axisystep: 5, gutter: gut, smooth: true,  symbol: "o"
            });
            if (opts.xlen < 25) {
              line.hoverColumn(fin, fout);
              line.symbols.attr({r:3});
            }
            else {
              line.symbols.attr({r:0});
            }

            r.g.axis(opts.gut+xo, opts.height-opts.gut, opts.width - 2*opts.gut, 0, opts.xlen, opts.xsteps, 0, opts.labels);
            r.g.text(xo + opts.width/9, gut, "Average Request Time (ms)").attr({"text-anchor":"start"});

            line = r.g.linechart(xo, opts.height, opts.width, opts.height, opts.xdata, opts.thruData, { 
              axis: "0 0 0 1", axisystep: 5, gutter: gut, smooth: true,  symbol: "o"
            });
            if (opts.xlen < 25) {
              line.hoverColumn(fin, fout);
              line.symbols.attr({r:3});
            }
            else {
              line.symbols.attr({r:0});
            }
            r.g.axis(opts.gut+xo, 2*opts.height-opts.gut, opts.width - 2*opts.gut, 0, opts.xlen, opts.xsteps, 0, opts.labels);
            r.g.text(xo + opts.width/9, opts.height+gut, "Average Throughput (bytes)").attr({"text-anchor":"start"});
        } else {
            opts.xlen = 0;
            r.g.linechart(xo, 0, opts.width, opts.height, [0], [0], {axis: "0 0 1 1", gutter: gut, smooth: true});
            r.g.txtattr.font = "12px 'Fontin Sans', Fontin-Sans, sans-serif";
            r.g.text(opts.width/2, opts.height/2, "No data");
        }
        labelBreaks(r,opts);
    },
    pie : function(opts) {
        var r,
            gut = 10,
            values = [],
            colors = [],
            legend = [],
            hrefs = [],
            prop,val;
        
        function createDiv(parent, suffix) {
          var el = document.getElementById(parent);
          var div = document.createElement("div");
          div.setAttribute("id", parent + suffix)
          div.setAttribute("class", "pie-chart");
          el.appendChild(div);
        }
        
        createDiv(opts.container, "_a");
        r = Raphael(opts.container + "_a");

        //createDiv("${container}", "_b");

        r.g.txtattr.font = "11px Arial, sans-serif";
        //var r = Raphael("${container}");

        for (prop in opts.data) {
          val = opts.data[prop];
          values.push(val.value);
          colors.push(val.color);
          //legend.push(val.label + ": " + val.value + " (%%.%%)");
          legend.push(val.label);
          hrefs.push('#'); 
        }

        pie = r.g.piechart( (opts.width + gut) / 2, (opts.height + gut) / 2, opts.width / 2, values, {
          legend: legend, legendpos: "east", legendmark: "s", 
          colors: colors, href: hrefs
        });
        
        function pieout() {
            this.sector.animate({scale: [1, 1, this.cx, this.cy]}, 500, "bounce");
            this.tags && this.tags.remove();
        }

        pie.hover(function () {
            this.sector.stop();
            this.sector.scale(1.05, 1.05, this.cx, this.cy);
            this.tags = r.set();
            var percent = this.value.value / this.total * 100; 
            this.tags.push(r.g.popup(this.mx, this.my, this.value.value + " (" + percent.toFixed(2) + "%)"));
        }, pieout);

        pie.subpie = null;
        pie.click(function() {
          if (pie.subpie) {
             pie.subpie.remove();
          }
          var value = opts.data[this.value.order];
          if (value.ops.length > 0) {
            //var s = Raphael("${container}_b");
            var values = [];
            var legend = [];
            for (var i in value.ops) {
                var op = value.ops[i];
                values.push(op.value);
                legend.push(op.name);
            }
            var subpie = r.g.piechart((opts.width + gut) / 2, (opts.height + gut) / 2 + opts.height +10, opts.width / 2, values, {
                legend: legend, legendpos: "east", legendmark: "s",
                //colors: ["#B20000", "#00B22C", "#5900B2", "#B2A000", "#00B27C", "#B25000"]
                colors: ["#B20000"]
            });

            subpie.hover(function() {
                this.sector.stop();
                this.sector.scale(1.05, 1.05, this.cx, this.cy);

                this.tags = r.set();

                var percent = this.value.value / this.total * 100; 
                this.tags.push(r.g.popup(this.mx, this.my, this.value.value + " (" + percent.toFixed(2) + "%)"));
            }, pieout);
            pie.subpie = subpie;
          }
        }); 

    }
}
