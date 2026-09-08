-- yunshu WMS P0 Sprint 2: 物料入库 / 生产领料 / 生产退料 + 库存事务

CREATE TABLE wm_transaction (
  transaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_type VARCHAR(64) NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  warehouse_code VARCHAR(64) NULL,
  warehouse_name VARCHAR(255) NULL,
  location_id BIGINT UNSIGNED NULL,
  location_code VARCHAR(64) NULL,
  location_name VARCHAR(255) NULL,
  area_id BIGINT UNSIGNED NULL,
  area_code VARCHAR(64) NULL,
  area_name VARCHAR(255) NULL,
  package_id BIGINT UNSIGNED NULL,
  package_code VARCHAR(64) NULL,
  source_doc_type VARCHAR(64) NULL,
  source_doc_id BIGINT UNSIGNED NULL,
  source_doc_code VARCHAR(64) NULL,
  source_doc_line_id BIGINT UNSIGNED NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  transaction_flag INT NOT NULL DEFAULT 1,
  transaction_quantity DOUBLE NOT NULL DEFAULT 0,
  transaction_date DATETIME NULL,
  related_transaction_id BIGINT UNSIGNED NULL,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (transaction_id),
  KEY idx_wm_tx_stock (material_stock_id),
  KEY idx_wm_tx_source (source_doc_type, source_doc_id)
) ENGINE=InnoDB COMMENT='库存事务(wm_transaction)';

CREATE TABLE wm_item_recpt (
  recpt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recpt_code VARCHAR(64) NOT NULL,
  recpt_name VARCHAR(255) NOT NULL,
  iqc_id BIGINT UNSIGNED NULL,
  iqc_code VARCHAR(64) NULL,
  notice_id BIGINT UNSIGNED NULL,
  notice_code VARCHAR(64) NULL,
  po_code VARCHAR(64) NULL,
  vendor_id BIGINT UNSIGNED NULL,
  vendor_code VARCHAR(64) NULL,
  vendor_name VARCHAR(255) NULL,
  vendor_nick VARCHAR(255) NULL,
  warehouse_id BIGINT UNSIGNED NULL,
  warehouse_code VARCHAR(64) NULL,
  warehouse_name VARCHAR(255) NULL,
  location_id BIGINT UNSIGNED NULL,
  location_code VARCHAR(64) NULL,
  location_name VARCHAR(255) NULL,
  area_id BIGINT UNSIGNED NULL,
  area_code VARCHAR(64) NULL,
  area_name VARCHAR(255) NULL,
  recpt_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (recpt_id),
  UNIQUE KEY uk_wm_item_recpt_code (recpt_code)
) ENGINE=InnoDB COMMENT='物料入库单';

CREATE TABLE wm_item_recpt_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recpt_id BIGINT UNSIGNED NOT NULL,
  notice_line_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity_recived DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
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
  produce_date DATETIME NULL,
  expire_date DATETIME NULL,
  lot_number VARCHAR(128) NULL,
  iqc_check CHAR(1) NULL,
  iqc_id BIGINT UNSIGNED NULL,
  iqc_code VARCHAR(64) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_item_recpt_line_recpt (recpt_id)
) ENGINE=InnoDB COMMENT='物料入库单行';

CREATE TABLE wm_item_recpt_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  recpt_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
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
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (detail_id),
  KEY idx_item_recpt_detail_recpt (recpt_id),
  KEY idx_item_recpt_detail_line (line_id)
) ENGINE=InnoDB COMMENT='物料入库单明细';

CREATE TABLE wm_issue_header (
  issue_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_code VARCHAR(64) NOT NULL,
  issue_name VARCHAR(255) NOT NULL,
  workstation_id BIGINT UNSIGNED NULL,
  workstation_code VARCHAR(64) NULL,
  workstation_name VARCHAR(255) NULL,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  task_id BIGINT UNSIGNED NULL,
  task_code VARCHAR(64) NULL,
  client_id BIGINT UNSIGNED NULL,
  client_code VARCHAR(64) NULL,
  client_name VARCHAR(255) NULL,
  client_nick VARCHAR(255) NULL,
  required_time DATETIME NULL,
  issue_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (issue_id),
  UNIQUE KEY uk_wm_issue_code (issue_code)
) ENGINE=InnoDB COMMENT='生产领料单头';

CREATE TABLE wm_issue_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity_issued DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_issue_line_issue (issue_id)
) ENGINE=InnoDB COMMENT='生产领料单行';

