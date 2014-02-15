<?php

# 1. read list of ids
# 2. dump files

$wid_key_list = $_REQUEST['list'];
if (is_null($wid_key_list)) {
	echo json_encode(array(
		"success" => 0,
		"reason" => "must supply list parameter"
	));
	return;
}

$wid_key_list = json_decode($wid_key_list);

require_once('SETTINGS.php');
require_once('ere_mysql.php');
db_open();

foreach ($wid_key_list as $pair) {
	$wid = intval($pair[0]);
	$key = $pair[1];
	
	# TODO: track when this fails and report it
	mysql_query_params("update words set active = 1 where wid = $1 and security_key = $2",
		array($wid, $key));
}

db_close();

echo json_encode(array(
	"success" => 1
));
return;
?>