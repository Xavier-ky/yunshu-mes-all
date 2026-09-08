-- Story seed 05: quality master data + IQC/IPQC/OQC/RQC linked to real WOs
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 风扇场景检测项 / 模板 ==========
INSERT INTO qc_index (index_id, index_code, index_name, index_type, qc_tool, qc_result_type, qc_result_spc, remark, create_by, create_time)
VALUES
  (10, 'QI-NOISE', '运行噪音', '性能', '分贝仪', 'FLOAT', 'dB', '老化测试噪音检测', 'qc01', NOW(3)),
  (11, 'QI-RPM', '转速', '性能', '转速表', 'FLOAT', 'rpm', '额定转速检测', 'qc01', NOW(3)),
  (12, 'QI-INSUL', '绝缘电阻', '安全', '兆欧表', 'FLOAT', 'MΩ', '电机绝缘检测', 'qc01', NOW(3))
ON DUPLICATE KEY UPDATE index_name = VALUES(index_name), remark = VALUES(remark);

INSERT INTO qc_defect (defect_id, defect_code, defect_name, index_type, defect_level, process_method, remark, create_by, create_time)
VALUES
  (10, 'DF-NOISE-HIGH', '噪音超标', '性能', 'MAJ', '返工调整动平衡', '风扇常见性能缺陷', 'qc01', NOW(3)),
  (11, 'DF-SHELL-CRACK', '外壳裂纹', '外观', 'CR', '报废', '注塑件外观缺陷', 'qc01', NOW(3)),
  (12, 'DF-WOBBLE', '扇叶偏摆', '功能', 'MAJ', '返工重新安装', '安装不到位导致', 'qc01', NOW(3))
ON DUPLICATE KEY UPDATE defect_name = VALUES(defect_name);

INSERT INTO qc_template (template_id, template_code, template_name, qc_types, enable_flag, remark, create_by, create_time)
VALUES (2, 'QT-FAN-001', '电风扇全检模板', 'IQC,PQC,OQC,RQC', 'Y', 'FS40/TS30/WS20通用', 'qc01', NOW(3))
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

INSERT INTO qc_template_index (record_id, template_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
VALUES
  (20, 2, 1, 'QI-APPEAR', '外观检查', '外观', '目视', '无划伤、无色差', NULL, NULL, NULL, NULL, 'qc01', NOW(3)),
  (21, 2, 10, 'QI-NOISE', '运行噪音', '性能', '分贝仪', '三档运行噪音', 55.0000, 'dB', 65.0000, 0.0000, 'qc01', NOW(3)),
  (22, 2, 11, 'QI-RPM', '转速', '性能', '转速表', '最高档转速', 1200.0000, 'rpm', 1300.0000, 1100.0000, 'qc01', NOW(3)),
  (23, 2, 12, 'QI-INSUL', '绝缘电阻', '安全', '兆欧表', '≥5MΩ', 5.0000, 'MΩ', NULL, 5.0000, 'qc01', NOW(3))
ON DUPLICATE KEY UPDATE index_name = VALUES(index_name);

INSERT INTO qc_template_product (record_id, template_id, item_id, item_code, item_name, specification, unit_of_measure, quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by, create_time)
SELECT 20, 2, mi.item_id, mi.item_code, mi.item_name, mi.specification, mi.unit_of_measure, 5, 0, 0, 5, 10, 'qc01', NOW(3)
FROM md_item mi JOIN product p ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR) AND p.product_code = 'FAN-FS40-A'
WHERE NOT EXISTS (SELECT 1 FROM qc_template_product WHERE record_id = 20);

INSERT INTO qc_template_product (record_id, template_id, item_id, item_code, item_name, specification, unit_of_measure, quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by, create_time)
SELECT 21, 2, mi.item_id, mi.item_code, mi.item_name, mi.specification, mi.unit_of_measure, 3, 0, 0, 5, 10, 'qc01', NOW(3)
FROM md_item mi JOIN product p ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR) AND p.product_code = 'FAN-TS30-B'
WHERE NOT EXISTS (SELECT 1 FROM qc_template_product WHERE record_id = 21);

-- 更新 V15 演示单为故事数据
UPDATE qc_iqc SET
  iqc_code = 'IQC-STORY-001', iqc_name = '55W电机来料检验',
  template_id = 2, vendor_id = (SELECT vendor_id FROM md_vendor WHERE vendor_code = 'V001'),
  vendor_code = 'V001', vendor_name = '精密电机科技',
  item_id = (SELECT mi.item_id FROM md_item mi WHERE mi.item_code = 'MAT-MOTOR-55W' LIMIT 1),
  item_code = 'MAT-MOTOR-55W', item_name = '55W风扇电机',
  quantity_recived = 500, quantity_check = 10, status = 'FINISHED',
  update_by = 'qc01', update_time = NOW(3)