CREATE TABLE wm_issue_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_id BIGINT UNSIGNED NOT NULL,
  line_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
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
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (detail_id),
  KEY idx_issue_detail_issue (issue_id),
  KEY idx_issue_detail_line (line_id)
) ENGINE=InnoDB COMMENT='生产领料单明细';

CREATE TABLE wm_rt_issue (
  rt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_code VARCHAR(64) NOT NULL,
  rt_name VARCHAR(255) NULL,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  workstation_id BIGINT UNSIGNED NULL,
  workstation_code VARCHAR(64) NULL,
  workstation_name VARCHAR(255) NULL,
  rt_type VARCHAR(64) NULL,
  rt_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (rt_id),
  UNIQUE KEY uk_wm_rt_issue_code (rt_code)
) ENGINE=InnoDB COMMENT='生产退料单头';

CREATE TABLE wm_rt_issue_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity_rt DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  ipqc_id BIGINT UNSIGNED NULL,
  ipqc_code VARCHAR(64) NULL,
  qc_flag CHAR(1) NULL DEFAULT 'N',
  quality_status VARCHAR(64) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_rt_issue_line_rt (rt_id)
) ENGINE=InnoDB COMMENT='生产退料单行';

CREATE TABLE wm_rt_issue_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_id BIGINT UNSIGNED NOT NULL,
  line_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
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
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (detail_id),
  KEY idx_rt_issue_detail_rt (rt_id),
  KEY idx_rt_issue_detail_line (line_id)
) ENGINE=InnoDB COMMENT='生产退料单明细';

-- 演示：物料入库单 PREPARE + APPROVED(可执行)
INSERT INTO wm_item_recpt (recpt_code, recpt_name, vendor_name, po_code, recpt_date, status, create_time)
VALUES ('IR-DEMO-001', '演示物料入库单', '演示供应商', 'PO-DEMO-001', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE recpt_name = VALUES(recpt_name);

INSERT INTO wm_item_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, create_time)
SELECT r.recpt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 100, CONCAT('B-', m.material_code), NOW()
FROM wm_item_recpt r
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE r.recpt_code = 'IR-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_item_recpt_line l WHERE l.recpt_id = r.recpt_id);

INSERT INTO wm_item_recpt (recpt_code, recpt_name, vendor_name, po_code, recpt_date, status, create_time)
VALUES ('IR-DEMO-002', '演示入库-待执行', '演示供应商', 'PO-DEMO-002', NOW(), 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO wm_item_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, create_time)
SELECT r.recpt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 50, CONCAT('B2-', m.material_code), NOW()
FROM wm_item_recpt r
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE r.recpt_code = 'IR-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM wm_item_recpt_line l WHERE l.recpt_id = r.recpt_id);

INSERT INTO wm_item_recpt_detail (line_id, recpt_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, create_time)
SELECT l.line_id, l.recpt_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_recived, l.batch_code,
       w.warehouse_id, w.warehouse_code, w.warehouse_name, sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name, NOW()
FROM wm_item_recpt_line l
JOIN wm_item_recpt r ON r.recpt_id = l.recpt_id AND r.recpt_code = 'IR-DEMO-002'
JOIN warehouse w ON w.warehouse_code = 'WH-RAW'
JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
JOIN storage_bin sb ON sb.zone_id = sz.zone_id
WHERE NOT EXISTS (SELECT 1 FROM wm_item_recpt_detail d WHERE d.recpt_id = l.recpt_id);

-- 演示：生产领料 PREPARE
INSERT INTO wm_issue_header (issue_code, issue_name, workorder_code, issue_date, status, create_time)
VALUES ('IS-DEMO-001', '演示生产领料', 'WO-DEMO', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE issue_name = VALUES(issue_name);

INSERT INTO wm_issue_line (issue_id, item_id, item_code, item_name, unit_name, quantity_issued, create_time)
SELECT h.issue_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 10, NOW()
FROM wm_issue_header h
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE h.issue_code = 'IS-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_issue_line l WHERE l.issue_id = h.issue_id);

-- 演示：生产退料 PREPARE
INSERT INTO wm_rt_issue (rt_code, rt_name, workorder_code, rt_type, rt_date, status, create_time)
VALUES ('RT-DEMO-001', '演示生产退料', 'WO-DEMO', '余料', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name);

INSERT INTO wm_rt_issue_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rt, qc_flag, quality_status, create_time)
SELECT h.rt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 5, 'N', 'OK', NOW()
FROM wm_rt_issue h
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE h.rt_code = 'RT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_issue_line l WHERE l.rt_id = h.rt_id);
