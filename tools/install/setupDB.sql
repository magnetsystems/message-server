# database: openfire
# mmx database user: mmx

drop database if exists openfire;
create database openfire 
	character set utf8;

drop user 'mmx'@'%';
create user 'mmx'@'%' identified by 'mmx';
grant all on openfire.* to 'mmx'@'localhost';
flush privileges;
