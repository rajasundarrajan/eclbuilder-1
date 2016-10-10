var graph;
var propEditor = {};
var currWidget = {};
var main = {};
var FormWidget = {};
var formFieldValue;
function resizeGraph() {
	if(graph){
		graph.resize();
	}
}

function createDashboardVisualization(target, params, editable) {
	jq('.printbtn').attr("disabled", true);
	var frame = null;
	$('#'+target).addClass("VisualizationChartWrapper");	
	jq('#'+target)
		.html('<div class="z-apply-loading-indicator" style="width:80px;height:25px;position:fixed;top: 50%;left: 50%;margin-top:0px;margin-left:50px;border:1px solid #ccc;vertical-align:middle;"><span class="z-apply-loading-icon"></span>Loading...</div><div id="chartHolder_'+target+'" class="chartHolder"></div><div id="propHolder_'+target+'" class="propertyEditor"></div>');
	params = jq.parseJSON(params);	
	var visualizeRoxie = false;
	var layout = "Hierarchy";
	var proxyMappings = null; 
    
	if (!window.location.origin)
		window.location.origin = window.location.protocol + "//" + window.location.host;
	
	var ctx = getContextPath();
	var hostUrl = window.location.origin + ctx + params.hpccId;
    
	proxyMappings = jq.parseJSON('{"' +  params.WsWorkunits + '/WUResult.json":"' + hostUrl + '/proxy-WUResult.do","' +
			params.WsWorkunits + '/WUInfo.json":"' + hostUrl + '/proxy-WUInfo.do","' + 
			params.WsEcl + '/submit/query/":"' + hostUrl + '/proxy-WsEcl.do"}');	
	console.log(params.url);
	console.log(params.layout);
	require(["src/marshaller/HTML", "src/other/Persist", "src/other/PropertyEditor", "src/form/Form"], function (HTMLMarshaller, Persist, PropertyEditor, Form) {    		
    		
    		if(params.layout){   
    			var parsedParams=jq.parseJSON(params.layout);
            	Persist.create(parsedParams.LayoutText, function(persistWidget) {
            		currWidget[target] = persistWidget;
            		if(parsedParams.formText != null){	            		
	    					formFieldValue=parsedParams.formText;	    					
            		}            		
        	    	persistWidget.target('chartHolder_'+target)
						.ddlUrl(params.url)
						.proxyMappings(proxyMappings)	
						.render(function (m) { 
							m._parentElement.style("height", "inherit");
							Persist.widgetWalker(m,function(w) {							   	
							   var sessionClick = w.click;
							   w.click = function () {
											   zAu.send(new zk.Event(zk.Widget.$(target), "onSessionAlive", "Session Alive Event Triggered", {toServer:true}));
											   sessionClick.apply(this, arguments);
							   }
							   if (w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
									 w.submit();
							   }
							});
							if (m.marshaller) {
							    var vizArr = m.marshaller.getVisualizationArray();
							    if (vizArr) {
							        vizArr.forEach(function (marshallerViz) {
							            var w = marshallerViz.widget;
							            if (w && w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
							                w.submit();
							            }
							        });
							    }
							}
							jq('.printbtn').attr("disabled", false);
							jq('#'+target+' .z-apply-loading-indicator').remove();	
					});        	    	
            	});            	
        	}else{        		
	    		main[target] = new HTMLMarshaller();	    		
	            main[target].ddlUrl(params.url)
					.proxyMappings(proxyMappings)								
					.target('chartHolder_'+target)
					.render(function (m) {
						m._parentElement.style("height", "inherit");
						Persist.widgetWalker(m,function(w) {
							var sessionClick = w.click;
							w.click = function () {
								   zAu.send(new zk.Event(zk.Widget.$(target), "onSessionAlive", "Session Alive Event Triggered", {toServer:true}));
								   sessionClick.apply(this, arguments);
							}
							if (w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
								w.submit();
							}
						});
						if (m.marshaller) {
						    var vizArr = m.marshaller.getVisualizationArray();
						    if (vizArr) {
						        vizArr.forEach(function (marshallerViz) {
						            var w = marshallerViz.widget;
						            if (w && w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
						                w.submit();
						            }
						        });
						    }
						}
						jq('.printbtn').attr("disabled", false);
						jq('#'+target+' .z-apply-loading-indicator').remove();						
				});
	    		currWidget[target] = main[target];
        	}    		
    		propEditor[target] = new PropertyEditor()
				.show_settings(false)
				.showData(false)
				.showFields(true)
				.target("propHolder_"+target)
			;
	});		
	jq("#propHolder_"+target).on('click',function(){
		zAu.send(new zk.Event(zk.Widget.$(target), "onSessionAlive", "Session Alive Event Triggered", {toServer:true}));
	});	
}

