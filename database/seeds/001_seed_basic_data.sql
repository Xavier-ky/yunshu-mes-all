-- Basic development seed data. Safe to rerun.

USE fan_mes;

INSERT INTO sys_department (dept_code, dept_name, dept_type, status)
VALUES
  ('MGMT', '管理部', 'MANAGEMENT', 'ENABLED'),
  ('PMC', '生产计划部', 'PMC', 'ENABLED'),
  ('PROD', '生产部', 'PRODUCTION', 'ENABLED'),
  ('WH', '仓储部', 'WAREHOUSE', 'ENABLED'),
  ('QC', '质量部', 'QUALITY', 'ENABLED'),
  ('EQ', '设备部', 'EQUIPMENT', 'ENABLED')
ON DUPLICATE KEY UPDATE dept_name = VALUES(dept_name), dept_type = VALUES(dept_type), status = VALUES(status);

-- 角色精简为 6 类（方案A）：PMC 计划并入生产主管，系统管理并入管理人员权限集
DELETE FROM sys_user_role WHERE role_id IN (SELECT role_id FROM sys_role WHERE role_code IN ('ADMIN', 'PMC_PLANNER'));
DELETE FROM sys_role WHERE role_code IN ('ADMIN', 'PMC_PLANNER');
-- 移除旧演示账号 pmc01（其职责已并入生产主管 supervisor01）
DELETE FROM sys_user_role WHERE user_id IN (SELECT user_id FROM sys_user WHERE username = 'pmc01');
DELETE FROM sys_user WHERE username = 'pmc01';

INSERT INTO sys_role (role_code, role_name, role_desc)
VALUES
  ('MANAGER', '管理人员', '查看看板、报表与追溯，并维护用户、角色、权限和系统参数'),
  ('PROD_SUPERVISOR', '生产主管', '负责订单、工单、排产、齐套、派工和现场生产管理'),
  ('WAREHOUSE_CLERK', '仓库物料员', '处理备料、领料、发料、退料和库存批次'),
  ('LINE_OPERATOR', '产线操作工人', '执行工位作业、扫码、报工和发起安灯'),
  ('QUALITY_INSPECTOR', '质检员', '执行首末件、巡检、成品检验和质量放行'),
  ('EQUIPMENT_MAINTAINER', '设备维修员', '处理设备点检、保养、报修和维修')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), role_desc = VALUES(role_desc);

INSERT INTO sys_user (username, password_hash, employee_no, real_name, dept_id, status)
VALUES
  ('admin', '{noop}admin123', 'U0001', '管理员', (SELECT dept_id FROM sys_department WHERE dept_code = 'MGMT'), 'ENABLED'),
  ('supervisor01', '{noop}123456', 'U0002', '生产主管', (SELECT dept_id FROM sys_department WHERE dept_code = 'PROD'), 'ENABLED'),
  ('warehouse01', '{noop}123456', 'U0003', '仓库物料员', (SELECT dept_id FROM sys_department WHERE dept_code = 'WH'), 'ENABLED'),
  ('qc01', '{noop}123456', 'U0004', '质检员', (SELECT dept_id FROM sys_department WHERE dept_code = 'QC'), 'ENABLED'),
  ('repair01', '{noop}123456', 'U0005', '设备维修员', (SELECT dept_id FROM sys_department WHERE dept_code = 'EQ'), 'ENABLED'),
  ('worker01', '{noop}123456', 'U0006', '产线操作工人', (SELECT dept_id FROM sys_department WHERE dept_code = 'PROD'), 'ENABLED')
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), dept_id = VALUES(dept_id), status = VALUES(status);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'MANAGER'
WHERE u.username = 'admin';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'PROD_SUPERVISOR'
WHERE u.username = 'supervisor01';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'WAREHOUSE_CLERK'
WHERE u.username = 'warehouse01';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'QUALITY_INSPECTOR'
WHERE u.username = 'qc01';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'EQUIPMENT_MAINTAINER'
WHERE u.username = 'repair01';

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_code = 'LINE_OPERATOR'
WHERE u.username = 'worker01';

INSERT INTO uom (unit_code, unit_name)
VALUES
  ('PCS', '件'),
  ('SET', '套'),
  ('BOX', '箱'),
  ('MIN', '分钟'),
  ('KWH', '千瓦时')
ON DUPLICATE KEY UPDATE unit_name = VALUES(unit_name);

INSERT INTO workshop (workshop_code, workshop_name)
VALUES ('WS-FAN-01', '电风扇总装车间')
ON DUPLICATE KEY UPDATE workshop_name = VALUES(workshop_name);

