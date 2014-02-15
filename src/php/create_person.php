<?php

# creates a person row
# returns id and secret key and name and country


# 1. read name and location from POST
# 2. if name is missing, use ANON_NAME ("someone_sweet")
# 3. if loc is missing, map ip to country name
# 4. generate new random key
# 5. set shit into db
# 6. return hos



$name = $_REQUEST['name'];
$loc = $_REQUEST['loc'];
$about = '';

if (is_null($name) || strlen($name) <= 0) {
	$name = 'someone_sweet';
}
if (is_null($loc) || strlen($loc) <= 0) {
	# Use the country name:
	require_once('ip_to_country.php');
	$loc = ip_to_country_long($_SERVER['REMOTE_ADDR']);
}





require_once('rand_string.php');

$security_key = rand_string(32);



require_once('ere_mysql.php');
db_open();
# verify the id and key

mysql_query_params(
	'insert into people (security_key, name, place, about, time, ip) '
	. 'values ($1, $2, $3, $4, $5, $6)',
	array($security_key, $name, $loc, $about, time(), $_SERVER['REMOTE_ADDR'])
);

$pid = mysql_insert_id();

# db_close();



echo json_encode(array(
	'success' => 1,
	'security_key' => $security_key,
	'name' => $name,
	'loc' => $loc,
	'about' => $about,
	'pid' => $pid
));
return;

?>