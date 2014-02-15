<?php

# 1. read list of ids
# 2. dump files

$all_ids_str = $_GET['ids'];
if (is_null($all_ids_str)) {
	echo pack('N', 0);
	return;
}

require_once('SETTINGS.php');

$all_ids = json_decode($all_ids_str);
echo pack('N', sizeof($all_ids));
foreach ($all_ids as $id) {	
	$file_name = $BLOB_DIR . '/' . $id;
	
	$size = @filesize($file_name);
	if (FALSE === $size) {
		$size = 0;
	}
	echo pack('N', $size);
	
	if (0 < $size) {
	#$n = 
	@readfile($file_name);
	#if (FALSE === $n) {
	#	echo pack('N', 0);
	#}
	}
}

?>