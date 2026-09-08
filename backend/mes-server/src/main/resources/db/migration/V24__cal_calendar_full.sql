-- yunshu cal module completion: team members, plan teams, holidays, team shifts

ALTER TABLE cal_plan
  ADD COLUMN status VARCHAR(64) NOT NULL DEFAULT 'PREPARE' COMMENT '计划状态' AFTER shift_method,
  ADD COLUMN shift_count INT NOT NULL DEFAULT 1 COMMENT '倒班天数' AFTER status;

ALTER TABLE cal_shift
  ADD COLUMN plan_id BIGINT UNSIGNED NULL COMMENT '计划ID' AFTER shift_id,
  ADD COLUMN order_num INT NOT NULL DEFAULT 1 COMMENT '序号' AFTER plan_id;

CREATE TABLE IF NOT EXISTS cal_team_member (
  member_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  team_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  user_name VARCHAR(64) NOT NULL,
  nick_name VARCHAR(64) NULL,
  tel VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (member_id),
  KEY idx_cal_team_member_team (team_id),
  KEY idx_cal_team_member_user (user_id)
) ENGINE=InnoDB COMMENT='班组成员';

CREATE TABLE IF NOT EXISTS cal_plan_team (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_id BIGINT UNSIGNED NOT NULL,
  team_id BIGINT UNSIGNED NOT NULL,
  team_code VARCHAR(64) NULL,
  team_name VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_cal_plan_team_plan (plan_id),
  KEY idx_cal_plan_team_team (team_id)
) ENGINE=InnoDB COMMENT='计划班组';

CREATE TABLE IF NOT EXISTS cal_holiday (
  holiday_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  the_day DATE NOT NULL,
  holiday_type VARCHAR(64) NULL COMMENT 'HOLIDAY/WORKDAY',
  start_time DATETIME(3) NULL,
  end_time DATETIME(3) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (holiday_id),
  UNIQUE KEY uk_cal_holiday_day (the_day)
) ENGINE=InnoDB COMMENT='节假日设置';

CREATE TABLE IF NOT EXISTS cal_teamshift (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  the_day VARCHAR(16) NOT NULL,
  team_id BIGINT UNSIGNED NOT NULL,
  team_name VARCHAR(255) NULL,
  shift_id BIGINT UNSIGNED NOT NULL,
  shift_name VARCHAR(255) NULL,
  order_num INT NULL,
  plan_id BIGINT UNSIGNED NULL,
  calendar_type VARCHAR(64) NULL,
  shift_type VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_cal_teamshift_day (the_day),
  KEY idx_cal_teamshift_team (team_id),
  KEY idx_cal_teamshift_plan (plan_id)
) ENGINE=InnoDB COMMENT='班组排班';

-- Link existing shifts to default plan
UPDATE cal_shift SET plan_id = (SELECT plan_id FROM cal_plan ORDER BY plan_id LIMIT 1), order_num = shift_id
WHERE plan_id IS NULL;

UPDATE cal_plan SET status = 'CONFIRMED', shift_count = 1,
  start_date = COALESCE(start_date, CURDATE()),
  end_date = COALESCE(end_date, DATE_ADD(CURDATE(), INTERVAL 30 DAY))
WHERE plan_code = 'PLAN-DEFAULT';

INSERT IGNORE INTO cal_team_member (team_id, user_id, user_name, nick_name, create_time)
SELECT t.team_id, u.user_id, u.username, u.real_name, NOW(3)
FROM cal_team t
CROSS JOIN (SELECT user_id, username, real_name FROM sys_user WHERE username = 'worker' LIMIT 1) u
WHERE t.team_code = 'TEAM-A';

INSERT IGNORE INTO cal_plan_team (plan_id, team_id, team_code, team_name, create_time)
SELECT p.plan_id, t.team_id, t.team_code, t.team_name, NOW(3)
FROM cal_plan p
JOIN cal_team t ON t.team_code IN ('TEAM-A', 'TEAM-B')
WHERE p.plan_code = 'PLAN-DEFAULT';

-- Demo team shifts for current month (single shift type)
INSERT IGNORE INTO cal_teamshift (the_day, team_id, team_name, shift_id, shift_name, order_num, plan_id, calendar_type, shift_type, create_time)
SELECT DATE_FORMAT(d.dt, '%Y-%m-%d'), t.team_id, t.team_name, s.shift_id, s.shift_name, 1,
       p.plan_id, t.calendar_type, p.shift_type, NOW(3)
FROM cal_plan p
JOIN cal_team t ON t.team_code = 'TEAM-A'
JOIN cal_shift s ON s.plan_id = p.plan_id AND s.order_num = 1
JOIN (
  SELECT DATE_ADD(DATE_FORMAT(CURDATE(), '%Y-%m-01'), INTERVAL seq DAY) AS dt
  FROM (
    SELECT 0 AS seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
    UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9
    UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14
    UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19
    UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24
    UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
    UNION SELECT 30
  ) nums
) d
WHERE p.plan_code = 'PLAN-DEFAULT'
  AND DAYOFWEEK(d.dt) NOT IN (1, 7);
