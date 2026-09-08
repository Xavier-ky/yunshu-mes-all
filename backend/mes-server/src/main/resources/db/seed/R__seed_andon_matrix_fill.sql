-- 安灯看板矩阵：第四条产线 + 各线补满 8 工位/行
SET NAMES utf8mb4;
USE fan_mes;

INSERT INTO production_line (workshop_id, line_code, line_name, rated_capacity, capacity_unit)
SELECT w.workshop_id, 'LINE-FAN-04', '电风扇精装配线', 450, 'PCS/DAY'
FROM workshop w
WHERE w.workshop_code = 'WS-FAN-01'
  AND NOT EXISTS (SELECT 1 FROM production_line WHERE line_code = 'LINE-FAN-04');

INSERT INTO workstation (line_id, station_code, station_name, station_type)
SELECT l.line_id, v.station_code, v.station_name, v.station_type
FROM production_line l
JOIN (
  SELECT 'LINE-FAN-01' AS line_code, 'ST-18' AS station_code, '终检贴标工位' AS station_name, 'TEST' AS station_type
  UNION ALL SELECT 'LINE-FAN-02', 'ST-19', '端子压接工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-02', 'ST-20', '运转测试工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-02', 'ST-21', '贴箱标工位', 'PACKAGE'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-22', '外壳安装工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-23', '动平衡复测工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-03', 'ST-24', '下线码垛工位', 'PACKAGE'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-25', '来料核对工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-26', '定子压装工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-27', '转子装配工位', 'ASSEMBLY'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-28', '整机磨合工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-29', '噪音检测工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-30', '外观终检工位', 'TEST'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-31', '装箱工位', 'PACKAGE'
  UNION ALL SELECT 'LINE-FAN-04', 'ST-32', '栈板出库工位', 'PACKAGE'
) v ON v.line_code = l.line_code
WHERE NOT EXISTS (SELECT 1 FROM workstation ws WHERE ws.station_code = v.station_code);
