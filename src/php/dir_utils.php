<?php

require_once('SETTINGS.php');

function dir_for_type($type) {
	global $IMAGE_DIR;
	global $BLOB_DIR;
	global $SNAPSHOT_DIR;
	global $SNAPSHOT_THUMB_DIR;
	global $JUNK_DIR;
	
	if ('image' == $type) {
		return $IMAGE_DIR;
	}
	if ('blob' == $type) {
		return $BLOB_DIR;
	}
	if ('snap' == $type) {
		return $SNAPSHOT_DIR;
	}
	if ('snap_thumb' == $type) {
		return $SNAPSHOT_THUMB_DIR;
	}
	return $JUNK_DIR;
}

?>