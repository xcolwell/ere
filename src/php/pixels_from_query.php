<?php

# Takes a search query, performs the query on google image search,

# { select and download a result } while (not valid and has more);
#
# if we get a result, store it in the answer.
# otherwise, return the "failed" status




# Note: 'id' is actually a 'wid', but we retain the name for legacy
$id = $_REQUEST['id'];
$key = $_REQUEST['key'];
$query = $_REQUEST['query'];
$n = $_REQUEST['n'];
if (is_null($id) || is_null($key) || is_null($query) || is_null($n)) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'must provide id, key, query, and n arguments'
	));
	return;
}

$id = intval($id);
$n = intval($n);



require_once('ere_mysql.php');
db_open();
# verify the id and key

$result = mysql_query_params(
	'select security_key from words where wid = $1',
	array($id)
);

$row = mysql_fetch_object($result);
$sec_fail = FALSE === $row || $key != $row->security_key;

mysql_free_result($result);


if ($sec_fail) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'bad security key'
	));
	return;
}


# Update the DB with the query:
mysql_query_params(
	'update words set words_text = $1 where wid = $2',
	array($query, $id)
);

db_close();



require_once('google_image_search.php');


$image_url = google_select_from_n($query, $n);

if (is_null($image_url)) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'no results'
	));
	return;
}

require_once('SETTINGS.php');

# read image into image dir, for the given id
$copy_success = copy(
	$image_url, 
	$IMAGE_DIR . '/' . $id
);

if (! $copy_success) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'could not copy'
	));
	return;
}

echo json_encode(array(
	'success' => 1
));
return;

?>