function displayProperties(sourceWidget,target) { 
	jq('#'+target+' .z-apply-loading-indicator').remove(); 
	propEditor[target]
        .widget(sourceWidget)
        .render(function(){        	
        	jq("#propHolder_"+target+" div").css("overflow","");
        })
    ;
}

function saveDashboardLayout(chartDivId){
	var wholeObject = {};
	wholeObject.formText = null;
	wholeObject.LayoutText = null;
	require(["src/other/Persist"], function (Persist) {		
		 if (FormWidget[chartDivId]){			 
			 wholeObject.formText = FormWidget[chartDivId].data();
		 }
		 wholeObject.LayoutText = Persist.serializeToObject(currWidget[chartDivId], null, false);	
		 console.log(JSON.stringify(wholeObject, null, "  "));
	     zAu.send(new zk.Event(zk.Widget.$(chartDivId), "onSave", JSON.stringify(wholeObject, null, "  "), {toServer:true}));
	});
}

function showProperties(show,chartDivId) {	
	if(show){	
		currWidget[chartDivId].designMode(true);
		displayProperties(currWidget[chartDivId],chartDivId);		
		jq('#propHolder_'+chartDivId).show();
		if($("body").innerWidth() > 1200){
			var styles = {
					 width:"69%",
					 overflow:"hidden"
				    };
			var propStyles = {
					width:"31%"
			};
			
		}else{
		 var styles = {
				 width:"64%",
				 overflow:"hidden"
			    };
		 var propStyles = {
					width:"36%"
			}
		} 
		jq('#chartHolder_'+chartDivId).css(styles);
		jq('.propertyEditor').css(propStyles);		
		currWidget[chartDivId].resize().render();			
	}else{				
		jq('#propHolder_'+chartDivId).hide();
		var styles = {
				 width:"100%",
				 overflow:"hidden"
			    };
		jq('#chartHolder_'+chartDivId).css(styles);
		currWidget[chartDivId].designMode(false).resize().render();		
	}	
		
}	


function createDatabombVisualization(target, ddlstring, editable, databombstring, layoutstring) {	
	var frame = null;
	$('#'+target).addClass("VisualizationChartWrapper");	
	jq('#'+target)
		.html('<div class="z-apply-loading-indicator" style="width:80px;height:25px;position:fixed;top: 50%;left: 50%;margin-top:0px;margin-left:50px;border:1px solid #ccc;vertical-align:middle;"><span class="z-apply-loading-icon"></span>Loading...</div><div id="chartHolder_'+target+'" class="chartHolder"></div><div id="propHolder_'+target+'" class="propertyEditor"></div>');	
	var visualizeRoxie = false;    
	params = jq.parseJSON(layoutstring);
	require(["src/marshaller/HTML", "src/other/Persist", "src/other/PropertyEditor", "src/form/Form"], function (HTMLMarshaller, Persist, PropertyEditor, Form) {
		if(params){    			    			
			Persist.create(params.LayoutText, function(persistWidget) {
				persistWidget.target('chartHolder_'+target)
					.ddlUrl(ddlstring)
					.databomb(databombstring)        	    			
					.render(function (m) {
						Persist.widgetWalker(m,function(w) {
							if (w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
								w.submit();
							}
						});
						if (m.marshaller) {
						    var vizArr = m.marshaller.getVisualizationArray();
						    if (vizArr) {
						        vizArr.forEach(function (marshallerViz) {
						            var w = marshallerViz.widget;
						            if (w && w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
						                w.submit();
						            }
						        });
						    }
						}
						jq('#'+target+' .z-apply-loading-indicator').remove();	
						currWidget[target] = persistWidget;								
				});        	    	
			 });            	
		}else{    
			main[target] = new HTMLMarshaller();	    		
			main[target].ddlUrl(ddlstring)
						.databomb(databombstring)																
							.target('chartHolder_'+target)
							.render(function (m) {
								Persist.widgetWalker(m,function(w) {
									if (w._class.indexOf("form_Form") !== -1 && Object.keys(w.values()).length) { // form and has default value
										w.submit();
									}
								});
								jq('#'+target+' .z-apply-loading-indicator').remove();									
							});
			currWidget[target] = main[target];
		}
		propEditor[target] = new PropertyEditor()
			.show_settings(false)
			.showData(false)
			.showFields(true)
			.target("propHolder_"+target)
		;
	});    		
}