INSERT INTO production_line (workshop_id, line_code, line_name, rated_capacity, capacity_unit)
VALUES (
  (SELECT workshop_id FROM workshop WHERE workshop_code = 'WS-FAN-01'),
  'LINE-FAN-01',
  '电风扇装配一线',
  800,
  'PCS/DAY'
)
ON DUPLICATE KEY UPDATE line_name = VALUES(line_name), rated_capacity = VALUES(rated_capacity);

INSERT INTO workstation (line_id, station_code, station_name, station_type)
VALUES
  ((SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'), 'ST-01', '电机装配工位', 'ASSEMBLY'),
  ((SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'), 'ST-02', '扇叶安装工位', 'ASSEMBLY'),
  ((SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'), 'ST-03', '老化测试工位', 'TEST'),
  ((SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'), 'ST-04', '包装工位', 'PACKAGE')
ON DUPLICATE KEY UPDATE station_name = VALUES(station_name), station_type = VALUES(station_type);

INSERT INTO factory_shift (shift_code, shift_name, start_time, end_time)
VALUES ('DAY', '白班', '08:00:00', '17:00:00')
ON DUPLICATE KEY UPDATE shift_name = VALUES(shift_name), start_time = VALUES(start_time), end_time = VALUES(end_time);

INSERT INTO product (product_code, product_name, product_model, product_category)
VALUES ('FAN-FS40-A', '40cm落地电风扇A型', 'FS40-A', 'FLOOR_FAN')
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), product_model = VALUES(product_model);

INSERT INTO product_spec (product_id, spec_name, spec_value, unit_code)
VALUES
  ((SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'), '扇叶直径', '40', 'CM'),
  ((SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'), '电机功率', '55', 'W'),
  ((SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'), '颜色', '白色', NULL)
ON DUPLICATE KEY UPDATE spec_value = VALUES(spec_value), unit_code = VALUES(unit_code);

INSERT INTO material (material_code, material_name, material_type, unit_id, is_key_material)
VALUES
  ('MAT-MOTOR-55W', '55W风扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1),
  ('MAT-BLADE-40', '40cm扇叶', 'BLADE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1),
  ('MAT-SHELL-FS40', 'FS40外壳组件', 'SHELL', (SELECT unit_id FROM uom WHERE unit_code = 'SET'), 1),
  ('MAT-SCREW-M4', 'M4螺丝', 'SCREW', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0),
  ('MAT-PACK-FS40', 'FS40包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0)
ON DUPLICATE KEY UPDATE material_name = VALUES(material_name), is_key_material = VALUES(is_key_material);

INSERT INTO bom (product_id, bom_code, bom_name, version_no, status)
VALUES ((SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'), 'BOM-FAN-FS40-A', 'FS40-A标准BOM', 'V1.0', 'RELEASED')
ON DUPLICATE KEY UPDATE bom_name = VALUES(bom_name), status = VALUES(status);

INSERT INTO bom_item (bom_id, material_id, qty_per, loss_rate, is_key_material)
VALUES
  ((SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'), (SELECT material_id FROM material WHERE material_code = 'MAT-MOTOR-55W'), 1, 0.01, 1),
  ((SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'), (SELECT material_id FROM material WHERE material_code = 'MAT-BLADE-40'), 1, 0.01, 1),
  ((SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'), (SELECT material_id FROM material WHERE material_code = 'MAT-SHELL-FS40'), 1, 0.01, 1),
  ((SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'), (SELECT material_id FROM material WHERE material_code = 'MAT-SCREW-M4'), 8, 0.03, 0),
  ((SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'), (SELECT material_id FROM material WHERE material_code = 'MAT-PACK-FS40'), 1, 0.01, 0)
ON DUPLICATE KEY UPDATE qty_per = VALUES(qty_per), loss_rate = VALUES(loss_rate), is_key_material = VALUES(is_key_material);

INSERT INTO process_step (step_code, step_name, step_type, standard_time_sec, piece_price)
VALUES
  ('STEP-MOTOR', '电机装配', 'ASSEMBLY', 120, 0.25),
  ('STEP-BLADE', '扇叶安装', 'ASSEMBLY', 90, 0.20),
  ('STEP-AGING', '老化测试', 'TEST', 300, 0.15),
  ('STEP-PACK', '包装入箱', 'PACKAGE', 80, 0.18)
ON DUPLICATE KEY UPDATE step_name = VALUES(step_name), standard_time_sec = VALUES(standard_time_sec), piece_price = VALUES(piece_price);

INSERT INTO process_route (route_code, route_name, version_no, status)
VALUES ('ROUTE-FS40-A', 'FS40-A标准装配路线', 'V1.0', 'RELEASED')
ON DUPLICATE KEY UPDATE route_name = VALUES(route_name), status = VALUES(status);

INSERT IGNORE INTO process_route_step (route_id, step_id, step_seq, station_type, is_must_pass)
VALUES
  ((SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'), (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'), 10, 'ASSEMBLY', 1),
  ((SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'), (SELECT step_id FROM process_step WHERE step_code = 'STEP-BLADE'), 20, 'ASSEMBLY', 1),
  ((SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'), (SELECT step_id FROM process_step WHERE step_code = 'STEP-AGING'), 30, 'TEST', 1),
  ((SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'), (SELECT step_id FROM process_step WHERE step_code = 'STEP-PACK'), 40, 'PACKAGE', 1);

INSERT IGNORE INTO product_route (product_id, route_id, is_default, status)
VALUES ((SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'), (SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'), 1, 'ENABLED');

INSERT INTO warehouse (warehouse_code, warehouse_name, warehouse_type)
VALUES
  ('WH-RAW', '原材料仓', 'RAW'),
  ('WH-FIN', '成品仓', 'FINISHED'),
  ('WH-SCRAP', '不良品仓', 'SCRAP')
ON DUPLICATE KEY UPDATE warehouse_name = VALUES(warehouse_name), warehouse_type = VALUES(warehouse_type);

INSERT INTO storage_location (warehouse_id, location_code, location_name)
VALUES
  ((SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'), 'RAW-A01', '原材料A01库位'),
  ((SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-FIN'), 'FIN-A01', '成品A01库位'),
  ((SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-SCRAP'), 'SCRAP-A01', '不良品A01库位')
ON DUPLICATE KEY UPDATE location_name = VALUES(location_name);

INSERT INTO barcode_type (type_code, type_name, object_type)
VALUES
  ('PRODUCT_SN', '产品码', 'PRODUCT_SN'),
  ('MATERIAL_BATCH', '材料码', 'INVENTORY_BATCH'),
  ('OUTER_BOX', '外箱码', 'PACKAGE'),
  ('PALLET', '栈板码', 'PACKAGE')
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name), object_type = VALUES(object_type);

INSERT INTO andon_type (type_code, type_name, handle_mode, priority)
VALUES
  ('MATERIAL_SHORTAGE', '缺料安灯', 'ASSIST_HANDLE', 'HIGH'),
  ('QUALITY_ABNORMAL', '质量异常', 'ASSIST_HANDLE', 'HIGH'),
  ('DEVICE_FAULT', '设备故障', 'ASSIST_HANDLE', 'HIGH'),
  ('PROCESS_HELP', '工艺求助', 'ASSIST_HANDLE', 'NORMAL')
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name), handle_mode = VALUES(handle_mode), priority = VALUES(priority);

INSERT INTO andon_reason (reason_code, reason_name, reason_category)
VALUES
  ('MAT_LACK', '物料不足', 'MATERIAL'),
  ('BAD_APPEARANCE', '外观不良', 'QUALITY'),
  ('NOISE_ABNORMAL', '噪音异常', 'QUALITY'),
  ('DEVICE_STOP', '设备停机', 'DEVICE')
ON DUPLICATE KEY UPDATE reason_name = VALUES(reason_name), reason_category = VALUES(reason_category);

INSERT INTO device_category (category_code, category_name)
VALUES
  ('AGING_TEST', '老化测试设备'),
  ('SCREW_MACHINE', '螺丝机'),
  ('BALANCE_MACHINE', '动平衡测试设备')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

INSERT INTO device_manufacturer (manufacturer_name, contact_person, contact_phone)
VALUES ('默认设备厂商', '售后联系人', '000-00000000');

INSERT INTO device (device_code, device_name, category_id, manufacturer_id, line_id, station_id, status)
VALUES (
  'DEV-AGING-01',
  '老化测试台01',
  (SELECT category_id FROM device_category WHERE category_code = 'AGING_TEST'),
  (SELECT manufacturer_id FROM device_manufacturer WHERE manufacturer_name = '默认设备厂商' LIMIT 1),
  (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'),
  (SELECT station_id FROM workstation WHERE station_code = 'ST-03'),
  'NORMAL'
)
ON DUPLICATE KEY UPDATE device_name = VALUES(device_name), status = VALUES(status);

INSERT INTO llm_provider (provider_code, provider_name, auth_type, status)
VALUES ('OPENAI_COMPATIBLE', 'OpenAI兼容大模型服务', 'API_KEY', 'ENABLED')
ON DUPLICATE KEY UPDATE provider_name = VALUES(provider_name), status = VALUES(status);

INSERT INTO llm_model (provider_id, model_code, model_name, model_type, context_window)
VALUES
  ((SELECT provider_id FROM llm_provider WHERE provider_code = 'OPENAI_COMPATIBLE'), 'default-chat-model', '默认对话模型', 'CHAT', 128000),
  ((SELECT provider_id FROM llm_provider WHERE provider_code = 'OPENAI_COMPATIBLE'), 'default-embedding-model', '默认向量模型', 'EMBEDDING', 8192)
ON DUPLICATE KEY UPDATE model_name = VALUES(model_name), model_type = VALUES(model_type), context_window = VALUES(context_window);

INSERT INTO agent_profile (agent_code, agent_name, agent_type, owner_dept_id, description, status)
VALUES
  ('AGENT_PLANNING', '生产计划Agent', 'PLANNING', (SELECT dept_id FROM sys_department WHERE dept_code = 'PMC'), '根据订单、产线产能、工厂日历和齐套情况给出排产建议', 'ENABLED'),
  ('AGENT_KITTING', '齐套与仓储Agent', 'KITTING', (SELECT dept_id FROM sys_department WHERE dept_code = 'WH'), '分析物料齐套、欠料原因和备料优先级', 'ENABLED'),
  ('AGENT_QUALITY', '质量分析Agent', 'QUALITY', (SELECT dept_id FROM sys_department WHERE dept_code = 'QC'), '分析质检、不良、返修和质量趋势', 'ENABLED'),
  ('AGENT_EQUIPMENT', '设备维修Agent', 'EQUIPMENT', (SELECT dept_id FROM sys_department WHERE dept_code = 'EQ'), '分析设备故障、点检、维修和OEE', 'ENABLED'),
  ('AGENT_TRACE', '追溯问答Agent', 'TRACE', (SELECT dept_id FROM sys_department WHERE dept_code = 'MGMT'), '根据产品码、物料批次和工单查询全流程追溯链路', 'ENABLED')
ON DUPLICATE KEY UPDATE agent_name = VALUES(agent_name), owner_dept_id = VALUES(owner_dept_id), description = VALUES(description), status = VALUES(status);

INSERT INTO agent_capability (capability_code, capability_name, capability_type, description)
VALUES
  ('RAG', '知识库检索增强', 'RAG', '从SOP、维修手册、质量标准和历史报告中检索依据'),
  ('TOOL_CALL', '业务工具调用', 'TOOL_CALL', '调用MES查询、报表和工单工具'),
  ('RISK_ALERT', '风险预警', 'ANALYSIS', '生成质量、设备、欠料和交期风险预警'),
  ('WORKFLOW', '多Agent工作流', 'WORKFLOW', '串联多个Agent完成复杂分析')
ON DUPLICATE KEY UPDATE capability_name = VALUES(capability_name), capability_type = VALUES(capability_type);

INSERT IGNORE INTO agent_capability_map (agent_id, capability_id, enable_status)
SELECT a.agent_id, c.capability_id, 'ENABLED'
FROM agent_profile a
CROSS JOIN agent_capability c
WHERE a.agent_code IN ('AGENT_PLANNING', 'AGENT_KITTING', 'AGENT_QUALITY', 'AGENT_EQUIPMENT', 'AGENT_TRACE');

INSERT INTO agent_connector (connector_code, connector_name, connector_type, auth_type, status)
VALUES
  ('MES_DB', 'MES数据库连接器', 'MES_DB', 'SYSTEM', 'ENABLED'),
  ('MES_API', 'MES业务接口连接器', 'MES_API', 'TOKEN', 'ENABLED'),
  ('VECTOR_STORE', '向量知识库连接器', 'VECTOR_STORE', 'API_KEY', 'ENABLED')
ON DUPLICATE KEY UPDATE connector_name = VALUES(connector_name), status = VALUES(status);

INSERT INTO agent_tool (connector_id, tool_code, tool_name, tool_type, risk_level)
VALUES
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_DB'), 'query_work_order', '查询生产工单', 'QUERY', 'LOW'),
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_DB'), 'query_inventory_shortage', '查询齐套欠料', 'QUERY', 'LOW'),
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_DB'), 'query_product_trace', '查询产品追溯', 'QUERY', 'LOW'),
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_DB'), 'query_quality_defects', '查询质量不良', 'QUERY', 'LOW'),
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_API'), 'create_andon_task', '创建安灯处理任务', 'COMMAND', 'HIGH'),
  ((SELECT connector_id FROM agent_connector WHERE connector_code = 'MES_API'), 'update_work_order_status', '更新工单状态', 'COMMAND', 'HIGH')
ON DUPLICATE KEY UPDATE tool_name = VALUES(tool_name), tool_type = VALUES(tool_type), risk_level = VALUES(risk_level);
