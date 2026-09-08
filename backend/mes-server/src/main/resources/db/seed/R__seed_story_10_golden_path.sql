-- Story seed 10: 黄金演示链（独立 WO-GP-*，不破坏 story_01–09）
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 订单 ==========
INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status, created_by)
SELECT 'CO-GP-20260714', '黄金演示客户', '2026-07-14', '2026-07-28', 'CONFIRMED',
       (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM customer_order WHERE order_no = 'CO-GP-20260714');

INSERT INTO customer_order_item (order_id, product_id, order_qty, technical_requirement)
SELECT o.order_id, p.product_id, 100.0000, '黄金演示链 FS40-A 100台'
FROM customer_order o
JOIN product p ON p.product_code = 'FAN-FS40-A'
WHERE o.order_no = 'CO-GP-20260714'
  AND NOT EXISTS (
    SELECT 1 FROM customer_order_item oi WHERE oi.order_id = o.order_id AND oi.product_id = p.product_id
  );

-- ========== 黄金工单（lifecycle 驱动演示）==========
INSERT INTO work_order (
  work_order_no, order_id, order_item_id, product_id, bom_id, route_id, item_id,
  plan_qty, completed_qty, defect_qty, status, lifecycle_status,
  planned_start_time, planned_end_time, priority
)
SELECT v.wo_no, o.order_id, oi.order_item_id, p.product_id, b.bom_id, pr.route_id, mi.item_id,
  v.plan_qty, v.completed_qty, 0, v.status, v.lifecycle,
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'NORMAL'
FROM (
  SELECT 'WO-GP-20260714' AS wo_no, 'CO-GP-20260714' AS co, 'FAN-FS40-A' AS pc,
    100 AS plan_qty, 0 AS completed_qty, 'DISPATCHED' AS status, 'RELEASED' AS lifecycle
  UNION ALL SELECT 'WO-GP-A', 'CO-GP-20260714', 'FAN-FS40-A',
    50, 0, 'DISPATCHED', 'KITTING_OK'
  UNION ALL SELECT 'WO-GP-B', 'CO-GP-20260714', 'FAN-FS40-A',
    80, 20, 'RUNNING', 'MATERIAL_ISSUED'
  UNION ALL SELECT 'WO-GP-C', 'CO-GP-20260714', 'FAN-FS40-A',
    60, 55, 'RUNNING', 'QC_PENDING'
  UNION ALL SELECT 'WO-GP-D', 'CO-GP-20260714', 'FAN-FS40-A',
    40, 40, 'RUNNING', 'QC_PASSED'
) v
JOIN customer_order o ON o.order_no = v.co
JOIN product p ON p.product_code = v.pc
JOIN bom b ON b.product_id = p.product_id AND b.version_no = 'V1.0'
JOIN process_route pr ON pr.route_code = 'ROUTE-FS40-A' AND pr.version_no = 'V1.0'
LEFT JOIN customer_order_item oi ON oi.order_id = o.order_id AND oi.product_id = p.product_id
LEFT JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
ON DUPLICATE KEY UPDATE
  lifecycle_status = VALUES(lifecycle_status),
  status = VALUES(status),
  plan_qty = VALUES(plan_qty),
  completed_qty = VALUES(completed_qty);

