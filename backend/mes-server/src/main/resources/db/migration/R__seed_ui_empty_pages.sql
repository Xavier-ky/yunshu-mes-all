-- 补齐空数据页面演示种子（可重复执行）
-- 注：生产报工/退料检验/系统消息 已由 R__seed_story_04/05/06 接管
SET NAMES utf8mb4;
USE fan_mes;

-- 设备故障原因
INSERT IGNORE INTO fault_cause (fault_cause_id, category_id, cause_code, cause_name, prevent_measure, status)
VALUES
  (9001, 1, 'FC-DEMO-001', '轴承磨损', '定期润滑保养，每班点检', 'ENABLED'),
  (9002, 1, 'FC-DEMO-002', '电机过热', '检查散热风扇与负载电流', 'ENABLED'),
  (9003, 3, 'FC-DEMO-003', '传感器失灵', '校准或更换传感器', 'ENABLED');

-- 操作工派工（我的工位）— 使用 worker01
UPDATE dispatch_task dt
JOIN sys_user u ON u.username = 'worker01'
SET dt.operator_id = u.user_id, dt.status = 'RUNNING'
WHERE dt.dispatch_no IN ('DT-20260701', 'DT-20260702', 'DT-20260709', 'DT-20260710', 'DT-20260711', 'DT-20260712', 'DT-20260713', 'DT-20260714');

-- 兼容 AGENTS.md 别名账号 worker
UPDATE dispatch_task dt
JOIN sys_user u ON u.username = 'worker'
SET dt.operator_id = u.user_id, dt.status = 'RUNNING'
WHERE dt.dispatch_no IN ('DT-20260701', 'DT-20260702')
  AND EXISTS (SELECT 1 FROM sys_user WHERE username = 'worker');
