<?php

# uses file upload
# file names should be in the format     "id:security_key:[full or thumb]"
#
# 

require_once('SETTINGS.php');
require_once('ere_mysql.php');
require_once('dir_utils.php');

db_open();

$status = array();

$keys = array_keys($_FILES);
foreach ($keys as $key) {
	$parts = explode(':', $key);
	
	$wid = intval($parts[0]);
	$security_key = $parts[1];
	$type = $parts[2];
	
	# 1. verify security
	# 2. if secure, copy the uploaded file to BLOBS/wid
	
	
	# verify the id and key
	$result = mysql_query_params(
		'select security_key from words where wid = $1',
		array($wid)
	);
	
	$row = mysql_fetch_object($result);
	$sec_fail = FALSE === $row || $security_key != $row->security_key;
	
	mysql_free_result($result);
	
	if ($sec_fail) {
		$status[] = array($wid, 0);
		continue;
	}
	
	copy($_FILES[$key]['tmp_name'],
		dir_for_type($type) . '/' . $wid);
	
	$status[] = array($wid, 1);
}

echo json_encode(array(
	'status' => $status
));

?>