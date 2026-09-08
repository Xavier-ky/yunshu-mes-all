-- yunshu WMS P0 Sprint 4: 条码清单 / 条码配置 / 装箱单 / 装箱明细

CREATE TABLE wm_barcode (
  barcode_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  barcode_formart VARCHAR(64) NOT NULL,
  barcode_type VARCHAR(64) NOT NULL,
  barcode_content VARCHAR(255) NOT NULL,
  bussiness_id BIGINT UNSIGNED NOT NULL,
  bussiness_code VARCHAR(64) NULL,
  bussiness_name VARCHAR(255) NULL,
  barcode_url VARCHAR(255) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NULL DEFAULT 0,
  attr4 INT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (barcode_id),
  KEY idx_wm_barcode_type_biz (barcode_type, bussiness_id)
) ENGINE=InnoDB COMMENT='条码清单';

CREATE TABLE wm_barcode_config (
  config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  barcode_formart VARCHAR(64) NOT NULL,
  barcode_type VARCHAR(64) NOT NULL,
  content_formart VARCHAR(255) NOT NULL,
  content_example VARCHAR(255) NULL,
  auto_gen_flag CHAR(1) NOT NULL DEFAULT 'Y',
  default_template VARCHAR(255) NULL,
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NULL DEFAULT 0,
  attr4 INT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_wm_barcode_config_type (barcode_type, barcode_formart)
) ENGINE=InnoDB COMMENT='条码配置';

CREATE TABLE wm_package (
  package_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  ancestors VARCHAR(255) NOT NULL DEFAULT '0',
  package_code VARCHAR(64) NULL,
  barcode_id BIGINT UNSIGNED NULL,
  barcode_content VARCHAR(255) NULL,
  barcode_url VARCHAR(255) NULL,
  package_date DATETIME NOT NULL,
  so_code VARCHAR(64) NULL,
  invoice_code VARCHAR(255) NULL,
  client_id BIGINT UNSIGNED NULL,
  client_code VARCHAR(64) NULL,
  client_name VARCHAR(255) NULL,
  client_nick VARCHAR(255) NULL,
  package_length DOUBLE NULL,
  package_width DOUBLE NULL,
  package_height DOUBLE NULL,
  size_unit VARCHAR(64) NULL,
  net_weight DOUBLE NULL,
  cross_weight DOUBLE NULL,
  weight_unit VARCHAR(64) NULL,
  inspector VARCHAR(64) NULL,
  inspector_name VARCHAR(64) NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  remark VARCHAR(500) NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NULL DEFAULT 0,
  attr4 INT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (package_id),
  UNIQUE KEY uk_wm_package_code (package_code),
  KEY idx_wm_package_parent (parent_id)
) ENGINE=InnoDB COMMENT='装箱单';

CREATE TABLE wm_package_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  package_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  quantity_package DOUBLE NOT NULL DEFAULT 0,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  batch_code VARCHAR(255) NULL,
  warehouse_id BIGINT UNSIGNED NULL,
  warehouse_code VARCHAR(64) NULL,
  warehouse_name VARCHAR(255) NULL,
  location_id BIGINT UNSIGNED NULL,
  location_code VARCHAR(64) NULL,
  location_name VARCHAR(255) NULL,
  area_id BIGINT UNSIGNED NULL,
  area_code VARCHAR(64) NULL,
  area_name VARCHAR(255) NULL,
  expire_date DATETIME NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NULL DEFAULT 0,
  attr4 INT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_wm_package_line_pkg (package_id)
) ENGINE=InnoDB COMMENT='装箱明细';

-- 条码配置 seed
INSERT INTO wm_barcode_config (config_id, barcode_formart, barcode_type, content_formart, content_example, auto_gen_flag, enable_flag, create_time)
VALUES
  (1, 'QR_CODE', 'ITEM', 'ITEM-{itemCode}', 'ITEM-MAT001', 'Y', 'Y', NOW()),
  (2, 'QR_CODE', 'PACKAGE', 'PACKAGE-{packageCode}', 'PACKAGE-PK001', 'Y', 'Y', NOW());

-- 条码清单 seed
INSERT INTO wm_barcode (barcode_id, barcode_formart, barcode_type, barcode_content, bussiness_id, bussiness_code, bussiness_name, barcode_url, enable_flag, create_time)
VALUES
  (1, 'QR_CODE', 'ITEM', 'ITEM-DEMO-M001', 1, 'M001', '演示物料A', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=ITEM-DEMO-M001', 'Y', NOW()),
  (2, 'QR_CODE', 'STOCK', 'STOCK-DEMO-001', 1, 'STK001', '演示库存记录', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=STOCK-DEMO-001', 'Y', NOW());

-- 装箱单 seed（外箱 + 子箱）
INSERT INTO wm_package (package_id, parent_id, ancestors, package_code, barcode_id, barcode_content, barcode_url, package_date, so_code, client_name, status, enable_flag, create_time)
VALUES
  (1, 0, '0', 'PK-DEMO-001', 3, 'PACKAGE-PK-DEMO-001', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=PACKAGE-PK-DEMO-001', NOW(), 'SO-DEMO-001', '演示客户A', 'PREPARE', 'Y', NOW()),
  (2, 0, '0', 'PK-DEMO-002', 4, 'PACKAGE-PK-DEMO-002', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=PACKAGE-PK-DEMO-002', NOW(), 'SO-DEMO-002', '演示客户B', 'PREPARE', 'Y', NOW());

INSERT INTO wm_barcode (barcode_id, barcode_formart, barcode_type, barcode_content, bussiness_id, bussiness_code, bussiness_name, barcode_url, enable_flag, create_time)
VALUES
  (3, 'QR_CODE', 'PACKAGE', 'PACKAGE-PK-DEMO-001', 1, 'PK-DEMO-001', '演示客户A', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=PACKAGE-PK-DEMO-001', 'Y', NOW()),
  (4, 'QR_CODE', 'PACKAGE', 'PACKAGE-PK-DEMO-002', 2, 'PK-DEMO-002', '演示客户B', 'https://api.qrserver.com/v1/create-qr-code/?size=120x120&data=PACKAGE-PK-DEMO-002', 'Y', NOW());

INSERT INTO wm_package_line (line_id, package_id, item_id, item_code, item_name, specification, unit_of_measure, quantity_package, workorder_code, batch_code, create_time)
VALUES
  (1, 1, 1, 'P001', '演示产品A', '标准型', 'PCS', 10, 'WO-DEMO-001', 'BATCH-001', NOW()),
  (2, 1, 2, 'P002', '演示产品B', '加强型', 'PCS', 5, 'WO-DEMO-002', 'BATCH-002', NOW());
