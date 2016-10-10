
/**
 * If the db name is changed other than "ramps" then the same should be updated in
 * "applicationContext.xml" file line 15 for DSP to connect to database.
 * 
 * The table aname and column names should only have lower case letters
 */
CREATE DATABASE IF NOT EXISTS ramps; 
  
USE ramps;

DROP TABLE IF EXISTS composition_details;

CREATE TABLE composition_details (
comp_name VARCHAR(100) NOT NULL,  
hpcc_con_id VARCHAR(100) NOT NULL,
hpcc_thor_cluster VARCHAR(100),
hpcc_roxie_cluster VARCHAR(100),
PRIMARY KEY(comp_name)
);

DROP TABLE IF EXISTS dermatology;

CREATE TABLE dermatology (
	composition_id varchar(50),
	composition_version varchar(20),
	user_id VARCHAR(100),
	gcid INT(11) NOT NULL DEFAULT 0,
	ddl VARCHAR(255),
	layout MEDIUMTEXT,
	ddl_hash VARCHAR(50),
	modified_date DATETIME,
	CONSTRAINT dermatology_constraint UNIQUE (user_id, composition_id, composition_version, gcid, ddl_hash)
);

CREATE TRIGGER hash_ddl_dermatology
  before INSERT ON dermatology 
  FOR EACH ROW
  SET new.ddl_hash= md5(new.ddl);

DROP TABLE IF EXISTS user_logs;

CREATE TABLE user_logs (
user_id VARCHAR(100),
session_id VARCHAR(100),
start_time BIGINT(20),
end_time BIGINT(20),
memory BIGINT(20),
action VARCHAR(100),
detail TEXT
);

DROP TABLE IF EXISTS group_permission;

CREATE TABLE group_permission (
	group_code VARCHAR(200) NOT NULL,
	ramps TINYINT(1) NOT NULL DEFAULT 0,
	dashboard TINYINT(1) NOT NULL DEFAULT 0,
	dashboard_grid TINYINT(1) NOT NULL DEFAULT 0,
	dashboard_list TINYINT(1) NOT NULL DEFAULT 0,
	dashboard_default_view CHAR(4) NOT NULL DEFAULT 0,
	dashboard_mandate_company_id TINYINT(1) NOT NULL DEFAULT 0,
	dashboard_advanced_mode TINYINT(1) NOT NULL DEFAULT 0,
	dashboard_convert_to_comp TINYINT(1) NOT NULL DEFAULT 0,
	ramps_grid TINYINT(1) NOT NULL DEFAULT 0,
	ramps_list TINYINT(1) NOT NULL DEFAULT 0,
	ramps_default_view CHAR(4) NOT NULL DEFAULT 0,
	ramps_mandate_company_id TINYINT(1) NOT NULL DEFAULT 0,
	ramps_view_plugin TINYINT(1) NOT NULL DEFAULT 0,
	import_file TINYINT(1) NOT NULL DEFAULT 0,
	keep_ecl TINYINT(1) NOT NULL DEFAULT 0,
	PRIMARY KEY(group_code)
);

DROP TABLE IF EXISTS dsp_users;

CREATE TABLE dsp_users (
	user_id VARCHAR(100) NOT NULL,
	perspective VARCHAR(50) NOT NULL,
	PRIMARY KEY(user_id)
);

DROP TABLE IF EXISTS cluster_settings;

CREATE TABLE cluster_settings (
	hpcc_con_id VARCHAR(100),
	cluster_type TINYINT(1) NOT NULL DEFAULT 0,
	PRIMARY KEY(hpcc_con_id)
);

DROP TABLE IF EXISTS repository_settings;

CREATE TABLE repository_settings (
	repo_name VARCHAR(100),
	repo_type TINYINT(1) NOT NULL DEFAULT 0,
	PRIMARY KEY(repo_name)
);

DROP TABLE IF EXISTS cluster_settings_permissions;

CREATE TABLE cluster_settings_permissions (
	hpcc_con_id VARCHAR(100),
	role VARCHAR(100),
	PRIMARY KEY(hpcc_con_id,role)
);

DROP TABLE IF EXISTS static_data;

CREATE TABLE static_data (
	user_id VARCHAR(100), 
	file_name VARCHAR(100), 
	file_content LONGTEXT NOT NULL, 
	PRIMARY KEY(user_id,file_name) 
);

DROP TABLE IF EXISTS application_settings;

CREATE TABLE application_settings (
	name VARCHAR(100),
	value VARCHAR(100),
	PRIMARY KEY(name)
);

DROP TABLE IF EXISTS application_values;

CREATE TABLE application_values (
category VARCHAR(50) NOT NULL,  
value VARCHAR(255) NOT NULL,
PRIMARY KEY(category, value)
);

INSERT INTO application_values (category, value)
VALUES ('BLACK_LIST_PLUGIN', 'HIPIE_Plugins.UseDataset.UseDataset');

DROP TABLE IF EXISTS comp_access_log;

CREATE TABLE comp_access_log (
	comp_id VARCHAR(100) NOT NULL,
	user_id VARCHAR(100) NOT NULL,
	access_count INT NOT NULL DEFAULT '0',
	is_favorite TINYINT(1) NOT NULL DEFAULT '0',
	PRIMARY KEY (comp_id, user_id)
)