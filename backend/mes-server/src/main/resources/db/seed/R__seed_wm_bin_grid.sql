-- 库位态势图演示：三级仓储网格 + 坐标 + 分散库存
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 库区（每仓一个演示库区）==========
INSERT INTO storage_zone (zone_code, zone_name, warehouse_id, area_flag, frozen_flag, create_time)
SELECT 'RAW-ZONE-A', '原料A区', w.warehouse_id, 'Y', 'N', NOW()
FROM warehouse w WHERE w.warehouse_code = 'WH-RAW'
  AND NOT EXISTS (SELECT 1 FROM storage_zone sz WHERE sz.zone_code = 'RAW-ZONE-A');

INSERT INTO storage_zone (zone_code, zone_name, warehouse_id, area_flag, frozen_flag, create_time)
SELECT 'FIN-ZONE-A', '成品A区', w.warehouse_id, 'Y', 'N', NOW()
FROM warehouse w WHERE w.warehouse_code = 'WH-FIN'
  AND NOT EXISTS (SELECT 1 FROM storage_zone sz WHERE sz.zone_code = 'FIN-ZONE-A');

INSERT INTO storage_zone (zone_code, zone_name, warehouse_id, area_flag, frozen_flag, create_time)
SELECT 'SCRAP-ZONE-A', '不良品A区', w.warehouse_id, 'Y', 'N', NOW()
FROM warehouse w WHERE w.warehouse_code = 'WH-SCRAP'
  AND NOT EXISTS (SELECT 1 FROM storage_zone sz WHERE sz.zone_code = 'SCRAP-ZONE-A');

-- ========== 库位网格：WH-RAW 6×5 ==========
INSERT INTO storage_bin (bin_code, bin_name, zone_id, position_x, position_y, position_z, max_loa, enable_flag, frozen_flag, create_time)
SELECT CONCAT('RAW-', CHAR(65 + r.n), LPAD(c.n + 1, 2, '0')),
       CONCAT('原料', CHAR(65 + r.n), '排', LPAD(c.n + 1, 2, '0'), '列'),
       sz.zone_id, c.n, r.n, 0, 1200, 'Y', 'N', NOW()
FROM storage_zone sz
JOIN warehouse w ON w.warehouse_id = sz.warehouse_id AND w.warehouse_code = 'WH-RAW'
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) c
WHERE sz.zone_code = 'RAW-ZONE-A'
ON DUPLICATE KEY UPDATE
  bin_name = VALUES(bin_name), zone_id = VALUES(zone_id),
  position_x = VALUES(position_x), position_y = VALUES(position_y),
  position_z = VALUES(position_z), max_loa = VALUES(max_loa), update_time = NOW();

-- ========== 库位网格：WH-FIN 5×4 ==========
INSERT INTO storage_bin (bin_code, bin_name, zone_id, position_x, position_y, position_z, max_loa, enable_flag, frozen_flag, create_time)
SELECT CONCAT('FIN-', CHAR(65 + r.n), LPAD(c.n + 1, 2, '0')),
       CONCAT('成品', CHAR(65 + r.n), '排', LPAD(c.n + 1, 2, '0'), '列'),
       sz.zone_id, c.n, r.n, 0, 800, 'Y', 'N', NOW()
FROM storage_zone sz
JOIN warehouse w ON w.warehouse_id = sz.warehouse_id AND w.warehouse_code = 'WH-FIN'
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) r
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) c
WHERE sz.zone_code = 'FIN-ZONE-A'
ON DUPLICATE KEY UPDATE
  bin_name = VALUES(bin_name), zone_id = VALUES(zone_id),
  position_x = VALUES(position_x), position_y = VALUES(position_y),
  position_z = VALUES(position_z), max_loa = VALUES(max_loa), update_time = NOW();

-- ========== 库位网格：WH-SCRAP 4×3 ==========
INSERT INTO storage_bin (bin_code, bin_name, zone_id, position_x, position_y, position_z, max_loa, enable_flag, frozen_flag, create_time)
SELECT CONCAT('SCRAP-', CHAR(65 + r.n), LPAD(c.n + 1, 2, '0')),
       CONCAT('不良', CHAR(65 + r.n), '排', LPAD(c.n + 1, 2, '0'), '列'),
       sz.zone_id, c.n, r.n, 0, 500, 'Y', 'N', NOW()
FROM storage_zone sz
JOIN warehouse w ON w.warehouse_id = sz.warehouse_id AND w.warehouse_code = 'WH-SCRAP'
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2) r
CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) c
WHERE sz.zone_code = 'SCRAP-ZONE-A'
ON DUPLICATE KEY UPDATE
  bin_name = VALUES(bin_name), zone_id = VALUES(zone_id),
  position_x = VALUES(position_x), position_y = VALUES(position_y),
  position_z = VALUES(position_z), max_loa = VALUES(max_loa), update_time = NOW();

