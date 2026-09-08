-- MySQL 8.0+
-- MES business transaction schema.

USE fan_mes;

CREATE TABLE customer_order (
  order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  customer_name VARCHAR(128) NOT NULL,
  order_date DATE NOT NULL,
  delivery_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_customer_order_no (order_no),
  KEY idx_customer_order_delivery (delivery_date, status),
  CONSTRAINT fk_customer_order_created_by
    FOREIGN KEY (created_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='客户订单主表';

CREATE TABLE customer_order_item (
  order_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  order_qty DECIMAL(18,4) NOT NULL,
  technical_requirement VARCHAR(1000) NULL,
  PRIMARY KEY (order_item_id),
  KEY idx_order_item_order (order_id),
  KEY idx_order_item_product (product_id),
  CONSTRAINT fk_order_item_order
    FOREIGN KEY (order_id) REFERENCES customer_order (order_id),
  CONSTRAINT fk_order_item_product
    FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB COMMENT='客户订单明细表';

CREATE TABLE work_order (
  work_order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_no VARCHAR(64) NOT NULL,
  order_id BIGINT UNSIGNED NULL,
  order_item_id BIGINT UNSIGNED NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  bom_id BIGINT UNSIGNED NULL,
  route_id BIGINT UNSIGNED NULL,
  plan_qty DECIMAL(18,4) NOT NULL,
  completed_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  defect_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  planned_start_time DATETIME(3) NULL,
  planned_end_time DATETIME(3) NULL,
  actual_start_time DATETIME(3) NULL,
  actual_end_time DATETIME(3) NULL,
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  version INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (work_order_id),
  UNIQUE KEY uk_work_order_no (work_order_no),
  KEY idx_work_order_order (order_id),
  KEY idx_work_order_product_status (product_id, status),
  KEY idx_work_order_plan_time (planned_start_time, planned_end_time),
  CONSTRAINT fk_work_order_order
    FOREIGN KEY (order_id) REFERENCES customer_order (order_id),
  CONSTRAINT fk_work_order_item
    FOREIGN KEY (order_item_id) REFERENCES customer_order_item (order_item_id),
  CONSTRAINT fk_work_order_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_work_order_bom
    FOREIGN KEY (bom_id) REFERENCES bom (bom_id),
  CONSTRAINT fk_work_order_route
    FOREIGN KEY (route_id) REFERENCES process_route (route_id)
) ENGINE=InnoDB COMMENT='生产工单表';

CREATE TABLE kitting_analysis (
  analysis_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  analysis_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  required_summary JSON NULL COMMENT '需求汇总',
  available_summary JSON NULL COMMENT '可用库存汇总',
  analyzed_by BIGINT UNSIGNED NULL,
  analysis_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (analysis_id),
  KEY idx_kitting_work_order (work_order_id),
  CONSTRAINT fk_kitting_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_kitting_user
    FOREIGN KEY (analyzed_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='齐套分析主表';

CREATE TABLE material_shortage (
  shortage_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  analysis_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  required_qty DECIMAL(18,4) NOT NULL,
  available_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  shortage_qty DECIMAL(18,4) NOT NULL,
  expected_arrival_time DATETIME(3) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  PRIMARY KEY (shortage_id),
  KEY idx_shortage_analysis (analysis_id),
  KEY idx_shortage_material_status (material_id, status),
  CONSTRAINT fk_shortage_analysis
    FOREIGN KEY (analysis_id) REFERENCES kitting_analysis (analysis_id),
  CONSTRAINT fk_shortage_material
    FOREIGN KEY (material_id) REFERENCES material (material_id)
) ENGINE=InnoDB COMMENT='欠料明细表';

CREATE TABLE production_task (
  task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  line_id BIGINT UNSIGNED NOT NULL,
  shift_id BIGINT UNSIGNED NULL,
  task_date DATE NOT NULL,
  task_qty DECIMAL(18,4) NOT NULL,
  completed_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  start_time DATETIME(3) NULL,
  end_time DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_production_task_no (task_no),
  KEY idx_production_task_work_order (work_order_id),
  KEY idx_production_task_line_date (line_id, task_date, status),
  CONSTRAINT fk_production_task_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_production_task_line
    FOREIGN KEY (line_id) REFERENCES production_line (line_id),
  CONSTRAINT fk_production_task_shift
    FOREIGN KEY (shift_id) REFERENCES factory_shift (shift_id)
) ENGINE=InnoDB COMMENT='生产任务单';

CREATE TABLE dispatch_task (
  dispatch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dispatch_no VARCHAR(64) NOT NULL,
  task_id BIGINT UNSIGNED NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  route_step_id BIGINT UNSIGNED NULL,
  step_id BIGINT UNSIGNED NOT NULL,
  station_id BIGINT UNSIGNED NULL,
  operator_id BIGINT UNSIGNED NULL,
  planned_qty DECIMAL(18,4) NOT NULL,
  completed_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  planned_start_time DATETIME(3) NULL,
  planned_end_time DATETIME(3) NULL,
  actual_start_time DATETIME(3) NULL,
  actual_end_time DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (dispatch_id),
  UNIQUE KEY uk_dispatch_task_no (dispatch_no),
  KEY idx_dispatch_task_task (task_id),
  KEY idx_dispatch_work_order_step (work_order_id, step_id),
  KEY idx_dispatch_operator_status (operator_id, status),
  CONSTRAINT fk_dispatch_task_task
    FOREIGN KEY (task_id) REFERENCES production_task (task_id),
  CONSTRAINT fk_dispatch_task_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_dispatch_task_route_step
    FOREIGN KEY (route_step_id) REFERENCES process_route_step (route_step_id),
  CONSTRAINT fk_dispatch_task_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id),
  CONSTRAINT fk_dispatch_task_station
    FOREIGN KEY (station_id) REFERENCES workstation (station_id),
  CONSTRAINT fk_dispatch_task_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='生产派工单';

CREATE TABLE task_operation_log (
  task_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_id BIGINT UNSIGNED NOT NULL,
  dispatch_id BIGINT UNSIGNED NULL,
  operation_type VARCHAR(32) NOT NULL COMMENT 'START/PAUSE/RESUME/FINISH/CANCEL',
  operator_id BIGINT UNSIGNED NOT NULL,
  operation_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  remark VARCHAR(500) NULL,
  PRIMARY KEY (task_log_id),
  KEY idx_task_operation_task_time (task_id, operation_time),
  CONSTRAINT fk_task_operation_task
    FOREIGN KEY (task_id) REFERENCES production_task (task_id),
  CONSTRAINT fk_task_operation_dispatch
    FOREIGN KEY (dispatch_id) REFERENCES dispatch_task (dispatch_id),
  CONSTRAINT fk_task_operation_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='任务操作日志';

CREATE TABLE warehouse (
  warehouse_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  warehouse_code VARCHAR(64) NOT NULL,
  warehouse_name VARCHAR(128) NOT NULL,
  warehouse_type VARCHAR(32) NOT NULL COMMENT 'RAW/WIP/FINISHED/SCRAP',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (warehouse_id),
  UNIQUE KEY uk_warehouse_code (warehouse_code)
) ENGINE=InnoDB COMMENT='仓库表';

CREATE TABLE storage_location (
  location_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  location_code VARCHAR(64) NOT NULL,
  location_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (location_id),
  UNIQUE KEY uk_storage_location_code (warehouse_id, location_code),
  CONSTRAINT fk_storage_location_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id)
) ENGINE=InnoDB COMMENT='库位表';

CREATE TABLE inventory_batch (
  batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_id BIGINT UNSIGNED NOT NULL,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  location_id BIGINT UNSIGNED NULL,
  batch_no VARCHAR(64) NOT NULL,
  supplier_batch_no VARCHAR(64) NULL,
  available_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  locked_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  quality_status VARCHAR(32) NOT NULL DEFAULT 'QUALIFIED',
  received_at DATETIME(3) NULL,
  expire_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_STOCK',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (batch_id),
  UNIQUE KEY uk_inventory_batch_material_batch (material_id, batch_no),
  KEY idx_inventory_batch_warehouse (warehouse_id, location_id),
  KEY idx_inventory_batch_quality (quality_status, status),
  CONSTRAINT fk_inventory_batch_material
    FOREIGN KEY (material_id) REFERENCES material (material_id),
  CONSTRAINT fk_inventory_batch_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
  CONSTRAINT fk_inventory_batch_location
    FOREIGN KEY (location_id) REFERENCES storage_location (location_id)
) ENGINE=InnoDB COMMENT='库存批次表';

CREATE TABLE material_requisition (
  requisition_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  requisition_no VARCHAR(64) NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  request_user_id BIGINT UNSIGNED NOT NULL,
  request_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  remark VARCHAR(500) NULL,
  PRIMARY KEY (requisition_id),
  UNIQUE KEY uk_requisition_no (requisition_no),
  KEY idx_requisition_work_order (work_order_id),
  CONSTRAINT fk_requisition_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_requisition_user
    FOREIGN KEY (request_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='领料单';

CREATE TABLE material_requisition_item (
  requisition_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  requisition_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  request_qty DECIMAL(18,4) NOT NULL,
  approved_qty DECIMAL(18,4) NULL,
  issued_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  PRIMARY KEY (requisition_item_id),
  KEY idx_requisition_item_requisition (requisition_id),
  KEY idx_requisition_item_material (material_id),
  CONSTRAINT fk_requisition_item_requisition
    FOREIGN KEY (requisition_id) REFERENCES material_requisition (requisition_id),
  CONSTRAINT fk_requisition_item_material
    FOREIGN KEY (material_id) REFERENCES material (material_id)
) ENGINE=InnoDB COMMENT='领料明细';

CREATE TABLE material_issue (
  issue_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_no VARCHAR(64) NOT NULL,
  requisition_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  issue_user_id BIGINT UNSIGNED NOT NULL,
  issue_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  PRIMARY KEY (issue_id),
  UNIQUE KEY uk_material_issue_no (issue_no),
  KEY idx_issue_requisition (requisition_id),
  KEY idx_issue_work_order (work_order_id),
  CONSTRAINT fk_issue_requisition
    FOREIGN KEY (requisition_id) REFERENCES material_requisition (requisition_id),
  CONSTRAINT fk_issue_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_issue_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
  CONSTRAINT fk_issue_user
    FOREIGN KEY (issue_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='发料单';

CREATE TABLE material_issue_item (
  issue_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  issue_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  issue_qty DECIMAL(18,4) NOT NULL,
  PRIMARY KEY (issue_item_id),
  KEY idx_issue_item_issue (issue_id),
  KEY idx_issue_item_batch (batch_id),
  CONSTRAINT fk_issue_item_issue
    FOREIGN KEY (issue_id) REFERENCES material_issue (issue_id),
  CONSTRAINT fk_issue_item_batch
    FOREIGN KEY (batch_id) REFERENCES inventory_batch (batch_id),
  CONSTRAINT fk_issue_item_material
    FOREIGN KEY (material_id) REFERENCES material (material_id)
) ENGINE=InnoDB COMMENT='发料明细';

CREATE TABLE material_return (
  return_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  return_no VARCHAR(64) NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  return_user_id BIGINT UNSIGNED NOT NULL,
  return_reason VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (return_id),
  UNIQUE KEY uk_material_return_no (return_no),
  KEY idx_material_return_work_order (work_order_id),
  CONSTRAINT fk_material_return_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_material_return_user
    FOREIGN KEY (return_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='退料单';

CREATE TABLE inventory_transaction (
  transaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_id BIGINT UNSIGNED NOT NULL,
  transaction_type VARCHAR(32) NOT NULL COMMENT 'INBOUND/ISSUE/RETURN/SCRAP/ADJUST/TRANSFER',
  biz_object_type VARCHAR(64) NOT NULL,
  biz_object_id BIGINT UNSIGNED NOT NULL,
  change_qty DECIMAL(18,4) NOT NULL,
  before_qty DECIMAL(18,4) NOT NULL,
  after_qty DECIMAL(18,4) NOT NULL,
  operator_id BIGINT UNSIGNED NULL,
  transaction_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  remark VARCHAR(500) NULL,
  PRIMARY KEY (transaction_id),
  KEY idx_inventory_tx_batch_time (batch_id, transaction_time),
  KEY idx_inventory_tx_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_inventory_tx_batch
    FOREIGN KEY (batch_id) REFERENCES inventory_batch (batch_id),
  CONSTRAINT fk_inventory_tx_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='库存流水';

CREATE TABLE product_sn (
  sn_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sn_code VARCHAR(128) NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  barcode_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (sn_id),
  UNIQUE KEY uk_product_sn_code (sn_code),
  KEY idx_product_sn_work_order (work_order_id),
  KEY idx_product_sn_product_status (product_id, status),
  CONSTRAINT fk_product_sn_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_product_sn_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_product_sn_barcode
    FOREIGN KEY (barcode_id) REFERENCES barcode_record (barcode_id)
) ENGINE=InnoDB COMMENT='产品序列号表';

CREATE TABLE product_process_state (
  state_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sn_id BIGINT UNSIGNED NOT NULL,
  current_step_id BIGINT UNSIGNED NULL,
  process_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
  last_report_id BIGINT UNSIGNED NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (state_id),
  UNIQUE KEY uk_product_process_state_sn (sn_id),
  KEY idx_product_process_state_step (current_step_id),
  CONSTRAINT fk_process_state_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_process_state_step
    FOREIGN KEY (current_step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='产品当前工序状态';

CREATE TABLE production_report (
  report_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_no VARCHAR(64) NOT NULL,
  dispatch_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  step_id BIGINT UNSIGNED NOT NULL,
  station_id BIGINT UNSIGNED NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  sn_id BIGINT UNSIGNED NULL,
  report_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DEFECT/INSPECTION/EQUIPMENT_COUNT/MATERIAL_BINDING/PACKAGE',
  good_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  defect_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  report_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  remark VARCHAR(500) NULL,
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_production_report_no (report_no),
  KEY idx_report_work_order_time (work_order_id, report_time),
  KEY idx_report_sn (sn_id),
  KEY idx_report_operator_time (operator_id, report_time),
  CONSTRAINT fk_report_dispatch
    FOREIGN KEY (dispatch_id) REFERENCES dispatch_task (dispatch_id),
  CONSTRAINT fk_report_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_report_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id),
  CONSTRAINT fk_report_station
    FOREIGN KEY (station_id) REFERENCES workstation (station_id),
  CONSTRAINT fk_report_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_report_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id)
) ENGINE=InnoDB COMMENT='生产报工表';

ALTER TABLE product_process_state
  ADD CONSTRAINT fk_process_state_last_report
    FOREIGN KEY (last_report_id) REFERENCES production_report (report_id);

CREATE TABLE production_report_detail (
  detail_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_id BIGINT UNSIGNED NOT NULL,
  item_type VARCHAR(64) NOT NULL,
  item_code VARCHAR(128) NOT NULL,
  item_value VARCHAR(500) NULL,
  PRIMARY KEY (detail_id),
  KEY idx_report_detail_report (report_id),
  CONSTRAINT fk_report_detail_report
    FOREIGN KEY (report_id) REFERENCES production_report (report_id)
) ENGINE=InnoDB COMMENT='报工扩展明细';

CREATE TABLE product_material_binding (
  binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sn_id BIGINT UNSIGNED NOT NULL,
  batch_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  step_id BIGINT UNSIGNED NULL,
  report_id BIGINT UNSIGNED NULL,
  bind_user_id BIGINT UNSIGNED NOT NULL,
  bind_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (binding_id),
  UNIQUE KEY uk_sn_material_batch (sn_id, material_id, batch_id),
  KEY idx_binding_batch (batch_id),
  KEY idx_binding_material (material_id),
  CONSTRAINT fk_binding_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_binding_batch
    FOREIGN KEY (batch_id) REFERENCES inventory_batch (batch_id),
  CONSTRAINT fk_binding_material
    FOREIGN KEY (material_id) REFERENCES material (material_id),
  CONSTRAINT fk_binding_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id),
  CONSTRAINT fk_binding_report
    FOREIGN KEY (report_id) REFERENCES production_report (report_id),
  CONSTRAINT fk_binding_user
    FOREIGN KEY (bind_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='产品关键物料绑定';

CREATE TABLE production_completion (
  completion_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  completion_no VARCHAR(64) NOT NULL,
  work_order_id BIGINT UNSIGNED NOT NULL,
  completed_qty DECIMAL(18,4) NOT NULL,
  defect_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  completion_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  PRIMARY KEY (completion_id),
  UNIQUE KEY uk_completion_no (completion_no),
  KEY idx_completion_work_order (work_order_id),
  CONSTRAINT fk_completion_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id)
) ENGINE=InnoDB COMMENT='生产完工单';

CREATE TABLE piece_wage_rule (
  wage_rule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NULL,
  step_id BIGINT UNSIGNED NOT NULL,
  unit_price DECIMAL(18,4) NOT NULL,
  effective_date DATE NOT NULL,
  expire_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (wage_rule_id),
  KEY idx_wage_rule_step (step_id),
  CONSTRAINT fk_wage_rule_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_wage_rule_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='工序计件工资规则';

CREATE TABLE piece_wage_record (
  wage_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_id BIGINT UNSIGNED NOT NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  wage_rule_id BIGINT UNSIGNED NULL,
  report_qty DECIMAL(18,4) NOT NULL,
  unit_price DECIMAL(18,4) NOT NULL,
  amount DECIMAL(18,4) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (wage_record_id),
  KEY idx_wage_operator_time (operator_id, created_at),
  CONSTRAINT fk_wage_report
    FOREIGN KEY (report_id) REFERENCES production_report (report_id),
  CONSTRAINT fk_wage_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_wage_rule
    FOREIGN KEY (wage_rule_id) REFERENCES piece_wage_rule (wage_rule_id)
) ENGINE=InnoDB COMMENT='计件工资记录';

CREATE TABLE inspection_item_category (
  category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_inspection_category_code (category_code)
) ENGINE=InnoDB COMMENT='检验项目分类';

CREATE TABLE inspection_item (
  item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  value_type VARCHAR(32) NOT NULL DEFAULT 'NUMBER',
  unit_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (item_id),
  UNIQUE KEY uk_inspection_item_code (item_code),
  CONSTRAINT fk_inspection_item_category
    FOREIGN KEY (category_id) REFERENCES inspection_item_category (category_id),
  CONSTRAINT fk_inspection_item_unit
    FOREIGN KEY (unit_id) REFERENCES uom (unit_id)
) ENGINE=InnoDB COMMENT='检验项目';

CREATE TABLE quality_standard (
  standard_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  standard_code VARCHAR(64) NOT NULL,
  standard_name VARCHAR(128) NOT NULL,
  customer_name VARCHAR(128) NULL,
  inspect_type VARCHAR(32) NOT NULL COMMENT 'FIRST_LAST/PATROL/FINISHED_INBOUND/SHIPPING',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (standard_id),
  UNIQUE KEY uk_quality_standard_code (standard_code),
  KEY idx_quality_standard_product (product_id, inspect_type),
  CONSTRAINT fk_quality_standard_product
    FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB COMMENT='检验标准方案';

CREATE TABLE quality_standard_item (
  standard_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  standard_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  lower_limit VARCHAR(64) NULL,
  upper_limit VARCHAR(64) NULL,
  standard_value VARCHAR(128) NULL,
  judge_rule VARCHAR(255) NULL,
  PRIMARY KEY (standard_item_id),
  UNIQUE KEY uk_standard_item (standard_id, item_id),
  CONSTRAINT fk_standard_item_standard
    FOREIGN KEY (standard_id) REFERENCES quality_standard (standard_id),
  CONSTRAINT fk_standard_item_item
    FOREIGN KEY (item_id) REFERENCES inspection_item (item_id)
) ENGINE=InnoDB COMMENT='检验标准明细';

CREATE TABLE quality_task (
  quality_task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quality_task_no VARCHAR(64) NOT NULL,
  work_order_id BIGINT UNSIGNED NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  inspect_type VARCHAR(32) NOT NULL,
  source_object_type VARCHAR(64) NULL,
  source_object_id BIGINT UNSIGNED NULL,
  standard_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (quality_task_id),
  UNIQUE KEY uk_quality_task_no (quality_task_no),
  KEY idx_quality_task_work_order (work_order_id),
  KEY idx_quality_task_source (source_object_type, source_object_id),
  CONSTRAINT fk_quality_task_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_quality_task_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_quality_task_standard
    FOREIGN KEY (standard_id) REFERENCES quality_standard (standard_id)
) ENGINE=InnoDB COMMENT='质检任务';

CREATE TABLE quality_record (
  quality_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quality_record_no VARCHAR(64) NOT NULL,
  quality_task_id BIGINT UNSIGNED NOT NULL,
  sn_id BIGINT UNSIGNED NULL,
  batch_no VARCHAR(64) NULL,
  inspector_id BIGINT UNSIGNED NOT NULL,
  inspect_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  result VARCHAR(32) NOT NULL COMMENT 'PASS/FAIL/CONCESSION',
  remark VARCHAR(500) NULL,
  PRIMARY KEY (quality_record_id),
  UNIQUE KEY uk_quality_record_no (quality_record_no),
  KEY idx_quality_record_task (quality_task_id),
  KEY idx_quality_record_sn (sn_id),
  CONSTRAINT fk_quality_record_task
    FOREIGN KEY (quality_task_id) REFERENCES quality_task (quality_task_id),
  CONSTRAINT fk_quality_record_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_quality_record_inspector
    FOREIGN KEY (inspector_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='质检记录';

CREATE TABLE quality_record_item (
  record_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quality_record_id BIGINT UNSIGNED NOT NULL,
  item_id BIGINT UNSIGNED NOT NULL,
  measured_value VARCHAR(128) NULL,
  judge_result VARCHAR(32) NOT NULL,
  remark VARCHAR(255) NULL,
  PRIMARY KEY (record_item_id),
  KEY idx_quality_record_item_record (quality_record_id),
  CONSTRAINT fk_quality_record_item_record
    FOREIGN KEY (quality_record_id) REFERENCES quality_record (quality_record_id),
  CONSTRAINT fk_quality_record_item_item
    FOREIGN KEY (item_id) REFERENCES inspection_item (item_id)
) ENGINE=InnoDB COMMENT='质检明细';

CREATE TABLE defect_record (
  defect_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  defect_no VARCHAR(64) NOT NULL,
  quality_record_id BIGINT UNSIGNED NULL,
  sn_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NULL,
  step_id BIGINT UNSIGNED NULL,
  defect_reason_id BIGINT UNSIGNED NULL,
  defect_desc VARCHAR(500) NOT NULL,
  severity VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  responsible_dept_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (defect_id),
  UNIQUE KEY uk_defect_record_no (defect_no),
  KEY idx_defect_sn (sn_id),
  KEY idx_defect_work_order (work_order_id),
  CONSTRAINT fk_defect_quality_record
    FOREIGN KEY (quality_record_id) REFERENCES quality_record (quality_record_id),
  CONSTRAINT fk_defect_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_defect_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_defect_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id),
  CONSTRAINT fk_defect_reason
    FOREIGN KEY (defect_reason_id) REFERENCES defect_reason (defect_reason_id),
  CONSTRAINT fk_defect_dept
    FOREIGN KEY (responsible_dept_id) REFERENCES sys_department (dept_id)
) ENGINE=InnoDB COMMENT='缺陷/不良记录';

CREATE TABLE rework_order (
  rework_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rework_no VARCHAR(64) NOT NULL,
  defect_id BIGINT UNSIGNED NOT NULL,
  sn_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NULL,
  rework_reason VARCHAR(500) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (rework_id),
  UNIQUE KEY uk_rework_no (rework_no),
  KEY idx_rework_defect (defect_id),
  CONSTRAINT fk_rework_defect
    FOREIGN KEY (defect_id) REFERENCES defect_record (defect_id),
  CONSTRAINT fk_rework_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_rework_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id)
) ENGINE=InnoDB COMMENT='返修单';

CREATE TABLE rework_record (
  rework_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rework_id BIGINT UNSIGNED NOT NULL,
  operator_id BIGINT UNSIGNED NOT NULL,
  repair_measure VARCHAR(1000) NOT NULL,
  rework_result VARCHAR(32) NOT NULL,
  finish_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (rework_record_id),
  KEY idx_rework_record_order (rework_id),
  CONSTRAINT fk_rework_record_order
    FOREIGN KEY (rework_id) REFERENCES rework_order (rework_id),
  CONSTRAINT fk_rework_record_operator
    FOREIGN KEY (operator_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='返修记录';

CREATE TABLE scrap_record (
  scrap_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sn_id BIGINT UNSIGNED NULL,
  defect_id BIGINT UNSIGNED NULL,
  scrap_reason VARCHAR(500) NOT NULL,
  approve_user_id BIGINT UNSIGNED NULL,
  scrap_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (scrap_id),
  KEY idx_scrap_sn (sn_id),
  CONSTRAINT fk_scrap_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_scrap_defect
    FOREIGN KEY (defect_id) REFERENCES defect_record (defect_id),
  CONSTRAINT fk_scrap_approver
    FOREIGN KEY (approve_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='报废记录';

CREATE TABLE quality_release (
  release_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  quality_record_id BIGINT UNSIGNED NOT NULL,
  release_result VARCHAR(32) NOT NULL COMMENT 'RELEASE/BLOCK/CONCESSION',
  approve_user_id BIGINT UNSIGNED NOT NULL,
  approve_opinion VARCHAR(500) NULL,
  approve_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (release_id),
  KEY idx_release_quality_record (quality_record_id),
  CONSTRAINT fk_release_quality_record
    FOREIGN KEY (quality_record_id) REFERENCES quality_record (quality_record_id),
  CONSTRAINT fk_release_approver
    FOREIGN KEY (approve_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='质量放行记录';

CREATE TABLE finished_inbound (
  inbound_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  inbound_no VARCHAR(64) NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  sn_id BIGINT UNSIGNED NULL,
  warehouse_id BIGINT UNSIGNED NOT NULL,
  quality_record_id BIGINT UNSIGNED NULL,
  inbound_qty DECIMAL(18,4) NOT NULL DEFAULT 1,
  inbound_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  PRIMARY KEY (inbound_id),
  UNIQUE KEY uk_finished_inbound_no (inbound_no),
  KEY idx_finished_inbound_sn (sn_id),
  CONSTRAINT fk_finished_inbound_product
    FOREIGN KEY (product_id) REFERENCES product (product_id),
  CONSTRAINT fk_finished_inbound_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_finished_inbound_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouse (warehouse_id),
  CONSTRAINT fk_finished_inbound_quality
    FOREIGN KEY (quality_record_id) REFERENCES quality_record (quality_record_id)
) ENGINE=InnoDB COMMENT='成品入库记录';

CREATE TABLE andon_type (
  andon_type_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  type_code VARCHAR(64) NOT NULL,
  type_name VARCHAR(128) NOT NULL,
  handle_mode VARCHAR(32) NOT NULL COMMENT 'NO_HANDLE/SELF_HANDLE/ASSIST_HANDLE',
  priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (andon_type_id),
  UNIQUE KEY uk_andon_type_code (type_code)
) ENGINE=InnoDB COMMENT='安灯类型';

CREATE TABLE andon_reason (
  reason_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reason_code VARCHAR(64) NOT NULL,
  reason_name VARCHAR(128) NOT NULL,
  reason_category VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (reason_id),
  UNIQUE KEY uk_andon_reason_code (reason_code)
) ENGINE=InnoDB COMMENT='安灯异常原因';

CREATE TABLE andon_exception_config (
  config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  andon_type_id BIGINT UNSIGNED NOT NULL,
  line_id BIGINT UNSIGNED NULL,
  receiver_role_id BIGINT UNSIGNED NULL,
  receiver_user_id BIGINT UNSIGNED NULL,
  notice_channel VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (config_id),
  KEY idx_andon_config_type_line (andon_type_id, line_id),
  CONSTRAINT fk_andon_config_type
    FOREIGN KEY (andon_type_id) REFERENCES andon_type (andon_type_id),
  CONSTRAINT fk_andon_config_line
    FOREIGN KEY (line_id) REFERENCES production_line (line_id),
  CONSTRAINT fk_andon_config_role
    FOREIGN KEY (receiver_role_id) REFERENCES sys_role (role_id),
  CONSTRAINT fk_andon_config_user
    FOREIGN KEY (receiver_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='安灯通知配置';

CREATE TABLE device_category (
  category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_code VARCHAR(64) NOT NULL,
  category_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (category_id),
  UNIQUE KEY uk_device_category_code (category_code)
) ENGINE=InnoDB COMMENT='设备类别';

CREATE TABLE device_manufacturer (
  manufacturer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  manufacturer_name VARCHAR(128) NOT NULL,
  contact_person VARCHAR(64) NULL,
  contact_phone VARCHAR(32) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (manufacturer_id)
) ENGINE=InnoDB COMMENT='设备制造商';

CREATE TABLE fault_cause (
  fault_cause_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NULL,
  cause_code VARCHAR(64) NOT NULL,
  cause_name VARCHAR(128) NOT NULL,
  prevent_measure VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (fault_cause_id),
  UNIQUE KEY uk_fault_cause_code (cause_code),
  CONSTRAINT fk_fault_cause_category
    FOREIGN KEY (category_id) REFERENCES device_category (category_id)
) ENGINE=InnoDB COMMENT='设备故障原因';

CREATE TABLE device (
  device_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_code VARCHAR(64) NOT NULL,
  device_name VARCHAR(128) NOT NULL,
  category_id BIGINT UNSIGNED NULL,
  manufacturer_id BIGINT UNSIGNED NULL,
  line_id BIGINT UNSIGNED NULL,
  station_id BIGINT UNSIGNED NULL,
  purchase_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (device_id),
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_device_line_station (line_id, station_id),
  CONSTRAINT fk_device_category
    FOREIGN KEY (category_id) REFERENCES device_category (category_id),
  CONSTRAINT fk_device_manufacturer
    FOREIGN KEY (manufacturer_id) REFERENCES device_manufacturer (manufacturer_id),
  CONSTRAINT fk_device_line
    FOREIGN KEY (line_id) REFERENCES production_line (line_id),
  CONSTRAINT fk_device_station
    FOREIGN KEY (station_id) REFERENCES workstation (station_id)
) ENGINE=InnoDB COMMENT='设备台账';

CREATE TABLE andon_event (
  andon_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  andon_no VARCHAR(64) NOT NULL,
  andon_type_id BIGINT UNSIGNED NOT NULL,
  reason_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NULL,
  sn_id BIGINT UNSIGNED NULL,
  device_id BIGINT UNSIGNED NULL,
  line_id BIGINT UNSIGNED NULL,
  station_id BIGINT UNSIGNED NULL,
  report_user_id BIGINT UNSIGNED NOT NULL,
  exception_desc VARCHAR(1000) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  occur_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  close_time DATETIME(3) NULL,
  PRIMARY KEY (andon_id),
  UNIQUE KEY uk_andon_no (andon_no),
  KEY idx_andon_status_time (status, occur_time),
  KEY idx_andon_work_order (work_order_id),
  KEY idx_andon_device (device_id),
  CONSTRAINT fk_andon_event_type
    FOREIGN KEY (andon_type_id) REFERENCES andon_type (andon_type_id),
  CONSTRAINT fk_andon_event_reason
    FOREIGN KEY (reason_id) REFERENCES andon_reason (reason_id),
  CONSTRAINT fk_andon_event_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_andon_event_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_andon_event_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_andon_event_line
    FOREIGN KEY (line_id) REFERENCES production_line (line_id),
  CONSTRAINT fk_andon_event_station
    FOREIGN KEY (station_id) REFERENCES workstation (station_id),
  CONSTRAINT fk_andon_event_report_user
    FOREIGN KEY (report_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='安灯事件';

CREATE TABLE andon_notice (
  notice_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  andon_id BIGINT UNSIGNED NOT NULL,
  receiver_id BIGINT UNSIGNED NOT NULL,
  notice_channel VARCHAR(32) NOT NULL,
  notice_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  send_time DATETIME(3) NULL,
  read_time DATETIME(3) NULL,
  PRIMARY KEY (notice_id),
  KEY idx_andon_notice_event (andon_id),
  KEY idx_andon_notice_receiver (receiver_id, notice_status),
  CONSTRAINT fk_andon_notice_event
    FOREIGN KEY (andon_id) REFERENCES andon_event (andon_id),
  CONSTRAINT fk_andon_notice_receiver
    FOREIGN KEY (receiver_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='安灯通知记录';

CREATE TABLE andon_task (
  andon_task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  andon_id BIGINT UNSIGNED NOT NULL,
  handler_id BIGINT UNSIGNED NOT NULL,
  assign_user_id BIGINT UNSIGNED NULL,
  assign_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED',
  PRIMARY KEY (andon_task_id),
  KEY idx_andon_task_event (andon_id),
  KEY idx_andon_task_handler (handler_id, status),
  CONSTRAINT fk_andon_task_event
    FOREIGN KEY (andon_id) REFERENCES andon_event (andon_id),
  CONSTRAINT fk_andon_task_handler
    FOREIGN KEY (handler_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_andon_task_assign_user
    FOREIGN KEY (assign_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='安灯处理任务';

CREATE TABLE andon_result (
  result_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  andon_task_id BIGINT UNSIGNED NOT NULL,
  handle_measure VARCHAR(1000) NOT NULL,
  result_desc VARCHAR(1000) NULL,
  close_user_id BIGINT UNSIGNED NULL,
  close_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (result_id),
  KEY idx_andon_result_task (andon_task_id),
  CONSTRAINT fk_andon_result_task
    FOREIGN KEY (andon_task_id) REFERENCES andon_task (andon_task_id),
  CONSTRAINT fk_andon_result_close_user
    FOREIGN KEY (close_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='安灯处理结果';

CREATE TABLE device_status_log (
  status_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  old_status VARCHAR(32) NULL,
  new_status VARCHAR(32) NOT NULL,
  change_reason VARCHAR(500) NULL,
  change_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  changed_by BIGINT UNSIGNED NULL,
  PRIMARY KEY (status_log_id),
  KEY idx_device_status_log_device_time (device_id, change_time),
  CONSTRAINT fk_device_status_log_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_device_status_log_user
    FOREIGN KEY (changed_by) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='设备状态日志';

CREATE TABLE maintenance_plan (
  maintenance_plan_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  plan_code VARCHAR(64) NOT NULL,
  plan_cycle VARCHAR(32) NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY',
  maintenance_items JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (maintenance_plan_id),
  UNIQUE KEY uk_maintenance_plan_code (plan_code),
  KEY idx_maintenance_plan_device (device_id),
  CONSTRAINT fk_maintenance_plan_device
    FOREIGN KEY (device_id) REFERENCES device (device_id)
) ENGINE=InnoDB COMMENT='设备保养计划';

CREATE TABLE maintenance_task (
  maintenance_task_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  maintenance_plan_id BIGINT UNSIGNED NOT NULL,
  device_id BIGINT UNSIGNED NOT NULL,
  planned_date DATE NOT NULL,
  maintainer_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  finish_time DATETIME(3) NULL,
  PRIMARY KEY (maintenance_task_id),
  KEY idx_maintenance_task_device_date (device_id, planned_date),
  KEY idx_maintenance_task_maintainer (maintainer_id, status),
  CONSTRAINT fk_maintenance_task_plan
    FOREIGN KEY (maintenance_plan_id) REFERENCES maintenance_plan (maintenance_plan_id),
  CONSTRAINT fk_maintenance_task_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_maintenance_task_user
    FOREIGN KEY (maintainer_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='设备保养任务';

CREATE TABLE device_inspection_item (
  inspection_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  category_id BIGINT UNSIGNED NULL,
  item_name VARCHAR(128) NOT NULL,
  standard_desc VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (inspection_item_id),
  KEY idx_device_inspection_item_category (category_id),
  CONSTRAINT fk_device_inspection_item_category
    FOREIGN KEY (category_id) REFERENCES device_category (category_id)
) ENGINE=InnoDB COMMENT='设备点检项目';

CREATE TABLE device_inspection_record (
  inspection_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  inspector_id BIGINT UNSIGNED NOT NULL,
  inspection_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  result VARCHAR(32) NOT NULL,
  detail_json JSON NULL,
  remark VARCHAR(500) NULL,
  PRIMARY KEY (inspection_id),
  KEY idx_device_inspection_device_time (device_id, inspection_time),
  CONSTRAINT fk_device_inspection_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_device_inspection_user
    FOREIGN KEY (inspector_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='设备点检记录';

CREATE TABLE repair_order (
  repair_order_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  repair_no VARCHAR(64) NOT NULL,
  device_id BIGINT UNSIGNED NOT NULL,
  reporter_id BIGINT UNSIGNED NOT NULL,
  fault_cause_id BIGINT UNSIGNED NULL,
  fault_desc VARCHAR(1000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
  report_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (repair_order_id),
  UNIQUE KEY uk_repair_no (repair_no),
  KEY idx_repair_device_status (device_id, status),
  CONSTRAINT fk_repair_order_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_repair_order_reporter
    FOREIGN KEY (reporter_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_repair_order_fault
    FOREIGN KEY (fault_cause_id) REFERENCES fault_cause (fault_cause_id)
) ENGINE=InnoDB COMMENT='设备报修单';

CREATE TABLE repair_record (
  repair_record_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  repair_order_id BIGINT UNSIGNED NOT NULL,
  maintainer_id BIGINT UNSIGNED NOT NULL,
  repair_solution VARCHAR(1000) NOT NULL,
  repair_result VARCHAR(32) NOT NULL,
  finish_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (repair_record_id),
  KEY idx_repair_record_order (repair_order_id),
  CONSTRAINT fk_repair_record_order
    FOREIGN KEY (repair_order_id) REFERENCES repair_order (repair_order_id),
  CONSTRAINT fk_repair_record_user
    FOREIGN KEY (maintainer_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='设备维修记录';

CREATE TABLE device_count_config (
  count_config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  signal_code VARCHAR(128) NOT NULL,
  bind_step_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (count_config_id),
  UNIQUE KEY uk_device_count_signal (device_id, signal_code),
  CONSTRAINT fk_device_count_config_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_device_count_config_step
    FOREIGN KEY (bind_step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='设备计数配置';

CREATE TABLE equipment_count_record (
  count_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  work_order_id BIGINT UNSIGNED NULL,
  step_id BIGINT UNSIGNED NULL,
  count_qty DECIMAL(18,4) NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'DEVICE',
  collect_time DATETIME(3) NOT NULL,
  PRIMARY KEY (count_id),
  KEY idx_equipment_count_device_time (device_id, collect_time),
  KEY idx_equipment_count_work_order (work_order_id),
  CONSTRAINT fk_equipment_count_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_equipment_count_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_equipment_count_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id)
) ENGINE=InnoDB COMMENT='设备计数记录';

CREATE TABLE device_oee_daily (
  oee_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  stat_date DATE NOT NULL,
  planned_time_min DECIMAL(18,4) NOT NULL DEFAULT 0,
  run_time_min DECIMAL(18,4) NOT NULL DEFAULT 0,
  stop_time_min DECIMAL(18,4) NOT NULL DEFAULT 0,
  output_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  good_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  availability_rate DECIMAL(9,6) NOT NULL DEFAULT 0,
  performance_rate DECIMAL(9,6) NOT NULL DEFAULT 0,
  quality_rate DECIMAL(9,6) NOT NULL DEFAULT 0,
  oee_rate DECIMAL(9,6) NOT NULL DEFAULT 0,
  PRIMARY KEY (oee_id),
  UNIQUE KEY uk_device_oee_daily (device_id, stat_date),
  CONSTRAINT fk_device_oee_device
    FOREIGN KEY (device_id) REFERENCES device (device_id)
) ENGINE=InnoDB COMMENT='设备OEE日统计';

CREATE TABLE energy_meter (
  meter_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  meter_code VARCHAR(64) NOT NULL,
  energy_type VARCHAR(32) NOT NULL COMMENT 'ELECTRICITY/WATER/GAS',
  unit_id BIGINT UNSIGNED NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (meter_id),
  UNIQUE KEY uk_energy_meter_code (meter_code),
  CONSTRAINT fk_energy_meter_device
    FOREIGN KEY (device_id) REFERENCES device (device_id),
  CONSTRAINT fk_energy_meter_unit
    FOREIGN KEY (unit_id) REFERENCES uom (unit_id)
) ENGINE=InnoDB COMMENT='能源计量表';

CREATE TABLE energy_reading (
  energy_reading_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  meter_id BIGINT UNSIGNED NOT NULL,
  device_id BIGINT UNSIGNED NOT NULL,
  energy_value DECIMAL(18,6) NOT NULL,
  output_qty DECIMAL(18,4) NULL,
  collect_time DATETIME(3) NOT NULL,
  PRIMARY KEY (energy_reading_id),
  KEY idx_energy_reading_meter_time (meter_id, collect_time),
  KEY idx_energy_reading_device_time (device_id, collect_time),
  CONSTRAINT fk_energy_reading_meter
    FOREIGN KEY (meter_id) REFERENCES energy_meter (meter_id),
  CONSTRAINT fk_energy_reading_device
    FOREIGN KEY (device_id) REFERENCES device (device_id)
) ENGINE=InnoDB COMMENT='能耗采集数据';

CREATE TABLE dashboard_config (
  dashboard_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dashboard_code VARCHAR(64) NOT NULL,
  dashboard_name VARCHAR(128) NOT NULL,
  dashboard_type VARCHAR(32) NOT NULL COMMENT 'LINE/WORKSHOP/CENTRAL/WECHAT',
  refresh_interval_sec INT UNSIGNED NOT NULL DEFAULT 60,
  config_json JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (dashboard_id),
  UNIQUE KEY uk_dashboard_code (dashboard_code)
) ENGINE=InnoDB COMMENT='看板配置';

CREATE TABLE dashboard_kpi_snapshot (
  snapshot_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  dashboard_id BIGINT UNSIGNED NOT NULL,
  kpi_code VARCHAR(64) NOT NULL,
  kpi_name VARCHAR(128) NOT NULL,
  kpi_value DECIMAL(18,6) NULL,
  kpi_text VARCHAR(255) NULL,
  stat_time DATETIME(3) NOT NULL,
  PRIMARY KEY (snapshot_id),
  KEY idx_dashboard_snapshot_time (dashboard_id, stat_time),
  CONSTRAINT fk_dashboard_snapshot_config
    FOREIGN KEY (dashboard_id) REFERENCES dashboard_config (dashboard_id)
) ENGINE=InnoDB COMMENT='看板KPI快照';

CREATE TABLE report_definition (
  report_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_code VARCHAR(64) NOT NULL,
  report_name VARCHAR(128) NOT NULL,
  report_type VARCHAR(32) NOT NULL COMMENT 'OUTPUT/QUALITY/OEE/TRACE/ANDON/INVENTORY',
  query_template JSON NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (report_id),
  UNIQUE KEY uk_report_code (report_code)
) ENGINE=InnoDB COMMENT='报表定义';

CREATE TABLE report_run_log (
  report_run_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_id BIGINT UNSIGNED NOT NULL,
  run_user_id BIGINT UNSIGNED NULL,
  query_condition JSON NULL,
  run_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (report_run_id),
  KEY idx_report_run_report_time (report_id, run_time),
  CONSTRAINT fk_report_run_report
    FOREIGN KEY (report_id) REFERENCES report_definition (report_id),
  CONSTRAINT fk_report_run_user
    FOREIGN KEY (run_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='报表运行日志';

CREATE TABLE trace_query_log (
  trace_query_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  query_type VARCHAR(32) NOT NULL COMMENT 'PRODUCT_SN/MATERIAL_BATCH/WORK_ORDER/QUALITY',
  query_key VARCHAR(128) NOT NULL,
  sn_id BIGINT UNSIGNED NULL,
  work_order_id BIGINT UNSIGNED NULL,
  query_user_id BIGINT UNSIGNED NULL,
  query_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  result_summary JSON NULL,
  PRIMARY KEY (trace_query_id),
  KEY idx_trace_query_key (query_type, query_key),
  KEY idx_trace_query_user_time (query_user_id, query_time),
  CONSTRAINT fk_trace_query_sn
    FOREIGN KEY (sn_id) REFERENCES product_sn (sn_id),
  CONSTRAINT fk_trace_query_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_trace_query_user
    FOREIGN KEY (query_user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='追溯查询日志';

CREATE TABLE wechat_user_binding (
  binding_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  openid VARCHAR(128) NOT NULL,
  unionid VARCHAR(128) NULL,
  bind_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (binding_id),
  UNIQUE KEY uk_wechat_openid (openid),
  KEY idx_wechat_user (user_id),
  CONSTRAINT fk_wechat_user_binding_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='微信小程序用户绑定';

CREATE TABLE mobile_operation_log (
  mobile_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  client_type VARCHAR(32) NOT NULL COMMENT 'TABLET/WECHAT/APP',
  operation_type VARCHAR(64) NOT NULL,
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  operation_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (mobile_log_id),
  KEY idx_mobile_log_user_time (user_id, operation_time),
  KEY idx_mobile_log_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_mobile_log_user
    FOREIGN KEY (user_id) REFERENCES sys_user (user_id)
) ENGINE=InnoDB COMMENT='移动端操作日志';

CREATE TABLE external_system (
  external_system_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  system_code VARCHAR(64) NOT NULL,
  system_name VARCHAR(128) NOT NULL,
  system_type VARCHAR(32) NOT NULL COMMENT 'ERP/WMS/QMS/IOT/OTHER',
  base_url VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (external_system_id),
  UNIQUE KEY uk_external_system_code (system_code)
) ENGINE=InnoDB COMMENT='外部系统';

CREATE TABLE api_endpoint (
  endpoint_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  external_system_id BIGINT UNSIGNED NULL,
  endpoint_code VARCHAR(64) NOT NULL,
  endpoint_name VARCHAR(128) NOT NULL,
  api_path VARCHAR(255) NOT NULL,
  http_method VARCHAR(16) NOT NULL,
  auth_type VARCHAR(32) NOT NULL DEFAULT 'TOKEN',
  direction VARCHAR(32) NOT NULL COMMENT 'INBOUND/OUTBOUND',
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (endpoint_id),
  UNIQUE KEY uk_api_endpoint_code (endpoint_code),
  KEY idx_api_endpoint_system (external_system_id),
  CONSTRAINT fk_api_endpoint_system
    FOREIGN KEY (external_system_id) REFERENCES external_system (external_system_id)
) ENGINE=InnoDB COMMENT='接口定义';

CREATE TABLE api_call_log (
  api_call_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  endpoint_id BIGINT UNSIGNED NOT NULL,
  request_no VARCHAR(64) NOT NULL,
  biz_object_type VARCHAR(64) NULL,
  biz_object_id BIGINT UNSIGNED NULL,
  request_payload JSON NULL,
  response_payload JSON NULL,
  call_status VARCHAR(32) NOT NULL,
  error_message VARCHAR(1000) NULL,
  latency_ms INT UNSIGNED NULL,
  call_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (api_call_id),
  UNIQUE KEY uk_api_call_request_no (request_no),
  KEY idx_api_call_endpoint_time (endpoint_id, call_time),
  KEY idx_api_call_biz (biz_object_type, biz_object_id),
  CONSTRAINT fk_api_call_endpoint
    FOREIGN KEY (endpoint_id) REFERENCES api_endpoint (endpoint_id)
) ENGINE=InnoDB COMMENT='接口调用日志';

CREATE TABLE sync_log (
  sync_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  external_system_id BIGINT UNSIGNED NOT NULL,
  sync_type VARCHAR(64) NOT NULL,
  biz_no VARCHAR(128) NOT NULL,
  sync_status VARCHAR(32) NOT NULL,
  request_payload JSON NULL,
  response_payload JSON NULL,
  error_message VARCHAR(1000) NULL,
  sync_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (sync_log_id),
  KEY idx_sync_log_system_time (external_system_id, sync_time),
  KEY idx_sync_log_biz (sync_type, biz_no),
  CONSTRAINT fk_sync_log_system
    FOREIGN KEY (external_system_id) REFERENCES external_system (external_system_id)
) ENGINE=InnoDB COMMENT='数据同步日志';

CREATE TABLE erp_work_order_map (
  map_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  erp_work_order_no VARCHAR(128) NOT NULL,
  erp_order_no VARCHAR(128) NULL,
  external_system_id BIGINT UNSIGNED NULL,
  sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  PRIMARY KEY (map_id),
  UNIQUE KEY uk_erp_work_order_no (erp_work_order_no),
  KEY idx_erp_work_order_work_order (work_order_id),
  CONSTRAINT fk_erp_work_order_map_work_order
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_erp_work_order_map_system
    FOREIGN KEY (external_system_id) REFERENCES external_system (external_system_id)
) ENGINE=InnoDB COMMENT='ERP工单映射';

CREATE TABLE api_unit_mapping (
  mapping_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  external_system_id BIGINT UNSIGNED NOT NULL,
  external_unit_code VARCHAR(64) NOT NULL,
  mes_unit_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (mapping_id),
  UNIQUE KEY uk_api_unit_mapping (external_system_id, external_unit_code),
  CONSTRAINT fk_api_unit_mapping_system
    FOREIGN KEY (external_system_id) REFERENCES external_system (external_system_id),
  CONSTRAINT fk_api_unit_mapping_unit
    FOREIGN KEY (mes_unit_id) REFERENCES uom (unit_id)
) ENGINE=InnoDB COMMENT='外部计量单位映射';

CREATE TABLE device_integration_config (
  config_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_id BIGINT UNSIGNED NOT NULL,
  protocol_type VARCHAR(32) NOT NULL COMMENT 'OPC_UA/MODBUS/MQTT/HTTP',
  gateway_code VARCHAR(64) NULL,
  signal_mapping JSON NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
  PRIMARY KEY (config_id),
  KEY idx_device_integration_device (device_id),
  CONSTRAINT fk_device_integration_device
    FOREIGN KEY (device_id) REFERENCES device (device_id)
) ENGINE=InnoDB COMMENT='设备对接配置';
