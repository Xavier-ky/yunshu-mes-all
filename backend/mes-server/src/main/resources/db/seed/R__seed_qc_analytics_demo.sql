-- 质量分析中心演示数据：待检源单 + 7日完成趋势 + 缺陷TOP + 待处置（幂等）
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 1. 修正待检源单状态（使 pending API 能查到）==========
UPDATE wm_rt_issue_line l
JOIN wm_rt_issue h ON h.rt_id = l.rt_id
SET l.quality_status = 'NT'
WHERE h.rt_code IN ('RT-STORY-002', 'RT-DEMO-001')
  AND (l.quality_status IS NULL OR l.quality_status = '' OR l.quality_status = 'PENDING');

UPDATE wm_rt_issue h SET h.status = 'UNCHECK'
WHERE h.rt_code = 'RT-STORY-002' AND h.status = 'PREPARE';

UPDATE wm_rt_sales_line l
JOIN wm_rt_sales h ON h.rt_id = l.rt_id
SET l.quality_status = 'NT'
WHERE h.rt_code IN ('RS-STORY-002', 'RTS-DEMO-QC')
  AND (l.quality_status IS NULL OR l.quality_status = '' OR l.quality_status = 'PENDING');

UPDATE wm_rt_sales h SET h.status = 'UNCHECK'
WHERE h.rt_code = 'RS-STORY-002' AND h.status = 'PREPARE';

UPDATE wm_product_sales_line l
JOIN wm_product_sales h ON h.sales_id = l.sales_id
SET l.oqc_id = NULL, l.oqc_code = NULL, l.quality_status = 'NT'
WHERE h.sales_code IN ('PS-STORY-002', 'PS-STORY-003')
  AND h.status NOT IN ('FINISHED', 'CANCELED');

UPDATE wm_product_produce_line l
JOIN wm_product_produce p ON p.record_id = l.record_id
SET l.quality_status = 'NT'
WHERE p.status NOT IN ('FINISHED', 'CANCELED')
  AND (l.quality_status IS NULL OR l.quality_status = '');

UPDATE pro_feedback f
SET f.status = 'FINISHED',
    f.quantity_uncheck = IFNULL(f.quantity_uncheck, IFNULL(f.quantity_feedback, f.quantity))
WHERE f.feedback_code IN ('FB-STORY-011', 'FB-STORY-015', 'FB-STORY-008')
  AND IFNULL(f.quantity_uncheck, 0) = 0
  AND NOT EXISTS (SELECT 1 FROM wm_product_produce pp WHERE pp.feedback_id = f.record_id);

