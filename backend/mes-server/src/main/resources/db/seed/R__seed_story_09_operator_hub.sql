-- Story seed 09: 我的工位指挥台演示数据（今日报工、派工进度、SN 绑定）
SET NAMES utf8mb4;
USE fan_mes;

-- 清理旧演示报工
DELETE FROM pro_feedback WHERE feedback_code LIKE 'FB-OPHUB-%';

-- ========== 今日报工（worker01 / worker 别名均可命中）==========
INSERT INTO pro_feedback (
  feedback_type, feedback_code, workstation_id, workstation_code, workstation_name,
  workorder_id, workorder_code, workorder_name, route_id, process_id, process_code, process_name,
  task_id, task_code, item_id, item_code, item_name, unit_of_measure,
  quantity, quantity_feedback, quantity_qualified, quantity_unquanlified,
  user_name, nick_name, feedback_time, status, remark, create_by, create_time
)
SELECT v.ftype, v.fcode, dt.station_id, ws.station_code, ws.station_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  IFNULL(pr.route_id, 0), ps.step_id, ps.step_code, ps.step_name,
  dt.dispatch_id, dt.dispatch_no, mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty, v.qty, v.q_qualified, v.q_unqualified,
  v.uname, u.real_name, v.fb_time, 'FINISHED', v.remark, 'worker01', NOW(3)
FROM (
  SELECT 'SELF' AS ftype, 'FB-OPHUB-001' AS fcode, 'DT-20260701' AS dt, 'worker01' AS uname,
    12 AS qty, 12 AS q_qualified, 0 AS q_unqualified,
    CONCAT(CURDATE(), ' 08:35:00') AS fb_time, '电机装配首件报工' AS remark
  UNION ALL SELECT 'SELF', 'FB-OPHUB-002', 'DT-20260702', 'worker01',
    15, 14, 1, CONCAT(CURDATE(), ' 09:20:00'), '扇叶安装报工'
  UNION ALL SELECT 'SELF', 'FB-OPHUB-003', 'DT-20260710', 'worker01',
    18, 18, 0, CONCAT(CURDATE(), ' 10:05:00'), 'TS30 电机装配报工'
  UNION ALL SELECT 'SELF', 'FB-OPHUB-004', 'DT-20260714', 'worker',
    10, 10, 0, CONCAT(CURDATE(), ' 11:30:00'), '包装工序报工'
  UNION ALL SELECT 'SELF', 'FB-OPHUB-005', 'DT-20260711', 'worker',
    8, 7, 1, CONCAT(CURDATE(), ' 14:15:00'), '扇叶安装二次报工'
) v
JOIN dispatch_task dt ON dt.dispatch_no = v.dt
JOIN work_order wo ON wo.work_order_id = dt.work_order_id
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
JOIN process_step ps ON ps.step_id = dt.step_id
JOIN workstation ws ON ws.station_id = dt.station_id
LEFT JOIN process_route pr ON pr.route_id = wo.route_id
JOIN sys_user u ON u.username = v.uname;

-- ========== 派工进度差异化（进度条可见）==========
UPDATE dispatch_task SET completed_qty = 80, planned_qty = 125, status = 'RUNNING'
WHERE dispatch_no = 'DT-20260701';

UPDATE dispatch_task SET completed_qty = 45, planned_qty = 125, status = 'RUNNING'
WHERE dispatch_no = 'DT-20260703';

UPDATE dispatch_task SET completed_qty = 68, planned_qty = 125, status = 'RUNNING'
WHERE dispatch_no = 'DT-20260714';

UPDATE dispatch_task SET completed_qty = 95, planned_qty = 160, status = 'RUNNING'
WHERE dispatch_no = 'DT-20260710';

UPDATE dispatch_task SET completed_qty = 85, planned_qty = 160, status = 'RUNNING'
WHERE dispatch_no = 'DT-20260711';

-- ========== WO-20260701 SN 物料绑定（最近绑定 Tab）==========
INSERT IGNORE INTO product_material_binding (sn_id, batch_id, material_id, step_id, bind_user_id, bind_time)
SELECT psn.sn_id, ib.batch_id, m.material_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  CONCAT(CURDATE(), ' 08:10:00')
FROM product_sn psn
JOIN work_order wo ON wo.work_order_id = psn.work_order_id AND wo.work_order_no = 'WO-20260701'
JOIN inventory_batch ib ON ib.batch_no = 'BATCH-MOTOR-202607'
JOIN material m ON m.material_id = ib.material_id AND m.material_code = 'MAT-MOTOR-55W'
WHERE psn.sn_code = 'SN-FS40-20260701-001';

INSERT IGNORE INTO product_material_binding (sn_id, batch_id, material_id, step_id, bind_user_id, bind_time)
SELECT psn.sn_id, ib.batch_id, m.material_id,
  (SELECT step_id FROM process_step WHERE step_code = 'STEP-BLADE'),
  (SELECT user_id FROM sys_user WHERE username = 'worker01'),
  CONCAT(CURDATE(), ' 09:45:00')
FROM product_sn psn
JOIN work_order wo ON wo.work_order_id = psn.work_order_id AND wo.work_order_no = 'WO-20260701'
JOIN inventory_batch ib ON ib.batch_no = 'BATCH-BLADE-202607'
JOIN material m ON m.material_id = ib.material_id AND m.material_code = 'MAT-BLADE-40'
WHERE psn.sn_code = 'SN-FS40-20260701-001'
  AND NOT EXISTS (
    SELECT 1 FROM product_material_binding pmb
    JOIN material m2 ON m2.material_id = pmb.material_id
    WHERE pmb.sn_id = psn.sn_id AND m2.material_code = 'MAT-BLADE-40'
  );
