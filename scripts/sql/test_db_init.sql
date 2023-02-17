CREATE DATABASE webgam_db_test;
CREATE USER 'webgam_admin'@'localhost' IDENTIFIED BY 'WEBGAM_admin_01';
GRANT ALL PRIVILEGES ON webgam_db_test.* TO 'webgam_admin'@'localhost';