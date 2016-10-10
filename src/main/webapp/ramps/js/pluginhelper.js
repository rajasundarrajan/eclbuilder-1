/**
 * Helper functions to manage HIPIE forms
 */
function savePlugin(pluginId, flow, htmlholderId) {
		
	var formObj = jq("#" + pluginId);
	var url = formObj.attr("action");
	var data = JSON.parse(JSON.stringify(formObj.serializeArray()));
	
	jq.post(url, data, function(response) {
		if ("success" == response.status) {
			zk.Widget.$(htmlholderId).setContent(response.formHtml);
			var payload = {};
			payload.status = "fail";
			if (jq('#errors').children().length == 0) {
				payload.status = "success";
			}
			
			payload.flow = flow;
			zAu.send(new zk.Event(zk.Widget.$(htmlholderId), 'onFormValidation', payload, {
				toServer : true
			}));
			
			//Evaluate javascript
			extractScriptAndExecute(htmlholderId);
			onformload();
		} else {
			alert(response.message);
		}
	});
}

function refactorPluginLabels() {
	jq(".plugintree-label .z-treecell-text").each(function(index, element) {
		var labelWrapper = "<p>" + jq(this).html() + "</p>";
		jq(this).html(labelWrapper);
	});
}

function extractScriptAndExecute(id) {
	domelement = document.getElementById(id).childNodes[0];
	var scripts = [];

	ret = domelement.childNodes;
	for (var i = 0; ret[i]; i++) {
		if (scripts && getNodeName(ret[i], "script") && (!ret[i].type || ret[i].type.toLowerCase() === "text/javascript")) {
			scripts.push(ret[i].parentNode ? ret[i].parentNode.removeChild(ret[i]) : ret[i]);
		}
	}

	for (script in scripts) {
		evalScript(scripts[script]);
	}
	
	jq( ".propertyforminput" ).hover(
			  function() {
			    var currentElementId = jq(this).parent().attr('id');			    
			    var styles = {
			    		visibility:"visible"						 
					    };
			    jq("#"+currentElementId+" #propertyformdescriptiondiv").css(styles);
			  }, function() {
				var currentElementId = jq(this).parent().attr('id');			    
			    var styles = {
						 visibility:"hidden"						 
					    };
			    jq("#"+currentElementId+" #propertyformdescriptiondiv").css(styles);			  
			  }
	);
}

function getNodeName(elem, name) {
	return elem.nodeName && elem.nodeName.toUpperCase() === name.toUpperCase();
}

function evalScript(elem) {
	data = (elem.text || elem.textContent || elem.innerHTML || "");

	var head = document.getElementsByTagName("head")[0] || document.documentElement, script = document.createElement("script");
	script.type = "text/javascript";
	script.appendChild(document.createTextNode(data));
	head.insertBefore(script, head.firstChild);
	head.removeChild(script);

	if (elem.parentNode) {
		elem.parentNode.removeChild(elem);
	}
}

// When a global variable is selected, call this method to populate/save the selected value into NON-UseDataset plugin
function fillVariable(elementName,activeHtmlHolder, button) {
	var payload = {};
	payload.name = elementName;
	
	//Sending position to Position the global variable popup
	position = $(button).offset();
	payload.x = position.left - $(window).scrollLeft();
	payload.y = position.top - $(window).scrollTop() + 25; //Adding 25 - the button height to position below the button
	
	zAu.send(new zk.Event(zk.Widget.$(activeHtmlHolder), 'onClickFillGCID', payload, {
		toServer : true
	}));
	
	
}