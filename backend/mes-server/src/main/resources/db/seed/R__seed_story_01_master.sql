-- Story seed 01: 3 SKU master data (FS40-A / TS30-B / WS20-C), clients, vendors, BOM, routes
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 客户（与订单 customer_name 对齐）==========
INSERT INTO md_client (client_code, client_name, client_nick, client_type, address, tel, contact1, contact1_tel, enable_flag, create_time)
VALUES
  ('C001', '华东家电连锁', '华东连锁', 'ENTERPRISE', '上海市浦东新区张江路88号', '021-58880001', '张经理', '13800001001', 'Y', NOW(3)),
  ('C002', '南方家电批发', '南方批发', 'ENTERPRISE', '广州市天河区体育西路168号', '020-38880002', '李总', '13800001002', 'Y', NOW(3)),
  ('C003', '北方商城集团', '北方商城', 'ENTERPRISE', '北京市朝阳区建国路99号', '010-68880003', '王采购', '13800001003', 'Y', NOW(3)),
  ('C004', '西域电器经销', '西域经销', 'ENTERPRISE', '成都市高新区天府大道520号', '028-88880004', '赵经理', '13800001004', 'Y', NOW(3)),
  ('C005', '珠三角家电连锁', '珠三角连锁', 'ENTERPRISE', '深圳市南山区科技园南路66号', '0755-88880005', '陈总监', '13800001005', 'Y', NOW(3))
ON DUPLICATE KEY UPDATE
  client_name = VALUES(client_name), client_nick = VALUES(client_nick),
  address = VALUES(address), tel = VALUES(tel), contact1 = VALUES(contact1);

-- ========== 供应商（与入库单 vendor 对齐）==========
INSERT INTO md_vendor (vendor_code, vendor_name, vendor_nick, vendor_type, address, tel, contact1, contact1_tel, enable_flag, create_time)
VALUES
  ('V001', '精密电机科技', '精密电机', 'MATERIAL', '苏州市工业园区星湖街328号', '0512-66660001', '刘工', '13900002001', 'Y', NOW(3)),
  ('V002', '扇叶注塑厂', '扇叶注塑', 'MATERIAL', '东莞市虎门镇工业路18号', '0769-66660002', '周厂长', '13900002002', 'Y', NOW(3)),
  ('V003', '外壳加工厂', '外壳加工', 'MATERIAL', '佛山市顺德区北滘镇制造路6号', '0757-66660003', '吴主管', '13900002003', 'Y', NOW(3)),
  ('V004', '标准件贸易公司', '标准件', 'MATERIAL', '温州市龙湾区紧固件市场A区', '0577-66660004', '郑销售', '13900002004', 'Y', NOW(3)),
  ('V005', '包装材料厂', '包装材料', 'MATERIAL', '义乌市稠江街道包装产业园', '0579-66660005', '孙经理', '13900002005', 'Y', NOW(3))
ON DUPLICATE KEY UPDATE
  vendor_name = VALUES(vendor_name), vendor_nick = VALUES(vendor_nick),
  address = VALUES(address), tel = VALUES(tel);

-- 对齐已有客户订单名称
UPDATE customer_order o
JOIN (
  SELECT 'CO-20260701' AS order_no, '华东家电连锁' AS customer_name
  UNION ALL SELECT 'CO-20260702', '南方家电批发'
  UNION ALL SELECT 'CO-20260703', '北方商城集团'
  UNION ALL SELECT 'CO-20260704', '西域电器经销'
  UNION ALL SELECT 'CO-20260705', '珠三角家电连锁'
) v ON v.order_no = o.order_no
SET o.customer_name = v.customer_name;

-- ========== 产品 SKU：台扇 TS30-B、壁扇 WS20-C ==========
INSERT INTO product (product_code, product_name, product_model, product_category, status)
VALUES
  ('FAN-TS30-B', '30cm台扇B型', 'TS30-B', 'TABLE_FAN', 'ENABLED'),
  ('FAN-WS20-C', '20cm壁扇C型', 'WS20-C', 'WALL_FAN', 'ENABLED')
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), product_model = VALUES(product_model);

INSERT INTO product_spec (product_id, spec_name, spec_value, unit_code)
SELECT p.product_id, v.spec_name, v.spec_value, v.unit_code
FROM product p
JOIN (
  SELECT 'FAN-TS30-B' AS pc, '扇叶直径' AS spec_name, '30' AS spec_value, 'CM' AS unit_code
  UNION ALL SELECT 'FAN-TS30-B', '电机功率', '40', 'W'
  UNION ALL SELECT 'FAN-TS30-B', '颜色', '白色', NULL
  UNION ALL SELECT 'FAN-WS20-C', '扇叶直径', '20', 'CM'
  UNION ALL SELECT 'FAN-WS20-C', '电机功率', '35', 'W'
  UNION ALL SELECT 'FAN-WS20-C', '颜色', '灰色', NULL
) v ON v.pc = p.product_code
WHERE NOT EXISTS (
  SELECT 1 FROM product_spec ps WHERE ps.product_id = p.product_id AND ps.spec_name = v.spec_name
);

