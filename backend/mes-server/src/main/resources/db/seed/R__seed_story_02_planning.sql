-- Story seed 02: orders, work orders, tasks, dispatch, kitting (full planning chain)
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 补充订单（含 TS30 / WS20）==========
INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status, created_by)
VALUES
  ('CO-20260706', '华东家电连锁', '2026-07-06', '2026-07-25', 'CONFIRMED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)),
  ('CO-20260707', '南方家电批发', '2026-07-07', '2026-07-28', 'CONFIRMED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)),
  ('CO-20260708', '北方商城集团', '2026-07-08', '2026-07-30', 'CREATED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1))
ON DUPLICATE KEY UPDATE customer_name = VALUES(customer_name), status = VALUES(status);

INSERT INTO customer_order_item (order_id, product_id, order_qty, technical_requirement)
SELECT o.order_id, p.product_id, v.order_qty, v.technical_requirement
FROM (
  SELECT 'CO-20260706' AS order_no, 'FAN-TS30-B' AS pc, 320.0000 AS order_qty, '华东渠道台扇夏季款' AS technical_requirement
  UNION ALL SELECT 'CO-20260707', 'FAN-WS20-C', 200.0000, '南方批发壁扇工程单'
  UNION ALL SELECT 'CO-20260708', 'FAN-TS30-B', 150.0000, '北方商城待确认台扇订单'
  UNION ALL SELECT 'CO-20260703', 'FAN-TS30-B', 120.0000, '北方商城台扇搭售批次'
) v
JOIN customer_order o ON o.order_no = v.order_no
JOIN product p ON p.product_code = v.pc
WHERE NOT EXISTS (
  SELECT 1 FROM customer_order_item oi WHERE oi.order_id = o.order_id AND oi.product_id = p.product_id
);

-- ========== 工单（TS30 / WS20 + 补充 FS40）==========
INSERT INTO work_order (
  work_order_no, order_id, order_item_id, product_id, bom_id, route_id, item_id,
  plan_qty, completed_qty, defect_qty, status,
  planned_start_time, planned_end_time, actual_start_time, actual_end_time, priority
)
SELECT v.wo_no, o.order_id, oi.order_item_id, p.product_id, b.bom_id, pr.route_id, mi.item_id,
  v.plan_qty, v.completed_qty, v.defect_qty, v.status,
  v.planned_start, v.planned_end, v.actual_start, v.actual_end, v.priority
FROM (
  SELECT 'WO-20260707' AS wo_no, 'CO-20260706' AS co, 'FAN-TS30-B' AS pc,
    320 AS plan_qty, 180 AS completed_qty, 2 AS defect_qty, 'RUNNING' AS status,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS planned_start, DATE_ADD(NOW(), INTERVAL 4 DAY) AS planned_end,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS actual_start, NULL AS actual_end, 'NORMAL' AS priority
  UNION ALL SELECT 'WO-20260708', 'CO-20260707', 'FAN-WS20-C',
    200, 60, 1, 'RUNNING',
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, 'NORMAL'
  UNION ALL SELECT 'WO-20260709', 'CO-20260708', 'FAN-TS30-B',
    150, 0, 0, 'CREATED',
    DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY),
    NULL, NULL, 'LOW'
  UNION ALL SELECT 'WO-20260710', 'CO-20260703', 'FAN-TS30-B',
    120, 120, 0, 'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'NORMAL'
) v
LEFT JOIN customer_order o ON o.order_no = v.co
JOIN product p ON p.product_code = v.pc
JOIN bom b ON b.product_id = p.product_id AND b.version_no = 'V1.0'
JOIN process_route pr ON pr.route_code = IF(v.pc = 'FAN-TS30-B', 'ROUTE-TS30-B', 'ROUTE-WS20-C') AND pr.version_no = 'V1.0'
LEFT JOIN customer_order_item oi ON oi.order_id = o.order_id AND oi.product_id = p.product_id
LEFT JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
ON DUPLICATE KEY UPDATE
  plan_qty = VALUES(plan_qty), completed_qty = VALUES(completed_qty),
  defect_qty = VALUES(defect_qty), status = VALUES(status),
  planned_start_time = VALUES(planned_start_time), planned_end_time = VALUES(planned_end_time),
  actual_start_time = VALUES(actual_start_time), actual_end_time = VALUES(actual_end_time);

-- ========== 生产任务 ==========
INSERT INTO production_task (task_no, work_order_id, line_id, shift_id, task_date, task_qty, completed_qty, status, start_time, end_time)
SELECT v.task_no, wo.work_order_id,
  (SELECT line_id FROM production_line WHERE line_code = v.line_code),
  (SELECT shift_id FROM factory_shift WHERE shift_code = 'DAY'),
  v.task_date, v.task_qty, v.completed_qty, v.status, v.start_time, v.end_time
FROM (
  SELECT 'PT-20260707' AS task_no, 'WO-20260707' AS wo, 'LINE-FAN-02' AS line_code,
    CURDATE() AS task_date, 320 AS task_qty, 180 AS completed_qty, 'RUNNING' AS status,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS start_time, NULL AS end_time
  UNION ALL SELECT 'PT-20260708', 'WO-20260708', 'LINE-FAN-03',
    CURDATE(), 200, 60, 'RUNNING', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL
  UNION ALL SELECT 'PT-20260709', 'WO-20260709', 'LINE-FAN-02',
    DATE_ADD(CURDATE(), INTERVAL 2 DAY), 150, 0, 'CREATED', NULL, NULL
  UNION ALL SELECT 'PT-20260710', 'WO-20260710', 'LINE-FAN-02',
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), 120, 120, 'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE
  task_qty = VALUES(task_qty), completed_qty = VALUES(completed_qty),
  status = VALUES(status), start_time = VALUES(start_time), end_time = VALUES(end_time);

-- ========== 派工（甘特时间轴）==========
INSERT INTO dispatch_task (
  dispatch_no, task_id, work_order_id, step_id, station_id, operator_id,
  planned_qty, completed_qty, status,
  planned_start_time, planned_end_time, actual_start_time, actual_end_time
)
SELECT v.dt_no,
  (SELECT task_id FROM production_task WHERE task_no = v.task_no),
  wo.work_order_id,
  (SELECT step_id FROM process_step WHERE step_code = v.step_code),
  (SELECT station_id FROM workstation WHERE station_code = v.station_code),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  v.planned_qty, v.completed_qty, v.status,
  v.planned_start, v.planned_end, v.actual_start, v.actual_end
FROM (
  SELECT 'DT-20260710' AS dt_no, 'PT-20260707' AS task_no, 'WO-20260707' AS wo,
    'STEP-MOTOR' AS step_code, 'ST-05' AS station_code,
    160 AS planned_qty, 95 AS completed_qty, 'RUNNING' AS status,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS planned_start, DATE_ADD(NOW(), INTERVAL 1 DAY) AS planned_end,
    DATE_SUB(NOW(), INTERVAL 2 DAY) AS actual_start, NULL AS actual_end
  UNION ALL SELECT 'DT-20260711', 'PT-20260707', 'WO-20260707',
    'STEP-BLADE', 'ST-05', 160, 85, 'RUNNING',
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY), NULL
  UNION ALL SELECT 'DT-20260712', 'PT-20260708', 'WO-20260708',
    'STEP-MOTOR', 'ST-07', 100, 35, 'RUNNING',
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY), NULL
  UNION ALL SELECT 'DT-20260713', 'PT-20260708', 'WO-20260708',
    'STEP-PACK', 'ST-08', 100, 25, 'RUNNING',
    NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), NOW(), NULL
  UNION ALL SELECT 'DT-20260714', 'PT-20260701', 'WO-20260701',
    'STEP-PACK', 'ST-04', 125, 68, 'RUNNING',
    NOW(), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NULL
  UNION ALL SELECT 'DT-20260715', 'PT-20260710', 'WO-20260710',
    'STEP-PACK', 'ST-06', 120, 120, 'COMPLETED',
    DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY),
    DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)
  UNION ALL SELECT 'DT-20260717', 'PT-20260709', 'WO-20260709',
    'STEP-MOTOR', 'ST-05', 150, 0, 'CREATED',
    '2026-07-17 08:00:00', '2026-07-17 16:00:00', NULL, NULL
  UNION ALL SELECT 'DT-20260718', 'PT-20260709', 'WO-20260709',
    'STEP-BLADE', 'ST-05', 150, 0, 'CREATED',
    '2026-07-18 08:00:00', '2026-07-18 16:00:00', NULL, NULL
  UNION ALL SELECT 'DT-20260719', 'PT-20260709', 'WO-20260709',
    'STEP-PACK', 'ST-06', 150, 0, 'CREATED',
    '2026-07-19 08:00:00', '2026-07-19 16:00:00', NULL, NULL
  UNION ALL SELECT 'DT-20260720', 'PT-20260710', 'WO-20260710',
    'STEP-MOTOR', 'ST-05', 120, 0, 'CREATED',
    '2026-07-20 08:00:00', '2026-07-20 16:00:00', NULL, NULL
  UNION ALL SELECT 'DT-20260721', 'PT-20260711', 'WO-20260711',
    'STEP-PACK', 'ST-08', 60, 0, 'CREATED',
    '2026-07-21 08:00:00', '2026-07-21 16:00:00', NULL, NULL
  UNION ALL SELECT 'DT-20260722', 'PT-20260711', 'WO-20260711',
    'STEP-AGING', 'ST-07', 60, 0, 'CREATED',
    '2026-07-22 08:00:00', '2026-07-22 16:00:00', NULL, NULL
) v
JOIN work_order wo ON wo.work_order_no = v.wo
ON DUPLICATE KEY UPDATE
  planned_qty = VALUES(planned_qty), completed_qty = VALUES(completed_qty), status = VALUES(status),
  planned_start_time = VALUES(planned_start_time), planned_end_time = VALUES(planned_end_time),
  actual_start_time = VALUES(actual_start_time), actual_end_time = VALUES(actual_end_time);

