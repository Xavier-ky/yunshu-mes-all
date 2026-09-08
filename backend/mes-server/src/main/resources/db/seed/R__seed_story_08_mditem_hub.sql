-- Story seed 08: 物料产品 Hub 演示数据扩充（物料清单 / BOM / 分类 / 单位 / 客户 / 供应商）
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 计量单位 ==========
INSERT INTO uom (unit_code, unit_name)
VALUES
  ('CM', '厘米'),
  ('M', '米'),
  ('KG', '千克'),
  ('G', '克'),
  ('ROL', '卷'),
  ('PAIR', '双')
ON DUPLICATE KEY UPDATE unit_name = VALUES(unit_name);

INSERT INTO md_unit_measure (measure_code, measure_name, primary_flag, enable_flag, create_time)
VALUES
  ('CM', '厘米', 'Y', 'Y', NOW(3)),
  ('M', '米', 'Y', 'Y', NOW(3)),
  ('KG', '千克', 'Y', 'Y', NOW(3)),
  ('G', '克', 'Y', 'Y', NOW(3)),
  ('ROL', '卷', 'Y', 'Y', NOW(3)),
  ('PAIR', '双', 'Y', 'Y', NOW(3))
ON DUPLICATE KEY UPDATE measure_name = VALUES(measure_name), enable_flag = VALUES(enable_flag);

-- ========== 物料产品分类（子类）==========
INSERT INTO md_item_type (item_type_code, item_type_name, parent_type_id, ancestors, item_or_product, order_num, enable_flag)
VALUES
  ('PRODUCT_CEIL', '吊扇', 1, '0,1', 'PRODUCT', 2, 'Y'),
  ('PRODUCT_BOX', '箱式扇', 1, '0,1', 'PRODUCT', 3, 'Y'),
  ('PRODUCT_TOWER', '塔扇', 1, '0,1', 'PRODUCT', 4, 'Y'),
  ('PRODUCT_IND', '工业扇', 1, '0,1', 'PRODUCT', 5, 'Y'),
  ('ITEM_ELEC', '电子件', 2, '0,2', 'ITEM', 5, 'Y')
ON DUPLICATE KEY UPDATE item_type_name = VALUES(item_type_name), enable_flag = VALUES(enable_flag);

-- ========== 客户 / 供应商 ==========
INSERT INTO md_client (client_code, client_name, client_nick, client_type, address, tel, contact1, contact1_tel, enable_flag, create_time)
VALUES
  ('C006', '中原家电经销', '中原经销', 'ENTERPRISE', '郑州市金水区花园路128号', '0371-88880006', '孙经理', '13800001006', 'Y', NOW(3)),
  ('C007', '长三角连锁超市', '长三角连锁', 'ENTERPRISE', '杭州市西湖区文三路200号', '0571-88880007', '钱总', '13800001007', 'Y', NOW(3)),
  ('C008', '东北批发中心', '东北批发', 'ENTERPRISE', '沈阳市铁西区建设大路66号', '024-88880008', '赵采购', '13800001008', 'Y', NOW(3)),
  ('C009', '云贵高原电器', '云贵电器', 'ENTERPRISE', '昆明市官渡区北京路188号', '0871-88880009', '周经理', '13800001009', 'Y', NOW(3)),
  ('C010', '海峡两岸贸易', '海峡贸易', 'ENTERPRISE', '厦门市思明区湖滨南路99号', '0592-88880010', '吴总监', '13800001010', 'Y', NOW(3))
ON DUPLICATE KEY UPDATE
  client_name = VALUES(client_name), client_nick = VALUES(client_nick),
  address = VALUES(address), tel = VALUES(tel), contact1 = VALUES(contact1);

INSERT INTO md_vendor (vendor_code, vendor_name, vendor_nick, vendor_type, address, tel, contact1, contact1_tel, enable_flag, create_time)
VALUES
  ('V006', '电容元件厂', '电容元件', 'MATERIAL', '常州市武进区电子产业园8号', '0519-66660006', '马工', '13900002006', 'Y', NOW(3)),
  ('V007', '线材电缆公司', '线材电缆', 'MATERIAL', '无锡市滨湖区电缆市场B区', '0510-66660007', '黄销售', '13900002007', 'Y', NOW(3)),
  ('V008', '遥控模块科技', '遥控模块', 'MATERIAL', '深圳市宝安区固戍工业园12栋', '0755-66660008', '林研发', '13900002008', 'Y', NOW(3)),
  ('V009', '标签印刷厂', '标签印刷', 'MATERIAL', '中山市小榄镇印刷路5号', '0760-66660009', '何厂长', '13900002009', 'Y', NOW(3)),
  ('V010', '轴承标准件商', '轴承标准件', 'MATERIAL', '宁波市镇海区轴承城C区', '0574-66660010', '徐经理', '13900002010', 'Y', NOW(3))
