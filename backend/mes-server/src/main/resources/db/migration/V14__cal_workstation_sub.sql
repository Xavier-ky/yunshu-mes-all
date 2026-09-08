-- yunshu cal module + workstation sub tables + workshop extensions

ALTER TABLE workshop
  ADD COLUMN area DOUBLE(12,2) NULL COMMENT '面积' AFTER workshop_name,
  ADD COLUMN charge VARCHAR(64) NULL COMMENT '负责人' AFTER area,
  ADD COLUMN remark VARCHAR(500) NOT NULL DEFAULT '' AFTER status;

CREATE TABLE md_workstation_machine (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workstation_id BIGINT UNSIGNED NOT NULL,
  machinery_id BIGINT UNSIGNED NOT NULL,
  machinery_code VARCHAR(64) NULL,
  machinery_name VARCHAR(255) NULL,
  quantity INT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_md_ws_machine_ws (workstation_id)
) ENGINE=InnoDB COMMENT='工位设备';

CREATE TABLE md_workstation_tool (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workstation_id BIGINT UNSIGNED NOT NULL,
  tool_id BIGINT UNSIGNED NOT NULL,
  tool_code VARCHAR(64) NULL,
  tool_name VARCHAR(255) NULL,
  quantity INT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_md_ws_tool_ws (workstation_id)
) ENGINE=InnoDB COMMENT='工位工具';

CREATE TABLE md_workstation_worker (
  record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workstation_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  user_name VARCHAR(64) NULL,
  nick_name VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  PRIMARY KEY (record_id),
  KEY idx_md_ws_worker_ws (workstation_id)
) ENGINE=InnoDB COMMENT='工位人员';

CREATE TABLE cal_team (
  team_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  team_code VARCHAR(64) NOT NULL,
  team_name VARCHAR(255) NOT NULL,
  calendar_type VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (team_id),
  UNIQUE KEY uk_cal_team_code (team_code)
) ENGINE=InnoDB COMMENT='班组';

CREATE TABLE cal_shift (
  shift_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shift_name VARCHAR(255) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (shift_id)
) ENGINE=InnoDB COMMENT='班次';

CREATE TABLE cal_plan (
  plan_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  plan_code VARCHAR(64) NOT NULL,
  plan_name VARCHAR(255) NOT NULL,
  calendar_type VARCHAR(64) NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  shift_type VARCHAR(64) NULL,
  shift_method VARCHAR(64) NULL,
  remark VARCHAR(500) NOT NULL DEFAULT '',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
  create_by VARCHAR(64) NOT NULL DEFAULT '',
  create_time DATETIME(3) NULL,
  update_by VARCHAR(64) NOT NULL DEFAULT '',
  update_time DATETIME(3) NULL,
  PRIMARY KEY (plan_id),
  UNIQUE KEY uk_cal_plan_code (plan_code)
) ENGINE=InnoDB COMMENT='排班计划';

-- seed shifts from factory_shift
INSERT INTO cal_shift (shift_id, shift_name, start_time, end_time, enable_flag, create_time)
SELECT shift_id, shift_name, start_time, end_time,
  IF(status = 'ENABLED', 'Y', 'N'), NOW(3)
FROM factory_shift;

INSERT INTO cal_team (team_code, team_name, enable_flag, create_time)
VALUES ('TEAM-A', 'A班组', 'Y', NOW(3)), ('TEAM-B', 'B班组', 'Y', NOW(3));

INSERT INTO cal_plan (plan_code, plan_name, shift_type, enable_flag, create_time)
VALUES ('PLAN-DEFAULT', '默认排班计划', 'SHIFT', 'Y', NOW(3));
