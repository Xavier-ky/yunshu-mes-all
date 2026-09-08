-- Remove orphaned native device references after V29 dropped device / device_oee_daily

DROP TABLE IF EXISTS device_integration_config;

SET @fk_exists := (
  SELECT COUNT(*)
  FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'andon_event'
    AND constraint_name = 'fk_andon_event_device'
    AND constraint_type = 'FOREIGN KEY'
);
SET @drop_fk_sql := IF(
  @fk_exists > 0,
  'ALTER TABLE andon_event DROP FOREIGN KEY fk_andon_event_device',
  'SELECT 1'
);
PREPARE drop_fk_stmt FROM @drop_fk_sql;
EXECUTE drop_fk_stmt;
DEALLOCATE PREPARE drop_fk_stmt;
