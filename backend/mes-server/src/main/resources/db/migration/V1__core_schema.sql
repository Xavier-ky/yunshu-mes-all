-- MySQL 8.0+
-- Core master data schema for the electric fan MES project.

CREATE DATABASE IF NOT EXISTS fan_mes
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE fan_mes;

CREATE TABLE sys_department (
  dept_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  dept_code VARCHAR(64) NOT NULL COMMENT '部门编码',
  dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
  dept_type VARCHAR(32) NOT NULL COMMENT '部门类型: MANAGEMENT/PMC/PRODUCTION/WAREHOUSE/QUALITY/EQUIPMENT',
  parent_dept_id BIGINT UNSIGNED NULL COMMENT '上级部门ID',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (dept_id),
  UNIQUE KEY uk_sys_department_code (dept_code),
  KEY idx_sys_department_parent (parent_dept_id),
  CONSTRAINT fk_sys_department_parent
    FOREIGN KEY (parent_dept_id) REFERENCES sys_department (dept_id)
) ENGINE=InnoDB COMMENT='部门表';

CREATE TABLE sys_user (
  user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL COMMENT '登录账号',
  password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
  employee_no VARCHAR(64) NULL COMMENT '员工工号',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  dept_id BIGINT UNSIGNED NULL COMMENT '所属部门',
  phone VARCHAR(32) NULL COMMENT '手机号',
  email VARCHAR(128) NULL COMMENT '邮箱',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
  last_login_at DATETIME(3) NULL COMMENT '最后登录时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_sys_user_username (username),
  UNIQUE KEY uk_sys_user_employee_no (employee_no),
  KEY idx_sys_user_dept (dept_id),
  CONSTRAINT fk_sys_user_dept
    FOREIGN KEY (dept_id) REFERENCES sys_department (dept_id)
) ENGINE=InnoDB COMMENT='系统用户表';

