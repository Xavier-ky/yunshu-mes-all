-- yunshu WMS P0 Sprint 3: 产品入库 / 采购退货 / 销售出库 / 销售退货

CREATE TABLE wm_product_recpt (
  recpt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recpt_code VARCHAR(64) NOT NULL,
  recpt_name VARCHAR(255) NULL,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  workorder_name VARCHAR(255) NULL,
  item_id BIGINT UNSIGNED NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
  recpt_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (recpt_id),
  UNIQUE KEY uk_wm_product_recpt_code (recpt_code)
) ENGINE=InnoDB COMMENT='产品入库单';

CREATE TABLE wm_product_recpt_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  recpt_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
  quantity_recived DOUBLE NOT NULL DEFAULT 0,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  workorder_name VARCHAR(255) NULL,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_product_recpt_line_recpt (recpt_id)
) ENGINE=InnoDB COMMENT='产品入库单行';

CREATE TABLE wm_product_recpt_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  recpt_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
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
  KEY idx_product_recpt_detail_recpt (recpt_id),
  KEY idx_product_recpt_detail_line (line_id)
) ENGINE=InnoDB COMMENT='产品入库单明细';

CREATE TABLE wm_rt_vendor (
  rt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_code VARCHAR(64) NOT NULL,
  rt_name VARCHAR(255) NOT NULL,
  po_code VARCHAR(64) NULL,
  vendor_id BIGINT UNSIGNED NULL,
  vendor_code VARCHAR(64) NULL,
  vendor_name VARCHAR(255) NULL,
  vendor_nick VARCHAR(255) NULL,
  rt_reason VARCHAR(255) NULL,
  transport_code VARCHAR(128) NULL,
  transport_tel VARCHAR(128) NULL,
  batch_code VARCHAR(255) NULL,
  rt_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (rt_id),
  UNIQUE KEY uk_wm_rt_vendor_code (rt_code)
) ENGINE=InnoDB COMMENT='采购退货单';

CREATE TABLE wm_rt_vendor_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  quantity_rted DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_rt_vendor_line_rt (rt_id)
) ENGINE=InnoDB COMMENT='采购退货单行';

CREATE TABLE wm_rt_vendor_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  rt_id BIGINT UNSIGNED NOT NULL,
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
  KEY idx_rt_vendor_detail_rt (rt_id),
  KEY idx_rt_vendor_detail_line (line_id)
) ENGINE=InnoDB COMMENT='采购退货单明细';

CREATE TABLE wm_product_sales (
  sales_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sales_code VARCHAR(64) NOT NULL,
  sales_name VARCHAR(255) NOT NULL,
  notice_id BIGINT UNSIGNED NULL,
  notice_code VARCHAR(64) NULL,
  so_code VARCHAR(64) NULL,
  client_id BIGINT UNSIGNED NULL,
  client_code VARCHAR(64) NULL,
  client_name VARCHAR(255) NULL,
  client_nick VARCHAR(255) NULL,
  recipient VARCHAR(128) NULL,
  tel VARCHAR(128) NULL,
  address VARCHAR(256) NULL,
  carrier VARCHAR(128) NULL,
  shipping_number VARCHAR(128) NULL,
  sales_date DATETIME NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (sales_id),
  UNIQUE KEY uk_wm_product_sales_code (sales_code)
) ENGINE=InnoDB COMMENT='销售出库单';

CREATE TABLE wm_product_sales_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sales_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
  quantity_sales DOUBLE NOT NULL DEFAULT 0,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  oqc_check CHAR(1) NULL,
  oqc_id BIGINT UNSIGNED NULL,
  oqc_code VARCHAR(64) NULL,
  quality_status VARCHAR(64) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_product_sales_line_sales (sales_id)
) ENGINE=InnoDB COMMENT='销售出库单行';

CREATE TABLE wm_product_sales_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  sales_id BIGINT UNSIGNED NOT NULL,
  material_stock_id BIGINT UNSIGNED NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
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
  KEY idx_product_sales_detail_sales (sales_id),
  KEY idx_product_sales_detail_line (line_id)
) ENGINE=InnoDB COMMENT='销售出库单明细';

CREATE TABLE wm_rt_sales (
  rt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_code VARCHAR(64) NOT NULL,
  rt_name VARCHAR(255) NOT NULL,
  so_code VARCHAR(64) NULL,
  client_id BIGINT UNSIGNED NULL,
  client_code VARCHAR(64) NULL,
  client_name VARCHAR(255) NULL,
  client_nick VARCHAR(255) NULL,
  rt_date DATETIME NULL,
  rt_reason VARCHAR(255) NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE',
  remark VARCHAR(500) NULL DEFAULT '',
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (rt_id),
  UNIQUE KEY uk_wm_rt_sales_code (rt_code)
) ENGINE=InnoDB COMMENT='销售退货单';