-- ========== 2. 到货通知 IQC 待检（5 单）==========
INSERT INTO wm_arrival_notice (notice_code, notice_name, po_code, vendor_id, vendor_code, vendor_name, vendor_nick, arrival_date, status, create_by, create_time)
SELECT v.notice_code, v.notice_name, v.po_code, mv.vendor_id, mv.vendor_code, mv.vendor_name, mv.vendor_nick, v.arrival_date, 'APPROVING', 'qc01', NOW(3)
FROM (
  SELECT 'AN-QC-001' AS notice_code, '55W电机到货待检' AS notice_name, 'PO-20260701' AS po_code, 'V001' AS vc, DATE_SUB(NOW(), INTERVAL 2 HOUR) AS arrival_date
  UNION ALL SELECT 'AN-QC-002', '30cm扇叶到货待检', 'PO-20260702', 'V002', DATE_SUB(NOW(), INTERVAL 5 HOUR)
  UNION ALL SELECT 'AN-QC-003', 'M4螺丝到货待检', 'PO-20260703', 'V004', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'AN-QC-004', 'TS30外壳到货待检', 'PO-20260704', 'V003', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'AN-QC-005', '40W电机到货待检', 'PO-20260705', 'V001', NOW()
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
WHERE NOT EXISTS (SELECT 1 FROM wm_arrival_notice n WHERE n.notice_code = v.notice_code);

INSERT INTO wm_arrival_notice_line (notice_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, quantity_arrival, iqc_check, create_time)
SELECT n.notice_id, mi.item_id, mi.item_code, mi.item_name, mi.specification, mi.unit_of_measure, IFNULL(u.unit_name, '件'), v.qty, 'Y', NOW(3)
FROM (
  SELECT 'AN-QC-001' AS nc, 'MAT-MOTOR-55W' AS mc, 500 AS qty
  UNION ALL SELECT 'AN-QC-002', 'MAT-BLADE-30', 800
  UNION ALL SELECT 'AN-QC-003', 'MAT-SCREW-M4', 5000
  UNION ALL SELECT 'AN-QC-004', 'MAT-SHELL-TS30', 600
  UNION ALL SELECT 'AN-QC-005', 'MAT-MOTOR-40W', 320
) v
JOIN wm_arrival_notice n ON n.notice_code = v.nc
JOIN md_item mi ON mi.item_code = v.mc
LEFT JOIN uom u ON u.unit_code = mi.unit_of_measure OR u.unit_name = mi.unit_of_measure
WHERE NOT EXISTS (SELECT 1 FROM wm_arrival_notice_line l WHERE l.notice_id = n.notice_id AND l.item_code = v.mc);

-- 修正 V17 演示到货行物料 ID
UPDATE wm_arrival_notice_line l
JOIN wm_arrival_notice n ON n.notice_id = l.notice_id
JOIN md_item mi ON mi.item_code = 'MAT-MOTOR-55W'
SET l.item_id = mi.item_id, l.item_code = mi.item_code, l.item_name = mi.item_name, l.iqc_check = 'Y', l.iqc_id = NULL
WHERE n.notice_code = 'AN-DEMO-001';

-- ========== 3. 外协入库 IQC 待检 ==========
INSERT INTO wm_outsource_recpt (recpt_code, recpt_name, workorder_id, workorder_code, vendor_id, vendor_code, vendor_name, recpt_date, status, create_by, create_time)
SELECT v.recpt_code, v.recpt_name, wo.work_order_id, wo.work_order_no, mv.vendor_id, mv.vendor_code, mv.vendor_name, NOW(3), 'UNCHECK', 'qc01', NOW(3)
FROM (
  SELECT 'OR-QC-001' AS recpt_code, 'WO-20260707扇叶外协待检' AS recpt_name, 'WO-20260707' AS wo, 'V002' AS vc
  UNION ALL SELECT 'OR-QC-002', 'WO-20260708电机外协待检', 'WO-20260708', 'V001'
  UNION ALL SELECT 'OR-QC-003', 'WO-20260711外协件待检', 'WO-20260711', 'V003'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN md_vendor mv ON mv.vendor_code = v.vc
WHERE NOT EXISTS (SELECT 1 FROM wm_outsource_recpt r WHERE r.recpt_code = v.recpt_code);

INSERT INTO wm_outsource_recpt_line (recpt_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_recived, batch_code, quality_status, iqc_check, create_time)
SELECT r.recpt_id, mi.item_id, mi.item_code, mi.item_name, 'PCS', '件', v.qty, v.batch_code, 'NT', 'Y', NOW(3)
FROM (
  SELECT 'OR-QC-001' AS rc, 'MAT-BLADE-30' AS mc, 120 AS qty, 'BATCH-OQC-001' AS batch_code
  UNION ALL SELECT 'OR-QC-002', 'MAT-MOTOR-40W', 80, 'BATCH-OQC-002'
  UNION ALL SELECT 'OR-QC-003', 'MAT-SHELL-TS30', 60, 'BATCH-OQC-003'
) v
JOIN wm_outsource_recpt r ON r.recpt_code = v.rc
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM wm_outsource_recpt_line l WHERE l.recpt_id = r.recpt_id AND l.item_code = v.mc);

-- ========== 4. 产出行 PQC 待检 ==========
INSERT INTO wm_product_produce (record_id, workorder_id, workorder_code, workorder_name, workstation_id, workstation_code, workstation_name,
  process_id, process_code, process_name, produce_date, status, remark, create_by, create_time)
SELECT v.rid, wo.work_order_id, wo.work_order_no, p.product_name,
  ws.station_id, ws.station_code, ws.station_name,
  ps.step_id, ps.step_code, ps.step_name, NOW(3), 'PREPARE', v.remark, 'worker01', NOW(3)
FROM (
  SELECT 93001 AS rid, 'WO-20260701' AS wo, 'ST-03' AS st, 'STEP-AGING' AS step, 'FS40老化产出待检' AS remark
  UNION ALL SELECT 93002, 'WO-20260707', 'ST-06', 'STEP-AGING', 'TS30整机测试产出待检'
  UNION ALL SELECT 93003, 'WO-20260708', 'ST-07', 'STEP-MOTOR', 'WS20电机产出待检'
  UNION ALL SELECT 93004, 'WO-20260711', 'ST-07', 'STEP-MOTOR', '壁扇装配产出待检'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN workstation ws ON ws.station_code = v.st
JOIN process_step ps ON ps.step_code = v.step
WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce pp WHERE pp.record_id = v.rid);

INSERT INTO wm_product_produce_line (line_id, record_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity_produce, batch_code, quality_status, create_by, create_time)
SELECT v.lid, v.rid, mi.item_id, mi.item_code, mi.item_name, 'PCS', '件', v.qty, v.batch, 'NT', 'worker01', NOW(3)
FROM (
  SELECT 930001 AS lid, 93001 AS rid, 'FAN-FS40-A' AS pc, 45 AS qty, 'PB-FS40-PQC-001' AS batch
  UNION ALL SELECT 930002, 93002, 'FAN-TS30-B', 38, 'PB-TS30-PQC-001'
  UNION ALL SELECT 930003, 93003, 'FAN-WS20-C', 22, 'PB-WS20-PQC-001'
  UNION ALL SELECT 930004, 93004, 'FAN-WS20-C', 18, 'PB-WS20-PQC-002'
) v
JOIN md_item mi ON mi.item_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM wm_product_produce_line l WHERE l.line_id = v.lid);

