-- Migration: Add 3D model position columns to production_line
-- For mapping line locations on the factory floor 3D model

ALTER TABLE production_line
  ADD COLUMN model_pos_x DECIMAL(12,2) NULL COMMENT '3D model X coordinate',
  ADD COLUMN model_pos_y DECIMAL(12,2) NULL COMMENT '3D model Y coordinate (ground height)',
  ADD COLUMN model_pos_z DECIMAL(12,2) NULL COMMENT '3D model Z coordinate';
