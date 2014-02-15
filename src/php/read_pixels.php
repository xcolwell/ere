<?php

$id = $_GET['id'];
if (is_null($id)) {
	return;
}

require_once('SETTINGS.php');

@readfile($IMAGE_DIR . '/' . $id);

?>