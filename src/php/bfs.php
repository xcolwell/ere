<?php

# BFS on the link graph
# depth and size can be unbounded;
# we make DEPTH number of queries, where DEPTH is the max depth
# 
# obviously this can be dangerously slow ...

$max_depth 			= $_GET['max_depth'];
$max_count 			= $_GET['max_count'];
$explore_children 	= $_GET['explore_children'];
$explore_parents 	= $_GET['explore_parents'];
$expand_items 		= $_GET['expand_items'];

if (is_null($max_depth) || is_null($max_count)) {
	echo json_encode(array(
		'success' => 0,
		'reason' => 'must supply max_depth and max_count parameters'
	));
	return;
}
if (is_null($explore_children)) {
	$explore_children = 1;
}
if (is_null($explore_parents)) {
	$explore_parents = 1;
}
if (is_null($expand_items)) {
	$expand_items = 0;
}
$explore_children = intval($explore_children);
$explore_parents = intval($explore_parents);
$expand_items = intval($expand_items);

$initial_wids = $_GET['wids'];
if (is_null($initial_wids) || strlen($initial_wids) <= 0) {
	$initial_wids = '[]';
}

$initial_wids = json_decode($initial_wids);



$max_depth = intval($max_depth);
$max_count = intval($max_count);


# Expansion conditions
$fringe 		= array();
$trail_count 	= 0;
$trail_list 	= '';
$depth 			= 0;

# Each element is an array of [parent, child]
$all_links  	= array();


##########################
# UTILITY FUNCTIONS
##########################

function conditions_passed() {
	global $max_depth;
	global $max_count;
	global $fringe;
	global $trail_count;
	global $depth;
	
	if (0 < $max_depth) {
		if ($max_depth <= $depth) {
			return false;
		}
	}
	
	if (0 < $max_count) {
		if ($max_count <= $trail_count) {
			return false;
		}
	}
	
	return true;
}

##########################
# END UTILITY FUNCTIONS
##########################


require_once('ere_mysql.php');

db_open();

foreach ($initial_wids as $initial_wid) {
	$fringe[] = intval($initial_wid);
}



# Note: current included size is sizeof(trail)
while (0 < sizeof($fringe) && conditions_passed()) {

	# 1. PULL BACK LINKS
	# 2. STORE LINKS
	# 3. 
	
	$fringe_list = str_list($fringe);
	
	$old_fringe = array();
	foreach ($fringe as $fringe_elem) {
		$old_fringe[$fringe_elem] = TRUE;
	}
	
	unset($fringe);
	$fringe = array();
	
	if ($explore_children || $explore_parents) {
	
	$merged_list = $trail_list;
	if (0 < strlen($merged_list)) {
		$merged_list .= ',';
	}
	$merged_list .= $fringe_list;
	
	if (0 < strlen($merged_list)) {
		$sql = 'select parent_wid, child_wid from active_links where ';
		if ($explore_children) {
			$sql .= '(parent_wid in (' . $fringe_list . ') and child_wid not in (' . $merged_list . '))';
		}
		if ($explore_parents) {
			if ($explore_children) {
				$sql .= ' or ';
			}
			$sql .= '(child_wid in (' . $fringe_list . ') and parent_wid not in (' . $merged_list . '))';
		}
	}
	else {
		$sql = 'select parent_wid, child_wid from active_links where ';
		if ($explore_children) {
			$sql .= '(parent_wid in (' . $fringe_list . '))';
		}
		if ($explore_parents) {
			if ($explore_children) {
				$sql .= ' or ';
			}
			$sql .= '(child_wid in (' . $fringe_list . '))';
		}
	}
	
	$result = mysql_query_params($sql,
			array());
	
	while ($row = mysql_fetch_object($result)) {
		$parent_wid = intval($row->parent_wid);
		$child_wid = intval($row->child_wid);
	
		# Links:
		$all_links[] = array($parent_wid, $child_wid);
		
		if (!$old_fringe[$parent_wid] || !$old_fringe[$child_wid]) {
			$e = $old_fringe[$parent_wid] ? $child_wid : $parent_wid;
			
			# Next fringe:
			$fringe[] = $e;
			
			$old_fringe[$e] = TRUE;
		}
	}
	
	mysql_free_result($result);
	}
	
	$depth += 1;
	
	# Update trail list:
	if (0 < $trail_count) {
		$trail_list .= ',';
	}
	$trail_list .= $fringe_list;
	$trail_count += sizeof($fringe_list);
}

$items = array();

if ($expand_items) {
	mysql_query_params('create temporary table tmp (wid int primary key)',
		array());

#		echo 'insert into tmp (wid) values (' .
#		implode('),(', explode(',', $trail_list)) . ')';
		
		
	mysql_query_params('insert into tmp (wid) values (' .
		implode('),(', explode(',', $trail_list)) . ')',
		array());
		
	$result = mysql_query_params(
		'select t.wid as wid, w.words_text as words_text, p.pid as pid, p.name as name, p.place as place ' .
		'from tmp t, words w, people p where t.wid = w.wid and w.pid = p.pid order by t.wid desc',
		array());
	
	while ($row = mysql_fetch_object($result)) {
		$items[] = array(
			intval($row->wid), 
			$row->words_text,
			intval($row->pid),
			$row->name,
			$row->place
		);
	}
	
	mysql_free_result($result);
	
	mysql_query_params('drop table tmp',
		array());
}

db_close();


echo '{"success": 1, ' .
	'"all_wids":[' . $trail_list . '], ' .
	'"links":' . json_encode($all_links) . 
	($expand_items ? ', "items":' . json_encode($items) : '') .
	'}';
return;
?>