-- Story seed 03: inventory batches, wm_material_stock, WM documents (12 pages)
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 扩展原材料批次（TS30/WS20 物料）==========
INSERT INTO inventory_batch (
  material_id, warehouse_id, location_id, batch_no, supplier_batch_no,
  available_qty, locked_qty, quality_status, received_at, expire_date, status
)
SELECT m.material_id,
  (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
  NULL,
  v.batch_no, v.supplier_batch, v.avail_qty, v.locked_qty, 'QUALIFIED',
  v.received_at, DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'IN_STOCK'
FROM (
  SELECT 'MAT-MOTOR-40W' AS mc, 'BATCH-MOTOR40-202607' AS batch_no, 'SUP-M40-2406' AS supplier_batch,
    800.0000 AS avail_qty, 50.0000 AS locked_qty, DATE_SUB(NOW(), INTERVAL 12 DAY) AS received_at
  UNION ALL SELECT 'MAT-MOTOR-35W', 'BATCH-MOTOR35-202607', 'SUP-M35-2406', 520.0000, 20.0000, DATE_SUB(NOW(), INTERVAL 11 DAY)
  UNION ALL SELECT 'MAT-BLADE-30', 'BATCH-BLADE30-202607', 'SUP-B30-2406', 650.0000, 0.0000, DATE_SUB(NOW(), INTERVAL 9 DAY)
  UNION ALL SELECT 'MAT-BLADE-20', 'BATCH-BLADE20-202607', 'SUP-B20-2406', 480.0000, 0.0000, DATE_SUB(NOW(), INTERVAL 8 DAY)
  UNION ALL SELECT 'MAT-SHELL-TS30', 'BATCH-SHELL-TS30-202607', 'SUP-SH30-2406', 520.0000, 30.0000, DATE_SUB(NOW(), INTERVAL 10 DAY)
  UNION ALL SELECT 'MAT-SHELL-WS20', 'BATCH-SHELL-WS20-202607', 'SUP-SH20-2406', 380.0000, 0.0000, DATE_SUB(NOW(), INTERVAL 7 DAY)
  UNION ALL SELECT 'MAT-PACK-TS30', 'BATCH-PACK-TS30-202607', 'SUP-PK30-2406', 900.0000, 0.0000, DATE_SUB(NOW(), INTERVAL 6 DAY)
  UNION ALL SELECT 'MAT-PACK-WS20', 'BATCH-PACK-WS20-202607', 'SUP-PK20-2406', 600.0000, 0.0000, DATE_SUB(NOW(), INTERVAL 5 DAY)
) v
JOIN material m ON m.material_code = v.mc
ON DUPLICATE KEY UPDATE
  available_qty = VALUES(available_qty), locked_qty = VALUES(locked_qty),
  quality_status = VALUES(quality_status), received_at = VALUES(received_at);

-- 同步 wm_material_stock
INSERT INTO wm_material_stock (
  item_id, item_code, item_name, specification, unit_name,
  batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT ib.material_id, m.material_code, m.material_name, '', IFNULL(u.unit_name, ''),
  ib.batch_id, ib.batch_no, ib.warehouse_id, w.warehouse_code, w.warehouse_name,
  sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name,
  ib.available_qty, ib.locked_qty, ib.received_at, ib.expire_date, 'N', NOW()
FROM inventory_batch ib
JOIN material m ON m.material_id = ib.material_id
JOIN warehouse w ON w.warehouse_id = ib.warehouse_id
LEFT JOIN uom u ON u.unit_id = m.unit_id
LEFT JOIN storage_bin sb ON sb.legacy_location_id = ib.location_id
LEFT JOIN storage_zone sz ON sz.zone_id = sb.zone_id
WHERE ib.batch_no LIKE 'BATCH-%202607'
  AND NOT EXISTS (SELECT 1 FROM wm_material_stock s WHERE s.batch_id = ib.batch_id);

-- ========== 库存导航分类与演示现有量 ==========
INSERT INTO md_item_type (
  item_type_code, item_type_name, parent_type_id, ancestors, item_or_product, order_num, enable_flag
)
VALUES
  ('ITEM_SEMI', '半成品', 2, '0,2', 'ITEM', 2, 'Y'),
  ('ITEM_AUX', '辅料', 2, '0,2', 'ITEM', 3, 'Y'),
  ('ITEM_SPARE', '备品备件', 2, '0,2', 'ITEM', 4, 'Y')
ON DUPLICATE KEY UPDATE item_type_name = VALUES(item_type_name), enable_flag = VALUES(enable_flag);

UPDATE md_item mi
JOIN md_item_type t ON t.item_type_code = 'ITEM_SEMI'
SET mi.item_type_id = t.item_type_id, mi.item_type_code = t.item_type_code, mi.item_type_name = t.item_type_name
WHERE mi.item_code LIKE 'MAT-SHELL-%';

UPDATE md_item mi
JOIN md_item_type t ON t.item_type_code = 'ITEM_AUX'
SET mi.item_type_id = t.item_type_id, mi.item_type_code = t.item_type_code, mi.item_type_name = t.item_type_name
WHERE mi.item_code LIKE 'MAT-PACK-%' OR mi.item_code LIKE 'MAT-SCREW-%';

UPDATE md_item mi
JOIN md_item_type t ON t.item_type_code = 'ITEM_SPARE'
SET mi.item_type_id = t.item_type_id, mi.item_type_code = t.item_type_code, mi.item_type_name = t.item_type_name
WHERE mi.item_code = 'MAT-MOTOR-60W';

UPDATE wm_material_stock s
JOIN md_item mi ON mi.item_code = s.item_code
SET s.item_type_id = mi.item_type_id
WHERE s.item_type_id IS NULL OR s.item_type_id <> mi.item_type_id;

INSERT INTO wm_material_stock (
  item_id, item_type_id, item_code, item_name, specification, unit_name,
  batch_code, warehouse_id, warehouse_code, warehouse_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT mi.item_id, mi.item_type_id, mi.item_code, mi.item_name, mi.specification, mi.unit_name,
  CONCAT('FIN-', mi.item_code, '-202607'), w.warehouse_id, w.warehouse_code, w.warehouse_name,
  CASE mi.item_code
    WHEN 'FAN-FS40-A' THEN 186
    WHEN 'FAN-TS30-B' THEN 92
    WHEN 'FAN-WS20-C' THEN 74
    ELSE 36
  END,
  0, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'N', NOW()
FROM md_item mi
JOIN warehouse w ON w.warehouse_code = 'WH-FIN'
WHERE mi.item_type_id = 3
  AND NOT EXISTS (
    SELECT 1 FROM wm_material_stock s
    WHERE s.item_code = mi.item_code AND s.warehouse_id = w.warehouse_id
  );

INSERT INTO wm_material_stock (
  item_id, item_type_id, item_code, item_name, specification, unit_name,
  batch_code, warehouse_id, warehouse_code, warehouse_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT mi.item_id, mi.item_type_id, mi.item_code, mi.item_name, mi.specification, mi.unit_name,
  'SPARE-MOTOR-202607', w.warehouse_id, w.warehouse_code, w.warehouse_name,
  24, 2, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 'N', NOW()
FROM md_item mi
JOIN warehouse w ON w.warehouse_code = 'WH-RAW'
WHERE mi.item_code = 'MAT-MOTOR-60W'
  AND NOT EXISTS (SELECT 1 FROM wm_material_stock s WHERE s.item_code = mi.item_code);

-- ========== 补充 10 条合理库存现有量（缺货补齐 / 第二批次 / 冻结 / 临期 / 不良）==========
INSERT INTO wm_material_stock (
  item_id, item_type_id, item_code, item_name, specification, unit_name,
  batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name,
  quantity_onhand, quantity_reserved, production_date, recpt_date, expire_date, frozen_flag, create_time
)
SELECT v.item_id, mi.item_type_id, mi.item_code, mi.item_name, mi.specification, mi.unit_name,
  v.batch_code, w.warehouse_id, w.warehouse_code, w.warehouse_name,
  sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name,
  v.qty, v.reserved, v.prod_date, v.recpt_date, v.expire_date, v.frozen, NOW()
FROM (
  SELECT 14 AS item_id, 'BATCH-BLADE40CM-202607' AS batch_code, 'WH-RAW' AS wh, 420 AS qty, 0 AS reserved,
    DATE_SUB(CURDATE(), INTERVAL 18 DAY) AS prod_date, DATE_SUB(CURDATE(), INTERVAL 15 DAY) AS recpt_date,
    DATE_ADD(CURDATE(), INTERVAL 540 DAY) AS expire_date, 'N' AS frozen
  UNION ALL SELECT 15, 'BATCH-MMOTOR-202607', 'WH-RAW', 310, 40,
    DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 16 DAY),
    DATE_ADD(CURDATE(), INTERVAL 720 DAY), 'N'
  UNION ALL SELECT 8, 'BATCH-MOTOR55-202607-B', 'WH-RAW', 360, 60,
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY),
    DATE_ADD(CURDATE(), INTERVAL 540 DAY), 'N'
  UNION ALL SELECT 11, 'BATCH-SCREW-M4-202607-B', 'WH-RAW', 5000, 200,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY),
    DATE_ADD(CURDATE(), INTERVAL 1095 DAY), 'N'
  UNION ALL SELECT 10, 'BATCH-SHELL-FS40-202607-B', 'WH-RAW', 280, 20,
    DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY),
    DATE_ADD(CURDATE(), INTERVAL 730 DAY), 'N'
  UNION ALL SELECT 1, 'FIN-FAN-FS40-A-202607-B', 'WH-FIN', 128, 24,
    DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY),
    DATE_ADD(CURDATE(), INTERVAL 730 DAY), 'N'
  UNION ALL SELECT 16, 'FIN-FAN-TS30-B-202607-B', 'WH-FIN', 65, 30,
    DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY),
    DATE_ADD(CURDATE(), INTERVAL 730 DAY), 'N'
  UNION ALL SELECT 17, 'FIN-FAN-WS20-C-HOLD', 'WH-FIN', 12, 0,
    DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 10 DAY),
    DATE_ADD(CURDATE(), INTERVAL 730 DAY), 'Y'
  UNION ALL SELECT 1, 'SCRAP-FAN-FS40-A-202607', 'WH-SCRAP', 6, 0,
    DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),
    DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'Y'
  UNION ALL SELECT 12, 'BATCH-PACK-FS40-NEAR', 'WH-RAW', 86, 0,
    DATE_SUB(CURDATE(), INTERVAL 330 DAY), DATE_SUB(CURDATE(), INTERVAL 320 DAY),
    DATE_ADD(CURDATE(), INTERVAL 18 DAY), 'N'
) v
JOIN md_item mi ON mi.item_id = v.item_id
JOIN warehouse w ON w.warehouse_code = v.wh
LEFT JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
LEFT JOIN storage_bin sb ON sb.zone_id = sz.zone_id
WHERE NOT EXISTS (
  SELECT 1 FROM wm_material_stock s
  WHERE s.item_code = mi.item_code AND s.batch_code = v.batch_code AND s.warehouse_id = w.warehouse_id
);

