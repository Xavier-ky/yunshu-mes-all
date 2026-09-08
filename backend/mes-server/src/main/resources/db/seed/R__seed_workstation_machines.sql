-- Bind demo dv_machinery to LINE-FAN-01 workstations (md_workstation_machine)
-- File encoding: UTF-8 (no BOM)

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE fan_mes;

DELETE FROM md_workstation_machine WHERE record_id BETWEEN 9001 AND 9005;

INSERT INTO md_workstation_machine (
  record_id, workstation_id, machinery_id, machinery_code, machinery_name, quantity, remark, create_by, create_time
)
SELECT 9001, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 电机装配', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M001'
WHERE ws.station_code = 'ST-01'
UNION ALL
SELECT 9002, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 扇叶安装', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M002'
WHERE ws.station_code = 'ST-02'
UNION ALL
SELECT 9003, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 老化测试', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M003'
WHERE ws.station_code = 'ST-03'
UNION ALL
SELECT 9004, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 包装', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M004'
WHERE ws.station_code = 'ST-04'
UNION ALL
SELECT 9005, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 线体传送', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M008'
WHERE ws.station_code = 'ST-01';