-- worker01 运行中派工（我的工位）
UPDATE dispatch_task dt
JOIN sys_user u ON u.username = 'worker01'
SET dt.operator_id = u.user_id, dt.status = 'RUNNING'
WHERE dt.dispatch_no IN ('DT-20260701','DT-20260702','DT-20260709','DT-20260710','DT-20260711','DT-20260712','DT-20260713','DT-20260714');

-- ========== 齐套分析 ==========
INSERT INTO kitting_analysis (work_order_id, analysis_status, required_summary, available_summary, analyzed_by, analysis_time)
SELECT wo.work_order_id, v.status, v.req_json, v.avail_json,
  (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1), v.analysis_time
FROM (
  SELECT 'WO-20260701' AS wo, 'COMPLETED' AS status,
    '{"MAT-MOTOR-55W":500,"MAT-BLADE-40":500,"MAT-SHELL-FS40":500}' AS req_json,
    '{"MAT-MOTOR-55W":2480,"MAT-BLADE-40":1860,"MAT-SHELL-FS40":1520}' AS avail_json,
    DATE_SUB(NOW(), INTERVAL 3 DAY) AS analysis_time
  UNION ALL SELECT 'WO-20260704', 'PARTIAL',
    '{"MAT-MOTOR-55W":250,"MAT-BLADE-40":250,"MAT-SCREW-M4":2000}',
    '{"MAT-MOTOR-55W":2480,"MAT-BLADE-40":1860,"MAT-SCREW-M4":168}',
    DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'WO-20260707', 'COMPLETED',
    '{"MAT-MOTOR-40W":320,"MAT-BLADE-30":320,"MAT-SHELL-TS30":320}',
    '{"MAT-MOTOR-40W":800,"MAT-BLADE-30":650,"MAT-SHELL-TS30":520}',
    NOW()
) v
JOIN work_order wo ON wo.work_order_no = v.wo
WHERE NOT EXISTS (
  SELECT 1 FROM kitting_analysis ka
  WHERE ka.work_order_id = wo.work_order_id AND ka.analysis_status = v.status
);

