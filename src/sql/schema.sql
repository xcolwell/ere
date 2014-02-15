

--drop table if exists words;
--drop table if exists people;


create table words (
	wid int not null auto_increment,
	security_key varchar(64) not null,
	pid int not null,
	words_text varchar(1024) not null,
	type tinyint not null,
	time bigint not null,
	ip varchar(11) not null,
	active boolean not null default 0,
	primary key (wid)
);

create table links (
	parent_wid int not null,
	child_wid int not null
);

create table people (
	pid int not null auto_increment,
	security_key varchar(64) not null,
	name varchar(256) not null,
	place varchar(256) not null,
	about varchar(1024) not null,
	time bigint not null,
	ip varchar(11) not null,
	primary key (pid)
);

create or replace view active_words as select * from words where active=1;

create or replace view active_links as select L.parent_wid, L.child_wid from links L, active_words w1, active_words w2 where w1.wid = L.child_wid and w2.wid = L.parent_wid;