ON DUPLICATE KEY UPDATE
  vendor_name = VALUES(vendor_name), vendor_nick = VALUES(vendor_nick),
  address = VALUES(address), tel = VALUES(tel);

-- ========== 新产品 SKU ==========
INSERT INTO product (product_code, product_name, product_model, product_category, status)
VALUES
  ('FAN-CF52-D', '52cm吊扇D型', 'CF52-D', 'CEILING_FAN', 'ENABLED'),
  ('FAN-IF60-E', '60cm工业扇E型', 'IF60-E', 'INDUSTRIAL_FAN', 'ENABLED'),
  ('FAN-BF16-F', '16寸箱式扇F型', 'BF16-F', 'BOX_FAN', 'ENABLED'),
  ('FAN-TF35-G', '35cm塔扇G型', 'TF35-G', 'TOWER_FAN', 'ENABLED')
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name), product_model = VALUES(product_model);

INSERT INTO product_spec (product_id, spec_name, spec_value, unit_code)
SELECT p.product_id, v.spec_name, v.spec_value, v.unit_code
FROM product p
JOIN (
  SELECT 'FAN-CF52-D' AS pc, '扇叶直径' AS spec_name, '52' AS spec_value, 'CM' AS unit_code
  UNION ALL SELECT 'FAN-CF52-D', '电机功率', '65', 'W'
  UNION ALL SELECT 'FAN-CF52-D', '档位数', '3', NULL
  UNION ALL SELECT 'FAN-IF60-E', '扇叶直径', '60', 'CM'
  UNION ALL SELECT 'FAN-IF60-E', '电机功率', '75', 'W'
  UNION ALL SELECT 'FAN-IF60-E', '防护等级', 'IP54', NULL
  UNION ALL SELECT 'FAN-BF16-F', '扇叶直径', '40', 'CM'
  UNION ALL SELECT 'FAN-BF16-F', '电机功率', '28', 'W'
  UNION ALL SELECT 'FAN-BF16-F', '颜色', '黑色', NULL
  UNION ALL SELECT 'FAN-TF35-G', '高度', '90', 'CM'
  UNION ALL SELECT 'FAN-TF35-G', '电机功率', '45', 'W'
  UNION ALL SELECT 'FAN-TF35-G', '遥控', '支持', NULL
) v ON v.pc = p.product_code
WHERE NOT EXISTS (
  SELECT 1 FROM product_spec ps WHERE ps.product_id = p.product_id AND ps.spec_name = v.spec_name
);

