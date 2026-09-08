-- Story seed 11: FS40-A 原材料库存补量（支持多次齐套/领料演示）
-- 覆盖 FAN-FS40-A BOM 5 项；按单台 100 台计，约可支撑 20+ 次齐套预留
SET NAMES utf8mb4;
USE fan_mes;

-- ========== 1. 抬升主批次可用量（不低于目标值）==========
UPDATE inventory_batch ib
JOIN material m ON m.material_id = ib.material_id
SET ib.available_qty = CASE m.material_code
    WHEN 'MAT-MOTOR-55W' THEN GREATEST(ib.available_qty, 6000.0000)
    WHEN 'MAT-BLADE-40'  THEN GREATEST(ib.available_qty, 5000.0000)
    WHEN 'MAT-SHELL-FS40' THEN GREATEST(ib.available_qty, 5000.0000)
    WHEN 'MAT-SCREW-M4'  THEN GREATEST(ib.available_qty, 12000.0000)
    WHEN 'MAT-PACK-FS40' THEN GREATEST(ib.available_qty, 5000.0000)
    ELSE ib.available_qty
  END,
  ib.quality_status = 'QUALIFIED',
  ib.status = 'IN_STOCK'
WHERE ib.batch_no IN (
  'BATCH-MOTOR-202607', 'BATCH-BLADE-202607', 'BATCH-SHELL-202607',
  'BATCH-SCREW-202607', 'BATCH-PACK-202607'
);

-- ========== 2. 追加补充批次（二次来料，便于后续演示）==========
INSERT INTO inventory_batch (
  material_id, warehouse_id, location_id, batch_no, supplier_batch_no,
  available_qty, locked_qty, quality_status, received_at, expire_date, status
)
SELECT m.material_id,
  (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
  NULL,
  v.batch_no, v.supplier_batch, v.avail_qty, 0.0000, 'QUALIFIED',
  NOW(), DATE_ADD(CURDATE(), INTERVAL 540 DAY), 'IN_STOCK'
FROM (
  SELECT 'MAT-MOTOR-55W' AS mc, 'BATCH-MOTOR-SUPP-20260714' AS batch_no,
    'SUP-MOTOR-2507-A' AS supplier_batch, 5000.0000 AS avail_qty
  UNION ALL SELECT 'MAT-BLADE-40', 'BATCH-BLADE-SUPP-20260714', 'SUP-BLADE-2507-A', 4000.0000
  UNION ALL SELECT 'MAT-SHELL-FS40', 'BATCH-SHELL-SUPP-20260714', 'SUP-SHELL-2507-A', 4000.0000
  UNION ALL SELECT 'MAT-SCREW-M4', 'BATCH-SCREW-SUPP-20260714', 'SUP-SCREW-2507-A', 20000.0000
  UNION ALL SELECT 'MAT-PACK-FS40', 'BATCH-PACK-SUPP-20260714', 'SUP-PACK-2507-A', 3000.0000
) v
JOIN material m ON m.material_code = v.mc
ON DUPLICATE KEY UPDATE
  available_qty = GREATEST(inventory_batch.available_qty, VALUES(available_qty)),
  quality_status = 'QUALIFIED',
  status = 'IN_STOCK';

-- ========== 3. 同步 wm_material_stock（已有批次）==========
UPDATE wm_material_stock s
JOIN inventory_batch ib ON s.batch_id = ib.batch_id
SET s.quantity_onhand = ib.available_qty,
    s.quantity_reserved = ib.locked_qty,
    s.expire_date = ib.expire_date
WHERE ib.batch_no IN (
  'BATCH-MOTOR-202607', 'BATCH-BLADE-202607', 'BATCH-SHELL-202607',
  'BATCH-SCREW-202607', 'BATCH-PACK-202607'
);

-- ========== 4. 补充批次写入 wm_material_stock ==========
INSERT INTO wm_material_stock (
  item_id, item_code, item_name, specification, unit_name,
  batch_id, batch_code, warehouse_id, warehouse_code, warehouse_name,
  location_id, location_code, location_name, area_id, area_code, area_name,
  quantity_onhand, quantity_reserved, recpt_date, expire_date, frozen_flag, create_time
)
SELECT ib.material_id, m.material_code, m.material_name, '', IFNULL(u.unit_name, 'PCS'),
  ib.batch_id, ib.batch_no, ib.warehouse_id, w.warehouse_code, w.warehouse_name,
  sz.zone_id, sz.zone_code, sz.zone_name, sb.bin_id, sb.bin_code, sb.bin_name,
  ib.available_qty, ib.locked_qty, ib.received_at, ib.expire_date, 'N', NOW()
FROM inventory_batch ib
JOIN material m ON m.material_id = ib.material_id
JOIN warehouse w ON w.warehouse_id = ib.warehouse_id
LEFT JOIN uom u ON u.unit_id = m.unit_id
LEFT JOIN storage_bin sb ON sb.legacy_location_id = ib.location_id
LEFT JOIN storage_zone sz ON sz.zone_id = sb.zone_id
WHERE ib.batch_no IN (
  'BATCH-MOTOR-SUPP-20260714', 'BATCH-BLADE-SUPP-20260714', 'BATCH-SHELL-SUPP-20260714',
  'BATCH-SCREW-SUPP-20260714', 'BATCH-PACK-SUPP-20260714'
)
AND NOT EXISTS (SELECT 1 FROM wm_material_stock s WHERE s.batch_id = ib.batch_id);

UPDATE wm_material_stock s
JOIN inventory_batch ib ON s.batch_id = ib.batch_id
SET s.quantity_onhand = ib.available_qty,
    s.quantity_reserved = ib.locked_qty
WHERE ib.batch_no IN (
  'BATCH-MOTOR-SUPP-20260714', 'BATCH-BLADE-SUPP-20260714', 'BATCH-SHELL-SUPP-20260714',
  'BATCH-SCREW-SUPP-20260714', 'BATCH-PACK-SUPP-20260714'
);