-- ========== 物料入库单 ==========
INSERT INTO wm_item_recpt (recpt_code, recpt_name, vendor_id, vendor_code, vendor_name, po_code, recpt_date, status, create_by, create_time)
SELECT v.recpt_code, v.recpt_name, mv.vendor_id, mv.vendor_code, mv.vendor_name, v.po_code, v.recpt_date, v.status, 'warehouse01', NOW()
FROM (
  SELECT 'IR-STORY-001' AS recpt_code, '55W电机来料入库' AS recpt_name, 'V001' AS vc, 'PO-20260701' AS po_code,
    DATE_SUB(NOW(), INTERVAL 14 DAY) AS recpt_date, 'FINISHED' AS status
  UNION ALL SELECT 'IR-STORY-002', '40W台扇电机入库', 'V001', 'PO-20260708', DATE_SUB(NOW(), INTERVAL 12 DAY), 'FINISHED'
  UNION ALL SELECT 'IR-STORY-003', 'M4螺丝补货入库', 'V004', 'PO-20260710', NOW(), 'PREPARE'
  UNION ALL SELECT 'IR-STORY-004', 'TS30包装箱入库', 'V005', 'PO-20260711', NOW(), 'APPROVED'
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
ON DUPLICATE KEY UPDATE recpt_name = VALUES(recpt_name), vendor_name = VALUES(vendor_name), status = VALUES(status);

INSERT INTO wm_item_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, create_time)
SELECT r.recpt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, '件'), v.qty, v.batch_code, NOW()
FROM wm_item_recpt r
JOIN (
  SELECT 'IR-STORY-001' AS rc, 'MAT-MOTOR-55W' AS mc, 500 AS qty, 'BATCH-MOTOR-202607' AS batch_code
  UNION ALL SELECT 'IR-STORY-002', 'MAT-MOTOR-40W', 300, 'BATCH-MOTOR40-202607'
  UNION ALL SELECT 'IR-STORY-003', 'MAT-SCREW-M4', 2000, 'BATCH-SCREW-202607'
  UNION ALL SELECT 'IR-STORY-004', 'MAT-PACK-TS30', 400, 'BATCH-PACK-TS30-202607'
) v ON v.rc = r.recpt_code
JOIN material m ON m.material_code = v.mc
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_item_recpt_line l WHERE l.recpt_id = r.recpt_id AND l.item_code = v.mc);

