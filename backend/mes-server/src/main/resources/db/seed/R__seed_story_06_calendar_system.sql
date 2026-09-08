-- Story seed 06: calendar holidays, team members, account aliases, messages, login logs
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 2026 节假日 / 调休 ==========
INSERT INTO cal_holiday (the_day, holiday_type, remark, create_by, create_time)
VALUES
  ('2026-01-01', 'HOLIDAY', '元旦', 'admin', NOW(3)),
  ('2026-01-02', 'HOLIDAY', '元旦假期', 'admin', NOW(3)),
  ('2026-01-28', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-01-29', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-01-30', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-01-31', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-02-01', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-02-02', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-02-03', 'HOLIDAY', '春节', 'admin', NOW(3)),
  ('2026-04-04', 'HOLIDAY', '清明节', 'admin', NOW(3)),
  ('2026-04-05', 'HOLIDAY', '清明节', 'admin', NOW(3)),
  ('2026-04-06', 'HOLIDAY', '清明节', 'admin', NOW(3)),
  ('2026-05-01', 'HOLIDAY', '劳动节', 'admin', NOW(3)),
  ('2026-05-02', 'HOLIDAY', '劳动节', 'admin', NOW(3)),
  ('2026-05-03', 'HOLIDAY', '劳动节', 'admin', NOW(3)),
  ('2026-05-04', 'HOLIDAY', '劳动节', 'admin', NOW(3)),
  ('2026-05-05', 'HOLIDAY', '劳动节', 'admin', NOW(3)),
  ('2026-06-08', 'HOLIDAY', '端午节', 'admin', NOW(3)),
  ('2026-06-09', 'HOLIDAY', '端午节', 'admin', NOW(3)),
  ('2026-06-10', 'HOLIDAY', '端午节', 'admin', NOW(3)),
  ('2026-09-15', 'HOLIDAY', '中秋节', 'admin', NOW(3)),
  ('2026-09-16', 'HOLIDAY', '中秋节', 'admin', NOW(3)),
  ('2026-09-17', 'HOLIDAY', '中秋节', 'admin', NOW(3)),
  ('2026-10-01', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-02', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-03', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-04', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-05', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-06', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-10-07', 'HOLIDAY', '国庆节', 'admin', NOW(3)),
  ('2026-02-08', 'WORKDAY', '春节调休上班', 'admin', NOW(3)),
  ('2026-09-20', 'WORKDAY', '中秋调休上班', 'admin', NOW(3)),
  ('2026-10-10', 'WORKDAY', '国庆调休上班', 'admin', NOW(3))
ON DUPLICATE KEY UPDATE holiday_type = VALUES(holiday_type), remark = VALUES(remark);

-- ========== 排班日历字典 ==========
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT v.dict_name, v.dict_type, '0', 'admin', NOW(), '排班日历旗舰页'
FROM (
  SELECT '日历类型' AS dict_name, 'mes_calendar_type' AS dict_type
  UNION ALL SELECT '班制类型', 'mes_shift_type'
  UNION ALL SELECT '轮班方式', 'mes_shift_method'
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type d WHERE d.dict_type = v.dict_type);

INSERT INTO sys_dict_data (
  dict_sort, dict_label, dict_value, dict_type, list_class,
  is_default, status, create_by, create_time, remark
)
SELECT v.dict_sort, v.dict_label, v.dict_value, v.dict_type, v.list_class,
       v.is_default, '0', 'admin', NOW(), '排班日历旗舰页'
