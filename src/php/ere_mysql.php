<?php

require_once('SETTINGS.php');
require_once('mysql_query_params.php');

function db_open() {
	global $SQL_SERVER;
	global $SQL_USER;
	global $SQL_PASS;
	global $SQL_DB;

	mysql_connect(
		$SQL_SERVER,
		$SQL_USER,
		$SQL_PASS
	);
	mysql_select_db($SQL_DB);
}

# note that our style, we don't call this explicity (most cases)
# and we let the PHP cleanup take care of it
function db_close() {
	mysql_close();
}


##########################
# SQL UTILITIES
##########################

function str_list($list) {
	$str = '';
	$size = sizeof($list);
	for ($i = 0; $i < $size; $i += 1) {
		if (0 < $i) {
			$str .= ',';
		}
		$str .= $list[$i];
	}
	return $str;
}

function str_list_int($list) {
	$str = '';
	$size = sizeof($list);
	for ($i = 0; $i < $size; $i += 1) {
		if (0 < $i) {
			$str .= ',';
		}
		$str .= intval($list[$i]);
	}
	return $str;
}

##########################
# END SQL UTILITIES
##########################


?>