-- ========== 5. 报工待检 PQC（quantity_uncheck）==========
INSERT INTO pro_feedback (
  feedback_type, feedback_code, workstation_id, workstation_code, workstation_name,
  workorder_id, workorder_code, workorder_name, route_id, process_id, process_code, process_name,
  task_id, task_code, item_id, item_code, item_name, unit_of_measure,
  quantity, quantity_feedback, quantity_qualified, quantity_unquanlified, quantity_uncheck,
  user_name, nick_name, feedback_time, status, remark, create_by, create_time
)
SELECT v.ftype, v.fcode, ws.station_id, ws.station_code, ws.station_name,
  wo.work_order_id, wo.work_order_no, p.product_name,
  IFNULL(pr.route_id, 0), ps.step_id, ps.step_code, ps.step_name,
  pt.task_id, pt.task_no, mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty, v.qty, v.q_ok, v.q_bad, v.q_uncheck,
  'worker01', u.real_name, NOW(3), 'FINISHED', v.remark, 'worker01', NOW(3)
FROM (
  SELECT 'SELF' AS ftype, 'FB-QC-UNCHECK-001' AS fcode, 'WO-20260704' AS wo, 'STEP-PACK' AS step, 'ST-04' AS st, 'PT-20260704' AS pt,
    50 AS qty, 48 AS q_ok, 2 AS q_bad, 50 AS q_uncheck, '包装报工待过程检' AS remark
  UNION ALL SELECT 'SELF', 'FB-QC-UNCHECK-002', 'WO-20260703', 'STEP-AGING', 'ST-06', 'PT-20260703', 35, 34, 1, 35, '整机老化报工待检'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
JOIN process_step ps ON ps.step_code = v.step
JOIN workstation ws ON ws.station_code = v.st
LEFT JOIN production_task pt ON pt.task_no = v.pt
LEFT JOIN process_route pr ON pr.route_id = wo.route_id
CROSS JOIN sys_user u
WHERE u.username = 'worker01'
  AND NOT EXISTS (SELECT 1 FROM pro_feedback f WHERE f.feedback_code = v.fcode);

-- ========== 6. 退料 RQC 待检 ==========
INSERT INTO wm_rt_issue (rt_code, rt_name, workorder_id, workorder_code, workstation_id, workstation_code, workstation_name, rt_type, rt_date, status, create_time)
SELECT v.rt_code, v.rt_name, wo.work_order_id, wo.work_order_no, ws.station_id, ws.station_code, ws.station_name, v.rt_type, NOW(), v.status, NOW()
FROM (
  SELECT 'RT-QC-001' AS rt_code, 'WO-20260704包装余料退检' AS rt_name, 'WO-20260704' AS wo, 'ST-04' AS st, '余料' AS rt_type, 'UNCHECK' AS status
  UNION ALL SELECT 'RT-QC-002', 'WO-20260708电机不良退检', 'WO-20260708', 'ST-07', '不良', 'PREPARE'
  UNION ALL SELECT 'RT-QC-003', 'WO-20260711线边退检', 'WO-20260711', 'ST-07', '余料', 'UNCHECK'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN workstation ws ON ws.station_code = v.st
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_issue h WHERE h.rt_code = v.rt_code);

INSERT INTO wm_rt_issue_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rt, qc_flag, quality_status, create_time)
SELECT h.rt_id, mi.item_id, mi.item_code, mi.item_name, IFNULL(u.unit_name, '件'), v.qty, 'Y', 'NT', NOW()
FROM (
  SELECT 'RT-QC-001' AS rc, 'MAT-SCREW-M4' AS mc, 18 AS qty
  UNION ALL SELECT 'RT-QC-002', 'MAT-MOTOR-40W', 3
  UNION ALL SELECT 'RT-QC-003', 'MAT-BLADE-30', 12
) v
JOIN wm_rt_issue h ON h.rt_code = v.rc
JOIN md_item mi ON mi.item_code = v.mc
LEFT JOIN uom u ON u.unit_code = mi.unit_of_measure OR u.unit_name = mi.unit_of_measure
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_issue_line l WHERE l.rt_id = h.rt_id AND l.item_code = v.mc);

