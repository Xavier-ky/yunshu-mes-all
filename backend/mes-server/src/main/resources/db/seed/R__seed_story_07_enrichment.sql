-- Story seed 07: 在 story 01~06 基础上增补 compat WM 链路与追溯深度数据
-- （原生 quality_task / material_issue / finished_inbound 等表已退役，见 V29）
SET NAMES utf8mb4;
USE fan_mes;

-- ========== SN 工序状态 + 扩展追溯（pro_sn_process / compat WM）==========（record_id = 10000 + sn序号*100 + 工序序号）
DELETE FROM pro_sn_process WHERE record_id BETWEEN 10100 AND 10999;

INSERT IGNORE INTO pro_sn_process (record_id, sn_id, sn_code, seq_num, process_code, process_name, workstation_id, user_id, quantity_input, quantity_output, input_time, create_by, create_time)
SELECT 10000 + seq.n * 100 + step.seq_num,
  psn.sn_id, psn.sn_code, step.seq_num, step.process_code, step.process_name,
  ws.station_id, u.user_id, 1, 1,
  DATE_SUB(NOW(), INTERVAL step.days_ago DAY), 'worker01', NOW()
FROM product_sn psn
JOIN (
  SELECT a.d + 1 AS n FROM (
    SELECT 0 d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
  ) a
) seq ON psn.sn_code = CONCAT('SN-FS40-20260705-', LPAD(seq.n, 3, '0'))
CROSS JOIN (
  SELECT 10 AS seq_num, 'STEP-MOTOR' AS process_code, '电机装配' AS process_name, 'ST-05' AS st, 6 AS days_ago
  UNION ALL SELECT 20, 'STEP-BLADE', '扇叶安装', 'ST-05', 5
  UNION ALL SELECT 30, 'STEP-AGING', '老化测试', 'ST-06', 4
  UNION ALL SELECT 40, 'STEP-PACK', '包装入箱', 'ST-06', 3
) step
JOIN workstation ws ON ws.station_code = step.st
CROSS JOIN sys_user u WHERE u.username = 'worker01';

-- 更多关键物料绑定
INSERT IGNORE INTO product_material_binding (sn_id, batch_id, material_id, step_id, bind_user_id, bind_time)
SELECT psn.sn_id, ib.batch_id, m.material_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM product_sn psn
JOIN (
  SELECT 'SN-FS40-20260705-002' AS sn, 'BATCH-MOTOR-202607' AS bn, 'MAT-MOTOR-55W' AS mc
  UNION ALL SELECT 'SN-FS40-20260705-003', 'BATCH-MOTOR-202607', 'MAT-MOTOR-55W'
  UNION ALL SELECT 'SN-FS40-20260705-004', 'BATCH-BLADE-202607', 'MAT-BLADE-40'
  UNION ALL SELECT 'SN-FS40-20260705-005', 'BATCH-BLADE-202607', 'MAT-BLADE-40'
  UNION ALL SELECT 'SN-FS40-20260701-001', 'BATCH-MOTOR-202607', 'MAT-MOTOR-55W'
) v ON v.sn = psn.sn_code
JOIN inventory_batch ib ON ib.batch_no = v.bn
JOIN material m ON m.material_id = ib.material_id AND m.material_code = v.mc;

-- ========== 报工关联物料消耗 / 产品产出 ==========
INSERT INTO wm_item_consume (record_id, workorder_id, workorder_code, workorder_name, workstation_id, workstation_code, workstation_name,
  process_id, process_code, process_name, feedback_id, consume_date, status, remark, create_by, create_time)
SELECT v.rid, wo.work_order_id, wo.work_order_no, p.product_name,
  ws.station_id, ws.station_code, ws.station_name,
  ps.step_id, ps.step_code, ps.step_name,
  pf.record_id, v.cdate, 'FINISHED', v.remark, 'worker01', NOW(3)
