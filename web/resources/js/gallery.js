/*
 * ENTER & RE-EXIT GALLERY
 */


function cellId(wid) {
	return "cell_" + wid
}

function hoverCellId(wid) {
	return "hover_cell_" + wid
}

function formatTitle(wid) {
	var info = infoMap[wid]
	return info[1] + " (" + info[3] + ", " + info[4] + ")"
}

function findPos(obj) {
	var curleft = curtop = 0;
	if (obj.offsetParent) {
			curleft += obj.offsetLeft;
			curtop += obj.offsetTop;
	}
	return [curleft,curtop];
}

function findPosr(obj) {
	var curleft = curtop = 0;
	if (obj.offsetParent) {
	do {
			curleft += obj.offsetLeft;
			curtop += obj.offsetTop;
	} while (obj = obj.offsetParent);
	}
	return [curleft,curtop];
}

function fromPx(s) {
	var i = s.indexOf('p')
	return parseInt(s.substring(0, i))
}

 
// - map of id -> info
// - map of id -> parent id
// - 

var mod = 0
// wid -> [wid, text, pid, name, place]
var infoMap = {}
// wid -> parent wid
var parentMap = {}



function loadLatest(offset, n, callback) {
//http://resisttheb.org/ere/core/get_latest.php?n=10
	new Ajax.Request('http://resisttheb.org/ere/core/get_latest.php',
  	{
    	method: 'get',
    	parameters: {
			'offset': offset,
    		'n': n
    	},
    	onSuccess: function(transport) {
			var response = transport.responseText
			if (null == response) {
				callback()
				return
			}
			
			var jsonObj = parseJson(response)
			if (0 == jsonObj.success) {
				callback()
				return
			}
			
			// Info:
			for (var i = 0; i < jsonObj.items.length; ++i) {
				var item = jsonObj.items[i]
				infoMap[item[0]] = item
			}
			
			callback()
		},
		onFailure: function(){
			callback()
    	}
    })
}

// populates info and sets up links
function load(wid, n, callback) {
//http://resisttheb.org/ere/core/bfs.php?max_depth=-1&max_count=-1&wids=[682]&expand_items=1
	new Ajax.Request('http://resisttheb.org/ere/core/bfs.php',
  	{
    	method: 'get',
    	parameters: {
    		'max_depth': n,
    		'max_count': -1,
    		'wids': '[' + wid + ']',
    		'expand_items': 1,
    		'explore_parents': 1,
    		'explore_children': 0
    	},
    	onSuccess: function(transport) {
			var response = transport.responseText
			if (null == response) {
				callback()
				return
			}
			
			var jsonObj = parseJson(response)
			if (0 == jsonObj.success) {
				callback()
				return
			}
			// Parent links:
			var cwid = wid
			for (var i = 1; i < jsonObj.all_wids.length; ++i) {
				var pwid = jsonObj.all_wids[i]
				parentMap[cwid] = pwid
				cwid = pwid
			}
			
			// Info:
			for (var i = 0 ; i < jsonObj.items.length; ++i) {
				var item = jsonObj.items[i]
				infoMap[item[0]] = item
			}
			
			callback()
		},
		onFailure: function(){
			callback()
    	}
    })
}


var headOffset = 0

function pull(rows) {
	size = rows * 5
	loadLatest(headOffset, size, sync)
	headOffset += size
}



function initGallery() {
	pull(3)
}

// adds cells for all wids currently in the map,
// in random order
function sync() {
	var wids = []
	for (var wid in infoMap) {
		wids.push(wid)
	}
	
	// Shuffle the wids:
//	for(var j, x, i = wids.length; i; j = parseInt(Math.random() * i), x = wids[--i], wids[i] = wids[j], wids[j] = x);
	wids.sort(function(a, b) {return b - a})
	
	for (var i = 0; i < wids.length; ++i) {
		addCell(wids[i])
	}
}


