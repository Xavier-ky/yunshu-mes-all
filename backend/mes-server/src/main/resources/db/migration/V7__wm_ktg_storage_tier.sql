-- yunshu WMS P0 Sprint 1: 三级仓储 + 库存现有量表

ALTER TABLE warehouse
  ADD COLUMN location VARCHAR(500) NULL COMMENT '位置' AFTER warehouse_type,
  ADD COLUMN area DOUBLE NULL COMMENT '面积' AFTER location,
  ADD COLUMN user_id BIGINT UNSIGNED NULL COMMENT '负责人用户ID' AFTER area,
  ADD COLUMN user_name VARCHAR(64) NULL COMMENT '负责人账号' AFTER user_id,
  ADD COLUMN charge VARCHAR(64) NULL COMMENT '负责人名称' AFTER user_name,
  ADD COLUMN frozen_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否冻结' AFTER charge,
  ADD COLUMN enable_flag CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用' AFTER frozen_flag,
  ADD COLUMN remark VARCHAR(500) NULL DEFAULT '' COMMENT '备注' AFTER enable_flag,
  ADD COLUMN attr1 VARCHAR(64) NULL AFTER remark,
  ADD COLUMN attr2 VARCHAR(255) NULL AFTER attr1,
  ADD COLUMN attr3 INT NOT NULL DEFAULT 0 AFTER attr2,
  ADD COLUMN attr4 INT NOT NULL DEFAULT 0 AFTER attr3,
  ADD COLUMN create_by VARCHAR(64) NULL DEFAULT '' AFTER attr4,
  ADD COLUMN create_time DATETIME NULL AFTER create_by,
  ADD COLUMN update_by VARCHAR(64) NULL DEFAULT '' AFTER create_time,
  ADD COLUMN update_time DATETIME NULL AFTER update_by;

CREATE TABLE storage_zone (
  zone_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库区ID(zone id)',
  zone_code VARCHAR(64) NOT NULL COMMENT '库区编码',
  zone_name VARCHAR(255) NOT NULL COMMENT '库区名称',
  warehouse_id BIGINT UNSIGNED NOT NULL COMMENT '仓库ID',
  area DOUBLE NULL COMMENT '面积',
  area_flag CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '是否开启库位管理',
  frozen_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否冻结',
  remark VARCHAR(500) NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (zone_id),
  UNIQUE KEY uk_storage_zone_code (zone_code),
  KEY idx_storage_zone_warehouse (warehouse_id),
  CONSTRAINT fk_storage_zone_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
) ENGINE=InnoDB COMMENT='库区(wm_storage_zone)';

CREATE TABLE storage_bin (
  bin_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库位ID(bin id)',
  bin_code VARCHAR(64) NOT NULL COMMENT '库位编码',
  bin_name VARCHAR(255) NOT NULL COMMENT '库位名称',
  zone_id BIGINT UNSIGNED NOT NULL COMMENT '库区ID',
  legacy_location_id BIGINT UNSIGNED NULL COMMENT '原 storage_location 映射',
  area DOUBLE NULL COMMENT '面积',
  max_loa DOUBLE NULL COMMENT '最大载重量',
  position_x INT NULL,
  position_y INT NULL,
  position_z INT NULL,
  enable_flag CHAR(1) NULL DEFAULT 'Y' COMMENT '是否启用',
  frozen_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否冻结',
  product_mixing CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '允许产品混放',
  batch_mixing CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '允许批次混放',
  remark VARCHAR(500) NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (bin_id),
  UNIQUE KEY uk_storage_bin_code (bin_code),
  KEY idx_storage_bin_zone (zone_id),
  KEY idx_storage_bin_legacy (legacy_location_id),
  CONSTRAINT fk_storage_bin_zone
    FOREIGN KEY (zone_id) REFERENCES storage_zone (zone_id)
) ENGINE=InnoDB COMMENT='库位(wm_storage_bin)';