WHERE iqc_id = 1;

UPDATE qc_ipqc SET
  ipqc_code = 'IPQC-STORY-001', ipqc_name = 'WO-20260701首件检验',
  ipqc_type = 'FIRST', template_id = 2,
  workorder_id = (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260701'),
  workorder_code = 'WO-20260701',
  workorder_name = (SELECT product_name FROM product p JOIN work_order wo ON wo.product_id = p.product_id WHERE wo.work_order_no = 'WO-20260701'),
  workstation_id = (SELECT station_id FROM workstation WHERE station_code = 'ST-01'),
  workstation_code = 'ST-01', workstation_name = '电机装配工位',
  item_id = (SELECT mi.item_id FROM md_item mi JOIN product p ON mi.attr1='PRODUCT' AND mi.attr2=CAST(p.product_id AS CHAR) JOIN work_order wo ON wo.product_id=p.product_id WHERE wo.work_order_no='WO-20260701' LIMIT 1),
  item_code = 'FAN-FS40-A', item_name = '40cm落地电风扇A型',
  quantity_check = 3, status = 'FINISHED', update_by = 'qc01', update_time = NOW(3)
WHERE ipqc_id = 1;

-- ========== 来料检验 IQC ==========
INSERT INTO qc_iqc (iqc_code, iqc_name, template_id, vendor_id, vendor_code, vendor_name, item_id, item_code, item_name, unit_of_measure, quantity_recived, quantity_check, status, create_by, create_time)
SELECT v.iqc_code, v.iqc_name, 2, mv.vendor_id, mv.vendor_code, mv.vendor_name,
  mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_recv, v.qty_check, v.status, 'qc01', NOW(3)
FROM (
  SELECT 'IQC-STORY-002' AS iqc_code, '40W台扇电机来料检' AS iqc_name, 'V001' AS vc, 'MAT-MOTOR-40W' AS mc,
    300 AS qty_recv, 8 AS qty_check, 'FINISHED' AS status
  UNION ALL SELECT 'IQC-STORY-003', '30cm扇叶来料检', 'V002', 'MAT-BLADE-30', 500, 10, 'FINISHED'
  UNION ALL SELECT 'IQC-STORY-004', 'M4螺丝来料待检', 'V004', 'MAT-SCREW-M4', 2000, 20, 'PREPARE'
  UNION ALL SELECT 'IQC-STORY-005', 'TS30外壳来料检', 'V003', 'MAT-SHELL-TS30', 400, 5, 'CONFIRMED'
) v
JOIN md_vendor mv ON mv.vendor_code = v.vc
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM qc_iqc q WHERE q.iqc_code = v.iqc_code);

-- ========== 过程检验 IPQC ==========
INSERT INTO qc_ipqc (ipqc_code, ipqc_name, ipqc_type, template_id, workorder_id, workorder_code, workorder_name,
  workstation_id, workstation_code, workstation_name, item_id, item_code, item_name, unit_of_measure, quantity_check, status, create_by, create_time)
SELECT v.ipqc_code, v.ipqc_name, v.ipqc_type, 2, wo.work_order_id, wo.work_order_no, p.product_name,
  ws.station_id, ws.station_code, ws.station_name, mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_check, v.status, 'qc01', NOW(3)
FROM (
  SELECT 'IPQC-STORY-002' AS ipqc_code, 'WO-20260707巡检' AS ipqc_name, 'PATROL' AS ipqc_type,
    'WO-20260707' AS wo, 'ST-05' AS st, 5 AS qty_check, 'FINISHED' AS status
  UNION ALL SELECT 'IPQC-STORY-003', 'WO-20260705末件检验', 'LAST', 'WO-20260705', 'ST-06', 3, 'FINISHED'
  UNION ALL SELECT 'IPQC-STORY-004', 'WO-20260708首件检验', 'FIRST', 'WO-20260708', 'ST-07', 2, 'PREPARE'
  UNION ALL SELECT 'IPQC-STORY-005', 'WO-20260701巡检', 'PATROL', 'WO-20260701', 'ST-03', 4, 'CONFIRMED'
) v
JOIN work_order wo ON wo.work_order_no = v.wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
JOIN workstation ws ON ws.station_code = v.st
WHERE NOT EXISTS (SELECT 1 FROM qc_ipqc q WHERE q.ipqc_code = v.ipqc_code);

-- ========== 出货检验 OQC ==========
INSERT INTO qc_oqc (oqc_code, oqc_name, template_id, source_doc_code, source_doc_type, client_id, client_code, client_name,
  batch_code, item_id, item_code, item_name, unit_of_measure, quantity_out, quantity_check, quantity_unqualified, quantity_quanlified,
  check_result, out_date, inspect_date, inspector, status, create_by, create_time)
