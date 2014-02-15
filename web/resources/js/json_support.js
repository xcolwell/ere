

function parseJson(jsonStr) {
	var jsonObj
	eval("jsonObj = " + jsonStr)
	return jsonObj
}


// we append this to repeated requests, to make their URL unique.
// why? because IE blowz chunks, and does stupid caching
var requestJunk = 0

function nextRequestJunk() {
	return requestJunk++
}