-- ========== 新物料 ==========
INSERT INTO material (material_code, material_name, material_type, unit_id, is_key_material, status)
VALUES
  ('MAT-MOTOR-65W', '65W吊扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-MOTOR-75W', '75W工业扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-MOTOR-28W', '28W箱式扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-MOTOR-45W-T', '45W塔扇电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-BLADE-52', '52cm吊扇叶片组', 'BLADE', (SELECT unit_id FROM uom WHERE unit_code = 'SET'), 1, 'ENABLED'),
  ('MAT-GRILLE-16', '16寸前网罩', 'SHELL', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-BASE-TF35', 'TF35底座组件', 'SHELL', (SELECT unit_id FROM uom WHERE unit_code = 'SET'), 1, 'ENABLED'),
  ('MAT-CAP-2.5UF', '2.5UF启动电容', 'CAPACITOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 1, 'ENABLED'),
  ('MAT-WIRE-1.0', '1.0mm²电源线', 'WIRE', (SELECT unit_id FROM uom WHERE unit_code = 'M'), 0, 'ENABLED'),
  ('MAT-REMOTE-RF', 'RF遥控器', 'REMOTE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-LABEL-CE', 'CE认证标签', 'LABEL', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-FOAM-PAD', '缓冲泡沫垫', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-PACK-CF52', 'CF52包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-PACK-IF60', 'IF60包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-PACK-BF16', 'BF16包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-PACK-TF35', 'TF35包装箱', 'PACKAGE', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-FILTER-01', '防尘滤网', 'FILTER', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-BEARING-6201', '6201深沟球轴承', 'BEARING', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED'),
  ('MAT-BELT-V', '三角传动带', 'BELT', (SELECT unit_id FROM uom WHERE unit_code = 'PCS'), 0, 'ENABLED')
ON DUPLICATE KEY UPDATE material_name = VALUES(material_name), is_key_material = VALUES(is_key_material);

-- ========== md_item 同步（新产品 / 新物料）==========
INSERT INTO md_item (item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT p.product_code, p.product_name, IFNULL(p.product_model, ''), 'PCS', '件',
  'PRODUCT',
  CASE p.product_category
    WHEN 'CEILING_FAN' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'PRODUCT_CEIL')
    WHEN 'INDUSTRIAL_FAN' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'PRODUCT_IND')
    WHEN 'BOX_FAN' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'PRODUCT_BOX')
    WHEN 'TOWER_FAN' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'PRODUCT_TOWER')
    ELSE 3
  END,
  CASE p.product_category
    WHEN 'CEILING_FAN' THEN 'PRODUCT_CEIL'
    WHEN 'INDUSTRIAL_FAN' THEN 'PRODUCT_IND'
    WHEN 'BOX_FAN' THEN 'PRODUCT_BOX'
    WHEN 'TOWER_FAN' THEN 'PRODUCT_TOWER'
    ELSE 'PRODUCT_FIN'
  END,
  CASE p.product_category
    WHEN 'CEILING_FAN' THEN '吊扇'
    WHEN 'INDUSTRIAL_FAN' THEN '工业扇'
    WHEN 'BOX_FAN' THEN '箱式扇'
    WHEN 'TOWER_FAN' THEN '塔扇'
    ELSE IFNULL(p.product_category, '成品')
  END,
  IF(p.status = 'ENABLED', 'Y', 'N'), 'PRODUCT', CAST(p.product_id AS CHAR), NOW(3), NOW(3)
FROM product p
WHERE p.product_code IN ('FAN-CF52-D', 'FAN-IF60-E', 'FAN-BF16-F', 'FAN-TF35-G') AND p.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM md_item mi WHERE mi.item_code = p.product_code);

INSERT INTO md_item (item_code, item_name, specification, unit_of_measure, unit_name,
  item_or_product, item_type_id, item_type_code, item_type_name, enable_flag, attr1, attr2, create_time, update_time)
