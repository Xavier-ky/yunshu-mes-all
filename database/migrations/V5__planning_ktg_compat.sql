-- yunshu planning compatibility: extend work_order / production_task, add work_order_bom

ALTER TABLE work_order
  ADD COLUMN work_order_name VARCHAR(255) NULL COMMENT '工单名称' AFTER work_order_no,
  ADD COLUMN parent_work_order_id BIGINT UNSIGNED NULL COMMENT '父工单' AFTER work_order_name,
  ADD COLUMN ancestors VARCHAR(500) NOT NULL DEFAULT '0' COMMENT '树路径' AFTER parent_work_order_id,
  ADD COLUMN work_order_type VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT 'SELF/OUTSOURCE/PURCHASE' AFTER ancestors,
  ADD COLUMN order_source VARCHAR(64) NOT NULL DEFAULT 'ORDER' COMMENT '来源类型' AFTER work_order_type,
  ADD COLUMN source_code VARCHAR(64) NULL COMMENT '来源单号' AFTER order_source,
  ADD COLUMN quantity_scheduled DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '已排产数量' AFTER completed_qty,
  ADD COLUMN quantity_changed DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '调整数量' AFTER quantity_scheduled,
  ADD COLUMN client_id BIGINT UNSIGNED NULL COMMENT '客户ID快照' AFTER quantity_changed,
  ADD COLUMN client_code VARCHAR(64) NULL COMMENT '客户编码' AFTER client_id,
  ADD COLUMN client_name VARCHAR(255) NULL COMMENT '客户名称' AFTER client_code,
  ADD COLUMN request_date DATETIME(3) NULL COMMENT '需求日期' AFTER client_name,
  ADD COLUMN finish_date DATETIME(3) NULL COMMENT '完成日期' AFTER request_date,
  ADD COLUMN cancel_date DATETIME(3) NULL COMMENT '取消日期' AFTER finish_date;

ALTER TABLE work_order
  ADD KEY idx_work_order_parent (parent_work_order_id),
  ADD CONSTRAINT fk_work_order_parent
    FOREIGN KEY (parent_work_order_id) REFERENCES work_order (work_order_id);

CREATE TABLE work_order_bom (
  line_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NOT NULL,
  material_code VARCHAR(64) NULL,
  material_name VARCHAR(255) NULL,
  specification VARCHAR(255) NULL,
  unit_code VARCHAR(32) NULL,
  item_or_product VARCHAR(20) NOT NULL DEFAULT 'ITEM' COMMENT 'ITEM/PRODUCT',
  quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (line_id),
  KEY idx_work_order_bom_wo (work_order_id),
  KEY idx_work_order_bom_material (material_id),
  CONSTRAINT fk_work_order_bom_wo
    FOREIGN KEY (work_order_id) REFERENCES work_order (work_order_id),
  CONSTRAINT fk_work_order_bom_material
    FOREIGN KEY (material_id) REFERENCES material (material_id)
) ENGINE=InnoDB COMMENT='工单BOM快照';

ALTER TABLE production_task
  MODIFY COLUMN line_id BIGINT UNSIGNED NULL,
  ADD COLUMN task_name VARCHAR(255) NULL COMMENT '任务名称' AFTER task_no,
  ADD COLUMN workstation_id BIGINT UNSIGNED NULL COMMENT '工位' AFTER line_id,
  ADD COLUMN route_id BIGINT UNSIGNED NULL COMMENT '工艺路线' AFTER workstation_id,
  ADD COLUMN step_id BIGINT UNSIGNED NULL COMMENT '工序' AFTER route_id,
  ADD COLUMN duration_minutes INT NULL COMMENT '时长分钟' AFTER step_id,
  ADD COLUMN color_code VARCHAR(32) NULL COMMENT '甘特颜色' AFTER duration_minutes,
  ADD COLUMN quantity_qualified DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '良品数' AFTER completed_qty,
  ADD COLUMN quantity_unqualified DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '不良数' AFTER quantity_qualified,
  ADD COLUMN request_date DATETIME(3) NULL COMMENT '需求日期' AFTER quantity_unqualified;

ALTER TABLE production_task
  ADD KEY idx_production_task_workstation (workstation_id),
  ADD KEY idx_production_task_step (step_id),
  ADD CONSTRAINT fk_production_task_workstation
    FOREIGN KEY (workstation_id) REFERENCES workstation (workstation_id),
  ADD CONSTRAINT fk_production_task_route
    FOREIGN KEY (route_id) REFERENCES process_route (route_id),
  ADD CONSTRAINT fk_production_task_step
    FOREIGN KEY (step_id) REFERENCES process_step (step_id);
