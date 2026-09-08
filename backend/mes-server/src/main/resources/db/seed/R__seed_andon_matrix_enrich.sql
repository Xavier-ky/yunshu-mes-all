-- 安灯看板产线工位矩阵：补充工位与适量 ACTIVE，使矩阵更饱满
SET NAMES utf8mb4;
USE fan_mes;

INSERT INTO workstation (line_id, station_code, station_name, station_type)
SELECT l.line_id, v.station_code, v.station_name, v.station_type
FROM production_line l
JOIN (
  SELECT 'LINE-FAN-01' AS line_code, 'ST-09' AS station_code, '前加工工位' AS station_name, 'ASSEMBLY' AS station_type
  UNION ALL SELECT 'LINE-FAN-01', 'ST-10', '线束绑扎工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-01', 'ST-11', '外观检工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-02', 'ST-12', '底座锁附工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-02', 'ST-13', '整机复检工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-02', 'ST-14', '扫码入库工位', 'PACKAGE'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-15', '电机预装工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-16', '扇叶平衡工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-17', '成品复核工位', 'TEST'
) v ON v.line_code = l.line_code
WHERE NOT EXISTS (SELECT 1 FROM workstation ws WHERE ws.station_code = v.station_code);

-- 线1 扇叶工位：设备类告警（适量）
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
  DATE_SUB(NOW(), INTERVAL 18 MINUTE), '扇叶安装工位伺服异响'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260701'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-BLADE'
WHERE ws.station_code = 'ST-02'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-02' AND r.andon_reason = '设备噪音异常' AND r.status = 'ACTIVE'
  );

-- 线2 新增工位：物料类告警
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
  '包装箱缺货', 'LEVEL2', 'ACTIVE',
  15, 'warehouse01', '仓库物料员',
  DATE_SUB(NOW(), INTERVAL 26 MINUTE), '扫码入库工位外箱库存不足'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260707'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-PACK'
WHERE ws.station_code = 'ST-14'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-14' AND r.andon_reason = '包装箱缺货' AND r.status = 'ACTIVE'
  );

-- 线1 外观检：质量类（15min+ 档）
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
  DATE_SUB(NOW(), INTERVAL 16 MINUTE), '外观检发现面板划伤'
FROM workstation ws
JOIN work_order wo ON wo.work_order_no = 'WO-20260701'
JOIN product p ON p.product_id = wo.product_id
JOIN process_step ps ON ps.step_code = 'STEP-AGING'
WHERE ws.station_code = 'ST-11'
  AND NOT EXISTS (
    SELECT 1 FROM pro_andon_record r
    WHERE r.workstation_code = 'ST-11' AND r.andon_reason = '首件检验不合格' AND r.status = 'ACTIVE'
  );

DELETE r1 FROM pro_andon_record r1
INNER JOIN pro_andon_record r2
  ON r1.workstation_code = r2.workstation_code
  AND r1.andon_reason = r2.andon_reason
  AND r1.status = 'ACTIVE' AND r2.status = 'ACTIVE'
  AND r1.record_id > r2.record_id;