SELECT m.material_code, m.material_name, '', IFNULL(u.unit_code, 'PCS'), IFNULL(u.unit_name, '件'),
  'ITEM',
  CASE
    WHEN m.material_type IN ('CAPACITOR', 'REMOTE') THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'ITEM_ELEC')
    WHEN m.material_code LIKE 'MAT-SHELL-%' OR m.material_code LIKE 'MAT-BASE-%' OR m.material_code LIKE 'MAT-GRILLE-%' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'ITEM_SEMI')
    WHEN m.material_type IN ('PACKAGE', 'LABEL', 'WIRE', 'FILTER') OR m.material_code LIKE 'MAT-PACK-%' OR m.material_code LIKE 'MAT-SCREW-%' OR m.material_code LIKE 'MAT-FOAM-%' OR m.material_code LIKE 'MAT-LABEL-%' OR m.material_code LIKE 'MAT-WIRE-%' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'ITEM_AUX')
    WHEN m.material_type IN ('BEARING', 'BELT') OR m.material_code LIKE 'MAT-BEARING-%' OR m.material_code LIKE 'MAT-BELT-%' OR m.material_code LIKE 'MAT-FILTER-%' THEN (SELECT item_type_id FROM md_item_type WHERE item_type_code = 'ITEM_SPARE')
    ELSE 4
  END,
  CASE
    WHEN m.material_type IN ('CAPACITOR', 'REMOTE') THEN 'ITEM_ELEC'
    WHEN m.material_code LIKE 'MAT-SHELL-%' OR m.material_code LIKE 'MAT-BASE-%' OR m.material_code LIKE 'MAT-GRILLE-%' OR m.material_code LIKE 'MAT-BLADE-%' THEN
      CASE WHEN m.material_type = 'BLADE' THEN 'ITEM_RAW' ELSE 'ITEM_SEMI' END
    WHEN m.material_type IN ('PACKAGE', 'LABEL', 'WIRE', 'FILTER') OR m.material_code LIKE 'MAT-PACK-%' OR m.material_code LIKE 'MAT-SCREW-%' OR m.material_code LIKE 'MAT-FOAM-%' OR m.material_code LIKE 'MAT-LABEL-%' OR m.material_code LIKE 'MAT-WIRE-%' THEN 'ITEM_AUX'
    WHEN m.material_type IN ('BEARING', 'BELT') OR m.material_code LIKE 'MAT-BEARING-%' OR m.material_code LIKE 'MAT-BELT-%' OR m.material_code LIKE 'MAT-FILTER-%' THEN 'ITEM_SPARE'
    ELSE 'ITEM_RAW'
  END,
  CASE
    WHEN m.material_type IN ('CAPACITOR', 'REMOTE') THEN '电子件'
    WHEN m.material_code LIKE 'MAT-SHELL-%' OR m.material_code LIKE 'MAT-BASE-%' OR m.material_code LIKE 'MAT-GRILLE-%' THEN '半成品'
    WHEN m.material_type IN ('PACKAGE', 'LABEL', 'WIRE', 'FILTER') OR m.material_code LIKE 'MAT-PACK-%' OR m.material_code LIKE 'MAT-SCREW-%' OR m.material_code LIKE 'MAT-FOAM-%' OR m.material_code LIKE 'MAT-LABEL-%' OR m.material_code LIKE 'MAT-WIRE-%' THEN '辅料'
    WHEN m.material_type IN ('BEARING', 'BELT') OR m.material_code LIKE 'MAT-BEARING-%' OR m.material_code LIKE 'MAT-BELT-%' OR m.material_code LIKE 'MAT-FILTER-%' THEN '备品备件'
    ELSE IFNULL(m.material_type, '原材料')
  END,
  IF(m.status = 'ENABLED', 'Y', 'N'), 'MATERIAL', CAST(m.material_id AS CHAR), NOW(3), NOW(3)
FROM material m
LEFT JOIN uom u ON m.unit_id = u.unit_id
WHERE m.material_code IN (
  'MAT-MOTOR-65W','MAT-MOTOR-75W','MAT-MOTOR-28W','MAT-MOTOR-45W-T',
  'MAT-BLADE-52','MAT-GRILLE-16','MAT-BASE-TF35','MAT-CAP-2.5UF',
  'MAT-WIRE-1.0','MAT-REMOTE-RF','MAT-LABEL-CE','MAT-FOAM-PAD',
  'MAT-PACK-CF52','MAT-PACK-IF60','MAT-PACK-BF16','MAT-PACK-TF35',
  'MAT-FILTER-01','MAT-BEARING-6201','MAT-BELT-V'
) AND m.is_deleted = 0
  AND NOT EXISTS (SELECT 1 FROM md_item mi WHERE mi.item_code = m.material_code);

-- ========== 原生 BOM 表 ==========
INSERT INTO bom (product_id, bom_code, bom_name, version_no, status)
SELECT p.product_id, v.bom_code, v.bom_name, 'V1.0', 'RELEASED'
FROM product p
JOIN (
  SELECT 'FAN-CF52-D' AS pc, 'BOM-FAN-CF52-D' AS bom_code, 'CF52-D标准BOM' AS bom_name
  UNION ALL SELECT 'FAN-IF60-E', 'BOM-FAN-IF60-E', 'IF60-E标准BOM'
  UNION ALL SELECT 'FAN-BF16-F', 'BOM-FAN-BF16-F', 'BF16-F标准BOM'
  UNION ALL SELECT 'FAN-TF35-G', 'BOM-FAN-TF35-G', 'TF35-G标准BOM'
) v ON v.pc = p.product_code
ON DUPLICATE KEY UPDATE bom_name = VALUES(bom_name), status = VALUES(status);