function addCell(wid) {
	// if the cell already exists, don't re-add ...
	if  ($(cellId(wid))) {
		return false
	}
	
	var cell = document.createElement('div')
	cell.setAttribute('id', cellId(wid))
	cell.setAttribute('class', 'cell')
	cell.setAttribute('className', 'cell')
	
	var img = document.createElement('img')
	img.setAttribute('src', 'http://resisttheb.org/ere/core/read.php?id=' + wid + '&type=snap_thumb')
	img.setAttribute('title', formatTitle(wid))
	img.setAttribute('onclick', 'click(' + wid + ')')
	cell.appendChild(img)
	
	$('cell-container').appendChild(cell)
	
	return true
}

function removeCell(wid) {
}


function hover(wid, aboveWid, aboveHover) {
//	if ($(cellId(wid))) {
//		$(cellId(wid)).style.display = 'none'
//	}

	var hcell = document.createElement('div');
	hcell.setAttribute('id', hoverCellId(wid))
	hcell.setAttribute('class', 'hcell')
	hcell.setAttribute('className', 'hcell')
	
	var img = document.createElement('img')
	img.setAttribute('src', 'http://resisttheb.org/ere/core/read.php?id=' + wid + '&type=snap_thumb')
	img.setAttribute('title', formatTitle(wid))
	img.setAttribute('onclick', 'hclick(' + wid + ')')
	hcell.appendChild(img)
	
	var x, y;
	var w, h;
	if (0 <= aboveWid) {
		// Position to be above the unhovered WID 
		// if aboveHover, uses hover elem, else uses normal elem
		// offsetWidth, offsetHeight
		var aboveElem = aboveHover ? $(hoverCellId(aboveWid)) : $(cellId(aboveWid))
		if (! aboveElem) {
			aboveElem = !aboveHover ? $(hoverCellId(aboveWid)) : $(cellId(aboveWid))
		}
		var abovePos = findPos(aboveElem)
		w = aboveElem.clientWidth
		h = aboveElem.clientHeight
		x = abovePos[0]
		y = abovePos[1] - h
	}
	else {
		// position above the non hover elem with this wid
		var aboveElem = $(cellId(wid))
		var abovePos = findPos(aboveElem)
		w = aboveElem.clientWidth
		h = aboveElem.clientHeight
		x = abovePos[0]
		y = abovePos[1]
	}
	
	hcell.style.left = x + 'px'
	hcell.style.top = y + 'px'
	hcell.style.width = w + 'px'
	hcell.style.height = h + 'px'
	
	$('cell-container').appendChild(hcell)
}

function unhover(wid) {
	var elem = $(hoverCellId(wid))
	if (elem && elem.parentNode) {
		elem.parentNode.removeChild(elem)
	}
//	if ($(cellId(wid))) {
//		$(cellId(wid)).style.display = 'block'
//	}
}



var menuHovered = []

function clearMenuHovered() {
	for (var i = 0; i < menuHovered.length; ++i) {
		unhover(menuHovered[i])
	}
	menuHovered = []
}

function click(wid) {
	++mod
	clearMenuHovered()
	
	hover(wid, -1)
	
	var expand = function() {
	var D = 4
		var elem = $(hoverCellId(wid))
		elem.style.top = (-D + fromPx(elem.style.top)) + 'px'
		elem.style.height = (D + fromPx(elem.style.height)) + 'px'
	}
	
	expand()
	
	var interval = setInterval(expand, 1000 / 24)
	
	var myMod = mod
	
	// TODO: load
	// TODO: hover, track hovered. on hckicl, clear all hovered
	
	var N = 5
	
	
	var post2 = function() {
		clearInterval(interval)
		unhover(wid)
		
		if (myMod != mod) {
			return
		}
		
		// Show parents above:
		
		clearMenuHovered()
		for (var i = 0; i < N && null != parentMap[wid]; ++i) {
			var pwid = parentMap[wid]
			var show = function() {
				if (myMod != mod) {
					return
				}
				hover(pwid, wid, true)
				menuHovered.push(pwid)
			}
			//setTimeout(show, 500 * i)
			show()
			wid = pwid
		}
		
	}
	
	var post = function() {
		setTimeout(post2, 1000)
	}
	
	load(wid, N, post)
}


function hclick(wid) {
	clearMenuHovered()
	if (null == $(cellId(wid))) {
		addCell(wid)
	}
}

