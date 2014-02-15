<?php

$n = $_GET['n'];
$offset = $_GET['offset'];
if (is_null($n)) {
	echo json_encode(array(
		'success' => 0,
		'reasons' => 'must supply n and offset parameters'
	));
	return;
}
$n = intval($n);
$offset = intval($offset);

$items = array();

require_once('ere_mysql.php');
db_open();

mysql_query_params('create temporary table tmp (wid int primary key)',
	array());

mysql_query_params('insert into tmp (wid) select wid from active_words order by wid desc limit $1, $2',
	array($offset, $n));

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

db_close();

echo json_encode(array(
	'success' => 1,
	'items' => $items
));
return;
?>