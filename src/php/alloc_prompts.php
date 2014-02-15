<?php
# allocates N "words"s
# for each, finds a set of parent words -- parent ids for each is passed in
# number of parents is passed in
# parent ids are selected uniformly
#
# once parents are selected, creates links in the db
#
# "words"s are initialize active=0
#
# returns a json list of [id, key, [[id, text, location]]]
# where there is one [text, location] pair for each parent of id
#
# 
#

#
# some percentage should not have parents ... and be totally free
#


##########################
# SUPPORTING FUNCTIONS
##########################

function map_offset_to_wid($offset) {	
	$result = mysql_query_params(
		'select wid from words where active = 1 order by wid asc limit $1, 1',
		array($offset)
	);
	
	if (is_null($result) || FALSE === $result) {
		return -1;
	}
	
	$row = mysql_fetch_object($result);
	$sec_fail = FALSE === $row;
	
	$wid = -1;
	if (! $sec_fail) {
		$wid = intval($row->wid);
	}
	
	mysql_free_result($result);
	return $wid;
}

##########################
# END SUPPORTING FUNCTIONS
##########################




require_once('ere_mysql.php');
db_open();


$pid = $_REQUEST['pid'];
$p_key = $_REQUEST['p_key'];

if (is_null($pid) || is_null($p_key)) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'must provide pid and p_key arguments'
	));
	return;
}

$pid = intval($pid);


# verify the id and key
$result = mysql_query_params(
	'select security_key from people where pid = $1',
	array($pid)
);

$row = mysql_fetch_object($result);
$sec_fail = FALSE === $row || $p_key != $row->security_key;

mysql_free_result($result);

if ($sec_fail) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'bad security key'
	));
	return;
}


# map from input offset to wid:
# select wid from words where active = 1 order by wid asc limit 1, $1



#1. read parent ids

# Note: 
# the number of sub-lists in this list determines the number
# of allocated prompts
$all_parent_offs_str = $_REQUEST['p_offs'];
if (is_null($all_parent_offs_str)) {
	$all_parent_offs_str = '[]';
}

# should be a list of lists
$all_parent_offs = json_decode($all_parent_offs_str);



$outs = array();


$DEFAULT_WORDS_TEXT = '';
$DEFAULT_WORDS_TYPE = -1;

require_once('rand_string.php');

foreach ($all_parent_offs as $parent_offs) {
	# 1. create the words row

	$security_key = rand_string(32);
	
	mysql_query_params(
		'insert into words (security_key, pid, words_text, type, time, ip, active) '
		. 'values ($1, $2, $3, $4, $5, $6, $7)',
		array($security_key, $pid, $DEFAULT_WORDS_TEXT, $DEFAULT_WORDS_TYPE,
		time(), $_SERVER['REMOTE_ADDR'], 0)
	);

	$wid = mysql_insert_id();
	
	

	# map each parent offset to a wid
	$parent_wids = array();
	foreach ($parent_offs as $parent_off) {
		$parent_wid = map_offset_to_wid($parent_off);
		if (0 <= $parent_wid) {
			$parent_wids[] = $parent_wid;
		} 
	}
	
	
	$parent_info = array();
	
	foreach ($parent_wids as $parent_wid) {
		$result = mysql_query_params(
			'select w.pid, w.words_text as words_text, p.name as name, p.place as place from words w, people p '
			. 'where w.wid = $1 and w.pid = p.pid',
			array($parent_wid)
		);
		
		if (is_null($result) || FALSE === $result) {
			continue;
		}
		
		$row = mysql_fetch_object($result);
		if (FALSE === $row) {
			continue;
		}
		
		$parent_info[] = array($parent_wid, $row->name, $row->place, $row->words_text);
		
		mysql_free_result($result);
	}
	
	$outs[] = array($wid, $security_key, $parent_info);
	
	
	# Create the links:
	foreach ($parent_wids as $parent_wid) {
		mysql_query_params(
			'insert into links (parent_wid, child_wid) values ($1, $2)',
			array($parent_wid, $wid)
		);
	}
}

# db_close();


echo json_encode(array(
	'success' => 1,
	'info' => $outs
));
return;

?>