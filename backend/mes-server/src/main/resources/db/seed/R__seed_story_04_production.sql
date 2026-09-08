-- Story seed 04: production feedback, SN, binding, completion, reports
SET NAMES utf8mb4;
USE fan_mes;

-- 清理旧演示报工（硬编码 ID / 错误账号）
DELETE FROM pro_feedback WHERE feedback_code IN ('FB-DEMO-001', 'FB-DEMO-002')
   OR record_id IN (9001, 9002);
DELETE FROM pro_feedback WHERE feedback_code LIKE 'FB-STORY-%';

-- ========== 生产报工（关联真实 WO / 工位 / md_item）==========
INSERT INTO pro_feedback (
  feedback_type, feedback_code, workstation_id, workstation_code, workstation_name,
  workorder_id, workorder_code, workorder_name, route_id, process_id, process_code, process_name,
  task_id, task_code, item_id, item_code, item_name, unit_of_measure,
  quantity, quantity_feedback, quantity_qualified, quantity_unquanlified,
  user_name, nick_name, feedback_time, status, remark, create_by, create_time
)
SELECT v.ftype, v.fcode, ws.station_id, ws.station_code, ws.station_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  IFNULL(pr.route_id, 0), ps.step_id, ps.step_code, ps.step_name,
  pt.task_id, pt.task_no, mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty, v.qty, v.q_qualified, v.q_unqualified,
  u.username, u.real_name, v.fb_time, v.status, v.remark, 'worker01', NOW(3)
FROM (
  SELECT 'SELF' AS ftype, 'FB-STORY-001' AS fcode, 'WO-20260701' AS wo, 'STEP-MOTOR' AS step, 'ST-01' AS st, 'PT-20260701' AS pt,
    80 AS qty, 78 AS q_qualified, 2 AS q_unqualified, DATE_SUB(NOW(), INTERVAL 1 DAY) AS fb_time, 'FINISHED' AS status, '电机装配报工' AS remark
  UNION ALL SELECT 'SELF', 'FB-STORY-002', 'WO-20260701', 'STEP-BLADE', 'ST-02', 'PT-20260701', 72, 72, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED', '扇叶安装报工'
  UNION ALL SELECT 'SELF', 'FB-STORY-003', 'WO-20260701', 'STEP-AGING', 'ST-03', 'PT-20260701', 45, 44, 1, NOW(), 'FINISHED', '老化测试报工'
  UNION ALL SELECT 'SELF', 'FB-STORY-004', 'WO-20260702', 'STEP-MOTOR', 'ST-05', 'PT-20260702', 90, 88, 2, NOW(), 'FINISHED', '二线底座装配'
  UNION ALL SELECT 'SELF', 'FB-STORY-005', 'WO-20260707', 'STEP-MOTOR', 'ST-05', 'PT-20260707', 95, 93, 2, DATE_SUB(NOW(), INTERVAL 2 DAY), 'FINISHED', 'TS30电机装配'
  UNION ALL SELECT 'SELF', 'FB-STORY-006', 'WO-20260707', 'STEP-BLADE', 'ST-05', 'PT-20260707', 85, 84, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 'FINISHED', 'TS30扇叶安装'
  UNION ALL SELECT 'SELF', 'FB-STORY-007', 'WO-20260708', 'STEP-MOTOR', 'ST-07', 'PT-20260708', 35, 34, 1, NOW(), 'FINISHED', 'WS20电机装配'
  UNION ALL SELECT 'SELF', 'FB-STORY-008', 'WO-20260703', 'STEP-PACK', 'ST-08', 'PT-20260703', 45, 45, 0, NOW(), 'PREPARE', '包装工序待确认'
  UNION ALL SELECT 'SELF', 'FB-STORY-009', 'WO-20260705', 'STEP-MOTOR', 'ST-05', 'PT-20260705', 200, 199, 1, DATE_SUB(NOW(), INTERVAL 6 DAY), 'FINISHED', '已完工批次报工'
  UNION ALL SELECT 'SELF', 'FB-STORY-010', 'WO-20260705', 'STEP-PACK', 'ST-06', 'PT-20260705', 200, 200, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), 'FINISHED', '已完工包装报工'
  UNION ALL SELECT 'SELF', 'FB-STORY-011', 'WO-20260701', 'STEP-PACK', 'ST-04', 'PT-20260701', 30, 28, 2, NOW(), 'PREPARE', '包装待检'
  UNION ALL SELECT 'SELF', 'FB-STORY-012', 'WO-20260710', 'STEP-PACK', 'ST-06', 'PT-20260710', 120, 120, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), 'FINISHED', 'TS30完工包装'
  UNION ALL SELECT 'SELF', 'FB-STORY-013', 'WO-20260708', 'STEP-PACK', 'ST-08', 'PT-20260708', 25, 25, 0, NOW(), 'RUNNING', '壁扇包装进行中'
  UNION ALL SELECT 'SELF', 'FB-STORY-014', 'WO-20260702', 'STEP-AGING', 'ST-06', 'PT-20260702', 60, 58, 2, NOW(), 'FINISHED', '整机测试报工'
  UNION ALL SELECT 'SELF', 'FB-STORY-015', 'WO-20260707', 'STEP-AGING', 'ST-06', 'PT-20260707', 40, 39, 1, NOW(), 'PREPARE', 'TS30老化待确认'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
