drop database if exists magnetmessaging;
create database magnetmessaging 
	character set utf8;

create user 'mmx'@'localhost' identified by 'mmx';
grant all on magnetmessaging.* to 'mmx'@'localhost';
flush privileges;