CREATE TABLE wm_rt_sales_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rt_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
  batch_id BIGINT UNSIGNED NULL,
  batch_code VARCHAR(255) NULL,
  quantity_rted DOUBLE NOT NULL DEFAULT 0,
  quality_status VARCHAR(64) NULL,
  remark VARCHAR(500) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (line_id),
  KEY idx_rt_sales_line_rt (rt_id)
) ENGINE=InnoDB COMMENT='销售退货单行';

CREATE TABLE wm_rt_sales_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  rt_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(64) NULL,
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
  KEY idx_rt_sales_detail_rt (rt_id),
  KEY idx_rt_sales_detail_line (line_id)
) ENGINE=InnoDB COMMENT='销售退货单明细';

-- 演示：产品入库 PREPARE
INSERT INTO wm_product_recpt (recpt_code, recpt_name, workorder_code, recpt_date, status, create_time)
VALUES ('PR-DEMO-001', '演示产品入库', 'WO-DEMO', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE recpt_name = VALUES(recpt_name);

INSERT INTO wm_product_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, workorder_code, create_time)
SELECT r.recpt_id, p.product_id, p.product_code, p.product_name, '件', 20, CONCAT('PB-', p.product_code), 'WO-DEMO', NOW()
FROM wm_product_recpt r
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE r.recpt_code = 'PR-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_product_recpt_line l WHERE l.recpt_id = r.recpt_id);

-- 演示：产品入库 APPROVED(可执行)
INSERT INTO wm_product_recpt (recpt_code, recpt_name, workorder_code, recpt_date, status, create_time)
VALUES ('PR-DEMO-002', '演示产品入库-待执行', 'WO-DEMO', NOW(), 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO wm_product_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, workorder_code, create_time)
SELECT r.recpt_id, p.product_id, p.product_code, p.product_name, '件', 10, CONCAT('PB2-', p.product_code), 'WO-DEMO', NOW()
FROM wm_product_recpt r
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE r.recpt_code = 'PR-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM wm_product_recpt_line l WHERE l.recpt_id = r.recpt_id);

INSERT INTO wm_product_recpt_detail (line_id, recpt_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, create_time)
SELECT l.line_id, l.recpt_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_recived, l.batch_code,
       w.warehouse_id, w.warehouse_code, w.warehouse_name, sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name, NOW()
FROM wm_product_recpt_line l
JOIN wm_product_recpt r ON r.recpt_id = l.recpt_id AND r.recpt_code = 'PR-DEMO-002'
JOIN warehouse w ON w.warehouse_code = 'WH-RAW'
JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
JOIN storage_bin sb ON sb.zone_id = sz.zone_id
WHERE NOT EXISTS (SELECT 1 FROM wm_product_recpt_detail d WHERE d.recpt_id = l.recpt_id);

-- 演示：采购退货 PREPARE
INSERT INTO wm_rt_vendor (rt_code, rt_name, vendor_name, po_code, rt_date, status, create_time)
VALUES ('RV-DEMO-001', '演示采购退货', '演示供应商', 'PO-DEMO-001', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name);

INSERT INTO wm_rt_vendor_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, create_time)
SELECT h.rt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 5, CONCAT('RB-', m.material_code), NOW()
FROM wm_rt_vendor h
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE h.rt_code = 'RV-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_vendor_line l WHERE l.rt_id = h.rt_id);

-- 演示：采购退货 APPROVED(可执行)
INSERT INTO wm_rt_vendor (rt_code, rt_name, vendor_name, po_code, rt_date, status, create_time)
VALUES ('RV-DEMO-002', '演示采购退货-待执行', '演示供应商', 'PO-DEMO-002', NOW(), 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO wm_rt_vendor_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, create_time)
SELECT h.rt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, ''), 3, CONCAT('RB2-', m.material_code), NOW()
FROM wm_rt_vendor h
JOIN material m ON m.material_code = (SELECT material_code FROM material WHERE is_deleted = 0 LIMIT 1)
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE h.rt_code = 'RV-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_vendor_line l WHERE l.rt_id = h.rt_id);

INSERT INTO wm_rt_vendor_detail (line_id, rt_id, material_stock_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, create_time)
SELECT l.line_id, l.rt_id, s.material_stock_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_rted, l.batch_code,
       s.warehouse_id, s.warehouse_code, s.warehouse_name, s.location_id, s.location_code, s.location_name,
       s.area_id, s.area_code, s.area_name, NOW()