function showDummyChart(chartType, uuid) {
	require(["src/amchart/Gauge", "src/chart/Bubble", "src/amchart/Scatter", "src/amchart/Pyramid","src/amchart/Bar", "src/amchart/Line", "src/amchart/Pie","src/map/ChoroplethStates","src/other/Table"],
	function ( Gauge, Bubble, Scatter, Pyramid, AmBar, AmLine, AmPie, ChoroplethStates, Table) {
		var bubblecolumns =  ["Subject"];
		var bubbledata = [
						  ["Geography", 75],
						  ["English", 45],
						  ["Math", 98],
						  ["Science", 66]
						  ];
		var columns = ["Subject", "Year 1", "Year 2", "Year 3"];
		var data = [
					["Geography", 75, 68, 65],
					["English", 45, 55, 52],
					["Math", 98, 92, 90],
					["Science", 66, 60, 72]
					];
		var tabledata = [
						 ["Label1", 37.665074, -122.384375, "green-dot.png", 37.665074],					                
						 ["Label9", 37.665074, -122.384375, "green-dot.png", 37.665074],
						 ["Label4", 37.665074, -122.384375, "green-dot.png", 37.665074],
						 ["Label5", 37.665074, -122.384375, "green-dot.png", 37.665074],
						 ["Label8", 37.665074, -122.384375, "green-dot.png", 37.665074],
						 ["Label1", 45.777062, -108.549835, "red-dot.png", 37.665074],
						 ["Label8", 37.665074, -122.384375, "green-dot.png", 37.665074],
						 ["Label1", 45.777062, -108.549835, "red-dot.png", 37.665074]
						 ];	
		var tablecolumns = ["Label", "Lat", "Long", "Pin","next"];
		if(chartType == 'GUAGE'){			    	  
			var bubble = new Gauge()
			.target(uuid)
			.numBands([3])
			.bandsColor(["#84b761","#fdd400","#cc4748"])
			.bandsEndValue([90,130,220])
			.bandsStartValue([0,90,130])
			.bandsInnerRadius([null, null, "95%"])
			.bottomText(["[[data]] km/h"])
			.high([220])
			.low([0])
			.data([100])
			.axisLineWidth([1])
			.axisAlpha([0.2])
			.tickAlpha([0.2])
			.valueInterval([20])			    	    
			.render();
		} else if(chartType == 'BUBBLE'){			    	  
			var bubble = new Bubble()
			.target(uuid)
			.columns(bubblecolumns)
			.data(bubbledata)			    	    
			.render();
		} else if(chartType == 'SCATTER'){			    	  
		  var stepline = new Scatter()
			.target(uuid)
			.columns(columns)
			.data(data)			    	    
			.render();
		} else if(chartType == 'STEPLINE'){			    	  
		  var stepline = new AmLine()
			.target(uuid)
			.columns(columns)
			.data(data)
			.stepLines(true)
			.render();
		} else if(chartType == 'DONUT'){			    	  
		   var donut = new AmPie()
		   .target(uuid)		    	   		
		   .columns(columns)			    	   		
		   .data(data)
		   .holePercent('60')
		   .render();
		} else if(chartType == 'PYRAMID'){
			var pyramid = new Pyramid()
			.target(uuid)		    	   		
			.columns(columns)
			.data(data)
			.render();		    	   
		} else if(chartType == 'BAR'){			    	  
			var bar = new AmBar()
			.target(uuid)
			.orientation('vertical')
			.columns(columns)
			.data(data)
			.render();
		} else if(chartType == 'COLUMN'){
			var column = new AmBar()
			.target(uuid)
			.orientation('horizontal')
			.columns(columns)
			.data(data)
			.render();
		}  else if(chartType == 'STACKCOLUMN'){	
			var stackColumn = new AmBar()
			.target(uuid)
			.orientation('horizontal')
			.columns(columns)
			.data(data).stacked('true')
			.render();
		} else if(chartType == 'LINE'){
			var line = new AmLine()
			.target(uuid)
			.columns(columns)
			.data(data)
			.render();			   
		} else if(chartType == 'PIE'){					  
			var pie = new AmPie()					   
			.target(uuid)
			.columns(columns)
			.data(data)
			.render();			   
		} else if(chartType == 'TABLE'){					  				            			              
			var table = new Table()
			.target(uuid)
			.columns(tablecolumns)
			.data(tabledata)
			.render();			   
		} else if(chartType == 'US_MAP'){
			var usmap = new ChoroplethStates()
			.target(uuid)
			.columns( ["State", "Weight"])
			.data([["AL", 4779736], ["AK", 710231], ["AZ", 6392017], ["AR", 2915918], ["CA", 37253956], ["CO", 5029196], ["CT", 3574097], ["DC", 601723], ["FL", 18801310], ["GA", 9687653], ["HI", 1360301], ["ID", 1567582], ["IL", 12830632], ["IN", 6483802], ["IA", 3046355], ["ME", 1328361], ["MD", 5773552], ["MA", 6547629], ["MI", 9883640], ["MN", 5303925], ["MS", 2967297], ["MO", 5988927], ["MT", 989415], ["NE", 1826341], ["NV", 2700551], ["NH", 1316470], ["NJ", 8791894], ["NM", 2059179], ["NY", 19378102], ["NC", 9535483], ["ND", 672591], ["OH", 11536504], ["OK", 3751351], ["OR", 3831074], ["PA", 12702379], ["RI", 1052567], ["SC", 4625364], ["SD", 814180], ["TN", 6346105], ["TX", 25145561], ["UT", 2763885], ["VT", 625741], ["VA", 8001024], ["WA", 6724540], ["WV", 1852994], ["WI", 5686986], ["WY", 563626]])
			.render();			   
		}else if(chartType == 'SCORED_SEARCH'){					   			            			              
			var scoredsearch =  new Table()
			.target(uuid)
			.columns(tablecolumns)
			.data(tabledata)
			.render();			   
		}
	   
	   
		var sampledatauuid = "sampledata_"+uuid;
		if ($("#"+uuid).attr('title') == 'US_MAP'){
			$("#"+uuid+" div").append("<a id='"+sampledatauuid+"' href='#' class='sampleDatatext'>SAMPLE</a>");
			$("#"+uuid+" svg").css('width','398px');
			$("#"+uuid+" svg").css('height','218px');
		}else if(chartType == 'TABLE'){
			$("#"+uuid+" .common_Widget").append("<a id='"+sampledatauuid+"' href='#' class='sampleDatatext'>SAMPLE</a>");			    	   
		}else{
			$("#"+uuid+" .amcharts-chart-div").append("<a id='"+sampledatauuid+"' href='#' class='sampleDatatext'>SAMPLE</a>");			    	   
		}      
			       
	});		
}