-- ========== 物料（TS30 / WS20 专用件）==========
INSERT INTO material (material_code, material_name, material_type, unit_id, is_key_material, status)
VALUES
  ('MAT-MOTOR-40W', '40W台扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-MOTOR-35W', '35W壁扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-BLADE-30', '30cm扇叶', 'BLADE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-BLADE-20', '20cm扇叶', 'BLADE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-SHELL-TS30', 'TS30外壳组件', 'SHELL', (SELECT unit_id FROM uom WHERE unit_code = 'SET'), 1, 'ENABLED'),
  ('MAT-SHELL-WS20', 'WS20外壳组件', 'SHELL', (SELECT unit_id FROM uom WHERE unit_code = 'SET'), 1, 'ENABLED'),
  ('MAT-PACK-TS30', 'TS30包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-PACK-WS20', 'WS20包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED')
ON DUPLICATE KEY UPDATE material_name = VALUES(material_name), is_key_material = VALUES(is_key_material);

-- ========== md_item 同步（新产品/物料）==========
INSERT INTO md_item (item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT p.product_code, p.product_name, IFNULL(p.product_model, ''), 'PCS', '件',
  'PRODUCT', 3, 'PRODUCT_FIN', IFNULL(p.product_category, '成品'),
  IF(p.status = 'ENABLED', 'Y', 'N'), 'PRODUCT', CAST(p.product_id AS CHAR), NOW(3), NOW(3)
FROM product p
WHERE p.product_code IN ('FAN-TS30-B', 'FAN-WS20-C') AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM md_item mi WHERE mi.item_code = p.product_code);

INSERT INTO md_item (item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT m.material_code, m.material_name, '', IFNULL(u.unit_code, 'PCS'), IFNULL(u.unit_name, '件'),
  'ITEM', 4, 'ITEM_RAW', IFNULL(m.material_type, '原材料'),
  IF(m.status = 'ENABLED', 'Y', 'N'), 'MATERIAL', CAST(m.material_id AS CHAR), NOW(3), NOW(3)
FROM material m
LEFT JOIN uom u ON m.unit_id = u.unit_id
WHERE m.material_code IN ('MAT-MOTOR-40W','MAT-MOTOR-35W','MAT-BLADE-30','MAT-BLADE-20',
  'MAT-SHELL-TS30','MAT-SHELL-WS20','MAT-PACK-TS30','MAT-PACK-WS20') AND m.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM md_item mi WHERE mi.item_code = m.material_code);

-- ========== BOM ==========
INSERT INTO bom (product_id, bom_code, bom_name, version_no, status)
SELECT p.product_id, v.bom_code, v.bom_name, 'V1.0', 'RELEASED'
FROM product p
JOIN (
  SELECT 'FAN-TS30-B' AS pc, 'BOM-FAN-TS30-B' AS bom_code, 'TS30-B标准BOM' AS bom_name
  UNION ALL SELECT 'FAN-WS20-C', 'BOM-FAN-WS20-C', 'WS20-C标准BOM'
) v ON v.pc = p.product_code
ON DUPLICATE KEY UPDATE bom_name = VALUES(bom_name), status = VALUES(status);

INSERT INTO bom_item (bom_id, material_id, qty_per, loss_rate, is_key_material)
SELECT b.bom_id, m.material_id, v.qty_per, v.loss_rate, v.is_key
FROM bom b
JOIN product p ON p.product_id = b.product_id
JOIN (
  SELECT 'BOM-FAN-TS30-B' AS bc, 'MAT-MOTOR-40W' AS mc, 1 AS qty_per, 0.01 AS loss_rate, 1 AS is_key
  UNION ALL SELECT 'BOM-FAN-TS30-B', 'MAT-BLADE-30', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-TS30-B', 'MAT-SHELL-TS30', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-TS30-B', 'MAT-SCREW-M4', 6, 0.03, 0
  UNION ALL SELECT 'BOM-FAN-TS30-B', 'MAT-PACK-TS30', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-WS20-C', 'MAT-MOTOR-35W', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-WS20-C', 'MAT-BLADE-20', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-WS20-C', 'MAT-SHELL-WS20', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-WS20-C', 'MAT-SCREW-M4', 6, 0.03, 0
  UNION ALL SELECT 'BOM-FAN-WS20-C', 'MAT-PACK-WS20', 1, 0.01, 0
) v ON v.bc = b.bom_code
JOIN material m ON m.material_code = v.mc
WHERE NOT EXISTS (
  SELECT 1 FROM bom_item bi WHERE bi.bom_id = b.bom_id AND bi.material_id = m.material_id
);

