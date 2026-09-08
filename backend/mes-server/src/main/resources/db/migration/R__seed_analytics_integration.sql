-- 分析集成演示种子（可重复执行）
SET NAMES utf8mb4;

DELETE FROM ureport_file_tbl
WHERE name_ IN ('demo_output_report.ureport.xml', 'demo_quality_report.ureport.xml');

-- 正式预置模板由 UreportTemplateInitializer 从 classpath:ureport-templates 安装；
-- 勿在此 INSERT NULL content_，否则会挡住初始化器补写。

INSERT IGNORE INTO report_definition (report_code, report_name, report_type, status) VALUES
('RPT_OUTPUT', '产量统计', 'OUTPUT', 'ENABLED'),
('RPT_QUALITY', '质量分析', 'QUALITY', 'ENABLED'),
('RPT_OEE', '设备OEE', 'OEE', 'ENABLED');

INSERT IGNORE INTO report_chart (chart_id, chart_code, chart_name, chart_type, business_type, api, options, enable_flag, create_by, create_time) VALUES
(1, 'CHART_OUTPUT', '日产量趋势', 'line', 'PRODUCTION', '/api/dashboard/summary', '{"title":{"text":"日产量"}}', 'Y', 'admin', NOW()),
(2, 'CHART_QUALITY', '合格率', 'pie', 'QUALITY', '/api/dashboard/summary', '{"title":{"text":"合格率"}}', 'Y', 'admin', NOW());

INSERT IGNORE INTO report_chart_role (chart_id, role_id)
SELECT 1, role_id FROM sys_role WHERE role_code IN ('MANAGER', 'PROD_SUPERVISOR') LIMIT 2;

INSERT IGNORE INTO external_system (external_system_id, system_code, system_name, system_type, base_url, status) VALUES
(1, 'SAP_ERP', 'SAP ERP', 'ERP', 'http://erp.example.com', 'ENABLED'),
(2, 'WMS_CLOUD', '云仓 WMS', 'WMS', 'http://wms.example.com', 'ENABLED'),
(3, 'IOT_GATE', 'IoT 网关', 'IOT', 'http://iot.example.com', 'ENABLED');

INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(1, 1, 'ERP_WO_PULL', 'ERP工单拉取', '/erp/workorders', 'GET', 'TOKEN', 'INBOUND', 'ENABLED'),
(2, 1, 'ERP_WO_PUSH', 'ERP工单回写', '/erp/workorders/sync', 'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(3, 2, 'WMS_STOCK', 'WMS库存查询', '/wms/stock', 'GET', 'TOKEN', 'INBOUND', 'ENABLED'),
(4, 3, 'IOT_TELEMETRY', '设备遥测', '/iot/telemetry', 'POST', 'TOKEN', 'INBOUND', 'ENABLED');

INSERT IGNORE INTO sync_log (sync_log_id, external_system_id, sync_type, biz_no, sync_status, sync_time) VALUES
(1, 1, 'WORK_ORDER', 'WO-20260701', 'SUCCESS', NOW()),
(2, 1, 'WORK_ORDER', 'WO-20260702', 'SUCCESS', NOW()),
(3, 2, 'INVENTORY', 'BATCH-MOTOR-202607', 'SUCCESS', NOW()),
(4, 3, 'DEVICE', 'DEV-AGING-01', 'SUCCESS', NOW());

UPDATE sync_log SET biz_no = 'WO-20260701', sync_status = 'SUCCESS' WHERE sync_log_id = 1;
UPDATE sync_log SET biz_no = 'WO-20260702', sync_status = 'SUCCESS' WHERE sync_log_id = 2;
UPDATE sync_log SET biz_no = 'BATCH-MOTOR-202607', sync_status = 'SUCCESS' WHERE sync_log_id = 3;
UPDATE sync_log SET biz_no = 'DEV-AGING-01', sync_status = 'SUCCESS' WHERE sync_log_id = 4;

INSERT IGNORE INTO print_template (template_id, template_code, template_name, template_type, template_json, paper_type, template_width, template_height, is_default, enable_flag, create_by, create_time) VALUES
(1, 'LBL_SN', 'SN标签模板', 'SN', '{"panels":[]}', 'A4', 100, 50, 'Y', 'Y', 'admin', NOW()),
(2, 'LBL_BATCH', '批次标签', 'BATCH', '{"panels":[]}', 'A4', 100, 50, 'N', 'Y', 'admin', NOW());

INSERT IGNORE INTO print_client (client_id, client_code, client_name, client_ip, client_port, status, create_by, create_time) VALUES
(1, 'PC_LINE1', '1号线打印客户端', '127.0.0.1', 17521, 'OFFLINE', 'admin', NOW());

INSERT IGNORE INTO print_printer_config (printer_id, client_id, printer_code, printer_name, printer_type, enable_flag, default_flag, create_by, create_time) VALUES
(1, 1, 'PRN_01', '斑马 ZT410', 'THERMAL', 'Y', 'Y', 'admin', NOW());

-- 补全演示图表 options（含 series 数据，卡片内可渲染）
UPDATE report_chart SET api = '', options = '{"title":{"text":"日产量趋势","left":"center"},"tooltip":{"trigger":"axis"},"grid":{"left":"8%","right":"4%","bottom":"8%","containLabel":true},"xAxis":{"type":"category","data":["周一","周二","周三","周四","周五","周六","周日"]},"yAxis":{"type":"value"},"series":[{"name":"产量","type":"line","smooth":true,"data":[120,132,101,134,90,230,210]}]}' WHERE chart_id = 1;
UPDATE report_chart SET api = '', options = '{"title":{"text":"合格率","left":"center"},"tooltip":{"trigger":"item"},"legend":{"orient":"vertical","left":"left"},"series":[{"name":"合格率","type":"pie","radius":"55%","data":[{"value":95,"name":"合格"},{"value":5,"name":"不合格"}]}]}' WHERE chart_id = 2;

