


// name -> text
var templateCache = {}

function clearTemplateCache() {
	templaceCache = {}
}

function getTemplateText(templateName) {
	return templateCache[templateName]
}

function setTemplateText(templateName, templateText) {
	templateCache[templateName] = templateText
}



function getTemplateUrl(templateName) {
	return absUrl("templates/" + templateName)
}

function processTemplate(templateName, processor) {
	var templateText = getTemplateText(templateName)
	if (null != templateText) {
		processor(getTemplateText(templateName))
	}
	else {
		new Ajax.Request(getTemplateUrl(templateName),
		{
			method: 'get',
			onSuccess: function(transport) {
				var text = transport.responseText || ''
				
				setTemplateText(templateName, text)
				
				processor(text)
			},
			onFailure: function() {
				processor('')
			}
		});
	}
}


// replaces all occurances of '${key}' with the value for 'key'
//   
function mapReplace(text, map) {
	var out = ""
//	alert("map replace: " + text)
	var re = new  RegExp()
	re.compile('\\$\\{([^}]+)\\}', 'ig')
	var i = 0
//	alert("pre loop   " + re + "   " + result)
	for (var result; result = re.exec(text);) {
//		alert("found result")
		var value = map[result[1]]
		if (null != value) {
			// Do the replacement
			// 1. append [i, result.index) to the buffer
			// 2. append value to the buffer
			// 3. set i = result.index + result[0].length
			if (i < result.index) {
				out += text.substring(i, result.index)
			}
			
			out += value
			
			i = result.index + result[0].length
		}
	}
	if (i < text.length) {
		out += text.substring(i, text.length)
	}
	
	return out
}