-- ========== 生产领料 ==========
INSERT INTO wm_issue_header (issue_code, issue_name, workorder_id, workorder_code, issue_date, status, create_time)
SELECT v.issue_code, v.issue_name, wo.work_order_id, wo.work_order_no, v.issue_date, v.status, NOW()
FROM (
  SELECT 'IS-STORY-001' AS issue_code, 'WO-20260701生产领料' AS issue_name, 'WO-20260701' AS wo,
    DATE_SUB(NOW(), INTERVAL 3 DAY) AS issue_date, 'FINISHED' AS status
  UNION ALL SELECT 'IS-STORY-002', 'WO-20260707台扇领料', 'WO-20260707', DATE_SUB(NOW(), INTERVAL 2 DAY), 'FINISHED'
  UNION ALL SELECT 'IS-STORY-003', 'WO-20260704待领料', 'WO-20260704', NOW(), 'PREPARE'
  UNION ALL SELECT 'IS-STORY-004', 'WO-20260708壁扇领料', 'WO-20260708', NOW(), 'APPROVED'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE issue_name = VALUES(issue_name), status = VALUES(status);

INSERT INTO wm_issue_line (issue_id, item_id, item_code, item_name, unit_name, quantity_issued, create_time)
SELECT h.issue_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, '件'), v.qty, NOW()
FROM wm_issue_header h
JOIN (
  SELECT 'IS-STORY-001' AS ic, 'MAT-MOTOR-55W' AS mc, 320 AS qty
  UNION ALL SELECT 'IS-STORY-001', 'MAT-BLADE-40', 320
  UNION ALL SELECT 'IS-STORY-002', 'MAT-MOTOR-40W', 180
  UNION ALL SELECT 'IS-STORY-002', 'MAT-BLADE-30', 180
  UNION ALL SELECT 'IS-STORY-003', 'MAT-MOTOR-55W', 250
  UNION ALL SELECT 'IS-STORY-004', 'MAT-MOTOR-35W', 60
) v ON v.ic = h.issue_code
JOIN material m ON m.material_code = v.mc
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_issue_line l WHERE l.issue_id = h.issue_id AND l.item_code = v.mc);