CREATE TABLE wm_material_stock (
  material_stock_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存记录ID',
  item_type_id BIGINT UNSIGNED NULL COMMENT '物料类型ID',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '物料ID',
  item_code VARCHAR(64) NULL,
  item_name VARCHAR(255) NULL,
  specification VARCHAR(500) NULL,
  unit_of_measure VARCHAR(64) NULL,
  unit_name VARCHAR(128) NULL,
  batch_id BIGINT UNSIGNED NULL COMMENT 'inventory_batch.batch_id',
  batch_code VARCHAR(255) NULL,
  workorder_id BIGINT UNSIGNED NULL,
  workorder_code VARCHAR(64) NULL,
  vendor_id BIGINT UNSIGNED NULL,
  vendor_code VARCHAR(64) NULL,
  vendor_name VARCHAR(255) NULL,
  vendor_nick VARCHAR(64) NULL,
  client_id BIGINT UNSIGNED NULL,
  client_code VARCHAR(64) NULL,
  client_name VARCHAR(255) NULL,
  client_nick VARCHAR(255) NULL,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  warehouse_code VARCHAR(64) NULL,
  warehouse_name VARCHAR(255) NULL,
  location_id BIGINT UNSIGNED NULL COMMENT '库区ID=storage_zone.zone_id',
  location_code VARCHAR(64) NULL,
  location_name VARCHAR(255) NULL,
  area_id BIGINT UNSIGNED NULL COMMENT '库位ID=storage_bin.bin_id',
  area_code VARCHAR(64) NULL,
  area_name VARCHAR(255) NULL,
  package_id BIGINT UNSIGNED NULL,
  package_code VARCHAR(64) NULL,
  quantity_onhand DOUBLE NOT NULL DEFAULT 0,
  quantity_reserved DOUBLE NOT NULL DEFAULT 0,
  production_date DATETIME NULL,
  recpt_date DATETIME NULL,
  expire_date DATETIME NULL,
  frozen_flag CHAR(1) NOT NULL DEFAULT 'N',
  attr1 VARCHAR(64) NULL,
  attr2 VARCHAR(255) NULL,
  attr3 INT NOT NULL DEFAULT 0,
  attr4 INT NOT NULL DEFAULT 0,
  create_by VARCHAR(64) NULL DEFAULT '',
  create_time DATETIME NULL,
  update_by VARCHAR(64) NULL DEFAULT '',
  update_time DATETIME NULL,
  PRIMARY KEY (material_stock_id),
  KEY idx_wm_stock_item (item_id),
  KEY idx_wm_stock_warehouse (warehouse_id, location_id, area_id),
  KEY idx_wm_stock_batch (batch_id),
  CONSTRAINT fk_wm_stock_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
) ENGINE=InnoDB COMMENT='库存现有量(wm_material_stock)';

-- 默认库区：每个仓库一条
INSERT INTO storage_zone (zone_code, zone_name, warehouse_id, area_flag, frozen_flag, create_time)
SELECT CONCAT(w.warehouse_code, '-ZONE'), CONCAT(w.warehouse_name, '默认库区'), w.warehouse_id, 'Y', 'N', NOW()
FROM warehouse w
WHERE NOT EXISTS (
  SELECT 1 FROM storage_zone sz WHERE sz.warehouse_id = w.warehouse_id
);

-- 旧 storage_location → storage_bin
INSERT INTO storage_bin (bin_code, bin_name, zone_id, legacy_location_id, enable_flag, frozen_flag, create_time)
SELECT sl.location_code, sl.location_name, sz.zone_id, sl.location_id,
       IF(sl.status = 'ENABLED', 'Y', 'N'), 'N', NOW()
FROM storage_location sl
JOIN storage_zone sz ON sz.warehouse_id = sl.warehouse_id
WHERE NOT EXISTS (
  SELECT 1 FROM storage_bin sb WHERE sb.legacy_location_id = sl.location_id
);

-- inventory_batch → wm_material_stock
INSERT INTO wm_material_stock (
  item_id, item_code, item_name, specification, unit_name,
  batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name,
  area_id, area_code, area_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT
  ib.material_id,
  m.material_code,
  m.material_name,
  '',
  IFNULL(u.unit_name, ''),
  ib.batch_id,
  ib.batch_no,
  ib.warehouse_id,
  w.warehouse_code,
  w.warehouse_name,
  sz.zone_id,
  sz.zone_code,
  sz.zone_name,
  sb.bin_id,
  sb.bin_code,
  sb.bin_name,
  ib.available_qty,
  ib.locked_qty,
  ib.received_at,
  ib.expire_date,
  'N',
  NOW()
FROM inventory_batch ib
JOIN material m ON m.material_id = ib.material_id
JOIN warehouse w ON w.warehouse_id = ib.warehouse_id
LEFT JOIN uom u ON u.unit_id = m.unit_id
LEFT JOIN storage_bin sb ON sb.legacy_location_id = ib.location_id
LEFT JOIN storage_zone sz ON sz.zone_id = sb.zone_id
WHERE NOT EXISTS (
  SELECT 1 FROM wm_material_stock s WHERE s.batch_id = ib.batch_id
);
