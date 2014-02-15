<?php

# takes in a list of offsets
# and an exclude set of wids, which will be skipped
#
# returns a list of wids for the given offsets
# 
# 

require_once('ere_mysql.php');

$offs = $_GET['offs'];
if (is_null($offs) || strlen($offs) <= 0) {
	$offs = '[]';
}
$offs = json_decode($offs);

$excludes = $_GET['excludes'];
if (is_null($excludes) || strlen($excludes) <= 0) {
	$excludes = '[]';
}
$excludes = json_decode($excludes);
$exclude_list = str_list_int($excludes);


$wids = array();

db_open();

foreach ($offs as $off) {
	$off = intval($off);
	
	if (0 < strlen($exclude_list)) {
		$result = mysql_query_params(
			'select wid from active_words where ' .
			'wid not in (' . $exclude_list . ') ' .
			'order by wid asc limit $1, 1',
			array($off)
		);
	}
	else {
		$result = mysql_query_params(
			'select wid from active_words ' .
			'order by wid asc limit $1, 1',
			array($off)
		);
	}

	if (FALSE === $result) {
		continue;
	}
	
	if ($row = mysql_fetch_object($result)) {
		$wids[] = intval($row->wid);
	}
	else {
		//$wids[] = -1;
	}
	
	mysql_free_result($result);
}

db_close();

echo json_encode(array(
	'success' => 1,
	'wids' => $wids
));
return;
?>