<?php

function rand_string($len) {
	$str = '';
	for ($i = 0; $i < $len; $i += 1) {
		$str .= map_char(rand() % 62);
	}
	return $str;
}

function map_char($num) { 
   $int = $num; $int+=48;
   ($int > 57) ? $int += 7 : null;
   ($int > 90) ? $int += 6 : null;
   return chr($int);
}

?>