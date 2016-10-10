(function(e,t){typeof define=="function"&&define.amd?define(["d3","topojson","./Choropleth","./us-counties","../common/Utility"],t):e.map_ChoroplethCounties=t(e.d3,e.topojson,e.map_Choropleth,e.map_usCounties,e.common_Utility)})(this,function(e,t,n,r,i){function f(){n.call(this),this.projection("albersUsaPr"),this._choroTopology=r.topology,this._choroTopologyObjects=r.topology.objects.counties}var s=t.feature(r.topology,r.topology.objects.counties).features,o={};for(var u in s)s[u].id&&(o[s[u].id]=s[u]);var a=e.format("05d");return f.prototype=Object.create(n.prototype),f.prototype.constructor=f,f.prototype._class+=" map_ChoroplethCounties",f.prototype.publish("onClickFormatFIPS",!1,"boolean","format FIPS code as a String on Click"),f.prototype.layerEnter=function(t,r,s){n.prototype.layerEnter.apply(this,arguments),this._choroplethCounties=this._choroplethTransform.insert("g",".mesh"),this._selection=new i.SimpleSelection(this._choroplethCounties),this.choroPaths=e.select(null)},f.prototype.layerUpdate=function(e){n.prototype.layerUpdate.apply(this,arguments),this.choroPaths=this._choroplethCounties.selectAll(".data").data(this.visible()?this.data():[],function(e){return e[0]});var t=this;this.choroPaths.enter().append("path").attr("class","data").call(this._selection.enter.bind(this._selection)).on("click",function(e){if(t._dataMap[e[0]]){var n=t.onClickFormatFIPS()?t._dataMap[e[0]].map(function(e,n){return t.onClickFormatFIPS()&&n===0?a(e):e}):t._dataMap[e[0]];t.click(t.rowToObj(n),"weight",t._selection.selected(this))}}).on("mouseover.tooltip",function(e){t.tooltipShow([r.countyNames[e[0]],t._dataMap[e[0]]?t._dataMap[e[0]][1]:"N/A"],t.columns(),1)}).on("mouseout.tooltip",function(e){t.tooltipShow()}).on("mousemove.tooltip",function(e){t.tooltipShow([r.countyNames[e[0]],t._dataMap[e[0]]?t._dataMap[e[0]][1]:"N/A"],t.columns(),1)}),this.choroPaths.attr("d",function(t){var n=e._d3GeoPath(o[t[0]]);return n||console.log("Unknown US County:  "+t),n}).style("fill",function(e){var n=t._palette(e[1],t._dataMinWeight,t._dataMaxWeight);return n}),this.choroPaths.exit().remove()},f});