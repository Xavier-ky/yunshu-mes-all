-- Access hub org hierarchy demo. Safe to rerun; keeps existing dept_id 1–6.
USE fan_mes;

INSERT INTO sys_department (dept_code, dept_name, dept_type, parent_dept_id, ancestors, status, order_num)
SELECT 'CORP', '云枢智造', 'COMPANY', NULL, '0', 'ENABLED', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_department WHERE dept_code = 'CORP' AND is_deleted = 0
);

SET @corp_id := (SELECT dept_id FROM sys_department WHERE dept_code = 'CORP' AND is_deleted = 0 LIMIT 1);

UPDATE sys_department
SET parent_dept_id = @corp_id,
    ancestors = CONCAT('0,', @corp_id)
WHERE dept_code IN ('MGMT', 'PMC', 'PROD', 'WH', 'QC', 'EQ')
  AND is_deleted = 0;

SET @mgmt_id := (SELECT dept_id FROM sys_department WHERE dept_code = 'MGMT' AND is_deleted = 0 LIMIT 1);
SET @prod_id := (SELECT dept_id FROM sys_department WHERE dept_code = 'PROD' AND is_deleted = 0 LIMIT 1);
SET @wh_id := (SELECT dept_id FROM sys_department WHERE dept_code = 'WH' AND is_deleted = 0 LIMIT 1);

INSERT INTO sys_department (dept_code, dept_name, dept_type, parent_dept_id, ancestors, status, order_num)
VALUES
  ('PROD_WS1', '一车间', 'WORKSHOP', @prod_id, CONCAT('0,', @corp_id, ',', @prod_id), 'ENABLED', 1),
  ('PROD_WS2', '二车间', 'WORKSHOP', @prod_id, CONCAT('0,', @corp_id, ',', @prod_id), 'ENABLED', 2),
  ('WH_RAW', '原料库', 'WAREHOUSE', @wh_id, CONCAT('0,', @corp_id, ',', @wh_id), 'ENABLED', 1),
  ('WH_FG', '成品库', 'WAREHOUSE', @wh_id, CONCAT('0,', @corp_id, ',', @wh_id), 'ENABLED', 2),
  ('MGMT_IT', '信息中心', 'MANAGEMENT', @mgmt_id, CONCAT('0,', @corp_id, ',', @mgmt_id), 'ENABLED', 1)
ON DUPLICATE KEY UPDATE
  dept_name = VALUES(dept_name),
  dept_type = VALUES(dept_type),
  parent_dept_id = VALUES(parent_dept_id),
  ancestors = VALUES(ancestors),
  status = VALUES(status),
  order_num = VALUES(order_num);

UPDATE sys_user
SET dept_id = (SELECT dept_id FROM sys_department WHERE dept_code = 'PROD_WS1' AND is_deleted = 0 LIMIT 1)
WHERE username = 'worker01'
  AND is_deleted = 0;
