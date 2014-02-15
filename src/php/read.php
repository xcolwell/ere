<?php

require_once('SETTINGS.php');
require_once('dir_utils.php');

$id 	= $_GET['id'];
$type 	= $_GET['type'];
if (is_null($id) || is_null($type)) {
	return;
}

@readfile(dir_for_type($type) . '/' . $id);

?>