JOIN process_step ps ON ps.step_code = v.step
JOIN workstation ws ON ws.station_code = v.st
LEFT JOIN production_task pt ON pt.task_no = v.pt
LEFT JOIN process_route pr ON pr.route_id = wo.route_id
CROSS JOIN sys_user u WHERE u.username = 'worker01';

-- ========== 产品 SN（WO-20260705 50 台 + WO-20260710 20 台）==========
INSERT IGNORE INTO product_sn (sn_code, product_id, work_order_id, status)
SELECT CONCAT('SN-FS40-20260705-', LPAD(seq.n, 3, '0')), p.product_id, wo.work_order_id, 'COMPLETED'
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-FS40-A'
CROSS JOIN (
  SELECT a.d + b.d * 10 + 1 AS n
  FROM (SELECT 0 d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
  CROSS JOIN (SELECT 0 d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b
) seq
WHERE wo.work_order_no = 'WO-20260705' AND seq.n <= 50;

INSERT IGNORE INTO product_sn (sn_code, product_id, work_order_id, status)
SELECT CONCAT('SN-TS30-20260710-', LPAD(seq.n, 3, '0')), p.product_id, wo.work_order_id, 'COMPLETED'
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-TS30-B'
CROSS JOIN (
  SELECT a.d + 1 AS n
  FROM (SELECT 0 d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
        UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
        UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
        UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19) a
) seq
WHERE wo.work_order_no = 'WO-20260710' AND seq.n <= 20;

-- 运行中工单 SN（WO-20260701 前 10 台）
INSERT IGNORE INTO product_sn (sn_code, product_id, work_order_id, status)
SELECT CONCAT('SN-FS40-20260701-', LPAD(seq.n, 3, '0')), p.product_id, wo.work_order_id, 'IN_PROCESS'
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-FS40-A'
CROSS JOIN (
  SELECT a.d + 1 AS n FROM (SELECT 0 d UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
) seq
WHERE wo.work_order_no = 'WO-20260701' AND seq.n <= 10;

-- ========== 关键物料绑定（SN ↔ 电机批次）==========
INSERT IGNORE INTO product_material_binding (sn_id, batch_id, material_id, step_id, bind_user_id, bind_time)
SELECT psn.sn_id, ib.batch_id, m.material_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  DATE_SUB(NOW(), INTERVAL 5 DAY)
FROM product_sn psn
JOIN work_order wo ON wo.work_order_id = psn.work_order_id AND wo.work_order_no = 'WO-20260705'
JOIN inventory_batch ib ON ib.batch_no = 'BATCH-MOTOR-202607'
JOIN material m ON m.material_id = ib.material_id AND m.material_code = 'MAT-MOTOR-55W'
WHERE psn.sn_code LIKE 'SN-FS40-20260705-%'
  AND RIGHT(psn.sn_code, 3) IN ('001','002','003','004','005','010','020','030','040','050');

-- ========== 报工记录 ==========
INSERT INTO production_report (report_no, dispatch_id, work_order_id, step_id, station_id, operator_id, good_qty, report_time)
SELECT v.rno, dt.dispatch_id, wo.work_order_id, ps.step_id, ws.station_id, u.user_id, v.good_qty, v.rtime
FROM (
  SELECT 'PR-STORY-001' AS rno, 'DT-20260701' AS dt, 'WO-20260701' AS wo, 'STEP-MOTOR' AS step, 'ST-01' AS st, 78 AS good_qty, DATE_SUB(NOW(), INTERVAL 1 DAY) AS rtime
  UNION ALL SELECT 'PR-STORY-002', 'DT-20260707', 'WO-20260705', 'STEP-MOTOR', 'ST-05', 199, DATE_SUB(NOW(), INTERVAL 6 DAY)
  UNION ALL SELECT 'PR-STORY-003', 'DT-20260715', 'WO-20260710', 'STEP-PACK', 'ST-06', 120, DATE_SUB(NOW(), INTERVAL 4 DAY)
) v
JOIN dispatch_task dt ON dt.dispatch_no = v.dt
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN process_step ps ON ps.step_code = v.step
JOIN workstation ws ON ws.station_code = v.st
CROSS JOIN sys_user u WHERE u.username = 'worker01'
ON DUPLICATE KEY UPDATE good_qty = VALUES(good_qty);

-- ========== 安灯历史（已关闭）==========
INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status, handle_time,
  handler_user_id, handler_user_name, handler_nick_name, create_time
)
SELECT ws.station_id, ws.station_code, ws.station_name,
  u.user_id, u.username, u.real_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '首件检验不合格', 'LEVEL2', 'CLOSED', DATE_SUB(NOW(), INTERVAL 3 DAY),
  h.user_id, h.username, h.real_name, DATE_SUB(NOW(), INTERVAL 3 DAY)
FROM workstation ws
CROSS JOIN sys_user u
CROSS JOIN work_order wo
CROSS JOIN sys_user h
JOIN product p ON wo.product_id = p.product_id
JOIN process_step ps ON ps.step_code = 'STEP-AGING'
WHERE ws.station_code = 'ST-03' AND u.username = 'worker01'
  AND wo.work_order_no = 'WO-20260701' AND h.username = 'qc01'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_record WHERE andon_reason = '首件检验不合格' AND status = 'CLOSED' AND workorder_code = 'WO-20260701');

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status, handle_time,
  handler_user_id, handler_user_name, handler_nick_name, create_time
)
SELECT ws.station_id, ws.station_code, ws.station_name,
  u.user_id, u.username, u.real_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '工序节拍异常', 'LEVEL3', 'CLOSED', DATE_SUB(NOW(), INTERVAL 1 DAY),
  h.user_id, h.username, h.real_name, DATE_SUB(NOW(), INTERVAL 1 DAY)