-- ========== 生产退料 ==========
INSERT INTO wm_rt_issue (rt_code, rt_name, workorder_code, rt_type, rt_date, status, create_time)
VALUES
  ('RT-STORY-001', 'WO-20260701余料退库', 'WO-20260701', '余料', DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED', NOW()),
  ('RT-STORY-002', 'WO-20260707退料待检', 'WO-20260707', '不良', NOW(), 'PREPARE', NOW())
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name), status = VALUES(status);

INSERT INTO wm_rt_issue_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rt, qc_flag, quality_status, create_time)
SELECT h.rt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, '件'), v.qty, v.qc_flag, v.qs, NOW()
FROM wm_rt_issue h
JOIN (
  SELECT 'RT-STORY-001' AS rc, 'MAT-SCREW-M4' AS mc, 24 AS qty, 'N' AS qc_flag, 'OK' AS qs
  UNION ALL SELECT 'RT-STORY-002', 'MAT-MOTOR-40W', 2, 'Y', 'PENDING'
) v ON v.rc = h.rt_code
JOIN material m ON m.material_code = v.mc
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_issue_line l WHERE l.rt_id = h.rt_id AND l.item_code = v.mc);

-- ========== 产品入库 ==========
INSERT INTO wm_product_recpt (recpt_code, recpt_name, workorder_id, workorder_code, recpt_date, status, create_by, create_time)
SELECT v.recpt_code, v.recpt_name, wo.work_order_id, wo.work_order_no, v.recpt_date, v.status, 'warehouse01', NOW()
FROM (
  SELECT 'PR-STORY-001' AS recpt_code, 'WO-20260705成品入库' AS recpt_name, 'WO-20260705' AS wo,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS recpt_date, 'FINISHED' AS status
  UNION ALL SELECT 'PR-STORY-002', 'WO-20260710台扇成品入库', 'WO-20260710', DATE_SUB(NOW(), INTERVAL 4 DAY), 'FINISHED'
  UNION ALL SELECT 'PR-STORY-003', 'WO-20260707成品待入库', 'WO-20260707', NOW(), 'PREPARE'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE recpt_name = VALUES(recpt_name), status = VALUES(status);

INSERT INTO wm_product_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, workorder_code, create_time)
SELECT r.recpt_id, p.product_id, p.product_code, p.product_name, '件', v.qty, v.batch_code, wo.work_order_no, NOW()
FROM wm_product_recpt r
JOIN work_order wo ON wo.work_order_no = r.workorder_code
JOIN product p ON p.product_id = wo.product_id
JOIN (
  SELECT 'PR-STORY-001' AS rc, 397 AS qty, 'PB-FS40-20260705' AS batch_code
  UNION ALL SELECT 'PR-STORY-002', 120, 'PB-TS30-20260710'
  UNION ALL SELECT 'PR-STORY-003', 50, 'PB-TS30-20260707'
) v ON v.rc = r.recpt_code
WHERE NOT EXISTS (SELECT 1 FROM wm_product_recpt_line l WHERE l.recpt_id = r.recpt_id);

