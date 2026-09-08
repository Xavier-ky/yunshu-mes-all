-- 安灯看板 + 发起页演示数据（可重复执行，按原因+工位+状态去重）
SET NAMES utf8mb4;
USE fan_mes;

-- 修正/补齐呼叫配置（四类原因：物料/设备/质量/工艺）
UPDATE pro_andon_config SET andon_reason = 'M4螺丝库存不足', andon_level = 'LEVEL2',
  handler_role_id = 3, handler_role_name = '仓库物料员'
WHERE config_id = 1 OR andon_reason LIKE 'M4%' LIMIT 1;

UPDATE pro_andon_config SET andon_reason = '设备噪音异常', andon_level = 'LEVEL1',
  handler_role_id = 6, handler_role_name = '设备维修员'
WHERE config_id = 2 OR andon_reason LIKE '设备%' LIMIT 1;

UPDATE pro_andon_config SET andon_reason = '首件检验不合格', andon_level = 'LEVEL2',
  handler_role_id = 5, handler_role_name = '质检员'
WHERE config_id = 3 OR andon_reason LIKE '首件%' LIMIT 1;

UPDATE pro_andon_config SET andon_reason = '工序节拍异常', andon_level = 'LEVEL3',
  handler_role_id = 2, handler_role_name = '生产主管',
  handler_user_id = (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1),
  handler_user_name = 'supervisor01',
  handler_nick_name = '生产主管'
WHERE config_id = 4 OR andon_reason LIKE '工序%' LIMIT 1;

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT '包装箱缺货', 'LEVEL2', 3, '仓库物料员', 15, 'warehouse01', '仓库物料员'
WHERE NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = '包装箱缺货');

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT '老化架温度偏高', 'LEVEL1', 6, '设备维修员', 17, 'repair01', '设备维修员'
WHERE NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = '老化架温度偏高');

-- ========== 待处置 ACTIVE（看板告警条 / 产线矩阵 / 分类卡片）==========
INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  'M4螺丝库存不足', 'LEVEL2', 'ACTIVE',
  15, 'warehouse01', '仓库物料员',
  DATE_SUB(NOW(), INTERVAL 38 MINUTE), '台扇二线底座工位缺料，已停线等待'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260707'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-MOTOR'
WHERE ws.station_code = 'ST-05'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-05' AND r.andon_reason = 'M4螺丝库存不足' AND r.status = 'ACTIVE'
  );

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '设备噪音异常', 'LEVEL1', 'ACTIVE',
  17, 'repair01', '设备维修员',
  DATE_SUB(NOW(), INTERVAL 8 MINUTE), '壁扇线电机工位异响，需机修现场确认'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260708'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-MOTOR'
WHERE ws.station_code = 'ST-07'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-07' AND r.andon_reason = '设备噪音异常' AND r.status = 'ACTIVE'
  );

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '首件检验不合格', 'LEVEL2', 'ACTIVE',
  16, 'qc01', '质检员',
  DATE_SUB(NOW(), INTERVAL 22 MINUTE), '老化测试首件尺寸超差，待复检'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260701'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-AGING'
WHERE ws.station_code = 'ST-03'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-03' AND r.andon_reason = '首件检验不合格' AND r.status = 'ACTIVE'
  );

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '工序节拍异常', 'LEVEL3', 'ACTIVE',
  14, 'supervisor01', '生产主管',
  DATE_SUB(NOW(), INTERVAL 12 MINUTE), '包装工位节拍低于标准，需班长协调人手'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260701'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-PACK'
WHERE ws.station_code = 'ST-04'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-04' AND r.andon_reason = '工序节拍异常' AND r.status = 'ACTIVE'
  );

-- ========== 本班已处置（趋势图 / KPI / 活动流）==========
INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, handle_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '包装箱缺货', 'LEVEL2', 'HANDLED',
  15, 'warehouse01', '仓库物料员',
  DATE_ADD(CURDATE(), INTERVAL 7 HOUR), DATE_ADD(CURDATE(), INTERVAL 8 HOUR), '已补货恢复包装'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260708'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-PACK'
WHERE ws.station_code = 'ST-08'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-08' AND r.andon_reason = '包装箱缺货' AND r.status = 'HANDLED'
      AND r.handle_time >= CURDATE()
  );

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, handle_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '老化架温度偏高', 'LEVEL1', 'HANDLED',
  17, 'repair01', '设备维修员',
  DATE_ADD(CURDATE(), INTERVAL 5 HOUR), DATE_ADD(CURDATE(), INTERVAL 6 HOUR), '调整温控参数后恢复正常'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260707'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-AGING'
WHERE ws.station_code = 'ST-06'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-06' AND r.andon_reason = '老化架温度偏高' AND r.status = 'HANDLED'
      AND r.handle_time >= CURDATE()
  );

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, handle_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  'M4螺丝库存不足', 'LEVEL2', 'HANDLED',
  15, 'warehouse01', '仓库物料员',
  DATE_ADD(CURDATE(), INTERVAL 9 HOUR), DATE_ADD(CURDATE(), INTERVAL 10 HOUR), '早班缺料已配送'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260702'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-MOTOR'
WHERE ws.station_code = 'ST-05'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-05' AND r.andon_reason = 'M4螺丝库存不足' AND r.status = 'HANDLED'
      AND r.handle_time >= CURDATE() AND r.remark = '早班缺料已配送'
  );

-- 本班刚关闭一条（活动流动态）
INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time, handle_time, remark
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  18, 'worker01', '产线操作工人',
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '包装箱缺货', 'LEVEL2', 'HANDLED',
  15, 'warehouse01', '仓库物料员',
  DATE_SUB(NOW(), INTERVAL 55 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), '刚完成补货闭环'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260703'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-PACK'
WHERE ws.station_code = 'ST-08'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_record r WHERE r.remark = '刚完成补货闭环');

-- 清理重复 ACTIVE（重复执行 seed 时）
DELETE r1 FROM pro_andon_record r1
INNER JOIN pro_andon_record r2
  ON r1.workstation_code = r2.workstation_code
  AND r1.andon_reason = r2.andon_reason
  AND r1.status = 'ACTIVE' AND r2.status = 'ACTIVE'
  AND r1.record_id > r2.record_id;
