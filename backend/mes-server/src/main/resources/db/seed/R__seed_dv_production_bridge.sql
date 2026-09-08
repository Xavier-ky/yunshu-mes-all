-- UTF-8 bridge: workstation names + extra machine bindings for production integration
-- Import: cmd /c "chcp 65001>nul && mysql -uroot -p051002sry --default-character-set=utf8mb4 fan_mes < R__seed_dv_production_bridge.sql"

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

USE fan_mes;

-- Fix garbled workstation display names on LINE-FAN-01
UPDATE workstation SET station_name = '电机装配工位' WHERE station_code = 'ST-01';
UPDATE workstation SET station_name = '扇叶安装工位' WHERE station_code = 'ST-02';
UPDATE workstation SET station_name = '老化测试工位' WHERE station_code = 'ST-03';
UPDATE workstation SET station_name = '包装工位' WHERE station_code = 'ST-04';
UPDATE workstation SET station_name = '螺丝上料工位' WHERE station_code = 'ST-05';
UPDATE workstation SET station_name = '动平衡工位' WHERE station_code = 'ST-06';
UPDATE workstation SET station_name = '终检工位' WHERE station_code = 'ST-07';

-- Extra bindings: screw feeder + wind tester (main production chain)
DELETE FROM md_workstation_machine WHERE record_id BETWEEN 9006 AND 9010;

INSERT INTO md_workstation_machine (
  record_id, workstation_id, machinery_id, machinery_code, machinery_name, quantity, remark, create_by, create_time
)
SELECT 9006, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 螺丝供料', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M006'
WHERE ws.station_code = 'ST-05'
UNION ALL
SELECT 9007, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 风速测试', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M007'
WHERE ws.station_code = 'ST-07'
UNION ALL
SELECT 9008, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 产线空压', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M005'
WHERE ws.station_code = 'ST-01'
UNION ALL
SELECT 9009, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 立柱旋紧', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M012'
WHERE ws.station_code = 'ST-06'
UNION ALL
SELECT 9010, ws.station_id, m.machinery_id, m.machinery_code, m.machinery_name, 1, 'demo: 冷却水机', 'seed', NOW(3)
FROM workstation ws
JOIN dv_machinery m ON m.machinery_code = 'DV-M017'
WHERE ws.station_code = 'ST-14';

-- Ensure equipment andon config points to repair01
UPDATE pro_andon_config
SET handler_role_id = (SELECT role_id FROM sys_role WHERE role_code = 'EQUIPMENT_MAINTAINER' LIMIT 1),
    handler_role_name = '设备维修员',
    handler_user_id = (SELECT user_id FROM sys_user WHERE username = 'repair01' LIMIT 1),
    handler_user_name = 'repair01',
    handler_nick_name = (SELECT real_name FROM sys_user WHERE username = 'repair01' LIMIT 1)
WHERE andon_reason = '设备噪音异常';