FROM (
  SELECT 1 AS dict_sort, '组装' AS dict_label, 'ZZ' AS dict_value,
         'mes_calendar_type' AS dict_type, 'primary' AS list_class, 'Y' AS is_default
  UNION ALL SELECT 2, '注塑', 'ZS', 'mes_calendar_type', 'success', 'N'
  UNION ALL SELECT 3, '机加工', 'CNC', 'mes_calendar_type', 'warning', 'N'
  UNION ALL SELECT 4, '仓库', 'CK', 'mes_calendar_type', 'info', 'N'
  UNION ALL SELECT 1, '单白班', 'SINGLE', 'mes_shift_type', 'primary', 'Y'
  UNION ALL SELECT 2, '两班倒', 'SHIFT_TWO', 'mes_shift_type', 'success', 'N'
  UNION ALL SELECT 3, '三班倒', 'SHIFT_THREE', 'mes_shift_type', 'warning', 'N'
  UNION ALL SELECT 1, '按天', 'DAY', 'mes_shift_method', 'primary', 'Y'
  UNION ALL SELECT 2, '按周', 'WEEK', 'mes_shift_method', 'success', 'N'
  UNION ALL SELECT 3, '按月', 'MONTH', 'mes_shift_method', 'warning', 'N'
  UNION ALL SELECT 4, '按季度', 'QUARTER', 'mes_shift_method', 'info', 'N'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data d
  WHERE d.dict_type = v.dict_type AND d.dict_value = v.dict_value
);

-- ========== 2026 年 7 月旗舰排班演示 ==========
INSERT INTO cal_holiday (the_day, holiday_type, remark, create_by, create_time)
VALUES
  ('2026-07-05', 'HOLIDAY', '周日休息', 'admin', NOW(3)),
  ('2026-07-12', 'HOLIDAY', '周日休息', 'admin', NOW(3)),
  ('2026-07-19', 'HOLIDAY', '周日休息', 'admin', NOW(3)),
  ('2026-07-26', 'HOLIDAY', '周日休息', 'admin', NOW(3))
ON DUPLICATE KEY UPDATE holiday_type = VALUES(holiday_type), remark = VALUES(remark);

INSERT INTO cal_team (team_code, team_name, calendar_type, remark, enable_flag, create_by, create_time)
VALUES
  ('ASM-TEAM-1', '组装一班', 'ZZ', '2026年7月组装两班倒', 'Y', 'admin', NOW(3)),
  ('ASM-TEAM-2', '组装二班', 'ZZ', '2026年7月组装两班倒', 'Y', 'admin', NOW(3)),
  ('WH-TEAM-1', '仓储班组', 'CK', '2026年7月仓储单白班', 'Y', 'admin', NOW(3))
ON DUPLICATE KEY UPDATE
  team_name = VALUES(team_name), calendar_type = VALUES(calendar_type),
  remark = VALUES(remark), enable_flag = 'Y';

INSERT INTO cal_plan (
  plan_code, plan_name, calendar_type, start_date, end_date, shift_type,
  shift_method, status, shift_count, remark, enable_flag, create_by, create_time
)
VALUES
  ('CAL-202607-ASM', '2026年7月组装两班倒', 'ZZ', '2026-07-01', '2026-07-31',
   'SHIFT_TWO', 'DAY', 'CONFIRMED', 1, '两个组装班组每日轮换白夜班', 'Y', 'admin', NOW(3)),
  ('CAL-202607-WH', '2026年7月仓储单白班', 'CK', '2026-07-01', '2026-07-31',
   'SINGLE', 'DAY', 'CONFIRMED', 1, '仓储班组工作日白班', 'Y', 'admin', NOW(3))
ON DUPLICATE KEY UPDATE
  plan_name = VALUES(plan_name), calendar_type = VALUES(calendar_type),
  start_date = VALUES(start_date), end_date = VALUES(end_date),
  shift_type = VALUES(shift_type), shift_method = VALUES(shift_method),
  status = VALUES(status), shift_count = VALUES(shift_count),
  remark = VALUES(remark), enable_flag = 'Y';

-- 班次按“计划 + 序号”幂等维护。
UPDATE cal_shift s
JOIN cal_plan p ON p.plan_id = s.plan_id
SET s.shift_name = CASE s.order_num WHEN 1 THEN '白班' ELSE '夜班' END,
    s.start_time = CASE s.order_num WHEN 1 THEN '08:00:00' ELSE '20:00:00' END,
    s.end_time = CASE s.order_num WHEN 1 THEN '20:00:00' ELSE '08:00:00' END,
    s.enable_flag = 'Y'
WHERE p.plan_code = 'CAL-202607-ASM' AND s.order_num IN (1, 2);