INSERT INTO wm_rt_sales (rt_code, rt_name, client_id, client_code, client_name, so_code, rt_date, status, create_by, create_time)
SELECT v.rt_code, v.rt_name, c.client_id, c.client_code, c.client_name, v.so_code, NOW(), v.status, 'warehouse01', NOW()
FROM (
  SELECT 'RS-QC-001' AS rt_code, '华东连锁TS30销退待检' AS rt_name, 'C001' AS cc, 'SO-20260706' AS so_code, 'UNCHECK' AS status
  UNION ALL SELECT 'RS-QC-002', '北方商城FS40销退待检', 'C003', 'SO-20260705', 'PREPARE'
) v
JOIN md_client c ON c.client_code = v.cc
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_sales h WHERE h.rt_code = v.rt_code);

INSERT INTO wm_rt_sales_line (rt_id, item_id, item_code, item_name, unit_name, quantity_rted, batch_code, quality_status, create_time)
SELECT h.rt_id, mi.item_id, mi.item_code, mi.item_name, '件', v.qty, v.batch, 'NT', NOW()
FROM (
  SELECT 'RS-QC-001' AS rc, 'FAN-TS30-B' AS pc, 5 AS qty, 'PB-TS30-20260710' AS batch
  UNION ALL SELECT 'RS-QC-002', 'FAN-FS40-A', 4, 'PB-FS40-20260705'
) v
JOIN wm_rt_sales h ON h.rt_code = v.rc
JOIN md_item mi ON mi.item_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM wm_rt_sales_line l WHERE l.rt_id = h.rt_id AND l.item_code = v.pc);

