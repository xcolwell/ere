var theNewHack = true


var pid
var securityKey

var loading = false
var lint = null

var lc = null

var seeds = []

function setSeeds(_seeds) {
	seeds = _seeds
}


function startLoading() {
	stopLoading()
	loading = true
	status('loading')
	lint = setInterval(loadingTick, 500)
}

function stopLoadingInterval() {
	if (lint) {
		clearInterval(lint)
		lint = null
	}
}

function stopLoading() {
	stopLoadingInterval();
	loading = false
	
	if (lc) {
		lc()
	}
	else {
		status('ready')
	}
}

function loadingTick() {
	var tn = document.createTextNode(' |')
	document.getElementById('status').appendChild(tn)
}


function enterERE(callback) {
	if (theNewHack) {
		callback()
	}
	else {
		runWhenLoaded(callback)
	}
}

function runWhenLoaded(_lc) {
	lc = _lc
	if (! loading) {
		lc()
	}
}

function ERE_launch() {
	// Turn off the background for when the applet takes control;
	// we don't want any pieces showing under the applet
	document.body.style.backgroundImage = 'none';

	status('launching')
	setTimeout(showApplet, 500)
}

function focusStatus() {
	$('status').style.color = 'rgb(222, 224, 40)'
	$('status').style.fontWeight = 'bold'
	$('status').style.fontSize = '14px'
}


function enter() {
	var name = $('name').value
	var loc = $('loc').value
	
	enterAs(name, loc)
}

function randomSeed() {
	// Disable ...
	enabled(false)
	
	if (! loading) {
		status('launching')
	}
	focusStatus();

	new Ajax.Request('http://resisttheb.org/ere/core/get_latest.php',
  	{
    	method: 'get',
    	parameters: {
    		'n': 5
    	},
    	onSuccess: function(transport) {
			var response = transport.responseText
			if (null == response) {
				enable(true)
				return
			}
			
			var jsonObj = parseJson(response)
			if (0 == jsonObj.success) {
				enable(true)
				return
			}
			
			var infoMap = {}
			var _seeds = []
			for (var i = 0; i < jsonObj.items.length; ++i) {
				var item = jsonObj.items[i]
				var wid = item[0]
				infoMap[wid] = item
				_seeds.push(wid)
			}
			
			setSeeds(_seeds)
			enterERE(ERE_launch)
		},
		onFailure: function(){
			enable(true)
    	}
    })
}

function enterAnon() {
	enterAs('', '')
}

function enterAs(name, loc) {
	// Disable ...
	enabled(false)
	
	//opacity('login-box', 100, 35, 2500)
	
	if (! loading) {
		status('launching')
	}
	focusStatus();

	new Ajax.Request('http://resisttheb.org/ere/core/create_person.php',
  	{
    	method: 'post',
    	parameters: {
    		'name': name,
    		'loc': loc
    	},
    	onSuccess: function(transport) {
			var response = transport.responseText
			if (null == response) {
				enable(true)
				return
			}
			
			var jsonObj = parseJson(response)
			if (0 == jsonObj.success) {
				enable(true)
				return
			}
			
			pid = jsonObj.pid
			securityKey = jsonObj.security_key
			
			createCookie('c_name', jsonObj.name, 3)
			createCookie('c_loc', jsonObj.loc, 3)
			
			$('name').value = jsonObj.name
			$('loc').value = jsonObj.loc

			setSeeds([])
			enterERE(ERE_launch)
		},
		onFailure: function(){
			enable(true)
    	}
    })
}

function enabled(enabled) {
	$('name').disabled = !enabled
	$('loc').disabled = !enabled
	$('enter').disabled = !enabled
	$('enter_anon').disabled = !enabled
	$('enter_scenery').disabled = !enabled
}



function populateWhoami() {
	var name = readCookie('c_name')
	var loc = readCookie('c_loc')
	
	if (name) {
		$('name').value = name
	}
	if (loc) {
		$('loc').value = loc
	}
}