INSERT INTO cal_shift (plan_id, order_num, shift_name, start_time, end_time, remark, enable_flag, create_by, create_time)
SELECT p.plan_id, v.order_num, v.shift_name, v.start_time, v.end_time,
       '2026年7月组装班次', 'Y', 'admin', NOW(3)
FROM cal_plan p
CROSS JOIN (
  SELECT 1 AS order_num, '白班' AS shift_name, '08:00:00' AS start_time, '20:00:00' AS end_time
  UNION ALL SELECT 2, '夜班', '20:00:00', '08:00:00'
) v
WHERE p.plan_code = 'CAL-202607-ASM'
  AND NOT EXISTS (
    SELECT 1 FROM cal_shift s WHERE s.plan_id = p.plan_id AND s.order_num = v.order_num
  );

UPDATE cal_shift s
JOIN cal_plan p ON p.plan_id = s.plan_id
SET s.shift_name = '白班', s.start_time = '08:00:00', s.end_time = '17:00:00', s.enable_flag = 'Y'
WHERE p.plan_code = 'CAL-202607-WH' AND s.order_num = 1;

INSERT INTO cal_shift (plan_id, order_num, shift_name, start_time, end_time, remark, enable_flag, create_by, create_time)
SELECT p.plan_id, 1, '白班', '08:00:00', '17:00:00',
       '2026年7月仓储班次', 'Y', 'admin', NOW(3)
FROM cal_plan p
WHERE p.plan_code = 'CAL-202607-WH'
  AND NOT EXISTS (SELECT 1 FROM cal_shift s WHERE s.plan_id = p.plan_id AND s.order_num = 1);

INSERT INTO cal_plan_team (plan_id, team_id, team_code, team_name, create_by, create_time)
SELECT p.plan_id, t.team_id, t.team_code, t.team_name, 'admin', NOW(3)
FROM cal_plan p
JOIN cal_team t ON
  (p.plan_code = 'CAL-202607-ASM' AND t.team_code IN ('ASM-TEAM-1', 'ASM-TEAM-2'))
  OR (p.plan_code = 'CAL-202607-WH' AND t.team_code = 'WH-TEAM-1')
WHERE NOT EXISTS (
  SELECT 1 FROM cal_plan_team pt WHERE pt.plan_id = p.plan_id AND pt.team_id = t.team_id
);

INSERT INTO cal_team_member (team_id, user_id, user_name, nick_name, create_by, create_time)
SELECT t.team_id, u.user_id, u.username, u.real_name, 'admin', NOW(3)
FROM cal_team t
JOIN sys_user u ON
  (t.team_code = 'ASM-TEAM-1' AND u.username IN ('worker01', 'qc01'))
  OR (t.team_code = 'ASM-TEAM-2' AND u.username = 'supervisor01')
  OR (t.team_code = 'WH-TEAM-1' AND u.username = 'warehouse01')
WHERE NOT EXISTS (
  SELECT 1 FROM cal_team_member tm WHERE tm.team_id = t.team_id AND tm.user_id = u.user_id
);

-- 27 个非周日工作日 × 2 个组装班组 = 54 条。
INSERT INTO cal_teamshift (
  the_day, team_id, team_name, shift_id, shift_name, order_num,
  plan_id, calendar_type, shift_type, remark, create_by, create_time
)
SELECT DATE_FORMAT(d.dt, '%Y-%m-%d'), t.team_id, t.team_name, s.shift_id, s.shift_name,
       s.order_num, p.plan_id, 'ZZ', 'SHIFT_TWO', '2026年7月组装轮班', 'admin', NOW(3)
FROM cal_plan p
JOIN cal_team t ON t.team_code IN ('ASM-TEAM-1', 'ASM-TEAM-2')
CROSS JOIN (
  SELECT DATE_ADD('2026-07-01', INTERVAL n.seq DAY) AS dt
  FROM (
    SELECT 0 seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
    UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
    UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
    UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29
    UNION ALL SELECT 30
  ) n
) d
JOIN cal_shift s ON s.plan_id = p.plan_id
  AND s.order_num = IF(
    MOD(DAY(d.dt) - 1, 2) = 0,
    IF(t.team_code = 'ASM-TEAM-1', 1, 2),
    IF(t.team_code = 'ASM-TEAM-1', 2, 1)
  )
