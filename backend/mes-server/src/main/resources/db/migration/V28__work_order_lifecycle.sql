-- 工单生命周期状态（流程打通脊柱；nullable 兼容存量工单；幂等）
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND COLUMN_NAME = 'lifecycle_status'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE work_order ADD COLUMN lifecycle_status VARCHAR(32) NULL COMMENT ''DRAFT/RELEASED/SCHEDULED/KITTING_OK/MATERIAL_ISSUED/IN_PROGRESS/QC_PENDING/QC_PASSED/QC_FAILED/COMPLETED'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'work_order'
    AND INDEX_NAME = 'idx_work_order_lifecycle'
);
SET @sql2 := IF(@idx_exists = 0,
  'CREATE INDEX idx_work_order_lifecycle ON work_order (lifecycle_status)',
  'SELECT 1');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
