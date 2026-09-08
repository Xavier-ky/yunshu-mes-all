-- yunshu planning demo seed (idempotent inserts)

INSERT INTO product (product_code, product_name, product_model, product_category, status)
SELECT 'P-FAN-001', '落地扇整机', 'FS-40-A', 'FINISHED', 'ENABLED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product WHERE product_code = 'P-FAN-001');

INSERT INTO material (material_code, material_name, material_type, unit_id, is_key_material, status)
SELECT 'M-MOTOR-001', '交流电机', 'MOTOR', (SELECT unit_id FROM uom WHERE unit_code = 'PCS' LIMIT 1), 1, 'ENABLED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM material WHERE material_code = 'M-MOTOR-001');

INSERT INTO customer_order (order_no, customer_name, order_date, delivery_date, status)
SELECT 'SO-2026-001', '华东电机有限公司', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'CONFIRMED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM customer_order WHERE order_no = 'SO-2026-001');

INSERT INTO work_order (
  work_order_no, work_order_name, product_id, plan_qty, status,
  work_order_type, order_source, source_code, client_name, request_date, ancestors
)
SELECT 'WO-2026-001', '落地扇生产工单', p.product_id, 100, 'PREPARE',
       'SELF', 'ORDER', 'SO-2026-001', '华东电机有限公司', DATE_ADD(NOW(), INTERVAL 7 DAY), '0'
FROM product p
WHERE p.product_code = 'P-FAN-001'
  AND NOT EXISTS (SELECT 1 FROM work_order WHERE work_order_no = 'WO-2026-001');
