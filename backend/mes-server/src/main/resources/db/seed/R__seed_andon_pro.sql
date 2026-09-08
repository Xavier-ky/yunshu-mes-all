-- Seed pro_andon_config / pro_andon_record for yunshu-ui Andon center demo
SET NAMES utf8mb4;
USE fan_mes;

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT 'M4螺丝库存不足', 'LEVEL2', r.role_id, r.role_name, NULL, NULL, NULL
FROM sys_role r WHERE r.role_code = 'WAREHOUSE_CLERK'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = 'M4螺丝库存不足');

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT '设备噪音异常', 'LEVEL1', r.role_id, r.role_name, NULL, NULL, NULL
FROM sys_role r WHERE r.role_code = 'EQUIPMENT_MAINTAINER'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = '设备噪音异常');

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT '首件检验不合格', 'LEVEL2', r.role_id, r.role_name, NULL, NULL, NULL
FROM sys_role r WHERE r.role_code = 'QUALITY_INSPECTOR'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = '首件检验不合格');

INSERT INTO pro_andon_config (andon_reason, andon_level, handler_role_id, handler_role_name, handler_user_id, handler_user_name, handler_nick_name)
SELECT '工序节拍异常', 'LEVEL3', r.role_id, r.role_name, u.user_id, u.username, u.real_name
FROM sys_role r, sys_user u
WHERE r.role_code = 'PROD_SUPERVISOR' AND u.username = 'supervisor01'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_config WHERE andon_reason = '工序节拍异常');

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status, create_time
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  u.user_id, u.username, u.real_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  'M4螺丝库存不足', 'LEVEL2', 'ACTIVE', NOW()
FROM workstation ws
CROSS JOIN sys_user u
CROSS JOIN work_order wo
LEFT JOIN product p ON wo.product_id = p.product_id
LEFT JOIN process_step ps ON ps.step_code = 'STEP-MOTOR'
WHERE ws.station_code = 'ST-05' AND u.username = 'worker01' AND wo.work_order_no = 'WO-20260702'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_record WHERE andon_reason = 'M4螺丝库存不足' AND status = 'ACTIVE');

INSERT INTO pro_andon_record (
  workstation_id, workstation_code, workstation_name,
  user_id, user_name, nick_name,
  workorder_id, workorder_code, workorder_name,
  process_id, process_code, process_name,
  andon_reason, andon_level, status, handle_time,
  handler_user_id, handler_user_name, handler_nick_name,
  create_time
)
SELECT
  ws.station_id, ws.station_code, ws.station_name,
  u.user_id, u.username, u.real_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  ps.step_id, ps.step_code, ps.step_name,
  '设备噪音异常', 'LEVEL1', 'HANDLED', NOW(),
  h.user_id, h.username, h.real_name,
  DATE_SUB(NOW(), INTERVAL 2 DAY)
FROM workstation ws
CROSS JOIN sys_user u
CROSS JOIN work_order wo
CROSS JOIN sys_user h
LEFT JOIN product p ON wo.product_id = p.product_id
LEFT JOIN process_step ps ON ps.step_code = 'STEP-AGING'
WHERE ws.station_code = 'ST-07' AND u.username = 'worker01' AND wo.work_order_no = 'WO-20260703' AND h.username = 'repair01'
  AND NOT EXISTS (SELECT 1 FROM pro_andon_record WHERE andon_reason = '设备噪音异常' AND status = 'HANDLED');