FROM workstation ws
CROSS JOIN sys_user u
CROSS JOIN work_order wo
CROSS JOIN sys_user h
JOIN product p ON wo.product_id = p.product_id
JOIN process_step ps ON ps.step_code = 'STEP-BLADE'
WHERE ws.station_code = 'ST-02' AND u.username = 'worker01'
  AND wo.work_order_no = 'WO-20260701' AND h.username = 'supervisor01'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_record WHERE andon_reason = '工序节拍异常' AND status = 'CLOSED' AND workorder_code = 'WO-20260701');

-- ========== 流转卡 / SN 工序追溯（分析集成页）==========
DELETE FROM pro_card_process WHERE card_id IN (1, 2);
DELETE FROM pro_card WHERE card_id IN (1, 2);

INSERT IGNORE INTO pro_card (card_id, card_code, workorder_id, workorder_code, workorder_name, item_code, item_name, unit_of_measure, quantity_transfered, status, create_by, create_time)
SELECT 1, 'CARD-STORY-001', wo.work_order_id, wo.work_order_no, p.product_name,
       p.product_code, p.product_name, 'PCS', IFNULL(wo.completed_qty, 0), 'PROCESSING', 'admin', NOW()
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id
WHERE wo.work_order_no = 'WO-20260701';

INSERT IGNORE INTO pro_card (card_id, card_code, workorder_id, workorder_code, workorder_name, item_code, item_name, unit_of_measure, quantity_transfered, status, create_by, create_time)
SELECT 2, 'CARD-STORY-002', wo.work_order_id, wo.work_order_no, p.product_name,
       p.product_code, p.product_name, 'PCS', IFNULL(wo.completed_qty, 0), 'FINISHED', 'admin', NOW()
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id
WHERE wo.work_order_no = 'WO-20260705';

INSERT IGNORE INTO pro_card_process (record_id, card_id, card_code, seq_num, process_code, process_name, workstation_id, user_id, quantity_input, quantity_output, input_time, create_by, create_time)
SELECT 1, 1, 'CARD-STORY-001', 10, ps.step_code, ps.step_name, ws.station_id, u.user_id, 100, 98, NOW(), 'admin', NOW()
FROM process_step ps
JOIN workstation ws ON ws.station_code = 'ST-01'
CROSS JOIN sys_user u WHERE u.username = 'worker01' AND ps.step_code = 'STEP-MOTOR';

DELETE FROM pro_sn_process WHERE record_id IN (1, 2);
INSERT IGNORE INTO pro_sn_process (record_id, sn_id, sn_code, seq_num, process_code, process_name, workstation_id, user_id, quantity_input, quantity_output, input_time, create_by, create_time)
SELECT 1, psn.sn_id, psn.sn_code, 10, 'STEP-MOTOR', '电机装配', ws.station_id, u.user_id, 1, 1, NOW(), 'admin', NOW()
FROM product_sn psn
JOIN workstation ws ON ws.station_code = 'ST-01'
CROSS JOIN sys_user u WHERE u.username = 'worker01' AND psn.sn_code = 'SN-FS40-20260705-001';

INSERT IGNORE INTO pro_sn_process (record_id, sn_id, sn_code, seq_num, process_code, process_name, workstation_id, user_id, quantity_input, quantity_output, input_time, create_by, create_time)
SELECT 2, psn.sn_id, psn.sn_code, 40, 'STEP-PACK', '包装入箱', ws.station_id, u.user_id, 1, 1, NOW(), 'admin', NOW()
FROM product_sn psn
JOIN workstation ws ON ws.station_code = 'ST-06'
CROSS JOIN sys_user u WHERE u.username = 'worker01' AND psn.sn_code = 'SN-FS40-20260705-001';