-- ========== 7. 近 7 日 IQC 完成记录（趋势 + 最近完成）==========
INSERT INTO qc_iqc (iqc_code, iqc_name, template_id, vendor_id, vendor_code, vendor_name, item_id, item_code, item_name, unit_of_measure,
  quantity_recived, quantity_check, quantity_qualified, quantity_unqualified, check_result, inspect_date, status, inspector, create_by, create_time)
SELECT v.iqc_code, v.iqc_name, 2, mv.vendor_id, mv.vendor_code, mv.vendor_name,
  mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_recv, v.qty_check, v.qty_q, v.qty_u, v.check_result, v.inspect_date, 'FINISHED', 'qc01', 'qc01', v.inspect_date
FROM (
  SELECT 'IQC-AN-001' AS iqc_code, '电机来料日检-6天前' AS iqc_name, 'V001' AS vc, 'MAT-MOTOR-55W' AS mc,
    200 AS qty_recv, 10 AS qty_check, 10 AS qty_q, 0 AS qty_u, 'ACCEPT' AS check_result, DATE_SUB(NOW(), INTERVAL 6 DAY) AS inspect_date
  UNION ALL SELECT 'IQC-AN-002', '扇叶来料日检-5天前', 'V002', 'MAT-BLADE-30', 300, 8, 7, 1, 'REJECT', DATE_SUB(NOW(), INTERVAL 5 DAY)
  UNION ALL SELECT 'IQC-AN-003', '螺丝来料日检-4天前', 'V004', 'MAT-SCREW-M4', 1000, 20, 20, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 4 DAY)
  UNION ALL SELECT 'IQC-AN-004', '外壳来料日检-3天前', 'V003', 'MAT-SHELL-TS30', 150, 5, 5, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 3 DAY)
  UNION ALL SELECT 'IQC-AN-005', '电机来料日检-2天前', 'V001', 'MAT-MOTOR-55W', 180, 10, 9, 1, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 2 DAY)
  UNION ALL SELECT 'IQC-AN-006', '扇叶来料日检-1天前', 'V002', 'MAT-BLADE-30', 250, 8, 8, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'IQC-AN-007', '电机来料日检-今日', 'V001', 'MAT-MOTOR-55W', 120, 6, 6, 0, 'ACCEPT', NOW()
  UNION ALL SELECT 'IQC-AN-008', '40W电机来料-6天前', 'V001', 'MAT-MOTOR-40W', 160, 8, 7, 1, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 6 DAY)
  UNION ALL SELECT 'IQC-AN-009', '35W电机来料-4天前', 'V001', 'MAT-MOTOR-35W', 90, 5, 5, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 4 DAY)
  UNION ALL SELECT 'IQC-AN-010', '40cm扇叶来料-2天前', 'V002', 'MAT-BLADE-40', 220, 10, 9, 1, 'REJECT', DATE_SUB(NOW(), INTERVAL 2 DAY)
  UNION ALL SELECT 'IQC-AN-011', '底座来料-1天前', 'V003', 'MAT-SHELL-TS30', 80, 4, 4, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'IQC-AN-012', '40cm扇叶来料-今日', 'V002', 'MAT-BLADE-40', 100, 5, 4, 1, 'ACCEPT', NOW()
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM qc_iqc q WHERE q.iqc_code = v.iqc_code);

-- ========== 8. 近 7 日 IPQC / OQC / RQC 完成 ==========
INSERT INTO qc_ipqc (ipqc_code, ipqc_name, ipqc_type, template_id, workorder_id, workorder_code, workorder_name,
  workstation_id, workstation_code, workstation_name, item_id, item_code, item_name, unit_of_measure,
  quantity_check, quantity_qualified, quantity_unqualified, check_result, inspect_date, status, inspector, create_by, create_time)
SELECT v.ipqc_code, v.ipqc_name, v.ipqc_type, 2, wo.work_order_id, wo.work_order_no, p.product_name,
  ws.station_id, ws.station_code, ws.station_name, mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty_check, v.qty_q, v.qty_u, v.check_result, v.inspect_date, 'FINISHED', 'qc01', 'qc01', v.inspect_date
