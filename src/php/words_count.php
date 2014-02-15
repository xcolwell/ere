<?php

require_once('ere_mysql.php');
db_open();

$result = mysql_query_params(
	'select count(wid) as c from active_words',
	array()
);

$row = mysql_fetch_object($result);
$sec_fail = FALSE === $row;

if (! $sec_fail) {
	$count = intval($row->c);
}

mysql_free_result($result);
# db_close();


if ($sec_fail) {
	echo json_encode(array(
		'success' => 0,
		'reason' => ''
	));
	return;
}

echo json_encode(array(
	'success' => 1,
	'count' => $count
));
return;

?>