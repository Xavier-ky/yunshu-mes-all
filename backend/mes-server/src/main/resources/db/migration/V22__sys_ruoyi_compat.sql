-- V22: 云枢系统管理 compat 层（扩展 sys_* 表）
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- Safe column add helper
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE sp_add_column_if_not_exists(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition VARCHAR(512)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

-- ---------------------------------------------------------------------------
-- sys_user compat columns
-- ---------------------------------------------------------------------------
CALL sp_add_column_if_not_exists('sys_user', 'nick_name', "VARCHAR(64) NULL COMMENT '用户昵称' AFTER real_name");
CALL sp_add_column_if_not_exists('sys_user', 'sex', "CHAR(1) NOT NULL DEFAULT '0' COMMENT '性别 0男 1女 2未知' AFTER email");
CALL sp_add_column_if_not_exists('sys_user', 'avatar', "VARCHAR(255) NULL DEFAULT '' COMMENT '头像地址' AFTER sex");
CALL sp_add_column_if_not_exists('sys_user', 'remark', "VARCHAR(500) NULL COMMENT '备注' AFTER avatar");
CALL sp_add_column_if_not_exists('sys_user', 'login_ip', "VARCHAR(128) NULL DEFAULT '' COMMENT '最后登录IP' AFTER last_login_at");

-- ---------------------------------------------------------------------------
-- sys_role compat columns
-- ---------------------------------------------------------------------------
CALL sp_add_column_if_not_exists('sys_role', 'role_key', "VARCHAR(100) NULL COMMENT '角色权限字符串' AFTER role_code");
CALL sp_add_column_if_not_exists('sys_role', 'role_sort', "INT NOT NULL DEFAULT 0 COMMENT '显示顺序' AFTER role_key");
CALL sp_add_column_if_not_exists('sys_role', 'data_scope', "CHAR(1) NOT NULL DEFAULT '1' COMMENT '数据范围' AFTER role_sort");
CALL sp_add_column_if_not_exists('sys_role', 'menu_check_strictly', "TINYINT NOT NULL DEFAULT 1 COMMENT '菜单树关联显示' AFTER data_scope");
CALL sp_add_column_if_not_exists('sys_role', 'dept_check_strictly', "TINYINT NOT NULL DEFAULT 1 COMMENT '部门树关联显示' AFTER menu_check_strictly");
CALL sp_add_column_if_not_exists('sys_role', 'remark', "VARCHAR(500) NULL COMMENT '备注' AFTER role_desc");

-- ---------------------------------------------------------------------------
-- sys_department compat columns
-- ---------------------------------------------------------------------------
CALL sp_add_column_if_not_exists('sys_department', 'order_num', "INT NOT NULL DEFAULT 0 COMMENT '显示顺序' AFTER dept_name");
CALL sp_add_column_if_not_exists('sys_department', 'leader', "VARCHAR(64) NULL COMMENT '负责人' AFTER order_num");
CALL sp_add_column_if_not_exists('sys_department', 'ancestors', "VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '祖级列表' AFTER parent_dept_id");
CALL sp_add_column_if_not_exists('sys_department', 'phone', "VARCHAR(32) NULL COMMENT '联系电话' AFTER leader");
CALL sp_add_column_if_not_exists('sys_department', 'email', "VARCHAR(128) NULL COMMENT '邮箱' AFTER phone");

DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;

-- ---------------------------------------------------------------------------
-- Backfill
-- ---------------------------------------------------------------------------
UPDATE sys_user SET nick_name = real_name WHERE nick_name IS NULL;
UPDATE sys_role SET role_key = role_code WHERE role_key IS NULL;

-- ---------------------------------------------------------------------------
-- sys_post
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_post (
  post_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  post_code VARCHAR(64) NOT NULL COMMENT '岗位编码',
  post_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
  post_sort INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (post_id),
  UNIQUE KEY uk_sys_post_code (post_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';

-- ---------------------------------------------------------------------------
-- sys_user_post
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_post (
  user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  post_id BIGINT UNSIGNED NOT NULL COMMENT '岗位ID',
  PRIMARY KEY (user_id, post_id),
  KEY idx_sys_user_post_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户与岗位关联表';

-- ---------------------------------------------------------------------------
-- sys_menu
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
  menu_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_name VARCHAR(50) NOT NULL COMMENT '菜单名称',
  parent_id BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  order_num INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  path VARCHAR(200) NOT NULL DEFAULT '' COMMENT '路由地址',
  component VARCHAR(255) NULL COMMENT '组件路径',
  query VARCHAR(255) NULL COMMENT '路由参数',
  is_frame INT NOT NULL DEFAULT 1 COMMENT '是否外链 0是 1否',
  is_cache INT NOT NULL DEFAULT 0 COMMENT '是否缓存 0缓存 1不缓存',
  menu_type CHAR(1) NOT NULL DEFAULT '' COMMENT 'M目录 C菜单 F按钮',
  visible CHAR(1) NOT NULL DEFAULT '0' COMMENT '0显示 1隐藏',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0正常 1停用',
  perms VARCHAR(100) NULL COMMENT '权限标识',
  icon VARCHAR(100) NOT NULL DEFAULT '#' COMMENT '菜单图标',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (menu_id),
  KEY idx_sys_menu_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单权限表';

-- ---------------------------------------------------------------------------
-- sys_role_menu
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  menu_id BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id),
  KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和菜单关联表';

-- ---------------------------------------------------------------------------
-- sys_role_dept
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_dept (
  role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  dept_id BIGINT UNSIGNED NOT NULL COMMENT '部门ID',
  PRIMARY KEY (role_id, dept_id),
  KEY idx_sys_role_dept_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色和部门关联表';

-- ---------------------------------------------------------------------------
-- sys_dict_type
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
  dict_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  dict_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典名称',
  dict_type VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (dict_id),
  UNIQUE KEY uk_sys_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表';

-- ---------------------------------------------------------------------------
-- sys_dict_data
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data (
  dict_code BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  dict_sort INT NOT NULL DEFAULT 0 COMMENT '字典排序',
  dict_label VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典标签',
  dict_value VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典键值',
  dict_type VARCHAR(100) NOT NULL DEFAULT '' COMMENT '字典类型',
  css_class VARCHAR(100) NULL COMMENT '样式属性',
  list_class VARCHAR(100) NULL DEFAULT 'default' COMMENT '表格回显样式',
  is_default CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否默认 Y是 N否',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (dict_code),
  KEY idx_sys_dict_data_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典数据表';

-- ---------------------------------------------------------------------------
-- sys_config
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_config (
  config_id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  config_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数名称',
  config_key VARCHAR(100) NOT NULL DEFAULT '' COMMENT '参数键名',
  config_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参数键值',
  config_type CHAR(1) NOT NULL DEFAULT 'N' COMMENT '系统内置 Y是 N否',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_sys_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';

-- ---------------------------------------------------------------------------
-- sys_notice
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_notice (
  notice_id INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  notice_title VARCHAR(50) NOT NULL COMMENT '公告标题',
  notice_type CHAR(1) NOT NULL COMMENT '公告类型 1通知 2公告',
  notice_content LONGBLOB NULL COMMENT '公告内容',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '公告状态 0正常 1关闭',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  remark VARCHAR(255) NULL COMMENT '备注',
  PRIMARY KEY (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知公告表';

-- ---------------------------------------------------------------------------
-- sys_logininfor
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_logininfor (
  info_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  user_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '用户账号',
  ipaddr VARCHAR(128) NOT NULL DEFAULT '' COMMENT '登录IP地址',
  login_location VARCHAR(255) NOT NULL DEFAULT '' COMMENT '登录地点',
  browser VARCHAR(50) NOT NULL DEFAULT '' COMMENT '浏览器类型',
  os VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作系统',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '登录状态 0成功 1失败',
  msg VARCHAR(255) NOT NULL DEFAULT '' COMMENT '提示消息',
  login_time DATETIME NULL COMMENT '访问时间',
  PRIMARY KEY (info_id),
  KEY idx_sys_logininfor_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统访问记录';

-- ---------------------------------------------------------------------------
-- sys_oper_log (compat shape)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log (
  oper_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  title VARCHAR(50) NOT NULL DEFAULT '' COMMENT '模块标题',
  business_type INT NOT NULL DEFAULT 0 COMMENT '业务类型',
  method VARCHAR(100) NOT NULL DEFAULT '' COMMENT '方法名称',
  request_method VARCHAR(10) NOT NULL DEFAULT '' COMMENT '请求方式',
  operator_type INT NOT NULL DEFAULT 0 COMMENT '操作类别',
  oper_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '操作人员',
  dept_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '部门名称',
  oper_url VARCHAR(255) NOT NULL DEFAULT '' COMMENT '请求URL',
  oper_ip VARCHAR(128) NOT NULL DEFAULT '' COMMENT '主机地址',
  oper_location VARCHAR(255) NOT NULL DEFAULT '' COMMENT '操作地点',
  oper_param VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '请求参数',
  json_result VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '返回参数',
  status INT NOT NULL DEFAULT 0 COMMENT '操作状态 0正常 1异常',
  error_msg VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '错误消息',
  oper_time DATETIME NULL COMMENT '操作时间',
  PRIMARY KEY (oper_id),
  KEY idx_sys_oper_log_time (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志记录';

-- ---------------------------------------------------------------------------
-- sys_auto_code_rule / part / result
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_auto_code_rule (
  rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  rule_code VARCHAR(64) NOT NULL COMMENT '规则编码',
  rule_name VARCHAR(255) NOT NULL COMMENT '规则名称',
  rule_desc VARCHAR(500) NULL COMMENT '描述',
  max_length INT NULL COMMENT '最大长度',
  is_padded CHAR(1) NOT NULL COMMENT '是否补齐',
  padded_char VARCHAR(20) NULL COMMENT '补齐字符',
  padded_method CHAR(1) NOT NULL DEFAULT 'L' COMMENT '补齐方式',
  enable_flag CHAR(1) NOT NULL DEFAULT 'Y' COMMENT '是否启用',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_sys_auto_code_rule_code (rule_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成规则表';

CREATE TABLE IF NOT EXISTS sys_auto_code_part (
  part_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分段ID',
  rule_id BIGINT UNSIGNED NOT NULL COMMENT '规则ID',
  part_index INT NOT NULL COMMENT '分段序号',
  part_type VARCHAR(20) NOT NULL COMMENT '分段类型',
  part_code VARCHAR(64) NULL COMMENT '分段编号',
  part_name VARCHAR(255) NULL COMMENT '分段名称',
  part_length INT NOT NULL COMMENT '分段长度',
  date_format VARCHAR(20) NULL COMMENT '日期时间格式',
  input_character VARCHAR(64) NULL COMMENT '输入字符',
  fix_character VARCHAR(64) NULL COMMENT '固定字符',
  seria_start_no INT NULL COMMENT '流水号起始值',
  seria_step INT NULL COMMENT '流水号步长',
  seria_now_no INT NULL COMMENT '流水号当前值',
  cycle_flag CHAR(1) NULL COMMENT '流水号是否循环',
  cycle_method VARCHAR(20) NULL COMMENT '循环方式',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (part_id),
  KEY idx_sys_auto_code_part_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成规则组成表';

CREATE TABLE IF NOT EXISTS sys_auto_code_result (
  code_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  rule_id BIGINT UNSIGNED NOT NULL COMMENT '规则ID',
  gen_date VARCHAR(20) NOT NULL COMMENT '生成日期时间',
  gen_index INT NULL COMMENT '最后产生的序号',
  last_result VARCHAR(64) NULL COMMENT '最后产生的值',
  last_serial_no INT NULL COMMENT '最后产生的流水号',
  last_input_char VARCHAR(64) NULL COMMENT '最后传入的参数',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (code_id),
  KEY idx_sys_auto_code_result_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='编码生成记录表';

-- ---------------------------------------------------------------------------
-- sys_message
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_message (
  message_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  message_type VARCHAR(64) NOT NULL COMMENT '消息类型',
  message_level VARCHAR(64) NOT NULL COMMENT '消息级别',
  message_title VARCHAR(64) NULL COMMENT '标题',
  message_content LONGBLOB NULL COMMENT '内容',
  sender_id BIGINT UNSIGNED NULL COMMENT '发送人ID',
  sender_name VARCHAR(64) NULL COMMENT '发送人名称',
  sender_nick VARCHAR(64) NULL COMMENT '发送人昵称',
  recipient_id BIGINT UNSIGNED NOT NULL COMMENT '接收人ID',
  recipient_name VARCHAR(64) NULL COMMENT '接收人名称',
  recipient_nick VARCHAR(64) NULL COMMENT '接收人昵称',
  process_time DATETIME NULL COMMENT '处理时间',
  call_back VARCHAR(255) NULL COMMENT '回调地址',
  status VARCHAR(64) NOT NULL DEFAULT 'UNREAD' COMMENT '状态',
  deleted_flag CHAR(1) NOT NULL DEFAULT 'N' COMMENT '是否删除',
  remark VARCHAR(500) NOT NULL DEFAULT '' COMMENT '备注',
  attr1 VARCHAR(64) NULL COMMENT '预留字段1',
  attr2 VARCHAR(255) NULL COMMENT '预留字段2',
  attr3 INT NOT NULL DEFAULT 0 COMMENT '预留字段3',
  attr4 INT NOT NULL DEFAULT 0 COMMENT '预留字段4',
  create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  create_time DATETIME NULL COMMENT '创建时间',
  update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  update_time DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (message_id),
  KEY idx_sys_message_recipient (recipient_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息表';
