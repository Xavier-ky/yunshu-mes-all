-- yunshu master data: md_item, md_item_type, md_product_bom, md_unit_measure, md_item_batch_config

CREATE TABLE md_item_type (
  item_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_type_code VARCHAR(64) NOT NULL,
  item_type_name VARCHAR(255) NOT NULL,
  parent_type_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  ancestors VARCHAR(255) NOT NULL DEFAULT '0',
  item_or_product VARCHAR(20) NOT NULL DEFAULT 'ITEM',
  order_num INT NOT NULL DEFAULT 1,
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
  PRIMARY KEY (item_type_id),
  UNIQUE KEY uk_md_item_type_code (item_type_code)
) ENGINE=InnoDB COMMENT='物料产品分类';

CREATE TABLE md_unit_measure (
  measure_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  measure_code VARCHAR(64) NOT NULL,
  measure_name VARCHAR(255) NOT NULL,
  primary_flag CHAR(1) NOT NULL DEFAULT 'Y',
  primary_id BIGINT UNSIGNED NULL,
  change_rate DOUBLE(12,4) NULL,
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
  PRIMARY KEY (measure_id),
  UNIQUE KEY uk_md_unit_measure_code (measure_code)
) ENGINE=InnoDB COMMENT='计量单位';

CREATE TABLE md_item (
  item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NOT NULL DEFAULT 'PCS',
  unit_name VARCHAR(64) NULL,
  item_or_product VARCHAR(20) NOT NULL,
  item_type_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  item_type_code VARCHAR(64) NOT NULL DEFAULT '',
  item_type_name VARCHAR(255) NOT NULL DEFAULT '',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  safe_stock_flag CHAR(1) NOT NULL DEFAULT 'N',
  min_stock DOUBLE(12,4) NOT NULL DEFAULT 0,
  max_stock DOUBLE(12,4) NOT NULL DEFAULT 0,
  high_value CHAR(1) NOT NULL DEFAULT 'N',
  batch_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL COMMENT 'legacy source type PRODUCT/MATERIAL',
  attr2 VARCHAR(255) NULL COMMENT 'legacy source id',
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_md_item_code (item_code),
  KEY idx_md_item_type (item_type_id),
  KEY idx_md_item_or_product (item_or_product)
) ENGINE=InnoDB COMMENT='物料产品';

CREATE TABLE md_product_bom (
  bom_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id BIGINT UNSIGNED NOT NULL,
  bom_item_id BIGINT UNSIGNED NOT NULL,
  bom_item_code VARCHAR(64) NOT NULL,
  bom_item_name VARCHAR(255) NOT NULL,
  bom_item_spec VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NOT NULL DEFAULT 'PCS',
  item_or_product VARCHAR(20) NOT NULL DEFAULT 'ITEM',
  quantity DOUBLE(12,4) NOT NULL DEFAULT 0,
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
  PRIMARY KEY (bom_id),
  KEY idx_md_product_bom_item (item_id),
  KEY idx_md_product_bom_bom_item (bom_item_id)
) ENGINE=InnoDB COMMENT='产品BOM';

CREATE TABLE md_item_batch_config (
  config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  item_id BIGINT UNSIGNED NOT NULL,
  produce_date_flag CHAR(1) NULL,
  expire_date_flag CHAR(1) NULL,
  recpt_date_flag CHAR(1) NULL,
  vendor_flag CHAR(1) NULL,
  client_flag CHAR(1) NULL,
  co_code_flag CHAR(1) NULL,
  po_code_flag CHAR(1) NULL,
  workorder_flag CHAR(1) NULL,
  task_flag CHAR(1) NULL,
  workstation_flag CHAR(1) NULL,
  tool_flag CHAR(1) NULL,
  mold_flag CHAR(1) NULL,
  lot_number_flag CHAR(1) NULL,
  quality_status_flag CHAR(1) NULL,
  enable_flag CHAR(1) NULL DEFAULT 'Y',
  remark VARCHAR(500) NOT NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (config_id),
  KEY idx_md_item_batch_config_item (item_id)
) ENGINE=InnoDB COMMENT='物料批次属性配置';

-- seed item types
INSERT INTO md_item_type (item_type_id, item_type_code, item_type_name, parent_type_id, ancestors, item_or_product, order_num)
VALUES
  (1, 'PRODUCT_ROOT', '产品分类', 0, '0', 'PRODUCT', 1),
  (2, 'ITEM_ROOT', '物料分类', 0, '0', 'ITEM', 2),
  (3, 'PRODUCT_FIN', '成品', 1, '0,1', 'PRODUCT', 1),
  (4, 'ITEM_RAW', '原材料', 2, '0,2', 'ITEM', 1);

-- seed units from uom
INSERT INTO md_unit_measure (measure_id, measure_code, measure_name, primary_flag, enable_flag, create_time)
SELECT unit_id, unit_code, unit_name, 'Y', 'Y', NOW(3) FROM uom;

-- migrate products
INSERT INTO md_item (item_id, item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT p.product_id, p.product_code, p.product_name, IFNULL(p.product_model, ''),
  'PCS', '件', 'PRODUCT', 3, 'PRODUCT_FIN', IFNULL(p.product_category, '成品'),
  IF(p.status = 'ENABLED', 'Y', 'N'), 'PRODUCT', CAST(p.product_id AS CHAR), p.created_at, p.updated_at
FROM product p WHERE p.is_deleted = 0;

-- migrate materials (offset ids to avoid collision - use new auto ids, store legacy in attr2)
INSERT INTO md_item (item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT m.material_code, m.material_name, '', IFNULL(u.unit_code, 'PCS'), IFNULL(u.unit_name, '件'),
  'ITEM', 4, 'ITEM_RAW', IFNULL(m.material_type, '原材料'),
  IF(m.status = 'ENABLED', 'Y', 'N'), 'MATERIAL', CAST(m.material_id AS CHAR), m.created_at, m.updated_at
FROM material m LEFT JOIN uom u ON m.unit_id = u.unit_id WHERE m.is_deleted = 0;

-- migrate BOM lines (map product_id -> md_item item_id for product, material_id -> md_item by attr2)
INSERT INTO md_product_bom (item_id, bom_item_id, bom_item_code, bom_item_name, unit_of_measure, item_or_product, quantity, enable_flag, create_time)
SELECT pi.item_id, mi.item_id, mi.item_code, mi.item_name, IFNULL(mi.unit_of_measure, 'PCS'), 'ITEM', bi.qty_per, 'Y', NOW(3)
FROM bom_item bi
JOIN bom b ON bi.bom_id = b.bom_id
JOIN md_item pi ON pi.attr1 = 'PRODUCT' AND pi.attr2 = CAST(b.product_id AS CHAR)
JOIN md_item mi ON mi.attr1 = 'MATERIAL' AND mi.attr2 = CAST(bi.material_id AS CHAR);

ALTER TABLE work_order ADD COLUMN item_id BIGINT UNSIGNED NULL COMMENT 'md_item ref' AFTER product_id;

UPDATE work_order wo
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(wo.product_id AS CHAR)
SET wo.item_id = mi.item_id
WHERE wo.product_id IS NOT NULL;
