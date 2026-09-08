-- Seed 3D model positions for demo production lines (idempotent)
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -1000 WHERE line_code = 'LINE-FAN-01' AND model_pos_x IS NULL;
UPDATE production_line SET model_pos_x = 529,  model_pos_y = 10, model_pos_z = -2500 WHERE line_code = 'LINE-FAN-02' AND model_pos_x IS NULL;
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -4000 WHERE line_code = 'LINE-FAN-03' AND model_pos_x IS NULL;

-- Force refresh known demo lines even if previously set differently
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -1000 WHERE line_code = 'LINE-FAN-01';
UPDATE production_line SET model_pos_x = 529,  model_pos_y = 10, model_pos_z = -2500 WHERE line_code = 'LINE-FAN-02';
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -4000 WHERE line_code = 'LINE-FAN-03';
