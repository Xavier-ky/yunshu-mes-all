-- V17: IQC 到货/外协源表 + IPQC 产出行质量状态 + 演示种子

CREATE TABLE IF NOT EXISTS wm_arrival_notice (
  notice_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '到货通知ID',
  notice_code VARCHAR(64) NOT NULL COMMENT '通知单编号',
  notice_name VARCHAR(255) NOT NULL COMMENT '通知单名称',
  po_code VARCHAR(64) NULL COMMENT '采购订单号',
  vendor_id BIGINT UNSIGNED NULL COMMENT '供应商ID',
  vendor_code VARCHAR(64) NULL COMMENT '供应商编码',
  vendor_name VARCHAR(255) NULL COMMENT '供应商名称',
  vendor_nick VARCHAR(255) NULL COMMENT '供应商简称',
  arrival_date DATETIME(3) NULL COMMENT '到货日期',
  contact VARCHAR(64) NULL COMMENT '联系人',
  tel VARCHAR(128) NULL COMMENT '联系电话',
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE' COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (notice_id),
  UNIQUE KEY uk_wm_arrival_notice_code (notice_code)
) ENGINE=InnoDB COMMENT='到货通知单';

CREATE TABLE IF NOT EXISTS wm_arrival_notice_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '行ID',
  notice_id BIGINT UNSIGNED NOT NULL COMMENT '通知单ID',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '物料ID',
  item_code VARCHAR(64) NULL COMMENT '物料编码',
  item_name VARCHAR(255) NULL COMMENT '物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位编码',
  unit_name VARCHAR(128) NULL COMMENT '单位名称',
  quantity_arrival DOUBLE(14,2) NOT NULL DEFAULT 0 COMMENT '到货数量',
  quantity_quanlified DOUBLE(14,2) NULL COMMENT '合格数量',
  iqc_check CHAR(1) NULL DEFAULT 'Y' COMMENT '是否检验',
  iqc_id BIGINT UNSIGNED NULL COMMENT '来料检验单ID',
  iqc_code VARCHAR(64) NULL COMMENT '来料检验单编号',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_arrival_notice_line_notice (notice_id),
  KEY idx_arrival_notice_line_iqc (iqc_check, iqc_id)
) ENGINE=InnoDB COMMENT='到货通知单行';

CREATE TABLE IF NOT EXISTS wm_outsource_recpt (
  recpt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '外协入库ID',
  recpt_code VARCHAR(64) NOT NULL COMMENT '入库单编号',
  recpt_name VARCHAR(255) NOT NULL COMMENT '入库单名称',
  workorder_id BIGINT UNSIGNED NULL COMMENT '工单ID',
  workorder_code VARCHAR(64) NULL COMMENT '工单编号',
  vendor_id BIGINT UNSIGNED NULL COMMENT '供应商ID',
  vendor_code VARCHAR(64) NULL COMMENT '供应商编码',
  vendor_name VARCHAR(255) NULL COMMENT '供应商名称',
  vendor_nick VARCHAR(255) NULL COMMENT '供应商简称',
  recpt_date DATETIME(3) NULL COMMENT '入库日期',
  status VARCHAR(64) NOT NULL DEFAULT 'PREPARE' COMMENT '单据状态',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (recpt_id),
  UNIQUE KEY uk_wm_outsource_recpt_code (recpt_code)
) ENGINE=InnoDB COMMENT='外协入库单';

CREATE TABLE IF NOT EXISTS wm_outsource_recpt_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '行ID',
  recpt_id BIGINT UNSIGNED NOT NULL COMMENT '入库单ID',
  item_id BIGINT UNSIGNED NOT NULL COMMENT '物料ID',
  item_code VARCHAR(64) NULL COMMENT '物料编码',
  item_name VARCHAR(255) NULL COMMENT '物料名称',
  specification VARCHAR(500) NULL COMMENT '规格型号',
  unit_of_measure VARCHAR(64) NULL COMMENT '单位编码',
  unit_name VARCHAR(128) NULL COMMENT '单位名称',
  quantity_recived DOUBLE(14,2) NOT NULL DEFAULT 0 COMMENT '接收数量',
  batch_id BIGINT UNSIGNED NULL COMMENT '批次ID',
  batch_code VARCHAR(255) NULL COMMENT '批次号',
  produce_date DATETIME(3) NULL COMMENT '生产日期',
  lot_number VARCHAR(128) NULL COMMENT '生产批号',
  expire_date DATETIME(3) NULL COMMENT '有效期',
  quality_status VARCHAR(64) NULL DEFAULT 'NT' COMMENT '质量状态',
  iqc_check CHAR(1) NULL DEFAULT 'Y' COMMENT '是否检验',
  iqc_id BIGINT UNSIGNED NULL COMMENT '检验单ID',
  iqc_code VARCHAR(64) NULL COMMENT '检验单编号',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  create_time DATETIME(3) NULL COMMENT '创建时间',
  update_time DATETIME(3) NULL COMMENT '更新时间',
  PRIMARY KEY (line_id),
  KEY idx_outsource_recpt_line_recpt (recpt_id),
  KEY idx_outsource_recpt_line_qs (quality_status)
) ENGINE=InnoDB COMMENT='外协入库单行';

