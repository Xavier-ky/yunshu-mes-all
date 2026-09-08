-- FAN-TS30（历史简码）补齐原生 BOM，与 FAN-TS30-B 物料结构一致，供齐套/工单快照使用

INSERT INTO bom (product_id, bom_code, bom_name, version_no, status)
SELECT p.product_id, 'BOM-FAN-TS30', 'TS30标准BOM', 'V1.0', 'RELEASED'
FROM product p
WHERE p.product_code = 'FAN-TS30'
  AND NOT EXISTS (
    SELECT 1 FROM bom b WHERE b.product_id = p.product_id AND b.bom_code = 'BOM-FAN-TS30'
  );

INSERT INTO bom_item (bom_id, material_id, qty_per, loss_rate, is_key_material)
SELECT b.bom_id, m.material_id, v.qty_per, v.loss_rate, v.is_key
FROM bom b
JOIN product p ON p.product_id = b.product_id AND p.product_code = 'FAN-TS30'
JOIN (
  SELECT 'MAT-MOTOR-40W' AS mc, 1 AS qty_per, 0.01 AS loss_rate, 1 AS is_key
  UNION ALL SELECT 'MAT-BLADE-30', 1, 0.01, 1
  UNION ALL SELECT 'MAT-SHELL-TS30', 1, 0.01, 1
  UNION ALL SELECT 'MAT-SCREW-M4', 6, 0.03, 0
  UNION ALL SELECT 'MAT-PACK-TS30', 1, 0.01, 0
) v
JOIN material m ON m.material_code = v.mc
WHERE NOT EXISTS (
  SELECT 1 FROM bom_item bi WHERE bi.bom_id = b.bom_id AND bi.material_id = m.material_id
);

-- compat 层 md_product_bom（工单 BOM 页）
INSERT INTO md_product_bom (item_id, bom_item_id, bom_item_code, bom_item_name, unit_of_measure, item_or_product, quantity, enable_flag, create_time)
SELECT pi.item_id, mi.item_id, mi.item_code, mi.item_name, IFNULL(mi.unit_of_measure, 'PCS'), 'ITEM', bi.qty_per, 'Y', NOW(3)
FROM bom_item bi
JOIN bom b ON bi.bom_id = b.bom_id
JOIN product p ON p.product_id = b.product_id AND p.product_code = 'FAN-TS30'
JOIN md_item pi ON pi.item_code = 'FAN-TS30'
JOIN md_item mi ON mi.attr1 = 'MATERIAL' AND mi.attr2 = CAST(bi.material_id AS CHAR)
WHERE NOT EXISTS (
  SELECT 1 FROM md_product_bom pb WHERE pb.item_id = pi.item_id AND pb.bom_item_id = mi.item_id
);

-- 工艺路线复用 TS30-B 标准路线
INSERT IGNORE INTO product_route (product_id, route_id, is_default, status)
SELECT p.product_id, pr.route_id, 1, 'ENABLED'
FROM product p
JOIN process_route pr ON pr.route_code = 'ROUTE-TS30-B' AND pr.version_no = 'V1.0'
WHERE p.product_code = 'FAN-TS30';

-- 已有工单补写 bom_id（齐套按 product 查 BOM，此项便于工单详情展示）
UPDATE work_order wo
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-TS30'
JOIN bom b ON b.product_id = p.product_id AND b.bom_code = 'BOM-FAN-TS30'
SET wo.bom_id = b.bom_id
WHERE wo.bom_id IS NULL;

-- 已有工单补 BOM 快照（work_order_bom），与 generateBomSnapshot 逻辑一致
INSERT INTO work_order_bom (work_order_id, material_id, material_code, material_name, unit_code, item_or_product, quantity)
SELECT wo.work_order_id, bi.material_id, m.material_code, m.material_name, 'PCS', 'ITEM',
       wo.plan_qty * bi.qty_per
FROM work_order wo
JOIN product p ON p.product_id = wo.product_id AND p.product_code = 'FAN-TS30'
JOIN bom b ON b.product_id = p.product_id AND b.bom_code = 'BOM-FAN-TS30'
JOIN bom_item bi ON bi.bom_id = b.bom_id
JOIN material m ON m.material_id = bi.material_id
WHERE NOT EXISTS (
  SELECT 1 FROM work_order_bom wob
  WHERE wob.work_order_id = wo.work_order_id AND wob.material_id = bi.material_id
);
