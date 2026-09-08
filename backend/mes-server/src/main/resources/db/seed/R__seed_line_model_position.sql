-- 产线 3D 模型坐标（与 lineMonitor mock / 3D 场景对齐）
-- 产线 3D 标记锚点（总览页 factory.glb 使用前端 DEFAULT_LINE_UV 归一化坐标）
-- 以下绝对坐标保留供 API/创建产线兼容；总览 3D 对 LINE-FAN-* / L-* 优先走 UV 映射
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -1000 WHERE line_code = 'LINE-FAN-01';
UPDATE production_line SET model_pos_x = 529,  model_pos_y = 10, model_pos_z = -2500 WHERE line_code = 'LINE-FAN-02';
UPDATE production_line SET model_pos_x = 300,  model_pos_y = 10, model_pos_z = -4000 WHERE line_code = 'LINE-FAN-03';