-- 演示：冻结 2 个库位
UPDATE storage_bin sb
JOIN storage_zone sz ON sz.zone_id = sb.zone_id
SET sb.frozen_flag = 'Y', sb.update_time = NOW()
WHERE sb.bin_code IN ('RAW-D04', 'FIN-B03');

-- ========== 将已有库存绑定到网格库位（按批次编码映射）==========
UPDATE wm_material_stock s
JOIN storage_bin sb ON sb.bin_code = CASE
  WHEN s.batch_code LIKE 'BATCH-BLADE40%' THEN 'RAW-A01'
  WHEN s.batch_code LIKE 'BATCH-MMOTOR%' THEN 'RAW-A02'
  WHEN s.batch_code LIKE 'BATCH-MOTOR55%' THEN 'RAW-B01'
  WHEN s.batch_code LIKE 'BATCH-SCREW%' THEN 'RAW-B02'
  WHEN s.batch_code LIKE 'BATCH-SHELL-FS40%' THEN 'RAW-C01'
  WHEN s.batch_code LIKE 'BATCH-PACK-FS40-NEAR%' THEN 'RAW-C02'
  WHEN s.batch_code LIKE 'FIN-FAN-FS40%' THEN 'FIN-A01'
  WHEN s.batch_code LIKE 'FIN-FAN-TS30%' THEN 'FIN-A02'
  WHEN s.batch_code LIKE 'FIN-FAN-WS20%' THEN 'FIN-B01'
  WHEN s.batch_code LIKE 'SCRAP-FAN%' THEN 'SCRAP-A01'
  ELSE NULL END
JOIN storage_zone sz ON sz.zone_id = sb.zone_id
SET s.area_id = sb.bin_id, s.area_code = sb.bin_code, s.area_name = sb.bin_name,
    s.location_id = sz.zone_id, s.location_code = sz.zone_code, s.location_name = sz.zone_name,
    s.update_time = NOW()
WHERE sb.bin_code IS NOT NULL;