INSERT INTO md_product_bom (item_id, bom_item_id, bom_item_code, bom_item_name, unit_of_measure, item_or_product, quantity, enable_flag, create_time)
SELECT pi.item_id, mi.item_id, mi.item_code, mi.item_name, IFNULL(mi.unit_of_measure, 'PCS'), 'ITEM', bi.qty_per, 'Y', NOW(3)
FROM bom_item bi
JOIN bom b ON bi.bom_id = b.bom_id
JOIN product p ON p.product_id = b.product_id AND p.product_code IN ('FAN-TS30-B', 'FAN-WS20-C')
JOIN md_item pi ON pi.attr1 = 'PRODUCT' AND pi.attr2 = CAST(p.product_id AS CHAR)
JOIN md_item mi ON mi.attr1 = 'MATERIAL' AND mi.attr2 = CAST(bi.material_id AS CHAR)
WHERE NOT EXISTS (
  SELECT 1 FROM md_product_bom pb WHERE pb.item_id = pi.item_id AND pb.bom_item_id = mi.item_id
);

-- ========== 工艺路线 ==========
INSERT INTO process_route (route_code, route_name, version_no, status)
VALUES
  ('ROUTE-TS30-B', 'TS30-B标准装配路线', 'V1.0', 'RELEASED'),
  ('ROUTE-WS20-C', 'WS20-C标准装配路线', 'V1.0', 'RELEASED')
ON DUPLICATE KEY UPDATE route_name = VALUES(route_name), status = VALUES(status);

INSERT IGNORE INTO process_route_step (route_id, step_id, step_seq, station_type, is_must_pass)
SELECT pr.route_id, ps.step_id, v.step_seq, v.station_type, 1
FROM process_route pr
JOIN (
  SELECT 'ROUTE-TS30-B' AS rc, 'STEP-MOTOR' AS sc, 10 AS step_seq, 'ASSEMBLY' AS station_type
  UNION ALL SELECT 'ROUTE-TS30-B', 'STEP-BLADE', 20, 'ASSEMBLY'
  UNION ALL SELECT 'ROUTE-TS30-B', 'STEP-AGING', 30, 'TEST'
  UNION ALL SELECT 'ROUTE-TS30-B', 'STEP-PACK', 40, 'PACKAGE'
  UNION ALL SELECT 'ROUTE-WS20-C', 'STEP-MOTOR', 10, 'ASSEMBLY'
  UNION ALL SELECT 'ROUTE-WS20-C', 'STEP-BLADE', 20, 'ASSEMBLY'
  UNION ALL SELECT 'ROUTE-WS20-C', 'STEP-AGING', 30, 'TEST'
  UNION ALL SELECT 'ROUTE-WS20-C', 'STEP-PACK', 40, 'PACKAGE'
) v ON v.rc = pr.route_code AND pr.version_no = 'V1.0'
JOIN process_step ps ON ps.step_code = v.sc;

INSERT IGNORE INTO product_route (product_id, route_id, is_default, status)
SELECT p.product_id, pr.route_id, 1, 'ENABLED'
FROM product p
JOIN process_route pr ON (
  (p.product_code = 'FAN-TS30-B' AND pr.route_code = 'ROUTE-TS30-B') OR
  (p.product_code = 'FAN-WS20-C' AND pr.route_code = 'ROUTE-WS20-C')
) AND pr.version_no = 'V1.0';

-- 工艺 compat 表
INSERT INTO pro_route (route_id, route_code, route_name, route_desc, enable_flag, attr1, create_time, update_time)
SELECT pr.route_id, pr.route_code, pr.route_name, CONCAT('版本 ', pr.version_no),
  IF(pr.status IN ('ENABLED', 'RELEASED', 'DRAFT'), 'Y', 'N'), CAST(pr.route_id AS CHAR), NOW(3), NOW(3)
FROM process_route pr
WHERE pr.route_code IN ('ROUTE-TS30-B', 'ROUTE-WS20-C') AND pr.version_no = 'V1.0'
ON DUPLICATE KEY UPDATE route_name = VALUES(route_name), enable_flag = VALUES(enable_flag);

INSERT IGNORE INTO pro_route_process (route_id, process_id, process_code, process_name, order_num, next_process_id, create_time)
SELECT prs.route_id, prs.step_id, ps.step_code, ps.step_name, prs.step_seq, 0, NOW(3)
FROM process_route_step prs
JOIN process_route pr ON pr.route_id = prs.route_id AND pr.route_code IN ('ROUTE-TS30-B', 'ROUTE-WS20-C')
JOIN process_step ps ON prs.step_id = ps.step_id;

INSERT IGNORE INTO pro_route_product (route_id, item_id, item_code, item_name, specification, unit_of_measure, unit_name, create_time)
SELECT pr.route_id, mi.item_id, mi.item_code, mi.item_name, mi.specification, mi.unit_of_measure, mi.unit_name, NOW(3)
FROM product_route ppr
JOIN process_route pr ON pr.route_id = ppr.route_id AND pr.route_code IN ('ROUTE-TS30-B', 'ROUTE-WS20-C')
JOIN product p ON p.product_id = ppr.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR);

-- work_order.item_id 补齐新 SKU
UPDATE work_order wo
JOIN product p ON p.product_id = wo.product_id
JOIN md_item mi ON mi.attr1 = 'PRODUCT' AND mi.attr2 = CAST(p.product_id AS CHAR)
SET wo.item_id = mi.item_id
WHERE wo.item_id IS NULL OR wo.item_id = 0;
