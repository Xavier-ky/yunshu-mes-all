-- Planning module demo seed. Safe to rerun (Flyway repeatable).
-- Depends on R__seed_basic_data.sql (master data, WO-20260701~03, DT-20260701~04).

USE fan_mes;

-- ========== 客户订单（CO-20260702 ~ CO-20260705）==========

INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status, created_by)
VALUES
  ('CO-20260702', '南方家电批发', '2026-07-02', '2026-07-18', 'CREATED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)),
  ('CO-20260703', '北方商城集团', '2026-07-03', '2026-07-20', 'CONFIRMED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)),
  ('CO-20260704', '西域电器经销', '2026-07-04', '2026-07-22', 'CONFIRMED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1)),
  ('CO-20260705', '珠三角家电连锁', '2026-07-05', '2026-07-10', 'COMPLETED',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01' LIMIT 1))
ON DUPLICATE KEY UPDATE
  customer_name = VALUES(customer_name),
  order_date = VALUES(order_date),
  delivery_date = VALUES(delivery_date),
  status = VALUES(status);

-- ========== 订单明细（FAN-FS40-A，含已有 CO-20260701）==========

INSERT INTO customer_order_item (order_id, product_id, order_qty, technical_requirement)
SELECT o.order_id, p.product_id, v.order_qty, v.technical_requirement
FROM (
  SELECT 'CO-20260701' AS order_no, 500.0000 AS order_qty, '华东渠道标准款 FS40-A' AS technical_requirement
  UNION ALL SELECT 'CO-20260702', 280.0000, '新单待确认，白色标准款'
  UNION ALL SELECT 'CO-20260703', 250.0000, '北方商城夏季促销批次'
  UNION ALL SELECT 'CO-20260704', 180.0000, '西域经销贴牌包装'
  UNION ALL SELECT 'CO-20260705', 400.0000, '珠三角连锁已交付批次'
) v
JOIN customer_order o ON o.order_no = v.order_no
JOIN product p ON p.product_code = 'FAN-FS40-A'
WHERE NOT EXISTS (
  SELECT 1 FROM customer_order_item oi
  WHERE oi.order_id = o.order_id AND oi.product_id = p.product_id
);

UPDATE customer_order_item oi
JOIN customer_order o ON o.order_id = oi.order_id
JOIN product p ON p.product_id = oi.product_id AND p.product_code = 'FAN-FS40-A'
JOIN (
  SELECT 'CO-20260701' AS order_no, 500.0000 AS order_qty, '华东渠道标准款 FS40-A' AS technical_requirement
  UNION ALL SELECT 'CO-20260702', 280.0000, '新单待确认，白色标准款'
  UNION ALL SELECT 'CO-20260703', 250.0000, '北方商城夏季促销批次'
  UNION ALL SELECT 'CO-20260704', 180.0000, '西域经销贴牌包装'
  UNION ALL SELECT 'CO-20260705', 400.0000, '珠三角连锁已交付批次'
) v ON v.order_no = o.order_no
SET oi.order_qty = v.order_qty, oi.technical_requirement = v.technical_requirement;

-- ========== 生产工单（WO-20260704 ~ WO-20260706）==========

INSERT INTO work_order (
  work_order_no, order_id, order_item_id, product_id, bom_id, route_id,
  plan_qty, completed_qty, defect_qty, status,
  planned_start_time, planned_end_time, actual_start_time, actual_end_time, priority
)
VALUES
  ('WO-20260704',
   (SELECT order_id FROM customer_order WHERE order_no = 'CO-20260703'),
   (SELECT oi.order_item_id FROM customer_order_item oi
    JOIN customer_order o ON o.order_id = oi.order_id
    JOIN product p ON p.product_id = oi.product_id AND p.product_code = 'FAN-FS40-A'
    WHERE o.order_no = 'CO-20260703' LIMIT 1),
   (SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'),
   (SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'),
   (SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'),
   250, 0, 0, 'CREATED',
   DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), NULL, NULL, 'NORMAL'),
  ('WO-20260705',
   (SELECT order_id FROM customer_order WHERE order_no = 'CO-20260705'),
   (SELECT oi.order_item_id FROM customer_order_item oi
    JOIN customer_order o ON o.order_id = oi.order_id
    JOIN product p ON p.product_id = oi.product_id AND p.product_code = 'FAN-FS40-A'
    WHERE o.order_no = 'CO-20260705' LIMIT 1),
   (SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'),
   (SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'),
   (SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'),
   400, 400, 3, 'COMPLETED',
   DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY),
   DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'NORMAL'),
  ('WO-20260706',
   (SELECT order_id FROM customer_order WHERE order_no = 'CO-20260702'),
   (SELECT oi.order_item_id FROM customer_order_item oi
    JOIN customer_order o ON o.order_id = oi.order_id
    JOIN product p ON p.product_id = oi.product_id AND p.product_code = 'FAN-FS40-A'
    WHERE o.order_no = 'CO-20260702' LIMIT 1),
   (SELECT product_id FROM product WHERE product_code = 'FAN-FS40-A'),
   (SELECT bom_id FROM bom WHERE bom_code = 'BOM-FAN-FS40-A' AND version_no = 'V1.0'),
   (SELECT route_id FROM process_route WHERE route_code = 'ROUTE-FS40-A' AND version_no = 'V1.0'),
   100, 0, 0, 'CANCELLED',
   DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 8 DAY), NULL, NULL, 'LOW')
ON DUPLICATE KEY UPDATE
  order_id = VALUES(order_id),
  order_item_id = VALUES(order_item_id),
  plan_qty = VALUES(plan_qty),
  completed_qty = VALUES(completed_qty),
  defect_qty = VALUES(defect_qty),
  status = VALUES(status),
  planned_start_time = VALUES(planned_start_time),
  planned_end_time = VALUES(planned_end_time),
  actual_start_time = VALUES(actual_start_time),
  actual_end_time = VALUES(actual_end_time),
  priority = VALUES(priority);

-- ========== 原材料库存批次（WH-RAW / RAW-A01）==========

INSERT INTO inventory_batch (
  material_id, warehouse_id, location_id, batch_no, supplier_batch_no,
  available_qty, locked_qty, quality_status, received_at, expire_date, status
)
VALUES
  ((SELECT material_id FROM material WHERE material_code = 'MAT-MOTOR-55W'),
   (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
   NULL,
   'BATCH-MOTOR-202607', 'SUP-MOTOR-2406', 2480.0000, 120.0000, 'QUALIFIED',
   DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'IN_STOCK'),
  ((SELECT material_id FROM material WHERE material_code = 'MAT-BLADE-40'),
   (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
   NULL,
   'BATCH-BLADE-202607', 'SUP-BLADE-2406', 1860.0000, 0.0000, 'QUALIFIED',
   DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 730 DAY), 'IN_STOCK'),
  ((SELECT material_id FROM material WHERE material_code = 'MAT-SHELL-FS40'),
   (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
   NULL,
   'BATCH-SHELL-202607', 'SUP-SHELL-2406', 1520.0000, 40.0000, 'QUALIFIED',
   DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(CURDATE(), INTERVAL 540 DAY), 'IN_STOCK'),
  ((SELECT material_id FROM material WHERE material_code = 'MAT-SCREW-M4'),
   (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
   NULL,
   'BATCH-SCREW-202607', 'SUP-SCREW-2405', 168.0000, 0.0000, 'QUALIFIED',
   DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY), 'IN_STOCK'),
  ((SELECT material_id FROM material WHERE material_code = 'MAT-PACK-FS40'),
   (SELECT warehouse_id FROM warehouse WHERE warehouse_code = 'WH-RAW'),
   NULL,
   'BATCH-PACK-202607', 'SUP-PACK-2406', 1180.0000, 0.0000, 'QUALIFIED',
   DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 365 DAY), 'IN_STOCK')
ON DUPLICATE KEY UPDATE
  warehouse_id = VALUES(warehouse_id),
  location_id = VALUES(location_id),
  available_qty = VALUES(available_qty),
  locked_qty = VALUES(locked_qty),
  quality_status = VALUES(quality_status),
  received_at = VALUES(received_at),
  expire_date = VALUES(expire_date),
  status = VALUES(status);

-- ========== 生产任务 ==========

INSERT INTO production_task (task_no, work_order_id, line_id, shift_id, task_date, task_qty, completed_qty, status, start_time, end_time)
VALUES
  ('PT-20260704',
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260704'),
   (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-01'),
   (SELECT shift_id FROM factory_shift WHERE shift_code = 'DAY'),
   CURDATE(), 250, 0, 'CREATED', NULL, NULL),
  ('PT-20260705',
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260705'),
   (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-02'),
   (SELECT shift_id FROM factory_shift WHERE shift_code = 'DAY'),
   DATE_SUB(CURDATE(), INTERVAL 5 DAY), 400, 400, 'COMPLETED',
   DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
  ('PT-20260706',
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260706'),
   (SELECT line_id FROM production_line WHERE line_code = 'LINE-FAN-03'),
   (SELECT shift_id FROM factory_shift WHERE shift_code = 'DAY'),
   DATE_ADD(CURDATE(), INTERVAL 2 DAY), 100, 0, 'CANCELLED', NULL, NULL)
ON DUPLICATE KEY UPDATE
  task_qty = VALUES(task_qty),
  completed_qty = VALUES(completed_qty),
  status = VALUES(status),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time);

-- ========== 派工任务（补充 DT-20260705 ~ DT-20260709）==========

INSERT INTO dispatch_task (
  dispatch_no, task_id, work_order_id, step_id, station_id, operator_id,
  planned_qty, completed_qty, status, planned_start_time, actual_start_time, actual_end_time
)
VALUES
  ('DT-20260705',
   (SELECT task_id FROM production_task WHERE task_no = 'PT-20260704'),
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260704'),
   (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
   (SELECT station_id FROM workstation WHERE station_code = 'ST-01'),
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   125, 0, 'CREATED', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL),
  ('DT-20260706',
   (SELECT task_id FROM production_task WHERE task_no = 'PT-20260704'),
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260704'),
   (SELECT step_id FROM process_step WHERE step_code = 'STEP-BLADE'),
   (SELECT station_id FROM workstation WHERE station_code = 'ST-02'),
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   125, 0, 'CREATED', DATE_ADD(NOW(), INTERVAL 1 DAY), NULL, NULL),
  ('DT-20260707',
   (SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260705'),
   (SELECT step_id FROM process_step WHERE step_code = 'STEP-MOTOR'),
   (SELECT station_id FROM workstation WHERE station_code = 'ST-05'),
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   200, 200, 'COMPLETED',
   DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
  ('DT-20260708',
   (SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260705'),
   (SELECT step_id FROM process_step WHERE step_code = 'STEP-PACK'),
   (SELECT station_id FROM workstation WHERE station_code = 'ST-06'),
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   200, 200, 'COMPLETED',
   DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ('DT-20260709',
   (SELECT task_id FROM production_task WHERE task_no = 'PT-20260703'),
   (SELECT work_order_id FROM work_order WHERE work_order_no = 'WO-20260703'),
   (SELECT step_id FROM process_step WHERE step_code = 'STEP-PACK'),
   (SELECT station_id FROM workstation WHERE station_code = 'ST-08'),
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   100, 45, 'RUNNING', NOW(), NOW(), NULL)
ON DUPLICATE KEY UPDATE
  planned_qty = VALUES(planned_qty),
  completed_qty = VALUES(completed_qty),
  status = VALUES(status),
  planned_start_time = VALUES(planned_start_time),
  actual_start_time = VALUES(actual_start_time),
  actual_end_time = VALUES(actual_end_time);

-- ========== 任务操作日志 ==========

DELETE FROM task_operation_log WHERE remark = 'planning demo seed';

INSERT IGNORE INTO task_operation_log (task_id, dispatch_id, operation_type, operator_id, operation_time, remark)
VALUES
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260704'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260705'),
   'START',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01'),
   CONCAT(CURDATE(), ' 08:00:00'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260707'),
   'START',
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 6 DAY), '%Y-%m-%d %H:%i:%s'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260707'),
   'FINISH',
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 5 DAY), '%Y-%m-%d %H:%i:%s'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260708'),
   'START',
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 4 DAY), '%Y-%m-%d %H:%i:%s'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260705'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260708'),
   'FINISH',
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 3 DAY), '%Y-%m-%d %H:%i:%s'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260703'),
   (SELECT dispatch_id FROM dispatch_task WHERE dispatch_no = 'DT-20260709'),
   'START',
   (SELECT user_id FROM sys_user WHERE username = 'worker01'),
   CONCAT(CURDATE(), ' 09:10:00'),
   'planning demo seed'),
  ((SELECT task_id FROM production_task WHERE task_no = 'PT-20260706'),
   NULL,
   'CANCEL',
   (SELECT user_id FROM sys_user WHERE username = 'supervisor01'),
   CONCAT(CURDATE(), ' 10:00:00'),
   'planning demo seed');