FROM wm_rt_vendor_line l
JOIN wm_rt_vendor h ON h.rt_id = l.rt_id AND h.rt_code = 'RV-DEMO-002'
JOIN wm_material_stock s ON s.item_id = l.item_id
JOIN warehouse w ON w.warehouse_id = s.warehouse_id AND w.warehouse_code = 'WH-RAW'
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_vendor_detail d WHERE d.rt_id = l.rt_id);

-- 演示：销售出库 PREPARE
INSERT INTO wm_product_sales (sales_code, sales_name, client_name, so_code, sales_date, status, create_time)
VALUES ('PS-DEMO-001', '演示销售出库', '演示客户', 'SO-DEMO-001', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE sales_name = VALUES(sales_name);

INSERT INTO wm_product_sales_line (sales_id, item_id, item_code, item_name, unit_name, quantity_sales, batch_code, create_time)
SELECT h.sales_id, p.product_id, p.product_code, p.product_name, '件', 8, CONCAT('SB-', p.product_code), NOW()
FROM wm_product_sales h
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE h.sales_code = 'PS-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_product_sales_line l WHERE l.sales_id = h.sales_id);

-- 演示：销售出库 APPROVED(可执行)
INSERT INTO wm_product_sales (sales_code, sales_name, client_name, so_code, sales_date, status, create_time)
VALUES ('PS-DEMO-002', '演示销售出库-待执行', '演示客户', 'SO-DEMO-002', NOW(), 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO wm_product_sales_line (sales_id, item_id, item_code, item_name, unit_name, quantity_sales, batch_code, create_time)
SELECT h.sales_id, p.product_id, p.product_code, p.product_name, '件', 4, CONCAT('SB2-', p.product_code), NOW()
FROM wm_product_sales h
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE h.sales_code = 'PS-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM wm_product_sales_line l WHERE l.sales_id = h.sales_id);

INSERT INTO wm_product_sales_detail (line_id, sales_id, material_stock_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, create_time)
SELECT l.line_id, l.sales_id, s.material_stock_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_sales, l.batch_code,
       s.warehouse_id, s.warehouse_code, s.warehouse_name, s.location_id, s.location_code, s.location_name,
       s.area_id, s.area_code, s.area_name, NOW()
FROM wm_product_sales_line l
JOIN wm_product_sales h ON h.sales_id = l.sales_id AND h.sales_code = 'PS-DEMO-002'
JOIN wm_material_stock s ON s.item_id = l.item_id
JOIN warehouse w ON w.warehouse_id = s.warehouse_id AND w.warehouse_code = 'WH-RAW'
WHERE NOT EXISTS (SELECT 1 FROM wm_product_sales_detail d WHERE d.sales_id = l.sales_id);

-- 演示：销售退货 PREPARE
INSERT INTO wm_rt_sales (rt_code, rt_name, client_name, so_code, rt_date, status, create_time)
VALUES ('RS-DEMO-001', '演示销售退货', '演示客户', 'SO-DEMO-001', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name);

INSERT INTO wm_rt_sales_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, quality_status, create_time)
SELECT h.rt_id, p.product_id, p.product_code, p.product_name, '件', 2, CONCAT('RSB-', p.product_code), 'OK', NOW()
FROM wm_rt_sales h
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE h.rt_code = 'RS-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_sales_line l WHERE l.rt_id = h.rt_id);

-- 演示：销售退货 APPROVED(可执行)
INSERT INTO wm_rt_sales (rt_code, rt_name, client_name, so_code, rt_date, status, create_time)
VALUES ('RS-DEMO-002', '演示销售退货-待执行', '演示客户', 'SO-DEMO-002', NOW(), 'APPROVED', NOW())
ON DUPLICATE KEY UPDATE status = 'APPROVED';

INSERT INTO wm_rt_sales_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, quality_status, create_time)
SELECT h.rt_id, p.product_id, p.product_code, p.product_name, '件', 2, CONCAT('RSB2-', p.product_code), 'OK', NOW()
FROM wm_rt_sales h
JOIN product p ON p.product_code = (SELECT product_code FROM product LIMIT 1)
WHERE h.rt_code = 'RS-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_sales_line l WHERE l.rt_id = h.rt_id);

INSERT INTO wm_rt_sales_detail (line_id, rt_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name, location_id, location_code, location_name, area_id, area_code, area_name, create_time)
SELECT l.line_id, l.rt_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_rted, l.batch_code,
       w.warehouse_id, w.warehouse_code, w.warehouse_name, sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name, NOW()
FROM wm_rt_sales_line l
JOIN wm_rt_sales h ON h.rt_id = l.rt_id AND h.rt_code = 'RS-DEMO-002'
JOIN warehouse w ON w.warehouse_code = 'WH-RAW'
JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
JOIN storage_bin sb ON sb.zone_id = sz.zone_id
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_sales_detail d WHERE d.rt_id = l.rt_id);