SELECT v.oqc_code, v.oqc_name, 2, v.src_doc, 'PRODUCT_SALES', c.client_id, c.client_code, c.client_name,
  v.batch_code, mi.item_id, mi.item_code, mi.item_name, 'PCS', v.qty_out, v.qty_check, v.qty_unq, v.qty_out - v.qty_unq,
  IF(v.qty_unq = 0, 'ACCEPT', 'REJECT'), v.out_date, v.inspect_date, 'qc01', v.status, 'qc01', NOW(3)
FROM (
  SELECT 'OQC-STORY-001' AS oqc_code, '珠三角连锁FS40出货检' AS oqc_name, 'PS-STORY-001' AS src_doc, 'C005' AS cc,
    'PB-FS40-20260705' AS batch_code, 'FAN-FS40-A' AS pc, 380 AS qty_out, 20 AS qty_check, 0 AS qty_unq,
    DATE_SUB(NOW(), INTERVAL 1 DAY) AS out_date, DATE_SUB(NOW(), INTERVAL 1 DAY) AS inspect_date, 'FINISHED' AS status
  UNION ALL SELECT 'OQC-STORY-002', '华东连锁TS30出货检', 'PS-STORY-002', 'C001',
    'PB-TS30-20260710', 'FAN-TS30-B', 100, 10, 1,
    NOW(), NOW(), 'PREPARE'
  UNION ALL SELECT 'OQC-STORY-003', '北方商城TS30出货检', 'PS-STORY-003', 'C003',
    'PB-TS30-20260710', 'FAN-TS30-B', 80, 8, 0,
    NOW(), NOW(), 'CONFIRMED'
) v
JOIN md_client c ON c.client_code = v.cc
JOIN md_item mi ON mi.item_code = v.pc
WHERE NOT EXISTS (SELECT 1 FROM qc_oqc q WHERE q.oqc_code = v.oqc_code);

-- OQC 检验行
INSERT INTO qc_oqc_line (oqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT o.oqc_id, ti.index_id, ti.index_code, ti.index_name, ti.index_type, ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure, ti.threshold_max, ti.threshold_min, 'qc01', NOW(3)
FROM qc_oqc o
JOIN qc_template_index ti ON ti.template_id = 2
WHERE o.oqc_code LIKE 'OQC-STORY-%'
  AND NOT EXISTS (SELECT 1 FROM qc_oqc_line l WHERE l.oqc_id = o.oqc_id AND l.index_id = ti.index_id);

-- ========== 退料检验 RQC ==========
DELETE FROM qc_rqc_line WHERE rqc_id = 9001;
DELETE FROM qc_rqc WHERE rqc_id = 9001;

INSERT INTO qc_rqc (rqc_code, rqc_name, template_id, source_doc_code, rqc_type, item_id, item_code, item_name, unit_of_measure,
  quantity_check, quantity_qualified, quantity_unqualified, check_result, status, remark, create_by, create_time)
SELECT v.rqc_code, v.rqc_name, 2, v.src_doc, 'RT', mi.item_id, mi.item_code, mi.item_name, 'PCS',
  v.qty_check, v.qty_ok, v.qty_unq, IF(v.qty_unq = 0, 'ACCEPT', 'REJECT'), v.status, v.remark, 'qc01', NOW(3)
FROM (
  SELECT 'RQC-STORY-001' AS rqc_code, 'WO-20260701余料退检' AS rqc_name, 'RT-STORY-001' AS src_doc,
    'MAT-SCREW-M4' AS mc, 24 AS qty_check, 24 AS qty_ok, 0 AS qty_unq, 'FINISHED' AS status, '余料退库检验合格' AS remark
  UNION ALL SELECT 'RQC-STORY-002', 'WO-20260707电机退检', 'RT-STORY-002',
    'MAT-MOTOR-40W', 2, 1, 1, 'PREPARE', '退料电机待复检'
) v
JOIN md_item mi ON mi.item_code = v.mc
WHERE NOT EXISTS (SELECT 1 FROM qc_rqc q WHERE q.rqc_code = v.rqc_code);

INSERT INTO qc_rqc_line (rqc_id, index_id, index_code, index_name, index_type, qc_tool, check_method, stander_val, unit_of_measure, threshold_max, threshold_min, create_by, create_time)
SELECT r.rqc_id, ti.index_id, ti.index_code, ti.index_name, ti.index_type, ti.qc_tool, ti.check_method, ti.stander_val, ti.unit_of_measure, ti.threshold_max, ti.threshold_min, 'qc01', NOW(3)
FROM qc_rqc r
JOIN qc_template_index ti ON ti.template_id = 2 AND ti.index_id IN (1, 3)
WHERE r.rqc_code LIKE 'RQC-STORY-%'
  AND NOT EXISTS (SELECT 1 FROM qc_rqc_line l WHERE l.rqc_id = r.rqc_id AND l.index_id = ti.index_id);