CREATE TABLE sys_role (
  role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
  role_desc VARCHAR(255) NULL COMMENT '角色说明',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (role_id),
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_permission (
  permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  permission_code VARCHAR(128) NOT NULL COMMENT '权限编码',
  permission_name VARCHAR(128) NOT NULL COMMENT '权限名称',
  resource_type VARCHAR(32) NOT NULL COMMENT '资源类型: MENU/BUTTON/API/DATA/AGENT_TOOL',
  resource_code VARCHAR(128) NOT NULL COMMENT '资源编码',
  action_code VARCHAR(64) NOT NULL COMMENT '动作编码: READ/CREATE/UPDATE/DELETE/EXECUTE/APPROVE',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (permission_id),
  UNIQUE KEY uk_sys_permission_code (permission_code)
) ENGINE=InnoDB COMMENT='权限表';

CREATE TABLE sys_user_role (
  user_role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_role_id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role (role_id),
  CONSTRAINT fk_sys_user_role_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_sys_user_role_role
    FOREIGN KEY (role_id) REFERENCES sys_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关系表';

CREATE TABLE sys_role_permission (
  role_permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (role_permission_id),
  UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
  KEY idx_sys_role_permission_permission (permission_id),
  CONSTRAINT fk_sys_role_permission_role
    FOREIGN KEY (role_id) REFERENCES sys_role (role_id),
  CONSTRAINT fk_sys_role_permission_permission
    FOREIGN KEY (permission_id) REFERENCES sys_permission (permission_id)
) ENGINE=InnoDB COMMENT='角色权限关系表';

CREATE TABLE sys_operation_log (
  log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
  biz_object_type VARCHAR(64) NULL COMMENT '业务对象类型',
  biz_object_id BIGINT UNSIGNED NULL COMMENT '业务对象ID',
  request_payload JSON NULL COMMENT '请求内容',
  result_status VARCHAR(32) NOT NULL COMMENT '结果状态',
  result_message VARCHAR(1000) NULL COMMENT '结果说明',
  operation_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (log_id),
  KEY idx_sys_operation_log_user_time (user_id, operation_time),
  KEY idx_sys_operation_log_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_sys_operation_log_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='系统操作日志';

CREATE TABLE workshop (
  workshop_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workshop_code VARCHAR(64) NOT NULL,
  workshop_name VARCHAR(100) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (workshop_id),
  UNIQUE KEY uk_workshop_code (workshop_code)
) ENGINE=InnoDB COMMENT='车间表';

CREATE TABLE production_line (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workshop_id BIGINT UNSIGNED NOT NULL,
  line_code VARCHAR(64) NOT NULL,
  line_name VARCHAR(100) NOT NULL,
  rated_capacity DECIMAL(18,4) NULL COMMENT '额定产能',
  capacity_unit VARCHAR(32) NULL COMMENT '产能单位',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (line_id),
  UNIQUE KEY uk_production_line_code (line_code),
  KEY idx_production_line_workshop (workshop_id),
  CONSTRAINT fk_production_line_workshop
    FOREIGN KEY (workshop_id) REFERENCES workshop (workshop_id)
) ENGINE=InnoDB COMMENT='产线表';

CREATE TABLE workstation (
  station_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  line_id BIGINT UNSIGNED NOT NULL,
  station_code VARCHAR(64) NOT NULL,
  station_name VARCHAR(100) NOT NULL,
  station_type VARCHAR(32) NULL COMMENT '工位类型',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (station_id),
  UNIQUE KEY uk_workstation_code (station_code),
  KEY idx_workstation_line (line_id),
  CONSTRAINT fk_workstation_line
    FOREIGN KEY (line_id) REFERENCES production_line (line_id)
) ENGINE=InnoDB COMMENT='工位表';

CREATE TABLE factory_shift (
  shift_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shift_code VARCHAR(64) NOT NULL,
  shift_name VARCHAR(100) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (shift_id),
  UNIQUE KEY uk_factory_shift_code (shift_code)
) ENGINE=InnoDB COMMENT='班次表';

CREATE TABLE factory_calendar (
  calendar_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  calendar_date DATE NOT NULL,
  shift_id BIGINT UNSIGNED NULL,
  is_workday TINYINT(1) NOT NULL DEFAULT 1,
  remark VARCHAR(255) NULL,
  PRIMARY KEY (calendar_id),
  UNIQUE KEY uk_factory_calendar_date_shift (calendar_date, shift_id),
  KEY idx_factory_calendar_shift (shift_id),
  CONSTRAINT fk_factory_calendar_shift
    FOREIGN KEY (shift_id) REFERENCES factory_shift (shift_id)
) ENGINE=InnoDB COMMENT='工厂日历表';

CREATE TABLE uom (
  unit_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_code VARCHAR(32) NOT NULL,
  unit_name VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (unit_id),
  UNIQUE KEY uk_uom_code (unit_code)
) ENGINE=InnoDB COMMENT='计量单位表';

CREATE TABLE product (
  product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(128) NOT NULL,
  product_model VARCHAR(128) NULL COMMENT '产品型号',
  product_category VARCHAR(64) NULL COMMENT '产品类别',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (product_id),
  UNIQUE KEY uk_product_code (product_code)
) ENGINE=InnoDB COMMENT='产品主数据';

CREATE TABLE product_spec (
  spec_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  spec_name VARCHAR(100) NOT NULL,
  spec_value VARCHAR(255) NOT NULL,
  unit_code VARCHAR(32) NULL,
  PRIMARY KEY (spec_id),
  UNIQUE KEY uk_product_spec_name (product_id, spec_name),
  CONSTRAINT fk_product_spec_product
    FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB COMMENT='产品规格表';

CREATE TABLE material (
  material_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_code VARCHAR(64) NOT NULL,
  material_name VARCHAR(128) NOT NULL,
  material_type VARCHAR(64) NOT NULL COMMENT '物料类型: MOTOR/BLADE/SHELL/BASE/SCREW/PACKAGE/FINISHED',
  unit_id BIGINT UNSIGNED NULL,
  is_key_material TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否关键追溯物料',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (material_id),
  UNIQUE KEY uk_material_code (material_code),
  KEY idx_material_type (material_type),
  KEY idx_material_unit (unit_id),
  CONSTRAINT fk_material_unit
    FOREIGN KEY (unit_id) REFERENCES uom (unit_id)
) ENGINE=InnoDB COMMENT='物料主数据';

CREATE TABLE bom (
  bom_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  bom_code VARCHAR(64) NOT NULL,
  bom_name VARCHAR(128) NULL,
  version_no VARCHAR(32) NOT NULL,
  effective_date DATE NULL,
  expire_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (bom_id),
  UNIQUE KEY uk_bom_code_version (bom_code, version_no),
  KEY idx_bom_product (product_id),
  CONSTRAINT fk_bom_product
    FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB COMMENT='BOM主表';

CREATE TABLE bom_item (
  bom_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bom_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  qty_per DECIMAL(18,6) NOT NULL COMMENT '单位用量',
  loss_rate DECIMAL(9,6) NOT NULL DEFAULT 0 COMMENT '损耗率',
  is_key_material TINYINT(1) NOT NULL DEFAULT 0,
  remark VARCHAR(255) NULL,
  PRIMARY KEY (bom_item_id),
  UNIQUE KEY uk_bom_item_material (bom_id, material_id),
  KEY idx_bom_item_material (material_id),
  CONSTRAINT fk_bom_item_bom
    FOREIGN KEY (bom_id) REFERENCES bom (bom_id),
  CONSTRAINT fk_bom_item_material
    FOREIGN KEY (material_id) REFERENCES material (material_id)
) ENGINE=InnoDB COMMENT='BOM明细表';

CREATE TABLE process_step (
  step_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_code VARCHAR(64) NOT NULL,
  step_name VARCHAR(128) NOT NULL,
  step_type VARCHAR(64) NOT NULL COMMENT '工序类型: ASSEMBLY/TEST/PACKAGE/QUALITY',
  standard_time_sec INT UNSIGNED NULL COMMENT '标准工时秒',
  piece_price DECIMAL(18,4) NULL COMMENT '计件单价',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (step_id),
  UNIQUE KEY uk_process_step_code (step_code)
) ENGINE=InnoDB COMMENT='工序表';

CREATE TABLE process_route (
  route_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_code VARCHAR(64) NOT NULL,
  route_name VARCHAR(128) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (route_id),
  UNIQUE KEY uk_process_route_code_version (route_code, version_no)
) ENGINE=InnoDB COMMENT='工艺路线主表';

CREATE TABLE process_route_step (
  route_step_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_id BIGINT UNSIGNED NOT NULL,
  step_id BIGINT UNSIGNED NOT NULL,
  step_seq INT UNSIGNED NOT NULL,
  station_type VARCHAR(64) NULL COMMENT '推荐工位类型',
  is_must_pass TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (route_step_id),
  UNIQUE KEY uk_route_step_seq (route_id, step_seq),
  UNIQUE KEY uk_route_step_step (route_id, step_id),
  KEY idx_route_step_step (step_id),
  CONSTRAINT fk_route_step_route
    FOREIGN KEY (route_id) REFERENCES process_route (route_id),
  CONSTRAINT fk_route_step_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='工艺路线工序明细';

CREATE TABLE product_route (
  product_route_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  route_id BIGINT UNSIGNED NOT NULL,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (product_route_id),
  UNIQUE KEY uk_product_route (product_id, route_id),
  KEY idx_product_route_route (route_id),
  CONSTRAINT fk_product_route_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_product_route_route
    FOREIGN KEY (route_id) REFERENCES process_route (route_id)
) ENGINE=InnoDB COMMENT='产品工艺路线关系表';

CREATE TABLE sop_file (
  sop_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id BIGINT UNSIGNED NOT NULL,
  sop_code VARCHAR(64) NOT NULL,
  sop_name VARCHAR(128) NOT NULL,
  file_type VARCHAR(32) NOT NULL COMMENT 'IMAGE/VIDEO/PDF/DOC',
  file_url VARCHAR(500) NOT NULL,
  version_no VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (sop_id),
  UNIQUE KEY uk_sop_code_version (sop_code, version_no),
  KEY idx_sop_step (step_id),
  CONSTRAINT fk_sop_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='SOP文件表';

CREATE TABLE defect_reason (
  defect_reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  step_id BIGINT UNSIGNED NULL,
  reason_code VARCHAR(64) NOT NULL,
  reason_name VARCHAR(128) NOT NULL,
  reason_category VARCHAR(64) NOT NULL,
  severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (defect_reason_id),
  UNIQUE KEY uk_defect_reason_code (reason_code),
  KEY idx_defect_reason_step (step_id),
  CONSTRAINT fk_defect_reason_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='工序/质量不良原因表';

CREATE TABLE barcode_type (
  barcode_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(64) NOT NULL COMMENT 'PRODUCT/MATERIAL/INNER_BOX/OUTER_BOX/PALLET',
  type_name VARCHAR(100) NOT NULL,
  object_type VARCHAR(64) NOT NULL COMMENT '绑定对象类型',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (barcode_type_id),
  UNIQUE KEY uk_barcode_type_code (type_code)
) ENGINE=InnoDB COMMENT='条码类型表';

CREATE TABLE barcode_rule (
  rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  barcode_type_id BIGINT UNSIGNED NOT NULL,
  rule_code VARCHAR(64) NOT NULL,
  rule_name VARCHAR(128) NOT NULL,
  code_mode VARCHAR(32) NOT NULL COMMENT 'UNIQUE/BATCH',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (rule_id),
  UNIQUE KEY uk_barcode_rule_code (rule_code),
  KEY idx_barcode_rule_type (barcode_type_id),
  CONSTRAINT fk_barcode_rule_type
    FOREIGN KEY (barcode_type_id) REFERENCES barcode_type (barcode_type_id)
) ENGINE=InnoDB COMMENT='条码规则表';

CREATE TABLE barcode_rule_segment (
  segment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id BIGINT UNSIGNED NOT NULL,
  segment_seq INT UNSIGNED NOT NULL,
  segment_type VARCHAR(32) NOT NULL COMMENT 'CONST/DATE/VARIABLE/SERIAL',
  segment_value VARCHAR(128) NULL,
  date_format VARCHAR(32) NULL,
  serial_length INT UNSIGNED NULL,
  reset_cycle VARCHAR(32) NULL COMMENT 'NONE/YEAR/MONTH/DAY',
  PRIMARY KEY (segment_id),
  UNIQUE KEY uk_barcode_rule_segment_seq (rule_id, segment_seq),
  CONSTRAINT fk_barcode_segment_rule
    FOREIGN KEY (rule_id) REFERENCES barcode_rule (rule_id)
) ENGINE=InnoDB COMMENT='条码规则片段';

CREATE TABLE barcode_template (
  template_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  template_file_url VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (template_id),
  UNIQUE KEY uk_barcode_template_code (template_code)
) ENGINE=InnoDB COMMENT='标签模板表';

CREATE TABLE barcode_template_param (
  param_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  template_id BIGINT UNSIGNED NOT NULL,
  param_name VARCHAR(64) NOT NULL,
  param_source VARCHAR(128) NOT NULL COMMENT '参数来源',
  PRIMARY KEY (param_id),
  UNIQUE KEY uk_barcode_template_param (template_id, param_name),
  CONSTRAINT fk_barcode_template_param_template
    FOREIGN KEY (template_id) REFERENCES barcode_template (template_id)
) ENGINE=InnoDB COMMENT='标签模板参数';

CREATE TABLE barcode_application_rule (
  app_rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id BIGINT UNSIGNED NOT NULL,
  template_id BIGINT UNSIGNED NULL,
  object_type VARCHAR(64) NOT NULL COMMENT 'PRODUCT/MATERIAL/PACKAGE',
  object_id BIGINT UNSIGNED NULL COMMENT '具体产品或物料ID，可为空表示通用',
  source_type VARCHAR(32) NOT NULL DEFAULT 'RULE_GENERATED' COMMENT 'RULE_GENERATED/PASSED_IN/EXTERNAL_IMPORT',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (app_rule_id),
  KEY idx_barcode_app_rule_rule (rule_id),
  KEY idx_barcode_app_rule_template (template_id),
  KEY idx_barcode_app_rule_object (object_type, object_id),
  CONSTRAINT fk_barcode_app_rule_rule
    FOREIGN KEY (rule_id) REFERENCES barcode_rule (rule_id),
  CONSTRAINT fk_barcode_app_rule_template
    FOREIGN KEY (template_id) REFERENCES barcode_template (template_id)
) ENGINE=InnoDB COMMENT='条码应用规则';

CREATE TABLE barcode_record (
  barcode_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  app_rule_id BIGINT UNSIGNED NOT NULL,
  barcode_value VARCHAR(128) NOT NULL,
  object_type VARCHAR(64) NOT NULL,
  object_id BIGINT UNSIGNED NULL,
  generate_source VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  generated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  generated_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (barcode_id),
  UNIQUE KEY uk_barcode_value (barcode_value),
  KEY idx_barcode_record_object (object_type, object_id),
  KEY idx_barcode_record_app_rule (app_rule_id),
  CONSTRAINT fk_barcode_record_app_rule
    FOREIGN KEY (app_rule_id) REFERENCES barcode_application_rule (app_rule_id),
  CONSTRAINT fk_barcode_record_user
    FOREIGN KEY (generated_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='条码记录表';

CREATE TABLE label_print_log (
  print_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  barcode_id BIGINT UNSIGNED NOT NULL,
  printer_name VARCHAR(128) NULL,
  print_count INT UNSIGNED NOT NULL DEFAULT 1,
  printed_by BIGINT UNSIGNED NULL,
  print_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (print_log_id),
  KEY idx_label_print_barcode (barcode_id),
  KEY idx_label_print_user_time (printed_by, print_time),
  CONSTRAINT fk_label_print_barcode
    FOREIGN KEY (barcode_id) REFERENCES barcode_record (barcode_id),
  CONSTRAINT fk_label_print_user
    FOREIGN KEY (printed_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='标签打印日志';

CREATE TABLE package_binding (
  package_binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  parent_barcode_id BIGINT UNSIGNED NOT NULL COMMENT '箱码或栈板码',
  child_barcode_id BIGINT UNSIGNED NOT NULL COMMENT '产品码或下级箱码',
  package_level VARCHAR(32) NOT NULL COMMENT 'INNER_BOX/OUTER_BOX/PALLET',
  bound_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  bound_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (package_binding_id),
  UNIQUE KEY uk_package_binding_child (child_barcode_id),
  KEY idx_package_binding_parent (parent_barcode_id),
  CONSTRAINT fk_package_binding_parent
    FOREIGN KEY (parent_barcode_id) REFERENCES barcode_record (barcode_id),
  CONSTRAINT fk_package_binding_child
    FOREIGN KEY (child_barcode_id) REFERENCES barcode_record (barcode_id),
  CONSTRAINT fk_package_binding_user
    FOREIGN KEY (bound_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='包装层级绑定表';