function showApplet() {
	document.getElementById('main').style.display = 'none';
	document.getElementById('applet').style.left = '0px';
	
	var post = function() {
		
	}
	
	if (theNewHack) {
		var params = {
			'user_pid': pid,
			'security_key' : securityKey,
			'seeds' : !seeds ? '' : seeds.join(','),
			'reset' : 1
		}
		injectApplet(params, function() {runWhenLoaded(post)})
	}
	else {
		var ere = null
		if (document.getElementById('xere')) {
			ere = document.getElementById('xere').getSubApplet()
		}
		else if (document.getElementById('yere')) {
			ere = document.getElementById('yere').getSubApplet()
		}
		
		// LIVE connect ...
		if (pid && securityKey) {
			ere.ERE_setUser(pid, securityKey)
		}
		else {
			ere.ERE_clearUser()
		}
		
		ere.ERE_setSeeds(seeds)
		ere.ERE_reset()
	}
}




function init() {
	$('enter').onclick = enter
	$('enter_anon').onclick = enterAnon
	$('enter_scenery').onclick = randomSeed

	enabled(true)
	populateWhoami()

	
	startLoading()
	initGallery()
	
	if (theNewHack) {
		// 09.XX.08 -- I started doing this because the loading code is just fucked up.
		stopLoadingInterval()
		status('ready')
	}
}


window.onload = init



//
//
//

function writeWarnings() {
//	if (theNewHack)
//		return
		
//if ('firefox' == BrowserDetect.browser.toLowerCase() && 3 == BrowserDetect.version) {
//	document.write('ERE may not work properly with your FF3')
//}

//if ('firefox' != BrowserDetect.browser.toLowerCase()) {
//	document.write('[please use <a href="http://www.mozilla.com/firefox/">firefox</a> ... <a href="http://www.apple.com/safari/">safari</a> has liveconnect bugs; <a href="http://www.microsoft.com/windows/products/winfamily/ie/default.mspx">ie</a> is just wiggy; <a href="http://www.opera.com/">opera</a> has issues; <a href="http://www.google.com/chrome">chrome</a> seems ok ...]')
//}


}

function writeApplet() {
	if (theNewHack)
		return
		
	var ALWAYS_USE_OUR_APPLET_CODE = true

	if (ALWAYS_USE_OUR_APPLET_CODE || 'windows' != BrowserDetect.OS.toLowerCase() ||
	'safari' == BrowserDetect.browser.toLowerCase() ||
	'opera' == BrowserDetect.browser.toLowerCase()
	) {
		injectApplet()
	}
	else {
  deployJava.runApplet(
  	// Attributes:
  	{
  		"id": "xere",
  		"code": "com.sun.opengl.util.JOGLAppletLauncher",
  		"codebase": "http://resisttheb.org/ere/lib/",
     	"archive": "jogl.jar;preload,gluegen-rt.jar;preload,ere_internal.jar;preload,substance-lite.jar",
     	"width": "100%",
     	"height": "100%"
	},
	// Parameters:
	{
		"archive": "jogl.jar;preload,gluegen-rt.jar;preload,ere_internal.jar;preload,substance-lite.jar",
		"codebase": "http://resisttheb.org/ere/lib/",
		"subapplet.classname": "org.resisttheb.ere.ui.Ere",
		"subapplet.displayname": "ENTER & RE-EXIT",
		"progressbar": "true",
		"cache_archive": "jogl.jar,gluegen-rt.jar,ere_internal.jar,substance-lite.jar",
		"cache_archive_ex": "jogl.jar;preload,gluegen-rt.jar;preload,ere_internal.jar;preload,substance-lite.jar"
	}
	, "1.5");
	}
}


function injectApplet(params, callback) {
	if (! params) {
		params = {}
	}
	
	var attribsStr = ''
	var paramsStr = ''
	
	for (var paramName in params) {
		var paramValue = params[paramName]
		
		attribsStr += ' ' + paramName + '=\"' + paramValue + '\"'
		paramsStr += '<param name=\"' + paramName + '\" value=\"' + paramValue + '\"></param>'
	}
	
	
	var replaceMap = {
		'ATTRIBS_STRING' : attribsStr,
		'PARAMS_STRING' : paramsStr
	}
	
	// LOAD AS HTML
		new Ajax.Request('http://resisttheb.org/ere/applet.txt',
		  {
		    method: 'get',
		    onSuccess: function(transport){
		      var response = transport.responseText || ''
			  
			  response = mapReplace(response, replaceMap)
//			  alert(response)
			  
		      $('applet').innerHTML = response
			  
			  if (callback) {
			  	callback()
			  }
		    },
		    onFailure: function(){
		    	alert('Our load hack failed ... mission failed (try Firefox)')
		    }
		  })
}
