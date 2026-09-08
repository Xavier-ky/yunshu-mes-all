-- 集成中心 Hub 演示数据扩充（外部系统 / 接口定义 / 同步日志）
-- 可重复执行：INSERT IGNORE + 按编码补全
SET NAMES utf8mb4;

-- ========== 外部系统（新增 3 个，共 6 个）==========
INSERT IGNORE INTO external_system (external_system_id, system_code, system_name, system_type, base_url, status) VALUES
(4, 'PLM_CLOUD', 'PLM 产品数据', 'PLM', 'http://plm.example.com', 'ENABLED'),
(5, 'QMS_CLOUD', '质量云平台', 'QMS', 'http://qms.example.com', 'ENABLED'),
(6, 'HR_ATTEND', '人事考勤系统', 'HR', 'http://hr.example.com', 'ENABLED');

-- ========== 接口定义 — SAP ERP（system_id=1）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(5,  1, 'ERP_BOM_SYNC',      'BOM主数据同步',     '/erp/bom/sync',           'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(6,  1, 'ERP_MATERIAL_PULL', '物料主数据拉取',   '/erp/materials',          'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(7,  1, 'ERP_FINISH_PUSH',   '完工入库回写',     '/erp/finished-goods',     'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(8,  1, 'ERP_ORDER_PULL',    '销售订单拉取',     '/erp/sales-orders',       'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(9,  1, 'ERP_SN_PUSH',       '序列号回传',       '/erp/serial-numbers',     'POST', 'TOKEN', 'OUTBOUND', 'ENABLED');

-- ========== 接口定义 — 云仓 WMS（system_id=2）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(10, 2, 'WMS_INBOUND',   '采购入库同步',   '/wms/inbound/receipt',    'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(11, 2, 'WMS_OUTBOUND',  '生产领料下发',   '/wms/outbound/issue',     'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(12, 2, 'WMS_TRANSFER',  '库间调拨',       '/wms/transfer',           'PUT',  'TOKEN', 'OUTBOUND', 'ENABLED'),
(13, 2, 'WMS_STOCK_LOCK','库存锁定',       '/wms/stock/lock',         'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(14, 2, 'WMS_BATCH',     '批次属性查询',   '/wms/batches',            'GET',  'TOKEN', 'INBOUND',  'ENABLED');

-- ========== 接口定义 — IoT 网关（system_id=3）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(15, 3, 'IOT_ALARM',     '设备告警上报',   '/iot/alarms',             'POST', 'TOKEN', 'INBOUND',  'ENABLED'),
(16, 3, 'IOT_OEE',       'OEE 数据推送',   '/iot/oee/daily',          'POST', 'TOKEN', 'INBOUND',  'ENABLED'),
(17, 3, 'IOT_STATUS',    '设备状态心跳',   '/iot/devices/status',     'POST', 'TOKEN', 'INBOUND',  'ENABLED'),
(18, 3, 'IOT_CMD',       '远程启停指令',   '/iot/devices/command',    'POST', 'TOKEN', 'OUTBOUND', 'DISABLED');

-- ========== 接口定义 — PLM（system_id=4）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(19, 4, 'PLM_PRODUCT',   '产品图纸同步',   '/plm/products/drawing',   'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(20, 4, 'PLM_BOM',       '工程 BOM 下发',  '/plm/bom/engineering',    'POST', 'TOKEN', 'INBOUND',  'ENABLED'),
(21, 4, 'PLM_ECO',       '工程变更单',     '/plm/eco/notice',         'POST', 'TOKEN', 'INBOUND',  'ENABLED'),
(22, 4, 'PLM_SOP',       'SOP 文档拉取',   '/plm/sop/documents',      'GET',  'TOKEN', 'INBOUND',  'ENABLED');

-- ========== 接口定义 — 质量云（system_id=5）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(23, 5, 'QMS_IQC_PUSH',  '来料检验结果回传', '/qms/iqc/results',      'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(24, 5, 'QMS_IPQC_PUSH', '过程检验结果回传', '/qms/ipqc/results',     'POST', 'TOKEN', 'OUTBOUND', 'ENABLED'),
(25, 5, 'QMS_DEFECT',    '缺陷代码同步',     '/qms/defect-codes',     'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(26, 5, 'QMS_RELEASE',   '质量放行通知',     '/qms/release/notify',   'POST', 'TOKEN', 'OUTBOUND', 'ENABLED');

-- ========== 接口定义 — 人事考勤（system_id=6）==========
INSERT IGNORE INTO api_endpoint (endpoint_id, external_system_id, endpoint_code, endpoint_name, api_path, http_method, auth_type, direction, status) VALUES
(27, 6, 'HR_ATTEND_PULL','考勤打卡同步',   '/hr/attendance/shifts',   'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(28, 6, 'HR_SKILL',      '技能矩阵查询',   '/hr/skills/matrix',       'GET',  'TOKEN', 'INBOUND',  'ENABLED'),
(29, 6, 'HR_PIECE',      '计件工资回写',   '/hr/piece-wage/sync',     'POST', 'TOKEN', 'OUTBOUND', 'ENABLED');

-- ========== 同步日志（扩充至 24 条，覆盖近 7 天）==========
INSERT IGNORE INTO sync_log (sync_log_id, external_system_id, sync_type, biz_no, sync_status, sync_time) VALUES
(5,  1, 'WORK_ORDER',  'WO-20260707',           'SUCCESS', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6,  1, 'WORK_ORDER',  'WO-20260708',           'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(7,  1, 'BOM',         'BOM-FAN-TS30-B',        'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(8,  1, 'MATERIAL',    'MAT-MOTOR-001',         'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(9,  1, 'FINISH',      'WO-20260705',           'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(10, 1, 'WORK_ORDER',  'WO-20260710',           'FAILED',  DATE_SUB(NOW(), INTERVAL 2 DAY)),
(11, 1, 'SN',          'SN-FS40-20260711-001',  'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(12, 2, 'INVENTORY',   'BATCH-MOTOR-202607',    'SUCCESS', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(13, 2, 'INBOUND',     'IR-STORY-001',          'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(14, 2, 'OUTBOUND',    'IS-STORY-001',          'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(15, 2, 'OUTBOUND',    'IS-STORY-002',          'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(16, 2, 'TRANSFER',    'TF-WH01-WH02-001',      'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(17, 2, 'INVENTORY',   'BATCH-BLADE-202607',    'FAILED',  DATE_SUB(NOW(), INTERVAL 1 DAY)),
(18, 3, 'DEVICE',      'DEV-AGING-01',          'SUCCESS', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(19, 3, 'DEVICE',      'DEV-PRESS-01',          'SUCCESS', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(20, 3, 'OEE',         'LINE-FAN-02',           'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(21, 3, 'ALARM',       'ALM-ST05-20260712',     'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(22, 4, 'PLM_BOM',     'BOM-FAN-WS20-C',        'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(23, 4, 'PLM_ECO',     'ECO-2026-0712',         'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(24, 5, 'IQC',         'IQC-STORY-001',         'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(25, 5, 'IPQC',        'IPQC-STORY-002',        'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(26, 5, 'DEFECT',      'DEF-CODE-SYNC',         'SUCCESS', NOW()),
(27, 6, 'ATTENDANCE',  'SHIFT-DAY-20260712',    'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(28, 6, 'PIECE_WAGE',  'PW-WO-20260701',        'SUCCESS', NOW());

-- ERP 工单映射（与集成故事呼应）
INSERT IGNORE INTO erp_work_order_map (map_id, work_order_id, erp_work_order_no, erp_order_no, external_system_id, sync_status)
SELECT v.mid, wo.work_order_id, v.erp_wo, v.erp_order, 1, v.sync_status
FROM (
  SELECT 1 AS mid, 'WO-20260701' AS wo, 'ERP-WO-88001' AS erp_wo, 'SO-20260701' AS erp_order, 'SYNCED' AS sync_status
  UNION ALL SELECT 2, 'WO-20260707', 'ERP-WO-88007', 'SO-20260706', 'SYNCED'
  UNION ALL SELECT 3, 'WO-20260708', 'ERP-WO-88008', 'SO-20260707', 'SYNCED'
  UNION ALL SELECT 4, 'WO-20260710', 'ERP-WO-88010', 'SO-20260703', 'PENDING'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
WHERE NOT EXISTS (SELECT 1 FROM erp_work_order_map m WHERE m.map_id = v.mid);