FROM (
  SELECT 91001 AS rid, 'FB-STORY-001' AS fb, 'WO-20260701' AS wo, 'ST-01' AS st, 'STEP-MOTOR' AS step,
    DATE_SUB(NOW(), INTERVAL 1 DAY) AS cdate, '电机装配工序物料消耗' AS remark
  UNION ALL SELECT 91002, 'FB-STORY-005', 'WO-20260707', 'ST-05', 'STEP-MOTOR',
    DATE_SUB(NOW(), INTERVAL 2 DAY), 'TS30电机装配消耗'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN workstation ws ON ws.station_code = v.st
JOIN process_step ps ON ps.step_code = v.step
JOIN pro_feedback pf ON pf.feedback_code = v.fb
WHERE NOT EXISTS (SELECT 1 FROM wm_item_consume c WHERE c.record_id = v.rid);

INSERT INTO wm_item_consume_line (line_id, record_id, material_stock_id, item_id, item_code, item_name, unit_name, quantity_consume, batch_id, batch_code, create_by, create_time)
SELECT v.lid, v.rid, s.material_stock_id, m.material_id, m.material_code, m.material_name, IFNULL(u.unit_name,'件'), v.qty, ib.batch_id, ib.batch_no, 'worker01', NOW(3)
FROM (
  SELECT 910001 AS lid, 91001 AS rid, 'MAT-MOTOR-55W' AS mc, 'BATCH-MOTOR-202607' AS bn, 78 AS qty
  UNION ALL SELECT 910002, 91001, 'MAT-SCREW-M4', 'BATCH-SCREW-202607', 624
  UNION ALL SELECT 910003, 91002, 'MAT-MOTOR-40W', 'BATCH-MOTOR40-202607', 93
) v
JOIN material m ON m.material_code = v.mc
JOIN inventory_batch ib ON ib.batch_no = v.bn AND ib.material_id = m.material_id
LEFT JOIN wm_material_stock s ON s.batch_id = ib.batch_id
LEFT JOIN uom u ON u.unit_id = m.unit_id
WHERE NOT EXISTS (SELECT 1 FROM wm_item_consume_line l WHERE l.line_id = v.lid);

INSERT INTO wm_product_produce (record_id, workorder_id, workorder_code, workorder_name, workstation_id, workstation_code, workstation_name,
  process_id, process_code, process_name, feedback_id, produce_date, status, remark, create_by, create_time)
SELECT v.rid, wo.work_order_id, wo.work_order_no, p.product_name,
  ws.station_id, ws.station_code, ws.station_name,
  ps.step_id, ps.step_code, ps.step_name,
  pf.record_id, v.pdate, 'FINISHED', v.remark, 'worker01', NOW(3)
FROM (
  SELECT 92001 AS rid, 'FB-STORY-010' AS fb, 'WO-20260705' AS wo, 'ST-06' AS st, 'STEP-PACK' AS step,
    DATE_SUB(NOW(), INTERVAL 3 DAY) AS pdate, 'FS40包装完工产出' AS remark
  UNION ALL SELECT 92002, 'FB-STORY-012', 'WO-20260710', 'ST-06', 'STEP-PACK',
    DATE_SUB(NOW(), INTERVAL 4 DAY), 'TS30包装完工产出'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN workstation ws ON ws.station_code = v.st
JOIN process_step ps ON ps.step_code = v.step
JOIN pro_feedback pf ON pf.feedback_code = v.fb
WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce pp WHERE pp.record_id = v.rid);

INSERT INTO wm_product_produce_line (line_id, record_id, item_id, item_code, item_name, unit_name, quantity_produce, batch_code, create_by, create_time)
SELECT v.lid, v.rid, mi.item_id, mi.item_code, mi.item_name, '件', v.qty, v.batch, 'worker01', NOW(3)
FROM (
  SELECT 920001 AS lid, 92001 AS rid, 'FAN-FS40-A' AS pc, 200 AS qty, 'PB-FS40-20260705' AS batch
  UNION ALL SELECT 920002, 92002, 'FAN-TS30-B', 120, 'PB-TS30-20260710'
) v
JOIN md_item mi ON mi.item_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce_line l WHERE l.line_id = v.lid);

-- ========== 补充订单 / 壁扇工单 ==========
INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status, created_by)
SELECT 'CO-20260709', '西域电器经销', '2026-07-09', '2026-07-26', 'CONFIRMED',
  (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM customer_order WHERE order_no = 'CO-20260709');

INSERT INTO customer_order_item (order_id, product_id, order_qty, technical_requirement)
SELECT o.order_id, p.product_id, 120.0000, '西域经销壁扇工程订单，灰色标准款'
FROM customer_order o
JOIN product p ON p.product_code = 'FAN-WS20-C'
WHERE o.order_no = 'CO-20260709'
  AND NOT EXISTS (
    SELECT 1 FROM customer_order_item oi WHERE oi.order_id = o.order_id AND oi.product_id = p.product_id
  );