-- IPQC 产出行质量状态
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wm_product_produce_line' AND COLUMN_NAME = 'quality_status'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE wm_product_produce_line ADD COLUMN quality_status VARCHAR(64) NULL COMMENT ''质量状态'' AFTER batch_code',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 演示：到货通知（待 IQC）
INSERT INTO wm_arrival_notice (notice_id, notice_code, notice_name, po_code, vendor_id, vendor_code, vendor_name, vendor_nick, arrival_date, status, create_by, create_time)
SELECT 1, 'AN-DEMO-001', 'Demo Arrival Notice', 'PO-DEMO-001', 1, 'V-DEMO', 'Demo Vendor', 'DemoVendor', NOW(3), 'APPROVING', 'system', NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wm_arrival_notice WHERE notice_code = 'AN-DEMO-001');

INSERT INTO wm_arrival_notice_line (notice_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_arrival, iqc_check, create_time)
SELECT n.notice_id, 1, 'MAT-MOTOR-55W', '55W Motor', 'PCS', 'PCS', 100, 'Y', NOW(3)
FROM wm_arrival_notice n
WHERE n.notice_code = 'AN-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_arrival_notice_line l WHERE l.notice_id = n.notice_id);

INSERT INTO wm_outsource_recpt (recpt_id, recpt_code, recpt_name, workorder_id, workorder_code, vendor_id, vendor_code, vendor_name, recpt_date, status, create_by, create_time)
SELECT 1, 'OR-DEMO-001', 'Demo Outsource Recpt', 1, 'WO-20260701', 1, 'V-DEMO', 'Demo Vendor', NOW(3), 'UNCHECK', 'system', NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wm_outsource_recpt WHERE recpt_code = 'OR-DEMO-001');

INSERT INTO wm_outsource_recpt_line (recpt_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_recived, quality_status, iqc_check, create_time)
SELECT r.recpt_id, 2, 'MAT-BLADE-40', '40cm Blade', 'PCS', 'PCS', 50, 'NT', 'Y', NOW(3)
FROM wm_outsource_recpt r
WHERE r.recpt_code = 'OR-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM wm_outsource_recpt_line l WHERE l.recpt_id = r.recpt_id);

-- 演示：产品产出待过程检（若尚无产出单）
INSERT INTO wm_product_produce (record_id, workorder_id, workorder_code, feedback_id, status, remark, create_by, create_time)
SELECT 9001, 1, 'WO-20260701', NULL, 'PREPARE', '', 'system', NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce WHERE record_id = 9001);

INSERT INTO wm_product_produce_line (line_id, record_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_produce, batch_code, remark, create_by, create_time)
SELECT 9001, 9001, 2, 'FAN-FS40', '40cm Stand Fan', 'PCS', '件', 20, 'BATCH-PQC-001', '', 'system', NOW(3)
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce_line WHERE line_id = 9001);

-- quality_status set after column add
UPDATE wm_product_produce_line SET quality_status = 'NT' WHERE line_id = 9001 AND (quality_status IS NULL OR quality_status = '');


-- 演示：销售退货待 RQC（若表存在且无待检行）
INSERT INTO wm_rt_sales (rt_id, rt_code, rt_name, client_id, client_code, client_name, rt_date, status, create_by, create_time)
SELECT 9001, 'RTS-DEMO-QC', '演示销售退货待检', 1, 'C-DEMO', '演示客户', NOW(3), 'PREPARE', 'system', NOW(3)
FROM DUAL
WHERE EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wm_rt_sales')
  AND NOT EXISTS (SELECT 1 FROM wm_rt_sales WHERE rt_code = 'RTS-DEMO-QC');

INSERT INTO wm_rt_sales_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, quality_status, create_time)
SELECT r.rt_id, 2, 'FAN-FS40', '40cm Stand Fan', '件', 5, 'BATCH-RTS-001', 'NT', NOW(3)
FROM wm_rt_sales r
WHERE r.rt_code = 'RTS-DEMO-QC'
  AND NOT EXISTS (SELECT 1 FROM wm_rt_sales_line l WHERE l.rt_id = r.rt_id AND l.quality_status = 'NT');