//Prints the Dashboard widgets
function printSVG(btnid) {	
	var currentTabpanelID=$("#"+btnid).closest('.z-tabpanel').attr('id');
	var container = $('#'+currentTabpanelID+' .chartHolder');
    //Get all the files from head tag 
    var headTags = $("head").html();
    //Get the HTML of chartHolder container Div
    var divElements = $(container).html();    
	var width = parseFloat(container.css("width"));//Get the container width
	var height = parseFloat(container.css("height"));//Get the container height								
	var printWindow = window.open('','printwindow','width=' + width + ',height=' + height);
	if (printWindow == null || typeof(printWindow)=='undefined') {
		alert('Please disable your pop-up blocker and click the "print" again.');
	}else{
		//write the html to the new window, link to css file		
		printWindow.document.write('<!DOCTYPE html><html><head>'+
									headTags+
									'<script type="text/javascript">'+
										'function printpage() {'+ 
											'var printButton=document.getElementById("printpagebutton");'+											
											'printButton.style.visibility="hidden";'+											
											'window.focus();'+							
											'if(navigator.userAgent.indexOf("Firefox") != -1 ){'+
												'window.close();'+
												'window.print();'+											
											'}else{'+												
												'window.print();'+																									
												'window.close();'+												
											'}'+											
										'}'+
									'</script>'+
									'<style>.other_Table th, .labels-wrapper th,.other_Table td, .rows-wrapper td {font-family: "Open Sans",Arial,Helvetica,Sans-Serif;font-size: 13px;line-height: 1.42857;}</style>'+
									'</head>'+
									'<body>'+
										'<div><input id="printpagebutton" type="button" value="Print this page" onclick="javascript:printpage();" style="left:50%;position:relative;margin:10px;border:1px solid #fff;background-color:aqua;"/></div>'+
										'<div class="VisualizationChartWrapper">'+
											$(container).html()+
										'</div>'+
									'</body>'+
									'</html>');
		if(navigator.userAgent.indexOf("Chrome") != -1 || ((navigator.userAgent.indexOf("MSIE") != -1 ) || (!!document.documentMode == true ))){
			printWindow.document.close();
		}	
						
	}    
}

function resizeDashboard(elmId){	
	if(currWidget[elmId]){
		currWidget[elmId].resize().render();
	}
}

function getContextPath() {
	var ctx = window.location.pathname.substring(0, window.location.pathname.indexOf("/",2));
	ctx += "/";	
	return ctx;
}

$(document).ready(function(){
	$("body").on("click","div.z-notification-content", function(){		
		selectElementText(this); // select the element's text we wish to read
		var paratext = getSelectionText(); // read the user selection		
	});
});

function selectElementText(el){
    var range = document.createRange(); // create new range object
    range.selectNodeContents(el); // set range to encompass desired element text
    var selection = window.getSelection(); // get Selection object from currently user selected text
    selection.removeAllRanges(); // unselect any user selected text (if any)
    selection.addRange(range); // add range to Selection object to select it
}

function getSelectionText(){
    var selectedText = ""
    if (window.getSelection){ // all modern browsers and IE9+
        selectedText = window.getSelection().toString();
    }
    return selectedText;
}