INSERT INTO work_order (
  work_order_no, order_id, order_item_id, product_id, bom_id, route_id, item_id,
  plan_qty, completed_qty, defect_qty, status,
  planned_start_time, planned_end_time, actual_start_time, priority
)
SELECT 'WO-20260711', o.order_id, oi.order_item_id, p.product_id, b.bom_id, pr.route_id, mi.item_id,
  120, 25, 0, 'RUNNING',
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'NORMAL'
FROM customer_order o
JOIN customer_order_item oi ON oi.order_id = o.order_id
JOIN product p ON p.product_id = oi.product_id AND p.product_code = 'FAN-WS20-C'
JOIN bom b ON b.product_id = p.product_id AND b.version_no = 'V1.0'
JOIN process_route pr ON pr.route_code = 'ROUTE-WS20-C' AND pr.version_no = 'V1.0'
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
WHERE o.order_no = 'CO-20260709'
ON DUPLICATE KEY UPDATE plan_qty = VALUES(plan_qty), completed_qty = VALUES(completed_qty), status = VALUES(status);

INSERT INTO production_task (task_no, work_order_id, line_id, shift_id, task_date, task_qty, completed_qty, status, start_time)
SELECT 'PT-20260711', wo.work_order_id,
  (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-03'),
  (SELECT shift_id FROM factory_shift WHERE shift_code = 'DAY'),
  CURDATE(), 120, 25, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM work_order wo WHERE wo.work_order_no = 'WO-20260711'
ON DUPLICATE KEY UPDATE completed_qty = VALUES(completed_qty), status = VALUES(status);

INSERT INTO dispatch_task (dispatch_no, task_id, work_order_id, step_id, station_id, operator_id,
  planned_qty, completed_qty, status, planned_start_time, actual_start_time)
SELECT 'DT-20260716',
  (SELECT task_id FROM production_task WHERE task_no = 'PT-20260711'),
  wo.work_order_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT station_id FROM workstation WHERE station_code = 'ST-07'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  60, 25, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM work_order wo WHERE wo.work_order_no = 'WO-20260711'
ON DUPLICATE KEY UPDATE completed_qty = VALUES(completed_qty), status = VALUES(status);

-- ========== IQC/IPQC 检验行补全 ==========
INSERT INTO qc_iqc_line (iqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT i.iqc_id, ti.index_id, ti.index_code, ti.index_name, ti.index_type, ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure, ti.threshold_max, ti.threshold_min, 'qc01', NOW(3)
FROM qc_iqc i
JOIN qc_template_index ti ON ti.template_id = 2
WHERE i.iqc_code LIKE 'IQC-STORY-%'
  AND NOT EXISTS (SELECT 1 FROM qc_iqc_line l WHERE l.iqc_id = i.iqc_id AND l.index_id = ti.index_id);

INSERT INTO qc_ipqc_line (ipqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT i.ipqc_id, ti.index_id, ti.index_code, ti.index_name, ti.index_type, ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure, ti.threshold_max, ti.threshold_min, 'qc01', NOW(3)
FROM qc_ipqc i
JOIN qc_template_index ti ON ti.template_id = 2
WHERE i.ipqc_code LIKE 'IPQC-STORY-%'
  AND NOT EXISTS (SELECT 1 FROM qc_ipqc_line l WHERE l.ipqc_id = i.ipqc_id AND l.index_id = ti.index_id);

-- ========== 工单展示字段回填：名称 / 订单编号 / 需求日期 ==========
UPDATE work_order wo
JOIN customer_order o ON o.order_no = 'CO-20260704'
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-FS40-A'
LEFT JOIN customer_order_item oi ON oi.order_id = o.order_id AND oi.product_id = p.product_id
SET wo.order_id = o.order_id, wo.order_item_id = oi.order_item_id
WHERE wo.work_order_no = 'WO-20260702' AND wo.order_id IS NULL;

UPDATE work_order wo
JOIN customer_order o ON o.order_no = 'CO-20260702'
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-FS40-A'
LEFT JOIN customer_order_item oi ON oi.order_id = o.order_id AND oi.product_id = p.product_id
SET wo.order_id = o.order_id, wo.order_item_id = oi.order_item_id
WHERE wo.work_order_no = 'WO-20260703' AND wo.order_id IS NULL;

UPDATE work_order wo
LEFT JOIN customer_order co ON co.order_id = wo.order_id
LEFT JOIN product p ON p.product_id = wo.product_id
SET
  wo.work_order_name = CONCAT(IFNULL(p.product_name, wo.work_order_no), '生产工单'),
  wo.source_code = COALESCE(NULLIF(wo.source_code, ''), co.order_no),
  wo.request_date = COALESCE(wo.request_date, co.delivery_date)
WHERE wo.is_deleted = 0
  AND (
    wo.work_order_name IS NULL OR wo.work_order_name = ''
    OR wo.source_code IS NULL OR wo.source_code = ''
    OR wo.request_date IS NULL
  );