INSERT INTO bom_item (bom_id, material_id, qty_per, loss_rate, is_key_material)
SELECT b.bom_id, m.material_id, v.qty_per, v.loss_rate, v.is_key
FROM bom b
JOIN (
  SELECT 'BOM-FAN-CF52-D' AS bc, 'MAT-MOTOR-65W' AS mc, 1 AS qty_per, 0.01 AS loss_rate, 1 AS is_key
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-BLADE-52', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-CAP-2.5UF', 1, 0.02, 1
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-WIRE-1.0', 1.5, 0.05, 0
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-SCREW-M4', 10, 0.03, 0
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-LABEL-CE', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-CF52-D', 'MAT-PACK-CF52', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-MOTOR-75W', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-BLADE-40', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-GRILLE-16', 2, 0.02, 1
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-BEARING-6201', 2, 0.02, 0
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-SCREW-M4', 12, 0.03, 0
  UNION ALL SELECT 'BOM-FAN-IF60-E', 'MAT-PACK-IF60', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-MOTOR-28W', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-BLADE-40', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-GRILLE-16', 1, 0.02, 1
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-CAP-2.5UF', 1, 0.02, 1
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-FOAM-PAD', 2, 0.05, 0
  UNION ALL SELECT 'BOM-FAN-BF16-F', 'MAT-PACK-BF16', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-MOTOR-45W-T', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-BASE-TF35', 1, 0.01, 1
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-REMOTE-RF', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-FILTER-01', 1, 0.02, 0
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-WIRE-1.0', 1.2, 0.05, 0
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-LABEL-CE', 1, 0.01, 0
  UNION ALL SELECT 'BOM-FAN-TF35-G', 'MAT-PACK-TF35', 1, 0.01, 0
) v ON v.bc = b.bom_code
JOIN material m ON m.material_code = v.mc
WHERE NOT EXISTS (
  SELECT 1 FROM bom_item bi WHERE bi.bom_id = b.bom_id AND bi.material_id = m.material_id
);

-- ========== md_product_bom（新产品 + 历史 SKU 补齐）==========
INSERT INTO md_product_bom (item_id, bom_item_id, bom_item_code, bom_item_name, unit_of_measure, item_or_product, quantity, enable_flag, create_time)
SELECT pi.item_id, mi.item_id, mi.item_code, mi.item_name, IFNULL(mi.unit_of_measure, 'PCS'), 'ITEM', bi.qty_per, 'Y', NOW(3)
FROM bom_item bi
JOIN bom b ON bi.bom_id = b.bom_id
JOIN product p ON p.product_id = b.product_id
JOIN md_item pi ON pi.attr1 = 'PRODUCT' AND pi.attr2 = CAST(p.product_id AS CHAR)
JOIN md_item mi ON mi.attr1 = 'MATERIAL' AND mi.attr2 = CAST(bi.material_id AS CHAR)
WHERE p.product_code IN ('FAN-CF52-D', 'FAN-IF60-E', 'FAN-BF16-F', 'FAN-TF35-G')
  AND NOT EXISTS (
    SELECT 1 FROM md_product_bom pb WHERE pb.item_id = pi.item_id AND pb.bom_item_id = mi.item_id
  );

-- 为无 BOM 的历史产品码补齐（复制对应正式 SKU 的 BOM）
INSERT INTO md_product_bom (item_id, bom_item_id, bom_item_code, bom_item_name, unit_of_measure, item_or_product, quantity, enable_flag, create_time)
SELECT orphan.item_id, pb.bom_item_id, pb.bom_item_code, pb.bom_item_name, pb.unit_of_measure, pb.item_or_product, pb.quantity, pb.enable_flag, NOW(3)
FROM (
  SELECT 'FAN-FS40' AS orphan_code, 'FAN-FS40-A' AS ref_code
  UNION ALL SELECT 'FAN-TS30', 'FAN-TS30-B'
  UNION ALL SELECT 'FAN-WS20', 'FAN-WS20-C'
  UNION ALL SELECT 'P-FAN-001', 'FAN-FS40-A'
) m
JOIN md_item orphan ON orphan.item_code = m.orphan_code
JOIN md_item ref ON ref.item_code = m.ref_code
JOIN md_product_bom pb ON pb.item_id = ref.item_id
WHERE NOT EXISTS (
  SELECT 1 FROM md_product_bom x WHERE x.item_id = orphan.item_id AND x.bom_item_id = pb.bom_item_id
);
