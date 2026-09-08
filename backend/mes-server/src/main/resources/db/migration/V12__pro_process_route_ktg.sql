-- yunshu process + route tables

CREATE TABLE pro_process (
  process_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  process_code VARCHAR(64) NOT NULL,
  process_name VARCHAR(255) NOT NULL,
  attention VARCHAR(1000) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (process_id),
  UNIQUE KEY uk_pro_process_code (process_code)
) ENGINE=InnoDB COMMENT='生产工序';

CREATE TABLE pro_process_content (
  content_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  process_id BIGINT UNSIGNED NOT NULL,
  order_num INT NOT NULL DEFAULT 0,
  content_text VARCHAR(500) NULL,
  device VARCHAR(255) NULL,
  material VARCHAR(255) NULL,
  doc_url VARCHAR(255) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (content_id),
  KEY idx_pro_process_content_process (process_id)
) ENGINE=InnoDB COMMENT='工序内容';

CREATE TABLE pro_route (
  route_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_code VARCHAR(64) NOT NULL,
  route_name VARCHAR(255) NOT NULL,
  route_desc VARCHAR(500) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL COMMENT 'legacy process_route_id',
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (route_id),
  UNIQUE KEY uk_pro_route_code (route_code)
) ENGINE=InnoDB COMMENT='工艺路线';

CREATE TABLE pro_route_process (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_id BIGINT UNSIGNED NOT NULL,
  process_id BIGINT UNSIGNED NOT NULL,
  process_code VARCHAR(64) NULL,
  process_name VARCHAR(255) NULL,
  order_num INT NOT NULL DEFAULT 1,
  next_process_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  next_process_code VARCHAR(64) NULL,
  next_process_name VARCHAR(255) NULL,
  link_type VARCHAR(64) NOT NULL DEFAULT 'SS',
  default_pre_time INT NOT NULL DEFAULT 0,
  default_suf_time INT NOT NULL DEFAULT 0,
  color_code CHAR(7) NOT NULL DEFAULT '#00AEF3',
  key_flag VARCHAR(64) NOT NULL DEFAULT 'N',
  is_check CHAR(1) NOT NULL DEFAULT 'N',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_pro_route_process_route (route_id)
) ENGINE=InnoDB COMMENT='工艺组成';

CREATE TABLE pro_route_product (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NOT NULL DEFAULT 'PCS',
  unit_name VARCHAR(64) NULL,
  quantity INT NOT NULL DEFAULT 1,
  production_time DOUBLE(12,2) NOT NULL DEFAULT 1,
  time_unit_type VARCHAR(64) NOT NULL DEFAULT 'MINUTE',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_pro_route_product_route (route_id),
  KEY idx_pro_route_product_item (item_id)
) ENGINE=InnoDB COMMENT='产品制程';

CREATE TABLE pro_route_product_bom (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_id BIGINT UNSIGNED NOT NULL,
  process_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NOT NULL DEFAULT 'PCS',
  unit_name VARCHAR(64) NULL,
  quantity DOUBLE(12,2) NOT NULL DEFAULT 1,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_pro_route_product_bom_route (route_id)
) ENGINE=InnoDB COMMENT='产品制程BOM';

INSERT INTO pro_process (process_id, process_code, process_name, enable_flag, attr1, create_time, update_time)
SELECT step_id, step_code, step_name,
  IF(status = 'ENABLED', 'Y', 'N'), CAST(step_id AS CHAR), created_at, updated_at
FROM process_step;

INSERT INTO pro_route (route_id, route_code, route_name, route_desc, enable_flag, attr1, create_time, update_time)
SELECT route_id, route_code, route_name, CONCAT('版本 ', version_no),
  IF(status IN ('ENABLED', 'DRAFT'), 'Y', 'N'), CAST(route_id AS CHAR), created_at, updated_at
FROM process_route;

INSERT INTO pro_route_process (route_id, process_id, process_code, process_name, order_num, next_process_id, create_time)
SELECT prs.route_id, prs.step_id, ps.step_code, ps.step_name, prs.step_seq, 0, NOW(3)
FROM process_route_step prs
JOIN process_step ps ON prs.step_id = ps.step_id;

INSERT INTO pro_route_product (route_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, create_time)
SELECT pr.route_id, mi.item_id, mi.item_code, mi.item_name, mi.specification,
  mi.unit_of_measure, mi.unit_name, NOW(3)
FROM product_route pr
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(pr.product_id AS CHAR);
