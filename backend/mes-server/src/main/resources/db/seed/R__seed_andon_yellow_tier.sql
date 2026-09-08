-- 安灯矩阵：刷新 15–29 分钟黄色预警档演示数据（可重复执行）
SET NAMES utf8mb4;
USE fan_mes;

UPDATE pro_andon_record r
INNER JOIN workstation w ON w.station_id = r.workstation_id
SET r.create_time = DATE_SUB(NOW(), INTERVAL 22 MINUTE)
WHERE r.status = 'ACTIVE'
  AND w.station_code IN ('ST-02', 'ST-11');

UPDATE pro_andon_record r
INNER JOIN workstation w ON w.station_id = r.workstation_id
SET r.create_time = DATE_SUB(NOW(), INTERVAL 26 MINUTE)
WHERE r.status = 'ACTIVE'
  AND w.station_code = 'ST-14';

UPDATE pro_andon_record r
INNER JOIN workstation w ON w.station_id = r.workstation_id
SET r.create_time = DATE_SUB(NOW(), INTERVAL 8 MINUTE)
WHERE r.status = 'ACTIVE'
  AND w.station_code = 'ST-17';

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
  '辅料配送延迟', 'LEVEL2', 'ACTIVE',
  15, 'warehouse01', '仓库物料员',
  DATE_SUB(NOW(), INTERVAL 8 MINUTE), 'SLA正常档演示'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260707'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-PACK'
WHERE ws.station_code = 'ST-17'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-17' AND r.status = 'ACTIVE'
  );

-- 落地扇线仅保留一条 20 分钟预警，便于整行显示黄色
UPDATE pro_andon_record r
INNER JOIN workstation w ON w.station_id = r.workstation_id
INNER JOIN production_line l ON l.line_id = w.line_id
SET r.status = 'HANDLED', r.handle_time = NOW(), r.remark = CONCAT(COALESCE(r.remark, ''), ' [seed:yellow-tier-demo]')
WHERE r.status = 'ACTIVE'
  AND l.line_code = 'LINE-FAN-04'
  AND w.station_code <> 'ST-18';

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
  '工装夹具未到位', 'LEVEL2', 'ACTIVE',
  17, 'repair01', '设备维修员',
  DATE_SUB(NOW(), INTERVAL 20 MINUTE), '落地扇线黄色预警演示'
FROM workstation ws
JOIN production_line l ON l.line_id = ws.line_id AND l.line_code = 'LINE-FAN-04'
JOIN work_order wo ON wo.work_order_no = 'WO-20260707'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-ASSY'
WHERE ws.station_code = 'ST-18'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-18' AND r.status = 'ACTIVE'
  );