FROM (
  SELECT 'IPQC-AN-001' AS ipqc_code, 'WO-20260701首件-6天前' AS ipqc_name, 'FIRST' AS ipqc_type, 'WO-20260701' AS wo, 'ST-01' AS st,
    3 AS qty_check, 3 AS qty_q, 0 AS qty_u, 'ACCEPT' AS check_result, DATE_SUB(NOW(), INTERVAL 6 DAY) AS inspect_date
  UNION ALL SELECT 'IPQC-AN-002', 'WO-20260701巡检-5天前', 'PATROL', 'WO-20260701', 'ST-03', 5, 5, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 5 DAY)
  UNION ALL SELECT 'IPQC-AN-003', 'WO-20260705末件-4天前', 'LAST', 'WO-20260705', 'ST-06', 4, 3, 1, 'REJECT', DATE_SUB(NOW(), INTERVAL 4 DAY)
  UNION ALL SELECT 'IPQC-AN-004', 'WO-20260707巡检-3天前', 'PATROL', 'WO-20260707', 'ST-05', 6, 6, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 3 DAY)
  UNION ALL SELECT 'IPQC-AN-005', 'WO-20260710首件-2天前', 'FIRST', 'WO-20260710', 'ST-06', 2, 2, 0, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 2 DAY)
  UNION ALL SELECT 'IPQC-AN-006', 'WO-20260708巡检-1天前', 'PATROL', 'WO-20260708', 'ST-07', 5, 4, 1, 'ACCEPT', DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'IPQC-AN-007', 'WO-20260711首件-今日', 'FIRST', 'WO-20260711', 'ST-07', 3, 3, 0, 'ACCEPT', NOW()
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
JOIN workstation ws ON ws.station_code = v.st
WHERE NOT EXISTS (SELECT 1 FROM qc_ipqc q WHERE q.ipqc_code = v.ipqc_code);

INSERT INTO qc_oqc (oqc_code, oqc_name, template_id, source_doc_code, source_doc_type, client_id, client_code, client_name,
  batch_code, item_id, item_code, item_name, unit_of_measure, quantity_out, quantity_check, quantity_unqualified, quantity_quanlified,
  check_result, out_date, inspect_date, inspector, status, create_by, create_time)
SELECT v.oqc_code, v.oqc_name, 2, v.src_doc, 'PRODUCT_SALES', c.client_id, c.client_code, c.client_name,
  v.batch_code, mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_out, v.qty_check, v.qty_unq, v.qty_out - v.qty_unq,
  IF(v.qty_unq = 0, 'ACCEPT', 'REJECT'), v.out_date, v.inspect_date, 'qc01', 'FINISHED', 'qc01', v.inspect_date
FROM (
  SELECT 'OQC-AN-001' AS oqc_code, 'FS40出货检-6天前' AS oqc_name, 'PS-STORY-001' AS src_doc, 'C005' AS cc, 'PB-FS40-20260705' AS batch_code, 'FAN-FS40-A' AS pc,
    200 AS qty_out, 15 AS qty_check, 0 AS qty_unq, DATE_SUB(NOW(), INTERVAL 6 DAY) AS out_date, DATE_SUB(NOW(), INTERVAL 6 DAY) AS inspect_date
  UNION ALL SELECT 'OQC-AN-002', 'TS30出货检-5天前', 'PS-STORY-002', 'C001', 'PB-TS30-20260710', 'FAN-TS30-B', 80, 8, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)
  UNION ALL SELECT 'OQC-AN-003', 'FS40出货检-4天前', 'PS-STORY-001', 'C005', 'PB-FS40-20260705', 'FAN-FS40-A', 150, 10, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)
  UNION ALL SELECT 'OQC-AN-003B', 'TS30出货检-3天前', 'PS-STORY-003', 'C003', 'PB-TS30-20260710', 'FAN-TS30-B', 60, 6, 0, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)
  UNION ALL SELECT 'OQC-AN-004', 'WS20出货检-2天前', 'PS-STORY-003', 'C003', 'PB-WS20-202607', 'FAN-WS20-C', 40, 4, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)
  UNION ALL SELECT 'OQC-AN-005', 'FS40出货检-1天前', 'PS-STORY-001', 'C005', 'PB-FS40-20260705', 'FAN-FS40-A', 100, 8, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)
  UNION ALL SELECT 'OQC-AN-006', 'TS30出货检-今日', 'PS-STORY-002', 'C001', 'PB-TS30-20260710', 'FAN-TS30-B', 50, 5, 0, NOW(), NOW()
) v
JOIN md_client c ON c.client_code = v.cc
JOIN md_item mi ON mi.item_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM qc_oqc q WHERE q.oqc_code = v.oqc_code);