-- ========== 销售出库 ==========
INSERT INTO wm_product_sales (sales_code, sales_name, client_id, client_code, client_name, so_code, sales_date, status, create_by, create_time)
SELECT v.sales_code, v.sales_name, c.client_id, c.client_code, c.client_name, v.so_code, v.sales_date, v.status, 'warehouse01', NOW()
FROM (
  SELECT 'PS-STORY-001' AS sales_code, '珠三角连锁FS40出库' AS sales_name, 'C005' AS cc, 'SO-20260705' AS so_code,
    DATE_SUB(NOW(), INTERVAL 1 DAY) AS sales_date, 'FINISHED' AS status
  UNION ALL SELECT 'PS-STORY-002', '华东连锁TS30出库', 'C001', 'SO-20260706', NOW(), 'PREPARE'
  UNION ALL SELECT 'PS-STORY-003', '北方商城TS30出库', 'C003', 'SO-20260710', NOW(), 'APPROVED'
) v
JOIN md_client c ON c.client_code = v.cc
ON DUPLICATE KEY UPDATE sales_name = VALUES(sales_name), client_name = VALUES(client_name), status = VALUES(status);

INSERT INTO wm_product_sales_line (sales_id, item_id, item_code, item_name, unit_name, quantity_sales, batch_code, create_time)
SELECT s.sales_id, p.product_id, p.product_code, p.product_name, '件', v.qty, v.batch_code, NOW()
FROM wm_product_sales s
JOIN (
  SELECT 'PS-STORY-001' AS sc, 'FAN-FS40-A' AS pc, 380 AS qty, 'PB-FS40-20260705' AS batch_code
  UNION ALL SELECT 'PS-STORY-002', 'FAN-TS30-B', 100, 'PB-TS30-20260710'
  UNION ALL SELECT 'PS-STORY-003', 'FAN-TS30-B', 80, 'PB-TS30-20260710'
) v ON v.sc = s.sales_code
JOIN product p ON p.product_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM wm_product_sales_line l WHERE l.sales_id = s.sales_id AND l.item_code = v.pc);