INSERT INTO material_shortage (analysis_id, material_id, required_qty, available_qty, shortage_qty, expected_arrival_time, status)
SELECT ka.analysis_id, m.material_id, v.required_qty, v.available_qty, v.shortage_qty, v.eta, v.status
FROM kitting_analysis ka
JOIN work_order wo ON wo.work_order_id = ka.work_order_id AND wo.work_order_no = 'WO-20260704'
JOIN (
  SELECT 'MAT-SCREW-M4' AS mc, 2000.0000 AS required_qty, 168.0000 AS available_qty,
    1832.0000 AS shortage_qty, DATE_ADD(NOW(), INTERVAL 2 DAY) AS eta, 'OPEN' AS status
) v ON 1=1
JOIN material m ON m.material_code = v.mc
WHERE NOT EXISTS (
  SELECT 1 FROM material_shortage ms WHERE ms.analysis_id = ka.analysis_id AND ms.material_id = m.material_id
);

-- ========== 任务操作日志（story）==========
DELETE FROM task_operation_log WHERE remark = 'story planning seed';

INSERT IGNORE INTO task_operation_log (task_id, dispatch_id, operation_type, operator_id, operation_time, remark)
SELECT pt.task_id, dt.dispatch_id, 'START', u.user_id, DATE_SUB(NOW(), INTERVAL 2 DAY), 'story planning seed'
FROM production_task pt
JOIN dispatch_task dt ON dt.task_id = pt.task_id AND dt.dispatch_no = 'DT-20260710'
CROSS JOIN sys_user u WHERE u.username = 'worker01';

INSERT IGNORE INTO task_operation_log (task_id, dispatch_id, operation_type, operator_id, operation_time, remark)
SELECT pt.task_id, dt.dispatch_id, 'START', u.user_id, DATE_SUB(NOW(), INTERVAL 1 DAY), 'story planning seed'
FROM production_task pt
JOIN dispatch_task dt ON dt.task_id = pt.task_id AND dt.dispatch_no = 'DT-20260712'
CROSS JOIN sys_user u WHERE u.username = 'worker01';
