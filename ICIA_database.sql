-- eezy academyProject
create database if not exists eezy_db;
create user if not exists eezy_user@localhost identified by '1234';
grant all privileges on eezy_db.* to eezy_user@localhost;
ALTER USER 'eezy_user'@'localhost' IDENTIFIED WITH mysql_native_password BY '1234';
FLUSH PRIVILEGES;

-- sts4 spring boot (class)
-- localhost 대신 '%' 를 넣으면 다른 컴퓨터로도 db 접속 가능함.
create database if not exists board_db;
create user if not exists b_user@'%' identified by '1234';
grant all privileges on board_db.* to b_user@'%';
ALTER USER 'b_user'@'%' IDENTIFIED WITH mysql_native_password BY '1234';
FLUSH PRIVILEGES;

-- VUS 특강 kakao API 연동
CREATE DATABASE ICIA_VRP CHARACTER SET = UTF8MB3 COLLATE = UTF8MB3_bin;
USE mysql;
CREATE USER 'vrp'@'localhost' IDENTIFIED BY 'vrp123!';
grant all privileges on ICIA_VRP.* to vrp@'localhost' with grant option;
USE ICIA_VRP;
flush privileges;