-- ========== 销售退货 ==========
INSERT INTO wm_rt_sales (rt_code, rt_name, client_name, so_code, rt_date, status, create_by, create_time)
VALUES
  ('RS-STORY-001', '珠三角连锁销售退货', '珠三角家电连锁', 'SO-20260705', DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED', 'warehouse01', NOW()),
  ('RS-STORY-002', '华东连锁待处理退货', '华东家电连锁', 'SO-20260706', NOW(), 'PREPARE', 'warehouse01', NOW())
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name), status = VALUES(status);

INSERT INTO wm_rt_sales_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, quality_status, create_time)
SELECT h.rt_id, p.product_id, p.product_code, p.product_name, '件', v.qty, v.batch_code, v.qs, NOW()
FROM wm_rt_sales h
JOIN (
  SELECT 'RS-STORY-001' AS rc, 'FAN-FS40-A' AS pc, 3 AS qty, 'PB-FS40-20260705' AS batch_code, 'PENDING' AS qs
  UNION ALL SELECT 'RS-STORY-002', 'FAN-TS30-B', 2, 'PB-TS30-20260710', 'PENDING'
) v ON v.rc = h.rt_code
JOIN product p ON p.product_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_sales_line l WHERE l.rt_id = h.rt_id AND l.item_code = v.pc);

-- ========== 供应商退货 ==========
INSERT INTO wm_rt_vendor (rt_code, rt_name, vendor_id, vendor_code, vendor_name, po_code, rt_date, status, create_by, create_time)
SELECT v.rt_code, v.rt_name, mv.vendor_id, mv.vendor_code, mv.vendor_name, v.po_code, v.rt_date, v.status, 'warehouse01', NOW()
FROM (
  SELECT 'RV-STORY-001' AS rt_code, '扇叶来料不良退货' AS rt_name, 'V002' AS vc, 'PO-20260620' AS po_code,
    DATE_SUB(NOW(), INTERVAL 5 DAY) AS rt_date, 'FINISHED' AS status
  UNION ALL SELECT 'RV-STORY-002', '电机批次退货待审', 'V001', 'PO-20260702', NOW(), 'PREPARE'
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
ON DUPLICATE KEY UPDATE rt_name = VALUES(rt_name), status = VALUES(status);

INSERT INTO wm_rt_vendor_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, create_time)
SELECT h.rt_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, '件'), v.qty, v.batch_code, NOW()
FROM wm_rt_vendor h
JOIN (
  SELECT 'RV-STORY-001' AS rc, 'MAT-BLADE-40' AS mc, 15 AS qty, 'BATCH-BLADE-202607' AS batch_code
  UNION ALL SELECT 'RV-STORY-002', 'MAT-MOTOR-55W', 5, 'BATCH-MOTOR-202607'
) v ON v.rc = h.rt_code
JOIN material m ON m.material_code = v.mc
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_vendor_line l WHERE l.rt_id = h.rt_id AND l.item_code = v.mc);

-- ========== 条码 / 装箱 ==========
-- 补齐条码配置（字典类型）
INSERT INTO wm_barcode_config (config_id, barcode_formart, barcode_type, content_formart, content_example, auto_gen_flag, enable_flag, create_time)
SELECT v.config_id, 'QR_CODE', v.barcode_type, v.content_formart, v.content_example, 'Y', 'Y', NOW()
FROM (
  SELECT 3 AS config_id, 'STOCK' AS barcode_type, 'STOCK-{stockCode}' AS content_formart, 'STOCK-STK001' AS content_example
  UNION ALL SELECT 4, 'WORKORDER', 'WORKORDER-{workorderCode}', 'WORKORDER-WO001'
  UNION ALL SELECT 5, 'MACHINERY', 'MACHINERY-{machineryCode}', 'MACHINERY-DV001'
) v
WHERE NOT EXISTS (SELECT 1 FROM wm_barcode_config c WHERE c.barcode_type = v.barcode_type AND c.barcode_formart = 'QR_CODE');