INSERT INTO qc_rqc (rqc_code, rqc_name, template_id, source_doc_code, rqc_type, item_id, item_code, item_name, unit_of_measure,
  quantity_check, quantity_qualified, quantity_unqualified, check_result, inspect_date, status, user_name, remark, create_by, create_time)
SELECT v.rqc_code, v.rqc_name, 2, v.src_doc, 'RT', mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty_check, v.qty_ok, v.qty_unq, IF(v.qty_unq = 0, 'ACCEPT', 'REJECT'), v.inspect_date, 'FINISHED', 'qc01', v.remark, 'qc01', v.inspect_date
FROM (
  SELECT 'RQC-AN-001' AS rqc_code, '余料退检-6天前' AS rqc_name, 'RT-STORY-001' AS src_doc, 'MAT-SCREW-M4' AS mc,
    24 AS qty_check, 24 AS qty_ok, 0 AS qty_unq, DATE_SUB(NOW(), INTERVAL 6 DAY) AS inspect_date, '余料退库检验合格' AS remark
  UNION ALL SELECT 'RQC-AN-002', '电机退检-5天前', 'RT-STORY-002', 'MAT-MOTOR-40W', 2, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), '退料电机复检'
  UNION ALL SELECT 'RQC-AN-003', '销退检验-4天前', 'RS-STORY-001', 'FAN-FS40-A', 3, 2, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), '客户退货检验'
  UNION ALL SELECT 'RQC-AN-004', '扇叶退检-2天前', 'RT-QC-001', 'MAT-BLADE-30', 10, 10, 0, DATE_SUB(NOW(), INTERVAL 2 DAY), '线边退料'
  UNION ALL SELECT 'RQC-AN-005', 'TS30销退-1天前', 'RS-QC-001', 'FAN-TS30-B', 2, 2, 0, DATE_SUB(NOW(), INTERVAL 1 DAY), '销退合格'
  UNION ALL SELECT 'RQC-AN-006', '螺丝退检-今日', 'RT-STORY-001', 'MAT-SCREW-M4', 8, 8, 0, NOW(), '今日退料检'
) v
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM qc_rqc q WHERE q.rqc_code = v.rqc_code);

-- 补齐 Story 已完成单的结果与日期
UPDATE qc_iqc SET check_result = IFNULL(check_result, 'ACCEPT'), inspect_date = COALESCE(inspect_date, update_time, create_time)
WHERE status = 'FINISHED' AND (check_result IS NULL OR inspect_date IS NULL);

UPDATE qc_ipqc SET check_result = IFNULL(check_result, 'ACCEPT'), inspect_date = COALESCE(inspect_date, update_time, create_time),
  quantity_qualified = IFNULL(quantity_qualified, quantity_check)
WHERE status = 'FINISHED' AND (check_result IS NULL OR inspect_date IS NULL);

UPDATE qc_oqc SET check_result = IFNULL(check_result, 'ACCEPT'), inspect_date = COALESCE(inspect_date, update_time, create_time)
WHERE status = 'FINISHED' AND (check_result IS NULL OR inspect_date IS NULL);

UPDATE qc_rqc SET check_result = IFNULL(check_result, 'ACCEPT'), inspect_date = COALESCE(inspect_date, update_time, create_time)
WHERE status = 'FINISHED' AND (check_result IS NULL OR inspect_date IS NULL);

