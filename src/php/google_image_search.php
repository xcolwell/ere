<?php

# google_select_from_n

# methods for google image search
# 
# can search for a term, and select (uniformly)
# one of the top N results
# returns an error code if there are no results

/**
 * Get a web file (HTML, XHTML, XML, image, etc.) from a URL.  Return an
 * array containing the HTTP server response header fields and content.
 */
function get_web_page( $url )
{
    $options = array(
        CURLOPT_RETURNTRANSFER => true,     // return web page
        CURLOPT_HEADER         => false,    // don't return headers
        CURLOPT_FOLLOWLOCATION => true,     // follow redirects
        CURLOPT_ENCODING       => "",       // handle all encodings
        CURLOPT_USERAGENT      => "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.14) Gecko/20080404 Firefox/2.0.0.14", // who am i
        CURLOPT_AUTOREFERER    => true,     // set referer on redirect
        CURLOPT_CONNECTTIMEOUT => 120,      // timeout on connect
        CURLOPT_TIMEOUT        => 120,      // timeout on response
        CURLOPT_MAXREDIRS      => 10,       // stop after 10 redirects
    );

    $ch      = curl_init( $url );
    curl_setopt_array( $ch, $options );
    $content = curl_exec( $ch );
    $err     = curl_errno( $ch );
    $errmsg  = curl_error( $ch );
    $header  = curl_getinfo( $ch );
    curl_close( $ch );

    $header['errno']   = $err;
    $header['errmsg']  = $errmsg;
    $header['content'] = $content;
    return $header;
}


function filemtime_remote($uri)
{
$uri = parse_url($uri);
$uri['port'] = isset($uri['port']) ? $uri['port'] : 80;

// TimeOut
$tout = 0.2;
$handle = @fsockopen($uri['host'], $uri['port'], $errno, $errstr,
$tout);
if(!$handle)
return 0;

fputs($handle,"HEAD $uri[path] HTTP/1.1\r\nHost: $uri[host]\r\n\r\n");
$result = 0;
while(!feof($handle))
{
$line = fgets($handle,1024);
if(!trim($line))
break;

$col = strpos($line,':');
if($col !== false)
{
$header = trim(substr($line,0,$col));
$value = trim(substr($line,$col+1));
if(strtolower($header) == 'last-modified')
{
$result = strtotime($value);
break;
}
}
}
fclose($handle);
return $result;
}

function url_exists($url) {
	return 0 < filemtime_remote($url);
}


function google_select_from_n($query_str, $n) {
	$url = "http://images.google.com/images?hl=en&q=" . urlencode($query_str);
	$header = get_web_page($url);
	
	$content = $header['content'];
	
	# dyn.Img\(\s*(?:"[^"]*"\s*,\s*){3,3}"([^"]*)"
	
	/*
	preg_match_all(
	"/dyn.Img\s*\(\s*(?:\"[^\"]*\"\s*,\s*){3,3}\"([^\"]*)/i",
		$content, $matches, PREG_SET_ORDER);
	*/
	
	
	preg_match_all(
	"\"([^\"]+\\.(?:jpg|png|gif|bmp))\"",
		$content, $matches, PREG_SET_ORDER);
	
	
	
	/*	
	echo sizeof($matches) . "\n\n";
	foreach ($matches as $val) {
	    echo url_exists($val[1]) . "   " . $val[1] . "\n\n";
	}
	*/

	$n = min($n, sizeof($matches));
	$indices = array();
	for ($i = 0; $i < $n; $i += 1) {
		$indices[] = $i;
	}
	
	shuffle($indices);
	
	$i = 0;
	for (; $i < $n && !url_exists($matches[$indices[$i]][1]); $i += 1) {
	}
	
	if ($i == $n) {
		return NULL;
	}
	return $matches[$indices[$i]][1];
}


# echo google_select_from_n("pink floyd", 10);


?>