INSERT INTO wm_barcode (barcode_id, barcode_formart, barcode_type, barcode_content, bussiness_id, bussiness_code, bussiness_name, barcode_url, enable_flag, create_time)
SELECT v.barcode_id, 'QR_CODE', v.btype, v.content, v.bid, v.bcode, v.bname,
  CONCAT('https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=', v.content), 'Y', NOW()
FROM (
  SELECT 90001 AS barcode_id, 'MATERIAL_BATCH' AS btype, 'BC-MOTOR-55W-202607' AS content,
    (SELECT material_id FROM material WHERE material_code = 'MAT-MOTOR-55W') AS bid,
    'MAT-MOTOR-55W' AS bcode, '55W风扇电机' AS bname
  UNION ALL SELECT 90002, 'PRODUCT_SN', 'SN-FS40-20260705-001',
    (SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'),
    'FAN-FS40-A', '40cm落地电风扇A型'
  UNION ALL SELECT 90003, 'OUTER_BOX', 'BOX-FS40-20260705-001',
    90003, 'BOX-FS40-20260705-001', 'FS40外箱码'
  UNION ALL SELECT 90004, 'ITEM', 'ITEM-MAT-BLADE-40',
    (SELECT item_id FROM md_item WHERE item_code = 'MAT-BLADE-40' LIMIT 1),
    'MAT-BLADE-40', '40cm扇叶'
  UNION ALL SELECT 90005, 'STOCK', 'STOCK-MAT-MOTOR-55W-1',
    (SELECT material_stock_id FROM wm_material_stock WHERE item_code = 'MAT-MOTOR-55W' LIMIT 1),
    'MAT-MOTOR-55W', '55W风扇电机'
  UNION ALL SELECT 90006, 'WORKORDER', 'WORKORDER-WO-20260701',
    (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260701' LIMIT 1),
    'WO-20260701', '40cm落地电风扇A型生产工单'
  UNION ALL SELECT 90007, 'MACHINERY', 'MACHINERY-DV-M001',
    (SELECT machinery_id FROM dv_machinery WHERE machinery_code = 'DV-M001' LIMIT 1),
    'DV-M001', '电机装配机'
  UNION ALL SELECT 90008, 'ITEM', 'ITEM-FAN-FS40-A',
    (SELECT item_id FROM md_item WHERE item_code = 'FAN-FS40-A' LIMIT 1),
    'FAN-FS40-A', '40cm落地电风扇A型'
) v
WHERE v.bid IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM wm_barcode b WHERE b.barcode_id = v.barcode_id);

UPDATE wm_barcode
SET barcode_url = CONCAT('https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=', barcode_content)
WHERE barcode_content IS NOT NULL AND barcode_content <> '';

INSERT INTO wm_package (package_id, parent_id, ancestors, package_code, barcode_id, barcode_content, package_date, so_code, client_name, status, enable_flag, create_time)
SELECT 90001, 0, '0', 'PKG-STORY-001', 90003, 'BOX-FS40-20260705-001', DATE_SUB(NOW(), INTERVAL 1 DAY),
  'SO-20260705', '珠三角家电连锁', 'FINISHED', 'Y', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wm_package WHERE package_id = 90001);

INSERT INTO wm_package_line (line_id, package_id, item_id, item_code, item_name, specification, unit_of_measure, quantity_package, workorder_code, batch_code, create_time)
SELECT 90001, 90001, p.product_id, p.product_code, p.product_name, p.product_model, 'PCS', 20, 'WO-20260705', 'PB-FS40-20260705', NOW()
FROM product p WHERE p.product_code = 'FAN-FS40-A'
  AND NOT EXISTS (SELECT 1 FROM wm_package_line WHERE line_id = 90001);