-- ========== 9. 待处置检验单（底栏 + KPI）==========
-- Story 已有 PREPARE/CONFIRMED；补充几条
INSERT INTO qc_iqc (iqc_code, iqc_name, template_id, vendor_id, vendor_code, vendor_name, item_id, item_code, item_name, unit_of_measure,
  quantity_recived, quantity_check, status, create_by, create_time)
SELECT v.iqc_code, v.iqc_name, 2, mv.vendor_id, mv.vendor_code, mv.vendor_name,
  mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_recv, v.qty_check, v.status, 'qc01', NOW(3)
FROM (
  SELECT 'IQC-DISP-001' AS iqc_code, '扇叶来料待处置' AS iqc_name, 'V002' AS vc, 'MAT-BLADE-40' AS mc, 200 AS qty_recv, 10 AS qty_check, 'PREPARE' AS status
  UNION ALL SELECT 'IQC-DISP-002', '外壳来料已确认', 'V003', 'MAT-SHELL-TS30', 150, 8, 'CONFIRMED'
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM qc_iqc q WHERE q.iqc_code = v.iqc_code);

-- ========== 10. 缺陷记录（不良 TOP10）==========
INSERT INTO qc_defect_record (qc_type, qc_id, line_id, defect_name, defect_level, defect_quantity, remark, create_by, create_time)
SELECT v.qc_type, doc.qc_id, 0, v.defect_name, v.defect_level, v.qty, v.remark, 'qc01', NOW(3)
FROM (
  SELECT 'IQC' AS qc_type, 'IQC-AN-002' AS code, '扇叶偏摆' AS defect_name, 'MAJ' AS defect_level, 3 AS qty, '来料外观抽检' AS remark
  UNION ALL SELECT 'IQC', 'IQC-AN-005', '噪音超标', 'MAJ', 2, '性能抽检'
  UNION ALL SELECT 'IQC', 'IQC-AN-010', '扇叶偏摆', 'MAJ', 2, '40cm扇叶'
  UNION ALL SELECT 'IQC', 'IQC-AN-012', '扇叶偏摆', 'MAJ', 1, '40cm扇叶来料'
  UNION ALL SELECT 'IQC', 'IQC-STORY-001', '外壳裂纹', 'CR', 1, 'Story 演示'
  UNION ALL SELECT 'PQC', 'IPQC-AN-003', '转速偏低', 'MAJ', 2, '末件检验'
  UNION ALL SELECT 'PQC', 'IPQC-AN-006', '噪音超标', 'MAJ', 1, '过程巡检'
  UNION ALL SELECT 'PQC', 'IPQC-STORY-001', '螺丝松动', 'MIN', 1, '首件检验'
  UNION ALL SELECT 'OQC', 'OQC-AN-002', '包装破损', 'MIN', 1, '出货检'
  UNION ALL SELECT 'OQC', 'OQC-AN-005', '标签错误', 'MIN', 2, '出货检'
  UNION ALL SELECT 'OQC', 'OQC-STORY-001', '色差明显', 'MAJ', 1, '外观'
  UNION ALL SELECT 'RQC', 'RQC-AN-002', '绝缘不足', 'CR', 1, '退料电机'
  UNION ALL SELECT 'RQC', 'RQC-AN-003', '扇罩变形', 'MAJ', 1, '销退'
) v
JOIN (
  SELECT 'IQC' AS qc_type, iqc_id AS qc_id, iqc_code AS code FROM qc_iqc
  UNION ALL SELECT 'PQC', ipqc_id, ipqc_code FROM qc_ipqc
  UNION ALL SELECT 'OQC', oqc_id, oqc_code FROM qc_oqc
  UNION ALL SELECT 'RQC', rqc_id, rqc_code FROM qc_rqc
) doc ON doc.qc_type = v.qc_type AND doc.code = v.code
WHERE NOT EXISTS (
  SELECT 1 FROM qc_defect_record d WHERE d.qc_type = v.qc_type AND d.qc_id = doc.qc_id AND d.defect_name = v.defect_name
);