WHERE p.plan_code = 'CAL-202607-ASM'
  AND DAYOFWEEK(d.dt) <> 1
  AND NOT EXISTS (
    SELECT 1 FROM cal_teamshift ts
    WHERE ts.plan_id = p.plan_id AND ts.team_id = t.team_id
      AND ts.the_day = DATE_FORMAT(d.dt, '%Y-%m-%d')
  );

-- 27 个非周日工作日 × 1 个仓储班组 = 27 条。
INSERT INTO cal_teamshift (
  the_day, team_id, team_name, shift_id, shift_name, order_num,
  plan_id, calendar_type, shift_type, remark, create_by, create_time
)
SELECT DATE_FORMAT(d.dt, '%Y-%m-%d'), t.team_id, t.team_name, s.shift_id, s.shift_name,
       1, p.plan_id, 'CK', 'SINGLE', '2026年7月仓储白班', 'admin', NOW(3)
FROM cal_plan p
JOIN cal_team t ON t.team_code = 'WH-TEAM-1'
JOIN cal_shift s ON s.plan_id = p.plan_id AND s.order_num = 1
CROSS JOIN (
  SELECT DATE_ADD('2026-07-01', INTERVAL n.seq DAY) AS dt
  FROM (
    SELECT 0 seq UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
    UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19
    UNION ALL SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24
    UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29
    UNION ALL SELECT 30
  ) n
) d
WHERE p.plan_code = 'CAL-202607-WH'
  AND DAYOFWEEK(d.dt) <> 1
  AND NOT EXISTS (
    SELECT 1 FROM cal_teamshift ts
    WHERE ts.plan_id = p.plan_id AND ts.team_id = t.team_id
      AND ts.the_day = DATE_FORMAT(d.dt, '%Y-%m-%d')
  );

-- ========== 班组成员（worker01 / qc01 / repair01）==========
INSERT INTO cal_team_member (team_id, user_id, user_name, nick_name, create_time)
SELECT t.team_id, u.user_id, u.username, u.real_name, NOW(3)
FROM cal_team t
JOIN (
  SELECT 'TEAM-A' AS team_code, 'worker01' AS username
  UNION ALL SELECT 'TEAM-A', 'qc01'
  UNION ALL SELECT 'TEAM-B', 'repair01'
  UNION ALL SELECT 'TEAM-B', 'supervisor01'
) wanted ON wanted.team_code = t.team_code
JOIN sys_user u ON u.username = wanted.username
WHERE NOT EXISTS (
  SELECT 1 FROM cal_team_member tm
  WHERE tm.team_id = t.team_id AND tm.user_id = u.user_id
);

-- ========== 账号别名（兼容 AGENTS.md 文档）==========
INSERT INTO sys_user (username, password_hash, employee_no, real_name, dept_id, status)
SELECT v.username, u.password_hash, v.emp_no, u.real_name, u.dept_id, 'ENABLED'
FROM (
  SELECT 'supervisor' AS username, 'supervisor01' AS src, 'P001' AS emp_no
  UNION ALL SELECT 'warehouse', 'warehouse01', 'W001'
  UNION ALL SELECT 'quality', 'qc01', 'Q001'
  UNION ALL SELECT 'repair', 'repair01', 'E001'
  UNION ALL SELECT 'worker', 'worker01', 'L001'
) v
JOIN sys_user u ON u.username = v.src
WHERE NOT EXISTS (SELECT 1 FROM sys_user x WHERE x.username = v.username);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u2.user_id, ur.role_id
FROM sys_user u2
JOIN sys_user u1 ON u1.username = CASE u2.username
  WHEN 'supervisor' THEN 'supervisor01' WHEN 'warehouse' THEN 'warehouse01'
  WHEN 'quality' THEN 'qc01' WHEN 'repair' THEN 'repair01' WHEN 'worker' THEN 'worker01' END
