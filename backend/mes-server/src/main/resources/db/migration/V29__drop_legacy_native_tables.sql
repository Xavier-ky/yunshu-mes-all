-- V29: 退役 Native V2 重复表与无引用 stub 表（保留黄金10步、Compat WM/QC、Agent、扩展 WM）
-- 删表前解除 inventory_batch → storage_location 外键，并清空 legacy location_id

SET NAMES utf8mb4;

-- 解除 inventory_batch 对 storage_location 的 FK（storage_bin.legacy_location_id 已保留映射）
ALTER TABLE inventory_batch DROP FOREIGN KEY fk_inventory_batch_location;
UPDATE inventory_batch SET location_id = NULL WHERE location_id IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 0;

-- Phase 1: stub / 无运行时引用
DROP TABLE IF EXISTS ai_chat_message;
DROP TABLE IF EXISTS ai_chat_session;
DROP TABLE IF EXISTS quality_record_item;
DROP TABLE IF EXISTS product_process_state;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS report_run_log;
DROP TABLE IF EXISTS dashboard_kpi_snapshot;
DROP TABLE IF EXISTS dashboard_config;
DROP TABLE IF EXISTS storage_location;
DROP TABLE IF EXISTS defect_reason;
DROP TABLE IF EXISTS sop_file;
DROP TABLE IF EXISTS piece_wage_record;
DROP TABLE IF EXISTS piece_wage_rule;
DROP TABLE IF EXISTS mobile_operation_log;
DROP TABLE IF EXISTS wechat_user_binding;
DROP TABLE IF EXISTS energy_reading;
DROP TABLE IF EXISTS energy_meter;

-- Phase 2+3: Native 重复（仓储 / 生产 / 质检 / 设备 / 条码 / 返工）
DROP TABLE IF EXISTS material_issue_item;
DROP TABLE IF EXISTS material_issue;
DROP TABLE IF EXISTS material_requisition_item;
DROP TABLE IF EXISTS material_requisition;
DROP TABLE IF EXISTS material_return;
DROP TABLE IF EXISTS inventory_transaction;
DROP TABLE IF EXISTS finished_inbound;
DROP TABLE IF EXISTS production_completion;

DROP TABLE IF EXISTS quality_release;
DROP TABLE IF EXISTS quality_record_item;
DROP TABLE IF EXISTS defect_record;
DROP TABLE IF EXISTS rework_record;
DROP TABLE IF EXISTS scrap_record;
DROP TABLE IF EXISTS rework_order;
DROP TABLE IF EXISTS quality_record;
DROP TABLE IF EXISTS quality_task;
DROP TABLE IF EXISTS quality_standard_item;
DROP TABLE IF EXISTS quality_standard;
DROP TABLE IF EXISTS inspection_item;
DROP TABLE IF EXISTS inspection_item_category;

DROP TABLE IF EXISTS label_print_log;
DROP TABLE IF EXISTS package_binding;
DROP TABLE IF EXISTS barcode_record;
DROP TABLE IF EXISTS barcode_application_rule;
DROP TABLE IF EXISTS barcode_template_param;
DROP TABLE IF EXISTS barcode_template;
DROP TABLE IF EXISTS barcode_rule_segment;
DROP TABLE IF EXISTS barcode_rule;
DROP TABLE IF EXISTS barcode_type;

DROP TABLE IF EXISTS device_oee_daily;
DROP TABLE IF EXISTS equipment_count_record;
DROP TABLE IF EXISTS device_count_config;
DROP TABLE IF EXISTS repair_record;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS device_inspection_record;
DROP TABLE IF EXISTS device_inspection_item;
DROP TABLE IF EXISTS maintenance_task;
DROP TABLE IF EXISTS maintenance_plan;
DROP TABLE IF EXISTS device_status_log;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS fault_cause;
DROP TABLE IF EXISTS device_manufacturer;
DROP TABLE IF EXISTS device_category;

SET FOREIGN_KEY_CHECKS = 1;