-- ========== 排产任务 + 派工（B/C 阶段演示）==========
INSERT INTO production_task (
  task_no, task_name, work_order_id, line_id, workstation_id, step_id,
  task_date, task_qty, completed_qty, status, start_time
)
SELECT v.task_no, v.task_name, wo.work_order_id,
  (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'),
  (SELECT station_id FROM workstation WHERE station_code = v.station),
  (SELECT step_id FROM process_step WHERE step_code = v.step_code),
  CURDATE(), v.task_qty, v.completed_qty, v.status, DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM (
  SELECT 'PT-GP-001' AS task_no, 'WO-GP-20260714' AS wo, '电机装配' AS task_name,
    'STEP-MOTOR' AS step_code, 'ST-01' AS station, 100 AS task_qty, 0 AS completed_qty, 'NORMAL' AS status
  UNION ALL SELECT 'PT-GP-B', 'WO-GP-B', 'WO-GP-B装配', 'STEP-MOTOR', 'ST-01', 80, 20, 'RUNNING'
  UNION ALL SELECT 'PT-GP-C', 'WO-GP-C', 'WO-GP-C装配', 'STEP-MOTOR', 'ST-01', 60, 55, 'RUNNING'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE task_qty = VALUES(task_qty), completed_qty = VALUES(completed_qty), status = VALUES(status);

INSERT INTO dispatch_task (
  dispatch_no, task_id, work_order_id, step_id, station_id, operator_id,
  planned_qty, completed_qty, status, planned_start_time
)
SELECT v.dt_no,
  (SELECT task_id FROM production_task WHERE task_no = v.task_no),
  wo.work_order_id,
  (SELECT step_id FROM process_step WHERE step_code = v.step_code),
  (SELECT station_id FROM workstation WHERE station_code = v.station),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  v.planned_qty, v.completed_qty, v.status, DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM (
  SELECT 'DT-GP-001' AS dt_no, 'PT-GP-001' AS task_no, 'WO-GP-20260714' AS wo,
    'STEP-MOTOR' AS step_code, 'ST-01' AS station, 100 AS planned_qty, 0 AS completed_qty, 'CREATED' AS status
  UNION ALL SELECT 'DT-GP-B', 'PT-GP-B', 'WO-GP-B', 'STEP-MOTOR', 'ST-01', 80, 20, 'RUNNING'
  UNION ALL SELECT 'DT-GP-C', 'PT-GP-C', 'WO-GP-C', 'STEP-MOTOR', 'ST-01', 60, 55, 'RUNNING'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE planned_qty = VALUES(planned_qty), completed_qty = VALUES(completed_qty), status = VALUES(status);

-- ========== 领料单（主链 + B 阶段）==========
INSERT INTO wm_issue_header (issue_code, issue_name, workorder_id, workorder_code, issue_date, status, create_time)
SELECT v.issue_code, v.issue_name, wo.work_order_id, wo.work_order_no, NOW(), v.status, NOW()
FROM (
  SELECT 'IS-GP-001' AS issue_code, 'WO-GP-20260714黄金领料' AS issue_name, 'WO-GP-20260714' AS wo, 'APPROVED' AS status
  UNION ALL SELECT 'IS-GP-B', 'WO-GP-B已领料', 'WO-GP-B', 'FINISHED'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE issue_name = VALUES(issue_name), status = VALUES(status);

INSERT INTO wm_issue_line (issue_id, item_id, item_code, item_name, unit_name, quantity_issued, create_time)
SELECT h.issue_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name, '件'), v.qty, NOW()
FROM wm_issue_header h
JOIN (
  SELECT 'IS-GP-001' AS ic, 'MAT-MOTOR-55W' AS mc, 100 AS qty
  UNION ALL SELECT 'IS-GP-001', 'MAT-BLADE-40', 100
  UNION ALL SELECT 'IS-GP-B', 'MAT-MOTOR-55W', 80
) v ON v.ic = h.issue_code
JOIN material m ON m.material_code = v.mc
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_issue_line l WHERE l.issue_id = h.issue_id AND l.item_code = v.mc);

-- 拣货明细（execute 必需：每行一条库存，数量 = 行数量）
DELETE d FROM wm_issue_detail d
JOIN wm_issue_header h ON h.issue_id = d.issue_id
WHERE h.issue_code IN ('IS-GP-001', 'IS-GP-B');

INSERT INTO wm_issue_detail (
  issue_id, line_id, material_stock_id, item_id, item_code, item_name, unit_name, quantity,
  batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name, create_time
)
SELECT h.issue_id, l.line_id, s.material_stock_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_issued,
  s.batch_code, s.warehouse_id, s.warehouse_code, s.warehouse_name,
  s.location_id, s.location_code, s.location_name, s.area_id, s.area_code, s.area_name, NOW()
FROM wm_issue_header h
JOIN wm_issue_line l ON l.issue_id = h.issue_id
JOIN wm_material_stock s ON s.material_stock_id = (
  SELECT MIN(ws.material_stock_id) FROM wm_material_stock ws
  JOIN warehouse w ON w.warehouse_id = ws.warehouse_id AND w.warehouse_code = 'WH-RAW'
  WHERE ws.item_id = l.item_id AND ws.quantity_onhand >= l.quantity_issued
)
WHERE h.issue_code IN ('IS-GP-001', 'IS-GP-B');

-- ========== 成品入库（主链 WO-GP-20260714 + D 阶段快照）==========
INSERT INTO wm_product_recpt (recpt_code, recpt_name, workorder_id, workorder_code, recpt_date, status, create_time)
SELECT v.recpt_code, v.recpt_name, wo.work_order_id, wo.work_order_no, NOW(), v.status, NOW()
FROM (
  SELECT 'PR-GP-001' AS recpt_code, 'WO-GP-20260714成品待入库' AS recpt_name, 'WO-GP-20260714' AS wo, 'PREPARE' AS status
  UNION ALL SELECT 'PR-GP-D', 'WO-GP-D成品待入库', 'WO-GP-D', 'PREPARE'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE recpt_name = VALUES(recpt_name), status = VALUES(status);

INSERT INTO wm_product_recpt_line (recpt_id, item_id, item_code, item_name, unit_name, quantity_recived, batch_code, workorder_code, create_time)
SELECT r.recpt_id, p.product_id, p.product_code, p.product_name, '件', v.qty, v.batch_code, wo.work_order_no, NOW()
FROM wm_product_recpt r
JOIN work_order wo ON wo.work_order_no = r.workorder_code
JOIN product p ON p.product_id = wo.product_id
JOIN (
  SELECT 'PR-GP-001' AS rc, 10 AS qty, 'PB-GP-20260714' AS batch_code
  UNION ALL SELECT 'PR-GP-D', 40, 'PB-GP-D-20260714'
) v ON v.rc = r.recpt_code
WHERE NOT EXISTS (SELECT 1 FROM wm_product_recpt_line l WHERE l.recpt_id = r.recpt_id);

INSERT INTO wm_product_recpt_detail (
  line_id, recpt_id, item_id, item_code, item_name, unit_name, quantity, batch_code,
  warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name, create_time
)
SELECT l.line_id, l.recpt_id, l.item_id, l.item_code, l.item_name, l.unit_name, l.quantity_recived, l.batch_code,
  w.warehouse_id, w.warehouse_code, w.warehouse_name,
  sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name, NOW()
FROM wm_product_recpt_line l
JOIN wm_product_recpt r ON r.recpt_id = l.recpt_id AND r.recpt_code IN ('PR-GP-001', 'PR-GP-D')
JOIN warehouse w ON w.warehouse_code = 'WH-FIN'
JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
JOIN storage_bin sb ON sb.zone_id = sz.zone_id
WHERE r.recpt_code IN ('PR-GP-001', 'PR-GP-D')
  AND NOT EXISTS (SELECT 1 FROM wm_product_recpt_detail d WHERE d.line_id = l.line_id);

-- ========== 报工（C 阶段待检快照）==========
INSERT INTO pro_feedback (
  feedback_type, feedback_code, workstation_id, workorder_id, workorder_code,
  process_id, task_id, item_id, item_code, item_name,
  quantity_qualified, quantity_uncheck, status, user_name, nick_name, feedback_time, create_time
)
SELECT 'SELF', v.fb_code,
  (SELECT station_id FROM workstation WHERE station_code = 'ST-01'),
  wo.work_order_id, wo.work_order_no,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = v.dt_no),
  mi.item_id, mi.item_code, mi.item_name,
  v.qty_ok, v.qty_uncheck, v.status, 'worker01', '产线操作工', NOW(), NOW()
FROM (
  SELECT 'FB-GP-C' AS fb_code, 'WO-GP-C' AS wo, 'DT-GP-C' AS dt_no,
    55 AS qty_ok, 5 AS qty_uncheck, 'FINISHED' AS status
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
WHERE NOT EXISTS (SELECT 1 FROM pro_feedback f WHERE f.feedback_code = v.fb_code);

-- ========== IPQC（C 待检 / D 已通过）==========
INSERT INTO qc_ipqc (
  ipqc_code, ipqc_name, ipqc_type, template_id, workorder_id, workorder_code,
  workstation_id, item_id, item_code, item_name, quantity_check,
  quantity_qualified, quantity_unqualified, check_result, status, create_by, create_time
)
SELECT v.ipqc_code, v.ipqc_name, 'PQC', 2, wo.work_order_id, wo.work_order_no,
  (SELECT station_id FROM workstation WHERE station_code = 'ST-01'),
  mi.item_id, mi.item_code, mi.item_name, v.qty_check,
  v.qty_ok, v.qty_ng, v.check_result, v.status, 'qc01', NOW()
FROM (
  SELECT 'IPQC-GP-C' AS ipqc_code, 'WO-GP-C过程检' AS ipqc_name, 'WO-GP-C' AS wo,
    5 AS qty_check, 0 AS qty_ok, 0 AS qty_ng, NULL AS check_result, 'PREPARE' AS status
  UNION ALL SELECT 'IPQC-GP-D', 'WO-GP-D已检', 'WO-GP-D',
    3, 3, 0, 'ACCEPT', 'FINISHED'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
WHERE NOT EXISTS (SELECT 1 FROM qc_ipqc q WHERE q.ipqc_code = v.ipqc_code);

-- ========== 产品 SN + 物料绑定（追溯 C6）==========
INSERT INTO product_sn (sn_code, product_id, work_order_id, status)
SELECT v.sn_code, wo.product_id, wo.work_order_id, 'IN_PRODUCTION'
FROM (
  SELECT 'SN-GP-20260714-001' AS sn_code, 'WO-GP-20260714' AS wo
  UNION ALL SELECT 'SN-GP-B-001', 'WO-GP-B'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE status = VALUES(status);

INSERT IGNORE INTO product_material_binding (sn_id, batch_id, material_id, step_id, bind_user_id, bind_time)
SELECT psn.sn_id, ib.batch_id, m.material_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  NOW()
FROM product_sn psn
JOIN work_order wo ON wo.work_order_id = psn.work_order_id AND wo.work_order_no = 'WO-GP-20260714'
JOIN inventory_batch ib ON ib.batch_no = 'BATCH-MOTOR-202607'
JOIN material m ON m.material_id = ib.material_id AND m.material_code = 'MAT-MOTOR-55W'
WHERE psn.sn_code = 'SN-GP-20260714-001';

-- WO-GP-B 已领料消耗快照（C4 演示）
INSERT INTO wm_item_consume (workorder_id, workorder_code, consume_date, status, remark, attr1, create_time)
SELECT wo.work_order_id, wo.work_order_no, NOW(), 'FINISHED', 'seed consume IS-GP-B',
  CAST(h.issue_id AS CHAR), NOW()
FROM work_order wo
JOIN wm_issue_header h ON h.workorder_id = wo.work_order_id AND h.issue_code = 'IS-GP-B'
WHERE NOT EXISTS (
  SELECT 1 FROM wm_item_consume c WHERE c.attr1 = CAST(h.issue_id AS CHAR)
);

-- WO-GP-C 报工→production_report 快照（C1 演示）
INSERT INTO production_report (report_no, dispatch_id, work_order_id, step_id, station_id, operator_id,
  report_type, good_qty, defect_qty, report_time, remark)
SELECT 'RPT-FB-GP-C',
  (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-GP-C'),
  wo.work_order_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT station_id FROM workstation WHERE station_code = 'ST-01'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  'NORMAL', 55, 0, NOW(), 'seed from FB-GP-C'
FROM work_order wo
WHERE wo.work_order_no = 'WO-GP-C'
  AND NOT EXISTS (SELECT 1 FROM production_report WHERE report_no = 'RPT-FB-GP-C');

INSERT INTO production_report_detail (report_id, item_type, item_code, item_value)
SELECT pr.report_id, 'PRO_FEEDBACK', 'FB-GP-C',
  CAST(f.record_id AS CHAR)
FROM production_report pr
JOIN pro_feedback f ON f.feedback_code = 'FB-GP-C'
WHERE pr.report_no = 'RPT-FB-GP-C'
  AND NOT EXISTS (
    SELECT 1 FROM production_report_detail d
    WHERE d.report_id = pr.report_id AND d.item_code = 'FB-GP-C'
  );