JOIN sys_user_role ur ON ur.user_id = u1.user_id
WHERE u2.username IN ('supervisor','warehouse','quality','repair','worker');

-- ========== 系统消息 ==========
INSERT INTO sys_message (
  message_id, message_type, message_level, message_title, message_content,
  sender_id, sender_name, sender_nick, recipient_id, recipient_name, recipient_nick,
  status, deleted_flag, remark, create_by, create_time
)
SELECT v.mid, v.mtype, v.mlevel, v.title, v.content,
  s.user_id, s.username, s.real_name, r.user_id, r.username, r.real_name,
  v.status, 'N', '', 'admin', v.ctime
FROM (
  SELECT 9101 AS mid, 'NOTICE' AS mtype, 'INFO' AS mlevel, '七月生产计划已发布' AS title,
    '本周重点工单：WO-20260701（FS40）、WO-20260707（TS30），请各工序查收排产。' AS content,
    'supervisor01' AS sender, 'supervisor01' AS recipient, 'UNREAD' AS status, NOW() AS ctime
  UNION ALL SELECT 9102, 'ALERT', 'WARN', 'M4螺丝库存预警',
    'WO-20260704齐套分析显示M4螺丝欠料1832件，请仓储尽快补货。', 'warehouse01', 'supervisor01', 'UNREAD', NOW()
  UNION ALL SELECT 9103, 'NOTICE', 'INFO', '待检验任务提醒',
    '您有3条来料检验、2条过程检验待处理。', 'admin', 'qc01', 'UNREAD', NOW()
  UNION ALL SELECT 9104, 'ALERT', 'WARN', '设备点检逾期',
    '老化测试台01点检计划已逾期，请设备部处理。', 'repair01', 'repair01', 'READ', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 9105, 'NOTICE', 'INFO', 'WO-20260705成品已入库',
    '400台FS40-A已完工入库，可进行出货检验。', 'warehouse01', 'qc01', 'UNREAD', DATE_SUB(NOW(), INTERVAL 2 DAY)
) v
JOIN sys_user s ON s.username = v.sender
JOIN sys_user r ON r.username = v.recipient
WHERE NOT EXISTS (SELECT 1 FROM sys_message m WHERE m.message_id = v.mid);

-- ========== 登录日志 ==========
INSERT INTO sys_logininfor (info_id, user_name, ipaddr, login_location, browser, os, status, msg, login_time)
SELECT v.iid, v.uname, v.ip, '内网', 'Chrome', 'Windows 10', v.status, v.msg, v.ltime
FROM (
  SELECT 9201 AS iid, 'admin' AS uname, '127.0.0.1' AS ip, '0' AS status, '登录成功' AS msg, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS ltime
  UNION ALL SELECT 9202, 'supervisor01', '192.168.1.101', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 3 HOUR)
  UNION ALL SELECT 9203, 'warehouse01', '192.168.1.102', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 4 HOUR)
  UNION ALL SELECT 9204, 'qc01', '192.168.1.103', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 5 HOUR)
  UNION ALL SELECT 9205, 'worker01', '192.168.1.201', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 1 HOUR)
  UNION ALL SELECT 9206, 'tester', '127.0.0.1', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 30 MINUTE)
  UNION ALL SELECT 9207, 'repair01', '192.168.1.104', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 6 HOUR)
  UNION ALL SELECT 9208, 'supervisor', '192.168.1.101', '0', '登录成功', DATE_SUB(NOW(), INTERVAL 1 DAY)
) v
WHERE NOT EXISTS (SELECT 1 FROM sys_logininfor l WHERE l.info_id = v.iid);

-- 修正 V24 中 worker 引用为 worker01
UPDATE cal_team_member ctm
JOIN sys_user u ON u.username = 'worker01'
SET ctm.user_id = u.user_id, ctm.user_name = u.username, ctm.nick_name = u.real_name
WHERE ctm.user_name = 'worker' OR (ctm.nick_name = 'Line Operator' AND ctm.user_name NOT LIKE '%01');
