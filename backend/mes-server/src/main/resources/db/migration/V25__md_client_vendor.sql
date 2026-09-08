-- yunshu master data: client & vendor

CREATE TABLE IF NOT EXISTS md_client (
  client_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  client_code VARCHAR(64) NOT NULL,
  client_name VARCHAR(255) NOT NULL,
  client_nick VARCHAR(255) NULL,
  client_en VARCHAR(255) NULL,
  client_type VARCHAR(64) NULL,
  client_des VARCHAR(500) NULL,
  address VARCHAR(500) NULL,
  website VARCHAR(255) NULL,
  email VARCHAR(128) NULL,
  tel VARCHAR(64) NULL,
  contact1 VARCHAR(64) NULL,
  contact1_tel VARCHAR(64) NULL,
  contact1_email VARCHAR(128) NULL,
  contact2 VARCHAR(64) NULL,
  contact2_tel VARCHAR(64) NULL,
  contact2_email VARCHAR(128) NULL,
  credit_code VARCHAR(64) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (client_id),
  UNIQUE KEY uk_md_client_code (client_code)
) ENGINE=InnoDB COMMENT='客户';

CREATE TABLE IF NOT EXISTS md_vendor (
  vendor_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  vendor_code VARCHAR(64) NOT NULL,
  vendor_name VARCHAR(255) NOT NULL,
  vendor_nick VARCHAR(255) NULL,
  vendor_en VARCHAR(255) NULL,
  vendor_type VARCHAR(64) NULL,
  vendor_des VARCHAR(500) NULL,
  address VARCHAR(500) NULL,
  website VARCHAR(255) NULL,
  email VARCHAR(128) NULL,
  tel VARCHAR(64) NULL,
  contact1 VARCHAR(64) NULL,
  contact1_tel VARCHAR(64) NULL,
  contact1_email VARCHAR(128) NULL,
  contact2 VARCHAR(64) NULL,
  contact2_tel VARCHAR(64) NULL,
  contact2_email VARCHAR(128) NULL,
  credit_code VARCHAR(64) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (vendor_id),
  UNIQUE KEY uk_md_vendor_code (vendor_code)
) ENGINE=InnoDB COMMENT='供应商';

INSERT IGNORE INTO md_client (client_code, client_name, client_nick, client_type, tel, enable_flag, create_time)
VALUES
  ('C001', '华东制造有限公司', '华东制造', 'ENTERPRISE', '021-88880001', 'Y', NOW(3)),
  ('C002', '南方电子科技', '南电科技', 'ENTERPRISE', '0755-88880002', 'Y', NOW(3)),
  ('C003', '北方重工集团', '北方重工', 'ENTERPRISE', '010-88880003', 'Y', NOW(3));

INSERT IGNORE INTO md_vendor (vendor_code, vendor_name, vendor_nick, vendor_type, tel, enable_flag, create_time)
VALUES
  ('V001', '精密零部件供应商', '精密零件', 'MATERIAL', '0571-66660001', 'Y', NOW(3)),
  ('V002', '标准件贸易公司', '标准件', 'MATERIAL', '0512-66660002', 'Y', NOW(3)),
  ('V003', '包装材料厂', '包装材料', 'MATERIAL', '020-66660003', 'Y', NOW(3));