-- ========== 补充网格演示库存（空/在库/冻结/临期/混放）==========
INSERT INTO wm_material_stock (
  item_id, item_type_id, item_code, item_name, specification, unit_name,
  batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT mi.item_id, mi.item_type_id, mi.item_code, mi.item_name, mi.specification, mi.unit_name,
  v.batch_code, w.warehouse_id, w.warehouse_code, w.warehouse_name,
  sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name,
  v.qty, v.reserved, v.recpt_date, v.expire_date, v.frozen, NOW()
FROM (
  SELECT 'MAT-MOTOR-55W' AS mc, 'GRID-RAW-A03-01' AS batch_code, 'WH-RAW' AS wh, 'RAW-A03' AS bin, 520 AS qty, 30 AS reserved,
    DATE_SUB(CURDATE(), INTERVAL 10 DAY) AS recpt_date, DATE_ADD(CURDATE(), INTERVAL 400 DAY) AS expire_date, 'N' AS frozen
  UNION ALL SELECT 'MAT-BLADE-40', 'GRID-RAW-A04-01', 'WH-RAW', 'RAW-A04', 380, 0,
    DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 500 DAY), 'N'
  UNION ALL SELECT 'MAT-SHELL-FS40', 'GRID-RAW-A05-01', 'WH-RAW', 'RAW-A05', 240, 15,
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 360 DAY), 'N'
  UNION ALL SELECT 'MAT-SCREW-M4', 'GRID-RAW-A06-01', 'WH-RAW', 'RAW-A06', 3200, 100,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 800 DAY), 'N'
  UNION ALL SELECT 'MAT-PACK-FS40', 'GRID-RAW-B03-01', 'WH-RAW', 'RAW-B03', 180, 0,
    DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 300 DAY), 'N'
  UNION ALL SELECT 'MAT-MOTOR-40W', 'GRID-RAW-B04-01', 'WH-RAW', 'RAW-B04', 410, 20,
    DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 450 DAY), 'N'
  UNION ALL SELECT 'MAT-BLADE-30', 'GRID-RAW-B05-01', 'WH-RAW', 'RAW-B05', 290, 0,
    DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 380 DAY), 'N'
  UNION ALL SELECT 'MAT-SHELL-TS30', 'GRID-RAW-B06-01', 'WH-RAW', 'RAW-B06', 160, 10,
    DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 320 DAY), 'N'
  UNION ALL SELECT 'MAT-PACK-TS30', 'GRID-RAW-C03-01', 'WH-RAW', 'RAW-C03', 95, 0,
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 18 DAY), 'N'
  UNION ALL SELECT 'MAT-MOTOR-35W', 'GRID-RAW-C04-01', 'WH-RAW', 'RAW-C04', 72, 0,
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 22 DAY), 'N'
  UNION ALL SELECT 'MAT-SCREW-M4', 'GRID-RAW-C05-FRZ', 'WH-RAW', 'RAW-C05', 890, 890,
    DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 600 DAY), 'Y'
  UNION ALL SELECT 'MAT-BLADE-20', 'GRID-RAW-D01-01', 'WH-RAW', 'RAW-D01', 210, 0,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 280 DAY), 'N'
  UNION ALL SELECT 'MAT-PACK-WS20', 'GRID-RAW-D02-01', 'WH-RAW', 'RAW-D02', 130, 5,
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 260 DAY), 'N'
  UNION ALL SELECT 'MAT-SHELL-WS20', 'GRID-RAW-D03-01', 'WH-RAW', 'RAW-D03', 88, 0,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 240 DAY), 'N'
  UNION ALL SELECT 'MAT-MOTOR-60W', 'GRID-RAW-D05-01', 'WH-RAW', 'RAW-D05', 45, 0,
    DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 200 DAY), 'N'
  UNION ALL SELECT 'MAT-MOTOR-55W', 'GRID-RAW-E01-01', 'WH-RAW', 'RAW-E01', 360, 40,
    DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_ADD(CURDATE(), INTERVAL 420 DAY), 'N'
  UNION ALL SELECT 'MAT-BLADE-40', 'GRID-RAW-E02-01', 'WH-RAW', 'RAW-E02', 275, 0,
    DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 410 DAY), 'N'
  UNION ALL SELECT 'MAT-SHELL-FS40', 'GRID-RAW-E03-MIX', 'WH-RAW', 'RAW-E03', 120, 0,
    DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 350 DAY), 'N'
  UNION ALL SELECT 'MAT-PACK-FS40', 'GRID-RAW-E03-MIX2', 'WH-RAW', 'RAW-E03', 80, 0,
    DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 340 DAY), 'N'
  UNION ALL SELECT 'FAN-FS40-A', 'GRID-FIN-A03-01', 'WH-FIN', 'FIN-A03', 96, 12,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'N'
  UNION ALL SELECT 'FAN-TS30-B', 'GRID-FIN-A04-01', 'WH-FIN', 'FIN-A04', 54, 8,
    DATE_SUB(CURDATE(), INTERVAL 4 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'N'
  UNION ALL SELECT 'FAN-WS20-C', 'GRID-FIN-A05-01', 'WH-FIN', 'FIN-A05', 38, 0,
    DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'N'
  UNION ALL SELECT 'FAN-FS40-A', 'GRID-FIN-B02-01', 'WH-FIN', 'FIN-B02', 72, 6,
    DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'N'
  UNION ALL SELECT 'FAN-TS30-B', 'GRID-FIN-B04-01', 'WH-FIN', 'FIN-B04', 41, 0,
    DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'N'
  UNION ALL SELECT 'FAN-WS20-C', 'GRID-FIN-C01-01', 'WH-FIN', 'FIN-C01', 28, 0,
    CURDATE(), DATE_ADD(CURDATE(), INTERVAL 20 DAY), 'N'
  UNION ALL SELECT 'FAN-FS40-A', 'GRID-FIN-C02-FRZ', 'WH-FIN', 'FIN-C02', 15, 15,
    DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 300 DAY), 'Y'
  UNION ALL SELECT 'FAN-TS30-B', 'GRID-FIN-D01-01', 'WH-FIN', 'FIN-D01', 33, 0,
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 280 DAY), 'N'
  UNION ALL SELECT 'FAN-WS20-C', 'GRID-FIN-D03-01', 'WH-FIN', 'FIN-D03', 19, 0,
    DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 260 DAY), 'N'
  UNION ALL SELECT 'FAN-FS40-A', 'GRID-SCRAP-A02-01', 'WH-SCRAP', 'SCRAP-A02', 4, 0,
    DATE_SUB(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 'N'
  UNION ALL SELECT 'FAN-TS30-B', 'GRID-SCRAP-B01-01', 'WH-SCRAP', 'SCRAP-B01', 3, 0,
    DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_ADD(CURDATE(), INTERVAL 80 DAY), 'N'
  UNION ALL SELECT 'FAN-WS20-C', 'GRID-SCRAP-B02-01', 'WH-SCRAP', 'SCRAP-B02', 2, 0,
    DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 70 DAY), 'N'
) v
JOIN md_item mi ON mi.item_code = v.mc
JOIN warehouse w ON w.warehouse_code = v.wh
JOIN storage_zone sz ON sz.warehouse_id = w.warehouse_id
  AND sz.zone_code = CASE v.wh WHEN 'WH-RAW' THEN 'RAW-ZONE-A' WHEN 'WH-FIN' THEN 'FIN-ZONE-A' ELSE 'SCRAP-ZONE-A' END
JOIN storage_bin sb ON sb.zone_id = sz.zone_id AND sb.bin_code = v.bin
WHERE NOT EXISTS (SELECT 1 FROM wm_material_stock s WHERE s.batch_code = v.batch_code);
