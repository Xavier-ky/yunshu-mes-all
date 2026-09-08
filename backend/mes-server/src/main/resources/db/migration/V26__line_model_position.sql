-- Migration: Add 3D model position columns to production_line (idempotent)
-- For mapping line locations on the factory floor 3D model

DROP PROCEDURE IF EXISTS sp_add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE sp_add_column_if_not_exists(
  IN p_table VARCHAR(64),
  IN p_column VARCHAR(64),
  IN p_definition VARCHAR(512)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_column
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END //
DELIMITER ;

CALL sp_add_column_if_not_exists('production_line', 'model_pos_x', "DECIMAL(12,2) NULL COMMENT '3D model X coordinate'");
CALL sp_add_column_if_not_exists('production_line', 'model_pos_y', "DECIMAL(12,2) NULL COMMENT '3D model Y coordinate (ground height)'");
CALL sp_add_column_if_not_exists('production_line', 'model_pos_z', "DECIMAL(12,2) NULL COMMENT '3D model Z coordinate'");
